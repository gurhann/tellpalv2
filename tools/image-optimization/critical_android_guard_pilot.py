#!/usr/bin/env python3
"""Android-only critical guard adapter; original iOS guard contracts stay unchanged.

The original Android release launcher, prerequisite admission, exact installed
APK audit, TLS/integrity checks and teardown own every operation. Mutations are
limited to already admitted observer returns, matching the iOS pilot faults.
"""
from contextlib import contextmanager
from hashlib import sha256
from pathlib import Path
import re
import time

import native_entry_driver as host
import critical_ios_guard_pilot as ios
from native_artwork_comparison import LedgerChunks, validate_comparison_audit, require_fresh_android_process
from native_entry_config import EntryError, HASH, exact_keys, frozen_json, private_file, require, strict_json, write_private

SCOPE = 'image-optimization-critical-android-guards-r1'
EXPECTED_STAGES = {'alternate-trust': 'NATIVE_PROBE', 'expired-envelope': 'NATIVE_ENVELOPE_TIME_TOKEN',
    'wrong-uid-envelope': 'NATIVE_ENVELOPE_FIELDS', 'corrupt-bytes': 'NATIVE_SETTLEMENT'}


class AndroidGuardPairing(host.LauncherPairing):
    def __init__(self, broker, platform='android'):
        require(platform == 'android', 'android_guard_selection_rejected')
        super().__init__(broker, platform)
        self.stage = None
        self.chunks = LedgerChunks()
        source = host.read_source(host.MOBILE / 'android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentRefusalDiagnostics.kt')
        self.allowed_stages = {item.encode('ascii') for item in re.findall(r'\("([A-Z_]{1,64})"\)', source.decode('utf-8'))}
        require(all(stage.encode('ascii') in self.allowed_stages for stage in EXPECTED_STAGES.values()),
            'android_guard_stage_schema_rejected')

    def __call__(self, line):
        prefix = b'INSTRUMENTATION_RESULT: diagnosticStage='
        if line.startswith(prefix):
            require(self.stage is None and line[len(prefix):] in self.allowed_stages, 'android_guard_stage_rejected')
            self.stage = line[len(prefix):].decode('ascii')
            return
        self.chunks.feed(line)
        super().__call__(line)


@contextmanager
def observed_android_driver(state, fault, alternate):
    require(ios.OBSERVER_LOCK.acquire(blocking=False), 'android_guard_overlap')
    originals = host.Broker, host.LauncherPairing, host.create_server
    def broker_factory(*args, **kwargs):
        require('broker' not in state, 'android_guard_dispatch_repeated')
        broker = ios.ObservedBroker(*args, fault=fault, alternate_der=alternate, **kwargs)
        state['broker'] = broker
        return broker
    def pairing_factory(*args, **kwargs):
        require('pairing' not in state, 'android_guard_dispatch_repeated')
        pairing = AndroidGuardPairing(*args, **kwargs)
        state['pairing'] = pairing
        return pairing
    def server_factory(*args, **kwargs):
        require('server' not in state, 'android_guard_dispatch_repeated')
        server = originals[2](*args, **kwargs)
        state['server'] = server
        return server
    try:
        host.Broker, host.LauncherPairing, host.create_server = broker_factory, pairing_factory, server_factory
        yield
    finally:
        host.Broker, host.LauncherPairing, host.create_server = originals
        ios.OBSERVER_LOCK.release()


