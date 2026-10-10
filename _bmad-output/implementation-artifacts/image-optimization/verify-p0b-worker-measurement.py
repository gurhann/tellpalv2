#!/usr/bin/env python3
"""Reread worker evidence independently; import neither collector nor encoder."""
import argparse
import gc
import hashlib
import importlib.util
import json
import math
import os
from pathlib import Path
import platform
import resource
import sys
import time

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('unchanged_pilot_verifier', HERE / 'verify-p0b-encoder-pilot.py')
V = importlib.util.module_from_spec(spec)
spec.loader.exec_module(V)
LIMITS = dict(V.LIMITS, child_seconds=120, batch_seconds=5400, batch_bytes=4 * 1024**3,
              log_bytes=1024**2, telemetry_bytes=2 * 1024**2, samples=256, events=64,
              idle_seconds=5, settle_seconds=30, sample_seconds=1, term_grace_seconds=2)
PROTOCOL = HERE / 'p0b-worker-measurement-protocol.md'
PARENT_PUBLIC_HASH = 'fd00e5ccfffc6a4a898284d80e2d98c699359cd337aa6aaf1d3d768560287f3a'
PARENT_SCOPE_HASH = '89fa6fc8465d6cdd1d6446742046c59995c81aab584108337ceda2439ef1f7b1'
# The approved immutable measurement scope is the historical trust anchor.
# A caller cannot introduce a new historical scope or supply claimed identities.
HISTORICAL_SCOPE_HASH = '7b28e780e1ffae4f5c1e3b5d08f434e4ba5681337f904435b1e928c249fd7ef4'
CODE_NAMES = ('run-p0b-worker-measurement.py', 'verify-p0b-worker-measurement.py')
# Darwin rusage timeval counters have microsecond precision. Two microseconds
# cover rounding at both observations, without admitting a meaningful CPU loss.
CPU_COUNTER_TOLERANCE_SECONDS = 0.000002
UNITS = {'clock': 'monotonic_nanoseconds', 'rss': 'bytes', 'threads': 'count',
         'cpu': 'seconds', 'allocated_disk': 'st_blocks_times_512_bytes', 'logical_disk': 'bytes'}
UNAVAILABLE = {'python_heap': 'unsupported', 'native_allocator': 'unsupported', 'pss': 'unsupported',
               'private_dirty': 'unsupported', 'graphics': 'unsupported', 'codec_threads': 'unsupported',
               'java_heap': 'inapplicable', 'container': 'inapplicable', 'workload_perturbation': 'unavailable'}
UNAVAILABLE = {key: {'value': None, 'reason': reason} for key, reason in UNAVAILABLE.items()}
UNEXERCISED = {key: {'status': 'inapplicable_unexercised', 'duration_ns': None} for key in
               ('download', 'source_decode_normalization', 'upload', 'stored_object_reread', 'job_claim_to_ready_commit')}
LIMITATIONS = dict(V.LIMITATIONS, worker_measurement='local_canonical_reference_only',
                  resource_qualification=False, sampled_peaks='observed_lower_bounds',
                  os_cache_background_thermal='uncontrolled', production_p95=None,
                  leak_device_safety=None, unavailable_counters=UNAVAILABLE, unexercised=UNEXERCISED)


def number(value, *, signed=False, integer=False):
    if type(value) not in ((int,) if integer else (int, float)) or not math.isfinite(value) or (not signed and value < 0):
        raise V.GateFailure('invalid_metric_number')
    return value


def environment():
    import subprocess
    def observed(command):
        return subprocess.check_output(command, timeout=10, text=True).strip()
    clock = time.get_clock_info('perf_counter')
    return {'os': platform.system(), 'os_version': platform.mac_ver()[0],
            'os_build': observed(['sw_vers', '-buildVersion']), 'machine': platform.machine(),
            'cpu': observed(['sysctl', '-n', 'machdep.cpu.brand_string']),
            'physical_cpus': int(observed(['sysctl', '-n', 'hw.physicalcpu'])),
            'logical_cpus': int(observed(['sysctl', '-n', 'hw.logicalcpu'])),
            'ram_bytes': int(observed(['sysctl', '-n', 'hw.memsize'])),
            'python': platform.python_version(), 'pillow': V.Image.__version__,
            'clock': {'implementation': clock.implementation, 'resolution_seconds': clock.resolution,
                      'monotonic': clock.monotonic, 'adjustable': clock.adjustable},
            'rss_api': 'proc_pidinfo_PROC_PIDTASKINFO_4_96_bytes', 'rusage_maxrss_unit': 'bytes'}


def validate_environment(value):
    expected = {'os': 'Darwin', 'os_version': '27.0.1', 'machine': 'arm64', 'cpu': 'Apple M2',
                'physical_cpus': 8, 'logical_cpus': 8, 'ram_bytes': 17179869184,
                'python': '3.14.6', 'pillow': '12.3.0'}
    if any(value.get(k) != v or type(value.get(k)) is not type(v) for k, v in expected.items()):
        raise V.GateFailure('measurement_environment_drift')
    if not value['clock']['monotonic'] or value['clock']['adjustable']:
        raise V.GateFailure('clock_domain_unsupported')


def code_identity():
    return [V.file_identity(HERE / name, name) for name in CODE_NAMES] + [V.file_identity(PROTOCOL, 'measurement_protocol')]


