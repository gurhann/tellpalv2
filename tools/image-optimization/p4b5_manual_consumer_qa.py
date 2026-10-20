#!/usr/bin/env python3
"""Sabit elle QA kapsamı. VM kullanmadan gerçek app receipt kapısını uygular."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time

import p4b5_consumer_qa as prior
import p4_consumer_qa as helpers
from p4b5_qa_scope import P4B5, P4B5_RETRY, P4B5_MANUAL

SCOPE = P4B5_MANUAL
PACKAGE = 'com.tellpal.tellpal_mobile_poc.readingmanualqa'
ROOT = prior.ROOT
RELATIVE_ROOT = 'files/manual-consumer-qa'
IDENTITY_KEYS = ['measurementId', 'proposalSha256', 'budgetRunId', 'nonce', 'pid', 'processStartTicks', 'lifetimeId', 'supportDirectory']
require = prior.require


def expected_prior_proof(proposal):
    return {**SCOPE.identity(), **{k: proposal[k] for k in ['previousAttemptsEvidence', 'historicalEvidence', 'priorConsumption']},
            'previousAttemptVerifiedByFileHashes': True, 'currentDatabaseQueried': False,
            'oldBackendLedgerExists': False, 'actualStagesExecuted': False}


def validate_prior_proof(proposal_bytes, proof):
    require(hashlib.sha256(proposal_bytes).hexdigest() == SCOPE.proposal_sha256, 'PRIOR_ATTEMPT_PROPOSAL_CHANGED')
    proposal = json.loads(proposal_bytes)
    require(prior.exact(proof, expected_prior_proof(proposal)), 'PRIOR_ATTEMPT_PROOF_CHANGED')
    require(len(proposal['previousAttemptsEvidence']) == 2, 'BOTH_PRIOR_ATTEMPTS_REQUIRED')
    for index, previous in enumerate(proposal['previousAttemptsEvidence']):
        selected = [P4B5, P4B5_RETRY][index]
        require(previous['privateRoot'] == str(selected.private_root) and previous['measurementId'] == selected.measurement_id
                and len(previous['files']) == [12, 6][index] and previous['terminalStatus'] == 'FAILED_NO_RETRY'
                and previous['mediaRequestsConsumed'] == 0 and previous['additionalDeliveryOperations'] == 0
                and previous['additionalDeliveryReservedBytes'] == 0, 'PRIOR_ATTEMPT_SCOPE_CHANGED')
        for name, sha in previous['files'].items():
            path = Path(name)
            require(not path.is_absolute() and '..' not in path.parts and str(path) == name, 'PRIOR_ATTEMPT_PATH_REFUSED')
            require(hashlib.sha256(helpers.private_file(selected.private_root / path, 4194304, external=True)).hexdigest() == sha, 'PRIOR_ATTEMPT_FILE_CHANGED')


def validate_receipts(raw_receipts, expected_owner=None, complete=True):
    """Hash zinciri ve gerçek PID/lifetime bağını saf olarak doğrular."""
    require(type(raw_receipts) is list and 0 < len(raw_receipts) <= 10, 'RECEIPT_COUNT')
    if complete:
        require(len(raw_receipts) == 10, 'TEN_PHASES_REQUIRED')
    previous, total, owner, records, last_epoch, events = '0' * 64, 0, None, [], None, []
    last_time = None
    for index, raw in enumerate(raw_receipts):
        require(type(raw) is bytes and 0 < len(raw) <= 1048576, 'RECEIPT_SIZE')
        total += len(raw)
        require(total <= 12582912, 'RECEIPT_TOTAL_SIZE')
        value = json.loads(raw)
        require(type(value) is dict and set(value) == set(IDENTITY_KEYS + ['ordinal', 'phase', 'previousSha256', 'recordedAtEpochMillis', 'sample']), 'RECEIPT_FIELDS')
        identity = {k: value[k] for k in IDENTITY_KEYS}
        require(all(identity.get(k) == v for k, v in SCOPE.identity().items())
                and type(identity['pid']) is int and identity['pid'] > 1
                and type(identity['processStartTicks']) is str and re.fullmatch('[1-9][0-9]*', identity['processStartTicks'])
                and type(identity['nonce']) is str and re.fullmatch('[a-f0-9]{64}', identity['nonce'])
                and type(identity['lifetimeId']) is str and re.fullmatch('[a-f0-9]{32}', identity['lifetimeId'])
                and identity['supportDirectory'] in ['/data/user/0/' + PACKAGE + '/files', '/data/data/' + PACKAGE + '/files'], 'RECEIPT_IDENTITY')
        owner = identity if owner is None else owner
        require(identity == owner and (expected_owner is None or identity == expected_owner), 'RECEIPT_OWNER_CHANGED')
        require(type(value['ordinal']) is int and value['ordinal'] == index + 1 and value['phase'] == prior.PHASES[index]
                and value['previousSha256'] == previous, 'RECEIPT_ORDER_OR_CHAIN')
        require(type(value['recordedAtEpochMillis']) is int and (last_time is None or value['recordedAtEpochMillis'] >= last_time), 'RECEIPT_CLOCK_ROLLBACK')
        last_time = value['recordedAtEpochMillis']
        sample = value['sample']
        require(type(sample) is dict and sample.get('resourceObservation', {}).get('lifetimeId') == owner['lifetimeId'], 'RECEIPT_LIFETIME')
        observed = sample.get('events')
        require(type(observed) is list and len(observed) <= 128 and observed[:len(events)] == events, 'EVENT_OVERFLOW_OR_REWRITE')
        events = observed
        if value['phase'].startswith('paint'):
            epoch = sample.get('paintedEpoch')
            require(type(epoch) is int and (last_epoch is None or epoch > last_epoch), 'PAINT_EPOCH_REPLAY')
            last_epoch = epoch
        records.append({'phase': value['phase'], 'sample': sample})
        previous = hashlib.sha256(raw).hexdigest()
    if complete:
        prior.validate_phase_resources(records)
    else:
        baseline = records[0]['sample']
        require(all(type(baseline.get(k)) is bool for k in prior.FLAGS)
                and baseline['ok'] and not baseline['configured'] and not baseline['stopped']
                and not baseline['credentialActive'] and not baseline['deadlineActive'] and not baseline['cubitClosed']
                and baseline.get('cubitCount') == 0 and baseline.get('events') == []
                and all(type(baseline.get(k)) is int and baseline[k] == 0 for k in prior.ACTIVE)
                and baseline['resourceObservation']['reservedSlots'] == 0
                and baseline['resourceObservation']['reservedDecodedBytes'] == '0', 'EMPTY_BASELINE_REQUIRED')
    return owner, records


def remaining(deadline):
    value = deadline - time.monotonic()
    require(value > 0, 'ADB_DEADLINE')
    return value


def validate_captures(raw_acks, raw_receipts, pngs):
    require(len(raw_acks) == len(pngs) == 4, 'FOUR_CAPTURE_ACKS_REQUIRED')
    owner, _ = validate_receipts(raw_receipts)
    total = sum(map(len, raw_receipts))
    for index, (raw, png) in enumerate(zip(raw_acks, pngs), 1):
        require(type(raw) is bytes and 0 < len(raw) <= 20480 and type(png) is bytes
                and png.startswith(b'\x89PNG\r\n\x1a\n') and len(png) <= 16777216, 'CAPTURE_SIZE')
        ack = json.loads(raw)
        paint = json.loads(raw_receipts[index])
        require(prior.exact(ack, {**owner, 'phase': 'paint' + str(index),
                'epoch': paint['sample']['paintedEpoch'], 'receiptSha256': hashlib.sha256(raw_receipts[index]).hexdigest(),
                'pngSha256': hashlib.sha256(png).hexdigest()}), 'CAPTURE_BINDING_CHANGED')
        total += len(raw)
    require(total <= 12582912, 'RECEIPT_TOTAL_SIZE')


class ManualAndroid(helpers.Android):
    """Mevcut ADB üstüne yalnız sabit app-private stdin/dosya işlemleri ekler."""
    def preflight(self, timeout=30):
        end = time.monotonic() + timeout
        checks = [(['get-state'], lambda v: v == 'device'),
                  (['shell', 'getprop', 'ro.kernel.qemu'], lambda v: v == '1'),
                  (['shell', 'wm', 'size'], lambda v: 'Override size: 822x1828' in v),
                  (['shell', 'wm', 'density'], lambda v: 'Override density: 320' in v),
                  (['shell', 'settings', 'get', 'system', 'font_scale'], lambda v: float(v) == 1)]
        for args, validate in checks:
            value = self.run(*args, timeout=remaining(end))
            remaining(end)
            require(validate(value), 'EMULATOR_PREFLIGHT_CHANGED')

    def private_shell(self, script, data=None, timeout=3):
        # Shell v2 uzak exit kodunu taşır; tek komut argümanı iki shell sınırında da korunur.
        command = shlex.join(['run-as', PACKAGE, 'sh', '-c', script])
        return subprocess.run(self.args + ['shell', '-T', command], input=data,
                              stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=timeout)

    def failure_marker(self, timeout=3):
        path = RELATIVE_ROOT + '/receipts/terminal-failure.json'
        result = self.private_shell(f'if [ -e {path} ]; then exit 1; fi; test ! -L {path}', timeout=timeout)
        require(result.returncode == 0, 'APP_TERMINAL_FAILURE')

    def capture_ack(self, value, timeout=3):
        require(type(value) is dict and set(value) == set(IDENTITY_KEYS + ['phase', 'epoch', 'receiptSha256', 'pngSha256']), 'CAPTURE_FIELDS')
        require(re.fullmatch('paint[1-4]', value['phase']) and type(value['epoch']) is int, 'CAPTURE_PHASE')
        for field in ['receiptSha256', 'pngSha256']:
            require(type(value[field]) is str and re.fullmatch('[a-f0-9]{64}', value[field]), 'CAPTURE_SHA')
        path = RELATIVE_ROOT + '/captures/capture-0' + value['phase'][-1] + '.json'
        directory = RELATIVE_ROOT + '/captures'
        raw = json.dumps(value, separators=(',', ':')).encode()
        require(len(raw) <= 20480, 'CAPTURE_SIZE')
        script = (f'umask 077; test ! -L files && test ! -L {RELATIVE_ROOT} && test ! -L {directory} && '
                  f'test "$(stat -c %a {directory})" = 700 && test ! -e {path} && test ! -e {path}.part && '
                  f'set -C && cat > {path}.part && chmod 600 {path}.part && mv -n {path}.part {path} && '
                  f'test ! -e {path}.part && printf \'OK\\n\'')
        result = self.private_shell(script, raw, timeout=timeout)
        require(result.returncode == 0 and result.stdout == b'OK\n', 'CAPTURE_ACK_REFUSED')
        return raw

    def capture_png(self, timeout=3):
        result = subprocess.run(self.args + ['exec-out', 'screencap', '-p'], capture_output=True, timeout=timeout)
        require(result.returncode == 0 and result.stdout.startswith(b'\x89PNG\r\n\x1a\n'), 'SCREENSHOT_REFUSED')
        return result.stdout

    def pid(self, timeout=15):
        result = subprocess.run(self.args + ['shell', 'pidof', PACKAGE], capture_output=True, timeout=timeout)
        require(result.returncode in [0, 1], 'PID_QUERY_FAILED')
        value = result.stdout.decode().strip()
        require(not value or re.fullmatch('[1-9][0-9]*', value), 'ONE_QA_PID_REQUIRED')
        return value

    def process_identity(self, pid, timeout=15):
        require(type(pid) is int and pid > 1, 'QA_PID_REQUIRED')
        end = time.monotonic() + timeout
        require(self.run('shell', 'cat', f'/proc/{pid}/cmdline', timeout=remaining(end)).split('\x00')[0] == PACKAGE, 'QA_PROCESS_COMMAND_CHANGED')
        fields = self.run('shell', 'cat', f'/proc/{pid}/stat', timeout=remaining(end)).rsplit(') ', 1)[-1].split()
        require(len(fields) > 19 and re.fullmatch('[1-9][0-9]*', fields[19]), 'QA_PROCESS_START_REQUIRED')
        return {'pid': pid, 'processStartTicks': fields[19]}

    def owner(self, expected, timeout=15):
        end = time.monotonic() + timeout
        require(self.pid(timeout=remaining(end)) == str(expected['pid']) and self.process_identity(expected['pid'], timeout=remaining(end)) ==
                {k: expected[k] for k in ['pid', 'processStartTicks']}, 'QA_OWNER_CHANGED')

    def private_root(self, timeout=3):
        root = self.run('shell', '-T', shlex.join(['run-as', PACKAGE, 'pwd']), timeout=timeout).strip()
        require(re.fullmatch(r'/data/(?:user/0|data)/' + re.escape(PACKAGE), root), 'APP_PRIVATE_ROOT_REQUIRED')
        return root

    def read_receipt(self, index, timeout=3):
        require(type(index) is int and 0 <= index < 10, 'RECEIPT_INDEX')
        end = time.monotonic() + timeout
        self.private_root(timeout=remaining(end))
        path = f'{RELATIVE_ROOT}/receipts/{index + 1:02d}-{prior.PHASES[index]}.json'
        # Sabit komut; receipt henüz yoksa yalnız NOT_READY dönülür.
        script = f'if [ -L files ] || [ -L {RELATIVE_ROOT} ] || [ -L {RELATIVE_ROOT}/receipts ] || [ -L {path} ]; then exit 1; fi; if [ ! -e {path} ]; then exit 3; fi; test ! -L files && test ! -L {RELATIVE_ROOT} && test ! -L {RELATIVE_ROOT}/receipts && test "$(stat -c %a {RELATIVE_ROOT})" = 700 && test "$(stat -c %a {RELATIVE_ROOT}/receipts)" = 700 && test ! -L {path} && test "$(stat -c %a {path})" = 600 && test "$(stat -c %s {path})" -le 1048576 && cat {path}'
        result = self.private_shell(script, timeout=remaining(end))
        require(result.returncode in [0, 3], 'PRIVATE_RECEIPT_READ_REFUSED')
        require(result.returncode == 3 or bool(result.stdout), 'EMPTY_PRIVATE_RECEIPT')
        require(time.monotonic() < end, 'ADB_DEADLINE')
        return None if result.returncode == 3 else result.stdout

    def write_configuration(self, value, timeout=3):
        end = time.monotonic() + timeout
        self.private_root(timeout=remaining(end))
        require(type(value) is dict and set(value) == set(IDENTITY_KEYS + ['configure'])
                and type(value['nonce']) is str and re.fullmatch('[a-f0-9]{64}', value['nonce']), 'HANDOFF_IDENTITY_REQUIRED')
        nonce = value['nonce']
        raw = json.dumps(value, separators=(',', ':')).encode()
        require(len(raw) <= 20480, 'CONFIGURATION_SIZE')
        path = RELATIVE_ROOT + '/startup/configure.json'
        # noclobber + sabit adlar; token yalnız subprocess stdin içindedir.
        script = (f'umask 077; test -d {RELATIVE_ROOT}/startup && test ! -L files && test ! -L {RELATIVE_ROOT} && '
                  f'test ! -L {RELATIVE_ROOT}/startup && test "$(stat -c %a {RELATIVE_ROOT}/startup)" = 700 && '
                  f'test "$(stat -c %a {RELATIVE_ROOT})" = 700 && test ! -e {RELATIVE_ROOT}/startup/handoff.owner && '
                  f'test ! -e {path} && test ! -e {path}.part && set -C && printf %s {nonce} > {RELATIVE_ROOT}/startup/handoff.owner && cat > {path}.part && '
                  f'chmod 600 {path}.part && mv -n {path}.part {path} && test ! -e {path}.part && printf \'OK\\n\'')
        result = self.private_shell(script, raw, timeout=remaining(end))
        require(result.returncode == 0 and result.stdout == b'OK\n', 'PRIVATE_STDIN_HANDOFF_REFUSED')
        require(time.monotonic() < end, 'ADB_DEADLINE')

    def remove_owned_configuration(self, nonce):
        require(type(nonce) is str and re.fullmatch('[a-f0-9]{64}', nonce), 'HANDOFF_NONCE_REQUIRED')
        self.private_root()
        path = RELATIVE_ROOT + '/startup/configure.json'
        owner = RELATIVE_ROOT + '/startup/handoff.owner'
        result = self.private_shell(f'if [ ! -e {owner} ]; then test ! -e {path} && test ! -e {path}.part; '
                 f'else test ! -L files && test ! -L {RELATIVE_ROOT} && test ! -L {RELATIVE_ROOT}/startup && '
                 f'test ! -L {owner} && test "$(stat -c %a {owner})" = 600 && test "$(cat {owner})" = {nonce} && '
                 f'test ! -L {path} && test ! -L {path}.part && rm -f {path} {path}.part {owner}; fi', timeout=3)
        require(result.returncode == 0, 'PRIVATE_CLEANUP_REFUSED')


class ManualController(prior.Controller):
    def __init__(self, private=None, root=ROOT):
        super().__init__(private=private, root=root, selected=SCOPE)
        self.package = PACKAGE

    def execution_approval_gate(self, run_path):
        approval = self.receipt('receipts/execution-approval-01.private.json')
        build, _ = self.selected_build()
        require(prior.exact(approval, {**SCOPE.identity(), 'decision': 'APPROVE_ONE_REAL_MANUAL_ATTEMPT',
                'runBindingSha256': prior.digest(run_path), 'apkSha256': build['apkSha256']}), 'SEPARATE_REAL_EXECUTION_APPROVAL_REQUIRED')

    def retry_bindings(self, run):
        proof_name = run.get('priorAttemptProofFile', '')
        require(re.fullmatch(r'prior-attempt-proof-[0-9]{4}\.private\.json', proof_name), 'NUMBERED_RECEIPT_REQUIRED')
        proof_path = self.private / 'receipts' / proof_name
        require(prior.digest(proof_path) == run.get('priorAttemptProofSha256'), 'REVIEWED_RECEIPT_CHANGED')
        validate_prior_proof((self.root / SCOPE.proposal).read_bytes(), prior.private_json(proof_path))
        python_name = run.get('pythonBindingFile', '')
        require(re.fullmatch(r'python-binding-[0-9]{4}\.private\.json', python_name), 'NUMBERED_RECEIPT_REQUIRED')
        path = self.private / 'receipts' / python_name
        require(prior.digest(path) == run.get('pythonBindingSha256'), 'REVIEWED_RECEIPT_CHANGED')
        binding = prior.private_json(path)
        executable, binary = Path(binding.get('pythonExecutable', '')), Path(binding.get('pythonBinary', ''))
        controller = self.copied_source() / 'tools/image-optimization/p4b5_manual_consumer_qa.py'
        require(all(binding.get(k) == v for k, v in SCOPE.identity().items())
                and executable == Path(sys.executable) and executable.resolve() == binary
                and prior.digest(binary) == binding.get('pythonBinarySha256') and Path(__file__).resolve() == controller
                and prior.digest(controller) == binding.get('controllerSha256'), 'PYTHON_RUNTIME_CHANGED')
        files = binding.get('files', {})
        for name in ['p4b5_manual_consumer_qa.py', 'p4b5_consumer_qa.py', 'p4b5_qa_scope.py', 'p4_consumer_qa.py',
                     'p4_resource_observation.py', 'p4_reading_comparison.py', 'native_test_login.py', 'native_entry_config.py']:
            require(str(controller.parent / name) in files, 'PYTHON_IMPORT_CLOSURE_REQUIRED')
        require(str(self.copied_source() / 'tools/mobile-test-account.json') in files, 'QA_ACCOUNT_BINDING_REQUIRED')
        for name, sha in files.items():
            path = Path(name)
            require(path.is_absolute() and path.resolve() == path and prior.digest(path) == sha, 'PYTHON_IMPORT_CHANGED')
        for module in list(sys.modules.values()):
            file = getattr(module, '__file__', None)
            name = getattr(module, '__name__', '')
            if file and (name in ['p4b5_manual_consumer_qa', 'p4b5_consumer_qa', 'p4b5_qa_scope', 'p4_consumer_qa',
                                 'p4_resource_observation', 'p4_reading_comparison', 'native_test_login', 'native_entry_config']):
                path = Path(file).resolve()
                require(str(path) in files and prior.digest(path) == files[str(path)], 'PYTHON_RUNTIME_IMPORT_CHANGED')
        require(run.get('pythonCommandPrefix') == [str(executable), str(controller), '--scope', SCOPE.measurement_id,
                '--proposal-sha256', SCOPE.proposal_sha256], 'PYTHON_COMMAND_PREFIX_CHANGED')

    def android_transport(self, android):
        require(android.args[1:] == ['-s', 'emulator-5554'], 'ADB_SERIAL_CHANGED')
        executable = Path(android.args[0])
        require(executable.is_absolute() and executable.resolve() == executable and executable.is_file(), 'EXISTING_ADB_REQUIRED')
        return {'adbExecutable': str(executable), 'adbSha256': prior.digest(executable)}

    def bound_android(self):
        # İlk kurulum claim'i, sonraki CLI çağrılarının cleanup aracını da sabitler.
        claim = self.receipt('install.claim.private.json')
        require(all(claim.get(k) == v for k, v in SCOPE.identity().items())
                and re.fullmatch('[a-f0-9]{64}', claim.get('runBindingSha256', '')), 'ADB_CLAIM_SCOPE_CHANGED')
        executable = Path(claim.get('adbExecutable', ''))
        require(executable.is_absolute() and executable.resolve() == executable and executable.is_file()
                and prior.digest(executable) == claim.get('adbSha256'), 'ADB_CLAIM_BINARY_CHANGED')
        return ManualAndroid(executable, 'emulator-5554')

    def arm(self, android):
        self.gate()
        build, directory = self.selected_build()
        android.preflight()
        require(not android.pid(), 'EXISTING_QA_PROCESS_REFUSED')
        self.claim('install', self.android_transport(android))
        install_end = time.monotonic() + 120
        android.run('install', '-r', str(directory / 'consumer.apk'), timeout=remaining(install_end))
        remaining(install_end)
        require(not android.pid(), 'QA_INITIAL_OWNERSHIP_CHANGED')
        installed = helpers.installed_apk_path(android.run('shell', 'pm', 'path', PACKAGE))
        installed_sha = android.run('shell', 'sha256sum', installed).split()
        require(len(installed_sha) == 2 and installed_sha[0] == build['apkSha256'], 'INSTALLED_APK_DRIFT')
        prior.write_json(self.private / 'android/installed-apk.private.json', {**SCOPE.identity(), 'apkSha256': installed_sha[0], 'verifiedBeforeFirstStart': True})
        self.claim('initial-start', {'apkSha256': build['apkSha256']})
        deadline = time.monotonic() + 30
        android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + helpers.ACTIVITY, timeout=remaining(deadline))
        remaining(deadline)
        while time.monotonic() < deadline:
            value = android.pid(timeout=min(3, deadline - time.monotonic()))
            if value:
                owner = android.process_identity(int(value), timeout=min(3, deadline - time.monotonic()))
                prior.write_json(self.private / 'android/owned-process.private.json', {**SCOPE.identity(), **owner})
                break
            time.sleep(.1)
        else:
            raise ValueError('APP_BASELINE_DEADLINE')
        while time.monotonic() < deadline:
            android.owner(owner, timeout=remaining(deadline))
            android.failure_marker(timeout=remaining(deadline))
            raw = android.read_receipt(0, timeout=remaining(deadline))
            remaining(deadline)
            if raw is not None:
                identity, _ = validate_receipts([raw], complete=False)
                require(all(identity[k] == owner[k] for k in owner) and identity['supportDirectory'] == android.private_root(timeout=remaining(deadline)) + '/files', 'BASELINE_PID_OR_SUPPORT_CHANGED')
                remaining(deadline)
                prior.write_json(self.private / 'android/baseline.private.json', {'identity': identity,
                        'rawSha256': hashlib.sha256(raw).hexdigest(), 'sample': json.loads(raw)['sample'], **SCOPE.identity()})
                return
            time.sleep(.1)
        raise ValueError('APP_BASELINE_DEADLINE')

    def baseline_gate(self, android, deadline=None):
        end = deadline if deadline is not None else time.monotonic() + 30
        baseline = self.receipt('android/baseline.private.json')
        android.preflight(timeout=remaining(end)); remaining(end)
        android.owner(baseline['identity'], timeout=remaining(end)); remaining(end)
        android.failure_marker(timeout=remaining(end)); remaining(end)
        raw = android.read_receipt(0, timeout=remaining(end)); remaining(end)
        require(raw is not None and hashlib.sha256(raw).hexdigest() == baseline['rawSha256'], 'BASELINE_DRIFT')
        owner, _ = validate_receipts([raw], baseline['identity'], complete=False)
        require(owner['supportDirectory'] == android.private_root(timeout=remaining(end)) + '/files', 'APP_SUPPORT_DIRECTORY_CHANGED')
        build, _ = self.selected_build()
        installed = helpers.installed_apk_path(android.run('shell', 'pm', 'path', PACKAGE, timeout=remaining(end)))
        require(android.run('shell', 'sha256sum', installed, timeout=remaining(end)).split()[0] == build['apkSha256'], 'INSTALLED_APK_DRIFT')
        remaining(end)
        return owner

    def run(self, android):
        self.gate()
        ready = self.ready(window=True)
        deadline = ready['windowDeadlineEpochMillis']; last_wall = time.time() * 1000
        mono_end = time.monotonic() + (deadline - last_wall) / 1000
        def check():
            nonlocal last_wall
            wall = time.time() * 1000
            require(last_wall <= wall < deadline, 'WINDOW_EXPIRED_OR_CLOCK_ROLLBACK')
            last_wall = wall
            remaining(mono_end)
        check()
        owner = self.baseline_gate(android, deadline=mono_end); check()
        self.backend_alive(ready, timeout=remaining(mono_end)); check()
        self.claim('measurement')
        output = self.private / 'observations'; output.mkdir(mode=0o700)
        port = ready['loopbackPort']
        check()
        rows = [line.split() for line in android.run('reverse', '--list', timeout=remaining(mono_end)).splitlines()]
        check()
        require(all(len(r) == 3 for r in rows) and not any(r[1] == 'tcp:' + str(port) for r in rows), 'REVERSE_PRESENT')
        prior.write_json(self.private / 'android/reverse-intent.private.json', {**SCOPE.identity(), 'serial': 'emulator-5554',
                'devicePort': port, 'hostPort': port, 'baselineMappingAbsent': True})
        android.run('reverse', '--no-rebind', 'tcp:' + str(port), 'tcp:' + str(port), timeout=remaining(mono_end)); check()
        auth = prior.private_json(self.state / 'qa-sign-in.ready.json')
        token = helpers.private_file(Path(auth['tokenFile']), 8192, external=True).decode().strip()
        configure = prior.runtime_configuration(ready, token, SCOPE)
        try:
            android.owner(owner, timeout=remaining(mono_end)); check()
            android.write_configuration({**owner, 'configure': configure}, timeout=remaining(mono_end)); check()
        finally:
            configure.clear(); token = None
        raws, acknowledgements, pngs = [], [], []
        for index, phase in enumerate(prior.PHASES):
            while True:
                check()
                android.owner(owner, timeout=remaining(mono_end)); check()
                android.failure_marker(timeout=remaining(mono_end)); check()
                self.backend_alive(ready, timeout=remaining(mono_end)); check()
                raw = android.read_receipt(index, timeout=remaining(mono_end)); check()
                if raw is not None:
                    raws.append(raw)
                    validate_receipts(raws, owner, complete=index == 9)
                    helpers.write_new(output / f'{index + 1:02d}-{phase}.private.json', raw)
                    if phase.startswith('paint'):
                        # Sonraki UI eylemi ACK'ye kadar kilitlidir.
                        png = android.capture_png(timeout=remaining(mono_end)); check()
                        android.owner(owner, timeout=remaining(mono_end)); check()
                        android.failure_marker(timeout=remaining(mono_end)); check()
                        require(android.read_receipt(index, timeout=remaining(mono_end)) == raw, 'CAPTURE_RECEIPT_DRIFT'); check()
                        png_sha = hashlib.sha256(png).hexdigest()
                        helpers.write_new(output / f'{index:02d}-paint.png', png)
                        ack = {**owner, 'phase': phase, 'epoch': json.loads(raw)['sample']['paintedEpoch'],
                               'receiptSha256': hashlib.sha256(raw).hexdigest(), 'pngSha256': png_sha}
                        ack_raw = android.capture_ack(ack, timeout=remaining(mono_end)); check()
                        helpers.write_new(output / f'capture-{index:02d}.private.json', ack_raw)
                        acknowledgements.append(ack_raw); pngs.append(png)
                    break
                time.sleep(min(.1, remaining(mono_end)))
        android.failure_marker(timeout=remaining(mono_end)); check()
        _, records = validate_receipts(raws, owner)
        validate_captures(acknowledgements, raws, pngs)
        require(all(ready['windowStartedAtEpochMillis'] <= json.loads(raw)['recordedAtEpochMillis'] < deadline for raw in raws[1:]), 'PHASE_OUTSIDE_WINDOW')
        result = prior.validate_samples(records, ready)
        self.gate(); check()
        prior.write_json(output / 'accepted.private.json', {**SCOPE.identity(), **result,
                'captureSha256': [hashlib.sha256(raw).hexdigest() for raw in acknowledgements],
                'rawSha256': {f'{index + 1:02d}-{phase}.private.json': hashlib.sha256(raw).hexdigest()
                    for index, (phase, raw) in enumerate(zip(prior.PHASES, raws))}})

    def cleanup_reserved_backend(self, pid):
        # İlk kayıt hata verse de yalnız aynı reservation/run ve exact argv sahibi alınır.
        reservation = self.receipt('backend-start.claim.private.json')
        run_path, run = self.latest_receipt('run-binding')
        require(all(value.get(k) == v for value in [reservation, run] for k, v in SCOPE.identity().items())
                and reservation.get('runBindingSha256') == prior.digest(run_path), 'BACKEND_RESERVATION_BINDING_CHANGED')
        cp_path, classpath = self.latest_receipt('classpath-binding')
        require(run.get('classpathBindingFile') == cp_path.name and run.get('classpathBindingSha256') == prior.digest(cp_path)
                and all(classpath.get(k) == v for k, v in SCOPE.identity().items()), 'BACKEND_CLASSPATH_BINDING_CHANGED')
        tools = self.receipt('receipts/operational-tools.private.json')
        java = (Path(tools['backendJavaHome']) / 'bin/java').resolve()
        require(str(java) == classpath.get('javaBinary') and prior.digest(java) == classpath.get('javaBinarySha256'), 'BACKEND_JAVA_BINDING_CHANGED')
        owner = self.backend_identity(pid)
        require(owner is not None and owner.get('exited') is not True, 'BACKEND_OWNER_NOT_PROVEN')
        prior.write_json(self.state / 'launch.private.json', {**SCOPE.identity(), **owner})
        return self.close_ready_backend() if (self.state / 'local-ready.json').exists() else self.cleanup_before_ready()

    def cleanup(self, android, for_success=False, backend_pid=None):
        self.claim('cleanup')
        results = {}
        def device():
            require(android is not None, 'OWNED_ADB_UNAVAILABLE')
            return android
        def clean(name, work):
            try:
                results[name] = {'status': 'PASS', 'result': work()}
            except Exception as error:
                results[name] = {'status': 'FAILED', 'code': helpers.safe_code(error)}
        baseline_path = self.private / 'android/baseline.private.json'
        if baseline_path.exists():
            def remove_configuration():
                baseline = prior.private_json(baseline_path)
                require(all(baseline.get(k) == v and baseline.get('identity', {}).get(k) == v for k, v in SCOPE.identity().items()), 'CLEANUP_BASELINE_SCOPE_CHANGED')
                return device().remove_owned_configuration(baseline['identity']['nonce'])
            clean('configuration', remove_configuration)
        owned = self.private / 'android/owned-process.private.json'
        if owned.exists():
            def stop_owned():
                owner = prior.private_json(owned)
                require(all(owner.get(k) == v for k, v in SCOPE.identity().items()), 'CLEANUP_PROCESS_SCOPE_CHANGED')
                # Ölüm başarılı temizliktir; PID değişimi başka süreci durduramaz.
                current = device().pid()
                if not current:
                    return {'processExitConfirmed': True}
                device().owner(owner)
                device().run('shell', 'am', 'force-stop', PACKAGE)
                require(not device().pid(), 'QA_PROCESS_NOT_STOPPED')
                return {'processExitConfirmed': True}
            clean('qaProcess', stop_owned)
        elif (self.private / 'initial-start.claim.private.json').exists():
            results['qaProcess'] = {'status': 'FAILED', 'code': 'POSITIVE_OWNED_PID_REQUIRED'}
        intent = self.private / 'android/reverse-intent.private.json'
        if intent.exists():
            clean('reverse', lambda: helpers.remove_owned_reverse(device(), prior.private_json(intent)))
        if not (self.state / 'launch.private.json').exists() and (self.private / 'backend-start.claim.private.json').exists():
            clean('backend', lambda: self.cleanup_reserved_backend(backend_pid))
        elif (self.state / 'local-ready.json').exists():
            clean('backend', self.close_ready_backend)
        elif (self.state / 'launch.private.json').exists():
            clean('backend', self.cleanup_before_ready)
        prior.write_json(self.private / 'cleanup.private.json', {**SCOPE.identity(), 'results': results,
                'ok': bool(results) and all(r['status'] == 'PASS' for r in results.values())})
        if not for_success and not (self.private / 'terminal.private.json').exists():
            prior.write_json(self.private / 'terminal.private.json', {**SCOPE.identity(), 'status': 'FAILED_NO_RETRY'})
        return results


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scope')
    parser.add_argument('--proposal-sha256')
    parser.add_argument('--stage', choices=['build', 'arm', 'reserve-backend', 'record-backend', 'login', 'open', 'run', 'cleanup'])
    parser.add_argument('--adb', type=Path)
    parser.add_argument('--aapt', type=Path)
    parser.add_argument('--pid', type=int)
    parser.add_argument('--admin-credential', type=Path)
    parser.add_argument('--web-api-config', type=Path)
    args = parser.parse_args(argv)
    if args.scope != SCOPE.measurement_id or args.proposal_sha256 != SCOPE.proposal_sha256:
        print('REFUSED_EXPLICIT_MANUAL_SCOPE_REQUIRED'); return 2
    if args.stage is None:
        print('DRY_RUN_ZERO_IO_MANUAL_QA'); return 0
    controller = ManualController()
    android = None
    try:
        controller.failure_writable = any((controller.private / (name + '.claim.private.json')).is_file() for name in ['install', 'initial-start', 'backend-start', 'login', 'window', 'measurement'])
        if any((controller.private / name).exists() for name in ['terminal.private.json', 'cleanup.claim.private.json', 'cleanup.private.json']):
            controller.failure_writable = False
        if controller.failure_writable:
            android = controller.bound_android()
        controller.ensure_open()
        if args.stage == 'build':
            print(controller.build(args)); return 0
        if args.adb is not None:
            if android is not None:
                require(Path(android.args[0]) == args.adb, 'ADB_CLAIM_ARGUMENT_CHANGED')
            else:
                android = ManualAndroid(args.adb, 'emulator-5554')
        require(android is not None, 'ADB_REQUIRED')
        if args.stage == 'record-backend':
            controller.gate(); controller.record_backend_launch(args.pid); return 0
        if args.stage == 'run':
            controller.run(android)
            results = controller.cleanup(android, for_success=True)
            require(bool(results) and all(r['status'] == 'PASS' for r in results.values())
                    and results.get('backend', {}).get('result', {}).get('measurementSucceeded') is True, 'CLEANUP_OR_BACKEND_FAILED')
            prior.write_json(controller.private / 'terminal.private.json', {**SCOPE.identity(),
                'status': 'FOUR_UI_PAINTS_VERIFIED_EMULATOR_ONLY', 'acceptedSha256': prior.digest(controller.private / 'observations/accepted.private.json'),
                'cleanupSha256': prior.digest(controller.private / 'cleanup.private.json'), 'physicalQualityAccepted': False})
        else:
            {'arm': controller.arm, 'reserve-backend': controller.reserve_backend,
             'login': lambda a: controller.login(args, a), 'open': controller.open_window,
             'cleanup': lambda a: (controller.gate(), controller.cleanup(a))}[args.stage](android)
        print('MANUAL_QA_STAGE_COMPLETED_PRIVATE_EVIDENCE'); return 0
    except Exception as error:
        if controller.failure_writable:
            if not (controller.private / 'cleanup.claim.private.json').exists():
                try: controller.cleanup(android, backend_pid=args.pid if args.stage == 'record-backend' else None)
                except Exception: pass
            prior.numbered(controller.private / 'receipts', 'terminal-failure', {**SCOPE.identity(), 'stage': args.stage,
                    'code': helpers.safe_code(error), 'status': 'FAILED_NO_RETRY'})
            if not (controller.private / 'terminal.private.json').exists():
                prior.write_json(controller.private / 'terminal.private.json', {**SCOPE.identity(), 'status': 'FAILED_NO_RETRY', 'stage': args.stage})
        print('REFUSED_MANUAL_QA_NO_RETRY_PRIVATE_EVIDENCE'); return 2


if __name__ == '__main__':
    sys.exit(main())
