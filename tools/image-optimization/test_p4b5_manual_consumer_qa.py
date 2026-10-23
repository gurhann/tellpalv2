"""Elle ölçümün gerçek dosya protokolü ve değişmez eski borç kapıları."""
import copy
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

import p4b5_manual_consumer_qa as qa
from test_p4b5_consumer_qa import records


def envelopes(items=None):
    items = records() if items is None else items
    previous, result, cumulative = '0' * 64, [], []
    for index, item in enumerate(items):
        sample = item['sample']
        sample['resourceObservation']['lifetimeId'] = 'a' * 32
        # Önceki fixture yalnız yükleme anlarını taşır. Gerçek probe olayları ekler.
        incoming = sample['events']
        if len(incoming) >= len(cumulative): cumulative = incoming
        sample['events'] = copy.deepcopy(cumulative)
        data = {**qa.SCOPE.identity(), 'nonce': 'b' * 64, 'pid': 123, 'processStartTicks': '456',
                'lifetimeId': 'a' * 32, 'supportDirectory': '/data/user/0/' + qa.PACKAGE + '/files', 'ordinal': index + 1, 'phase': item['phase'],
                'previousSha256': previous, 'recordedAtEpochMillis': 1000 + index, 'sample': sample}
        raw = json.dumps(data).encode();result.append(raw);previous = hashlib.sha256(raw).hexdigest()
    return result


def mutate(raws, index, change):
    values = [json.loads(raw) for raw in raws]
    change(values[index])
    previous = '0' * 64
    result = []
    for value in values:
        value['previousSha256'] = previous
        raw = json.dumps(value).encode();result.append(raw);previous = hashlib.sha256(raw).hexdigest()
    return result


