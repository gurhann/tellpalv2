"""Yeni ölçümün typed/phase/kota ve tekrar kapıları; gerçek hak tüketmez."""
import copy
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import p4b5_consumer_qa as qa
from test_p4_consumer_qa import expected, snapshot


def records(baseline=7):
    result = []
    for index, phase in enumerate(qa.PHASES):
        sample = snapshot(min(max(index - 1, 0), 3)) if index else {'events': [], 'cubitCount': 0, 'status': None}
        for key in qa.ACTIVE:
            sample[key] = 0
        sample.update(ok=True, configured=index > 0, stopped=phase == 'stopped', cubitClosed=phase in ['closedUi', 'stopped'],
                      credentialActive=0 < index < 9, deadlineActive=0 < index < 9)
        loaded = 1 <= index <= 5
        sample['activeImages'] = 1 if loaded else 0
        if index > 5:
            sample['status'] = 'background'
        state = next((e for e in reversed(sample['events']) if e.get('kind') == 'state'), None)
        decoded = state['decodedWidth'] * state['decodedHeight'] * 4 if loaded else 0
        sample.update(observedLifecycle='paused' if phase == 'background' else 'resumed',
                      currentEpoch=(min(index, 4) if loaded else (None if index == 0 else 0)),
                      lastPaintedEpoch=(None if index == 0 else min(index, 4)), liveRasterAttached=loaded)
        sample['resourceObservation'] = {'version': 1, 'lifetimeId': 'one_isolate', 'reservedSlots': int(loaded),
            'reservedDecodedBytes': str(decoded), 'bufferCreationAttempts': str(baseline + min(index, 4))}
        events = []
        ordinal = 0
        for event in sample['events']:
            if event['kind'] == 'http':
                ordinal += 1
                events.append({'kind': 'httpReservation', 'endpoint': event['endpoint'], 'ordinal': ordinal})
            events.append(event)
        sample['events'] = events
        result.append({'phase': phase, 'sample': sample})
    return result


