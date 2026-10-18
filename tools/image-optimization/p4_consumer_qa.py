#!/usr/bin/env python3
"""P4a5: tek onaylı retained LIVE denemesi; varsayılan giriş yan etkisizdir."""
import argparse
import base64
import json
import math
import os
from pathlib import Path
import re
import shlex
import socket
import subprocess
import sys
import time
from urllib.parse import urlsplit

import p4_reading_comparison as prior

ROOT = Path(__file__).resolve().parents[2]
PRIVATE = Path('/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb')
PLAN = ROOT / 'be/docs/evidence/p4-consumer-qa/bounded-run-proposal-2026-10-18.json'
PROPOSAL_SHA = '079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6'
TERMINAL_SHA = '96dee3fb9e251a7438103f851dec78a09372d858e284e4967440a60de61c5fd1'
READY_SHA = '5cd354c3210b3a6af017bc807287c3d6f80e6ed81bcae19946da1730dd1d44b7'
RUN = prior.RUN_ID
UID = prior.QA_UID
PACKAGE = 'com.tellpal.tellpal_mobile_poc.readingconsumerqa'
ACTIVITY = prior.ACTIVITY
ENTRY = 'test_driver/reading_consumer_qa_entry.dart'
PROBE = 'test_driver/reading_consumer_qa_probe.dart'
ORDER = [1, 2, 3, 1]
WAITING_FAILURE_SHA = 'd5c7ceaa339b958535ed869c745f9f4c58001e31f88ccfabf08ae60af9ce8454'
WAITING_APK_SHA = '102521e8c77a30784e3f2af185f5e810d539edfb69f76eea8aa4a541a9685166'
RECOVERY_HASHES = {
    'backend/failure.json': '87ba6bbfd5751234b9f07b8255f5fe0ddd7a34b971244638c0a62a11c0fcf952',
    'backend/ledger.log': 'b3fd94a0c26845e73ac46a9849ff5760a3d81c3ad9a324d5cbc3f3c54162ebbb',
    'receipts/local-backend-failure-cleanup-01.private.json': '8032f024e22439d7f1c6663d0e1644476b6eccc22069c257b68c1c5bc72951da',
    'receipts/local-db-role-readonly-01.private.json': '1a4069601265be6cb82bb4188a3d8bffb69bdea5d2c1f955bf55acfd579b2289',
    'receipts/root-local-backend-refusal-handoff-01.private.json': '9f65f6905ebfabee1abf526395d12abf9141a4e94225650a61def22e5107b7d5',
    'android/local-waiting-reservation-01.private.json': 'ed86d98fe5c4dfa386d851e8a1e182d6fadfc6e1d576b902a1988198c78537c8',
    'android/connection.private.json': '3ecd2bc551933c72b52884f0e77175c7ac8b622216ac77525b2240808f0cc551',
    'recovery-protected-before.private.json': 'bae525c0fbcc08a1688bf9a41eca4fcd5fa098be2dcdb1a15cd040b74714cdc7',
    'receipts/local-runtime-recovery-human-approval-01.private.json': '1f052a5805b63eb85e26b866030a3398e449bd01d7bb9a200bee2f90833cbd4e',
}
RECOVERY_APPROVAL = 'android/local-waiting-approval-02.private.json'
RECOVERY_QA_RESERVATION = 'android/local-waiting-reservation-02.private.json'
require, digest, private_file, private_dir, write_new, write_json, command = (
    prior.require, prior.digest, prior.private_file, prior.private_dir, prior.write_new, prior.write_json, prior.command)


def plan():
    require(digest(PLAN) == PROPOSAL_SHA, 'PROPOSAL_CHANGED')
    proposal = json.loads(PLAN.read_bytes())
    terminal = ROOT / proposal['priorEvidence']['terminalLedger']
    require(len(private_file(terminal, 65536)) == 11068 and digest(terminal) == TERMINAL_SHA, 'TERMINAL_CHANGED')
    ready = terminal.with_name('ready.json')
    require(digest(ready) == READY_SHA, 'READY_CHANGED')
    return proposal, json.loads(private_file(ready))


def approval(directory):
    value = json.loads(private_file(directory / 'approval.json', 4096))
    require(value == {'proposalSha256': PROPOSAL_SHA, 'decision': 'A', 'runId': RUN, 'qaUid': UID,
        'contentId': 94, 'windowMillis': 180000, 'newProductionAllowed': False}, 'APPROVAL_REQUIRED')
    require(type(value['contentId']) is int and type(value['windowMillis']) is int, 'APPROVAL_INTEGER_REQUIRED')


def state_root(args):
    selected = private_dir(args.private_dir, external=True)
    require(selected == PRIVATE, 'APPROVED_PRIVATE_ROOT_REQUIRED')
    return selected


def recovery_expected():
    return {'proposalSha256': PROPOSAL_SHA, 'decision': 'A', 'runId': RUN, 'qaUid': UID,
        'evidenceHashes': RECOVERY_HASHES, 'buildName': 'build-0002', 'apkSha256': WAITING_APK_SHA,
        'localDbUsername': 'gurhankucuk', 'additionalQaStarts': 1, 'qaStartsTotalCeiling': 3,
        'additionalBackendStarts': 1, 'backendStartsTotalCeiling': 2, 'measurementCeilingsChanged': False}


def exact_json(value, expected):
    return json.dumps(value, sort_keys=True) == json.dumps(expected, sort_keys=True)


def recovery_gate(private, before_start=True):
    """Yalnız kapanmış sıfır tüketimli ilk hatanın insan onaylı kurtarmasını kabul eder."""
    for name, expected in RECOVERY_HASHES.items():
        require(digest_file(private / name) == expected, 'RECOVERY_EVIDENCE_CHANGED')
    sealed = json.loads(private_file(private / 'recovery-protected-before.private.json'))
    for name, expected in sealed['files'].items():
        relative = Path(name)
        require(not relative.is_absolute() and '..' not in relative.parts, 'RECOVERY_SEAL_PATH_REFUSED')
        require(digest_file(private / relative) == expected, 'RECOVERY_PROTECTED_FILE_CHANGED')
    ledger = private_file(private / 'backend/ledger.log', 65536).decode().splitlines()
    require(len(ledger) == 3 and ledger[0] == 'OPEN_PENDING ' + PROPOSAL_SHA + ' ' + TERMINAL_SHA + ' 11068'
        and ledger[1] == 'PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086'
        and ledger[2] == 'CLOSED 0 0 0 0 0 0', 'RECOVERY_PRIOR_SPEND_OR_WINDOW')
    if before_start:
        require(not any((private / name).exists() for name in ['backend-02/failure.json', 'backend-02/backend-closed.json',
            'backend-02/close.request', 'android/arm-failure-03.private.json']), 'RECOVERY_ALREADY_FAILED_OR_CLOSED')
        require(not any((private / folder / name).exists() for folder in ['backend', 'backend-02']
            for name in ['login.reservation.private.json', 'qa-sign-in.ready.json', 'open.request', 'window.json', 'mobile.attempt.json']),
            'RECOVERY_AFTER_AUTH_FORBIDDEN')
    value = json.loads(private_file(private / RECOVERY_APPROVAL, 8192))
    require(exact_json(value, recovery_expected()), 'LOCAL_RUNTIME_RECOVERY_APPROVAL_REQUIRED')
    return digest(private / RECOVERY_APPROVAL)


