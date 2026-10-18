"""P4a5 çağrı öncesi korumaları; gerçek ADB/auth/storage açılmaz."""
import base64
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import p4_consumer_qa as qa

FIXTURES = qa.ROOT / 'be/src/test/resources/p4'


def expected():
    value = json.loads((FIXTURES / 'consumer-retained-ready.json').read_bytes())
    value['proposalSha256'] = qa.PROPOSAL_SHA
    value['leaseMillis'] = 180000
    value['pid'] = 44
    value['localGateStartedAtEpochMillis'] = 2000000000000
    value['localGateDeadlineEpochMillis'] = 2000000300000
    value['windowStartedAtEpochMillis'] = 2000000000000
    value['windowDeadlineEpochMillis'] = 2000000180000
    return value


def connection():
    return {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE, 'serial': 'emulator-5554',
        'pid': 22, 'processStartTicks': '123456', 'hostPort': 45678, 'devicePort': 12345,
        'apkSha256': 'a' * 64, 'websocket': 'ws://127.0.0.1:45678/private_nonce/ws', 'buildName': 'build-0001'}


def snapshot(order=0):
    ready = expected()
    events = [{'kind': 'http', 'endpoint': 'manifest', 'ok': True, 'payloadBytes': 1000}]
    for number in range(order + 1):
        index = qa.ORDER[number]
        entry = ready['descriptor']['images'][index]
        candidate = entry['renditions'][0]
        src = entry['source']['raster']
        slot = {'logicalWidth': 411, 'logicalHeight': 456.6666666667 if index == 1 else 700.5,
                'dpr': 2, 'fit': 'cover' if index == 1 else 'contain'}
        physical = (822, qa.math.ceil(slot['logicalHeight'] * 2))
        required = qa.fit_dimensions(src['width'], src['height'], *physical, slot['fit'])
        decode = qa.fit_dimensions(822, 1334, *required, 'cover')
        event = {'kind': 'state', 'status': 'loaded', 'epoch': number + 1,
            'entryIndex': index, 'canonicalSource': False, **ready['identity'],
            'role': 'readingDetail' if index == 1 else 'readingPage',
            'pageId': entry['source']['binding']['pageId'], 'pageNumber': entry['source']['binding']['pageNumber'],
            'profileFingerprint': candidate['profileFingerprint'], 'renditionKey': candidate['renditionKey'],
            'sourceSha256': src['sha256'], 'displaySha256': candidate['raster']['sha256'],
            'expectedPayloadBytes': candidate['raster']['byteSize'],
            'physicalWidth': physical[0], 'physicalHeight': physical[1],
            'requiredWidth': required[0], 'requiredHeight': required[1],
            'decodedWidth': decode[0], 'decodedHeight': decode[1], 'decodeWidth': decode[0], 'decodeHeight': decode[1],
            'activeBuffers': 0, 'activeDescriptors': 0, 'activeCodecs': 0}
        events += [{'kind': 'http', 'endpoint': 'bytes', 'ok': True, 'payloadBytes': 3292721}, event]
    return {'status': 'loaded', 'activeImages': 1, 'cubitCount': 1, 'events': events,
        'paintedEpoch': order + 1, 'slot': slot}


