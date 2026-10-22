"""P5e sınırları; iOS kurulumu yapılmadan çalışan ret ve native günlük testleri."""
from datetime import datetime, timedelta
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import copy
import errno
from contextlib import ExitStack
import sys
from unittest.mock import patch

import p5e_iphone_offline as qa
from p5e_session_validation import validate_session


class P5eTests(unittest.TestCase):
    def profile(self):
        return {'TeamIdentifier': ['TEAM'], 'ExpirationDate': datetime.now() + timedelta(days=1),
                'ProvisionedDevices': [qa.DEVICE], 'Entitlements': {
                    'application-identifier': 'TEAM.' + qa.PACKAGE, 'get-task-allow': True}}

    def test_profile_exact_device_expiry_and_entitlements(self):
        qa.validate_profile(self.profile())
        mutations = [lambda p: p['Entitlements'].update({'application-identifier': 'TEAM.*'}),
                     lambda p: p.update({'ProvisionedDevices': ['other']}),
                     lambda p: p.update({'ExpirationDate': datetime.now() - timedelta(days=1)}),
                     lambda p: p['Entitlements'].update({'aps-environment': 'development'})]
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                profile = self.profile()
                mutate(profile)
                with self.assertRaises(ValueError):
                    qa.validate_profile(profile)

    def test_device_requires_exact_physical_wired_iphone12(self):
        hardware = {'udid': qa.DEVICE, 'marketingName': 'iPhone 12', 'reality': 'physical'}
        data = {'properties': {'hardware': hardware,
                              'connection': {'state': 'connected', 'transportType': 'wired'},
                              'state': {'bootState': 'booted'}}}
        qa.validate_device(data)
        for field, value in [('udid', 'other'), ('marketingName', 'iPhone 13'), ('reality', 'simulated')]:
            with self.subTest(field=field):
                changed = json.loads(json.dumps(data))
                changed['properties']['hardware'][field] = value
                with self.assertRaises(ValueError):
                    qa.validate_device(changed)
        data['properties']['connection']['transportType'] = 'network'
        with self.assertRaises(ValueError):
            qa.validate_device(data)

    def test_closure_excludes_normal_entry_firebase_http_and_p5d_cubit(self):
        closure = qa.source_closure()
        self.assertTrue(any('p5e_cubit.dart' in path for path in closure))
        self.assertTrue(any('p5d_manifest_repository.dart' in path for path in closure))
        self.assertFalse(any(path.endswith(('main.dart', 'p5d_cubit.dart')) for path in closure))
        for name in closure:
            text = (qa.MOBILE / name).read_text()
            self.assertNotIn('package:firebase', text)
            self.assertNotIn('package:http/', text)

    def test_insufficient_space_stops_before_prepare(self):
        with patch.object(qa.shutil, 'disk_usage', return_value=shutil._ntuple_diskusage(10, 9, 1)):
            with self.assertRaisesRegex(ValueError, 'space_insufficient'):
                qa.space(Path('.'))

    def test_offline_build_guard_and_no_deletion_commands(self):
        source = Path(qa.__file__).read_text()
        self.assertIn("'pub', 'get', '--offline'", source)
        self.assertIn("(deny network*)", qa.NETWORK_POLICY)
        self.assertNotIn('uninstall', source)
        self.assertNotIn('allowProvisioningUpdates', source)
        self.assertNotIn('rmtree', source)

    def test_offline_wrapper_starts_child_but_denies_local_socket_bind(self):
        if sys.platform != 'darwin' or not shutil.which('sandbox-exec'):
            self.skipTest('Mevcut macOS sandbox-exec yok')
        with tempfile.TemporaryDirectory(prefix='p5e-sandbox-test-') as temp:
            run = Path(temp)
            sentinel = run / 'child-started'
            script = """import errno,pathlib,socket,sys
pathlib.Path(sys.argv[1]).write_text('CHILD_STARTED')
try:
    sock=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    sock.bind(('127.0.0.1',0))
except OSError as error:
    print('SOCKET_REFUSED:'+str(error.errno),flush=True)
    sys.exit(0 if error.errno in (errno.EPERM,errno.EACCES) else 3)
else:
    sock.close()
    sys.exit(4)
"""
            output = qa.command([sys.executable, '-c', script, str(sentinel)], run, 'sandbox-bind', offline=True)
            self.assertEqual(sentinel.read_text(), 'CHILD_STARTED')
            self.assertIn(output.strip(), (f'SOCKET_REFUSED:{errno.EPERM}'.encode(), f'SOCKET_REFUSED:{errno.EACCES}'.encode()))

    def test_missing_sandbox_refuses_before_any_child_starts(self):
        with tempfile.TemporaryDirectory(prefix='p5e-no-sandbox-') as temp, \
             patch.object(qa.shutil, 'which', return_value=None), patch.object(qa.subprocess, 'run') as child:
            with self.assertRaisesRegex(ValueError, 'offline_guard_missing'):
                qa.command([sys.executable, '-c', 'raise RuntimeError()'], Path(temp), 'no-sandbox', offline=True)
            child.assert_not_called()
            self.assertEqual(list(Path(temp).iterdir()), [])

    def test_real_backup_inventory_rejects_changed_missing_and_empty_files(self):
        for mode in ('equal', 'same-size-different-bytes', 'missing', 'empty'):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory(prefix='p5e-backup-test-') as temp:
                run = Path(temp)
                def copy_from_device(output, label, args):
                    destination = Path(args[args.index('--destination') + 1])
                    destination.mkdir()
                    if mode != 'empty':
                        (destination / 'data.bin').write_bytes(b'abcd' if label == 'backup-first' or mode != 'same-size-different-bytes' else b'WXYZ')
                        if mode != 'missing' or label == 'backup-first':
                            (destination / 'other.bin').write_bytes(b'12')
                    return {}
                with patch.object(qa, 'device_json', side_effect=copy_from_device):
                    if mode == 'equal':
                        result = qa.backup(run)
                        self.assertTrue(result['verified'])
                        self.assertEqual((result['files'], result['bytes']), (2, 6))
                        inventory = json.loads((run / 'backup-inventory.private.json').read_bytes())
                        self.assertEqual(inventory['data.bin']['sha256'], qa.digest(b'abcd'))
                    else:
                        with self.assertRaisesRegex(ValueError, 'qa_backup_verification_refused'):
                            qa.backup(run)
                        self.assertFalse((run / 'backup-inventory.private.json').exists())

    def test_install_boundary_timeout_reports_unknown_and_native_response_is_required(self):
        for response in (subprocess.TimeoutExpired('devicectl', 60), OSError('transport_lost'), {},
                         {'installedApplications': [{'bundleID': 'other', 'installationURL': 'file:///P5eOffline.app/'}]}):
            with self.subTest(response=response):
                result = {'installation': 'NOT_RUN'}
                with patch.object(qa, 'device_json', side_effect=response if isinstance(response, Exception) else None,
                                  return_value=response):
                    with self.assertRaises((ValueError, OSError, subprocess.TimeoutExpired)):
                        qa.install_app(Path('/unused'), Path('/unused/P5eOffline.app'), result)
                self.assertEqual(result['installation'], 'OUTCOME_UNKNOWN')
        result = {'installation': 'NOT_RUN'}
        with patch.object(qa, 'device_json', return_value={'installedApplications': [
                {'bundleID': qa.PACKAGE, 'installationURL': 'file:///private/app/P5eOffline.app/'}]}):
            qa.install_app(Path('/unused'), Path('/unused/P5eOffline.app'), result)
        self.assertEqual(result['installation'], 'INSTALLED')

    def test_launch_options_precede_bundle_so_they_are_not_app_arguments(self):
        with tempfile.TemporaryDirectory(prefix='p5e-launch-arguments-') as temp:
            run = Path(temp)
            captured = []
            def command(argv, output, label, **kwargs):
                captured.append(argv)
                target = Path(argv[argv.index('--json-output') + 1])
                target.write_text(json.dumps({'result': {'process': {'processIdentifier': 123}}}))
            with patch.object(qa, 'command', side_effect=command):
                result = qa.device_json(run, 'launch', ['device', 'process', 'launch', qa.PACKAGE])
            argv = captured[0]
            self.assertEqual(argv[:5], ['xcrun', 'devicectl', 'device', 'process', 'launch'])
            self.assertEqual(argv[-1], qa.PACKAGE)
            self.assertLess(argv.index('--device'), argv.index(qa.PACKAGE))
            self.assertLess(argv.index('--timeout'), argv.index(qa.PACKAGE))
            self.assertLess(argv.index('--json-output'), argv.index(qa.PACKAGE))
            self.assertEqual(result['process']['processIdentifier'], 123)

    def test_locked_device_and_backup_mismatch_prevent_install(self):
        qa.validate_lock({'passcodeRequired': False, 'unlockedSinceBoot': True})
        for value in ({}, {'passcodeRequired': True, 'unlockedSinceBoot': True}):
            with self.assertRaisesRegex(ValueError, 'device_unlock_required'):
                qa.validate_lock(value)
        with tempfile.TemporaryDirectory(prefix='p5e-install-test-') as temp:
            root = Path(temp).resolve()
            run = root / 'p5e-iphone-aaaaaaaaaaaa'
            run.mkdir()
            app = run / 'derived-data/Build/Products/Release-iphoneos/P5eOffline.app'
            (run / 'result.private.json').write_text(json.dumps({'status': 'BUILD_VERIFIED',
                'installation': 'NOT_RUN', 'bundlePath': str(app), 'bundleInventory': {},
                'bundleAudit': {}, 'bundleBytes': 10}))
            (run / 'source-before.private.json').write_text(json.dumps({'repository': {}, 'inputs': {}}))
            (run / 'old-output-before.private.json').write_text('{}')
            (run / 'snapshot-binding.private.json').write_text('{}')
            calls = []
            def device(attempt, label, args):
                calls.append(args)
                return {'passcodeRequired': False}
            with patch.object(qa, 'QA_ROOT', root), patch.object(qa, 'preserved', return_value={'unchanged': True}), \
                 patch.object(qa, 'inventory', return_value={}), patch.object(qa, 'signing', return_value=(None, {}, None)), \
                 patch.object(qa, 'validate_bundle', return_value={}), patch.object(qa, 'space'), \
                 patch.object(qa, 'validate_device'), patch.object(qa, 'device_json', side_effect=device), \
                 patch.object(qa, 'command'), patch.object(qa, 'backup', side_effect=ValueError('qa_backup_verification_refused')):
                _, result = qa.install_verified(run)
            self.assertEqual(result['status'], 'INSTALL_FAILED')
            self.assertEqual(result['installation'], 'NOT_RUN')
            self.assertFalse(any(args[:3] == ['device', 'install', 'app'] for args in calls))

    def test_both_install_flows_keep_not_run_before_boundary_and_unknown_on_timeout(self):
        for flow in ('build-and-install', 'install-verified'):
            for failure in ('locked', 'install-timeout'):
                with self.subTest(flow=flow, failure=failure), tempfile.TemporaryDirectory(prefix='p5e-install-outcome-') as temp:
                    root = Path(temp).resolve()
                    run = root / 'p5e-iphone-bbbbbbbbbbbb'
                    run.mkdir()
                    app = run / 'derived-data/Build/Products/Release-iphoneos/P5eOffline.app'
                    (run / 'result.private.json').write_text(json.dumps({'status': 'BUILD_VERIFIED',
                        'installation': 'NOT_RUN', 'bundlePath': str(app), 'bundleInventory': {},
                        'bundleAudit': {}, 'bundleBytes': 10}))
                    (run / 'source-before.private.json').write_text(json.dumps({'repository': {}, 'inputs': {}}))
                    (run / 'old-output-before.private.json').write_text('{}')
                    (run / 'snapshot-binding.private.json').write_text('{}')
                    profile_file = root / 'profile'
                    profile_file.write_bytes(b'existing-profile')
                    boundary_calls = []
                    hardware = {'udid': qa.DEVICE, 'marketingName': 'iPhone 12', 'reality': 'physical'}
                    def device(output, label, args):
                        if args[:3] == ['device', 'install', 'app']:
                            boundary_calls.append(args)
                            raise subprocess.TimeoutExpired('devicectl', 60)
                        if args[:3] == ['device', 'info', 'lockState']:
                            return {'passcodeRequired': failure == 'locked'}
                        return {'properties': {'hardware': hardware,
                            'connection': {'state': 'connected', 'transportType': 'wired'},
                            'state': {'bootState': 'booted'}}}
                    def prepare(output, *args):
                        mobile = output / 'host'
                        mobile.mkdir()
                        return mobile, 'nonce'
                    with ExitStack() as patches:
                        for target, value in [
                            ('QA_ROOT', root), ('preserved', lambda *args: {'unchanged': True}),
                            ('inventory', lambda *args: {}), ('signing', lambda *args: (profile_file, {}, 'identity')),
                            ('validate_bundle', lambda *args: {}), ('space', lambda *args: {}),
                            ('snapshot_binding', lambda *args: {}), ('source_closure', lambda: {}),
                            ('prepare', prepare), ('command', lambda *args, **kwargs: b''),
                            ('backup', lambda *args: {'verified': True}), ('device_json', device)]:
                            patches.enter_context(patch.object(qa, target, value))
                        patches.enter_context(patch.object(qa.p5d, 'fixture_cases', return_value=([], {})))
                        patches.enter_context(patch.object(qa.p5d, 'repo_hashes', return_value={}))
                        patches.enter_context(patch.object(qa.p5d, 'old_output_inventory', return_value={}))
                        _, result = qa.install_verified(run) if flow == 'install-verified' else qa.run_all(root / 'report.json', True)
                    self.assertEqual(result['installation'], 'NOT_RUN' if failure == 'locked' else 'OUTCOME_UNKNOWN')
                    self.assertEqual(len(boundary_calls), 0 if failure == 'locked' else 1)
                    self.assertIn(result['status'], ('FAILED', 'INSTALL_FAILED'))
                    self.assertIn('device_unlock_required' if failure == 'locked' else 'TimeoutExpired', result['reason'])

    def session(self):
        pins = [dict(caseId=f'{asset}-{arm}', assetId=asset,
                     role='cover' if asset in (1950, 8303) else 'page', sha256='pinned-' + arm)
                for asset in qa.p5d.IDS for arm in ('original', 'PNG', 'WEBP')]
        rows = [dict(event='start'), dict(event='network', network='satisfied', sequence=1),
                dict(event='network', network='unsatisfied', sequence=2)]
        for pin in pins:
            for crop in (False, True):
                geometry = dict(logicalWidth=390, logicalHeight=400, dpr=3,
                    fit='1to1' if crop else 'cover' if pin['role'] == 'cover' else 'contain',
                    filterQuality='none' if crop else 'low')
                if crop:
                    x, y = qa.p5d.TEXT_CROPS[pin['assetId']]
                    geometry['roi'] = dict(x=x, y=y, width=1170, height=600, physicalPixelsPerSourcePixel=1)
                rows.append(dict(event='presentation', network='unsatisfied', nativeNetwork='unsatisfied',
                    nativeNetworkSequence=2, case=pin.copy(), crop=crop, decodedWidth=1712,
                    decodedHeight=2778, decoderColorSpace='displayP3', geometry=geometry))
        rows.append(dict(event='seal', network='unsatisfied', nativeNetwork='unsatisfied', nativeNetworkSequence=2))
        rows.append(dict(event='network', network='satisfied', sequence=3))
        for index, row in enumerate(rows):
            row.update(runNonce='nonce', sessionId='session', eventSequence=index + 1)
        return rows, pins

    def test_collection_accepts_only_sealed_exact_24_views(self):
        rows, pins = self.session()
        self.assertTrue(validate_session(rows, 'nonce', pins)['all24Views'])
        mutations = [lambda r: r[3]['case'].update(sha256='wrong'),
                     lambda r: r[3]['geometry'].update(dpr=2),
                     lambda r: r[5]['geometry'].update(logicalWidth=389),
                     lambda r: r[4]['geometry']['roi'].update(x=0),
                     lambda r: r[3].update(nativeNetworkSequence=1),
                     lambda r: r[3].update(eventSequence=99),
                     lambda r: r[4].update(event='network', network='satisfied', sequence=3),
                     lambda r: r[4].update(event='lifecycle', active=False),
                     lambda r: r.pop(-2), lambda r: r.pop(3)]
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                changed = copy.deepcopy(rows)
                mutate(changed)
                with self.assertRaises(ValueError):
                    validate_session(changed, 'nonce', pins)

    def test_23_views_reaches_incomplete_rejection_after_sequence_is_repaired(self):
        rows, pins = self.session()
        rows.pop(3)
        for sequence, row in enumerate(rows, start=1):
            row['eventSequence'] = sequence
        with self.assertRaisesRegex(ValueError, '^incomplete_24_views$'):
            validate_session(rows, 'nonce', pins)

    @unittest.skipUnless(shutil.which('swiftc'), 'Mevcut Swift SDK yok')
    def test_native_session_relaunch_and_write_failure_are_executed(self):
        with tempfile.TemporaryDirectory(prefix='p5e-native-test-') as temp:
            root = Path(temp)
            main = root / 'main.swift'
            main.write_text('''import Foundation
let documents = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
let nonce = String(repeating: "a", count: 32)
let first = try P5eSession(documents: documents, nonce: nonce)
try first.append(["event": "first"])
let second = try P5eSession(documents: documents, nonce: nonce)
try second.append(["event": "second"])
let file = documents.appendingPathComponent("p5e-" + nonce + "/session.jsonl")
let lines = try String(contentsOf: file, encoding: .utf8).split(separator: "\\n")
precondition(lines.count == 4)
let badNonce = String(repeating: "b", count: 32)
let bad = try P5eSession(documents: documents, nonce: badNonce)
let badFile = documents.appendingPathComponent("p5e-" + badNonce + "/session.jsonl")
try FileManager.default.moveItem(at: badFile, to: badFile.appendingPathExtension("prior"))
try FileManager.default.createDirectory(at: badFile, withIntermediateDirectories: false)
do { try bad.append(["event": "bad"]); fatalError("write accepted") } catch {}
precondition(!bad.healthy)
do { try bad.append(["event": "later"]); fatalError("recovery accepted") } catch {}
let offlineNonce = String(repeating: "c", count: 32)
let offline = try P5eSession(documents: documents, nonce: offlineNonce)
try offline.append(["event": "network", "network": "unsatisfied"])
try offline.append(["event": "presentation", "network": "unsatisfied"])
try offline.append(["event": "seal", "network": "unsatisfied"])
try offline.append(["event": "network", "network": "satisfied"])
precondition(offline.healthy)
let returned = try P5eSession(documents: documents, nonce: String(repeating: "d", count: 32))
try returned.append(["event": "presentation", "network": "unsatisfied"])
try returned.append(["event": "network", "network": "satisfied"])
do { try returned.append(["event": "seal", "network": "unsatisfied"]); fatalError("bad seal") } catch {}
precondition(!returned.healthy)
print("native-session-pass")
''')
            binary = root / 'session-test'
            result = subprocess.run(['swiftc', '-module-cache-path', str(root / 'module-cache'),
                    str(qa.HOST / 'P5eSession.swift'), str(main), '-o', str(binary)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            result = subprocess.run([str(binary), str(root)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('native-session-pass', result.stdout)


if __name__ == '__main__':
    unittest.main()
