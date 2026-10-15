#!/usr/bin/env python3
"""Bounded source-bound native comparison, private ledgers and descriptive summaries.

Allocation is an operator action. This tool never creates cloud devices, renews
credentials, installs builds or qualifies visual quality without a human verdict.
"""
import argparse
import base64
from contextlib import contextmanager
from hashlib import sha256
import math
from pathlib import Path
import statistics
import sys
import threading
import time

import native_entry_driver as host
from critical_ios_guard_pilot import (HASH_FIELDS, INT_FIELDS, DOUBLE_FIELDS, BOOL_FIELDS,
    STRING_FIELDS, provider_binding, validate_audit, OBSERVER_LOCK)
from native_entry_config import (EntryError, HASH, exact_keys, finite_number, frozen_json,
    INPUT_KEYS, load_config, load_manifest, private_file, private_output, require, strict_json, write_private)
from native_fixture_server import Broker

SCOPE = 'image-optimization-native-comparison-r1'
PRIMARY = 'native_comparison_r1'
QUALITY = 'native_quality_r1'
LOCK = OBSERVER_LOCK
MAX_LEDGER = 262144


def run_plan(platform):
    require(platform in ('ios', 'android'), 'platform_rejected')
    plan = []
    for repetition in range(1, 6 if platform == 'ios' else 2):
        for source in (71, 19):
            arms = ('jpeg', 'webp') if (repetition % 2 == 1 if platform == 'ios' else source == 71) else ('webp', 'jpeg')
            for arm in arms:
                plan.append({'platform': platform, 'source': source, 'repetition': repetition,
                    'arm': arm, 'caseId': f'c{source}_{arm}_q85_r1', 'purpose': PRIMARY})
    return plan


def quality_plan(platform):
    require(platform in ('ios', 'android'), 'platform_rejected')
    return [{'platform': platform, 'source': source, 'arm': arm,
        'caseId': f'c{source}_{"png_reference" if arm == "png" else arm + "_q85"}_r1',
        'purpose': QUALITY, 'maximumRetries': 1} for source in (71, 19) for arm in ('png', 'jpeg', 'webp')]


def budget_before_create(snapshot, *, now=None):
    """A pending full six-minute debit is charged before every proposed allocation."""
    now = time.time() if now is None else now
    exact_keys(snapshot, ('observedAt', 'catalogObservedAt', 'usageObservedAt', 'inventoryObservedAt',
        'ownedReceiptsVerified', 'ownershipUnknown', 'cleanupUnknown', 'activeOrPending',
        'reservationCount', 'reconciledUpperMinutes', 'minuteUsd', 'conversionTryPerUsd',
        'noCostTierMinutes', 'catalogSha256', 'usageSha256', 'inventorySha256', 'ownedReceiptsSha256',
        'costModelRevision', 'maximumReservations'))
    for key in ('observedAt', 'catalogObservedAt', 'usageObservedAt', 'inventoryObservedAt'):
        require(finite_number(snapshot[key]) and 0 <= now - snapshot[key] <= 300, 'budget_snapshot_stale')
    for key in ('catalogSha256', 'usageSha256', 'inventorySha256', 'ownedReceiptsSha256'):
        require(type(snapshot[key]) is str and HASH.fullmatch(snapshot[key]), 'budget_receipt_unverified')
    require(snapshot['ownedReceiptsVerified'] is True and snapshot['ownershipUnknown'] is False and
        snapshot['cleanupUnknown'] is False and type(snapshot['activeOrPending']) is int and
        snapshot['activeOrPending'] == 0, 'allocation_ownership_unknown')
    require(snapshot['costModelRevision'] == 'closed_root_ceil_plus_one_second_r2' and
        type(snapshot['maximumReservations']) is int and snapshot['maximumReservations'] == 13,
        'budget_policy_revision_rejected')
    require(type(snapshot['reservationCount']) is int and 0 <= snapshot['reservationCount'] < 13 and
        type(snapshot['reconciledUpperMinutes']) is int and snapshot['reconciledUpperMinutes'] >= 40,
        'budget_reservation_refused')
    require(snapshot['minuteUsd'] == 0.15 and snapshot['noCostTierMinutes'] == 30 and
        finite_number(snapshot['conversionTryPerUsd']) and snapshot['conversionTryPerUsd'] > 0,
        'budget_catalog_changed')
    minutes = snapshot['reconciledUpperMinutes'] + 6
    usd = max(0, minutes - 30) * 0.15
    amount_try = usd * snapshot['conversionTryPerUsd']
    require(usd < 10 and amount_try < 489.82, 'budget_limit_refused')
    return {'pendingDebitMinutes': 6, 'projectedMonthlyUpperMinutes': minutes,
        'projectedUsageCostUpperUsd': usd, 'projectedUsageCostUpperTry': amount_try,
        'invoice': 'unverified', 'reportedTry0IsSeparate': True,
        'costModelRevision': snapshot['costModelRevision'], 'maximumReservations': 13}