class ConsumerQaTest(unittest.TestCase):
    def test_default_does_not_read_files_credentials_or_device(self):
        with patch.object(qa, 'plan', side_effect=AssertionError), patch.object(qa, 'Android', side_effect=AssertionError):
            self.assertEqual(qa.main([]), 0)

    def test_terminal_and_ready_fixtures_match_approved_chain(self):
        self.assertEqual(qa.digest(FIXTURES / 'consumer-terminal-ledger.txt'), qa.TERMINAL_SHA)
        self.assertEqual((FIXTURES / 'consumer-terminal-ledger.txt').stat().st_size, 11068)
        self.assertEqual(qa.digest(FIXTURES / 'consumer-retained-ready.json'), qa.READY_SHA)
        self.assertEqual(qa.digest(FIXTURES / 'consumer-proposal.json'), qa.PROPOSAL_SHA)

    def test_four_distinct_real_geometry_receipts_use_integer_oracle(self):
        for order in range(4):
            result = qa.validate_loaded(snapshot(order), expected(), order)
            self.assertEqual(result['render']['entryIndex'], qa.ORDER[order])
        self.assertNotEqual(snapshot(1)['slot']['logicalHeight'], 694)

    def test_paint_epoch_identity_role_page_bytes_profile_and_decode_cannot_drift(self):
        for field, value in [('epoch', 2), ('contentId', 71), ('jobId', 'other'), ('role', 'readingPage'),
                ('pageId', 1427), ('pageNumber', 1), ('displaySha256', 'other'), ('expectedPayloadBytes', 1),
                ('renditionKey', 'other'), ('profileFingerprint', 'other'), ('decodedHeight', 1),
                ('requiredWidth', 1), ('canonicalSource', True), ('activeCodecs', 1)]:
            value_snapshot = snapshot()
            value_snapshot['events'][-1][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                qa.validate_loaded(value_snapshot, expected(), 0)

    def test_extra_http_or_loaded_state_and_missing_manifest_refuse(self):
        for mutate in [lambda s: s['events'].append(copy.deepcopy(s['events'][-1])),
                lambda s: s['events'].append({'kind': 'http', 'endpoint': 'bytes', 'ok': True, 'payloadBytes': 1}),
                lambda s: s['events'].pop(0), lambda s: s.update(cubitCount=2),
                lambda s: s['slot'].update(dpr=1), lambda s: s.update(paintedEpoch=None)]:
            value = snapshot(); mutate(value)
            with self.assertRaises(ValueError):
                qa.validate_loaded(value, expected(), 0)

    def test_shortage_is_failure_and_not_silently_scaled(self):
        value = snapshot(1)
        value['slot']['logicalWidth'] = 412
        with self.assertRaises(ValueError):
            qa.validate_loaded(value, expected(), 1)

    def test_approval_requires_exact_integer_scope_and_fixed_private_root(self):
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve()
            approval = {'proposalSha256': qa.PROPOSAL_SHA, 'decision': 'A', 'runId': qa.RUN,
                'qaUid': qa.UID, 'contentId': 94, 'windowMillis': 180000, 'newProductionAllowed': False}
            qa.write_json(root / 'approval.json', approval)
            qa.approval(root)
            (root / 'approval.json').unlink()
            qa.write_json(root / 'approval.json', {**approval, 'windowMillis': 180000.0})
            with self.assertRaises(ValueError): qa.approval(root)

    def test_vm_drives_real_text_and_tooltip_controls_without_select_command(self):
        vm = object.__new__(qa.Vm); vm.isolate = 'main';calls = []
        vm.rpc = lambda name, params: calls.append((name, params)) or {}
        vm.tap('QA sayfalarını aç'); vm.tap('Sonraki'); vm.tap('QA detail ekranına dön', True); vm.tap('QA akışını kapat', True)
        self.assertEqual([c[1]['command'] for c in calls], ['tap'] * 4)
        self.assertEqual(calls[2][1]['finderType'], 'ByTooltipMessage')

    def test_single_login_reservation_precedes_credential_load_and_survives_failure(self):
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();(root / 'backend').mkdir(mode=0o700)
            qa.write_json(root / 'backend/local-ready.json', expected())
            from types import SimpleNamespace
            args = SimpleNamespace(admin_credential='unused', web_api_config='unused')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'), patch.object(qa, 'login_device_gate'), patch.object(qa, 'login_backend_gate'), patch.object(qa, 'plan', return_value=({}, expected())), \
                    patch('native_test_login.load_inputs', side_effect=RuntimeError('synthetic')) as load:
                with self.assertRaises(RuntimeError): qa.login(args)
                self.assertEqual(load.call_count, 1)
                with self.assertRaises(FileExistsError): qa.login(args)
                self.assertEqual(load.call_count, 1)
            value = json.loads((root / 'backend/login.reservation.private.json').read_bytes())
            self.assertEqual(value['qaSignInInvocationsTotal'], 3)
            self.assertEqual(value['firebaseVerifierReservationsTotal'], 21)

    def test_runtime_window_and_jwt_claims_are_bound_without_token_output(self):
        def jwt(uid=qa.UID):
            payload = {'sub': uid, 'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd',
                'iat': 2000000000, 'exp': 2000003600}
            return 'e30.' + base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip('=') + '.synthetic'
        with patch.object(qa.time, 'time', return_value=2000000001):
            self.assertEqual(qa.runtime_configuration(expected(), jwt())['contentId'], 94)
            with self.assertRaises(ValueError): qa.runtime_configuration(expected(), jwt('other'))
        with patch.object(qa.time, 'time', return_value=2000000180):
            with self.assertRaises(ValueError): qa.runtime_configuration(expected(), jwt())

class ActualRunSafetyTest(unittest.TestCase):
    def exercise(self, mode, recovery=False):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve()
            if recovery:
                LocalRuntimeRecoveryTest.fixture(self, root)
                LocalRuntimeRecoveryTest.reserve(self, root)
                LocalRuntimeRecoveryTest.claim(self, root)
            else:
                (root / 'backend').mkdir(mode=0o700);(root / 'android').mkdir(mode=0o700)
            state = root / ('backend-02' if recovery else 'backend')
            original_files = {name: (root / name).read_bytes() for name in qa.RECOVERY_HASHES} if recovery else {}
            ready = expected()
            ready['loopbackPort'] = 12456
            qa.write_json(state / 'ready.json', ready)
            qa.write_json(state / 'local-ready.json', ready)
            connection_value = {**connection(), **({'proposalSha256': 'wrong'} if mode == 'invalid-connection' else {})}
            if recovery:
                connection_value.update(buildName='build-0002', apkSha256=qa.WAITING_APK_SHA,
                    recoveryApprovalSha256=qa.digest(root / qa.RECOVERY_APPROVAL),
                    qaReservationSha256=qa.digest(root / qa.RECOVERY_QA_RESERVATION))
            qa.write_json(root / 'android' / ('connection-03.private.json' if recovery else 'connection.private.json'), connection_value)
            qa.write_json(root / 'android/build.json', {'apkSha256': 'a' * 64, 'sourceHashes': {}})
            payload = {'sub': qa.UID, 'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd', 'iat': 2000000000, 'exp': 2000003600}
            token = 'e30.' + base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip('=') + '.synthetic'
            qa.write_new(state / 'id-token.private', token.encode())
            qa.write_json(state / 'qa-sign-in.ready.json', {'runId': qa.RUN, 'qaUid': qa.UID, 'tokenFile': str(state / 'id-token.private')})
            clock = [2000000181.0 if mode == 'expired' else 2000000001.0]
            trace = []
            flags = {'background': False, 'reverse': False, 'returned': False, 'stopping': False, 'stopObservations': 0}
            case = self
            class Android:
                def preflight(self):
                    trace.append('preflight')
                    if mode == 'preflight': raise ValueError('synthetic')
                def pid(self, **kwargs):
                    if flags['stopping']:
                        flags['stopObservations'] += 1
                        if mode == 'stop-identity-exit' and not flags.get('identityExited'): return '22'
                        if mode == 'stop-timeout': return '22'
                        if mode == 'stop-replaced-pid': return '99'
                        if mode == 'stop-replaced-ticks': return '22'
                        if mode == 'stop-delayed' and flags['stopObservations'] < 3: return '22'
                        return ''
                    if flags.get('preHome') and mode == 'before-home-death': return ''
                    if flags['background'] and mode == 'home-death': return ''
                    if flags['background'] and mode == 'home-replaced-pid': return '99'
                    return '22'
                def process_identity(self, pid, **kwargs):
                    if flags['stopping'] and mode == 'stop-identity-exit':
                        flags['identityExited'] = True
                        raise OSError('SYNTHETIC_PROC_EXIT_DURING_IDENTITY')
                    if flags['stopping'] and mode == 'stop-replaced-ticks': return {'pid': pid, 'processStartTicks': '999999'}
                    if flags['background'] and mode == 'home-replaced-command': raise ValueError('QA_PROCESS_COMMAND_CHANGED')
                    ticks = '999999' if (flags['background'] and mode == 'home-replaced-ticks') or (flags['returned'] and mode == 'return-replaced') else '123456'
                    return {'pid': pid, 'processStartTicks': ticks}
                def memory(self): return {'kind': 'synthetic'}
                def screenshot(self, path):
                    trace.append(path.name)
                    if path.name == '04.png': flags['preHome'] = True
                    if mode == 'late-screenshot' and path.name == '05-closed.png': clock[0] += 181
                    return 'synthetic'
                def run(self, *args):
                    trace.append(args)
                    if args == ('forward', '--list'): return 'emulator-5554 tcp:53421 tcp:39447\nemulator-5554 tcp:45678 tcp:12345\n'
                    if args == ('reverse', '--list'):
                        return 'host-unrelated tcp:50001 tcp:50002\n' + (
                            'host-owned tcp:12456 tcp:12456\n' if flags['reverse'] or mode == 'reverse-present' else '')
                    if args == ('reverse', '--no-rebind', 'tcp:12456', 'tcp:12456'):
                        intent = json.loads((state / 'reverse-intent.private.json').read_bytes())
                        case.assertTrue(intent['baselineMappingAbsent']);case.assertEqual(intent['pid'], 22)
                        flags['reverse'] = True
                        if mode == 'reverse-timeout': raise qa.subprocess.TimeoutExpired(['adb', 'reverse'], 15)
                    if args == ('reverse', '--remove', 'tcp:12456'): flags['reverse'] = False
                    if 'KEYCODE_HOME' in args: flags['background'] = True
                    if args[:3] == ('shell', 'am', 'start'): flags['returned'] = True
                    if args == ('shell', 'am', 'force-stop', qa.PACKAGE):
                        flags['stopping'] = True;flags['stopStartedAt'] = clock[0]
            class Vm:
                def __init__(self, url):
                    case.assertEqual(url, connection_value['websocket'])
                    self.order, self.closed, self.stopped = 0, False, False
                def ready(self): pass
                def tap(self, text, tooltip=False):
                    trace.append(text)
                    if text == 'QA akışını kapat': self.closed = True
                    else: self.order += 1
                def data(self, message):
                    if message['action'] == 'configure': return {'ok': True}
                    if message['action'] == 'stop': self.stopped = True
                    value = snapshot(self.order)
                    value.update(ok=True, activeBuffers=0, activeDescriptors=0, activeCodecs=0,
                        activeImages=0 if flags['background'] or self.closed or self.stopped else 1,
                        cubitClosed=self.closed, credentialActive=not self.stopped, deadlineActive=not self.stopped)
                    return value
                def close(self):
                    trace.append('vmClose')
                    if mode == 'cleanup-failure': raise ValueError('synthetic')
            def choose_build(*args):
                if mode == 'source-changed': raise ValueError('SOURCE_COPY_CHANGED')
                return {'apkSha256': connection_value['apkSha256'], 'sourceHashes': {}}, root / 'android'
            def close_backend(state):
                trace.append('backendClosed')
                case.assertEqual(state, root / ('backend-02' if recovery else 'backend'))
                if mode in ['backend-crash', 'backend-watchdog', 'backend-drain']:
                    return real_close_backend(state)
                return 'DRAINED'
            real_close_backend = qa.close_backend
            backend_killed = []
            if mode == 'backend-drain':
                qa.write_json(state / 'backend-closed.json', {'proposalSha256': qa.PROPOSAL_SHA, 'runId': qa.RUN, 'pid': 44, 'closed': True})
            from types import SimpleNamespace
            def ps(*args, **kwargs):
                running = mode == 'backend-watchdog' and not backend_killed
                command = ('java -cp cache com.tellpal.v2.support.P4ConsumerQaSetup --execute ' + str(state)).encode()
                return SimpleNamespace(returncode=0 if running else 1, stdout=command if running else b'')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'), \
                    patch.object(qa, 'plan', return_value=({}, ready)), patch.object(qa, 'sources', return_value={}), \
                    patch.object(qa, 'selected_build', side_effect=choose_build), \
                    patch.object(qa, 'Vm', Vm), patch.object(qa, 'close_backend', side_effect=close_backend), \
                    patch.object(qa.subprocess, 'run', side_effect=ps), patch.object(qa.os, 'kill', side_effect=lambda pid, sig: backend_killed.append(pid)), \
                    patch.object(qa.time, 'time', side_effect=lambda: clock[0]), \
                    patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0] - 2000000000), patch.object(qa.time, 'sleep', side_effect=lambda seconds: clock.__setitem__(0, clock[0] + seconds)):
                if mode in ['success', 'backend-drain', 'stop-delayed', 'stop-identity-exit']: qa.run(SimpleNamespace(), Android())
                else:
                    with self.assertRaises((ValueError, qa.subprocess.TimeoutExpired)): qa.run(SimpleNamespace(), Android())
            result = json.loads((root / 'observations/attempt-result.json').read_bytes())
            self.assertEqual(result['finished'], mode in ['success', 'backend-drain', 'stop-delayed', 'stop-identity-exit'])
            self.assertEqual((root / 'observations/verification.json').exists(), mode in ['success', 'backend-drain', 'stop-delayed', 'stop-identity-exit'])
            self.assertIn('backendClosed', trace)
            if mode == 'reverse-timeout':
                self.assertIn(('reverse', '--remove', 'tcp:12456'), trace)
                self.assertFalse(flags['reverse'])
                self.assertEqual(result['cleanup']['reverse']['status'], 'PASS')
                self.assertEqual(result['statesRecorded'], 0)
            if mode == 'reverse-present':
                self.assertNotIn(('reverse', '--no-rebind', 'tcp:12456', 'tcp:12456'), trace)
                self.assertNotIn(('reverse', '--remove', 'tcp:12456'), trace)
                self.assertFalse((state / 'reverse-intent.private.json').exists())
                self.assertNotIn('reverse', result['cleanup'])
            self.assertNotIn(('reverse', '--remove', 'tcp:50001'), trace)
            if mode.startswith('home-'):
                self.assertFalse(any(isinstance(call, tuple) and call[:3] == ('shell', 'am', 'start') for call in trace))
                self.assertFalse((state / 'foreground-return.private.json').exists())
            if mode == 'before-home-death':
                self.assertNotIn(('shell', 'input', 'keyevent', 'KEYCODE_HOME'), trace)
                self.assertFalse(any(isinstance(call, tuple) and call[:3] == ('shell', 'am', 'start') for call in trace))
            if mode == 'return-replaced':
                self.assertTrue(flags['returned']);self.assertFalse((state / 'foreground-return.private.json').exists())
            if mode in ['success', 'backend-drain', 'stop-delayed', 'stop-identity-exit']:
                self.assertTrue(json.loads((state / 'foreground-return.private.json').read_bytes())['verifiedAfterReturn'])
            if mode.startswith('stop-'):
                self.assertEqual(trace.count(('shell', 'am', 'force-stop', qa.PACKAGE)), 1)
                self.assertLessEqual(clock[0] - flags['stopStartedAt'], 2.00001)
                self.assertEqual(result['cleanup']['qaProcess']['status'], 'PASS' if mode in ['stop-delayed', 'stop-identity-exit'] else 'FAILED')
                if mode == 'stop-delayed': self.assertGreaterEqual(flags['stopObservations'], 3)
                if mode == 'stop-identity-exit': self.assertEqual(flags['stopObservations'], 2)
            if recovery:
                self.assertTrue((state / 'mobile.attempt.json').exists())
                self.assertFalse((root / 'backend/mobile.attempt.json').exists())
                for filename, data in original_files.items(): self.assertEqual((root / filename).read_bytes(), data)
            if mode == 'invalid-connection':
                self.assertNotIn(('forward', '--remove', 'tcp:45678'), trace)
                self.assertNotIn(('shell', 'am', 'force-stop', qa.PACKAGE), trace)
            else:
                self.assertIn(('forward', '--remove', 'tcp:45678'), trace)
            return result

    def test_recovery_run_uses_new_backend_claim_auth_connection_and_preserves_old_files(self):
        self.assertTrue(self.exercise('success', recovery=True)['finished'])

    def test_reverse_side_effect_then_timeout_is_cleaned_by_real_recovery_run(self):
        self.exercise('reverse-timeout', recovery=True)

    def test_preexisting_reverse_is_never_rebound_or_removed_by_real_recovery_run(self):
        self.exercise('reverse-present', recovery=True)

    def test_home_process_death_or_replacement_never_issues_return_launch(self):
        for mode in ['before-home-death', 'home-death', 'home-replaced-pid', 'home-replaced-ticks', 'home-replaced-command']:
            with self.subTest(mode=mode): self.exercise(mode, recovery=True)

    def test_changed_process_after_return_fails_instead_of_certifying_continuity(self):
        self.exercise('return-replaced', recovery=True)

    def test_delayed_owned_qa_exit_allows_real_run_success_only_after_absence(self):
        self.assertTrue(self.exercise('stop-delayed', recovery=True)['finished'])

    def test_owned_exit_between_pid_and_identity_is_confirmed_by_fresh_absence_in_real_run(self):
        self.assertTrue(self.exercise('stop-identity-exit', recovery=True)['finished'])

    def test_owned_qa_remaining_alive_after_two_seconds_makes_real_run_fail(self):
        self.assertFalse(self.exercise('stop-timeout', recovery=True)['finished'])

    def test_replaced_qa_after_single_stop_makes_real_run_fail_without_another_stop(self):
        for mode in ['stop-replaced-pid', 'stop-replaced-ticks']:
            with self.subTest(mode=mode): self.exercise(mode, recovery=True)

    def test_build_or_source_failure_cleans_owned_connection_before_vm_exists(self):
        self.assertEqual(self.exercise('source-changed')['statesRecorded'], 0)

    def test_invalid_connection_does_not_close_unrelated_process_or_forward(self):
        self.assertEqual(self.exercise('invalid-connection')['statesRecorded'], 0)

    def test_real_backend_helper_crash_without_receipt_makes_run_fail(self):
        result = self.exercise('backend-crash')
        self.assertEqual(result['cleanup']['backendClose']['status'], 'FAILED')

    def test_real_backend_watchdog_helper_cannot_write_run_success(self):
        result = self.exercise('backend-watchdog')
        self.assertEqual(result['cleanup']['backendClose']['status'], 'FAILED')
        self.assertEqual(result['cleanup']['backendClose']['code'], 'BACKEND_WATCHDOG_OR_MISSING_DRAIN')

    def test_real_backend_drained_receipt_and_exit_allow_success(self):
        self.assertTrue(self.exercise('backend-drain')['finished'])

    def test_real_run_success_drives_four_ui_states_and_cleanup(self):
        self.assertEqual(self.exercise('success')['statesRecorded'], 4)

    def test_final_screenshot_past_window_cannot_create_success(self):
        self.assertEqual(self.exercise('late-screenshot')['statesRecorded'], 4)

    def test_preflight_failure_still_records_failed_attempt_and_cleans_owned_mapping(self):
        self.assertEqual(self.exercise('preflight')['statesRecorded'], 0)

    def test_expired_configuration_still_cleans_before_vm_exists(self):
        self.assertEqual(self.exercise('expired')['statesRecorded'], 0)

    def test_vm_cleanup_failure_does_not_hide_backend_cleanup_or_claim_success(self):
        self.exercise('cleanup-failure')

    def test_offline_build_failures_and_patches_keep_distinct_immutable_receipts(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();mobile = root / 'source/app/mobile'
            (mobile / 'android').mkdir(parents=True);(root / 'android').mkdir()
            (root / 'jdk/bin').mkdir(parents=True);(root / 'jdk/bin/java').write_bytes(b'fake')
            (root / 'flutter/bin').mkdir(parents=True);(root / 'flutter/bin/flutter').write_bytes(b'fake')
            (root / 'sdk').mkdir()
            qa.write_json(root / 'operational-tools.private.json', {'flutterRoot': str(root / 'flutter'), 'androidSdk': str(root / 'sdk')})
            apk = mobile / 'build/app/outputs/apk/debug/app-debug.apk';apk.parent.mkdir(parents=True);apk.write_bytes(b'fake-apk')
            (mobile / qa.ENTRY).parent.mkdir(exist_ok=True);(mobile / qa.ENTRY).write_text('entry');(mobile / qa.PROBE).write_text('probe')
            calls = []
            def process(argv, **kwargs):
                self.assertIn('--offline', argv);self.assertIn('-Ptarget=' + qa.ENTRY, argv)
                kwargs['stdout'].write(('build-%s' % len(calls)).encode());calls.append(argv)
                return SimpleNamespace(returncode=1 if len(calls) == 1 else 0)
            args = SimpleNamespace(java_home=root / 'jdk', aapt='fake-aapt')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'sources', return_value={}), \
                    patch.object(qa.subprocess, 'run', side_effect=process), \
                    patch.object(qa, 'command', return_value=("package: name='" + qa.PACKAGE + "'").encode()):
                with self.assertRaises(ValueError): qa.build(args)
                qa.build(args)
            self.assertEqual((root / 'android/builds/build-0001/build.log').read_bytes(), b'build-0')
            self.assertFalse((root / 'android/builds/build-0001/build.json').exists())
            with patch.object(qa, 'sources', return_value={}):
                receipt, directory = qa.selected_build(root)
                self.assertEqual(receipt['buildName'], 'build-0002')
                self.assertEqual(directory.name, 'build-0002')
            with patch.object(qa, 'sources', return_value={'changed': 'source'}):
                with self.assertRaises(ValueError): qa.selected_build(root)


class ReviewBoundaryTest(unittest.TestCase):
    def test_build_rejects_source_changed_in_both_copies_during_compile(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();mobile = root / 'source/app/mobile'
            (mobile / 'android').mkdir(parents=True);(root / 'android').mkdir()
            for selected in ['jdk/bin/java', 'flutter/bin/flutter']:
                target = root / selected;target.parent.mkdir(parents=True);target.write_bytes(b'fake')
            (root / 'sdk').mkdir()
            qa.write_json(root / 'operational-tools.private.json', {'flutterRoot': str(root / 'flutter'), 'androidSdk': str(root / 'sdk')})
            (mobile / qa.ENTRY).parent.mkdir();(mobile / qa.ENTRY).write_text('entry');(mobile / qa.PROBE).write_text('probe')
            changed = [False]
            def source_hashes(path): return {'source': 'changed' if changed[0] else 'initial'}
            def process(*args, **kwargs):
                changed[0] = True
                kwargs['stdout'].write(b'synthetic compile completed')
                return SimpleNamespace(returncode=0)
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'sources', side_effect=source_hashes), \
                    patch.object(qa.subprocess, 'run', side_effect=process), patch.object(qa, 'command') as aapt:
                with self.assertRaisesRegex(ValueError, 'SOURCE_CHANGED_DURING_BUILD'):
                    qa.build(SimpleNamespace(java_home=root / 'jdk', aapt='unused'))
                aapt.assert_not_called()
            self.assertFalse((root / 'android/builds/build-0001/build.json').exists())
            self.assertTrue((root / 'android/builds/build-0001/build.log').exists())

    def arm_failure(self, mode):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();(root / 'android').mkdir()
            build = root / 'build';build.mkdir();(build / 'consumer.apk').write_bytes(b'fake-apk')
            receipt = {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE, 'apkSha256': qa.digest(build / 'consumer.apk'), 'sourceHashes': {}, 'buildName': 'build-0001'}
            trace = [];started = [False];mapped = [False]
            self_case = self
            class Android:
                def preflight(self): return {'serial': 'emulator-5554'}
                def pid(self, **kwargs): return '22' if started[0] else ''
                def process_identity(self, pid, **kwargs): return {'pid': pid, 'processStartTicks': '123456'}
                def run(self, *args, **kwargs):
                    trace.append(args)
                    if args[:3] == ('shell', 'am', 'start'): started[0] = True
                    if args[:1] == ('logcat',):
                        if mode == 'vm': raise ValueError('VM_DISCOVERY_FAILED')
                        return 'Dart VM service is listening on http://127.0.0.1:12345/private_nonce/'
                    if args == ('forward', '--no-rebind', 'tcp:45678', 'tcp:12345'):
                        intent = json.loads((root / 'android/arm-forward-intent.private.json').read_bytes())
                        self_case.assertTrue(intent['baselineMappingAbsent'])
                        self_case.assertEqual((intent['pid'], intent['processStartTicks'], intent['hostPort'], intent['devicePort']), (22, '123456', 45678, 12345))
                        mapped[0] = True
                        if mode == 'forward-timeout': raise qa.subprocess.TimeoutExpired(['adb', 'forward'], 15)
                        return ''
                    if args == ('forward', '--list'):
                        return 'emulator-5554 tcp:53421 tcp:39447\n' + ('emulator-5554 tcp:45678 tcp:9999\n' if mode == 'existing-forward' else 'emulator-5554 tcp:45678 tcp:12345\n' if mapped[0] else '')
                    if args == ('forward', '--remove', 'tcp:45678'): mapped[0] = False
                    if args == ('shell', 'am', 'force-stop', qa.PACKAGE):
                        if mode == 'cleanup': raise ValueError('QA_STOP_FAILED')
                        started[0] = False
                    return ''
            real_write = qa.write_json
            def write(path, value):
                if path.name == 'connection.private.json': raise OSError('synthetic receipt failure')
                return real_write(path, value)
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'selected_build', return_value=(receipt, build)), \
                    patch.object(qa, 'sources', return_value={}), patch.object(qa, 'available_host_port', return_value=45678), patch.object(qa, 'write_json', side_effect=write):
                with self.assertRaises(Exception): qa.arm(SimpleNamespace(), Android())
            result = json.loads((root / 'android/arm-failure.private.json').read_bytes())
            self.assertIn(('shell', 'am', 'force-stop', qa.PACKAGE), trace)
            self.assertNotIn(('forward', '--remove', 'tcp:53421'), trace)
            if mode not in ['vm', 'existing-forward']:
                self.assertIn(('forward', '--remove', 'tcp:45678'), trace)
                self.assertFalse(mapped[0])
                self.assertIn(('forward', '--no-rebind', 'tcp:45678', 'tcp:12345'), trace)
                self.assertNotIn(('forward', '--remove-all'), trace)
            else:
                self.assertNotIn('forward', result['cleanup'])
                if mode == 'existing-forward':
                    self.assertNotIn(('forward', '--no-rebind', 'tcp:45678', 'tcp:12345'), trace)
                    self.assertFalse((root / 'android/arm-forward-intent.private.json').exists())
            return result

    def test_arm_vm_discovery_failure_cleans_only_new_qa_process(self):
        self.assertEqual(self.arm_failure('vm')['cleanup']['qaProcess']['status'], 'PASS')

    def test_arm_preexisting_host_forward_is_preserved_without_command_or_intent(self):
        value = self.arm_failure('existing-forward')
        self.assertNotIn('forward', value['cleanup'])
        self.assertEqual(value['cleanup']['qaProcess']['status'], 'PASS')

    def test_arm_forward_side_effect_then_timeout_cleans_predeclared_owned_mapping(self):
        value = self.arm_failure('forward-timeout')
        self.assertEqual(value['cleanup']['forward']['status'], 'PASS')
        self.assertEqual(value['cleanup']['forward']['result'], 'REMOVED_OWNED_MAPPING')
        self.assertEqual(value['cleanup']['qaProcess']['status'], 'PASS')

    def test_arm_connection_receipt_failure_cleans_new_forward_and_qa(self):
        value = self.arm_failure('receipt')
        self.assertEqual(value['cleanup']['forward']['status'], 'PASS')
        self.assertEqual(value['cleanup']['qaProcess']['status'], 'PASS')

    def test_arm_cleanup_failure_is_preserved_without_touching_old_mapping(self):
        value = self.arm_failure('cleanup')
        self.assertEqual(value['cleanup']['forward']['status'], 'PASS')
        self.assertEqual(value['cleanup']['qaProcess']['status'], 'FAILED')
        self.assertEqual(value['cleanup']['qaProcess']['code'], 'QA_STOP_FAILED')

    def test_stale_or_closed_backend_does_not_reserve_single_login(self):
        from types import SimpleNamespace
        for mode in ['gone', 'wrong-command', 'expired', 'too-short', 'missing-start', 'missing-deadline', 'rollback', 'wrong-type', 'failure.json', 'backend-closed.json', 'close.request', 'window.json', 'open.request']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as name:
                root = Path(name).resolve();state = root / 'backend';state.mkdir()
                ready = expected()
                if mode == 'missing-start': del ready['localGateStartedAtEpochMillis']
                if mode == 'missing-deadline': del ready['localGateDeadlineEpochMillis']
                if mode == 'wrong-type': ready['localGateStartedAtEpochMillis'] = 2000000000000.0
                qa.write_json(state / 'local-ready.json', ready)
                if mode.endswith('.json') or mode.endswith('.request'): qa.write_new(state / mode, b'closed')
                now = 1999999999 if mode == 'rollback' else 2000000301 if mode == 'expired' else 2000000289 if mode == 'too-short' else 2000000001
                command = ('java com.tellpal.v2.support.P4ConsumerQaSetup --execute ' + str(state)).encode()
                process = SimpleNamespace(returncode=1 if mode == 'gone' else 0, stdout=b'java unrelated' if mode == 'wrong-command' else command)
                with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'), patch.object(qa, 'login_device_gate'), \
                        patch.object(qa, 'plan', return_value=({}, ready)), patch.object(qa.time, 'time', return_value=now), \
                        patch.object(qa.subprocess, 'run', return_value=process), patch('native_test_login.load_inputs') as credential:
                    with self.assertRaises(ValueError): qa.login(SimpleNamespace())
                    credential.assert_not_called()
                self.assertFalse((state / 'login.reservation.private.json').exists())

    def test_valid_local_backend_gate_still_needs_single_reservation_before_credential_io(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();state = root / 'backend';state.mkdir();ready = expected()
            qa.write_json(state / 'local-ready.json', ready)
            command = ('java -cp cache com.tellpal.v2.support.P4ConsumerQaSetup --execute ' + str(state)).encode()
            def credential(*args):
                self.assertTrue((state / 'login.reservation.private.json').is_file())
                raise RuntimeError('synthetic')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'), patch.object(qa, 'login_device_gate'), \
                    patch.object(qa, 'plan', return_value=({}, ready)), patch.object(qa.time, 'time', return_value=2000000001), \
                    patch.object(qa.subprocess, 'run', return_value=SimpleNamespace(returncode=0, stdout=command)), \
                    patch('native_test_login.load_inputs', side_effect=credential):
                with self.assertRaises(RuntimeError): qa.login(SimpleNamespace(admin_credential='unused', web_api_config='unused'))
            self.assertFalse((state / 'window.json').exists())

    def close_backend_case(self, mode):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            state = Path(name).resolve();qa.write_json(state / 'local-ready.json', expected())
            receipt = {'closed': True, 'pid': 44, 'proposalSha256': qa.PROPOSAL_SHA, 'runId': qa.RUN}
            if mode == 'wrong-pid': receipt['pid'] = 45
            if mode == 'wrong-proposal': receipt['proposalSha256'] = 'changed'
            if mode not in ['missing', 'watchdog']:
                qa.write_json(state / 'backend-closed.json', receipt)
            if mode == 'failure': qa.write_json(state / 'failure.json', {'status': 'FAILED'})
            elapsed, calls, killed = [0.0], [0], []
            command = ('java -cp cache com.tellpal.v2.support.P4ConsumerQaSetup --execute ' + str(state)).encode()
            def ps(*args, **kwargs):
                calls[0] += 1
                if mode == 'watchdog': alive = not killed
                elif mode == 'drain': alive = calls[0] < 3
                elif mode == 'foreign': return SimpleNamespace(returncode=0, stdout=b'java unrelated')
                else: alive = False
                return SimpleNamespace(returncode=0 if alive else 1, stdout=command if alive else b'')
            with patch.object(qa.subprocess, 'run', side_effect=ps), patch.object(qa.os, 'kill', side_effect=lambda pid, sig: killed.append((pid, sig))), \
                    patch.object(qa.time, 'monotonic', side_effect=lambda: elapsed[0]), patch.object(qa.time, 'sleep', side_effect=lambda seconds: elapsed.__setitem__(0, elapsed[0] + seconds)):
                if mode in ['drain', 'already-drained']:
                    self.assertEqual(qa.close_backend(state), 'DRAINED_MATCHED_RECEIPT_AND_PROCESS_EXIT')
                else:
                    with self.assertRaises(ValueError): qa.close_backend(state)
            if mode == 'watchdog': self.assertEqual([pid for pid, sig in killed], [44])
            else: self.assertEqual(killed, [])
            return calls[0]

    def test_backend_cleanup_requires_matched_closed_receipt_and_observed_process_exit(self):
        self.assertGreaterEqual(self.close_backend_case('drain'), 3)
        self.close_backend_case('already-drained')

    def test_backend_crash_missing_or_wrong_receipt_and_watchdog_are_failures(self):
        for mode in ['missing', 'wrong-pid', 'wrong-proposal', 'failure', 'watchdog', 'foreign']:
            with self.subTest(mode=mode): self.close_backend_case(mode)

    def test_replaced_qa_pid_or_mapping_is_preserved(self):
        from types import SimpleNamespace
        android = SimpleNamespace(pid=lambda: '99', run=lambda *args: 'emulator-5554 tcp:45678 tcp:9999')
        with self.assertRaisesRegex(ValueError, 'QA_OWNERSHIP_CHANGED'): qa.close_owned_qa(android, connection())
        with self.assertRaisesRegex(ValueError, 'FORWARD_OWNERSHIP_CHANGED'): qa.remove_owned_forward(android, connection())


class ArmLaunchRaceTest(unittest.TestCase):
    def exercise_launch(self, mode, manual=False):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();(root / 'android').mkdir()
            build = root / 'build';build.mkdir();(build / 'consumer.apk').write_bytes(b'fake-apk')
            apk_sha = qa.digest(build / 'consumer.apk')
            receipt = {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE, 'apkSha256': apk_sha,
                'sourceHashes': {}, 'buildName': 'build-0002' if manual else 'build-0001'}
            original_sha = 'unused'
            if manual:
                qa.write_json(root / 'android/arm-failure.private.json', {'status': 'ORIGINAL_FAILURE'})
                original_sha = qa.digest(root / 'android/arm-failure.private.json')
                qa.write_json(root / 'android/local-waiting-approval-01.private.json', {
                    'proposalSha256': qa.PROPOSAL_SHA, 'originalArmFailureSha256': original_sha,
                    'decision': 'A', 'localWaitingOnly': True, 'maxStarts': 1,
                    'buildName': 'build-0002', 'apkSha256': apk_sha})
            clock = [0.0]
            trace = [];started = [False];pid_reads = [0];post_cancel_reads = [0];stop_calls = [0];alive = [False];mapped = [False]
            intent_name = 'arm-launch-intent-02.private.json' if manual else 'arm-launch-intent.private.json'
            self_case = self
            class Android:
                def preflight(self): trace.append('preflight');return {'serial': 'emulator-5554'}
                def pid(self, **kwargs):
                    trace.append(('pid', clock[0]))
                    if not started[0]: return ''
                    if stop_calls[0]:
                        post_cancel_reads[0] += 1
                        if stop_calls[0] == 1 and post_cancel_reads[0] == 2:
                            alive[0] = True
                            trace.append(('late_cleanup_start', 13943))
                        return '13943' if alive[0] else ''
                    pid_reads[0] += 1
                    if mode == 'late-pid' and pid_reads[0] >= 2:
                        alive[0] = True
                    return '13943' if alive[0] else ''
                def process_identity(self, pid, **kwargs):
                    self_case.assertEqual(pid, 13943)
                    return {'pid': pid, 'processStartTicks': '777777'}
                def run(self, *args, **kwargs):
                    trace.append(args)
                    if args[:3] == ('shell', 'am', 'start'):
                        self_case.assertTrue((root / 'android' / intent_name).is_file())
                        proof = json.loads((root / 'android' / intent_name).read_bytes())
                        self_case.assertEqual(proof['apkSha256'], apk_sha)
                        self_case.assertTrue(proof['initialNoPid'] and proof['installCompleted'])
                        self_case.assertEqual(proof['amStartComponent'], qa.PACKAGE + '/' + qa.ACTIVITY)
                        if manual: self_case.assertTrue((root / 'android/local-waiting-reservation-01.private.json').is_file())
                        started[0] = True
                    if args[:1] == ('logcat',):
                        return 'Dart VM service is listening on http://127.0.0.1:12345/private_nonce/'
                    if args == ('forward', '--list'):
                        return 'emulator-5554 tcp:53421 tcp:39447\n' + ('emulator-5554 tcp:45678 tcp:12345\n' if mapped[0] else '')
                    if args == ('forward', '--no-rebind', 'tcp:45678', 'tcp:12345'): mapped[0] = True
                    if args == ('shell', 'am', 'force-stop', qa.PACKAGE):
                        stop_calls[0] += 1
                        if mode != 'stuck-cleanup': alive[0] = False
                    return ''
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'selected_build', return_value=(receipt, build)), \
                    patch.object(qa, 'sources', return_value={}), patch.object(qa, 'available_host_port', return_value=45678), \
                    patch.object(qa, 'WAITING_FAILURE_SHA', original_sha), patch.object(qa, 'WAITING_APK_SHA', apk_sha), \
                    patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), \
                    patch.object(qa.time, 'sleep', side_effect=lambda seconds: clock.__setitem__(0, clock[0] + seconds)):
                if mode == 'late-pid':
                    qa.arm(SimpleNamespace(), Android())
                    connection = json.loads((root / 'android/connection.private.json').read_bytes())
                    self.assertEqual((connection['pid'], connection['processStartTicks']), (13943, '777777'))
                    self.assertLess(clock[0], 30)
                    self.assertEqual(stop_calls[0], 0)
                    if manual:
                        before = len(trace)
                        with self.assertRaisesRegex(ValueError, 'LOCAL_WAITING_START_ALREADY_RESERVED'):
                            qa.arm(SimpleNamespace(), Android())
                        self.assertEqual(len(trace), before)
                        self.assertEqual(qa.digest(root / 'android/arm-failure.private.json'), original_sha)
                        self.assertFalse((root / 'android/arm-failure-02.private.json').exists())
                else:
                    with self.assertRaisesRegex(ValueError, 'ARM_PID_DEADLINE'):
                        qa.arm(SimpleNamespace(), Android())
                    self.assertGreaterEqual(clock[0], 29.9)
                    self.assertLessEqual(clock[0], 32.01)
                    failure = json.loads((root / 'android/arm-failure.private.json').read_bytes())
                    self.assertEqual(failure['code'], 'ARM_PID_DEADLINE')
                    self.assertIn(('late_cleanup_start', 13943), trace)
                    self.assertEqual(stop_calls[0], 2)
                    if mode == 'late-cleanup':
                        result = failure['cleanup']['qaProcess']
                        self.assertEqual(result['status'], 'PASS')
                        self.assertEqual(result['result']['observedProcesses'], [{'pid': 13943, 'processStartTicks': '777777'}])
                        self.assertTrue(result['result']['queuedStartCancelled'] and result['result']['absenceConfirmed'])
                        self.assertFalse(alive[0])
                    else:
                        self.assertEqual(failure['cleanup']['qaProcess']['status'], 'FAILED')
                        self.assertEqual(failure['cleanup']['qaProcess']['code'], 'ARM_CLEANUP_DEADLINE')
                    self.assertFalse((root / 'android/connection.private.json').exists())
                    self.assertFalse(mapped[0])
            self.assertEqual(sum(isinstance(row, tuple) and row[:3] == ('shell', 'am', 'start') for row in trace), 1)
            self.assertNotIn(('forward', '--remove', 'tcp:53421'), trace)
            self.assertFalse((root / 'backend/window.json').exists())
            self.assertFalse((root / 'backend/login.reservation.private.json').exists())

    def test_single_launch_observes_empty_pid_then_late_pid_inside_same_vm_deadline(self):
        self.exercise_launch('late-pid')

    def test_deadline_failure_cancels_queued_late_process_and_bounds_non_drain_failure(self):
        for mode in ['late-cleanup', 'stuck-cleanup']:
            with self.subTest(mode=mode): self.exercise_launch(mode)

    def test_old_failure_without_manual_approval_refuses_before_any_device_call(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();(root / 'android').mkdir()
            qa.write_json(root / 'android/arm-failure.private.json', {'status': 'ORIGINAL_FAILURE'})
            before = qa.digest(root / 'android/arm-failure.private.json')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'selected_build') as select:
                with self.assertRaisesRegex(ValueError, 'ARM_PREVIOUS_FAILURE_NO_RETRY'):
                    qa.arm(SimpleNamespace(), SimpleNamespace())
                select.assert_not_called()
            self.assertEqual(qa.digest(root / 'android/arm-failure.private.json'), before)
            self.assertFalse((root / 'android/local-waiting-reservation-01.private.json').exists())

    def test_manual_approval_is_single_local_waiting_launch_bound_to_old_failure_and_apk(self):
        self.exercise_launch('late-pid', manual=True)

    def test_manual_approval_rejects_wrong_identity_start_count_or_nonwaiting_scope(self):
        for changed in [{'originalArmFailureSha256': 'changed'}, {'maxStarts': 2}, {'maxStarts': 1.0},
                {'localWaitingOnly': False}, {'apkSha256': 'changed'}]:
            with self.subTest(changed=changed), tempfile.TemporaryDirectory() as name:
                root = Path(name).resolve();(root / 'android').mkdir()
                qa.write_json(root / 'android/arm-failure.private.json', {'status': 'ORIGINAL_FAILURE'})
                original_sha = qa.digest(root / 'android/arm-failure.private.json')
                qa.write_json(root / 'android/local-waiting-approval-01.private.json', {
                    'proposalSha256': qa.PROPOSAL_SHA, 'originalArmFailureSha256': original_sha,
                    'decision': 'A', 'localWaitingOnly': True, 'maxStarts': 1,
                    'buildName': 'build-0002', 'apkSha256': qa.WAITING_APK_SHA, **changed})
                with patch.object(qa, 'WAITING_FAILURE_SHA', original_sha):
                    with self.assertRaisesRegex(ValueError, 'LOCAL_WAITING_APPROVAL_REQUIRED'):
                        qa.waiting_arm_scope(root)
                self.assertFalse((root / 'android/local-waiting-reservation-01.private.json').exists())
                self.assertEqual(qa.digest(root / 'android/arm-failure.private.json'), original_sha)


