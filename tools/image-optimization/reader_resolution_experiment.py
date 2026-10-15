#!/usr/bin/env python3
"""Private, finite reader-resolution admission, preparation and evidence checks.

No reservation, credential acquisition, installation, production request or human
quality acceptance is performed by this module. Original 1.6 evidence is read-only.
"""
import argparse
from dataclasses import dataclass, field
from hashlib import sha256
from io import BytesIO
import math
from pathlib import Path
import secrets
import statistics
import struct
import sys
import time
import zipfile
import zlib

from PIL import Image, __version__ as PILLOW_VERSION
from native_entry_config import (EntryError, HASH, MAX_FILE, MAX_PIXELS, READER_BATCHES,
    READER_CASES, SCOPE as FIXTURE_SCOPE, exact_keys, finite_number, frozen_json,
    no_symlink, private_file, private_output, require, strict_json,
    validate_reader_entries, write_private)

SCOPE = 'image-optimization-reader-resolution-r1'
MAGIC = b'tellpal-p0a-rgba8-srgb-straight-v1\0'
PINS = {
    'downloads/cms-content71-source-20261010T074854692644Z.zip': '146b2d9f6fb5dab606513441b841b6874743058d79d17bbd62ffcab4e456a436',
    'acquisition/source-zip-content71-20261010T074854692644Z.json': 'c4173503a8e49ca4f500f208aa877e9da9ff5e0c3d9c35ac375a44e529af67e5',
    'preparation-v7/preparation-receipt.json': '8c4dd77fb14f0d1448cf98ef623098b3da5b01743afc83c60b384085481b1f8d',
}
GEOMETRY_FIELDS = ('observedSlotWidth', 'observedSlotHeight', 'observedDpr',
    'textScaleFactor', 'paddingLeft', 'paddingTop', 'paddingRight', 'paddingBottom',
    'viewInsetLeft', 'viewInsetTop', 'viewInsetRight', 'viewInsetBottom', 'orientation')
BASE_TRY = 73.73
TRY_PER_USD = 48.98225000067947
PROTECTED_STREAMING_USD = 5.85
RESERVE_USD = .50
PHASES = ('baseline', 'reader', 'natural_retention', 'owned_eviction', 'complete')
SOURCE_SIZE = (1712, 2778)
HISTORICAL_PRIMARY_BATCHES = frozenset(('reader_resolution_a_r1',
    'reader_resolution_b_r1', 'reader_resolution_c_r1'))
SELECTED075_BATCH = 'reader_resolution_075_r1'


def digest(data):
    return sha256(data).hexdigest()


def geometry(value):
    exact_keys(value, GEOMETRY_FIELDS)
    for key in GEOMETRY_FIELDS[:-1]:
        require(finite_number(value[key]) and value[key] >= 0, 'reader_geometry_rejected')
    require(all(value[key] > 0 for key in GEOMETRY_FIELDS[:4]) and
        value['orientation'] in ('portrait', 'landscape'), 'reader_geometry_rejected')
    return {key: value[key] for key in GEOMETRY_FIELDS}


def require_same_geometry(expected, observed):
    require(geometry(expected) == geometry(observed), 'reader_geometry_changed')
    return digest(frozen_json(geometry(expected)))


def contain_dimensions(width, height, slot_width, slot_height, dpr, factor=1.0):
    require(all(type(number) is int and 1 <= number <= 8192 for number in (width, height)) and
        width * height <= MAX_PIXELS, 'reader_source_geometry_rejected')
    require(all(finite_number(number) and number > 0 for number in (slot_width, slot_height, dpr)) and
        type(factor) is float and factor in (1.0, .75), 'reader_demand_rejected')
    products = (slot_width * dpr, slot_height * dpr)
    require(all(finite_number(number) for number in products), 'reader_demand_rejected')
    physical = tuple(math.ceil(number) for number in products)
    demand_scale = min(physical[0] / width, physical[1] / height)
    scale = min(1.0, demand_scale * factor)
    return {'width': min(width, math.ceil(width * scale)),
        'height': min(height, math.ceil(height * scale)),
        'demandWidth': physical[0], 'demandHeight': physical[1],
        'sourcePixelsSufficient': demand_scale <= 1, 'factor': factor,
        'fit': 'contain', 'upscaled': False, 'cropped': False}


@dataclass(frozen=True, repr=False)
class SourcePage:
    page_number: int
    asset_id: int | None
    source_sha256: str
    reference_sha256: str
    width: int
    height: int
    pixels: bytes = field(repr=False)
    content_id: int = 71
    language: str | None = None


def canonical_pixels(data, width, height):
    require(type(width) is int and type(height) is int and 0 < width <= 8192 and 0 < height <= 8192 and
        width * height <= MAX_PIXELS and data.startswith(MAGIC) and
        len(data) == len(MAGIC) + 8 + width * height * 4, 'reader_reference_rejected')
    require(struct.unpack('>II', data[len(MAGIC):len(MAGIC) + 8]) == (width, height), 'reader_reference_rejected')
    pixels = data[len(MAGIC) + 8:]
    require(all(alpha == 255 for alpha in memoryview(pixels)[3::4]), 'reader_alpha_rejected')
    return pixels


def admit_sources(root):
    """Bind retained ZIP, CMS order, originals and v7 normalized pixels without writes."""
    root = no_symlink(root)
    inputs = {}
    for name, expected in PINS.items():
        content = private_file(root / name, 100 * 1024 * 1024, exact_mode=False)
        require(digest(content) == expected, 'reader_source_pin_rejected')
        inputs[name] = content
    acquisition = strict_json(inputs[next(name for name in PINS if name.startswith('acquisition/'))], 1024 * 1024)
    preparation = strict_json(inputs['preparation-v7/preparation-receipt.json'], 1024 * 1024)
    require(acquisition.get('content_id') == 71 and acquisition.get('archive_sha256') ==
        PINS[next(name for name in PINS if name.startswith('downloads/'))] and
        preparation.get('schema') == 'p0a-normalization-preparation-v3' and
        not preparation.get('source_failures') and
        all(check.get('result') == 'pass' for check in preparation.get('checks', [])) and
        len(preparation.get('checks', [])) >= 5, 'reader_provenance_rejected')
    records = acquisition.get('records')
    require(type(records) is list and len(records) == 19, 'reader_source_order_rejected')
    pages = [row for row in records if row.get('role') == 'story_page_source']
    require([row.get('page_number') for row in pages] == list(range(1, 19)) and
        len({row.get('sha256') for row in pages[:6]}) == 6, 'reader_source_order_rejected')
    expected_sources = {row.get('sha256') for row in records}
    require(len(expected_sources) == acquisition.get('unique_entry_hashes') and all(type(value) is str and HASH.fullmatch(value)
        for value in expected_sources), 'reader_source_hash_rejected')
    archive = inputs[next(name for name in PINS if name.startswith('downloads/'))]
    require(zip_source_hashes(archive) == expected_sources | {acquisition.get('manifest_sha256')},
        'reader_archive_binding_rejected')
    references = {row.get('source_hash'): row for row in preparation.get('references', [])}
    admitted = []
    for row in pages:
        source_hash = row['sha256']
        require(row.get('content_id') == 71 and row.get('language') is None and
            row.get('source_owner_role') == 'story_page' and type(row.get('asset_id')) is int and
            row.get('cms_binding_verification', {}).get('status') == 'confirmed', 'reader_source_binding_rejected')
        require(digest(private_file(root / 'sources' / (source_hash + '.source'), MAX_FILE,
            exact_mode=False)) == source_hash, 'reader_original_changed')
        ref = references.get(source_hash, {})
        require(ref.get('id') == source_hash + '-rgba8-srgb-v1' and
            (ref.get('width'), ref.get('height')) == SOURCE_SIZE and
            (row.get('metadata', {}).get('width'), row.get('metadata', {}).get('height')) == SOURCE_SIZE,
            'reader_reference_binding_rejected')
        raw = private_file(root / 'preparation-v7/normalized-references' / (ref['id'] + '.pixels'),
            MAX_PIXELS * 4 + 128, exact_mode=False)
        require(digest(raw) == ref.get('pixel_sha256') and len(raw) == ref.get('bytes'), 'reader_reference_changed')
        pixels = canonical_pixels(raw, ref['width'], ref['height'])
        if row['page_number'] <= 6:
            admitted.append(SourcePage(row['page_number'], row['asset_id'], source_hash,
                ref['pixel_sha256'], ref['width'], ref['height'], pixels))
    return tuple(admitted), {'schemaVersion': 1, 'scope': SCOPE, 'inputSha256': dict(PINS),
        'pageCount': 18, 'selectedPageNumbers': list(range(1, 7)), 'qaIdsSynthetic': True,
        'qaRevision': 'reader-resolution-r1', 'publication': None, 'audio': None,
        'pages': [{'pageNumber': page.page_number, 'assetId': page.asset_id,
            'sourceSha256': page.source_sha256, 'referenceSha256': page.reference_sha256,
            'width': page.width, 'height': page.height} for page in admitted]}