class ManualQaTest(unittest.TestCase):
    def test_explicit_scope_dry_run_has_zero_io_and_unknown_scope_refuses(self):
        with patch.object(qa, 'ManualController', side_effect=AssertionError('IO forbidden')):
            self.assertEqual(qa.main([]), 2)
            self.assertEqual(qa.main(['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256]), 0)
            self.assertEqual(qa.main(['--scope', 'wrong', '--proposal-sha256', qa.SCOPE.proposal_sha256]), 2)

    def test_ten_phase_receipts_keep_one_owner_and_four_buffer_attempts(self):
        owner, observed = qa.validate_receipts(envelopes())
        self.assertEqual(owner['pid'], 123)
        self.assertEqual(len(observed), 10)
        self.assertEqual(observed[-1]['sample']['resourceObservation']['bufferCreationAttempts'], '11')

    def test_missing_duplicate_reordered_truncated_chain_and_extra_receipt_refuse(self):
        value = envelopes()
        for invalid in [value[:-1], value[1:], value[:3] + value[2:], list(reversed(value)), value + [value[-1]],
                        value[:5] + [value[5][:-1]] + value[6:]]:
            with self.subTest(length=len(invalid)), self.assertRaises((ValueError, json.JSONDecodeError)):
                qa.validate_receipts(invalid)
        invalid = list(value);invalid[2] = invalid[2].replace(b'"previousSha256": "', b'"previousSha256": "1')
        with self.assertRaises(ValueError): qa.validate_receipts(invalid)

    def test_scope_nonce_pid_lifetime_phase_epoch_boolean_and_leak_mutations_refuse(self):
        for change in [lambda v: v.update(nonce='c' * 64), lambda v: v.update(pid=999),
                       lambda v: v.update(processStartTicks='999'), lambda v: v.update(lifetimeId='c' * 32),
                       lambda v: v.update(proposalSha256='c' * 64), lambda v: v.update(phase='paint1'),
                       lambda v: v['sample'].update(paintedEpoch=1), lambda v: v['sample'].update(configured=1),
                       lambda v: v['sample']['resourceObservation'].update(bufferCreationAttempts='12')]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                qa.validate_receipts(mutate(envelopes(), 3, change))
        for index in [6, 7, 8, 9]:
            for field in qa.prior.ACTIVE:
                with self.assertRaises(ValueError):
                    qa.validate_receipts(mutate(envelopes(), index, lambda v: v['sample'].update({field: 1})))

    def test_event_overflow_rewrite_fifth_media_and_new_background_http_refuse(self):
        for index, change in [
            (9, lambda v: v['sample'].update(events=[{}] * 129)),
            (4, lambda v: v['sample'].update(events=[])),
            (9, lambda v: v['sample']['events'].append({'kind': 'httpReservation', 'endpoint': 'bytes', 'ordinal': 6})),
            (6, lambda v: v['sample']['events'].append({'kind': 'httpReservation', 'endpoint': 'status', 'ordinal': 6})),
        ]:
            with self.assertRaises(ValueError): qa.validate_receipts(mutate(envelopes(), index, change))

    def test_both_prior_file_chains_are_hashed_without_old_mutation(self):
        proposal = (qa.ROOT / qa.SCOPE.proposal).read_bytes();value = json.loads(proposal)
        proof = qa.expected_prior_proof(value)
        files = {str(Path(previous['privateRoot']) / name): sha.encode()
                 for previous in value['previousAttemptsEvidence'] for name, sha in previous['files'].items()}
        calls = []
        def read(path, *args, **kwargs):
            calls.append(str(path));return files[str(path)]
        # Her kayıt için ayrı byte fixture ve aynı beklenen hash arayüzü.
        real_sha = qa.hashlib.sha256
        class Digest:
            def __init__(self, raw): self.raw = raw
            def hexdigest(self):
                return self.raw.decode() if len(self.raw) == 64 else real_sha(self.raw).hexdigest()
        with patch.object(qa.helpers, 'private_file', side_effect=read), patch.object(qa.hashlib, 'sha256', Digest):
            qa.validate_prior_proof(proposal, proof)
            self.assertEqual(len(calls), 18)
            for filename in files:
                old = files[filename];files[filename] = b'drift'
                with self.assertRaisesRegex(ValueError, 'PRIOR_ATTEMPT_FILE_CHANGED'):
                    qa.validate_prior_proof(proposal, proof)
                files[filename] = old
        invalid = copy.deepcopy(proof);invalid['priorConsumption']['deliveryOperations'] = 0
        with self.assertRaisesRegex(ValueError, 'PRIOR_ATTEMPT_PROOF_CHANGED'): qa.validate_prior_proof(proposal, invalid)

    @unittest.skipUnless(os.environ.get('P4B5_MANUAL_DART_JSON_LOG'), 'Yeni manual scope gerçek Dart JSON logu seçilmedi')
    def test_fresh_manual_scope_native_json_passes_phase_resource_validation(self):
        chains = []
        for line in Path(os.environ['P4B5_MANUAL_DART_JSON_LOG']).read_text().splitlines():
            if 'P4B5_MANUAL_RESOURCE_JSON:' not in line: continue
            item = json.loads(line.split('P4B5_MANUAL_RESOURCE_JSON:', 1)[1])
            report = qa.prior.validate_phase_resources(item['records'])
            self.assertEqual(report['bufferCreationAttemptsDelta'], '4')
            self.assertTrue(report['measured'])
            chains.append(item['chain'])
        self.assertEqual(chains, ['loopback-native'])

    def test_private_transfer_uses_stdin_exact_path_no_token_in_arguments(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        value = {k: json.loads(envelopes()[0])[k] for k in qa.IDENTITY_KEYS}
        value['configure'] = {'token': 'SYNTHETIC_SECRET'}
        with patch.object(android, 'private_root', return_value='/data/user/0/' + qa.PACKAGE), patch.object(qa.subprocess, 'run') as run:
            run.return_value.returncode = 0
            run.return_value.stdout = b'OK\n'
            android.write_configuration(value)
            args, kwargs = run.call_args
            self.assertNotIn('SYNTHETIC_SECRET', str(args))
            self.assertIn(b'SYNTHETIC_SECRET', kwargs['input'])
            self.assertIn('set -C', args[0][-1])
            self.assertIn('test ! -L', args[0][-1])
            self.assertIn('mv -n', args[0][-1])
            self.assertEqual(kwargs['stderr'], qa.subprocess.DEVNULL)
            self.assertEqual(args[0][3:5], ['shell', '-T'])
            with self.assertRaisesRegex(ValueError, 'CONFIGURATION_SIZE'):
                android.write_configuration({**value, 'configure': {'token': 'x' * 20481}})

    def test_cleanup_requires_owned_nonce_and_removes_both_atomic_transfer_names(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        with patch.object(android, 'private_root'), patch.object(android, 'private_shell') as run:
            run.return_value.returncode = 0
            android.remove_owned_configuration('a' * 64)
            command = run.call_args.args[-1]
            self.assertIn('handoff.owner', command)
            self.assertIn('$(cat ', command)
            self.assertIn('configure.json.part', command)
            self.assertIn('test ! -L', command)
            with self.assertRaisesRegex(ValueError, 'HANDOFF_NONCE_REQUIRED'):
                android.remove_owned_configuration('../other')
            self.assertEqual(run.call_count, 1)

    def test_closed_scope_and_second_claim_do_not_reopen(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory);controller = qa.ManualController(root, root)
            controller.claim('measurement')
            original = (root / 'measurement.claim.private.json').read_bytes()
            with self.assertRaises(FileExistsError): controller.claim('measurement')
            self.assertEqual(original, (root / 'measurement.claim.private.json').read_bytes())
            (root / 'terminal.private.json').write_text('old closed')
            with self.assertRaisesRegex(ValueError, 'ATTEMPT_CLOSED_NO_RETRY'): controller.ensure_open()


if __name__ == '__main__': unittest.main()

class ManualAdapterBoundaryTest(unittest.TestCase):
    def test_shell_v2_quotes_script_as_one_remote_argument_and_not_ready_is_only_exit_three(self):
        import shlex
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        for code, output, expected in [(3, b'', None), (0, b'{"receipt":true}', b'{"receipt":true}')]:
            with patch.object(android, 'private_root'), patch.object(qa.subprocess, 'run', return_value=Mock(returncode=code, stdout=output)) as run:
                self.assertEqual(android.read_receipt(0), expected)
                command = run.call_args.args[0]
                self.assertEqual(command[3:5], ['shell', '-T'])
                remote = shlex.split(command[5])
                self.assertEqual(remote[:4], ['run-as', qa.PACKAGE, 'sh', '-c'])
                self.assertEqual(len(remote), 5)
                self.assertIn('exit 3', remote[4])
                self.assertIn('$(stat -c %a ', remote[4])
        for code, output in [(0, b''), (1, b''), (2, b'{}'), (127, b'')]:
            with patch.object(android, 'private_root'), patch.object(qa.subprocess, 'run', return_value=Mock(returncode=code, stdout=output)):
                with self.assertRaises(ValueError): android.read_receipt(0)

    def test_four_ack_bindings_require_current_receipt_epoch_and_png(self):
        raws = envelopes();owner, _ = qa.validate_receipts(raws)
        pngs = [b'\x89PNG\r\n\x1a\n' + bytes([n]) for n in range(4)]
        acknowledgements = [json.dumps({**owner, 'phase': 'paint' + str(n),
            'epoch': json.loads(raws[n])['sample']['paintedEpoch'],
            'receiptSha256': hashlib.sha256(raws[n]).hexdigest(),
            'pngSha256': hashlib.sha256(pngs[n - 1]).hexdigest()}).encode() for n in range(1, 5)]
        qa.validate_captures(acknowledgements, raws, pngs)
        for field in ['phase', 'epoch', 'nonce', 'pid', 'lifetimeId', 'receiptSha256', 'pngSha256']:
            invalid = list(acknowledgements);ack = json.loads(invalid[0]);ack[field] = 'wrong';invalid[0] = json.dumps(ack).encode()
            with self.subTest(field=field), self.assertRaises(ValueError): qa.validate_captures(invalid, raws, pngs)
        for invalid in [acknowledgements[:-1], acknowledgements[::-1]]:
            with self.assertRaises(ValueError): qa.validate_captures(invalid, raws, pngs)

    def test_credential_cleanup_is_independent_of_dead_or_changed_process_and_failure(self):
        for status in ['dead', 'changed', 'cleanup-failed', 'owner-file-damaged']:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();(root / 'android').mkdir()
                qa.prior.write_json(root / 'android/owned-process.private.json', {**qa.SCOPE.identity(), 'pid': 123, 'processStartTicks': '456'})
                qa.prior.write_json(root / 'android/baseline.private.json', {**qa.SCOPE.identity(), 'identity': {**qa.SCOPE.identity(), 'nonce': 'a' * 64}})
                controller = qa.ManualController(root, root);android = Mock()
                android.remove_owned_configuration.return_value = None
                android.pid.return_value = '' if status == 'dead' else '999'
                if status == 'owner-file-damaged': (root / 'android/owned-process.private.json').write_text('broken')
                android.owned_process_state.return_value = 'absent' if status == 'dead' else 'alive'
                if status == 'changed': android.owned_process_state.side_effect = ValueError('QA_OWNER_CHANGED')
                if status == 'cleanup-failed':
                    android.remove_owned_configuration.side_effect = ValueError('PRIVATE_CLEANUP_REFUSED')
                    android.owned_process_state.side_effect = ['alive', 'absent']
                results = controller.cleanup(android)
                android.remove_owned_configuration.assert_called_once_with('a' * 64)
                if status in ['changed', 'owner-file-damaged']: android.run.assert_not_called()
                if status == 'cleanup-failed':
                    android.run.assert_called_once_with('shell', 'am', 'force-stop', qa.PACKAGE)
                    self.assertEqual(results['configuration']['status'], 'FAILED')
                self.assertEqual(qa.prior.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')

    def test_receipt_call_cannot_succeed_after_its_deadline(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        def slow(*args, **kwargs):
            import time
            time.sleep(.01)
            return Mock(returncode=0, stdout=b'{}')
        with patch.object(android, 'private_root'), patch.object(android, 'private_shell', side_effect=slow):
            with self.assertRaisesRegex(ValueError, 'ADB_DEADLINE'): android.read_receipt(0, timeout=.001)

class ManualApprovalBoundaryTest(unittest.TestCase):
    def test_real_arm_gate_validates_artifact_chain_and_separate_approval_before_any_claim_or_adb(self):
        import dataclasses
        for mutation in ['valid', 'decision', 'scope', 'run', 'apk']:
            with self.subTest(mutation=mutation), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();source = root / 'source-0002'
                for path in [source, root / 'receipts', root / 'reviews']: path.mkdir(mode=0o700)
                selected = dataclasses.replace(qa.SCOPE, private_root=root)
                identity = selected.identity()
                proposal = source / selected.proposal;proposal.parent.mkdir(parents=True)
                proposal.write_bytes((qa.ROOT / selected.proposal).read_bytes())
                one = source / 'one.py';one.write_text('synthetic-source')
                source_path = root / 'receipts/source-binding-0001.private.json'
                qa.prior.write_json(source_path, {**identity, 'sourceDirectory': 'source-0002', 'files': {'one.py': qa.prior.digest(one)}})
                qa.prior.write_json(root / 'receipts/human-approval-01.private.json', {**identity, 'answer': 'A', 'scope': 'LOCAL_IMPLEMENTATION_ONLY'})
                binary = root / 'One.class';binary.write_bytes(b'synthetic-class')
                classpath = root / 'receipts/classpath-binding-0001.private.json'
                qa.prior.write_json(classpath, {**identity, 'files': {str(binary): qa.prior.digest(binary)}})
                build_root = root / 'android/builds/build-0001';build_root.mkdir(parents=True)
                apk = build_root / 'consumer.apk';apk.write_bytes(b'synthetic-apk')
                qa.prior.write_json(build_root / 'build.private.json', {**identity, 'entry': selected.entry, 'package': qa.PACKAGE,
                    'apkSha256': qa.prior.digest(apk), 'sourceBindingSha256': qa.prior.digest(source_path)})
                selected_path = root / 'receipts/selected-build-0001.private.json'
                qa.prior.write_json(selected_path, {**identity, 'buildName': 'build-0001', 'apkSha256': qa.prior.digest(apk)})
                run = dict(identity)
                for key, path in [('sourceBinding', source_path), ('classpathBinding', classpath), ('selectedBuild', selected_path)]:
                    run[key + 'File'] = path.name;run[key + 'Sha256'] = qa.prior.digest(path)
                run_path = root / 'receipts/run-binding-0001.private.json';qa.prior.write_json(run_path, run)
                for n in range(1, 4):
                    qa.prior.write_json(root / f'reviews/pre-run-0001-{n:02d}.private.json', {**identity,
                        'sourceBindingSha256': qa.prior.digest(source_path), 'runBindingSha256': qa.prior.digest(run_path),
                        'status': 'PASS', 'unresolvedFindings': 0, 'reviewerId': f'synthetic-{n}'})
                approval = {**identity, 'decision': 'APPROVE_ONE_REAL_MANUAL_ATTEMPT',
                    'runBindingSha256': qa.prior.digest(run_path), 'apkSha256': qa.prior.digest(apk)}
                field = {'decision': 'decision', 'scope': 'measurementId', 'run': 'runBindingSha256', 'apk': 'apkSha256'}.get(mutation)
                if field: approval[field] = 'wrong'
                qa.prior.write_json(root / 'receipts/execution-approval-01.private.json', approval)
                android = Mock()
                with patch.object(qa, 'SCOPE', selected), patch.object(qa.prior, 'P4B5_MANUAL', selected):
                    controller = qa.ManualController(root, source)
                    if mutation == 'valid':
                        controller.execution_approval_gate(run_path)
                        # Gerçek artifact/onay kapısından sonra eksik prior proof başarıya dönüşmez.
                        with self.assertRaisesRegex(ValueError, 'NUMBERED_RECEIPT_REQUIRED'): controller.arm(android)
                    else:
                        with self.assertRaisesRegex(ValueError, 'SEPARATE_REAL_EXECUTION_APPROVAL_REQUIRED'): controller.arm(android)
                self.assertEqual(android.mock_calls, [])
                self.assertFalse((root / 'install.claim.private.json').exists())
                self.assertFalse((root / 'backend-start.claim.private.json').exists())

class ManualInvocationTerminalTest(unittest.TestCase):
    def test_previous_real_claim_closes_failure_before_new_stage_claim_but_local_error_and_closed_attempt_do_not_mutate(self):
        for mode in ['claimed', 'local', 'closed']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();(root / 'receipts').mkdir(mode=0o700)
                controller = qa.ManualController(root, root)
                if mode != 'local': controller.claim('install')
                if mode == 'closed': qa.prior.write_json(root / 'terminal.private.json', {**qa.SCOPE.identity(), 'status': 'FAILED_NO_RETRY'})
                before = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}
                with patch.object(qa, 'ManualController', return_value=controller), patch.object(qa, 'ManualAndroid', side_effect=AssertionError('ADB forbidden')):
                    self.assertEqual(qa.main(['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256, '--stage', 'arm']), 2)
                if mode == 'claimed':
                    self.assertEqual(qa.prior.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')
                    self.assertEqual(len(list((root / 'receipts').glob('terminal-failure-*.private.json'))), 1)
                    self.assertFalse((root / 'initial-start.claim.private.json').exists())
                else:
                    self.assertEqual(before, {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()})

class ManualRealShellContractTest(unittest.TestCase):
    def test_adapter_executes_quoted_remote_shell_exit_stdin_permissions_and_create_new_contract(self):
        import shlex
        import sys
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve();binary = root / 'bin';binary.mkdir()
            private = root / qa.RELATIVE_ROOT
            for path in [private / 'startup', private / 'receipts', private / 'captures']:
                path.mkdir(mode=0o700, parents=True);path.chmod(0o700)
            private.chmod(0o700)
            run_as = binary / 'run-as'
            run_as.write_text('#!/bin/sh\n[ "$1" = ' + shlex.quote(qa.PACKAGE) + ' ] || exit 9\nshift\nexec "$@"\n');run_as.chmod(0o700)
            stat = binary / 'stat'
            stat.write_text('#!' + sys.executable + '\nimport os,sys\ns=os.stat(sys.argv[3],follow_symlinks=False)\nprint(oct(s.st_mode & 0o777)[2:] if sys.argv[2]=="%a" else s.st_size)\n');stat.chmod(0o700)
            adb = root / 'adb'
            adb.write_text('#!' + sys.executable + '\nimport os,subprocess,sys\n'
                + 'assert sys.argv[1:5]==["-s","emulator-5554","shell","-T"]\n'
                + 'env=os.environ.copy();env["PATH"]=' + repr(str(binary)) + '+os.pathsep+env["PATH"]\n'
                + 'result=subprocess.run(["/bin/sh","-c",sys.argv[5]],cwd=' + repr(str(root)) + ',env=env,input=sys.stdin.buffer.read(),stdout=subprocess.PIPE,stderr=subprocess.PIPE)\n'
                + 'sys.stdout.buffer.write(result.stdout);sys.exit(result.returncode)\n');adb.chmod(0o700)
            android = qa.ManualAndroid(adb, 'emulator-5554')
            owner = {k: json.loads(envelopes()[0])[k] for k in qa.IDENTITY_KEYS}
            value = {**owner, 'configure': {'token': 'SYNTHETIC_SECRET'}}
            with patch.object(android, 'private_root', return_value='/data/user/0/' + qa.PACKAGE):
                self.assertIsNone(android.read_receipt(0))
                receipt = private / 'receipts/01-baseline.json';receipt.write_bytes(b'{"receipt":true}');receipt.chmod(0o600)
                self.assertEqual(android.read_receipt(0), b'{"receipt":true}')
                receipt.chmod(0o644)
                with self.assertRaisesRegex(ValueError, 'PRIVATE_RECEIPT_READ_REFUSED'): android.read_receipt(0)
                receipt.unlink();receipt.symlink_to(root / 'absent')
                with self.assertRaisesRegex(ValueError, 'PRIVATE_RECEIPT_READ_REFUSED'): android.read_receipt(0)
                android.write_configuration(value)
                configure = private / 'startup/configure.json'
                self.assertEqual(json.loads(configure.read_bytes()), value)
                self.assertEqual(configure.stat().st_mode & 0o777, 0o600)
                original = configure.read_bytes()
                with self.assertRaisesRegex(ValueError, 'PRIVATE_STDIN_HANDOFF_REFUSED'): android.write_configuration(value)
                self.assertEqual(configure.read_bytes(), original)
                ack = {**owner, 'phase': 'paint1', 'epoch': 1, 'receiptSha256': 'a' * 64, 'pngSha256': 'b' * 64}
                self.assertEqual(json.loads(android.capture_ack(ack)), ack)
                with self.assertRaisesRegex(ValueError, 'CAPTURE_ACK_REFUSED'): android.capture_ack(ack)
                android.remove_owned_configuration(owner['nonce'])
                self.assertEqual(list((private / 'startup').iterdir()), [])

class _CliOwnedAndroid:
    def __init__(self, executable, credential, nonce, mode):
        self.args = [str(executable), '-s', 'emulator-5554']
        self.credential, self.nonce, self.mode = credential, nonce, mode
        self.alive = mode != 'dead'
        self.mapping = True
        self.calls = []

    def remove_owned_configuration(self, nonce):
        self.calls.append(('configuration', nonce))
        qa.require(nonce == self.nonce, 'HANDOFF_NONCE_REQUIRED')
        qa.require(self.mode != 'configuration-error', 'PRIVATE_CLEANUP_REFUSED')
        self.credential.unlink()

    def pid(self, timeout=15):
        return ('999' if self.mode == 'changed-pid' else '123') if self.alive else ''

    def owner(self, expected, timeout=15):
        self.calls.append(('owner', expected['pid']))
        qa.require(self.pid() == str(expected['pid']), 'QA_OWNER_CHANGED')

    def owned_process_state(self, expected):
        if not self.alive: return 'absent'
        self.owner(expected)
        return 'alive'

    def run(self, *args, timeout=15):
        self.calls.append(args)
        if args == ('shell', 'am', 'force-stop', qa.PACKAGE):
            self.alive = False
            return ''
        if args == ('reverse', '--list'):
            return 'emulator-5554 tcp:12345 tcp:12345' if self.mapping else ''
        if args == ('reverse', '--remove', 'tcp:12345'):
            self.mapping = False
            return ''
        raise AssertionError(args)


class _ArmOwnedAndroid(_CliOwnedAndroid):
    def preflight(self): pass

    def failure_marker(self, timeout=3): pass

    def private_root(self, timeout=3): return '/data/user/0/' + qa.PACKAGE

    def read_receipt(self, index, timeout=3):
        assert index == 0
        return envelopes()[0]

    def process_identity(self, pid, timeout=15):
        assert pid == 123
        return {'pid': 123, 'processStartTicks': '456'}

    def run(self, *args, timeout=15):
        self.calls.append(args)
        if args[0] == 'install': return 'Success'
        if args == ('shell', 'pm', 'path', qa.PACKAGE): return 'package:/data/app/synthetic/base.apk'
        if args == ('shell', 'sha256sum', '/data/app/synthetic/base.apk'): return 'c' * 64 + ' /data/app/synthetic/base.apk'
        if args[:3] == ('shell', 'am', 'start'):
            self.alive = True
            return ''
        self.calls.pop()
        return super().run(*args, timeout=timeout)


def arm_synthetic_controller(controller, android, root):
    android.alive = False
    with patch.object(controller, 'gate'), patch.object(controller, 'selected_build', return_value=({'apkSha256': 'c' * 64}, root)):
        controller.arm(android)
    claim = qa.prior.private_json(root / 'install.claim.private.json')
    assert claim['adbExecutable'] == android.args[0]
    assert claim['adbSha256'] == qa.prior.digest(Path(android.args[0]))
    assert claim['runBindingSha256'] == qa.prior.digest(root / 'receipts/run-binding-0001.private.json')
    return (root / 'install.claim.private.json').read_bytes()


class ManualRealCliCleanupTest(unittest.TestCase):
    def test_record_backend_error_cleans_bound_resources_with_supplied_or_omitted_adb(self):
        for mode in ['supplied', 'omitted', 'dead', 'changed-pid', 'configuration-error', 'wrong-argument', 'backend-failure']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve()
                for name in ['receipts', 'android', 'backend']:
                    (root / name).mkdir(mode=0o700)
                controller = qa.ManualController(root, root)
                run_path = root / 'receipts/run-binding-0001.private.json'
                qa.prior.write_json(run_path, qa.SCOPE.identity())
                adb = root / 'adb';adb.write_bytes(b'synthetic-bound-adb')
                nonce = 'b' * 64
                credential = root / 'synthetic-credential';credential.write_bytes(b'SYNTHETIC_SECRET');credential.chmod(0o600)
                android = _ArmOwnedAndroid(adb, credential, nonce, 'supplied')
                install_claim = arm_synthetic_controller(controller, android, root)
                android.mode = mode;android.alive = mode != 'dead'
                controller.claim('backend-start')
                qa.prior.write_json(root / 'android/reverse-intent.private.json', {**qa.SCOPE.identity(), 'serial': 'emulator-5554', 'devicePort': 12345, 'hostPort': 12345, 'baselineMappingAbsent': True})
                qa.prior.write_json(root / 'backend/launch.private.json', {**qa.SCOPE.identity(), 'pid': 456, 'processStartIdentity': 'synthetic-start', 'commandSha256': 'a' * 64})
                if mode == 'backend-failure': qa.prior.write_json(root / 'backend/failure.json', {'status': 'FAILED_NO_RETRY'})
                def alive(owner, timeout=3):
                    self.assertEqual(owner['pid'], 456)
                    return not (root / 'backend/close.request').exists()
                arguments = ['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256,
                             '--stage', 'record-backend', '--pid', '1']
                if mode != 'omitted': arguments += ['--adb', str(root / 'other-adb' if mode == 'wrong-argument' else adb)]
                with patch.object(qa, 'ManualController', return_value=controller), \
                        patch.object(qa, 'ManualAndroid', return_value=android) as adapter, \
                        patch.object(controller, 'gate'), \
                        patch.object(controller, 'owned_backend_alive', side_effect=alive), \
                        patch.object(qa.os, 'kill', side_effect=AssertionError('Gerçek sinyal yasak')):
                    self.assertEqual(qa.main(arguments), 2)
                    adapter.assert_called_once_with(adb, 'emulator-5554', qa.SCOPE)
                self.assertEqual((root / 'install.claim.private.json').read_bytes(), install_claim)
                cleanup = qa.prior.private_json(root / 'cleanup.private.json')
                self.assertEqual(set(cleanup['results']), {'configuration', 'qaProcess', 'reverse', 'backend'})
                self.assertEqual(cleanup['results']['backend']['result']['processExitConfirmed'], True)
                self.assertFalse(android.mapping)
                self.assertEqual(credential.exists(), mode == 'configuration-error')
                self.assertEqual(android.alive, mode == 'changed-pid')
                if mode == 'changed-pid':
                    self.assertNotIn(('shell', 'am', 'force-stop', qa.PACKAGE), android.calls)
                    self.assertEqual(cleanup['results']['qaProcess']['code'], 'QA_OWNER_CHANGED')
                if mode == 'configuration-error':
                    self.assertEqual(cleanup['results']['configuration']['code'], 'PRIVATE_CLEANUP_REFUSED')
                failure = qa.prior.private_json(next((root / 'receipts').glob('terminal-failure-*.private.json')))
                self.assertEqual(failure['code'], {'wrong-argument': 'ADB_CLAIM_ARGUMENT_CHANGED', 'backend-failure': 'ATTEMPT_CLOSED_NO_RETRY'}.get(mode, 'BACKEND_PID_REQUIRED'))
                self.assertEqual(qa.prior.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')
                with self.assertRaisesRegex(ValueError, 'ATTEMPT_CLOSED_NO_RETRY'): controller.ensure_open()

    def test_first_record_backend_error_recovers_reserved_exact_owner_without_launch_or_ready(self):
        import shlex
        import subprocess
        states = ['prepared', 'dead', 'changed-start', 'changed-command', 'wrong-java', 'startup-pid', 'zombie', 'missing-pid', 'invalid-pid', 'reservation-run', 'classpath-drift', 'java-drift']
        for failure in ['wrong-adb', 'early-failure']:
            for state in states:
                with self.subTest(failure=failure, state=state), tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve()
                    for name in ['receipts', 'android', 'backend', 'java/bin']: (root / name).mkdir(parents=True, mode=0o700)
                    controller = qa.ManualController(root, root)
                    java = root / 'java/bin/java';java.write_bytes(b'synthetic-bound-java')
                    cp = root / 'receipts/classpath-binding-0001.private.json'
                    qa.prior.write_json(cp, {**qa.SCOPE.identity(), 'entries': [str(root / 'classes')],
                            'javaBinary': str(java), 'javaBinarySha256': qa.prior.digest(java)})
                    qa.prior.write_json(root / 'receipts/operational-tools.private.json', {'backendJavaHome': str(root / 'java')})
                    qa.prior.write_json(root / 'receipts/run-binding-0001.private.json', {**qa.SCOPE.identity(),
                            'classpathBindingFile': cp.name, 'classpathBindingSha256': qa.prior.digest(cp)})
                    adb = root / 'adb';adb.write_bytes(b'synthetic-bound-adb')
                    credential = root / 'synthetic-credential';credential.write_bytes(b'SYNTHETIC_SECRET')
                    android = _ArmOwnedAndroid(adb, credential, 'b' * 64, 'supplied')
                    install_claim = arm_synthetic_controller(controller, android, root)
                    with patch.object(controller, 'gate'), patch.object(controller, 'baseline_gate'):
                        controller.reserve_backend(android)
                    self.assertFalse((root / 'backend/launch.private.json').exists())
                    self.assertFalse((root / 'backend/local-ready.json').exists())
                    if failure == 'early-failure': qa.prior.write_json(root / 'backend/failure.json', {'status': 'FAILED_NO_RETRY'})
                    if state == 'reservation-run':
                        path = root / 'backend-start.claim.private.json'
                        value = qa.prior.private_json(path);value['runBindingSha256'] = '0' * 64
                        path.write_text(json.dumps(value));path.chmod(0o600)
                    if state == 'classpath-drift':
                        value = qa.prior.private_json(cp);value['entries'] = [str(root / 'changed-classes')]
                        cp.write_text(json.dumps(value));cp.chmod(0o600)
                    if state == 'java-drift': java.write_bytes(b'changed-java')
                    if state == 'startup-pid': qa.prior.write_json(root / 'backend/startup.claim.private.json', {**qa.SCOPE.identity(), 'pid': 457})
                    words = [str(java), '-Xmx1g', '-cp', str(root / 'classes'), 'com.tellpal.v2.support.P4b5ConsumerQaSetup', *qa.SCOPE.backend_arguments(controller.state)]
                    inspected = []
                    def ps(command, **kwargs):
                        self.assertEqual(command, ['ps', '-p', '456', '-o', 'stat=', '-o', 'lstart=', '-o', 'command='])
                        inspected.append(command)
                        if state == 'dead' or (root / 'backend/close.request').exists():
                            return subprocess.CompletedProcess(command, 1, b'', b'')
                        argv = words + ['foreign'] if state == 'changed-command' else [str(root / 'foreign-java'), *words[1:]] if state == 'wrong-java' else words
                        start = 'Mon Oct 19 10:00:01 2026' if state == 'changed-start' and len(inspected) > 1 else 'Mon Oct 19 10:00:00 2026'
                        status = 'Z' if state == 'zombie' else 'S'
                        return subprocess.CompletedProcess(command, 0, (status + ' ' + start + ' ' + shlex.join(argv)).encode(), b'')
                    arguments = ['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256,
                                 '--stage', 'record-backend', '--adb', str(root / 'wrong-adb' if failure == 'wrong-adb' else adb)]
                    if state != 'missing-pid': arguments += ['--pid', '1' if state == 'invalid-pid' else '456']
                    with patch.object(qa, 'ManualController', return_value=controller), \
                            patch.object(qa, 'ManualAndroid', return_value=android), patch.object(controller, 'gate'), \
                            patch.object(qa.prior.subprocess, 'run', side_effect=ps), \
                            patch.object(qa.os, 'kill', side_effect=AssertionError('Başka süreç veya gerçek sinyal yasak')):
                        self.assertEqual(qa.main(arguments), 2)
                    self.assertEqual((root / 'install.claim.private.json').read_bytes(), install_claim)
                    cleanup = qa.prior.private_json(root / 'cleanup.private.json')
                    self.assertFalse(credential.exists())
                    self.assertFalse(android.alive)
                    backend = cleanup['results']['backend']
                    if state == 'prepared':
                        self.assertTrue(cleanup['ok'])
                        self.assertEqual(backend['status'], 'PASS')
                        self.assertTrue(backend['result']['processExitConfirmed'])
                        launch = qa.prior.private_json(root / 'backend/launch.private.json')
                        self.assertEqual(launch['pid'], 456)
                        self.assertEqual(launch['processStartIdentity'], 'Mon Oct 19 10:00:00 2026')
                        self.assertEqual(launch['commandSha256'], hashlib.sha256(shlex.join(words).encode()).hexdigest())
                        self.assertTrue((root / 'backend/close.request').exists())
                    else:
                        self.assertFalse(cleanup['ok'])
                        self.assertEqual(backend['status'], 'FAILED')
                        self.assertEqual(backend['code'], {'dead': 'BACKEND_OWNER_NOT_PROVEN', 'zombie': 'BACKEND_OWNER_NOT_PROVEN',
                                'changed-start': 'BACKEND_OWNERSHIP_CHANGED', 'changed-command': 'BACKEND_OWNERSHIP_CHANGED',
                                'wrong-java': 'BACKEND_EXACT_ARGV_CHANGED', 'startup-pid': 'BACKEND_STARTUP_OWNER_CHANGED',
                                'missing-pid': 'BACKEND_PID_REQUIRED', 'invalid-pid': 'BACKEND_PID_REQUIRED',
                                'reservation-run': 'BACKEND_RESERVATION_BINDING_CHANGED', 'classpath-drift': 'BACKEND_CLASSPATH_BINDING_CHANGED',
                                'java-drift': 'BACKEND_JAVA_BINDING_CHANGED'}[state])
                        self.assertFalse((root / 'backend/close.request').exists())
                    failure_receipt = qa.prior.private_json(next((root / 'receipts').glob('terminal-failure-*.private.json')))
                    self.assertEqual(failure_receipt['code'], 'ADB_CLAIM_ARGUMENT_CHANGED' if failure == 'wrong-adb' else 'ATTEMPT_CLOSED_NO_RETRY')
                    if state in ['reservation-run', 'classpath-drift', 'java-drift', 'missing-pid', 'invalid-pid']: self.assertEqual(inspected, [])

    def test_ready_before_first_record_recovers_missing_launch_drains_and_preserves_existing_or_invalid_owner(self):
        import shlex
        import subprocess
        states = ['prepared', 'dead', 'changed-start', 'changed-command', 'startup-pid', 'existing-launch', 'corrupt-launch', 'foreign-launch', 'bad-drain']
        for failure in ['wrong-adb', 'early-failure']:
            for state in states:
                with self.subTest(failure=failure, state=state), tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve()
                    for name in ['receipts', 'android', 'backend', 'java/bin', 'be/src/test/resources/p4']: (root / name).mkdir(parents=True, mode=0o700)
                    controller = qa.ManualController(root, root)
                    fixture_path = root / 'be/src/test/resources/p4/consumer-retained-ready.json'
                    fixture_path.write_bytes((qa.ROOT / 'be/src/test/resources/p4/consumer-retained-ready.json').read_bytes())
                    fixture = json.loads(fixture_path.read_bytes())
                    java = root / 'java/bin/java';java.write_bytes(b'synthetic-bound-java')
                    cp = root / 'receipts/classpath-binding-0001.private.json'
                    qa.prior.write_json(cp, {**qa.SCOPE.identity(), 'entries': [str(root / 'classes')], 'javaBinary': str(java), 'javaBinarySha256': qa.prior.digest(java)})
                    qa.prior.write_json(root / 'receipts/operational-tools.private.json', {'backendJavaHome': str(root / 'java')})
                    source = root / 'receipts/source-binding-0001.private.json';qa.prior.write_json(source, {**qa.SCOPE.identity(), 'sourceDirectory': 'source-0001'})
                    selected = root / 'receipts/selected-build-0001.private.json';qa.prior.write_json(selected, {**qa.SCOPE.identity(), 'apkSha256': 'c' * 64})
                    run = {**qa.SCOPE.identity(), 'priorAttemptProofFile': 'prior-attempt-proof-0001.private.json', 'priorAttemptProofSha256': 'a' * 64,
                           'pythonBindingFile': 'python-binding-0001.private.json', 'pythonBindingSha256': 'b' * 64}
                    for key, path in [('sourceBinding', source), ('classpathBinding', cp), ('selectedBuild', selected)]: run.update({key + 'File': path.name, key + 'Sha256': qa.prior.digest(path)})
                    run_path = root / 'receipts/run-binding-0001.private.json';qa.prior.write_json(run_path, run)
                    adb = root / 'adb';adb.write_bytes(b'synthetic-bound-adb')
                    credential = root / 'synthetic-credential';credential.write_bytes(b'SYNTHETIC_SECRET')
                    android = _ArmOwnedAndroid(adb, credential, 'b' * 64, 'supplied')
                    install_claim = arm_synthetic_controller(controller, android, root)
                    with patch.object(controller, 'gate'), patch.object(controller, 'baseline_gate'): controller.reserve_backend(android)
                    links = controller.binding_links()
                    qa.prior.write_json(root / 'backend/startup.claim.private.json', {**qa.SCOPE.identity(), 'pid': 457 if state == 'startup-pid' else 456, 'runBindingSha256': qa.prior.digest(run_path)})
                    qa.prior.write_json(root / 'backend/local-ready.json', {**fixture, **qa.SCOPE.identity(), **links, 'pid': 456, 'loopbackPort': 12345})
                    words = [str(java), '-Xmx1g', '-cp', str(root / 'classes'), 'com.tellpal.v2.support.P4b5ConsumerQaSetup', *qa.SCOPE.backend_arguments(controller.state)]
                    owner = {'pid': 456, 'processStartIdentity': 'Mon Oct 19 10:00:00 2026', 'commandSha256': hashlib.sha256(shlex.join(words).encode()).hexdigest()}
                    launch = root / 'backend/launch.private.json'
                    if state in ['existing-launch', 'foreign-launch']: qa.prior.write_json(launch, {**qa.SCOPE.identity(), **owner, **({'pid': 457} if state == 'foreign-launch' else {})})
                    if state == 'corrupt-launch': launch.write_bytes(b'{');launch.chmod(0o600)
                    preserved = launch.read_bytes() if launch.exists() else None
                    if preserved is None: self.assertFalse(launch.exists())
                    if failure == 'early-failure': qa.prior.write_json(root / 'backend/failure.json', {'status': 'FAILED_NO_RETRY'})
                    inspected = []
                    def ps(command, **kwargs):
                        self.assertEqual(command, ['ps', '-p', '456', '-o', 'stat=', '-o', 'lstart=', '-o', 'command='])
                        inspected.append(command)
                        if state == 'dead': return subprocess.CompletedProcess(command, 1, b'', b'')
                        if (root / 'backend/close.request').exists():
                            ledger = 'OPEN_PENDING synthetic\nPRIOR_ATTEMPT ' + links['priorAttemptProofSha256'] + '\n' + ''.join(f'DELIVERY_RESERVED {n} {39+n}\n' for n in range(1, 14)) + 'CLOSED 4 13170884 10 1 0 4\n'
                            qa.helpers.write_new(root / 'backend/ledger.log', ledger.encode())
                            qa.prior.write_json(root / 'backend/backend-closed.json', {**qa.SCOPE.identity(), 'pid': 456, 'closed': True, 'drainVerified': state != 'bad-drain',
                                    'outcome': 'SUCCESS', 'bindings': links, 'terminalLedgerSha256': qa.prior.digest(root / 'backend/ledger.log'), 'terminalLedgerLine': ledger.splitlines()[-1],
                                    'deliveryOperations': 13, 'reservedBytes': 13170884, 'finalBudget': {'operations': 52, 'reserved_bytes': 57913854, 'max_operations': 64, 'max_bytes': 67108864}})
                            return subprocess.CompletedProcess(command, 1, b'', b'')
                        argv = words + ['foreign'] if state == 'changed-command' else words
                        start = 'Mon Oct 19 10:00:01 2026' if state == 'changed-start' and len(inspected) > 1 else owner['processStartIdentity']
                        return subprocess.CompletedProcess(command, 0, ('S ' + start + ' ' + shlex.join(argv)).encode(), b'')
                    arguments = ['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256,
                                 '--stage', 'record-backend', '--pid', '456', '--adb', str(root / 'wrong-adb' if failure == 'wrong-adb' else adb)]
                    with patch.object(qa, 'ManualController', return_value=controller), patch.object(qa, 'ManualAndroid', return_value=android), \
                            patch.object(controller, 'gate'), patch.object(qa.prior.subprocess, 'run', side_effect=ps), \
                            patch.object(qa.os, 'kill', side_effect=AssertionError('Gerçek veya yabancı sürece sinyal yasak')):
                        self.assertEqual(qa.main(arguments), 2)
                    self.assertEqual((root / 'install.claim.private.json').read_bytes(), install_claim)
                    if preserved is not None: self.assertEqual(launch.read_bytes(), preserved)
                    self.assertFalse(credential.exists());self.assertFalse(android.alive)
                    cleanup = qa.prior.private_json(root / 'cleanup.private.json');backend = cleanup['results']['backend']
                    succeeds = failure == 'wrong-adb' and state in ['prepared', 'existing-launch']
                    self.assertEqual(cleanup['ok'], succeeds)
                    self.assertEqual(backend['status'], 'PASS' if succeeds else 'FAILED')
                    if succeeds:
                        self.assertTrue(backend['result']['drainVerified']);self.assertTrue(backend['result']['measurementSucceeded'])
                        self.assertEqual(qa.prior.private_json(launch)['pid'], 456)
                    else:
                        expected = {'dead': 'BACKEND_OWNER_NOT_PROVEN', 'changed-start': 'BACKEND_OWNERSHIP_CHANGED', 'changed-command': 'BACKEND_OWNERSHIP_CHANGED',
                                    'startup-pid': 'BACKEND_CLAIM_PID', 'corrupt-launch': 'CLEANUP_FAILED', 'foreign-launch': 'BACKEND_READY_OWNER_CHANGED',
                                    'bad-drain': 'BACKEND_SUCCESS_DRAIN_REQUIRED'}
                        self.assertEqual(backend['code'], 'BACKEND_MEASUREMENT_FAILED' if failure == 'early-failure' and state in ['prepared', 'existing-launch', 'bad-drain'] else expected[state])
                    closed = state in ['prepared', 'existing-launch', 'bad-drain']
                    self.assertEqual((root / 'backend/close.request').exists(), closed)
                    if state in ['dead', 'changed-command']: self.assertFalse(launch.exists())
                    if state in ['corrupt-launch', 'foreign-launch']: self.assertEqual(inspected, [])
                    refusal = qa.prior.private_json(next((root / 'receipts').glob('terminal-failure-*.private.json')))
                    self.assertEqual(refusal['code'], 'ADB_CLAIM_ARGUMENT_CHANGED' if failure == 'wrong-adb' else 'ATTEMPT_CLOSED_NO_RETRY')
                    self.assertEqual(qa.prior.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')

    def test_unavailable_bound_adb_still_attempts_backend_cleanup_without_other_device_command(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            for name in ['receipts', 'backend']: (root / name).mkdir(mode=0o700)
            controller = qa.ManualController(root, root)
            controller.claim('backend-start')
            qa.prior.write_json(root / 'backend/launch.private.json', {**qa.SCOPE.identity(), 'pid': 456, 'processStartIdentity': 'synthetic-start', 'commandSha256': 'a' * 64})
            with patch.object(qa, 'ManualController', return_value=controller), \
                    patch.object(qa, 'ManualAndroid', side_effect=AssertionError('Bağsız ADB yasak')), \
                    patch.object(controller, 'owned_backend_alive', side_effect=lambda owner, timeout=3: not (root / 'backend/close.request').exists()), \
                    patch.object(qa.os, 'kill', side_effect=AssertionError('Gerçek sinyal yasak')):
                self.assertEqual(qa.main(['--scope', qa.SCOPE.measurement_id, '--proposal-sha256', qa.SCOPE.proposal_sha256, '--stage', 'record-backend', '--pid', '1']), 2)
            self.assertTrue(qa.prior.private_json(root / 'cleanup.private.json')['results']['backend']['result']['processExitConfirmed'])


class _RunPhaseAndroid:
    def __init__(self, raws):
        self.raws = raws
        self.acknowledgements = []
        self.configuration = None

    def run(self, *args, timeout=3):
        if args == ('reverse', '--list'): return ''
        if args == ('reverse', '--no-rebind', 'tcp:12345', 'tcp:12345'): return ''
        raise AssertionError(args)

    def owner(self, expected, timeout=3):
        assert expected == {k: json.loads(self.raws[0])[k] for k in qa.IDENTITY_KEYS}

    def failure_marker(self, timeout=3): pass

    def write_configuration(self, value, timeout=3): self.configuration = value.copy()

    def read_receipt(self, index, timeout=3): return self.raws[index]

    def capture_png(self, timeout=3): return b'\x89PNG\r\n\x1a\nSYNTHETIC'

    def capture_ack(self, value, timeout=3):
        self.acknowledgements.append(value.copy())
        return json.dumps(value).encode()


class ManualRealRunWindowTest(unittest.TestCase):
    def test_real_run_accepts_window_boundaries_and_rejects_outside_phase_timestamps_before_accepted_file(self):
        import base64
        from test_p4b5_consumer_qa import expected
        start, deadline = 2000000000000, 2000000120000
        for mode in ['valid', 'at-start', 'before-deadline', 'before-start', 'at-deadline', 'after-deadline']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve()
                for name in ['receipts', 'android', 'backend']: (root / name).mkdir(mode=0o700)
                controller = qa.ManualController(root, root)
                ready = {**expected(), **qa.SCOPE.identity(), 'loopbackPort': 12345,
                         'windowStartedAtEpochMillis': start, 'windowDeadlineEpochMillis': deadline}
                values = [json.loads(raw) for raw in envelopes()]
                for index, value in enumerate(values): value['recordedAtEpochMillis'] = start + index if index else start - 2
                if mode == 'at-start': values[1]['recordedAtEpochMillis'] = start
                if mode == 'before-start': values[1]['recordedAtEpochMillis'] = start - 1
                if mode == 'before-deadline': values[-1]['recordedAtEpochMillis'] = deadline - 1
                if mode == 'at-deadline': values[-1]['recordedAtEpochMillis'] = deadline
                if mode == 'after-deadline': values[-1]['recordedAtEpochMillis'] = deadline + 1
                raws, previous = [], '0' * 64
                for value in values:
                    value['previousSha256'] = previous
                    raw = json.dumps(value).encode();raws.append(raw);previous = hashlib.sha256(raw).hexdigest()
                android = _RunPhaseAndroid(raws)
                owner, _ = qa.validate_receipts([raws[0]], complete=False)
                claims = {'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd', 'sub': qa.prior.UID, 'iat': start // 1000, 'exp': start // 1000 + 600}
                token = 'e30.' + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=') + '.c2ln'
                token_file = root / 'synthetic-token';token_file.write_text(token);token_file.chmod(0o600)
                qa.prior.write_json(root / 'backend/qa-sign-in.ready.json', {'tokenFile': str(token_file)})
                # Yalnız cihaz/backend/giriş kapıları sentetiktir; gerçek run ve tüm kanıt doğrulayıcıları çalışır.
                with patch.object(controller, 'gate'), patch.object(controller, 'ready', return_value=ready), \
                        patch.object(controller, 'baseline_gate', return_value=owner), patch.object(controller, 'backend_alive'), \
                        patch.object(qa.time, 'time', return_value=(start + 1000) / 1000):
                    if mode in ['before-start', 'at-deadline', 'after-deadline']:
                        with self.assertRaisesRegex(ValueError, '^PHASE_OUTSIDE_WINDOW$'): controller.run(android)
                        self.assertFalse((root / 'observations/accepted.private.json').exists())
                    else:
                        controller.run(android)
                        accepted = qa.prior.private_json(root / 'observations/accepted.private.json')
                        self.assertEqual(accepted['phasesAccepted'], qa.prior.PHASES)
                        self.assertEqual(accepted['mediaPayloadBytes'], 13170884)
                self.assertEqual(len(android.acknowledgements), 4)
                self.assertEqual(len(list((root / 'observations').glob('*-*.private.json'))), 14)


class ManualProcessTransportTest(unittest.TestCase):
    def transport(self, mode):
        import subprocess
        calls = [];stopped = [False]
        def run(arguments, **kwargs):
            self.assertGreater(kwargs['timeout'], 0);self.assertLessEqual(kwargs['timeout'], 15)
            command = arguments[3:];calls.append(command)
            if command == ['shell', 'pidof', qa.PACKAGE]:
                value = b'999' if mode == 'different-pid' else b'123' if mode in ['same-discovery', 'discovery-conflict'] and not stopped[0] else b''
                return subprocess.CompletedProcess(arguments, 0 if value else 1, value, b'UNTRUSTED_TOKEN')
            if command == ['shell', 'ls', '-1', '/proc']:
                if mode == 'proc-denied' or (mode == 'after-unreadable' and stopped[0]):
                    return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                present = mode not in ['absent', 'discovery-conflict'] and (not stopped[0] or mode in ['still-alive', 'after-unreadable', 'after-reused'])
                return subprocess.CompletedProcess(arguments, 0, b'self\nstat\n' + (b'123\n' if present else b''), b'')
            if command == ['shell', 'cat', '/proc/123/cmdline']:
                if mode in ['cmdline-denied', 'absent']:
                    return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                if mode == 'timeout': raise subprocess.TimeoutExpired(arguments, kwargs['timeout'], b'UNTRUSTED_TOKEN')
                return subprocess.CompletedProcess(arguments, 0, b'FOREIGN_SECRET_COMMAND' if mode == 'foreign-command' else qa.PACKAGE.encode() + b'\x00', b'')
            if command == ['shell', 'cat', '/proc/123/stat']:
                if mode == 'stat-denied': return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                ticks = '999' if mode == 'different-ticks' or (mode == 'after-reused' and stopped[0]) else '456'
                value = '123 (synthetic) ' + ' '.join(ticks if index == 19 else '0' for index in range(20))
                return subprocess.CompletedProcess(arguments, 0, value.encode(), b'')
            if command == ['shell', 'am', 'force-stop', qa.PACKAGE]:
                stopped[0] = True
                return subprocess.CompletedProcess(arguments, 0, b'', b'')
            raise AssertionError(command)
        return run, calls

    def assert_private_diagnostics(self, android):
        value = qa.safe_android_diagnostics(android)
        self.assertLessEqual(len(value), 8)
        self.assertTrue(all(set(event) == {'operation', 'code'} for event in value))
        self.assertNotIn('UNTRUSTED_TOKEN', json.dumps(value));self.assertNotIn('FOREIGN_SECRET_COMMAND', json.dumps(value))
        self.assertNotIn('/proc', json.dumps(value))
        return value

    def test_real_owner_uses_positive_proc_identity_even_when_discovery_is_empty(self):
        for mode in ['alive', 'same-discovery', 'different-pid', 'different-ticks', 'foreign-command', 'cmdline-denied', 'stat-denied', 'absent', 'timeout']:
            with self.subTest(mode=mode):
                android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554');transport, calls = self.transport(mode)
                with patch.object(qa.subprocess, 'run', side_effect=transport):
                    if mode in ['alive', 'same-discovery']:
                        android.owner({'pid': 123, 'processStartTicks': '456'})
                        self.assertIn(['shell', 'cat', '/proc/123/cmdline'], calls)
                        self.assertIn(['shell', 'cat', '/proc/123/stat'], calls)
                    else:
                        with self.assertRaises(ValueError): android.owner({'pid': 123, 'processStartTicks': '456'})
                self.assert_private_diagnostics(android)

    def test_real_cleanup_requires_explicit_proc_absence_and_never_stops_changed_or_uncertain_owner(self):
        expected_codes = {'different-pid': 'QA_OWNER_CHANGED', 'different-ticks': 'QA_OWNER_CHANGED', 'foreign-command': 'QA_PROCESS_COMMAND_CHANGED',
                          'proc-denied': 'QA_PROCESS_READ_FAILED', 'cmdline-denied': 'QA_PROCESS_READ_FAILED', 'stat-denied': 'QA_PROCESS_READ_FAILED',
                          'discovery-conflict': 'QA_PROCESS_STATE_UNCERTAIN', 'still-alive': 'QA_PROCESS_NOT_STOPPED',
                          'after-unreadable': 'QA_PROCESS_READ_FAILED', 'after-reused': 'QA_OWNER_CHANGED', 'timeout': 'ADB_DEADLINE'}
        for mode in ['alive', 'absent', *expected_codes]:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();(root / 'android').mkdir()
                qa.prior.write_json(root / 'android/owned-process.private.json', {**qa.SCOPE.identity(), 'pid': 123, 'processStartTicks': '456'})
                controller = qa.ManualController(root, root);android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
                transport, calls = self.transport(mode)
                with patch.object(qa.subprocess, 'run', side_effect=transport): result = controller.cleanup(android)['qaProcess']
                stops = calls.count(['shell', 'am', 'force-stop', qa.PACKAGE])
                self.assertEqual(stops, 1 if mode in ['alive', 'still-alive', 'after-unreadable', 'after-reused'] else 0)
                if mode in ['alive', 'absent']:
                    self.assertEqual(result, {'status': 'PASS', 'result': {'processExitConfirmed': True}})
                    self.assertIn({'operation': 'proc_list', 'code': 'PROC_ABSENT'}, self.assert_private_diagnostics(android))
                else:
                    self.assertEqual(result, {'status': 'FAILED', 'code': expected_codes[mode]})
                    self.assertNotIn('processExitConfirmed', result)
                receipt = qa.prior.private_json(root / 'cleanup.private.json')
                self.assertEqual(receipt['androidDiagnostics'], self.assert_private_diagnostics(android))
                self.assertEqual(receipt['ok'], mode in ['alive', 'absent'])

    def test_diagnostics_keep_only_eight_fixed_codes(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        transport, _ = self.transport('alive')
        with patch.object(qa.subprocess, 'run', side_effect=transport):
            for _ in range(12): android.owner({'pid': 123, 'processStartTicks': '456'})
        self.assertEqual(len(self.assert_private_diagnostics(android)), 8)



def scoped_envelopes(selected, start):
    """İki sabit kapsam için aynı sentetik faz zincirini kurar."""
    values = [json.loads(raw) for raw in envelopes()]
    previous, result = '0' * 64, []
    for index, value in enumerate(values):
        value.update(selected.identity(), supportDirectory='/data/user/0/' + selected.package + '/files',
                     recordedAtEpochMillis=start + index if index else start - 2)
        value['sample'].update(selected.identity())
        value['previousSha256'] = previous
        raw = json.dumps(value).encode()
        result.append(raw);previous = hashlib.sha256(raw).hexdigest()
    return result


class _ScopedRunPhaseAndroid(_RunPhaseAndroid):
    def write_configuration(self, value, timeout=3):
        # Controller belleğini temizlemeden önce teslim edilen payload'ı sabitle.
        self.configuration = copy.deepcopy(value)


class ManualBothScopesRunTest(unittest.TestCase):
    def test_real_run_preserves_selected_configuration_receipts_captures_and_window_boundaries(self):
        import base64
        from test_p4b5_consumer_qa import expected
        start, deadline = 2000000000000, 2000000120000
        for selected in [qa.P4B5_MANUAL, qa.P4B5_MANUAL_RETRY]:
            for mode in ['valid', 'at-start', 'before-deadline', 'before-start', 'at-deadline', 'after-deadline']:
                with self.subTest(scope=selected.measurement_id, mode=mode), tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve()
                    for name in ['receipts', 'android', 'backend']: (root / name).mkdir(mode=0o700)
                    controller = qa.ManualController(root, root, selected)
                    ready = {**expected(), **selected.identity(), 'loopbackPort': 12345,
                             'windowStartedAtEpochMillis': start, 'windowDeadlineEpochMillis': deadline}
                    raws = scoped_envelopes(selected, start)
                    stamp = {'at-start': (1, start), 'before-start': (1, start - 1),
                             'before-deadline': (9, deadline - 1), 'at-deadline': (9, deadline),
                             'after-deadline': (9, deadline + 1)}.get(mode)
                    if stamp:
                        index, value = stamp
                        raws = mutate(raws, index, lambda item: item.update(recordedAtEpochMillis=value))
                    android = _ScopedRunPhaseAndroid(raws)
                    owner, _ = qa.validate_receipts([raws[0]], complete=False, selected=selected)
                    claims = {'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd',
                              'sub': qa.prior.UID, 'iat': start // 1000, 'exp': start // 1000 + 600}
                    token = 'e30.' + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=') + '.c2ln'
                    token_file = root / 'synthetic-token';token_file.write_text(token);token_file.chmod(0o600)
                    qa.prior.write_json(root / 'backend/qa-sign-in.ready.json', {'tokenFile': str(token_file)})
                    # Gerçek run/configuration/receipt/ACK kapıları; yalnız dış kaynaklar sentetiktir.
                    with patch.object(controller, 'gate'), patch.object(controller, 'ready', return_value=ready), \
                            patch.object(controller, 'baseline_gate', return_value=owner), patch.object(controller, 'backend_alive'), \
                            patch.object(qa.time, 'time', return_value=(start + 1000) / 1000), \
                            patch.object(qa.prior, 'runtime_configuration', wraps=qa.prior.runtime_configuration) as configure:
                        if mode in ['before-start', 'at-deadline', 'after-deadline']:
                            with self.assertRaisesRegex(ValueError, '^PHASE_OUTSIDE_WINDOW$'): controller.run(android)
                            self.assertFalse((root / 'observations/accepted.private.json').exists())
                        else:
                            controller.run(android)
                            accepted = qa.prior.private_json(root / 'observations/accepted.private.json')
                            for key, value in selected.identity().items(): self.assertEqual(accepted[key], value)
                            self.assertEqual(accepted['phasesAccepted'], qa.prior.PHASES)
                            self.assertEqual(accepted['mediaPayloadBytes'], 13170884)
                            self.assertEqual(accepted['resourceReport']['bufferCreationAttemptsDelta'], '4')
                            self.assertFalse(accepted['physicalQualityAccepted'])
                            self.assertEqual(accepted['captureSha256'], [
                                hashlib.sha256((root / f'observations/capture-{n:02d}.private.json').read_bytes()).hexdigest()
                                for n in range(1, 5)])
                            self.assertEqual(accepted['rawSha256'], {
                                f'{n + 1:02d}-{phase}.private.json': hashlib.sha256(raw).hexdigest()
                                for n, (phase, raw) in enumerate(zip(qa.prior.PHASES, raws))})
                        configure.assert_called_once_with(ready, token, selected)
                    self.assertEqual(android.configuration, {**owner, 'configure': {
                        'action': 'configure', 'runId': selected.identity()['budgetRunId'],
                        'measurementId': selected.measurement_id, 'proposalSha256': selected.proposal_sha256,
                        'uid': qa.prior.UID, 'contentId': 94, 'port': 12345, 'token': token,
                        'windowStartedAtEpochMillis': start, 'windowDeadlineEpochMillis': deadline}})
                    self.assertEqual(qa.prior.private_json(root / 'measurement.claim.private.json'),
                                     {**selected.identity(), 'state': 'RESERVED_NO_AUTOMATIC_RETRY'})
                    for key, value in selected.identity().items():
                        self.assertEqual(qa.prior.private_json(root / 'android/reverse-intent.private.json')[key], value)
                    self.assertEqual(len(android.acknowledgements), 4)
                    for n, phase in enumerate(qa.prior.PHASES):
                        self.assertEqual((root / f'observations/{n + 1:02d}-{phase}.private.json').read_bytes(), raws[n])
                    for n, ack in enumerate(android.acknowledgements, 1):
                        self.assertEqual(ack, {**owner, 'phase': f'paint{n}',
                            'epoch': json.loads(raws[n])['sample']['paintedEpoch'],
                            'receiptSha256': hashlib.sha256(raws[n]).hexdigest(),
                            'pngSha256': hashlib.sha256((root / f'observations/{n:02d}-paint.png').read_bytes()).hexdigest()})
                    self.assertEqual(len(list((root / 'observations').glob('*.png'))), 4)
                    self.assertEqual(len(list((root / 'observations').glob('*-*.private.json'))), 14)


class ManualBothScopesCleanupTest(unittest.TestCase):
    assert_private_diagnostics = ManualProcessTransportTest.assert_private_diagnostics

    def transport(self, selected, mode):
        import subprocess
        calls, stopped = [], [False]
        def run(arguments, **kwargs):
            self.assertGreater(kwargs['timeout'], 0);self.assertLessEqual(kwargs['timeout'], 15)
            command = arguments[3:];calls.append(command)
            self.assertEqual(arguments[:3], ['/synthetic/adb', '-s', 'emulator-5554'])
            if command == ['shell', 'pidof', selected.package]:
                value = b'999' if mode == 'different-pid' else b'123' if mode in ['same-discovery', 'discovery-conflict'] and not stopped[0] else b''
                return subprocess.CompletedProcess(arguments, 0 if value else 1, value, b'UNTRUSTED_TOKEN')
            if command == ['shell', 'ls', '-1', '/proc']:
                if mode == 'proc-denied' or (mode == 'after-unreadable' and stopped[0]):
                    return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                present = mode not in ['absent', 'discovery-conflict'] and (not stopped[0] or mode in ['still-alive', 'after-unreadable', 'after-reused'])
                return subprocess.CompletedProcess(arguments, 0, b'self\nstat\n' + (b'123\n' if present else b''), b'')
            if command == ['shell', 'cat', '/proc/123/cmdline']:
                if mode in ['cmdline-denied', 'absent']:
                    return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                if mode == 'timeout': raise subprocess.TimeoutExpired(arguments, kwargs['timeout'], b'UNTRUSTED_TOKEN')
                return subprocess.CompletedProcess(arguments, 0, b'FOREIGN_SECRET_COMMAND' if mode == 'foreign-command' else selected.package.encode() + b'\x00', b'')
            if command == ['shell', 'cat', '/proc/123/stat']:
                if mode == 'stat-denied': return subprocess.CompletedProcess(arguments, 1, b'', b'UNTRUSTED_TOKEN')
                ticks = '999' if mode == 'different-ticks' or (mode == 'after-reused' and stopped[0]) else '456'
                value = '123 (synthetic) ' + ' '.join(ticks if index == 19 else '0' for index in range(20))
                return subprocess.CompletedProcess(arguments, 0, value.encode(), b'')
            if command == ['shell', 'am', 'force-stop', selected.package]:
                stopped[0] = True
                return subprocess.CompletedProcess(arguments, 0, b'', b'')
            raise AssertionError(command)
        return run, calls

    def test_real_cleanup_checks_proc_and_stops_only_selected_package_for_both_scopes(self):
        expected_codes = {'different-pid': 'QA_OWNER_CHANGED', 'different-ticks': 'QA_OWNER_CHANGED',
            'foreign-command': 'QA_PROCESS_COMMAND_CHANGED', 'proc-denied': 'QA_PROCESS_READ_FAILED',
            'cmdline-denied': 'QA_PROCESS_READ_FAILED', 'stat-denied': 'QA_PROCESS_READ_FAILED',
            'discovery-conflict': 'QA_PROCESS_STATE_UNCERTAIN', 'still-alive': 'QA_PROCESS_NOT_STOPPED',
            'after-unreadable': 'QA_PROCESS_READ_FAILED', 'after-reused': 'QA_OWNER_CHANGED', 'timeout': 'ADB_DEADLINE'}
        for selected in [qa.P4B5_MANUAL, qa.P4B5_MANUAL_RETRY]:
            for mode in ['alive', 'same-discovery', 'absent', *expected_codes]:
                with self.subTest(scope=selected.measurement_id, mode=mode), tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve();(root / 'android').mkdir()
                    qa.prior.write_json(root / 'android/owned-process.private.json', {
                        **selected.identity(), 'pid': 123, 'processStartTicks': '456'})
                    controller = qa.ManualController(root, root, selected)
                    android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554', selected)
                    transport, calls = self.transport(selected, mode)
                    with patch.object(qa.subprocess, 'run', side_effect=transport): result = controller.cleanup(android)['qaProcess']
                    stop_count = 1 if mode in ['alive', 'same-discovery', 'still-alive', 'after-unreadable', 'after-reused'] else 0
                    self.assertEqual(calls.count(['shell', 'am', 'force-stop', selected.package]), stop_count)
                    self.assertIn(['shell', 'pidof', selected.package], calls)
                    self.assertTrue(all(call[-1] == selected.package for call in calls if call[:3] == ['shell', 'am', 'force-stop']))
                    if mode in ['alive', 'same-discovery', 'absent']:
                        self.assertEqual(result, {'status': 'PASS', 'result': {'processExitConfirmed': True}})
                        self.assertIn(['shell', 'ls', '-1', '/proc'], calls)
                        self.assertIn({'operation': 'proc_list', 'code': 'PROC_ABSENT'}, self.assert_private_diagnostics(android))
                    else:
                        self.assertEqual(result, {'status': 'FAILED', 'code': expected_codes[mode]})
                        self.assertNotIn('processExitConfirmed', result)
                    if stop_count:
                        stop_index = calls.index(['shell', 'am', 'force-stop', selected.package])
                        self.assertIn(['shell', 'cat', '/proc/123/cmdline'], calls[:stop_index])
                        self.assertIn(['shell', 'cat', '/proc/123/stat'], calls[:stop_index])
                        self.assertIn(['shell', 'ls', '-1', '/proc'], calls[stop_index + 1:])
                    receipt = qa.prior.private_json(root / 'cleanup.private.json')
                    self.assertEqual(receipt['androidDiagnostics'], self.assert_private_diagnostics(android))
                    self.assertEqual(receipt['ok'], mode in ['alive', 'same-discovery', 'absent'])
                    terminal = qa.prior.private_json(root / 'terminal.private.json')
                    for key, value in selected.identity().items():
                        self.assertEqual(receipt[key], value);self.assertEqual(terminal[key], value)
                    self.assertEqual(terminal['status'], 'FAILED_NO_RETRY')
