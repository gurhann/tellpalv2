from copy import deepcopy
from hashlib import sha256
import unittest
from unittest.mock import Mock

import critical_android_guard_pilot as android
import critical_ios_guard_pilot as ios
from native_entry_config import EntryError, frozen_json
from test_critical_ios_guard_pilot import observation, ledger


def android_observation(fault):
    value = observation(fault)
    value.update(schemaVersion=1, scope=android.SCOPE, platform='android', mode='release',
        launcher={'status': 'ok', 'exitCode': 0},
        nativeStage=android.EXPECTED_STAGES.get(fault),
        nativeReason='NATIVE_LAUNCHER_SETTLED' if fault == 'normal-teardown' else 'NATIVE_LAUNCHER_REFUSED',
        originalDriverReason=None if fault == 'normal-teardown' else
            'launcher_pairing_unsettled' if fault == 'alternate-trust' else 'native_entry_refused')
    for key in ('sourceSha256', 'artifactSha256', 'testArtifactSha256', 'installationReceiptSha256',
        'prerequisiteReceiptSha256', 'sessionSha256', 'manifestSha256', 'publicManifestSha256'):
        value[key] = 'a' * 64
    if fault == 'normal-teardown':
        value['originalEntryOutcome'].update(fixtureDeliveryCount=1, platform='android', mode='release',
            fixtureBodyBytes=value['fixtureByteSize'], fixtureOutputSha256=value['fixtureOutputSha256'],
            **{key: value[key] for key in ('generation', 'sessionSha256', 'caseSha256', 'artifactSha256', 'testArtifactSha256')})
    return value


class AndroidGuardTests(unittest.TestCase):
    def test_pin_refusal_matches_original_driver_pre_pairing_guard_order(self):
        pairing = Mock(count=0, pending=None, native_result_count=1, native_result='refused')
        pairing.ready.is_set.return_value = False
        with self.assertRaisesRegex(EntryError, '^launcher_pairing_unsettled$'):
            android.host.validate_entry_outcome(Mock(), pairing)
        receipt = android_observation('alternate-trust')
        android.validate_android_observation(receipt)
        with self.assertRaises(EntryError):
            android.validate_android_observation({**receipt, 'originalDriverReason': 'native_entry_refused'})
        for fault in ('expired-envelope', 'wrong-uid-envelope', 'corrupt-bytes'):
            with self.assertRaises(EntryError):
                android.validate_android_observation({**android_observation(fault),
                    'originalDriverReason': 'launcher_pairing_unsettled'})

    def test_five_actual_android_marker_and_boundary_contracts(self):
        for fault in ios.FAULTS:
            receipt = android_observation(fault)
            android.validate_android_observation(receipt)
            for key, bad in (('platform', 'ios'), ('scope', ios.PILOT_SCOPE), ('mode', 'profile'),
                ('sourceUnchanged', False), ('ownedCleanup', False), ('nativeResultCount', True),
                ('nativeResultCount', 0), ('nativeReason', None), ('nativeResult', None),
                ('launcher', {'status': 'failed', 'exitCode': 1})):
                with self.subTest(fault=fault, key=key), self.assertRaises(EntryError):
                    android.validate_android_observation({**receipt, key: bad})
            if fault != 'normal-teardown':
                for value in (None, 'NATIVE_SETTLEMENT' if fault != 'corrupt-bytes' else 'NATIVE_PROBE'):
                    with self.assertRaises(EntryError): android.validate_android_observation({**receipt, 'nativeStage': value})
                with self.assertRaises(EntryError):
                    android.validate_android_observation({**receipt, 'originalDriverReason': 'tool_failed'})

    def test_no_false_ios_validation_or_normal_positive_without_single_transfer(self):
        receipt = android_observation('normal-teardown')
        for count in (0, 2, True):
            changed = deepcopy(receipt); changed['originalEntryOutcome']['fixtureDeliveryCount'] = count
            with self.assertRaises(EntryError): android.validate_android_observation(changed)
        for key, bad in (('mode', 'profile'), ('platform', 'ios'), ('generation', False), ('sessionSha256', '0' * 64), ('fixtureBodyBytes', True)):
            changed = deepcopy(receipt); changed['originalEntryOutcome'][key] = bad
            with self.assertRaises(EntryError): android.validate_android_observation(changed)
        with self.assertRaises(EntryError):
            android.AndroidGuardPairing(Mock(), 'ios')
        with self.assertRaises(EntryError): ios.GuardPairing(Mock(), 'android')

    def test_each_refusal_retains_original_boundary_guards(self):
        for fault in ('alternate-trust', 'expired-envelope', 'wrong-uid-envelope'):
            receipt = android_observation(fault)
            receipt['boundaryCounts']['fixtureDispatch'] = 1
            with self.assertRaises(EntryError): android.validate_android_observation(receipt)
        for key in ('claim', 'fixtureDispatch', 'fixtureFlushed', 'originalDeliveryGuardRefused'):
            receipt = android_observation('corrupt-bytes'); receipt['boundaryCounts'][key] = 0
            with self.assertRaises(EntryError): android.validate_android_observation(receipt)

    def test_normal_and_corrupt_require_fresh_bound_private_ledger(self):
        for fault in ('corrupt-bytes', 'normal-teardown'):
            receipt = android_observation(fault)
            raw = frozen_json(ledger(fault))
            self.assertEqual(ios.validate_ledger(raw, receipt)['freshSessionProviderBinding'], 'observed')
            with self.assertRaises(EntryError):
                ios.validate_ledger(raw, {**receipt, 'priorLedgerSha256': sha256(raw).hexdigest()})
            value = ledger(fault)
            value['records'] = [row for row in value['records'] if not (row.get('event') == 'ownership' and row.get('ownership') == 'released')]
            with self.assertRaises(EntryError): ios.validate_ledger(frozen_json(value), receipt)
            value = ledger(fault); value['records'][0]['providerSha256'] = '0' * 64
            with self.assertRaises(EntryError): ios.validate_ledger(frozen_json(value), receipt)

    def test_exact_real_android_status_markers_and_duplicate_stage(self):
        pairing = android.AndroidGuardPairing(Mock())
        for line in (b'INSTRUMENTATION_RESULT: reason=NATIVE_LAUNCHER_REFUSED',
            b'INSTRUMENTATION_RESULT: diagnosticStage=NATIVE_PROBE', b'INSTRUMENTATION_CODE: 0'):
            pairing(line)
        self.assertEqual(pairing.native_result, 'refused')
        self.assertEqual(pairing.stage, 'NATIVE_PROBE')
        with self.assertRaises(EntryError): pairing(b'INSTRUMENTATION_RESULT: diagnosticStage=NATIVE_PROBE')
        with self.assertRaises(EntryError): android.AndroidGuardPairing(Mock())(b'INSTRUMENTATION_RESULT: diagnosticStage=SECRET')
        with self.assertRaises(EntryError):
            pairing(b'INSTRUMENTATION_RESULT: reason=NATIVE_LAUNCHER_SETTLED')


if __name__ == '__main__': unittest.main()
