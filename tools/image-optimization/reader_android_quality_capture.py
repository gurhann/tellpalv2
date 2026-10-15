"""Reader-only, bounded Android captures; native stdout is always consumed separately."""
import base64
from hashlib import sha256
from io import BytesIO
import math
import os
from pathlib import Path
import selectors
import signal
import subprocess
import threading
import time

from PIL import Image
import native_entry_driver as host
from native_entry_config import (EntryError, HASH, READER_CASES, exact_keys, finite_number,
    private_file, private_output, require, strict_json, write_private)
from native_artwork_comparison import (CAPTURE_ID_FIELDS, CAPTURE_GEOMETRY_FIELDS,
    CAPTURE_CONTEXT_FIELDS, stable_capture_binding)

QUALITY_BATCHES = ('reader_quality_full_r1', 'reader_quality_100_r1', 'reader_quality_075_r1')
MAX_PNG = 8 * 1024 * 1024


def validate_reader_capture_binding(row, binding, case, geometry):
    """Use independently admitted per-case providers and discovered reader geometry."""
    require(type(binding) is dict and binding.get('batchId') in QUALITY_BATCHES and
        case in READER_CASES[:2] and type(binding.get('providers')) is dict and
        case in binding['providers'], 'reader_capture_binding_rejected')
    exact_keys(row, CAPTURE_ID_FIELDS | CAPTURE_GEOMETRY_FIELDS | CAPTURE_CONTEXT_FIELDS |
        {'nativeObservedAtEpochMilliseconds'})
    provider = binding['providers'][case]
    expected = {'generation': binding.get('generation'), 'sessionSha256': binding.get('sessionSha256'),
        'caseSha256': sha256(case.encode()).hexdigest(),
        'providerSha256': provider.get('expectedProviderSha256'),
        'attemptSha256': provider.get('expectedAttemptSha256')}
    require(type(expected['generation']) is int and expected['generation'] >= 0 and
        all(type(expected[key]) is str and HASH.fullmatch(expected[key]) for key in
            CAPTURE_ID_FIELDS - {'generation'}) and type(row['generation']) is int and
        all(row[key] == value for key, value in expected.items()), 'reader_capture_binding_rejected')
    require(type(geometry) is dict and geometry.get('orientation') == 'portrait' and
        binding.get('geometry') == geometry and
        all(finite_number(row[key]) and row[key] > 0 and row[key] == geometry.get(key)
            for key in ('observedSlotWidth', 'observedSlotHeight', 'observedDpr')) and
        all(type(provider.get(expected_key)) is int and provider[expected_key] > 0 and
            type(row[key]) is int and row[key] == provider[expected_key] for key, expected_key in
            (('actualDecodeWidth', 'expectedDecodeWidth'), ('actualDecodeHeight', 'expectedDecodeHeight'))) and
        row['actualDecodeWidth'] * row['actualDecodeHeight'] <= 16777216 and
        row['phase'] == 'hold' and row['orientation'] == geometry['orientation'] and
        row['nativeContext'] == 'foreground_activity', 'reader_capture_context_rejected')
    observed, deadline = row['nativeObservedAtEpochMilliseconds'], row['holdDeadlineEpochMilliseconds']
    require(type(observed) is int and type(deadline) is int and observed > 0 and
        0 < deadline - observed <= 10000, 'reader_capture_clock_rejected')


def screencap_command(config):
    return ['adb', '-s', config.values['deviceSelector'], 'exec-out', 'screencap', '-p']


