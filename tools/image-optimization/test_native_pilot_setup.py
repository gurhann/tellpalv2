#!/usr/bin/env python3
"""Synthetic setup/privacy regressions; no native/cloud qualification is exercised."""
import copy
from hashlib import sha256
import io
import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
import unittest
from unittest import mock

import native_pilot_preflight as preflight
import replay_p0c_preparation as replay


class PrivateTest(unittest.TestCase):
    def setUp(self):
        self.parent = Path(tempfile.mkdtemp(prefix='pilot-synthetic-', dir=Path(tempfile.gettempdir()).resolve()))
        self.parent.chmod(0o700)
        self.addCleanup(shutil.rmtree, self.parent)

    def private_file(self, name, data):
        path = self.parent / name
        preflight.write_private(path, data)
        return path


class ConfigurationMatrix(PrivateTest):
    def setUp(self):
        super().setUp()
        self.config = preflight.default_config()
        self.snapshot = {'synthetic-source': 'a' * 64}

    def reject(self, config):
        with self.assertRaises(preflight.SetupError):
            preflight.validate_config(config, self.snapshot)

    def test_offline_valid_selection_does_not_qualify_any_gate(self):
        preflight.validate_config(self.config, self.snapshot)
        report = preflight.build_report(self.config, self.snapshot)
        self.assertFalse(report['native_execution_ready'])
        self.assertEqual('blocked', report['readiness_status'])
        self.assertEqual(set(preflight.GATE_ACTIONS), {row['id'] for row in report['gates']})
        self.assertTrue(all(row['status'] == 'open' and row['verification_ref'] is None for row in report['gates']))
        self.assertTrue(all(row['value'] is None and row['reason'] for row in report['counters'].values()))
        self.assertEqual([], report['collection']['devices'])
        self.assertEqual('unverified', report['qa_account']['account_verification']['availability'])

    def test_valid_cloud_declarations_remain_unverified(self):
        self.config['cloud'].update(project_alias='selected-project', account_alias='selected-account',
                                    quota_minutes=30, spending_limit_usd=2.5)
        preflight.validate_config(self.config, self.snapshot)
        self.assertEqual('unverified', preflight.build_report(self.config, self.snapshot)['cloud_declarations']['availability'])

    def test_wrong_uid_provider_platform_kind_model_and_duplicate_alias_rejected(self):
        for field, value in (('provider', 'firebase_test_lab'), ('platform', 'ios'),
                             ('kind', 'emulator'), ('selected_model', 'Synthetic Physical Phone')):
            with self.subTest(field=field):
                config = copy.deepcopy(self.config)
                config['selection']['android'][field] = value
                self.reject(config)
        for field, value in (('qa_uid', 'PRIVATE-RAW-UID'), ('schema_version', True), ('scope', 'other')):
            with self.subTest(field=field):
                config = copy.deepcopy(self.config)
                config[field] = value
                self.reject(config)
        self.config['selection']['android']['alias'] = self.config['selection']['ios']['alias']
        self.reject(self.config)

    def test_secret_unrecognized_and_forged_observation_fields_rejected(self):
        for target, key in (('', 'token'), ('selection', 'connected'), ('cloud', 'project_id'),
                            ('collection_limits', 'auth_command')):
            with self.subTest(key=key):
                config = copy.deepcopy(self.config)
                (config if not target else config[target])[key] = 'PRIVATE-SECRET'
                self.reject(config)
        for key in ('observed_model', 'ram', 'counter_support', 'runnable'):
            with self.subTest(key=key):
                config = copy.deepcopy(self.config)
                config['selection']['ios'][key] = True
                self.reject(config)

    def test_boolean_nonfinite_negative_and_wrong_numeric_bounds_rejected(self):
        for key in ('quota_minutes', 'spending_limit_usd'):
            for value in (True, False, float('nan'), float('inf'), float('-inf'), -1, '30', 1000001, 10 ** 1000):
                with self.subTest(key=key, value=value):
                    config = copy.deepcopy(self.config)
                    config['cloud'][key] = value
                    self.reject(config)
        for key, values in (('timeout_seconds', (True, 0, 31, 1.5, float('inf'))),
                            ('max_output_bytes', (False, 1023, 1048577, 1024.0))):
            for value in values:
                with self.subTest(key=key, value=value):
                    config = copy.deepcopy(self.config)
                    config['collection_limits'][key] = value
                    self.reject(config)

    def test_exact_current_snapshot_accepted_and_changed_source_rejected(self):
        self.config['source_snapshot'] = self.snapshot.copy()
        preflight.validate_config(self.config, self.snapshot)
        for claims in ({}, {'synthetic-source': 'b' * 64}, {'synthetic-source': 'bad'},
                       {**self.snapshot, 'extra': 'a' * 64}):
            with self.subTest(claims=claims):
                self.config['source_snapshot'] = claims
                self.reject(self.config)

    def test_strict_json_rejections_do_not_expose_raw_values(self):
        for data in (b'{"PRIVATE-KEY":1,"PRIVATE-KEY":2}', b'{"secret":NaN}',
                     b'{"secret":Infinity}', b'{"secret":1e999}', b'{"PRIVATE-SECRET":'):
            with self.subTest(data=data):
                with self.assertRaises(preflight.SetupError) as error:
                    preflight.strict_json(data)
                self.assertNotIn('PRIVATE', str(error.exception))


