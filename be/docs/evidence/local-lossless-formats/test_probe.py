"""Yerel format kanıtının hatalı girdileri ve sınır ihlallerini reddettiğini sınar."""
import errno
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

from PIL import Image, PngImagePlugin


PROBE_PATH = Path(__file__).with_name('probe.py')
SPEC = importlib.util.spec_from_file_location('local_lossless_formats_probe', PROBE_PATH)
probe = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = probe
SPEC.loader.exec_module(probe)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def pin_for(path, pixels):
    data = path.read_bytes()
    return {'assetId': 9778, 'dimensions': [822, 1334],
            'newPngSha256': digest(data), 'newPngBytes': len(data),
            'rgbSha256': digest(pixels), 'rgbBytesCompared': len(pixels)}


def riff(chunks):
    body = b'WEBP' + b''.join(kind + struct.pack('<I', len(data)) + data +
                             (b'\0' if len(data) % 2 else b'') for kind, data in chunks)
    return b'RIFF' + struct.pack('<I', len(body)) + body


OPTIMIZED_CHILD = r'''
import json, runpy, sys
from contextlib import ExitStack
from unittest.mock import patch

main = runpy.run_path(sys.argv[1], run_name='optimization_guard_test')['main']
scope = main.__globals__
calls = []
def reject(label):
    def trapped(*args, **kwargs):
        calls.append(label)
        raise RuntimeError('UNEXPECTED_IO:' + label)
    return trapped

with ExitStack() as patches:
    patches.enter_context(patch.dict(scope, {'worker': reject('worker')}))
    patches.enter_context(patch.object(scope['signal'], 'signal', reject('signal:signal')))
    patches.enter_context(patch.object(scope['signal'], 'alarm', reject('signal:alarm')))
    patches.enter_context(patch.object(scope['socket'], 'socket', reject('network:socket')))
    for name in ('resolve', 'mkdir', 'read_bytes', 'read_text', 'write_bytes', 'write_text', 'open'):
        patches.enter_context(patch.object(scope['Path'], name, reject('filesystem:' + name)))
    patches.enter_context(patch.object(scope['subprocess'], 'run', reject('process:run')))
    patches.enter_context(patch.object(scope['Image'], 'open', reject('filesystem:Image.open')))
    patches.enter_context(patch('builtins.open', reject('filesystem:open')))
    sys.argv = [sys.argv[1], '--worker']
    try:
        main()
    except RuntimeError as failure:
        result = {'rejection': str(failure), 'calls': calls, 'optimize': sys.flags.optimize}
    else:
        raise RuntimeError('OPTIMIZED_MAIN_DID_NOT_REJECT')
print(json.dumps(result))
'''


class RasterValidationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory(prefix='lossless-formats-test-')
        cls.root = Path(cls.directory.name)
        cls.image = Image.new('RGB', (822, 1334), (19, 71, 163))
        cls.pixels = cls.image.tobytes()
        cls.png = cls.root / 'baseline.png'
        cls.image.save(cls.png, format='PNG')
        cls.pin = pin_for(cls.png, cls.pixels)
        buffer = io.BytesIO()
        cls.image.save(buffer, format='WEBP', lossless=True, quality=100, method=6, exact=True)
        cls.webp = buffer.getvalue()

    @classmethod
    def tearDownClass(cls):
        cls.image.close()
        cls.directory.cleanup()

    def test_png_full_rgb_bytes_match_the_pinned_raster(self):
        pixels, metadata = probe.validate_png(self.png, self.pin)
        self.assertEqual(pixels, self.pixels)
        self.assertEqual(len(pixels), 822 * 1334 * 3)
        self.assertIsInstance(metadata, dict)

    def test_png_wrong_file_hash_is_rejected(self):
        pin = {**self.pin, 'newPngSha256': '0' * 64}
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(self.png, pin)

    def test_png_wrong_rgb_hash_is_rejected_after_valid_payload_hash(self):
        pin = {**self.pin, 'rgbSha256': '0' * 64}
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(self.png, pin)

    def test_png_wrong_dimensions_are_rejected_with_matching_payload_hash(self):
        path = self.root / 'wrong-size.png'
        with Image.new('RGB', (821, 1334), (19, 71, 163)) as image:
            image.save(path, format='PNG')
            pin = pin_for(path, image.tobytes())
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin)

    def test_png_rgba_is_rejected_even_when_all_alpha_values_are_opaque(self):
        path = self.root / 'rgba.png'
        with self.image.convert('RGBA') as image:
            image.save(path, format='PNG')
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin_for(path, self.pixels))

    def test_png_metadata_is_rejected_with_matching_payload_hash(self):
        path = self.root / 'metadata.png'
        metadata = PngImagePlugin.PngInfo()
        metadata.add_text('Comment', 'Sentetik test verisi')
        self.image.save(path, format='PNG', pnginfo=metadata)
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin_for(path, self.pixels))

    def test_png_icc_profile_is_rejected(self):
        path = self.root / 'icc.png'
        self.image.save(path, format='PNG', icc_profile=b'synthetic-test-icc')
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin_for(path, self.pixels))

    def test_a_different_format_with_png_extension_is_rejected(self):
        path = self.root / 'jpeg-disguised-as-png.png'
        self.image.save(path, format='JPEG')
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin_for(path, self.pixels))

    def test_png_multiple_frames_are_rejected(self):
        path = self.root / 'animated.png'
        with Image.new('RGB', (822, 1334), (83, 17, 46)) as second:
            self.image.save(path, format='PNG', save_all=True, append_images=[second], duration=100)
        with self.assertRaises(probe.ProbeError):
            probe.validate_png(path, pin_for(path, self.pixels))

    def test_webp_lossless_payload_matches_every_rgb_byte(self):
        metadata = probe.validate_webp(self.webp, self.pixels, self.pin)
        self.assertIsInstance(metadata, dict)

    def test_webp_lossy_payload_cannot_be_labelled_lossless(self):
        output = io.BytesIO()
        self.image.save(output, format='WEBP', lossless=False, quality=70)
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(output.getvalue(), self.pixels, self.pin)

    def test_webp_changed_expected_rgb_is_rejected(self):
        changed = self.pixels[:-1] + bytes([self.pixels[-1] ^ 1])
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(self.webp, changed, self.pin)

    def test_webp_wrong_pinned_rgb_hash_is_rejected(self):
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(self.webp, self.pixels, {**self.pin, 'rgbSha256': '0' * 64})

    def test_webp_wrong_geometry_is_rejected(self):
        output = io.BytesIO()
        with Image.new('RGB', (821, 1334), (19, 71, 163)) as image:
            image.save(output, format='WEBP', lossless=True, quality=100, method=6, exact=True)
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(output.getvalue(), self.pixels, self.pin)

    def test_webp_truncated_payload_is_rejected(self):
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(self.webp[:-1], self.pixels, self.pin)

    def test_webp_trailing_bytes_are_rejected(self):
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(self.webp + b'UNEXPECTED', self.pixels, self.pin)

    def test_webp_extra_chunk_is_rejected_even_with_valid_riff_size(self):
        body = self.webp[8:] + b'JUNK' + struct.pack('<I', 2) + b'xy'
        extra = b'RIFF' + struct.pack('<I', len(body)) + body
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(extra, self.pixels, self.pin)

    def test_webp_metadata_container_is_rejected(self):
        # VP8X bileşiği bu dar deneyde metadata taşıyarak kayıpsız kabulünü genişletemez.
        payload = riff([(b'VP8X', b'\x08' + b'\0' * 9), (b'EXIF', b'test')])
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(payload, self.pixels, self.pin)

    def test_webp_corrupt_vp8l_is_rejected(self):
        payload = riff([(b'VP8L', b'not-a-valid-lossless-raster')])
        with self.assertRaises(probe.ProbeError):
            probe.validate_webp(payload, self.pixels, self.pin)


