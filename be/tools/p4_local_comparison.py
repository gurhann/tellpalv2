#!/usr/bin/env python3
"""Onaylı P4a2 için tek kullanımlık yerel klon/fixture çalıştırıcısı."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import stat
import subprocess
import sys
import time
from urllib.parse import urlsplit, urlunsplit

from p3_local_cms import load_environment, private_create

BE = Path(__file__).resolve().parents[1]
ROOT = BE.parent
PLAN = BE / 'docs/evidence/p4-reading-images/bounded-run-proposal-2026-10-17.json'
PLAN_SHA = 'f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3'
RUN_ID = 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09'
CLONE = 'tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09'
MAIN = 'com.tellpal.v2.support.P4LocalComparisonSetup'
DIRECTORY = BE / 'var' / RUN_ID


def require(condition, code):
    if not condition:
        raise ValueError(code)


def checked_plan(path=PLAN):
    data = path.read_bytes()
    require(hashlib.sha256(data).hexdigest() == PLAN_SHA, 'PLAN_HASH_CHANGED')
    plan = json.loads(data)
    require(plan['runId'] == RUN_ID and plan['database']['name'] == CLONE, 'PLAN_SCOPE_CHANGED')
    return plan


def private_directory(path):
    path.mkdir(mode=0o700, parents=True, exist_ok=True)
    info = path.lstat()
    require(path.resolve() == path and stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid()
            and stat.S_IMODE(info.st_mode) == 0o700, 'PRIVATE_DIRECTORY_REQUIRED')


def private_read(path, maximum):
    path = path.absolute()
    info = path.lstat()
    require(path.resolve() == path and stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid()
            and stat.S_IMODE(info.st_mode) == 0o600 and 0 < info.st_size <= maximum, 'PRIVATE_INPUT_REQUIRED')
    descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(descriptor, 'rb') as stream:
        return stream.read(maximum + 1)


def strict_json(data):
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, 'DUPLICATE_JSON_KEY')
            result[key] = value
        return result
    return json.loads(data, object_pairs_hook=pairs,
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError('NONFINITE_JSON')))


def credential_gate(directory, now=None):
    now = int(time.time() * 1000) if now is None else now
    attempt = strict_json(private_read(directory / 'qa-sign-in.attempt.json', 4096))
    ready_bytes = private_read(directory / 'qa-sign-in.ready.json', 8192)
    ready = strict_json(ready_bytes)
    common = {'runId': RUN_ID, 'planSha256': PLAN_SHA, 'qaUid': 'jLRypTmoBrhhX21HsDJojurcKTC3'}
    require(set(attempt) == set(common) | {'reservedQaSignInInvocations', 'reservedFirebaseVerifierInvocations'}
            and all(attempt.get(key) == value for key, value in common.items())
            and type(attempt['reservedQaSignInInvocations']) is int and attempt['reservedQaSignInInvocations'] == 1
            and type(attempt['reservedFirebaseVerifierInvocations']) is int and attempt['reservedFirebaseVerifierInvocations'] == 1,
            'ONE_SHOT_SIGN_IN_RESERVATION_REQUIRED')
    require(set(ready) == set(common) | {'tokenFile', 'expiresAtEpochMillis', 'verifiedAtEpochMillis'}
            and all(ready.get(key) == value for key, value in common.items()), 'QA_CREDENTIAL_SCOPE')
    require(type(ready['expiresAtEpochMillis']) is int and type(ready['verifiedAtEpochMillis']) is int
            and 0 < ready['verifiedAtEpochMillis'] <= now and ready['expiresAtEpochMillis'] > now + 900000,
            'QA_CREDENTIAL_EXPIRED')
    token_path = Path(ready['tokenFile'])
    require(token_path.is_absolute() and not token_path.resolve().is_relative_to(ROOT), 'TOKEN_OUTSIDE_REPOSITORY_REQUIRED')
    token = private_read(token_path, 8192).decode('ascii').strip()
    parts = token.split('.')
    require(len(parts) == 3 and all(re.fullmatch('[A-Za-z0-9_-]+', part) for part in parts), 'QA_TOKEN_FORMAT')
    claims = strict_json(base64.urlsafe_b64decode(parts[1] + '=' * (-len(parts[1]) % 4)))
    require(claims.get('sub') == common['qaUid'] and claims.get('user_id', common['qaUid']) == common['qaUid']
            and claims.get('aud') == 'tellpal-ee0dd' and claims.get('iss') == 'https://securetoken.google.com/tellpal-ee0dd'
            and type(claims.get('exp')) is int and type(claims.get('iat')) is int
            and 0 < claims['iat'] * 1000 <= now + 60000 and 0 < claims['exp'] - claims['iat'] <= 3600
            and claims['exp'] * 1000 == ready['expiresAtEpochMillis'], 'QA_TOKEN_CLAIMS')
    return hashlib.sha256(ready_bytes).hexdigest()


def source_files(plan, paths):
    require(len(paths) == 3, 'EXACT_THREE_SOURCES_REQUIRED')
    result = []
    for expected, path in zip(plan['sources'], paths):
        absolute = path.absolute()
        require(absolute.resolve() == absolute and absolute.is_file(), 'SOURCE_PATH_INVALID')
        require(absolute.stat().st_size == expected['bytes'], 'SOURCE_BYTES_CHANGED')
        digest = hashlib.sha256()
        with absolute.open('rb') as stream:
            for block in iter(lambda: stream.read(65536), b''):
                digest.update(block)
        require(digest.hexdigest() == expected['sha256'], 'SOURCE_HASH_CHANGED')
        result.append(str(absolute))
    return result


def database_urls(environment):
    source = environment.get('TELLPAL_DB_URL', 'jdbc:postgresql://localhost:5432/tellpal_v2')
    parsed = urlsplit(source.removeprefix('jdbc:'))
    require(parsed.hostname in ('localhost', '127.0.0.1', '::1') and not parsed.query
            and not parsed.fragment and not parsed.username and not parsed.password
            and re.fullmatch('/[A-Za-z0-9_]+', parsed.path), 'LOCAL_SOURCE_DB_REQUIRED')
    require(parsed.path != '/' + CLONE, 'SOURCE_MUST_DIFFER_FROM_CLONE')
    clone = 'jdbc:' + urlunsplit((parsed.scheme, parsed.netloc, '/' + CLONE, '', ''))
    return source, clone


def scoped_environment(environment, clone):
    result = environment.copy()
    require(not any(name.startswith(('TELLPAL_READING_COMPARISON_', 'TELLPAL_SECURITY_FIREBASE_'))
                    for name in result), 'AMBIGUOUS_CONFIGURATION')
    result.update({'TELLPAL_DB_URL': clone, 'TELLPAL_ASSET_PROCESSING_POLLER_ENABLED': 'false',
                   'TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX': 'local/' + RUN_ID,
                   'SPRING_PROFILES_ACTIVE': 'local'})
    return result


def sdk_configuration(path):
    require(path is not None, "EXPLICIT_SDK_CONFIG_REQUIRED")
    path = path.absolute()
    info = path.lstat()
    require(path.resolve() == path and stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid()
            and stat.S_IMODE(info.st_mode) == 0o600 and info.st_size <= 8192, "PRIVATE_SDK_CONFIG_REQUIRED")
    config = strict_json(private_read(path, 8192))
    require(set(config) == {"projectId", "bucket", "credentialsPath"}, "SDK_CONFIG_KEYS")
    require(config["projectId"] == "tellpal-ee0dd" and config["bucket"] == "tellpal-ee0dd.appspot.com", "SDK_PROJECT_BUCKET_CHANGED")
    credential = Path(config["credentialsPath"])
    require(credential.is_absolute() and credential.resolve() == credential and credential.is_file(), "SDK_CREDENTIAL_REQUIRED")
    return config


def overlay(plan, sdk=None):
    values = {'tellpal.asset.storage.firebase.path-prefix': plan['storagePrefix'],
              'tellpal.asset.processing.poller.enabled': False,
              'tellpal.asset.reading.test-engine-enabled': True,
              'tellpal.content.reading-publication.enabled': True,
              'tellpal.security.firebase.stub-tokens-enabled': False,
              'tellpal.security.firebase.check-revoked': True,
              'tellpal.security.admin.bootstrap.username': '',
              'tellpal.security.admin.bootstrap.password': '',
              'spring.flyway.target': 37,
              'server.address': '127.0.0.1', 'server.port': 0}
    if sdk is not None:
        values.update({'tellpal.asset.storage.firebase.project-id': sdk['projectId'],
                       'tellpal.asset.storage.firebase.bucket-name': sdk['bucket'],
                       'tellpal.asset.storage.firebase.credentials-path': sdk['credentialsPath'],
                       'tellpal.security.firebase.project-id': sdk['projectId'],
                       'tellpal.security.firebase.credentials-path': sdk['credentialsPath']})
    for role in ('READING_COVER', 'READING_DETAIL', 'READING_PAGE'):
        for name, value in plan['preparation'].items():
            key = re.sub(r'([A-Z])', r'-\1', name).lower().replace('mi-b', 'mi-b')
            values[f'tellpal.content.reading-publication.profiles.{role}.{key}'] = value
    def encoded(value):
        raw = str(value).lower() if isinstance(value, bool) else str(value)
        return ''.join(char if 32 <= ord(char) < 127 and char not in '\\' else ('\\\\' if char == '\\' else f'\\u{ord(char):04x}') for char in raw)
    return '\n'.join(f'{key}={encoded(value)}'
                     for key, value in values.items()) + '\n'


def pg_environment(environment, read_only):
    result = {name: value for name, value in environment.items()
              if name in ('PATH', 'HOME', 'TMPDIR', 'LANG', 'LC_ALL')}
    result.update({'PGPASSWORD': environment.get('TELLPAL_DB_PASSWORD', 'tellpal'),
                   'PGUSER': environment.get('TELLPAL_DB_USERNAME', 'tellpal'),
                   'PGCONNECT_TIMEOUT': '5',
                   'PGOPTIONS': '-c default_transaction_read_only=on' if read_only else ''})
    return result


def pg_command(name, environment=None):
    selected = os.environ if environment is None else environment
    found = shutil.which(name, path=selected.get('PATH'))
    require(found is not None, 'POSTGRES_TOOLS_REQUIRED')
    return found


def pg_query(environment, source, query):
    result = subprocess.run([pg_command('psql', environment), '-X', '-w', '-At', '-v', 'ON_ERROR_STOP=1',
                             '-d', source.removeprefix('jdbc:'), '-c', query],
                            env=pg_environment(environment, True), capture_output=True, timeout=15)
    require(result.returncode == 0, 'SOURCE_READONLY_QUERY_FAILED')
    return result.stdout.decode().strip()


class PostgresToolchainRefused(ValueError):
    """Araç veya sunucu sürümü uygun değilse yalnız sabit hata kodu taşır."""


def validate_pg_toolchain(environment, source):
    """Operator PATH'indeki dört araç, salt okunur kaynak sunucunun major'ıyla eşleşir."""
    try:
        raw_server = pg_query(environment, source, 'SHOW server_version_num')
        require(re.fullmatch(r'[1-9][0-9]{5,6}', raw_server), 'POSTGRES_VERSION_INVALID')
        major = int(raw_server) // 10000
        require(major >= 10, 'POSTGRES_VERSION_INVALID')
        for name in ('psql', 'pg_dump', 'pg_restore', 'createdb'):
            result = subprocess.run([pg_command(name, environment), '--version'],
                                    env=pg_environment(environment, True), capture_output=True, timeout=5)
            require(result.returncode == 0, 'POSTGRES_VERSION_INVALID')
            version = result.stdout.decode('ascii').strip()
            match = re.fullmatch(re.escape(name) + r' \(PostgreSQL\) ([1-9][0-9]*)\.[0-9]+(?: \([^\r\n]+\))?', version)
            require(match is not None and int(match[1]) == major, 'POSTGRES_VERSION_INVALID')
        return major
    except (OSError, ValueError, subprocess.SubprocessError):
        raise PostgresToolchainRefused('POSTGRES_TOOLCHAIN_VERSION_REFUSED') from None


def source_identity(environment, source):
    query = "select current_database() || ':' || inet_server_port() || ':' || current_user || ':' || current_setting('transaction_read_only')"
    identity = pg_query(environment, source, query)
    require(identity.endswith(':on'), 'SOURCE_READONLY_REQUIRED')
    version = pg_query(environment, source, "select max(version::integer) from flyway_schema_history where success")
    require(version == '36', 'SOURCE_V36_REQUIRED')
    exists = pg_query(environment, source, "select count(*) from pg_database where datname='" + CLONE + "'")
    require(exists == '0', 'CLONE_ALREADY_EXISTS_NO_RETRY')
    return identity


def clone_database(environment, source):
    """Kaynak yalnız read-only pg_dump ile açılır; restore yalnız sabit yeni klona gider."""
    dump = DIRECTORY / 'source.dump'
    with dump.open('xb') as output:
        os.chmod(dump, 0o600)
        result = subprocess.run([pg_command('pg_dump', environment), '-w', '-Fc', '--no-owner', '--no-privileges',
                                 '-d', source.removeprefix('jdbc:')],
                                env=pg_environment(environment, True), stdout=output, stderr=subprocess.PIPE)
    require(result.returncode == 0, 'SOURCE_DUMP_FAILED_NO_RETRY')
    target = urlsplit(source.removeprefix('jdbc:'))
    args = ['-h', target.hostname, '-p', str(target.port or 5432), '-w']
    result = subprocess.run([pg_command('createdb', environment), *args, '-T', 'template0', CLONE],
                            env=pg_environment(environment, False), capture_output=True)
    require(result.returncode == 0, 'CLONE_CREATE_FAILED_NO_RETRY')
    result = subprocess.run([pg_command('pg_restore', environment), *args, '--no-owner', '--no-privileges',
                             '--exit-on-error', '-d', CLONE, str(dump)],
                            env=pg_environment(environment, False), capture_output=True)
    require(result.returncode == 0, 'CLONE_RESTORE_FAILED_NO_RETRY')


def java_command(java):
    executable = java or shutil.which('java')
    require(executable, 'JAVA_REQUIRED')
    dependency = BE / 'target/p4-local-comparison.classpath'
    require(dependency.is_file(), 'PREPARE_REQUIRED')
    classpath = os.pathsep.join((str(BE / 'target/test-classes'), str(BE / 'target/classes'), dependency.read_text().strip()))
    return [executable, '-Xmx1024m', '-XX:MaxDirectMemorySize=128m', '-Djava.awt.headless=true',
            '-cp', classpath, MAIN]


def launch(args, plan, environment):
    source, clone = database_urls(environment)
    validate_pg_toolchain(environment, source)
    files = source_files(plan, args.source)
    sdk = sdk_configuration(args.sdk_config)
    identity = source_identity(environment, source)
    require(args.approval_sha256 == PLAN_SHA, 'EXACT_APPROVAL_HASH_REQUIRED')
    private_directory(DIRECTORY)
    config = DIRECTORY / 'application-p4.properties'
    require(not (DIRECTORY / 'attempt.json').exists(), 'EXISTING_ATTEMPT_NO_RETRY')
    command = java_command(args.java)
    if args.preflight:
        print('PREFLIGHT_PASS_SOURCE_V36_READONLY_HASHES_APPROVAL_NO_CLOUD')
        return 0
    credential_sha = credential_gate(DIRECTORY)
    private_create(DIRECTORY / 'attempt.json', json.dumps({'planSha256': PLAN_SHA,
                   'sourceIdentity': identity, 'state': 'STARTED_NO_RETRY_NO_CLEANUP'}) + '\n')
    private_create(config, overlay(plan, sdk))
    runtime = {'plan': str(PLAN), 'planSha256': PLAN_SHA, 'sources': files,
               'sourceUrl': source, 'cloneUrl': clone, 'sourceIdentity': identity, 'sdk': sdk, 'credentialReceiptSha256': credential_sha}
    private_create(DIRECTORY / 'inputs.json', json.dumps(runtime) + '\n')
    clone_database(environment, source)
    scoped = scoped_environment(environment, clone)
    log = DIRECTORY / 'backend.log'
    with log.open('xb') as output:
        os.chmod(log, 0o600)
        process = subprocess.Popen(command + ['--execute', str(DIRECTORY)], cwd=BE, env=scoped,
                                   stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    private_create(DIRECTORY / 'backend.pid', str(process.pid) + '\n')
    print('STARTED_NO_RETRY PID', process.pid, 'PRIVATE_RECEIPT', DIRECTORY)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument('--prepare', action='store_true')
    modes.add_argument('--preflight', action='store_true')
    modes.add_argument('--execute', action='store_true')
    parser.add_argument('--approval-sha256')
    parser.add_argument('--env-file', type=Path, default=BE / '.env')
    parser.add_argument('--source', type=Path, action='append', default=[])
    parser.add_argument('--java')
    parser.add_argument('--sdk-config', type=Path)
    args = parser.parse_args(argv)
    if not (args.prepare or args.preflight or args.execute):
        print('DRY_RUN_ZERO_FILE_CREDENTIAL_DB_NETWORK_IO')
        print('P4A2_FIXED_PLAN_SINGLE_CLONE_SINGLE_FIXTURE_NO_RETRY_NO_CLEANUP')
        return 0
    try:
        plan = checked_plan()
        if args.prepare:
            result = subprocess.run(['bash', str(BE / 'mvnw'), '-o', '-q', '-DskipTests', 'test-compile',
                                     'dependency:build-classpath', '-Dmdep.includeScope=test',
                                     '-Dmdep.outputFile=target/p4-local-comparison.classpath'], cwd=BE)
            return 0 if result.returncode == 0 else 2
        environment = load_environment(args.env_file)
        return launch(args, plan, environment)
    except PostgresToolchainRefused:
        print('POSTGRES_TOOLCHAIN_VERSION_REFUSED')
        return 2
    except (OSError, ValueError, KeyError, subprocess.SubprocessError):
        print('REFUSED_LOCAL_PLAN_CONFIGURATION_OR_EXISTING_ATTEMPT_NO_RETRY')
        return 2


if __name__ == '__main__':
    sys.exit(main())
