"""Reader source reproduction, fresh native markers and actual owned-child failures."""
import base64
from copy import deepcopy
from hashlib import sha256
from io import BytesIO
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from PIL import Image
import native_entry_config as config
import reader_resolution_experiment as reader
import reader_android_quality_capture as capture
import test_reader_resolution_experiment as fixtures


class QualityInputs(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='reader-quality-test-')
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name).resolve()
        self.root.chmod(0o700)
        self.geometry = fixtures.geometry()
        self.geometry.update(observedSlotWidth=20.0, observedSlotHeight=30.0)
        self.pages, originals, references = [], [], []
        for content_id in (71, 19):
            pixels = bytes((content_id, 40, 80, 255)) * 1500
            raw = reader.MAGIC + __import__('struct').pack('>II', 30, 50) + pixels
            reference = reader.digest(raw)
            original = reader.encode_page(reader.SourcePage(1, None, 'a' * 64, reference, 30, 50, pixels),
                {'width': 30, 'height': 50})
            source = reader.digest(original)
            page = reader.SourcePage(1, None, source, reference, 30, 50, pixels, content_id, 'tr')
            self.pages.append(page)
            original_path = self.put(f'original-{content_id}.png', original)
            self.put(f'preparation-v7/normalized-references/{source}-rgba8-srgb-v1.pixels', raw)
            references.append({'source_hash': source, 'pixel_sha256': reference, 'width': 30, 'height': 50})
            originals.append({'caseId': f'c{content_id}_png_reference_r1',
                'purposeAcceptanceReceipt': 'native_quality_r1', 'mime': 'image/png',
                'sourceSha256': source, 'referenceSha256': reference, 'outputSha256': source,
                'width': 30, 'height': 50, 'byteSize': len(original), 'filePath': str(original_path)})
        preparation = config.frozen_json({'references': references})
        self.put('preparation-v7/preparation-receipt.json', preparation)
        original_manifest = config.frozen_json({'scope': config.SCOPE, 'batchId': 'native_quality_r1',
            'entries': originals})
        original_path = self.put('original-manifest.json', original_manifest)
        self.retained = self.put('retained.json', config.frozen_json({'scope': 'retained-tr-quality',
            'originalManifestPath': str(original_path), 'originalManifestSha256': reader.digest(original_manifest),
            'entries': originals, 'normalization': 'v7', 'productionActions': False}))
        pins = {**reader.PINS, 'preparation-v7/preparation-receipt.json': reader.digest(preparation)}
        patcher = patch.object(reader, 'PINS', pins)
        patcher.start(); self.addCleanup(patcher.stop)
        self.pages, self.admission = reader.admit_quality_sources(self.root, self.retained)
        primary = tuple(reader.SourcePage(index, None, 'a' * 64, 'b' * 64, 30, 50, self.pages[0].pixels)
            for index in range(1, 7))
        prepared = reader.prepare_variants(primary, self.admission, self.geometry, self.root / 'prepared',
            batches=capture.QUALITY_BATCHES, quality_pages=self.pages)
        self.manifests = {}
        for batch, receipt in prepared['manifests'].items():
            data = config.strict_json(Path(receipt['path']).read_bytes())
            public = {**data, 'entries': [{key: value for key, value in entry.items() if key != 'filePath'}
                for entry in data['entries']]}
            self.manifests[batch] = config.Manifest(tuple(data['entries']),
                {entry['caseId']: Path(entry['filePath']).read_bytes() for entry in data['entries']},
                receipt['sha256'], config.frozen_json(public))
        self.select(capture.QUALITY_BATCHES[0])
        png = BytesIO(); Image.new('RGB', (40, 60), (71, 40, 80)).save(png, format='PNG')
        self.png = png.getvalue()

    def put(self, name, data):
        path = self.root / name
        path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        config.write_private(path, data)
        return path

    def select(self, batch):
        self.manifest = self.manifests[batch]
        self.binding = reader.reader_binding(self.manifest, {'batchId': batch, 'generation': 0,
            'sessionId': batch, 'accessAlias': 'synthetic'}, self.geometry)
        self.selected = SimpleNamespace(values={'platform': 'android', 'mode': 'release', 'batchId': batch,
            'deviceSelector': 'synthetic-device'})
        self.provenance = reader.quality_variant_provenance(self.manifest, self.geometry, self.root, self.retained)

    def row(self, case=config.READER_CASES[0], *, at=None, deadline=None, **changes):
        identity = self.binding['providers'][case]
        at = int(time.time() * 1000) if at is None else at
        return {'generation': 0, 'sessionSha256': self.binding['sessionSha256'],
            'caseSha256': reader.digest(case.encode()), 'providerSha256': identity['expectedProviderSha256'],
            'attemptSha256': identity['expectedAttemptSha256'],
            **{key: self.geometry[key] for key in reader.GEOMETRY_FIELDS[:3]},
            'actualDecodeWidth': identity['expectedDecodeWidth'], 'actualDecodeHeight': identity['expectedDecodeHeight'],
            'phase': 'hold', 'orientation': 'portrait', 'nativeContext': 'foreground_activity',
            'nativeObservedAtEpochMilliseconds': at,
            'holdDeadlineEpochMilliseconds': at + 9000 if deadline is None else deadline, **changes}

    def command(self, code=None):
        return [sys.executable, '-B', '-c', code or
            ('import base64,sys; sys.stdout.buffer.write(base64.b64decode(' + repr(base64.b64encode(self.png)) + '))')]

    def marker(self, collector, row):
        for line in (b'INSTRUMENTATION_STATUS: event=quality-binding',
                b'INSTRUMENTATION_STATUS: bindingBase64=' + base64.b64encode(config.frozen_json(row)),
                b'INSTRUMENTATION_STATUS_CODE: 3'):
            collector.feed(line)

    def collection(self, root=None):
        root = (root or self.root / 'captures') / 'reader-quality-captures'
        root.parent.mkdir(mode=0o700)
        root.mkdir(mode=0o700)
        config.write_private(root.parent / 'reader-quality-worker.json', {'completedCases': list(config.READER_CASES[:2]),
            'workerTerminated': True, 'reason': None, 'qualityAccepted': None})
        for case in config.READER_CASES[:2]:
            folder = root / case; folder.mkdir(mode=0o700)
            before = self.row(case, at=1000, deadline=10000)
            after = self.row(case, at=1001, deadline=10000)
            config.write_private(folder / 'quality.png', self.png)
            config.write_private(folder / 'quality-capture.json', {'schemaVersion': 1,
                'before': before, 'after': after, 'beforeReadEpochMilliseconds': 1000,
                'afterReadEpochMilliseconds': 1001, 'pngSha256': reader.digest(self.png), 'pngBytes': len(self.png),
                'capture': 'exact_device_adb_screencap', 'orientation': 'portrait', 'attachments': False})
        return root