class LedgerChunks:
    """Reconstruct only whitelisted numbered private instrumentation payloads."""
    def __init__(self):
        self.fields, self.parts, self.count, self.digest = {}, [], None, None

    def feed(self, line):
        prefix = b'INSTRUMENTATION_STATUS: '
        if line.startswith(prefix):
            field, sep, value = line[len(prefix):].partition(b'=')
            if sep and field in (b'event', b'index', b'count', b'ledgerSha256', b'payloadBase64'):
                require(field not in self.fields, 'ledger_chunk_duplicate')
                self.fields[field] = value
        elif line.startswith(b'INSTRUMENTATION_STATUS_CODE:'):
            fields, self.fields = self.fields, {}
            if fields.get(b'event') != b'ledger-chunk':
                return
            require(line.strip() == b'INSTRUMENTATION_STATUS_CODE: 2' and set(fields) ==
                {b'event', b'index', b'count', b'ledgerSha256', b'payloadBase64'}, 'ledger_chunk_rejected')
            try:
                index, count = int(fields[b'index']), int(fields[b'count'])
                digest = fields[b'ledgerSha256'].decode('ascii')
                require(1 <= count <= 86 and index == len(self.parts) and index < count and
                    HASH.fullmatch(digest) and len(fields[b'payloadBase64']) <= 4096, 'ledger_chunk_rejected')
                part = base64.b64decode(fields[b'payloadBase64'], validate=True)
            except (ValueError, UnicodeError):
                raise EntryError('ledger_chunk_rejected') from None
            if self.count is None: self.count, self.digest = count, digest
            require(count == self.count and digest == self.digest and 1 <= len(part) <= 3072 and
                (index == count - 1 or len(part) == 3072), 'ledger_chunk_rejected')
            self.parts.append(part)
            require(sum(map(len, self.parts)) <= MAX_LEDGER, 'ledger_capacity')

    def finish(self):
        data = b''.join(self.parts)
        require(self.count is not None and len(self.parts) == self.count and
            sha256(data).hexdigest() == self.digest, 'ledger_incomplete')
        return data


def ledger_rows(data, binding):
    ledger = strict_json(data, MAX_LEDGER)
    exact_keys(ledger, ('schemaVersion', 'records', 'droppedCount'))
    require(type(ledger['schemaVersion']) is int and ledger['schemaVersion'] == 1 and
        type(ledger['droppedCount']) is int and ledger['droppedCount'] == 0 and
        type(ledger['records']) is list and 1 <= len(ledger['records']) <= 512, 'ledger_capacity')
    prior_clock = -1
    for row in ledger['records']:
        require(type(row) is dict, 'ledger_schema_rejected')
        for key, value in row.items():
            valid = (type(value) is str and HASH.fullmatch(value)) if key in HASH_FIELDS else \
                (value is None or type(value) is int and value >= (-1 if key == 'frameNumber' else 0)) if key in INT_FIELDS else \
                finite_number(value) if key in DOUBLE_FIELDS else type(value) is bool if key in BOOL_FIELDS else \
                (type(value) is str and value in STRING_FIELDS[key]) if key in STRING_FIELDS else False
            require(valid, 'ledger_schema_rejected')
        require(row.get('clockDomain') == 'dart_monotonic' and row.get('clockUnit') == 'microseconds' and
            type(row.get('monotonicMicroseconds')) is int and row['monotonicMicroseconds'] >= prior_clock,
            'ledger_clock_rejected')
        prior_clock = row['monotonicMicroseconds']
        for key in ('generation', 'caseSha256', 'providerSha256', 'attemptSha256', 'sessionSha256'):
            require(key not in row or row[key] == binding[key], 'ledger_stale')
        require(row.get('generation') == binding['generation'], 'ledger_stale')
        target_fields = {'generation', 'caseSha256', 'providerSha256', 'attemptSha256'}
        target_event = row.get('event') in {'request', 'body', 'encoded_verified', 'decoded', 'draw', 'layout'} or (
            row.get('event') == 'cache_state' and row.get('cacheState') == 'namespace_keys_absent') or (
            row.get('event') == 'ownership' and row.get('ownership') in {'owned', 'released'})
        if target_event: require(target_fields <= row.keys(), 'ledger_binding_missing')
        if row.get('event') in {'dispatch', 'phase', 'memory_sample', 'release_complete'}:
            require({'generation', 'sessionSha256'} <= row.keys(), 'ledger_binding_missing')
        if row.get('event') == 'dispatch': require('caseSha256' in row, 'ledger_binding_missing')
    require(not any(row.get('event') == 'failure' for row in ledger['records']), 'run_failed')
    return ledger['records']


def memory_summary(rows, platform):
    phases = [row for row in rows if row.get('event') == 'phase']
    names = [row.get('phase') for row in phases]
    require(names == ['baseline', 'load', 'hold', 'releasing', 'post_release', 'complete'], 'phase_sequence_rejected')
    bounds = {row['phase']: row['monotonicMicroseconds'] for row in phases}
    require(bounds['load'] - bounds['baseline'] >= 30000000 and
        0 <= bounds['hold'] - bounds['load'] <= 10000000 and
        bounds['releasing'] - bounds['hold'] >= 10000000 and
        bounds['complete'] - bounds['post_release'] >= 30000000, 'phase_duration_rejected')
    release = [row for row in rows if row.get('event') == 'release_complete']
    require(len(release) == 1 and bounds['releasing'] <= release[0]['monotonicMicroseconds'] <= bounds['post_release'],
        'release_unverified')
    counter = 'ios_phys_footprint' if platform == 'ios' else 'android_total_pss'
    samples = [row for row in rows if row.get('event') == 'memory_sample']
    accepted = []
    for row in samples:
        start, end = row.get('sampleStartMicroseconds'), row.get('sampleEndMicroseconds')
        require(type(start) is int and type(end) is int and 0 <= start <= end <= row['monotonicMicroseconds'] and
            row.get('observerOverheadMicroseconds') == end - start and row.get('sessionSha256') is not None,
            'sample_clock_rejected')
        if row.get('memoryBytes') is not None:
            require(row.get('memoryCounter') == counter and row.get('memoryReason') == 'none' and
                type(row.get('nativeReadMicroseconds')) is int, 'sample_counter_rejected')
        else: require(row.get('memoryReason') != 'none', 'sample_counter_rejected')
        require(row.get('phase') in bounds and bounds[row['phase']] <= start, 'sample_phase_rejected')
        if accepted:
            require(start >= accepted[-1]['sampleEndMicroseconds'], 'sample_overlap_rejected')
            require(row.get('sampleCadenceMicroseconds') == start - accepted[-1]['sampleStartMicroseconds'], 'sample_cadence_rejected')
        else: require(row.get('sampleCadenceMicroseconds') is None, 'sample_cadence_rejected')
        accepted.append(row)
    windows = {
        'baseline': (bounds['baseline'], bounds['load']),
        'loadOnstage': (bounds['load'], bounds['releasing']),
        'finalRelease10s': (bounds['complete'] - 10000000, bounds['complete']),
    }
    summaries = {}
    for name, (start, end) in windows.items():
        window = [row for row in samples if start <= row['sampleStartMicroseconds'] < end and row['sampleEndMicroseconds'] <= end]
        values = [row['memoryBytes'] for row in window if row.get('memoryBytes') is not None]
        starts = [row['sampleStartMicroseconds'] for row in window]
        boundaries = [start, *starts, end]
        summaries[name] = {'sampleCount': len(window), 'validCount': len(values), 'nullCount': len(window) - len(values),
            'expectedCountAt2Hz': math.ceil((end - start) / 500000),
            'maximumGapMicroseconds': max(b - a for a, b in zip(boundaries, boundaries[1:])),
            'observerMicroseconds': sum(row['observerOverheadMicroseconds'] for row in window),
            'nativeReadMicroseconds': sum(row.get('nativeReadMicroseconds') or 0 for row in window),
            'medianBytes': statistics.median(values) if values else None,
            'sampledMaximumBytes': max(values) if values else None}
    base = summaries['baseline']['medianBytes']
    peak = summaries['loadOnstage']['sampledMaximumBytes']
    release_median = summaries['finalRelease10s']['medianBytes']
    return {'counter': counter, 'windows': summaries,
        'eligibility': 'observed_samples' if all(summaries[name]['validCount'] > 0 for name in summaries) else 'missing_phase_counter',
        'sampledLoadOnstageDeltaBytes': peak - base if peak is not None and base is not None else None,
        'finalReleaseDeltaBytes': release_median - base if release_median is not None and base is not None else None,
        'truePeak': None, 'leakConclusion': None, 'packageGpuRetention': 'unknown',
        'accountingViewsComparableAcrossPlatforms': False}