def digest_file(path):
    require(path.resolve() == path.absolute() and path.is_file() and not path.is_symlink(), 'RECOVERY_FILE_REQUIRED')
    return digest(path)


def recovery_reservation(approval_sha, kind):
    return {'proposalSha256': PROPOSAL_SHA, 'runId': RUN, 'recoveryApprovalSha256': approval_sha,
        'kind': kind, 'state': 'RESERVED_NO_AUTOMATIC_RETRY', 'maxStarts': 1,
        'backendDirectory': 'backend-02', 'connectionFile': 'android/connection-03.private.json'}


def reserve_backend(args):
    private = state_root(args)
    approval_sha = recovery_gate(private)
    state = private_dir(private / 'backend-02')
    approval(state)
    inputs = json.loads(private_file(state / 'inputs.private.json', 65536))
    require(inputs.get('localDbUsername') == 'gurhankucuk', 'EXPLICIT_LOCAL_DB_USER_REQUIRED')
    require(not any((state / name).exists() for name in ['startup.claim.private.json', 'ledger.log',
        'failure.json', 'local-ready.json', 'backend-closed.json', 'close.request']), 'BACKEND_RECOVERY_ALREADY_USED')
    write_json(state / 'startup.reservation.private.json', recovery_reservation(approval_sha, 'BACKEND_START'))
    print('BACKEND_START_RESERVED_BEFORE_JVM_NO_DB_OR_SDK_IO')


def active_paths(private):
    if not recovery_selected(private):
        return private / 'backend', private / 'android/connection.private.json'
    # Başlamış ölçümde ön kapıyı yeniden açmadan aynı değişmez onay/rezervasyonlar doğrulanır.
    approval_sha = recovery_gate(private, before_start=False)
    state = private_dir(private / 'backend-02')
    for path, kind in [(private / RECOVERY_QA_RESERVATION, 'QA_START'),
            (state / 'startup.reservation.private.json', 'BACKEND_START')]:
        require(exact_json(json.loads(private_file(path, 8192)), recovery_reservation(approval_sha, kind)),
            'RECOVERY_RESERVATION_CHANGED')
    claim = json.loads(private_file(state / 'startup.claim.private.json', 8192))
    require(claim.get('recoveryApprovalSha256') == approval_sha
        and claim.get('startupReservationSha256') == digest(state / 'startup.reservation.private.json')
        and type(claim.get('pid')) is int and claim['pid'] > 1, 'BACKEND_START_NOT_CLAIMED')
    return state, private / 'android/connection-03.private.json'


def recovery_selected(private):
    return any((private / name).exists() for name in ['backend/failure.json', 'recovery-protected-before.private.json',
        RECOVERY_APPROVAL, RECOVERY_QA_RESERVATION, 'backend-02'])


def validate_backend_claim(state, ready):
    if state.name == 'backend-02':
        claim = json.loads(private_file(state / 'startup.claim.private.json', 8192))
        require(ready.get('pid') == claim['pid'] and type(ready.get('pid')) is int, 'BACKEND_CLAIM_PID_CHANGED')


def sources(mobile):
    selected = [mobile / 'pubspec.yaml', mobile / 'pubspec.lock']
    for name in ['lib', 'test_driver', 'android']:
        selected += [p for p in (mobile / name).rglob('*') if p.is_file()
                     and not set(p.relative_to(mobile).parts).intersection({'build', '.gradle'})
                     and p.name not in {'local.properties'}]
    return {str(p.relative_to(mobile)): digest(p) for p in sorted(selected)}


def build(args):
    private = state_root(args)
    mobile = private / 'source/app/mobile'
    require(mobile.is_dir() and mobile.resolve() == mobile, 'PRIVATE_SOURCE_REQUIRED')
    source_snapshot = sources(mobile)
    require(source_snapshot == sources(ROOT / 'app/mobile'), 'SOURCE_COPY_CHANGED')
    entry_sha, probe_sha = digest(mobile / ENTRY), digest(mobile / PROBE)
    require(args.java_home is not None and (args.java_home / 'bin/java').is_file(), 'JAVA_REQUIRED')
    tool_paths = json.loads(private_file(private / 'operational-tools.private.json'))
    require((Path(tool_paths['flutterRoot']) / 'bin/flutter').is_file()
        and Path(tool_paths['androidSdk']).is_dir(), 'EXISTING_SDK_REQUIRED')
    properties = ('flutter.sdk=' + tool_paths['flutterRoot'] + '\nsdk.dir=' + tool_paths['androidSdk'] + '\nflutter.buildMode=debug\nflutter.versionName=1.0.0\nflutter.versionCode=1\n').encode()
    local = mobile / 'android/local.properties'
    if local.exists():
        require(private_file(local) == properties, 'LOCAL_SDK_CONFIG_CHANGED')
    else:
        write_new(local, properties)
    builds = private / 'android/builds'
    builds.mkdir(mode=0o700, exist_ok=True)
    existing = [int(p.name[6:]) for p in builds.iterdir() if re.fullmatch(r'build-[0-9]{4}', p.name)]
    attempt = builds / ('build-%04d' % (max(existing, default=0) + 1))
    attempt.mkdir(mode=0o700)
    init = attempt / 'consumer.init.gradle'
    script = """gradle.beforeProject { project ->
    project.pluginManager.withPlugin('com.android.application') {
        if (project.path == ':app') {
            if (project.findProperty('target') != 'test_driver/reading_consumer_qa_entry.dart' ||
                project.findProperty('imageExperiment') != null ||
                gradle.startParameter.taskNames != [':app:assembleDebug']) {
                throw new GradleException('CONSUMER_QA_TARGET_REFUSED')
            }
            project.extensions.getByName('androidComponents').finalizeDsl { android ->
                android.defaultConfig.applicationId = 'com.tellpal.tellpal_mobile_poc.readingconsumerqa'
            }
        }
    }
}
"""
    write_new(init, script.encode())
    env = os.environ.copy()
    env['JAVA_HOME'] = str(args.java_home)
    with open(attempt / 'build.log', 'xb') as log:
        os.chmod(log.name, 0o600)
        result = subprocess.run(['sh', str(mobile / 'android/gradlew'), '--offline', '--console=plain',
            '-p', str(mobile / 'android'), '-I', str(init), ':app:assembleDebug',
            '-Ptarget=' + ENTRY, '-Ptarget-platform=android-arm64'], env=env,
            stdout=log, stderr=subprocess.STDOUT, timeout=600)
    require(result.returncode == 0, 'BUILD_FAILED_PRIVATE_LOG')
    require(sources(mobile) == source_snapshot and sources(ROOT / 'app/mobile') == source_snapshot, 'SOURCE_CHANGED_DURING_BUILD')
    source = mobile / 'build/app/outputs/apk/debug/app-debug.apk'
    badging = command([args.aapt, 'dump', 'badging', source]).decode()
    require(re.search(r"package: name='" + re.escape(PACKAGE) + "'", badging), 'APK_PACKAGE_REFUSED')
    apk = attempt / 'consumer.apk'
    write_new(apk, source.read_bytes())
    require(sources(mobile) == source_snapshot and sources(ROOT / 'app/mobile') == source_snapshot, 'SOURCE_CHANGED_DURING_BUILD')
    write_json(attempt / 'build.json', {'proposalSha256': PROPOSAL_SHA, 'package': PACKAGE,
        'buildName': attempt.name, 'apkSha256': digest(apk), 'sourceHashes': source_snapshot, 'entrySha256': entry_sha,
        'probeSha256': probe_sha, 'initSha256': digest(init), 'mode': 'debug'})
    print('PRIVATE_OFFLINE_DEBUG_APK_READY_NO_AUTH_NO_MEDIA')


