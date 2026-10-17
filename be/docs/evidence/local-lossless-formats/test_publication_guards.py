"""Gerçek worker yolunda bozuk native çıktı ve kanıt değişikliğinin PASS'i engellediğini sınar."""
from contextlib import ExitStack
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock


PROBE_PATH = Path(__file__).with_name('probe.py')
SPEC = importlib.util.spec_from_file_location('local_lossless_publication_probe', PROBE_PATH)
probe = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = probe
SPEC.loader.exec_module(probe)


class WorkerPublicationGuardTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix='local-format-worker-test-')
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name).resolve()
        for relative in (probe.EVIDENCE, Path('be/docs/evidence/png-lossless-compression'),
                         probe.OLD_RUN, probe.PREFLIGHT, probe.PNG_RUN / 'new', probe.INITIAL_TIMING_RUN):
            (self.root / relative).mkdir(parents=True, exist_ok=True)
        memlog = self.root / probe.MEMLOG
        memlog.parent.mkdir(parents=True, exist_ok=True)
        memlog.write_bytes(b'synthetic previous memory')
        self.protected = self.root / 'be/docs/evidence/png-lossless-compression/synthetic-proof.txt'
        self.protected.write_bytes(b'synthetic previous proof')
        self.pixels = b'\x11\x22\x33\x44\x55\x66'
        self.payload = b'synthetic-webp-payload'
        self.png = self.root / probe.PNG_RUN / 'new/9778.png'
        self.png.write_bytes(b'synthetic PNG reference')
        prior_webp = self.root / probe.INITIAL_TIMING_RUN / '9778.webp'
        prior_webp.write_bytes(self.payload)
        prior = {'status': 'PASS', 'encoder': {'format': 'WEBP', **probe.ENCODER},
                 'images': [{'assetId': 9778, 'webp': {
                     'path': str(prior_webp.relative_to(self.root)), 'sha256': probe.digest(self.payload)}}]}
        (self.root / probe.INITIAL_TIMING_RUN / 'verification.json').write_text(json.dumps(prior))
        pin = {'newPngBytes': self.png.stat().st_size, 'existingJpegBytes': 123,
               'existingJpegSha256': 'a' * 64, 'existingJpegDimensions': [1712, 2778]}
        self.loaded = {9778: (pin, self.png, self.pixels, {'synthetic': True})}

    def run_worker(self, native_pixels, mutate_protected=False):
        def decode_samples(*args):
            if mutate_protected:
                self.protected.write_bytes(b'synthetic changed proof')
            return {'samples': []}

        with ExitStack() as patches:
            patches.enter_context(mock.patch.object(probe, '__file__', str(self.root / probe.EVIDENCE / 'probe.py')))
            patches.enter_context(mock.patch.object(probe, 'ASSET_IDS', (9778,)))
            patches.enter_context(mock.patch.object(probe, 'DIMENSIONS', (2, 1)))
            patches.enter_context(mock.patch.object(probe, 'RGB_BYTES', len(self.pixels)))
            patches.enter_context(mock.patch.object(probe, 'require_network_denied', return_value={'synthetic': True}))
            patches.enter_context(mock.patch.object(probe, 'native_network_proof', return_value={'synthetic': True}))
            patches.enter_context(mock.patch.object(probe, 'load_fixed_inputs', return_value=self.loaded))
            patches.enter_context(mock.patch.object(probe, 'encode_webp', return_value=(self.payload, 1.0)))
            patches.enter_context(mock.patch.object(probe, 'validate_webp', return_value={'synthetic': True}))
            patches.enter_context(mock.patch.object(probe, 'decode_samples', side_effect=decode_samples))
            native = patches.enter_context(mock.patch.object(
                probe, 'sips_decode', side_effect=[(native_pixels[0], {'synthetic': 'PNG'}),
                                                  (native_pixels[1], {'synthetic': 'WEBP'})]))
            with self.assertRaises(probe.ProbeError) as failure:
                probe.worker()
            self.assertEqual(native.call_count, 2)
        previous = self.root / probe.INITIAL_TIMING_RUN
        new_runs = [path for path in (self.root / 'be/var').glob('local-lossless-formats-*') if path != previous]
        self.assertEqual(len(new_runs), 1)
        private_run = new_runs[0]
        self.assertEqual(private_run.stat().st_mode & 0o777, 0o700)
        record = json.loads((private_run / 'failure.json').read_text())
        self.assertEqual(record['status'], 'FAIL')
        self.assertEqual(record['error'], str(failure.exception))
        self.assertFalse((private_run / 'verification.json').exists())
        for filename in ('verification.json', 'verification.md'):
            self.assertFalse((self.root / probe.EVIDENCE / filename).exists())
        return str(failure.exception), record, private_run

    def test_single_native_rgb_byte_mismatch_leaves_only_private_failure(self):
        changed = self.pixels[:-1] + bytes([self.pixels[-1] ^ 1])
        failure, record, _ = self.run_worker((self.pixels, changed))
        self.assertEqual(failure, 'SIPS_FULL_RGB_MISMATCH')
        self.assertTrue(record['protectedArtifactsUnchanged'])
        self.assertEqual(self.protected.read_bytes(), b'synthetic previous proof')

    def test_protected_file_change_during_measurement_cannot_publish_pass(self):
        failure, record, private_run = self.run_worker((self.pixels, self.pixels), mutate_protected=True)
        self.assertEqual(failure, 'PROTECTED_ARTIFACT_CHANGED')
        self.assertFalse(record['protectedArtifactsUnchanged'])
        self.assertTrue((private_run / '9778-measurement.json').exists())


class PublishCleanupGuardTest(unittest.TestCase):
    def test_cleanup_close_failure_preserves_primary_error_and_closes_other_stream(self):
        with tempfile.TemporaryDirectory(prefix='format-publish-cleanup-test-') as directory:
            root = Path(directory)
            retained = root / 'keep.txt'
            retained.write_bytes(b'previous unrelated evidence')
            original_open = Path.open
            closed, underlying = [], []

            class Stream:
                def __init__(self, path, raw):
                    self.path, self.raw = path, raw

                def write(self, data):
                    return self.raw.write(data)

                def close(self):
                    closed.append(self.path.name)
                    self.raw.close()
                    if self.path.name == 'verification.md':
                        raise OSError('synthetic cleanup close failure')

            def tracked_open(path, *args, **kwargs):
                raw = original_open(path, *args, **kwargs)
                underlying.append(raw)
                return Stream(path, raw)

            primary = RuntimeError('synthetic first rendering failure')
            with mock.patch.object(Path, 'open', new=tracked_open), \
                    mock.patch.object(probe, 'markdown', side_effect=primary):
                with self.assertRaises(RuntimeError) as failure:
                    probe.publish(root, {})
            self.assertIs(failure.exception, primary)
            self.assertEqual(closed, ['verification.md', 'verification.json'])
            self.assertTrue(all(stream.closed for stream in underlying))
            self.assertEqual(retained.read_bytes(), b'previous unrelated evidence')
            self.assertEqual(sorted(path.name for path in root.iterdir()), ['keep.txt'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