def zip_source_hashes(data):
    try:
        with zipfile.ZipFile(BytesIO(data)) as archive:
            infos = archive.infolist()
            require(1 <= len(infos) <= 64, 'reader_archive_rejected')
            hashes, total = set(), 0
            for info in infos:
                if info.is_dir():
                    continue
                require(not info.flag_bits & 1 and info.file_size <= MAX_FILE and
                    ((info.external_attr >> 16) & 0o170000) in (0, 0o100000), 'reader_archive_rejected')
                total += info.file_size
                require(total <= 100 * 1024 * 1024, 'reader_archive_rejected')
                content = archive.read(info)
                require(len(content) == info.file_size, 'reader_archive_rejected')
                hashes.add(digest(content))
            return hashes
    except EntryError:
        raise
    except (ValueError, OSError, zipfile.BadZipFile, RuntimeError):
        raise EntryError('reader_archive_rejected') from None


def admit_quality_sources(corpus_root, retained_inputs_path):
    """Reuse the two original TR pixel-equal PNG references and their v7 bindings."""
    retained_data = private_file(retained_inputs_path)
    retained = strict_json(retained_data)
    exact_keys(retained, ('scope', 'originalManifestPath', 'originalManifestSha256', 'entries',
        'normalization', 'productionActions'))
    original_data = private_file(retained['originalManifestPath'], 1024 * 1024)
    require(digest(original_data) == retained['originalManifestSha256'], 'reader_quality_original_changed')
    original = strict_json(original_data, 1024 * 1024)
    require(original.get('scope') == FIXTURE_SCOPE and original.get('batchId') == 'native_quality_r1' and
        type(original.get('entries')) is list and type(retained['entries']) is list and
        len(retained['entries']) == 2, 'reader_quality_provenance_rejected')
    preparation_data = private_file(Path(corpus_root) / 'preparation-v7/preparation-receipt.json', 1024 * 1024)
    require(digest(preparation_data) == PINS['preparation-v7/preparation-receipt.json'], 'reader_source_pin_rejected')
    preparation = strict_json(preparation_data, 1024 * 1024)
    pages = []
    for content_id, entry in zip((71, 19), retained['entries']):
        require(entry in original['entries'] and entry.get('caseId') == f'c{content_id}_png_reference_r1' and
            entry.get('purposeAcceptanceReceipt') == 'native_quality_r1' and entry.get('mime') == 'image/png',
            'reader_quality_source_rejected')
        refs = [ref for ref in preparation.get('references', []) if ref.get('source_hash') == entry['sourceSha256']]
        require(len(refs) == 1 and refs[0].get('pixel_sha256') == entry['referenceSha256'] and
            (refs[0].get('width'), refs[0].get('height')) == (entry.get('width'), entry.get('height')),
            'reader_quality_reference_rejected')
        raw = private_file(Path(corpus_root) / 'preparation-v7/normalized-references' /
            (entry['sourceSha256'] + '-rgba8-srgb-v1.pixels'), MAX_PIXELS * 4 + 128, exact_mode=False)
        require(digest(raw) == entry['referenceSha256'], 'reader_reference_changed')
        pixels = canonical_pixels(raw, entry['width'], entry['height'])
        content = private_file(entry['filePath'], MAX_FILE, exact_mode=False)
        require(digest(content) == entry['outputSha256'] and len(content) == entry['byteSize'],
            'reader_quality_png_changed')
        expected_rgb = Image.frombytes('RGBA', (entry['width'], entry['height']), pixels).convert('RGB').tobytes()
        verify_png(content, (entry['width'], entry['height']), expected_rgb)
        pages.append(SourcePage(1, None, entry['sourceSha256'], entry['referenceSha256'],
            entry['width'], entry['height'], pixels, content_id, 'tr'))
    return tuple(pages), {'scope': SCOPE, 'retainedInputsSha256': digest(retained_data),
        'originalManifestSha256': digest(original_data), 'v7PreparationSha256': digest(preparation_data),
        'contentIds': [71, 19], 'pageNumbers': [1, 1], 'language': 'tr',
        'referencePixelEquality': 'verified', 'humanQualityAccepted': None}


def encode_page(page, dimensions):
    require(PILLOW_VERSION == '12.0.0', 'reader_encoder_version_rejected')
    original = Image.frombytes('RGBA', (page.width, page.height), page.pixels).convert('RGB')
    size = (dimensions['width'], dimensions['height'])
    image = original if size == original.size else original.resize(size, Image.Resampling.LANCZOS, reducing_gap=None)
    output = BytesIO()
    image.save(output, format='PNG', compress_level=9, optimize=False)
    content = output.getvalue()
    require(len(content) <= MAX_FILE, 'reader_encoded_size_rejected')
    verify_png(content, size, image.tobytes())
    return content


def verify_png(content, size, expected_pixels):
    try:
        with Image.open(BytesIO(content)) as image:
            require(image.format == 'PNG' and image.size == size and getattr(image, 'n_frames', 1) == 1,
                'reader_encoded_geometry_rejected')
            image.verify()
        with Image.open(BytesIO(content)) as image:
            image.load()
            require(not image.info and image.mode == 'RGB' and image.tobytes() == expected_pixels,
                'reader_encoded_pixels_rejected')
    except EntryError:
        raise
    except Exception:
        raise EntryError('reader_encoded_decode_rejected') from None