class PrivatePathMatrix(PrivateTest):
    def test_unique_private_output_directories_and_files(self):
        first = preflight.create_private_output(self.parent)
        second = preflight.create_private_output(self.parent)
        self.assertNotEqual(first, second)
        self.assertEqual(0o700, stat.S_IMODE(first.stat().st_mode))
        preflight.write_private(first / 'receipt.json', {'value': None})
        self.assertEqual(0o600, stat.S_IMODE((first / 'receipt.json').stat().st_mode))
        with self.assertRaises(preflight.SetupError):
            preflight.write_private(first / 'receipt.json', {'value': 'overwrite'})

    def test_repository_nonprivate_and_symlink_outputs_rejected(self):
        public = self.parent / 'public'
        public.mkdir(mode=0o755)
        public.chmod(0o755) # The privacy fixture must not depend on a prior test's umask.
        link = self.parent / 'escape'
        link.symlink_to(self.parent, target_is_directory=True)
        for path in (preflight.ROOT, public, link, self.parent / 'missing'):
            with self.subTest(path=path):
                with self.assertRaises(preflight.SetupError):
                    preflight.create_private_output(path)

    def test_private_config_requires_regular_small_private_nonsymlink_file(self):
        config = self.private_file('config.json', b'{}')
        self.assertEqual(b'{}', preflight.private_file(config))
        config.chmod(0o644)
        with self.assertRaises(preflight.SetupError):
            preflight.private_file(config)
        config.chmod(0o600)
        link = self.parent / 'config-link'
        link.symlink_to(config)
        for path in (link, self.parent, preflight.ROOT / 'tools/mobile-test-account.json'):
            with self.subTest(path=path):
                with self.assertRaises(preflight.SetupError):
                    preflight.private_file(path)
        with self.assertRaises(preflight.SetupError):
            preflight.private_file(config, max_bytes=1)