def qualify(data, binding, outcome, *, platform, purpose=PRIMARY, expected_provider=None):
    """Admission must come from provider_binding(admitted_manifest, envelope, case_id), never ledger dimensions."""
    require(platform in ('ios', 'android') and purpose in (PRIMARY, QUALITY), 'selection_rejected')
    exact_keys(binding, ('generation', 'caseSha256', 'providerSha256', 'attemptSha256', 'sessionSha256'))
    require(type(binding['generation']) is int and binding['generation'] >= 0 and
        all(type(binding[key]) is str and HASH.fullmatch(binding[key]) for key in binding if key != 'generation'),
        'binding_rejected')
    exact_keys(expected_provider, ('expectedProviderSha256', 'expectedAttemptSha256',
        'expectedDecodeWidth', 'expectedDecodeHeight'))
    require(all(type(expected_provider[key]) is str and HASH.fullmatch(expected_provider[key]) and
        expected_provider[key] == binding[target] for key, target in
        (('expectedProviderSha256', 'providerSha256'), ('expectedAttemptSha256', 'attemptSha256'))) and
        all(type(expected_provider[key]) is int and expected_provider[key] > 0 for key in
            ('expectedDecodeWidth', 'expectedDecodeHeight')) and
        expected_provider['expectedDecodeWidth'] * expected_provider['expectedDecodeHeight'] <= 16777216,
        'expected_provider_rejected')
    require(outcome.get('mode') == 'release' and outcome.get('platform') == platform and
        outcome.get('sessionSha256') == binding['sessionSha256'] and outcome.get('generation') == binding['generation'] and
        outcome.get('caseSha256') == binding['caseSha256'] and outcome.get('nativeResult') == 'settled' and
        type(outcome.get('generation')) is int and type(outcome.get('fixtureDeliveryCount')) is int and
        outcome['fixtureDeliveryCount'] == 1 and type(outcome.get('fixtureBodyBytes')) is int and outcome['fixtureBodyBytes'] > 0 and
        all(type(outcome.get(key)) is str and HASH.fullmatch(outcome[key]) for key in ('fixtureOutputSha256', 'artifactSha256', 'testArtifactSha256')) and
        outcome.get('teardown') == 'authenticated_app_revoke_observed', 'outcome_unverified')
    rows = ledger_rows(data, binding)
    def first(event):
        matches = [row for row in rows if row.get('event') == event]
        require(matches, 'event_unavailable')
        return matches[0]
    for event in ('dispatch', 'encoded_verified', 'decoded', 'layout', 'release_complete'):
        require(sum(row.get('event') == event for row in rows) == 1, 'event_duplicate_or_missing')
    requests = [row for row in rows if row.get('event') == 'request']
    body_rows = [row for row in rows if row.get('event') == 'body']
    require(len(requests) == 1 and requests[0].get('requestCount') == 1 and
        type(requests[0].get('requestCount')) is int and requests[0].get('retryCount') == 0 and
        type(requests[0].get('retryCount')) is int and body_rows and
        all(type(row.get('bodyBytes')) is int and row['bodyBytes'] > 0 for row in body_rows) and
        sum(row['bodyBytes'] for row in body_rows) == outcome['fixtureBodyBytes'], 'transfer_unverified')
    require(all('retryCount' not in row or row['retryCount'] == 0 for row in rows), 'retry_unverified')
    owned = [row for row in rows if row.get('event') == 'ownership' and row.get('ownership') == 'owned']
    released = [row for row in rows if row.get('event') == 'ownership' and row.get('ownership') == 'released']
    require(len(released) == 1 and len(owned) == 1 and all(owned[0].get(key) == binding[key] for key in
        ('generation', 'caseSha256', 'providerSha256', 'attemptSha256')) and
        all(key in owned[0] for key in ('sourceSha256', 'referenceSha256', 'settingsSha256', 'scopeSha256')), 'ownership_unverified')
    absence = [row for row in rows if row.get('event') == 'cache_state' and row.get('cacheState') == 'namespace_keys_absent']
    require(len(absence) == 1 and absence[0]['monotonicMicroseconds'] <= first('encoded_verified')['monotonicMicroseconds'], 'cache_absence_unverified')
    verified, decoded, draw, dispatched, layout = map(first, ('encoded_verified', 'decoded', 'draw', 'dispatch', 'layout'))
    require(dispatched['monotonicMicroseconds'] <= absence[0]['monotonicMicroseconds'] <= requests[0]['monotonicMicroseconds'] and
        all(requests[0]['monotonicMicroseconds'] <= row['monotonicMicroseconds'] <= verified['monotonicMicroseconds'] for row in body_rows),
        'transfer_sequence_rejected')
    require(verified.get('encodedVerified') is True and dispatched['monotonicMicroseconds'] <=
        verified['monotonicMicroseconds'] <= decoded['monotonicMicroseconds'] <= draw['monotonicMicroseconds'],
        'event_sequence_rejected')
    require(all(key in decoded for key in ('actualDecodeWidth', 'actualDecodeHeight')) and
        decoded['actualDecodeWidth'] > 0 and decoded['actualDecodeHeight'] > 0 and
        all(key in layout and layout[key] > 0 for key in ('observedSlotWidth', 'observedSlotHeight', 'observedDpr')) and
        layout['observedSlotWidth'] == 280 and layout['observedSlotHeight'] == 460, 'layout_unavailable')
    require(draw['monotonicMicroseconds'] <= layout['monotonicMicroseconds'] and
        layout['actualDecodeWidth'] == decoded['actualDecodeWidth'] and layout['actualDecodeHeight'] == decoded['actualDecodeHeight'],
        'layout_decode_mismatch')
    require(decoded['actualDecodeWidth'] == expected_provider['expectedDecodeWidth'] and
        decoded['actualDecodeHeight'] == expected_provider['expectedDecodeHeight'], 'admitted_decode_mismatch')
    load = [row for row in rows if row.get('event') == 'phase' and row.get('phase') == 'load']
    hold = [row for row in rows if row.get('event') == 'phase' and row.get('phase') == 'hold']
    require(len(load) == len(hold) == 1 and all(load[0]['monotonicMicroseconds'] <= row['monotonicMicroseconds'] <
        hold[0]['monotonicMicroseconds'] for row in
        (dispatched, owned[0], absence[0], requests[0], *body_rows, verified, decoded, draw, layout)),
        'target_outside_load')
    require(draw['monotonicMicroseconds'] < released[0]['monotonicMicroseconds'] <=
        first('release_complete')['monotonicMicroseconds'], 'release_unverified')
    result = {'schemaVersion': 1, 'scope': SCOPE, 'status': 'observed', 'platform': platform, 'purpose': purpose,
        **binding, 'ledgerSha256': sha256(data).hexdigest(), 'recordCount': len(rows),
        'dispatchToReadyMicroseconds': decoded['monotonicMicroseconds'] - dispatched['monotonicMicroseconds'],
        'dispatchToTargetPaintMicroseconds': draw['monotonicMicroseconds'] - dispatched['monotonicMicroseconds'],
        'clock': 'dart_monotonic', 'rasterCompletion': None, 'physicalPresentation': None,
        'layout': {key: layout[key] for key in ('observedSlotWidth', 'observedSlotHeight', 'observedDpr')},
        'decode': {key: decoded[key] for key in ('actualDecodeWidth', 'actualDecodeHeight')},
        'fixtureBodyBytes': outcome['fixtureBodyBytes'], 'fixtureOutputSha256': outcome['fixtureOutputSha256'],
        'comparisonIdentity': {key: owned[0][key] for key in ('sourceSha256', 'referenceSha256', 'settingsSha256', 'scopeSha256')},
        'artifactSha256': outcome.get('artifactSha256'), 'testArtifactSha256': outcome.get('testArtifactSha256'),
        'memory': memory_summary(rows, platform) if purpose == PRIMARY else None,
        'quality': 'human_verdict_pending', 'qualityQualifiedSavings': None,
        'osNetworkColdCertified': False}
    if purpose == QUALITY:
        require(not any(row.get('event') == 'memory_sample' for row in rows), 'quality_sampling_rejected')
        phase_rows = [row for row in rows if row.get('event') == 'phase']
        require([row['phase'] for row in phase_rows] == ['load', 'hold', 'releasing', 'complete'], 'phase_sequence_rejected')
        bounds = {row['phase']: row['monotonicMicroseconds'] for row in phase_rows}
        require(0 <= bounds['hold'] - bounds['load'] <= 10000000 and
            bounds['releasing'] - bounds['hold'] >= 10000000 and
            bounds['releasing'] <= first('release_complete')['monotonicMicroseconds'] <= bounds['complete'],
            'phase_duration_rejected')
    return result