def validate_android_observation(receipt):
    require(type(receipt) is dict and type(receipt.get('schemaVersion')) is int and receipt['schemaVersion'] == 1 and
        receipt.get('scope') == SCOPE and receipt.get('platform') == 'android' and receipt.get('mode') == 'release' and
        receipt.get('fault') in ios.FAULTS and receipt.get('sourceUnchanged') is True and receipt.get('ownedCleanup') is True,
        'android_guard_observation_unqualified')
    fault = receipt['fault']
    for key in ('sourceSha256', 'artifactSha256', 'testArtifactSha256', 'installationReceiptSha256',
        'prerequisiteReceiptSha256', 'sessionSha256', 'caseSha256', 'manifestSha256', 'publicManifestSha256'):
        require(type(receipt.get(key)) is str and HASH.fullmatch(receipt[key]), 'android_guard_identity_rejected')
    result, counts = receipt['launcher'], receipt['boundaryCounts']
    exact_keys(counts, ('brokerProbe', 'backendProbe', 'pairing', 'claim', 'fixtureDispatch', 'fixtureFlushed',
        'originalDeliveryGuardRefused'))
    require(all(type(value) is int and 0 <= value <= 4 for value in counts.values()) and
        type(receipt['nativeResultCount']) is int and receipt['nativeResultCount'] == 1 and
        type(receipt['pairingReportCount']) is int and receipt['pairingReportCount'] in (0, 1) and
        type(result.get('exitCode')) is int and result['exitCode'] == 0 and result.get('status') == 'ok',
        'android_guard_native_result_unverified')
    # `am instrument` exits zero for a native refused result: exact Android result markers are required.
    if fault == 'normal-teardown':
        require(receipt['nativeResult'] == 'settled' and receipt['nativeReason'] == 'NATIVE_LAUNCHER_SETTLED' and
            receipt['nativeStage'] is None and receipt['originalDriverReason'] is None and
            type(receipt.get('originalEntryOutcome')) is dict and
            receipt['originalEntryOutcome'].get('teardown') == 'authenticated_app_revoke_observed' and
            type(receipt['originalEntryOutcome'].get('fixtureDeliveryCount')) is int and
            receipt['originalEntryOutcome']['fixtureDeliveryCount'] == 1 and
            receipt['originalEntryOutcome'].get('mode') == 'release' and receipt['originalEntryOutcome'].get('platform') == 'android' and
            type(receipt['originalEntryOutcome'].get('generation')) is int and
            type(receipt.get('generation')) is int and receipt['generation'] >= 0 and
            all(receipt['originalEntryOutcome'].get(key) == receipt[key] for key in
                ('generation', 'sessionSha256', 'caseSha256', 'artifactSha256', 'testArtifactSha256')) and
            type(receipt['originalEntryOutcome'].get('fixtureBodyBytes')) is int and
            type(receipt.get('fixtureByteSize')) is int and receipt['fixtureByteSize'] > 0 and
            receipt['originalEntryOutcome']['fixtureBodyBytes'] == receipt['fixtureByteSize'] and
            receipt['originalEntryOutcome'].get('fixtureOutputSha256') == receipt['fixtureOutputSha256'],
            'android_guard_teardown_unverified')
    else:
        # A pin refusal occurs before a pairing report. The unchanged positive
        # driver therefore refuses unsettled pairing before checking its native
        # outcome; later envelope/integrity refusals have already paired.
        expected_driver_reason = 'launcher_pairing_unsettled' if fault == 'alternate-trust' else 'native_entry_refused'
        require(receipt['nativeResult'] == 'refused' and receipt['nativeReason'] == 'NATIVE_LAUNCHER_REFUSED' and
            receipt['nativeStage'] == EXPECTED_STAGES[fault] and receipt['originalDriverReason'] == expected_driver_reason,
            'android_guard_refusal_unverified')
    if fault == 'alternate-trust':
        require(all(value == 0 for value in counts.values()) and receipt['pairingReportCount'] == 0 and
            receipt['admissionSuccessful'] is False, 'android_guard_boundary_crossed')
        return
    require(all(counts[key] == 1 for key in ('brokerProbe', 'backendProbe', 'pairing', 'claim')) and
        receipt['pairingReportCount'] == 1 and receipt['admissionSuccessful'] is True, 'android_guard_admission_unverified')
    if fault in ('expired-envelope', 'wrong-uid-envelope'):
        require(all(counts[key] == 0 for key in ('fixtureDispatch', 'fixtureFlushed', 'originalDeliveryGuardRefused')),
            'android_guard_boundary_crossed')
    elif fault == 'corrupt-bytes':
        mutation = receipt['mutation']
        require(counts['fixtureDispatch'] > 0 and counts['fixtureDispatch'] == counts['fixtureFlushed'] ==
            counts['originalDeliveryGuardRefused'] and mutation.get('changedByteCount') == 1 and
            type(mutation.get('changedByteCount')) is int and mutation.get('wireBytes') == receipt['fixtureByteSize'] and
            type(mutation.get('wireSha256')) is str and HASH.fullmatch(mutation['wireSha256']) and
            mutation['wireSha256'] != receipt['fixtureOutputSha256'], 'android_guard_corruption_unverified')
    else:
        require(counts['fixtureDispatch'] == counts['fixtureFlushed'] == 1 and counts['originalDeliveryGuardRefused'] == 0,
            'android_guard_delivery_unverified')


