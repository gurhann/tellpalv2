#!/usr/bin/env python3
"""Verify Story 1.5 preparation with local reads only; never discover or run devices."""
import argparse
from datetime import datetime
from hashlib import sha256
import json
import math
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
BASELINE = '7e3ee8f58a210196ae6988dfa38481d5bd9a7957'
SCOPE = 'image-optimization-p0c-1'
AVAILABILITY = {'observed', 'historical_only', 'unverified', 'unavailable', 'not_applicable'}
HASH = re.compile(r'[0-9a-f]{64}')
RECEIPTS = {
    'E-ADB-VERSION': ('command_receipt', 'adb-version.json'),
    'E-ADB-DEVICES': ('command_receipt', 'adb-final.json'),
    'E-XCODE': ('command_receipt', 'xcode-version.json'),
    'E-XCRUN': ('command_receipt', 'xcrun-version.json'),
    'E-XCTRACE': ('command_receipt', 'trace-version.json'),
    'E-IOS-DEVICES': ('native_json', 'devicectl-escalated.json'),
    'E-SIMULATORS': ('native_json', 'simctl-escalated.json'),
    'E-FLUTTER-CACHE': ('cached_sdk_identity', 'flutter-cached-version.json'),
    'E-DART-DIRECT': ('command_receipt', 'dart-direct-version.json'),
    'E-INITIAL-ADB-DEVICES': ('failed_sandbox_receipt', 'adb-devices.json'),
    'E-INITIAL-IOS-DEVICES': ('failed_sandbox_receipt', 'ios-devices.json'),
    'E-INITIAL-SIMULATOR-DEVICES': ('failed_sandbox_receipt', 'simulator-devices.json'),
    'E-INITIAL-DART-VERSION': ('failed_sandbox_receipt', 'dart-version.json'),
}
TOOL_SOURCES = {
    'adb': 'E-ADB-VERSION', 'xcode': 'E-XCODE', 'xcrun': 'E-XCRUN',
    'xctrace': 'E-XCTRACE', 'flutter-cached-sdk': 'E-FLUTTER-CACHE',
    'dart-direct': 'E-DART-DIRECT',
}
COVERAGE = {
    'constrained-android-phone': ('android', 'phone', 'API 24'),
    'constrained-android-tablet': ('android', 'tablet', 'API 24'),
    'oldest-available-iphone': ('ios', 'phone', 'iOS 15'),
    'oldest-available-ipad': ('ios', 'tablet', 'iPadOS 15'),
    'modern-android-control': ('android', 'any', 'API 24'),
    'modern-apple-control': ('ios', 'any', 'iOS/iPadOS 15'),
    'android-api24-floor': ('android', 'any', 'API 24'),
    'iphone-ios15-floor': ('ios', 'phone', 'iOS 15'),
    'ipad-ipados15-floor': ('ios', 'tablet', 'iPadOS 15'),
}
COUNTERS = {
    'dart_heap': ('flutter', 'bytes', 'flutter-cached-sdk', 'E-FLUTTER-CACHE'),
    'dart_external': ('flutter', 'bytes', 'flutter-cached-sdk', 'E-FLUTTER-CACHE'),
    'raster_cache': ('flutter', 'bytes', 'flutter-cached-sdk', 'E-FLUTTER-CACHE'),
    'decoded_lru': ('flutter', 'bytes_and_count', 'source', 'source_snapshot'),
    'live_pending_owners': ('flutter', 'count', 'source', 'source_snapshot'),
    'first_target_raster': ('flutter', 'microseconds', 'source', 'source_snapshot'),
    'android_rss': ('android', 'bytes', 'adb', 'E-ADB-VERSION'),
    'android_pss': ('android', 'bytes', 'adb', 'E-ADB-VERSION'),
    'android_private_dirty': ('android', 'bytes', 'adb', 'E-ADB-VERSION'),
    'android_native': ('android', 'bytes', 'adb', 'E-ADB-VERSION'),
    'android_graphics': ('android', 'bytes', 'adb', 'E-ADB-VERSION'),
    'ios_footprint': ('ios', 'bytes', 'xctrace', 'E-XCTRACE'),
    'ios_allocations_graphics': ('ios', 'bytes', 'xctrace', 'E-XCTRACE'),
}
GATES = {'NATIVE-ENTRY', 'NATIVE-DEVICE', 'NATIVE-CANDIDATE', 'NATIVE-PROTOCOL', 'NATIVE-DECISIONS'}
PUBLIC_REFERENCE_HOSTS = {'developer.android.com', 'developer.apple.com', 'firebase.google.com',
                          'api.flutter.dev', 'docs.flutter.dev', 'api.dart.dev'}
