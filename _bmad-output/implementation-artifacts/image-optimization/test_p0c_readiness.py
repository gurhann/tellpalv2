#!/usr/bin/env python3
"""Synthetic readiness regressions; no device discovery, builds, auth, or network."""
import copy
from hashlib import sha256
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


HERE = Path(__file__).resolve().parent
VERIFIER_PATH = HERE / 'verify-p0c-readiness.py'
MATRIX_PATH = HERE / 'p0c-device-matrix.json'
SPEC = importlib.util.spec_from_file_location('p0c_readiness_verifier', VERIFIER_PATH)
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)
SYNTHETIC_RUNTIME = 'com.apple.CoreSimulator.SimRuntime.iOS-99-1'
SIMULATOR_GAPS = {
    'product_type': 'unverified',
    'os_build': 'unverified',
    'pairing': 'not_applicable',
    'ddi_services_available': 'not_applicable',
}


def observed(value, evidence_ref):
    return {'value': value, 'availability': 'observed',
            'evidence_ref': evidence_ref, 'reason': None}


def unknown(availability, evidence_ref):
    return {'value': None, 'availability': availability,
            'evidence_ref': evidence_ref, 'reason': 'Synthetic discovery does not report this field.'}


def device_fixture():
    """Use explicit fake Apple families and stable opaque aliases for both kinds."""
    physical_raw = []
    simulator_raw = []
    devices = []
    for index, (family, form_factor) in enumerate((('iPhone', 'phone'), ('iPad', 'tablet'))):
        raw = {
            'hardwareProperties': {'platform': 'iOS', 'reality': 'physical',
                                   'marketingName': family + ' Synthetic', 'productType': family + '99,1'},
            'deviceProperties': {'osVersionNumber': '99.1', 'osBuildUpdate': 'SYNTHETIC-BUILD',
                                 'ddiServicesAvailable': False},
            'connectionProperties': {'tunnelState': 'disconnected', 'pairingState': 'paired'},
        }
        physical_raw.append(raw)
        devices.append({
            'alias': f'ios-physical-{index + 1}', 'kind': 'physical', 'form_factor': form_factor,
            'evidence_pointer': f'/result/devices/{index}',
            'model': observed(family + ' Synthetic', 'E-IOS-DEVICES'),
            'product_type': observed(family + '99,1', 'E-IOS-DEVICES'),
            'os': observed('99.1', 'E-IOS-DEVICES'),
            'os_build': observed('SYNTHETIC-BUILD', 'E-IOS-DEVICES'),
            'connection': observed('disconnected', 'E-IOS-DEVICES'),
            'pairing': observed('paired', 'E-IOS-DEVICES'),
            'ddi_services_available': observed(False, 'E-IOS-DEVICES'),
        })
        simulator_raw.append({
            'deviceTypeIdentifier': 'com.apple.CoreSimulator.SimDeviceType.' + family + '-Synthetic',
            'isAvailable': True, 'state': 'Shutdown',
        })
        devices.append({
            'alias': f'ios-simulator-{index + 1}', 'kind': 'simulator', 'form_factor': form_factor,
            'evidence_pointer': f'/devices/{SYNTHETIC_RUNTIME}/{index}',
            'model': observed(family + ' Synthetic', 'E-SIMULATORS'),
            'os': observed('99.1', 'E-SIMULATORS'),
            'connection': observed('Shutdown', 'E-SIMULATORS'),
            **{field: unknown(availability, 'E-SIMULATORS') for field, availability in SIMULATOR_GAPS.items()},
        })
    matrix = {
        'devices': devices,
        'virtual_runtime_summary': [{'runtime': SYNTHETIC_RUNTIME, 'registered_count': 2,
                                     'available_count': 2, 'booted_count': 0}],
    }
    receipts = {'E-IOS-DEVICES': {'result': {'devices': physical_raw}},
                'E-SIMULATORS': {'devices': {SYNTHETIC_RUNTIME: simulator_raw}}}
    return matrix, receipts


def by_id(records, identifier):
    return next(row for row in records if row['id'] == identifier)


def by_alias(matrix, alias):
    return next(row for row in matrix['devices'] if row['alias'] == alias)