CAPTURE_ID_FIELDS = {'generation', 'caseSha256', 'providerSha256', 'attemptSha256', 'sessionSha256'}
CAPTURE_GEOMETRY_FIELDS = {'observedSlotWidth', 'observedSlotHeight', 'observedDpr', 'actualDecodeWidth', 'actualDecodeHeight'}
CAPTURE_CONTEXT_FIELDS = {'phase', 'orientation', 'nativeContext', 'holdDeadlineEpochMilliseconds'}


def stable_capture_binding(row):
    return {key: value for key, value in row.items() if key != 'nativeObservedAtEpochMilliseconds'}


def validate_capture_binding(row, binding, platform):
    exact_keys(binding, CAPTURE_ID_FIELDS)
    require(type(binding['generation']) is int and binding['generation'] >= 0 and
        all(type(binding[key]) is str and HASH.fullmatch(binding[key]) for key in CAPTURE_ID_FIELDS - {'generation'}),
        'capture_binding_rejected')
    fields = CAPTURE_ID_FIELDS | CAPTURE_GEOMETRY_FIELDS | CAPTURE_CONTEXT_FIELDS
    if platform == 'android': fields |= {'nativeObservedAtEpochMilliseconds'}
    exact_keys(row, fields)
    require(type(row['generation']) is int and row['generation'] >= 0 and
        all(type(row[key]) is str and HASH.fullmatch(row[key]) for key in CAPTURE_ID_FIELDS - {'generation'}) and
        all(row.get(key) == value for key, value in binding.items()), 'capture_binding_rejected')
    require(all(finite_number(row[key]) and row[key] > 0 for key in ('observedSlotWidth', 'observedSlotHeight', 'observedDpr')) and
        row['observedSlotWidth'] == 280 and row['observedSlotHeight'] == 460 and
        all(type(row[key]) is int and row[key] > 0 for key in ('actualDecodeWidth', 'actualDecodeHeight')) and
        row['phase'] == 'hold' and row['orientation'] == 'portrait' and row['nativeContext'] ==
        ('foreground_activity' if platform == 'android' else 'foreground_application') and
        type(row['holdDeadlineEpochMilliseconds']) is int and row['holdDeadlineEpochMilliseconds'] > 0,
        'capture_context_rejected')
    if platform == 'android':
        require(type(row['nativeObservedAtEpochMilliseconds']) is int and
            0 < row['holdDeadlineEpochMilliseconds'] - row['nativeObservedAtEpochMilliseconds'] <= 10000,
            'capture_hold_expired')


