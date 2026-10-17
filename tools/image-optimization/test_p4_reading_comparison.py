"""Mobil sürücü korumaları; testler cihaz/login/bulut çağrısı yapmaz."""
import base64
import contextlib
import io
import json
import os
from pathlib import Path
import tempfile
import types
import sys
import time
import unittest
from unittest.mock import patch, Mock
import p4_reading_comparison as p4


def token(**changes):
    claims = {'sub': p4.QA_UID, 'aud': 'tellpal-ee0dd',
        'iss': 'https://securetoken.google.com/tellpal-ee0dd',
        'exp': int(time.time()) + 3600, 'iat': int(time.time())}
    claims.update(changes)
    return 'synthetic.' + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=') + '.synthetic'


def ready():
    return {'runId': p4.RUN_ID, 'planSha256': p4.PLAN_SHA, 'contentId': 94, 'loopbackPort': 12456}


class LocalVm:
    def __init__(self):
        self.epoch = 1
        self.background = False
        self.stopped = False
        self.messages = []
    def ready(self):
        pass
    def close(self):
        pass
    def data(self, message):
        self.messages.append(dict(message))
        action = message['action']
        if action == 'select':
            self.epoch += 1
        if action == 'stop':
            self.stopped = True
        snapshot = {'ok': True, 'status': 'error' if self.background or self.stopped else 'loaded',
                    'stopped': self.stopped, 'activeImages': 0 if self.background or self.stopped else 1,
                    'activeBuffers': 0, 'activeDescriptors': 0, 'activeCodecs': 0,
                    'events': [{'kind': 'http', 'endpoint': 'bytes'} for _ in range(6)] +
                        [{'kind': 'state', 'status': 'loaded', 'epoch': self.epoch}]}
        if action == 'paint':
            snapshot['paintedEpoch'] = self.epoch
        return snapshot


@contextlib.contextmanager
def local_run():
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory).resolve()
        state, private = root/'state', root/'private'
        state.mkdir(mode=0o700); private.mkdir(mode=0o700)
        token_file = root/'token'
        p4.write_new(token_file, token().encode())
        p4.write_json(state/'ready.json', {**ready(), 'identity': {'contentId':94, 'language':'tr'}})
        p4.write_json(state/'qa-sign-in.ready.json', {'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,
                      'qaUid':p4.QA_UID,'tokenFile':str(token_file)})
        p4.write_json(private/'connection.private.json', {'pid':123,'hostPort':23456,
                      'websocket':'ws://127.0.0.1:23456/synthetic/ws','device':{},'apkSha256':'synthetic'})
        args=type('Args', (), {'output':root/'new-output','private_dir':private})()
        vm=LocalVm();android=Mock()
        def device(*arguments, **keywords):
            if arguments[:2] == ('shell','pidof'): return '123'
            if arguments == ('shell','input','keyevent','KEYCODE_HOME'): vm.background=True
            return ''
        android.run.side_effect=device
        android.memory.return_value={}
        android.screenshot.return_value='synthetic-sha'
        with patch.object(p4,'STATE',state),patch.object(p4,'Vm',return_value=vm) as vm_factory, \
             patch.object(p4,'validate_loaded',return_value={}),patch.object(p4.time,'sleep'):
            yield args,android,vm,vm_factory,state