def capture_png(config, output, *, deadline, read_binding, binding, case, geometry, cancelled):
    """Keep the original PNG before validating a fresh after-marker; kill owned children."""
    require(config.values.get('platform') == 'android' and config.values.get('batchId') in QUALITY_BATCHES and
        type(binding) is dict and config.values['batchId'] == binding.get('batchId'),
        'reader_capture_selection_rejected')
    before = read_binding(case, first=True)
    validate_reader_capture_binding(before, binding, case, geometry)
    require(before['holdDeadlineEpochMilliseconds'] - before['nativeObservedAtEpochMilliseconds'] > 6000,
        'reader_capture_hold_budget_refused')
    process, selector = None, selectors.DefaultSelector()
    chunks, size, retained = [], 0, False
    try:
        require(not cancelled.is_set() and time.monotonic() < deadline, 'reader_capture_cancelled')
        process = subprocess.Popen(screencap_command(config), stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, stdin=subprocess.DEVNULL, env=host.tool_environment(), start_new_session=True)
        selector.register(process.stdout, selectors.EVENT_READ)
        expires = min(deadline, time.monotonic() + 5)
        while selector.get_map() or process.poll() is None:
            require(not cancelled.is_set(), 'reader_capture_cancelled')
            require(time.monotonic() < expires, 'reader_capture_timeout')
            for key, _ in selector.select(min(.05, max(0, expires - time.monotonic()))):
                chunk = os.read(key.fileobj.fileno(), 65536)
                if not chunk:
                    selector.unregister(key.fileobj)
                    continue
                chunks.append(chunk[:max(0, MAX_PNG - size)])
                size += len(chunk)
                require(size <= MAX_PNG, 'reader_capture_capacity')
        require(process.wait(timeout=max(.001, expires - time.monotonic())) == 0, 'reader_capture_failed')
        png = b''.join(chunks)
        write_private(Path(output) / 'quality.png', png)
        retained = True
        after = read_binding(case, first=False)
        validate_reader_capture_binding(after, binding, case, geometry)
        require(stable_capture_binding(before) == stable_capture_binding(after), 'reader_capture_binding_changed')
        require(before['nativeObservedAtEpochMilliseconds'] < after['nativeObservedAtEpochMilliseconds'],
            'reader_capture_clock_rejected')
        receipt = {'schemaVersion': 1, 'before': before, 'after': after,
            'beforeReadEpochMilliseconds': before['nativeObservedAtEpochMilliseconds'],
            'afterReadEpochMilliseconds': after['nativeObservedAtEpochMilliseconds'],
            'pngSha256': sha256(png).hexdigest(), 'pngBytes': len(png),
            'capture': 'exact_device_adb_screencap', 'orientation': 'portrait', 'attachments': False}
        write_private(Path(output) / 'quality-capture.json', receipt)
        return receipt
    finally:
        if process is not None:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait(timeout=1)
            process.stdout.close()
        selector.close()
        if chunks and not retained:
            write_private(Path(output) / 'screencap-incomplete.bin', b''.join(chunks))


def qualify_collection(root, binding, manifest, geometry, provenance):
    """Re-read both immutable captures; a partial collection can never be published."""
    root = Path(root)
    worker = strict_json(private_file(root.parent / 'reader-quality-worker.json'))
    exact_keys(worker, ('completedCases', 'workerTerminated', 'reason', 'qualityAccepted'))
    require(worker['workerTerminated'] is True and worker['completedCases'] == list(READER_CASES[:2]) and
        worker['reason'] is None and worker['qualityAccepted'] is None, 'reader_capture_worker_unqualified')
    require(set(path.name for path in root.iterdir()) == set(READER_CASES[:2]) and
        len(manifest.entries) == len(provenance) == 2, 'reader_capture_collection_incomplete')
    results = []
    for case, entry, source in zip(READER_CASES[:2], manifest.entries, provenance):
        folder = root / case
        require(set(path.name for path in folder.iterdir()) == {'quality.png', 'quality-capture.json'} and
            entry['caseId'] == case and source['sourceSha256'] == entry['sourceSha256'] and
            source['referenceSha256'] == entry['referenceSha256'] and
            source['outputSha256'] == entry['outputSha256'] and source['pixelsVerified'] is True,
            'reader_capture_provenance_rejected')
        png = private_file(folder / 'quality.png', MAX_PNG)
        sidecar = private_file(folder / 'quality-capture.json')
        receipt = strict_json(sidecar)
        exact_keys(receipt, ('schemaVersion', 'before', 'after', 'beforeReadEpochMilliseconds',
            'afterReadEpochMilliseconds', 'pngSha256', 'pngBytes', 'capture', 'orientation', 'attachments'))
        require(type(receipt['schemaVersion']) is int and receipt['schemaVersion'] == 1 and
            type(receipt['pngBytes']) is int and receipt['pngBytes'] == len(png) > 0 and
            receipt['pngSha256'] == sha256(png).hexdigest() and
            receipt['capture'] == 'exact_device_adb_screencap' and receipt['orientation'] == 'portrait' and
            receipt['attachments'] is False, 'reader_capture_unverified')
        for row in (receipt['before'], receipt['after']):
            validate_reader_capture_binding(row, binding, case, geometry)
        require(stable_capture_binding(receipt['before']) == stable_capture_binding(receipt['after']),
            'reader_capture_binding_changed')
        before, after = receipt['beforeReadEpochMilliseconds'], receipt['afterReadEpochMilliseconds']
        require(type(before) is int and type(after) is int and
            before == receipt['before']['nativeObservedAtEpochMilliseconds'] and
            after == receipt['after']['nativeObservedAtEpochMilliseconds'] and before < after <
            receipt['before']['holdDeadlineEpochMilliseconds'], 'reader_capture_clock_rejected')
        try:
            with Image.open(BytesIO(png)) as image:
                require(image.format == 'PNG' and image.n_frames == 1 and 0 < image.width < image.height and
                    image.width * image.height <= 16777216 and image.width >= math.ceil(
                    geometry['observedSlotWidth'] * geometry['observedDpr']) and image.height >= math.ceil(
                    geometry['observedSlotHeight'] * geometry['observedDpr']), 'reader_capture_format_rejected')
                image.verify()
            with Image.open(BytesIO(png)) as image:
                image.load()
                dimensions = list(image.size)
        except EntryError:
            raise
        except Exception:
            raise EntryError('reader_capture_format_rejected') from None
        results.append({'caseId': case, 'contentId': source['contentId'], 'pageNumber': 1,
            'captureSha256': sha256(png).hexdigest(), 'sidecarSha256': sha256(sidecar).hexdigest(),
            'captureDimensions': dimensions, 'providerBinding': stable_capture_binding(receipt['before']),
            'beforeReadEpochMilliseconds': before, 'afterReadEpochMilliseconds': after, 'provenance': source,
            'qualityAccepted': None})
    return {'batchId': binding['batchId'], 'status': 'complete', 'captures': results,
        'qualityAccepted': None, 'qualityQualifiedSavings': None}