def qualify_capture(png, receipt, binding, provenance):
    """Only a current native hold with equal identities can certify a capture."""
    from io import BytesIO
    from PIL import Image
    exact_keys(receipt, ('schemaVersion', 'before', 'after', 'beforeReadEpochMilliseconds',
        'afterReadEpochMilliseconds', 'pngSha256', 'pngBytes', 'capture', 'orientation', 'attachments'))
    require(type(receipt['schemaVersion']) is int and receipt['schemaVersion'] == 1 and
        type(receipt['pngBytes']) is int and receipt['pngBytes'] > 0 and
        receipt['capture'] in ('private_xctest_screen', 'exact_device_adb_screencap') and
        receipt['orientation'] == 'portrait' and receipt['attachments'] is False and
        receipt['pngSha256'] == sha256(png).hexdigest() and receipt['pngBytes'] == len(png), 'capture_unverified')
    platform = 'ios' if receipt['capture'] == 'private_xctest_screen' else 'android'
    for row in (receipt['before'], receipt['after']): validate_capture_binding(row, binding, platform)
    require(stable_capture_binding(receipt['before']) == stable_capture_binding(receipt['after']), 'capture_binding_rejected')
    before_at, after_at = receipt['beforeReadEpochMilliseconds'], receipt['afterReadEpochMilliseconds']
    deadline = receipt['before']['holdDeadlineEpochMilliseconds']
    require(type(before_at) is int and type(after_at) is int and deadline - 10000 <= before_at <= after_at < deadline,
        'capture_hold_expired')
    if platform == 'android':
        require(before_at == receipt['before']['nativeObservedAtEpochMilliseconds'] and
            after_at == receipt['after']['nativeObservedAtEpochMilliseconds'], 'capture_clock_rejected')
    with Image.open(BytesIO(png)) as image:
        require(image.format == 'PNG' and 0 < image.width < image.height and image.width * image.height <= 16777216 and image.n_frames == 1, 'capture_format_rejected')
        require(image.width >= math.ceil(receipt['before']['observedSlotWidth'] * receipt['before']['observedDpr']) and
            image.height >= math.ceil(receipt['before']['observedSlotHeight'] * receipt['before']['observedDpr']),
            'capture_layout_unavailable')
        image.load()
        width, height = image.size
    exact_keys(provenance, ('sourceSha256', 'referenceSha256', 'referencePngSha256', 'normalizedPixelEquality'))
    require(all(type(provenance[key]) is str and HASH.fullmatch(provenance[key]) for key in
        ('sourceSha256', 'referenceSha256', 'referencePngSha256')) and
        provenance['normalizedPixelEquality'] is True, 'png_provenance_unverified')
    return {'captureSha256': sha256(png).hexdigest(), 'captureWidth': width, 'captureHeight': height,
        'providerBinding': stable_capture_binding(receipt['before']), 'pngProvenance': provenance,
        'quality': 'human_verdict_pending', 'lettering': None, 'diacritics': None, 'lines': None,
        'gradients': None, 'color': None, 'framing': None, 'qualityQualifiedSavings': None}


class AndroidQualityBindingReader:
    """A read waits for a newer exact-Activity native observation, never cached UI state."""
    def __init__(self, binding):
        self.binding = binding
        self.condition = threading.Condition()
        self.fields, self.row, self.revision, self.closed = {}, None, 0, False
        self.received_at = 0.0

    def feed(self, line):
        prefix = b'INSTRUMENTATION_STATUS: '
        if line.startswith(prefix):
            field, sep, value = line[len(prefix):].partition(b'=')
            if sep and field in (b'event', b'bindingBase64'):
                require(field not in self.fields, 'quality_binding_duplicate')
                self.fields[field] = value
        elif line.startswith(b'INSTRUMENTATION_STATUS_CODE:'):
            fields, self.fields = self.fields, {}
            if fields.get(b'event') != b'quality-binding': return
            require(line.strip() == b'INSTRUMENTATION_STATUS_CODE: 3' and set(fields) == {b'event', b'bindingBase64'} and
                len(fields[b'bindingBase64']) <= 5500, 'quality_binding_rejected')
            try: row = strict_json(base64.b64decode(fields[b'bindingBase64'], validate=True), 4096)
            except ValueError: raise EntryError('quality_binding_rejected') from None
            if row is not None:
                require(self.binding() is not None, 'quality_session_unavailable')
                validate_capture_binding(row, self.binding(), 'android')
            with self.condition:
                self.row, self.received_at = row, time.monotonic()
                self.revision += 1
                self.condition.notify_all()

    def read(self):
        expires = time.monotonic() + .6
        with self.condition:
            revision = self.revision
            while not self.closed and self.revision <= revision and time.monotonic() < expires:
                self.condition.wait(timeout=max(0, expires - time.monotonic()))
            require(not self.closed and self.revision > revision and self.row is not None and
                time.monotonic() - self.received_at <= .25, 'quality_binding_unavailable')
            validate_capture_binding(self.row, self.binding(), 'android')
            return dict(self.row)

    def close(self):
        with self.condition:
            self.closed = True
            self.condition.notify_all()