class AndroidLaunchBoundaryTest(unittest.TestCase):
    def launch_proof(self, root):
        (root / 'android').mkdir()
        proof = {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE,
            'serial': 'emulator-5554', 'initialNoPid': True, 'installCompleted': True,
            'amStartComponent': qa.PACKAGE + '/' + qa.ACTIVITY, 'apkSha256': 'a' * 64}
        qa.write_json(root / 'android/arm-launch-intent.private.json', proof)
        return proof

    def test_replaced_pid_or_start_ticks_refuses_before_any_force_stop(self):
        from types import SimpleNamespace
        for current in [{'pid': 23, 'processStartTicks': '123456'},
                {'pid': 22, 'processStartTicks': '999999'}]:
            with self.subTest(current=current), tempfile.TemporaryDirectory() as name:
                root = Path(name).resolve();proof = self.launch_proof(root)
                stopped = []
                android = SimpleNamespace(pid=lambda **kw: str(current['pid']),
                    process_identity=lambda pid, **kw: current,
                    run=lambda *args, **kw: stopped.append(args))
                with self.assertRaisesRegex(ValueError, 'QA_PROCESS_REPLACED'):
                    qa.cancel_owned_launch(android, root, proof,
                        {'pid': 22, 'processStartTicks': '123456'}, 'arm-launch-intent.private.json')
                self.assertEqual(stopped, [])

    def test_real_android_pid_cmdline_stat_share_last_startup_tenth_second(self):
        from types import SimpleNamespace
        clock = [29.9];calls = []
        def boundary(args, timeout, **kwargs):
            calls.append((args[3:], timeout))
            if args[3:5] == ['shell', 'pidof']:
                clock[0] += .02;output = b'22'
            elif args[-1].endswith('/cmdline'):
                clock[0] += .03;output = qa.PACKAGE.encode() + b'\x00'
            else:
                clock[0] += .01;output = ('22 (qa) ' + ' '.join(['0'] * 19 + ['123456'])).encode()
            return SimpleNamespace(returncode=0, stdout=output)
        with patch.object(qa.subprocess, 'run', side_effect=boundary), \
                patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]):
            pid, owned = qa.observe_launch_pid(qa.Android('/mock/adb', 'emulator-5554'), 30)
        self.assertEqual((pid, owned), ('22', {'pid': 22, 'processStartTicks': '123456'}))
        self.assertEqual(len(calls), 3)
        for actual, expected in zip([call[1] for call in calls], [.1, .08, .05]):
            self.assertAlmostEqual(actual, expected)
        self.assertLess(clock[0], 30)

    def test_real_android_blocked_pid_consumes_only_remaining_startup_budget(self):
        clock = [29.9];timeouts = []
        def boundary(args, timeout, **kwargs):
            timeouts.append(timeout);clock[0] += timeout
            raise qa.subprocess.TimeoutExpired(args, timeout)
        with patch.object(qa.subprocess, 'run', side_effect=boundary), \
                patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]):
            with self.assertRaises(qa.subprocess.TimeoutExpired):
                qa.observe_launch_pid(qa.Android('/mock/adb', 'emulator-5554'), 30)
        self.assertEqual(len(timeouts), 1)
        self.assertAlmostEqual(timeouts[0], .1)
        self.assertAlmostEqual(clock[0], 30)

    def test_real_android_cleanup_identity_and_stop_share_two_second_budget(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = Path(name).resolve();proof = self.launch_proof(root)
            clock = [30.0];calls = []
            def boundary(args, timeout, **kwargs):
                calls.append((args[3:], timeout))
                if args[3:5] == ['shell', 'pidof']:
                    clock[0] += 1.8;output = b'22'
                elif args[-1].endswith('/cmdline'):
                    clock[0] += .04;output = qa.PACKAGE.encode() + b'\x00'
                elif args[-1].endswith('/stat'):
                    clock[0] += .06;output = ('22 (qa) ' + ' '.join(['0'] * 19 + ['123456'])).encode()
                else:
                    clock[0] += timeout
                    raise qa.subprocess.TimeoutExpired(args, timeout)
                return SimpleNamespace(returncode=0, stdout=output)
            with patch.object(qa.subprocess, 'run', side_effect=boundary), \
                    patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]):
                with self.assertRaises(qa.subprocess.TimeoutExpired):
                    qa.cancel_owned_launch(qa.Android('/mock/adb', 'emulator-5554'), root, proof,
                        {'pid': 22, 'processStartTicks': '123456'}, 'arm-launch-intent.private.json')
            self.assertEqual(calls[-1][0], ['shell', 'am', 'force-stop', qa.PACKAGE])
            for actual, expected in zip([call[1] for call in calls], [2, .2, .16, .1]):
                self.assertAlmostEqual(actual, expected)
            self.assertEqual(len(calls), 4)
            self.assertAlmostEqual(clock[0], 32)


