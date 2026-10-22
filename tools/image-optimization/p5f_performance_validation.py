"""P5f kayıtları P5e sunum kanıtından bağımsız olarak doğrulanır."""
from __future__ import annotations
from collections import Counter
import hashlib
import json
import math
from statistics import median

ORDERS = ((0, 1, 2), (0, 2, 1), (1, 0, 2), (1, 2, 0), (2, 0, 1), (2, 1, 0))


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def plan():
    rows = [dict(ordinal=i, round=-1, index=i, warmup=True) for i in range(12)]
    for turn in range(12):
        for image in (range(4) if turn % 2 == 0 else range(3, -1, -1)):
            for arm in ORDERS[turn % 6]:
                rows.append(dict(ordinal=len(rows), round=turn, index=image * 3 + arm, warmup=False))
    return rows


def number(value, name, *, positive=False):
    require(type(value) in (int, float) and math.isfinite(value) and (value > 0 if positive else value >= 0), name)
    return value


def validate_session(data: bytes, nonce: str, pins: list[dict]):
    require(len(data) <= 4 * 1024 * 1024 and data.endswith(b'\n'), 'session_size_or_truncation')
    lines = data.splitlines(keepends=True)
    rows = [json.loads(line) for line in lines]
    require(len(rows) == 159, 'event_count_refused')
    require(rows[0].get('event') == 'start' and rows[0].get('schemaVersion') == 1 and
            rows[0].get('scope') == 'P5F_IPHONE_PERFORMANCE', 'start_refused')
    session = rows[0].get('sessionId')
    require(isinstance(session, str) and session, 'session_id_refused')
    for sequence, row in enumerate(rows, 1):
        require(row.get('runNonce') == nonce and row.get('sessionId') == session and
                type(row.get('eventSequence')) is int and row['eventSequence'] == sequence, 'identity_or_sequence_refused')
    require(rows[1].get('event') == 'begin' and rows[1].get('network') == 'unsatisfied' and
            rows[1].get('thermal') == 'nominal' and rows[1].get('lowPower') is False, 'begin_refused')
    trials = rows[2:-1]
    for expected, row in zip(plan(), trials, strict=True):
        require(row.get('event') == 'trial' and all(type(row.get(k)) is type(v) and row[k] == v for k, v in expected.items()), 'plan_or_duplicate_refused')
        index, epoch = expected['index'], expected['ordinal'] + 1
        require(row.get('case') == pins[index] and row.get('epoch') == epoch, 'pin_or_epoch_refused')
        require(row.get('nativeNetwork') == 'unsatisfied' and row.get('cleanup') is True, 'trial_condition_refused')
        proof = row.get('proof', {})
        require(proof.get('epoch') == epoch and proof.get('index') == index and proof.get('crop') is False and
                type(proof.get('frameNumber')) is int and proof['frameNumber'] > 0 and
                row.get('timingFrameNumber') == proof['frameNumber'], 'frame_refused')
        for key in ('logicalWidth', 'logicalHeight', 'dpr'):
            number(proof.get(key), 'geometry_refused', positive=True)
        require(proof.get('fit') in ('contain', 'cover') and proof.get('filterQuality') == 'low', 'geometry_refused')
        for key in ('readMicros', 'decodeMicros', 'firstDrawMicros', 'decodeDrawMicros', 'rasterMicros', 'buildMicros', 'trialMicros'):
            number(row.get(key), 'timing_missing_or_invalid')
        require(row['decodeDrawMicros'] == row['decodeMicros'] + row['firstDrawMicros'] and
                row['trialMicros'] < 10_000_000 and row['trialMicros'] >= row['readMicros'] + row['decodeDrawMicros'] and
                row['rasterMicros'] <= row['trialMicros'] and row['buildMicros'] <= row['trialMicros'], 'timing_limit_refused')
        memory = row.get('memory', {})
        require(memory.get('counter') == 'ios_phys_footprint' and memory.get('valid') is True and
                memory.get('thermal') == 'nominal' and memory.get('lowPower') is False and
                memory.get('sampleIntervalMicros') == 10000, 'counter_refused')
        for key in ('baselineBytes', 'loadedBytes', 'afterReleaseBytes', 'sampledMaxBytes'):
            value = number(memory.get(key), 'counter_missing', positive=True)
            require(type(value) is int and value <= 512 * 1024 ** 2, 'memory_limit_refused')
        require(memory['sampledMaxBytes'] >= max(memory[k] for k in ('baselineBytes', 'loadedBytes', 'afterReleaseBytes')), 'maximum_refused')
        gaps = memory.get('sampleGapsMicros')
        require(isinstance(gaps, list) and gaps and type(memory.get('samples')) is int and memory['samples'] == len(gaps) + 1, 'samples_refused')
        for gap in gaps:
            number(gap, 'gap_refused')
        number(memory.get('sampledDurationMicros'), 'gap_or_duration_refused')
        require(memory.get('maxGapMicros') == max(gaps) and memory.get('sampledDurationMicros') >= sum(gaps) and
                memory['sampledDurationMicros'] <= row['trialMicros'], 'gap_or_duration_refused')
    require(len({r['proof']['frameNumber'] for r in trials}) == 156, 'duplicate_frame_refused')
    seal = rows[-1]
    require(seal.get('event') == 'seal' and seal.get('completed') == 144 and seal.get('warmups') == 12 and
            seal.get('cleanup') is True and seal.get('prefixSha256') == hashlib.sha256(b''.join(lines[:-1])).hexdigest(), 'seal_refused')
    number(seal.get('sessionMicros'), 'session_duration_refused', positive=True)
    require(seal['sessionMicros'] < 600_000_000 and seal['sessionMicros'] >= sum(r['trialMicros'] for r in trials), 'session_deadline_refused')
    measured = [row for row in trials if not row['warmup']]
    require(Counter(row['index'] for row in measured) == Counter({i: 12 for i in range(12)}), 'sample_count_refused')
    # Aynı rolün geometri parametreleri tüm tekrar ve formatlarda sabittir.
    for image in range(4):
        geometries = {json.dumps({k: r['proof'][k] for k in ('logicalWidth', 'logicalHeight', 'dpr', 'fit', 'filterQuality')}, sort_keys=True)
                      for r in measured if r['index'] // 3 == image}
        require(len(geometries) == 1, 'geometry_changed')
    summary = summarize(measured, pins)
    return dict(status='SEALED_VERIFIED', scope='P5F_IPHONE_PERFORMANCE', measuredTrials=144,
                warmups=12, sessionMicros=seal['sessionMicros'], summary=summary,
                productionAcceptance='NOT_RUN', gpuPeak='NOT_MEASURED', panelPresentation='NOT_MEASURED')


def summarize(rows, pins):
    metrics = ('readMicros', 'decodeMicros', 'firstDrawMicros', 'decodeDrawMicros', 'rasterMicros', 'buildMicros')
    result = {}
    for index, pin in enumerate(pins):
        samples = [r for r in rows if r['index'] == index]
        values = {key: [r[key] for r in samples] for key in metrics}
        values['sampledMaxBytes'] = [r['memory']['sampledMaxBytes'] for r in samples]
        result[pin['caseId']] = {
            'samples': len(samples),
            'metrics': {key: dict(median=median(v), min=min(v), max=max(v)) for key, v in values.items()},
            'rounds': [{ 'round': r['round'], **{key: r[key] for key in metrics},
                         'sampledMaxBytes': r['memory']['sampledMaxBytes'], 'maxGapMicros': r['memory']['maxGapMicros']} for r in samples],
            'roundDeltas': {key: [v[n] - v[n - 1] for n in range(1, len(v))] for key, v in values.items()},
        }
    paired = {}
    for asset in (1950, 1952, 8303, 8306):
        original = {r['round']: r for r in rows if r['case']['caseId'] == f'{asset}-original'}
        paired[str(asset)] = {}
        for arm in ('PNG', 'WEBP'):
            paired[str(asset)][arm] = []
            for candidate in (r for r in rows if r['case']['caseId'] == f'{asset}-{arm}'):
                reference = original[candidate['round']]
                paired[str(asset)][arm].append({
                    'round': candidate['round'], 'originalOrdinal': reference['ordinal'],
                    'candidateOrdinal': candidate['ordinal'],
                    **{key: candidate[key] - reference[key] for key in metrics},
                    'sampledMaxBytes': candidate['memory']['sampledMaxBytes'] - reference['memory']['sampledMaxBytes'],
                })
    pilot = {}
    for asset in (8303, 8306):
        original = result[f'{asset}-original']['metrics']
        webp = result[f'{asset}-WEBP']['metrics']
        decode_ok = all(webp[key]['median'] <= original[key]['median'] * 1.2 for key in ('decodeMicros', 'decodeDrawMicros'))
        memory_delta = webp['sampledMaxBytes']['median'] - original['sampledMaxBytes']['median']
        pilot[str(asset)] = dict(decodeDrawWithin20Percent=decode_ok, memoryDeltaBytes=memory_delta,
                                memoryWithinPilotBudget=memory_delta <= max(8 * 1024 ** 2, original['sampledMaxBytes']['median'] * .1))
    return dict(cases=result, pairedRoundDeltas=paired, pofudukLocalPilot=pilot, budgetScope='LOCAL_PILOT_ONLY')
