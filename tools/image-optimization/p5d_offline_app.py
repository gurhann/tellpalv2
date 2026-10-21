#!/usr/bin/env python3
"""P5c dosyalarını değiştirmeden ayrı ve ağ izinsiz Android QA çalıştırır."""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import selectors
import shutil
import subprocess
import time
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[2]
MOBILE = ROOT / 'app/mobile'
HOST = Path(__file__).with_name('p5d_android_host')
QA_ROOT = Path('/Volumes/MAC_EK/tellpal-qa-private')
PACKAGE = 'com.tellpal.p5d.offline'
TEST_PACKAGE = PACKAGE + '.test'
DEVICE = 'emulator-5554'
REPORT_SHA = '960d9945c87598bea04144a493d118a16df8e29f7ce85745c87e8d08ceefa000'
IDS = (1950, 1952, 8303, 8306)
TEXT_CROPS = {1950: (400, 760), 1952: (440, 2160), 8303: (240, 1410), 8306: (240, 240)}
MAX_ENCODED = 32 * 1024 * 1024
MAX_PIXELS = 4756000
ACTIVE_EMULATOR_LOG = QA_ROOT / 'manual-offline-images-20261019-01/logs/emulator-open-20261020T062855Z.stderr'


def require(value, reason):
    if not value:
        raise ValueError(reason)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_hash(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def write_new(path, value):
    data = value if isinstance(value, bytes) else json.dumps(value, ensure_ascii=False, indent=2).encode()
    with Path(path).open('xb') as stream:
        stream.write(data)
    Path(path).chmod(0o600)


def fixture_cases(report_path):
    """Her giriş SHA, ICC, format ve tam boyutla yeniden doğrulanır."""
    from PIL import Image
    report_path = Path(report_path)
    require(report_path.is_file() and not report_path.is_symlink(), 'report_missing')
    require(file_hash(report_path) == REPORT_SHA, 'report_sha_refused')
    report = json.loads(report_path.read_bytes())
    require(report['status'] == 'LOCAL_LOSSLESS_COMPLETE', 'p5c_incomplete')
    cases, files = [], {}
    for asset_id in IDS:
        selected = [s for s in report['sources'] if s['assetId'] == asset_id]
        require(len(selected) == 1, 'source_mapping_refused')
        source = selected[0]
        require(source['pilotContentId'] == (18 if asset_id < 8000 else 62), 'content_mapping_refused')
        for arm in ('original', 'PNG', 'WEBP'):
            meta = source['source'] if arm == 'original' else source['candidates'][arm]
            filename = source['sourceFile'] if arm == 'original' else meta['file']
            require(Path(filename).name == filename, 'fixture_path_refused')
            path = report_path.parent / filename
            require(path.is_file() and not path.is_symlink(), 'fixture_missing')
            expected_sha = source['sourceSha256'] if arm == 'original' else meta['sha256']
            size = source['sourceBytes'] if arm == 'original' else meta['bytes']
            require(0 < size <= MAX_ENCODED and path.stat().st_size == size, 'encoded_limit')
            require(file_hash(path) == expected_sha, 'fixture_sha_refused')
            with Image.open(path) as image:
                require(image.format == meta['format'] and image.size == (1712, 2778) and
                        image.width * image.height <= MAX_PIXELS and getattr(image, 'n_frames', 1) == 1,
                        'format_dimensions_or_frames_refused')
                icc = image.info.get('icc_profile')
                icc_sha = digest(icc) if icc else None
                require(icc_sha == meta['metadata']['icc']['sha256'], 'icc_refused')
            if arm != 'original':
                require(meta['allRgbBytesEqual'] and meta['allIccBytesEqual'] and
                        icc_sha == source['source']['metadata']['icc']['sha256'], 'p5c_equality_refused')
            row = dict(caseId=f'{asset_id}-{arm}', assetId=asset_id, contentId=source['pilotContentId'],
                       role='cover' if asset_id in (1950, 8303) else 'page', arm=arm,
                       format=meta['format'], asset='assets/p5d/' + filename,
                       bytes=size, sha256=expected_sha, iccSha256=icc_sha, width=1712, height=2778)
            cases.append(row)
            files[str(path)] = expected_sha
    return cases, files


def source_closure():
    """Normal main/auth kompozisyonu kopyalanmaz; yalnız import/part kapanışı alınır."""
    pending = [MOBILE / 'lib/app/p5d_offline_qa.dart']
    files = {}
    allowed = {'flutter', 'flutter_bloc', 'auto_route', 'get_it', 'injectable',
               'freezed_annotation', 'crypto', 'dartz', 'equatable', 'json_annotation'}
    while pending:
        path = pending.pop().resolve(strict=True)
        require(path.is_relative_to(MOBILE / 'lib') and not path.is_symlink(), 'source_scope_refused')
        name = str(path.relative_to(MOBILE))
        if name in files:
            continue
        data = path.read_bytes()
        files[name] = digest(data)
        for target in re.findall(r"(?:import|export|part)\s+['\"]([^'\"]+)['\"]", data.decode()):
            if target.startswith('dart:'):
                continue
            if target.startswith('package:tellpal_mobile_poc/'):
                pending.append(MOBILE / 'lib' / target.split('/', 1)[1])
            elif target.startswith('package:'):
                require(target.split(':', 1)[1].split('/')[0] in allowed, 'native_or_network_dependency_refused')
            else:
                pending.append(path.parent / target)
    return files


def repo_hashes():
    result = subprocess.run(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'],
                            cwd=ROOT, check=True, capture_output=True).stdout
    return {name: file_hash(ROOT / name) for name in result.decode().split('\0')
            if name and (ROOT / name).is_file() and not (ROOT / name).is_symlink()}


def old_output_inventory():
    inventory = {}
    for path in QA_ROOT.rglob('*'):
        if not path.is_file() or path.is_symlink():
            continue
        info = path.stat()
        row = {'bytes': info.st_size, 'mtimeNs': info.st_mtime_ns}
        if path == ACTIVE_EMULATOR_LOG:
            with path.open('rb') as stream:
                row['prefixSha256'] = digest(stream.read(info.st_size))
        inventory[str(path)] = row
    return inventory


def check_preservation(before, inputs, old):
    require(repo_hashes() == before, 'working_tree_changed')
    require(all(file_hash(Path(name)) == value for name, value in inputs.items()), 'p5c_source_changed')
    appended = []
    for name, expected in old.items():
        path = Path(name)
        require(path.is_file(), 'old_qa_output_missing')
        if path == ACTIVE_EMULATOR_LOG and 'prefixSha256' in expected:
            with path.open('rb') as stream:
                require(digest(stream.read(expected['bytes'])) == expected['prefixSha256'], 'active_log_prefix_changed')
            require(path.stat().st_size >= expected['bytes'], 'active_log_truncated')
            appended.append({'file': str(path), 'beforeBytes': expected['bytes'],
                             'appendedBytes': path.stat().st_size - expected['bytes'],
                             'originalPrefixSha256': expected['prefixSha256']})
        else:
            require(path.stat().st_size == expected['bytes'] and path.stat().st_mtime_ns == expected['mtimeNs'],
                    'old_qa_output_changed')
    canonical = lambda value: digest(json.dumps(value, sort_keys=True).encode())
    return {'repositoryFiles': len(before), 'p5cFiles': len(inputs), 'oldQaFiles': len(old), 'unchanged': True,
            'sourceHashesBeforeSha256': canonical(before), 'sourceHashesAfterSha256': canonical(repo_hashes()),
            'p5cHashesBeforeSha256': canonical(inputs),
            'p5cHashesAfterSha256': canonical({name: file_hash(Path(name)) for name in inputs}),
            'activeEmulatorLogAppends': appended,
            'oldQaCheck': 'size_mtime_except_hash_protected_active_log_prefix'}


def checked(argv, cwd, output, label, timeout=600, java_home=None):
    env = dict(os.environ, PUB_ENVIRONMENT='p5d-offline', FLUTTER_SUPPRESS_ANALYTICS='true',
               DART_SUPPRESS_ANALYTICS='true')
    if java_home is not None:
        env['JAVA_HOME'] = str(java_home)
    with (output / (label + '.log')).open('xb') as log:
        result = subprocess.run([str(v) for v in argv], cwd=cwd, env=env, stdout=log,
                                stderr=subprocess.STDOUT, timeout=timeout)
    require(result.returncode == 0, label + '_failed_no_download_attempted')


def prepare(run, cases, sources, flutter, sdk):
    mobile = run / 'host'
    mobile.mkdir(mode=0o700)
    for name, sha in sources.items():
        data = (MOBILE / name).read_bytes()
        require(digest(data) == sha, 'source_changed_during_snapshot')
        target = mobile / name
        target.parent.mkdir(parents=True, exist_ok=True)
        write_new(target, data)
    shutil.copytree(HOST, mobile / 'android')
    (mobile / 'assets/p5d').mkdir(parents=True)
    write_new(mobile / 'assets/p5d/manifest.json',
              {'schemaVersion': 1, 'reportSha256': REPORT_SHA, 'cases': cases})
    # Bağımlılıklar yalnız mevcut uygulamanın sabit sürümleridir; native plugin yoktur.
    pubspec = '''name: tellpal_mobile_poc
publish_to: none
environment:
  sdk: ^3.11.0
dependencies:
  flutter:
    sdk: flutter
  flutter_bloc: 9.1.1
  auto_route: 10.3.0
  get_it: 8.3.0
  injectable: 2.7.1+4
  freezed_annotation: 3.1.0
  crypto: 3.0.7
  dartz: 0.10.1
  equatable: 2.1.0
  json_annotation: 4.11.0
flutter:
  assets:
    - assets/p5d/
'''
    write_new(mobile / 'pubspec.yaml', pubspec.encode())
    write_new(mobile / 'android/local.properties', f'flutter.sdk={flutter}\nsdk.dir={sdk}\n'.encode())
    shutil.copyfile(MOBILE / 'pubspec.lock', mobile / 'pubspec.lock')
    return mobile



def private_maven(run, flutter):
    """Mevcut aynı koordinatları kopyalar; Gradle repository kimliği cache farkını aşar."""
    import xml.etree.ElementTree as ET
    engine = (flutter / 'bin/internal/engine.version').read_text().strip()
    require(re.fullmatch(r'[a-f0-9]{40}', engine), 'engine_revision_refused')
    version = '1.0.0-' + engine
    coordinates = [('io.flutter', 'flutter_embedding_release', version),
                   ('io.flutter', 'arm64_v8a_release', version),
                   ('com.google.guava', 'listenablefuture', '1.0'),
                   ('androidx.core', 'core-ktx', '1.2.0')]
    cache = (Path.home() / '.gradle/caches/modules-2/files-2.1').resolve(strict=True)
    evidence = []
    queue = [(group, artifact, version, True) for group, artifact, version in coordinates]
    seen = set()
    while queue:
        group, artifact, selected_version, needs_jar = queue.pop(0)
        if (group, artifact, selected_version) in seen:
            continue
        seen.add((group, artifact, selected_version))
        origin = cache / group / artifact / selected_version
        target = run / 'offline-maven' / group.replace('.', '/') / artifact / selected_version
        target.mkdir(parents=True)
        archive_type = 'aar' if (group, artifact) == ('androidx.core', 'core-ktx') else 'jar'
        for extension in (('pom', archive_type) if needs_jar else ('pom',)):
            name = f'{artifact}-{selected_version}.{extension}'
            matches = list(origin.glob('*/' + name))
            require(len(matches) == 1 and matches[0].is_file(), 'exact_cached_artifact_missing:' + name)
            source = matches[0]
            data = source.read_bytes()
            require(hashlib.sha1(data).hexdigest() == source.parent.name, 'cache_artifact_hash_refused')
            if extension == 'pom':
                pom = ET.fromstring(data)
                namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
                actual_group = pom.findtext('m:groupId', namespaces=namespace) or pom.findtext('m:parent/m:groupId', namespaces=namespace)
                actual_version = pom.findtext('m:version', namespaces=namespace) or pom.findtext('m:parent/m:version', namespaces=namespace)
                require(actual_group == group and pom.findtext('m:artifactId', namespaces=namespace) == artifact and
                        actual_version == selected_version, 'cache_coordinate_refused')
                parent = pom.find('m:parent', namespace)
                if parent is not None:
                    queue.append(tuple(parent.findtext('m:' + key, namespaces=namespace)
                                       for key in ('groupId', 'artifactId', 'version')) + (False,))
            write_new(target / name, data)
            evidence.append({'coordinate': f'{group}:{artifact}:{selected_version}',
                             'file': name, 'sha256': digest(data), 'source': str(source)})
    write_new(run / 'local-maven-evidence.private.json', evidence)
    return evidence


def audit_apk(apk, aapt, output, package, test=False):
    require(apk.is_file(), 'apk_missing')
    manifest = subprocess.run([str(aapt), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'],
                              check=True, capture_output=True, text=True).stdout
    badging = subprocess.run([str(aapt), 'dump', 'badging', str(apk)],
                             check=True, capture_output=True, text=True).stdout
    write_new(output / (package + '-manifest.txt'), manifest.encode())
    require(f"package: name='{package}'" in badging and package in manifest, 'apk_package_refused')
    require('android.permission.INTERNET' not in manifest and not re.search(r'\bE: provider\b', manifest),
            'apk_network_or_provider_refused')
    if test:
        require('com.tellpal.p5d.P5dInstrumentation' in manifest and PACKAGE in manifest, 'instrumentation_refused')
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if re.fullmatch(r'classes\d*\.dex', name):
                dex = archive.read(name)
                require(b'com/google/firebase' not in dex and b'com/google/android/gms' not in dex and
                        b'firebase_core' not in dex and b'just_audio' not in dex,
                        'apk_plugin_refused')
    return {'sha256': file_hash(apk), 'package': package, 'internet': False, 'providers': 0,
            'nativePlugins': 0, 'inspection': 'aapt_manifest_and_dex'}


class AdbCommandError(RuntimeError):
    pass


def adb(*args, timeout=30, **kwargs):
    retryable = (args[0] in ('pull', 'exec-out') or
        (args[0] == 'shell' and (args[1] in ('df', 'getprop', 'sha256sum', 'stat', 'find', 'ls') or
         args[1:3] in (('pm', 'list'), ('pm', 'path')) or args[1:] == ('am', 'force-stop', PACKAGE))))
    attempts = 3 if retryable else 1
    for attempt in range(attempts):
        result = subprocess.run(['adb', '-s', DEVICE, *args], check=False, timeout=timeout,
                                capture_output=True, **kwargs)
        if result.returncode == 0:
            return result.stdout
        if attempt + 1 < attempts:
            time.sleep(0.25)
    raise AdbCommandError('adb_returncode=' + str(result.returncode) + '; command=' + repr(args) +
                          '; stderr=' + result.stderr.decode(errors='replace')[:2000] +
                          '; stdout=' + result.stdout.decode(errors='replace')[:2000])


def instrument(run, nonce):
    argv = ['adb', '-s', DEVICE, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'p5dRunNonce', nonce,
            TEST_PACKAGE + '/com.tellpal.p5d.P5dInstrumentation']
    process = subprocess.Popen(argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=0)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    deadline, buffer, transcript = time.monotonic() + 30, b'', b''
    last_case = None
    progress_log = (run / 'instrumentation-progress.private.log').open('xb')
    try:
        while process.poll() is None:
            if time.monotonic() > deadline:
                adb('shell', 'am', 'force-stop', PACKAGE)
                raise ValueError('case_watchdog_30_seconds')
            for key, _ in selector.select(0.25):
                data = os.read(key.fileobj.fileno(), 65536)
                if not data:
                    continue
                transcript += data
                progress_log.write(data)
                progress_log.flush()
                buffer += data
                while b'\n' in buffer:
                    line, buffer = buffer.split(b'\n', 1)
                    match = re.search(rb'p5dProgress=case_(\d+)_sample_\d+', line)
                    if match and match.group(1) != last_case:
                        last_case = match.group(1)
                        deadline = time.monotonic() + 30
        transcript += process.stdout.read()
        require(process.returncode == 0 and b'INSTRUMENTATION_RESULT: p5dResult=PASS' in transcript,
                'instrumentation_failed:returncode=' + str(process.returncode))
    finally:
        if process.poll() is None:
            process.kill()
        selector.close()
        progress_log.close()
        write_new(run / 'instrumentation.log', transcript)
        try:
            adb('shell', 'am', 'force-stop', PACKAGE)
        except Exception as cleanup_error:
            write_new(run / 'instrumentation-cleanup-error.private.json', {'reason': str(cleanup_error)})


def compare_readbacks(folder, runtime, *, _frame_shape=(1712, 2778)):
    """Native Flutter çıktısını karşılaştırır; Pillow RGB çıktısını referans almaz."""
    import array
    width, height = _frame_shape
    samples = runtime['samples']
    require(len(samples) == 48 and runtime['acceptedCases'] == 12 and runtime['completedSamples'] == 48,
            'incomplete_samples')
    require(all(runtime[name] == 0 for name in ('activeImages', 'activeBuffers', 'activeDescriptors', 'activeCodecs')),
            'native_resources_leaked')
    require([(s.get('index'), s.get('sample')) for s in samples] ==
            [(index, sample) for index in range(12) for sample in range(4)], 'sample_order_refused')
    results = []
    for index in range(12):
        rows = [s for s in samples if s['index'] == index]
        require([r['sample'] for r in rows] == [0, 1, 2, 3] and all(r['decodedWidth'] == width and
                r['decodedHeight'] == height and r['viewport'] == rows[0]['viewport'] for r in rows), 'sample_protocol_refused')
    for group in range(4):
        group_samples = [samples[(group * 3 + arm) * 4 + 3] for arm in range(3)]
        require(all(s['viewport'] == group_samples[0]['viewport'] for s in group_samples), 'format_geometry_changed')
        for left, right, label in ((0, 1, 'original_to_png'), (1, 2, 'png_to_webp')):
            result = {'assetId': IDS[group], 'comparison': label, 'readbacks': {}, 'humanQualityAcceptance': 'NOT_RUN',
                      'physicalDisplayP3Acceptance': 'NOT_RUN'}
            for kind in ('rawRgba', 'rawExtendedRgba128'):
                if any(s[kind]['status'] == 'UNSUPPORTED' for s in (group_samples[left], group_samples[right])):
                    result['readbacks'][kind] = {'status': 'UNSUPPORTED'}
                    continue
                arrays = []
                for sample in (group_samples[left], group_samples[right]):
                    meta = sample[kind]
                    require(meta['bytes'] == width * height * (4 if kind == 'rawRgba' else 16), 'readback_dimensions_refused')
                    path = folder / meta['file']
                    require(path.name == meta['file'] and path.stat().st_size == meta['bytes'] and
                            file_hash(path) == meta['sha256'], 'readback_integrity_refused')
                    values = array.array('B' if kind == 'rawRgba' else 'f')
                    with path.open('rb') as stream:
                        values.fromfile(stream, meta['bytes'] // values.itemsize)
                    arrays.append(values)
                changed, maximum, total = 0, 0.0, 0.0
                for a, b in zip(*arrays):
                    delta = abs(a - b)
                    require(delta == delta and delta != float('inf'), 'nonfinite_readback')
                    changed += delta != 0
                    maximum = max(maximum, delta)
                    total += delta
                result['readbacks'][kind] = dict(changedChannels=changed, maxAbsDelta=maximum,
                    meanAbsDelta=total / len(arrays[0]), totalChannels=len(arrays[0]))
            results.append(result)
    return results





def validate_frame_timings(runtime):
    measured = 0
    for sample in runtime['samples']:
        frames = sample.get('frames', [])
        if sample.get('frameTimingStatus') != 'MEASURED':
            require(not frames, 'unexpected_unmeasured_frames')
            continue
        require(len(frames) == 1, 'missing_bound_frame')
        frame = frames[0]
        require(frame['frameNumber'] == sample['paintFrameNumber'] and
                frame['frameworkVsyncDeltaMicros'] == frame['vsyncMicros'] - sample['paintVsyncMicros'] and
                frame['epoch'] == sample['epoch'] and all(frame[key] >= 0 for key in
                ('buildMicros', 'rasterMicros', 'totalSpanMicros')), 'frame_epoch_or_timestamp_refused')
        measured += 1
    return {'measuredSamples': measured, 'missingSamples': len(runtime['samples']) - measured,
            'scope': 'paint_frameNumber_epoch_bound_engine_and_framework_timestamps_reported_separately'}


def validate_presentations(folder, runtime, *, _frame_shape=(1712, 2778)):
    from PIL import Image
    captures = runtime.get('captures', [])
    require(len(captures) == 12 and runtime.get('presentedCases') == 12, 'incomplete_presentations')
    evidence = []
    for index, capture in enumerate(captures):
        require(capture['index'] == index, 'capture_binding_refused')
        proof_epoch = capture['slot']['epoch']
        for mode, suffix in ((False, 'slot'), (True, 'crop-1to1')):
            proof = capture['crop' if mode else 'slot']
            require(proof['epoch'] == proof_epoch and proof['index'] == index and proof['crop'] == mode and
                    proof.get('nativePresentationVerified') is True and
                    proof.get('nativeCaptureSource') == 'FlutterSurfaceViewPixelCopy', 'capture_binding_refused')
            capture_path = folder / f'case-{index}-{suffix}.png'
            require(capture_path.is_file(), 'capture_file_missing:' + capture_path.name)
            with Image.open(capture_path) as image:
                require(image.size == (proof['nativeSurfaceWidth'], proof['nativeSurfaceHeight']), 'surface_capture_dimensions_refused')
                left, top, cell = proof['nativeMarkerLeft'], proof['nativeMarkerTop'], proof['markerCellPixels']
                require(left == proof['markerX'] + proof['nativeFlutterOffsetX'] and
                        top == proof['markerY'] + proof['nativeFlutterOffsetY'], 'surface_marker_offset_refused')
                require(cell == 4 and left >= 0 and top >= 0 and left + 8 <= image.width and top + 8 <= image.height,
                        'marker_geometry_refused')
                rgb = image.convert('RGB')
                for y in range(8):
                    for x in range(8):
                        expected = proof['markerColors'][y // 4 * 2 + x // 4]
                        require(rgb.getpixel((left + x, top + y)) ==
                                ((expected >> 16) & 255, (expected >> 8) & 255, expected & 255), 'marker_pixels_refused')
                if mode:
                    roi = proof['roi']
                    require((roi['sourceX'], roi['sourceY']) == TEXT_CROPS[IDS[index // 3]] and
                            0 < roi['sourceWidth'] <= 1200 and 0 < roi['sourceHeight'] <= 600 and
                            roi['sourceWidth'] == roi['destinationPhysicalWidth'] and
                            roi['sourceHeight'] == roi['destinationPhysicalHeight'] and
                            roi['physicalPixelsPerSourcePixel'] == 1, 'crop_1to1_refused')
        slot_sha = file_hash(folder / f'case-{index}-slot.png')
        crop_sha = file_hash(folder / f'case-{index}-crop-1to1.png')
        require(slot_sha != crop_sha, 'stale_screenshot_refused')
        evidence.append({'index': index, 'slotSha256': slot_sha, 'cropSha256': crop_sha,
                         'presentationVerified': True, 'roi': capture['crop']['roi'],
                         'pixelProof': validate_roi_pixels(folder, runtime, capture, _frame_shape)})
    return evidence


def validate_roi_pixels(folder, runtime, capture, frame_shape):
    from PIL import Image
    index = capture['index']
    candidates = [s for s in runtime['samples'] if s['index'] == index and s['sample'] == 3]
    require(len(candidates) == 1, 'crop_native_sample_refused')
    sample = candidates[0]
    width, height = frame_shape
    require((sample['decodedWidth'], sample['decodedHeight']) == frame_shape, 'crop_native_dimensions_refused')
    meta = sample['rawRgba']
    path = folder / meta['file']
    require(meta['status'] == 'MEASURED' and Path(meta['file']).name == meta['file'] and
            meta['bytes'] == width * height * 4 and path.is_file() and
            path.stat().st_size == meta['bytes'] and file_hash(path) == meta['sha256'], 'crop_native_integrity_refused')
    proof = capture['crop']
    roi = proof['roi']
    x, y, w, h = (roi[key] for key in ('sourceX', 'sourceY', 'sourceWidth', 'sourceHeight'))
    left = roi['globalPhysicalLeft'] + proof['nativeFlutterOffsetX']
    top = roi['globalPhysicalTop'] + proof['nativeFlutterOffsetY']
    require(x >= 0 and y >= 0 and x + w <= width and y + h <= height and left >= 0 and top >= 0,
            'crop_bounds_refused')
    data = path.read_bytes()
    expected = b''.join(data[((y + line) * width + x) * 4:((y + line) * width + x + w) * 4]
                        for line in range(h))
    screenshot = folder / f'case-{index}-crop-1to1.png'
    with Image.open(screenshot) as image:
        require(left + w <= image.width and top + h <= image.height, 'crop_bounds_refused')
        actual = image.convert('RGBA').crop((left, top, left + w, top + h)).tobytes()
    require(expected == actual, 'crop_pixels_refused')
    return dict(sourceRoi=[x, y, w, h], surfaceRoi=[left, top, w, h], channels=len(expected),
                changedChannels=0, maxAbsDelta=0, nativeRawSha256=meta['sha256'],
                screenshotSha256=file_hash(screenshot))


def host_space(root):
    raw_bytes = 12 * 1712 * 2778 * (4 + 16)
    build_bytes, receipt_bytes = 2 * 1024 ** 3, 8 * 1024 ** 2
    required = raw_bytes + build_bytes + receipt_bytes
    available = shutil.disk_usage(root).free
    require(available >= required, 'host_space_insufficient:' + str(available) + ':' + str(required))
    return dict(rawDatasetBytes=raw_bytes, buildBudgetBytes=build_bytes, receiptMarginBytes=receipt_bytes,
                requiredBytes=required, availableBytes=available)


def result_summary(run, result):
    def bounded(value, limit=512):
        return str(value)[:limit] if value is not None else None
    primary = result.get('primaryError', {})
    preservation = result.get('preservation', {})
    summary = dict(output=bounded(run), status=result.get('status'), acceptedCases=result.get('acceptedCases', 0),
        reason=bounded(result.get('reason')), primaryError={'type': bounded(primary.get('type'), 80),
            'message': bounded(primary.get('message'))},
        preservation={key: preservation[key] for key in ('unchanged', 'sourceHashesBeforeSha256',
            'sourceHashesAfterSha256', 'p5cHashesBeforeSha256', 'p5cHashesAfterSha256') if key in preservation},
        cleanupErrors=[bounded(value, 250) for value in result.get('cleanupErrors', [])[-3:]],
        receiptError=bounded(result.get('receiptError')))
    if 'reason' in preservation:
        summary['preservation']['reason'] = bounded(preservation['reason'])
    return summary


def device_space(apk_bytes):
    raw_bytes = 12 * 1712 * 2778 * (4 + 16)
    margin = 256 * 1024 * 1024
    required = max(1536 * 1024 * 1024, raw_bytes + apk_bytes + margin)
    columns = adb('shell', 'df', '-k', '/sdcard').decode().splitlines()[-1].split()
    require(len(columns) == 6 and columns[3].isdigit(), 'device_space_unreadable')
    available = int(columns[3]) * 1024
    require(available >= required, 'device_space_insufficient:' + str(available) + ':' + str(required))
    return dict(rawDatasetBytes=raw_bytes, apkBytes=apk_bytes, marginBytes=margin,
                requiredBytes=required, availableBytes=available)


def prior_package_owned(package):
    listing = adb('shell', 'pm', 'list', 'packages', package).decode().splitlines()
    if 'package:' + package not in listing:
        return False
    paths = adb('shell', 'pm', 'path', package).decode().splitlines()
    require(len(paths) == 1 and paths[0].startswith('package:/data/app/') and
            re.fullmatch(r'package:[A-Za-z0-9_./=+~\-]+', paths[0]), 'installed_package_path_refused')
    installed_sha = adb('shell', 'sha256sum', paths[0][8:]).decode().split()[0]
    pattern = 'host/build/app/outputs/apk/release/app-release.apk' if package == PACKAGE else 'host/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
    matches = [path for root in QA_ROOT.glob('p5d-offline-*') for path in [root / pattern]
               if path.is_file() and file_hash(path) == installed_sha]
    require(bool(matches), 'existing_qa_package_ownership_refused')
    return True


def reuse_readback(run, target, meta):
    # Eski kanıt bağımsız kopyalanır; yeni native makbuz hash/boyut bağını belirler.
    for previous in sorted(QA_ROOT.glob('p5d-offline-*')):
        source = previous / 'device-output' / meta['file']
        receipt = previous / 'result.private.json'
        if previous == run or not receipt.is_file() or not source.is_file():
            continue
        if source.stat().st_size != meta['bytes'] or file_hash(source) != meta['sha256']:
            continue
        shutil.copyfile(source, target)
        require(target.stat().st_size == meta['bytes'] and file_hash(target) == meta['sha256'], 'readback_copy_refused')
        require(target.stat().st_ino != source.stat().st_ino, 'readback_alias_refused')
        return {'source': str(source), 'file': meta['file'], 'bytes': meta['bytes'], 'sha256': meta['sha256']}
    return None


def pull_evidence(run, nonce):
    origin = '/sdcard/Android/data/' + PACKAGE + '/files/' + nonce + '/'
    folder = run / 'device-output'
    folder.mkdir(exist_ok=True)
    if not (folder / 'runtime.json').exists():
        adb('pull', origin + 'runtime.json', str(folder / 'runtime.json'), timeout=60)
    runtime = json.loads((folder / 'runtime.json').read_bytes())
    require(runtime.get('runNonce') == nonce, 'runtime_nonce_refused')
    for index in range(12):
        for kind in ('slot', 'crop-1to1'):
            name = f'case-{index}-{kind}.png'
            if not (folder / name).exists():
                adb('pull', origin + name, str(folder / name), timeout=60)
    reused = []
    for sample in runtime['samples']:
        if sample['sample'] != 3:
            continue
        for kind in ('rawRgba', 'rawExtendedRgba128'):
            meta = sample[kind]
            if meta['status'] != 'MEASURED':
                continue
            require(Path(meta['file']).name == meta['file'], 'readback_path_refused')
            path = folder / meta['file']
            if path.exists() and path.stat().st_size == meta['bytes'] and file_hash(path) == meta['sha256']:
                continue
            require(not path.exists(), 'existing_transfer_refused')
            copied = reuse_readback(run, path, meta)
            if copied:
                reused.append(copied)
            else:
                adb('pull', origin + meta['file'], str(path), timeout=240)
            require(path.stat().st_size == meta['bytes'] and file_hash(path) == meta['sha256'], 'readback_transfer_refused')
    write_new(run / 'readback-reuse.private.json', reused)
    return runtime


def finalize_run(run, result, before, inputs, old, cases, installed, nonce):
    result['cleanupErrors'] = []
    if installed:
        try:
            adb('shell', 'am', 'force-stop', PACKAGE)
        except Exception as cleanup_error:
            result['cleanupErrors'].append(str(cleanup_error))
        if not (run / 'device-output/runtime.json').exists():
            try:
                folder = run / 'device-output'
                folder.mkdir(exist_ok=True)
                adb('pull', '/sdcard/Android/data/' + PACKAGE + '/files/' + nonce + '/runtime.json',
                    str(folder / 'runtime.json'), timeout=60)
            except Exception as cleanup_error:
                result['cleanupErrors'].append(str(cleanup_error))
        partial = run / 'device-output/runtime.json'
        if partial.exists() and 'runtime' not in result:
            try:
                result['runtime'] = json.loads(partial.read_bytes())
                result['decodedCases'] = result['runtime'].get('acceptedCases', 0)
                result['acceptedCases'] = 0
            except Exception as cleanup_error:
                result['cleanupErrors'].append(str(cleanup_error))
    samples = result.get('runtime', {}).get('samples', [])
    measured = {index for index in range(len(cases))
        if [s.get('sample') for s in samples if s.get('index') == index] == [0, 1, 2, 3]}
    presented = {entry['index'] for entry in result.get('captureEvidence', [])
                 if entry.get('presentationVerified') is True and entry.get('pixelProof')}
    result['missingMeasurementCases'] = [row['caseId'] for index, row in enumerate(cases) if index not in measured]
    result['missingPresentationCases'] = [row['caseId'] for index, row in enumerate(cases) if index not in presented]
    result['missingCases'] = [row['caseId'] for index, row in enumerate(cases)
                              if index not in measured or index not in presented]
    try:
        result['preservation'] = check_preservation(before, inputs, old)
    except Exception as error:
        result['status'] = 'PRESERVATION_REFUSED'
        result['preservation'] = {'unchanged': False, 'reason': str(error)}
    try:
        write_new(run / 'source-after.private.json', {'repository': repo_hashes(),
                  'inputs': {name: file_hash(Path(name)) for name in inputs}})
    except Exception as cleanup_error:
        result['cleanupErrors'].append(str(cleanup_error))
    try:
        write_new(run / 'result.private.json', result)
    except Exception as error:
        result['receiptError'] = type(error).__name__ + ':' + str(error)
        result['statusBeforeReceiptFailure'] = result['status']
        result['status'] = 'RECEIPT_WRITE_FAILED'
        print(json.dumps(result_summary(run, result), ensure_ascii=False), flush=True)


def run_all(report_path, output_root=QA_ROOT):
    require(output_root == QA_ROOT and QA_ROOT.is_dir() and QA_ROOT.resolve() == QA_ROOT, 'output_root_refused')
    host_space_evidence = host_space(QA_ROOT)
    cases, inputs = fixture_cases(report_path)
    inputs[str(Path(report_path))] = REPORT_SHA
    sources = source_closure()
    before, old = repo_hashes(), old_output_inventory()
    run = QA_ROOT / ('p5d-offline-' + uuid.uuid4().hex[:12])
    run.mkdir(mode=0o700)
    nonce = uuid.uuid4().hex
    write_new(run / 'old-output-before.private.json', old)
    result = {'hostSpaceBeforeSnapshot': host_space_evidence, 'runNonce': nonce, 'scope': 'P5D_OFFLINE_APP', 'status': 'NOT_RUN', 'acceptedCases': 0,
              'physicalDisplayP3Acceptance': 'NOT_RUN', 'humanQualityAcceptance': 'NOT_RUN',
              'productionProfileDecision': 'NOT_RUN', 'httpTransferMeasured': False,
              'modeledBufferBudgetBytes': 192 * 1024 * 1024, 'processRssLimit': False,
              'buildMode': 'release', 'deviceSelector': DEVICE, 'caseWatchdogSeconds': 30}
    installed = False
    try:
        flutter_executable = shutil.which('flutter')
        require(flutter_executable, 'flutter_missing')
        flutter = Path(flutter_executable).resolve().parents[1]
        sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or '/Volumes/MAC_EK/dev/android_sdk')
        gradles = list((Path.home() / '.gradle/wrapper/dists/gradle-8.14-all').glob('*/gradle-8.14/bin/gradle'))
        require(len(gradles) == 1 and (sdk / 'platforms/android-36/android.jar').is_file(), 'gradle_or_sdk_cache_missing')
        java_home = Path(subprocess.run(['/usr/libexec/java_home', '-v', '17'], check=True,
                                        capture_output=True, text=True).stdout.strip())
        require((java_home / 'bin/java').is_file(), 'java17_cache_missing')
        keystore = Path.home() / '.android/debug.keystore'
        require(keystore.is_file(), 'existing_signing_missing_no_setup')
        aapt = sdk / 'build-tools/35.0.0/aapt'
        require(aapt.is_file(), 'aapt_missing')
        result['localMavenArtifacts'] = private_maven(run, flutter)
        mobile = prepare(run, cases, sources, flutter, sdk)
        for row in cases:
            source = Path(report_path).parent / Path(row['asset']).name
            require(file_hash(source) == row['sha256'], 'source_changed_during_snapshot')
            write_new(mobile / row['asset'], source.read_bytes())
        write_new(run / 'source-before.private.json', {'repository': before, 'inputs': inputs, 'closure': sources})
        checked([flutter / 'bin/flutter', '--no-version-check', '--suppress-analytics', 'pub', 'get', '--offline'], mobile, run, 'pub-offline')
        plugins = mobile / '.flutter-plugins-dependencies'
        if plugins.exists():
            metadata = json.loads(plugins.read_bytes())
            require(not metadata.get('plugins', {}).get('android'), 'native_plugin_refused')
        checked([gradles[0], '--offline', '--no-daemon', ':app:assembleRelease', ':app:assembleReleaseAndroidTest',
                 '-Ptarget=lib/app/p5d_offline_qa.dart', '-Ptarget-platform=android-arm64',
                 '-Pdart-defines=' + base64.b64encode(b'TELLPAL_P5D_OFFLINE=true').decode(),
                 '-Pp5dExistingKeystore=' + str(keystore)], mobile / 'android', run, 'gradle-offline', java_home=java_home)
        app = mobile / 'build/app/outputs/apk/release/app-release.apk'
        test = mobile / 'build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
        result['apkAudit'] = [audit_apk(app, aapt, run, PACKAGE), audit_apk(test, aapt, run, TEST_PACKAGE, True)]
        result['flutterVersion'] = subprocess.run([str(flutter / 'bin/flutter'), '--no-version-check', '--suppress-analytics', '--version', '--machine'],
                                                   check=True, capture_output=True, text=True).stdout
        require(adb('shell', 'getprop', 'ro.kernel.qemu').strip() == b'1', 'physical_device_refused')
        require(adb('shell', 'getprop', 'ro.product.cpu.abi').strip() == b'arm64-v8a', 'emulator_abi_refused')
        result['deviceSpaceBeforeInstall'] = device_space(app.stat().st_size + test.stat().st_size)
        owned_app = prior_package_owned(PACKAGE)
        owned_test = prior_package_owned(TEST_PACKAGE)
        adb('install', *(['-r'] if owned_app else []), str(app), timeout=120)
        installed = True
        adb('install', *(['-r'] if owned_test else []), str(test), timeout=120)
        instrument(run, nonce)
        runtime = pull_evidence(run, nonce)
        result['frameTimingEvidence'] = validate_frame_timings(runtime)
        result['captureEvidence'] = validate_presentations(run / 'device-output', runtime)
        logs = adb('logcat', '-d', '--pid=' + str(runtime['pid']), '-v', 'brief')
        write_new(run / 'renderer.private.log', logs)
        result['rendererEvidence'] = 'Impeller opt-out' if b'Impeller opt-out' in logs else 'MANIFEST_ONLY'
        result['comparisons'] = compare_readbacks(run / 'device-output', runtime)
        result.update(status='MEASURED' if result['frameTimingEvidence']['measuredSamples'] == 48
                      else 'PARTIAL_METRICS', acceptedCases=runtime['acceptedCases'], runtime=runtime)
    except Exception as error:
        result['reason'] = str(error)
        result['primaryError'] = {'type': type(error).__name__, 'message': str(error)}
    finally:
        finalize_run(run, result, before, inputs, old, cases, installed, nonce)
    return run, result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', required=True, type=Path)
    args = parser.parse_args()
    try:
        path, result = run_all(args.report)
    except Exception as error:
        result = {'status': 'PREFLIGHT_REFUSED', 'primaryError': {'type': type(error).__name__, 'message': str(error)},
                  'preservation': {'unchanged': None, 'reason': 'not_verified_before_result_context'}}
        print(json.dumps(result_summary(None, result), ensure_ascii=False), flush=True)
        return 1
    print(json.dumps(result_summary(path, result), ensure_ascii=False))
    return 0 if result['status'] == 'MEASURED' else 1


if __name__ == '__main__':
    raise SystemExit(main())