def observe_android(config, manifest, output, deadline, *, fault, case_id, build_path, prerequisite_path,
                    audit_path, alternate_path=None, prior_ledger_path=None):
    """One fresh Android invocation, including the original >195s reservation guard.

    Output is an already admitted unique private directory. Every create/install
    and reservation-root cancellation remains owned by the external operator.
    """
    require(config.values['platform'] == 'android' and config.values['mode'] == 'release' and
        fault in ios.FAULTS and case_id in manifest.objects, 'android_guard_selection_rejected')
    build, proof = host.validate_physical_prerequisites(config, build_path, prerequisite_path)
    audit_hash = validate_comparison_audit(audit_path, build_path, build, proof, 'android')
    require((alternate_path is not None) == (fault == 'alternate-trust'), 'fault_rejected')
    alternate = ios.alternate_certificate(alternate_path, config) if alternate_path else None
    prior_hash = sha256(private_file(prior_ledger_path, ios.MAX_LEDGER_BYTES)).hexdigest() if prior_ledger_path else None
    original_source = host.source_snapshot()[1]
    tool_hashes = {name: sha256((Path(__file__).parent / name).read_bytes()).hexdigest() for name in
        ('critical_android_guard_pilot.py', 'critical_ios_guard_pilot.py', 'native_artwork_comparison.py')}
    write_private(Path(output) / 'guard-tool-freeze.json', {'sourceSha256': original_source, 'toolSha256': tool_hashes})
    require_fresh_android_process(config, output, deadline)
    state, outcome, original_failure = {}, None, None
    with observed_android_driver(state, fault, alternate):
        try:
            outcome = host.verify_entry(config, manifest, output, deadline, case_id=case_id,
                build_path=build_path, prerequisite_path=prerequisite_path)
        except EntryError as error: original_failure = str(error)
    require(all(key in state for key in ('broker', 'pairing', 'server')), original_failure or 'android_guard_dispatch_unavailable')
    broker, pairing, server = (state[key] for key in ('broker', 'pairing', 'server'))
    path = Path(output) / 'launcher.json'
    launcher = strict_json(private_file(path)) if path.exists() else {'status': 'failed', 'exitCode': None}
    entry = next(item for item in manifest.entries if item['caseId'] == case_id)
    with broker.lock, server.connection_lock:
        receipt = {'schemaVersion': 1, 'scope': SCOPE, 'fault': fault, 'platform': 'android', 'mode': 'release',
            'sourceSha256': original_source, 'artifactSha256': build['artifactSha256'], 'testArtifactSha256': build['testArtifactSha256'],
            'installationReceiptSha256': audit_hash, 'prerequisiteReceiptSha256': sha256(private_file(prerequisite_path)).hexdigest(),
            'observedAt': int(time.time() * 1000), 'originalDriverReason': original_failure,
            'caseSha256': sha256(case_id.encode()).hexdigest(), 'sessionSha256': sha256(broker.session_id.encode()).hexdigest(),
            'generation': broker.generation, 'manifestSha256': manifest.private_sha256, 'publicManifestSha256': manifest.public_sha256,
            'fixtureOutputSha256': entry['outputSha256'], 'fixtureByteSize': entry['byteSize'], 'nativeResult': pairing.native_result,
            'nativeResultCount': pairing.native_result_count, 'nativeReason': pairing.native_reason.decode('ascii') if pairing.native_reason else None,
            'nativeStage': pairing.stage, 'pairingReportCount': pairing.count, 'admissionSuccessful': broker.admission_successful,
            'boundaryCounts': dict(broker.counts), 'mutation': dict(broker.mutation), 'providerBinding': dict(broker.identity),
            'launcher': launcher, 'originalEntryOutcome': outcome, 'priorLedgerSha256': prior_hash,
            'sourceUnchanged': host.source_snapshot()[1] == original_source and all(sha256((Path(__file__).parent / name).read_bytes()).hexdigest() ==
                digest for name, digest in tool_hashes.items()),
            'ownedCleanup': server.socket.fileno() == -1 and not server.workers and not server.connections and not broker.active and
                broker.token is None and not broker.timer.is_alive() and (pairing.worker is None or not pairing.worker.is_alive()),
            'status': 'unqualified', 'reason': 'android_guard_observation_unqualified',
            'quality': 'human_verdict_pending', 'latency': 'not_qualified', 'memory': 'not_qualified'}
    # Retain a full sanitized ledger even if later qualification fails; no run-as/debug flag is used.
    if pairing.chunks.count is not None:
        try: write_private(Path(output) / 'device-ledger.json', pairing.chunks.finish())
        except EntryError: pass
    try:
        validate_android_observation(receipt)
        if fault in ('corrupt-bytes', 'normal-teardown'):
            data = pairing.chunks.finish()
            qualified = ios.validate_ledger(data, receipt)
            receipt['ledgerQualification'] = qualified
        receipt['status'], receipt['reason'] = 'observed', 'native_guard_and_required_ownership_observed'
    except EntryError as error: receipt['reason'] = str(error)
    write_private(Path(output) / 'observation-receipt.json', receipt)
    return receipt