def prepare_variants(pages, admission, observed_geometry, output, *, batches=None, quality_pages=None):
    """Every candidate is resized directly from immutable v7, never another candidate."""
    require(type(pages) is tuple and len(pages) == 6 and
        tuple(page.page_number for page in pages) == tuple(range(1, 7)), 'reader_source_order_rejected')
    selected = HISTORICAL_PRIMARY_BATCHES if batches is None else frozenset(batches)
    require(selected and selected <= READER_BATCHES, 'reader_purpose_rejected')
    require(observed_geometry is not None or selected == {'reader_resolution_a_r1'}, 'reader_geometry_unavailable')
    frozen = geometry(observed_geometry) if observed_geometry is not None else None
    if any(batch.startswith('reader_quality_') for batch in selected):
        require(type(quality_pages) is tuple and len(quality_pages) == 2 and
            tuple(page.content_id for page in quality_pages) == (71, 19) and
            all(page.language == 'tr' and page.page_number == 1 for page in quality_pages),
            'reader_lettered_quality_sources_required')
    output = private_output(output, [], create=True)
    write_private(output / 'source-admission.json', admission)
    manifests, variants = {}, []
    for batch in sorted(selected):
        entries = []
        is_full = batch in ('reader_resolution_a_r1', 'reader_resolution_b_r1', 'reader_quality_full_r1')
        factor = .75 if batch in ('reader_quality_075_r1', SELECTED075_BATCH) else 1.0
        selected_pages = quality_pages if batch.startswith('reader_quality_') else pages
        for page, case_id in zip(selected_pages, READER_CASES):
            demand = ({'width': page.width, 'height': page.height, 'sourcePixelsSufficient': None}
                if frozen is None else contain_dimensions(page.width, page.height,
                    frozen['observedSlotWidth'], frozen['observedSlotHeight'], frozen['observedDpr'], factor))
            dimensions = {'width': page.width, 'height': page.height} if is_full else {
                key: demand[key] for key in ('width', 'height')}
            content = encode_page(page, dimensions)
            filename = batch + '-' + case_id + '.png'
            write_private(output / filename, content)
            discover = batch == 'reader_resolution_a_r1'
            entries.append({'caseId': case_id, 'role': 'isolated_raster_fixture',
                'arm': 'control' if is_full else 'candidate', 'sourceSha256': page.source_sha256,
                'referenceSha256': page.reference_sha256, 'outputSha256': digest(content),
                'mime': 'image/png', 'byteSize': len(content), **dimensions,
                'encoderRevision': 'reader_v7_png_pillow12_r1' if is_full else 'reader_lanczos_pillow12_r1',
                'profileRevision': 'reader_discover_r1' if discover else 'reader_frozen_r1',
                'filePath': str(output / filename), 'logicalSlotWidth': 1 if discover else frozen['observedSlotWidth'],
                'logicalSlotHeight': 1 if discover else frozen['observedSlotHeight'],
                'dpr': 1 if discover else frozen['observedDpr'], 'fit': 'contain', 'alignmentX': 0,
                'alignmentY': 0, 'purposeAcceptanceReceipt': batch})
            # Demand is independent of encoded dimensions and actual decoder observations.
            variants.append({'batchId': batch, 'caseId': case_id, 'demand': demand,
                'encodedWidth': dimensions['width'], 'encodedHeight': dimensions['height']})
        validate_reader_entries(batch, entries)
        manifest = {'schemaVersion': 1, 'scope': FIXTURE_SCOPE, 'batchId': batch,
            'corpusSha256': digest(frozen_json(admission)),
            'referenceSha256': digest(frozen_json([page.reference_sha256 for page in selected_pages])),
            'sourceSha256': digest(frozen_json([page.source_sha256 for page in selected_pages])), 'entries': entries}
        path = output / (batch + '-manifest.json')
        write_private(path, manifest)
        manifests[batch] = {'path': str(path), 'sha256': digest(frozen_json(manifest) + b'\n')}
    receipt = {'schemaVersion': 1, 'scope': SCOPE, 'geometry': frozen,
        'geometrySha256': digest(frozen_json(frozen)) if frozen is not None else None,
        'pillow': PILLOW_VERSION, 'resampler': 'direct_v7_LANCZOS_reducing_gap_none',
        'preparationToolSha256': digest(Path(__file__).read_bytes()),
        'runtime': {'python': sys.version.split()[0], 'pillow': PILLOW_VERSION,
            'zlibCompile': zlib.ZLIB_VERSION, 'zlibRuntime': zlib.ZLIB_RUNTIME_VERSION},
        'pngSettings': {'mode': 'RGB', 'compressLevel': 9, 'optimize': False,
            'resampler': 'LANCZOS', 'reducingGap': None, 'input': 'direct_v7_rgba8_srgb_pixels'},
        'manifests': manifests, 'variants': variants, 'qualityAccepted': None,
        'audio': None, 'publication': None}
    write_private(output / 'preparation-receipt.json', receipt)
    return receipt


def closed_root_minutes(seconds):
    require(finite_number(seconds) and seconds >= 0, 'reader_root_duration_rejected')
    return math.ceil((seconds + 1) / 60)


def budget_before_create(snapshot, *, now=None):
    """Separate two-window gate; retained debt, failures and debit receipts never reset."""
    now = time.time() if now is None else now
    exact_keys(snapshot, ('observedAt', 'catalogObservedAt', 'usageObservedAt', 'inventoryObservedAt',
        'catalogSha256', 'usageSha256', 'inventorySha256', 'ownedReceiptsSha256',
        'ownedReceiptsVerified', 'ownershipUnknown', 'cleanupUnknown', 'activeOrPending',
        'oldUpperMinutesRetained', 'originalClosedRootMinutes', 'originalReservationCount',
        'reportedMonthlyUsageMinutes', 'historicalMaximumQuotaMinutes', 'minuteUsd',
        'conversionTryPerUsd', 'nonDeviceTry', 'nonDeviceCostVerified', 'nonDeviceReceiptSha256',
        'noCostTierMinutes', 'maximumNewWindows', 'windows'))
    for key in ('observedAt', 'catalogObservedAt', 'usageObservedAt', 'inventoryObservedAt'):
        require(finite_number(snapshot[key]) and 0 <= now - snapshot[key] <= 300, 'reader_budget_snapshot_stale')
    for key in ('catalogSha256', 'usageSha256', 'inventorySha256', 'ownedReceiptsSha256', 'nonDeviceReceiptSha256'):
        require(type(snapshot[key]) is str and HASH.fullmatch(snapshot[key]), 'reader_budget_receipt_unverified')
    require(snapshot['ownedReceiptsVerified'] is True and snapshot['ownershipUnknown'] is False and
        snapshot['cleanupUnknown'] is False and type(snapshot['activeOrPending']) is int and
        snapshot['activeOrPending'] == 0 and snapshot['nonDeviceCostVerified'] is True, 'reader_budget_ownership_unknown')
    require(type(snapshot['maximumNewWindows']) is int and snapshot['maximumNewWindows'] == 2 and
        type(snapshot['originalReservationCount']) is int and snapshot['originalReservationCount'] == 13 and
        type(snapshot['oldUpperMinutesRetained']) is int and snapshot['oldUpperMinutesRetained'] >= 40 and
        type(snapshot['originalClosedRootMinutes']) is int and snapshot['originalClosedRootMinutes'] >= 29,
        'reader_budget_policy_rejected')
    require(type(snapshot['windows']) is list and len(snapshot['windows']) < 2, 'reader_window_limit_refused')
    reconciled, seen = [], set()
    for window in snapshot['windows']:
        exact_keys(window, ('ordinal', 'debitReceiptSha256', 'creationReceiptSha256', 'cleanupReceiptSha256',
            'status', 'debitedMinutes', 'wholeRootSeconds'))
        require(type(window['ordinal']) is int and window['ordinal'] == len(seen) + 1 and
            type(window['debitedMinutes']) is int and window['debitedMinutes'] == 6 and window['status'] == 'finished',
            'reader_prior_window_unsettled')
        for key in ('debitReceiptSha256', 'creationReceiptSha256', 'cleanupReceiptSha256'):
            require(type(window[key]) is str and HASH.fullmatch(window[key]), 'reader_budget_receipt_unverified')
        require(window['debitReceiptSha256'] not in seen, 'reader_debit_replayed')
        seen.add(window['debitReceiptSha256'])
        reconciled.append(closed_root_minutes(window['wholeRootSeconds']))
    require(snapshot['minuteUsd'] == .15 and snapshot['noCostTierMinutes'] == 30 and
        finite_number(snapshot['conversionTryPerUsd']) and snapshot['conversionTryPerUsd'] > 0 and
        finite_number(snapshot['nonDeviceTry']) and snapshot['nonDeviceTry'] >= BASE_TRY,
        'reader_budget_catalog_rejected')
    for key in ('reportedMonthlyUsageMinutes', 'historicalMaximumQuotaMinutes'):
        require(type(snapshot[key]) is int and snapshot[key] >= 0, 'reader_budget_usage_rejected')
    prior_minutes = snapshot['oldUpperMinutesRetained'] + snapshot['originalClosedRootMinutes']
    protected = max(PROTECTED_STREAMING_USD, max(0, prior_minutes - 30) * .15)
    extension_debit = (len(snapshot['windows']) + 1) * 6
    extension_charge = sum(max(6, value) for value in reconciled) + 6
    fresh_total = max(snapshot['reportedMonthlyUsageMinutes'], snapshot['historicalMaximumQuotaMinutes'])
    streaming_upper = max(protected + extension_charge * .15, max(0, fresh_total + 6 - 30) * .15)
    conversion = min(TRY_PER_USD, snapshot['conversionTryPerUsd'])
    upper = snapshot['nonDeviceTry'] / conversion + streaming_upper + RESERVE_USD
    require(upper < 10, 'reader_total_budget_refused')
    return {'schemaVersion': 1, 'scope': SCOPE, 'ordinal': len(snapshot['windows']) + 1,
        'pendingDebitMinutes': 6, 'pendingDebitUsd': .90, 'totalExtensionDebitedMinutes': extension_debit,
        'chargedExtensionUpperMinutes': extension_charge,
        'closedRootCeilMinutes': reconciled, 'protectedStreamingUpperUsd': protected,
        'projectedStreamingUpperUsd': streaming_upper, 'freshMonthlyUsagePlusPendingMinutes': fresh_total + 6,
        'nonDeviceUpperUsd': snapshot['nonDeviceTry'] / conversion, 'reserveUsd': RESERVE_USD,
        'projectedAllServicesUpperUsd': upper, 'maximumNewWindows': 2,
        'rootDeadlineSeconds': 300, 'cancelAtSeconds': 280, 'latestEntrySeconds': 105,
        'requiredRemainingSecondsExclusive': 195, 'snapshotSha256': digest(frozen_json(snapshot)),
        'invoice': 'unverified', 'providerHardCap': False}