class ReaderCaptureTests(QualityInputs):
    def test_075_provenance_rejects_substituted_100_dimensions_before_comparing_bytes(self):
        full_size = self.manifests['reader_quality_100_r1'].entries[0]
        self.select('reader_quality_075_r1')
        entry = self.manifest.entries[0]
        self.assertNotEqual((entry['width'], entry['height']), (full_size['width'], full_size['height']))
        with patch.dict(entry, width=full_size['width'], height=full_size['height']):
            with self.assertRaisesRegex(config.EntryError, 'reader_quality_resize_binding_rejected'):
                reader.quality_variant_provenance(self.manifest, self.geometry, self.root, self.retained)

    def test_unstarted_worker_cleanup_preserves_start_failure_and_records_termination(self):
        output = self.root / 'worker-start-failure'; output.mkdir(mode=0o700)
        collector = capture.ReaderAndroidQualityCollector(self.selected, output,
            binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + 1)
        failure = RuntimeError('synthetic_worker_start_failure')
        try:
            with patch.object(threading.Thread, 'start', side_effect=failure):
                with self.assertRaises(RuntimeError) as caught:
                    self.marker(collector, self.row())
            self.assertIs(caught.exception, failure)
        finally:
            collector.close()
        collector.close()
        worker = config.strict_json((output / 'reader-quality-worker.json').read_bytes())
        self.assertTrue(worker['workerTerminated'])
        self.assertEqual(worker['completedCases'], [])
        with self.assertRaisesRegex(config.EntryError, 'collection_incomplete'):
            collector.finish()

    def test_reader_geometry_and_independent_per_case_decode_never_use_legacy280x460(self):
        for batch in capture.QUALITY_BATCHES:
            self.select(batch)
            for case in config.READER_CASES[:2]:
                capture.validate_reader_capture_binding(self.row(case), self.binding, case, self.geometry)
        for key, value in (('observedSlotWidth', 280), ('observedDpr', True), ('actualDecodeWidth', True),
                ('phase', 'reader'), ('orientation', 'landscape'), ('nativeContext', 'background_activity'),
                ('generation', True), ('sessionSha256', 'a' * 64), ('providerSha256', 'a' * 64),
                ('attemptSha256', 'a' * 64), ('caseSha256', 'a' * 64), ('unknown', 'value'),
                ('nativeObservedAtEpochMilliseconds', True), ('holdDeadlineEpochMilliseconds', True)):
            with self.subTest(key=key), self.assertRaises(config.EntryError):
                capture.validate_reader_capture_binding(self.row(**{key: value}), self.binding,
                    config.READER_CASES[0], self.geometry)
        for observed in (0, 10000, 10001, -1):
            with self.subTest(observed=observed), self.assertRaises(config.EntryError):
                capture.validate_reader_capture_binding(self.row(at=observed, deadline=10000), self.binding,
                    config.READER_CASES[0], self.geometry)

    def test_original_bound_pngs_require_exact_source_dimensions_pixels_and_bytes(self):
        for batch in capture.QUALITY_BATCHES:
            self.select(batch)
            self.assertEqual([item['contentId'] for item in self.provenance], [71, 19])
            self.assertTrue(all(item['pixelsVerified'] for item in self.provenance))
            original = self.manifest.objects[config.READER_CASES[0]]
            self.manifest.objects[config.READER_CASES[0]] = original + b'x'
            with self.assertRaisesRegex(config.EntryError, 'reader_quality_bytes_rejected'):
                reader.quality_variant_provenance(self.manifest, self.geometry, self.root, self.retained)
            self.manifest.objects[config.READER_CASES[0]] = original
        for source_root, retained in ((None, self.retained), (self.root, None)):
            with self.assertRaisesRegex(config.EntryError, 'reader_quality_sources_required'):
                reader.quality_variant_provenance(self.manifest, self.geometry, source_root, retained)
        original = self.root / 'original-71.png'
        original.write_bytes(original.read_bytes() + b'x')
        with self.assertRaisesRegex(config.EntryError, 'reader_quality_png_changed'):
            reader.admit_quality_sources(self.root, self.retained)

    def test_exact_device_command_and_real_child_png_with_native_before_after_times(self):
        self.assertEqual(capture.screencap_command(self.selected),
            ['adb', '-s', 'synthetic-device', 'exec-out', 'screencap', '-p'])
        before, after = self.row(at=1000, deadline=10000), self.row(at=1001, deadline=10000)
        reads = iter((before, after))
        output = self.root / 'direct'; output.mkdir(mode=0o700)
        with patch.object(capture, 'screencap_command', return_value=self.command()):
            receipt = capture.capture_png(self.selected, output, deadline=time.monotonic() + 2,
                read_binding=lambda *args, **kwargs: next(reads), binding=self.binding,
                case=config.READER_CASES[0], geometry=self.geometry, cancelled=threading.Event())
        self.assertEqual((output / 'quality.png').read_bytes(), self.png)
        self.assertEqual(receipt['beforeReadEpochMilliseconds'], before['nativeObservedAtEpochMilliseconds'])
        self.assertEqual(receipt['afterReadEpochMilliseconds'], after['nativeObservedAtEpochMilliseconds'])

    def test_changed_null_or_invalid_after_keeps_original_png_without_qualified_sidecar(self):
        for number, after in enumerate((None, self.row(at=1001, deadline=10001),
                self.row(at=1001, deadline=10000, actualDecodeWidth=900))):
            output = self.root / f'after-{number}'; output.mkdir(mode=0o700)
            reads = iter((self.row(at=1000, deadline=10000), after))
            with patch.object(capture, 'screencap_command', return_value=self.command()), self.assertRaises(config.EntryError):
                capture.capture_png(self.selected, output, deadline=time.monotonic() + 2,
                    read_binding=lambda *args, **kwargs: next(reads), binding=self.binding,
                    case=config.READER_CASES[0], geometry=self.geometry, cancelled=threading.Event())
            self.assertEqual((output / 'quality.png').read_bytes(), self.png)
            self.assertFalse((output / 'quality-capture.json').exists())

    def test_actual_overflow_timeout_nonzero_exit_and_inherited_pipe_are_bounded_and_owned(self):
        child_pid = self.root / 'pipe-child-pid'
        child = 'import os,time; open(' + repr(str(child_pid)) + ',"w").write(str(os.getpid())); time.sleep(30)'
        scripts = {
            'overflow': 'import sys; sys.stdout.buffer.write(b"x" * (8*1024*1024+1))',
            'timeout': 'import sys,time; sys.stdout.buffer.write(b"partial"); sys.stdout.flush(); time.sleep(30)',
            'exit': 'import sys; sys.stdout.buffer.write(b"partial"); sys.exit(3)',
            'pipe': 'import subprocess,sys; subprocess.Popen([sys.executable,"-c",' + repr(child) + '])'}
        for label, script in scripts.items():
            output = self.root / label; output.mkdir(mode=0o700)
            spawned = []
            original = subprocess.Popen
            def launch(*args, **kwargs):
                process = original(*args, **kwargs); spawned.append(process); return process
            started = time.monotonic()
            with patch.object(capture, 'screencap_command', return_value=self.command(script)), \
                    patch.object(capture.subprocess, 'Popen', side_effect=launch), self.assertRaises(config.EntryError):
                capture.capture_png(self.selected, output, deadline=started + .2,
                    read_binding=lambda *args, **kwargs: self.row(), binding=self.binding,
                    case=config.READER_CASES[0], geometry=self.geometry, cancelled=threading.Event())
            self.assertLess(time.monotonic() - started, 1.5)
            self.assertTrue(spawned and all(process.poll() is not None for process in spawned))
            self.assertFalse((output / 'quality-capture.json').exists())
            if label in ('timeout', 'exit', 'overflow'):
                self.assertTrue((output / 'screencap-incomplete.bin').exists())
            if label == 'pipe':
                self.assertTrue(child_pid.exists())
                pid = int(child_pid.read_text())
                observed = subprocess.run(['ps', '-p', str(pid), '-o', 'stat='],
                    capture_output=True, text=True, check=False)
                self.assertTrue(observed.returncode == 1 or observed.stdout.strip().startswith('Z'))

    def test_complete_collection_rechecks_png_sidecars_binding_clock_and_source_provenance(self):
        root = self.collection()
        result = capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)
        self.assertEqual(len(result['captures']), 2)
        self.assertIsNone(result['qualityAccepted']); self.assertIsNone(result['qualityQualifiedSavings'])
        path = root / config.READER_CASES[0] / 'quality-capture.json'
        original = path.read_bytes()
        for key, value in (('beforeReadEpochMilliseconds', True), ('afterReadEpochMilliseconds', 1000),
                ('afterReadEpochMilliseconds', 1002), ('pngSha256', 'a' * 64), ('pngBytes', True),
                ('attachments', True), ('extra', 'value')):
            receipt = config.strict_json(original); receipt[key] = value
            path.write_bytes(config.frozen_json(receipt))
            with self.subTest(key=key), self.assertRaises(config.EntryError):
                capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)
        path.write_bytes(original)
        wrong = deepcopy(self.provenance); wrong[0]['pixelsVerified'] = False
        with self.assertRaisesRegex(config.EntryError, 'provenance'):
            capture.qualify_collection(root, self.binding, self.manifest, self.geometry, wrong)
        png_path = root / config.READER_CASES[1] / 'quality.png'
        png_path.unlink()
        with self.assertRaises(config.EntryError):
            capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)

    def test_live_missing_or_failed_worker_prevents_independent_collection_qualification(self):
        root = self.collection()
        path = root.parent / 'reader-quality-worker.json'
        original = path.read_bytes()
        for key, value in (('workerTerminated', False), ('completedCases', [config.READER_CASES[0]]),
                ('reason', 'reader_capture_timeout'), ('qualityAccepted', True)):
            worker = config.strict_json(original); worker[key] = value
            path.write_bytes(config.frozen_json(worker))
            with self.subTest(key=key), self.assertRaisesRegex(config.EntryError, 'worker_unqualified'):
                capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)
        path.unlink()
        with self.assertRaises(config.EntryError):
            capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)

    def test_corrupt_undersized_landscape_and_animation_pngs_cannot_qualify(self):
        root = self.collection()
        folder = root / config.READER_CASES[0]
        invalids = [b'not-png']
        for size in ((1, 2), (6, 4)):
            output = BytesIO(); Image.new('RGB', size).save(output, format='PNG'); invalids.append(output.getvalue())
        animation = BytesIO()
        Image.new('RGB', (4, 6)).save(animation, format='PNG', save_all=True,
            append_images=[Image.new('RGB', (4, 6), 'red')]); invalids.append(animation.getvalue())
        for png in invalids:
            (folder / 'quality.png').write_bytes(png)
            receipt = config.strict_json((folder / 'quality-capture.json').read_bytes())
            receipt.update(pngSha256=reader.digest(png), pngBytes=len(png))
            (folder / 'quality-capture.json').write_bytes(config.frozen_json(receipt))
            with self.subTest(length=len(png)), self.assertRaises(config.EntryError):
                capture.qualify_collection(root, self.binding, self.manifest, self.geometry, self.provenance)

    def test_worker_collects_two_cases_without_blocking_stdout_and_terminates(self):
        output = self.root / 'worker'; output.mkdir(mode=0o700)
        collector = capture.ReaderAndroidQualityCollector(self.selected, output,
            binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + 3)
        with patch.object(capture, 'screencap_command', return_value=self.command()):
            started = time.monotonic()
            deadlines = [int(time.time() * 1000) + 9000, int(time.time() * 1000) + 9300]
            while time.monotonic() - started < .8 and len(collector.completed) < 2:
                index = 0 if time.monotonic() - started < .3 else 1
                case = config.READER_CASES[index]
                self.marker(collector, self.row(case, deadline=deadlines[index]))
                time.sleep(.02)
            collector.close(); collector.close()
        collector.finish()
        self.assertFalse(collector.worker.is_alive())
        qualified = capture.qualify_collection(collector.root, self.binding, self.manifest, self.geometry, self.provenance)
        self.assertEqual(qualified['status'], 'complete')
        worker = config.strict_json((output / 'reader-quality-worker.json').read_bytes())
        self.assertTrue(worker['workerTerminated'])

    def test_stdout_feed_does_not_wait_for_screenshot_and_close_interrupts_live_worker(self):
        output = self.root / 'blocked'; output.mkdir(mode=0o700)
        collector = capture.ReaderAndroidQualityCollector(self.selected, output,
            binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + 5)
        entered = threading.Event()
        def blocked(*args, **kwargs):
            entered.set(); collector.cancelled.wait(timeout=3)
            raise config.EntryError('reader_capture_cancelled')
        with patch.object(capture, 'capture_png', side_effect=blocked):
            now = int(time.time() * 1000)
            self.marker(collector, self.row(at=now, deadline=now + 9000))
            self.assertTrue(entered.wait(timeout=1))
            started = time.monotonic()
            for offset in range(1, 30):
                self.marker(collector, self.row(at=now + offset, deadline=now + 9000))
            self.assertLess(time.monotonic() - started, .2)
            collector.close()
        self.assertFalse(collector.worker.is_alive())
        with self.assertRaises(config.EntryError): collector.finish()

    def test_stale_wrong_case_provider_native_utc_null_and_missing_markers_refuse(self):
        for index, row in enumerate((self.row(at=1, deadline=9001), self.row(config.READER_CASES[1]),
                self.row(providerSha256='a' * 64), self.row(caseSha256='a' * 64))):
            output = self.root / f'marker-{index}'; output.mkdir(mode=0o700)
            collector = capture.ReaderAndroidQualityCollector(self.selected, output,
                binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + .1)
            with self.subTest(index=index), self.assertRaises(config.EntryError): self.marker(collector, row)
            collector.close()
        output = self.root / 'null'; output.mkdir(mode=0o700)
        collector = capture.ReaderAndroidQualityCollector(self.selected, output,
            binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + .1)
        self.marker(collector, None)
        with self.assertRaises(config.EntryError): collector.read(config.READER_CASES[0], first=False)
        collector.close()
        with self.assertRaises(config.EntryError): collector.finish()

    def test_intervening_null_changed_and_replayed_native_markers_cannot_hide_between_reads(self):
        for index, variant in enumerate(('null', 'changed', 'replayed')):
            output = self.root / f'intervening-{index}'; output.mkdir(mode=0o700)
            collector = capture.ReaderAndroidQualityCollector(self.selected, output,
                binding=lambda: self.binding, geometry=self.geometry, deadline=time.monotonic() + 1)
            now = int(time.time() * 1000)
            row = self.row(at=now, deadline=now + 9000)
            with patch.object(capture.ReaderAndroidQualityCollector, '_collect'):
                self.marker(collector, row)
                collector.active_capture = capture.stable_capture_binding(row)
                changed = (None if variant == 'null' else
                    {**row, 'nativeObservedAtEpochMilliseconds': now + 1, 'holdDeadlineEpochMilliseconds': now + 9001}
                    if variant == 'changed' else row)
                with self.subTest(variant=variant), self.assertRaises(config.EntryError):
                    self.marker(collector, changed)
            collector.close()