class ProbeMatrix(PrivateTest):
    def probe(self, script, **kwargs):
        return preflight.run_bounded([sys.executable, '-B', '-c', script], self.parent, 'synthetic-probe', **kwargs)

    def test_success_raw_identifiers_and_paths_stay_private(self):
        result = self.probe("print('RAW-SERIAL PRIVATE-URL PRIVATE-TOKEN /private/raw-path')")
        self.assertEqual('ok', result['status'])
        self.assertIn('RAW-SERIAL', (self.parent / result['stdout_file']).read_text())
        for path in self.parent.iterdir():
            self.assertEqual(0o600, stat.S_IMODE(path.stat().st_mode))
        self.assertEqual(sha256((self.parent / result['stdout_file']).read_bytes()).hexdigest(), result['stdout_sha256'])

    def test_missing_executable_is_unknown_with_receipt(self):
        result = preflight.run_bounded(['/synthetic/absent/private-executable'], self.parent, 'missing-probe')
        self.assertEqual('missing_executable', result['status'])
        self.assertIsNone(result['exit_code'])
        self.assertIn('unknown', result['reason'])
        self.assertTrue((self.parent / 'missing-probe.json').is_file())

    def test_failed_device_service_does_not_become_zero_devices(self):
        result = self.probe("import sys; print('RAW-SERIAL access denied', file=sys.stderr); sys.exit(7)")
        self.assertEqual('failed', result['status'])
        self.assertEqual(7, result['exit_code'])
        self.assertNotIn('RAW-SERIAL', result['reason'])

    def test_timeout_kills_bounded_child_and_retains_receipt(self):
        result = self.probe('import time; time.sleep(5)', timeout_seconds=0.1)
        self.assertEqual('timeout', result['status'])
        self.assertIsNotNone(result['exit_code'])
        self.assertTrue((self.parent / 'synthetic-probe.json').is_file())

    def test_output_flood_is_capped_before_private_disk_write(self):
        result = self.probe("import os; os.write(1,b'x'*500000); os.write(2,b'y'*500000)", max_bytes=1024)
        self.assertEqual('output_limit', result['status'])
        self.assertLessEqual(result['stdout_bytes'] + result['stderr_bytes'], 1024)
        self.assertLessEqual((self.parent / result['stdout_file']).stat().st_size +
                             (self.parent / result['stderr_file']).stat().st_size, 1024)

    def test_exact_pipe_budget_is_successful(self):
        result = self.probe("import os; os.write(1,b'x'*1024)", max_bytes=1024)
        self.assertEqual('ok', result['status'])
        self.assertEqual(0, result['exit_code'])
        self.assertEqual(1024, result['stdout_bytes'])
        self.assertEqual(0, result['stderr_bytes'])
        self.assertEqual(b'x' * 1024, (self.parent / result['stdout_file']).read_bytes())

    def test_one_byte_pipe_budget_excess_is_rejected_and_retained_output_bounded(self):
        result = self.probe("import os; os.write(1,b'x'*1025)", max_bytes=1024)
        self.assertEqual('output_limit', result['status'])
        self.assertLessEqual(result['stdout_bytes'] + result['stderr_bytes'], 1024)
        self.assertLessEqual((self.parent / result['stdout_file']).stat().st_size +
                             (self.parent / result['stderr_file']).stat().st_size, 1024)

    def test_native_sidefile_is_capped_and_hashed_privately(self):
        native = self.parent / 'native.json'
        result = self.probe(f"from pathlib import Path; Path({str(native)!r}).write_bytes(b'x'*500000)",
                            bounded_file=native, max_bytes=1024)
        self.assertIn(result['status'], ('output_limit', 'failed'))
        self.assertLessEqual(native.stat().st_size, 512)
        self.assertLessEqual(native.stat().st_size + result['stdout_bytes'] + result['stderr_bytes'], 1024)
        self.assertEqual(0o600, stat.S_IMODE(native.stat().st_mode))
        self.assertEqual(sha256(native.read_bytes()).hexdigest(), result['native_sha256'])

    def test_exact_sidefile_and_pipe_budgets_are_successful(self):
        native = self.parent / 'native.json'
        result = self.probe(f"import os; from pathlib import Path; Path({str(native)!r}).write_bytes(b'n'*512); "
                            "os.write(1,b'x'*512)", bounded_file=native, max_bytes=1024)
        self.assertEqual('ok', result['status'])
        self.assertEqual(0, result['exit_code'])
        self.assertEqual(512, result['native_bytes'])
        self.assertEqual(512, result['stdout_bytes'])
        self.assertEqual(0, result['stderr_bytes'])
        self.assertEqual(b'n' * 512, native.read_bytes())
        self.assertEqual(b'x' * 512, (self.parent / result['stdout_file']).read_bytes())
        self.assertEqual(sha256(native.read_bytes()).hexdigest(), result['native_sha256'])

    def test_one_byte_sidefile_budget_excess_is_rejected_and_retained_output_bounded(self):
        native = self.parent / 'native.json'
        result = self.probe(f"from pathlib import Path; Path({str(native)!r}).write_bytes(b'n'*513)",
                            bounded_file=native, max_bytes=1024)
        self.assertIn(result['status'], ('output_limit', 'failed'))
        self.assertNotEqual(0, result['exit_code'])
        self.assertLessEqual(native.stat().st_size, 512)
        self.assertLessEqual(native.stat().st_size + (self.parent / result['stdout_file']).stat().st_size +
                             (self.parent / result['stderr_file']).stat().st_size, 1024)
        self.assertEqual(native.stat().st_size, result['native_bytes'])
        self.assertEqual(sha256(native.read_bytes()).hexdigest(), result['native_sha256'])

    def test_reused_receipts_never_overwrite_existing_private_evidence(self):
        self.private_file('synthetic-probe.stdout', b'previous')
        with self.assertRaises(FileExistsError):
            self.probe("print('replace')")
        self.assertEqual(b'previous', (self.parent / 'synthetic-probe.stdout').read_bytes())


