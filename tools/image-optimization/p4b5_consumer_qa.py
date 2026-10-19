#!/usr/bin/env python3
"""P4b5 tek denemesinin kapsam, aşama ve gerçek kaynak/phase kabulü."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
from urllib.parse import urlsplit

import p4_consumer_qa as helpers
from p4_resource_observation import validate_resource_observations

ROOT = Path(__file__).resolve().parents[2]
PRIVATE = Path('/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d')
PROPOSAL = 'be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json'
PROPOSAL_SHA = 'e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833'
MEASUREMENT = 'p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8'
RUN = 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09'
ENTRY = 'test_driver/reading_p4b5_qa_entry.dart'
PACKAGE = helpers.PACKAGE
UID = helpers.UID
PHASES = ['baseline', 'paint1', 'paint2', 'paint3', 'paint4', 'beforeBackground', 'background', 'resumed', 'closedUi', 'stopped']
FLAGS = ['ok', 'configured', 'stopped', 'cubitClosed', 'credentialActive', 'deadlineActive']
ACTIVE = ['activeBuffers', 'activeDescriptors', 'activeCodecs', 'activeImages']
require = helpers.require


def scope():
    return {'proposalSha256': PROPOSAL_SHA, 'measurementId': MEASUREMENT, 'budgetRunId': RUN}


def machine_approval():
    return {**scope(), 'decision': 'A', 'qaUid': UID, 'contentId': 94, 'windowMillis': 120000, 'newProductionAllowed': False}


def exact(value, expected):
    return json.dumps(value, sort_keys=True) == json.dumps(expected, sort_keys=True)


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def private_json(path):
    return json.loads(helpers.private_file(path))


def write_json(path, value):
    """CREATE_NEW, 0600 ve fsync önceki kanıtın üstüne yazmayı kapatır."""
    data = (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode()
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, 'wb') as out:
            out.write(data)
            out.flush()
            os.fsync(out.fileno())
        parent_fd = os.open(Path(path).parent, os.O_RDONLY)
        try:
            os.fsync(parent_fd)
        finally:
            os.close(parent_fd)
    except BaseException:
        raise


def numbered(directory, prefix, value):
    for number in range(1, 10000):
        path = directory / f'{prefix}-{number:04d}.private.json'
        try:
            write_json(path, value)
            return path
        except FileExistsError:
            continue
    raise ValueError('RECEIPT_SPACE_EXHAUSTED')


class Vm(helpers.Vm):
    """P4b5 RPC yüzeyi: kayıt beklenir; reddedilmiş RPC tekrar gönderilmez."""
    METHODS = frozenset(['getVM', 'getIsolate', 'streamListen', 'streamCancel', 'ext.flutter.driver'])
    STAGES = frozenset(['arm', 'baseline-gate', 'measurement'])

    def _protocol(self, evidence, stage, deadline=None, owner_check=None):
        require(stage in self.STAGES, 'VM_STAGE_REFUSED')
        self.evidence, self.stage, self.startup_deadline = Path(evidence), stage, deadline
        self.owner_check = owner_check
        self.sequence, self.isolate, self.events = 0, None, []

    def __init__(self, url, evidence, stage, deadline=None, owner_check=None):
        import socket
        import websocket
        self._protocol(evidence, stage, deadline, owner_check)
        parsed = urlsplit(url)
        require(parsed.scheme == 'ws' and parsed.hostname == '127.0.0.1' and not parsed.query
                and not parsed.username and not parsed.password and not parsed.fragment
                and parsed.port is not None and 1024 <= parsed.port <= 65535
                and re.fullmatch(r'/[A-Za-z0-9_=-]+/ws', parsed.path), 'VM_URL_REFUSED')
        direct = socket.create_connection(('127.0.0.1', parsed.port), timeout=self._remaining())
        candidate = None
        try:
            candidate = websocket.create_connection(url, socket=direct, timeout=self._remaining(), suppress_origin=True, redirect_limit=0)
            require(candidate.getstatus() == 101, 'VM_HANDSHAKE_REFUSED')
            self._remaining()
        except BaseException:
            if candidate is not None: candidate.close()
            direct.close()
            raise
        self.socket, self._transport = candidate, direct

    def _remaining(self, rpc_deadline=None):
        if self.owner_check is not None: self.owner_check()
        now = time.monotonic()
        deadlines = [value for value in [self.startup_deadline, rpc_deadline] if value is not None]
        end = min(deadlines) if deadlines else now + 10
        require(end > now, 'VM_DEADLINE')
        return end - now

    def _receive(self, deadline):
        self.socket.settimeout(self._remaining(deadline))
        raw = self.socket.recv()
        self._remaining(deadline)
        require(len(raw) <= 1048576, 'VM_RESPONSE_LIMIT')
        value = json.loads(raw)
        require(type(value) is dict, 'VM_RESPONSE_FORMAT')
        if value.get('method') == 'streamNotify':
            params = value.get('params', {})
            if type(params) is dict and params.get('streamId') == 'Isolate' and type(params.get('event')) is dict:
                event = params['event']
                reference = event.get('isolate', {})
                require(type(reference) is dict, 'VM_ISOLATE_CHANGED')
                require(not (event.get('kind') == 'IsolateExit' and reference.get('id') == self.isolate)
                        and not (event.get('kind') == 'IsolateStart' and reference.get('name') == 'main' and reference.get('id') != self.isolate), 'VM_ISOLATE_CHANGED')
                self.events.append(event)
                require(len(self.events) <= 256, 'VM_EVENT_LIMIT')
        return value

    def rpc(self, method, params=None):
        require(type(method) is str and method in self.METHODS, 'VM_METHOD_REFUSED')
        self.socket.settimeout(self._remaining())
        self.sequence += 1
        self.socket.send(json.dumps({'jsonrpc': '2.0', 'id': self.sequence, 'method': method, 'params': params or {}}))
        deadline = time.monotonic() + 10
        for _ in range(256):
            result = self._receive(deadline)
            if type(result.get('id')) is int and result['id'] == self.sequence:
                if 'error' in result:
                    error = result['error']
                    code = error.get('code') if type(error) is dict else None
                    receipt = {**scope(), 'stage': self.stage, 'rpcMethod': method}
                    if type(code) is int and -2147483648 <= code <= 2147483647: receipt['rpcErrorCode'] = code
                    numbered(self.evidence, 'vm-rpc-failure', receipt)
                    raise ValueError('VM_REFUSED')
                require(type(result.get('result')) is dict, 'VM_RESPONSE_FORMAT')
                return result['result']
        raise ValueError('VM_RESPONSE_LIMIT')

    @staticmethod
    def _main(vm):
        isolates = vm.get('isolates')
        require(type(isolates) is list, 'VM_ISOLATE_CHANGED')
        main = [value for value in isolates if type(value) is dict and value.get('name') == 'main']
        require(len(main) == 1 and type(main[0].get('id')) is str, 'VM_ISOLATE_CHANGED')
        return main[0]['id']

    def ready(self):
        self.isolate = self._main(self.rpc('getVM'))
        self.rpc('streamListen', {'streamId': 'Isolate'})
        isolate = self.rpc('getIsolate', {'isolateId': self.isolate})
        require(isolate.get('id') == self.isolate and type(isolate.get('extensionRPCs')) is list, 'VM_ISOLATE_CHANGED')
        registered = 'ext.flutter.driver' in isolate['extensionRPCs']
        deadline = time.monotonic() + 10 if self.startup_deadline is None else self.startup_deadline
        while not registered:
            self._remaining(deadline)
            while self.events:
                event = self.events.pop(0)
                reference = event.get('isolate', {})
                require(type(reference) is dict, 'VM_ISOLATE_CHANGED')
                require(not (event.get('kind') == 'IsolateExit' and reference.get('id') == self.isolate)
                        and not (event.get('kind') == 'IsolateStart' and reference.get('name') == 'main' and reference.get('id') != self.isolate), 'VM_ISOLATE_CHANGED')
                if event.get('kind') == 'ServiceExtensionAdded' and event.get('extensionRPC') == 'ext.flutter.driver' and reference.get('id') == self.isolate:
                    registered = True
            if not registered: self._receive(deadline)
        require(self._main(self.rpc('getVM')) == self.isolate, 'VM_ISOLATE_CHANGED')
        self.rpc('streamCancel', {'streamId': 'Isolate'})
        self.rpc('ext.flutter.driver', {'isolateId': self.isolate, 'command': 'set_frame_sync', 'enabled': 'false'})


def require_arm_owner(android, owned, deadline):
    require(android.pid(timeout=helpers.arm_remaining(deadline)) == str(owned['pid']), 'QA_PROCESS_CHANGED')
    require(android.process_identity(owned['pid'], timeout=helpers.arm_remaining(deadline))
            == {'pid': owned['pid'], 'processStartTicks': owned['processStartTicks']}, 'QA_PROCESS_REPLACED')
    helpers.arm_remaining(deadline)


def decimal_value(text):
    value = 0
    for digit in text:
        value = value * 10 + ord(digit) - ord('0')
    return value


def validate_before_background(paint, sample):
    require(sample.get('status') == paint.get('status') == 'loaded'
            and sample.get('observedLifecycle') == paint.get('observedLifecycle') == 'resumed'
            and sample.get('liveRasterAttached') is True and paint.get('liveRasterAttached') is True
            and type(paint.get('paintedEpoch')) is int
            and sample.get('currentEpoch') == sample.get('lastPaintedEpoch') == paint['paintedEpoch']
            and sample.get('slot') == paint.get('slot')
            and sample.get('resourceObservation') == paint.get('resourceObservation')
            and all(sample.get(key) == paint.get(key) for key in ACTIVE), 'BEFORE_BACKGROUND_PAINT_BINDING')
    loaded = lambda value: [e for e in value.get('events', []) if e.get('kind') == 'state' and e.get('status') == 'loaded']
    require(loaded(sample) == loaded(paint), 'BEFORE_BACKGROUND_EPOCH_CHANGED')


def validate_backend_result(state, ready, links):
    require(not (state / 'failure.json').exists(), 'BACKEND_MEASUREMENT_FAILED')
    receipt = private_json(state / 'backend-closed.json')
    require(all(receipt.get(k) == v for k, v in scope().items()) and type(receipt.get('pid')) is int
            and receipt['pid'] == ready['pid'] and receipt.get('closed') is True
            and receipt.get('drainVerified') is True and receipt.get('outcome') == 'SUCCESS', 'BACKEND_SUCCESS_DRAIN_REQUIRED')
    require(exact(receipt.get('bindings'), links), 'BACKEND_RESULT_BINDING_DRIFT')
    ledger = helpers.private_file(state / 'ledger.log', 65536)
    require(hashlib.sha256(ledger).hexdigest() == receipt.get('terminalLedgerSha256'), 'TERMINAL_LEDGER_DRIFT')
    lines = ledger.decode().splitlines()
    require(lines and lines[-1] == receipt.get('terminalLedgerLine') and lines[-1].startswith('CLOSED ')
            and not any(line.startswith(('FAILED_', 'FAILED ')) for line in lines), 'TERMINAL_LEDGER_FAILED')
    parts = lines[-1].split()
    require(len(parts) == 7 and all(re.fullmatch(r'0|[1-9][0-9]*', value) for value in parts[1:]), 'TERMINAL_LEDGER_FORMAT')
    reads, byte_count, auth, manifests, statuses, media = map(int, parts[1:])
    require(reads == media == 4 and byte_count == 13170884 and manifests == 1 and 0 <= statuses <= 12
            and auth == 1 + manifests + statuses + media * 2, 'TERMINAL_LEDGER_COUNTS')
    delivery = auth - 1 + reads
    require(delivery <= 25 and type(receipt.get('deliveryOperations')) is int and receipt['deliveryOperations'] == delivery
            and type(receipt.get('reservedBytes')) is int and receipt['reservedBytes'] == byte_count, 'TERMINAL_DELIVERY_COUNTS')
    expected_db = {'operations': 39 + delivery, 'reserved_bytes': 44742970 + byte_count,
                   'max_operations': 64, 'max_bytes': 67108864}
    require(exact(receipt.get('finalBudget'), expected_db), 'FINAL_DB_LEDGER_RECONCILIATION')
    # Her rezervasyonun terminal toplamına kadar logda bulunduğu da doğrulanır.
    deliveries = [line for line in lines if line.startswith('DELIVERY_RESERVED ')]
    require(deliveries == [f'DELIVERY_RESERVED {n} {39 + n}' for n in range(1, delivery + 1)], 'TERMINAL_DELIVERY_SEQUENCE')
    return {'processExitConfirmed': True, 'drainVerified': True, 'measurementSucceeded': True,
            'terminalLedgerSha256': receipt['terminalLedgerSha256'], 'finalBudget': expected_db}


def validate_phase_resources(records):
    """Ham JSON'u değiştirmeden P4b4 ve phase kabulünü birlikte uygular."""
    require(type(records) is list and len(records) == len(PHASES), 'PHASE_SEQUENCE_REQUIRED')
    require([r.get('phase') for r in records] == PHASES, 'PHASE_ORDER')
    samples = [r.get('sample') for r in records]
    report = validate_resource_observations(samples)
    require(report.get('schema') == 'v1' and report.get('measured') is True, 'MEASURED_V1_REQUIRED')
    baseline = decimal_value(report['baselineBufferCreationAttempts'])
    frozen_reservations = None
    for index, (phase, sample) in enumerate(zip(PHASES, samples)):
        for name in FLAGS + (['drained'] if 'drained' in sample else []):
            require(type(sample.get(name)) is bool, 'BOOLEAN_CONTROL_REQUIRED')
        require(sample['ok'] is True, 'PROBE_REFUSED')
        events = sample.get('events')
        require(type(events) is list and all(type(e) is dict for e in events), 'EVENTS_REQUIRED')
        http = [e for e in events if e.get('kind') == 'http']
        reservations = [e for e in events if e.get('kind') == 'httpReservation']
        for event in http:
            require(type(event.get('ok')) is bool and event['ok'] is True, 'HTTP_BOOLEAN_OR_FAILURE')
            require(type(event.get('payloadBytes')) is int and event['payloadBytes'] >= 0, 'HTTP_PAYLOAD_REQUIRED')
        for sequence in [http, reservations]:
            counts = {endpoint: sum(e.get('endpoint') == endpoint for e in sequence) for endpoint in ['manifest', 'status', 'bytes']}
            require(sum(counts.values()) == len(sequence) and counts['manifest'] <= 1 and counts['status'] <= 12
                    and counts['bytes'] <= 4 and len(sequence) <= 17, 'HTTP_LIMIT')
        require([e.get('ordinal') for e in reservations] == list(range(1, len(reservations) + 1)), 'HTTP_ORDINAL')
        require(all(sum(e.get('endpoint') == endpoint for e in http) <= sum(e.get('endpoint') == endpoint for e in reservations)
                    for endpoint in ['manifest', 'status', 'bytes']), 'UNRESERVED_HTTP')
        attempts = decimal_value(sample['resourceObservation']['bufferCreationAttempts'])
        expected_delta = 0 if index == 0 else min(index, 4)
        require(attempts - baseline == expected_delta, 'BUFFER_ATTEMPT_DELTA')
        if phase == 'baseline':
            require(not sample['configured'] and not sample['stopped'] and not sample['cubitClosed']
                    and not sample['credentialActive'] and not sample['deadlineActive']
                    and type(sample.get('cubitCount')) is int and sample['cubitCount'] == 0 and not reservations and not http, 'BASELINE_NOT_EMPTY')
        else:
            require(sample['configured'] and type(sample.get('cubitCount')) is int and sample['cubitCount'] == 1, 'CONFIGURATION_OR_CUBIT')
        if phase.startswith('paint') or phase == 'beforeBackground':
            require(not sample['stopped'] and not sample['cubitClosed'] and sample['credentialActive'] and sample['deadlineActive'], 'LOADED_CONTROL_STATE')
        if phase.startswith('paint'):
            require(sample.get('observedLifecycle') == 'resumed' and sample.get('liveRasterAttached') is True
                    and sample.get('currentEpoch') == sample.get('lastPaintedEpoch') == sample.get('paintedEpoch'), 'PAINT_LIFECYCLE_OR_EPOCH')
        if phase == 'beforeBackground':
            validate_before_background(samples[4], sample)
            frozen_reservations = reservations
        if phase in ['background', 'resumed']:
            require(not sample['stopped'] and not sample['cubitClosed'] and sample['credentialActive'] and sample['deadlineActive'], 'BACKGROUND_CONTROL_STATE')
            require((phase == 'background' and sample.get('observedLifecycle') in ['paused', 'hidden', 'inactive'])
                    or (phase == 'resumed' and sample.get('observedLifecycle') == 'resumed'), 'BACKGROUND_LIFECYCLE')
        if phase in ['background', 'resumed', 'closedUi', 'stopped']:
            require(reservations == frozen_reservations, 'BACKGROUND_NEW_RESERVATION')
        if phase in ['baseline', 'background', 'resumed', 'closedUi', 'stopped']:
            # Phase kendi drain iddiasını taşır; stopped=false bu denetimi atlatamaz.
            require(all(type(sample.get(name)) is int and sample[name] == 0 for name in ACTIVE)
                    and sample['resourceObservation']['reservedSlots'] == 0
                    and sample['resourceObservation']['reservedDecodedBytes'] == '0', 'PHASE_NOT_DRAINED')
        if phase in ['closedUi', 'stopped']:
            require(sample['cubitClosed'], 'CUBIT_NOT_CLOSED')
        if phase == 'stopped':
            require(sample['stopped'] and not sample['credentialActive'] and not sample['deadlineActive'], 'STOP_OWNERSHIP')
    require(report['bufferCreationAttemptsDelta'] == '4', 'FOUR_NATIVE_ATTEMPTS_REQUIRED')
    return report


