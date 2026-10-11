#!/usr/bin/env python3
"""Private, read-only selected-pilot preparation; this never admits native execution."""
import argparse
from datetime import datetime, timezone
from hashlib import sha256
import json
import math
import os
from pathlib import Path
import re
import resource
import selectors
import shutil
import signal
import stat
import subprocess
import sys
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[2]
SCOPE = 'image-optimization-selected-pilot-r1'
SPEC_DIR = '_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/'
ARTIFACT_DIR = '_bmad-output/implementation-artifacts/image-optimization/'
SOURCE_PATHS = (
    'AGENTS.md', 'tools/mobile-test-account.json',
    'app/mobile/lib/main_library_dev.dart', 'app/mobile/lib/app/library_dev_harness.dart',
    *(SPEC_DIR + name for name in ('SPEC.md', 'native-pilot-scope.md',
                                  'native-readiness-contract.md', 'native-entry-proposal.md',
                                  'native-measurement-protocol.md')),
    *(ARTIFACT_DIR + name for name in ('spec-1-5-selected-pilot-setup.md',
                                     'p0c-device-matrix.json', 'verify-p0c-readiness.py',
                                     'test_p0c_readiness.py', 'p0c-native-entry-proposal.md',
                                     'p0c-native-measurement-protocol.md', 'p0c-native-readiness.md')),
    'tools/image-optimization/native_pilot_preflight.py',
    'tools/image-optimization/replay_p0c_preparation.py',
)
GATE_ACTIONS = {
    'NATIVE-ENTRY': 'Accept the exact OQ-6 entry, implement its isolation and verify positive/negative platform guards.',
    'NATIVE-DEVICE': 'Qualify the connected local iPhone and a human-owned Streaming physical reservation, build and counters.',
    'NATIVE-CANDIDATE': 'Independently verify actual candidate/reference files and admit purpose, role, geometry and delivery cells.',
    'NATIVE-PROTOCOL': 'Freeze device/build, cache/journey/order/repeat/counter cells and finite evidence limits.',
    'NATIVE-DECISIONS': 'Identify unresolved quality/resource criteria and OQ dependencies before any native batch.',
}
ALIAS = re.compile(r'[a-z][a-z0-9-]{0,39}')
HASH = re.compile(r'[0-9a-f]{64}')
MAX_CONFIG_BYTES = 65536


class SetupError(ValueError):
    """An error whose message is a fixed, safe public diagnostic."""


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, 'Invalid CLI arguments rejected.\n')


def require(condition, message):
    if not condition:
        raise SetupError(message)


def now():
    return datetime.now(timezone.utc).isoformat(timespec='seconds').replace('+00:00', 'Z')


def strict_json(data):
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, 'Duplicate JSON key rejected.')
            result[key] = value
        return result
    def number(value):
        parsed = float(value)
        require(math.isfinite(parsed), 'Nonfinite JSON number rejected.')
        return parsed
    try:
        return json.loads(data, object_pairs_hook=pairs, parse_float=number,
                          parse_constant=lambda _: (_ for _ in ()).throw(SetupError('Nonfinite JSON number rejected.')))
    except SetupError:
        raise
    except (UnicodeError, ValueError, RecursionError, OverflowError):
        raise SetupError('Malformed JSON rejected.') from None


def exact_keys(value, required, optional=()):
    require(isinstance(value, dict) and set(required) <= set(value) <= set(required) | set(optional),
            'Unrecognized or incomplete configuration rejected.')


def no_symlink(path):
    """Reject user-controlled symlinks rather than following an escape."""
    path = Path(os.path.abspath(path))
    for component in (path, *path.parents):
        try:
            require(not component.is_symlink(), 'Symlink path rejected.')
        except OSError:
            raise SetupError('Path inspection failed.') from None
    return path


def outside_repository(path):
    resolved = path.resolve()
    require(not resolved.is_relative_to(ROOT.resolve()), 'Repository output/input path rejected.')
    return resolved


def private_file(path, max_bytes=MAX_CONFIG_BYTES):
    path = outside_repository(no_symlink(path))
    try:
        fd = os.open(path, os.O_RDONLY | getattr(os, 'O_NOFOLLOW', 0) | getattr(os, 'O_NONBLOCK', 0))
        with os.fdopen(fd, 'rb') as handle:
            info = os.fstat(handle.fileno())
            require(stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid() and info.st_mode & 0o077 == 0,
                    'Nonprivate or nonregular file rejected.')
            require(info.st_size <= max_bytes, 'Private input size limit exceeded.')
            data = handle.read(max_bytes + 1)
            require(len(data) <= max_bytes, 'Private input size limit exceeded.')
            return data
    except OSError:
        raise SetupError('Private input unavailable.') from None


