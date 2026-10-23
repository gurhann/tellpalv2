"""Gerçek süreç adapter'ının başlangıç retlerini sentetik transport ile sınar."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import p4b5_manual_consumer_qa as qa


class StartupDiagnosticsTest(unittest.TestCase):
    def transport(self, selected, cmdline, *, failure=None, invalid_stat=False):
        calls = []

        def run(arguments, **kwargs):
            self.assertGreater(kwargs['timeout'], 0)
            self.assertLessEqual(kwargs['timeout'], 15)
            self.assertEqual(arguments[:3], ['/synthetic/adb', '-s', 'emulator-5554'])
            command = arguments[3:]
            calls.append(command)
            if command == ['shell', 'pidof', selected.package]:
                return subprocess.CompletedProcess(arguments, 0, b'123')
            if command == ['shell', 'ls', '-1', '/proc']:
                return subprocess.CompletedProcess(arguments, 0, b'self\nstat\n123\n')
            operation = 'cmdline' if command == ['shell', 'cat', '/proc/123/cmdline'] else 'stat'
            self.assertEqual(command, ['shell', 'cat', f'/proc/123/{operation}'])
            if failure and operation == failure[0]:
                if failure[1] == 'timeout':
                    raise subprocess.TimeoutExpired(arguments, kwargs['timeout'], b'PRIVATE_SECRET')
                if failure[1] == 'exception':
                    raise OSError('PRIVATE_SECRET /untrusted/path')
                return subprocess.CompletedProcess(arguments, 1, b'PRIVATE_SECRET')
            stat = b'PRIVATE_SECRET' if invalid_stat else (
                '123 (synthetic) ' + ' '.join('456' if i == 19 else '0' for i in range(20))
            ).encode()
            return subprocess.CompletedProcess(arguments, 0, cmdline if operation == 'cmdline' else stat)

        return run, calls

    def assert_safe(self, android):
        diagnostics = qa.safe_android_diagnostics(android)
        self.assertLessEqual(len(diagnostics), 8)
        self.assertTrue(all(set(value) == {'operation', 'code'} for value in diagnostics))
        serialized = json.dumps(diagnostics)
        for secret in ['PRIVATE_SECRET', '/proc', '/untrusted', android.package]:
            self.assertNotIn(secret, serialized)
        return diagnostics

    def test_exact_package_bytes_keep_existing_nul_and_no_nul_acceptance(self):
        for selected in [qa.P4B5_MANUAL, qa.P4B5_MANUAL_RETRY]:
            for suffix in [b'', b'\x00', b'\x00PRIVATE_SECRET\xff']:
                with self.subTest(scope=selected.measurement_id, suffix=suffix):
                    android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554', selected)
                    transport, calls = self.transport(selected, selected.package.encode() + suffix)
                    with patch.object(qa.subprocess, 'run', side_effect=transport):
                        self.assertEqual(android.process_identity(123), {'pid': 123, 'processStartTicks': '456'})
                    self.assertEqual(len(calls), 2)
                    self.assertEqual(self.assert_safe(android), [])

    def test_empty_malformed_and_foreign_cmdline_have_distinct_terminal_codes_without_signal(self):
        cases = [(b'', 'EMPTY_CMDLINE', 'QA_PROCESS_CMDLINE_EMPTY'),
                 (b'\x00PRIVATE_SECRET', 'EMPTY_CMDLINE', 'QA_PROCESS_CMDLINE_EMPTY'),
                 (b'\xffPRIVATE_SECRET', 'INVALID_CMDLINE', 'QA_PROCESS_CMDLINE_INVALID'),
                 (b'PRIVATE_SECRET\n', 'INVALID_CMDLINE', 'QA_PROCESS_CMDLINE_INVALID'),
                 (b'PRIVATE_SECRET', 'COMMAND_CHANGED', 'QA_PROCESS_COMMAND_CHANGED')]
        for selected in [qa.P4B5_MANUAL, qa.P4B5_MANUAL_RETRY]:
            cases_for_scope = cases + [(selected.package.encode() + ending, 'INVALID_CMDLINE', 'QA_PROCESS_CMDLINE_INVALID')
                                       for ending in [b' ', b'\n', b'\r', b'\xff']]
            cases_for_scope.append((selected.package.encode() + b':remote', 'COMMAND_CHANGED', 'QA_PROCESS_COMMAND_CHANGED'))
            for raw, code, refusal in cases_for_scope:
                with self.subTest(scope=selected.measurement_id, raw=raw), tempfile.TemporaryDirectory() as directory:
                    root = Path(directory).resolve()
                    (root / 'android').mkdir()
                    qa.prior.write_json(root / 'android/owned-process.private.json', {
                        **selected.identity(), 'pid': 123, 'processStartTicks': '456'})
                    android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554', selected)
                    transport, calls = self.transport(selected, raw)
                    with patch.object(qa.subprocess, 'run', side_effect=transport):
                        result = qa.ManualController(root, root, selected).cleanup(android)
                    self.assertEqual(result['qaProcess'], {'status': 'FAILED', 'code': refusal})
                    self.assertEqual(calls, [['shell', 'pidof', selected.package], ['shell', 'ls', '-1', '/proc'],
                                             ['shell', 'cat', '/proc/123/cmdline']])
                    expected = [{'operation': 'cmdline', 'code': code}]
                    self.assertEqual(self.assert_safe(android), expected)
                    self.assertEqual(qa.prior.private_json(root / 'cleanup.private.json')['androidDiagnostics'], expected)
                    self.assertEqual(qa.prior.private_json(root / 'terminal.private.json')['status'], 'FAILED_NO_RETRY')

    def test_cmdline_and_stat_read_failures_and_timeouts_are_not_retried(self):
        for operation in ['cmdline', 'stat']:
            for failure, code, refusal in [('returncode', 'READ_FAILED', 'QA_PROCESS_READ_FAILED'),
                                           ('exception', 'READ_FAILED', 'QA_PROCESS_READ_FAILED'),
                                           ('timeout', 'DEADLINE', 'ADB_DEADLINE')]:
                with self.subTest(operation=operation, failure=failure):
                    android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
                    transport, calls = self.transport(qa.SCOPE, qa.PACKAGE.encode(), failure=(operation, failure))
                    with patch.object(qa.subprocess, 'run', side_effect=transport):
                        with self.assertRaisesRegex(ValueError, '^' + refusal + '$'):
                            android.process_identity(123)
                    self.assertEqual(len(calls), 1 if operation == 'cmdline' else 2)
                    self.assertEqual(self.assert_safe(android), [{'operation': operation, 'code': code}])

    def test_invalid_stat_still_refuses_with_start_identity_diagnostic(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        transport, calls = self.transport(qa.SCOPE, qa.PACKAGE.encode(), invalid_stat=True)
        with patch.object(qa.subprocess, 'run', side_effect=transport):
            with self.assertRaisesRegex(ValueError, '^QA_PROCESS_START_REQUIRED$'):
                android.process_identity(123)
        self.assertEqual(len(calls), 2)
        self.assertEqual(self.assert_safe(android), [{'operation': 'stat', 'code': 'INVALID_OUTPUT'}])

    def test_real_refusals_preserve_eight_event_limit_and_filter_untrusted_diagnostics(self):
        android = qa.ManualAndroid(Path('/synthetic/adb'), 'emulator-5554')
        transport, calls = self.transport(qa.SCOPE, b'')
        with patch.object(qa.subprocess, 'run', side_effect=transport):
            for _ in range(12):
                with self.assertRaisesRegex(ValueError, '^QA_PROCESS_CMDLINE_EMPTY$'):
                    android.process_identity(123)
        self.assertEqual(len(calls), 12)
        self.assertEqual(self.assert_safe(android), [{'operation': 'cmdline', 'code': 'EMPTY_CMDLINE'}] * 8)
        android._diagnostics += [{'operation': 'PRIVATE_SECRET', 'code': 'EMPTY_CMDLINE'},
                                 {'operation': 'cmdline', 'code': 'PRIVATE_SECRET'},
                                 {'operation': 'cmdline', 'code': 'INVALID_CMDLINE', 'path': 'PRIVATE_SECRET'}]
        filtered = self.assert_safe(android)
        self.assertEqual(len(filtered), 6)
        self.assertEqual(filtered[-1], {'operation': 'cmdline', 'code': 'INVALID_CMDLINE'})


if __name__ == '__main__':
    unittest.main()
