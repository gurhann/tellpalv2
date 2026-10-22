#!/usr/bin/env python3
"""P5f için ayrı performans oturumu, ağ engelli Release build ve veri korumalı QA güncellemesi."""
from __future__ import annotations
import argparse
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import time
import uuid

import p5d_offline_app as p5d
from p5f_performance_validation import validate_session

ROOT, MOBILE = p5d.ROOT, p5d.MOBILE
HOST = Path(__file__).with_name('p5e_ios_host')
QA_ROOT = Path('/Volumes/MAC_EK/tellpal-qa-private')
PACKAGE = 'com.tellpal.tellpalMobilePoc.imageexperiment.release'
DEVICE = '00008101-00051D683A08801E'
PINNED_EVIDENCE_ROOTS = ('p5d-offline-63b8351d1cf4', 'p5e-iphone-8db5002f4a8a',
                         'p5e-iphone-993b7e1f5e6c')
NETWORK_POLICY = '(version 1)(allow default)(deny network*)'
ALLOWED = {'flutter', 'flutter_bloc', 'auto_route', 'get_it', 'injectable',
           'freezed_annotation', 'crypto', 'dartz', 'equatable', 'json_annotation'}
require, file_hash, digest, write_new = p5d.require, p5d.file_hash, p5d.digest, p5d.write_new


def source_closure():
    pending, files = [MOBILE / 'lib/app/p5e_offline_qa.dart'], {}
    while pending:
        original = pending.pop()
        require(not original.is_symlink(), 'source_symlink_refused')
        path = original.resolve(strict=True)
        require(path.is_relative_to(MOBILE / 'lib'), 'source_scope_refused')
        name = str(path.relative_to(MOBILE))
        if name in files:
            continue
        data = path.read_bytes()
        files[name] = digest(data)
        for target in re.findall(r"(?:import|export|part)\s+['\"]([^'\"]+)['\"]", data.decode()):
            if target.startswith('dart:'):
                require(target not in ('dart:ffi', 'dart:isolate'), 'native_escape_refused')
            elif target.startswith('package:tellpal_mobile_poc/'):
                pending.append(MOBILE / 'lib' / target.split('/', 1)[1])
            elif target.startswith('package:'):
                require(target.split(':', 1)[1].split('/')[0] in ALLOWED, 'network_or_plugin_dependency_refused')
            else:
                pending.append(path.parent / target)
    return files


def inventory(folder):
    result = {}
    for path in folder.rglob('*'):
        require(not path.is_symlink(), 'backup_symlink_refused')
        if path.is_file():
            result[str(path.relative_to(folder))] = {'bytes': path.stat().st_size, 'sha256': file_hash(path)}
    return result


def space(root, required=6 * 1024 ** 3):
    available = shutil.disk_usage(root).free
    require(available >= required, 'space_insufficient:' + str(available) + ':' + str(required))
    return {'availableBytes': available, 'requiredBytes': required}


def environment(run):
    env = dict(os.environ, TMPDIR=str(run / 'tmp'), XDG_CACHE_HOME=str(run / 'cache'),
               CLANG_MODULE_CACHE_PATH=str(run / 'cache/clang'),
               SWIFT_MODULECACHE_PATH=str(run / 'cache/swift'),
               FLUTTER_SUPPRESS_ANALYTICS='true', DART_SUPPRESS_ANALYTICS='true',
               PUB_CACHE=str((Path.home() / '.pub-cache').resolve()))
    return env