def create_private_output(output_parent=None, prefix='tellpal-native-pilot-'):
    try:
        if output_parent is None:
            parent = outside_repository(Path(tempfile.gettempdir()).resolve())
        else:
            parent = outside_repository(no_symlink(output_parent))
            info = parent.stat()
            require(stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid() and info.st_mode & 0o077 == 0,
                    'Output parent must be an existing private directory.')
        output = Path(tempfile.mkdtemp(prefix=prefix, dir=parent))
        os.chmod(output, 0o700)
        return output
    except OSError:
        raise SetupError('Private output creation failed.') from None


def write_private(path, data):
    if not isinstance(data, bytes):
        data = (json.dumps(data, indent=2, allow_nan=False) + '\n').encode()
    try:
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, 'O_NOFOLLOW', 0), 0o600)
        with os.fdopen(fd, 'wb') as handle:
            handle.write(data)
    except OSError:
        raise SetupError('Private receipt write failed.') from None


def source_snapshot():
    result = {}
    try:
        for name in SOURCE_PATHS:
            path = ROOT / name
            require(path.is_file() and not path.is_symlink(), 'Selected source unavailable.')
            result[name] = sha256(path.read_bytes()).hexdigest()
    except OSError:
        raise SetupError('Selected source read failed.') from None
    return result


def canonical_uid():
    try:
        value = strict_json((ROOT / 'tools/mobile-test-account.json').read_bytes())
        require(isinstance(value, dict) and isinstance(value.get('firebaseUid'), str),
                'Canonical QA selection invalid.')
        return value['firebaseUid']
    except OSError:
        raise SetupError('Canonical QA selection unavailable.') from None


def default_config():
    return {
        'schema_version': 1, 'scope': SCOPE, 'qa_uid': canonical_uid(),
        'selection': {
            'ios': {'alias': 'pilot-ios', 'platform': 'ios', 'kind': 'physical',
                    'provider': 'local', 'selected_model': 'iPhone 12'},
            'android': {'alias': 'pilot-android', 'platform': 'android', 'kind': 'physical',
                        'provider': 'android_device_streaming', 'selected_model': None}},
        'cloud': {'project_alias': None, 'account_alias': None,
                  'quota_minutes': None, 'spending_limit_usd': None},
        'collection_limits': {'timeout_seconds': 8, 'max_output_bytes': 262144},
    }


def validate_config(config, snapshot):
    exact_keys(config, ('schema_version', 'scope', 'qa_uid', 'selection', 'cloud', 'collection_limits'),
               ('source_snapshot',))
    require(type(config['schema_version']) is int and config['schema_version'] == 1 and config['scope'] == SCOPE,
            'Configuration schema/scope rejected.')
    require(config['qa_uid'] == canonical_uid(), 'Noncanonical QA selection rejected.')
    exact_keys(config['selection'], ('ios', 'android'))
    aliases = set()
    for platform, provider, model in (('ios', 'local', 'iPhone 12'),
                                      ('android', 'android_device_streaming', None)):
        row = config['selection'][platform]
        exact_keys(row, ('alias', 'platform', 'kind', 'provider', 'selected_model'))
        require(isinstance(row['alias'], str) and ALIAS.fullmatch(row['alias']) and row['alias'] not in aliases,
                'Invalid or duplicate opaque alias rejected.')
        aliases.add(row['alias'])
        require(row['platform'] == platform and row['kind'] == 'physical' and row['provider'] == provider and
                row['selected_model'] == model, 'Selected platform/provider/model rejected.')
    exact_keys(config['cloud'], ('project_alias', 'account_alias', 'quota_minutes', 'spending_limit_usd'))
    for key in ('project_alias', 'account_alias'):
        value = config['cloud'][key]
        require(value is None or isinstance(value, str) and ALIAS.fullmatch(value), 'Invalid opaque cloud alias rejected.')
    for key in ('quota_minutes', 'spending_limit_usd'):
        value = config['cloud'][key]
        require(value is None or type(value) in (int, float) and 0 <= value <= 1000000 and math.isfinite(value),
                'Invalid cloud numeric declaration rejected.')
    exact_keys(config['collection_limits'], ('timeout_seconds', 'max_output_bytes'))
    for key, lower, upper in (('timeout_seconds', 1, 30), ('max_output_bytes', 1024, 1048576)):
        value = config['collection_limits'][key]
        require(type(value) is int and lower <= value <= upper, 'Invalid collection bound rejected.')
    if 'source_snapshot' in config:
        claims = config['source_snapshot']
        require(isinstance(claims, dict) and set(claims) == set(snapshot) and
                all(isinstance(value, str) and HASH.fullmatch(value) for value in claims.values()),
                'Incomplete source snapshot rejected.')
        require(claims == snapshot, 'Stale source snapshot rejected; explicit recheck required.')
    return config