PUBLIC_FILES = [
    HERE / 'p0c-device-matrix.json', HERE / 'p0c-native-entry-proposal.md',
    HERE / 'p0c-native-measurement-protocol.md', HERE / 'p0c-native-readiness.md',
    HERE / 'spec-1-5-native-measurement-admission.md', Path(__file__), HERE / 'test_p0c_readiness.py',
    *(ROOT / '_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission').glob('*.md'),
]
REQUIRED_SOURCES = {
    'AGENTS.md', 'architecture.md', 'be/docs/project-memory.md', 'tools/mobile-test-account.json',
    'app/mobile/lib/main_library_dev.dart', 'app/mobile/lib/app/library_dev_harness.dart',
    'app/mobile/lib/app/library_configuration.dart', 'app/mobile/lib/app/injection.dart',
    'app/mobile/lib/app/library_app.dart', 'app/mobile/lib/app/router.dart',
    'app/mobile/lib/core/diagnostics/library_diagnostics.dart',
    'app/mobile/lib/core/network/library_image_cache.dart',
    'app/mobile/lib/core/network/story_detail_image_cache.dart',
    'app/mobile/lib/core/network/library_transfer_file_service.dart',
    'app/mobile/lib/core/network/authenticated_transport.dart',
    'app/mobile/lib/core/preview/presentation/widgets/preview_card.dart',
    'app/mobile/lib/features/story_detail/presentation/widgets/story_detail_hero.dart',
    'app/mobile/android/app/build.gradle.kts', 'app/mobile/android/app/src/main/AndroidManifest.xml',
    'app/mobile/ios/Podfile', 'app/mobile/ios/Runner.xcodeproj/project.pbxproj',
    'app/mobile/pubspec.lock', 'app/mobile/ios/Podfile.lock', 'app/mobile/.metadata',
    'be/src/main/resources/application.yml',
    'be/src/main/java/com/tellpal/v2/user/web/mobile/LibraryHomeTestController.java',
    'be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java',
    'be/src/main/java/com/tellpal/v2/user/infrastructure/firebase/FirebaseAdminTokenVerifier.java',
    *[f'be/docs/adr/{name}' for name in (
        'ADR-0017-story-page-media-manifests.md', 'ADR-0018-flutter-skill-architecture-policy.md',
        'ADR-0019-library-home-catalog.md')],
    *[f'_bmad-output/implementation-artifacts/image-optimization/{name}' for name in (
        'p0a-slot-source-inventory.md', 'p0a-slot-source-inventory.json', 'p0a-corpus.json',
        'p0a-trial-protocol.md', 'p0b-worker-findings.md', 'p0b-comparison.json')],
    *[f'_bmad-output/implementation-artifacts/mobile/{name}' for name in (
        'library-1-1-platform-builds.json', 'library-1-2-flutter-build-evidence.json')],
    *[f'_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/{name}' for name in (
        'SPEC.md', 'native-readiness-contract.md', 'native-entry-proposal.md',
        'native-measurement-protocol.md')],
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def text(value, label):
    require(isinstance(value, str) and bool(value.strip()), f'{label}: missing text')
    return value


def numeric(value, label, integer=False):
    require(type(value) in (int, float) and (type(value) is int or math.isfinite(value)) and value >= 0,
            f'{label}: invalid nonnegative numeric value')
    require(not integer or type(value) is int, f'{label}: integer required')


def timestamp(value, label):
    text(value, label)
    require(bool(re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})', value)),
            f'{label}: invalid timestamp')
    parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
    require(parsed.tzinfo is not None, f'{label}: missing timezone')
    return parsed


def checksum(value, label):
    require(isinstance(value, str) and HASH.fullmatch(value), f'{label}: invalid SHA-256')


def unique(records, key, label):
    require(isinstance(records, list), f'{label}: array required')
    result = {}
    for row in records:
        require(isinstance(row, dict), f'{label}: object required')
        name = text(row.get(key), label)
        require(name not in result, f'{label}: duplicate identifier')
        result[name] = row
    return result


def observation(row, label, sources, expected_ref=None, value_type=None):
    require(isinstance(row, dict) and set(row) == {'value', 'availability', 'evidence_ref', 'reason'},
            f'{label}: complete observation shape required')
    require(row['availability'] in AVAILABILITY, f'{label}: unknown availability')
    ref = row['evidence_ref']
    require(ref in sources or ref == 'source_snapshot', f'{label}: missing/unbound provenance')
    require(expected_ref is None or ref == expected_ref, f'{label}: wrong receipt-source binding')
    if row['value'] is None:
        require(row['availability'] != 'observed', f'{label}: observed null')
        text(row['reason'], label)
    else:
        require(row['availability'] in {'observed', 'historical_only'}, f'{label}: unavailable value')
        require(ref == 'source_snapshot' or sources[ref]['kind'] != 'failed_sandbox_receipt',
                f'{label}: failed query cannot supply observed evidence')
        if value_type == 'number':
            numeric(row['value'], label, integer=True)
            require(row['value'] > 0, f'{label}: observed RAM must be positive')
        elif value_type == 'bool':
            require(type(row['value']) is bool, f'{label}: boolean required')
        elif value_type == 'text':
            text(row['value'], label)


