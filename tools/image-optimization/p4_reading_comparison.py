#!/usr/bin/env python3
"""Tek onaylı P4a2 denemesi için ayrı Android debug sürücüsü. Login yapmaz."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import socket
import subprocess
import sys
import time
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'be/docs/evidence/p4-reading-images/bounded-run-proposal-2026-10-17.json'
PLAN_SHA = 'f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3'
RUN_ID = 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09'
STATE = ROOT / 'be/var' / RUN_ID
QA_UID = 'jLRypTmoBrhhX21HsDJojurcKTC3'
PACKAGE = 'com.tellpal.tellpal_mobile_poc.readingcomparison.media'
ACTIVITY = 'com.tellpal.tellpal_mobile_poc.MainActivity'
ENTRY = 'test_driver/reading_comparison_entry.dart'
CHOICES = [(1, False), (1, True), (2, True), (2, False), (3, False), (3, True)]
ORIGINAL_OPEN_MILLIS = 1792269638011
ORIGINAL_DEADLINE_MILLIS = ORIGINAL_OPEN_MILLIS + 900000
CONTINUATION_LEDGER_SHA = '60086ea1ad1bdccbcc2423481fcd5a20cd4777602c3e87b1c3636732b298b839'


def require(value, code='COMPARISON_REFUSED'):
    if not value:
        raise ValueError(code)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def private_file(path, maximum=1048576, external=False):
    path = path.absolute()
    info = path.lstat()
    require(path.resolve() == path and stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid()
            and stat.S_IMODE(info.st_mode) == 0o600 and info.st_size <= maximum)
    require(not external or not path.is_relative_to(ROOT))
    return path.read_bytes()


def private_dir(path, external=False):
    path = path.absolute()
    info = path.lstat()
    require(path.resolve() == path and stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid()
            and stat.S_IMODE(info.st_mode) == 0o700)
    require(not external or not path.is_relative_to(ROOT))
    return path


def write_new(path, value):
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600), 'wb') as output:
        output.write(value)
        output.flush()
        os.fsync(output.fileno())


def write_json(path, value):
    write_new(path, (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode())


def plan():
    require(digest(PLAN) == PLAN_SHA, 'PLAN_CHANGED')
    return json.loads(PLAN.read_bytes())


def command(args, timeout=15, env=None):
    result = subprocess.run([str(a) for a in args], capture_output=True, timeout=timeout, env=env)
    require(result.returncode == 0, 'LOCAL_COMMAND_REFUSED')
    return result.stdout


class Android:
    def __init__(self, adb, serial):
        require(serial == 'emulator-5554', 'EXACT_EMULATOR_REQUIRED')
        self.args = [str(adb), '-s', serial]

    def run(self, *args, timeout=15):
        return command(self.args + list(args), timeout).decode().strip()

    def preflight(self):
        require(self.run('get-state') == 'device')
        require(self.run('shell', 'getprop', 'ro.kernel.qemu') == '1')
        require('Override size: 822x1828' in self.run('shell', 'wm', 'size'), 'DISPLAY_CHANGED')
        require('Override density: 320' in self.run('shell', 'wm', 'density'), 'DPR_CHANGED')
        require(float(self.run('shell', 'settings', 'get', 'system', 'font_scale')) == 1, 'FONT_CHANGED')
        return {'serial': 'emulator-5554', 'physicalDevice': False,
                'displayWidth': 822, 'displayHeight': 1828, 'densityDpi': 320, 'fontScale': 1}

    def memory(self):
        text = self.run('shell', 'dumpsys', 'meminfo', '-s', PACKAGE)
        pss = re.search(r'TOTAL PSS:\s*(\d+)', text)
        rss = re.search(r'TOTAL RSS:\s*(\d+)', text)
        require(pss is not None, 'MEMORY_OBSERVATION_UNAVAILABLE')
        return {'totalPssKiB': int(pss[1]), 'totalRssKiB': int(rss[1]) if rss else None,
                'kind': 'DEBUG_EMULATOR_SNAPSHOT_NOT_PEAK_PROFILE'}

    def screenshot(self, path):
        data = command(self.args + ['exec-out', 'screencap', '-p'])
        require(data.startswith(b'\x89PNG\r\n\x1a\n') and len(data) <= 16777216)
        write_new(path, data)
        return hashlib.sha256(data).hexdigest()


def build(args):
    output = private_dir(args.private_dir, external=True)
    require(args.java_home is not None and (args.java_home / 'bin/java').is_file(), 'JAVA_17_REQUIRED')
    # Geçici init script yalnız bu girişin debug paketini ayırır. Repo Gradle dosyası değişmez.
    script = '''gradle.beforeProject { project ->
    project.pluginManager.withPlugin('com.android.application') {
        if (project.path == ':app') {
            if (project.findProperty('target') != 'test_driver/reading_comparison_entry.dart' ||
                project.findProperty('imageExperiment') != null ||
                gradle.startParameter.taskNames != [':app:assembleDebug']) {
                throw new GradleException('P4_COMPARISON_TARGET_REFUSED')
            }
            project.extensions.getByName('androidComponents').finalizeDsl { android ->
                android.defaultConfig.applicationId = 'com.tellpal.tellpal_mobile_poc.readingcomparison.media'
            }
        }
    }
}
'''
    init = output / 'reading-comparison.init.gradle'
    write_new(init, script.encode())
    environment = os.environ.copy()
    environment['JAVA_HOME'] = str(args.java_home)
    result = subprocess.run([str(ROOT / 'app/mobile/android/gradlew'), '--offline', '--console=plain',
        '-p', str(ROOT / 'app/mobile/android'), '-I', str(init), ':app:assembleDebug',
        '-Ptarget=' + ENTRY, '-Ptarget-platform=android-arm64'], env=environment,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=600)
    write_new(output / 'build.log', result.stdout)
    require(result.returncode == 0, 'BUILD_FAILED_PRIVATE_LOG')
    source = ROOT / 'app/mobile/build/app/outputs/apk/debug/app-debug.apk'
    badging = command([args.aapt, 'dump', 'badging', source]).decode()
    require(re.search(r"package: name='" + re.escape(PACKAGE) + "'", badging) is not None, 'APK_PACKAGE_REFUSED')
    apk = output / 'reading-comparison.apk'
    write_new(apk, source.read_bytes())
    receipt = {'planSha256': PLAN_SHA, 'package': PACKAGE, 'apkSha256': digest(apk),
        'entrySha256': digest(ROOT / 'app/mobile' / ENTRY),
        'probeSha256': digest(ROOT / 'app/mobile/test_driver/reading_comparison_probe.dart'),
        'initSha256': digest(init), 'mode': 'debug', 'physicalQualityEvidence': False}
    write_json(output / 'build.json', receipt)
    print('ISOLATED_DEBUG_APK_READY_NO_AUTH_NO_MEDIA')


def arm(args, android):
    output = private_dir(args.private_dir, external=True)
    receipt = json.loads(private_file(output / 'build.json'))
    require(receipt['planSha256'] == PLAN_SHA and receipt['package'] == PACKAGE)
    require(receipt['entrySha256'] == digest(ROOT / 'app/mobile' / ENTRY)
            and receipt['probeSha256'] == digest(ROOT / 'app/mobile/test_driver/reading_comparison_probe.dart'))
    apk = output / 'reading-comparison.apk'
    require(receipt['apkSha256'] == digest(apk), 'APK_CHANGED')
    device = android.preflight()
    android.run('install', '-r', str(apk), timeout=120)
    android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + ACTIVITY)
    pid = android.run('shell', 'pidof', PACKAGE)
    require(re.fullmatch(r'[1-9][0-9]*', pid) is not None)
    uri = None
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        logs = android.run('logcat', '-d', '--pid=' + pid, '-s', 'flutter:I')
        matches = re.findall(r'Dart VM service is listening on (http://127\.0\.0\.1:\d+/[A-Za-z0-9_=-]+/)', logs)
        if matches:
            uri = matches[-1]
            break
        time.sleep(.25)
    require(uri is not None, 'VM_UNAVAILABLE_NO_MEDIA_STARTED')
    parsed = urlsplit(uri)
    host_port = android.run('forward', 'tcp:0', 'tcp:' + str(parsed.port))
    require(host_port.isdigit())
    write_json(output / 'connection.private.json', {'pid': int(pid), 'hostPort': int(host_port),
        'websocket': 'ws://127.0.0.1:' + host_port + parsed.path + 'ws', 'device': device,
        'apkSha256': receipt['apkSha256']})
    print('QA_ARMED_WAITING_PRIVATE_RUNTIME_CONFIG_NO_AUTH_NO_MEDIA')


class Vm:
    def __init__(self, url):
        import websocket
        parsed = urlsplit(url)
        require(parsed.scheme == 'ws' and parsed.hostname == '127.0.0.1' and not parsed.query
            and not parsed.username and not parsed.password and not parsed.fragment
            and parsed.port is not None and 1024 <= parsed.port <= 65535
            and re.fullmatch(r'/[A-Za-z0-9_=-]+/ws', parsed.path))
        direct = socket.create_connection(('127.0.0.1', parsed.port), timeout=10)
        candidate = None
        try:
            candidate = websocket.create_connection(url, socket=direct, timeout=10, suppress_origin=True, redirect_limit=0)
            require(candidate.getstatus() == 101, 'VM_HANDSHAKE_REFUSED')
        except BaseException:
            if candidate is not None:
                try:
                    candidate.close()
                except Exception:
                    pass
            direct.close()
            raise
        self.socket = candidate
        self._transport = direct
        self.sequence = 0
        self.isolate = None

    def rpc(self, method, params=None):
        self.sequence += 1
        self.socket.send(json.dumps({'jsonrpc': '2.0', 'id': self.sequence, 'method': method, 'params': params or {}}))
        deadline = time.monotonic() + 10
        for _ in range(256):
            require(time.monotonic() < deadline, 'VM_DEADLINE')
            self.socket.settimeout(max(.01, deadline - time.monotonic()))
            raw = self.socket.recv()
            require(len(raw) <= 1048576)
            result = json.loads(raw)
            if result.get('id') == self.sequence:
                require('error' not in result, 'VM_REFUSED')
                return result['result']
        raise ValueError('VM_RESPONSE_LIMIT')

    def ready(self):
        isolates = self.rpc('getVM')['isolates']
        candidates = [i for i in isolates if i['name'] == 'main']
        require(len(candidates) == 1)
        self.isolate = candidates[0]['id']
        # Driver frame-sync medya I/O boyunca bekleyip mesajı tekrar yollamaz.
        self.rpc('ext.flutter.driver', {'isolateId': self.isolate, 'command': 'set_frame_sync', 'enabled': 'false'})

    def data(self, message):
        result = self.rpc('ext.flutter.driver', {'isolateId': self.isolate, 'command': 'request_data',
            'message': json.dumps(message), 'timeout': '10000'})
        require(not result.get('isError'), 'PROBE_REFUSED')
        value = json.loads(result['response']['message'])
        require(value.get('ok') is True, 'PROBE_REFUSED')
        return value

    def close(self):
        try:
            self.socket.close()
        finally:
            self._transport.close()


def runtime_configuration(ready, token):
    require(ready['planSha256'] == PLAN_SHA and ready['runId'] == RUN_ID)
    require(type(ready['contentId']) is int and ready['contentId'] > 0 and ready['contentId'] not in [71, 93, 112])
    require(type(ready['loopbackPort']) is int and 1024 <= ready['loopbackPort'] <= 65535)
    require(len(token) <= 8192 and re.fullmatch(r'[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', token))
    claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
    require(claims['aud'] == 'tellpal-ee0dd' and claims['iss'] == 'https://securetoken.google.com/tellpal-ee0dd'
        and claims['sub'] == QA_UID and type(claims['exp']) is int and claims['exp'] > time.time() + 180
        and type(claims['iat']) is int and claims['iat'] <= time.time()
        and claims.get('user_id', QA_UID) == QA_UID, 'TOKEN_SCOPE_OR_EXPIRY')
    return {'action': 'configure', 'runId': RUN_ID, 'planSha256': PLAN_SHA, 'uid': QA_UID,
        'contentId': ready['contentId'], 'port': ready['loopbackPort'], 'token': token}


def validate_loaded(snapshot, ready, index, canonical):
    require(snapshot['status'] == 'loaded' and snapshot['activeImages'] == 1)
    states = [e for e in snapshot['events'] if e['kind'] == 'state' and e['status'] == 'loaded']
    require(states, 'NO_RENDER_EVIDENCE')
    state = states[-1]
    require(type(state.get('epoch')) is int and snapshot.get('paintedEpoch') == state['epoch'], 'PAINT_RECEIPT_REQUIRED')
    identity = ready['identity']
    require(state['entryIndex'] == index and state['canonicalSource'] == canonical)
    for name in ['contentId', 'language', 'revision', 'manifestDigest', 'jobId', 'liveGeneration']:
        require(state[name] == identity[name], 'LIVE_IDENTITY_CHANGED')
    require(state['requiredWidth'] == 822 and state['requiredHeight'] == 1334)
    require(state['decodedWidth'] == (1712 if canonical else 822)
            and state['decodedHeight'] == (2778 if canonical else 1334))
    require(state['role'] == ('readingDetail' if index == 1 else 'readingPage')
            and state['pageNumber'] == (0 if index == 1 else index - 1))
    approved = plan()
    descriptor = ready['descriptor']
    require(len(descriptor['images']) == 4)
    entry = descriptor['images'][index]
    require(len(entry['renditions']) == 1)
    candidate = entry['renditions'][0]
    source = entry['source']['raster']
    source_plan = approved['sources'][index - 1]
    role = 'READING_DETAIL' if index == 1 else 'READING_PAGE'
    profile = approved['runtimePreflight']['roleFingerprints'][role]
    require(source['sha256'] == source_plan['sha256'] and source['byteSize'] == source_plan['bytes'])
    require(candidate['profileFingerprint'] == profile and state['profileFingerprint'] == profile
        and state['renditionKey'] == candidate['renditionKey'] and state['sourceSha256'] == source['sha256'])
    metadata = source if canonical else candidate['raster']
    require(state['displaySha256'] == metadata['sha256'] and state['expectedPayloadBytes'] == metadata['byteSize'])
    slot = snapshot['slot']
    require(slot is not None and slot['dpr'] == 2 and abs(slot['logicalWidth'] - 411) < .01)
    require(abs(slot['logicalHeight'] - (456.66666666666663 if index == 1 else 694)) < .01)
    requests = [e for e in snapshot['events'] if e['kind'] == 'http' and e['endpoint'] == 'bytes']
    require(requests and all(e['ok'] for e in requests))
    require(requests[-1]['payloadBytes'] == state['expectedPayloadBytes'])
    require(state['activeBuffers'] == state['activeDescriptors'] == state['activeCodecs'] == 0)
    return {'render': state, 'httpPayload': requests[-1], 'slot': slot,
            'paintedEpoch': snapshot['paintedEpoch']}


def run(args, android):
    state = getattr(args, 'state_dir', None) or STATE
    state = state.absolute()
    window = None
    window_deadline = ORIGINAL_DEADLINE_MILLIS
    if state != STATE:
        require(state in [STATE / 'mobile-continuation', STATE / 'mobile-window-180'], 'CONTINUATION_SCOPE_CHANGED')
        private_dir(state)
        approval = json.loads(private_file(state / 'approval.json'))
        require(approval.get('runId') == RUN_ID and approval.get('planSha256') == PLAN_SHA
                and approval.get('contentId') == 94 and approval.get('newProductionAllowed') is False
                and approval.get('priorLedgerSha256') == digest(STATE / 'ledger.log'), 'CONTINUATION_APPROVAL_REQUIRED')
        if state == STATE / 'mobile-window-180':
            require(approval['priorLedgerSha256'] == CONTINUATION_LEDGER_SHA
                    and type(approval.get('windowMillis')) is int and approval['windowMillis'] == 180000
                    and type(approval.get('additionalLoginVerifications')) is int
                    and approval['additionalLoginVerifications'] == 1, 'APPROVED_WINDOW_REQUIRED')
            window = json.loads(private_file(state / 'window.json'))
            require(type(window.get('startedAtEpochMillis')) is int
                    and type(window.get('deadlineEpochMillis')) is int
                    and window['deadlineEpochMillis'] - window['startedAtEpochMillis'] == 180000
                    and type(window.get('windowMillis')) is int and window['windowMillis'] == 180000
                    and window.get('priorLedgerSha256') == CONTINUATION_LEDGER_SHA
                    and type(window.get('additionalLoginVerifications')) is int
                    and window['additionalLoginVerifications'] == 1, 'APPROVED_WINDOW_CHANGED')
            window_deadline = window['deadlineEpochMillis']
            require(window['startedAtEpochMillis'] <= time.time() * 1000 < window_deadline, 'CONTINUATION_WINDOW_EXPIRED')
        else:
            require(ORIGINAL_OPEN_MILLIS <= time.time() * 1000 < window_deadline, 'CONTINUATION_WINDOW_EXPIRED')
        require(not (state / 'mobile.attempt.json').exists(), 'ATTEMPT_EXISTS_NO_RETRY')
    output = args.output.absolute()
    require(not output.exists(), 'OUTPUT_EXISTS_NO_RETRY')
    output.mkdir(mode=0o700)
    private_dir(output)
    private = private_dir(args.private_dir, external=True)
    connection = json.loads(private_file(private / 'connection.private.json'))
    ready = json.loads(private_file(state / 'ready.json'))
    auth = json.loads(private_file(state / 'qa-sign-in.ready.json'))
    if state != STATE:
        prior = json.loads(private_file(STATE / 'ready.json'))
        require(ready['contentId'] == 94 and ready['identity'] == prior['identity']
                and ready['descriptor'] == prior['descriptor'], 'CONTINUATION_LIVE_CHANGED')
        if window is not None:
            require(ready.get('windowStartedAtEpochMillis') == window['startedAtEpochMillis']
                    and ready.get('windowDeadlineEpochMillis') == window_deadline, 'READY_WINDOW_CHANGED')
    require(auth['runId'] == RUN_ID and auth['planSha256'] == PLAN_SHA and auth['qaUid'] == QA_UID)
    token = private_file(Path(auth['tokenFile']), 8192, external=True).decode().strip()
    configuration = runtime_configuration(ready, token)
    require(ready['identity']['contentId'] == ready['contentId'] and ready['identity']['language'] == 'tr')
    remaining = min(180, (window_deadline - time.time() * 1000) / 1000) if state != STATE else 180
    deadline = time.monotonic() + remaining
    def check_window():
        require(time.monotonic() < deadline and (state == STATE or time.time() * 1000 < window_deadline), 'MOBILE_WINDOW_EXPIRED')
    check_window()
    android.preflight()
    check_window()
    require(android.run('shell', 'pidof', PACKAGE) == str(connection['pid']), 'QA_PROCESS_CHANGED')
    check_window()
    write_json(state / 'mobile.attempt.json', {'runId': RUN_ID, 'planSha256': PLAN_SHA, 'state': 'STARTED_NO_RETRY'})
    port = ready['loopbackPort']
    vm = None
    reversed_port = False
    verified = False
    observations = []
    final = {'ok': False, 'status': 'NOT_CONFIGURED'}
    try:
        check_window()
        android.run('reverse', '--no-rebind', 'tcp:' + str(port), 'tcp:' + str(port))
        reversed_port = True
        check_window()
        vm = Vm(connection['websocket'])
        check_window()
        vm.ready()
        check_window()
        baseline = android.memory()
        check_window()
        vm.data(configuration)
        configuration.clear()
        token = None
        check_window()
        for order, (index, canonical) in enumerate(CHOICES):
            check_window()
            if order:
                vm.data({'action': 'select', 'entryIndex': index, 'canonicalSource': canonical})
                check_window()
            snapshot = None
            for _ in range(240):
                check_window()
                snapshot = vm.data({'action': 'snapshot'})
                check_window()
                require(snapshot['status'] != 'error', 'MEDIA_REFUSED_NO_RETRY')
                if snapshot['status'] == 'loaded':
                    break
                time.sleep(.25)
            loaded = [e for e in snapshot['events'] if e['kind'] == 'state' and e['status'] == 'loaded']
            require(loaded and type(loaded[-1].get('epoch')) is int, 'LOADED_EPOCH_REQUIRED')
            epoch = loaded[-1]['epoch']
            check_window()
            snapshot = vm.data({'action': 'paint', 'epoch': epoch})
            check_window()
            require(snapshot.get('paintedEpoch') == epoch, 'PAINT_EPOCH_CHANGED')
            observation = validate_loaded(snapshot, ready, index, canonical)
            observation['memory'] = android.memory()
            check_window()
            screenshot = output / (f'{order + 1:02d}-entry-{index}-' + ('source.png' if canonical else 'rendition.png'))
            observation['screenshotSha256'] = android.screenshot(screenshot)
            check_window()
            observation['screenshot'] = screenshot.name
            observations.append(observation)
            write_json(output / f'{order + 1:02d}-observation.json', observation)
            check_window()
        before_background = vm.data({'action': 'snapshot'})
        check_window()
        android.run('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        time.sleep(.5)
        check_window()
        background = vm.data({'action': 'snapshot'})
        check_window()
        require(background['activeImages'] == 0 and background['status'] == 'error', 'BACKGROUND_RETAINED_IMAGE')
        android.run('shell', 'am', 'start', '-n', PACKAGE + '/' + ACTIVITY)
        time.sleep(.5)
        check_window()
        returned = vm.data({'action': 'snapshot'})
        check_window()
        require(returned['activeImages'] == 0 and returned['status'] == 'error', 'RETURN_READMISSION_REQUIRED')
        def media_count(s):
            return len([e for e in s['events'] if e['kind'] == 'http' and e['endpoint'] == 'bytes'])
        require(media_count(before_background) == media_count(returned) == 6, 'UNPLANNED_MEDIA_REQUEST')
        final = vm.data({'action': 'stop'})
        check_window()
        require(all(final[name] == 0 for name in ['activeBuffers', 'activeDescriptors', 'activeCodecs', 'activeImages']))
        time.sleep(.25)
        closed_screenshot = android.screenshot(output / '07-closed.png')
        check_window()
        closed_memory = android.memory()
        check_window()
        write_json(output / 'verification.json', {'planSha256': PLAN_SHA, 'runId': RUN_ID,
            'device': connection['device'], 'apkSha256': connection['apkSha256'], 'baselineMemory': baseline,
            'observations': observations, 'background': background, 'returned': returned,
            'closed': final, 'closedScreenshotSha256': closed_screenshot,
            'closedMemory': closed_memory, 'status': 'SIX_STATES_VERIFIED_EMULATOR_ONLY'})
        verified = True
        print('SIX_STATES_VERIFIED_EMULATOR_ONLY_PHYSICAL_QUALITY_OPEN')
    finally:
        cleanup = {}
        def clean(name, action):
            try:
                action()
                cleanup[name] = 'PASS'
            except Exception:
                cleanup[name] = 'FAILED'
        if vm is not None:
            clean('vmStop', lambda: vm.data({'action': 'stop'}))
            clean('vmClose', vm.close)
        else:
            cleanup.update({'vmStop': 'NOT_CREATED', 'vmClose': 'NOT_CREATED'})
        configuration.clear()
        token = None
        # Her sahipliğin temizliği bağımsızdır; bir hata diğer makbuzları engellemez.
        if reversed_port:
            clean('reverse', lambda: android.run('reverse', '--remove', 'tcp:' + str(port)))
        else:
            cleanup['reverse'] = 'NOT_CREATED'
        clean('forward', lambda: android.run('forward', '--remove', 'tcp:' + str(connection['hostPort'])))
        write_json(output / 'attempt-result.json', {'runId': RUN_ID, 'statesRecorded': len(observations),
            'finished': verified, 'cleanup': cleanup})


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument('--build', action='store_true')
    modes.add_argument('--arm', action='store_true')
    modes.add_argument('--device-preflight', action='store_true')
    modes.add_argument('--run', action='store_true')
    parser.add_argument('--private-dir', type=Path)
    parser.add_argument('--state-dir', type=Path)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--adb', type=Path)
    parser.add_argument('--aapt', type=Path)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--serial', default='emulator-5554')
    args = parser.parse_args(argv)
    if not any([args.build, args.arm, args.device_preflight, args.run]):
        print('DRY_RUN_NO_INPUT_CREDENTIAL_DEVICE_NETWORK_IO_NO_LOGIN')
        return 0
    try:
        plan()
        if args.build:
            build(args)
        else:
            require(args.adb is not None)
            android = Android(args.adb, args.serial)
            if args.device_preflight:
                print(json.dumps(android.preflight()))
            elif args.arm:
                arm(args, android)
            else:
                require(args.output is not None and args.private_dir is not None)
                run(args, android)
        return 0
    except Exception:
        print('REFUSED_COMPARISON_PRIVATE_EVIDENCE_PRESERVED_NO_RETRY')
        return 2


if __name__ == '__main__':
    sys.exit(main())