def validate_samples(records, ready):
    report = validate_phase_resources(records)
    samples = [r['sample'] for r in records]
    require(all(type(samples[index + 1].get('paintedEpoch')) is int for index in range(4)), 'PAINT_EPOCH_INTEGER_REQUIRED')
    paints = [helpers.validate_loaded(samples[index + 1], ready, index) for index in range(4)]
    final_http = [e for e in samples[-1]['events'] if e.get('kind') == 'http']
    media = [e for e in final_http if e['endpoint'] == 'bytes']
    require(len(media) == 4 and all(e['payloadBytes'] == 3292721 for e in media)
            and sum(e['payloadBytes'] for e in media) == 13170884, 'FOUR_PAYLOADS_REQUIRED')
    return {'resourceReport': report, 'phasesAccepted': PHASES, 'paints': paints,
            'mediaPayloadBytes': 13170884, 'httpCompleted': len(final_http), 'physicalQualityAccepted': False}


def runtime_configuration(ready, token):
    require(type(token) is str and len(token) <= 8192 and re.fullmatch(r'[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', token), 'TOKEN_FORMAT')
    claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
    require(claims.get('aud') == 'tellpal-ee0dd' and claims.get('iss') == 'https://securetoken.google.com/tellpal-ee0dd'
            and claims.get('sub') == UID and claims.get('user_id', UID) == UID
            and type(claims.get('exp')) is int and claims['exp'] > time.time() + 180
            and type(claims.get('iat')) is int and claims['iat'] <= time.time(), 'TOKEN_SCOPE_OR_EXPIRY')
    start, end = ready.get('windowStartedAtEpochMillis'), ready.get('windowDeadlineEpochMillis')
    require(type(start) is int and type(end) is int and end - start == 120000 and start <= time.time() * 1000 < end, 'WINDOW_EXPIRED')
    return {'action': 'configure', 'runId': RUN, 'measurementId': MEASUREMENT, 'proposalSha256': PROPOSAL_SHA,
            'uid': UID, 'contentId': 94, 'port': ready['loopbackPort'], 'token': token,
            'windowStartedAtEpochMillis': start, 'windowDeadlineEpochMillis': end}