def parse_json(data):
    """Parse an already captured byte snapshot with strict JSON evidence semantics."""
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, 'Duplicate JSON object key')
            result[key] = value
        return result
    def finite_float(value):
        parsed = float(value)
        require(math.isfinite(parsed), 'Nonfinite JSON number')
        return parsed
    return json.loads(data, object_pairs_hook=pairs, parse_float=finite_float,
                      parse_constant=lambda _: (_ for _ in ()).throw(ValueError('Nonfinite JSON number')))


def load_json(path):
    return parse_json(path.read_bytes())


def simulator_metadata_gaps(device):
    """A simulator device listing cannot supply these physical/platform metadata fields."""
    expected = {'product_type': 'unverified', 'os_build': 'unverified',
                'pairing': 'not_applicable', 'ddi_services_available': 'not_applicable'}
    for field, availability in expected.items():
        row = device[field]
        require(isinstance(row, dict) and set(row) == {'value', 'availability', 'evidence_ref', 'reason'} and
                row['value'] is None and row['availability'] == availability and row['evidence_ref'] == 'E-SIMULATORS',
                'Unsupported simulator metadata claim')
        text(row['reason'], 'simulator metadata gap')


def receipt_form_factor(raw, kind):
    """Classify only explicit supported Apple product families present in the receipt."""
    if kind == 'physical':
        hardware = raw['hardwareProperties']
        product = re.fullmatch(r'(iPhone|iPad)\d+,\d+', hardware['productType'])
        model = re.match(r'^(iPhone|iPad)(?:\s|$)', hardware['marketingName'])
        require(product is not None and model is not None and product[1] == model[1],
                'Unsupported/conflicting physical form-factor receipt')
        family = product[1]
    else:
        require(kind == 'simulator', 'Unsupported form-factor receipt kind')
        identifier = re.match(r'^com\.apple\.CoreSimulator\.SimDeviceType\.(iPhone|iPad)(?:-|$)',
                              raw['deviceTypeIdentifier'])
        require(identifier is not None, 'Unsupported simulator form-factor receipt')
        family = identifier[1]
    return {'iPhone': 'phone', 'iPad': 'tablet'}[family]


def local_file(relative):
    require(isinstance(relative, str) and not Path(relative).is_absolute() and '..' not in Path(relative).parts,
            'Repository source path must be relative and bounded')
    path = ROOT / relative
    require(path.is_file() and not path.is_symlink() and ROOT in path.resolve().parents,
            'Missing/unsafe repository source')
    return path


def sources_and_guards(matrix):
    sources = unique(matrix['source_snapshot'], 'path', 'source snapshot')
    require(set(sources) == REQUIRED_SOURCES, 'Source snapshot coverage changed')
    tracked = set(subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', BASELINE], cwd=ROOT,
                                        stderr=subprocess.DEVNULL).decode().splitlines())
    for name, row in sources.items():
        require(set(row) == {'path', 'sha256'}, 'Unexpected source snapshot fields')
        checksum(row['sha256'], 'source hash')
        data = local_file(name).read_bytes()
        require(sha256(data).hexdigest() == row['sha256'], f'Changed current source hash: {name}')
        if name in tracked:
            baseline_data = subprocess.check_output(['git', 'show', f'{BASELINE}:{name}'], cwd=ROOT,
                                                    stderr=subprocess.DEVNULL)
            require(data == baseline_data, f'Preserved source differs from baseline: {name}')
    entry = local_file('app/mobile/lib/main_library_dev.dart').read_text()
    require(entry.index('if (kReleaseMode)') < entry.index('waitForConfiguration()') and
            'Development entry unavailable' in entry and 'maximumSize = 48' in entry and
            'maximumSizeBytes = 32 * 1024 * 1024' in entry, 'Library release/cache controls changed')
    harness = local_file('app/mobile/lib/app/library_dev_harness.dart').read_text()
    require(harness.count("if (kReleaseMode) throw StateError('Development entry is unavailable')") == 2,
            'Harness release guards changed')
    for name in ('web/mobile/LibraryHomeTestController.java', 'application/LibraryHomeTestQueryService.java'):
        require('@Profile({"local","test"})' in local_file('be/src/main/java/com/tellpal/v2/user/' + name).read_text(),
                'Backend local/test guard changed')
    service = local_file('be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java').read_text()
    require('users.resolveOrCreateByIdToken' in service and 'if(!allowed.contains(uid))' in service and
            '${tellpal.library.test.allowed-firebase-uids:}' in service and
            'this.allowed.containsAll(this.subscribers)' in service, 'Firebase/allowlist/audience guards changed')
    require(load_json(local_file('tools/mobile-test-account.json'))['firebaseUid'] == 'jLRypTmoBrhhX21HsDJojurcKTC3',
            'Default QA UID changed')
    require('minSdk = 24' in local_file('app/mobile/android/app/build.gradle.kts').read_text(), 'Android floor changed')
    require("platform :ios, '15.0'" in local_file('app/mobile/ios/Podfile').read_text(), 'Apple floor changed')
    xcode = local_file('app/mobile/ios/Runner.xcodeproj/project.pbxproj').read_text()
    require(set(re.findall(r'IPHONEOS_DEPLOYMENT_TARGET = ([^;]+);', xcode)) == {'15.0'}, 'Xcode floors changed')
    return sources