class Android(prior.Android):
    def pid(self, timeout=15):
        result = subprocess.run(self.args + ["shell", "pidof", PACKAGE], capture_output=True, timeout=timeout)
        require(result.returncode in [0, 1], "PID_QUERY_FAILED")
        return result.stdout.decode().strip()

    def process_identity(self, pid, timeout=15):
        require(type(pid) is int and pid > 1, 'QA_PID_REQUIRED')
        deadline = time.monotonic() + timeout
        require(self.run('shell', 'cat', '/proc/' + str(pid) + '/cmdline', timeout=arm_remaining(deadline)).split('\x00')[0] == PACKAGE,
            'QA_PROCESS_COMMAND_CHANGED')
        stat = self.run('shell', 'cat', '/proc/' + str(pid) + '/stat', timeout=arm_remaining(deadline))
        fields = stat.rsplit(') ', 1)[-1].split()
        require(len(fields) > 19 and re.fullmatch(r'[1-9][0-9]*', fields[19]), 'QA_PROCESS_START_REQUIRED')
        return {'pid': pid, 'processStartTicks': fields[19]}

    def memory(self):
        text = self.run('shell', 'dumpsys', 'meminfo', '-s', PACKAGE)
        pss = re.search(r'TOTAL PSS:\s*(\d+)', text)
        rss = re.search(r'TOTAL RSS:\s*(\d+)', text)
        require(pss is not None, 'MEMORY_UNAVAILABLE')
        return {'totalPssKiB': int(pss[1]), 'totalRssKiB': int(rss[1]) if rss else None,
            'kind': 'DEBUG_EMULATOR_SNAPSHOT_NOT_PEAK_PROFILE'}


def selected_build(private, build_name=None):
    builds = private / 'android/builds'
    if build_name is not None:
        require(type(build_name) is str and re.fullmatch(r'build-[0-9]{4}', build_name), 'BUILD_NAME_REFUSED')
        candidates = [builds / build_name]
    else:
        candidates = sorted([p for p in builds.iterdir() if re.fullmatch(r'build-[0-9]{4}', p.name)
            and (p / 'build.json').is_file()], reverse=True)
    current_sources = sources(ROOT / 'app/mobile')
    for attempt in candidates:
        receipt = json.loads(private_file(attempt / 'build.json'))
        if receipt['sourceHashes'] != current_sources:
            continue
        require(receipt['proposalSha256'] == PROPOSAL_SHA and receipt['package'] == PACKAGE
            and receipt['buildName'] == attempt.name and receipt['apkSha256'] == digest(attempt / 'consumer.apk'), 'BUILD_CHANGED')
        return receipt, attempt
    raise ValueError('NO_MATCHING_IMMUTABLE_BUILD')


def safe_code(error):
    value = str(error)
    return value if re.fullmatch(r'[A-Z0-9_]{1,100}', value) else 'CLEANUP_FAILED'


def connection_identity(private):
    recovery = recovery_selected(private)
    path = private / 'android/connection.private.json'
    if recovery:
        approval_sha = recovery_gate(private, before_start=False)
        require(exact_json(json.loads(private_file(private / RECOVERY_QA_RESERVATION, 8192)),
            recovery_reservation(approval_sha, 'QA_START')), 'RECOVERY_RESERVATION_CHANGED')
        path = private / 'android/connection-03.private.json'
    value = json.loads(private_file(path))
    if recovery:
        require(value.get('recoveryApprovalSha256') == digest(private / RECOVERY_APPROVAL)
            and value.get('qaReservationSha256') == digest(private / RECOVERY_QA_RESERVATION)
            and value.get('buildName') == 'build-0002' and value.get('apkSha256') == WAITING_APK_SHA, 'RECOVERY_CONNECTION_CHANGED')
    require(value.get('proposalSha256') == PROPOSAL_SHA and value.get('package') == PACKAGE
        and value.get('serial') == 'emulator-5554' and type(value.get('pid')) is int and value['pid'] > 1
        and type(value.get('hostPort')) is int and 1024 <= value['hostPort'] <= 65535
        and type(value.get('devicePort')) is int and 1024 <= value['devicePort'] <= 65535
        and type(value.get('processStartTicks')) is str and re.fullmatch(r'[1-9][0-9]*', value['processStartTicks'])
        and type(value.get('apkSha256')) is str and re.fullmatch(r'[a-f0-9]{64}', value['apkSha256'])
        and type(value.get('buildName')) is str and re.fullmatch(r'build-[0-9]{4}', value['buildName']), 'CONNECTION_IDENTITY_REFUSED')
    parsed = urlsplit(value.get('websocket', ''))
    require(parsed.scheme == 'ws' and parsed.hostname == '127.0.0.1' and parsed.port == value['hostPort']
        and not parsed.query and not parsed.fragment and not parsed.username and not parsed.password
        and re.fullmatch(r'/[A-Za-z0-9_=-]+/ws', parsed.path), 'CONNECTION_ENDPOINT_REFUSED')
    return value


def remove_owned_forward(android, connection):
    rows = [line.split() for line in android.run('forward', '--list').splitlines() if line.strip()]
    local, remote = 'tcp:' + str(connection['hostPort']), 'tcp:' + str(connection['devicePort'])
    matching = [row for row in rows if len(row) == 3 and row[1] == local]
    if not matching:
        return 'ALREADY_ABSENT'
    require(matching == [['emulator-5554', local, remote]], 'FORWARD_OWNERSHIP_CHANGED')
    android.run('forward', '--remove', local)
    return 'REMOVED_OWNED_MAPPING'


def remove_owned_reverse(android, intent):
    rows = [line.split() for line in android.run('reverse', '--list').splitlines() if line.strip()]
    require(all(len(row) == 3 for row in rows), 'REVERSE_LIST_INVALID')
    local, remote = 'tcp:' + str(intent['devicePort']), 'tcp:' + str(intent['hostPort'])
    matching = [row for row in rows if len(row) == 3 and row[1] == local]
    if not matching:
        return 'ALREADY_ABSENT'
    require(len(matching) == 1 and matching[0][1:] == [local, remote]
        and intent['serial'] == 'emulator-5554' and intent['baselineMappingAbsent'] is True,
        'REVERSE_OWNERSHIP_CHANGED')
    android.run('reverse', '--remove', local)
    return 'REMOVED_OWNED_MAPPING'


