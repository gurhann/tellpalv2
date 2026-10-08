#!/usr/bin/env python3
"""Read-only verification of retained Story 1.1 evidence and geometric demand."""
from decimal import Decimal, ROUND_CEILING, ROUND_FLOOR, localcontext
from fractions import Fraction
from hashlib import sha256
from pathlib import Path
import copy
import json
import re
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
INVENTORY = HERE / 'p0a-slot-source-inventory.json'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def git(*arguments):
    return subprocess.check_output(['git', *arguments], cwd=ROOT, stderr=subprocess.DEVNULL)


def checksum(data):
    return sha256(data).hexdigest()


def number(value):
    return Fraction(str(value))


def ceiling(value):
    return (value.numerator + value.denominator - 1) // value.denominator


def fitted(dimensions, slot, dpr, fit):
    """Keep demand independent of any intrinsic or decoder ceiling."""
    iw, ih = map(number, dimensions)
    lw, lh = map(number, slot)
    dpr = number(dpr)
    require(min(iw, ih, lw, lh, dpr) > 0, 'Nonpositive geometry')
    require(fit in ('contain', 'cover'), 'Unsupported fit')
    pw, ph = ceiling(lw * dpr), ceiling(lh * dpr)
    scale = (min if fit == 'contain' else max)(Fraction(pw) / iw, Fraction(ph) / ih)
    bounded = min(Fraction(1), scale)
    return {
        'physical_slot': [pw, ph],
        'scale': str(scale),
        'required_full_image': [ceiling(iw * scale), ceiling(ih * scale)],
        'bounded_decode': [min(int(iw), ceiling(iw * bounded)), min(int(ih), ceiling(ih * bounded))],
        'intrinsic_shortage': scale > 1,
    }


def decimal_fitted(dimensions, slot, dpr, fit):
    """Independent Decimal cross-check; fixtures avoid recurring-boundary ambiguity."""
    with localcontext() as context:
        context.prec = 60
        iw, ih = map(lambda v: Decimal(str(v)), dimensions)
        lw, lh = map(lambda v: Decimal(str(v)), slot)
        dpr = Decimal(str(dpr))
        ceil = lambda v: int(v.to_integral_value(rounding=ROUND_CEILING))
        pw, ph = ceil(lw * dpr), ceil(lh * dpr)
        scale = (min if fit == 'contain' else max)(Decimal(pw) / iw, Decimal(ph) / ih)
        return [pw, ph], [ceil(iw * scale), ceil(ih * scale)]


def hero_decode(dimensions, tablet):
    with localcontext() as context:
        context.prec = 60
        width, height = map(Decimal, dimensions)
        scale = min(Decimal(1), Decimal(1600 if tablet else 1080) / width,
                    Decimal(2048) / height, (Decimal(4194304) / (width * height)).sqrt())
        return [max(1, int((axis * scale).to_integral_value(rounding=ROUND_FLOOR))) for axis in (width, height)]