class OwnedQaClosureAdapterTest(unittest.TestCase):
    def exercise(self, mode):
        from types import SimpleNamespace
        clock = [100.0];trace = [];stopped = [False];observations = [0];stop_time = [None];identity_exit = [False]
        def process(argv, timeout, **kwargs):
            args = argv[3:];trace.append((args, timeout))
            duration = min(.005, timeout);clock[0] += duration
            if duration < .005: raise qa.subprocess.TimeoutExpired(argv, timeout)
            if args == ['shell', 'pidof', qa.PACKAGE]:
                pid = '22'
                if stopped[0]:
                    observations[0] += 1
                    if identity_exit[0]:
                        if mode in ['exit-cmdline', 'exit-stat']: pid = ''
                        if mode == 'exit-replacement': pid = '99'
                        if mode == 'exit-query-failure': return SimpleNamespace(returncode=2, stdout=b'')
                    if mode == 'delayed' and observations[0] >= 3: pid = ''
                    if mode == 'replacement-pid': pid = '99'
                return SimpleNamespace(returncode=0 if pid else 1, stdout=pid.encode())
            if args[-1:] == ['/proc/22/cmdline']:
                if stopped[0] and mode in ['exit-cmdline', 'exit-still-alive', 'exit-replacement', 'exit-query-failure']:
                    identity_exit[0] = True
                    return SimpleNamespace(returncode=1, stdout=b'')
                package = 'synthetic-other-package' if stopped[0] and mode == 'replacement-command' else qa.PACKAGE
                return SimpleNamespace(returncode=0, stdout=package.encode() + b'\x00')
            if args[-1:] == ['/proc/22/stat']:
                if stopped[0] and mode == 'exit-stat':
                    identity_exit[0] = True
                    return SimpleNamespace(returncode=1, stdout=b'')
                ticks = '999999' if stopped[0] and mode == 'replacement-ticks' else '123456'
                return SimpleNamespace(returncode=0, stdout=('22 (qa) ' + ' '.join(['0'] * 19 + [ticks])).encode())
            self.assertEqual(args, ['shell', 'am', 'force-stop', qa.PACKAGE])
            stopped[0] = True;stop_time[0] = clock[0]
            return SimpleNamespace(returncode=0, stdout=b'')
        with patch.object(qa.subprocess, 'run', side_effect=process), \
                patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), \
                patch.object(qa.time, 'sleep', side_effect=lambda seconds: clock.__setitem__(0, clock[0] + seconds)):
            if mode in ['delayed', 'exit-cmdline', 'exit-stat']:
                self.assertEqual(qa.close_owned_qa(qa.Android('/mock/adb', 'emulator-5554'), connection()), 'STOPPED_OWNED_QA')
                if mode == 'delayed': self.assertGreaterEqual(observations[0], 3)
                else:
                    self.assertEqual(observations[0], 2)
                    self.assertEqual(trace[-1][0], ['shell', 'pidof', qa.PACKAGE])
            else:
                with self.assertRaises(ValueError): qa.close_owned_qa(qa.Android('/mock/adb', 'emulator-5554'), connection())
        self.assertEqual(sum(args == ['shell', 'am', 'force-stop', qa.PACKAGE] for args, timeout in trace), 1)
        self.assertLessEqual(clock[0] - stop_time[0], 2.00001)
        post_stop = False
        for args, timeout in trace:
            if args == ['shell', 'am', 'force-stop', qa.PACKAGE]: post_stop = True;continue
            if post_stop: self.assertLessEqual(timeout, 2)
        self.assertFalse(any('/proc/99/' in str(args) for args, timeout in trace))

    def test_real_adapter_waits_for_delayed_exit_after_one_stop(self):
        self.exercise('delayed')

    def test_real_adapter_still_live_after_bounded_wait_is_failure(self):
        self.exercise('live')

    def test_real_adapter_proc_exit_during_cmdline_or_stat_requires_fresh_absence(self):
        for mode in ['exit-cmdline', 'exit-stat']:
            with self.subTest(mode=mode): self.exercise(mode)

    def test_real_adapter_replacement_never_receives_a_second_stop(self):
        for mode in ['replacement-pid', 'replacement-ticks', 'replacement-command', 'exit-still-alive', 'exit-replacement', 'exit-query-failure']:
            with self.subTest(mode=mode): self.exercise(mode)


