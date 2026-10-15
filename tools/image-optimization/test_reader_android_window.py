"""Absolute, identity-bound sequential entry without cloud/provider operations."""
from dataclasses import FrozenInstanceError
from hashlib import sha256
import math
from pathlib import Path
import tempfile
import threading
from concurrent.futures import ThreadPoolExecutor
import unittest
from unittest.mock import patch

from native_entry_config import EntryError, frozen_json
import reader_android_window as window
import reader_resolution_experiment as reader


class WindowInputs(unittest.TestCase):
    def setUp(self):
        self.elapsed = 60
        self.root_epoch = 1000000
        self.root_monotonic = 100.0
        patcher = patch.object(window.time, 'monotonic', side_effect=lambda: self.root_monotonic + self.elapsed)
        patcher.start(); self.addCleanup(patcher.stop)
        patcher = patch.object(window.time, 'time', side_effect=lambda: self.root_epoch / 1000 + self.elapsed)
        patcher.start(); self.addCleanup(patcher.stop)
        self.arguments = dict(root_epoch_milliseconds=self.root_epoch, root_monotonic=self.root_monotonic,
            reservation_expires_at_milliseconds=self.root_epoch + 470000,
            reservation_receipt_sha256='1' * 64, allocation_sha256='2' * 64,
            device_selector_sha256=sha256(b'synthetic-device').hexdigest(), device_unit_sha256='4' * 64,
            source_sha256='5' * 64, artifact_sha256='6' * 64, test_artifact_sha256='7' * 64,
            certificate_sha256='8' * 64, watchdog_receipt_sha256='9' * 64)
        self.build = {'sourceSha256': '5' * 64, 'artifactSha256': '6' * 64,
            'testArtifactSha256': '7' * 64, 'certificateSha256': '8' * 64}
        self.proof = {**self.build, 'deviceSelectorSha256': self.arguments['device_selector_sha256'],
            'reservationReceiptSha256': '1' * 64, 'reservationExpiresAt': self.root_epoch + 470000,
            'provider': 'android_device_streaming', 'quotaVerified': True, 'deviceModel': 'Moto E20'}

    def values(self, batch):
        return {'batchId': batch, 'platform': 'android', 'mode': 'release'}