def capture_android(config, output, *, deadline, read_binding, expected_binding):
    """The operator supplies live before/after bindings from the exact owned Activity.

    ADB stdout is a bounded private PNG stream from the admitted selector. No
    screenshot is attached or logged to the terminal; stale bindings refuse it.
    """
    import os
    import selectors
    import signal
    import subprocess
    require(config.values['platform'] == 'android' and config.values['batchId'] == QUALITY,
        'capture_selection_rejected')
    before = read_binding()
    validate_capture_binding(before, expected_binding, 'android')
    require(before['holdDeadlineEpochMilliseconds'] - before['nativeObservedAtEpochMilliseconds'] > 6000, 'capture_hold_budget_refused')
    command = ['adb', '-s', config.values['deviceSelector'], 'exec-out', 'screencap', '-p']
    process = None
    selector = selectors.DefaultSelector()
    chunks, size = [], 0
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
            stdin=subprocess.DEVNULL, env=host.tool_environment(), start_new_session=True)
        selector.register(process.stdout, selectors.EVENT_READ)
        expires = min(deadline, time.monotonic() + 5)
        while selector.get_map():
            require(time.monotonic() < expires, 'capture_timeout')
            for key, _ in selector.select(min(.05, max(0, expires - time.monotonic()))):
                chunk = os.read(key.fileobj.fileno(), 65536)
                if not chunk: selector.unregister(key.fileobj); continue
                size += len(chunk)
                require(size <= 8 * 1024 * 1024, 'capture_capacity')
                chunks.append(chunk)
        require(process.wait(timeout=max(.001, expires - time.monotonic())) == 0, 'capture_failed')
        png = b''.join(chunks)
        write_private(Path(output) / 'quality.png', png) # Retain the original even if the after-read refuses.
        after = read_binding()
        validate_capture_binding(after, expected_binding, 'android')
        require(stable_capture_binding(before) == stable_capture_binding(after), 'capture_binding_changed')
        png = b''.join(chunks)
        require(png.startswith(b'\x89PNG\r\n\x1a\n'), 'capture_format_rejected')
        receipt = {'schemaVersion': 1, 'before': before, 'after': after,
            'beforeReadEpochMilliseconds': before['nativeObservedAtEpochMilliseconds'],
            'afterReadEpochMilliseconds': after['nativeObservedAtEpochMilliseconds'], 'pngSha256': sha256(png).hexdigest(),
            'pngBytes': len(png), 'capture': 'exact_device_adb_screencap', 'orientation': 'portrait', 'attachments': False}
        write_private(Path(output) / 'quality-capture.json', receipt)
        return receipt
    finally:
        if process is not None:
            try: os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError: pass
            process.wait(timeout=1)
            process.stdout.close()
        selector.close()


def paired_differences(results):
    """Input is already qualified. Preserve missing pairs and never pool Android quantiles."""
    pairs = {}
    for item in results:
        key = (item['platform'], item['source'], item['repetition'])
        require(item['arm'] in ('jpeg', 'webp') and item['arm'] not in pairs.setdefault(key, {}), 'pair_duplicate')
        pairs[key][item['arm']] = item
    output = []
    for (platform, source, repetition), arms in sorted(pairs.items()):
        complete = set(arms) == {'jpeg', 'webp'} and all(item.get('status') == 'observed' for item in arms.values())
        if complete:
            left, right = arms['jpeg'], arms['webp']
            require(all(left.get(key) == right.get(key) and left.get(key) is not None for key in
                ('scope', 'purpose', 'layout', 'decode', 'comparisonIdentity', 'artifactSha256', 'testArtifactSha256')) and
                type(left.get('comparisonIdentity')) is dict and set(left['comparisonIdentity']) ==
                {'sourceSha256', 'referenceSha256', 'settingsSha256', 'scopeSha256'} and
                type(left.get('memory')) is dict and type(right.get('memory')) is dict and
                left['memory'].get('counter') == right['memory'].get('counter') ==
                ('ios_phys_footprint' if platform == 'ios' else 'android_total_pss'), 'pair_incomparable')
        output.append({'platform': platform, 'source': source, 'repetition': repetition,
            'status': 'paired' if complete else 'incomplete', 'denominator': 1 if complete else 0,
            'readyDifferenceMicroseconds': arms['webp']['dispatchToReadyMicroseconds'] - arms['jpeg']['dispatchToReadyMicroseconds'] if complete else None,
            'paintDifferenceMicroseconds': arms['webp']['dispatchToTargetPaintMicroseconds'] - arms['jpeg']['dispatchToTargetPaintMicroseconds'] if complete else None,
            'bodyDifferenceBytes': arms['webp']['fixtureBodyBytes'] - arms['jpeg']['fixtureBodyBytes'] if complete else None,
            'quantile': None})
    return output