class LocalRuntimeRecoveryTest(unittest.TestCase):
    def test_installed_apk_path_accepts_android_tilde_directory_and_rejects_split_or_shell_input(self):
        path = '/data/app/~~opaque_nonce==/com.tellpal.example-opaque_nonce==/base.apk'
        self.assertEqual(qa.installed_apk_path('package:' + path + '\n'), path)
        for value in ['package:' + path + '\npackage:/data/app/other/split_config.arm64.apk',
                'package:/data/app/synthetic/split_config.arm64.apk', 'package:/data/app/a b/base.apk',
                'package:/data/app/$(id)/base.apk', 'package:/data/app/a;id/base.apk',
                'package:/data/app/`id`/base.apk', 'package:/data/app/a\\nb/base.apk',
                ' package:' + path, 'package:/data/app/../synthetic/base.apk', 'package:/data/app/./synthetic/base.apk']:
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, 'INSTALLED_QA_APK_PATH_REQUIRED'):
                qa.installed_apk_path(value)
    def fixture(self, root):
        for name in ['android', 'backend', 'backend-02', 'receipts']:
            (root / name).mkdir(mode=0o700)
        zero = ('OPEN_PENDING ' + qa.PROPOSAL_SHA + ' ' + qa.TERMINAL_SHA + ' 11068\n'
            'PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086\nCLOSED 0 0 0 0 0 0\n')
        for name in qa.RECOVERY_HASHES:
            if name != 'recovery-protected-before.private.json':
                qa.write_new(root / name, zero.encode() if name == 'backend/ledger.log' else b'{}\n')
        sealed = {name: qa.digest(root / name) for name in qa.RECOVERY_HASHES if name != 'recovery-protected-before.private.json'}
        qa.write_json(root / 'recovery-protected-before.private.json', {'files': sealed})
        hashes = {name: qa.digest(root / name) for name in qa.RECOVERY_HASHES}
        self.addCleanup(patch.stopall)
        patch.object(qa, 'RECOVERY_HASHES', hashes).start()
        qa.write_json(root / qa.RECOVERY_APPROVAL, qa.recovery_expected())
        qa.write_json(root / 'backend-02/inputs.private.json', {'localDbUsername': 'gurhankucuk'})
        return root

    def reserve(self, root):
        from types import SimpleNamespace
        with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'):
            qa.reserve_backend(SimpleNamespace())

    def claim(self, root):
        sha = qa.digest(root / qa.RECOVERY_APPROVAL)
        qa.write_json(root / qa.RECOVERY_QA_RESERVATION, qa.recovery_reservation(sha, 'QA_START'))
        qa.write_json(root / 'backend-02/startup.claim.private.json', {'recoveryApprovalSha256': sha,
            'startupReservationSha256': qa.digest(root / 'backend-02/startup.reservation.private.json'), 'pid': 44})

    def test_python_and_java_share_exact_recovery_approval_fixture(self):
        self.assertEqual(qa.recovery_expected(), json.loads((FIXTURES / 'consumer-recovery-approval.json').read_bytes()))

    def test_missing_or_wrong_approval_rejects_before_any_device_or_backend_start(self):
        from types import SimpleNamespace
        for change in [None, {'decision': 'B'}, {'additionalQaStarts': 2}, {'additionalQaStarts': True},
                {'additionalBackendStarts': 1.0}, {'localDbUsername': 'tellpal'}, {'measurementCeilingsChanged': True}]:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as name:
                root = self.fixture(Path(name).resolve())
                (root / qa.RECOVERY_APPROVAL).unlink()
                if change is not None: qa.write_json(root / qa.RECOVERY_APPROVAL, {**qa.recovery_expected(), **change})
                android = unittest.mock.Mock()
                with patch.object(qa, 'state_root', return_value=root):
                    with self.assertRaises((ValueError, FileNotFoundError)): qa.arm(SimpleNamespace(), android)
                    with self.assertRaises((ValueError, FileNotFoundError)): qa.reserve_backend(SimpleNamespace())
                android.assert_not_called();self.assertEqual(android.method_calls, [])
                self.assertFalse((root / qa.RECOVERY_QA_RESERVATION).exists())
                self.assertFalse((root / 'backend-02/startup.reservation.private.json').exists())

    def test_each_original_evidence_and_sealed_file_is_immutable(self):
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve())
            qa.recovery_gate(root)
            for relative in qa.RECOVERY_HASHES:
                path = root / relative;original = path.read_bytes()
                path.write_bytes(original + b'changed')
                with self.subTest(relative=relative), self.assertRaises(ValueError): qa.recovery_gate(root)
                path.write_bytes(original)
            (root / 'backend/failure.json').unlink()
            self.assertTrue(qa.recovery_selected(root))
            with self.assertRaises(ValueError): qa.waiting_arm_scope(root)

    def test_nonzero_ledger_or_window_cannot_be_rebound_as_recovery_evidence(self):
        for change in ['spend', 'window', 'unclosed']:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as name:
                root = self.fixture(Path(name).resolve());path = root / 'backend/ledger.log'
                original = path.read_text()
                path.write_text(original.replace('CLOSED 0', 'CLOSED 1') if change == 'spend' else
                    original + 'WINDOW_STARTED 1 180001\n' if change == 'window' else original.replace('CLOSED 0 0 0 0 0 0\n', ''))
                qa.RECOVERY_HASHES['backend/ledger.log'] = qa.digest(path)
                seal_path = root / 'recovery-protected-before.private.json'
                sealed = json.loads(seal_path.read_bytes());sealed['files']['backend/ledger.log'] = qa.digest(path)
                seal_path.write_text(json.dumps(sealed))
                qa.RECOVERY_HASHES['recovery-protected-before.private.json'] = qa.digest(seal_path)
                with self.assertRaisesRegex(ValueError, 'RECOVERY_PRIOR_SPEND_OR_WINDOW'): qa.recovery_gate(root)

    def test_backend_reservation_requires_explicit_db_user_and_is_exclusive_before_jvm(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve());inputs = root / 'backend-02/inputs.private.json'
            for value in [{}, {'localDbUsername': 'tellpal'}]:
                inputs.write_text(json.dumps(value))
                with self.assertRaisesRegex(ValueError, 'EXPLICIT_LOCAL_DB_USER_REQUIRED'): self.reserve(root)
                self.assertFalse((root / 'backend-02/startup.reservation.private.json').exists())
            inputs.write_text('{"localDbUsername":"gurhankucuk"}')
            with patch.object(qa.subprocess, 'run', side_effect=AssertionError):
                self.reserve(root)
                self.assertFalse((root / qa.RECOVERY_QA_RESERVATION).exists())
                with self.assertRaises(FileExistsError): self.reserve(root)
            self.assertEqual(json.loads((root / 'backend-02/startup.reservation.private.json').read_bytes())['kind'], 'BACKEND_START')
            self.assertFalse((root / 'backend-02/startup.claim.private.json').exists())

    def test_auth_or_window_in_either_backend_refuses_additional_local_starts(self):
        for folder in ['backend', 'backend-02']:
            for filename in ['login.reservation.private.json', 'qa-sign-in.ready.json', 'window.json', 'open.request']:
                with self.subTest(folder=folder, filename=filename), tempfile.TemporaryDirectory() as name:
                    root = self.fixture(Path(name).resolve());qa.write_new(root / folder / filename, b'{}')
                    with self.assertRaisesRegex(ValueError, 'RECOVERY_AFTER_AUTH_FORBIDDEN'): qa.waiting_arm_scope(root)
                    with self.assertRaisesRegex(ValueError, 'RECOVERY_AFTER_AUTH_FORBIDDEN'): self.reserve(root)

    def test_a_new_qa_or_backend_failure_cannot_spend_the_other_unused_start(self):
        for filename in ['android/arm-failure-03.private.json', 'backend-02/failure.json',
                'backend-02/backend-closed.json', 'backend-02/close.request']:
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as name:
                root = self.fixture(Path(name).resolve());qa.write_new(root / filename, b'{}')
                with self.assertRaisesRegex(ValueError, 'RECOVERY_ALREADY_FAILED_OR_CLOSED'): qa.waiting_arm_scope(root)
                with self.assertRaisesRegex(ValueError, 'RECOVERY_ALREADY_FAILED_OR_CLOSED'): self.reserve(root)

    def test_active_paths_require_both_separate_reservations_and_matched_jvm_claim(self):
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve());self.reserve(root)
            with self.assertRaises(FileNotFoundError): qa.active_paths(root)
            self.claim(root)
            state, path = qa.active_paths(root)
            self.assertEqual((state.name, path.name), ('backend-02', 'connection-03.private.json'))
            qa.validate_backend_claim(state, expected())
            with self.assertRaisesRegex(ValueError, 'BACKEND_CLAIM_PID_CHANGED'): qa.validate_backend_claim(state, {**expected(), 'pid': 45})
            for target, kind in [(root / qa.RECOVERY_QA_RESERVATION, 'BACKEND_START'),
                    (state / 'startup.reservation.private.json', 'QA_START')]:
                before = target.read_bytes();target.write_text(json.dumps(qa.recovery_reservation(qa.digest(root / qa.RECOVERY_APPROVAL), kind)))
                with self.assertRaisesRegex(ValueError, 'RECOVERY_RESERVATION_CHANGED'): qa.active_paths(root)
                target.write_bytes(before)
            self.assertTrue((root / 'android/connection.private.json').exists())

    def test_recovery_connection_is_bound_to_new_approval_reservation_and_same_apk(self):
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve())
            qa.write_json(root / qa.RECOVERY_QA_RESERVATION,
                qa.recovery_reservation(qa.digest(root / qa.RECOVERY_APPROVAL), 'QA_START'))
            value = {**connection(), 'buildName': 'build-0002', 'apkSha256': qa.WAITING_APK_SHA,
                'recoveryApprovalSha256': qa.digest(root / qa.RECOVERY_APPROVAL),
                'qaReservationSha256': qa.digest(root / qa.RECOVERY_QA_RESERVATION)}
            path = root / 'android/connection-03.private.json';qa.write_json(path, value)
            self.assertEqual(qa.connection_identity(root), value)
            self.assertFalse((root / 'backend-02/startup.reservation.private.json').exists())
            with self.assertRaises(FileNotFoundError): qa.active_paths(root)
            for changed in [{'recoveryApprovalSha256': 'other'}, {'qaReservationSha256': 'other'},
                    {'buildName': 'build-0001'}, {'apkSha256': 'a' * 64}]:
                path.write_text(json.dumps({**value, **changed}))
                with self.subTest(changed=changed), self.assertRaisesRegex(ValueError, 'RECOVERY_CONNECTION_CHANGED'):
                    qa.connection_identity(root)

    def test_recovery_login_reserves_only_new_backend_and_uses_new_owned_connection(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve());self.reserve(root);self.claim(root)
            value = {**connection(), 'buildName': 'build-0002', 'apkSha256': qa.WAITING_APK_SHA,
                'recoveryApprovalSha256': qa.digest(root / qa.RECOVERY_APPROVAL),
                'qaReservationSha256': qa.digest(root / qa.RECOVERY_QA_RESERVATION)}
            qa.write_json(root / 'android/connection-03.private.json', value)
            state = root / 'backend-02';qa.write_json(state / 'local-ready.json', expected())
            android = unittest.mock.Mock();android.pid.return_value = '22'
            android.process_identity.return_value = {'pid': 22, 'processStartTicks': '123456'}
            receipt = {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE, 'apkSha256': qa.WAITING_APK_SHA, 'sourceHashes': {}}
            real_digest = qa.digest
            def artifact_digest(path): return qa.WAITING_APK_SHA if path == root / 'build/consumer.apk' else real_digest(path)
            def credential(admin, config, output):
                self.assertEqual(output, state / 'id-token.private')
                self.assertTrue((state / 'login.reservation.private.json').is_file())
                raise RuntimeError('SYNTHETIC_STOP_BEFORE_AUTH')
            with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'approval'), \
                    patch.object(qa, 'Android', return_value=android), patch.object(qa, 'selected_build', return_value=(receipt, root / 'build')), \
                    patch.object(qa, 'sources', return_value={}), patch.object(qa, 'digest', side_effect=artifact_digest), \
                    patch.object(qa, 'plan', return_value=({}, expected())), patch.object(qa.time, 'time', return_value=2000000001), \
                    patch.object(qa, 'backend_process_command', return_value=['java']), \
                    patch('native_test_login.load_inputs', side_effect=credential):
                with self.assertRaisesRegex(RuntimeError, 'SYNTHETIC_STOP_BEFORE_AUTH'):
                    qa.login(SimpleNamespace(adb='/unused', admin_credential='/unused', web_api_config='/unused'))
            self.assertFalse((root / 'backend/login.reservation.private.json').exists())
            self.assertEqual(android.process_identity.call_args.args, (22,))

    def test_recovery_arm_spends_before_device_calls_uses_existing_apk_and_never_reopens_old_connection(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as name:
            root = self.fixture(Path(name).resolve());self.reserve(root)
            build = root / 'build';build.mkdir();(build / 'consumer.apk').write_bytes(b'fake')
            apk_sha = qa.digest(build / 'consumer.apk')
            approval_path = root / qa.RECOVERY_APPROVAL
            started, mapped = [False], [False];trace = [];case = self
            class Android:
                def preflight(self):
                    case.assertTrue((root / qa.RECOVERY_QA_RESERVATION).exists())
                    trace.append('preflight');return {}
                def pid(self, **kw): return '22' if started[0] else ''
                def process_identity(self, pid, **kw): return {'pid': pid, 'processStartTicks': '123456'}
                def run(self, *args, **kw):
                    trace.append(args)
                    if args[:3] == ('shell', 'am', 'start'): started[0] = True
                    if args[:3] == ('shell', 'pm', 'path'): return 'package:/data/app/~~opaque_nonce==/com.tellpal.example-opaque_nonce==/base.apk'
                    if args[:2] == ('shell', 'sha256sum'):
                        case.assertEqual(args[2], '/data/app/~~opaque_nonce==/com.tellpal.example-opaque_nonce==/base.apk')
                        return apk_sha + '  ' + args[2]
                    if args[:1] == ('logcat',): return 'Dart VM service is listening on http://127.0.0.1:12345/private_nonce/'
                    if args[:2] == ('forward', '--no-rebind'): mapped[0] = True
                    if args == ('forward', '--list'): return 'emulator-5554 tcp:53421 tcp:39447\n'
                    return ''
            old = {n: (root / n).read_bytes() for n in qa.RECOVERY_HASHES}
            with patch.object(qa, 'WAITING_APK_SHA', apk_sha):
                approval_path.write_text(json.dumps(qa.recovery_expected()))
                receipt = {'proposalSha256': qa.PROPOSAL_SHA, 'package': qa.PACKAGE, 'buildName': 'build-0002',
                    'apkSha256': apk_sha, 'sourceHashes': {}}
                with patch.object(qa, 'state_root', return_value=root), patch.object(qa, 'selected_build', return_value=(receipt, build)), \
                        patch.object(qa, 'sources', return_value={}), patch.object(qa, 'available_host_port', return_value=45678):
                    qa.arm(SimpleNamespace(), Android())
                    with self.assertRaisesRegex(ValueError, 'LOCAL_RUNTIME_RECOVERY_QA_ALREADY_RESERVED'): qa.arm(SimpleNamespace(), Android())
            self.assertFalse(any(isinstance(call, tuple) and call[0] == 'install' for call in trace))
            self.assertEqual(sum(isinstance(call, tuple) and call[:3] == ('shell', 'am', 'start') for call in trace), 1)
            self.assertTrue((root / 'android/connection-03.private.json').is_file())
            self.assertFalse((root / 'android/arm-failure-03.private.json').exists())
            for filename, data in old.items(): self.assertEqual((root / filename).read_bytes(), data)


if __name__ == '__main__':
    unittest.main()