def command(argv, run, label, *, cwd=None, offline=False, timeout=600):
    if offline:
        require(shutil.which('sandbox-exec'), 'offline_guard_missing')
        argv = ['sandbox-exec', '-p', NETWORK_POLICY, *argv]
    output = run / (label + '.private.log')
    with output.open('xb') as log:
        os.chmod(output, 0o600)
        result = subprocess.run([str(v) for v in argv], cwd=cwd, env=environment(run),
                                stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
    require(result.returncode == 0, label + '_failed')
    return output.read_bytes()


def device_json(run, label, args):
    target = run / (label + '.private.json')
    # launch paketinden sonraki değerleri uygulama argümanı sayar; seçenekler önce gelir.
    command(['xcrun', 'devicectl', *args[:3], '--device', DEVICE, '--timeout', '60',
             '--json-output', target, *args[3:]], run, label, timeout=90)
    return json.loads(target.read_bytes())['result']


def validate_device(data):
    properties = data.get('properties', {})
    hardware = properties.get('hardware', data.get('hardwareProperties', {}))
    connection = properties.get('connection', data.get('connectionProperties', {}))
    state = properties.get('state', data.get('deviceProperties', {}))
    require(hardware.get('udid') == DEVICE and hardware.get('marketingName') == 'iPhone 12' and
            hardware.get('reality') == 'physical', 'device_refused')
    require(connection.get('transportType') == 'wired' and
            (connection.get('state') == 'connected' or connection.get('tunnelState') == 'connected'), 'wired_device_required')
    require(state.get('bootState') == 'booted', 'device_not_booted')


def validate_lock(data):
    require(data.get('passcodeRequired') is False, 'device_unlock_required')


def signing(run):
    folder = Path.home() / 'Library/Developer/Xcode/UserData/Provisioning Profiles'
    selected = []
    for path in folder.glob('*.mobileprovision'):
        data = plistlib.loads(subprocess.run(['security', 'cms', '-D', '-i', str(path)],
                                check=True, capture_output=True).stdout)
        if data.get('Entitlements', {}).get('application-identifier', '').endswith('.' + PACKAGE):
            validate_profile(data)
            selected.append((path, data))
    require(len(selected) == 1, 'exact_existing_signing_profile_required')
    path, profile = selected[0]
    identities = subprocess.run(['security', 'find-identity', '-v', '-p', 'codesigning'],
                                check=True, capture_output=True, text=True).stdout
    certs = [hashlib.sha1(cert).hexdigest().upper() for cert in profile['DeveloperCertificates']]
    valid = [value for value in certs if value in identities]
    require(len(valid) == 1, 'existing_signing_identity_required')
    return path, profile, valid[0]


def validate_profile(profile):
    entitlements = profile.get('Entitlements', {})
    require(entitlements.get('application-identifier') == profile['TeamIdentifier'][0] + '.' + PACKAGE,
            'profile_application_refused')
    require(DEVICE in profile.get('ProvisionedDevices', []) and
            profile['ExpirationDate'].replace(tzinfo=timezone.utc) > datetime.now(timezone.utc),
            'profile_device_or_expiry_refused')
    require(entitlements.get('get-task-allow') is True and
            set(entitlements) <= {'application-identifier', 'com.apple.developer.team-identifier',
                                  'get-task-allow', 'keychain-access-groups'}, 'profile_entitlement_refused')
    require(set(entitlements.get('keychain-access-groups', [])) <=
            {profile['TeamIdentifier'][0] + '.*', profile['TeamIdentifier'][0] + '.' + PACKAGE, 'com.apple.token'},
            'profile_keychain_refused')


def prepare(run, cases, sources, report, profile, identity):
    mobile = run / 'host'
    mobile.mkdir(mode=0o700)
    for name, sha in sources.items():
        data = (MOBILE / name).read_bytes()
        require(digest(data) == sha, 'source_changed_during_snapshot')
        target = mobile / name
        target.parent.mkdir(parents=True, exist_ok=True)
        write_new(target, data)
    pubspec = '''name: tellpal_mobile_poc
publish_to: none
environment:
  sdk: ^3.11.0
dependencies:
  flutter:
    sdk: flutter
  flutter_bloc: 9.1.1
  auto_route: 10.3.0
  get_it: 8.3.0
  injectable: 2.7.1+4
  freezed_annotation: 3.1.0
  crypto: 3.0.7
  dartz: 0.10.1
  equatable: 2.1.0
  json_annotation: 4.11.0
flutter:
  uses-material-design: true
  assets:
    - assets/p5d/
'''
    write_new(mobile / 'pubspec.yaml', pubspec.encode())
    write_new(mobile / 'pubspec.lock', (MOBILE / 'pubspec.lock').read_bytes())
    assets = mobile / 'assets/p5d'
    assets.mkdir(parents=True)
    write_new(assets / 'manifest.json', {'schemaVersion': 1, 'reportSha256': p5d.REPORT_SHA, 'cases': cases})
    for row in cases:
        source = report.parent / Path(row['asset']).name
        require(file_hash(source) == row['sha256'], 'fixture_changed_during_snapshot')
        write_new(mobile / row['asset'], source.read_bytes())
    ios = mobile / 'ios'
    shutil.copytree(HOST, ios)
    nonce = uuid.uuid4().hex
    flutter = Path(shutil.which('flutter') or '').resolve().parents[1]
    engine = flutter / 'bin/cache/artifacts/engine/ios-release/Flutter.xcframework/ios-arm64/Flutter.framework'
    require(engine.is_dir(), 'existing_ios_release_cache_required')
    (ios / 'Flutter').mkdir(exist_ok=True)
    os.symlink(engine, ios / 'Flutter/Flutter.framework')
    definitions = ','.join(base64.b64encode(value.encode()).decode() for value in
                          ('TELLPAL_P5E_OFFLINE=true', 'TELLPAL_P5E_RUN=' + nonce, 'TELLPAL_P5F_PERFORMANCE=true'))
    settings = {'FLUTTER_ROOT': flutter, 'FLUTTER_APPLICATION_PATH': mobile,
        'FLUTTER_TARGET': 'lib/app/p5e_offline_qa.dart', 'FLUTTER_BUILD_DIR': 'build',
        'FLUTTER_BUILD_NAME': '1.0.0', 'FLUTTER_BUILD_NUMBER': '1', 'DART_DEFINES': definitions,
        'DART_OBFUSCATION': 'false', 'TRACK_WIDGET_CREATION': 'false', 'TREE_SHAKE_ICONS': 'true',
        'PACKAGE_CONFIG': mobile / '.dart_tool/package_config.json', 'P5E_RUN_NONCE': nonce, 'P5F_ENABLED': 'true',
        'DEVELOPMENT_TEAM': profile['TeamIdentifier'][0], 'CODE_SIGN_IDENTITY': identity,
        'PROVISIONING_PROFILE_SPECIFIER': profile['UUID']}
    write_new(ios / 'P5e.xcconfig', ('\n'.join(str(k) + '=' + str(v) for k, v in settings.items()) + '\n').encode())
    command(['ruby', 'project.rb'], run, 'project', cwd=ios, offline=True)
    return mobile, nonce


def snapshot_binding(mobile, sources, cases):
    names = list(sources) + [row['asset'] for row in cases] + ['assets/p5d/manifest.json', 'pubspec.yaml', 'pubspec.lock']
    names += ['ios/' + str(path.relative_to(HOST)) for path in HOST.rglob('*') if path.is_file()]
    names += ['ios/P5e.xcconfig', 'ios/P5eOffline.xcodeproj/project.pbxproj']
    return {name: file_hash(mobile / name) for name in names}


def validate_bundle(app, profile):
    info = plistlib.loads((app / 'Info.plist').read_bytes())
    require(info.get('CFBundleIdentifier') == PACKAGE and info.get('DTPlatformName') == 'iphoneos', 'bundle_identity_refused')
    embedded = plistlib.loads(subprocess.run(['security', 'cms', '-D', '-i', str(app / 'embedded.mobileprovision')],
                             check=True, capture_output=True).stdout)
    require(embedded['UUID'] == profile['UUID'], 'bundle_profile_refused')
    validate_profile(embedded)
    native = {p.name for p in (app / 'Frameworks').glob('*.framework')}
    require(native == {'App.framework', 'Flutter.framework'}, 'bundle_plugin_refused')
    entitlements = plistlib.loads(subprocess.run(['codesign', '-d', '--entitlements', ':-', str(app)],
                                  check=True, capture_output=True).stdout)
    require(entitlements.get('application-identifier') == profile['TeamIdentifier'][0] + '.' + PACKAGE and
            set(entitlements) <= {'application-identifier', 'com.apple.developer.team-identifier',
                                  'get-task-allow', 'keychain-access-groups'}, 'bundle_entitlement_refused')
    require(set(entitlements.get('keychain-access-groups', [])) <= {profile['TeamIdentifier'][0] + '.' + PACKAGE},
            'binary_keychain_group_refused')
    require(not (app / 'GoogleService-Info.plist').exists(), 'firebase_bundle_refused')
    return {'packageId': PACKAGE, 'frameworks': sorted(native), 'profileUuid': profile['UUID'],
            'executableSha256': file_hash(app / info['CFBundleExecutable'])}


def backup(run):
    common = ['--source', '.', '--domain-type', 'appDataContainer', '--domain-identifier', PACKAGE]
    for label in ('backup-first', 'backup-verification'):
        device_json(run, label, ['device', 'copy', 'from', *common, '--destination', str(run / label)])
    first, second = inventory(run / 'backup-first'), inventory(run / 'backup-verification')
    require(first == second and first, 'qa_backup_verification_refused')
    write_new(run / 'backup-inventory.private.json', first)
    return {'verified': True, 'files': len(first), 'bytes': sum(v['bytes'] for v in first.values()),
            'inventorySha256': digest(json.dumps(first, sort_keys=True).encode()),
            'oldExecutableRestoreGuaranteed': False}


def installation_guard(build_run):
    # Eski ve yarım kalan çağrılar da aynı nonce için kalıcı mutasyon sınırıdır.
    boundary = build_run / 'installation-boundary.private.json'
    require(not boundary.exists() and not boundary.is_symlink(), 'nonce_installation_already_started')
    for previous in [build_run, *build_run.glob('install-attempt-*')]:
        require(previous.is_dir() and not previous.is_symlink(), 'installation_record_refused')
        require(not any((previous / name).exists() or (previous / name).is_symlink() for name in ('install.private.log', 'install.private.json')),
                'nonce_installation_already_started')
        record = previous / 'result.private.json'
        if record.exists():
            require(record.is_file() and not record.is_symlink(), 'installation_record_refused')
            require(json.loads(record.read_bytes()).get('installation') not in ('INSTALLED', 'OUTCOME_UNKNOWN'),
                    'nonce_installation_already_started')


def install_app(run, app, result):
    build_run = run.parent if run.name.startswith('install-attempt-') else run
    installation_guard(build_run)
    # Exclusive kayıt paralel çağrıları da cihaz çağrısından önce durdurur.
    write_new(build_run / 'installation-boundary.private.json', {'installation': 'OUTCOME_UNKNOWN', 'attempt': run.name})
    # Çağrı sınırından sonra transport hatası, cihazda kurulum olmadığını kanıtlamaz.
    result['installation'] = 'OUTCOME_UNKNOWN'
    response = device_json(run, 'install', ['device', 'install', 'app', str(app)])
    installed = response.get('installedApplications')
    require(isinstance(installed, list) and len(installed) == 1 and
            installed[0].get('bundleID') == PACKAGE and
            isinstance(installed[0].get('installationURL'), str) and
            installed[0]['installationURL'].endswith('/P5eOffline.app/'), 'installation_response_refused')
    result['installation'] = 'INSTALLED'


def p5c_report_inventory(report):
    require(file_hash(report) == p5d.REPORT_SHA, 'report_sha_refused')
    sources = json.loads(report.read_bytes())['sources']
    require(len(sources) == 41, 'p5c_41_sources_required')
    result = {str(report): {'sha256': file_hash(report), 'bytes': report.stat().st_size, 'prefixOnly': False}}
    for source in sources:
        files = [(source['sourceFile'], source['sourceSha256'], source['sourceBytes'])]
        files += [(source['candidates'][arm]['file'], source['candidates'][arm]['sha256'], source['candidates'][arm]['bytes']) for arm in ('PNG', 'WEBP')]
        for name, sha, size in files:
            require(Path(name).name == name, 'p5c_path_refused')
            path = report.parent / name
            require(path.is_file() and not path.is_symlink() and path.stat().st_size == size and file_hash(path) == sha,
                    'p5c_full_media_pin_refused')
            require(str(path) not in result, 'p5c_duplicate_path_refused')
            result[str(path)] = {'sha256': sha, 'bytes': size, 'prefixOnly': False}
    require(len(result) == 124, 'p5c_123_media_required')
    return result


def protected_outputs(excluded_caches=None, *, report=None, scope_record=None, progress=None):
    # Yalnız bu deneyin dayandığı P5 kanıtları okunur; eski P0/P4 arşivleri açılmaz.
    ignored = {'build', 'derived-data', 'cache', 'tmp', '.dart_tool', '.gradle', 'emulator', 'sdk'}
    result = {}
    started = time.monotonic()
    last_progress = started
    current_root = ''
    entries = list(QA_ROOT.iterdir())
    relevant = [p for p in entries if re.match(r'^p5[cdef](?:[-_.]|$)', p.name)]
    require(set(PINNED_EVIDENCE_ROOTS) <= {p.name for p in relevant}, 'pinned_evidence_baseline_missing')
    require(all((QA_ROOT / name).is_dir() and not (QA_ROOT / name).is_symlink()
                for name in PINNED_EVIDENCE_ROOTS), 'pinned_evidence_directory_required')
    partial_roots = []
    if scope_record is not None:
        scope_record.update(policy='DECISION_PINNED_P5_BASELINES_PLUS_SMALL_RECORDS',
            protectedFullRoots=[str(QA_ROOT / name) for name in PINNED_EVIDENCE_ROOTS],
            smallRecordRoots=[str(p) for p in relevant if p.name not in PINNED_EVIDENCE_ROOTS],
            smallRecordMaxBytes=32 * 1024 ** 2, excludedRawScopes=partial_roots,
            oldRootWritePolicy='NO_WRITES; all mutation destinations are new run directories',
            excludedHistoricalEntries=[p.name for p in entries if p not in relevant],
            excludedGeneratedDirectoryNames=sorted(ignored))

    def emit_progress(stage):
        nonlocal last_progress
        last_progress = time.monotonic()
        if progress is not None:
            progress({'phase': stage, 'root': current_root, 'protectedFiles': len(result),
                      'protectedBytes': sum(row.get('bytes', 0) for row in result.values()),
                      'elapsedSeconds': round(last_progress - started, 1), 'relatedRoots': len(relevant),
                      'fullEvidenceRoots': len(PINNED_EVIDENCE_ROOTS)})

    def add(path):
        if path.is_symlink():
            target = os.readlink(path)
            result[str(path)] = {'symlinkTarget': target, 'sha256': digest(os.fsencode(target))}
        elif path.is_file():
            result[str(path)] = {'sha256': file_hash(path), 'bytes': path.stat().st_size,
                                 'prefixOnly': path == p5d.ACTIVE_EMULATOR_LOG}
        if time.monotonic() - last_progress >= 10:
            emit_progress('HASHING')

    def scan(root):
        for folder, dirs, names in os.walk(root, followlinks=False):
            generated = []
            for name in dirs:
                candidate = Path(folder) / name
                marker = candidate / 'pyvenv.cfg'
                if name == '.venv' and not candidate.is_symlink() and marker.is_file() and not marker.is_symlink():
                    generated.append(name)
                    if excluded_caches is not None:
                        excluded_caches.append({'path': str(candidate), 'kind': 'generated_python_dependency_cache',
                                                'marker': 'pyvenv.cfg', 'markerSha256': file_hash(marker)})
            dirs[:] = [name for name in dirs if name not in ignored and name not in generated]
            links = [name for name in dirs if (Path(folder) / name).is_symlink()]
            for name in names + links:
                add(Path(folder) / name)

    for root in relevant:
        current_root = root.name
        emit_progress('ROOT_BEGIN')
        if root.is_symlink() or root.is_file():
            if root.suffix.lower() in {'.json', '.jsonl', '.log', '.md', '.txt'} and (
                    root.is_symlink() or root.stat().st_size <= 32 * 1024 ** 2):
                add(root)
            else:
                partial_roots.append({'root': str(root), 'policy': 'excluded_large_or_non_record_file', 'writePolicy': 'NO_WRITES'})
            emit_progress('ROOT_END')
            continue
        if root.name not in PINNED_EVIDENCE_ROOTS:
            record_folders = [root]
            children = list(root.iterdir())
            record_folders += [p for p in children if p.is_dir() and not p.is_symlink() and
                              (p.name.startswith('collection-') or p.name.startswith('install-attempt-'))]
            partial_roots.append({'root': str(root),
                'excludedDirectories': [p.name for p in children if p.is_dir() and p not in record_folders],
                'policy': 'only_small_top_level_collection_install_records; no raw media or partial binary reads',
                'writePolicy': 'NO_WRITES'})
            for records in record_folders:
                for path in records.iterdir():
                    if path.suffix.lower() in {'.json', '.jsonl', '.log', '.md', '.txt'} and (
                            path.is_symlink() or path.is_file() and path.stat().st_size <= 32 * 1024 ** 2):
                        add(path)
            emit_progress('SMALL_RECORDS_END')
            continue
        scan(root)
        # Yalnız kararın kabul edilmiş dayanaklarındaki eski paketler tam SHA ile korunur.
        for relative in ('derived-data/Build/Products', 'host/build/app/outputs', 'host/build/ios/iphoneos'):
            products = root / relative
            if products.is_dir() and not products.is_symlink():
                scan(products)
        emit_progress('ROOT_END')
    if report is not None:
        current_root = 'P5C_123_PINNED_MEDIA'
        emit_progress('ROOT_BEGIN')
        result.update(p5c_report_inventory(report))
        if scope_record is not None:
            scope_record.update(p5cMediaFiles=123, p5cReportSha256=p5d.REPORT_SHA,
                                p5cRoot=str(report.parent))
        emit_progress('ROOT_END')
    return result


def preserved(before, inputs, old):
    # CLI yalnız kendi yeni dizinini yazar; önceki P5c/P5d dosyaları aynen kalır.
    require(p5d.repo_hashes() == before, 'working_tree_changed')
    require(all(file_hash(Path(name)) == sha for name, sha in inputs.items()), 'p5c_source_changed')
    for name, expected in old.items():
        path = Path(name)
        if isinstance(expected, dict) and 'symlinkTarget' in expected:
            require(path.is_symlink() and os.readlink(path) == expected['symlinkTarget'] and
                    digest(os.fsencode(os.readlink(path))) == expected['sha256'], 'old_evidence_symlink_changed')
            continue
        require(path.is_file() and not path.is_symlink(), 'old_evidence_missing')
        if isinstance(expected, str):
            require(file_hash(path) == expected, 'old_evidence_changed')
        elif expected['prefixOnly']:
            with path.open('rb') as stream:
                require(path.stat().st_size >= expected['bytes'] and digest(stream.read(expected['bytes'])) == expected['sha256'], 'old_evidence_prefix_changed')
        else:
            require(path.stat().st_size == expected['bytes'] and file_hash(path) == expected['sha256'], 'old_evidence_changed')
    canonical = lambda value: digest(json.dumps(value, sort_keys=True).encode())
    return {'unchanged': True, 'repositoryFiles': len(before), 'p5cFiles': len(inputs),
            'oldQaFiles': len(old), 'repositorySha256': canonical(before), 'oldQaSha256': canonical(old)}


def run_all(report, install=False):
    require(QA_ROOT.is_dir() and QA_ROOT.resolve() == QA_ROOT, 'private_root_refused')
    available = space(QA_ROOT)
    require(shutil.disk_usage(ROOT).free >= 96 * 1024 ** 2, 'system_disk_margin_insufficient')
    cases, inputs = p5d.fixture_cases(report)
    inputs[str(report)] = p5d.REPORT_SHA
    sources = source_closure()
    excluded_caches = []
    protection_scope = {}
    def progress(row):
        print(json.dumps({'stage': 'PROTECTION_PREFLIGHT', **row}, ensure_ascii=False), flush=True)
    before, old = p5d.repo_hashes(), protected_outputs(excluded_caches, report=report, scope_record=protection_scope, progress=progress)
    run = QA_ROOT / ('p5f-iphone-' + uuid.uuid4().hex[:12])
    run.mkdir(mode=0o700)
    (run / 'tmp').mkdir()
    (run / 'cache').mkdir()
    write_new(run / 'old-output-before.private.json', old)
    write_new(run / 'excluded-caches.private.json', {'policy': 'only_.venv_with_regular_pyvenv.cfg_marker',
                                                   'directories': excluded_caches})
    write_new(run / 'protection-scope.private.json', protection_scope)
    result = {'scope': 'P5F_IPHONE_PERFORMANCE', 'reportPath': str(report), 'status': 'NOT_RUN', 'space': available,
              'excludedGeneratedCaches': len(excluded_caches), 'protectedP5cMediaFiles': 123,
              'protectionPolicy': protection_scope['policy'],
              'installation': 'NOT_RUN', 'humanQualityAcceptance': 'NOT_RUN',
              'physicalP3Acceptance': 'NOT_RUN', 'productionProfileDecision': 'NOT_RUN',
              'oldExecutableRestoreGuaranteed': False, 'deviceDiskBeforeInstall': 'NOT_MEASURED'}
    try:
        validate_device(device_json(run, 'device', ['device', 'info', 'details']))
        path, profile, identity = signing(run)
        result['signingProfileSha256'] = file_hash(path)
        mobile, nonce = prepare(run, cases, sources, report, profile, identity)
        result['runNonce'] = nonce
        write_new(run / 'source-before.private.json', {'repository': before, 'inputs': inputs, 'closure': sources})
        command(['flutter', '--no-version-check', '--suppress-analytics', 'pub', 'get', '--offline'],
                run, 'pub-offline', cwd=mobile, offline=True)
        plugin_file = mobile / '.flutter-plugins-dependencies'
        require(not plugin_file.exists() or not any(json.loads(plugin_file.read_bytes()).get('plugins', {}).values()),
                'native_plugin_refused')
        frozen_snapshot = snapshot_binding(mobile, sources, cases)
        write_new(run / 'snapshot-binding.private.json', frozen_snapshot)
        command(['xcodebuild', '-quiet', '-project', 'P5eOffline.xcodeproj', '-scheme', 'P5eOffline',
                 '-configuration', 'Release', '-sdk', 'iphoneos', '-destination', 'generic/platform=iOS',
                 '-derivedDataPath', str(run / 'derived-data'), '-disableAutomaticPackageResolution',
                 '-skipPackageUpdates', '-jobs', '2', 'COMPILER_INDEX_STORE_ENABLE=NO',
                 'CLANG_MODULE_CACHE_PATH=' + str(run / 'cache/clang'),
                 'SWIFT_MODULE_CACHE_PATH=' + str(run / 'cache/swift'), 'build'],
                run, 'xcode-build', cwd=mobile / 'ios', offline=True)
        app = run / 'derived-data/Build/Products/Release-iphoneos/P5eOffline.app'
        command(['codesign', '--verify', '--deep', '--strict', app], run, 'signature-verify', offline=True)
        require(snapshot_binding(mobile, sources, cases) == frozen_snapshot, 'snapshot_changed_during_build')
        result['bundleAudit'] = validate_bundle(app, profile)
        result['bundleInventory'] = inventory(app)
        result['bundlePath'] = str(app)
        result['bundleBytes'] = sum(p.stat().st_size for p in app.rglob('*') if p.is_file())
        result['status'] = 'BUILD_VERIFIED'
        if install:
            validate_device(device_json(run, 'device-before-install', ['device', 'info', 'details']))
            require(snapshot_binding(mobile, sources, cases) == frozen_snapshot, 'snapshot_changed_before_install')
            validate_lock(device_json(run, 'lock-before-backup', ['device', 'info', 'lockState']))
            result['backup'] = backup(run)
            result['preservationBeforeInstall'] = preserved(before, inputs, old)
            validate_lock(device_json(run, 'lock-before-install', ['device', 'info', 'lockState']))
            install_app(run, app, result)
            device_json(run, 'launch', ['device', 'process', 'launch', PACKAGE])
            result['status'] = 'AWAITING_OFFLINE_PERFORMANCE'
    except Exception as error:
        result['status'] = 'FAILED'
        result['reason'] = type(error).__name__ + ':' + str(error)
    finally:
        try:
            result['preservation'] = preserved(before, inputs, old)
        except Exception as error:
            result['preservation'] = {'unchanged': False, 'reason': str(error)}
            result['status'] = 'PRESERVATION_REFUSED'
        write_new(run / 'result.private.json', result)
    return run, result


def validate_run_path(run):
    require(run.parent == QA_ROOT and re.fullmatch(r'p5f-iphone-[0-9a-f]{12}', run.name) and
            run.resolve() == run and not run.is_symlink(), 'run_path_refused')


def install_verified(run):
    validate_run_path(run)
    installation_guard(run)
    built = json.loads((run / 'result.private.json').read_bytes())
    require(built.get('status') == 'BUILD_VERIFIED' and built.get('installation') == 'NOT_RUN', 'verified_build_required')
    attempt = run / ('install-attempt-' + uuid.uuid4().hex[:8])
    attempt.mkdir(mode=0o700)
    (attempt / 'tmp').mkdir()
    (attempt / 'cache').mkdir()
    result = {'status': 'INSTALL_FAILED', 'installation': 'NOT_RUN', 'buildRun': str(run),
              'oldExecutableRestoreGuaranteed': False, 'deviceDiskBeforeInstall': 'NOT_MEASURED'}
    before = json.loads((run / 'source-before.private.json').read_bytes())
    old = json.loads((run / 'old-output-before.private.json').read_bytes())
    try:
        result['preservationBeforeInstall'] = preserved(before['repository'], before['inputs'], old)
        app = run / 'derived-data/Build/Products/Release-iphoneos/P5eOffline.app'
        require(str(app) == built['bundlePath'] and inventory(app) == built['bundleInventory'], 'verified_bundle_changed')
        _, profile, _ = signing(attempt)
        require(validate_bundle(app, profile) == built['bundleAudit'], 'bundle_signing_changed')
        frozen = json.loads((run / 'snapshot-binding.private.json').read_bytes())
        require(all(file_hash(run / 'host' / name) == sha for name, sha in frozen.items()), 'snapshot_changed_before_install')
        space(QA_ROOT, built['bundleBytes'] * 3 + 512 * 1024 ** 2)
        validate_device(device_json(attempt, 'device', ['device', 'info', 'details']))
        validate_lock(device_json(attempt, 'lock-before-backup', ['device', 'info', 'lockState']))
        command(['codesign', '--verify', '--deep', '--strict', app], attempt, 'signature-verify', offline=True)
        result['backup'] = backup(attempt)
        result['preservationBeforeMutation'] = preserved(before['repository'], before['inputs'], old)
        validate_lock(device_json(attempt, 'lock-before-install', ['device', 'info', 'lockState']))
        install_app(attempt, app, result)
        device_json(attempt, 'launch', ['device', 'process', 'launch', PACKAGE])
        result['status'] = 'AWAITING_OFFLINE_PERFORMANCE'
    except Exception as error:
        result['reason'] = type(error).__name__ + ':' + str(error)
    finally:
        try:
            result['preservation'] = preserved(before['repository'], before['inputs'], old)
        except Exception as error:
            result['status'] = 'PRESERVATION_REFUSED'
            result['preservation'] = {'unchanged': False, 'reason': str(error)}
        write_new(attempt / 'result.private.json', result)
    return attempt, result


def collect(run):
    validate_run_path(run)
    result = json.loads((run / 'result.private.json').read_bytes())
    nonce = result['runNonce']
    target = run / ('collection-' + uuid.uuid4().hex[:8])
    device_json(run, 'collect-' + uuid.uuid4().hex[:8], ['device', 'copy', 'from',
        '--source', 'Documents/p5f-' + nonce, '--domain-type', 'appDataContainer',
        '--domain-identifier', PACKAGE, '--destination', str(target)])
    data = (target / 'session.jsonl').read_bytes()
    pins = p5d.fixture_cases(Path(result['reportPath']))[0]
    summary = validate_session(data, nonce, pins)
    summary['sessionSha256'] = file_hash(target / 'session.jsonl')
    before = json.loads((run / 'source-before.private.json').read_bytes())
    old = json.loads((run / 'old-output-before.private.json').read_bytes())
    summary['preservation'] = preserved(before['repository'], before['inputs'], old)
    write_new(target / 'summary.private.json', summary)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--install', action='store_true')
    parser.add_argument('--collect', type=Path)
    parser.add_argument('--install-run', type=Path)
    args = parser.parse_args()
    if args.install_run:
        attempt, result = install_verified(args.install_run)
        print(json.dumps({'privateAttempt': str(attempt), 'status': result['status'],
            'reason': result.get('reason'), 'installation': result['installation'],
            'oldExecutableRestoreGuaranteed': False}, ensure_ascii=False))
        return 0 if result['status'] == 'AWAITING_OFFLINE_PERFORMANCE' else 1
    if args.collect:
        print(json.dumps(collect(args.collect), ensure_ascii=False))
        return 0
    require(args.report is not None, 'report_required')
    run, result = run_all(args.report, args.install)
    print(json.dumps({'privateRun': str(run), 'status': result['status'],
        'reason': result.get('reason'), 'installation': result['installation'],
        'oldExecutableRestoreGuaranteed': False, 'preservation': result.get('preservation', {}).get('unchanged')}, ensure_ascii=False))
    expected = 'AWAITING_OFFLINE_PERFORMANCE' if args.install else 'BUILD_VERIFIED'
    return 0 if result['status'] == expected else 1


if __name__ == '__main__':
    raise SystemExit(main())
