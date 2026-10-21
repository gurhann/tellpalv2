"""Ağsız paket sınırları ve kaynak koruma retleri."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import p5d_offline_app as qa


class P5dTests(unittest.TestCase):
    def test_only_twelve_fixed_full_size_cases_are_pinned(self):
        pins = (qa.MOBILE / 'lib/features/reading_images/p5d_offline/domain/p5d_pins.dart').read_text()
        self.assertEqual(pins.count("'caseId':"), 12)
        self.assertEqual(pins.count("'width': 1712"), 12)
        self.assertEqual(pins.count("'height': 2778"), 12)
        self.assertEqual(qa.IDS, (1950, 1952, 8303, 8306))

    def test_wrong_report_hash_is_rejected_before_image_open(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'report.json'
            path.write_text('{}')
            with self.assertRaisesRegex(ValueError, 'report_sha_refused'):
                qa.fixture_cases(path)

    def test_closure_excludes_normal_main_and_native_plugins(self):
        closure = qa.source_closure()
        self.assertIn('lib/app/p5d_offline_qa.dart', closure)
        self.assertIn('lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart', closure)
        self.assertNotIn('lib/main.dart', closure)
        self.assertFalse(any('local_lossless_qa' in name for name in closure))

    def test_manifest_templates_have_no_permissions_or_providers(self):
        for path in qa.HOST.rglob('AndroidManifest.xml'):
            text = path.read_text()
            self.assertNotIn('uses-permission', text)
            import xml.etree.ElementTree as ET
            for node in ET.fromstring(text).iter('provider'):
                self.assertEqual(node.attrib.get('{http://schemas.android.com/tools}node'), 'remove')

    def test_existing_output_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'result.json'
            qa.write_new(path, {'value': 1})
            with self.assertRaises(FileExistsError):
                qa.write_new(path, {'value': 2})
            self.assertEqual(json.loads(path.read_text()), {'value': 1})

    def test_working_tree_change_rejects_preservation(self):
        with patch.object(qa, 'repo_hashes', return_value={'file': 'changed'}):
            with self.assertRaisesRegex(ValueError, 'working_tree_changed'):
                qa.check_preservation({'file': 'before'}, {}, {})

    def test_p5c_source_change_rejects_preservation(self):
        with patch.object(qa, 'repo_hashes', return_value={}), patch.object(qa, 'file_hash', return_value='changed'):
            with self.assertRaisesRegex(ValueError, 'p5c_source_changed'):
                qa.check_preservation({}, {'/private/tmp/source': 'before'}, {})

    def test_partial_samples_are_not_accepted(self):
        with self.assertRaisesRegex(ValueError, 'incomplete_samples'):
            qa.compare_readbacks(Path('/private/tmp'), {'samples': [], 'acceptedCases': 0, 'completedSamples': 0})

    def test_apk_internet_provider_and_plugin_are_rejected_before_install(self):
        with tempfile.TemporaryDirectory() as folder:
            folder = Path(folder)
            apk = folder / 'test.apk'
            with zipfile.ZipFile(apk, 'w') as out:
                out.writestr('classes.dex', b'com/google/firebase')
            class Result:
                def __init__(self, text): self.stdout = text
            package = qa.PACKAGE
            base = f'E: manifest\n A: package="{package}"\n'
            for suffix, reason in [('android.permission.INTERNET', 'apk_network_or_provider_refused'),
                                   ('E: provider', 'apk_network_or_provider_refused'),
                                   ('', 'apk_plugin_refused')]:
                with self.subTest(reason=reason, suffix=suffix):
                    output = folder / ('out' + str(len(list(folder.iterdir()))))
                    output.mkdir()
                    with patch.object(qa.subprocess, 'run', side_effect=[Result(base + suffix), Result(f"package: name='{package}'")]):
                        with self.assertRaisesRegex(ValueError, reason):
                            qa.audit_apk(apk, 'aapt', output, package)

    def test_device_selector_and_stop_scope_are_fixed(self):
        with patch.object(qa.subprocess, 'run') as run:
            run.return_value.returncode = 0
            qa.adb('shell', 'am', 'force-stop', qa.PACKAGE)
            self.assertEqual(run.call_args.args[0], ['adb', '-s', 'emulator-5554', 'shell', 'am', 'force-stop', 'com.tellpal.p5d.offline'])




class P5dBranchTests(unittest.TestCase):
    def _fixture(self, folder):
        sources = []
        for asset in qa.IDS:
            content = 18 if asset < 8000 else 62
            meta = {'format': 'PNG', 'metadata': {'icc': {'sha256': None}}}
            original = folder / f'source-{asset}.png'
            original.write_bytes(b'synthetic-original')
            source = {'assetId': asset, 'pilotContentId': content,
                      'source': meta, 'sourceFile': original.name,
                      'sourceSha256': qa.file_hash(original), 'sourceBytes': original.stat().st_size,
                      'candidates': {}}
            for arm in ('PNG', 'WEBP'):
                candidate = folder / f'{asset}.{arm.lower()}'
                candidate.write_bytes(b'synthetic-' + arm.encode())
                source['candidates'][arm] = dict(meta, format=arm, file=candidate.name,
                    sha256=qa.file_hash(candidate), bytes=candidate.stat().st_size,
                    allRgbBytesEqual=True, allIccBytesEqual=True)
            sources.append(source)
        report = folder / 'report.json'
        report.write_text(json.dumps({'status': 'LOCAL_LOSSLESS_COMPLETE', 'sources': sources}))
        return report

    def test_sha_dimensions_icc_and_byte_limit_each_reach_their_own_rejection(self):
        class Image:
            def __init__(self, path, dimensions=(1712, 2778), icc=None):
                self.format = 'WEBP' if str(path).endswith('.webp') else 'PNG'
                self.size = dimensions
                self.width, self.height = dimensions
                self.info = {'icc_profile': icc} if icc else {}
            def __enter__(self): return self
            def __exit__(self, *args): pass
        for branch in ('sha', 'dimensions', 'icc', 'limit'):
            with self.subTest(branch=branch), tempfile.TemporaryDirectory() as directory:
                folder = Path(directory)
                report = self._fixture(folder)
                if branch == 'sha':
                    target = folder / 'source-1950.png'
                    target.write_bytes(b'x' * target.stat().st_size)
                opener = lambda p: Image(p, dimensions=(1, 1) if branch == 'dimensions' else (1712, 2778),
                                         icc=b'different ICC' if branch == 'icc' else None)
                reason = {'sha': 'fixture_sha_refused', 'dimensions': 'format_dimensions_or_frames_refused',
                          'icc': 'icc_refused', 'limit': 'encoded_limit'}[branch]
                with patch.object(qa, 'REPORT_SHA', qa.file_hash(report)), patch('PIL.Image.open', side_effect=opener), \
                     patch.object(qa, 'MAX_ENCODED', 1 if branch == 'limit' else 32 * 1024 * 1024):
                    with self.assertRaisesRegex(ValueError, reason):
                        qa.fixture_cases(report)

    def test_different_native_buffers_report_deltas_without_quality_acceptance(self):
        import array
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            samples = []
            for index in range(12):
                readbacks = {}
                for kind in ('rawRgba', 'rawExtendedRgba128'):
                    # Farklı kaynak pikselleri: PNG-WebP farkı ölçülebilir olmalıdır.
                    values = array.array('B' if kind == 'rawRgba' else 'f',
                                         [index % 3, 2, 3, 255] if kind == 'rawRgba' else [index % 3 / 255, .2, .3, 1])
                    data = values.tobytes()
                    path = folder / f'{index}-{kind}.raw'
                    path.write_bytes(data)
                    readbacks[kind] = dict(status='MEASURED', file=path.name, bytes=len(data), sha256=qa.digest(data))
                for sample in range(4):
                    samples.append(dict(index=index, sample=sample, decodedWidth=1, decodedHeight=1,
                        viewport={'dpr': 3, 'fit': 'contain'}, **readbacks))
            runtime = dict(samples=samples, acceptedCases=12, completedSamples=48,
                           activeImages=0, activeBuffers=0, activeDescriptors=0, activeCodecs=0)
            result = qa.compare_readbacks(folder, runtime, _frame_shape=(1, 1))
            self.assertEqual(len(result), 8)
            for pair in result:
                self.assertGreater(pair['readbacks']['rawRgba']['changedChannels'], 0)
                self.assertGreater(pair['readbacks']['rawExtendedRgba128']['maxAbsDelta'], 0)
                self.assertEqual(pair['humanQualityAcceptance'], 'NOT_RUN')
                self.assertEqual(pair['physicalDisplayP3Acceptance'], 'NOT_RUN')
            original_order = runtime['samples']
            runtime['samples'] = original_order[4:8] + original_order[:4] + original_order[8:]
            with self.assertRaisesRegex(ValueError, 'sample_order_refused'):
                qa.compare_readbacks(folder, runtime, _frame_shape=(1, 1))
            runtime['samples'] = original_order
            for row in runtime['samples']:
                if row['index'] == 1:
                    row['viewport'] = {'dpr': 2, 'fit': 'contain'}
            with self.assertRaisesRegex(ValueError, 'format_geometry_changed'):
                qa.compare_readbacks(folder, runtime, _frame_shape=(1, 1))
            runtime['activeImages'] = 1
            with self.assertRaisesRegex(ValueError, 'native_resources_leaked'):
                qa.compare_readbacks(folder, runtime, _frame_shape=(1, 1))

    def test_p5d_annotations_cannot_enter_normal_di_or_router_scans(self):
        for path in (qa.MOBILE / 'lib/features/reading_images/p5d_offline').rglob('*.dart'):
            if path.name.endswith('.freezed.dart'):
                continue
            text = path.read_text()
            self.assertNotIn('@Injectable(', text)
            self.assertNotIn('@LazySingleton(', text)
            self.assertNotIn('@RoutePage(', text)



class P5dProtectionTests(unittest.TestCase):
    def test_only_known_live_log_append_preserves_original_prefix(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            active = root / 'emulator.stderr'
            other = root / 'old.png'
            active.write_bytes(b'original log')
            other.write_bytes(b'original image')
            with patch.object(qa, 'QA_ROOT', root), patch.object(qa, 'ACTIVE_EMULATOR_LOG', active), \
                 patch.object(qa, 'repo_hashes', return_value={}):
                inventory = qa.old_output_inventory()
                active.write_bytes(b'original log\nnew emulator line')
                self.assertTrue(qa.check_preservation({}, {}, inventory)['unchanged'])
                active.write_bytes(b'corrupted log\nnew emulator line')
                with self.assertRaisesRegex(ValueError, 'active_log_prefix_changed'):
                    qa.check_preservation({}, {}, inventory)
                active.write_bytes(b'original log\nnew emulator line')
                other.write_bytes(b'changed image')
                with self.assertRaisesRegex(ValueError, 'old_qa_output_changed'):
                    qa.check_preservation({}, {}, inventory)

    def test_missing_or_stale_native_capture_is_not_accepted(self):
        with self.assertRaisesRegex(ValueError, 'incomplete_presentations'):
            qa.validate_presentations(Path('/private/tmp'), {'captures': [], 'presentedCases': 0})


class P5dTimingTests(unittest.TestCase):
    def test_empty_and_stale_frame_are_never_reported_measured(self):
        self.assertEqual(qa.validate_frame_timings({'samples': [{'frames': [], 'frameTimingStatus': 'NO_TIMING_CALLBACK'}]})['measuredSamples'], 0)
        sample = {'frameTimingStatus': 'MEASURED', 'paintFrameNumber': 42, 'paintVsyncMicros': 100,
                  'epoch': 7, 'frames': []}
        with self.assertRaisesRegex(ValueError, 'missing_bound_frame'):
            qa.validate_frame_timings({'samples': [sample]})
        frame = dict(frameNumber=41, vsyncMicros=100, frameworkVsyncDeltaMicros=0, epoch=7, buildMicros=1, rasterMicros=2, totalSpanMicros=4)
        sample['frames'] = [frame]
        with self.assertRaisesRegex(ValueError, 'frame_epoch_or_timestamp_refused'):
            qa.validate_frame_timings({'samples': [sample]})
        frame['frameNumber'] = 42
        self.assertEqual(qa.validate_frame_timings({'samples': [sample]})['measuredSamples'], 1)
        frame['epoch'] = 6
        with self.assertRaisesRegex(ValueError, 'frame_epoch_or_timestamp_refused'):
            qa.validate_frame_timings({'samples': [sample]})


class P5dPresentationTests(unittest.TestCase):
    def setUp(self):
        crop_patch = patch.object(qa, 'TEXT_CROPS', {asset: (index + 1, index + 2) for index, asset in enumerate(qa.IDS)})
        crop_patch.start()
        self.addCleanup(crop_patch.stop)

    def _captures(self, folder):
        from PIL import Image
        captures, samples = [], []
        for index in range(12):
            capture = {'index': index}
            source = Image.new('RGBA', (32, 32))
            source.putdata([(x * 7 % 256, y * 11 % 256, index * 13 % 256, 255) for y in range(32) for x in range(32)])
            raw = source.tobytes()
            raw_path = folder / f'case-{index}.raw'
            raw_path.write_bytes(raw)
            samples.append(dict(index=index, sample=3, decodedWidth=32, decodedHeight=32,
                rawRgba=dict(status='MEASURED', file=raw_path.name, bytes=len(raw), sha256=qa.digest(raw))))
            for crop, suffix in ((False, 'slot'), (True, 'crop-1to1')):
                colors = [0, 0xffffff, 0x11223d if crop else 0x1122c5, 0xeeddc2 if crop else 0xeedd3a]
                image = Image.new('RGB', (32, 32), (20 if crop else 40, 50, 60))
                for y in range(8):
                    for x in range(8):
                        value = colors[y // 4 * 2 + x // 4]
                        image.putpixel((x, y), ((value >> 16) & 255, (value >> 8) & 255, value & 255))
                if crop:
                    x, y = qa.TEXT_CROPS[qa.IDS[index // 3]]
                    image.paste(source.crop((x, y, x + 16, y + 16)).convert('RGB'), (8, 8))
                image.save(folder / f'case-{index}-{suffix}.png')
                proof = dict(epoch=index + 1, index=index, crop=crop, nativePresentationVerified=True,
                             nativeMarkerLeft=0, nativeMarkerTop=0, markerCellPixels=4, markerColors=colors,
                             nativeCaptureSource='FlutterSurfaceViewPixelCopy', nativeSurfaceWidth=32, nativeSurfaceHeight=32,
                             markerX=0, markerY=0, nativeFlutterOffsetX=0, nativeFlutterOffsetY=0)
                if crop:
                    x, y = qa.TEXT_CROPS[qa.IDS[index // 3]]
                    proof['roi'] = dict(sourceX=x, sourceY=y, sourceWidth=16, sourceHeight=16,
                        destinationPhysicalWidth=16, destinationPhysicalHeight=16, physicalPixelsPerSourcePixel=1,
                        globalPhysicalLeft=8, globalPhysicalTop=8)
                capture['crop' if crop else 'slot'] = proof
            captures.append(capture)
        return dict(captures=captures, presentedCases=12, samples=samples)

    def test_native_marker_binding_and_fixed_text_roi_are_checked(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            runtime = self._captures(folder)
            self.assertEqual(len(qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))), 12)
            proof = runtime['captures'][0]['crop']
            for key, wrong in [('epoch', 0), ('index', 1), ('crop', False), ('nativePresentationVerified', False), ('nativeCaptureSource', 'stale')]:
                original = proof[key]
                proof[key] = wrong
                with self.assertRaisesRegex(ValueError, 'capture_binding_refused'):
                    qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
                proof[key] = original
            proof['nativeSurfaceWidth'] = 33
            with self.assertRaisesRegex(ValueError, 'surface_capture_dimensions_refused'):
                qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
            proof['nativeSurfaceWidth'] = 32
            proof['nativeFlutterOffsetX'] = 1
            with self.assertRaisesRegex(ValueError, 'surface_marker_offset_refused'):
                qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
            proof['nativeFlutterOffsetX'] = 0
            proof['roi']['sourceY'] += 1
            with self.assertRaisesRegex(ValueError, 'crop_1to1_refused'):
                qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
            proof['roi']['sourceY'] -= 1
            proof['markerColors'][2] ^= 1
            with self.assertRaisesRegex(ValueError, 'marker_pixels_refused'):
                qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))

    def test_actual_png_wrong_source_row_scale_or_content_is_rejected(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            runtime = self._captures(folder)
            path = folder / 'case-0-crop-1to1.png'
            original = path.read_bytes()
            raw = Image.frombytes('RGBA', (32, 32), (folder / 'case-0.raw').read_bytes())
            x, y = qa.TEXT_CROPS[qa.IDS[0]]
            for corruption in ('source_y_plus_one', 'scale', 'content'):
                with self.subTest(corruption=corruption), Image.open(path) as screenshot:
                    image = screenshot.copy()
                    region = raw.crop((x, y + 1, x + 16, y + 17)) if corruption == 'source_y_plus_one' else raw.crop((x, y, x + 16, y + 16))
                    if corruption == 'scale':
                        region = region.resize((8, 8), Image.Resampling.NEAREST).resize((16, 16), Image.Resampling.NEAREST)
                    if corruption == 'content':
                        region = Image.new('RGB', (16, 16), (255, 0, 0))
                    image.paste(region.convert('RGB'), (8, 8))
                    image.save(path)
                with self.assertRaisesRegex(ValueError, 'crop_pixels_refused'):
                    qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
                path.write_bytes(original)

    def test_missing_slot_or_crop_is_missing_presentation_even_with_all_measurements(self):
        for suffix in ('slot', 'crop-1to1'):
            with self.subTest(suffix=suffix), tempfile.TemporaryDirectory() as directory:
                folder = Path(directory)
                runtime = self._captures(folder)
                runtime['samples'] = [dict(index=i, sample=n) for i in range(12) for n in range(4)]
                (folder / f'case-0-{suffix}.png').unlink()
                with self.assertRaisesRegex(ValueError, 'capture_file_missing'):
                    qa.validate_presentations(folder, runtime, _frame_shape=(32, 32))
                result = dict(status='NOT_RUN', acceptedCases=0, runtime=runtime)
                with patch.object(qa, 'repo_hashes', return_value={}):
                    qa.finalize_run(folder, result, {}, {}, {}, [{'caseId': f'case-{i}'} for i in range(12)], False, 'a' * 32)
                self.assertEqual(result['missingMeasurementCases'], [])
                self.assertIn('case-0', result['missingPresentationCases'])
                self.assertIn('case-0', result['missingCases'])

    def test_reuse_copies_only_exact_native_hash_without_aliasing_old_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            old = root / 'p5d-offline-old'
            (old / 'device-output').mkdir(parents=True)
            (old / 'result.private.json').write_text('{}')
            source = old / 'device-output/case-0.raw'
            source.write_bytes(b'exact-native-buffer')
            run = root / 'p5d-offline-new'
            run.mkdir()
            target = run / source.name
            meta = dict(file=source.name, bytes=source.stat().st_size, sha256=qa.file_hash(source))
            with patch.object(qa, 'QA_ROOT', root):
                self.assertIsNone(qa.reuse_readback(run, target, dict(meta, sha256='different')))
                receipt = qa.reuse_readback(run, target, meta)
                self.assertEqual(receipt['source'], str(source))
                self.assertEqual(qa.file_hash(target), meta['sha256'])
                self.assertNotEqual(source.stat().st_ino, target.stat().st_ino)


class P5dEnvironmentTests(unittest.TestCase):
    def test_missing_exact_cached_artifact_stops_without_any_subprocess(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.gradle/caches/modules-2/files-2.1').mkdir(parents=True)
            flutter = root / 'flutter'
            (flutter / 'bin/internal').mkdir(parents=True)
            (flutter / 'bin/internal/engine.version').write_text('a' * 40)
            run = root / 'run'
            run.mkdir()
            with patch.object(qa.Path, 'home', return_value=root), patch.object(qa.subprocess, 'run') as command:
                with self.assertRaisesRegex(ValueError, 'exact_cached_artifact_missing:flutter_embedding_release-1.0.0-' + 'a' * 40):
                    qa.private_maven(run, flutter)
                command.assert_not_called()
            self.assertFalse((run / 'local-maven-evidence.private.json').exists())

    def test_host_no_space_rejects_before_snapshot_build_or_subprocess(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory(dir='/private/tmp') as directory:
            root = Path(directory)
            with patch.object(qa, 'QA_ROOT', root), patch.object(qa.shutil, 'disk_usage', return_value=SimpleNamespace(free=0)), \
                 patch.object(qa, 'fixture_cases') as fixtures, patch.object(qa.subprocess, 'run') as command:
                with self.assertRaisesRegex(ValueError, 'host_space_insufficient'):
                    qa.run_all(root / 'report.json', output_root=root)
                fixtures.assert_not_called()
                command.assert_not_called()
                self.assertEqual(list(root.iterdir()), [])

    def test_insufficient_device_space_stops_before_install_and_models_dataset_plus_margin(self):
        with patch.object(qa, 'adb', return_value=b'Filesystem 1K-blocks Used Available Use% Mounted on\n/dev/fuse 6000000 5999999 1 100% /storage/emulated\n') as command:
            with self.assertRaisesRegex(ValueError, 'device_space_insufficient'):
                qa.device_space(100 * 1024 * 1024)
            command.assert_called_once_with('shell', 'df', '-k', '/sdcard')
        with patch.object(qa, 'adb', return_value=b'Filesystem 1K-blocks Used Available Use% Mounted on\n/dev/fuse 6000000 1000000 5000000 20% /storage/emulated\n'):
            proof = qa.device_space(100 * 1024 * 1024)
            self.assertEqual(proof['rawDatasetBytes'], 1141424640)
            self.assertGreaterEqual(proof['requiredBytes'], 1536 * 1024 * 1024)
            self.assertGreaterEqual(proof['requiredBytes'], proof['rawDatasetBytes'] + proof['apkBytes'] + proof['marginBytes'])


class P5dFinalizationTests(unittest.TestCase):
    def test_cleanup_transport_error_still_seals_primary_error_and_preservation(self):
        with tempfile.TemporaryDirectory() as directory:
            run = Path(directory)
            result = dict(status='NOT_RUN', acceptedCases=0, primaryError={'message': 'primary'})
            with patch.object(qa, 'adb', side_effect=qa.AdbCommandError('transport closed')), patch.object(qa, 'repo_hashes', return_value={}):
                qa.finalize_run(run, result, {}, {}, {}, [], True, 'a' * 32)
            sealed = json.loads((run / 'result.private.json').read_bytes())
            self.assertEqual(sealed['primaryError'], {'message': 'primary'})
            self.assertTrue(sealed['cleanupErrors'])
            self.assertTrue(sealed['preservation']['unchanged'])
            self.assertTrue((run / 'source-after.private.json').is_file())


class P5dNoSpaceFinalizationTests(unittest.TestCase):
    def test_enospc_on_result_write_keeps_primary_and_preservation_in_bounded_stdout(self):
        import errno
        import io
        from contextlib import redirect_stdout
        for unchanged in (True, False):
            with self.subTest(unchanged=unchanged), tempfile.TemporaryDirectory() as directory:
                run = Path(directory)
                original_write = qa.write_new
                def write(path, value):
                    if path.name == 'result.private.json':
                        raise OSError(errno.ENOSPC, 'No space left on device')
                    return original_write(path, value)
                result = dict(status='NOT_RUN', acceptedCases=0,
                              primaryError={'type': 'NativeFailure', 'message': 'primary:' + 'x' * 100000})
                stdout = io.StringIO()
                before = {} if unchanged else {'protected': 'old'}
                with patch.object(qa, 'write_new', side_effect=write), patch.object(qa, 'repo_hashes', return_value={}), redirect_stdout(stdout):
                    qa.finalize_run(run, result, before, {}, {}, [], False, 'a' * 32)
                summary = json.loads(stdout.getvalue())
                self.assertLessEqual(len(stdout.getvalue().encode()), 8192)
                self.assertEqual(summary['status'], 'RECEIPT_WRITE_FAILED')
                self.assertEqual(summary['primaryError']['type'], 'NativeFailure')
                self.assertTrue(summary['primaryError']['message'].startswith('primary:'))
                self.assertEqual(summary['preservation']['unchanged'], unchanged)
                self.assertIn('No space', summary['receiptError'])
                self.assertTrue((run / 'source-after.private.json').is_file())


class P5dAdbTests(unittest.TestCase):
    def test_idempotent_read_retries_bounded_and_keeps_stderr(self):
        import subprocess
        failed = subprocess.CompletedProcess([], 1, stdout=b'', stderr=b'transport closed')
        with patch.object(qa.subprocess, 'run', return_value=failed) as command, patch.object(qa.time, 'sleep'):
            with self.assertRaisesRegex(qa.AdbCommandError, 'transport closed'):
                qa.adb('shell', 'df', '-k', '/sdcard')
            self.assertEqual(command.call_count, 3)
        with patch.object(qa.subprocess, 'run', return_value=failed) as command:
            with self.assertRaises(qa.AdbCommandError):
                qa.adb('install', 'own.apk')
            self.assertEqual(command.call_count, 1)


if __name__ == '__main__':
    unittest.main()
