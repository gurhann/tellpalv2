"""Yeni sabit kapsam geçmişi, ayrı onayı ve gerçek native kimliğini korur."""
import copy
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

import p4b5_manual_consumer_qa as qa
from p4b5_qa_scope import P4B5_MANUAL_RETRY as RETRY, P4B5_MANUAL as MANUAL
from test_p4b5_manual_consumer_qa import envelopes, mutate


class ManualRetryQaTest(unittest.TestCase):
    def test_new_scope_default_dry_run_and_vm_refusal(self):
        self.assertIs(qa.select_scope(), qa.P4B5)
        self.assertIs(qa.select_scope(RETRY.measurement_id), RETRY)
        with patch.object(qa, 'ManualController', side_effect=AssertionError('IO forbidden')):
            self.assertEqual(qa.main(['--scope', RETRY.measurement_id, '--proposal-sha256', RETRY.proposal_sha256]), 0)
        for selected in [MANUAL, RETRY]:
            with patch.object(qa.prior, 'Controller', side_effect=AssertionError('IO forbidden')):
                self.assertEqual(qa.prior.main(['--scope', selected.measurement_id, '--proposal-sha256', selected.proposal_sha256, '--stage', 'arm']), 2)

    def test_three_prior_chains_hash_all_fifty_files_and_each_missing_hash_refuses(self):
        raw = (qa.ROOT / RETRY.proposal).read_bytes()
        value = json.loads(raw)
        proof = qa.expected_prior_proof(value, RETRY)
        files = {str(Path(previous['privateRoot']) / name): sha.encode()
                 for previous in value['previousAttemptsEvidence'] for name, sha in previous['files'].items()}
        real_sha = hashlib.sha256
        class Digest:
            def __init__(self, data): self.data = data
            def hexdigest(self):
                return self.data.decode() if len(self.data) == 64 else real_sha(self.data).hexdigest()
        with patch.object(qa.helpers, 'private_file', side_effect=lambda path, *a, **kw: files[str(path)]) as reads, patch.object(qa.hashlib, 'sha256', Digest):
            qa.validate_prior_proof(raw, proof, RETRY)
            self.assertEqual(reads.call_count, 50)
            for path in files:
                before = files[path]; files[path] = b'changed'
                with self.subTest(path=path), self.assertRaisesRegex(ValueError, 'PRIOR_ATTEMPT_FILE_CHANGED'):
                    qa.validate_prior_proof(raw, proof, RETRY)
                files[path] = before
        for index in range(3):
            for field, value in [('measurementId', 'old'), ('qaInitialStartsConsumed', 0), ('runBindingSha256', '0' * 64), ('files', {})]:
                invalid = copy.deepcopy(proof); invalid['previousAttemptsEvidence'][index][field] = value
                with self.subTest(index=index, field=field), self.assertRaisesRegex(ValueError, 'PRIOR_ATTEMPT_PROOF_CHANGED'):
                    qa.validate_prior_proof(raw, invalid, RETRY)
        invalid = copy.deepcopy(proof); invalid['previousAttemptsEvidence'].pop()
        with self.assertRaises(ValueError): qa.validate_prior_proof(raw, invalid, RETRY)

    def test_new_package_presence_refuses_before_claim_and_install_never_replaces(self):
        for installed in ['', 'package:/existing.apk']:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp); controller = qa.ManualController(root, root, RETRY)
                android = Mock(); android.pid.return_value = ''; android.package_absent.return_value = not bool(installed)
                # Kurulum sınırına kadar gerçek controller; kurulumdan sonra sentetik hata durdurur.
                def run(*args, **kw):
                    if args[:3] == ('shell', 'pm', 'path'): return installed
                    raise ValueError('INSTALL_BOUNDARY')
                android.run.side_effect = run
                with patch.object(controller, 'gate'), patch.object(controller, 'selected_build', return_value=({'apkSha256': 'a' * 64}, root)), patch.object(controller, 'android_transport', return_value={}):
                    with self.assertRaisesRegex(ValueError, 'QA_PACKAGE_ALREADY_INSTALLED' if installed else 'INSTALL_BOUNDARY'):
                        controller.arm(android)
                self.assertEqual((root / 'install.claim.private.json').exists(), not bool(installed))
                if not installed:
                    self.assertEqual(android.run.call_args.args[:2], ('install', str(root / 'consumer.apk')))
                    self.assertNotIn('-r', android.run.call_args.args)

    def test_package_absence_accepts_only_empty_pm_result_and_no_transport_or_permission_error(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554', RETRY)
        for code, output, errors in [(0, b'', b''), (1, b'', b''), (0, b'package:/installed.apk\n', b''), (1, b'', b'permission denied'), (255, b'', b'')]:
            with self.subTest(code=code, output=output, errors=errors), patch.object(qa.subprocess, 'run', return_value=Mock(returncode=code, stdout=output, stderr=errors)) as run:
                if errors or code not in [0, 1]:
                    with self.assertRaisesRegex(ValueError, 'QA_PACKAGE_QUERY_FAILED'): android.package_absent()
                else: self.assertEqual(android.package_absent(), output == b'')
                self.assertEqual(run.call_args.args[0][-4:], ['shell', 'pm', 'path', RETRY.package])

    def test_selected_adapter_process_and_private_commands_use_only_new_package(self):
        adapter = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554', RETRY)
        with patch.object(qa.subprocess, 'run', return_value=Mock(returncode=0, stdout=b'OK\n')) as run:
            adapter.private_shell('true')
            self.assertIn(RETRY.package, run.call_args.args[0][-1])
            self.assertNotIn(MANUAL.package + ' ', run.call_args.args[0][-1])
        with patch.object(adapter, '_process_query', return_value=Mock(returncode=0, stdout=b'123')) as read:
            self.assertEqual(adapter.pid(), '123')
            self.assertEqual(read.call_args.args[0], ['shell', 'pidof', RETRY.package])

    def test_exact_separate_execution_approval_rejects_missing_extra_and_types(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); (root / 'receipts').mkdir(); run = root / 'run'; run.write_bytes(b'synthetic-run')
            controller = qa.ManualController(root, root, RETRY)
            approved = {**RETRY.identity(), 'decision': 'APPROVE_ONE_REAL_MANUAL_ATTEMPT', 'runBindingSha256': qa.prior.digest(run), 'apkSha256': 'a' * 64}
            with patch.object(controller, 'selected_build', return_value=({'apkSha256': 'a' * 64}, root)):
                with self.assertRaises(FileNotFoundError): controller.execution_approval_gate(run)
                for value in [approved, {**approved, 'note': 'human text'}, {k:v for k,v in approved.items() if k != 'decision'}, {**approved, 'decision': True}]:
                    with patch.object(controller, 'receipt', return_value=value):
                        if value == approved: controller.execution_approval_gate(run)
                        else:
                            with self.assertRaisesRegex(ValueError, 'SEPARATE_REAL_EXECUTION_APPROVAL_REQUIRED'): controller.execution_approval_gate(run)
            self.assertFalse(list(root.glob('*.claim.private.json')))

    def test_receipt_outer_relabel_cannot_hide_old_inner_native_identity(self):
        values = [json.loads(raw) for raw in envelopes()]
        previous = '0' * 64; raws = []
        for value in values:
            value.update(RETRY.identity(), supportDirectory='/data/user/0/' + RETRY.package + '/files', previousSha256=previous)
            value['sample'].update(RETRY.identity())
            raw = json.dumps(value).encode(); raws.append(raw); previous = hashlib.sha256(raw).hexdigest()
        qa.validate_receipts(raws, selected=RETRY)
        for field, value in MANUAL.identity().items():
            if field == 'budgetRunId': continue
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, 'RECEIPT_SAMPLE_SCOPE_CHANGED'):
                qa.validate_receipts(mutate(raws, 3, lambda item: item['sample'].update({field: value})), selected=RETRY)
        with self.assertRaises(ValueError): qa.validate_receipts(raws, selected=MANUAL)

    def test_loaded_owned_module_and_extra_old_import_entry_cannot_be_whitelisted(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp).resolve(); selected = root / 'source-0002'; old = root / 'source'
            selected.mkdir(); old.mkdir()
            controller = selected / 'p4b5_manual_consumer_qa.py'
            current = selected / 'p4b5_consumer_qa.py'; stale = old / current.name
            current.write_bytes(b'new'); stale.write_bytes(b'old')
            files = {str(current): qa.prior.digest(current)}
            module = SimpleNamespace(__name__='p4b5_consumer_qa', __file__=str(current))
            qa.validate_owned_imports(controller, files, [module])
            # Hash listesi hem yeni hem eski core'u içerince de eski import meşru olmaz.
            both = {**files, str(stale): qa.prior.digest(stale)}
            for loaded, binding in [(module, both), (SimpleNamespace(__name__=module.__name__, __file__=str(stale)), files), (SimpleNamespace(__name__=module.__name__, __file__=str(stale)), both), (SimpleNamespace(__name__=module.__name__), files)]:
                with self.assertRaisesRegex(ValueError, 'PYTHON_OWNED_IMPORT_OUTSIDE_SELECTED_SOURCE'):
                    qa.validate_owned_imports(controller, binding, [loaded])

    @unittest.skipUnless(os.environ.get('P4B5_MANUAL_RETRY_DART_JSON_LOG'), 'Yeni retry Dart JSON logu seçilmedi')
    def test_fresh_retry_native_json_inner_and_outer_identities_are_bound(self):
        found = []
        for line in Path(os.environ['P4B5_MANUAL_RETRY_DART_JSON_LOG']).read_text().splitlines():
            prefix = 'P4B5_MANUAL_RETRY_RESOURCE_JSON:'
            if prefix not in line: continue
            value = json.loads(line.split(prefix, 1)[1])
            self.assertEqual(qa.validate_native_envelope(value, RETRY)['bufferCreationAttemptsDelta'], '4')
            found.append(value)
            for target in ['outer', 'inner', 'missing']:
                invalid = copy.deepcopy(value)
                if target == 'outer': invalid['measurementId'] = MANUAL.measurement_id
                elif target == 'inner': invalid['records'][1]['sample']['proposalSha256'] = MANUAL.proposal_sha256
                else: invalid['records'][0]['sample'].pop('measurementId')
                with self.assertRaises(ValueError): qa.validate_native_envelope(invalid, RETRY)
        self.assertEqual(len(found), 1)
