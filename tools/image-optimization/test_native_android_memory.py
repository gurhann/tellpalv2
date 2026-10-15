"""Required offline execution of the actual frozen production Kotlin memory sampler."""
import importlib.util
from pathlib import Path
import unittest


class AndroidMemoryNativeTests(unittest.TestCase):
    def test_actual_kotlin_sampler_pss_bytes_nulls_and_reader_failure(self):
        path = Path(__file__).parent / 'native-tests/run_android_memory_test.py'
        spec = importlib.util.spec_from_file_location('native_memory_test', path)
        runner = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(runner)
        result = runner.run()
        self.assertIn('5 cases; 5 OS reads; no skips', result)


if __name__ == '__main__':
    unittest.main()
