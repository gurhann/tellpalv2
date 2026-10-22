"""Mühürlü P5e oturumunu native ağ sırası ve değişmez geometriyle doğrular."""
from __future__ import annotations
import math
from p5d_offline_app import require, TEXT_CROPS


def validate_session(rows, nonce, pins):
    require(rows and all(row.get('runNonce') == nonce for row in rows), 'session_nonce_refused')
    groups = {}
    for row in rows:
        key = row.get('sessionId')
        require(isinstance(key, str) and key, 'session_id_refused')
        groups.setdefault(key, []).append(row)
    sealed = [group for group in groups.values() if any(row.get('event') == 'seal' for row in group)]
    require(bool(sealed), 'sealed_session_missing')
    # En son mühürlü oturum bağımsız kabul edilir; önceki denemeler arşivde kalır.
    group = sealed[-1]
    require(group[0].get('event') == 'start', 'session_start_missing')
    require([row.get('eventSequence') for row in group] == list(range(1, len(group) + 1)), 'session_log_gap')
    network, sequence, started, finished = 'unknown', 0, False, False
    presentations, geometry_by_asset = [], {}
    pins_by_case = {row['caseId']: row for row in pins}
    for row in group:
        event = row.get('event')
        if finished:
            require(event != 'presentation' and event != 'seal', 'presentation_after_seal')
            continue
        if event == 'network':
            require(row.get('sequence') == sequence + 1, 'network_sequence_gap')
            sequence = row['sequence']
            network = row.get('network')
            require(not started or network == 'unsatisfied', 'network_returned_before_seal')
        elif event == 'lifecycle':
            require(not started or row.get('active') is True, 'background_before_seal')
        elif event in ('presentation', 'seal'):
            require(network == row.get('network') == row.get('nativeNetwork') == 'unsatisfied' and
                    row.get('nativeNetworkSequence') == sequence, 'presentation_network_refused')
            if event == 'seal':
                require(started, 'empty_session_seal')
                finished = True
                continue
            started = True
            pin = row.get('case')
            require(isinstance(pin, dict) and pin == pins_by_case.get(pin.get('caseId')), 'session_pin_refused')
            require(row.get('decodedWidth') == 1712 and row.get('decodedHeight') == 2778 and
                    row.get('decoderColorSpace') in ('sRGB', 'extendedSRGB', 'displayP3'), 'decoder_metadata_refused')
            require(type(row.get('crop')) is bool, 'crop_mode_refused')
            validate_geometry(row, geometry_by_asset)
            presentations.append(row)
        elif event == 'deviceSpace':
            require(not started and isinstance(row.get('availableDiskBytesAfterInstall'), int) and
                    row['availableDiskBytesAfterInstall'] >= 16 * 1024 * 1024, 'native_disk_measurement_refused')
        else:
            require(event == 'start' and row is group[0], 'unknown_session_event')
    require(finished, 'session_seal_missing')
    expected = {(pin['caseId'], mode) for pin in pins for mode in (False, True)}
    seen = {(row['case']['caseId'], row['crop']) for row in presentations}
    require(expected <= seen, 'incomplete_24_views')
    return {'presentations': len(presentations), 'distinctViews': len(seen), 'all24Views': True,
            'sessionId': group[0]['sessionId'], 'offlineSession': 'SEALED_VERIFIED',
            'humanQualityAcceptance': 'NOT_RUN'}


def validate_geometry(row, previous):
    geometry, pin, crop = row.get('geometry'), row['case'], row['crop']
    require(isinstance(geometry, dict), 'geometry_missing')
    for key in ('logicalWidth', 'logicalHeight', 'dpr'):
        value = geometry.get(key)
        require(isinstance(value, (int, float)) and not isinstance(value, bool) and
                math.isfinite(value) and value > 0, 'geometry_value_refused')
    require(geometry['dpr'] == 3, 'iphone12_dpr_refused')
    require(geometry.get('fit') == ('1to1' if crop else 'cover' if pin['role'] == 'cover' else 'contain') and
            geometry.get('filterQuality') == ('none' if crop else 'low'), 'geometry_filter_refused')
    key = (pin['assetId'], crop)
    if key in previous:
        require(previous[key] == geometry, 'format_viewport_mismatch')
    previous[key] = geometry
    if crop:
        roi = geometry.get('roi', {})
        require((roi.get('x'), roi.get('y')) == TEXT_CROPS[pin['assetId']] and
                roi.get('width') == min(math.floor(geometry['logicalWidth'] * geometry['dpr']), 1200) and
                roi.get('height') == min(math.floor(geometry['logicalHeight'] * geometry['dpr']), 600) and
                roi.get('physicalPixelsPerSourcePixel') == 1, 'crop_1to1_refused')
    else:
        require('roi' not in geometry, 'unexpected_crop')