class SequentialWindowTests(WindowInputs):
    def test_concurrent_entries_issue_only_one_A_and_preserve_075_and_cleanup_order(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        start = threading.Barrier(2)
        original = window.SequentialAndroidLease
        def delayed_lease(*args):
            threading.Event().wait(.03)
            return original(*args)
        def enter():
            start.wait(timeout=2)
            try: return owned.enter(window.ORDER[0])
            except EntryError: return None
        with patch.object(window, 'SequentialAndroidLease', side_effect=delayed_lease), ThreadPoolExecutor(2) as pool:
            futures = [pool.submit(enter) for _ in range(2)]
            results = [future.result(timeout=3) for future in futures]
        accepted = [result for result in results if result is not None]
        self.assertEqual(len(accepted), 1)
        owned.finish(accepted[0], succeeded=True, geometry_sha256='a' * 64)
        lease = owned.enter(window.ORDER[1], geometry_sha256='a' * 64)
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        owned.finish(lease, succeeded=True)
        owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        with self.assertRaisesRegex(EntryError, 'order'):
            owned.enter(window.ORDER[2], geometry_sha256='a' * 64)

    def test_malformed_lease_window_has_finite_refusal_before_attribute_access(self):
        for malformed in (None, object(), 'raw-window', {}):
            lease = window.SequentialAndroidLease(malformed, window.ORDER[0], 100, 280)
            with self.subTest(value=type(malformed)), self.assertRaisesRegex(EntryError, '^reader_sequential_lease_rejected$'):
                lease.validate(self.values(window.ORDER[0]), self.build, self.proof)

    def test_real_run_preserves_native_exception_after_deadline_or_clock_failure_and_blocks_next_entry(self):
        import native_entry_driver as host
        import native_artwork_comparison as comparison
        from native_entry_config import Manifest
        from types import SimpleNamespace
        for clock_failure in (False, True):
            self.elapsed = 60
            owned = window.SequentialAndroidWindow(**self.arguments)
            selected = SimpleNamespace(values=self.values(window.ORDER[0]))
            entries = tuple({'caseId': case, 'mime': 'image/png', 'fit': 'contain', 'alignmentX': 0,
                'alignmentY': 0, 'purposeAcceptanceReceipt': window.ORDER[0], 'profileRevision': 'reader_discover_r1',
                'logicalSlotWidth': 1, 'logicalSlotHeight': 1, 'dpr': 1} for case in reader.READER_CASES)
            manifest = Manifest(entries, {}, 'a' * 64, b'{}')
            original_error = EntryError('native_timeout_original')
            def failed_native(*args, **kwargs):
                self.elapsed = 241
                if clock_failure: self.root_epoch += 10000
                raise original_error
            with tempfile.TemporaryDirectory() as folder, \
                    patch.object(host, 'validate_physical_prerequisites', return_value=(self.build, {'deviceModel': 'Moto E20'})), \
                    patch.object(host, 'source_snapshot', return_value=({}, '5' * 64)), \
                    patch.object(comparison, 'validate_comparison_audit'), \
                    patch.object(comparison, 'require_fresh_android_process'), \
                    patch.object(host, 'verify_entry', side_effect=failed_native):
                with self.assertRaisesRegex(EntryError, '^native_timeout_original$') as caught:
                    reader._run(selected, manifest, Path(folder).resolve(), sequential_window=owned,
                        build_path='build', prerequisite_path='proof', audit_path='audit')
                self.assertIs(caught.exception, original_error)
            if clock_failure: self.root_epoch -= 10000
            self.elapsed = 60
            with self.assertRaisesRegex(EntryError, 'order'):
                owned.enter(window.ORDER[1], geometry_sha256='a' * 64)
            self.assertIsNone(owned._active)

    def test_successful_late_run_still_refuses_and_consumes_lease(self):
        from types import SimpleNamespace
        observed = dict(zip(reader.GEOMETRY_FIELDS, (2, 3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'portrait')))
        owned = window.SequentialAndroidWindow(**self.arguments)
        def late_success(*args, **kwargs):
            self.elapsed = 241
            return {'geometry': observed}
        with patch.object(reader, '_run_observed', side_effect=late_success):
            with self.assertRaisesRegex(EntryError, 'completion_late'):
                reader._run(SimpleNamespace(values=self.values(window.ORDER[0])), None, None, sequential_window=owned)
        self.assertIsNone(owned._active)
        self.assertTrue(owned._failed)

    def test_absolute_470_expiry_450_cancel_478_cleanup_and_no_extension(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        self.assertEqual(owned.cancel_monotonic, self.root_monotonic + 450)
        self.assertEqual(owned.cleanup_monotonic, self.root_monotonic + 478)
        for value in (self.root_epoch + 470001, self.root_epoch, True, None):
            with self.subTest(expiry=value), self.assertRaises(EntryError):
                window.SequentialAndroidWindow(**{**self.arguments, 'reservation_expires_at_milliseconds': value})
        with self.assertRaises(FrozenInstanceError): owned.reservation_expires_at_milliseconds += 1
        for key in ('allocation_sha256', 'device_unit_sha256', 'watchdog_receipt_sha256'):
            with self.subTest(key=key), self.assertRaises(EntryError):
                window.SequentialAndroidWindow(**{**self.arguments, key: None})

    def test_A075C_order_no_replay_overlap_or_partial_phase_and_native_max180(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        with self.assertRaisesRegex(EntryError, 'order'): owned.enter(window.ORDER[1])
        for index, batch in enumerate(window.ORDER):
            self.elapsed = 60 + index * 120
            lease = owned.enter(batch, geometry_sha256=None if index == 0 else 'a' * 64)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            self.assertGreaterEqual(lease.deadline - lease.entered_monotonic, 150)
            window.validate_lease(lease, self.values(batch), self.build, self.proof)
            with self.assertRaisesRegex(EntryError, 'order'): owned.enter(batch)
            self.elapsed += 119
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
            with self.assertRaisesRegex(EntryError, 'lease'):
                window.validate_lease(lease, self.values(batch), self.build, self.proof)
        with self.assertRaisesRegex(EntryError, 'order'): owned.enter(window.ORDER[0])

    def test_entry150_before_cancel_is_inclusive_and_shorter_or_expiring_runs_refuse(self):
        self.elapsed = 300
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0])
        self.assertEqual(lease.deadline - lease.entered_monotonic, 150)
        self.elapsed = 300.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            window.SequentialAndroidWindow(**self.arguments).enter(window.ORDER[0])
        self.elapsed = 60
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            window.SequentialAndroidWindow(**{**self.arguments,
                'reservation_expires_at_milliseconds': self.root_epoch + 224999}).enter(window.ORDER[0])

    def test_dispatch_rechecks_full_phase_budget_after_preparation(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0])
        self.elapsed = 90
        lease.require_full_entry_budget()
        self.elapsed = 90.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'): lease.require_full_entry_budget()

    def test_reader_run_passes_one_lease_and_failed_invocation_blocks_replay(self):
        from types import SimpleNamespace
        observed = dict(zip(reader.GEOMETRY_FIELDS, (2, 3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'portrait')))
        owned = window.SequentialAndroidWindow(**self.arguments)
        selected = SimpleNamespace(values=self.values(window.ORDER[0]))
        with tempfile.TemporaryDirectory() as folder, \
                patch.object(reader, '_run_observed', return_value={'geometry': observed}) as run:
            result = reader._run(selected, None, Path(folder).resolve(), sequential_window=owned)
            self.assertEqual(result['geometry'], observed)
            lease = run.call_args.kwargs['sequential_lease']
            self.assertIs(lease.window, owned)
        selected.values['batchId'] = window.ORDER[1]
        with patch.object(reader, '_run_observed', side_effect=EntryError('native_refused')):
            with self.assertRaisesRegex(EntryError, 'native_refused'):
                reader._run(selected, None, None, sequential_window=owned, frozen_geometry=observed)
        selected.values['batchId'] = window.ORDER[2]
        with self.assertRaisesRegex(EntryError, 'order'):
            reader._run(selected, None, None, sequential_window=owned, frozen_geometry=observed)
        with patch.object(reader, '_run_observed', return_value='legacy') as legacy:
            self.assertEqual(reader._run(selected, None, None, root_elapsed_seconds=104,
                reservation_remaining_seconds=196), 'legacy')
            self.assertNotIn('sequential_lease', legacy.call_args.kwargs)

    def test_wrong_batch_platform_build_allocation_expiry_and_device_refuse(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0])
        for key, value in (('batchId', window.ORDER[1]), ('platform', 'ios'), ('mode', 'profile')):
            with self.subTest(key=key), self.assertRaises(EntryError):
                lease.validate({**self.values(window.ORDER[0]), key: value}, self.build, self.proof)
        for key, value in (('reservationReceiptSha256', 'a' * 64), ('reservationExpiresAt', self.root_epoch + 471000),
                ('deviceSelectorSha256', 'a' * 64), ('deviceModel', 'Pixel 7'), ('deviceModel', None),
                ('deviceModel', 123), ('deviceModel', {}), ('deviceModel', []), ('quotaVerified', False)):
            with self.subTest(key=key), self.assertRaises(EntryError):
                lease.validate(self.values(window.ORDER[0]), self.build, {**self.proof, key: value})
        for key in self.build:
            with self.subTest(key=key), self.assertRaises(EntryError):
                lease.validate(self.values(window.ORDER[0]), {**self.build, key: 'a' * 64}, self.proof)
        forged = window.SequentialAndroidLease(owned, window.ORDER[0], lease.entered_monotonic, lease.deadline)
        with self.assertRaisesRegex(EntryError, 'lease'):
            window.validate_lease(forged, self.values(window.ORDER[0]), self.build, self.proof)
        with self.assertRaisesRegex(EntryError, 'lease'):
            window.validate_lease(object(), self.values(window.ORDER[0]), self.build, self.proof)

    def test_failure_geometry_change_late_completion_and_clock_jump_refuse_further_entry(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0]); owned.finish(lease, succeeded=False)
        with self.assertRaisesRegex(EntryError, 'order'): owned.enter(window.ORDER[1], geometry_sha256='a' * 64)
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0]); owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        with self.assertRaisesRegex(EntryError, 'geometry'): owned.enter(window.ORDER[1], geometry_sha256='b' * 64)
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0]); self.elapsed = 241
        with self.assertRaisesRegex(EntryError, 'completion_late'): owned.finish(lease, succeeded=False)
        self.elapsed = 60
        with patch.object(window.time, 'time', return_value=9000), self.assertRaisesRegex(EntryError, 'clock'):
            window.SequentialAndroidWindow(**self.arguments).enter(window.ORDER[0])

    def test_only_owned_timely_terminal_cleanup_qualifies_even_after_failed_entry(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0]); owned.finish(lease, succeeded=False)
        for allocation, terminal in (('f' * 64, True), ('2' * 64, False)):
            with self.assertRaisesRegex(EntryError, 'cleanup'):
                owned.validate_cleanup(allocation_sha256=allocation, terminal_observed=terminal)
        self.elapsed = 478
        receipt = owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.assertFalse(receipt['automaticRetry'])
        self.elapsed = 478.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)