def arithmetic_fixtures():
    # Expected values are fixed, not generated from inventory calculations.
    cases = [
        ('small-source-shortage', [100, 100], ['200', '200'], '1', 'cover', [200, 200], [200, 200], [100, 100], True),
        ('tall-cover', [100, 400], ['200', '200'], '1', 'cover', [200, 200], [200, 800], [100, 400], True),
        ('tall-contain', [100, 400], ['200', '200'], '1', 'contain', [200, 200], [50, 200], [50, 200], False),
        ('wide-cover', [400, 100], ['200', '200'], '1', 'cover', [200, 200], [800, 200], [400, 100], True),
        ('wide-contain', [400, 100], ['200', '200'], '1', 'contain', [200, 200], [200, 50], [200, 50], False),
        ('fractional-dpr', [1000, 500], ['100.2', '50.1'], '1.25', 'cover', [126, 63], [126, 63], [126, 63], False),
        ('phone-original', [512, 909], ['157.59', '210.12'], '3', 'cover', [473, 631], [473, 840], [473, 840], False),
        ('phone-rendition', [270, 480], ['157.59', '210.12'], '3', 'cover', [473, 631], [473, 841], [270, 480], True),
        ('tablet-original', [512, 909], ['247.635', '330.18'], '2', 'cover', [496, 661], [496, 881], [496, 881], False),
        ('rotated-phone', [512, 909], ['79.275', '105.7'], '3', 'cover', [238, 318], [238, 423], [238, 423], False),
        ('split-window', [428, 695], ['167.25', '223'], '1.5', 'cover', [251, 335], [251, 408], [251, 408], False),
    ]
    for name, dimensions, slot, dpr, fit, physical, required, bounded, shortage in cases:
        observed = fitted(dimensions, slot, dpr, fit)
        require(observed['physical_slot'] == physical and observed['required_full_image'] == required
                and observed['bounded_decode'] == bounded and observed['intrinsic_shortage'] == shortage,
                f'Fixed arithmetic fixture failed: {name}')
        require(decimal_fitted(dimensions, slot, dpr, fit) == (physical, required), f'Decimal mismatch: {name}')
    require(hero_decode([4000, 4000], False) == [1080, 1080], 'Phone decoder cap')
    require(hero_decode([4000, 4000], True) == [1600, 1600], 'Tablet decoder cap')
    require(hero_decode([4000, 12000], True) == [682, 2048], 'Tall decoder height cap/floor')
    require(hero_decode([100, 100], True) == [100, 100], 'No upscale decoder cap')
    # Different failures must stay distinguishable when only the decoder is capped.
    original = fitted([1000, 1000], [200, 200], 1, 'cover')
    rendition = fitted([100, 100], [200, 200], 1, 'cover')
    require(not original['intrinsic_shortage'] and rendition['intrinsic_shortage'], 'Rendition-only shortage lost')
    original = fitted([4000, 4000], [1200, 1200], 1, 'cover')
    decode = hero_decode([4000, 4000], False)
    require(not original['intrinsic_shortage'] and original['required_full_image'] == [1200, 1200]
            and decode == [1080, 1080] and all(d < r for d, r in zip(decode, original['required_full_image'])),
            'Decode-only shortage lost despite sufficient original/rendition')
    for dimensions, slot, dpr in [([0, 1], [1, 1], 1), ([1, 1], [-1, 1], 1), ([1, 1], [1, 1], 0)]:
        try:
            fitted(dimensions, slot, dpr, 'cover')
        except ValueError:
            continue
        raise ValueError('Invalid geometry accepted')
    return len(cases) + 6


def resolve(document, pointer):
    value = document
    for part in pointer.split('/')[1:]:
        part = part.replace('~1', '/').replace('~0', '~')
        value = value[int(part)] if isinstance(value, list) else value[part]
    return value


# Expected fields belong to the verifier, never to the inventory being checked.
BYTE_FIELDS = {'sha256': 'sha256', 'bytes': 'bytes', 'width': 'width', 'height': 'height'}
ARTWORK_FIELDS = {**BYTE_FIELDS, 'bytes': 'byteSize'}
HERO_FIELDS = {**ARTWORK_FIELDS, 'sha256': 'checksumSha256', 'asset_id': 'assetId', 'mime': 'mimeType'}
METADATA_FIELDS = {'mime': 'mime', 'has_alpha': 'has_alpha', 'frames': 'frames', 'exif': 'exif', 'icc': 'icc'}
HERO_POINTER = r'/matrices/\d+/cases/\d+/hero'
EVIDENCE_SCHEMAS = {
    'source_row': ('artwork', r'/sources/\d+', {**ARTWORK_FIELDS, 'language': 'language', 'source_label': 'source'}),
    'rendition_row': ('artwork', r'/renditions/\d+', {**ARTWORK_FIELDS, 'external_key': 'externalKey', 'language': 'language', 'profile': 'profile'}),
    'hero_row': ('http', HERO_POINTER, HERO_FIELDS),
    'actual_source_row': ('http', r'/matrices/\d+/actualSources/\d+', {**HERO_FIELDS, 'language': 'language'}),
    'control_row': ('small_jpeg', r'/sources/\d+', BYTE_FIELDS),
    'source_asset_id': ('http', HERO_POINTER, {'asset_id': 'assetId'}),
    'source_mime': ('http', HERO_POINTER, {'mime': 'mimeType'}),
    'source_metadata': ('small_jpeg', r'/sources/\d+/metadata', METADATA_FIELDS),
    'rendition_output': ('small_jpeg', r'/results/\d+', {**BYTE_FIELDS, 'sample_id': 'sample_id'}),
    'rendition_mime': ('small_jpeg', r'/results/\d+/metadata', {'mime': 'mime'}),
    'hero_case': ('http', r'/matrices/\d+/cases/\d+', {'language': 'contentLanguage', 'content_id': 'contentId'}),
    'control_metadata': ('small_jpeg', r'/sources/\d+/metadata', METADATA_FIELDS),
    'control_corpus': ('corpus', r'/samples/\d+', {'id': 'id', 'sha256': 'sha256'}),
}