def debit_before_create(snapshot, output, *, now=None):
    decision = budget_before_create(snapshot, now=now)
    # Exclusive private receipt creation must complete before any caller creates a root.
    write_private(Path(output), decision)
    return decision


def entry_window_guard(root_elapsed_seconds, remaining_seconds):
    require(all(finite_number(number) for number in (root_elapsed_seconds, remaining_seconds)) and
        0 <= root_elapsed_seconds < 105 and remaining_seconds > 195, 'reader_entry_window_refused')


def reader_binding(manifest, envelope, frozen_geometry=None):
    from critical_ios_guard_pilot import digest_fields, dart_double, DOUBLE_FIELDS
    require(validate_reader_entries(envelope['batchId'], manifest.entries), 'reader_purpose_rejected')
    require(type(envelope['generation']) is int and envelope['generation'] >= 0, 'reader_binding_rejected')
    namespace = digest_fields([FIXTURE_SCOPE, envelope['batchId'], envelope['sessionId'], envelope['generation'],
        envelope['accessAlias'], *[value for item in manifest.entries for value in
            (item['role'], item['sourceSha256'], item['profileRevision'])]])
    full = envelope['batchId'] in ('reader_resolution_a_r1', 'reader_quality_full_r1')
    fields = ('caseId', 'role', 'arm', 'sourceSha256', 'referenceSha256', 'outputSha256', 'mime', 'byteSize',
        'width', 'height', 'encoderRevision', 'profileRevision', 'logicalSlotWidth', 'logicalSlotHeight',
        'dpr', 'fit', 'alignmentX', 'alignmentY', 'purposeAcceptanceReceipt')
    providers = {}
    for entry in manifest.entries:
        dimensions = contain_dimensions(entry['width'], entry['height'], entry['logicalSlotWidth'],
            entry['logicalSlotHeight'], entry['dpr'])
        provider = digest_fields([namespace, *[dart_double(entry[key]) if key in DOUBLE_FIELDS else entry[key]
            for key in fields], dimensions['width'], dimensions['height'], 'full' if full else 'bounded'])
        providers[entry['caseId']] = {'expectedProviderSha256': provider,
            'expectedAttemptSha256': digest((provider + '::library-transfer::reader_session').encode()),
            'expectedDecodeWidth': entry['width'] if full else dimensions['width'],
            'expectedDecodeHeight': entry['height'] if full else dimensions['height']}
    return {'batchId': envelope['batchId'], 'generation': envelope['generation'],
        'sessionSha256': digest(envelope['sessionId'].encode()), 'providers': providers,
        'geometry': geometry(frozen_geometry) if frozen_geometry is not None else None,
        'publicManifestSha256': manifest.public_sha256}


def reader_ledger_rows(data, binding):
    from critical_ios_guard_pilot import HASH_FIELDS, INT_FIELDS, DOUBLE_FIELDS, BOOL_FIELDS, STRING_FIELDS
    hashes = HASH_FIELDS | {'modelSha256', 'abisSha256', 'deviceFactsSha256'}
    integers = INT_FIELDS | {'pageIndex', 'selectionGeneration', 'decodedKeepAliveCount', 'frameCount',
        'frameBuildTotalMicroseconds', 'frameRasterTotalMicroseconds', 'frameBuildMaxMicroseconds',
        'frameRasterMaxMicroseconds', 'apiLevel', 'memTotalBytes', 'memoryClassMiB', 'backgroundCount',
        'returnCount', 'backgroundEpochMilliseconds', 'returnedEpochMilliseconds'}
    doubles = DOUBLE_FIELDS | set(GEOMETRY_FIELDS[:-1])
    booleans = BOOL_FIELDS | {'lowRamDevice', 'isPhysicalDevice', 'backgrounded'}
    strings = {key: set(values) for key, values in STRING_FIELDS.items()}
    strings['phase'].update(PHASES)
    strings['event'].update(('reader_geometry', 'reader_selection', 'page_paint', 'frame_aggregate',
        'device_facts', 'lifecycle'))
    strings['reason'].update(('lifecycle_unavailable', 'device_facts_unavailable', 'reader_incomplete'))
    strings.update(orientation={'portrait', 'landscape'}, platform={'ios', 'android'},
        deviceReason={'none', 'unsupported', 'read_failed', 'invalid_value', 'unavailable'}, qemu={'0', '1', 'unavailable'})
    ledger = strict_json(data, 250000)
    exact_keys(ledger, ('schemaVersion', 'records', 'droppedCount'))
    require(type(ledger['schemaVersion']) is int and ledger['schemaVersion'] == 1 and
        type(ledger['droppedCount']) is int and ledger['droppedCount'] == 0 and
        type(ledger['records']) is list and 1 <= len(ledger['records']) <= 512, 'reader_ledger_capacity')
    clock = -1
    providers = binding['providers']
    by_case = {digest(case.encode()): value for case, value in providers.items()}
    for row in ledger['records']:
        require(type(row) is dict, 'reader_ledger_schema_rejected')
        for key, value in row.items():
            valid = (type(value) is str and HASH.fullmatch(value)) if key in hashes else \
                (value is None or type(value) is int and value >= (-1 if key == 'frameNumber' else 0)) if key in integers else \
                finite_number(value) if key in doubles else \
                (type(value) is bool or value is None and key in {'lowRamDevice', 'isPhysicalDevice'}) if key in booleans else \
                type(value) is str and value in strings[key] if key in strings else False
            require(valid, 'reader_ledger_schema_rejected')
        require(row.get('clockDomain') == 'dart_monotonic' and row.get('clockUnit') == 'microseconds' and
            type(row.get('monotonicMicroseconds')) is int and row['monotonicMicroseconds'] >= clock,
            'reader_ledger_clock_rejected')
        clock = row['monotonicMicroseconds']
        require(row.get('generation') == binding['generation'] and
            ('sessionSha256' not in row or row['sessionSha256'] == binding['sessionSha256']), 'reader_ledger_stale')
        if 'caseSha256' in row:
            expected = by_case.get(row['caseSha256'])
            require(expected is not None and all(key not in row or row[key] == expected[target]
                for key, target in (('providerSha256', 'expectedProviderSha256'),
                    ('attemptSha256', 'expectedAttemptSha256'))), 'reader_provider_stale')
        if row.get('event') in ('decoded', 'page_paint'):
            require(type(row.get('pageIndex')) is int and 0 <= row['pageIndex'] < 6 and
                row.get('caseSha256') == digest(READER_CASES[row['pageIndex']].encode()) and
                {'caseSha256', 'providerSha256', 'attemptSha256'} <= row.keys(), 'reader_page_binding_rejected')
        require(row.get('event') != 'failure', 'reader_run_failed')
    return ledger['records']