def verification_provenance(scope, historical_code_root=None, *, scope_sha256=None):
    """Final rereading may bind immutable measured sources; workers never use this."""
    if historical_code_root is None:
        measured = code_identity()
        source_kind = 'current_scope_bound_sources'
    else:
        if scope_sha256 != HISTORICAL_SCOPE_HASH:
            raise V.GateFailure('unrecognized_historical_measurement_scope')
        root = V.no_symlinks(Path(historical_code_root))
        names = (*CODE_NAMES, PROTOCOL.name)
        if not root.is_dir() or {p.name for p in root.iterdir()} != set(names):
            raise V.GateFailure('historical_code_witness_inventory')
        for name in names:
            path = V.no_symlinks(root / name)
            # Git restores source witnesses as mode 100644. Their exact hashes,
            # rather than checkout permissions, bind the historical source bytes.
            if not path.is_file(): raise V.GateFailure('historical_code_witness_file_type')
        measured = [V.file_identity(root / name, name) for name in CODE_NAMES]
        measured.append(V.file_identity(root / PROTOCOL.name, 'measurement_protocol'))
        source_kind = 'historical_scope_bound_witnesses'
    if not V.same(measured, scope['measurement_code']):
        raise V.GateFailure('measurement_code_witness_mismatch')
    return {'measurement_source_kind': source_kind,
            'measurement_code': measured,
            'final_verification_code': [V.file_identity(Path(__file__).resolve(), 'verify-p0b-worker-measurement.py')]}


class Meter:
    """Child writes bounded phase events; supervisor owns live endpoint captures."""
    def __init__(self, request):
        self.request = request
        self.stream = os.fdopen(int(os.environ['P0B_EVENT_FD']), 'wb', buffering=0)
        self.count = 0

    def event(self, name, phase=None):
        usage = resource.getrusage(resource.RUSAGE_SELF)
        row = {'attempt_id': self.request['attempt_id'], 'stage': self.request['stage'],
               'pid': os.getpid(), 'seq': self.count, 'name': name, 'phase': phase,
               't_ns': time.perf_counter_ns(), 'user_seconds': usage.ru_utime, 'system_seconds': usage.ru_stime}
        raw = V.canonical(row) + b'\n'
        if self.count >= LIMITS['events']: raise V.GateFailure('event_limit')
        self.stream.write(raw)
        self.count += 1
        if name != 'phase_start' and sys.stdin.buffer.readline() != b'event\n': raise V.GateFailure('boundary_handshake_failure')

    def phase(self, name, function):
        self.event('phase_start', name)
        value = function()
        self.event('phase_end', name)
        return value

    def wait(self, name):
        self.event(name + '_start')
        if sys.stdin.buffer.readline() != b'continue\n': raise V.GateFailure('endpoint_handshake_failure')
        self.event(name + '_end')

    def finish(self, cleanup):
        self.phase('cleanup', cleanup)
        self.wait('settle')
        self.event('complete')
        self.stream.close()


def check_request(request, path):
    args = argparse.Namespace(**{k: Path(v) for k, v in request['inputs'].items()})
    batch = Path(request['scope']).parent
    scope = V.load_json(V.private_path(Path(request['scope'])))
    V.check_integrity(args, scope['parent_scope'])
    if not V.same(code_identity(), scope['measurement_code']) or not V.same(environment(), scope['environment']):
        raise V.GateFailure('prerequisite_integrity_failed')
    pair = request['pair']
    if pair not in scope['parent_scope']['planned_pairs'] or request['arm'] not in ('control', 'candidate'):
        raise V.GateFailure('attempt_request_mismatch')
    if path.parent.name != request['attempt_id'] or Path(request['output']) != path.parent / 'output.bin':
        raise V.GateFailure('attempt_request_mismatch')
    V.safe_batch(Path(request['output']), V.input_paths(args) + [Path(scope['parent_batch'])])
    return scope


def verification_worker(request, path):
    meter = Meter(request)
    meter.event('bootstrap_start')
    check_request(request, path)
    meter.event('bootstrap_end')
    meter.wait('idle')
    observation = meter.phase('output_verification', lambda: V.inspect_output(
        Path(request['output']), Path(request['reference']), request['pair'], request['arm']))
    def cleanup(): gc.collect()
    meter.finish(cleanup)
    print(json.dumps({'status': 'PASS', 'observation': observation}))


def read_jsonl(path, cap):
    V.no_symlinks(path)
    if path.stat().st_size > LIMITS['telemetry_bytes']: raise V.GateFailure('telemetry_byte_limit')
    raw = path.read_bytes()
    complete = raw.endswith(b'\n') or not raw
    lines = raw.splitlines()
    tail = None
    if not complete: tail = lines.pop().decode('utf8', errors='replace')
    if len(lines) > cap: raise V.GateFailure('telemetry_record_limit')
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result: raise V.GateFailure('duplicate_json_key')
            result[key] = value
        return result
    rows = [json.loads(line, object_pairs_hook=unique, parse_constant=lambda _: (_ for _ in ()).throw(V.GateFailure('nonfinite_json'))) for line in lines]
    return rows, tail


def disk_valid(value):
    if set(value) != {'output', 'tmp', 'evidence', 'retained_batch'}: raise V.GateFailure('disk_categories')
    for entry in value.values():
        if set(entry) != {'logical_bytes', 'allocated_bytes'}: raise V.GateFailure('disk_units')
        for item in entry.values(): number(item, integer=True)