class SevenMinuteProfileTests(WindowInputs):
    def setUp(self):
        super().setUp()
        patcher = patch.object(window, '_CONSUMED_TERMINAL_LEDGERS', set())
        patcher.start(); self.addCleanup(patcher.stop)
        temporary = tempfile.TemporaryDirectory(prefix='reader-prior-terminal-')
        self.addCleanup(temporary.cleanup)
        self.base = Path(temporary.name).resolve(); self.base.chmod(0o700)
        self.prior_path = self.base / 'cleanup-receipt.json'
        self.prior = {'scope': 'selected075-authoritative-create-owned-watchdog',
            'state': 'FINISHED', 'terminalObserved': True, 'elapsedMillis': 38985,
            'onlySuccessfulPostResponseNameCancelled': True,
            'absoluteCleanupActionProcessDeadline': True, 'providerTimesNotCleanupOwnership': True,
            'cancelAtRootSeconds': 450, 'cleanupBudgetMaximumSeconds': 28, 'nameSha256': 'a' * 64}
        self.arguments.update(profile=window.SEVEN_MINUTES_PROFILE,
            reservation_expires_at_milliseconds=self.root_epoch + 418000,
            prior_terminal_ledger_path=str(self.prior_path),
            prior_whole_root_milliseconds=39013, current_allocation_name_sha256='b' * 64)
        self.write_prior()
        self.proof['reservationExpiresAt'] = self.root_epoch + 418000

    def write_prior(self):
        content = frozen_json(self.prior)
        self.prior_path.write_bytes(content); self.prior_path.chmod(0o600)
        self.arguments['prior_terminal_ledger_sha256'] = sha256(content).hexdigest()

    def test_finite_profile418_390_418_and_one_plus_seven_preserve_eight_total_minutes(self):
        default = window.SequentialAndroidWindow(**{key: value for key, value in self.arguments.items()
            if key not in ('profile', 'prior_terminal_ledger_path', 'prior_terminal_ledger_sha256',
                'prior_whole_root_milliseconds', 'current_allocation_name_sha256')})
        owned = window.SequentialAndroidWindow(**self.arguments)
        self.assertEqual(owned.cancel_monotonic, self.root_monotonic + 390)
        self.assertEqual(owned.cleanup_monotonic, self.root_monotonic + 418)
        receipt = owned.limits_receipt
        self.assertEqual(receipt['maximumProviderExpiryRootMilliseconds'], 418000)
        self.assertEqual(receipt['priorWholeRootMilliseconds'], 39013)
        self.assertEqual(receipt['priorNewAllocationBillableMinutesRetained'], 1)
        self.assertEqual(receipt['currentAllocationMaximumBillableMinutes'], math.ceil((418 + 1) / 60))
        self.assertEqual(receipt['totalNewAllocationBillableMinutesUpper'], 8)
        self.assertEqual(receipt['totalNewAllocationBillableMinutesUpper'],
            default.limits_receipt['totalNewAllocationBillableMinutesUpper'])
        self.assertEqual(receipt['retainedModeledUpperUsd'], default.limits_receipt['retainedModeledUpperUsd'])
        self.assertEqual(receipt['retainedModeledUpperUsd'], '9.95523914272981')
        self.assertFalse(receipt['historicalDebitsAndReserveChanged'])
        self.assertFalse(receipt['currentInvoice'])
        for profile in ('arbitrary', None, True):
            with self.subTest(profile=profile), self.assertRaisesRegex(EntryError, 'profile_rejected'):
                window.SequentialAndroidWindow(**{**self.arguments, 'profile': profile})
        with self.assertRaisesRegex(EntryError, 'expiry_rejected'):
            window.SequentialAndroidWindow(**{**self.arguments,
                'reservation_expires_at_milliseconds': self.root_epoch + 418001})
        with self.assertRaisesRegex(EntryError, 'prior_rejected'):
            window.SequentialAndroidWindow(**{**self.arguments, 'profile': window.DEFAULT_PROFILE})

    def test_concurrent_constructors_cannot_both_consume_the_same_prior_terminal(self):
        class SlowContains(set):
            def __contains__(self, key):
                present = super().__contains__(key)
                threading.Event().wait(.03)
                return present
        start = threading.Barrier(2)
        def construct():
            start.wait(timeout=2)
            try: return window.SequentialAndroidWindow(**self.arguments)
            except EntryError as error:
                self.assertEqual(str(error), 'reader_sequential_prior_replayed')
                return None
        with patch.object(window, '_CONSUMED_TERMINAL_LEDGERS', SlowContains()), ThreadPoolExecutor(2) as pool:
            futures = [pool.submit(construct) for _ in range(2)]
            results = [future.result(timeout=3) for future in futures]
        self.assertEqual(sum(value is not None for value in results), 1)

    def test_prior_original_hash_terminal_ownership_duration_and_same_name_refuse(self):
        for key, value in (('prior_terminal_ledger_path', None), ('prior_terminal_ledger_sha256', None),
                ('prior_terminal_ledger_sha256', 'f' * 64), ('prior_whole_root_milliseconds', None),
                ('prior_whole_root_milliseconds', True), ('prior_whole_root_milliseconds', 38984),
                ('prior_whole_root_milliseconds', 59001), ('current_allocation_name_sha256', None),
                ('current_allocation_name_sha256', self.prior['nameSha256'])):
            with self.subTest(key=key, value=value), self.assertRaises(EntryError):
                window.SequentialAndroidWindow(**{**self.arguments, key: value})
        for key, value in (('scope', 'image-optimization-authoritative-create-owned-watchdog'),
                ('state', 'ACTIVE'), ('terminalObserved', False), ('elapsedMillis', 39014),
                ('elapsedMillis', True), ('onlySuccessfulPostResponseNameCancelled', False),
                ('absoluteCleanupActionProcessDeadline', False), ('providerTimesNotCleanupOwnership', False),
                ('cancelAtRootSeconds', 390), ('cleanupBudgetMaximumSeconds', 29), ('nameSha256', None)):
            previous = self.prior[key]; self.prior[key] = value; self.write_prior()
            with self.subTest(prior_field=key), self.assertRaisesRegex(EntryError, 'prior_rejected'):
                window.SequentialAndroidWindow(**self.arguments)
            self.prior[key] = previous; self.write_prior()
        self.prior_path.chmod(0o644)
        with self.assertRaisesRegex(EntryError, 'file_not_private'):
            window.SequentialAndroidWindow(**self.arguments)

    def test_same_domain_name_comparison_and_prior_receipt_cannot_authorize_another_window(self):
        # POST bytes and provider names intentionally use different hash domains.
        owned = window.SequentialAndroidWindow(**{**self.arguments,
            'allocation_sha256': self.prior['nameSha256']})
        with self.assertRaisesRegex(EntryError, 'prior_replayed'):
            window.SequentialAndroidWindow(**{**self.arguments,
                'current_allocation_name_sha256': 'c' * 64, 'allocation_sha256': 'd' * 64})
        self.assertEqual(owned.limits_receipt['totalNewAllocationBillableMinutesUpper'], 8)

    def test_prior59000_millisecond_boundary_retains_exactly_one_billable_minute(self):
        owned = window.SequentialAndroidWindow(**{**self.arguments, 'prior_whole_root_milliseconds': 59000})
        self.assertEqual(math.ceil((59000 + 1000) / 60000), 1)
        self.assertEqual(owned.limits_receipt['priorNewAllocationBillableMinutesRetained'], 1)
        self.assertEqual(owned.limits_receipt['totalNewAllocationBillableMinutesUpper'], 8)

    def test_new_profile150_native180_order_identity_and_C_unrun_when_time_is_short(self):
        owned = window.SequentialAndroidWindow(**self.arguments)
        for index, batch in enumerate(window.ORDER[:2]):
            self.elapsed = 60 + index * 120
            lease = owned.enter(batch, geometry_sha256=None if index == 0 else 'a' * 64)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            lease.validate(self.values(batch), self.build, self.proof)
            with self.assertRaisesRegex(EntryError, 'identity'):
                lease.validate(self.values(batch), self.build, {**self.proof,
                    'reservationReceiptSha256': 'f' * 64})
            with self.assertRaisesRegex(EntryError, 'order'): owned.enter(batch)
            self.elapsed += 119
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        self.elapsed = 300
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            owned.enter(window.ORDER[2], geometry_sha256='a' * 64)
        self.elapsed = 418
        cleanup = owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.assertEqual(cleanup['cancelAtRootSeconds'], 390)
        self.assertEqual(cleanup['hardCleanupRootSeconds'], 418)
        self.elapsed = 418.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)

    def test_new_profile_entry_boundary240_and_short_provider_expiry_refuse_partial_runs(self):
        self.elapsed = 240
        owned = window.SequentialAndroidWindow(**self.arguments)
        lease = owned.enter(window.ORDER[0])
        self.assertEqual(lease.deadline - lease.entered_monotonic, 150)
        self.elapsed = 240.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'): lease.require_full_entry_budget()
        owned.finish(lease, succeeded=False)
        # A separate synthetic receipt exercises an independently shorter expiry.
        window._CONSUMED_TERMINAL_LEDGERS.clear()
        self.elapsed = 60
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            window.SequentialAndroidWindow(**{**self.arguments,
                'reservation_expires_at_milliseconds': self.root_epoch + 224999}).enter(window.ORDER[0])