class InventoryMatrix(PrivateTest):
    def test_empty_success_inventory_is_observed_only_for_that_receipt(self):
        self.assertEqual([], preflight.parse_adb_inventory(b'List of devices attached\n\n', 'synthetic'))

    def test_adb_transport_does_not_identify_physical_streaming_device(self):
        rows = preflight.parse_adb_inventory(b'List of devices attached\nRAW-SERIAL device product:private\n', 'synthetic')
        self.assertEqual('device', rows[0]['connection']['value'])
        self.assertIsNone(rows[0]['kind']['value'])
        self.assertIsNone(rows[0]['selected_device_match']['value'])
        self.assertNotIn('RAW-SERIAL', json.dumps(rows))

    def test_malformed_inventory_and_failed_native_outcome_rejected(self):
        for data in (b'error RAW-SERIAL', b'List of devices attached\nRAW-SERIAL invented\n', b'\xff'):
            with self.subTest(data=data):
                with self.assertRaises(preflight.SetupError):
                    preflight.parse_adb_inventory(data, 'synthetic')
        for data in (b'{}', b'{"info":{"outcome":"failed"},"result":{"devices":[]}}',
                     b'{"info":{"outcome":"success"},"result":{"devices":[null]}}'):
            with self.subTest(data=data):
                with self.assertRaises(preflight.SetupError):
                    preflight.parse_ios_inventory(data, 'synthetic')

    def test_ios_connection_stays_separate_from_selected_runnable_device(self):
        raw = {'info': {'outcome': 'success'}, 'result': {'devices': [
            {'identifier': 'RAW-DEVICE-ID', 'hardwareProperties': {'reality': 'physical', 'platform': 'iOS'},
             'connectionProperties': {'tunnelState': 'disconnected'}}]}}
        rows = preflight.parse_ios_inventory(json.dumps(raw).encode(), 'synthetic')
        self.assertEqual('physical', rows[0]['kind']['value'])
        self.assertEqual('disconnected', rows[0]['connection']['value'])
        self.assertIsNone(rows[0]['runnable']['value'])
        self.assertIsNone(rows[0]['model']['value'])
        self.assertNotIn('RAW-DEVICE-ID', json.dumps(rows))

    def test_mixed_legacy_physical_simulated_and_nonios_rows_preserve_nullable_fields(self):
        raw = {'info': {'outcome': 'success'}, 'result': {'devices': [
            {'hardwareProperties': {'platform': 'iOS'}, 'connectionProperties': {}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': 'physical', 'marketingName': 'iPhone Synthetic'},
             'connectionProperties': {'tunnelState': 'disconnected'},
             'deviceProperties': {'osVersionNumber': '99.1', 'osBuildUpdate': 'SYNTHETIC'}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': 'simulated'},
             'connectionProperties': {'tunnelState': 'connected'}},
            {'hardwareProperties': {'platform': 'tvOS', 'reality': 'physical'},
             'connectionProperties': {'tunnelState': 'connected'}}, {}]}}
        rows = preflight.parse_ios_inventory(json.dumps(raw).encode(), 'synthetic')
        self.assertEqual(5, len(rows))
        self.assertIsNone(rows[0]['kind']['value'])
        self.assertIsNone(rows[0]['connection']['value'])
        self.assertEqual('physical', rows[1]['kind']['value'])
        self.assertEqual('disconnected', rows[1]['connection']['value'])
        self.assertEqual('iPhone Synthetic', rows[1]['model']['value'])
        self.assertEqual('99.1', rows[1]['os_version']['value'])
        self.assertEqual('simulated', rows[2]['kind']['value'])
        self.assertIsNone(rows[3]['platform']['value'])
        self.assertIsNone(rows[4]['platform']['value'])
        self.assertTrue(all(row['selected_device_match']['value'] is None and row['runnable']['value'] is None for row in rows))

    def test_mixed_nonstring_ios_reality_and_connection_fields_remain_nullable_per_row(self):
        raw = {'info': {'outcome': 'success'}, 'result': {'devices': [
            {'identifier': 'RAW-VALID-ID',
             'hardwareProperties': {'platform': 'iOS', 'reality': 'physical'},
             'connectionProperties': {'tunnelState': 'connected'}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': ['physical']},
             'connectionProperties': {'tunnelState': 'disconnected'}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': {'private': 'RAW-REALITY'}},
             'connectionProperties': {'tunnelState': 'connected'}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': 'simulated'},
             'connectionProperties': {'tunnelState': ['connected']}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': 'physical'},
             'connectionProperties': {'tunnelState': {'private': 'RAW-TUNNEL'}}},
            {'hardwareProperties': {'platform': 'iOS', 'reality': 'physical'},
             'connectionProperties': {'tunnelState': 'connecting'}}]}}
        rows = preflight.parse_ios_inventory(json.dumps(raw).encode(), 'synthetic-ios')
        self.assertEqual(6, len(rows))
        self.assertEqual(['physical', None, None, 'simulated', 'physical', 'physical'],
                         [row['kind']['value'] for row in rows])
        self.assertEqual(['connected', 'disconnected', 'connected', None, None, 'connecting'],
                         [row['connection']['value'] for row in rows])
        self.assertTrue(all(row['platform']['value'] == 'ios' for row in rows))
        for index, field in ((1, 'kind'), (2, 'kind'), (3, 'connection'), (4, 'connection')):
            with self.subTest(index=index, field=field):
                self.assertEqual('unverified', rows[index][field]['availability'])
                self.assertTrue(rows[index][field]['reason'])
                self.assertEqual('synthetic-ios', rows[index][field]['evidence_ref'])
        self.assertTrue(all(row['selected_device_match']['value'] is None and row['runnable']['value'] is None for row in rows))
        self.assertNotIn('RAW-', json.dumps(rows))

    def test_collect_only_readonly_allowlisted_probes_and_failed_inventory_unknown(self):
        calls = []
        def synthetic(argv, directory, probe_id, **kwargs):
            calls.append(argv)
            result = {'status': 'failed', 'exit_code': 1, 'reason': 'Synthetic access failure.',
                      'stdout_file': probe_id + '.stdout', 'stderr_file': probe_id + '.stderr',
                      'stdout_sha256': sha256(b'').hexdigest()}
            preflight.write_private(directory / result['stdout_file'], b'')
            preflight.write_private(directory / result['stderr_file'], b'PRIVATE-RAW-SERIAL')
            return result
        with mock.patch.object(preflight, 'run_bounded', side_effect=synthetic):
            result = preflight.collect_local(self.parent, {'timeout_seconds': 1, 'max_output_bytes': 1024})
        self.assertEqual(8, len(calls))
        self.assertFalse(any(any(action in argv for action in ('login', 'create', 'connect', 'remove', 'boot', 'install'))
                             for argv in calls))
        self.assertTrue(all(row['result']['value'] is None for row in result['inventories']))
        self.assertEqual([], result['devices'])
        self.assertIsNone(result['streaming_client_probe']['value'])
        self.assertNotIn('PRIVATE-RAW-SERIAL', json.dumps(result))

    def test_legacy_or_malformed_tool_output_never_supplies_version(self):
        for data in (b'legacy launcher deprecated', b'RAW-TOKEN /private/path'):
            self.assertIsNone(preflight.tool_identity('android-version', data, 'synthetic')['value'])
        identity = preflight.tool_identity('adb-version', b'Android Debug Bridge version 1.0.41\nVersion 35.0.0\nInstalled as /private/tool', 'synthetic')
        self.assertEqual({'version': '1.0.41', 'build': '35.0.0'}, identity['value'])
        self.assertNotIn('/private/tool', json.dumps(identity))