class P4b5AcceptanceTest(unittest.TestCase):
    def test_default_has_zero_file_credential_device_or_network_io(self):
        with patch.object(qa.Controller, 'gate', side_effect=AssertionError), patch.object(qa, 'private_json', side_effect=AssertionError):
            self.assertEqual(qa.main([]), 0)

    def test_nonzero_baseline_four_paints_and_stopped_false_drain_are_measured(self):
        result = qa.validate_samples(records(), expected())
        self.assertEqual(result['resourceReport']['baselineBufferCreationAttempts'], '7')
        self.assertEqual(result['resourceReport']['finalBufferCreationAttempts'], '11')
        self.assertEqual(result['resourceReport']['bufferCreationAttemptsDelta'], '4')
        self.assertEqual(result['mediaPayloadBytes'], 13170884)
        self.assertFalse(result['physicalQualityAccepted'])

    def test_attempt_scope_extra_decode_lifetime_reset_and_legacy_matrix(self):
        for change in [
            lambda r: r[2]['sample']['resourceObservation'].update(bufferCreationAttempts='8'),
            lambda r: r[6]['sample']['resourceObservation'].update(bufferCreationAttempts='12'),
            lambda r: r[6]['sample']['resourceObservation'].update(bufferCreationAttempts='1'),
            lambda r: r[3]['sample']['resourceObservation'].update(lifetimeId='another'),
            lambda r: r[2]['sample'].pop('resourceObservation'),
            lambda r: r[2]['sample']['events'][-1].update(decodedWidth=1),
            lambda r: r.reverse(),
            lambda r: [item['sample'].pop('resourceObservation') for item in r],
        ]:
            value = records(); change(value)
            with self.subTest(change=change), self.assertRaises(ValueError):
                qa.validate_samples(value, expected())

    def test_fifth_native_attempt_stays_terminal_through_all_drain_phases(self):
        value = records()
        for item in value[6:]:
            item['sample']['resourceObservation']['bufferCreationAttempts'] = '12'
        with self.assertRaises(ValueError): qa.validate_samples(value, expected())

    def test_phase_drain_is_required_for_every_handle_slot_byte_even_stopped_false(self):
        for phase in ['background', 'resumed', 'closedUi', 'stopped']:
            for key in qa.ACTIVE + ['reservedSlots', 'reservedDecodedBytes']:
                value = records(); sample = value[qa.PHASES.index(phase)]['sample']
                sample['stopped'] = False
                if key in qa.ACTIVE:
                    sample[key] = 1
                else:
                    sample['resourceObservation'][key] = 1 if key == 'reservedSlots' else '4'
                with self.subTest(phase=phase, key=key), self.assertRaises(ValueError):
                    qa.validate_samples(value, expected())

    def test_every_control_and_http_ok_requires_exact_boolean(self):
        for flag in qa.FLAGS + ['drained']:
            for invalid in [None, 0, 1, 'true', 'false', [], {}]:
                value = records(); value[6]['sample'][flag] = invalid
                with self.subTest(flag=flag, invalid=invalid), self.assertRaises(ValueError):
                    qa.validate_samples(value, expected())
            if flag != 'drained':
                value = records(); value[6]['sample'].pop(flag)
                with self.assertRaises(ValueError): qa.validate_samples(value, expected())
        for invalid in [None, 0, 1, 'true', [], False]:
            value = records(); value[1]['sample']['events'][1]['ok'] = invalid
            with self.assertRaises(ValueError): qa.validate_samples(value, expected())

    def test_string_configured_flag_in_drain_phase_is_rejected_by_type_guard(self):
        value = records(); value[6]['sample']['configured'] = 'true'
        with self.assertRaises(ValueError): qa.validate_samples(value, expected())

    def test_post_background_completion_is_allowed_but_new_reservation_is_not(self):
        value = records()
        for record in value[6:]:
            record['sample']['events'].append({'kind': 'httpReservation', 'endpoint': 'status', 'ordinal': 6})
        with self.assertRaisesRegex(ValueError, 'BACKGROUND_NEW_RESERVATION'):
            qa.validate_samples(value, expected())

    def test_raw_snapshots_remain_byte_equivalent_after_acceptance(self):
        value = records(); before = json.dumps(value, sort_keys=True)
        qa.validate_samples(value, expected())
        self.assertEqual(json.dumps(value, sort_keys=True), before)

    def test_claims_are_create_new_0600_and_never_reopen_after_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            controller = qa.Controller(root, root)
            controller.claim('login')
            first = (root / 'login.claim.private.json').read_bytes()
            with self.assertRaises(FileExistsError): controller.claim('login')
            self.assertEqual((root / 'login.claim.private.json').read_bytes(), first)
            self.assertEqual((root / 'login.claim.private.json').stat().st_mode & 0o777, 0o600)

    def test_install_reservation_precedes_call_and_failure_closes_replay(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve(); (root / 'receipts').mkdir(mode=0o700)
            (root / 'android').mkdir(mode=0o700)
            controller = qa.Controller(root, root)
            seen = []
            class Android:
                def __init__(self, *args): pass
                def preflight(self): return {}
                def pid(self): return ''
                def run(self, *args, **kwargs):
                    seen.append(args)
                    self_case.assertTrue((root / 'install.claim.private.json').exists())
                    raise ValueError('SYNTHETIC_INSTALL_FAILURE')
            self_case = self
            with patch.object(qa, 'Controller', return_value=controller), patch.object(controller, 'gate'), \
                    patch.object(controller, 'selected_build', return_value=({'apkSha256': 'a' * 64}, root)), \
                    patch.object(qa.helpers, 'Android', Android):
                args = ['--stage', 'arm', '--proposal-sha256', qa.PROPOSAL_SHA, '--adb', '/synthetic/adb']
                self.assertEqual(qa.main(args), 2)
                self.assertEqual(qa.main(args), 2)
            self.assertEqual(len(seen), 1)
            self.assertFalse((root / 'initial-start.claim.private.json').exists())
            self.assertTrue((root / 'terminal.private.json').exists())
            self.assertEqual(qa.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')

    def test_retry_installed_apk_sha_is_checked_before_initial_start_claim(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve();(root / 'receipts').mkdir();(root / 'android').mkdir()
            c = qa.Controller(root, root, selected=qa.P4B5_RETRY);calls = []
            class Android:
                def preflight(self): pass
                def pid(self): return ''
                def run(self, *args, **kwargs):
                    calls.append(args)
                    if args[:3] == ('shell', 'pm', 'path'): return 'package:/data/app/synthetic/base.apk'
                    if args[:2] == ('shell', 'sha256sum'): return 'b' * 64 + ' /data/app/synthetic/base.apk'
                    if args[:3] == ('shell', 'am', 'start'): raise AssertionError('First start forbidden')
                    return ''
            with patch.object(c, 'gate'), patch.object(c, 'selected_build', return_value=({'apkSha256': 'a' * 64}, root)), self.assertRaisesRegex(ValueError, 'INSTALLED_APK_DRIFT'):
                c.arm(Android())
            self.assertTrue((root / 'install.claim.private.json').exists())
            self.assertFalse((root / 'initial-start.claim.private.json').exists())
            self.assertEqual(qa.private_json(root / 'install.claim.private.json')['measurementId'], qa.P4B5_RETRY.measurement_id)
            self.assertEqual([args[0] for args in calls], ['install', 'shell', 'shell'])

    def test_scope_source_and_three_independent_reviews_gate_before_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve(); (root / 'receipts').mkdir(mode=0o700)
            (root / 'reviews').mkdir(mode=0o700); (root / 'source').mkdir(mode=0o700)
            (root / qa.PROPOSAL).parent.mkdir(parents=True)
            (root / qa.PROPOSAL).write_bytes((qa.ROOT / qa.PROPOSAL).read_bytes())
            (root / 'one.py').write_text('synthetic')
            (root / 'source/one.py').write_text('synthetic')
            qa.write_json(root / 'receipts/human-approval-01.private.json', {**qa.scope(), 'answer': 'A — Planı ve tek sınırlı denemeyi onayla'})
            binding = {**qa.scope(), 'files': {'one.py': qa.digest(root / 'one.py')}}
            qa.write_json(root / 'receipts/source-binding-0001.private.json', binding)
            qa.write_json(root / 'receipts/selected-build-0001.private.json', {})
            qa.write_json(root / 'receipts/classpath-binding-0001.private.json', {'files': {str(root / 'one.py'): qa.digest(root / 'one.py')}})
            qa.write_json(root / 'receipts/run-binding-0001.private.json', {**qa.scope(),
                'sourceBindingSha256': qa.digest(root / 'receipts/source-binding-0001.private.json'),
                'sourceBindingFile': 'source-binding-0001.private.json', 'classpathBindingFile': 'classpath-binding-0001.private.json', 'selectedBuildFile': 'selected-build-0001.private.json',
                'selectedBuildSha256': qa.digest(root / 'receipts/selected-build-0001.private.json'),
                'classpathBindingSha256': qa.digest(root / 'receipts/classpath-binding-0001.private.json')})
            for index in range(1, 4):
                qa.write_json(root / f'reviews/pre-run-0001-{index:02d}.private.json', {**qa.scope(),
                    'sourceBindingSha256': qa.digest(root / 'receipts/source-binding-0001.private.json'),
                    'runBindingSha256': qa.digest(root / 'receipts/run-binding-0001.private.json'),
                    'status': 'PASS', 'unresolvedFindings': 0, 'reviewerId': f'reviewer-{index}'})
            controller = qa.Controller(root, root)
            with patch.object(qa, 'PRIVATE', root):
                self.assertEqual(controller.gate(), binding)
                (root / 'source/one.py').write_text('drift')
                with self.assertRaisesRegex(ValueError, 'SOURCE_DRIFT'): controller.gate()
                (root / 'source/one.py').write_text('synthetic')
                qa.write_json(root / 'terminal.private.json', {**qa.scope(), 'status': 'FAILED_NO_RETRY'})
                with self.assertRaisesRegex(ValueError, 'ATTEMPT_CLOSED_NO_RETRY'): controller.gate()

    def test_real_local_pre_ready_cleanup_uses_pid_start_and_stops_owned_synthetic_child(self):
        import subprocess
        import sys
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve(); state = root / 'backend'; state.mkdir(mode=0o700)
            controller = qa.Controller(root, root)
            qa.write_json(root / 'backend-start.claim.private.json', qa.scope())
            # Gerçek Java/backend/SDK çalıştırılmaz; argv yalnız sahiplik parser'ını sınar.
            settled = root / 'child-ready'
            child = subprocess.Popen([sys.executable, '-c', 'import sys,time; from pathlib import Path; Path(sys.argv[1]).write_text("READY"); time.sleep(15)', str(settled),
                'com.tellpal.v2.support.P4b5ConsumerQaSetup', '--execute', str(state)],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            try:
                # macOS Python launcher exec'i bitmeden alınan argv hash'i kararlı değildir.
                import time
                end = time.monotonic() + 3
                while not settled.exists() and time.monotonic() < end: time.sleep(.01)
                self.assertTrue(settled.exists())
                owner = controller.record_backend_launch(child.pid)
                self.assertEqual(owner['pid'], child.pid)
                qa.write_json(state / 'startup.claim.private.json', {**qa.scope(), 'pid': child.pid})
                result = controller.cleanup_before_ready()
                child.wait(timeout=2)
                self.assertTrue(result['processExitConfirmed'])
                self.assertFalse(result['drainVerified']);self.assertFalse(result['measurementSucceeded'])
                self.assertFalse((state / 'backend-closed.json').exists())
            finally:
                if child.poll() is None: child.kill()
                child.wait(timeout=2)

    def test_pre_ready_owned_backend_cleanup_is_bounded_and_never_claims_drain(self):
        self.exercise_pre_ready('stop')

    def test_pre_ready_backend_replacement_never_receives_a_signal(self):
        self.exercise_pre_ready('replacement')

    def test_pre_ready_backend_startup_claim_must_match_launch_owner(self):
        self.exercise_pre_ready('wrong-claim')

    def test_pre_ready_backend_already_exited_needs_no_signal(self):
        self.exercise_pre_ready('exited')

    def test_pre_ready_backend_timeout_is_failure_after_owned_term_kill_only(self):
        self.exercise_pre_ready('timeout')

    def exercise_pre_ready(self, mode):
        import signal
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve(); state = root / 'backend'; state.mkdir(mode=0o700)
            controller = qa.Controller(root, root)
            owner = {'pid': 99999, 'processStartIdentity': 'Mon Oct 19 10:00:00 2026', 'commandSha256': 'a' * 64}
            qa.write_json(root / 'backend-start.claim.private.json', qa.scope())
            clock = [0.0]; signals = []; exited = [mode == 'exited']; observations = [0]
            def inspect(pid, timeout=3):
                observations[0] += 1
                if exited[0]: return None
                if mode == 'replacement' and observations[0] > 2:
                    return {**owner, 'processStartIdentity': 'Mon Oct 19 10:00:01 2026'}
                return owner
            def kill(pid, sig):
                self.assertEqual(pid, owner['pid']);signals.append(sig)
                if mode == 'stop': exited[0] = True
            with patch.object(controller, 'backend_identity', return_value=owner):
                controller.record_backend_launch(owner['pid'])
            qa.write_json(state / 'startup.claim.private.json', {**qa.scope(), 'pid': 88888 if mode == 'wrong-claim' else owner['pid']})
            with patch.object(controller, 'backend_identity', side_effect=inspect), patch.object(qa.os, 'kill', side_effect=kill), \
                    patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), \
                    patch.object(qa.time, 'sleep', side_effect=lambda duration: clock.__setitem__(0, clock[0] + duration)):
                if mode in ['stop', 'exited']:
                    result = controller.cleanup_before_ready()
                    self.assertTrue(result['processExitConfirmed'])
                    self.assertFalse(result['drainVerified']);self.assertFalse(result['measurementSucceeded'])
                else:
                    with self.assertRaises(ValueError): controller.cleanup_before_ready()
            self.assertLessEqual(clock[0], 5)
            if mode in ['replacement', 'wrong-claim', 'exited']: self.assertEqual(signals, [])
            if mode == 'stop': self.assertEqual(signals, [signal.SIGTERM])
            if mode == 'timeout': self.assertEqual(signals, [signal.SIGTERM, signal.SIGKILL])
            self.assertFalse((state / 'backend-closed.json').exists())

    def test_before_background_early_home_drain_epoch_slot_and_lifecycle_are_rejected(self):
        for mutate in [lambda s: s.update(status='background', liveRasterAttached=False),
                       lambda s: s.update(currentEpoch=99), lambda s: s.update(lastPaintedEpoch=99),
                       lambda s: s.update(observedLifecycle='paused'), lambda s: s.update(slot={'changed': True})]:
            value = records(); mutate(value[5]['sample'])
            with self.subTest(mutation=mutate), self.assertRaises(ValueError): qa.validate_samples(value, expected())

    def test_background_and_resume_cannot_hide_early_stop_closed_cubit_or_deadline(self):
        for phase in ['background', 'resumed']:
            for key, wrong in [('stopped', True), ('cubitClosed', True), ('credentialActive', False), ('deadlineActive', False), ('observedLifecycle', 'detached')]:
                value = records(); value[qa.PHASES.index(phase)]['sample'][key] = wrong
                with self.subTest(phase=phase, field=key), self.assertRaises(ValueError): qa.validate_samples(value, expected())

    def backend_fixture(self, root):
        state = root / 'backend'; state.mkdir(mode=0o700)
        owner = {'pid': 99999, 'processStartIdentity': 'Mon Oct 19 10:00:00 2026', 'commandSha256': 'a' * 64}
        qa.write_json(state / 'launch.private.json', {**qa.scope(), **owner})
        qa.write_json(state / 'startup.claim.private.json', {**qa.scope(), 'pid': owner['pid'], 'runBindingSha256': 'run-sha'})
        ready = {'pid': owner['pid']}; links = {'runBindingSha256': 'run-sha'}
        ledger = 'OPEN_PENDING synthetic\n' + ''.join(f'DELIVERY_RESERVED {n} {39+n}\n' for n in range(1, 14)) + 'CLOSED 4 13170884 10 1 0 4\n'
        qa.helpers.write_new(state / 'ledger.log', ledger.encode())
        receipt = {**qa.scope(), 'pid': owner['pid'], 'closed': True, 'drainVerified': True, 'outcome': 'SUCCESS',
            'bindings': links, 'terminalLedgerSha256': qa.digest(state / 'ledger.log'), 'terminalLedgerLine': ledger.splitlines()[-1],
            'deliveryOperations': 13, 'reservedBytes': 13170884,
            'finalBudget': {'operations': 52, 'reserved_bytes': 57913854, 'max_operations': 64, 'max_bytes': 67108864}}
        qa.write_json(state / 'backend-closed.json', receipt)
        return qa.Controller(root, root), owner, ready, links, receipt

    def test_successful_backend_requires_healthy_terminal_ledger_and_post_close_db(self):
        for mode in ['pass', 'failure-file', 'failed-outcome', 'db-delta', 'ledger-failed', 'ledger-sha', 'bindings']:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve(); c, owner, ready, links, receipt = self.backend_fixture(root)
                if mode == 'failure-file': qa.write_json(c.state / 'failure.json', {'status': 'FAILED_NO_RETRY'})
                if mode == 'failed-outcome': receipt['outcome'] = 'FAILED_NO_RETRY'
                if mode == 'db-delta': receipt['finalBudget']['operations'] += 1
                if mode == 'ledger-sha': receipt['terminalLedgerSha256'] = 'b' * 64
                if mode == 'ledger-failed':
                    (c.state / 'ledger.log').write_text((c.state / 'ledger.log').read_text().replace('CLOSED 4', 'FAILED_CLOSED 4'))
                    receipt['terminalLedgerSha256'] = qa.digest(c.state / 'ledger.log');receipt['terminalLedgerLine'] = (c.state / 'ledger.log').read_text().splitlines()[-1]
                if mode == 'bindings': receipt['bindings'] = {'runBindingSha256': 'other'}
                (c.state / 'backend-closed.json').write_text(json.dumps(receipt))
                if mode == 'pass': self.assertTrue(qa.validate_backend_result(c.state, ready, links)['measurementSucceeded'])
                else:
                    with self.subTest(mode=mode), self.assertRaises(ValueError): qa.validate_backend_result(c.state, ready, links)

    def test_retry_backend_result_requires_exactly_one_prior_attempt_matching_bound_sha(self):
        for mode in ['valid', 'missing', 'duplicate', 'wrong-sha']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve();controller, owner, ready, links, receipt = self.backend_fixture(root)
                links['priorAttemptProofSha256'] = 'a' * 64
                receipt.update(qa.scope(qa.P4B5_RETRY))
                receipt['bindings'] = links
                prior = 'PRIOR_ATTEMPT ' + links['priorAttemptProofSha256'] + '\n'
                lines = [] if mode == 'missing' else [prior, prior] if mode == 'duplicate' else [
                    'PRIOR_ATTEMPT ' + 'b' * 64 + '\n' if mode == 'wrong-sha' else prior]
                ledger = (controller.state / 'ledger.log').read_text()
                first, rest = ledger.split('\n', 1)
                ledger = first + '\n' + ''.join(lines) + rest
                (controller.state / 'ledger.log').write_text(ledger)
                # Her varyantın dış SHA'sı ve terminal makbuzu tutarlıdır; yalnız PRIOR_ATTEMPT kuralı ayrıştırır.
                receipt['terminalLedgerSha256'] = qa.digest(controller.state / 'ledger.log')
                receipt['terminalLedgerLine'] = ledger.splitlines()[-1]
                (controller.state / 'backend-closed.json').write_text(json.dumps(receipt))
                before = {p.name: p.read_bytes() for p in controller.state.iterdir() if p.is_file()}
                if mode == 'valid':
                    result = qa.validate_backend_result(controller.state, ready, links, qa.P4B5_RETRY)
                    self.assertTrue(result['measurementSucceeded'])
                    self.assertEqual(result['finalBudget']['operations'], 52)
                else:
                    with self.assertRaisesRegex(ValueError, '^TERMINAL_PRIOR_ATTEMPT_BINDING$'):
                        qa.validate_backend_result(controller.state, ready, links, qa.P4B5_RETRY)
                self.assertEqual(before, {p.name: p.read_bytes() for p in controller.state.iterdir() if p.is_file()})

    def test_ready_backend_exited_or_waited_exit_cannot_turn_failure_into_success(self):
        for after_wait in [False, True]:
            for mode in ['pass', 'failure-file', 'failed-outcome', 'db-delta', 'ledger-failed']:
                with tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve(); c, owner, ready, links, receipt = self.backend_fixture(root)
                    if mode == 'failure-file': qa.write_json(c.state / 'failure.json', {'status': 'FAILED_NO_RETRY'})
                    if mode == 'failed-outcome': receipt['outcome'] = 'FAILED_NO_RETRY'
                    if mode == 'db-delta': receipt['finalBudget']['operations'] += 1
                    if mode == 'ledger-failed':
                        (c.state / 'ledger.log').write_text((c.state / 'ledger.log').read_text().replace('CLOSED 4', 'FAILED_CLOSED 4'))
                        receipt['terminalLedgerSha256'] = qa.digest(c.state / 'ledger.log');receipt['terminalLedgerLine'] = (c.state / 'ledger.log').read_text().splitlines()[-1]
                    (c.state / 'backend-closed.json').write_text(json.dumps(receipt))
                    sequence = [owner, None] if after_wait else [None]
                    with patch.object(c, 'ready', return_value=ready), patch.object(c, 'binding_links', return_value=links), patch.object(c, 'backend_identity', side_effect=sequence), patch.object(qa.os, 'kill') as kill:
                        if mode != 'pass':
                            with self.subTest(after_wait=after_wait, mode=mode), self.assertRaises(ValueError): c.close_ready_backend()
                        else: self.assertTrue(c.close_ready_backend()['measurementSucceeded'])
                    kill.assert_not_called()

    def test_ready_backend_same_pid_and_argv_with_new_start_identity_never_signals_or_succeeds(self):
        for later in [False, True, 'zombie']:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve(); c, owner, ready, links, receipt = self.backend_fixture(root)
                replacement = {**owner, 'processStartIdentity': 'Mon Oct 19 10:00:01 2026'}
                sequence = [owner, replacement] if later is True else [({**replacement, 'exited': True} if later == 'zombie' else replacement), None]
                with patch.object(c, 'ready', return_value=ready), patch.object(c, 'binding_links', return_value=links), patch.object(c, 'backend_identity', side_effect=sequence), patch.object(qa.os, 'kill') as kill:
                    with self.assertRaisesRegex(ValueError, 'BACKEND_OWNERSHIP_CHANGED'): c.close_ready_backend()
                kill.assert_not_called()

    def test_ready_binding_drift_is_rejected_before_backend_or_credentials(self):
        for mode in ['pass', 'ready-run', 'ready-classpath', 'claim-run']:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve(); c, owner, _, links, receipt = self.backend_fixture(root)
                c.root = qa.ROOT
                ready = {**expected(), **qa.scope(), **links, 'pid': owner['pid'], 'loopbackPort': 12456}
                if mode == 'ready-run': ready['runBindingSha256'] = 'wrong'
                if mode == 'ready-classpath': ready['classpathBindingSha256'] = 'wrong';links['classpathBindingSha256'] = 'approved'
                if mode == 'claim-run':
                    (c.state / 'startup.claim.private.json').write_text(json.dumps({**qa.scope(), 'pid': owner['pid'], 'runBindingSha256': 'wrong'}))
                qa.write_json(c.state / 'local-ready.json', ready)
                with patch.object(c, 'binding_links', return_value=links), patch.object(c, 'backend_identity', side_effect=AssertionError('No process IO required')):
                    if mode == 'pass': self.assertEqual(c.ready()['pid'], owner['pid'])
                    else:
                        with self.assertRaisesRegex(ValueError, 'BACKEND_READY_BINDING_DRIFT'): c.ready()

    def test_cleanup_zero_or_partial_consumption_closes_all_real_stages_before_io(self):
        for partial in [False, True]:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory).resolve(); (root / 'receipts').mkdir(mode=0o700)
                c = qa.Controller(root, root)
                if partial: qa.write_json(root / 'install.claim.private.json', {**qa.scope(), 'state': 'RESERVED_NO_AUTOMATIC_RETRY'})
                with patch.object(qa.helpers, 'Android', side_effect=AssertionError('ADB forbidden')):
                    c.cleanup(None)
                self.assertEqual(qa.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')
                for call in [lambda: c.arm(None), lambda: c.reserve_backend(None), lambda: c.login(None, None), lambda: c.open_window(None), lambda: c.run(None), lambda: c.record_backend_launch(99999)]:
                    with self.assertRaisesRegex(ValueError, 'ATTEMPT_CLOSED_NO_RETRY'): call()
                # Terminal dosyası kaybolsa da cleanup claim'i yeniden açamaz.
                (root / 'terminal.private.json').unlink()
                with self.assertRaisesRegex(ValueError, 'ATTEMPT_CLOSED_NO_RETRY'): c.ensure_open()

    @unittest.skipUnless(os.environ.get('P4B5_DART_JSON_LOG'), 'Özel gerçek Dart JSON logu seçilmedi')
    def test_real_dart_native_loopback_raw_json_is_accepted_by_phase_resources(self):
        chains = []
        for line in Path(os.environ['P4B5_DART_JSON_LOG']).read_text().splitlines():
            if 'P4B5_RESOURCE_JSON:' in line:
                item = json.loads(line.split('P4B5_RESOURCE_JSON:', 1)[1])
                report = qa.validate_phase_resources(item['records'])
                raw = json.dumps(item['records'], sort_keys=True)
                with self.assertRaises(ValueError):
                    qa.validate_before_background(item['records'][4]['sample'], item['records'][6]['sample'])
                early_stop = list(item['records'])
                early_stop[6] = {'phase': 'background', 'sample': item['records'][-1]['sample']}
                with self.assertRaises(ValueError): qa.validate_phase_resources(early_stop)
                self.assertEqual(json.dumps(item['records'], sort_keys=True), raw)
                self.assertTrue(report['measured'])
                self.assertEqual(report['bufferCreationAttemptsDelta'], '4')
                chains.append(item['chain'])
        self.assertEqual(chains, ['loopback-native'])

    @unittest.skipUnless(os.environ.get('P4B5_RETRY_DART_JSON_LOG'), 'Yeni scope gerçek Dart JSON logu seçilmedi')
    def test_retry_native_loopback_json_preserves_four_paints_and_phase_drain(self):
        chains = []
        for line in Path(os.environ['P4B5_RETRY_DART_JSON_LOG']).read_text().splitlines():
            if 'P4B5_RETRY_RESOURCE_JSON:' not in line: continue
            item = json.loads(line.split('P4B5_RETRY_RESOURCE_JSON:', 1)[1])
            before = json.dumps(item, sort_keys=True)
            report = qa.validate_phase_resources(item['records'])
            self.assertTrue(report['measured'])
            self.assertEqual(report['bufferCreationAttemptsDelta'], '4')
            for phase in ['background', 'resumed', 'closedUi', 'stopped']:
                invalid = copy.deepcopy(item['records'])
                invalid[qa.PHASES.index(phase)]['sample']['resourceObservation']['reservedSlots'] = 1
                with self.assertRaises(ValueError): qa.validate_phase_resources(invalid)
            self.assertEqual(json.dumps(item, sort_keys=True), before)
            chains.append(item['chain'])
        self.assertEqual(chains, ['loopback-native'])



class P4b5VmPatchTest(unittest.TestCase):
    class Socket:
        def __init__(self, clock, mode='late', code=-32601):
            self.clock, self.mode, self.code = clock, mode, code
            self.sent, self.queue, self.timeouts = [], [], []
        def settimeout(self, value): self.timeouts.append(value)
        def send(self, raw):
            request = json.loads(raw);self.sent.append(request)
            method, ident = request['method'], request['id']
            result = {}
            if method == 'getVM': result = {'isolates': [{'id': 'isolate/1' if self.mode != 'main-drift' or len(self.sent) == 1 else 'isolate/2', 'name': 'main'}]}
            if method == 'getIsolate':
                result = {'id': 'isolate/2' if self.mode == 'isolate-drift' else 'isolate/1', 'extensionRPCs': ['ext.flutter.driver'] if self.mode in ['already', 'rpc-error', 'main-drift', 'late-exit', 'late-start'] else []}
            reply = {'id': ident, 'result': result}
            if method == 'ext.flutter.driver' and self.mode == 'rpc-error':
                reply = {'id': ident, 'error': {'code': self.code, 'message': 'SECRET_TOKEN ws://127.0.0.1:1234/SECRET_KEY/ws', 'data': {'token': 'SECRET_TOKEN'}}}
            if method == 'getIsolate' and self.mode == 'registration-race':
                self.queue.append(self.event())
            if method == 'streamCancel' and self.mode in ['late-exit', 'late-start']:
                self.queue.append(self.event(kind='IsolateExit' if self.mode == 'late-exit' else 'IsolateStart', isolate='isolate/1' if self.mode == 'late-exit' else 'isolate/2'))
            self.queue.append(reply)
        def event(self, kind='ServiceExtensionAdded', isolate='isolate/1'):
            return {'method': 'streamNotify', 'params': {'streamId': 'Isolate', 'event': {'kind': kind, 'extensionRPC': 'ext.flutter.driver', 'isolate': {'id': isolate, 'name': 'main'}}}}
        def recv(self):
            if self.queue: return json.dumps(self.queue.pop(0))
            if self.mode == 'late': self.clock[0] = 25;return json.dumps(self.event())
            if self.mode == 'foreign-then-late':
                self.mode = 'late';return json.dumps(self.event(isolate='isolate/other'))
            if self.mode == 'exit': return json.dumps(self.event(kind='IsolateExit'))
            if self.mode == 'start': return json.dumps(self.event(kind='IsolateStart', isolate='isolate/2'))
            self.clock[0] = 30
            raise TimeoutError('SECRET_TOKEN')

    def vm(self, root, clock, mode='late', code=-32601, owner=None):
        value = object.__new__(qa.Vm)
        value._protocol(root, 'arm', 30, owner)
        value.socket = self.Socket(clock, mode, code)
        return value

    def test_extension_registration_list_late_event_race_and_foreign_event_obey_original_deadline(self):
        for mode in ['already', 'late', 'registration-race', 'foreign-then-late']:
            with tempfile.TemporaryDirectory() as directory:
                clock = [0.0];checks = []
                vm = self.vm(Path(directory), clock, mode, owner=lambda: checks.append(clock[0]))
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]): vm.ready()
                methods = [request['method'] for request in vm.socket.sent]
                self.assertEqual(methods, ['getVM', 'streamListen', 'getIsolate', 'getVM', 'streamCancel', 'ext.flutter.driver'])
                self.assertEqual(vm.isolate, 'isolate/1')
                self.assertGreater(len(checks), 6)
                self.assertLess(clock[0], 30)
                self.assertEqual(len(list(Path(directory).iterdir())), 0)

    def test_arm_uses_p4b5_protocol_and_same_owned_isolate_before_any_baseline(self):
        clock = [0.0];case = self
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve();(root / 'receipts').mkdir();(root / 'android').mkdir()
            owner = {'pid': 100, 'processStartTicks': '200'};baseline = records()[0]['sample']
            class Socket(self.Socket):
                def send(self, raw):
                    super().send(raw)
                    request = json.loads(raw)
                    if request['method'] == 'ext.flutter.driver' and request['params'].get('command') == 'request_data':
                        self.queue[-1]['result'] = {'response': {'message': json.dumps(baseline)}}
            class Vm(qa.Vm):
                def __init__(self, url, evidence, stage, deadline, owner_check):
                    case.assertEqual(deadline, 30);case.assertEqual(stage, 'arm')
                    self._protocol(evidence, stage, deadline, owner_check);self.socket = Socket(clock)
                def close(self): pass
            class Android:
                started = False
                def preflight(self): pass
                def pid(self, timeout=15): return '100' if self.started else ''
                def process_identity(self, pid, timeout=15): return owner
                def run(self, *args, **kwargs):
                    if args[:3] == ('shell', 'am', 'start'): self.started = True
                    if args[:3] == ('shell', 'pm', 'path'): return 'package:/data/app/synthetic/base.apk'
                    if args[:2] == ('shell', 'sha256sum'): return 'a' * 64 + ' /data/app/synthetic/base.apk'
                    if args[0] == 'logcat': return 'Dart VM service is listening on http://127.0.0.1:23456/synthetic/'
                    return ''
            controller = qa.Controller(root, root)
            with patch.object(controller, 'gate'), patch.object(controller, 'selected_build', return_value=({'apkSha256': 'a' * 64}, root)), patch.object(qa.helpers, 'observe_launch_pid', return_value=('100', owner)), patch.object(qa.helpers, 'available_host_port', return_value=12345), patch.object(qa.helpers, 'Vm', side_effect=AssertionError('Legacy VM forbidden')), patch.object(qa, 'Vm', Vm), patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]):
                controller.arm(Android())
            saved = qa.private_json(root / 'android/baseline.private.json')
            self.assertEqual(saved['isolateId'], 'isolate/1');self.assertEqual(saved['sample'], baseline)
            self.assertEqual(clock[0], 25)

    def test_retry_scope_late_driver_and_rpc_failure_keep_one_deadline_and_safe_identity(self):
        for mode in ['late', 'rpc-error']:
            with tempfile.TemporaryDirectory() as directory:
                clock = [0.0];root = Path(directory).resolve();vm = self.vm(root, clock, mode)
                vm.qa_scope = qa.P4B5_RETRY
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]):
                    if mode == 'rpc-error':
                        with self.assertRaisesRegex(ValueError, 'VM_REFUSED'): vm.ready()
                        receipt = qa.private_json(next(root.glob('vm-rpc-failure-*.private.json')))
                        self.assertEqual({key: receipt[key] for key in qa.scope(qa.P4B5_RETRY)}, qa.scope(qa.P4B5_RETRY))
                        self.assertEqual(receipt['rpcErrorCode'], -32601)
                    else:
                        vm.ready();self.assertLess(clock[0], 30)
                self.assertEqual(sum(request['method'] == 'ext.flutter.driver' for request in vm.socket.sent), 1)

    def test_never_registered_isolate_exit_replacement_and_deadline_never_call_driver(self):
        for mode in ['never', 'isolate-drift', 'main-drift', 'exit', 'start', 'late-exit', 'late-start']:
            with tempfile.TemporaryDirectory() as directory:
                clock = [0.0];vm = self.vm(Path(directory), clock, mode)
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), self.assertRaises((ValueError, TimeoutError)):
                    vm.ready()
                self.assertNotIn('ext.flutter.driver', [request['method'] for request in vm.socket.sent])
                self.assertLessEqual(clock[0], 30)
        with tempfile.TemporaryDirectory() as directory:
            clock = [30.0];vm = self.vm(Path(directory), clock, 'already')
            with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), self.assertRaisesRegex(ValueError, 'VM_DEADLINE'): vm.ready()
            self.assertEqual(vm.socket.sent, [])

    def test_owned_pid_start_and_deadline_guard_prevents_driver_before_and_during_wait(self):
        owner = {'pid': 100, 'processStartTicks': '200'}
        for mode in ['pid', 'start', 'late-pid', 'deadline']:
            clock = [0.0];calls = [0]
            class Android:
                def pid(self, timeout):
                    calls[0] += 1
                    return '101' if mode == 'pid' or (mode == 'late-pid' and calls[0] > 8) else '100'
                def process_identity(self, pid, timeout):
                    if mode == 'deadline': clock[0] = 30
                    return {**owner, 'processStartTicks': '201'} if mode == 'start' else owner
            with tempfile.TemporaryDirectory() as directory:
                vm = self.vm(Path(directory), clock, 'late', owner=lambda: qa.require_arm_owner(Android(), owner, 30))
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), self.assertRaises(ValueError): vm.ready()
                self.assertNotIn('ext.flutter.driver', [request['method'] for request in vm.socket.sent])

    def test_rpc_failure_records_only_allowlisted_stage_method_numeric_code_once_without_secrets(self):
        for code in [-32601, True, 'SECRET_TOKEN', {'token': 'SECRET_TOKEN'}, 2**80]:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory);clock = [0.0];vm = self.vm(root, clock, 'rpc-error', code)
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), self.assertRaisesRegex(ValueError, 'VM_REFUSED'): vm.ready()
                self.assertEqual(sum(request['method'] == 'ext.flutter.driver' for request in vm.socket.sent), 1)
                paths = list(root.glob('vm-rpc-failure-*.private.json'));self.assertEqual(len(paths), 1)
                raw = paths[0].read_text();record = json.loads(raw)
                self.assertEqual(set(record), set(qa.scope()) | {'stage', 'rpcMethod'} | ({'rpcErrorCode'} if code == -32601 else set()))
                self.assertEqual(record['stage'], 'arm');self.assertEqual(record['rpcMethod'], 'ext.flutter.driver')
                for secret in ['SECRET_TOKEN', 'SECRET_KEY', 'ws://', '127.0.0.1', 'params', 'message', 'data', 'error', 'body', 'token']:
                    self.assertNotIn(secret, raw)
                self.assertEqual(paths[0].stat().st_mode & 0o777, 0o600)
                saved = paths[0].read_bytes()
                with patch.object(qa.time, 'monotonic', side_effect=lambda: clock[0]), self.assertRaises(ValueError):
                    vm.rpc('SECRET_TOKEN', {'token': 'SECRET_TOKEN'})
                self.assertEqual(paths[0].read_bytes(), saved);self.assertEqual(len(list(root.iterdir())), 1)
                with self.assertRaises(ValueError): vm._protocol(root, 'SECRET_TOKEN')

if __name__ == '__main__':
    unittest.main()