class NineMinuteProfileTests(WindowInputs):
    def test_r3_is_finite538_510_538_nine_minutes_and_has_no_old_ten_dollar_claim(self):
        args = {**self.arguments, 'profile': window.NINE_MINUTES_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 538000}
        owned = window.SequentialAndroidWindow(**args)
        self.assertEqual(owned.cancel_monotonic, self.root_monotonic + 510)
        self.assertEqual(owned.cleanup_monotonic, self.root_monotonic + 538)
        receipt = owned.limits_receipt
        self.assertEqual(receipt['maximumProviderExpiryRootMilliseconds'], 538000)
        self.assertEqual(receipt['currentAllocationMaximumBillableMinutes'], 9)
        self.assertIsNone(receipt['priorNewAllocationBillableMinutesRetained'])
        self.assertIsNone(receipt['totalNewAllocationBillableMinutesUpper'])
        self.assertEqual(receipt['limitScope'], 'current_allocation_only')
        self.assertEqual(math.ceil((538 + 1) / 60), 9)
        self.assertIsNone(receipt['retainedModeledUpperUsd'])
        self.assertFalse(receipt['historicalTenUsdScopeApplies'])
        self.assertFalse(receipt['historicalDebitsAndReserveChanged'])
        for key, value in (('prior_terminal_ledger_path', '/private/tmp/old'),
                ('prior_terminal_ledger_sha256', 'a' * 64), ('prior_whole_root_milliseconds', 1),
                ('current_allocation_name_sha256', 'b' * 64)):
            with self.subTest(key=key), self.assertRaisesRegex(EntryError, 'prior_rejected'):
                window.SequentialAndroidWindow(**{**args, key: value})
        for value in (self.root_epoch + 538001, True, None):
            with self.assertRaisesRegex(EntryError, 'expiry_rejected'):
                window.SequentialAndroidWindow(**{**args, 'reservation_expires_at_milliseconds': value})
        with self.assertRaisesRegex(EntryError, 'profile_rejected'):
            window.SequentialAndroidWindow(**{**args, 'profile': 'nine_minutes_matrix_r4'})

    def test_r3_full_order_and_entry360_boundary_cleanup538_remain_bounded(self):
        args = {**self.arguments, 'profile': window.NINE_MINUTES_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 538000}
        owned = window.SequentialAndroidWindow(**args)
        proof = {**self.proof, 'reservationExpiresAt': self.root_epoch + 538000}
        for index, batch in enumerate(window.ORDER):
            self.elapsed = 60 + index * 120
            lease = owned.enter(batch, geometry_sha256=None if index == 0 else 'a' * 64)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            lease.validate(self.values(batch), self.build, proof)
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        self.elapsed = 360
        last = window.SequentialAndroidWindow(**args).enter(window.ORDER[0])
        self.assertEqual(last.deadline - last.entered_monotonic, 150)
        self.elapsed = 360.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            window.SequentialAndroidWindow(**args).enter(window.ORDER[0])
        self.elapsed = 538
        receipt = owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.assertEqual(receipt['hardCleanupRootSeconds'], 538)
        self.elapsed = 538.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)