def derive_child(events, samples, lifecycle, attempt_id, stage):
    """Derive endpoint and phase values exclusively from ordered raw records."""
    if lifecycle.get('units') != UNITS: raise V.GateFailure('counter_unit_mismatch')
    for key in ('start_ns', 'end_ns', 'supervisor_pid', 'deadline_ns'): number(lifecycle[key], integer=True)
    if lifecycle['end_ns'] < lifecycle['start_ns']: raise V.GateFailure('lifecycle_order')
    if lifecycle['status'] not in ('success', 'failure', 'timeout', 'spawn_failure', 'instrumentation_failure'):
        raise V.GateFailure('lifecycle_status')
    pid = lifecycle.get('pid')
    if pid is not None:
        number(pid, integer=True)
        if pid <= 0: raise V.GateFailure('invalid_pid')
    for key in ('term_sent','kill_sent','deadline_expired'):
        if type(lifecycle[key]) is not bool: raise V.GateFailure('invalid_lifecycle_boolean')
    for key, flag in (('term_ns','term_sent'),('kill_ns','kill_sent')):
        if lifecycle[flag]:
            number(lifecycle[key],integer=True)
            if not lifecycle['start_ns'] <= lifecycle[key] <= lifecycle['end_ns']: raise V.GateFailure('signal_timestamp')
        elif lifecycle[key] is not None: raise V.GateFailure('signal_timestamp_without_signal')
    if lifecycle['kill_sent'] and (not lifecycle['term_sent'] or lifecycle['kill_ns'] < lifecycle['term_ns']+2*10**9): raise V.GateFailure('signal_grace')
    if lifecycle.get('attempt_id') != attempt_id or lifecycle.get('stage') != stage: raise V.GateFailure('lifecycle_ownership')
    terminal = lifecycle.get('terminal')
    if pid is not None:
        if not isinstance(terminal, dict) or terminal.get('pid') != pid: raise V.GateFailure('missing_terminal_rusage')
        for key in ('user_seconds', 'system_seconds'): number(terminal[key])
        number(terminal['maxrss_bytes'],integer=True)
        number(terminal['reaped_ns'],integer=True)
        if not lifecycle['start_ns'] <= terminal['reaped_ns'] <= lifecycle['end_ns']: raise V.GateFailure('terminal_timestamp')
        exit_code, signal_ = terminal.get('exit_code'), terminal.get('signal')
        if (exit_code is None) == (signal_ is None): raise V.GateFailure('missing_or_ambiguous_exit_status')
        if exit_code is not None:
            number(exit_code,integer=True)
            if exit_code > 255: raise V.GateFailure('exit_code_range')
        if signal_ is not None:
            number(signal_,integer=True)
            if not 1 <= signal_ <= 64: raise V.GateFailure('signal_range')
        if lifecycle['status']=='success' and (exit_code != 0 or signal_ is not None or lifecycle['term_sent']): raise V.GateFailure('success_terminal_mismatch')
        if lifecycle['status']=='failure' and exit_code==0 and signal_ is None: raise V.GateFailure('failure_terminal_mismatch')
    elif lifecycle['status'] != 'spawn_failure': raise V.GateFailure('missing_pid')
    if lifecycle['status'] == 'timeout' and (not lifecycle.get('term_sent') or not lifecycle.get('deadline_expired')):
        raise V.GateFailure('timeout_lifecycle_unproved')
    if lifecycle['status']=='timeout' and lifecycle['term_ns'] < lifecycle['deadline_ns']: raise V.GateFailure('timeout_before_deadline')
    if lifecycle['status']=='success' and terminal['reaped_ns'] > lifecycle['deadline_ns']: raise V.GateFailure('success_after_child_deadline')
    last = lifecycle['start_ns']; previous_cpu=(0,0)
    for index, event in enumerate(events):
        if set(event) != {'attempt_id', 'stage', 'pid', 'seq', 'name', 'phase', 't_ns', 'user_seconds', 'system_seconds'}:
            raise V.GateFailure('event_fields')
        if event['attempt_id'] != attempt_id or event['stage'] != stage or type(event['pid']) is not int or event['pid'] != pid or type(event['seq']) is not int or event['seq'] != index:
            raise V.GateFailure('event_replay_or_ownership')
        number(event['t_ns'], integer=True)
        number(event['user_seconds']); number(event['system_seconds'])
        if not last <= event['t_ns'] <= lifecycle['end_ns']: raise V.GateFailure('event_order_or_clock_domain')
        if event['user_seconds'] < previous_cpu[0] or event['system_seconds'] < previous_cpu[1]: raise V.GateFailure('cpu_event_order')
        previous_cpu=(event['user_seconds'],event['system_seconds'])
        last = event['t_ns']
    if terminal and any(terminal[key] + CPU_COUNTER_TOLERANCE_SECONDS < previous_cpu[index]
                        for index, key in enumerate(('user_seconds', 'system_seconds'))):
        raise V.GateFailure('terminal_cpu_smaller_than_phase_event')
    last = lifecycle['start_ns']
    periodic = []
    for index, sample in enumerate(samples):
        if sample['attempt_id'] != attempt_id or sample['stage'] != stage or type(sample['pid']) is not int or sample['pid'] != pid or type(sample['supervisor_pid']) is not int or sample['supervisor_pid'] != lifecycle['supervisor_pid'] or type(sample['seq']) is not int or sample['seq'] != index:
            raise V.GateFailure('sample_replay_or_ownership')
        for key in ('t_ns', 'planned_ns', 'capture_cost_ns', 'lateness_ns'): number(sample[key], integer=True)
        if not last <= sample['t_ns'] <= lifecycle['end_ns']: raise V.GateFailure('sample_order_or_clock_domain')
        if sample['lateness_ns'] != max(0, sample['t_ns'] - sample['planned_ns']): raise V.GateFailure('sample_lateness')
        if sample['error'] is None:
            number(sample['rss_bytes'], integer=True); number(sample['threads'], integer=True)
        elif sample['rss_bytes'] is not None or sample['threads'] is not None: raise V.GateFailure('failed_sample_nonnull')
        disk_valid(sample['disk'])
        if sample['kind']=='boundary':
            number(sample['event_seq'],integer=True)
            if sample['event_seq'] >= len(events) or sample['planned_ns'] != events[sample['event_seq']]['t_ns'] or sample['t_ns'] < events[sample['event_seq']]['t_ns']: raise V.GateFailure('boundary_capture_binding')
        elif sample['event_seq'] is not None: raise V.GateFailure('unexpected_event_seq')
        if sample['kind'] == 'periodic': periodic.append(sample)
        elif sample['kind'] not in ('boundary', 'idle_endpoint', 'settled_endpoint'): raise V.GateFailure('sample_kind')
        last = sample['t_ns']
    if periodic:
        for sample in periodic:
            tick = sample['tick']; number(tick, integer=True)
            if sample['planned_ns'] != lifecycle['start_ns'] + tick * 10**9: raise V.GateFailure('sample_tick_schedule')
        if [s['tick'] for s in periodic] != list(range(len(periodic))): raise V.GateFailure('missing_periodic_tick')
    complete = lifecycle['status'] == 'success'
    expected_phases = ['canonical_read', 'projection', 'encoding'] if stage == 'encode' else ['output_verification']
    expected = [('bootstrap_start', None), ('bootstrap_end', None), ('idle_start', None), ('idle_end', None)]
    for phase in expected_phases + ['cleanup']: expected.extend([('phase_start', phase), ('phase_end', phase)])
    expected.extend([('settle_start', None), ('settle_end', None), ('complete', None)])
    names = [(e['name'], e['phase']) for e in events]
    if names != expected[:len(names)] or complete and names != expected: raise V.GateFailure('phase_sequence')
    if complete and sorted(s['event_seq'] for s in samples if s['kind']=='boundary') != list(range(len(events))): raise V.GateFailure('missing_or_duplicate_boundary_snapshot')
    if complete and (any(e['t_ns'] > terminal['reaped_ns'] for e in events) or any(s['t_ns'] > terminal['reaped_ns'] for s in samples)): raise V.GateFailure('observation_after_reap')
    if complete and (not samples or any(s['error'] is not None for s in samples)):
        raise V.GateFailure('required_live_capture_failed')
    if complete:
        if not periodic: raise V.GateFailure('missing_periodic_sampling')
        # Delayed planned ticks remain visible. Coverage must still reach the
        # complete event in actual time; a removed tail cannot pass as complete.
        if events[-1]['t_ns'] - periodic[-1]['t_ns'] > LIMITS['sample_seconds'] * 10**9:
            raise V.GateFailure('missing_periodic_tail')
    def endpoint(kind, begin_name, end_name, seconds):
        begins = [e for e in events if e['name'] == begin_name] if begin_name != 'cleanup_end' else [e for e in events if e['name']=='phase_end' and e['phase']=='cleanup']
        ends = [e for e in events if e['name'] == end_name]
        found = [s for s in samples if s['kind'] == kind]
        if len(found) != 1 or not begins or not ends:
            if complete: raise V.GateFailure('missing_live_endpoint')
            return {'rss_bytes': None, 'reason': 'expected_partial_lifecycle'}
        sample = found[0]
        if not begins[0]['t_ns'] + seconds * 10**9 <= sample['t_ns'] <= ends[0]['t_ns']:
            raise V.GateFailure('endpoint_window')
        if sample['error'] is not None: raise V.GateFailure('endpoint_capture_failed')
        return {'rss_bytes': sample['rss_bytes'], 'os_threads': sample['threads'], 't_ns': sample['t_ns'],
                'capture_delay_ns': sample['t_ns'] - begins[0]['t_ns'] - seconds * 10**9, 'reason': None}
    idle = endpoint('idle_endpoint', 'idle_start', 'idle_end', 5)
    settled = endpoint('settled_endpoint', 'cleanup_end', 'settle_end', 30)
    phases = {}
    bootstrap = [e for e in events if e['name'] in ('bootstrap_start','bootstrap_end')]
    if len(bootstrap)==2:
        phases['bootstrap_guards']={'duration_ns':bootstrap[1]['t_ns']-bootstrap[0]['t_ns'],'user_seconds':bootstrap[1]['user_seconds']-bootstrap[0]['user_seconds'],'system_seconds':bootstrap[1]['system_seconds']-bootstrap[0]['system_seconds'],'reason':None}
    else: phases['bootstrap_guards']={'duration_ns':None,'user_seconds':None,'system_seconds':None,'reason':'expected_partial_lifecycle'}
    for phase in expected_phases + ['cleanup']:
        matching = [e for e in events if e['phase'] == phase]
        if len(matching) != 2:
            phases[phase] = {'duration_ns': None, 'user_seconds': None, 'system_seconds': None,
                             'reason': 'expected_partial_lifecycle', 'periodic_samples': 0, 'sampled_peak_rss_bytes': None}
            continue
        start, end = matching
        values = {'duration_ns': end['t_ns'] - start['t_ns'],
                  'user_seconds': end['user_seconds'] - start['user_seconds'],
                  'system_seconds': end['system_seconds'] - start['system_seconds']}
        for val in values.values(): number(val)
        covered = [s for s in periodic if start['t_ns'] <= s['t_ns'] <= end['t_ns'] and s['error'] is None]
        boundary = [s for s in samples if s['kind'] == 'boundary' and s.get('event_seq') in (start['seq'], end['seq'])]
        if complete and len(boundary) != 2: raise V.GateFailure('missing_phase_boundary_capture')
        values.update(reason=None, periodic_samples=len(covered), sampled_peak_rss_bytes=max((s['rss_bytes'] for s in covered), default=None),
                      peak_null_reason=None if covered else 'no_periodic_sample_in_short_or_delayed_phase',
                      boundary_captures=[{'event_seq': s['event_seq'], 'rss_bytes': s['rss_bytes'], 'os_threads': s['threads'], 't_ns': s['t_ns'],
                                          'delay_ns': s['t_ns'] - events[s['event_seq']]['t_ns']} for s in boundary])
        phases[phase] = values
    valid_samples = [s for s in samples if s['error'] is None]
    times = [s['t_ns'] for s in periodic]
    for key in ('collector_user_seconds', 'collector_system_seconds', 'observer_wall_ns'): number(lifecycle[key])
    return {'complete': complete, 'phases': phases, 'idle': idle, 'settled': settled,
            'settled_minus_idle_rss_bytes': settled['rss_bytes'] - idle['rss_bytes'] if idle['rss_bytes'] is not None and settled['rss_bytes'] is not None else None,
            'sampled_lifetime_peak_os_threads': max((s['threads'] for s in valid_samples), default=None),
            'sampled_lifetime_peak_rss_bytes': max((s['rss_bytes'] for s in valid_samples), default=None),
            'kernel_lifetime_high_water_rss_bytes': terminal['maxrss_bytes'] if terminal else None,
            'lifetime_cpu': {k: terminal[k] if terminal else None for k in ('user_seconds', 'system_seconds')},
            'coverage': {'periodic_count': len(periodic), 'capture_count': len(samples), 'failures': sum(s['error'] is not None for s in samples),
                         'maximum_periodic_gap_ns': max((b-a for a,b in zip(times, times[1:])), default=None),
                         'maximum_lateness_ns': max((s['lateness_ns'] for s in periodic), default=None),
                         'capture_cost_ns': sum(s['capture_cost_ns'] for s in samples)},
            'disk_sampled_peaks': {category: {unit: max((s['disk'][category][unit] for s in samples), default=None) for unit in ('logical_bytes','allocated_bytes')} for category in ('output','tmp','evidence','retained_batch')},
            'observer': {k: lifecycle[k] for k in ('collector_user_seconds','collector_system_seconds','observer_wall_ns')}}