def main(argv=None):
    from native_entry_config import INPUT_KEYS, load_config, load_manifest, private_output
    from native_artwork_comparison import backend_inputs
    parser = host.SafeParser(description='Critical release Android guard observer')
    commands = parser.add_subparsers(dest='mode', required=True)
    execute = commands.add_parser('run')
    for name in ('config', 'case-id', 'build-receipt', 'prerequisite-receipt', 'audit-receipt'):
        execute.add_argument('--' + name, required=True)
    execute.add_argument('--fault', choices=ios.FAULTS, required=True)
    execute.add_argument('--alternate-certificate')
    execute.add_argument('--prior-ledger')
    args = parser.parse_args(argv)
    output = None
    try:
        config, manifest = load_config(args.config), None
        require(config.values['platform'] == 'android' and config.values['mode'] == 'release', 'android_guard_selection_rejected')
        manifest = load_manifest(config)
        files = [args.config, args.build_receipt, args.prerequisite_receipt, args.audit_receipt,
            *[config.values[key] for key in INPUT_KEYS], *backend_inputs(config), *[item['filePath'] for item in strict_json(
                private_file(config.values['manifestPath'], 100 * 1024 * 1024))['entries']]]
        files += [value for value in (args.alternate_certificate, args.prior_ledger) if value is not None]
        output = private_output(config.values['privateOutputRoot'], files, create=True)
        result = observe_android(config, manifest, output, time.monotonic() + host.TOTAL_SECONDS,
            fault=args.fault, case_id=args.case_id, build_path=args.build_receipt,
            prerequisite_path=args.prerequisite_receipt, audit_path=args.audit_receipt,
            alternate_path=args.alternate_certificate, prior_ledger_path=args.prior_ledger)
        projection = {'scope': SCOPE, 'status': result['status'], 'reason': result['reason'],
            'receiptSha256': sha256(frozen_json(result)).hexdigest()}
        print(frozen_json(projection).decode('ascii'))
        return 0 if result['status'] == 'observed' else 4
    except EntryError as error:
        result = {'schemaVersion': 1, 'scope': SCOPE, 'status': 'unqualified', 'reason': str(error)}
    except (OSError, ValueError, KeyError, TypeError):
        result = {'schemaVersion': 1, 'scope': SCOPE, 'status': 'unqualified', 'reason': 'android_guard_operation_failed'}
    if output is not None: write_private(Path(output) / 'observation-failure.json', result)
    print(frozen_json(result).decode('ascii'))
    return 4


if __name__ == '__main__':
    import sys
    sys.exit(main())