class ReaderAndroidQualityCollector:
    """One finite worker performs two captures while Pairing keeps draining stdout."""
    def __init__(self, config, output, *, binding, geometry, deadline):
        require(config.values['platform'] == 'android' and config.values['batchId'] in QUALITY_BATCHES,
            'reader_capture_selection_rejected')
        self.config, self.binding, self.geometry, self.deadline = config, binding, geometry, deadline
        self.root = private_output(Path(output) / 'reader-quality-captures', [], create=True)
        self.output = Path(output)
        self.condition, self.cancelled = threading.Condition(), threading.Event()
        self.fields, self.row, self.revision, self.received_at = {}, None, 0, 0
        self.worker, self.error, self.completed = None, None, []
        self.case_index, self.native_observed = -1, -1
        self.closed = False
        self.active_capture = None

    def feed(self, line):
        prefix = b'INSTRUMENTATION_STATUS: '
        if line.startswith(prefix):
            field, sep, value = line[len(prefix):].partition(b'=')
            if sep and field in (b'event', b'bindingBase64'):
                require(field not in self.fields, 'reader_capture_marker_duplicate')
                self.fields[field] = value
            return
        if not line.startswith(b'INSTRUMENTATION_STATUS_CODE:'):
            return
        fields, self.fields = self.fields, {}
        if fields.get(b'event') != b'quality-binding':
            return
        require(line.strip() == b'INSTRUMENTATION_STATUS_CODE: 3' and
            set(fields) == {b'event', b'bindingBase64'} and len(fields[b'bindingBase64']) <= 5500,
            'reader_capture_marker_rejected')
        try:
            row = strict_json(base64.b64decode(fields[b'bindingBase64'], validate=True), 4096)
        except ValueError:
            raise EntryError('reader_capture_marker_rejected') from None
        if row is not None:
            matches = [case for case in READER_CASES[:2] if type(row) is dict and
                row.get('caseSha256') == sha256(case.encode()).hexdigest()]
            require(len(matches) == 1, 'reader_capture_case_rejected')
            validate_reader_capture_binding(row, self.binding(), matches[0], self.geometry)
            index = READER_CASES.index(matches[0])
            observed = row['nativeObservedAtEpochMilliseconds']
            require(index in (self.case_index, self.case_index + 1) and observed > self.native_observed and
                abs(time.time() * 1000 - observed) <= 2000, 'reader_capture_marker_stale')
            self.case_index, self.native_observed = index, observed
        with self.condition:
            require(self.active_capture is None or row is not None and
                stable_capture_binding(row) == self.active_capture, 'reader_capture_binding_changed')
            self.row, self.received_at = row, time.monotonic()
            self.revision += 1
            if row is not None and self.worker is None:
                self.worker = threading.Thread(target=self._collect, name='reader-quality-capture', daemon=True)
                self.worker.start()
            self.condition.notify_all()

    def read(self, case, *, first):
        expires = self.deadline if first else min(self.deadline, time.monotonic() + .6)
        with self.condition:
            revision = self.revision
            while not self.cancelled.is_set() and time.monotonic() < expires:
                if self.revision > revision:
                    revision = self.revision
                    if first and (self.row is None or self.row['caseSha256'] != sha256(case.encode()).hexdigest()):
                        require(self.row is None or self.case_index < READER_CASES.index(case),
                            'reader_capture_case_order_rejected')
                    else:
                        require(self.row is not None and time.monotonic() - self.received_at <= .25,
                            'reader_capture_marker_unavailable')
                        validate_reader_capture_binding(self.row, self.binding(), case, self.geometry)
                        self.active_capture = stable_capture_binding(self.row) if first else None
                        return dict(self.row)
                self.condition.wait(timeout=min(.05, max(0, expires - time.monotonic())))
        raise EntryError('reader_capture_marker_unavailable')

    def _collect(self):
        try:
            for case in READER_CASES[:2]:
                folder = private_output(self.root / case, [], create=True)
                capture_png(self.config, folder, deadline=self.deadline, read_binding=self.read,
                    binding=self.binding(), case=case, geometry=self.geometry, cancelled=self.cancelled)
                self.completed.append(case)
        except Exception as error:
            self.error = str(error) if isinstance(error, EntryError) else 'reader_capture_operation_failed'

    def poll(self):
        if self.error is not None:
            raise EntryError(self.error)

    def close(self):
        if self.closed:
            return
        self.closed = True
        self.cancelled.set()
        with self.condition:
            self.condition.notify_all()
        if self.worker is not None and self.worker.ident is not None:
            self.worker.join(timeout=2)
        try:
            write_private(self.output / 'reader-quality-worker.json', {'completedCases': list(self.completed),
                'workerTerminated': self.worker is None or not self.worker.is_alive(),
                'reason': self.error, 'qualityAccepted': None})
        except Exception:
            self.error = self.error or 'reader_capture_cleanup_receipt_failed'

    def finish(self):
        require(self.worker is not None and not self.worker.is_alive() and
            self.completed == list(READER_CASES[:2]), 'reader_capture_collection_incomplete')
        self.poll()