def public_documents(matrix_path):
    files = [matrix_path if path.name == 'p0c-device-matrix.json' else path for path in PUBLIC_FILES]
    count = 0
    for path in files:
        require(path.is_file() and not path.is_symlink(), f'Missing deliverable: {path.name}')
        content = path.read_text()
        require(not re.search(r'(?:/' + r'Users/[^\s]+|/' + r'private/(?:tmp|var)/[^\s]+|[A-Z]:\\Users\\)', content),
                f'Private filesystem path in {path.name}')
        require(not re.search(r'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|'
                              r'(?i:X-Goog-' + r'Signature=|X-Amz-' + r'Signature=|-----BEGIN .*PRIVATE ' + r'KEY-----)', content),
                f'Credential/private URL in {path.name}')
        require(not re.search(r'\b[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}\b|'
                              r'\b[0-9A-Fa-f]{8}-[0-9A-Fa-f]{16}\b', content),
                f'Raw device identifier in {path.name}')
        # This one loopback template is a proposed command, never an observed endpoint.
        url_content = content.replace('https://127.0.0.1:<device-port>', 'BROKER_ORIGIN_TEMPLATE')
        for target in re.findall(r'https?://[^\s)\'"`]+', url_content):
            parsed = urlsplit(target)
            require(parsed.scheme == 'https' and parsed.hostname in PUBLIC_REFERENCE_HOSTS and
                    not parsed.query and not parsed.username, f'Private/non-reference URL in {path.name}')
        require(all(line == line.rstrip() for line in content.splitlines()), f'Trailing whitespace in {path.name}')
        if path.suffix != '.md':
            continue
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', content):
            parsed = urlsplit(target)
            if parsed.scheme in {'http', 'https'}:
                require(parsed.scheme == 'https' and parsed.hostname in PUBLIC_REFERENCE_HOSTS, 'Non-public reference URL')
                require(not parsed.query, 'Query-bearing public reference URL')
                continue
            require(not parsed.scheme and not target.startswith('/'), 'Absolute/private document link')
            resolved = (path.parent / unquote(parsed.path)).resolve()
            require(resolved.is_file() and ROOT in resolved.parents, f'Broken local link in {path.name}')
            count += 1
    entry = (HERE / 'p0c-native-entry-proposal.md').read_text()
    protocol = (HERE / 'p0c-native-measurement-protocol.md').read_text()
    require(all(term in entry for term in ('OQ-6 is open', 'takeSession', 'revoke', 'ImageExperimentUITests',
                'imageExperiment=true', 'human acceptance', 'rollback', 'local/test', 'Firebase', 'GetIt')),
            'Entry proposal missing review boundary')
    require(all(term in protocol for term in ('network_cold', 'disk_warm_decoded_cold', 'decoded_warm',
                'frameNumber', 'unverified_clock_mapping', 'signed', 'settlement_timeout', 'Synthetic pressure',
                'delivered_payload_bytes', 'overlap', 'Five', '30 uninterrupted', '1 sample/second')),
            'Protocol proposal missing measurement boundary')
    return count