def load_child(directory, record, attempt_id, stage):
    expected = {stage + suffix for suffix in ('.stdout', '.stderr', '.events.jsonl', '.samples.jsonl', '.lifecycle.json')}
    if set(record['files']) != expected: raise V.GateFailure('missing_or_extra_child_files')
    for name, identity in record['files'].items():
        if not V.same(V.file_identity(directory / name, name), identity): raise V.GateFailure('child_file_hash_mismatch')
        if (directory / name).stat().st_mode & 0o222: raise V.GateFailure('mutable_child_evidence')
        if name.endswith(('.stdout','.stderr')) and identity['bytes'] > LIMITS['log_bytes']: raise V.GateFailure('log_byte_limit')
    events, tail = read_jsonl(directory / (stage + '.events.jsonl'), LIMITS['events'])
    samples, sample_tail = read_jsonl(directory / (stage + '.samples.jsonl'), LIMITS['samples'])
    lifecycle = V.load_json(directory / (stage + '.lifecycle.json'))
    if sample_tail or tail is not None and lifecycle['status'] not in ('timeout','failure','instrumentation_failure'):
        raise V.GateFailure('incomplete_record_without_termination')
    if tail is not None and lifecycle.get('terminal', {}).get('signal') is None: raise V.GateFailure('incomplete_event_unproved')
    if record['status'] != lifecycle['status']: raise V.GateFailure('child_status_mismatch')
    return derive_child(events, samples, lifecycle, attempt_id, stage)