@contextmanager
def observed_entry(state, *, capture_config=None, output=None, deadline=None):
    """Observe admitted returns without weakening original guards or cleanup."""
    require(LOCK.acquire(blocking=False), 'comparison_overlap')
    original_broker, original_pairing = host.Broker, host.LauncherPairing
    class Observed(Broker):
        def claim(self, value):
            data = super().claim(value)
            envelope = strict_json(data)
            identity = provider_binding(self.manifest, envelope, self.case_id)
            state['expected_provider'] = identity
            state['binding'] = {'generation': envelope['generation'],
                'sessionSha256': sha256(envelope['sessionId'].encode()).hexdigest(),
                'caseSha256': sha256(self.case_id.encode()).hexdigest(),
                'providerSha256': identity['expectedProviderSha256'], 'attemptSha256': identity['expectedAttemptSha256']}
            return data
    class Pairing(original_pairing):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.chunks = LedgerChunks()
            state['pairing'] = self
            self.reader = AndroidQualityBindingReader(lambda: state.get('binding')) if capture_config is not None else None
            self.capture_thread = None
            self.capture_error = None
        def capture(self):
            try:
                state['capture'] = capture_android(capture_config, output, deadline=deadline,
                    read_binding=self.reader.read, expected_binding=state['binding'])
            except EntryError as error:
                self.capture_error = str(error)
                write_private(Path(output) / 'quality-capture-failure.json', {'scope': SCOPE, 'reason': str(error)})
            except (OSError, ValueError, KeyError, TypeError):
                self.capture_error = 'capture_failed'
                write_private(Path(output) / 'quality-capture-failure.json', {'scope': SCOPE, 'reason': 'capture_failed'})
        def __call__(self, line):
            self.chunks.feed(line)
            if self.reader is not None:
                self.reader.feed(line)
                if self.reader.row is not None and self.capture_thread is None:
                    self.capture_thread = threading.Thread(target=self.capture, daemon=True)
                    self.capture_thread.start()
            super().__call__(line)
        def poll(self):
            super().poll()
            if self.capture_error is not None: raise EntryError(self.capture_error)
        def close(self):
            if self.reader is not None: self.reader.close()
            if self.capture_thread is not None: self.capture_thread.join(timeout=6)
            super().close()
    try:
        host.Broker, host.LauncherPairing = Observed, Pairing
        yield
    finally:
        host.Broker, host.LauncherPairing = original_broker, original_pairing
        LOCK.release()


def validate_comparison_audit(path, build_path, build, proof, platform):
    if platform == 'ios': return validate_audit(path, build_path, build, proof)
    require(platform == 'android' and build.get('platform') == 'android' and build.get('mode') == 'release' and
        type(build.get('expectedAppId')) is str and build['expectedAppId'].endswith('.imageexperiment.release') and
        build.get('testAppId') == build['expectedAppId'] + '.test', 'audit_identity_rejected')
    data = private_file(path)
    audit = strict_json(data)
    exact_keys(audit, ('scope', 'observedAt', 'buildReceiptSha256', 'artifacts', 'sourceSha256', 'deviceSelectorSha256'))
    require(audit.get('scope') == 'image-optimization-actual-android-apk-readback' and
        audit.get('buildReceiptSha256') == sha256(private_file(build_path, 2 * 1024 * 1024)).hexdigest() and
        audit.get('sourceSha256') == build['sourceSha256'] and
        audit.get('deviceSelectorSha256') == proof['deviceSelectorSha256'] and
        proof['installationReceiptSha256'] == sha256(data).hexdigest(), 'audit_binding_rejected')
    artifacts = audit.get('artifacts')
    require(type(artifacts) is list and len(artifacts) == 2, 'audit_binding_rejected')
    for item, id_key, hash_key, installed_key in zip(artifacts,
        ('expectedAppId', 'testAppId'), ('artifactSha256', 'testArtifactSha256'),
        ('installedArtifactSha256', 'installedTestArtifactSha256')):
        exact_keys(item, ('bundleId', 'observedSha256', 'certificateSha256', 'certificateBinding',
            'independentDeviceCertificateExtraction', 'fullDeviceApkPullPerformed', 'deviceHashMethod',
            'regularFileMetadataStable', 'observedByteCount'))
        artifact_path = build['artifactPath' if id_key == 'expectedAppId' else 'testArtifactPath']
        observed_host = host.artifact_observation(artifact_path)
        artifact_size = observed_host['byteCount']
        require(observed_host['sha256'] == build[hash_key], 'audit_artifact_changed')
        require(item.get('bundleId') == build[id_key] and item.get('observedSha256') == build[hash_key] == proof[installed_key] and
            item.get('certificateSha256') == build['certificateSha256'] and
            item.get('certificateBinding') == 'host_verified_signature_and_exact_full_device_apk_equivalence' and
            item.get('independentDeviceCertificateExtraction') is False and item.get('fullDeviceApkPullPerformed') is False and
            item.get('deviceHashMethod') == 'toybox_sha256sum' and item.get('regularFileMetadataStable') is True and
            type(item.get('observedByteCount')) is int and item['observedByteCount'] == artifact_size and artifact_size > 0, 'audit_binding_rejected')
    require(type(audit.get('observedAt')) is int and 0 <= int(time.time() * 1000) - audit['observedAt'] <= 300000,
        'audit_stale')
    return sha256(data).hexdigest()


def require_fresh_android_process(config, output, deadline):
    host.checked_run(['adb', '-s', config.values['deviceSelector'], 'shell', 'am', 'force-stop',
        config.values['expectedAppId']], output, 'fresh-process-stop', deadline=deadline)
    stopped = host.run_bounded(['adb', '-s', config.values['deviceSelector'], 'shell', 'pidof',
        config.values['expectedAppId']], output, 'fresh-process-check', seconds=min(5, deadline - time.monotonic()))
    require(stopped['exitCode'] == 1 and (Path(output) / 'fresh-process-check.stdout').read_bytes().strip() == b'',
        'fresh_process_unverified')