def verify(matrix_path, private_root=None):
    matrix = load_json(matrix_path)
    require(isinstance(matrix, dict) and set(matrix) == {
        'schema_version', 'scope', 'observed_at', 'baseline_commit', 'preparation_complete', 'native_execution_ready',
        'collection_scope', 'evidence_sources', 'source_snapshot', 'probes', 'tools', 'devices',
        'virtual_runtime_summary', 'coverage', 'counters', 'experiment_build', 'gates', 'summary',
        'historical_evidence', 'limitations'}, 'Unexpected/missing matrix fields')
    require(type(matrix.get('schema_version')) is int and matrix['schema_version'] == 1, 'Invalid schema version')
    require(matrix.get('scope') == SCOPE and matrix.get('baseline_commit') == BASELINE, 'Wrong scope/baseline')
    observed_at = timestamp(matrix.get('observed_at'), 'matrix timestamp')
    require(type(matrix.get('preparation_complete')) is bool, 'Invalid preparation status')
    require(matrix.get('native_execution_ready') is False, 'Preparation cannot claim native readiness')
    require(matrix.get('collection_scope') ==
            'read_only_installed_tools_and_registered_device_discovery_no_boot_build_install_auth_or_workload',
            'Expanded collection scope')
    sources = unique(matrix['evidence_sources'], 'id', 'receipt sources')
    require(set(sources) == set(RECEIPTS), 'Missing/unexpected receipt sources')
    for name, row in sources.items():
        require(set(row) == {'id', 'kind', 'receipt_file', 'receipt_sha256', 'storage', 'verified_in_this_collection'},
                'Invalid receipt source shape')
        require((row['kind'], row['receipt_file']) == RECEIPTS[name] and row['storage'] == 'private_external',
                'Changed receipt-source binding')
        checksum(row['receipt_sha256'], 'receipt hash')
        require(row['verified_in_this_collection'] is True, 'Collection receipt not verified')
    require(len({row['receipt_sha256'] for row in sources.values()}) == len(sources), 'Duplicate receipt hashes')
    probes = unique(matrix['probes'], 'id', 'probes')
    require(set(probes) == {'adb-version', 'adb-devices', 'xcode-version', 'xcrun-version',
                          'ios-devices', 'simulator-devices', 'trace-version', 'dart-version'}, 'Probe coverage changed')
    by_file = {row['receipt_file']: row for row in sources.values()}
    for name, row in probes.items():
        require(set(row) == {'id', 'observed_at', 'exit_code', 'duration_seconds', 'receipt_sha256',
                             'scope', 'availability', 'reason'}, 'Invalid probe shape')
        require(timestamp(row['observed_at'], 'probe timestamp') <= observed_at, 'Probe after collection')
        require(type(row['exit_code']) is int, 'Invalid probe exit code')
        numeric(row['duration_seconds'], 'probe duration')
        require(row['scope'] == 'sandbox_initial' and row['receipt_sha256'] == by_file[name + '.json']['receipt_sha256'],
                'Probe receipt-source binding changed')
        require(row['availability'] == ('observed' if row['exit_code'] == 0 else 'unavailable'), 'Invalid probe availability')
        if row['exit_code'] != 0:
            text(row['reason'], 'failed sandbox reason')
            require(by_file[name + '.json']['kind'] == 'failed_sandbox_receipt', 'Failed sandbox receipt relabeled')
    tools = unique(matrix['tools'], 'id', 'tools')
    require(set(tools) == set(TOOL_SOURCES), 'Tool coverage changed')
    for name, row in tools.items():
        expected_keys = {'id', 'identity', 'executed', 'runtime_qualification',
                         'executable_or_package_identity', 'permissions_and_attachment'}
        if name == 'flutter-cached-sdk':
            expected_keys |= {'framework_revision', 'engine_revision', 'dart_sdk_version', 'devtools_version', 'identity_scope'}
        require(set(row) == expected_keys, 'Unexpected/missing tool fields')
        observation(row['identity'], f'tool {name}', sources, TOOL_SOURCES[name], 'text')
        for gap in ('executable_or_package_identity', 'permissions_and_attachment'):
            observation(row[gap], f'tool {name} {gap}', sources, TOOL_SOURCES[name], 'text')
            require(row[gap]['value'] is None, 'Unverified tool execution capability invented')
        text(row['executed'], 'tool query scope')
        require(row['runtime_qualification'] is False, 'Installed tool is not native qualification')
    sdk = tools['flutter-cached-sdk']
    for key in ('framework_revision', 'engine_revision'):
        require(isinstance(sdk.get(key), str) and re.fullmatch(r'[0-9a-f]{40}', sdk[key]), 'Invalid cached SDK revision')
    require(sdk.get('identity_scope') == 'installed_cached_build_inputs_not_experiment_runtime', 'SDK scope changed')
    devices = unique(matrix['devices'], 'alias', 'devices')
    for alias, row in devices.items():
        expected_keys = {'alias', 'kind', 'platform', 'form_factor', 'model', 'product_type', 'os', 'os_build',
                         'metadata_provenance', 'connection', 'pairing', 'ddi_services_available', 'ram_bytes',
                         'runnable', 'qualified_for_native_batch', 'evidence_pointer', 'observed_at',
                         'floor_control_role', 'next_action', 'observed_at_kind'}
        if row.get('kind') == 'simulator':
            expected_keys.add('model_source_field')
        require(set(row) == expected_keys, 'Unexpected/missing device fields')
        require(timestamp(row['observed_at'], 'device discovery timestamp') == observed_at,
                'Device discovery timestamp changed')
        require(row['observed_at_kind'] == 'discovery_projection_timestamp_not_live_runtime',
                'Discovery timestamp relabeled live runtime')
        text(row['floor_control_role'], 'device role gap')
        text(row['next_action'], 'device next action')
        require(re.fullmatch(r'(?:ios|android)-(?:physical|simulator|emulator)-[1-9][0-9]*', alias), 'Non-opaque device alias')
        require(row['kind'] in {'physical', 'emulator', 'simulator'} and row['platform'] in {'ios', 'android'} and
                row['form_factor'] in {'phone', 'tablet'}, 'Invalid device category')
        expected_ref = 'E-IOS-DEVICES' if row['kind'] == 'physical' else 'E-SIMULATORS'
        require(alias.startswith('ios-') and row['platform'] == 'ios', 'Unobserved device expansion')
        require(row['kind'] in {'physical', 'simulator'}, 'Unobserved emulator record')
        for field in ('model', 'product_type', 'os', 'os_build', 'connection', 'pairing'):
            observation(row[field], f'{alias} {field}', sources, expected_ref, 'text')
        for field in ('ddi_services_available', 'runnable'):
            observation(row[field], f'{alias} {field}', sources, expected_ref, 'bool')
        observation(row['ram_bytes'], f'{alias} RAM', sources, expected_ref, 'number')
        require(row['ram_bytes']['value'] is None and row['runnable']['value'] is None,
                'Discovery cannot invent RAM/runnable observations')
        require(row['qualified_for_native_batch'] is False, 'Discovery record cannot qualify native batch')
        require(row['metadata_provenance'] == ('registered_device_metadata_not_live_hardware_query'
                if row['kind'] == 'physical' else 'registered_simulator_profile_not_booted_runtime'),
                'Device provenance changed')
        pointer = text(row['evidence_pointer'], 'device evidence pointer')
        require(bool(re.fullmatch(r'/result/devices/\d+' if row['kind'] == 'physical' else
                                 r'/devices/com\.apple\.CoreSimulator\.SimRuntime\.iOS-[0-9-]+/\d+', pointer)),
                'Invalid device receipt pointer')
        if row['kind'] == 'simulator':
            require(row.get('model_source_field') == 'deviceTypeIdentifier', 'Simulator model source changed')
            simulator_metadata_gaps(row)
    require(len({row['evidence_pointer'] for row in devices.values()}) == len(devices), 'Duplicate device receipt pointer')
    coverage = unique(matrix['coverage'], 'id', 'coverage')
    require(set(coverage) == set(COVERAGE), 'Missing required physical/floor coverage')
    for name, row in coverage.items():
        require(set(row) == {'id', 'required_kind', 'platform', 'form_factor', 'supported_floor', 'device_aliases',
                             'availability', 'native_qualified', 'reason', 'next_action', 'evidence_refs',
                             'checked_at', 'evidence_age_basis', 'intended_roles', 'intended_journeys',
                             'intended_scope_status'}, 'Invalid coverage shape')
        require(timestamp(row['checked_at'], 'coverage timestamp') == observed_at, 'Coverage evidence age lost')
        require(row['evidence_refs'] == (['E-ADB-DEVICES'] if row['platform'] == 'android' else
                ['E-IOS-DEVICES', 'E-SIMULATORS']), 'Coverage discovery binding changed')
        text(row['evidence_age_basis'], 'coverage age limitation')
        require(row['intended_scope_status'] == 'proposed_unverified', 'Proposed scope relabeled observed')
        require(row['intended_roles'] == ['current_library_listing', 'current_story_recommendation', 'current_story_detail_hero']
                and row['intended_journeys'] == ['list-scroll-reversal-v1', 'detail-back-overlap-v1'],
                'Coverage role/journey scope expanded')
        require((row['platform'], row['form_factor'], row['supported_floor']) == COVERAGE[name] and
                row['required_kind'] == 'physical', 'Coverage category/floor changed')
        require(row['availability'] in AVAILABILITY, 'Invalid coverage availability')
        require(row['native_qualified'] is False and row['availability'] != 'observed', 'False physical readiness')
        require(isinstance(row['device_aliases'], list) and len(row['device_aliases']) == len(set(row['device_aliases'])),
                'Invalid coverage aliases')
        for alias in row['device_aliases']:
            require(alias in devices, 'Unbound coverage alias')
            device = devices[alias]
            require(device['kind'] == 'physical' and device['platform'] == row['platform'] and
                    row['form_factor'] in {'any', device['form_factor']}, 'Virtual/incompatible physical coverage')
        text(row['reason'], 'coverage reason')
        text(row['next_action'], 'coverage next action')
    counters = unique(matrix['counters'], 'id', 'counters')
    require(set(counters) == set(COUNTERS), 'Counter coverage changed')
    for name, row in counters.items():
        require(set(row) == {'id', 'platform', 'unit', 'tool', 'observation', 'sampling_support', 'overhead', 'scope_description'},
                'Invalid counter shape')
        text(row['scope_description'], 'counter scope gap')
        platform, unit, tool, ref = COUNTERS[name]
        require((row['platform'], row['unit'], row['tool']) == (platform, unit, tool), 'Invalid counter unit/tool/scope')
        observation(row['observation'], f'counter {name}', sources, ref)
        value = row['observation']['value']
        if value is not None:
            numeric(value, f'counter {name}')
        require(value is None, 'Native counters were not measured in preparation')
        require(row['sampling_support'] == 'unverified' and row['overhead'] == 'unmeasured', 'Invented counter capability')
    build = matrix['experiment_build']
    require(set(build) == {'status', 'target', 'artifact_sha256', 'engine_runtime', 'renderer', 'reason'}, 'Invalid build shape')
    require(build['status'] == 'not_implemented' and all(build[key] is None for key in
            ('target', 'artifact_sha256', 'engine_runtime', 'renderer')), 'Invented native experiment build')
    text(build['reason'], 'build gap')
    gates = unique(matrix['gates'], 'id', 'gates')
    require(set(gates) == GATES, 'Missing native gates')
    for row in gates.values():
        require(set(row) == {'id', 'status', 'acceptance_ref', 'implementation_ref', 'verification_ref', 'reason', 'next_action'},
                'Invalid gate shape')
        require(row['status'] == 'open' and all(row[key] is None for key in
                ('acceptance_ref', 'implementation_ref', 'verification_ref')), 'Native gate closed by preparation')
        text(row['reason'], 'gate cause')
        text(row['next_action'], 'gate next action')
    runtimes = unique(matrix['virtual_runtime_summary'], 'runtime', 'virtual runtimes')
    for row in runtimes.values():
        require(set(row) == {'runtime', 'registered_count', 'available_count', 'booted_count', 'evidence_ref'},
                'Invalid runtime shape')
        require(row['evidence_ref'] == 'E-SIMULATORS', 'Runtime provenance lost')
        for key in ('registered_count', 'available_count', 'booted_count'):
            numeric(row[key], 'runtime count', integer=True)
        require(row['booted_count'] <= row['available_count'] <= row['registered_count'], 'Impossible runtime counts')
    physical = [row for row in devices.values() if row['kind'] == 'physical']
    simulators = [row for row in devices.values() if row['kind'] == 'simulator']
    expected_summary = {
        'adb_connected_devices': 0, 'registered_physical_ios_devices': len(physical),
        'connected_physical_ios_devices': sum(row['connection']['value'] == 'connected' for row in physical),
        'registered_available_simulators': sum(row['available_count'] for row in runtimes.values()),
        'booted_simulators': sum(row['booted_count'] for row in runtimes.values()),
        'qualified_physical_devices': 0,
    }
    require(set(matrix['summary']) == set(expected_summary), 'Summary schema changed')
    for key, value in matrix['summary'].items():
        numeric(value, 'summary count', integer=True)
        require(value == expected_summary[key], 'Summary not derived from evidence')
    require(len(simulators) == expected_summary['registered_available_simulators'], 'Simulator observation coverage missing')
    for row in matrix['historical_evidence']:
        require(set(row) == {'source', 'availability', 'claims'}, 'Invalid historical shape')
        require(row['source'] in REQUIRED_SOURCES and row['availability'] == 'historical_only', 'Historical evidence relabeled')
        text(row['claims'], 'historical boundary')
    require(bool(matrix['historical_evidence']) and isinstance(matrix['limitations'], list) and len(matrix['limitations']) >= 6,
            'Historical/discovery limitations missing')
    for limitation in matrix['limitations']:
        text(limitation, 'discovery limitation')
    sources_and_guards(matrix)
    links = public_documents(matrix_path)
    if private_root is not None:
        replay_private(matrix, private_root)
    return {'preparation_checks': 'PASS', 'preparation_complete': matrix['preparation_complete'],
            'native_execution_ready': False, 'evidence_mode': 'private_receipt_replay' if private_root else 'public_only',
            'private_receipts_rehashed': len(sources) if private_root else 0,
            'physical_devices_qualified': 0, 'coverage_rows': len(coverage), 'counter_gaps': len(counters),
            'open_native_gates': sorted(gates), 'source_hashes_checked': len(matrix['source_snapshot']),
            'local_links_checked': links,
            'limitation': None if private_root else 'Private receipt bytes/observation replay were not checked.'}