def counts(arms):
    attempts = [a for row in arms for a in row['attempts']]
    return {'pairs': len({r['pair_id'] for r in arms}), 'scheduled_arms': len(arms), 'actual_attempts': len(attempts),
            'successful_attempts': sum(a['status'] == 'success' for a in attempts), 'failed_attempts': sum(a['status'] != 'success' for a in attempts),
            'retries': sum(a['kind'] == 'retry' for a in attempts), 'output_verified_arms': sum(r['output_valid'] for r in arms),
            'resource_valid_arms': sum(r['resource_valid'] for r in arms), 'failed_arms': sum(r['execution_status']=='failed' for r in arms),
            'not_run_arms': sum(r['execution_status']=='not_run' for r in arms)}


def file_footprint(root,exclude=None):
    result={'logical_bytes':0,'allocated_bytes':0}; seen=set(); exclude=exclude or set()
    for path in root.rglob('*'):
        V.no_symlinks(path)
        if not path.is_file() or path in exclude: continue
        stat=path.stat(); key=(stat.st_dev,stat.st_ino)
        if key in seen: continue
        seen.add(key); result['logical_bytes']+=stat.st_size; result['allocated_bytes']+=stat.st_blocks*512
    return result


def verify_batch(batch, args, *, historical_code_root=None):
    scope = V.load_json(batch / 'scope.json'); result = V.load_json(batch / 'execution-result.json')
    if scope.get('schema_version') != 1 or type(scope.get('schema_version')) is not int or scope.get('artifact') != 'p0b-worker-scope' or result.get('schema_version') != 1 or type(result.get('schema_version')) is not int or result.get('artifact') != 'p0b-worker-execution' or result.get('batch_id') != scope.get('batch_id'): raise V.GateFailure('scope_or_result_schema')
    corpus = V.load_json(args.corpus); schedule = V.make_schedule(corpus)
    allowed_stops={None,'prerequisite_integrity_failed','batch_deadline','disk_cap','cancellation','instrumentation_failure','prerequisite_failure','parent_evidence_drift','rss_api_admission_failed','measurement_environment_drift','bounded_guard_failed','guard_deadline','guard_log_overflow'}
    if result['stop_reason'] not in allowed_stops: raise V.GateFailure('unknown_global_stop')
    number(scope['batch_start_ns'],integer=True); number(result['end_ns'],integer=True)
    if result['end_ns'] < scope['batch_start_ns']: raise V.GateFailure('batch_clock_order')
    if result['stop_reason'] is not None:
        stopped=V.load_json(batch/'stopped.json')
        if stopped['category']!=result['stop_reason'] or not scope['batch_start_ns'] <= number(stopped['t_ns'],integer=True) <= result['end_ns']: raise V.GateFailure('global_stop_receipt')
    stop_timestamp=V.load_json(batch/'stopped.json')['t_ns'] if result['stop_reason'] is not None else None
    diagnostics = []; arms = []; directories = set(); stop_seen = False
    scope_hash = V.stream_hash(batch / 'scope.json')[0]
    if result['scope_sha256'] != scope_hash or scope['limits'] != LIMITS or scope['units'] != UNITS or scope['limitations'] != LIMITATIONS:
        raise V.GateFailure('scope_or_limits_mismatch')
    sdk_base=Path('/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk')
    sdk_names=('usr/include/libproc.h','usr/include/sys/proc_info.h','usr/include/sys/stat.h','usr/share/man/man2/getrusage.2')
    if not V.same(scope['sdk_provenance'],[V.file_identity(sdk_base/name,name) for name in sdk_names]): raise V.GateFailure('sdk_provenance_changed')
    provenance = verification_provenance(scope, historical_code_root, scope_sha256=scope_hash)
    if not V.same(scope['environment'], environment()): raise V.GateFailure('code_or_environment_drift')
    validate_environment(scope['environment'])
    evidence = V.preflight(args)
    if evidence['status'] != 'PASS' or not V.same(scope['parent_scope'], V.make_scope(corpus, evidence, scope['parent_scope']['batch_id'])):
        raise V.GateFailure('parent_scope_drift')
    if not V.same(result['input_snapshot_after'], evidence['input_snapshot']): raise V.GateFailure('input_snapshot_after_mismatch')
    if V.stream_hash(Path(scope['parent_batch']) / 'scope.json')[0] != PARENT_SCOPE_HASH or V.stream_hash(HERE / 'p0b-encoder-pilot.json')[0] != PARENT_PUBLIC_HASH:
        raise V.GateFailure('parent_evidence_drift')
    parent_result = V.verify_result(V.load_json(Path(scope['parent_batch']) / 'scope.json'), V.load_json(Path(scope['parent_batch']) / 'execution-result.json'), Path(scope['parent_batch']), args, evidence, inspect=V.inspect_output)
    if parent_result['status'] != 'PASS' or not V.same(parent_result, V.load_json(HERE / 'p0b-encoder-pilot.json')): raise V.GateFailure('parent_output_reverification_failed')
    parent_outputs = {(r['pair_id'],r['arm']): r['output'] for r in parent_result['arms']}
    if not V.same(V.load_json(batch/'planned-ledger.json'),{'schedule':schedule}): raise V.GateFailure('planned_ledger_mismatch')
    if not V.same(V.load_json(batch/'private-inputs.json'),{key:str(getattr(args,key)) for key in ('source_root','reference_root','preparation_receipt','freeze_manifest','corpus','protocol')}): raise V.GateFailure('private_input_binding')
    if not V.same(scope['schedule'], schedule) or len(result['arms']) != 28: raise V.GateFailure('schedule_mismatch')
    pairs = {p['id']:p for p in corpus['trial_plan']['planned_pairs']}
    latest_end = scope['batch_start_ns']
    for index, row in enumerate(result['arms']):
        scheduled = schedule[index]
        if (row['pair_id'],row['arm']) != (scheduled['pair_id'],scheduled['arm']): raise V.GateFailure('reordered_arm')
        public = {'pair_id': row['pair_id'], 'arm': row['arm'], 'execution_status': row['status'], 'attempts': [], 'output': None,
                  'output_valid': False, 'resource_valid': False, 'measurements': None}
        arms.append(public)
        if len(row['attempts']) > 2: raise V.GateFailure('attempt_ceiling')
        for number_, attempt in enumerate(row['attempts']):
            ident = scheduled['initial_id' if number_==0 else 'retry_id']; kind = 'initial' if number_==0 else 'retry'
            if attempt['id'] != ident or attempt['kind'] != kind or ident in directories or stop_seen or any(a['status']=='success' for a in public['attempts']):
                raise V.GateFailure('attempt_schedule_or_stop_rule')
            directories.add(ident)
            history = {'id':ident,'kind':kind,'status':attempt['status'],'failure':attempt['failure'], 'output_valid':False,'resource_valid':False,'measurements':None,'diagnostics':[]}
            public['attempts'].append(history)
            directory = batch / 'attempts' / ident
            try:
                started = V.load_json(directory / 'started.json')
                if started['scope_sha256'] != scope_hash or started['id'] != ident or started['pair_id'] != scheduled['pair_id'] or started['arm'] != scheduled['arm'] or started['kind'] != kind or started['settings_sha256'] != scheduled['settings_sha256']:
                    raise V.GateFailure('started_binding')
                number(started['t_ns'],integer=True)
                if stop_timestamp is not None and started['t_ns'] >= stop_timestamp: raise V.GateFailure('attempt_after_global_stop')
                if started['t_ns'] < latest_end: raise V.GateFailure('overlapping_attempts')
                if not V.same(V.load_json(directory / 'completed.json'),attempt): raise V.GateFailure('completed_binding')
                pair = pairs[row['pair_id']]
                expected_request = V.expected_request(args,batch,directory,pair,row['arm'])
                expected_request.update(attempt_id=ident, stage='encode')
                if not V.same(V.load_json(directory / 'request.json'),expected_request): raise V.GateFailure('request_binding')
                allowed_failures={'encode_failure','encode_timeout','encode_spawn_failure','readback_failure','readback_timeout','readback_spawn_failure','predecessor_output_mismatch','instrumentation_failure','prerequisite_integrity_failed','batch_deadline','disk_cap','cancellation'}
                if attempt['status'] not in ('success','failed') or attempt['status']=='success' and attempt['failure'] is not None or attempt['status']=='failed' and attempt['failure'] not in allowed_failures: raise V.GateFailure('attempt_status_or_failure')
                # Output rereading is independent of admissible resource records.
                # Preserve a good file even when a later lifecycle check rejects RSS.
                if (directory / 'output.bin').exists():
                    try:
                        observed = V.inspect_output(directory / 'output.bin',args.reference_root / (pair['reference_id']+'.pixels'),pair,row['arm'])
                        if not V.same(observed,parent_outputs[(row['pair_id'],row['arm'])]): raise V.GateFailure('predecessor_output_mismatch')
                        history['output_valid']=True
                        if attempt['status']=='success': public.update(output=observed,output_valid=True)
                        else: history['partial_output']=observed
                    except (ValueError,OSError): history['diagnostics'].append('output_reread_failed')
                if 'encode' not in attempt['children']: raise V.GateFailure('attempt_missing_encode_lifecycle')
                child_metrics = {}
                for stage, child in attempt['children'].items():
                    if stage not in ('encode','verify'): raise V.GateFailure('child_stage')
                    child_metrics[stage] = load_child(directory,child,ident,stage)
                    lifecycle = V.load_json(directory / (stage + '.lifecycle.json'))
                    if lifecycle['deadline_ns'] != lifecycle['start_ns']+LIMITS['child_seconds']*10**9: raise V.GateFailure('child_deadline_changed')
                    if lifecycle['start_ns'] < started['t_ns'] or lifecycle['start_ns'] < latest_end: raise V.GateFailure('child_overlap_or_start')
                    latest_end = lifecycle['end_ns']
                number(attempt['end_ns'],integer=True)
                if attempt['end_ns'] < latest_end or attempt['end_ns'] > result['end_ns']: raise V.GateFailure('attempt_end_order')
                latest_end = attempt['end_ns']
                final_disk=V.load_json(directory/'final-disk.json')
                disk_valid(final_disk['snapshot'])
                number(final_disk['t_ns'],integer=True)
                if final_disk['t_ns'] < attempt['end_ns'] or final_disk['excluded_names']!=['final-disk.json']: raise V.GateFailure('attempt_final_disk_binding')
                history['final_disk']=final_disk['snapshot']
                failure=attempt['failure']
                if failure and failure.startswith('encode_') and (set(child_metrics)!={'encode'} or attempt['children']['encode']['status']!=failure.removeprefix('encode_')): raise V.GateFailure('encode_failure_lifecycle_mismatch')
                if failure and (failure.startswith('readback_') or failure=='predecessor_output_mismatch') and (set(child_metrics)!={'encode','verify'} or attempt['children']['encode']['status']!='success' or failure.startswith('readback_') and attempt['children']['verify']['status']!=failure.removeprefix('readback_')): raise V.GateFailure('readback_failure_lifecycle_mismatch')
                if attempt['status'] == 'success':
                    if set(child_metrics) != {'encode','verify'} or attempt['failure'] is not None: raise V.GateFailure('success_lifecycle_missing')
                if attempt['status'] == 'success' and not history['output_valid']: raise V.GateFailure('successful_output_invalid')
                resource_valid = attempt['status']=='success' and all(c['complete'] for c in child_metrics.values())
                active_values = [value['duration_ns'] for child in child_metrics.values() for name,value in child['phases'].items() if name not in ('cleanup','bootstrap_guards')]
                measurements = {'children':child_metrics,'attempt_wall_ns':attempt['end_ns']-started['t_ns'],
                                'active_pipeline_wall_ns':sum(active_values) if len(active_values)==4 and all(x is not None for x in active_values) else None}
                number(measurements['attempt_wall_ns'],integer=True)
                history.update(resource_valid=resource_valid,measurements=measurements)
                if resource_valid: public.update(resource_valid=True,measurements=measurements)
            except (ValueError,OSError,KeyError,TypeError) as exc:
                history['diagnostics'].append(str(exc) if isinstance(exc,V.GateFailure) else 'attempt_evidence_invalid')
                diagnostics.append({'severity':'invalid','category':'attempt_evidence_invalid','attempt_id':ident})
                # Historical rereading does not pretend a newly detected evidence
                # error stopped the past execution. Keep all actual attempts/files.
            if attempt['failure'] in ('instrumentation_failure','prerequisite_integrity_failed','batch_deadline','disk_cap','cancellation'):
                stop_seen = True
        expected_status = 'success' if any(a['status']=='success' for a in public['attempts']) else 'failed' if public['attempts'] else 'not_run'
        if row['status'] != expected_status: raise V.GateFailure('arm_execution_status')
    if {p.name for p in (batch/'attempts').iterdir()} != directories: raise V.GateFailure('unrecorded_attempt_directory')
    final_disk=V.load_json(batch/'final-disk.json')
    if final_disk['excluded_names'] != ['final-disk.json']: raise V.GateFailure('final_disk_exclusion')
    files=final_disk['files']; named=set()
    for identity in files:
        name=identity['id']
        if name in named or name=='final-disk.json' or Path(name).is_absolute() or '..' in Path(name).parts: raise V.GateFailure('final_disk_inventory_identity')
        named.add(name)
        if not V.same(identity,V.file_identity(batch/name,name)): raise V.GateFailure('final_disk_file_drift')
        if (batch/name).stat().st_mode & 0o222: raise V.GateFailure('mutable_batch_evidence')
    actual_files={str(p.relative_to(batch)) for p in batch.rglob('*') if p.is_file() and p!=batch/'final-disk.json'}
    if named != actual_files: raise V.GateFailure('final_disk_missing_or_extra_files')
    measured=file_footprint(batch,exclude={batch/'final-disk.json'})
    if not V.same(measured,final_disk['snapshot']): raise V.GateFailure('final_disk_snapshot_mismatch')
    if stop_seen and result['stop_reason'] is None: raise V.GateFailure('missing_global_stop')
    comparisons=[]
    by_arm={(r['pair_id'],r['arm']):r for r in arms}
    for pair in corpus['trial_plan']['planned_pairs']:
        control,candidate=[by_arm[(pair['id'],arm)]['output'] for arm in ('control','candidate')]
        original=pair['original_byte_denominator']; original_bytes=original['bytes']
        deltas={}
        for name,denom in (('normalized-same-geometry-jpeg-q85',control['bytes'] if control else None),('original-source',original_bytes)):
            deltas[name]=None
            if candidate and denom is not None and type(denom) is int and denom>0:
                deltas[name]={'denominator_bytes':denom,'candidate_bytes':candidate['bytes'],'candidate_minus_denominator_bytes':candidate['bytes']-denom,'candidate_to_denominator_ratio':candidate['bytes']/denom}
        comparisons.append({'id':pair['id'],'geometry':pair['geometry'],'binding_id':pair['binding_id'],'control':control,'candidate':candidate,
                            'raw_byte_comparisons':deltas,'original_byte_denominator':original,'visual_verdict':'pending','reader_verdict':'pending','native_verdict':'pending','quality_qualified_savings':None})
    output={'schema_version':1,'artifact':'p0b-worker-comparison','status':'INVALID' if diagnostics else 'INCOMPLETE' if result['stop_reason'] or not all(r['output_valid'] and r['resource_valid'] for r in arms) else 'PASS',
            'batch_id':scope['batch_id'],'scope_sha256':scope_hash,'private_result_sha256':V.stream_hash(batch/'execution-result.json')[0],
            'environment':scope['environment'],'counter_units':UNITS,'counts':counts(arms),'arms':arms,'pairs':comparisons,'limitations':LIMITATIONS,
            'retained_batch_disk':file_footprint(batch),'final_disk_snapshot':final_disk['snapshot'],
            'stop_reason':result['stop_reason'],'diagnostics':diagnostics,'parent_public_sha256':PARENT_PUBLIC_HASH,'code':scope['measurement_code'],
            'sdk_provenance':scope['sdk_provenance'],'verification_provenance':provenance,
            'historical_controls_resource_observations':None,'p3_diagnostic_resource_observations':None}
    V.scan_public(output)
    return output