def run_bounded(argv, receipt_dir, probe_id, timeout_seconds=8, max_bytes=262144, env=None, cwd=None,
                bounded_file=None):
    """Run an internal read-only argument array; raw output/provenance remains private."""
    require(isinstance(argv, list) and argv and all(isinstance(arg, str) for arg in argv), 'Invalid probe rejected.')
    require(ALIAS.fullmatch(probe_id), 'Invalid probe receipt alias rejected.')
    receipt_dir = Path(receipt_dir)
    started = now()
    output_paths = [receipt_dir / (probe_id + suffix) for suffix in ('.stdout', '.stderr')]
    process = None
    exit_code = None
    status = 'failed'
    reason = 'Read-only probe failed; availability remains unknown.'
    handles = []
    selector = selectors.DefaultSelector()
    written = 0
    side_budget = max_bytes // 2 if bounded_file is not None else 0
    pipe_budget = max_bytes - side_budget
    try:
        for path in output_paths:
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            handles.append(os.fdopen(fd, 'wb'))
        try:
            def file_limit():
                if bounded_file is not None:
                    os.umask(0o077)
                    resource.setrlimit(resource.RLIMIT_FSIZE, (side_budget, side_budget))
            process = subprocess.Popen(argv, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       env=env, cwd=cwd, start_new_session=True, preexec_fn=file_limit)
        except FileNotFoundError:
            status, reason = 'missing_executable', 'Executable unavailable; availability remains unknown.'
        except OSError:
            status, reason = 'failed', 'Probe could not start; availability remains unknown.'
        if process is not None:
            selector.register(process.stdout, selectors.EVENT_READ, handles[0])
            selector.register(process.stderr, selectors.EVENT_READ, handles[1])
            deadline = time.monotonic() + timeout_seconds
            while True:
                exit_code = process.poll()
                side_bytes = Path(bounded_file).stat().st_size if bounded_file is not None and Path(bounded_file).exists() else 0
                if bounded_file is not None and side_bytes > side_budget:
                    status, reason = 'output_limit', 'Probe output limit exceeded; availability remains unknown.'
                    break
                if exit_code is not None and not selector.get_map():
                    status = 'ok' if exit_code == 0 else 'failed'
                    reason = None if exit_code == 0 else 'Read-only probe returned failure; availability remains unknown.'
                    if bounded_file is not None and exit_code == -signal.SIGXFSZ:
                        status, reason = 'output_limit', 'Probe output limit exceeded; availability remains unknown.'
                    break
                if time.monotonic() >= deadline:
                    status, reason = 'timeout', 'Read-only probe timed out; availability remains unknown.'
                    break
                for key, _ in selector.select(min(0.05, max(0, deadline - time.monotonic()))):
                    chunk = os.read(key.fileobj.fileno(), 8192)
                    if not chunk:
                        selector.unregister(key.fileobj)
                        continue
                    retained_chunk = chunk[:max(0, pipe_budget - written)]
                    key.data.write(retained_chunk)
                    written += len(retained_chunk)
                    if len(retained_chunk) < len(chunk):
                        status, reason = 'output_limit', 'Probe output limit exceeded; availability remains unknown.'
                        break
                if status == 'output_limit':
                    break
            if status in {'timeout', 'output_limit'}:
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
            exit_code = process.wait(timeout=2)
    finally:
        if process is not None and process.poll() is None:
            try:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=2)
            except (OSError, subprocess.TimeoutExpired):
                pass
        for handle in handles:
            handle.close()
        selector.close()
        if process is not None:
            if process.stdout is not None:
                process.stdout.close()
            if process.stderr is not None:
                process.stderr.close()
    retained = []
    remaining = max_bytes
    for path in output_paths:
        with path.open('r+b') as handle:
            data = handle.read(remaining)
            handle.truncate(len(data))
        remaining -= len(data)
        retained.append((len(data), sha256(data).hexdigest()))
    if bounded_file is not None and Path(bounded_file).exists():
        os.chmod(bounded_file, 0o600)
        with Path(bounded_file).open('r+b') as handle:
            handle.truncate(min(handle.seek(0, 2), remaining))
    result = {'status': status, 'exit_code': exit_code, 'reason': reason,
              'provenance': {'probe_id': probe_id, 'started_at': started, 'finished_at': now(),
                             'argv': argv, 'executable_path': shutil.which(argv[0])},
              'stdout_file': output_paths[0].name, 'stderr_file': output_paths[1].name,
              'stdout_bytes': retained[0][0], 'stderr_bytes': retained[1][0],
              'stdout_sha256': retained[0][1], 'stderr_sha256': retained[1][1]}
    if bounded_file is not None and Path(bounded_file).exists():
        native_data = Path(bounded_file).read_bytes()
        result.update(native_file=Path(bounded_file).name, native_bytes=len(native_data),
                      native_sha256=sha256(native_data).hexdigest())
    write_private(receipt_dir / (probe_id + '.json'), result)
    return result