class CliPrivacyMatrix(PrivateTest):
    def cli(self, script, args):
        return subprocess.run([sys.executable, '-B', str(script), *args], stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=10, check=False)

    def test_default_offline_report_is_valid_blocked_and_console_has_no_private_path(self):
        result = self.cli(Path(preflight.__file__), ['--output-parent', str(self.parent)])
        self.assertEqual(3, result.returncode, result.stderr.decode())
        self.assertNotIn(str(self.parent).encode(), result.stdout + result.stderr)
        reports = list(self.parent.glob('*/report.json'))
        self.assertEqual(1, len(reports))
        report = json.loads(reports[0].read_text())
        self.assertEqual('offline', report['collection_mode'])
        self.assertFalse(report['native_execution_ready'])
        self.assertEqual(0o600, stat.S_IMODE(reports[0].stat().st_mode))

    def test_invalid_private_config_error_never_echoes_private_values(self):
        config = preflight.default_config()
        config['qa_uid'] = 'PRIVATE-SECRET-RAW-UID'
        path = self.private_file('config.json', config)
        result = self.cli(Path(preflight.__file__), ['--config', str(path), '--output-parent', str(self.parent)])
        self.assertEqual(2, result.returncode)
        self.assertNotIn(b'PRIVATE-SECRET', result.stdout + result.stderr)
        self.assertNotIn(str(path).encode(), result.stdout + result.stderr)
        self.assertEqual([], list(self.parent.glob('*/report.json')))

    def test_integer_beyond_interpreter_digit_limit_is_fixed_safe_rejection_without_report(self):
        previous_limit = sys.get_int_max_str_digits()
        digit_limit = sys.int_info.str_digits_check_threshold
        data = json.dumps(preflight.default_config()).replace('"quota_minutes": null',
                '"quota_minutes": ' + '9' * (digit_limit + 1)).encode()
        self.assertLessEqual(len(data), preflight.MAX_CONFIG_BYTES)
        path = self.private_file('config.json', data)
        try:
            sys.set_int_max_str_digits(digit_limit)
            with (mock.patch.object(preflight, 'source_snapshot', return_value={'synthetic-source': 'a' * 64}),
                  mock.patch('sys.stdout', new_callable=io.StringIO) as stdout,
                  mock.patch('sys.stderr', new_callable=io.StringIO) as stderr):
                code = preflight.main(['--config', str(path), '--output-parent', str(self.parent)])
        finally:
            sys.set_int_max_str_digits(previous_limit)
        self.assertEqual(2, code)
        self.assertEqual('', stdout.getvalue())
        self.assertEqual('preflight rejected: Malformed JSON rejected.\n', stderr.getvalue())
        self.assertNotIn(str(path), stderr.getvalue())
        self.assertEqual([], list(self.parent.glob('*/report.json')))

    def test_successful_synthetic_local_collection_writes_devices_counts_evidence_and_open_gates(self):
        ios_payload = {'info': {'outcome': 'success'}, 'result': {'devices': [
            {'identifier': 'RAW-IOS-DEVICE',
             'hardwareProperties': {'platform': 'iOS', 'reality': 'physical', 'marketingName': 'iPhone Synthetic'},
             'connectionProperties': {'tunnelState': 'connected'},
             'deviceProperties': {'osVersionNumber': '99.1', 'osBuildUpdate': 'SYNTHETIC'}},
            {'identifier': 'RAW-IOS-SIMULATOR',
             'hardwareProperties': {'platform': 'iOS', 'reality': 'simulated'},
             'connectionProperties': {'tunnelState': 'disconnected'}}]}}
        outputs = {
            'adb-version': b'Android Debug Bridge version 1.0.41\nVersion 35.0.0\n',
            'xcode-version': b'Xcode 99.1\nBuild version SYNTHETIC\n',
            'xcrun-version': b'xcrun version 99.1\n',
            'xctrace-version': b'xctrace version 99.1\n',
            'android-version': b'Android 99.1\n',
            'android-streaming-help': b'create connect projects\n',
            'adb-inventory': b'List of devices attached\nRAW-ANDROID-DEVICE device product:private\n'
                             b'RAW-ANDROID-OFFLINE offline product:private\n',
            'ios-inventory': b'Synthetic devicectl inventory written.\n',
        }
        calls = []
        def synthetic(argv, directory, probe_id, **kwargs):
            calls.append((probe_id, argv))
            data = outputs[probe_id]
            result = {'status': 'ok', 'exit_code': 0, 'reason': None,
                      'stdout_file': probe_id + '.stdout', 'stderr_file': probe_id + '.stderr',
                      'stdout_bytes': len(data), 'stderr_bytes': 0,
                      'stdout_sha256': sha256(data).hexdigest(), 'stderr_sha256': sha256(b'').hexdigest()}
            preflight.write_private(directory / result['stdout_file'], data)
            preflight.write_private(directory / result['stderr_file'], b'')
            if probe_id == 'ios-inventory':
                native = kwargs['bounded_file']
                native_data = json.dumps(ios_payload).encode()
                preflight.write_private(native, native_data)
                result.update(native_file=native.name, native_bytes=len(native_data),
                              native_sha256=sha256(native_data).hexdigest())
            preflight.write_private(directory / (probe_id + '.json'), result)
            return result
        snapshot = {'synthetic-source': 'a' * 64}
        with (mock.patch.object(preflight, 'source_snapshot', return_value=snapshot),
              mock.patch.object(preflight, 'run_bounded', side_effect=synthetic),
              mock.patch('sys.stdout', new_callable=io.StringIO) as stdout,
              mock.patch('sys.stderr', new_callable=io.StringIO) as stderr):
            code = preflight.main(['--collect-local', '--output-parent', str(self.parent)])
        self.assertEqual(3, code, stderr.getvalue())
        self.assertEqual('', stderr.getvalue())
        self.assertNotIn(str(self.parent), stdout.getvalue())
        reports = list(self.parent.glob('*/report.json'))
        self.assertEqual(1, len(reports))
        report = json.loads(reports[0].read_text())
        self.assertEqual('local', report['collection_mode'])
        self.assertEqual(snapshot, report['source_snapshot'])
        self.assertFalse(report['native_execution_ready'])
        self.assertEqual('blocked', report['readiness_status'])
        self.assertEqual(set(preflight.GATE_ACTIONS), {row['id'] for row in report['gates']})
        self.assertTrue(all(row['status'] == 'open' and row['acceptance_ref'] is None and
                            row['implementation_ref'] is None and row['verification_ref'] is None
                            for row in report['gates']))
        collection = report['collection']
        self.assertEqual(set(outputs), {probe_id for probe_id, _ in calls})
        self.assertEqual(8, len(calls))
        self.assertIn(('adb-inventory', ['adb', 'devices', '-l']), calls)
        ios_call = next(argv for probe_id, argv in calls if probe_id == 'ios-inventory')
        self.assertEqual(['xcrun', 'devicectl', 'list', 'devices', '--json-output'], ios_call[:-1])
        self.assertEqual({'adb-inventory': 2, 'ios-inventory': 2},
                         {row['alias']: row['result']['value']['listed_count'] for row in collection['inventories']})
        for inventory in collection['inventories']:
            self.assertEqual('observed', inventory['result']['availability'])
            self.assertEqual(inventory['alias'], inventory['result']['evidence_ref'])
            self.assertEqual('ok', inventory['probe_status'])
        devices = collection['devices']
        self.assertEqual(4, len(devices))
        self.assertEqual(['adb-observation-001', 'adb-observation-002', 'ios-observation-001', 'ios-observation-002'],
                         [row['alias'] for row in devices])
        self.assertEqual(['android', 'android', 'ios', 'ios'], [row['platform']['value'] for row in devices])
        self.assertEqual(['device', 'offline', 'connected', 'disconnected'],
                         [row['connection']['value'] for row in devices])
        self.assertEqual([None, None, 'physical', 'simulated'], [row['kind']['value'] for row in devices])
        self.assertEqual('iPhone Synthetic', devices[2]['model']['value'])
        self.assertEqual('99.1', devices[2]['os_version']['value'])
        self.assertEqual('SYNTHETIC', devices[2]['os_build']['value'])
        for index, device in enumerate(devices):
            evidence_ref = 'adb-inventory' if index < 2 else 'ios-inventory'
            self.assertTrue(all(field['evidence_ref'] == evidence_ref for key, field in device.items() if key != 'alias'))
            self.assertIsNone(device['selected_device_match']['value'])
            self.assertIsNone(device['runnable']['value'])
            self.assertTrue((reports[0].parent / 'receipts' / (evidence_ref + '.json')).is_file())
        self.assertTrue(collection['raw_receipts_private'])
        self.assertEqual('observed', collection['streaming_client_probe']['availability'])
        self.assertIs(True, collection['streaming_client_probe']['value'])
        self.assertEqual('android-streaming-help', collection['streaming_client_probe']['evidence_ref'])
        self.assertTrue(all(row['value'] is None for row in report['counters'].values()))
        self.assertNotIn('RAW-', json.dumps(report))
        self.assertEqual(0o600, stat.S_IMODE(reports[0].stat().st_mode))

    def test_invalid_cli_arguments_are_sanitized_for_both_tools(self):
        for script in (Path(preflight.__file__), Path(replay.__file__)):
            with self.subTest(script=script):
                result = self.cli(script, ['--PRIVATE-SECRET-ARG=/private/raw-device'])
                self.assertEqual(2, result.returncode)
                self.assertNotIn(b'PRIVATE-SECRET', result.stdout + result.stderr)
                self.assertNotIn(b'/private/raw-device', result.stdout + result.stderr)

    def test_source_change_during_collection_rejects_current_report(self):
        with (mock.patch.object(preflight, 'source_snapshot', side_effect=[{'source': 'a' * 64}, {'source': 'b' * 64}]),
             mock.patch.object(preflight, 'collect_local', return_value={}),
             mock.patch('sys.stdout', new_callable=io.StringIO) as stdout,
             mock.patch('sys.stderr', new_callable=io.StringIO) as stderr):
            code = preflight.main(['--collect-local', '--output-parent', str(self.parent)])
        self.assertEqual(2, code)
        self.assertIn('explicit recheck', stderr.getvalue())
        self.assertEqual('', stdout.getvalue())
        self.assertEqual([], list(self.parent.glob('*/report.json')))