class PublicMatrixCliTests(unittest.TestCase):
    """Run only the Python verifier CLI, whose subprocesses are local read-only Git."""
    @classmethod
    def setUpClass(cls):
        cls.public_matrix = VERIFIER.load_json(MATRIX_PATH)

    def cli(self, matrix_path):
        return subprocess.run([sys.executable, '-B', str(VERIFIER_PATH), '--matrix', str(matrix_path)],
                              cwd=VERIFIER.ROOT, capture_output=True, text=True, check=False, timeout=30)

    def assert_rejected(self, mutate, expected_reason):
        matrix = copy.deepcopy(self.public_matrix)
        mutate(matrix)
        with tempfile.TemporaryDirectory(prefix='p0c-synthetic-matrix-') as directory:
            path = Path(directory) / 'matrix.json'
            path.write_text(json.dumps(matrix, indent=2) + '\n', encoding='utf-8')
            result = self.cli(path)
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertEqual(result.stdout, '')
        report = json.loads(result.stderr)
        self.assertEqual(report['preparation_checks'], 'FAIL')
        self.assertIn(expected_reason, report['reason'])

    def counter_value(self, value):
        def mutate(matrix):
            row = matrix['counters'][0]['observation']
            row.update(value=value, availability='observed', reason=None)
        return mutate

    def test_public_matrix_cli_passes_without_native_qualification(self):
        result = self.cli(MATRIX_PATH)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, '')
        report = json.loads(result.stdout)
        self.assertEqual(report['preparation_checks'], 'PASS')
        self.assertIs(report['preparation_complete'], True)
        self.assertIs(report['native_execution_ready'], False)
        self.assertEqual(report['evidence_mode'], 'public_only')
        self.assertEqual(report['private_receipts_rehashed'], 0)
        self.assertEqual(report['physical_devices_qualified'], 0)
        self.assertEqual(report['coverage_rows'], 9)
        self.assertEqual(report['counter_gaps'], 13)
        self.assertEqual(report['open_native_gates'], sorted(VERIFIER.GATES))
        self.assertEqual(report['source_hashes_checked'], len(VERIFIER.REQUIRED_SOURCES))
        self.assertGreater(report['local_links_checked'], 0)
        self.assertIsNotNone(report['limitation'])

    def test_false_native_readiness_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix.__setitem__('native_execution_ready', True),
                             'Preparation cannot claim native readiness')

    def test_false_disconnected_qualification_is_rejected(self):
        self.assert_rejected(lambda matrix: by_alias(matrix, 'ios-physical-1').__setitem__(
            'qualified_for_native_batch', True), 'Discovery record cannot qualify native batch')

    def test_virtual_physical_substitution_is_rejected(self):
        self.assert_rejected(lambda matrix: by_id(matrix['coverage'], 'oldest-available-iphone').__setitem__(
            'device_aliases', ['ios-simulator-1']), 'Virtual/incompatible physical coverage')

    def test_lost_provenance_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['tools'][0]['identity'].__setitem__('evidence_ref', ''),
                             'missing/unbound provenance')

    def test_failed_sandbox_as_observed_is_rejected(self):
        self.assert_rejected(lambda matrix: by_id(matrix['probes'], 'adb-devices').__setitem__(
            'availability', 'observed'), 'Invalid probe availability')

    def test_wrong_receipt_binding_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['evidence_sources'][0].__setitem__(
            'receipt_file', 'synthetic-wrong-receipt.json'), 'Changed receipt-source binding')

    def test_changed_source_hash_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['source_snapshot'][0].__setitem__('sha256', '0' * 64),
                             'Changed current source hash')

    def test_boolean_counter_is_rejected(self):
        self.assert_rejected(self.counter_value(True), 'invalid nonnegative numeric value')

    def test_negative_counter_is_rejected(self):
        self.assert_rejected(self.counter_value(-1), 'invalid nonnegative numeric value')

    def test_nonfinite_counter_is_rejected(self):
        self.assert_rejected(self.counter_value(float('nan')), 'Nonfinite JSON number')

    def test_unknown_counter_unit_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['counters'][0].__setitem__('unit', 'synthetic-unit'),
                             'Invalid counter unit/tool/scope')

    def test_missing_floor_coverage_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix.__setitem__('coverage', [row for row in matrix['coverage']
                             if row['id'] != 'android-api24-floor']), 'Missing required physical/floor coverage')

    def test_duplicate_alias_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['devices'].append(copy.deepcopy(matrix['devices'][0])),
                             'duplicate identifier')

    def test_invalid_timestamp_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix.__setitem__('observed_at', 'invalid-synthetic-timestamp'),
                             'invalid timestamp')

    def test_boolean_summary_count_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['summary'].__setitem__('adb_connected_devices', False),
                             'invalid nonnegative numeric value')

    def test_native_gate_closure_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['gates'][0].__setitem__('status', 'closed'),
                             'Native gate closed by preparation')

    def test_invented_runtime_build_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['experiment_build'].__setitem__('renderer', 'synthetic-renderer'),
                             'Invented native experiment build')

    def test_unknown_ram_as_zero_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['devices'][0]['ram_bytes'].update(
            value=0, availability='observed', reason=None), 'observed RAM must be positive')

    def test_raw_device_field_is_rejected(self):
        self.assert_rejected(lambda matrix: matrix['devices'][0].__setitem__('raw_device_id', 'synthetic-device-id'),
                             'Unexpected/missing device fields')

    def test_duplicate_json_key_cli_is_rejected(self):
        with tempfile.TemporaryDirectory(prefix='p0c-synthetic-matrix-') as directory:
            path = Path(directory) / 'matrix.json'
            path.write_bytes(b'{"schema_version":1,"schema_version":1}')
            result = self.cli(path)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, '')
        self.assertEqual(json.loads(result.stderr),
                         {'preparation_checks': 'FAIL', 'reason': 'Duplicate JSON object key'})