def memory_summary(rows, bounds, platform):
    counter = 'ios_phys_footprint' if platform == 'ios' else 'android_total_pss'
    samples = [row for row in rows if row.get('event') == 'memory_sample']
    phase_ends = {name: bounds[next_name] for name, next_name in zip(PHASES, PHASES[1:])}
    previous = None
    for sample in samples:
        start, end = sample.get('sampleStartMicroseconds'), sample.get('sampleEndMicroseconds')
        require(type(start) is int and type(end) is int and 0 <= start <= end <= sample['monotonicMicroseconds'] and
            sample.get('observerOverheadMicroseconds') == end - start and
            sample.get('sampleCadenceMicroseconds') == (None if previous is None else start - previous['sampleStartMicroseconds']) and
            (previous is None or start >= previous['sampleEndMicroseconds']), 'reader_sample_clock_rejected')
        require(sample.get('phase') in phase_ends and
            bounds[sample['phase']] <= start < phase_ends[sample['phase']], 'reader_sample_phase_rejected')
        if sample.get('memoryBytes') is not None:
            require(sample.get('memoryCounter') == counter and sample.get('memoryReason') == 'none' and
                type(sample.get('nativeReadMicroseconds')) is int, 'reader_sample_counter_rejected')
        else:
            require(sample.get('memoryReason') not in (None, 'none'), 'reader_sample_counter_rejected')
        previous = sample
    summary = {}
    for name, next_name in zip(PHASES, PHASES[1:]):
        start, end = bounds[name], bounds[next_name]
        window = [row for row in samples if start <= row['sampleStartMicroseconds'] < end and
            row['sampleEndMicroseconds'] <= end]
        crossing = [row for row in samples if start <= row['sampleStartMicroseconds'] < end and
            row['sampleEndMicroseconds'] > end]
        values = [row['memoryBytes'] for row in window if row.get('memoryBytes') is not None]
        boundaries = [start, *[row['sampleStartMicroseconds'] for row in window], end]
        summary[name] = {'sampleCount': len(window), 'validCount': len(values),
            'nullCount': len(window) - len(values), 'expectedCountAt1Hz': math.ceil((end - start) / 1000000),
            'maximumGapMicroseconds': max(right - left for left, right in zip(boundaries, boundaries[1:])),
            'medianBytes': statistics.median(values) if values else None,
            'sampledMaximumBytes': max(values) if values else None,
            'observerMicroseconds': sum(row['observerOverheadMicroseconds'] for row in window),
            'nativeReadCount': sum(type(row.get('nativeReadMicroseconds')) is int for row in window),
            'nativeReadNullCount': sum(row.get('nativeReadMicroseconds') is None for row in window),
            'nativeReadMicroseconds': sum(row['nativeReadMicroseconds'] for row in window
                if type(row.get('nativeReadMicroseconds')) is int) if any(
                    type(row.get('nativeReadMicroseconds')) is int for row in window) else None,
            'crossingSampleCount': len(crossing),
            'crossingValidCount': sum(row.get('memoryBytes') is not None for row in crossing),
            'crossingNullCount': sum(row.get('memoryBytes') is None for row in crossing),
            'crossingObserverMicroseconds': sum(row['observerOverheadMicroseconds'] for row in crossing),
            'crossingNativeReadCount': sum(type(row.get('nativeReadMicroseconds')) is int for row in crossing),
            'crossingNativeReadNullCount': sum(row.get('nativeReadMicroseconds') is None for row in crossing),
            'crossingNativeReadMicroseconds': sum(row['nativeReadMicroseconds'] for row in crossing
                if type(row.get('nativeReadMicroseconds')) is int) if not crossing or any(
                    type(row.get('nativeReadMicroseconds')) is int for row in crossing) else None}
        if crossing:
            summary[name]['crossingSamples'] = [{
                **{key: row.get(key) for key in ('phase', 'sampleStartMicroseconds', 'sampleEndMicroseconds',
                    'monotonicMicroseconds', 'sampleCadenceMicroseconds', 'observerOverheadMicroseconds',
                    'memoryBytes', 'memoryCounter', 'memoryReason', 'nativeReadMicroseconds',
                    'nativeResultCode', 'nativeResultCount', 'thermalState')},
                'reason': 'phase_boundary_crossing',
                'crossedBoundaries': [{'phase': phase, 'monotonicMicroseconds': boundary}
                    for phase, boundary in bounds.items()
                    if row['sampleStartMicroseconds'] < boundary < row['sampleEndMicroseconds']],
            } for row in crossing]
    base = summary['baseline']['medianBytes']
    has_crossing = any(value['crossingSampleCount'] for value in summary.values())
    eligible = all(value['validCount'] and value['nullCount'] == 0 and
        value['maximumGapMicroseconds'] <= 2000000 for value in summary.values())
    return {'counter': counter, 'windows': summary,
        'eligibility': 'phase_crossing_counter_unavailable' if has_crossing else
            'observed_samples' if eligible else 'missing_or_gapped_phase_counter',
        'sampledOnstageDeltaBytes': summary['reader']['sampledMaximumBytes'] - base
            if base is not None and summary['reader']['sampledMaximumBytes'] is not None else None,
        'naturalRetentionDeltaBytes': summary['natural_retention']['medianBytes'] - base
            if base is not None and summary['natural_retention']['medianBytes'] is not None else None,
        'ownedEvictionDeltaBytes': summary['owned_eviction']['medianBytes'] - base
            if base is not None and summary['owned_eviction']['medianBytes'] is not None else None,
        'truePeak': None, 'leakConclusion': None, 'packageGpuRetention': 'unknown'}


def require_primary_selection_journey(rows, bounds):
    # Mirrors the Cubit's forward/back/rapid/final selections, including initial reader selection.
    expected = (0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 5, 0, 4, 1, 3, 2, 5)
    selections = [row for row in rows if row.get('event') == 'reader_selection']
    require(tuple(row.get('pageIndex') for row in selections) == expected and
        tuple(row.get('selectionGeneration') for row in selections) == tuple(range(1, 19)) and
        all(bounds['reader'] <= row['monotonicMicroseconds'] < bounds['natural_retention']
            for row in selections), 'reader_selection_journey_rejected')
    rapid_times = [row['monotonicMicroseconds'] for row in selections[11:17]]
    # The source targets 100 ms. This observation bound keeps the entire burst below the
    # 1 Hz memory cadence; it allows timer jitter and is not precise timing/performance proof.
    require(all(0 < right - left <= 1000000 for left, right in zip(rapid_times, rapid_times[1:])) and
        rapid_times[-1] - rapid_times[0] <= 1000000, 'reader_selection_rapid_rejected')