def pointer(document, path):
    current = document
    for part in path.strip('/').split('/'):
        part = part.replace('~1', '/').replace('~0', '~')
        current = current[int(part)] if isinstance(current, list) else current[part]
    return current


def replay_private(matrix, private_root):
    """Rehash fixed receipt filenames, then independently reproject the public observations."""
    private_root = private_root.resolve()
    require(private_root.is_dir() and private_root != ROOT and ROOT not in private_root.parents,
            'Private receipt root must be outside repository')
    receipts = {}
    for source in matrix['evidence_sources']:
        path = private_root / source['receipt_file']
        require(path.is_file() and not path.is_symlink() and path.resolve().parent == private_root, 'Unsafe/missing private receipt')
        data = path.read_bytes()
        require(sha256(data).hexdigest() == source['receipt_sha256'], 'Private receipt hash mismatch')
        receipts[source['id']] = parse_json(data)
    replay_observations(matrix, receipts)


def replay_observations(matrix, receipts):
    # Receipt projection is independent of the matrix's declared evidence pointer mappings.
    physical_pointers = {f'/result/devices/{index}' for index, row in
                         enumerate(receipts['E-IOS-DEVICES']['result']['devices'])
                         if row['hardwareProperties']['platform'] == 'iOS' and
                         row['hardwareProperties'].get('reality') == 'physical'}
    simulator_pointers = {f'/devices/{runtime}/{index}' for runtime, rows in
                          receipts['E-SIMULATORS']['devices'].items() for index, row in enumerate(rows)
                          if row['isAvailable'] is True}
    require({row['evidence_pointer'] for row in matrix['devices'] if row['kind'] == 'physical'} == physical_pointers and
            {row['evidence_pointer'] for row in matrix['devices'] if row['kind'] == 'simulator'} == simulator_pointers,
            'Private device observation inventory mismatch')
    for device in matrix['devices']:
        raw = pointer(receipts['E-IOS-DEVICES' if device['kind'] == 'physical' else 'E-SIMULATORS'], device['evidence_pointer'])
        require(device['form_factor'] == receipt_form_factor(raw, device['kind']),
                'Private device form-factor replay mismatch')
        if device['kind'] == 'physical':
            expected = {
                'model': raw['hardwareProperties']['marketingName'],
                'product_type': raw['hardwareProperties']['productType'],
                'os': raw['deviceProperties']['osVersionNumber'],
                'os_build': raw['deviceProperties']['osBuildUpdate'],
                'connection': raw['connectionProperties']['tunnelState'],
                'pairing': raw['connectionProperties']['pairingState'],
                'ddi_services_available': raw['deviceProperties']['ddiServicesAvailable'],
            }
        else:
            simulator_metadata_gaps(device)
            expected = {'model': raw['deviceTypeIdentifier'].split('.')[-1].replace('-', ' '),
                        'os': device['evidence_pointer'].split('/')[2].split('iOS-')[1].replace('-', '.'),
                        'connection': raw['state']}
            require(raw['isAvailable'] is True, 'Unavailable simulator presented as available')
        for field, value in expected.items():
            require(device[field]['value'] == value, 'Private device observation replay mismatch')
    raw_runtime = receipts['E-SIMULATORS']['devices']
    expected_runtime = {key: (len(rows), sum(row['isAvailable'] for row in rows),
                             sum(row['state'] == 'Booted' for row in rows)) for key, rows in raw_runtime.items()}
    require({row['runtime']: (row['registered_count'], row['available_count'], row['booted_count'])
             for row in matrix['virtual_runtime_summary']} == expected_runtime, 'Private simulator summary mismatch')
    replay_tools_and_probes(matrix, receipts)


