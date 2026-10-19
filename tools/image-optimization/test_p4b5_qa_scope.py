"""Kapalı scope matrisi; dosya/kimlik/argv sapması yan etkiden önce reddedilir."""
import copy
import dataclasses
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import p4b5_qa_scope as scopes
import p4b5_consumer_qa as qa


class P4b5ScopeTest(unittest.TestCase):
    def test_only_two_immutable_choices_preserve_legacy_default_and_argv(self):
        self.assertIs(scopes.select_scope(), scopes.P4B5)
        for selected in scopes.SCOPES:
            self.assertIs(scopes.select_scope(selected.measurement_id), selected)
            with self.assertRaises(dataclasses.FrozenInstanceError): selected.entry = 'other.dart'
            expected = ([] if selected is scopes.P4B5 else ['--scope', selected.measurement_id]) + ['--execute', '/synthetic/backend']
            self.assertEqual(selected.backend_arguments(Path('/synthetic/backend')), expected)
            proposal = qa.ROOT / selected.proposal
            self.assertEqual(qa.digest(proposal), selected.proposal_sha256)
        self.assertEqual(qa.scope(), scopes.P4B5.identity())
        self.assertEqual(qa.machine_approval(scopes.P4B5_RETRY)['measurementId'], scopes.P4B5_RETRY.measurement_id)

    def test_unknown_scope_never_reads_or_writes_or_constructs_controller(self):
        for args in [['--scope', 'unknown'], ['--scope', 'unknown', '--stage', 'arm']]:
            with patch.object(qa, 'Controller', side_effect=AssertionError('IO forbidden')), patch.object(qa, 'private_json', side_effect=AssertionError):
                self.assertEqual(qa.main(args), 2)

    def test_closed_both_scopes_reject_main_before_failure_cleanup_or_device(self):
        for selected in scopes.SCOPES:
            for marker in ['terminal.private.json', 'cleanup.claim.private.json', 'cleanup.private.json', 'backend/close.request', 'backend/backend-closed.json', 'backend/failure.json']:
                with tempfile.TemporaryDirectory() as directory:
                    root = Path(directory);(root / 'backend').mkdir();(root / marker).write_text('untouched')
                    c = qa.Controller(root, root, selected=selected)
                    before = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}
                    with patch.object(qa, 'Controller', return_value=c), patch.object(qa.helpers, 'Android', side_effect=AssertionError('ADB forbidden')), patch.object(qa, 'numbered', side_effect=AssertionError('write forbidden')):
                        self.assertEqual(qa.main(['--scope', selected.measurement_id, '--proposal-sha256', selected.proposal_sha256, '--stage', 'arm', '--adb', '/synthetic/adb']), 2)
                    self.assertEqual(before, {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()})

    def test_wrong_root_and_mixed_proposal_do_not_make_failure_receipts(self):
        for selected in scopes.SCOPES:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory);c = qa.Controller(root, root, selected=selected)
                with self.assertRaisesRegex(ValueError, 'PRIVATE_ROOT_REQUIRED'): c.gate()
                with patch.object(qa, 'Controller', return_value=c), patch.object(qa.helpers, 'Android', side_effect=AssertionError):
                    self.assertEqual(qa.main(['--scope', selected.measurement_id, '--stage', 'arm', '--proposal-sha256', 'wrong']), 2)
                self.assertEqual(list(root.iterdir()), [])

    def test_prior_proof_exact_metadata_and_all_twelve_hashes_are_required(self):
        proposal = (qa.ROOT / scopes.P4B5_RETRY.proposal).read_bytes()
        value = json.loads(proposal);proof = qa.expected_prior_proof(value)
        with patch.object(qa, 'validate_prior_files') as files_gate:
            qa.validate_prior_proof(proposal, proof)
            files_gate.assert_called_once_with(value['previousAttemptEvidence'])
            for field in ['historicalEvidence', 'priorConsumption', 'previousAttemptEvidence']:
                invalid = copy.deepcopy(proof);invalid[field] = {}
                with self.assertRaises(ValueError): qa.validate_prior_proof(proposal, invalid)
            for field in ['previousAttemptVerifiedByFileHashes', 'currentDatabaseQueried', 'oldBackendLedgerExists', 'actualStagesExecuted']:
                invalid = copy.deepcopy(proof);invalid[field] = 0
                with self.assertRaises(ValueError): qa.validate_prior_proof(proposal, invalid)
        data = {str(scopes.P4B5.private_root / ('file-' + str(n))): ('synthetic-' + str(n)).encode() for n in range(12)}
        previous = {'privateRoot': str(scopes.P4B5.private_root), 'files': {Path(k).name: qa.hashlib.sha256(v).hexdigest() for k, v in data.items()}}
        with patch.object(qa.helpers, 'private_file', side_effect=lambda path, *args, **kwargs: data[str(path)]):
            qa.validate_prior_files(previous)
            for name in data:
                original = data[name];data[name] = b'drift'
                with self.assertRaisesRegex(ValueError, 'PRIOR_ATTEMPT_FILE_CHANGED'): qa.validate_prior_files(previous)
                data[name] = original

    def test_retry_arm_real_gate_rejects_run_bound_receipt_drift_before_install_or_device(self):
        for damaged in ['priorAttemptProof', 'pythonBinding']:
            with self.subTest(receipt=damaged), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();source = root / 'source'
                for path in [source, root / 'receipts', root / 'reviews']:
                    path.mkdir(mode=0o700)
                # Yalnız sentetik fixture kökü değişir; kapsam kimliği ve gerçek gate korunur.
                selected = dataclasses.replace(scopes.P4B5_RETRY, private_root=root)
                identity = selected.identity()
                proposal = source / selected.proposal;proposal.parent.mkdir(parents=True)
                proposal.write_bytes((qa.ROOT / selected.proposal).read_bytes())
                one = source / 'one.py';one.write_text('synthetic-source')
                source_path = root / 'receipts/source-binding-0001.private.json'
                qa.write_json(source_path, {**identity, 'files': {'one.py': qa.digest(one)}})
                qa.write_json(root / 'receipts/human-approval-01.private.json', {**identity, 'answer': 'A — Planı ve tek sınırlı denemeyi onayla'})
                binary = root / 'One.class';binary.write_text('synthetic-class')
                classpath_path = root / 'receipts/classpath-binding-0001.private.json'
                qa.write_json(classpath_path, {**identity, 'files': {str(binary): qa.digest(binary)}})
                build_root = root / 'android/builds/build-0001';build_root.mkdir(parents=True)
                apk = build_root / 'consumer.apk';apk.write_bytes(b'synthetic-apk')
                qa.write_json(build_root / 'build.private.json', {**identity, 'entry': selected.entry, 'package': qa.PACKAGE,
                    'apkSha256': qa.digest(apk), 'sourceBindingSha256': qa.digest(source_path)})
                selected_path = root / 'receipts/selected-build-0001.private.json'
                qa.write_json(selected_path, {**identity, 'buildName': 'build-0001', 'apkSha256': qa.digest(apk)})
                prior_path = root / 'receipts/prior-attempt-proof-0001.private.json'
                qa.write_json(prior_path, qa.expected_prior_proof(json.loads(proposal.read_bytes())))
                python_path = root / 'receipts/python-binding-0001.private.json'
                qa.write_json(python_path, {**identity, 'synthetic': True})
                run = {**identity}
                for name, path in [('sourceBinding', source_path), ('classpathBinding', classpath_path),
                                   ('selectedBuild', selected_path), ('priorAttemptProof', prior_path), ('pythonBinding', python_path)]:
                    run[name + 'File'] = path.name;run[name + 'Sha256'] = qa.digest(path)
                run_path = root / 'receipts/run-binding-0001.private.json';qa.write_json(run_path, run)
                for index in range(1, 4):
                    qa.write_json(root / f'reviews/pre-run-0001-{index:02d}.private.json', {**identity,
                        'sourceBindingSha256': qa.digest(source_path), 'runBindingSha256': qa.digest(run_path),
                        'status': 'PASS', 'unresolvedFindings': 0, 'reviewerId': f'synthetic-reviewer-{index}'})
                # İncelenen run/review zinciri sabittir; yalnız bağlı makbuz sonradan bozulur.
                path = prior_path if damaged == 'priorAttemptProof' else python_path
                path.write_text(json.dumps({**identity, 'tampered': True}))
                before = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}
                android = Mock();android.pid.return_value = '999'
                with patch.object(qa, 'P4B5_RETRY', selected), \
                        patch.object(qa, 'validate_prior_files') as historical_files:
                    controller = qa.Controller(root=root / 'source', selected=selected)
                    with patch.object(controller, 'retry_bindings', wraps=controller.retry_bindings) as retry_gate:
                        with self.assertRaisesRegex(ValueError, '^REVIEWED_RECEIPT_CHANGED$'):
                            controller.arm(android)
                        retry_gate.assert_called_once_with(run)
                    if damaged == 'pythonBinding': historical_files.assert_called_once()
                    else: historical_files.assert_not_called()
                self.assertEqual(android.mock_calls, [])
                self.assertFalse((root / 'install.claim.private.json').exists())
                self.assertFalse((root / 'initial-start.claim.private.json').exists())
                self.assertEqual(before, {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()})

    def test_python_binary_controller_import_and_command_prefix_mutations(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve();source = root / 'source';helper = source / 'tools/image-optimization';helper.mkdir(parents=True)
            binary = root / 'python';binary.write_bytes(b'synthetic-python')
            for name in ['p4b5_consumer_qa.py', 'p4b5_qa_scope.py', 'p4_consumer_qa.py', 'p4_resource_observation.py', 'p4_reading_comparison.py', 'native_test_login.py', 'native_entry_config.py']:
                (helper / name).write_text('synthetic-' + name)
            binding = {**scopes.P4B5_RETRY.identity(), 'pythonBinary': str(binary), 'pythonExecutable': str(binary), 'pythonBinarySha256': qa.digest(binary),
                       'controller': str(helper / 'p4b5_consumer_qa.py'), 'controllerSha256': qa.digest(helper / 'p4b5_consumer_qa.py'),
                       'files': {str(p): qa.digest(p) for p in helper.iterdir()}}
            run = {'pythonCommandPrefix': [str(binary), binding['controller'], '--scope', scopes.P4B5_RETRY.measurement_id, '--proposal-sha256', scopes.P4B5_RETRY.proposal_sha256]}
            qa.validate_python_binding(binding, scopes.P4B5_RETRY, source, run, runtime=False)
            for mode in ['scope', 'binary', 'controller', 'import', 'missing-import', 'prefix']:
                altered = copy.deepcopy(binding);changed_run = copy.deepcopy(run)
                if mode == 'scope': altered['measurementId'] = scopes.P4B5.measurement_id
                if mode == 'binary': altered['pythonBinarySha256'] = 'b' * 64
                if mode == 'controller': altered['controllerSha256'] = 'b' * 64
                if mode == 'import': altered['files'][str(helper / 'p4b5_qa_scope.py')] = 'b' * 64
                if mode == 'missing-import': altered['files'].pop(str(helper / 'native_test_login.py'))
                if mode == 'prefix': changed_run['pythonCommandPrefix'].append('extra')
                with self.subTest(mode=mode), self.assertRaises(ValueError): qa.validate_python_binding(altered, scopes.P4B5_RETRY, source, changed_run, runtime=False)

    def test_runtime_configuration_requires_selected_ready_scope_before_token_or_media(self):
        import base64,time
        payload = {'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd', 'sub': qa.UID, 'iat': int(time.time()) - 10, 'exp': int(time.time()) + 600}
        token = 'e30.' + base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip('=') + '.c2ln'
        for selected in scopes.SCOPES:
            ready = {**selected.identity(), 'loopbackPort': 12345, 'windowStartedAtEpochMillis': int(time.time() * 1000) - 1000, 'windowDeadlineEpochMillis': int(time.time() * 1000) - 1000 + 120000}
            self.assertEqual(qa.runtime_configuration(ready, token, selected)['measurementId'], selected.measurement_id)
            for field in ['proposalSha256', 'measurementId', 'budgetRunId']:
                altered = {**ready, field: 'wrong'}
                with self.subTest(scope=selected.measurement_id, field=field), self.assertRaisesRegex(ValueError, 'RUNTIME_SCOPE_CHANGED'):
                    qa.runtime_configuration(altered, token, selected)

    def test_backend_exact_scope_tail_refuses_cross_scope_or_extra_arguments(self):
        selected = scopes.P4B5_RETRY
        with tempfile.TemporaryDirectory() as directory:
            c = qa.Controller(Path(directory), Path(directory), selected=selected)
            for args in [scopes.P4B5.backend_arguments(c.state), [*selected.backend_arguments(c.state), 'extra']]:
                output = ('S Mon Oct 19 10:00:00 2026 /synthetic/java com.tellpal.v2.support.P4b5ConsumerQaSetup ' + ' '.join(args)).encode()
                result = type('Result', (), {'returncode': 0, 'stdout': output})()
                with patch.object(qa.subprocess, 'run', return_value=result), self.assertRaisesRegex(ValueError, 'BACKEND_OWNERSHIP_CHANGED'): c.backend_identity(100)

if __name__ == '__main__': unittest.main()