class TwelveMinuteProfileTests(WindowInputs):
    def test_r11_is_finite718_690_718_twelve_minutes_and_has_no_old_ten_dollar_claim(self):
        args = {**self.arguments, 'profile': window.TWELVE_MINUTES_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 718000}
        owned = window.SequentialAndroidWindow(**args)
        self.assertEqual(owned.cancel_monotonic, self.root_monotonic + 690)
        self.assertEqual(owned.cleanup_monotonic, self.root_monotonic + 718)
        receipt = owned.limits_receipt
        self.assertEqual(receipt['maximumProviderExpiryRootMilliseconds'], 718000)
        self.assertEqual(receipt['currentAllocationMaximumBillableMinutes'], 12)
        self.assertIsNone(receipt['priorNewAllocationBillableMinutesRetained'])
        self.assertIsNone(receipt['totalNewAllocationBillableMinutesUpper'])
        self.assertEqual(receipt['limitScope'], 'current_allocation_only')
        self.assertEqual(math.ceil((718 + 1) / 60), 12)
        self.assertIsNone(receipt['retainedModeledUpperUsd'])
        self.assertFalse(receipt['historicalTenUsdScopeApplies'])
        self.assertFalse(receipt['historicalDebitsAndReserveChanged'])
        for key, value in (('prior_terminal_ledger_path', '/private/tmp/old'),
                ('prior_terminal_ledger_sha256', 'a' * 64), ('prior_whole_root_milliseconds', 1),
                ('current_allocation_name_sha256', 'b' * 64)):
            with self.subTest(key=key), self.assertRaisesRegex(EntryError, 'prior_rejected'):
                window.SequentialAndroidWindow(**{**args, key: value})
        for value in (self.root_epoch + 718001, True, None):
            with self.assertRaisesRegex(EntryError, 'expiry_rejected'):
                window.SequentialAndroidWindow(**{**args, 'reservation_expires_at_milliseconds': value})
        with self.assertRaisesRegex(EntryError, 'profile_rejected'):
            window.SequentialAndroidWindow(**{**args, 'profile': 'twelve_minutes_matrix_r12'})

    def test_r11_full_order_and_entry540_boundary_cleanup718_remain_bounded(self):
        args = {**self.arguments, 'profile': window.TWELVE_MINUTES_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 718000}
        owned = window.SequentialAndroidWindow(**args)
        proof = {**self.proof, 'reservationExpiresAt': self.root_epoch + 718000}
        for index, batch in enumerate(window.ORDER):
            self.elapsed = 60 + index * 120
            lease = owned.enter(batch, geometry_sha256=None if index == 0 else 'a' * 64)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            lease.validate(self.values(batch), self.build, proof)
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        self.elapsed = 540
        last = window.SequentialAndroidWindow(**args).enter(window.ORDER[0])
        self.assertEqual(last.deadline - last.entered_monotonic, 150)
        self.elapsed = 540.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            window.SequentialAndroidWindow(**args).enter(window.ORDER[0])
        self.elapsed = 718
        receipt = owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.assertEqual(receipt['hardCleanupRootSeconds'], 718)
        self.elapsed = 718.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)