class PreservationTest(unittest.TestCase):
    def test_same_snapshot_is_accepted(self):
        probe.verify_preservation({'protected.txt': 'a' * 64}, {'protected.txt': 'a' * 64})

    def test_changed_hash_missing_file_and_added_file_are_rejected(self):
        before = {'protected.txt': 'a' * 64}
        for after in ({'protected.txt': 'b' * 64}, {}, {**before, 'extra.txt': 'c' * 64}):
            with self.subTest(after=after), self.assertRaises(probe.ProbeError):
                probe.verify_preservation(before, after)

    def test_real_temporary_file_change_is_visible_to_snapshot(self):
        with tempfile.TemporaryDirectory(prefix='format-preservation-test-') as directory:
            root = Path(directory)
            protected = root / 'protected'
            protected.mkdir()
            path = protected / 'note.txt'
            path.write_bytes(b'previous proof')
            before = probe.snapshot(root, [protected])
            path.write_bytes(b'changed proof')
            after = probe.snapshot(root, [protected])
            self.assertNotEqual(before, after)
            with self.assertRaises(probe.ProbeError):
                probe.verify_preservation(before, after)

    def test_existing_public_result_is_rejected_without_overwriting(self):
        for filename in ('verification.json', 'verification.md'):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                path = root / filename
                path.write_bytes(b'previous evidence')
                with self.assertRaises(probe.ProbeError):
                    probe.ensure_public_absent(root)
                self.assertEqual(path.read_bytes(), b'previous evidence')

    def test_absent_public_results_are_accepted_without_creating_them(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            probe.ensure_public_absent(root)
            self.assertEqual(list(root.iterdir()), [])

    def test_publish_collision_preserves_existing_json_and_removes_only_own_md_reservation(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            existing = root / 'verification.json'
            existing.write_bytes(b'previous evidence')
            with mock.patch.object(probe, 'markdown', side_effect=AssertionError('UNEXPECTED_RENDER')):
                with self.assertRaises(FileExistsError):
                    probe.publish(root, {})
            self.assertEqual(existing.read_bytes(), b'previous evidence')
            self.assertFalse((root / 'verification.md').exists())
            self.assertEqual(sorted(path.name for path in root.iterdir()), ['verification.json'])


class EncodeTimingTest(unittest.TestCase):
    def test_timer_excludes_plugin_rgb_and_buffer_preparation_and_payload_copy(self):
        events = []
        image = mock.Mock()
        output = mock.Mock()
        image.save.side_effect = lambda *args, **kwargs: events.append('save')
        output.getvalue.side_effect = lambda: (events.append('getvalue') or b'encoded-webp')
        ticks = iter((1_000_000, 4_000_000))

        def clock():
            events.append('clock')
            return next(ticks)

        with mock.patch.object(probe.Image, 'init', side_effect=lambda: events.append('plugin')), \
                mock.patch.dict(probe.Image.SAVE, {'WEBP': object()}), \
                mock.patch.object(probe.Image, 'frombytes',
                                  side_effect=lambda *args: (events.append('rgb') or image)), \
                mock.patch.object(probe.io, 'BytesIO',
                                  side_effect=lambda: (events.append('buffer') or output)), \
                mock.patch.object(probe.time, 'perf_counter_ns', side_effect=clock):
            payload, elapsed = probe.encode_webp(b'loaded-rgb')

        self.assertEqual(payload, b'encoded-webp')
        self.assertEqual(elapsed, 3.0)
        clocks = [index for index, event in enumerate(events) if event == 'clock']
        self.assertEqual(len(clocks), 2)
        for preparation in ('plugin', 'rgb', 'buffer'):
            self.assertLess(events.index(preparation), clocks[0])
        self.assertLess(clocks[0], events.index('save'))
        self.assertLess(events.index('save'), clocks[1])
        self.assertLess(clocks[1], events.index('getvalue'))
        image.save.assert_called_once_with(output, format='WEBP', **probe.ENCODER)
        image.close.assert_called_once()


class NetworkGuardTest(unittest.TestCase):
    def socket_with_bind(self, outcome):
        candidate = mock.MagicMock()
        candidate.__enter__.return_value = candidate
        if outcome is not None:
            candidate.bind.side_effect = outcome
        return candidate

    def test_only_permission_denied_by_sandbox_proves_network_guard(self):
        candidate = self.socket_with_bind(PermissionError(errno.EPERM, 'Operation not permitted'))
        with mock.patch.object(probe.socket, 'socket', return_value=candidate):
            probe.require_network_denied()
        candidate.bind.assert_called_once()

    def test_successful_loopback_bind_is_rejected(self):
        candidate = self.socket_with_bind(None)
        with mock.patch.object(probe.socket, 'socket', return_value=candidate):
            with self.assertRaises(probe.ProbeError):
                probe.require_network_denied()

    def test_unrelated_socket_failure_cannot_prove_network_guard(self):
        for number in (errno.EACCES, errno.EADDRINUSE):
            candidate = self.socket_with_bind(OSError(number, 'synthetic test'))
            with self.subTest(errno=number), mock.patch.object(probe.socket, 'socket', return_value=candidate):
                with self.assertRaises(probe.ProbeError):
                    probe.require_network_denied()


class OptimizationGuardTest(unittest.TestCase):
    def check_mode(self, flags, optimize):
        environment = dict(os.environ, PYTHONDONTWRITEBYTECODE='1')
        environment.pop('PYTHONOPTIMIZE', None)
        if optimize:
            environment['PYTHONOPTIMIZE'] = optimize
        completed = subprocess.run([sys.executable, *flags, '-c', OPTIMIZED_CHILD, str(PROBE_PATH)],
                                   env=environment, capture_output=True, text=True, timeout=20)
        self.assertEqual(completed.returncode, 0, completed.stderr)
        proof = json.loads(completed.stdout)
        self.assertEqual(proof['calls'], [])
        self.assertIn('PYTHON_OPTIMIZATION_NOT_SUPPORTED', proof['rejection'])
        self.assertEqual(proof['optimize'], 1)

    def test_dash_o_rejects_before_arguments_filesystem_and_processes(self):
        self.check_mode(['-O'], None)

    def test_pythonoptimize_rejects_before_arguments_filesystem_and_processes(self):
        self.check_mode([], '1')


if __name__ == '__main__':
    unittest.main(verbosity=2)