def qualify_quality_matrix(results):
    """Exactly six independently qualified captures; human decisions remain separate."""
    require(type(results) is list and len(results) == 3 and
        tuple(result.get('batchId') for result in results) == QUALITY_BATCHES,
        'reader_quality_matrix_incomplete')
    captures, sessions = [], set()
    for result in results:
        collection = result.get('qualityCaptures', {})
        require(result.get('platform') == 'android' and result.get('qualification') == 'awaiting_human_quality' and
            result.get('deviceFactsReason') == 'observed' and type(result.get('deviceFacts')) is dict and
            result.get('lowMemoryPhysicalQualification') == 'observed' and
            collection.get('status') == 'complete' and collection.get('batchId') == result['batchId'] and
            len(collection.get('captures', [])) == 2, 'reader_quality_matrix_incomplete')
        require(all(type(result.get(key)) is str and HASH.fullmatch(result[key]) for key in
            ('sessionSha256', 'deviceUnitSha256', 'allocationSha256', 'artifactSha256', 'testArtifactSha256')),
            'reader_quality_matrix_identity_changed')
        require(result.get('sessionSha256') not in sessions, 'reader_quality_matrix_session_reused')
        sessions.add(result['sessionSha256'])
        if captures:
            require(result.get('geometry') == results[0].get('geometry') and
                result.get('deviceUnitSha256') is not None and
                result['deviceUnitSha256'] == results[0].get('deviceUnitSha256') and
                result.get('allocationSha256') is not None and
                result['allocationSha256'] == results[0].get('allocationSha256') and
                result.get('artifactSha256') == results[0].get('artifactSha256') and
                result.get('testArtifactSha256') == results[0].get('testArtifactSha256'),
                'reader_quality_matrix_identity_changed')
        require(tuple(capture.get('caseId') for capture in collection['captures']) == READER_CASES[:2],
            'reader_quality_matrix_incomplete')
        for index, item in enumerate(collection['captures']):
            require(item.get('contentId') == (71, 19)[index] and item.get('pageNumber') == 1 and
                item.get('providerBinding', {}).get('sessionSha256') == result['sessionSha256'] and
                all(type(item.get(key)) is str and HASH.fullmatch(item[key]) for key in
                    ('captureSha256', 'sidecarSha256')), 'reader_quality_matrix_incomplete')
            source = item.get('provenance')
            require(type(source) is dict and all(type(source.get(key)) is str and HASH.fullmatch(source[key])
                for key in ('sourceSha256', 'referenceSha256', 'retainedInputsSha256',
                    'originalManifestSha256', 'v7PreparationSha256')), 'reader_quality_matrix_source_missing')
            if captures:
                original = captures[index].get('provenance', {})
                require(all(source.get(key) == original.get(key) for key in ('sourceSha256',
                    'referenceSha256', 'retainedInputsSha256', 'originalManifestSha256', 'v7PreparationSha256')),
                    'reader_quality_matrix_source_changed')
        captures.extend(collection['captures'])
    return {'status': 'awaiting_human_quality', 'captureCount': 6, 'captures': captures,
        'qualityAccepted': None, 'qualityQualifiedSavings': None}