def projection(evidence, documents, purpose):
    expected_input, pointer_pattern, expected_fields = EVIDENCE_SCHEMAS[purpose]
    require(set(evidence) == {'input', 'pointer', 'fields'}, f'Evidence shape: {purpose}')
    require(evidence['input'] == expected_input, f'Evidence input: {purpose}')
    require(re.fullmatch(pointer_pattern, evidence['pointer']) is not None, f'Evidence pointer: {purpose}')
    require(evidence['fields'] == expected_fields, f'Evidence mapping must be complete and exact: {purpose}')
    source = resolve(documents[expected_input], evidence['pointer'])
    return {dest: source[key] for dest, key in expected_fields.items()}


def verify(inventory_path=None, report=True):
    inventory_path = INVENTORY if inventory_path is None else inventory_path
    fixed_count = arithmetic_fixtures()
    data = json.loads(inventory_path.read_text())
    require(data['schema_version'] == 1, 'Schema version')
    head = data['provenance']['head']
    require(re.fullmatch(r'[0-9a-f]{40}', head) is not None, 'Full Git HEAD required')
    require(git('cat-file', '-t', head).decode().strip() == 'commit', 'Captured HEAD not a commit')
    inputs = data['provenance']['inputs']
    require(len({entry['id'] for entry in inputs}) == len(inputs), 'Duplicate input identity')
    documents = {}
    before = {}
    for entry in inputs:
        path = entry['path']
        require(not Path(path).is_absolute() and '..' not in Path(path).parts, 'Input escapes repository')
        contents = (ROOT / path).read_bytes()
        before[path] = checksum(contents)
        require(before[path] == entry['sha256_before'] == entry['sha256_after'], f'Retained input changed: {path}')
        require(len(contents) == entry['bytes'], f'Input byte size differs: {path}')
        try:
            blob = git('show', f'{head}:{path}')
        except subprocess.CalledProcessError:
            blob = None
        require(entry['tracked_at_capture'] == (blob is not None), f'Tracked provenance: {path}')
        require(entry['head_blob_sha256'] == (checksum(blob) if blob is not None else None), f'HEAD provenance: {path}')
        require(entry['dirty_at_capture'] == (blob != contents), f'Dirty provenance: {path}')
        if path.endswith('.json'):
            documents[entry['id']] = json.loads(contents)
    require({'context', 'contract', 'artwork', 'http', 'corpus', 'small_jpeg', 'color'} <= {e['id'] for e in inputs}, 'Required context/evidence missing')
    for item in data['provenance']['dirty_snapshot']:
        require(set(item) == {'status', 'path'} and len(item['status']) == 2, 'Dirty snapshot shape')
        require(not Path(item['path']).is_absolute() and '..' not in Path(item['path']).parts, 'Unsafe dirty path')
    art, http, small = documents['artwork'], documents['http'], documents['small_jpeg']
    sources = data['source_bindings']
    renditions = data['rendition_observations']
    heroes = data['hero_observations']
    actual = data['actual_source_observations']
    for collection, purpose in ((sources, 'source_row'), (renditions, 'rendition_row'),
                                (heroes, 'hero_row'), (actual, 'actual_source_row'),
                                (data['historical_controls'], 'control_row')):
        require(len({row['id'] for row in collection}) == len(collection), 'Duplicate row id')
        for row in collection:
            for key, value in projection(row['evidence'], documents, purpose).items():
                require(row[key] == value, f'Evidence mismatch: {row["id"]}/{key}')
            require(re.fullmatch('[a-f0-9]{64}', row['sha256']) is not None, 'Invalid source/output hash')
            require(min(row['width'], row['height'], row['bytes']) > 0, 'Invalid recorded dimensions/bytes')
    require([row['evidence']['pointer'] for row in sources] == [f'/sources/{i}' for i in range(len(art['sources']))], 'Incomplete source bindings')
    require([row['evidence']['pointer'] for row in renditions] == [f'/renditions/{i}' for i in range(len(art['renditions']))], 'Incomplete rendition observations')
    hero_pointers = [f'/matrices/{mi}/cases/{ci}/hero' for mi, matrix in enumerate(http['matrices'])
                     for ci, case in enumerate(matrix['cases']) if case.get('hero') is not None]
    require([row['evidence']['pointer'] for row in heroes] == hero_pointers, 'Current hero collection incomplete or includes historical probes')
    actual_pointers = [f'/matrices/{mi}/actualSources/{ai}' for mi, matrix in enumerate(http['matrices'])
                       for ai in range(len(matrix.get('actualSources', [])))]
    require([row['evidence']['pointer'] for row in actual] == actual_pointers, 'actualSources collection incomplete')
    for source in sources:
        require(source['orientation'] is None and source['normalized_dimensions'] is None, 'Unobserved normalization promoted')
        require(source['encoded_ratio'] == str(Fraction(source['width'], source['height'])), 'Source ratio')
        expected_ids, expected_mimes = [], []
        for hero in heroes:
            if hero['language'] == source['language'] and all(hero[k] == source[k] for k in ('sha256', 'bytes', 'width', 'height')):
                expected_ids.append({**hero['evidence'], 'fields': {'asset_id': 'assetId'}})
                expected_mimes.append({**hero['evidence'], 'fields': {'mime': 'mimeType'}})
        require(source['asset_id_observations'] == expected_ids and source['mime_observations'] == expected_mimes, 'Source ID/MIME joins incomplete or inferred')
        expected_metadata = []
        for si, ctrl in enumerate(small['sources']):
            if all(source[k] == ctrl[k] for k in ('sha256', 'bytes', 'width', 'height')):
                expected_metadata.append({'input': 'small_jpeg', 'pointer': f'/sources/{si}/metadata', 'fields': {'mime': 'mime', 'has_alpha': 'has_alpha', 'frames': 'frames', 'exif': 'exif', 'icc': 'icc'}})
        require(source['metadata_observations'] == expected_metadata, 'Source metadata joins')
        for collection, purpose in ((source['metadata_observations'], 'source_metadata'),
                                    (source['asset_id_observations'], 'source_asset_id'),
                                    (source['mime_observations'], 'source_mime')):
            for evidence in collection:
                projection(evidence, documents, purpose)
    for rendition in renditions:
        require(rendition['asset_id'] is None and rendition['mime'] is None, 'Unverified listing ID/MIME promoted')
        require(rendition['orientation'] is None and rendition['normalized_dimensions'] is None, 'Rendition orientation unavailable')
        require(rendition['encoded_ratio'] == str(Fraction(rendition['width'], rendition['height'])), 'Rendition ratio')
        matching = [i for i, row in enumerate(small['results']) if all(rendition[k] == row[k] for k in ('sha256', 'bytes', 'width', 'height'))]
        require([join['output_evidence']['pointer'] for join in rendition['verified_source_hash_joins']] == [f'/results/{i}' for i in matching], 'Rendition hash join completeness')
        for join in rendition['verified_source_hash_joins']:
            output = projection(join['output_evidence'], documents, 'rendition_output')
            require(all(output[k] == rendition[k] for k in ('sha256', 'bytes', 'width', 'height')), 'Rendition byte identity join')
            ctrl = next(row for row in small['sources'] if row['id'] == output['sample_id'])
            require(join['source_sha256'] == ctrl['sha256'] and join['source_dimensions'] == [ctrl['width'], ctrl['height']], 'Original hash relation')
            expected_bindings = [s['id'] for s in sources if s['sha256'] == ctrl['sha256'] and s['language'] == rendition['language']]
            require(join['source_binding_ids'] == expected_bindings, 'Rendition/source binding language relation')
            require(join['actual_mime_evidence']['pointer'] == join['output_evidence']['pointer'] + '/metadata', 'Rendition MIME pointer relation')
            require(projection(join['actual_mime_evidence'], documents, 'rendition_mime')['mime'] == 'image/jpeg', 'Recorded actual rendition MIME')
    for hero in heroes:
        require(hero['case_evidence']['pointer'] + '/hero' == hero['evidence']['pointer'], 'Hero case pointer relation')
        case = projection(hero['case_evidence'], documents, 'hero_case')
        require(hero['language'] == case['language'], 'Hero language identity')
        expected_bindings = [s['id'] for s in sources if s['language'] == hero['language'] and all(s[k] == hero[k] for k in ('sha256', 'bytes', 'width', 'height'))]
        require(hero['source_binding_ids'] == expected_bindings and expected_bindings, 'Hero/source join')
        require(hero['orientation'] is None and hero['normalized_dimensions'] is None, 'Hero normalization unavailable')
    expected_control_ids = [row['id'] for row in small['sources']]
    require([row['id'] for row in data['historical_controls']] == expected_control_ids, 'Retained control completeness')
    for control in data['historical_controls']:
        require(control['category'] == ('separate_p3_diagnostic' if control['id'] == 'en-story11-p3' else 'historical_small_jpeg_control'), 'P3 diagnostic pooled with controls')
        require(control['metadata_evidence']['pointer'] == control['evidence']['pointer'] + '/metadata', 'Control metadata pointer relation')
        require(projection(control['metadata_evidence'], documents, 'control_metadata')['mime'] == control['mime'], 'Control MIME evidence')
        require(projection(control['corpus_evidence'], documents, 'control_corpus') == {'id': control['id'], 'sha256': control['sha256']}, 'Corpus/control identity')
        require(control['orientation'] is None and control['normalized_dimensions'] is None, 'Control normalization unavailable')
    require(small['provenance']['corpus_sha256'] == checksum((ROOT / next(e['path'] for e in inputs if e['id'] == 'corpus')).read_bytes()), 'Historical corpus hash')
    color = documents['color']
    require({c['sourceSha256'] for c in color['cases']} == {c['sha256'] for c in data['historical_controls']}, 'Color diagnostic corpus relation')
    expected_counts = {
        'source_bindings': 21, 'unique_source_hashes': 19, 'rendition_observations': 52,
        'unique_rendition_hashes': 38, 'hero_observations': 29, 'unique_hero_hashes': 8,
        'actual_source_observations': 6, 'unique_actual_source_hashes': 3,
        'rendition_observations_with_original_hash_join': 22, 'small_jpeg_controls': 6,
        'separate_p3_diagnostics': 1, 'encoded_experiment_results': 63,
    }
    for prefix, rows in [('source', sources), ('rendition', renditions), ('hero', heroes), ('actual_source', actual)]:
        count_key = 'source_bindings' if prefix == 'source' else prefix + '_observations'
        unique_key = 'unique_' + prefix + '_hashes'
        require(len(rows) == data['counts'][count_key] == expected_counts[count_key], f'Observation count: {prefix}')
        require(len({r['sha256'] for r in rows}) == data['counts'][unique_key] == expected_counts[unique_key], f'Unique hash count: {prefix}')
    for key, expected in expected_counts.items():
        require(data['counts'][key] == expected, f'Fixed evidence count: {key}')
    require(sum(bool(r['verified_source_hash_joins']) for r in renditions) == 22, 'Original joins count')
    require(len(small['results']) == data['counts']['encoded_experiment_results'], 'Encoded results are byte observations')
    roles = {row['id']: row for row in data['roles']}
    require(set(roles) == {'library_listing', 'story_recommendation', 'story_detail_hero', 'legacy_reader_poc', 'planned_reader_page', 'story_listening_art', 'meditation_listening_art', 'lullaby_listing_static', 'lullaby_listening_animation'}, 'Role coverage')
    input_by_id = {row['id']: row for row in inputs}
    gap_ids = {gap['id'] for gap in data['gaps']}
    for role in roles.values():
        require(role['gap_ids'] and set(role['gap_ids']) <= gap_ids, 'Role gaps missing')
        for anchor in role['anchors']:
            require(1 <= anchor['line'] <= len((ROOT / input_by_id[anchor['input']]['path']).read_text().splitlines()), 'Invalid code anchor')
        require(role['status'] in ('accepted_code', 'legacy_poc', 'unimplemented'), 'Role status')
    viewports = {row['id']: row for row in data['viewports']}
    require(set(viewports) == {'phone_portrait', 'tablet_portrait', 'phone_rotated', 'split_window_600', 'window_599'}, 'Viewport coverage')
    for viewport in viewports.values():
        require(viewport['evidence_kind'] == 'hypothetical_calculation' and viewport['device_observation'] is None, 'Scenario promoted to device measurement')
        require(set(viewport['insets']) == {'top', 'right', 'bottom', 'left'}, 'Missing inset axes')
    geometries = {row['id']: row for row in data['geometries']}
    require(len(geometries) == len(data['geometries']) == data['counts']['geometry_groups'], 'Geometry count/identity')
    observed = {row['id']: row for row in renditions + heroes}
    covered_ids = []
    for geometry in geometries.values():
        require(geometry['kind'] in ('listing', 'hero') and geometry['observation_ids'], 'Geometry lacks evidence')
        for oid in geometry['observation_ids']:
            observation = observed[oid]
            require(geometry['intrinsic_dimensions'] == [observation['width'], observation['height']], 'Geometry dimensions not from observation')
            if geometry['kind'] == 'listing':
                require(geometry['profile'] == observation['profile'], 'Geometry listing profile mismatch')
            original = [observation['width'], observation['height']] if geometry['kind'] == 'hero' else (observation['verified_source_hash_joins'][0]['source_dimensions'] if observation['verified_source_hash_joins'] else None)
            require(geometry['original_dimensions'] == original, 'Geometry original relation')
            covered_ids.append(oid)
    require(sorted(covered_ids) == sorted(observed), 'Each observation must appear in exactly one geometry group')
    expected_cells = set()
    for role in roles.values():
        for viewport in viewports.values():
            if role['status'] != 'accepted_code':
                expected_cells.add((role['id'], viewport['id'], None))
            else:
                for geometry in geometries.values():
                    if (geometry['kind'] == 'hero') == (role['id'] == 'story_detail_hero'):
                        tablet = min(number(viewport['width']), number(viewport['height'])) >= 600
                        if geometry['kind'] == 'listing' and not tablet and geometry['profile'] == 'tablet':
                            continue
                        expected_cells.add((role['id'], viewport['id'], geometry['id']))
    cells = data['cells']
    require(len({c['id'] for c in cells}) == len(cells), 'Duplicate cell id')
    require({(c['role'], c['viewport'], c['geometry']) for c in cells} == expected_cells and len(cells) == len(expected_cells), 'Incomplete/duplicate role-viewport-geometry cells')
    for cell in cells:
        role, viewport = roles[cell['role']], viewports[cell['viewport']]
        if cell['geometry'] is None:
            require(cell['status'] == 'unavailable' and cell['reason_gap_ids'] == role['gap_ids'], 'Unimplemented/PoC cell must stay unavailable')
            continue
        geometry = geometries[cell['geometry']]
        tablet = min(number(viewport['width']), number(viewport['height'])) >= 600
        if role['id'] == 'story_detail_hero':
            width = number(viewport['width'])
            slot = [width, width / (number('1.1') if tablet else number('.9'))]
        else:
            height = number(viewport['height']) * number('.230') + 16
            slot = [height * number('.75'), height]
        selection = 'localized_original' if geometry['kind'] == 'hero' else ('tablet_to_phone_fallback_if_tablet_absent' if tablet and geometry['profile'] == 'phone' else 'layout_profile')
        require(cell['selection_path'] == selection, 'Selection/fallback path mismatch')
        require(cell['logical_slot'] == list(map(str, slot)) and cell['tablet'] == tablet, 'Slot formula/breakpoint mismatch')
        require(cell['status'] == 'conditional_calculation' and cell['fit'] == 'cover' and cell['alignment'] == 'center', 'Framing/calculation classification')
        intrinsic, original = geometry['intrinsic_dimensions'], geometry['original_dimensions']
        required = fitted(original or intrinsic, slot, viewport['dpr'], 'cover')
        rendered = fitted(intrinsic, slot, viewport['dpr'], 'cover')
        require(cell['demand'] == required and cell['selected_image_demand'] == rendered, 'Unclamped two-axis demand or bounded decode differs')
        require(cell['original_shortage'] == (required['intrinsic_shortage'] if original else None), 'Original shortage hidden or invented')
        require(cell['rendition_shortage'] == any(i < r for i, r in zip(intrinsic, required['required_full_image'])), 'Rendition shortage')
        decode = hero_decode(intrinsic, tablet) if geometry['kind'] == 'hero' else intrinsic
        request = {'width': decode[0], 'height': decode[1]} if geometry['kind'] == 'hero' else {'width': 600 if tablet else 360, 'height': None}
        if geometry['kind'] == 'listing':
            require(intrinsic[0] <= 360 if not tablet else intrinsic[0] <= 600, 'Listing no-upscale model cannot assume bytes below decode request')
        require(cell['current_decode_request'] == request and cell['modeled_decode_dimensions'] == decode, 'Current bounded decode model')
        require(cell['decode_shortage'] == any(d < r for d, r in zip(decode, rendered['required_full_image'])), 'Decode clamp erased demand')
        require(all(cell[k] is None for k in ('actual_decoded_dimensions', 'actual_decode_shortage', 'resource_shortage', 'quality_qualified')), 'Native/quality/resource observation invented')
    require(data['counts']['calculated_cells'] == sum(c['geometry'] is not None for c in cells), 'Calculated cell count')
    require(data['counts']['unavailable_role_viewport_cells'] == sum(c['geometry'] is None for c in cells), 'Unavailable cell count')
    require(data['evidence_policy']['plans_are_encoded_bytes'] is False, 'Plans counted as bytes')
    require(all(data['gates'][k] is False for k in ('corpus_qualified', 'native_measured', 'profiles_approved', 'full_p0_complete')), 'Open gate closed without evidence')
    raw = inventory_path.read_text()
    require(not re.search(r'https?://|/Users/|/home/|[?&](?:token|X-Goog-|signature)|Bearer\s', raw, re.I), 'Private location/URL/credential copied')
    markdown = (HERE / 'p0a-slot-source-inventory.md').read_text()
    for link in re.findall(r'\]\(([^)]+)\)', markdown):
        target = link.split('#')[0]
        require(not target.startswith(('http:', 'https:', '/')), 'Documentation link must be local')
        require((HERE / target).resolve().exists(), f'Broken local link: {target}')
    for path, digest in before.items():
        require(checksum((ROOT / path).read_bytes()) == digest, f'Input changed during verification: {path}')
    if report:
        print(f'PASS: {len(inputs)} retained inputs; {fixed_count} fixed arithmetic/decode fixtures; provenance, allowlisted joins, counts, {len(cells)} complete cells, privacy and local links.')
        print('OPEN: normalized orientation, corpus/quality/native/resource qualification and full P0. No media fetched, authenticated, encoded or modified.')