def selected075_provenance(manifest, observed, source_root):
    """Independently admit originals and reproduce all six finite 0.75x PNGs."""
    require(source_root is not None, 'reader_selected075_sources_required')
    pages, admission = admit_sources(source_root)
    public = strict_json(manifest.public_bytes)
    require(public.get('batchId') == SELECTED075_BATCH and
        public.get('corpusSha256') == digest(frozen_json(admission)) and
        public.get('sourceSha256') == digest(frozen_json([page.source_sha256 for page in pages])) and
        public.get('referenceSha256') == digest(frozen_json([page.reference_sha256 for page in pages])),
        'reader_selected075_source_binding_rejected')
    provenance = []
    for page, entry in zip(pages, manifest.entries):
        expected = contain_dimensions(page.width, page.height, observed['observedSlotWidth'],
            observed['observedSlotHeight'], observed['observedDpr'], .75)
        original_demand = contain_dimensions(page.width, page.height, observed['observedSlotWidth'],
            observed['observedSlotHeight'], observed['observedDpr'])
        require(original_demand['sourcePixelsSufficient'] is True,
            'reader_selected075_original_shortage')
        require(entry['sourceSha256'] == page.source_sha256 and
            entry['referenceSha256'] == page.reference_sha256 and
            (entry['width'], entry['height']) == (expected['width'], expected['height']) and
            entry['arm'] == 'candidate' and entry['encoderRevision'] == 'reader_lanczos_pillow12_r1',
            'reader_selected075_resize_binding_rejected')
        encoded = encode_page(page, expected)
        require(manifest.objects.get(entry['caseId']) == encoded and
            entry['byteSize'] == len(encoded) and entry['outputSha256'] == digest(encoded),
            'reader_selected075_bytes_rejected')
        provenance.append({'pageIndex': page.page_number - 1,
            'sourceSha256': page.source_sha256, 'referenceSha256': page.reference_sha256,
            'outputSha256': digest(encoded), 'originalSourceDemand': original_demand,
            'preparedDimensions': expected})
    return {'factor': .75, 'sourceBinding': 'independently_admitted_originals',
        'resampler': 'direct_v7_LANCZOS_reducing_gap_none',
        'pngSettings': {'mode': 'RGB', 'compressLevel': 9, 'optimize': False},
        'displayUpsamplingRequired': True, 'pages': provenance}


def reload_reader_manifest(config, previous, binding):
    """Re-admit private inputs after the broker retires its owned encoded bytes."""
    from native_entry_config import Configuration, load_manifest
    # The original run directory now exists. Re-admission validates all original
    # manifest/input paths against a fresh, uncreated sibling output candidate.
    candidate = Path(config.values['privateOutputRoot']).parent / ('reader-readmission-' + secrets.token_hex(16))
    admission_config = Configuration({**config.values, 'privateOutputRoot': str(candidate)}, config.config_sha256)
    refreshed = load_manifest(admission_config)
    require(refreshed.private_sha256 == previous.private_sha256 and
        refreshed.public_sha256 == previous.public_sha256 == binding.get('publicManifestSha256') and
        binding.get('batchId') == config.values['batchId'], 'reader_reloaded_manifest_binding_rejected')
    return refreshed