def unknown(reason, evidence_ref='setup-scope'):
    return {'availability': 'unverified', 'value': None, 'reason': reason, 'evidence_ref': evidence_ref}


def observed(value, evidence_ref):
    return {'availability': 'observed', 'value': value, 'reason': None, 'evidence_ref': evidence_ref}


def parse_adb_inventory(data, evidence_ref):
    try:
        lines = data.decode('utf-8', errors='strict').splitlines()
        require(lines and lines[0].strip() == 'List of devices attached', 'Malformed inventory receipt.')
        devices = []
        for line in lines[1:]:
            if not line.strip():
                continue
            parts = line.split()
            require(len(parts) >= 2 and parts[1] in {'device', 'offline', 'unauthorized', 'recovery',
                                                   'sideload', 'bootloader', 'host', 'no'},
                    'Malformed inventory receipt.')
            devices.append({'alias': f'adb-observation-{len(devices) + 1:03d}', 'platform': observed('android', evidence_ref),
                            'kind': unknown('ADB transport does not verify physical hardware or provider.', evidence_ref),
                            'connection': observed(parts[1], evidence_ref),
                            'model': unknown('Model/API/RAM and selected reservation binding require separate qualification.', evidence_ref),
                            'os_version': unknown('ADB inventory does not qualify API/OS version.', evidence_ref),
                            'os_build': unknown('ADB inventory does not qualify the OS build.', evidence_ref),
                            'selected_device_match': unknown('Raw transport identity is not a selected-pilot binding.', evidence_ref),
                            'runnable': unknown('No build/install/run qualification performed.', evidence_ref)})
        return devices
    except (UnicodeError, SetupError):
        raise SetupError('Malformed inventory receipt; availability remains unknown.') from None


def parse_ios_inventory(data, evidence_ref):
    payload = strict_json(data)
    require(isinstance(payload, dict) and isinstance(payload.get('info'), dict) and
            payload['info'].get('outcome') == 'success' and isinstance(payload.get('result'), dict) and
            isinstance(payload['result'].get('devices'), list), 'Malformed inventory receipt; availability remains unknown.')
    devices = []
    for raw in payload['result']['devices']:
        require(isinstance(raw, dict), 'Malformed inventory receipt; availability remains unknown.')
        hardware = raw.get('hardwareProperties') if isinstance(raw.get('hardwareProperties'), dict) else {}
        connection = raw.get('connectionProperties') if isinstance(raw.get('connectionProperties'), dict) else {}
        properties = raw.get('deviceProperties') if isinstance(raw.get('deviceProperties'), dict) else {}
        # Preserve the service observation privately; selection and runnable status remain separate.
        reality = hardware.get('reality')
        state = connection.get('tunnelState')
        platform = hardware.get('platform')
        platform_value = observed('ios', evidence_ref) if platform == 'iOS' else unknown(
            'Service record does not establish an iOS platform; other Apple/legacy rows remain unqualified.', evidence_ref)
        def metadata(value, reason):
            return observed(value, evidence_ref) if isinstance(value, str) and value and len(value) <= 256 else unknown(reason, evidence_ref)
        devices.append({'alias': f'ios-observation-{len(devices) + 1:03d}', 'platform': platform_value,
                        'kind': observed(reality, evidence_ref) if isinstance(reality, str) and reality in {'physical', 'simulated'} else
                            unknown('Service record lacks a supported physical/simulated reality.', evidence_ref),
                        'connection': observed(state, evidence_ref) if isinstance(state, str) and state in {'connected', 'disconnected', 'connecting'} else
                            unknown('Service record lacks a supported connection state.', evidence_ref),
                        'model': metadata(hardware.get('marketingName'), 'Service record lacks exact observed model metadata.'),
                        'os_version': metadata(properties.get('osVersionNumber'), 'Service record lacks OS version metadata.'),
                        'os_build': metadata(properties.get('osBuildUpdate'), 'Service record lacks OS build metadata.'),
                        'selected_device_match': unknown('No selected-device identity binding performed.', evidence_ref),
                        'runnable': unknown('No signing/build/install/run qualification performed.', evidence_ref)})
    return devices