class Controller:
    def __init__(self, private=PRIVATE, root=ROOT):
        self.private, self.root = private, root
        self.state = private / 'backend'

    def receipt(self, name):
        return private_json(self.private / name)

    def latest_receipt(self, prefix, directory='receipts'):
        paths = [path for path in (self.private / directory).glob(prefix + '-*.private.json')
                 if re.fullmatch(re.escape(prefix) + r'-[0-9]{4}\.private\.json', path.name)]
        require(bool(paths), 'NUMBERED_RECEIPT_REQUIRED')
        path = sorted(paths)[-1]
        return path, private_json(path)

    def source_binding(self):
        return self.latest_receipt('source-binding')

    def ensure_open(self):
        require(not any((self.private / name).exists() for name in ['terminal.private.json', 'cleanup.claim.private.json', 'cleanup.private.json'])
                and not any((self.state / name).exists() for name in ['close.request', 'backend-closed.json', 'failure.json']), 'ATTEMPT_CLOSED_NO_RETRY')

    def gate(self, reviews=True):
        self.ensure_open()
        require(self.private == PRIVATE and helpers.private_dir(self.private, external=True) == PRIVATE, 'PRIVATE_ROOT_REQUIRED')
        require(digest(self.root / PROPOSAL) == PROPOSAL_SHA, 'PROPOSAL_CHANGED')
        approval = self.receipt('receipts/human-approval-01.private.json')
        require(approval.get('proposalSha256') == PROPOSAL_SHA and approval.get('measurementId') == MEASUREMENT
                and approval.get('answer') == 'A — Planı ve tek sınırlı denemeyi onayla', 'HUMAN_APPROVAL_REQUIRED')
        binding_path, binding = self.source_binding()
        require(binding.get('proposalSha256') == PROPOSAL_SHA and binding.get('measurementId') == MEASUREMENT, 'SOURCE_BINDING_SCOPE')
        require(type(binding.get('files')) is dict and bool(binding['files']), 'SOURCE_BINDINGS_REQUIRED')
        for name, sha in binding['files'].items():
            relative = Path(name)
            require(not relative.is_absolute() and '..' not in relative.parts, 'SOURCE_PATH_REFUSED')
            require(digest(self.root / relative) == binding.get('repositoryFiles', binding['files'])[name]
                    and digest(self.private / 'source' / relative) == sha, 'SOURCE_DRIFT')
        if reviews:
            run_path, run = self.latest_receipt('run-binding')
            selected_path, _ = self.latest_receipt('selected-build')
            classpath_path, classpath = self.latest_receipt('classpath-binding')
            require(type(classpath.get('files')) is dict and bool(classpath['files']), 'CLASSPATH_BINDINGS_REQUIRED')
            for name, sha in classpath['files'].items():
                path = Path(name)
                require(path.is_absolute() and path.resolve() == path and path.is_file() and digest(path) == sha, 'CLASSPATH_DRIFT')
            require(all(run.get(k) == v for k, v in scope().items())
                    and run.get('sourceBindingFile') == binding_path.name and run.get('classpathBindingFile') == classpath_path.name and run.get('selectedBuildFile') == selected_path.name
                    and run.get('sourceBindingSha256') == digest(binding_path)
                    and run.get('selectedBuildSha256') == digest(selected_path)
                    and run.get('classpathBindingSha256') == digest(classpath_path), 'RUN_BINDING_DRIFT')
            generation = run_path.name[len('run-binding-'):len('run-binding-') + 4]
            seen = set()
            for number in range(1, 4):
                review = self.receipt(f'reviews/pre-run-{generation}-{number:02d}.private.json')
                require(all(review.get(k) == v for k, v in scope().items())
                        and review.get('sourceBindingSha256') == digest(binding_path)
                        and review.get('runBindingSha256') == digest(run_path)
                        and review.get('status') == 'PASS' and type(review.get('unresolvedFindings')) is int and review['unresolvedFindings'] == 0
                        and type(review.get('reviewerId')) is str, 'THREE_PRE_RUN_REVIEWS_REQUIRED')
                seen.add(review['reviewerId'])
            require(len(seen) == 3, 'INDEPENDENT_REVIEWERS_REQUIRED')
            for name in ['install', 'initial-start', 'backend-start', 'login', 'window', 'measurement']:
                claimed = self.private / f'{name}.claim.private.json'
                if claimed.exists():
                    require(private_json(claimed).get('runBindingSha256') == digest(run_path), 'ATTEMPT_BINDING_FROZEN')
        return binding

    def claim(self, name, details=None):
        binding = {}
        if list((self.private / 'receipts').glob('run-binding-*.private.json')):
            run_path, _ = self.latest_receipt('run-binding')
            binding['runBindingSha256'] = digest(run_path)
        write_json(self.private / f'{name}.claim.private.json', {**scope(), **binding, 'state': 'RESERVED_NO_AUTOMATIC_RETRY', **(details or {})})

    def build(self, args):
        binding = self.gate(reviews=False)
        binding_path, _ = self.source_binding()
        binding_sha = digest(binding_path)
        mobile = self.private / 'source/app/mobile'
        tools = self.receipt('receipts/operational-tools.private.json')
        java = Path(tools['javaHome'])
        require((java / 'bin/java').is_file() and Path(tools['androidSdk']).is_dir(), 'EXISTING_TOOLS_REQUIRED')
        builds = self.private / 'android/builds'
        builds.mkdir(mode=0o700, parents=True, exist_ok=True)
        number = 1 + max([int(p.name[6:]) for p in builds.iterdir() if re.fullmatch('build-[0-9]{4}', p.name)], default=0)
        directory = builds / f'build-{number:04d}'
        directory.mkdir(mode=0o700)
        init = directory / 'p4b5.init.gradle'
        helpers.write_new(init, ("gradle.beforeProject { p -> p.pluginManager.withPlugin('com.android.application') { if(p.path == ':app') { "
            "if(p.findProperty('target') != '" + ENTRY + "' || p.findProperty('imageExperiment') != null || gradle.startParameter.taskNames != [':app:assembleDebug']) "
            "throw new GradleException('P4B5_TARGET_REFUSED'); p.extensions.getByName('androidComponents').finalizeDsl { a -> "
            "a.defaultConfig.applicationId = '" + PACKAGE + "' } } } }\n").encode())
        environment = os.environ.copy()
        environment.update(JAVA_HOME=str(java), ANDROID_HOME=tools['androidSdk'], ANDROID_SDK_ROOT=tools['androidSdk'])
        log_fd = os.open(directory / 'build.log', os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(log_fd, 'wb') as log:
            gradles = sorted((Path.home() / '.gradle/wrapper/dists/gradle-8.14-all').glob('*/gradle-8.14/bin/gradle'))
            require(len(gradles) == 1 and gradles[0].is_file(), 'EXISTING_GRADLE_REQUIRED')
            environment['TMPDIR'] = str(self.private / 'tmp')
            result = subprocess.run([str(gradles[0]), '--offline', '--console=plain', '-p', str(mobile / 'android'),
                '-I', str(init), ':app:assembleDebug', '-Ptarget=' + ENTRY, '-Ptarget-platform=android-arm64', '-Pandroid.builder.sdkDownload=false'], env=environment,
                stdout=log, stderr=subprocess.STDOUT, timeout=600)
            log.flush(); os.fsync(log.fileno())
        require(result.returncode == 0, 'BUILD_FAILED_PRIVATE_LOG')
        self.gate(reviews=False)
        require(digest(self.source_binding()[0]) == binding_sha, 'SOURCE_BINDING_CHANGED_DURING_BUILD')
        apk = directory / 'consumer.apk'
        helpers.write_new(apk, (mobile / 'build/app/outputs/apk/debug/app-debug.apk').read_bytes())
        require(args.aapt is not None, 'AAPT_REQUIRED')
        badging = helpers.command([args.aapt, 'dump', 'badging', apk]).decode()
        require(re.search(r"package: name='" + re.escape(PACKAGE) + "'", badging), 'APK_PACKAGE_REFUSED')
        write_json(directory / 'build.private.json', {**scope(), 'apkSha256': digest(apk), 'sourceBindingSha256': binding_sha,
            'package': PACKAGE, 'mode': 'debug', 'entry': ENTRY})
        return str(directory)

    def selected_build(self):
        _, selected = self.latest_receipt('selected-build')
        name = selected.get('buildName')
        require(type(name) is str and re.fullmatch('build-[0-9]{4}', name), 'BUILD_NAME_REQUIRED')
        directory = self.private / 'android/builds' / name
        build = private_json(directory / 'build.private.json')
        require(all(build.get(k) == v for k, v in scope().items()) and build.get('entry') == ENTRY and build.get('package') == PACKAGE
                and build.get('sourceBindingSha256') == digest(self.source_binding()[0])
                and selected.get('apkSha256') == build.get('apkSha256') == digest(directory / 'consumer.apk'), 'BUILD_CHANGED')
        return build, directory

    def connection(self, android):
        value = self.receipt('android/connection.private.json')
        require(all(value.get(k) == v for k, v in scope().items()), 'CONNECTION_SCOPE')
        build, _ = self.selected_build()
        require(value.get('apkSha256') == build['apkSha256'], 'APK_CHANGED')
        android.preflight()
        helpers.require_owned_qa(android, value)
        installed = helpers.installed_apk_path(android.run('shell', 'pm', 'path', PACKAGE))
        installed_sha = android.run('shell', 'sha256sum', installed).split()
        require(len(installed_sha) == 2 and installed_sha[0] == build['apkSha256'], 'INSTALLED_APK_DRIFT')
        parsed = urlsplit(value.get('websocket', ''))
        require(parsed.scheme == 'ws' and parsed.hostname == '127.0.0.1' and parsed.port == value.get('hostPort')
                and re.fullmatch(r'/[A-Za-z0-9_=-]+/ws', parsed.path) and not parsed.query and not parsed.fragment and not parsed.username and not parsed.password, 'VM_ENDPOINT_REFUSED')
        return value

    def binding_links(self):
        run_path, run = self.latest_receipt('run-binding')
        source_path, _ = self.source_binding(); cp_path, cp = self.latest_receipt('classpath-binding')
        selected_path, selected = self.latest_receipt('selected-build')
        require(all(run.get(k) == v for k, v in scope().items())
                and run.get('sourceBindingFile') == source_path.name and run.get('sourceBindingSha256') == digest(source_path)
                and run.get('classpathBindingFile') == cp_path.name and run.get('classpathBindingSha256') == digest(cp_path)
                and run.get('selectedBuildFile') == selected_path.name and run.get('selectedBuildSha256') == digest(selected_path), 'RUN_BINDING_DRIFT')
        return {'runBindingFile': run_path.name, 'runBindingSha256': digest(run_path),
                **{k: run[k] for k in ['sourceBindingFile', 'sourceBindingSha256', 'classpathBindingFile', 'classpathBindingSha256', 'selectedBuildFile', 'selectedBuildSha256']},
                'apkSha256': selected['apkSha256'], 'servingJavaBinarySha256': cp['javaBinarySha256']}

    def ready(self, window=False):
        ready = private_json(self.state / ('ready.json' if window else 'local-ready.json'))
        fixture = json.loads((self.root / 'be/src/test/resources/p4/consumer-retained-ready.json').read_bytes())
        require(all(ready.get(k) == v for k, v in scope().items()) and ready.get('identity') == fixture['identity']
                and ready.get('descriptor') == fixture['descriptor'], 'RETAINED_LIVE_CHANGED')
        claim = private_json(self.state / 'startup.claim.private.json')
        require(type(ready.get('pid')) is int and ready['pid'] > 1 and claim.get('pid') == ready['pid'], 'BACKEND_CLAIM_PID')
        links = self.binding_links()
        require(all(ready.get(k) == v for k, v in links.items()) and claim.get('runBindingSha256') == links['runBindingSha256'], 'BACKEND_READY_BINDING_DRIFT')
        require(type(ready.get('loopbackPort')) is int and 1024 <= ready['loopbackPort'] <= 65535, 'BACKEND_PORT')
        return ready

    def backend_owner(self, ready=None):
        launch = private_json(self.state / 'launch.private.json')
        require(all(launch.get(k) == v for k, v in scope().items()), 'BACKEND_LAUNCH_SCOPE')
        owner = {key: launch.get(key) for key in ['pid', 'processStartIdentity', 'commandSha256']}
        require(type(owner['pid']) is int and owner['pid'] > 1 and type(owner['processStartIdentity']) is str
                and type(owner['commandSha256']) is str and re.fullmatch('[a-f0-9]{64}', owner['commandSha256']), 'BACKEND_LAUNCH_IDENTITY')
        if ready is not None:
            require(ready.get('pid') == owner['pid'], 'BACKEND_READY_OWNER_CHANGED')
        startup = self.state / 'startup.claim.private.json'
        if startup.exists():
            claim = private_json(startup)
            require(claim.get('proposalSha256') == PROPOSAL_SHA and claim.get('measurementId') == MEASUREMENT
                    and type(claim.get('pid')) is int and claim['pid'] == owner['pid'], 'BACKEND_STARTUP_OWNER_CHANGED')
        return owner

    def owned_backend_alive(self, owner, timeout=3):
        current = self.backend_identity(owner['pid'], timeout=timeout)
        if current is not None and current.get('exited') is True:
            require(current.get('pid') == owner['pid'] and current.get('processStartIdentity') == owner['processStartIdentity'], 'BACKEND_OWNERSHIP_CHANGED')
            return False
        require(current is None or current == owner, 'BACKEND_OWNERSHIP_CHANGED')
        return current is not None

    def backend_alive(self, ready):
        self.ensure_open()
        require(self.owned_backend_alive(self.backend_owner(ready)), 'BACKEND_NOT_RUNNING')

    def arm(self, android):
        self.gate()
        build, directory = self.selected_build()
        android.preflight()
        require(not android.pid(), 'EXISTING_QA_PROCESS_REFUSED')
        self.claim('install')
        android.run('install', '-r', str(directory / 'consumer.apk'), timeout=120)
        require(not android.pid(), 'QA_INITIAL_OWNERSHIP_CHANGED')
        self.claim('initial-start', {'apkSha256': build['apkSha256']})
        android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + helpers.ACTIVITY)
        deadline = time.monotonic() + 30
        pid, owned = helpers.observe_launch_pid(android, deadline)
        write_json(self.private / 'android/owned-process.private.json', {**scope(), **owned})
        uri = None
        while time.monotonic() < deadline:
            logs = android.run('logcat', '-d', '--pid=' + pid, '-s', 'flutter:I', timeout=helpers.arm_remaining(deadline))
            found = re.findall(r'Dart VM service is listening on (http://127\.0\.0\.1:\d+/[A-Za-z0-9_=-]+/)', logs)
            if found:
                uri = found[-1]; break
            time.sleep(.15)
        require(uri is not None, 'WAITING_VM_UNAVAILABLE')
        parsed = urlsplit(uri)
        host = helpers.available_host_port()
        forward = {**scope(), **owned, 'hostPort': host, 'devicePort': parsed.port, 'serial': 'emulator-5554', 'baselineMappingAbsent': True}
        rows = [line.split() for line in android.run('forward', '--list').splitlines()]
        require(not any(len(row) == 3 and row[1] == 'tcp:' + str(host) for row in rows), 'FORWARD_PRESENT')
        helpers.require_owned_qa(android, owned)
        write_json(self.private / 'android/forward-intent.private.json', forward)
        android.run('forward', '--no-rebind', 'tcp:' + str(host), 'tcp:' + str(parsed.port))
        connection = {**forward, 'websocket': 'ws://127.0.0.1:' + str(host) + parsed.path + 'ws', 'apkSha256': build['apkSha256']}
        write_json(self.private / 'android/connection.private.json', connection)
        vm = Vm(connection['websocket'], self.private / 'receipts', 'arm', deadline, lambda: require_arm_owner(android, owned, deadline))
        try:
            vm.ready()
            baseline = vm.data({'action': 'snapshot'})
            report = validate_resource_observations([baseline])
            require(report.get('measured') is True and all(type(baseline.get(k)) is bool for k in FLAGS)
                    and baseline['ok'] and not baseline['configured'] and not baseline['stopped']
                    and baseline['cubitCount'] == 0 and not baseline['credentialActive'] and not baseline['deadlineActive']
                    and all(baseline[k] == 0 for k in ACTIVE) and baseline['resourceObservation']['reservedSlots'] == 0, 'EMPTY_BASELINE_REQUIRED')
            write_json(self.private / 'android/baseline.private.json', {**scope(), 'isolateId': vm.isolate, 'sample': baseline, **owned})
        finally:
            vm.close()

    def baseline_gate(self, android):
        connection = self.connection(android)
        original = self.receipt('android/baseline.private.json')
        vm = Vm(connection['websocket'], self.private / 'receipts', 'baseline-gate')
        try:
            vm.ready()
            require(vm.isolate == original.get('isolateId'), 'VM_ISOLATE_CHANGED')
            sample = vm.data({'action': 'snapshot'})
            require(all(type(sample.get(k)) is bool for k in FLAGS) and sample['ok']
                    and not sample['configured'] and not sample['stopped'] and not sample['cubitClosed']
                    and not sample['credentialActive'] and not sample['deadlineActive'] and sample.get('cubitCount') == 0
                    and sample.get('events') == [] and all(type(sample.get(k)) is int and sample[k] == 0 for k in ACTIVE)
                    and sample.get('resourceObservation') == original['sample']['resourceObservation'], 'BASELINE_DRIFT')
        finally:
            vm.close()
        return connection

    def reserve_backend(self, android):
        self.gate(); self.baseline_gate(android)
        self.claim('backend-start')
        self.state.mkdir(mode=0o700, exist_ok=True)
        write_json(self.state / 'approval.json', machine_approval())
        write_json(self.state / 'startup.reservation.private.json', {**scope(), 'kind': 'BACKEND_START', 'state': 'RESERVED_NO_AUTOMATIC_RETRY', 'maxStarts': 1})

    def login(self, args, android):
        self.gate(); self.baseline_gate(android)
        ready = self.ready(); self.backend_alive(ready)
        now = time.time() * 1000
        require(type(ready.get('localGateStartedAtEpochMillis')) is int and type(ready.get('localGateDeadlineEpochMillis')) is int
                and ready['localGateStartedAtEpochMillis'] <= now < ready['localGateDeadlineEpochMillis'] - 12000
                and ready['localGateDeadlineEpochMillis'] - ready['localGateStartedAtEpochMillis'] == 300000, 'LOGIN_GATE_EXPIRED')
        self.claim('login')
        reservation = {**scope(), 'qaSignInInvocationsTotal': 4, 'firebaseVerifierReservationsTotal': 32,
                       'accountLookupReserved': 1, 'tokenExchangeReserved': 1, 'loginVerifierReserved': 1}
        write_json(self.state / 'login.reservation.private.json', reservation)
        import native_test_login as native
        inputs = native.load_inputs(args.admin_credential, args.web_api_config, self.state / 'id-token.private')
        require(inputs.uid == UID and inputs.project == 'tellpal-ee0dd', 'LOGIN_IDENTITY_CHANGED')
        token = native.login_in_worker(inputs)
        native.write_token(inputs, token)
        claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
        write_json(self.state / 'qa-sign-in.ready.json', {**reservation, 'qaUid': UID, 'tokenFile': str(inputs.output), 'expiresAtEpochMillis': claims['exp'] * 1000})

    def open_window(self, android):
        self.gate(); self.baseline_gate(android)
        ready = self.ready(); self.backend_alive(ready)
        require((self.state / 'qa-sign-in.ready.json').is_file(), 'LOGIN_REQUIRED')
        self.claim('window')
        helpers.write_new(self.state / 'open.request', b'OPEN\n')
        until = time.monotonic() + 5
        while time.monotonic() < until and not (self.state / 'ready.json').exists():
            self.backend_alive(ready); time.sleep(.05)
        self.ready(window=True)

    def run(self, android):
        self.gate(); connection = self.connection(android)
        ready = self.ready(window=True); self.backend_alive(ready)
        self.claim('measurement')
        output = self.private / 'observations'; output.mkdir(mode=0o700)
        vm = Vm(connection['websocket'], self.private / 'receipts', 'measurement')
        records = []
        deadline = ready['windowDeadlineEpochMillis']; last_wall = time.time() * 1000
        mono_end = time.monotonic() + (deadline - last_wall) / 1000
        def check():
            nonlocal last_wall
            wall = time.time() * 1000
            require(last_wall <= wall < deadline and time.monotonic() < mono_end, 'WINDOW_EXPIRED_OR_CLOCK_ROLLBACK')
            last_wall = wall
            helpers.require_owned_qa(android, connection)
        def capture(phase, action=None):
            check()
            sample = vm.data(action or {'action': 'snapshot'})
            require(sample.get('ok') is True, 'PROBE_REFUSED')
            item = {'phase': phase, 'sample': sample}; records.append(item)
            write_json(output / f'{len(records):02d}-{phase}.private.json', item)
            return sample
        try:
            vm.ready()
            original = self.receipt('android/baseline.private.json')
            require(vm.isolate == original['isolateId'], 'VM_ISOLATE_CHANGED')
            baseline = capture('baseline')
            require(baseline['resourceObservation'] == original['sample']['resourceObservation'], 'BASELINE_DRIFT')
            port = ready['loopbackPort']
            rows = [line.split() for line in android.run('reverse', '--list').splitlines()]
            require(all(len(r) == 3 for r in rows) and not any(r[1] == 'tcp:' + str(port) for r in rows), 'REVERSE_PRESENT')
            write_json(self.private / 'android/reverse-intent.private.json', {**scope(), 'serial': 'emulator-5554', 'devicePort': port, 'hostPort': port, 'baselineMappingAbsent': True})
            android.run('reverse', '--no-rebind', 'tcp:' + str(port), 'tcp:' + str(port))
            auth = private_json(self.state / 'qa-sign-in.ready.json')
            token = helpers.private_file(Path(auth['tokenFile']), 8192, external=True).decode().strip()
            check(); configuration = runtime_configuration(ready, token)
            require(vm.data(configuration).get('ok') is True, 'CONFIGURE_REFUSED')
            configuration.clear(); token = None
            transitions = [('QA sayfalarını aç', False), ('Sonraki', False), ('QA detail ekranına dön', True)]
            for order in range(4):
                if order:
                    check(); vm.tap(*transitions[order - 1])
                until = time.monotonic() + 45
                while True:
                    check(); sample = vm.data({'action': 'snapshot'})
                    require(sample.get('ok') is True and sample.get('status') != 'error', 'MEDIA_REFUSED')
                    loaded = [e for e in sample['events'] if e.get('kind') == 'state' and e.get('status') == 'loaded']
                    if sample.get('status') == 'loaded' and len(loaded) == order + 1:
                        break
                    require(time.monotonic() < until, 'PAINT_TIMEOUT'); time.sleep(.1)
                sample = capture(f'paint{order + 1}', {'action': 'paint', 'epoch': loaded[-1]['epoch']})
                helpers.validate_loaded(sample, ready, order)
                android.screenshot(output / f'{order + 1:02d}-paint.png')
                numbered(output, 'memory', {'phase': f'paint{order + 1}', **android.memory()})
            before = capture('beforeBackground')
            validate_before_background(records[4]['sample'], before)
            check(); android.run('shell', 'input', 'keyevent', 'KEYCODE_HOME'); time.sleep(.4)
            capture('background'); check(); android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + helpers.ACTIVITY); time.sleep(.4)
            capture('resumed'); check(); vm.tap('QA akışını kapat', True)
            for _ in range(50):
                check(); sample = vm.data({'action': 'snapshot'})
                if sample.get('cubitClosed') is True:
                    break
                time.sleep(.1)
            capture('closedUi'); capture('stopped', {'action': 'stop'})
            android.screenshot(output / '05-closed.png')
            result = validate_samples(records, ready)
            self.gate()
            write_json(output / 'accepted.private.json', {**scope(), **result, 'rawSha256': {p.name: digest(p) for p in output.glob('*-*.private.json')}})
        except Exception as error:
            numbered(output, 'attempt-failure', {**scope(), 'code': helpers.safe_code(error), 'status': 'FAILED_NO_RETRY'})
            raise
        finally:
            try:
                stopped = vm.data({'action': 'stop'})
                numbered(output, 'cleanup-stop', stopped)
            finally:
                vm.close()

    def backend_identity(self, pid, timeout=3):
        require(type(pid) is int and pid > 1, 'BACKEND_PID_REQUIRED')
        environment = os.environ.copy(); environment['LC_ALL'] = 'C'
        result = subprocess.run(['ps', '-p', str(pid), '-o', 'stat=', '-o', 'lstart=', '-o', 'command='],
                                capture_output=True, timeout=timeout, env=environment)
        if result.returncode == 1:
            return None
        require(result.returncode == 0, 'BACKEND_INSPECTION_FAILED')
        status, _, row = result.stdout.decode().strip().partition(' ')
        match = re.fullmatch(r'\s*([A-Za-z]{3}\s+[A-Za-z]{3}\s+[0-9]{1,2}\s+[0-9:]{8}\s+[0-9]{4})\s+(.+?)\s*', row)
        require(match is not None, 'BACKEND_START_IDENTITY_REQUIRED')
        if status.startswith('Z'):
            return {'pid': pid, 'processStartIdentity': ' '.join(match[1].split()), 'exited': True}
        words = shlex.split(match[2])
        require('com.tellpal.v2.support.P4b5ConsumerQaSetup' in words and words[-2:] == ['--execute', str(self.state)], 'BACKEND_OWNERSHIP_CHANGED')
        return {'pid': pid, 'processStartIdentity': ' '.join(match[1].split()),
                'commandSha256': hashlib.sha256(match[2].encode()).hexdigest()}

    def record_backend_launch(self, pid):
        self.ensure_open()
        reservation = private_json(self.private / 'backend-start.claim.private.json')
        require(all(reservation.get(k) == v for k, v in scope().items()), 'BACKEND_START_NOT_RESERVED')
        owner = self.backend_identity(pid)
        require(owner is not None and owner.get('exited') is not True, 'BACKEND_ALREADY_EXITED')
        write_json(self.state / 'launch.private.json', {**scope(), **owner})
        return owner

    def cleanup_before_ready(self):
        # Bu makbuz yalnız owned süreç çıkışını doğrular; native drain başarısı üretmez.
        owner = self.backend_owner()
        import signal
        deadline = time.monotonic() + 5
        def alive():
            remaining = deadline - time.monotonic()
            require(remaining > 0, 'PRE_READY_CLEANUP_TIMEOUT')
            return self.owned_backend_alive(owner, timeout=min(1, remaining))
        def wait(seconds):
            until = min(deadline, time.monotonic() + seconds)
            while time.monotonic() < until:
                if not alive(): return False
                time.sleep(min(.1, max(0, until - time.monotonic())))
            return alive()
        if alive():
            if not (self.state / 'close.request').exists():
                helpers.write_new(self.state / 'close.request', b'CLOSE\n')
            running = wait(.5)
            for sig, seconds in [(signal.SIGTERM, 2), (signal.SIGKILL, 1)]:
                if not running: break
                require(alive(), 'BACKEND_EXIT_RACE_NO_SIGNAL')
                os.kill(owner['pid'], sig)
                running = wait(seconds)
            require(not running, 'OWNED_BACKEND_NOT_TERMINATED')
        return {'status': 'TERMINAL_PRE_READY_PROCESS_EXIT_CONFIRMED', 'pid': owner['pid'],
                'processExitConfirmed': True, 'drainVerified': False, 'measurementSucceeded': False}

    def close_ready_backend(self):
        ready = self.ready(); owner = self.backend_owner(ready)
        def completed():
            return validate_backend_result(self.state, ready, self.binding_links())
        if not self.owned_backend_alive(owner):
            return completed()
        if not (self.state / 'close.request').exists():
            helpers.write_new(self.state / 'close.request', b'CLOSE\n')
        until = time.monotonic() + 8
        while time.monotonic() < until:
            if not self.owned_backend_alive(owner): return completed()
            time.sleep(.1)
        import signal
        for sig in [signal.SIGTERM, signal.SIGKILL]:
            if not self.owned_backend_alive(owner):
                raise ValueError('BACKEND_WATCHDOG_WITHOUT_DRAIN')
            os.kill(owner['pid'], sig)
            until = time.monotonic() + 2
            while time.monotonic() < until:
                if not self.owned_backend_alive(owner): raise ValueError('BACKEND_WATCHDOG_WITHOUT_DRAIN')
                time.sleep(.1)
        raise ValueError('OWNED_BACKEND_NOT_TERMINATED')

    def cleanup(self, android, for_success=False):
        self.claim('cleanup')
        results = {}
        def clean(name, work):
            try:
                results[name] = {'status': 'PASS', 'result': work()}
            except Exception as error:
                results[name] = {'status': 'FAILED', 'code': helpers.safe_code(error)}
        try:
            for name, work in [('reverse', lambda p: helpers.remove_owned_reverse(android, p)), ('forward', lambda p: helpers.remove_owned_forward(android, p))]:
                intent = self.private / f'android/{name}-intent.private.json'
                if intent.exists(): clean(name, lambda path=intent, action=work: action(private_json(path)))
            owned = self.private / 'android/owned-process.private.json'
            if owned.exists(): clean('qaProcess', lambda: helpers.close_owned_qa(android, private_json(owned)))
            elif (self.private / 'initial-start.claim.private.json').exists(): results['qaProcess'] = {'status': 'FAILED', 'code': 'POSITIVE_OWNED_PID_REQUIRED'}
            if (self.state / 'local-ready.json').exists(): clean('backend', self.close_ready_backend)
            elif (self.state / 'launch.private.json').exists(): clean('backend', self.cleanup_before_ready)
            write_json(self.private / 'cleanup.private.json', {**scope(), 'results': results, 'ok': bool(results) and all(r['status'] == 'PASS' for r in results.values())})
            return results
        finally:
            if not for_success and not (self.private / 'terminal.private.json').exists():
                write_json(self.private / 'terminal.private.json', {**scope(), 'status': 'FAILED_NO_RETRY', 'reason': 'CLEANUP_CLOSED_ATTEMPT'})


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stage', choices=['build', 'arm', 'reserve-backend', 'record-backend', 'login', 'open', 'run', 'cleanup'])
    parser.add_argument('--proposal-sha256')
    parser.add_argument('--adb', type=Path)
    parser.add_argument('--pid', type=int)
    parser.add_argument('--aapt', type=Path)
    parser.add_argument('--admin-credential', type=Path)
    parser.add_argument('--web-api-config', type=Path)
    args = parser.parse_args(argv)
    if args.stage is None:
        print('DRY_RUN_ZERO_FILE_CREDENTIAL_DEVICE_DB_NETWORK_IO_P4B5'); return 0
    controller = Controller()
    try:
        require(args.proposal_sha256 == PROPOSAL_SHA, 'EXACT_PROPOSAL_REQUIRED')
        if args.stage == 'build':
            print(controller.build(args)); return 0
        if args.stage == 'record-backend':
            controller.record_backend_launch(args.pid)
            print('OWNED_BACKEND_PID_START_RECORDED_NO_AUTH'); return 0
        require(args.adb is not None, 'ADB_REQUIRED')
        android = helpers.Android(args.adb, 'emulator-5554')
        if args.stage == 'arm': controller.arm(android)
        elif args.stage == 'reserve-backend': controller.reserve_backend(android)
        elif args.stage == 'login': controller.login(args, android)
        elif args.stage == 'open': controller.open_window(android)
        elif args.stage == 'run':
            controller.run(android)
            results = controller.cleanup(android, for_success=True)
            require(bool(results) and all(r['status'] == 'PASS' for r in results.values()), 'CLEANUP_FAILED')
            require(results.get('backend', {}).get('result', {}).get('measurementSucceeded') is True, 'BACKEND_MEASUREMENT_SUCCESS_REQUIRED')
            accepted = controller.receipt('observations/accepted.private.json')
            write_json(controller.private / 'terminal.private.json', {**scope(), 'status': 'FOUR_UI_PAINTS_VERIFIED_EMULATOR_ONLY',
                'acceptedSha256': digest(controller.private / 'observations/accepted.private.json'),
                'cleanupSha256': digest(controller.private / 'cleanup.private.json'), 'physicalQualityAccepted': False})
        else: controller.cleanup(android)
        print('P4B5_STAGE_COMPLETED_PRIVATE_EVIDENCE'); return 0
    except Exception as error:
        if args.stage != 'build' and controller.private.exists():
            if args.stage != 'cleanup' and 'android' in locals() and not (controller.private / 'cleanup.claim.private.json').exists():
                try:
                    controller.cleanup(android)
                except Exception as cleanup_error:
                    numbered(controller.private / 'receipts', 'cleanup-failure', {**scope(), 'code': helpers.safe_code(cleanup_error)})
            numbered(controller.private / 'receipts', 'terminal-failure', {**scope(), 'stage': args.stage, 'code': helpers.safe_code(error), 'status': 'FAILED_NO_RETRY'})
            if not (controller.private / 'terminal.private.json').exists():
                write_json(controller.private / 'terminal.private.json', {**scope(), 'status': 'FAILED_NO_RETRY', 'stage': args.stage})
        print('REFUSED_P4B5_NO_RETRY_PRIVATE_EVIDENCE'); return 2


if __name__ == '__main__':
    sys.exit(main())
