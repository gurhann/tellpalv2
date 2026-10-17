"""Optimizasyonlu Python girişinin argüman ve I/O işlemlerinden önce reddini sınar."""
import json
import os
from pathlib import Path
import subprocess
import sys
import unittest


CHILD = r'''
import json, runpy, sys
from contextlib import ExitStack
from unittest.mock import patch

main = runpy.run_path(sys.argv[1], run_name='optimization_guard_test')['main']
scope = main.__globals__
events = []
def reject(label):
    def trapped(*args, **kwargs):
        events.append(label)
        raise RuntimeError('UNEXPECTED_IO:' + label)
    return trapped

with ExitStack() as patches:
    patches.enter_context(patch.object(scope['argparse'], 'ArgumentParser', reject('args')))
    for name in ('resolve', 'mkdir', 'read_bytes', 'read_text', 'write_bytes', 'write_text', 'stat', 'rglob', 'is_file', 'open'):
        patches.enter_context(patch.object(scope['Path'], name, reject('filesystem:' + name)))
    patches.enter_context(patch.object(scope['shutil'], 'copyfile', reject('filesystem:copyfile')))
    patches.enter_context(patch.object(scope['subprocess'], 'run', reject('process:run')))
    patches.enter_context(patch.object(scope['subprocess'], 'Popen', reject('process:Popen')))
    patches.enter_context(patch.object(scope['zipfile'], 'ZipFile', reject('filesystem:ZipFile')))
    patches.enter_context(patch.object(scope['Image'], 'open', reject('filesystem:Image.open')))
    patches.enter_context(patch('builtins.open', reject('filesystem:open')))
    try:
        main()
    except RuntimeError as failure:
        result = {'rejection': str(failure), 'calls': events, 'optimize': sys.flags.optimize}
    else:
        raise RuntimeError('OPTIMIZED_MAIN_DID_NOT_REJECT')
print(json.dumps(result))
'''


class OptimizationGuardTest(unittest.TestCase):
    def check_mode(self, flags, optimize):
        environment = dict(os.environ, PYTHONDONTWRITEBYTECODE='1')
        environment.pop('PYTHONOPTIMIZE', None)
        if optimize:
            environment['PYTHONOPTIMIZE'] = optimize
        probe = str(Path(__file__).with_name('probe.py'))
        entry = subprocess.run([sys.executable, *flags, probe], env=environment, capture_output=True, text=True)
        self.assertNotEqual(entry.returncode, 0)
        self.assertIn('PYTHON_OPTIMIZATION_NOT_SUPPORTED', entry.stderr)
        self.assertNotIn('usage:', entry.stderr)
        result = subprocess.run([sys.executable, *flags, '-c', CHILD, probe],
                                env=environment, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        proof = json.loads(result.stdout)
        self.assertEqual(proof['rejection'], 'PYTHON_OPTIMIZATION_NOT_SUPPORTED')
        self.assertEqual(proof['calls'], [])
        self.assertEqual(proof['optimize'], 1)
        print(json.dumps({'mode': flags or 'PYTHONOPTIMIZE=1', 'cliRejected': True, **proof}))

    def test_dash_o_rejects_before_arguments_filesystem_and_processes(self):
        self.check_mode(['-O'], None)

    def test_environment_optimization_rejects_before_arguments_filesystem_and_processes(self):
        self.check_mode([], '1')


if __name__ == '__main__':
    unittest.main(verbosity=2)