def tool_identity(probe_id, data, evidence_ref):
    """Extract only version/build tokens; detailed tool output remains in its receipt."""
    patterns = {
        'adb-version': rb'Android Debug Bridge version ([0-9.]+).*?Version ([0-9A-Za-z._-]+)',
        'xcode-version': rb'Xcode ([0-9.]+).*?Build version ([0-9A-Za-z._-]+)',
        'xcrun-version': rb'xcrun version ([0-9]+(?:\.[0-9]+)*)',
        'xctrace-version': rb'xctrace version ([0-9.]+)',
        'android-version': rb'(?:android|Android)[^\r\n]*?([0-9]+\.[0-9]+(?:\.[0-9]+)?)',
    }
    pattern = patterns.get(probe_id)
    match = re.search(pattern, data, re.DOTALL) if pattern else None
    if match is None:
        return unknown('Exact tool version/build not established by this receipt.', evidence_ref)
    return observed({'version': match[1].decode('ascii'),
                     'build': match[2].decode('ascii') if len(match.groups()) > 1 else None}, evidence_ref)


def collect_local(output, limits):
    limits = {'timeout_seconds': limits['timeout_seconds'], 'max_bytes': limits['max_output_bytes']}
    receipts = output / 'receipts'
    receipts.mkdir(mode=0o700)
    probes = [('adb-version', ['adb', 'version']), ('xcode-version', ['xcodebuild', '-version']),
              ('xcrun-version', ['xcrun', '--version']), ('xctrace-version', ['xcrun', 'xctrace', 'version']),
              ('android-version', ['android', '-V']), ('android-streaming-help', ['android', 'device', 'remote', '-h'])]
    records = {}
    for probe_id, argv in probes:
        records[probe_id] = run_bounded(argv, receipts, probe_id, **limits)
    records['adb-inventory'] = run_bounded(['adb', 'devices', '-l'], receipts, 'adb-inventory', **limits)
    ios_file = receipts / 'ios-inventory-native.json'
    records['ios-inventory'] = run_bounded(['xcrun', 'devicectl', 'list', 'devices', '--json-output', str(ios_file)],
                                           receipts, 'ios-inventory', bounded_file=ios_file, **limits)
    tools = []
    for probe_id, _ in probes:
        record = records[probe_id]
        identity_data = (receipts / record['stdout_file']).read_bytes() + (receipts / record['stderr_file']).read_bytes()
        tools.append({'alias': probe_id, 'identity': tool_identity(probe_id, identity_data, probe_id)
                      if record['status'] == 'ok' else unknown(record['reason'], probe_id),
                      'probe_status': record['status']})
    # Both successful CLI identity/help probes are necessary; PATH alone does not qualify the legacy launcher.
    help_data = (receipts / records['android-streaming-help']['stdout_file']).read_bytes()
    streaming = (records['android-version']['status'] == records['android-streaming-help']['status'] == 'ok' and
                 next(row for row in tools if row['alias'] == 'android-version')['identity']['availability'] == 'observed' and
                 all(re.search(rb'\b' + command + rb'\b', help_data) for command in (b'create', b'connect', b'projects')))
    devices, inventories = [], []
    for name, parser, path in (('adb-inventory', parse_adb_inventory, receipts / records['adb-inventory']['stdout_file']),
                                ('ios-inventory', parse_ios_inventory, ios_file)):
        record = records[name]
        reason = record['reason']
        rows = None
        if record['status'] == 'ok':
            try:
                rows = parser(path.read_bytes(), name)
            except (OSError, SetupError):
                reason = 'Malformed or unavailable inventory receipt; availability remains unknown.'
        inventories.append({'alias': name, 'result': observed({'listed_count': len(rows)}, name) if rows is not None
                            else unknown(reason, name), 'probe_status': record['status']})
        if rows is not None:
            devices.extend(rows)
    return {'tools': tools, 'inventories': inventories, 'devices': devices,
            'streaming_client_probe': observed(True, 'android-streaming-help') if streaming else
                unknown('Current Streaming CLI not qualified by both identity/help probes.', 'android-streaming-help'),
            'raw_receipts_private': True}