def run(config, manifest, output, *, case_id, build_path, prerequisite_path, audit_path):
    require(config.values['mode'] == 'release' and config.values['batchId'] in (PRIMARY, QUALITY), 'selection_rejected')
    entry = next((entry for entry in manifest.entries if entry['caseId'] == case_id), None)
    require(entry is not None and entry['purposeAcceptanceReceipt'] == config.values['batchId'] and
        entry['logicalSlotWidth'] == 280 and entry['logicalSlotHeight'] == 460 and entry['fit'] == 'contain' and
        entry['alignmentX'] == entry['alignmentY'] == 0, 'purpose_geometry_rejected')
    build, proof = host.validate_physical_prerequisites(config, build_path, prerequisite_path)
    validate_comparison_audit(audit_path, build_path, build, proof, config.values['platform'])
    source_digest = host.source_snapshot()[1]
    tool_digests = {name: sha256((Path(__file__).parent / name).read_bytes()).hexdigest() for name in
        ('native_artwork_comparison.py', 'critical_ios_guard_pilot.py', 'critical_android_guard_pilot.py')}
    write_private(Path(output) / 'comparison-tool-freeze.json', {'schemaVersion': 1,
        'sourceSha256': source_digest, 'toolSha256': tool_digests,
        'manifestSha256': manifest.private_sha256, 'publicManifestSha256': manifest.public_sha256,
        'artifactSha256': build['artifactSha256'], 'testArtifactSha256': build['testArtifactSha256']})
    deadline = time.monotonic() + host.TOTAL_SECONDS
    if config.values['platform'] == 'android': require_fresh_android_process(config, output, deadline)
    state = {}
    with observed_entry(state, capture_config=config if config.values['platform'] == 'android' and config.values['batchId'] == QUALITY else None,
            output=output, deadline=deadline):
        outcome = host.verify_entry(config, manifest, output, deadline, case_id=case_id,
            build_path=build_path, prerequisite_path=prerequisite_path,
            quality_capture=config.values['batchId'] == QUALITY)
    require(outcome['fixtureDeliveryCount'] == 1 and 'binding' in state, 'comparison_delivery_unverified')
    require(host.source_snapshot()[1] == source_digest and all(sha256((Path(__file__).parent / name).read_bytes()).hexdigest() ==
        digest for name, digest in tool_digests.items()), 'comparison_source_changed')
    write_private(Path(output) / 'run-binding.json', state['binding'])
    write_private(Path(output) / 'expected-provider.json', state['expected_provider'])
    if config.values['platform'] == 'android':
        if config.values['batchId'] == QUALITY: require('capture' in state, 'capture_unavailable')
        data = state['pairing'].chunks.finish()
        write_private(Path(output) / 'device-ledger.json', data)
        result = qualify(data, state['binding'], outcome, platform='android', purpose=config.values['batchId'],
            expected_provider=state['expected_provider'])
        write_private(Path(output) / 'comparison-result.json', result)
        return result
    return {'scope': SCOPE, 'status': 'awaiting_private_device_ledger', **state['binding'],
        'qualityCaptureLocation': 'private_runner_application_support' if config.values['batchId'] == QUALITY else None}


def backend_inputs(config):
    receipt = strict_json(private_file(config.values['backendStartupReceiptPath'], 256 * 1024), 256 * 1024)
    require(type(receipt) is dict and all(type(receipt.get(key)) is str for key in ('artifactPath', 'configPath')),
        'backend_receipt_rejected')
    return [receipt['artifactPath'], receipt['configPath']]


def main(argv=None):
    parser = host.SafeParser(description='Bounded native artwork comparison')
    commands = parser.add_subparsers(dest='mode', required=True)
    plan = commands.add_parser('plan'); plan.add_argument('--platform', choices=('ios', 'android'), required=True)
    execute = commands.add_parser('run')
    for name in ('config', 'case-id', 'build-receipt', 'prerequisite-receipt', 'audit-receipt'):
        execute.add_argument('--' + name, required=True)
    check = commands.add_parser('qualify')
    for name in ('device-ledger', 'run-binding', 'verification-receipt', 'expected-provider', 'private-output', 'platform'):
        check.add_argument('--' + name, required=True)
    check.add_argument('--purpose', choices=(PRIMARY, QUALITY), default=PRIMARY)
    args = parser.parse_args(argv)
    output = None
    try:
        if args.mode == 'plan':
            result = {'primary': run_plan(args.platform), 'quality': quality_plan(args.platform)}
        elif args.mode == 'run':
            config = load_config(args.config); manifest = load_manifest(config)
            output = private_output(config.values['privateOutputRoot'], [args.config, args.build_receipt,
                args.prerequisite_receipt, args.audit_receipt, *[config.values[key] for key in INPUT_KEYS],
                *backend_inputs(config), *[item['filePath'] for item in strict_json(
                    private_file(config.values['manifestPath'], 100 * 1024 * 1024))['entries']]], create=True)
            result = run(config, manifest, output, case_id=args.case_id, build_path=args.build_receipt,
                prerequisite_path=args.prerequisite_receipt, audit_path=args.audit_receipt)
        else:
            output = private_output(args.private_output, [args.device_ledger, args.run_binding, args.verification_receipt,
                args.expected_provider], create=True)
            result = qualify(private_file(args.device_ledger, MAX_LEDGER), strict_json(private_file(args.run_binding)),
                strict_json(private_file(args.verification_receipt)), platform=args.platform, purpose=args.purpose,
                expected_provider=strict_json(private_file(args.expected_provider)))
            write_private(Path(output) / 'comparison-result.json', result)
        print(frozen_json(result).decode('ascii')); return 0
    except EntryError as error:
        result = {'schemaVersion': 1, 'scope': SCOPE, 'status': 'failed', 'reason': str(error)}
        if output is not None: write_private(Path(output) / 'comparison-failure.json', result)
        print(frozen_json(result).decode('ascii')); return 4
    except (OSError, ValueError, KeyError, TypeError):
        result = {'schemaVersion': 1, 'scope': SCOPE, 'status': 'refused', 'reason': 'operation_failed'}
        if output is not None: write_private(Path(output) / 'comparison-failure.json', result)
        print(frozen_json(result).decode('ascii')); return 4


if __name__ == '__main__': sys.exit(main())