class QualityProfileTests(WindowInputs):
    def quality_window(self, **changes):
        return window.SequentialAndroidWindow(**{**self.arguments,
            'profile': window.QUALITY_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 718000, **changes})

    def test_quality_A_full_100_075_order_is_isolated_and_each_native_entry_is_bounded(self):
        owned = self.quality_window()
        self.assertEqual(owned.order, window.QUALITY_ORDER)
        self.assertEqual(window.SequentialAndroidWindow(**self.arguments).order, window.ORDER)
        proof = {**self.proof, 'reservationExpiresAt': self.root_epoch + 718000}
        for index, batch in enumerate(window.QUALITY_ORDER):
            self.elapsed = 60 + 120 * index
            lease = owned.enter(batch, geometry_sha256=None if index == 0 else 'a' * 64)
            lease.validate(self.values(batch), self.build, proof)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            self.assertGreaterEqual(lease.deadline - lease.entered_monotonic, 150)
            with self.assertRaisesRegex(EntryError, 'order'):
                owned.enter(batch, geometry_sha256='a' * 64)
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        with self.assertRaisesRegex(EntryError, 'order'):
            owned.enter(window.QUALITY_ORDER[-1], geometry_sha256='a' * 64)

    def test_quality_refuses_primary_wrong_order_geometry_failed_run_and_wrong_allocation(self):
        owned = self.quality_window()
        for batch in (*window.ORDER[1:], *window.QUALITY_ORDER[1:]):
            with self.subTest(batch=batch), self.assertRaisesRegex(EntryError, 'order'):
                owned.enter(batch)
        lease = owned.enter(window.QUALITY_ORDER[0])
        owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
        with self.assertRaisesRegex(EntryError, 'geometry'):
            owned.enter(window.QUALITY_ORDER[1], geometry_sha256='b' * 64)
        lease = owned.enter(window.QUALITY_ORDER[1], geometry_sha256='a' * 64)
        with self.assertRaisesRegex(EntryError, 'identity'):
            lease.validate(self.values(window.QUALITY_ORDER[1]), self.build,
                {**self.proof, 'reservationExpiresAt': self.root_epoch + 718000,
                    'reservationReceiptSha256': 'b' * 64})
        owned.finish(lease, succeeded=False)
        with self.assertRaisesRegex(EntryError, 'order'):
            owned.enter(window.QUALITY_ORDER[2], geometry_sha256='a' * 64)

    def test_quality718_expiry_690_cancellation_718_cleanup_and_dispatch150_boundary(self):
        owned = self.quality_window()
        limits = owned.limits_receipt
        self.assertEqual(limits['maximumProviderExpiryRootMilliseconds'], 718000)
        self.assertEqual(limits['cancelAtRootSeconds'], 690)
        self.assertEqual(limits['hardCleanupRootSeconds'], 718)
        self.assertIsNone(limits['retainedModeledUpperUsd'])
        self.assertFalse(limits['historicalTenUsdScopeApplies'])
        self.elapsed = 540
        lease = owned.enter(window.QUALITY_ORDER[0])
        self.assertEqual(lease.deadline - lease.entered_monotonic, 150)
        self.elapsed = 540.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'):
            lease.require_full_entry_budget()
        owned.finish(lease, succeeded=False)
        self.elapsed = 718
        owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.elapsed = 718.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        for changes in ({'reservation_expires_at_milliseconds': self.root_epoch + 718001},
                {'prior_whole_root_milliseconds': 1}, {'current_allocation_name_sha256': 'b' * 64}):
            with self.subTest(changes=changes), self.assertRaises(EntryError):
                self.quality_window(**changes)
        self.elapsed = 60
        for profile in (window.DEFAULT_PROFILE, window.SEVEN_MINUTES_PROFILE,
                window.NINE_MINUTES_PROFILE, window.TWELVE_MINUTES_PROFILE):
            # Existing windows retain their primary-only order without a quality slot.
            if profile == window.SEVEN_MINUTES_PROFILE:
                continue
            old = window.SequentialAndroidWindow(**{**self.arguments, 'profile': profile})
            with self.assertRaisesRegex(EntryError, 'order'):
                old.enter(window.QUALITY_ORDER[1])