def negative_projection_checks():
    """Exercise malformed projections through the same verifier used for delivery."""
    baseline = json.loads(INVENTORY.read_text())
    cases = []

    def add(name, edit):
        candidate = copy.deepcopy(baseline)
        edit(candidate)
        cases.append((name, candidate))

    def omit_and_change(collection, field):
        def edit(data):
            row = data[collection][0]
            row[field] += 1
            del row['evidence']['fields'][field]
        return edit

    add('actualSources bytes changed with bytes mapping omitted', omit_and_change('actual_source_observations', 'bytes'))
    add('hero asset ID changed with asset_id mapping omitted', omit_and_change('hero_observations', 'asset_id'))
    for collection in ('source_bindings', 'rendition_observations', 'historical_controls'):
        add(collection + ' required bytes mapping omitted', omit_and_change(collection, 'bytes'))

    def wrong_byte_mapping(data):
        row = data['actual_source_observations'][0]
        row['bytes'] = row['asset_id']
        row['evidence']['fields']['bytes'] = 'assetId'

    def wrong_asset_mapping(data):
        row = data['hero_observations'][0]
        row['asset_id'] = row['width']
        row['evidence']['fields']['asset_id'] = 'width'

    add('actualSources bytes remapped to globally allowed assetId', wrong_byte_mapping)
    add('hero asset ID remapped to globally allowed width', wrong_asset_mapping)
    # Every auxiliary projection has a separate purpose-specific exact schema.
    selectors = [
        ('source_asset_id', lambda d: next(s['asset_id_observations'][0] for s in d['source_bindings'] if s['asset_id_observations'])),
        ('source_mime', lambda d: next(s['mime_observations'][0] for s in d['source_bindings'] if s['mime_observations'])),
        ('source_metadata', lambda d: next(s['metadata_observations'][0] for s in d['source_bindings'] if s['metadata_observations'])),
        ('rendition_output', lambda d: next(r['verified_source_hash_joins'][0]['output_evidence'] for r in d['rendition_observations'] if r['verified_source_hash_joins'])),
        ('rendition_mime', lambda d: next(r['verified_source_hash_joins'][0]['actual_mime_evidence'] for r in d['rendition_observations'] if r['verified_source_hash_joins'])),
        ('hero_case', lambda d: d['hero_observations'][0]['case_evidence']),
        ('control_metadata', lambda d: d['historical_controls'][0]['metadata_evidence']),
        ('control_corpus', lambda d: d['historical_controls'][0]['corpus_evidence']),
    ]
    for purpose, select in selectors:
        def remove_field(data, select=select):
            evidence = select(data)
            del evidence['fields'][next(iter(evidence['fields']))]
        def broaden_fields(data, select=select):
            evidence = select(data)
            evidence['fields']['unrequested_copy'] = next(iter(evidence['fields'].values()))
        def remap_field(data, select=select):
            evidence = select(data)
            destination = next(iter(evidence['fields']))
            evidence['fields'][destination] = 'width' if evidence['fields'][destination] != 'width' else 'height'
        add(purpose + ' required mapping omitted', remove_field)
        add(purpose + ' wrong source mapping rejected', remap_field)
        add(purpose + ' extra destination rejected', broaden_fields)
    with tempfile.TemporaryDirectory(prefix='p0a-projection-checks-') as directory:
        path = Path(directory) / 'inventory.json'
        for name, candidate in cases:
            path.write_text(json.dumps(candidate))
            try:
                verify(path, report=False)
            except ValueError as error:
                # Failures must come from evidence validation, rather than a
                # later count/geometry check accidentally masking the defect.
                require(any(term in str(error) for term in ('Evidence', 'Source ID/MIME joins', 'Source metadata joins')),
                        f'Negative projection case failed for unrelated reason: {name}: {error}')
            else:
                raise ValueError(f'Corrupted projection accepted: {name}')
    print(f'PASS: {len(cases)} malformed projection inventories rejected through verify(); retained inventory unchanged.')


if __name__ == '__main__':
    try:
        verify()
        negative_projection_checks()
    except (ValueError, KeyError, OSError, StopIteration, subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}', file=sys.stderr)
        sys.exit(1)