class ReceiptProjectionTests(unittest.TestCase):
    def replay(self, matrix, receipts):
        with mock.patch.object(VERIFIER, 'replay_tools_and_probes') as tools:
            VERIFIER.replay_observations(matrix, receipts)
        tools.assert_called_once_with(matrix, receipts)

    def assert_replay_rejected(self, matrix, receipts, reason):
        with mock.patch.object(VERIFIER, 'replay_tools_and_probes') as tools:
            with self.assertRaisesRegex(ValueError, reason):
                VERIFIER.replay_observations(matrix, receipts)
        tools.assert_not_called()

    def assert_family(self, alias, kind, form_factor):
        matrix, receipts = device_fixture()
        device = by_alias(matrix, alias)
        raw = VERIFIER.pointer(receipts['E-IOS-DEVICES' if kind == 'physical' else 'E-SIMULATORS'],
                               device['evidence_pointer'])
        self.assertEqual(VERIFIER.receipt_form_factor(raw, kind), form_factor)
        self.replay(matrix, receipts)

    def test_physical_phone_form_factor_is_derived_from_receipt(self):
        self.assert_family('ios-physical-1', 'physical', 'phone')

    def test_physical_tablet_form_factor_is_derived_from_receipt(self):
        self.assert_family('ios-physical-2', 'physical', 'tablet')

    def test_simulator_phone_form_factor_is_derived_from_receipt(self):
        self.assert_family('ios-simulator-1', 'simulator', 'phone')

    def test_simulator_tablet_form_factor_is_derived_from_receipt(self):
        self.assert_family('ios-simulator-2', 'simulator', 'tablet')

    def test_declared_form_factor_cannot_override_receipt(self):
        for alias in ('ios-physical-1', 'ios-physical-2', 'ios-simulator-1', 'ios-simulator-2'):
            with self.subTest(alias=alias):
                matrix, receipts = device_fixture()
                row = by_alias(matrix, alias)
                row['form_factor'] = 'tablet' if row['form_factor'] == 'phone' else 'phone'
                self.assert_replay_rejected(matrix, receipts, 'Private device form-factor replay mismatch')

    def test_conflicting_physical_product_and_marketing_families_are_rejected(self):
        for product, model in (('iPhone99,1', 'iPad Synthetic'), ('iPad99,1', 'iPhone Synthetic')):
            with self.subTest(product=product, model=model):
                matrix, receipts = device_fixture()
                receipts['E-IOS-DEVICES']['result']['devices'][0]['hardwareProperties'].update(
                    productType=product, marketingName=model)
                self.assert_replay_rejected(matrix, receipts, 'Unsupported/conflicting physical form-factor receipt')

    def test_unsupported_physical_product_families_are_rejected(self):
        for product, model in (('Mac99,1', 'Mac Synthetic'), ('iPhoneSynthetic', 'iPhone Synthetic'),
                               ('iPhone99,1', 'Synthetic Phone'), ('iPad99,1', 'iPadSynthetic')):
            with self.subTest(product=product, model=model):
                matrix, receipts = device_fixture()
                receipts['E-IOS-DEVICES']['result']['devices'][0]['hardwareProperties'].update(
                    productType=product, marketingName=model)
                self.assert_replay_rejected(matrix, receipts, 'Unsupported/conflicting physical form-factor receipt')

    def test_unsupported_simulator_identifiers_are_rejected(self):
        identifiers = ('com.apple.CoreSimulator.SimDeviceType.AppleTV-Synthetic',
                       'synthetic.iPhone-Synthetic', 'com.apple.CoreSimulator.SimDeviceType.iPhoneSynthetic')
        for identifier in identifiers:
            with self.subTest(identifier=identifier):
                matrix, receipts = device_fixture()
                receipts['E-SIMULATORS']['devices'][SYNTHETIC_RUNTIME][0]['deviceTypeIdentifier'] = identifier
                self.assert_replay_rejected(matrix, receipts, 'Unsupported simulator form-factor receipt')

    def test_simulator_absent_metadata_retains_exact_null_availability_and_provenance(self):
        matrix, receipts = device_fixture()
        for device in matrix['devices']:
            if device['kind'] == 'simulator':
                for field, availability in SIMULATOR_GAPS.items():
                    with self.subTest(alias=device['alias'], field=field):
                        self.assertEqual(device[field], unknown(availability, 'E-SIMULATORS'))
        self.replay(matrix, receipts)

    def test_simulator_absent_metadata_values_cannot_be_invented(self):
        values = {'product_type': 'iPhone99,1', 'os_build': 'SYNTHETIC-BUILD',
                  'pairing': 'paired', 'ddi_services_available': False}
        for field, value in values.items():
            with self.subTest(field=field):
                matrix, receipts = device_fixture()
                by_alias(matrix, 'ios-simulator-1')[field] = observed(value, 'E-SIMULATORS')
                self.assert_replay_rejected(matrix, receipts, 'Unsupported simulator metadata claim')

    def test_simulator_absent_metadata_availability_cannot_be_relabeled(self):
        for field, availability in SIMULATOR_GAPS.items():
            with self.subTest(field=field):
                matrix, receipts = device_fixture()
                by_alias(matrix, 'ios-simulator-1')[field]['availability'] = (
                    'not_applicable' if availability == 'unverified' else 'unverified')
                self.assert_replay_rejected(matrix, receipts, 'Unsupported simulator metadata claim')

    def test_simulator_absent_metadata_provenance_cannot_be_rebound(self):
        for field in SIMULATOR_GAPS:
            with self.subTest(field=field):
                matrix, receipts = device_fixture()
                by_alias(matrix, 'ios-simulator-1')[field]['evidence_ref'] = 'E-IOS-DEVICES'
                self.assert_replay_rejected(matrix, receipts, 'Unsupported simulator metadata claim')

    def test_simulator_absent_metadata_requires_nonempty_reason(self):
        for field in SIMULATOR_GAPS:
            for reason in (None, '', '   '):
                with self.subTest(field=field, reason=reason):
                    matrix, receipts = device_fixture()
                    by_alias(matrix, 'ios-simulator-1')[field]['reason'] = reason
                    self.assert_replay_rejected(matrix, receipts, 'simulator metadata gap: missing text')

    def test_simulator_absent_metadata_requires_complete_observation_shape(self):
        for field in SIMULATOR_GAPS:
            with self.subTest(field=field):
                matrix, receipts = device_fixture()
                by_alias(matrix, 'ios-simulator-1')[field].pop('reason')
                self.assert_replay_rejected(matrix, receipts, 'Unsupported simulator metadata claim')