def replay_tools_and_probes(matrix, receipts):
    """Command receipts are data; their recorded commands are never executed."""
    def output(name):
        row = receipts[name]
        require(row.get('exit_code') == 0, 'Successful discovery receipt failed')
        return row['output' if name == 'E-ADB-DEVICES' else 'stdout'].strip()
    tools = {row['id']: row for row in matrix['tools']}
    adb = output('E-ADB-VERSION').splitlines()
    require(tools['adb']['identity']['value'] == adb[0].replace('Bridge version ', 'Bridge ') +
            ' / platform-tools ' + adb[1].removeprefix('Version '),
            'Private ADB identity mismatch')
    require(tools['xcode']['identity']['value'] == output('E-XCODE').replace('\nBuild version ', ' / build '),
            'Private Xcode identity mismatch')
    require(tools['xcrun']['identity']['value'] == output('E-XCRUN').removeprefix('xcrun version ').removesuffix('.'),
            'Private xcrun identity mismatch')
    require(tools['xctrace']['identity']['value'] == output('E-XCTRACE').removeprefix('xctrace version ')
            .replace(' (', ' / ').removesuffix(')'), 'Private xctrace identity mismatch')
    dart = output('E-DART-DIRECT')
    require(tools['dart-direct']['identity']['value'] == re.sub(r'^Dart SDK version: ([^ ]+) \(([^)]+)\).* on "([^"]+)"$',
            r'\1 \2 \3', dart), 'Private Dart identity mismatch')
    sdk = receipts['E-FLUTTER-CACHE']['value']
    tool = tools['flutter-cached-sdk']
    require(tool['identity']['value'] == sdk['flutterVersion'] + ' ' + sdk['channel'] and
            tool['framework_revision'] == sdk['frameworkRevision'] and tool['engine_revision'] == sdk['engineRevision'] and
            tool['dart_sdk_version'] == sdk['dartSdkVersion'] and tool['devtools_version'] == sdk['devToolsVersion'],
            'Private cached SDK identity mismatch')
    devices = output('E-ADB-DEVICES').splitlines()
    require(devices.count('List of devices attached') == 1, 'Missing/ambiguous ADB device-list header')
    start = devices.index('List of devices attached') + 1
    require(len([line for line in devices[start:] if line.strip()]) ==
            matrix['summary']['adb_connected_devices'], 'Private ADB device count mismatch')
    source_by_file = {row['receipt_file']: row['id'] for row in matrix['evidence_sources']}
    for probe in matrix['probes']:
        raw = receipts[source_by_file[probe['id'] + '.json']]
        # The retained public discovery durations were rounded to milliseconds; not workload timings.
        require(probe['exit_code'] == raw['exit_code'] and probe['observed_at'] == raw['observed_at'] and
                probe['duration_seconds'] == round(raw['duration_seconds'], 3), 'Private probe replay mismatch')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--matrix', type=Path, default=HERE / 'p0c-device-matrix.json')
    parser.add_argument('--private-evidence-root', type=Path,
                        help='Optional external receipt directory; never printed or queried')
    args = parser.parse_args()
    try:
        result = verify(args.matrix, args.private_evidence_root)
    except (ValueError, KeyError, TypeError, IndexError, OverflowError, OSError, subprocess.SubprocessError) as error:
        # Private exception payloads can carry raw keys/paths; print a fixed category in replay mode.
        message = 'Private replay or preparation validation failed.' if args.private_evidence_root else str(error)
        print(json.dumps({'preparation_checks': 'FAIL', 'reason': message}), file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