class HistoricalArchiveMatrix(PrivateTest):
    def archive(self, members):
        archive = self.parent / 'input.tar'
        with tarfile.open(archive, 'w') as tar:
            for name, kind, data in members:
                info = tarfile.TarInfo(name)
                if kind == 'file':
                    info.size = len(data)
                    tar.addfile(info, io.BytesIO(data))
                else:
                    info.type = {'symlink': tarfile.SYMTYPE, 'hardlink': tarfile.LNKTYPE,
                                 'fifo': tarfile.FIFOTYPE, 'directory': tarfile.DIRTYPE}[kind]
                    info.linkname = '../outside'
                    tar.addfile(info)
        archive.chmod(0o600)
        return archive

    def destination(self):
        path = self.parent / 'snapshot'
        path.mkdir(mode=0o700)
        return path

    def test_safe_regular_archive_private_extraction(self):
        archive = self.archive([('safe/file.txt', 'file', b'historical')])
        destination = self.destination()
        result = replay.safe_extract_archive(archive, destination)
        self.assertEqual(10, result['extracted_bytes'])
        self.assertEqual(b'historical', (destination / 'safe/file.txt').read_bytes())
        self.assertEqual(0o600, stat.S_IMODE((destination / 'safe/file.txt').stat().st_mode))

    def test_traversal_links_absolute_git_fifo_and_duplicates_rejected(self):
        rows = [('..' + '/escape', 'file'), ('/absolute', 'file'), ('C:/drive', 'file'),
                ('safe\\escape', 'file'), ('.git/config', 'file'), ('safe/link', 'symlink'),
                ('safe/link', 'hardlink'), ('safe/pipe', 'fifo')]
        for index, (name, kind) in enumerate(rows):
            with self.subTest(name=name, kind=kind):
                temporary = self.parent / f'row-{index}'
                temporary.mkdir(mode=0o700)
                archive = self.archive([(name, kind, b'x')])
                with self.assertRaises(preflight.SetupError):
                    replay.safe_extract_archive(archive, temporary)
        archive = self.archive([('duplicate', 'file', b'one'), ('duplicate', 'file', b'two')])
        with self.assertRaises(preflight.SetupError):
            replay.safe_extract_archive(archive, self.destination())

    def test_file_total_entry_and_reused_destination_limits_rejected(self):
        archive = self.archive([('one', 'file', b'ab'), ('two', 'file', b'cd')])
        for index, limits in enumerate(({'max_entries': 1}, {'max_total_bytes': 3}, {'max_file_bytes': 1})):
            with self.subTest(limits=limits):
                destination = self.parent / f'bounded-{index}'
                destination.mkdir(mode=0o700)
                with self.assertRaises(preflight.SetupError):
                    replay.safe_extract_archive(archive, destination, **limits)
        destination = self.destination()
        preflight.write_private(destination / 'existing', b'preserve')
        with self.assertRaises(preflight.SetupError):
            replay.safe_extract_archive(archive, destination)

    def test_original_snapshot_tamper_is_rejected(self):
        directory = self.parent / replay.ARTIFACT_DIRECTORY
        directory.mkdir(parents=True)
        for name in replay.ORIGINAL_HASHES:
            preflight.write_private(directory / name, b'tampered PRIVATE-RAW')
        with self.assertRaises(preflight.SetupError) as error:
            replay.check_original_files(self.parent)
        self.assertNotIn('PRIVATE', str(error.exception))

    def test_historical_result_cannot_claim_current_readiness(self):
        valid = {'preparation_checks': 'PASS', 'preparation_complete': True, 'native_execution_ready': False,
                 'source_hashes_checked': 43, 'physical_devices_qualified': 0,
                 'private_receipts_rehashed': 0}
        # Verify the exact archived verifier schema; changes below must never be accepted.
        valid.update(scope='image-optimization-p0c-1', open_native_gates=sorted(replay.GATES))
        replay.validate_verifier_result(valid)
        for key, value in (('native_execution_ready', True), ('source_hashes_checked', 42),
                           ('source_hashes_checked', True), ('physical_devices_qualified', 1),
                           ('preparation_checks', 'FAIL')):
            with self.subTest(key=key):
                result = valid.copy()
                result[key] = value
                with self.assertRaises(preflight.SetupError):
                    replay.validate_verifier_result(result)

    def test_historical_test_count_skips_and_failures_rejected(self):
        self.assertEqual(42, replay.validate_test_output(b'Ran 42 tests in 0.123s\n\nOK\n')['tests_run'])
        for data in (b'Ran 41 tests in 0.123s\n\nOK\n', b'Ran 42 tests in 0.123s\n\nOK (skipped=1)\n',
                     b'Ran 42 tests in 0.123s\n\nFAILED (errors=1)\n'):
            with self.subTest(data=data):
                with self.assertRaises(preflight.SetupError):
                    replay.validate_test_output(data)

    def test_missing_historical_commit_retains_failed_private_git_receipt(self):
        empty_repo = self.parent / 'not-a-repository'
        empty_repo.mkdir(mode=0o700)
        result = replay.archive_preparation(empty_repo, self.parent, replay.historical_environment(empty_repo))
        self.assertEqual('failed', result['status'])
        self.assertNotEqual(0, result['exit_code'])
        self.assertTrue((self.parent / result['stderr_file']).is_file())
        self.assertNotIn(str(empty_repo), json.dumps({key: result[key] for key in ('status', 'reason')}))