def retained_failure(batch,args,category):
    output={'artifact':'p0b-worker-comparison','status':'INVALID','limitations':LIMITATIONS,'diagnostics':[{'severity':'invalid','category':category}]}
    try:
        stored=V.load_json(batch/'execution-result.json')
        ledger={(r.get('pair_id'),r.get('arm')):r for r in stored.get('arms',[]) if isinstance(r,dict)}
        arms=[]
        for scheduled in V.make_schedule(V.load_json(args.corpus)):
            row=ledger.get((scheduled['pair_id'],scheduled['arm']),{})
            attempts=[]
            for attempt in row.get('attempts',[])[:2]:
                if not isinstance(attempt,dict): continue
                failure=attempt.get('failure')
                allowed={'encode_failure','encode_timeout','encode_spawn_failure','readback_failure','readback_timeout','readback_spawn_failure','predecessor_output_mismatch','instrumentation_failure','prerequisite_integrity_failed','batch_deadline','disk_cap','cancellation',None}
                attempts.append({'id':attempt.get('id') if attempt.get('id') in (scheduled['initial_id'],scheduled['retry_id']) else 'unverified_identity','kind':attempt.get('kind') if attempt.get('kind') in ('initial','retry') else 'unverified_kind','status':attempt.get('status') if attempt.get('status') in ('success','failed') else 'unverified','failure':failure if isinstance(failure,(str,type(None))) and failure in allowed else 'unverified_failure','output_valid':False,'resource_valid':False,'measurements':None})
            arms.append({'pair_id':scheduled['pair_id'],'arm':scheduled['arm'],'execution_status':row.get('status') if row.get('status') in ('success','failed','not_run') else 'not_run','attempts':attempts,'output':None,'output_valid':False,'resource_valid':False,'measurements':None})
        batch_id=stored.get('batch_id'); scope_hash=stored.get('scope_sha256'); stop=stored.get('stop_reason')
        output.update(batch_id=batch_id if isinstance(batch_id,str) and __import__('re').fullmatch(r'[a-z0-9][a-z0-9._-]{0,95}',batch_id) else None,scope_sha256=scope_hash if isinstance(scope_hash,str) and V.P0A.SHA.fullmatch(scope_hash) else None,arms=arms,counts=counts(arms),stop_reason=stop if stop in ('instrumentation_failure','prerequisite_integrity_failed','batch_deadline','disk_cap','cancellation',None) else 'unverified_stop')
        V.scan_public(output)
    except Exception:
        output={'artifact':'p0b-worker-comparison','status':'INVALID','limitations':LIMITATIONS,'diagnostics':[{'severity':'invalid','category':category}]}
    return output


def main():
    parser=V.parser(); parser.add_argument('--meter-request',type=Path)
    parser.add_argument('--historical-code-root',type=Path,help='Final verification only: immutable sources rehashed against the recorded scope')
    args=parser.parse_args()
    try:
        if args.meter_request:
            if args.historical_code_root: raise V.GateFailure('historical_code_option_final_verification_only')
            request=V.load_json(V.private_path(args.meter_request)); request['stage']='verify'
            verification_worker(request,args.meter_request); return 0
        result=verify_batch(V.safe_batch(args.batch_root,V.input_paths(args)),args,historical_code_root=args.historical_code_root)
    except Exception as exc:
        category=str(exc) if isinstance(exc,V.GateFailure) else 'verification_failure:'+type(exc).__name__
        result=retained_failure(args.batch_root,args,category) if args.batch_root else {'status':'INVALID','artifact':'p0b-worker-comparison','diagnostics':[{'severity':'invalid','category':category}]}
    print(json.dumps(result,indent=2,allow_nan=False)); return {'PASS':0,'INCOMPLETE':1,'INVALID':2}[result['status']]

if __name__=='__main__': raise SystemExit(main())