class SeededQualityProfileTests(WindowInputs):
    def quality_window(self, **changes):
        return window.SequentialAndroidWindow(**{**self.arguments,
            'profile': window.SEEDED_QUALITY_PROFILE,
            'reservation_expires_at_milliseconds': self.root_epoch + 718000,
            'expected_geometry_sha256': 'a' * 64, **changes})

    def test_seed_is_required_and_immutable_before_any_entry_and_rejected_by_every_old_profile(self):
        for value in (None, True, {}, '', 'A' * 64, 'a' * 63):
            with self.subTest(value=value), self.assertRaisesRegex(EntryError, 'geometry_seed_rejected'):
                self.quality_window(expected_geometry_sha256=value)
        owned = self.quality_window()
        self.assertEqual(owned._geometry_sha256, 'a' * 64)
        self.assertEqual(owned.expected_geometry_sha256, 'a' * 64)
        self.assertEqual(owned.limits_receipt['expectedGeometrySha256'], 'a' * 64)
        self.assertEqual(owned.limits_receipt['geometrySeedScope'], 'expected_layout_only')
        with self.assertRaises(FrozenInstanceError): owned.expected_geometry_sha256 = 'b' * 64
        for profile in (window.DEFAULT_PROFILE, window.SEVEN_MINUTES_PROFILE,
                window.NINE_MINUTES_PROFILE, window.TWELVE_MINUTES_PROFILE, window.QUALITY_PROFILE):
            with self.subTest(profile=profile), self.assertRaisesRegex(EntryError, 'geometry_seed_rejected'):
                window.SequentialAndroidWindow(**{**self.arguments, 'profile': profile,
                    'reservation_expires_at_milliseconds': self.root_epoch + window._PROFILE_LIMITS[profile][0],
                    'expected_geometry_sha256': 'a' * 64})

    def test_seeded_full100075_order_without_A_and_first_entry_requires_matching_frozen_hash(self):
        owned = self.quality_window()
        self.assertEqual(owned.order, window.SEEDED_QUALITY_ORDER)
        self.assertEqual(owned.order, window.QUALITY_ORDER[1:])
        for batch in (*window.ORDER, *window.SEEDED_QUALITY_ORDER[1:]):
            with self.subTest(batch=batch), self.assertRaisesRegex(EntryError, 'order'):
                owned.enter(batch, geometry_sha256='a' * 64)
        for value in (None, 'b' * 64):
            with self.subTest(value=value), self.assertRaisesRegex(EntryError, 'geometry'):
                owned.enter(window.SEEDED_QUALITY_ORDER[0], geometry_sha256=value)
        proof = {**self.proof, 'reservationExpiresAt': self.root_epoch + 718000}
        for index, batch in enumerate(window.SEEDED_QUALITY_ORDER):
            self.elapsed = 60 + index * 40
            lease = owned.enter(batch, geometry_sha256='a' * 64)
            lease.validate(self.values(batch), self.build, proof)
            self.assertGreaterEqual(lease.deadline - lease.entered_monotonic, 150)
            self.assertLessEqual(lease.deadline - lease.entered_monotonic, 180)
            with self.assertRaisesRegex(EntryError, 'order'):
                owned.enter(batch, geometry_sha256='a' * 64)
            owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
            self.assertEqual(owned._geometry_sha256, 'a' * 64)
        with self.assertRaisesRegex(EntryError, 'order'):
            owned.enter(window.SEEDED_QUALITY_ORDER[0], geometry_sha256='a' * 64)

    def test_first_and_later_success_cannot_replace_or_omit_the_expected_geometry(self):
        for index in range(3):
            for observed in (None, 'b' * 64):
                owned = self.quality_window()
                for batch in window.SEEDED_QUALITY_ORDER[:index]:
                    lease = owned.enter(batch, geometry_sha256='a' * 64)
                    owned.finish(lease, succeeded=True, geometry_sha256='a' * 64)
                lease = owned.enter(window.SEEDED_QUALITY_ORDER[index], geometry_sha256='a' * 64)
                with self.subTest(index=index, observed=observed), self.assertRaisesRegex(EntryError, 'geometry'):
                    owned.finish(lease, succeeded=True, geometry_sha256=observed)
                self.assertEqual(owned._geometry_sha256, 'a' * 64)
                self.assertTrue(owned._failed)
                self.assertIsNone(owned._active)

    def test_seeded_window_keeps_exact_expiry_cancel_cleanup_and_final_dispatch_budget(self):
        owned = self.quality_window()
        self.assertEqual(owned.limits_receipt['maximumProviderExpiryRootMilliseconds'], 718000)
        self.assertEqual(owned.cancel_monotonic, self.root_monotonic + 690)
        self.assertEqual(owned.cleanup_monotonic, self.root_monotonic + 718)
        self.assertIsNone(owned.limits_receipt['retainedModeledUpperUsd'])
        self.assertFalse(owned.limits_receipt['historicalTenUsdScopeApplies'])
        self.elapsed = 540
        lease = owned.enter(window.SEEDED_QUALITY_ORDER[0], geometry_sha256='a' * 64)
        lease.require_full_entry_budget()
        self.elapsed = 540.001
        with self.assertRaisesRegex(EntryError, 'entry_refused'): lease.require_full_entry_budget()
        owned.finish(lease, succeeded=False)
        self.elapsed = 718
        owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        self.elapsed = 718.001
        with self.assertRaisesRegex(EntryError, 'cleanup'):
            owned.validate_cleanup(allocation_sha256='2' * 64, terminal_observed=True)
        for changes in ({'reservation_expires_at_milliseconds': self.root_epoch + 718001},
                {'prior_whole_root_milliseconds': 1}, {'current_allocation_name_sha256': 'b' * 64}):
            with self.subTest(changes=changes), self.assertRaises(EntryError): self.quality_window(**changes)

    def test_reader_run_checks_all13_actual_geometry_fields_before_publication_and_blocks_changed_success(self):
        from types import SimpleNamespace
        observed = dict(zip(reader.GEOMETRY_FIELDS, (2, 3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'portrait')))
        expected_hash = sha256(frozen_json(reader.geometry(observed))).hexdigest()
        selected = SimpleNamespace(values=self.values(window.SEEDED_QUALITY_ORDER[0]))
        for key in reader.GEOMETRY_FIELDS:
            owned = self.quality_window(expected_geometry_sha256=expected_hash)
            changed = {**observed, key: 'landscape' if key == 'orientation' else observed[key] + .25}
            with tempfile.TemporaryDirectory() as folder, \
                    patch.object(reader, '_run_observed', return_value={'geometry': changed}):
                output = Path(folder).resolve()
                with self.subTest(key=key), self.assertRaisesRegex(EntryError, 'geometry'):
                    reader._run(selected, None, output, sequential_window=owned, frozen_geometry=observed)
                self.assertFalse((output / 'reader-result.json').exists())
                self.assertEqual(owned._geometry_sha256, expected_hash)
                with self.assertRaisesRegex(EntryError, 'order'):
                    owned.enter(window.SEEDED_QUALITY_ORDER[1], geometry_sha256=expected_hash)


if __name__ == '__main__':
    unittest.main()