class HistoricalReceiptMatrix(PrivateTest):
    def receipts(self):
        directory = self.parent / 'original-receipts'
        directory.mkdir(mode=0o700)
        rows = []
        for index in range(13):
            name = f'synthetic-receipt-{index}.json'
            data = json.dumps({'synthetic_private_id': f'RAW-{index}'}).encode()
            preflight.write_private(directory / name, data)
            rows.append({'id': f'E-SYNTHETIC-{index}', 'receipt_file': name, 'receipt_sha256': sha256(data).hexdigest()})
        return {'evidence_sources': rows}, directory

    def test_exact_same_read_receipt_hashes_snapshot_privately(self):
        matrix, directory = self.receipts()
        snapshot = self.parent / 'receipt-snapshot'
        snapshot.mkdir(mode=0o700)
        result = replay.validate_private_receipts(matrix, directory, snapshot_dir=snapshot)
        self.assertEqual(13, len(result))
        for row in result:
            original, copied = directory / row['receipt_file'], snapshot / row['receipt_file']
            self.assertEqual(original.read_bytes(), copied.read_bytes())
            self.assertEqual(0o600, stat.S_IMODE(copied.stat().st_mode))
        self.assertNotIn('RAW-', json.dumps(result))

    def test_wrong_receipt_hash_missing_binding_and_duplicate_receipts_rejected(self):
        matrix, directory = self.receipts()
        for case in ('hash', 'count', 'duplicate'):
            with self.subTest(case=case):
                changed = copy.deepcopy(matrix)
                if case == 'hash':
                    changed['evidence_sources'][0]['receipt_sha256'] = 'a' * 64
                elif case == 'count':
                    changed['evidence_sources'].pop()
                else:
                    changed['evidence_sources'][1] = changed['evidence_sources'][0]
                with self.assertRaises(preflight.SetupError):
                    replay.validate_private_receipts(changed, directory)

    def test_nonprivate_and_symlink_raw_receipts_rejected(self):
        matrix, directory = self.receipts()
        first = directory / matrix['evidence_sources'][0]['receipt_file']
        first.chmod(0o644)
        with self.assertRaises(preflight.SetupError):
            replay.validate_private_receipts(matrix, directory)
        first.chmod(0o600)
        directory.chmod(0o755)
        with self.assertRaises(preflight.SetupError):
            replay.validate_private_receipts(matrix, directory)
        directory.chmod(0o700)
        private_copy = self.parent / 'private-copy.json'
        first.rename(private_copy)
        first.symlink_to(private_copy)
        with self.assertRaises(preflight.SetupError):
            replay.validate_private_receipts(matrix, directory)


if __name__ == '__main__':
    unittest.main()