class ReceiptByteSnapshotTests(unittest.TestCase):
    def receipt_source(self, data):
        return {'evidence_sources': [{'id': 'E-IOS-DEVICES', 'receipt_file': 'devicectl-escalated.json',
                                      'receipt_sha256': sha256(data).hexdigest()}]}

    def test_private_receipt_hash_and_parse_use_the_same_single_read(self):
        first = b'{"synthetic_snapshot":"first"}'
        later = b'{"synthetic_snapshot":"later"}'
        matrix = self.receipt_source(first)
        with tempfile.TemporaryDirectory(prefix='p0c-synthetic-receipt-') as directory:
            root = Path(directory).resolve()
            path = root / matrix['evidence_sources'][0]['receipt_file']
            path.write_bytes(first)
            reads = []

            def read_snapshot(selected):
                self.assertEqual(selected, path)
                reads.append(selected)
                return first if len(reads) == 1 else later

            with mock.patch.object(Path, 'read_bytes', autospec=True, side_effect=read_snapshot) as read:
                with mock.patch.object(VERIFIER, 'parse_json', wraps=VERIFIER.parse_json) as parse:
                    with mock.patch.object(VERIFIER, 'replay_observations') as replay:
                        VERIFIER.replay_private(matrix, root)
        self.assertEqual(read.call_count, 1)
        parse.assert_called_once_with(first)
        replay.assert_called_once_with(matrix, {'E-IOS-DEVICES': {'synthetic_snapshot': 'first'}})

    def test_receipt_hash_mismatch_is_rejected_before_parse_or_projection(self):
        original = b'{"synthetic_snapshot":"first"}'
        matrix = self.receipt_source(original)
        with tempfile.TemporaryDirectory(prefix='p0c-synthetic-receipt-') as directory:
            root = Path(directory)
            (root / matrix['evidence_sources'][0]['receipt_file']).write_bytes(b'{"synthetic_snapshot":"changed"}')
            with mock.patch.object(VERIFIER, 'parse_json') as parse:
                with mock.patch.object(VERIFIER, 'replay_observations') as replay:
                    with self.assertRaisesRegex(ValueError, 'Private receipt hash mismatch'):
                        VERIFIER.replay_private(matrix, root)
        parse.assert_not_called()
        replay.assert_not_called()

    def test_load_json_reads_one_snapshot(self):
        path = mock.Mock()
        path.read_bytes.side_effect = [b'{"synthetic_snapshot":"first"}', b'{"synthetic_snapshot":"later"}']
        self.assertEqual(VERIFIER.load_json(path), {'synthetic_snapshot': 'first'})
        path.read_bytes.assert_called_once_with()

    def test_duplicate_json_keys_are_rejected_at_every_object_depth(self):
        for data in (b'{"synthetic":1,"synthetic":2}', b'{"outer":{"synthetic":1,"synthetic":2}}'):
            with self.subTest(data=data):
                with self.assertRaisesRegex(ValueError, 'Duplicate JSON object key'):
                    VERIFIER.parse_json(data)

    def test_nonfinite_json_constants_are_rejected(self):
        for constant in (b'NaN', b'Infinity', b'-Infinity'):
            with self.subTest(constant=constant):
                with self.assertRaisesRegex(ValueError, 'Nonfinite JSON number'):
                    VERIFIER.parse_json(b'{"synthetic":' + constant + b'}')

    def test_json_exponent_overflow_is_rejected(self):
        for exponent in (b'1e309', b'-1e309'):
            with self.subTest(exponent=exponent):
                with self.assertRaisesRegex(ValueError, 'Nonfinite JSON number'):
                    VERIFIER.parse_json(b'{"synthetic":' + exponent + b'}')

    def test_hash_matching_malformed_private_receipts_are_rejected_before_projection(self):
        cases = ((b'{"synthetic":1,"synthetic":2}', 'Duplicate JSON object key'),
                 (b'{"synthetic":NaN}', 'Nonfinite JSON number'),
                 (b'{"synthetic":1e309}', 'Nonfinite JSON number'))
        for data, reason in cases:
            with self.subTest(reason=reason):
                matrix = self.receipt_source(data)
                with tempfile.TemporaryDirectory(prefix='p0c-synthetic-receipt-') as directory:
                    root = Path(directory)
                    (root / matrix['evidence_sources'][0]['receipt_file']).write_bytes(data)
                    with mock.patch.object(VERIFIER, 'replay_observations') as replay:
                        with self.assertRaisesRegex(ValueError, reason):
                            VERIFIER.replay_private(matrix, root)
                    replay.assert_not_called()


if __name__ == '__main__':
    unittest.main()
