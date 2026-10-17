"""Sabit üç PNG için ağsız, yerel ve kayıpsız WebP karşılaştırması."""
import errno
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import signal
import socket
import statistics
import struct
import subprocess
import sys
import tempfile
import time
import zlib

from PIL import Image, features, __version__ as pillow_version


ASSET_IDS = (9778, 9816, 9818)
DIMENSIONS = (822, 1334)
RGB_BYTES = 3_289_644
GOLD_SHA256 = '6d3b7a4432a8f94b6ff073d332cbcd98dd0e4ac2338d2f6b46606e1c030a7ce2'
EVIDENCE = Path('be/docs/evidence/local-lossless-formats')
GOLD = Path('be/docs/evidence/png-lossless-compression/verification.json')
PNG_RUN = Path('be/var/png-lossless-run-20261018-c1f43a2e')
PREFLIGHT = Path('be/var/p4a2-preflight-dc591777-9e53-4e14-856f-f13fec00fa8c')
OLD_RUN = Path('be/var/p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09')
MEMLOG = Path('_bmad-output/party-mode/memories/installed/.memlog.md')
INITIAL_TIMING_RUN = Path('be/var/local-lossless-formats-vbqqt735')
ENCODER = {'lossless': True, 'quality': 100, 'method': 6, 'exact': True}
POLICY = '(version 1)(allow default)(deny network*)'
PROCESS_TIMEOUT_SECONDS = 30
WORKER_TIMEOUT_SECONDS = 240


class ProbeError(RuntimeError):
    """Kanıt koşulu sağlanmadığında PASS üretilmesini engeller."""


def ensure(condition, code):
    if not condition:
        raise ProbeError(code)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def sha(path):
    with path.open('rb') as source:
        hasher = hashlib.sha256()
        for block in iter(lambda: source.read(1024 * 1024), b''):
            hasher.update(block)
    return hasher.hexdigest()


def exclusive(path, data):
    with path.open('x', encoding='utf-8') as output:
        output.write(data)


def json_text(data):
    return json.dumps(data, ensure_ascii=False, indent=2) + '\n'


def ensure_public_absent(evidence):
    ensure(not any((evidence / name).exists() for name in ('verification.json', 'verification.md')),
           'PUBLIC_EVIDENCE_ALREADY_EXISTS')


def publish(evidence, result):
    """İki public dosyayı exclusive ayırır; çakışmada yalnız kendi boş kaydını kaldırır."""
    created, streams = [], []
    try:
        for name in ('verification.md', 'verification.json'):
            path = evidence / name
            streams.append(path.open('x', encoding='utf-8'))
            created.append(path)
        streams[0].write(markdown(result))
        streams[0].close()
        streams[1].write(json_text(result))
        streams[1].close()
    except BaseException as failure:
        for stream in streams:
            try:
                stream.close()
            except BaseException as cleanup_failure:
                failure.add_note('Yayın akışı kapatılamadı: ' + str(cleanup_failure))
        for path in created:
            try:
                path.unlink()
            except BaseException as cleanup_failure:
                failure.add_note('Yeni yayın dosyası temizlenemedi: ' + str(cleanup_failure))
        raise