def build_report(config, snapshot, local=None):
    report = {'schema_version': 1, 'scope': SCOPE, 'report_alias': 'setup-' + uuid.uuid4().hex[:16],
              'created_at': now(), 'collection_mode': 'local' if local is not None else 'offline',
              'readiness_status': 'blocked', 'native_execution_ready': False,
              'selection': config['selection'], 'source_snapshot': snapshot,
              'qa_account': {'alias': 'canonical-qa', 'selection_evidence': 'tools/mobile-test-account.json',
                             'account_verification': unknown('Account existence/enabled status not queried.')},
              'cloud_declarations': {'availability': 'unverified', 'value': config['cloud'],
                                     'evidence_ref': 'private-config-or-default',
                                     'reason': 'Declarations do not verify account, project, IAM, quota or payment access.'},
              'collection': local or {'tools': [], 'inventories': [], 'devices': [],
                                      'streaming_client_probe': unknown('Offline plan; local collection not requested.'),
                                      'raw_receipts_private': True},
              'gates': [{'id': key, 'status': 'open', 'acceptance_ref': None, 'implementation_ref': None,
                         'verification_ref': None, 'next_action': action} for key, action in GATE_ACTIONS.items()],
              'coverage_gaps': ['Android API 24', 'iOS 15', 'iPadOS 15', 'low-RAM physical classes',
                                'Android tablets', 'iPads', 'other modern controls',
                                'reader/alpha/EXIF/animation/non-STORY native quality'],
              'setup_gaps': ['Current selected-device connection and runnable isolated signed artifact unqualified.',
                             'Streaming project/account/IAM/quota and human-owned reservation unverified.',
                             'Token-free broker and local/test backend device reachability unverified.',
                             'Exact OQ-6 acceptance, implementation and positive/negative verification absent.'],
              'build': unknown('No app/native build, signing, install or execution performed.'),
              'counters': {key: unknown('Actual device/build/mode/tool support and overhead unverified.') for key in
                           ('ram', 'dart_heap', 'dart_external', 'decoded_live_pending', 'first_target_raster',
                            'physical_presentation', 'android_rss_pss_native_graphics', 'ios_footprint_allocations')},
              'limitations': ['Selection is not observation or measurement admission.',
                              'Failed probes are access/tool failures, never evidence of device absence.',
                              'Streaming video is not physical color, target paint or presentation evidence.',
                              'No benchmark, authenticated access, cloud session or candidate delivery performed.']}
    return report


def main(argv=None):
    parser = SafeParser(description=__doc__)
    parser.add_argument('--config', help='Private strict JSON configuration; never contains credentials.')
    parser.add_argument('--collect-local', action='store_true', help='Explicit bounded read-only local probes.')
    parser.add_argument('--output-parent', help='Existing private directory outside repository; unique child is created.')
    args = parser.parse_args(argv)
    try:
        snapshot = source_snapshot()
        config = default_config() if args.config is None else strict_json(private_file(args.config))
        validate_config(config, snapshot)
        output = create_private_output(args.output_parent)
        local = collect_local(output, config['collection_limits']) if args.collect_local else None
        require(source_snapshot() == snapshot, 'Sources changed during collection; explicit recheck required.')
        report = build_report(config, snapshot, local)
        write_private(output / 'report.json', report)
        print(f"{report['report_alias']} timestamp={report['created_at']} status=blocked gates=open native_execution_ready=false")
        return 3
    except SetupError as error:
        print('preflight rejected: ' + str(error), file=sys.stderr)
        return 2
    except Exception:
        print('preflight failed: private operational error; no native readiness claim.', file=sys.stderr)
        return 4


if __name__ == '__main__':
    sys.exit(main())