class ReaderQualityIntegrationTests(QualityInputs):
    def fixture(self):
        manifest, _, rows, outcome = fixtures.ReaderResultTests.fixture(None,
            self.selected.values['batchId'], admitted_manifest=self.manifest, observed_geometry=self.geometry)
        self.binding.update(physicalTwoGiBAdmitted=True, deviceUnitSha256='d' * 64, allocationSha256='e' * 64)
        for row in rows:
            row['sessionSha256'] = self.binding['sessionSha256']
            for case in config.READER_CASES[:2]:
                if row.get('caseSha256') == reader.digest(case.encode()):
                    row['providerSha256'] = self.binding['providers'][case]['expectedProviderSha256']
                    row['attemptSha256'] = self.binding['providers'][case]['expectedAttemptSha256']
            if row['event'] == 'device_facts':
                row.update(platform='android', apiLevel=30, lowRamDevice=True, memTotalBytes=1800 * 1024 ** 2,
                    memoryClassMiB=128, isPhysicalDevice=None, qemu='unavailable')
        outcome.update(platform='android', sessionSha256=self.binding['sessionSha256'],
            artifactSha256='a' * 64, testArtifactSha256='b' * 64)
        return config.frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}), outcome

    def qualify(self, data, outcome, root):
        return reader.qualify(data, self.binding, outcome, self.manifest, platform='android',
            source_root=self.root, quality_inputs_path=self.retained, quality_capture_root=root)

    def test_android_quality_requires_complete_collection_while_legacy_ios_remains_separate(self):
        data, outcome = self.fixture()
        with self.assertRaisesRegex(config.EntryError, 'collection_incomplete'):
            self.qualify(data, outcome, None)
        root = self.collection()
        result = self.qualify(data, outcome, root)
        self.assertEqual(result['qualification'], 'awaiting_human_quality')
        self.assertEqual(len(result['qualityCaptures']['captures']), 2)
        self.assertIsNone(result['memory']); self.assertIsNone(result['qualityAccepted'])
        (root / config.READER_CASES[1] / 'quality.png').unlink()
        with self.assertRaises(config.EntryError): self.qualify(data, outcome, root)
        ios = {**outcome, 'platform': 'ios'}
        result = reader.qualify(data, self.binding, ios, self.manifest, platform='ios')
        self.assertIsNone(result['qualityCaptures'])

    def test_quality_matrix_requires_all_six_fresh_same_unit_build_source_geometry_captures(self):
        results = []
        for index, batch in enumerate(capture.QUALITY_BATCHES):
            self.select(batch)
            data, outcome = self.fixture()
            root = self.collection(self.root / f'matrix-{index}')
            results.append(self.qualify(data, outcome, root))
        matrix = capture.qualify_quality_matrix(results)
        self.assertEqual(matrix['captureCount'], 6)
        self.assertEqual(matrix['status'], 'awaiting_human_quality')
        self.assertIsNone(matrix['qualityAccepted']); self.assertIsNone(matrix['qualityQualifiedSavings'])
        for changed in (results[:2], [results[0], results[0], results[2]], list(reversed(results))):
            with self.assertRaises(config.EntryError): capture.qualify_quality_matrix(changed)
        for key, value in (('sessionSha256', results[0]['sessionSha256']), ('geometry', {}),
                ('deviceUnitSha256', 'f' * 64), ('allocationSha256', 'f' * 64),
                ('artifactSha256', None), ('testArtifactSha256', 'f' * 64)):
            changed = deepcopy(results); changed[1][key] = value
            with self.subTest(key=key), self.assertRaises(config.EntryError): capture.qualify_quality_matrix(changed)
        changed = deepcopy(results); changed[1]['qualityCaptures']['captures'].pop()
        with self.assertRaises(config.EntryError): capture.qualify_quality_matrix(changed)
        changed = deepcopy(results); changed[1]['qualityCaptures']['captures'][0]['provenance']['sourceSha256'] = 'f' * 64
        with self.assertRaisesRegex(config.EntryError, 'source_changed'): capture.qualify_quality_matrix(changed)
        for key in ('sourceSha256', 'referenceSha256', 'retainedInputsSha256',
                'originalManifestSha256', 'v7PreparationSha256'):
            for value in (None, '', True, 'not-a-hash'):
                changed = deepcopy(results)
                for arm in changed:
                    for item in arm['qualityCaptures']['captures']:
                        if value is None:
                            item['provenance'].pop(key)
                        else:
                            item['provenance'][key] = value
                with self.subTest(provenance=key, value=value), self.assertRaisesRegex(
                        config.EntryError, 'source_missing'):
                    capture.qualify_quality_matrix(changed)
        for index in range(3):
            for key, value in (('deviceFactsReason', 'unavailable'), ('deviceFactsReason', None),
                    ('deviceFacts', None), ('lowMemoryPhysicalQualification', 'unavailable'),
                    ('lowMemoryPhysicalQualification', None)):
                changed = deepcopy(results); changed[index][key] = value
                with self.subTest(arm=index, key=key), self.assertRaises(config.EntryError):
                    capture.qualify_quality_matrix(changed)

    def test_real_pairing_integrates_async_collector_reload_and_publication_after_full_collection(self):
        import native_entry_driver as host
        import native_artwork_comparison as comparison
        from native_fixture_server import Broker
        output = self.root / 'run'; output.mkdir(mode=0o700)
        self.selected = config.Configuration({**self.selected.values,
            'manifestPath': str(self.root / 'prepared' / (self.selected.values['batchId'] + '-manifest.json')),
            'privateOutputRoot': str(output)}, 'a' * 64)
        data, outcome = self.fixture()
        envelope = config.frozen_json({'batchId': self.selected.values['batchId'], 'generation': 0,
            'sessionId': self.selected.values['batchId'], 'accessAlias': 'synthetic'})
        original_broker, original_pairing = host.Broker, host.LauncherPairing
        def verify(*args, **kwargs):
            self.assertTrue(kwargs['quality_capture'])
            broker = host.Broker(self.selected, self.manifest, lambda _: {})
            pairing = host.LauncherPairing(broker, 'android')
            pairing.chunks = SimpleNamespace(feed=lambda _: None, finish=lambda: data)
            broker.claim({})
            started = time.monotonic()
            deadlines = [int(time.time() * 1000) + 9000, int(time.time() * 1000) + 9300]
            try:
                while time.monotonic() - started < .8:
                    index = 0 if time.monotonic() - started < .3 else 1
                    self.marker(SimpleNamespace(feed=pairing), self.row(config.READER_CASES[index], deadline=deadlines[index]))
                    pairing.poll()
                    self.assertFalse((output / 'reader-result.json').exists())
                    time.sleep(.02)
            finally:
                pairing.close(); broker.close()
            self.assertEqual(self.manifest.objects, {})
            return outcome
        with patch.object(host, 'validate_physical_prerequisites', return_value=(
                {'artifactSha256': 'a' * 64, 'testArtifactSha256': 'b' * 64}, {'deviceModel': 'Moto E20'})), \
                patch.object(comparison, 'validate_comparison_audit'), \
                patch.object(comparison, 'require_fresh_android_process'), \
                patch.object(host, 'source_snapshot', return_value=({}, 'c' * 64)), \
                patch.object(Broker, 'claim', return_value=envelope), \
                patch.object(host, 'verify_entry', side_effect=verify), \
                patch.object(capture, 'screencap_command', return_value=self.command()):
            result = reader.run(self.selected, self.manifest, output, build_path='build', prerequisite_path='proof',
                audit_path='audit', frozen_geometry=self.geometry, source_root=self.root, quality_inputs_path=self.retained,
                root_elapsed_seconds=10, reservation_remaining_seconds=460)
        self.assertIs(host.Broker, original_broker); self.assertIs(host.LauncherPairing, original_pairing)
        self.assertFalse(comparison.LOCK.locked())
        self.assertEqual(result['qualification'], 'awaiting_human_quality')
        self.assertEqual(len(result['qualityCaptures']['captures']), 2)
        self.assertTrue((output / 'reader-result.json').is_file())
        self.assertTrue((output / 'reader-quality-collection.json').is_file())
        self.assertTrue(config.strict_json((output / 'reader-quality-worker.json').read_bytes())['workerTerminated'])

    def test_quality_source_refusal_precedes_prerequisites_auth_and_native_dispatch(self):
        import native_entry_driver as host
        output = self.root / 'source-refusal'; output.mkdir(mode=0o700)
        with patch.object(host, 'validate_physical_prerequisites') as prerequisites, \
                patch.object(host, 'verify_entry') as dispatch, patch.object(host, 'Broker') as broker:
            with self.assertRaisesRegex(config.EntryError, 'reader_quality_sources_required'):
                reader.run(self.selected, self.manifest, output, build_path='build', prerequisite_path='proof',
                    audit_path='audit', frozen_geometry=self.geometry, source_root=None,
                    quality_inputs_path=self.retained, root_elapsed_seconds=10, reservation_remaining_seconds=460)
        prerequisites.assert_not_called(); dispatch.assert_not_called(); broker.assert_not_called()
        failure = config.strict_json((output / 'reader-failure-receipt.json').read_bytes())
        self.assertEqual(failure['reason'], 'reader_quality_sources_required')
        self.assertFalse((output / 'reader-result.json').exists())

    def test_native_refusal_closes_real_worker_preserves_original_error_and_never_publishes_partial(self):
        import native_entry_driver as host
        import native_artwork_comparison as comparison
        from native_fixture_server import Broker
        output = self.root / 'failed-run'; output.mkdir(mode=0o700)
        data, outcome = self.fixture()
        envelope = config.frozen_json({'batchId': self.selected.values['batchId'], 'generation': 0,
            'sessionId': self.selected.values['batchId'], 'accessAlias': 'synthetic'})
        original_error = config.EntryError('native_original_refusal')
        def verify(*args, **kwargs):
            broker = host.Broker(self.selected, self.manifest, lambda _: {})
            pairing = host.LauncherPairing(broker, 'android')
            pairing.chunks = SimpleNamespace(feed=lambda _: None, finish=lambda: data)
            broker.claim({})
            self.marker(SimpleNamespace(feed=pairing), self.row())
            # The reader finally must close the collector even if a launcher fails before its own close.
            broker.close(); pairing.close()
            raise original_error
        with patch.object(host, 'validate_physical_prerequisites', return_value=(
                {'artifactSha256': 'a' * 64, 'testArtifactSha256': 'b' * 64}, {'deviceModel': 'Moto E20'})), \
                patch.object(comparison, 'validate_comparison_audit'), \
                patch.object(comparison, 'require_fresh_android_process'), \
                patch.object(host, 'source_snapshot', return_value=({}, 'c' * 64)), \
                patch.object(Broker, 'claim', return_value=envelope), \
                patch.object(host, 'verify_entry', side_effect=verify):
            with self.assertRaises(config.EntryError) as caught:
                reader.run(self.selected, self.manifest, output, build_path='build', prerequisite_path='proof',
                    audit_path='audit', frozen_geometry=self.geometry, source_root=self.root, quality_inputs_path=self.retained,
                    root_elapsed_seconds=10, reservation_remaining_seconds=460)
        self.assertIs(caught.exception, original_error)
        self.assertFalse((output / 'reader-result.json').exists())
        self.assertFalse((output / 'reader-quality-collection.json').exists())
        self.assertTrue(config.strict_json((output / 'reader-quality-worker.json').read_bytes())['workerTerminated'])


if __name__ == '__main__':
    unittest.main()