def qualify(data, binding, outcome, manifest, *, platform, source_root=None):
    require(platform in ('ios', 'android') and binding.get('batchId') in READER_BATCHES and
        validate_reader_entries(binding['batchId'], manifest.entries) and
        binding.get('publicManifestSha256') == manifest.public_sha256 and
        type(binding.get('providers')) is dict and set(binding['providers']) ==
        {entry['caseId'] for entry in manifest.entries},
        'reader_binding_rejected')
    require(outcome.get('mode') == 'release' and outcome.get('platform') == platform and
        outcome.get('sessionSha256') == binding.get('sessionSha256') and
        outcome.get('generation') == binding.get('generation') and outcome.get('nativeResult') == 'settled' and
        outcome.get('teardown') == 'authenticated_app_revoke_observed', 'reader_outcome_unverified')
    rows = reader_ledger_rows(data, binding)
    quality = binding['batchId'].startswith('reader_quality_')
    required_pages = set(range(2 if quality else 6))
    geometries = [row for row in rows if row.get('event') == 'reader_geometry']
    require(len(geometries) == 1, 'reader_geometry_unavailable')
    observed = geometry({key: geometries[0].get(key) for key in GEOMETRY_FIELDS})
    if binding.get('geometry') is not None:
        require_same_geometry(binding['geometry'], observed)
    elif binding['batchId'] != 'reader_resolution_a_r1':
        raise EntryError('reader_geometry_freeze_missing')
    for entry in manifest.entries:
        if entry['profileRevision'] != 'reader_discover_r1':
            require((entry['logicalSlotWidth'], entry['logicalSlotHeight'], entry['dpr']) ==
                tuple(observed[key] for key in GEOMETRY_FIELDS[:3]), 'reader_geometry_changed')
    selected075 = (selected075_provenance(manifest, observed, source_root)
        if binding['batchId'] == SELECTED075_BATCH else None)
    painted = [row for row in rows if row.get('event') == 'page_paint']
    require(required_pages <= {row['pageIndex'] for row in painted}, 'reader_page_paint_unavailable')
    page_results = []
    for index in sorted(required_pages):
        case, entry = READER_CASES[index], manifest.entries[index]
        identity = binding['providers'][case]
        require(all(type(identity.get(key)) is str and HASH.fullmatch(identity[key]) for key in
            ('expectedProviderSha256', 'expectedAttemptSha256')) and all(type(identity.get(key)) is int and
            identity[key] > 0 for key in ('expectedDecodeWidth', 'expectedDecodeHeight')), 'reader_provider_binding_rejected')
        admitted_decode = contain_dimensions(entry['width'], entry['height'], entry['logicalSlotWidth'],
            entry['logicalSlotHeight'], entry['dpr'])
        full = binding['batchId'] in ('reader_resolution_a_r1', 'reader_quality_full_r1')
        independent_size = (entry['width'], entry['height']) if full else (
            admitted_decode['width'], admitted_decode['height'])
        require((identity['expectedDecodeWidth'], identity['expectedDecodeHeight']) == independent_size,
            'reader_expected_decode_rejected')
        decoded = [row for row in rows if row.get('event') == 'decoded' and row['pageIndex'] == index]
        require(decoded and all((row.get('actualDecodeWidth'), row.get('actualDecodeHeight')) ==
            (identity['expectedDecodeWidth'], identity['expectedDecodeHeight']) for row in decoded), 'reader_decode_mismatch')
        case_hash = digest(case.encode())
        verified = [row for row in rows if row.get('event') == 'encoded_verified' and
            row.get('caseSha256') == case_hash and row.get('providerSha256') == identity['expectedProviderSha256'] and
            row.get('encodedVerified') is True]
        require(verified and min(row['monotonicMicroseconds'] for row in verified) <=
            min(row['monotonicMicroseconds'] for row in decoded), 'reader_encoded_unverified')
        paints = [row for row in painted if row['pageIndex'] == index]
        require(all(any(decode['monotonicMicroseconds'] <= paint['monotonicMicroseconds'] for decode in decoded)
            for paint in paints), 'reader_paint_sequence_rejected')
        delivery = outcome.get('fixtureDeliveries', {}).get(case, {})
        require(type(delivery.get('count')) is int and delivery['count'] >= 1 and
            delivery.get('bodyBytes') == delivery['count'] * entry['byteSize'] and
            delivery.get('outputSha256') == entry['outputSha256'], 'reader_transfer_unverified')
        body = [row for row in rows if row.get('event') == 'body' and row.get('caseSha256') == case_hash]
        require(body and all(type(row.get('bodyBytes')) is int and row['bodyBytes'] > 0 for row in body) and
            sum(row['bodyBytes'] for row in body) == delivery['bodyBytes'], 'reader_transfer_unverified')
        expected = contain_dimensions(entry['width'], entry['height'], observed['observedSlotWidth'],
            observed['observedSlotHeight'], observed['observedDpr'])
        page_results.append({'pageIndex': index, 'caseSha256': digest(case.encode()),
            'decode': {key: identity[key] for key in ('expectedDecodeWidth', 'expectedDecodeHeight')},
            'firstTargetPaintMicroseconds': min(row['monotonicMicroseconds'] for row in paints),
            'transfers': delivery, 'sourceDemand': expected})
    phases = [row for row in rows if row.get('event') == 'phase']
    expected_phases = ('reader', 'complete') if quality else PHASES
    require(tuple(row.get('phase') for row in phases) == expected_phases, 'reader_phase_sequence_rejected')
    bounds = {row['phase']: row['monotonicMicroseconds'] for row in phases}
    require(all(bounds['reader'] <= row['monotonicMicroseconds'] <= bounds['complete' if quality else 'natural_retention']
        for row in painted), 'reader_paint_outside_phase')
    if quality:
        require(bounds['complete'] - bounds['reader'] <= 20000000 and not any(
            row.get('event') == 'memory_sample' for row in rows), 'reader_quality_budget_rejected')
        memory = None
    else:
        require(bounds['reader'] - bounds['baseline'] >= 20000000 and
            0 < bounds['natural_retention'] - bounds['reader'] <= 60000000 and
            bounds['owned_eviction'] - bounds['natural_retention'] >= 30000000 and
            bounds['complete'] - bounds['owned_eviction'] >= 10000000 and
            bounds['complete'] - bounds['baseline'] <= 180000000, 'reader_phase_duration_rejected')
        require_primary_selection_journey(rows, bounds)
        memory = memory_summary(rows, bounds, platform)
    caches = [row for row in rows if row.get('event') == 'cache_state']
    if not quality:
        release = [row for row in rows if row.get('event') == 'release_complete']
        require(len(release) == 1 and bounds['owned_eviction'] <= release[0]['monotonicMicroseconds'] <=
            bounds['complete'], 'reader_release_unavailable')
        for phase in ('natural_retention', 'owned_eviction'):
            for case in READER_CASES:
                states = [row for row in caches if row.get('phase') == phase and
                    row.get('caseSha256') == digest(case.encode())]
                require(states and all(all(type(row.get(key)) is int for key in ('decodedLiveCount',
                    'decodedPendingCount', 'decodedKeepAliveCount', 'decodedLruBytes')) for row in states),
                    'reader_cache_ownership_unavailable')
    facts = [row for row in rows if row.get('event') == 'device_facts']
    lifecycle = [row for row in rows if row.get('event') == 'lifecycle']
    lifecycle_observed = (bool(lifecycle) and all(type(lifecycle[-1].get(key)) is int and
        lifecycle[-1][key] >= 1 for key in ('backgroundCount', 'returnCount')) and lifecycle[-1].get('backgrounded') is False and
        all(type(lifecycle[-1].get(key)) is int for key in
            ('backgroundEpochMilliseconds', 'returnedEpochMilliseconds')) and
        lifecycle[-1]['returnedEpochMilliseconds'] >= lifecycle[-1]['backgroundEpochMilliseconds'])
    operator_physical = binding.get('physicalTwoGiBAdmitted') is True and platform == 'android'
    facts_available = (len(facts) == 1 and (facts[0].get('isPhysicalDevice') is True or
        facts[0].get('isPhysicalDevice') is None and operator_physical) and
        facts[0].get('platform') == platform and all(type(facts[0].get(key)) is str and
            HASH.fullmatch(facts[0][key]) for key in ('modelSha256', 'deviceFactsSha256')))
    low_memory = (binding.get('physicalTwoGiBAdmitted') is True and facts_available and
        platform == 'android' and facts[0].get('qemu') != '1' and
        facts[0].get('apiLevel') == 30 and facts[0].get('lowRamDevice') is True and
        type(facts[0].get('memTotalBytes')) is int and 1536 * 1024 ** 2 <= facts[0]['memTotalBytes'] <= 2 * 1024 ** 3 and
        type(facts[0].get('memoryClassMiB')) is int and facts[0]['memoryClassMiB'] > 0)
    observation_eligible = (not quality and memory['eligibility'] == 'observed_samples' and
        lifecycle_observed and facts_available and (platform != 'android' or low_memory))
    metric_eligible = quality or observation_eligible and all(
        page['sourceDemand']['sourcePixelsSufficient'] is True for page in page_results)
    shortages = [page['pageIndex'] for page in page_results if page['sourceDemand']['sourcePixelsSufficient'] is not True]
    frames = [row for row in rows if row.get('event') == 'frame_aggregate']
    frame_observations = [type(row.get('frameCount')) is int and row['frameCount'] > 0 for row in frames]
    frame_reason = ('observed' if frames and all(frame_observations) else
        'partially_observed' if any(frame_observations) else 'unavailable')
    return {'schemaVersion': 1, 'scope': SCOPE, 'status': 'observed', 'platform': platform,
        'qualification': 'awaiting_human_quality' if quality else 'qualified' if metric_eligible else 'unavailable',
        'sourceSufficiencyReason': 'insufficient_pixels' if shortages else 'sufficient',
        'sourceShortagePages': shortages,
        'intentionalUndersamplingPerformanceEligibility': ('observed_samples' if observation_eligible else
            'unavailable') if selected075 is not None else 'not_applicable',
        'intentionalUndersamplingProvenance': selected075,
        'batchId': binding['batchId'], 'ledgerSha256': digest(data), 'recordCount': len(rows), 'droppedCount': 0,
        'sessionSha256': binding['sessionSha256'], 'geometry': observed, 'pages': page_results,
        'memory': memory, 'frames': frames, 'framesReason': frame_reason,
        'cacheOwnership': caches, 'deviceFacts': facts[0] if len(facts) == 1 else None,
        'deviceFactsReason': 'observed' if facts_available else 'unavailable',
        'physicalEvidence': 'operator_physical_prerequisite' if operator_physical else 'native_observation',
        'lowMemoryPhysicalQualification': 'observed' if low_memory else 'unavailable',
        'lifecycle': lifecycle[-1] if lifecycle else None,
        'lifecycleReason': 'observed' if lifecycle_observed else 'unavailable',
        'deviceUnitSha256': binding.get('deviceUnitSha256'), 'crossReservationCausalComparison': False,
        'physicalPresentation': None, 'qualityAccepted': None, 'audioGeometry': 'untested',
        'repeatability': None, 'p95': None, 'jpegWebpPerformance': 'unqualified'}


def run(config, manifest, output, **arguments):
    try:
        return _run(config, manifest, output, **arguments)
    except (EntryError, OSError) as error:
        try:
            write_private(Path(output) / 'reader-failure-receipt.json', {'scope': SCOPE,
                'status': 'failed', 'reason': str(error) if isinstance(error, EntryError) else 'operation_failed',
                'qualityAccepted': None, 'automaticRetry': False})
        except (EntryError, OSError):
            pass
        raise