def require_network_denied():
    """İşaret bayrağına güvenmez; çalışan süreçte gerçek loopback bind reddi ister."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
            probe.bind(('127.0.0.1', 0))
    except OSError as failure:
        ensure(failure.errno == errno.EPERM, 'NETWORK_DENIAL_IS_NOT_EPERM')
        return {'loopbackBindDenied': True, 'errno': failure.errno,
                'error': os.strerror(failure.errno)}
    raise ProbeError('NETWORK_GUARD_REQUIRED')


def command(argv, timeout=PROCESS_TIMEOUT_SECONDS):
    started = time.perf_counter_ns()
    completed = subprocess.run(argv, capture_output=True, text=True, timeout=timeout,
                               env=dict(os.environ, LC_ALL='C'))
    elapsed = (time.perf_counter_ns() - started) / 1_000_000
    return completed, elapsed


def native_network_proof():
    completed, elapsed = command(['/usr/bin/nc', '-l', '127.0.0.1', '0'], timeout=5)
    ensure(completed.returncode != 0 and 'Operation not permitted' in completed.stderr,
           'NATIVE_CHILD_NETWORK_GUARD_REQUIRED')
    return {'command': ['/usr/bin/nc', '-l', '127.0.0.1', '0'],
            'returnCode': completed.returncode, 'stdout': completed.stdout,
            'stderr': completed.stderr, 'elapsedMillis': elapsed,
            'nativeChildInheritedDenial': True, 'binarySha256': sha(Path('/usr/bin/nc'))}


def snapshot(root, roots):
    result = {}
    for entry in sorted(roots):
        ensure(entry.exists() and not entry.is_symlink(), 'PROTECTED_PATH_MISSING_OR_SYMLINK')
        files = [entry] if entry.is_file() else sorted(entry.rglob('*'))
        for path in files:
            ensure(not path.is_symlink(), 'PROTECTED_SYMLINK')
            if path.is_file():
                result[str(path.relative_to(root))] = sha(path)
    return result


def verify_preservation(before, after):
    ensure(before == after, 'PROTECTED_ARTIFACT_CHANGED')


def validate_png_container(data, expected_dimensions):
    ensure(data[:8] == b'\x89PNG\r\n\x1a\n', 'PNG_SIGNATURE_INVALID')
    offset, chunks, header = 8, [], None
    while offset < len(data):
        ensure(offset + 12 <= len(data), 'PNG_CHUNK_TRUNCATED')
        length = int.from_bytes(data[offset:offset + 4], 'big')
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        ensure(end <= len(data), 'PNG_CHUNK_TRUNCATED')
        body = data[offset + 8:end - 4]
        crc = int.from_bytes(data[end - 4:end], 'big')
        ensure(zlib.crc32(kind + body) & 0xffffffff == crc, 'PNG_CRC_INVALID')
        ensure(kind in (b'IHDR', b'IDAT', b'IEND'), 'PNG_METADATA_OR_UNKNOWN_CHUNK')
        if kind == b'IHDR':
            ensure(not chunks and length == 13, 'PNG_HEADER_INVALID')
            header = struct.unpack('>IIBBBBB', body)
        chunks.append(kind)
        offset = end
        if kind == b'IEND':
            ensure(length == 0 and offset == len(data), 'PNG_TRAILING_BYTES')
            break
    ensure(header is not None and tuple(header[:2]) == tuple(expected_dimensions), 'PNG_DIMENSIONS_INVALID')
    ensure(header[2:] == (8, 2, 0, 0, 0), 'PNG_RGB8_REQUIRED')
    ensure(chunks[0] == b'IHDR' and chunks[-1] == b'IEND'
           and chunks.count(b'IHDR') == chunks.count(b'IEND') == 1
           and all(kind == b'IDAT' for kind in chunks[1:-1]) and len(chunks) >= 3,
           'PNG_CHUNK_ORDER_INVALID')
    return [kind.decode('ascii') for kind in chunks]


def inspect_image(source, expected_format, dimensions, allowed_info=()):
    with Image.open(source) as image:
        image.load()
        ensure(image.format == expected_format, 'IMAGE_FORMAT_INVALID')
        ensure(getattr(image, 'n_frames', 1) == 1, 'SINGLE_FRAME_REQUIRED')
        ensure(image.size == tuple(dimensions), 'IMAGE_DIMENSIONS_INVALID')
        ensure(image.mode == 'RGB', 'OPAQUE_RGB_REQUIRED')
        ensure(not set(image.info).difference(allowed_info) and not image.getexif(), 'IMAGE_METADATA_FORBIDDEN')
        pixels = image.tobytes()
        return pixels, {'format': image.format, 'dimensions': list(image.size), 'mode': image.mode,
                        'frames': getattr(image, 'n_frames', 1), 'opaque': True,
                        'metadataPresent': False, 'rgbBytes': len(pixels), 'rgbSha256': digest(pixels)}


def validate_png(path, pin):
    data = path.read_bytes()
    ensure(digest(data) == pin['newPngSha256'], 'PNG_SHA256_MISMATCH')
    ensure(len(data) == pin['newPngBytes'], 'PNG_BYTE_COUNT_MISMATCH')
    chunks = validate_png_container(data, pin['dimensions'])
    pixels, metadata = inspect_image(io.BytesIO(data), 'PNG', pin['dimensions'])
    ensure(digest(pixels) == pin['rgbSha256'], 'PNG_RGB_PIN_MISMATCH')
    ensure(len(pixels) == pin.get('rgbBytesCompared', RGB_BYTES), 'PNG_RGB_BYTE_COUNT_MISMATCH')
    metadata['chunks'] = chunks
    return pixels, metadata


def validate_webp(payload, expected_rgb, pin):
    ensure(len(payload) >= 26 and payload[:4] == b'RIFF' and payload[8:12] == b'WEBP', 'WEBP_RIFF_INVALID')
    ensure(int.from_bytes(payload[4:8], 'little') + 8 == len(payload), 'WEBP_RIFF_LENGTH_INVALID')
    ensure(payload[12:16] == b'VP8L', 'WEBP_VP8L_REQUIRED')
    length = int.from_bytes(payload[16:20], 'little')
    ensure(length >= 5 and 20 + length + length % 2 == len(payload), 'WEBP_SINGLE_CHUNK_REQUIRED')
    ensure(length % 2 == 0 or payload[-1] == 0, 'WEBP_PADDING_INVALID')
    ensure(payload[20] == 0x2f, 'WEBP_VP8L_SIGNATURE_INVALID')
    bits = int.from_bytes(payload[21:25], 'little')
    dimensions = [(bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1]
    ensure(dimensions == list(pin['dimensions']), 'WEBP_DIMENSIONS_INVALID')
    ensure(bits >> 28 == 0, 'WEBP_ALPHA_OR_VERSION_FORBIDDEN')
    pixels, metadata = inspect_image(io.BytesIO(payload), 'WEBP', pin['dimensions'],
                                     ('loop', 'background', 'timestamp', 'duration'))
    ensure(pixels == expected_rgb and digest(pixels) == pin['rgbSha256'], 'WEBP_RGB_MISMATCH')
    ensure(len(pixels) == pin.get('rgbBytesCompared', RGB_BYTES), 'WEBP_RGB_BYTE_COUNT_MISMATCH')
    metadata.update({'chunks': ['VP8L'], 'allRgbBytesEqual': True, 'rgbMatchesGoldenPin': True})
    return metadata


def load_fixed_inputs(root):
    ensure(sha(root / GOLD) == GOLD_SHA256, 'GOLD_EVIDENCE_SHA256_MISMATCH')
    gold = json.loads((root / GOLD).read_text())
    pins = {row['assetId']: row for row in gold['images']}
    ensure(set(pins) == set(ASSET_IDS), 'GOLD_ASSET_IDS_INVALID')
    loaded = {}
    for asset_id in ASSET_IDS:
        pin = pins[asset_id]
        ensure(pin['dimensions'] == list(DIMENSIONS) and pin['rgbBytesCompared'] == RGB_BYTES,
               'GOLD_GEOMETRY_INVALID')
        png = root / PNG_RUN / 'new' / f'{asset_id}.png'
        pixels, metadata = validate_png(png, pin)
        jpeg = root / PREFLIGHT / f'source-{asset_id}.jpg'
        ensure(sha(jpeg) == pin['existingJpegSha256'] and jpeg.stat().st_size == pin['existingJpegBytes'],
               'JPEG_REFERENCE_PIN_MISMATCH')
        with Image.open(jpeg) as image:
            ensure(image.format == 'JPEG' and image.mode == 'RGB'
                   and list(image.size) == pin['existingJpegDimensions'] == [1712, 2778],
                   'JPEG_REFERENCE_GEOMETRY_INVALID')
        loaded[asset_id] = (pin, png, pixels, metadata)
    return loaded


def encode_webp(pixels):
    # İlk plugin yüklemesinin dosya erişimleri encode süresine girmemelidir.
    Image.init()
    ensure('WEBP' in Image.SAVE, 'WEBP_ENCODER_UNAVAILABLE')
    image = Image.frombytes('RGB', DIMENSIONS, pixels)
    output = io.BytesIO()
    started = time.perf_counter_ns()
    image.save(output, format='WEBP', **ENCODER)
    elapsed = (time.perf_counter_ns() - started) / 1_000_000
    payload = output.getvalue()
    image.close()
    return payload, elapsed


def decode_samples(png, webp, expected_rgb):
    raw, by_format = [], {'PNG': [], 'WEBP': []}
    for index in range(5):
        order = (('PNG', png), ('WEBP', webp)) if index % 2 == 0 else (('WEBP', webp), ('PNG', png))
        for format_name, path in order:
            started = time.perf_counter_ns()
            with Image.open(path) as image:
                image.load()
                elapsed = (time.perf_counter_ns() - started) / 1_000_000
                pixels = image.tobytes()
            ensure(pixels == expected_rgb, 'TIMED_DECODE_RGB_MISMATCH')
            by_format[format_name].append(elapsed)
            raw.append({'round': index + 1, 'format': format_name, 'openLoadMillis': elapsed,
                        'rgbSha256': digest(pixels), 'allRgbBytesEqual': True})
    return {'samples': raw, 'medianMillis': {key: statistics.median(values) for key, values in by_format.items()},
            'rounds': 5, 'alternatingFirstFormat': True, 'scope': 'PILLOW_OPEN_AND_LOAD_ONLY'}


def sips_decode(source, output):
    ensure(not output.exists(), 'SIPS_OUTPUT_ALREADY_EXISTS')
    argv = ['/usr/bin/sips', '-s', 'format', 'png', str(source), '--out', str(output)]
    completed, elapsed = command(argv)
    record = {'command': argv, 'returnCode': completed.returncode, 'stdout': completed.stdout,
              'stderr': completed.stderr, 'startupDecodePngExportMillis': elapsed}
    ensure(completed.returncode == 0 and output.is_file(), 'SIPS_FULL_DECODE_FAILED:' + json_text(record))
    with Image.open(output) as image:
        image.load()
        ensure(image.format == 'PNG' and image.size == DIMENSIONS, 'SIPS_OUTPUT_GEOMETRY_INVALID')
        ensure(getattr(image, 'n_frames', 1) == 1 and image.mode in ('RGB', 'RGBA'), 'SIPS_OUTPUT_MODE_INVALID')
        if image.mode == 'RGBA':
            ensure(image.getchannel('A').getextrema() == (255, 255), 'SIPS_OUTPUT_NOT_OPAQUE')
        pixels = image.convert('RGB').tobytes()
        record.update({'outputSha256': sha(output), 'outputBytes': output.stat().st_size,
                       'outputMode': image.mode, 'dimensions': list(image.size),
                       'rgbBytes': len(pixels), 'rgbSha256': digest(pixels), 'opaque': True})
    return pixels, record


def measure_image(root, work, asset_id, loaded, prior_row):
    pin, png, pixels, png_metadata = loaded
    payload, encode_millis = encode_webp(pixels)
    webp_metadata = validate_webp(payload, pixels, pin)
    prior_payload = (root / prior_row['webp']['path']).read_bytes()
    ensure(digest(prior_payload) == prior_row['webp']['sha256'], 'INITIAL_WEBP_HASH_CHANGED')
    validate_webp(prior_payload, pixels, pin)
    ensure(payload == prior_payload, 'TIMING_CORRECTION_BITSTREAM_CHANGED')
    webp = work / f'{asset_id}.webp'
    with webp.open('xb') as output:
        output.write(payload)
    samples = decode_samples(png, webp, pixels)
    native_png, png_sips = sips_decode(png, work / f'{asset_id}-sips-from-png.png')
    native_webp, webp_sips = sips_decode(webp, work / f'{asset_id}-sips-from-webp.png')
    ensure(len(native_png) == len(native_webp) == RGB_BYTES and native_png == native_webp,
           'SIPS_FULL_RGB_MISMATCH')
    return {'assetId': asset_id, 'dimensions': list(DIMENSIONS), 'rgbBytesCompared': len(pixels),
            'rgbSha256': digest(pixels), 'allRgbBytesEqual': True, 'rgbMatchesGoldenPin': True,
            'png': {'path': str(png.relative_to(root)), 'bytes': pin['newPngBytes'],
                    'sha256': sha(png), 'validation': png_metadata},
            'webp': {'path': str(webp.relative_to(root)), 'bytes': len(payload), 'sha256': digest(payload),
                     'validation': webp_metadata, 'memoryEncodeMillis': encode_millis},
            'savedBytes': pin['newPngBytes'] - len(payload),
            'savedPercent': 100 * (1 - len(payload) / pin['newPngBytes']), 'pillowDecode': samples,
            'timingCorrection': {'previousWebpSha256': digest(prior_payload),
                                 'sameBitstreamAsPreviousRun': True, 'samePinnedRgbAsPreviousRun': True},
            'imageIoDecode': {'png': png_sips, 'webp': webp_sips, 'allRgbBytesEqualWithinImageIo': True,
                              'rgbBytesCompared': len(native_png), 'rgbSha256': digest(native_png)},
            'existingJpegReference': {'path': str(PREFLIGHT / f'source-{asset_id}.jpg'),
                                      'bytes': pin['existingJpegBytes'], 'sha256': pin['existingJpegSha256'],
                                      'dimensions': pin['existingJpegDimensions'], 'reencoded': False}}


def markdown(result):
    rows = result['images']
    lines = ['# Yerel kayıpsız format karşılaştırması', '',
             f"Sonuç: **{result['status']}**. Ölçüm: {result['measuredAtUtc']}. Üç mevcut PNG aynı geometride kayıpsız WebP ile karşılaştırıldı.", '',
             '## Aynı geometride PNG ↔ WebP', '',
             'Her dosya 822×1334, tek kare ve opaque RGB. Her birinde **3.289.644 RGB byte** altın pinle ve PNG ↔ WebP arasında tamamen eşleşti. RIFF yalnız bir `VP8L` chunk içeriyor; ICC/EXIF/XMP, alpha ve animasyon yok.', '',
             '| Görsel | PNG byte | WebP byte | Tasarruf byte | Tasarruf |',
             '|---|---:|---:|---:|---:|']
    for row in rows:
        lines.append(f"| {row['assetId']} | {row['png']['bytes']:,} | {row['webp']['bytes']:,} | {row['savedBytes']:,} | %{row['savedPercent']:.2f} |")
    lines += ['', '## Yerel süreler', '',
              'Encode süresi önceden yüklenmiş RGB’nin yalnız belleğe WebP encode işlemini kapsar. Pillow encoder kayıtları süre başlamadan yüklendi. Dosya okuma, RGB hazırlama, doğrulama ve diske yazma kapsam dışıdır. Pillow `open+load` için beş tur dönüşümlü sıra kullanıldı; tabloda medyan var. Ham on örnek JSON içindedir. Önbellek temizlenmedi; encode ısınması yapılmadı. Bu dar örnekler benchmark veya mobil performans kanıtı değildir.', '',
              '| Görsel | WebP belleğe encode ms | PNG open+load medyan ms | WebP open+load medyan ms | sips PNG ms | sips WebP ms |',
              '|---|---:|---:|---:|---:|---:|']
    for row in rows:
        native = row['imageIoDecode']
        med = row['pillowDecode']['medianMillis']
        lines.append(f"| {row['assetId']} | {row['webp']['memoryEncodeMillis']:.3f} | {med['PNG']:.3f} | {med['WEBP']:.3f} | {native['png']['startupDecodePngExportMillis']:.3f} | {native['webp']['startupDecodePngExportMillis']:.3f} |")
    lines += ['', '`/usr/bin/sips` her PNG ve WebP’yi ayrı, tam PNG çıktısına decode etti. ImageIO içindeki bütün RGB byte’ları eşleşti. sips süreleri süreç başlangıcı + decode + PNG export + süreç bitişini içerir; Pillow veya tarihî Java süreleriyle kıyaslanmaz. sips çıktıları Pillow ile yalnız RGB byte karşılaştırması için okundu; bağımsız kaynak decode işlemini ImageIO yaptı.', '',
              '## Ayrı JPEG byte referansı', '',
              'Aşağıdaki değişmeyen JPEG’ler **1712×2778** boyutundadır. Yeniden encode edilmedi. Geometri farklı olduğundan yalnız mevcut dosya boyutu referansıdır; format veya kalite karşılaştırması değildir.', '',
              '| Görsel | Mevcut JPEG byte | SHA256 |', '|---|---:|---|']
    for row in rows:
        jpeg = row['existingJpegReference']
        lines.append(f"| {row['assetId']} | {jpeg['bytes']:,} | `{jpeg['sha256']}` |")
    versions = result['versions']
    lines += ['', '## Tekrarlanabilirlik ve sınırlar', '',
              f"Python {versions['python']}; Pillow {versions['pillow']}; libwebp {versions['libwebp']}; {versions['sips']}; {versions['platform']}. Tek deney ayarı: `lossless=True, quality=100, method=6, exact=True`. Başka aday, resize veya kalite taraması yok.", '',
              f"Çalışma dizini: `{result['privateRun']}` (izin `0700`). WebP’ler ve sips PNG çıktıları bu özel dizindedir. Public JSON yalnız hash, parametre, sürüm ve ölçüm kaydı içerir; korunan dosyaların içeriği yayımlanmaz.", '',
              f"Önceki PNG evidence, tüm eski PNG run dizinleri, #94 dizini, preflight ve `.memlog.md` dahil {result['preservation']['fileCount']} korunan dosyanın önce/sonra hash listeleri eşit. Altın verification SHA256: `{GOLD_SHA256}`. Başlangıçta var olan `.memlog.md` içeriği korunur.", '',
              f"Ölçüm düzeltmesi: `{result['measurementCorrection']['previousRun']}` ilk deneme kaydı değiştirilmeden korundu. İlk `Image.save` çağrısındaki plugin yüklemesi encode süresine girebildiği için encoder kayıtları zamanlayıcıdan önce açıkça yüklendi ve yeni özel run oluşturuldu. İlk public JSON/Markdown, ilk run içine `initial-public-verification.*` adıyla taşındı; ham kayıtlar ve çıktılar duruyor. Tek adayın parametreleri aynı kaldı. Her yeni WebP önceki run ile byte düzeyinde aynı; iki run da aynı altın RGB pinlerini karşılıyor. İlk süreler sonuç tablosuna alınmadı.", '',
              'macOS `sandbox-exec` `(deny network*)` kuralı bütün ölçüm sürecini ve sips dahil alt süreçleri kapsadı. Gerçek loopback bind `EPERM` ile reddedildi; native `/usr/bin/nc` de devraldığı ağ yasağında aynı reddi verdi. Paket kurulmadı; bulut, DB, giriş veya cihaz oturumu kullanılmadı. Her alt süreç için en çok 30 saniye; native ağ kontrolünde 5 saniye; tüm ölçümde 240 saniye sınırı var. Dosyalar exclusive oluşturulur; mevcut public kanıtın üzerine yazılmaz.', '',
              '**Açık kapılar:** P0 ham kanıt sınırı ve P4b açık kalır. Yerel byte gerçek transfer değildir. RGB eşitliği fiziksel kalite onayı değildir. Üretim profili veya uygulama codec kararı alınmadı; Java/Flutter/CMS, altyapı ve dağıtım değişmedi.', '']
    return '\n'.join(lines)


def alarm_timeout(signum, frame):
    raise ProbeError('WORKER_DEADLINE_EXCEEDED')


def worker():
    network = require_network_denied()
    root = Path(__file__).resolve().parents[4]
    evidence = root / EVIDENCE
    ensure_public_absent(evidence)
    native_guard = native_network_proof()
    protected_roots = [root / 'be/docs/evidence/png-lossless-compression', root / OLD_RUN,
                       root / PREFLIGHT, root / MEMLOG]
    protected_roots += sorted((root / 'be/var').glob('png-lossless*'))
    protected_roots += sorted((root / 'be/var').glob('local-lossless-formats-*'))
    before = snapshot(root, protected_roots)
    loaded = load_fixed_inputs(root)
    prior_verification = root / INITIAL_TIMING_RUN / 'verification.json'
    prior = json.loads(prior_verification.read_text())
    ensure(prior['status'] == 'PASS' and prior['encoder'] == {'format': 'WEBP', **ENCODER},
           'INITIAL_TIMING_RUN_INVALID')
    prior_rows = {row['assetId']: row for row in prior['images']}
    ensure(set(prior_rows) == set(ASSET_IDS), 'INITIAL_TIMING_RUN_IDS_INVALID')
    work = Path(tempfile.mkdtemp(prefix='local-lossless-formats-', dir=root / 'be/var'))
    ensure(work.stat().st_mode & 0o777 == 0o700, 'PRIVATE_RUN_MODE_INVALID')
    exclusive(work / 'run.json', json_text({'status': 'RUNNING', 'encoder': ENCODER,
                                           'protectedSha256Before': before}))
    try:
        rows = []
        for asset_id in ASSET_IDS:
            row = measure_image(root, work, asset_id, loaded[asset_id], prior_rows[asset_id])
            rows.append(row)
            exclusive(work / f'{asset_id}-measurement.json', json_text(row))
        after = snapshot(root, protected_roots)
        verify_preservation(before, after)
        ensure_public_absent(evidence)
        version, _ = command(['/usr/bin/sips', '--version'])
        ensure(version.returncode == 0, 'SIPS_VERSION_FAILED')
        result = {'status': 'PASS', 'measuredAtUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
                  'baselineCommit': 'ac6c9720a60d908f433bc3e46c75eb158545f5d7',
                  'goldEvidenceSha256': GOLD_SHA256, 'privateRun': str(work.relative_to(root)),
                  'probeSha256': sha(Path(__file__)),
                  'testProbeSha256': sha(evidence / 'test_probe.py'),
                  'measurementCorrection': {'previousRun': str(INITIAL_TIMING_RUN),
                                            'previousVerificationSha256': sha(prior_verification),
                                            'reason': 'Pillow plugin yüklemesi zamanlayıcıdan önce tamamlandı.',
                                            'previousRunUnchanged': True, 'sameEncoderParameters': True,
                                            'sameWebpBitstreams': True, 'samePinnedRgb': True,
                                            'initialPublicEvidencePreserved': {
                                                name: sha(root / INITIAL_TIMING_RUN / name)
                                                for name in ('initial-public-verification.json',
                                                             'initial-public-verification.md')}},
                  'encoder': {'format': 'WEBP', **ENCODER},
                  'versions': {'python': platform.python_version(), 'pythonExecutable': sys.executable,
                               'pillow': pillow_version, 'libwebp': features.version('webp'),
                               'sips': version.stdout.strip(), 'sipsSha256': sha(Path('/usr/bin/sips')),
                               'platform': platform.platform()},
                  'networkGuard': {'mechanism': 'macOS sandbox-exec', 'policy': POLICY,
                                   'scope': 'WORKER_AND_ALL_INHERITED_NATIVE_CHILDREN',
                                   'python': network, 'nativeChild': native_guard},
                  'images': rows, 'preservation': {'unchanged': True, 'fileCount': len(before),
                                                 'beforeSha256': before, 'afterSha256': after},
                  'limits': ['LOCAL_FILES_ONLY_NO_TRANSFER_OR_MOBILE_PERFORMANCE_CLAIM',
                             'ONE_FIXED_LOSSLESS_WEBP_CANDIDATE_NO_RESIZE_OR_QUALITY_SEARCH',
                             'ENCODE_LOADED_RGB_TO_MEMORY_ONLY_NO_FILE_IO',
                             'PILLOW_OPEN_LOAD_FIVE_ALTERNATING_ROUNDS_NO_CACHE_FLUSH',
                             'SIPS_TIME_INCLUDES_STARTUP_DECODE_PNG_EXPORT_AND_EXIT',
                             'NO_HISTORICAL_JAVA_TIME_COMPARISON',
                             'EXISTING_JPEG_DIFFERENT_GEOMETRY_BYTE_REFERENCE_ONLY',
                             'NO_PHYSICAL_QUALITY_APPROVAL_P0_RAW_EVIDENCE_AND_P4B_REMAIN_OPEN'],
                  'timeoutsSeconds': {'childProcess': PROCESS_TIMEOUT_SECONDS, 'nativeBindProbe': 5,
                                      'worker': WORKER_TIMEOUT_SECONDS}}
        exclusive(work / 'verification.json', json_text(result))
        publish(evidence, result)
        print(json_text({'status': 'PASS', 'privateRun': result['privateRun'],
                         'images': [{'assetId': row['assetId'], 'pngBytes': row['png']['bytes'],
                                     'webpBytes': row['webp']['bytes'], 'savedPercent': row['savedPercent']}
                                    for row in rows]}), end='')
    except BaseException as failure:
        failure_record = {'status': 'FAIL', 'type': type(failure).__name__, 'error': str(failure)}
        try:
            after = snapshot(root, protected_roots)
            failure_record['protectedArtifactsUnchanged'] = before == after
            failure_record['protectedSha256After'] = after
        except Exception as preservation_failure:
            failure_record['preservationCheckError'] = str(preservation_failure)
        exclusive(work / 'failure.json', json_text(failure_record))
        raise


def main():
    if not __debug__:
        raise ProbeError('PYTHON_OPTIMIZATION_NOT_SUPPORTED')
    if sys.argv[1:] == ['--worker']:
        signal.signal(signal.SIGALRM, alarm_timeout)
        signal.alarm(WORKER_TIMEOUT_SECONDS)
        try:
            worker()
        finally:
            signal.alarm(0)
        return
    ensure(not sys.argv[1:], 'NO_CUSTOM_INPUTS_OR_OPTIONS_ALLOWED')
    argv = ['/usr/bin/sandbox-exec', '-p', POLICY, sys.executable, '-B', str(Path(__file__).resolve()), '--worker']
    completed = subprocess.run(argv, capture_output=True, text=True, timeout=WORKER_TIMEOUT_SECONDS + 5,
                               env=dict(os.environ, PYTHONDONTWRITEBYTECODE='1'))
    if completed.returncode:
        raise ProbeError('SANDBOXED_PROBE_FAILED:\n' + completed.stdout + completed.stderr)
    print(completed.stdout, end='')


if __name__ == '__main__':
    main()