def require_owned_qa(android, connection):
    require(android.pid() == str(connection['pid']), 'QA_PROCESS_CHANGED')
    require(android.process_identity(connection['pid']) == {'pid': connection['pid'],
        'processStartTicks': connection['processStartTicks']}, 'QA_PROCESS_REPLACED')


def close_owned_qa(android, connection):
    pid = android.pid()
    if not pid:
        return 'ALREADY_EXITED'
    require(pid == str(connection['pid']), 'QA_OWNERSHIP_CHANGED')
    current = android.process_identity(connection['pid'])
    require(current == {'pid': connection['pid'], 'processStartTicks': connection['processStartTicks']}, 'QA_PROCESS_REPLACED')
    android.run('shell', 'am', 'force-stop', PACKAGE)
    deadline = time.monotonic() + 2
    while time.monotonic() < deadline:
        pid = android.pid(timeout=arm_remaining(deadline, 'QA_STOP_DEADLINE'))
        if not pid:
            return 'STOPPED_OWNED_QA'
        require(pid == str(connection['pid']), 'QA_OWNERSHIP_CHANGED')
        try:
            current = android.process_identity(connection['pid'], timeout=arm_remaining(deadline, 'QA_STOP_DEADLINE'))
        except Exception:
            require(not android.pid(timeout=arm_remaining(deadline, 'QA_STOP_DEADLINE')), 'QA_STOP_IDENTITY_UNCONFIRMED')
            arm_remaining(deadline, 'QA_STOP_DEADLINE')
            return 'STOPPED_OWNED_QA'
        require(current == {'pid': connection['pid'], 'processStartTicks': connection['processStartTicks']}, 'QA_PROCESS_REPLACED')
        remaining = deadline - time.monotonic()
        if remaining > 0:
            time.sleep(min(.1, remaining))
    raise ValueError('OWNED_QA_NOT_TERMINATED')


def available_host_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as reserved:
        reserved.bind(('127.0.0.1', 0))
        port = reserved.getsockname()[1]
    require(type(port) is int and 1024 <= port <= 65535, 'HOST_PORT_REFUSED')
    return port


def arm_remaining(deadline, code='ARM_PREPARATION_DEADLINE'):
    remaining = deadline - time.monotonic()
    require(remaining > 0, code)
    return min(15, remaining)


def installed_apk_path(response):
    value = response.rstrip('\r\n')
    require(re.fullmatch(r'package:/data/app/[A-Za-z0-9_=/+.~-]+/base\.apk', value), 'INSTALLED_QA_APK_PATH_REQUIRED')
    path = value[len('package:'):]
    require(all(part not in ['', '.', '..'] for part in path.split('/')[1:]), 'INSTALLED_QA_APK_PATH_REQUIRED')
    return path


def observe_launch_pid(android, deadline):
    for _ in range(300):
        pid = android.pid(timeout=arm_remaining(deadline, 'ARM_PID_DEADLINE'))
        if pid:
            require(re.fullmatch(r'[1-9][0-9]*', pid), 'QA_PID_UNAVAILABLE')
            owned = android.process_identity(int(pid), timeout=arm_remaining(deadline, 'ARM_PID_DEADLINE'))
            arm_remaining(deadline, 'ARM_PID_DEADLINE')
            return pid, owned
        time.sleep(min(.1, arm_remaining(deadline, 'ARM_PID_DEADLINE')))
    raise ValueError('ARM_PID_DEADLINE')


