"""Ağsız eski/yeni paketli motor ve bağımsız Pillow doğrulaması."""
import argparse
import copy
import hashlib
import json
import os
import shutil
import subprocess
import time
import zipfile
from pathlib import Path
from PIL import Image, __version__ as pillow_version


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command):
    completed = subprocess.run(command, capture_output=True, text=True)
    if completed.returncode:
        raise RuntimeError(completed.stdout + completed.stderr)


def decode(path):
    start = time.perf_counter_ns()
    with Image.open(path) as image:
        image.load()
        assert image.format == 'PNG' and image.mode == 'RGB'
        size = list(image.size)
        pixels = image.tobytes()
    return pixels, size, (time.perf_counter_ns() - start) / 1_000_000


def main():
    if not __debug__:
        raise RuntimeError('PYTHON_OPTIMIZATION_NOT_SUPPORTED')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project-root', type=Path, required=True)
    parser.add_argument('--new-jar', type=Path, required=True)
    parser.add_argument('--workdir', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    args = parser.parse_args()
    root = args.project_root.resolve()
    work = args.workdir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    candidate = work / 'candidate.jar'
    shutil.copyfile(args.new_jar.resolve(), candidate)
    evidence = root / 'be/docs/evidence/png-lossless-compression'
    baseline = root / 'be/var/png-lossless-20261018-91ab1145-80bd-4dbe-a461-4fa28fa46787'
    sources = root / 'be/var/p4a2-preflight-dc591777-9e53-4e14-856f-f13fec00fa8c'
    old_run = root / 'be/var/p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09'
    previous = root / 'be/docs/evidence/p4-reading-images/android-measurement-2026-10-18/verification.json'
    plan = root / 'be/docs/evidence/p4-reading-images/bounded-run-proposal-2026-10-17.json'
    protected = [path for folder in (baseline, sources, old_run) for path in folder.rglob('*') if path.is_file()]
    protected += [previous, root / '_bmad-output/party-mode/memories/installed/.memlog.md']
    before = {str(path.relative_to(root)): sha(path) for path in sorted(protected)}
    baseline_proof = json.loads((baseline / 'baseline.json').read_text())
    assert sha(baseline / 'baseline.jar') == baseline_proof['jarSha256']
    previous_data = json.loads(previous.read_text())
    assert sha(root / '_bmad-output/party-mode/memories/installed/.memlog.md') == baseline_proof['memlogSha256']
    assert sha(old_run / 'ledger.log') == baseline_proof['parentLedgerSha256']
    assert sha(old_run / 'mobile-window-180/ledger.log') == previous_data['preservation']['childLedgerSha256']
    for name, proof in previous_data['preservation']['previousFailureFiles'].items():
        assert sha(old_run / name) == proof['sha256']
    expected = {observation['render']['entryIndex']: observation['render']
                for observation in previous_data['observations'] if not observation['render']['canonicalSource']}
    classes = work / 'probe-classes'
    classes.mkdir()
    classpath = str(root / 'be/target/classes') + ':' + (baseline / 'dependencies.classpath').read_text().strip()
    run([str(args.java_home / 'bin/javac'), '-cp', classpath, '-d', str(classes), str(evidence / 'PngLosslessProbe.java')])
    policy = '(version 1)(allow default)(deny network*)(allow process-exec* (with no-sandbox) (literal "/bin/ps"))'
    guard = ['/usr/bin/sandbox-exec', '-p', policy]
    rss_probe = subprocess.run(guard + ['/bin/ps', '-o', 'rss=', '-p', str(os.getpid())], capture_output=True, text=True)
    assert rss_probe.returncode == 0 and int(rss_probe.stdout.strip()) > 0
    denied = subprocess.run(guard + ['/usr/bin/python3', '-c',
            'import socket; s=socket.socket(); s.bind(("127.0.0.1",0))'], capture_output=True, text=True)
    assert denied.returncode != 0 and 'Operation not permitted' in denied.stderr
    engine_proofs = {}
    for engine, jar in (('old', baseline / 'baseline.jar'), ('new', candidate)):
        output = work / engine
        command = guard + [str(args.java_home / 'bin/java'), '-Xmx1024m', '-Djava.awt.headless=true',
                '-Dloader.main=PngLosslessProbe', '-Dloader.path=' + str(classes), '-cp', str(jar),
                'org.springframework.boot.loader.launch.PropertiesLauncher', str(sources), str(output), str(plan)]
        run(command)
        metadata = json.loads((output / 'metadata.json').read_text())
        assert metadata['classpath'] == str(jar)
        with zipfile.ZipFile(jar) as packaged:
            binaries = {name: hashlib.sha256(packaged.read('BOOT-INF/classes/' + name)).hexdigest()
                        for name in baseline_proof['classes']}
        engine_proofs[engine] = {'jarSha256': sha(jar), 'jarBytes': jar.stat().st_size,
                'classSha256': binaries, 'metadataSha256': sha(output / 'metadata.json'),
                'profile': metadata['profile'], 'profiles': metadata['profiles'], 'results': metadata['results']}
    assert engine_proofs['old']['classSha256'] == {name: proof['sha256'] for name, proof in baseline_proof['classes'].items()}
    assert engine_proofs['old']['profile'] == engine_proofs['new']['profile']
    for role in engine_proofs['old']['profiles']:
        descriptors = []
        for engine, quality in (('old', '1.0'), ('new', '0.0')):
            descriptor = copy.deepcopy(engine_proofs[engine]['profiles'][role]['descriptor'])
            assert descriptor['encoder'].pop('compressionQuality') == quality
            descriptor.pop('runtimeAndEngineBinaries')
            descriptors.append(descriptor)
        assert descriptors[0] == descriptors[1]
    rows = []
    for index, asset_id in enumerate((9778, 9816, 9818), start=1):
        source = sources / f'canonical-review-{asset_id}.png'
        assert sha(source) == expected[index]['sourceSha256']
        old, new = (work / engine / f'{asset_id}.png' for engine in ('old', 'new'))
        assert sha(old) == expected[index]['displaySha256']
        assert old.stat().st_size == expected[index]['expectedPayloadBytes']
        old_pixels, old_size, old_decode = decode(old)
        new_pixels, new_size, new_decode = decode(new)
        assert old_size == new_size == [822, 1334]
        assert old_pixels == new_pixels
        assert new.stat().st_size < old.stat().st_size
        jpeg = sources / f'source-{asset_id}.jpg'
        jpeg_bytes = previous_data['payloadTotals']['oldJpegMetadataBytes'][str(asset_id)]
        assert jpeg.stat().st_size == jpeg_bytes
        with Image.open(jpeg) as image:
            jpeg_size = list(image.size)
        old_result = engine_proofs['old']['results'][index - 1]
        new_result = engine_proofs['new']['results'][index - 1]
        assert old_result['renditionKeys'].keys() == new_result['renditionKeys'].keys()
        assert all(old_result['renditionKeys'][key] != new_result['renditionKeys'][key] for key in old_result['renditionKeys'])
        rows.append({'assetId': asset_id, 'sourceSha256': sha(source), 'sourceBytes': source.stat().st_size,
                'dimensions': old_size, 'rgbBytesCompared': len(old_pixels), 'allRgbBytesEqual': True,
                'rgbSha256': hashlib.sha256(old_pixels).hexdigest(), 'oldPayloadMatches94': True,
                'oldPngBytes': old.stat().st_size, 'newPngBytes': new.stat().st_size,
                'oldPngSha256': sha(old), 'newPngSha256': sha(new),
                'savedBytes': old.stat().st_size - new.stat().st_size,
                'savedPercent': 100 * (1 - new.stat().st_size / old.stat().st_size),
                'existingJpegBytes': jpeg_bytes, 'existingJpegDimensions': jpeg_size, 'existingJpegSha256': sha(jpeg),
                'newPngToExistingJpegRatio': new.stat().st_size / jpeg_bytes,
                'oldProcessMillis': old_result['processMillis'], 'newProcessMillis': new_result['processMillis'],
                'pillowOldDecodeMillis': old_decode, 'pillowNewDecodeMillis': new_decode})
    after = {str(path.relative_to(root)): sha(path) for path in sorted(protected)}
    assert before == after
    result = {'status': 'PASS', 'measuredAtUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
            'networkGuard': {'mechanism': 'macOS sandbox-exec', 'policy': policy,
                'scope': 'PACKAGED_PARENT_AND_INHERITED_IMAGE_CHILD', 'loopbackBindDenied': True,
                'rssException': 'ONLY_UNMODIFIED_SYSTEM_BIN_PS_EXECUTES_WITH_NO_SANDBOX',
                'systemPsSha256': sha(Path('/bin/ps')), 'rssProbeSucceeded': True},
            'pillowVersion': pillow_version, 'priorEvidenceSha256': sha(previous),
            'descriptorChangesLimitedToCompressionAndEngineBinaries': True,
            'baselineCommit': baseline_proof['baselineCommit'], 'engines': engine_proofs, 'images': rows,
            'protectedArtifactsUnchanged': True, 'protectedFileCount': len(before), 'protectedSha256': before,
            'limits': ['LOCAL_FILES_ONLY_NO_CLOUD_DB_SESSION_DEVICE_OR_LIVE_MUTATION',
                'PROCESS_TIME_INCLUDES_STARTUP_HANDSHAKE_DECODE_RESIZE_ENCODE_MONITOR_READ_CLEANUP_NOT_PURE_ENCODE',
                'PILLOW_FULL_DECODE_TIME_IS_SEPARATE_SINGLE_SAMPLE_NOT_BENCHMARK',
                'EXISTING_JPEG_IS_PRIOR_FILE_BASELINE_DIFFERENT_GEOMETRY_NOT_FORMAT_OR_QUALITY_QUALIFICATION',
                'NO_PHYSICAL_QUALITY_OR_PRODUCTION_PROFILE_P4B_OR_P0_RAW_EVIDENCE_APPROVAL']}
    (evidence / 'verification.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'status': result['status'], 'protectedFiles': len(before),
            'images': [{key: row[key] for key in ('assetId', 'oldPngBytes', 'newPngBytes', 'savedPercent', 'allRgbBytesEqual')} for row in rows]}))


if __name__ == '__main__':
    main()