def _run(config, manifest, output, *, sequential_window=None, **arguments):
    if sequential_window is None:
        return _run_observed(config, manifest, output, **arguments)
    from reader_android_window import SequentialAndroidWindow
    require(type(sequential_window) is SequentialAndroidWindow and
        config.values['platform'] == 'android' and
        arguments.get('root_elapsed_seconds') is None and
        arguments.get('reservation_remaining_seconds') is None,
        'reader_sequential_lease_rejected')
    frozen = arguments.get('frozen_geometry')
    frozen_sha256 = digest(frozen_json(geometry(frozen))) if frozen is not None else None
    lease = sequential_window.enter(config.values['batchId'], geometry_sha256=frozen_sha256)
    result = None
    failed = False
    try:
        result = _run_observed(config, manifest, output, sequential_lease=lease, **arguments)
    except BaseException:
        failed = True
        raise
    finally:
        try:
            sequential_window.finish(lease, succeeded=result is not None,
                geometry_sha256=digest(frozen_json(result['geometry'])) if result is not None else None)
        except BaseException:
            if not failed:
                raise
    write_private(Path(output) / 'reader-result.json', result)
    return result


def _run_observed(config, manifest, output, *, build_path, prerequisite_path, audit_path,
        frozen_geometry=None, root_elapsed_seconds=None, reservation_remaining_seconds=None,
        sequential_lease=None, source_root=None):
    """Observe an already installed reader launch; never create a paid allocation."""
    import native_entry_driver as host
    from native_fixture_server import Broker
    from native_artwork_comparison import (LedgerChunks, LOCK, validate_comparison_audit,
        require_fresh_android_process)
    from reader_terminal_observation import ReaderTerminalObservation
    require(config.values['mode'] == 'release' and
        validate_reader_entries(config.values['batchId'], manifest.entries), 'reader_selection_rejected')
    if config.values['batchId'] != 'reader_resolution_a_r1':
        geometry(frozen_geometry)
    sequential_arguments = {'sequential_lease': sequential_lease} if sequential_lease is not None else {}
    build, proof = host.validate_physical_prerequisites(config, build_path, prerequisite_path,
        **sequential_arguments)
    validate_comparison_audit(audit_path, build_path, build, proof, config.values['platform'])
    if config.values['platform'] == 'android':
        require(config.values['batchId'] != 'reader_resolution_b_r1' and
            type(proof.get('deviceModel')) is str and
            proof.get('deviceModel', '').lower() in ('moto e20', 'motorola moto e20'), 'reader_device_selection_rejected')
        if sequential_lease is None:
            entry_window_guard(root_elapsed_seconds, reservation_remaining_seconds)
    if config.values['batchId'] == SELECTED075_BATCH:
        selected075_provenance(manifest, geometry(frozen_geometry), source_root)
    source_digest = host.source_snapshot()[1]
    tools = {name: digest((Path(__file__).parent / name).read_bytes()) for name in
        ('reader_resolution_experiment.py', 'reader_android_window.py', 'reader_terminal_observation.py', 'native_entry_config.py',
            'native_fixture_server.py', 'native_entry_driver.py')}
    output = Path(output)
    write_private(output / 'reader-tool-freeze.json', {'scope': SCOPE, 'sourceSha256': source_digest,
        'toolSha256': tools, 'privateManifestSha256': manifest.private_sha256,
        'publicManifestSha256': manifest.public_sha256, 'artifactSha256': build['artifactSha256'],
        'testArtifactSha256': build['testArtifactSha256']})
    deadline = (time.monotonic() + host.TOTAL_SECONDS if sequential_lease is None else sequential_lease.deadline)
    if config.values['platform'] == 'android':
        require_fresh_android_process(config, output, deadline)
    require(LOCK.acquire(blocking=False), 'reader_overlap')
    original_broker, original_pairing = host.Broker, host.LauncherPairing
    state = {}
    terminal = ReaderTerminalObservation(output, manifest, build, source_digest)
    class ObservedBroker(Broker):
        def claim(self, value):
            data = super().claim(value)
            state['binding'] = reader_binding(self.manifest, strict_json(data), frozen_geometry)
            state['binding']['physicalTwoGiBAdmitted'] = config.values['platform'] == 'android'
            if sequential_lease is not None:
                state['binding']['deviceUnitSha256'] = sequential_lease.window.device_unit_sha256
                state['binding']['allocationSha256'] = sequential_lease.window.allocation_sha256
            write_private(output / 'run-binding.json', state['binding'])
            return data
        def revoke_from_app(self, *args, **kwargs):
            try:
                terminal.request()
            except Exception:
                pass
            try:
                result = super().revoke_from_app(*args, **kwargs)
            except Exception:
                try:
                    terminal.exception()
                except Exception:
                    pass
                raise
            try:
                terminal.response(result)
            except Exception:
                pass
            return result
        def close(self):
            try:
                terminal.before_close(self)
            except Exception:
                pass
            return super().close()
    class Pairing(original_pairing):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.chunks = LedgerChunks()
            state['pairing'] = self
        def __call__(self, line):
            self.chunks.feed(line)
            super().__call__(line)
    try:
        host.Broker, host.LauncherPairing = ObservedBroker, Pairing
        outcome = host.verify_entry(config, manifest, output, deadline, case_id=READER_CASES[0],
            build_path=build_path, prerequisite_path=prerequisite_path,
            quality_capture=config.values['batchId'].startswith('reader_quality_'), **sequential_arguments)
    except Exception:
        if config.values['platform'] == 'android':
            try:
                terminal.failed_ledger(state.get('pairing'), state.get('binding'))
            except Exception:
                pass
        raise
    finally:
        host.Broker, host.LauncherPairing = original_broker, original_pairing
        LOCK.release()
    require('binding' in state and host.source_snapshot()[1] == source_digest and all(
        digest((Path(__file__).parent / name).read_bytes()) == expected for name, expected in tools.items()),
        'reader_source_changed')
    if config.values['platform'] == 'android':
        data = state['pairing'].chunks.finish()
        write_private(output / 'device-ledger.json', data)
        refreshed = (reload_reader_manifest(config, manifest, state['binding'])
            if config.values['batchId'] == SELECTED075_BATCH else manifest)
        try:
            result = qualify(data, state['binding'], outcome, refreshed,
                platform='android', source_root=source_root)
        finally:
            if refreshed is not manifest:
                refreshed.objects.clear()
        if sequential_lease is None:
            write_private(output / 'reader-result.json', result)
        return result
    return {'scope': SCOPE, 'status': 'awaiting_private_device_ledger',
        'sessionSha256': state['binding']['sessionSha256'], 'qualityAccepted': None}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('prepare', 'budget-debit'))
    parser.add_argument('--source-root')
    parser.add_argument('--geometry')
    parser.add_argument('--snapshot')
    parser.add_argument('--output', required=True)
    parser.add_argument('--batch', action='append', choices=sorted(READER_BATCHES))
    args = parser.parse_args(argv)
    try:
        if args.command == 'prepare':
            require(args.source_root is not None and args.snapshot is None, 'reader_cli_rejected')
            pages, admission = admit_sources(args.source_root)
            observed = strict_json(private_file(args.geometry)) if args.geometry else None
            result = prepare_variants(pages, admission, observed, args.output, batches=args.batch)
            public = {'status': 'prepared', 'scope': SCOPE, 'receiptSha256': digest(frozen_json(result))}
        else:
            require(args.snapshot is not None and args.source_root is None and args.geometry is None and
                args.batch is None, 'reader_cli_rejected')
            result = debit_before_create(strict_json(private_file(args.snapshot)), args.output)
            public = {'status': 'debited_before_create', 'scope': SCOPE, 'ordinal': result['ordinal'],
                'projectedAllServicesUpperUsd': result['projectedAllServicesUpperUsd']}
        print(frozen_json(public).decode())
        return 0
    except (EntryError, OSError):
        print(frozen_json({'status': 'refused', 'scope': SCOPE}).decode())
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