def cancel_owned_launch(android, private, launch, owned, intent_name):
    require(intent_name in ['arm-launch-intent.private.json', 'arm-launch-intent-02.private.json', 'arm-launch-intent-03.private.json'], 'LAUNCH_INTENT_SCOPE_REQUIRED')
    proof = json.loads(private_file(private / 'android' / intent_name, 8192))
    require(proof == launch and proof.get('proposalSha256') == PROPOSAL_SHA and proof.get('package') == PACKAGE
        and proof.get('serial') == 'emulator-5554' and proof.get('initialNoPid') is True
        and proof.get('installCompleted') is True and proof.get('amStartComponent') == PACKAGE + '/' + ACTIVITY and re.fullmatch(r'[a-f0-9]{64}', proof.get('apkSha256', '')),
        'QUEUED_LAUNCH_OWNERSHIP_REQUIRED')
    deadline = time.monotonic() + 2
    observed, stable_since, late_stop = [], None, False
    pid = android.pid(timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
    if pid:
        require(re.fullmatch(r'[1-9][0-9]*', pid), 'QA_PID_UNAVAILABLE')
        current = android.process_identity(int(pid), timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
        require(owned is None or current == owned, 'QA_PROCESS_REPLACED')
        owned = current
        observed.append(current)
    # Boş pidof queued başlangıcı dışlamaz. Yalnız kurup başlattığımız ayrı paket iptal edilir.
    android.run('shell', 'am', 'force-stop', PACKAGE, timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
    for _ in range(22):
        pid = android.pid(timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
        if pid:
            require(re.fullmatch(r'[1-9][0-9]*', pid), 'QA_PID_UNAVAILABLE')
            current = android.process_identity(int(pid), timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
            require(owned is None or current == owned, 'QA_PROCESS_REPLACED')
            if not late_stop:
                owned = current
                observed.append(current)
                android.run('shell', 'am', 'force-stop', PACKAGE, timeout=arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE'))
                late_stop = True
            stable_since = None
        else:
            now = time.monotonic()
            if stable_since is None:
                stable_since = now
            if now - stable_since >= .2:
                return {'state': 'OWNED_LAUNCH_CANCELLED', 'queuedStartCancelled': True,
                    'observedProcesses': observed, 'absenceConfirmed': True}
        time.sleep(min(.1, arm_remaining(deadline, 'ARM_CLEANUP_DEADLINE')))
    raise ValueError('ARM_CLEANUP_DEADLINE')


def waiting_arm_scope(private):
    if recovery_selected(private):
        approval_sha = recovery_gate(private)
        require(not any((private / 'android' / name).exists() for name in ['local-waiting-reservation-02.private.json',
            'arm-failure-03.private.json', 'arm-launch-intent-03.private.json', 'arm-forward-intent-03.private.json',
            'connection-03.private.json']), 'LOCAL_RUNTIME_RECOVERY_QA_ALREADY_RESERVED')
        return {'failure': 'arm-failure-03.private.json', 'launch': 'arm-launch-intent-03.private.json',
            'forward': 'arm-forward-intent-03.private.json', 'connection': 'connection-03.private.json',
            'manual': True, 'recovery': True, 'approvalSha256': approval_sha}
    normal = {'failure': 'arm-failure.private.json', 'launch': 'arm-launch-intent.private.json',
        'forward': 'arm-forward-intent.private.json', 'manual': False}
    original = private / 'android/arm-failure.private.json'
    if not original.exists():
        return normal
    approval_path = private / 'android/local-waiting-approval-01.private.json'
    require(approval_path.is_file(), 'ARM_PREVIOUS_FAILURE_NO_RETRY')
    require(digest(original) == WAITING_FAILURE_SHA, 'ARM_ORIGINAL_FAILURE_CHANGED')
    value = json.loads(private_file(approval_path, 4096))
    expected = {'proposalSha256': PROPOSAL_SHA, 'originalArmFailureSha256': WAITING_FAILURE_SHA,
        'decision': 'A', 'localWaitingOnly': True, 'maxStarts': 1,
        'buildName': 'build-0002', 'apkSha256': WAITING_APK_SHA}
    require(value == expected and value.get('localWaitingOnly') is True
        and type(value.get('maxStarts')) is int, 'LOCAL_WAITING_APPROVAL_REQUIRED')
    require(not any((private / 'android' / name).exists() for name in ['local-waiting-reservation-01.private.json',
        'arm-failure-02.private.json', 'arm-launch-intent-02.private.json', 'arm-forward-intent-02.private.json']),
        'LOCAL_WAITING_START_ALREADY_RESERVED')
    return {'failure': 'arm-failure-02.private.json', 'launch': 'arm-launch-intent-02.private.json',
        'forward': 'arm-forward-intent-02.private.json', 'manual': True, 'approvalSha256': digest(approval_path)}


def arm(args, android):
    private = state_root(args)
    scope = waiting_arm_scope(private)
    require(not any((private / 'backend' / name).exists() for name in ['login.reservation.private.json', 'qa-sign-in.ready.json', 'window.json']), 'ARM_AFTER_AUTH_FORBIDDEN')
    receipt, build_dir = selected_build(private, 'build-0002') if scope['manual'] else selected_build(private)
    apk = build_dir / 'consumer.apk'
    require(receipt['proposalSha256'] == PROPOSAL_SHA and receipt['package'] == PACKAGE
        and receipt['apkSha256'] == digest(apk) and receipt['sourceHashes'] == sources(ROOT / 'app/mobile'), 'BUILD_CHANGED')
    connection_path = private / 'android' / scope.get('connection', 'connection.private.json')
    require(not connection_path.exists(), 'ALREADY_ARMED')
    if scope.get('recovery'):
        require(receipt['buildName'] == 'build-0002' and receipt['apkSha256'] == WAITING_APK_SHA, 'LOCAL_WAITING_APK_CHANGED')
        write_json(private / RECOVERY_QA_RESERVATION, recovery_reservation(scope['approvalSha256'], 'QA_START'))
    elif scope['manual']:
        require(receipt['buildName'] == 'build-0002' and receipt['apkSha256'] == WAITING_APK_SHA, 'LOCAL_WAITING_APK_CHANGED')
        write_json(private / 'android/local-waiting-reservation-01.private.json', {'proposalSha256': PROPOSAL_SHA,
            'originalArmFailureSha256': WAITING_FAILURE_SHA, 'approvalSha256': scope['approvalSha256'],
            'localWaitingOnly': True, 'maxStarts': 1, 'state': 'RESERVED_NO_AUTOMATIC_RETRY'})
    owned, forward, launch, started = None, None, None, False
    try:
        device = android.preflight()
        require(not android.pid(), 'EXISTING_QA_PROCESS_REFUSED')
        if scope.get('recovery'):
            installed = installed_apk_path(android.run('shell', 'pm', 'path', PACKAGE))
            installed_sha = android.run('shell', 'sha256sum', installed).split()
            require(len(installed_sha) == 2 and installed_sha[0] == WAITING_APK_SHA, 'INSTALLED_QA_APK_CHANGED')
        else:
            android.run('install', '-r', str(apk), timeout=120)
        require(not android.pid() and digest(apk) == receipt['apkSha256'], 'INSTALL_OR_INITIAL_OWNERSHIP_CHANGED')
        launch = {'proposalSha256': PROPOSAL_SHA, 'package': PACKAGE, 'serial': 'emulator-5554',
            'initialNoPid': True, 'installCompleted': True, 'apkSha256': receipt['apkSha256'], 'buildName': receipt['buildName'],
            'amStartComponent': PACKAGE + '/' + ACTIVITY, 'maxVmPreparationSeconds': 30}
        if scope.get('recovery'):
            launch.update(installationPerformed=False, existingInstallVerified=True,
                recoveryApprovalSha256=scope['approvalSha256'], qaReservationSha256=digest(private / RECOVERY_QA_RESERVATION))
        write_json(private / 'android' / scope['launch'], launch)
        deadline = time.monotonic() + 30
        started = True
        android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + ACTIVITY, timeout=arm_remaining(deadline))
        pid, owned = observe_launch_pid(android, deadline)
        uri = None
        while time.monotonic() < deadline:
            logs = android.run('logcat', '-d', '--pid=' + pid, '-s', 'flutter:I', timeout=arm_remaining(deadline))
            found = re.findall(r'Dart VM service is listening on (http://127\.0\.0\.1:\d+/[A-Za-z0-9_=-]+/)', logs)
            if found:
                uri = found[-1]
                break
            time.sleep(min(.25, arm_remaining(deadline)))
        require(uri is not None, 'WAITING_VM_UNAVAILABLE')
        arm_remaining(deadline)
        parsed = urlsplit(uri)
        require(parsed.port is not None and 1024 <= parsed.port <= 65535, 'VM_PORT_REFUSED')
        host_port = available_host_port()
        local = 'tcp:' + str(host_port)
        rows = [line.split() for line in android.run('forward', '--list').splitlines() if line.strip()]
        require(not any(len(row) == 3 and row[1] == local for row in rows), 'HOST_FORWARD_ALREADY_PRESENT')
        require(android.pid() == pid and android.process_identity(int(pid)) == owned, 'QA_PROCESS_REPLACED')
        candidate = {'hostPort': host_port, 'devicePort': parsed.port}
        write_json(private / 'android' / scope['forward'], {**owned, **candidate,
            'proposalSha256': PROPOSAL_SHA, 'serial': 'emulator-5554', 'baselineMappingAbsent': True})
        # Cevap kaybolsa da olası yan etki yalnız önceden ayrılan bu eşlemeye aittir.
        forward = candidate
        android.run('forward', '--no-rebind', local, 'tcp:' + str(parsed.port))
        port = str(host_port)
        recovery = {key: launch[key] for key in ['recoveryApprovalSha256', 'qaReservationSha256']} if scope.get('recovery') else {}
        write_json(connection_path, {**owned, **forward, **recovery,
            'proposalSha256': PROPOSAL_SHA, 'package': PACKAGE, 'serial': 'emulator-5554',
            'websocket': 'ws://127.0.0.1:' + port + parsed.path + 'ws', 'device': device,
            'apkSha256': receipt['apkSha256'], 'buildName': receipt['buildName']})
        print('CONSUMER_WAITING_NO_AUTH_NO_MEDIA')
    except Exception as failure:
        cleanup = {}
        def clean(name, action):
            try:
                cleanup[name] = {'status': 'PASS', 'result': action()}
            except Exception as error:
                cleanup[name] = {'status': 'FAILED', 'code': safe_code(error)}
        if forward is not None:
            clean('forward', lambda: remove_owned_forward(android, forward))
        if started:
            clean('qaProcess', lambda: cancel_owned_launch(android, private, launch, owned, scope['launch']))
        write_json(private / 'android' / scope['failure'], {'proposalSha256': PROPOSAL_SHA,
            'status': 'FAILED', 'code': safe_code(failure), 'cleanup': cleanup})
        raise


class Vm(prior.Vm):
    def tap(self, text, tooltip=False):
        result = self.rpc('ext.flutter.driver', {'isolateId': self.isolate, 'command': 'tap',
            'finderType': 'ByTooltipMessage' if tooltip else 'ByText', 'text': text, 'timeout': '5000'})
        require(not result.get('isError'), 'UI_TAP_REFUSED')


def login_device_gate(private, args):
    require(args.adb is not None, 'ARMED_DEVICE_REQUIRED')
    connection = connection_identity(private)
    build, build_dir = selected_build(private, connection['buildName'])
    require(build['proposalSha256'] == PROPOSAL_SHA and build['package'] == PACKAGE
        and connection['apkSha256'] == build['apkSha256'] == digest(build_dir / 'consumer.apk')
        and build['sourceHashes'] == sources(ROOT / 'app/mobile'), 'BUILD_OR_DEVICE_CHANGED')
    android = Android(args.adb, 'emulator-5554')
    android.preflight()
    require(android.pid() == str(connection['pid']), 'QA_PROCESS_CHANGED')
    require(android.process_identity(connection['pid']) == {'pid': connection['pid'], 'processStartTicks': connection['processStartTicks']}, 'QA_PROCESS_REPLACED')


def backend_process_command(state, pid):
    require(type(pid) is int and pid > 1, 'BACKEND_PID_REQUIRED')
    result = subprocess.run(['ps', '-p', str(pid), '-o', 'command='], capture_output=True, timeout=3)
    if result.returncode == 1:
        return None
    require(result.returncode == 0, 'BACKEND_PROCESS_INSPECTION_FAILED')
    words = shlex.split(result.stdout.decode().strip())
    require('com.tellpal.v2.support.P4ConsumerQaSetup' in words
        and len(words) >= 3 and words[-2:] == ['--execute', str(state)], 'BACKEND_OWNERSHIP_CHANGED')
    return words


def login_backend_gate(state, ready):
    require(not any((state / name).exists() for name in ['failure.json', 'backend-closed.json', 'close.request', 'window.json', 'open.request']), 'LOGIN_GATE_CLOSED')
    started, deadline = ready.get('localGateStartedAtEpochMillis'), ready.get('localGateDeadlineEpochMillis')
    now = time.time() * 1000
    require(type(started) is int and type(deadline) is int and deadline - started == 300000
        and started <= now and deadline - now > 12000, 'LOGIN_GATE_STALE_OR_TOO_SHORT')
    require(backend_process_command(state, ready.get('pid')) is not None, 'LOGIN_BACKEND_NOT_RUNNING')
    require(not any((state / name).exists() for name in ['failure.json', 'backend-closed.json', 'close.request', 'window.json', 'open.request'])
        and time.time() * 1000 >= now and deadline - time.time() * 1000 > 12000, 'LOGIN_GATE_CLOSED')


def login(args):
    private = state_root(args)
    state, _ = active_paths(private)
    approval(state)
    login_device_gate(private, args)
    ready = json.loads(private_file(state / 'local-ready.json'))
    proposal, expected = plan()
    validate_ready(ready, expected)
    validate_backend_claim(state, ready)
    login_backend_gate(state, ready)
    # Tekil rezervasyon, worker veya credential okumasından önce diske zorlanır.
    reservation = {'proposalSha256': PROPOSAL_SHA, 'priorLedgerSha256': TERMINAL_SHA,
        'qaSignInInvocationsTotal': 3, 'firebaseVerifierReservationsTotal': 21,
        'accountLookupReserved': 1, 'tokenExchangeReserved': 1, 'loginVerifierReserved': 1}
    write_json(state / 'login.reservation.private.json', reservation)
    import native_test_login as native
    inputs = native.load_inputs(args.admin_credential, args.web_api_config, state / 'id-token.private')
    require(inputs.uid == UID and inputs.project == 'tellpal-ee0dd', 'LOGIN_IDENTITY_CHANGED')
    token = native.login_in_worker(inputs)
    native.write_token(inputs, token)
    claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
    write_json(state / 'qa-sign-in.ready.json', {'proposalSha256': PROPOSAL_SHA, 'runId': RUN, 'qaUid': UID,
        'expiresAtEpochMillis': claims['exp'] * 1000, 'tokenFile': str(inputs.output),
        'qaSignInInvocationsTotal': 3, 'firebaseVerifierReservationsTotal': 21})
    token = None
    print('SINGLE_RESERVED_LOGIN_COMPLETED_PRIVATE_TOKEN')


def validate_ready(ready, expected):
    require(ready.get('proposalSha256') == PROPOSAL_SHA and ready.get('runId') == RUN
        and type(ready.get('contentId')) is int and ready['contentId'] == 94
        and ready.get('identity') == expected['identity'] and ready.get('descriptor') == expected['descriptor'], 'RETAINED_LIVE_CHANGED')
    require(type(ready.get('loopbackPort')) is int and 1024 <= ready['loopbackPort'] <= 65535, 'LOOPBACK_PORT_REQUIRED')


def runtime_configuration(ready, token):
    require(len(token) <= 8192 and re.fullmatch(r'[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', token), 'TOKEN_FORMAT')
    claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
    require(claims['aud'] == 'tellpal-ee0dd' and claims['iss'] == 'https://securetoken.google.com/tellpal-ee0dd'
        and claims['sub'] == UID and type(claims['exp']) is int and claims['exp'] > time.time() + 180
        and type(claims['iat']) is int and claims['iat'] <= time.time() and claims.get('user_id', UID) == UID, 'TOKEN_SCOPE_OR_EXPIRY')
    start, end = ready['windowStartedAtEpochMillis'], ready['windowDeadlineEpochMillis']
    require(type(start) is int and type(end) is int and end - start == 180000 and start <= time.time() * 1000 < end, 'WINDOW_EXPIRED')
    return {'action': 'configure', 'runId': RUN, 'proposalSha256': PROPOSAL_SHA, 'uid': UID, 'contentId': 94,
        'port': ready['loopbackPort'], 'token': token, 'windowStartedAtEpochMillis': start, 'windowDeadlineEpochMillis': end}


def fit_dimensions(sw, sh, tw, th, fit):
    require(fit in ['cover', 'contain'] and min(sw, sh, tw, th) > 0, 'GEOMETRY_INVALID')
    width = tw * sh <= th * sw if fit == 'contain' else tw * sh >= th * sw
    numerator, denominator = (tw, sw) if width else (th, sh)
    return ((sw * numerator + denominator - 1) // denominator, (sh * numerator + denominator - 1) // denominator)


def validate_loaded(snapshot, ready, order):
    index = ORDER[order]
    require(snapshot['status'] == 'loaded' and snapshot['activeImages'] == snapshot['cubitCount'] == 1, 'OWNERSHIP_OR_STATE')
    loaded = [e for e in snapshot['events'] if e['kind'] == 'state' and e['status'] == 'loaded']
    require(len(loaded) == order + 1, 'EXTRA_OR_MISSING_LOADED_STATE')
    state = loaded[-1]
    require(type(state.get('epoch')) is int and snapshot.get('paintedEpoch') == state['epoch'], 'PAINT_REQUIRED')
    require(state['entryIndex'] == index and state['canonicalSource'] is False, 'ENTRY_OR_FALLBACK')
    for name in ['contentId', 'language', 'liveGeneration', 'revision', 'manifestDigest', 'jobId']:
        require(state[name] == ready['identity'][name], 'LIVE_CHANGED')
    entry = ready['descriptor']['images'][index]
    source, candidate = entry['source'], entry['renditions'][0]
    binding, raster = source['binding'], candidate['raster']
    require(state['role'] == ('readingDetail' if index == 1 else 'readingPage')
        and state['pageId'] == binding['pageId'] and state['pageNumber'] == binding['pageNumber'], 'ROLE_PAGE_CHANGED')
    for name in ['profileFingerprint', 'renditionKey']:
        require(state[name] == candidate[name], 'RENDITION_CHANGED')
    require(state['sourceSha256'] == source['raster']['sha256'] and state['displaySha256'] == raster['sha256']
        and state['expectedPayloadBytes'] == raster['byteSize'], 'BYTE_IDENTITY_CHANGED')
    slot = snapshot['slot']
    require(slot and slot['dpr'] == 2 and abs(slot['logicalWidth'] - 411) < .01
        and slot['logicalHeight'] > 0 and slot['fit'] == ('cover' if index == 1 else 'contain'), 'SLOT_CHANGED')
    physical = (math.ceil(slot['logicalWidth'] * slot['dpr']), math.ceil(slot['logicalHeight'] * slot['dpr']))
    required = fit_dimensions(source['raster']['width'], source['raster']['height'], *physical, slot['fit'])
    require((state['physicalWidth'], state['physicalHeight']) == physical
        and (state['requiredWidth'], state['requiredHeight']) == required, 'GEOMETRY_ORACLE_MISMATCH')
    require(raster['width'] >= required[0] and raster['height'] >= required[1], 'RENDITION_SHORTAGE')
    decode = fit_dimensions(raster['width'], raster['height'], *required, 'cover')
    require((state['decodedWidth'], state['decodedHeight']) == decode
        and (state['decodeWidth'], state['decodeHeight']) == decode, 'DECODE_MISMATCH')
    http = [e for e in snapshot['events'] if e['kind'] == 'http']
    media = [e for e in http if e['endpoint'] == 'bytes']
    require(all(e['ok'] for e in http) and len(media) == order + 1 and media[-1]['payloadBytes'] == raster['byteSize'], 'PAYLOAD_MISMATCH')
    require(len([e for e in http if e['endpoint'] == 'manifest']) == 1
        and len([e for e in http if e['endpoint'] == 'status']) <= 18 and len(http) <= 23, 'HTTP_LIMIT')
    require(all(state[name] == 0 for name in ['activeBuffers', 'activeDescriptors', 'activeCodecs']), 'DECODE_RESOURCES_RETAINED')
    return {'render': state, 'slot': slot, 'paintedEpoch': snapshot['paintedEpoch'], 'httpPayload': media[-1]}


def backend_closed_receipt(state, pid):
    require(not (state / 'failure.json').exists(), 'BACKEND_FAILED_WITHOUT_DRAIN')
    require((state / 'backend-closed.json').is_file(), 'BACKEND_DRAIN_RECEIPT_MISSING')
    receipt = json.loads(private_file(state / 'backend-closed.json', 4096))
    require(receipt.get('proposalSha256') == PROPOSAL_SHA and receipt.get('runId') == RUN
        and type(receipt.get('pid')) is int and receipt['pid'] == pid and receipt.get('closed') is True,
        'BACKEND_CLOSED_RECEIPT_MISMATCH')
    return 'DRAINED_MATCHED_RECEIPT_AND_PROCESS_EXIT'


def close_backend(state):
    ready = json.loads(private_file(state / 'local-ready.json'))
    require(ready.get('proposalSha256') == PROPOSAL_SHA and ready.get('runId') == RUN, 'BACKEND_SCOPE_CHANGED')
    pid = ready.get('pid')
    import signal
    if backend_process_command(state, pid) is None:
        return backend_closed_receipt(state, pid)
    if not (state / 'close.request').exists():
        write_new(state / 'close.request', b'CLOSE\n')
    until = time.monotonic() + 5
    while time.monotonic() < until:
        if backend_process_command(state, pid) is None:
            return backend_closed_receipt(state, pid)
        time.sleep(.1)
    # Sonlu watchdog yalnız sahip olduğumuz süreci kapatır; native drain başarısı üretmez.
    for sig in [signal.SIGTERM, signal.SIGKILL]:
        if backend_process_command(state, pid) is None:
            raise ValueError('BACKEND_WATCHDOG_OR_MISSING_DRAIN')
        try:
            os.kill(pid, sig)
        except ProcessLookupError:
            raise ValueError('BACKEND_WATCHDOG_OR_MISSING_DRAIN') from None
        until = time.monotonic() + 2
        while time.monotonic() < until:
            if backend_process_command(state, pid) is None:
                raise ValueError('BACKEND_WATCHDOG_OR_MISSING_DRAIN')
            time.sleep(.1)
    raise ValueError('OWNED_BACKEND_NOT_TERMINATED')


def run(args, android):
    private = state_root(args)
    state, _ = active_paths(private)
    output = private / 'observations'
    output.mkdir(mode=0o700)
    vm, reverse, verified = None, None, False
    connection, ready, evidence = None, None, None
    configuration, token = {}, None
    observations, cleanup = [], {}
    def clean(name, work):
        try:
            result = work()
            cleanup[name] = {'status': 'PASS', 'result': result}
        except Exception as error:
            cleanup[name] = {'status': 'FAILED', 'code': safe_code(error)}
    try:
        connection = connection_identity(private)
        approval(state)
        proposal, expected = plan()
        candidate = connection
        receipt, build_dir = selected_build(private, candidate['buildName'])
        require(receipt['sourceHashes'] == sources(ROOT / 'app/mobile')
            and candidate['apkSha256'] == receipt['apkSha256']
            and type(candidate['pid']) is int and candidate['pid'] > 1
            and type(candidate['hostPort']) is int and 1024 <= candidate['hostPort'] <= 65535,
            'SOURCE_OR_APK_CHANGED')
        connection = candidate
        ready = json.loads(private_file(state / 'ready.json'))
        validate_ready(ready, expected)
        validate_backend_claim(state, ready)
        auth = json.loads(private_file(state / 'qa-sign-in.ready.json'))
        require(auth['runId'] == RUN and auth['qaUid'] == UID, 'AUTH_SCOPE')
        token = private_file(Path(auth['tokenFile']), 8192, external=True).decode().strip()
        configuration = runtime_configuration(ready, token)
        write_json(state / 'mobile.attempt.json', {'proposalSha256': PROPOSAL_SHA, 'status': 'STARTED_NO_RETRY'})
        wall_end = ready['windowDeadlineEpochMillis']
        mono_end = time.monotonic() + (wall_end - time.time() * 1000) / 1000
        last_wall = time.time() * 1000
        def check():
            nonlocal last_wall
            now = time.time() * 1000
            require(last_wall <= now < wall_end and time.monotonic() < mono_end, 'WINDOW_EXPIRED_OR_CLOCK_ROLLBACK')
            last_wall = now
        check()
        android.preflight()
        check()
        require_owned_qa(android, connection)
        check()
        port = ready['loopbackPort']
        rows = [line.split() for line in android.run('reverse', '--list').splitlines() if line.strip()]
        require(all(len(row) == 3 for row in rows), 'REVERSE_LIST_INVALID')
        require(not any(row[1] == 'tcp:' + str(port) for row in rows), 'REVERSE_ALREADY_PRESENT')
        intent = {'proposalSha256': PROPOSAL_SHA, 'serial': 'emulator-5554',
            'devicePort': port, 'hostPort': port, 'baselineMappingAbsent': True,
            'pid': connection['pid'], 'processStartTicks': connection['processStartTicks']}
        write_json(state / 'reverse-intent.private.json', intent)
        reverse = intent
        android.run('reverse', '--no-rebind', 'tcp:' + str(port), 'tcp:' + str(port))
        check()
        vm = Vm(connection['websocket'])
        check()
        vm.ready()
        check()
        baseline = android.memory()
        check()
        vm.data(configuration)
        configuration.clear()
        token = None
        check()
        transitions = [('QA sayfalarını aç', False), ('Sonraki', False), ('QA detail ekranına dön', True)]
        for order in range(4):
            check()
            if order:
                text, tooltip = transitions[order - 1]
                vm.tap(text, tooltip)
                check()
            snapshot = None
            for _ in range(240):
                check()
                snapshot = vm.data({'action': 'snapshot'})
                check()
                require(snapshot['status'] != 'error', 'MEDIA_REFUSED_NO_RETRY')
                loaded = [e for e in snapshot['events'] if e['kind'] == 'state' and e['status'] == 'loaded']
                if snapshot['status'] == 'loaded' and len(loaded) == order + 1:
                    break
                time.sleep(.15)
            require(loaded and len(loaded) == order + 1, 'PAINT_NOT_READY')
            check()
            snapshot = vm.data({'action': 'paint', 'epoch': loaded[-1]['epoch']})
            check()
            observation = validate_loaded(snapshot, ready, order)
            check()
            observation['memory'] = android.memory()
            check()
            observation['screenshotSha256'] = android.screenshot(output / f'{order + 1:02d}.png')
            check()
            observations.append(observation)
            write_json(output / f'{order + 1:02d}.json', observation)
            check()
        before = vm.data({'action': 'snapshot'})
        check()
        require_owned_qa(android, connection)
        check()
        android.run('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        check()
        time.sleep(.4)
        check()
        background = vm.data({'action': 'snapshot'})
        check()
        require_owned_qa(android, connection)
        check()
        android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + ACTIVITY)
        check()
        require_owned_qa(android, connection)
        check()
        write_json(state / 'foreground-return.private.json', {'proposalSha256': PROPOSAL_SHA,
            'pid': connection['pid'], 'processStartTicks': connection['processStartTicks'],
            'verifiedBeforeHome': True, 'verifiedBeforeReturn': True, 'verifiedAfterReturn': True,
            'atomicProcessRaceExcluded': False})
        time.sleep(.4)
        check()
        returned = vm.data({'action': 'snapshot'})
        check()
        def requests(s):
            return [e for e in s['events'] if e['kind'] in ['http', 'httpReservation']]
        require(background['activeImages'] == returned['activeImages'] == 0
            and requests(before) == requests(background) == requests(returned), 'BACKGROUND_EXTRA_IO_OR_IMAGE')
        vm.tap('QA akışını kapat', True)
        check()
        for _ in range(50):
            check()
            closed_ui = vm.data({'action': 'snapshot'})
            check()
            if closed_ui['cubitClosed']:
                break
            time.sleep(.1)
        require(closed_ui['cubitClosed'] and all(closed_ui[n] == 0 for n in ['activeBuffers', 'activeDescriptors', 'activeCodecs', 'activeImages']), 'UI_CLOSE_NOT_DRAINED')
        final = vm.data({'action': 'stop'})
        check()
        require(not final['credentialActive'] and not final['deadlineActive'], 'OWNERSHIP_NOT_CLOSED')
        android.screenshot(output / '05-closed.png')
        check()
        closed_memory = android.memory()
        check()
        evidence = {'proposalSha256': PROPOSAL_SHA, 'runId': RUN,
            'status': 'FOUR_UI_PAINTS_VERIFIED_EMULATOR_ONLY', 'baselineMemory': baseline,
            'observations': observations, 'background': background, 'returned': returned,
            'closedUi': closed_ui, 'closed': final, 'closedMemory': closed_memory, 'physicalQualityAccepted': False}
        check()
        verified = True
    finally:
        configuration.clear()
        token = None
        if vm:
            clean('vmStop', lambda: vm.data({'action': 'stop'}))
            clean('vmClose', vm.close)
        if reverse:
            clean('reverse', lambda: remove_owned_reverse(android, reverse))
        if connection:
            clean('forward', lambda: remove_owned_forward(android, connection))
            clean('qaProcess', lambda: close_owned_qa(android, connection))
        clean('backendClose', lambda: close_backend(state))
        finished = verified and all(v['status'] == 'PASS' for v in cleanup.values())
        write_json(output / 'attempt-result.json', {'proposalSha256': PROPOSAL_SHA,
            'statesRecorded': len(observations), 'finished': finished, 'cleanup': cleanup})
        if finished:
            write_json(output / 'verification.json', evidence)
    require(finished, 'CLEANUP_OR_VERIFICATION_FAILED')
    print('FOUR_UI_PAINTS_VERIFIED_EMULATOR_ONLY_PHYSICAL_GATES_OPEN')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    for mode in ['build', 'arm', 'reserve-backend', 'login', 'run']:
        modes.add_argument('--' + mode, action='store_true')
    parser.add_argument('--private-dir', type=Path, default=PRIVATE)
    parser.add_argument('--adb', type=Path)
    parser.add_argument('--aapt', type=Path)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--admin-credential', type=Path)
    parser.add_argument('--web-api-config', type=Path)
    args = parser.parse_args(argv)
    if not any([args.build, args.arm, args.reserve_backend, args.login, args.run]):
        print('DRY_RUN_ZERO_FILE_CREDENTIAL_DEVICE_NETWORK_IO')
        return 0
    try:
        plan()
        if args.build:
            build(args)
        elif args.reserve_backend:
            reserve_backend(args)
        elif args.login:
            login(args)
        else:
            require(args.adb is not None, 'ADB_REQUIRED')
            android = Android(args.adb, 'emulator-5554')
            arm(args, android) if args.arm else run(args, android)
        return 0
    except Exception:
        print('REFUSED_CONSUMER_QA_NO_RETRY_PRIVATE_EVIDENCE')
        return 2


if __name__ == '__main__':
    sys.exit(main())