class DriverGuardsTest(unittest.TestCase):
    def test_continuation_rejects_other_directory_before_device_io(self):
        with local_run() as (args,android,vm,vm_factory,state):
            args.state_dir=state/'other'
            with self.assertRaisesRegex(ValueError,'CONTINUATION_SCOPE_CHANGED'):
                p4.run(args,android)
            android.run.assert_not_called()
            vm_factory.assert_not_called()

    def test_continuation_rejects_changed_prior_ledger_before_device_io(self):
        with local_run() as (args,android,vm,vm_factory,state):
            child=state/'mobile-continuation';child.mkdir(mode=0o700)
            p4.write_new(state/'ledger.log',b'prior')
            p4.write_json(child/'approval.json',{'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,
                'contentId':94,'newProductionAllowed':False,'priorLedgerSha256':'wrong'})
            args.state_dir=child
            with self.assertRaisesRegex(ValueError,'CONTINUATION_APPROVAL_REQUIRED'):
                p4.run(args,android)
            android.run.assert_not_called()
            vm_factory.assert_not_called()

    @patch.object(p4.time,'time',return_value=1792270138.011)
    def test_continuation_rejects_changed_live_before_device_io(self, fixed_time):
        with local_run() as (args,android,vm,vm_factory,state):
            child=state/'mobile-continuation';child.mkdir(mode=0o700)
            p4.write_new(state/'ledger.log',b'prior')
            p4.write_json(child/'approval.json',{'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,
                'contentId':94,'newProductionAllowed':False,'priorLedgerSha256':p4.digest(state/'ledger.log')})
            p4.write_json(child/'ready.json',{**ready(),'contentId':95,'identity':{}})
            p4.write_new(child/'qa-sign-in.ready.json',(state/'qa-sign-in.ready.json').read_bytes())
            args.state_dir=child
            with self.assertRaisesRegex(ValueError,'CONTINUATION_LIVE_CHANGED'):
                p4.run(args,android)
            android.run.assert_not_called()
            vm_factory.assert_not_called()

    @patch.object(p4.time,'time',return_value=1792270138.011)
    def test_approved_continuation_preserves_parent_attempt_and_is_single_use(self, fixed_time):
        with local_run() as (args,android,vm,vm_factory,state):
            child=state/'mobile-continuation';child.mkdir(mode=0o700)
            p4.write_new(state/'ledger.log',b'prior')
            p4.write_json(state/'mobile.attempt.json',{'state':'FAILED_PRESERVED'})
            prior=(state/'mobile.attempt.json').read_bytes()
            parent=json.loads((state/'ready.json').read_bytes());parent['descriptor']={}
            (state/'ready.json').write_text(json.dumps(parent))
            p4.write_json(child/'approval.json',{'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,
                'contentId':94,'newProductionAllowed':False,'priorLedgerSha256':p4.digest(state/'ledger.log')})
            p4.write_json(child/'ready.json',parent)
            p4.write_new(child/'qa-sign-in.ready.json',(state/'qa-sign-in.ready.json').read_bytes())
            args.state_dir=child
            p4.run(args,android)
            self.assertTrue(json.loads((args.output/'attempt-result.json').read_bytes())['finished'])
            self.assertEqual((state/'mobile.attempt.json').read_bytes(),prior)
            self.assertEqual((state/'ledger.log').read_bytes(),b'prior')
            self.assertTrue((child/'mobile.attempt.json').exists())
            args.output=args.output.parent/'second-output'
            vm_factory.reset_mock();android.reset_mock()
            with self.assertRaisesRegex(ValueError,'ATTEMPT_EXISTS_NO_RETRY'):p4.run(args,android)
            vm_factory.assert_not_called()
            android.run.assert_not_called()

    def test_expired_continuation_refuses_before_android_or_vm(self):
        with local_run() as (args,android,vm,vm_factory,state):
            child=state/'mobile-continuation';child.mkdir(mode=0o700)
            p4.write_new(state/'ledger.log',b'prior')
            p4.write_json(child/'approval.json',{'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,
                'contentId':94,'newProductionAllowed':False,'priorLedgerSha256':p4.digest(state/'ledger.log')})
            args.state_dir=child
            with patch.object(p4.time,'time',return_value=1792270538.011),self.assertRaisesRegex(ValueError,'CONTINUATION_WINDOW_EXPIRED'):
                p4.run(args,android)
            android.run.assert_not_called();vm_factory.assert_not_called()
            self.assertFalse((child/'mobile.attempt.json').exists())

    def test_existing_mobile_attempt_refuses_new_output_before_reverse_or_vm(self):
        with local_run() as (args,android,vm,vm_factory,state):
            marker=state/'mobile.attempt.json'
            p4.write_json(marker, {'state':'FAILED_NO_RETRY'})
            original=marker.read_bytes()
            with self.assertRaises(FileExistsError):
                p4.run(args,android)
            vm_factory.assert_not_called()
            self.assertEqual(vm.messages, [])
            self.assertFalse(any(call.args[0] == 'reverse' for call in android.run.call_args_list))
            self.assertEqual(marker.read_bytes(), original)

    def test_late_closed_screenshot_failure_does_not_claim_finished(self):
        with local_run() as (args,android,vm,vm_factory,state):
            def screenshot(path):
                if path.name == '07-closed.png': raise OSError('synthetic')
                return 'synthetic-sha'
            android.screenshot.side_effect=screenshot
            with self.assertRaises(OSError):
                p4.run(args,android)
            result=json.loads((args.output/'attempt-result.json').read_bytes())
            self.assertEqual(result['statesRecorded'],6)
            self.assertFalse(result['finished'])
            self.assertFalse((args.output/'verification.json').exists())

    def test_verification_write_failure_cannot_be_reported_finished(self):
        with local_run() as (args,android,vm,vm_factory,state):
            actual=p4.write_json
            def write(path,value):
                if path.name=='verification.json': raise OSError('synthetic')
                actual(path,value)
            with patch.object(p4,'write_json',side_effect=write), self.assertRaises(OSError):
                p4.run(args,android)
            result=json.loads((args.output/'attempt-result.json').read_bytes())
            self.assertFalse(result['finished'])

    def test_reverse_cleanup_failure_preserves_forward_cleanup_and_final_receipt(self):
        with local_run() as (args,android,vm,vm_factory,state):
            original=android.run.side_effect
            def device(*arguments,**keywords):
                if arguments[:2] == ('reverse','--remove'): raise OSError('synthetic')
                return original(*arguments,**keywords)
            android.run.side_effect=device
            p4.run(args,android)
            result=json.loads((args.output/'attempt-result.json').read_bytes())
            self.assertTrue(result['finished'])
            self.assertEqual(result['cleanup']['reverse'],'FAILED')
            self.assertEqual(result['cleanup']['forward'],'PASS')
            android.run.assert_any_call('forward','--remove','tcp:23456')

    def test_wrong_painted_epoch_refuses_before_any_screenshot(self):
        with local_run() as (args,android,vm,vm_factory,state):
            original=vm.data
            def data(message):
                result=original(message)
                if message['action']=='paint': result['paintedEpoch']=999
                return result
            vm.data=data
            with self.assertRaisesRegex(ValueError,'PAINT_EPOCH_CHANGED'):
                p4.run(args,android)
            android.screenshot.assert_not_called()
            result=json.loads((args.output/'attempt-result.json').read_bytes())
            self.assertFalse(result['finished'])

    def test_default_does_not_read_plan_or_start_device_or_login(self):
        with patch.object(p4, 'plan', side_effect=AssertionError('file read')), \
                patch.object(p4, 'command', side_effect=AssertionError('process')), \
                contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(0, p4.main([]))
        self.assertIn('NO_LOGIN', output.getvalue())

    def test_approved_profile_and_exactly_six_states(self):
        plan = p4.plan()
        self.assertEqual(plan['geometry']['expectedOutput'], [822, 1334])
        self.assertEqual(len(p4.CHOICES), 6)
        self.assertEqual(len(set(p4.CHOICES)), 6)
        self.assertEqual(p4.CHOICES[0], (1, False))

    def test_identity_expiry_scope_and_old_content_refuse(self):
        config = p4.runtime_configuration(ready(), token())
        self.assertEqual(config['port'], 12456)
        for jwt in [token(sub='different'), token(aud='other'), token(exp=int(time.time()) + 100), token(iat=int(time.time()) + 3600)]:
            with self.assertRaises(ValueError):
                p4.runtime_configuration(ready(), jwt)
        for content in [71, 93, 112, -1, True]:
            with self.assertRaises(ValueError):
                p4.runtime_configuration({**ready(), 'contentId': content}, token())

    def test_secret_must_be_private_external_regular_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            path = root / 'token'
            p4.write_new(path, b'synthetic')
            self.assertEqual(p4.private_file(path, external=True), b'synthetic')
            os.chmod(path, 0o644)
            with self.assertRaises(ValueError):
                p4.private_file(path, external=True)
            os.chmod(path, 0o600)
            link = root / 'link'
            link.symlink_to(path)
            with self.assertRaises(ValueError):
                p4.private_file(link)

    def test_durable_reservation_is_exclusive_and_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'attempt.json'
            p4.write_json(path, {'state': 'STARTED'})
            with self.assertRaises(FileExistsError):
                p4.write_json(path, {'state': 'RESET'})
            self.assertEqual(json.loads(path.read_bytes())['state'], 'STARTED')

    def test_only_exact_emulator_is_accepted_and_changed_display_refuses(self):
        with self.assertRaises(ValueError):
            p4.Android('/explicit/adb', 'physical-device')
        device = p4.Android('/explicit/adb', 'emulator-5554')
        with patch.object(device, 'run', side_effect=['device', '1', 'Override size: 1080x2400']):
            with self.assertRaisesRegex(ValueError, 'DISPLAY_CHANGED'):
                device.preflight()

    def test_vm_uses_owned_direct_loopback_socket_even_with_environment_proxy(self):
        direct,websocket=Mock(),Mock()
        websocket.getstatus.return_value=101
        module=types.SimpleNamespace(create_connection=Mock(return_value=websocket))
        url='ws://127.0.0.1:12345/synthetic/ws'
        with patch.dict(os.environ,{'HTTP_PROXY':'http://proxy.invalid:8080','NO_PROXY':''}), \
             patch.dict(sys.modules,{'websocket':module}), \
             patch.object(p4.socket,'create_connection',return_value=direct) as connect:
            vm=p4.Vm(url)
            connect.assert_called_once_with(('127.0.0.1',12345),timeout=10)
            module.create_connection.assert_called_once_with(url,socket=direct,timeout=10,suppress_origin=True,redirect_limit=0)
            direct.close.assert_not_called()
            websocket.close.side_effect=OSError('synthetic_close')
            with self.assertRaises(OSError):
                vm.close()
            direct.close.assert_called_once()

    def test_vm_non_upgrade_handshake_is_closed_without_redirect(self):
        direct,websocket=Mock(),Mock()
        websocket.getstatus.return_value=302
        module=types.SimpleNamespace(create_connection=Mock(return_value=websocket))
        with patch.dict(sys.modules,{'websocket':module}), \
             patch.object(p4.socket,'create_connection',return_value=direct):
            with self.assertRaisesRegex(ValueError,'VM_HANDSHAKE_REFUSED'):
                p4.Vm('ws://127.0.0.1:12345/synthetic/ws')
            self.assertEqual(module.create_connection.call_args.kwargs['redirect_limit'],0)
            websocket.close.assert_called_once()
            direct.close.assert_called_once()

    def test_vm_handshake_failure_closes_only_the_owned_socket(self):
        direct=Mock()
        module=types.SimpleNamespace(create_connection=Mock(side_effect=OSError('synthetic_handshake')))
        with patch.dict(os.environ,{'HTTP_PROXY':'http://proxy.invalid:8080','NO_PROXY':''}), \
             patch.dict(sys.modules,{'websocket':module}), \
             patch.object(p4.socket,'create_connection',return_value=direct):
            with self.assertRaises(OSError):
                p4.Vm('ws://127.0.0.1:12345/synthetic/ws')
            direct.close.assert_called_once()

    def test_vm_connection_cannot_be_remote_or_http(self):
        for url in ['ws://remote.example/ws', 'wss://127.0.0.1/ws', 'ws://127.0.0.1/ws?token=synthetic']:
            with self.assertRaises(ValueError):
                p4.Vm(url)

    def test_loaded_evidence_requires_actual_payload_geometry_and_same_live(self):
        identity = {'contentId': 94, 'language': 'tr', 'revision': 'r', 'manifestDigest': 'd', 'jobId': 'j', 'liveGeneration': 1}
        state = {**identity, 'kind': 'state', 'status': 'loaded', 'entryIndex': 1, 'canonicalSource': False,
            'requiredWidth': 822, 'requiredHeight': 1334, 'decodedWidth': 822, 'decodedHeight': 1334,
            'role': 'readingDetail', 'pageNumber': 0, 'expectedPayloadBytes': 123,
            'activeBuffers': 0, 'activeDescriptors': 0, 'activeCodecs': 0, 'epoch': 7}
        approved = p4.plan()
        profile = approved['runtimePreflight']['roleFingerprints']['READING_DETAIL']
        source = approved['sources'][0]
        state.update({'sourceSha256': source['sha256'], 'displaySha256': 'e' * 64,
            'renditionKey': 'f' * 64, 'profileFingerprint': profile})
        payload = {'kind': 'http', 'endpoint': 'bytes', 'ok': True, 'payloadBytes': 123}
        snapshot = {'status': 'loaded', 'activeImages': 1, 'paintedEpoch': 7,
            'slot': {'logicalWidth': 411, 'logicalHeight': 456.66666666666663, 'dpr': 2},
            'events': [payload, state]}
        entry = {'source': {'raster': {'sha256': source['sha256'], 'byteSize': source['bytes']}},
            'renditions': [{'profileFingerprint': profile, 'renditionKey': 'f' * 64,
                'raster': {'sha256': 'e' * 64, 'byteSize': 123}}]}
        bound = {**ready(), 'identity': identity, 'descriptor': {'images': [entry] * 4}}
        for receipt in [None, 6]:
            with self.assertRaisesRegex(ValueError, 'PAINT_RECEIPT_REQUIRED'):
                p4.validate_loaded({**snapshot, 'paintedEpoch': receipt}, bound, 1, False)
        result = p4.validate_loaded(snapshot, bound, 1, False)
        self.assertEqual(result['httpPayload']['payloadBytes'], 123)
        for change in [{'payloadBytes': 124}, {'ok': False}]:
            with self.assertRaises(ValueError):
                p4.validate_loaded({**snapshot, 'events': [{**payload, **change}, state]}, bound, 1, False)
        with self.assertRaises(ValueError):
            p4.validate_loaded(snapshot, {**bound, 'identity': {**identity, 'revision': 'new'}}, 1, False)


if __name__ == '__main__':
    unittest.main()
