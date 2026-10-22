"""P5f denge, mühür, sayaç ve veri koruma retleri."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import shutil
import subprocess
import unittest
from unittest.mock import patch
from contextlib import ExitStack
from types import SimpleNamespace
import p5f_iphone_performance as host
import p5f_performance_validation as validation

NONCE = 'a' * 32
PINS = [dict(caseId=f'{asset}-{arm}') for asset in (1950, 1952, 8303, 8306) for arm in ('original', 'PNG', 'WEBP')]


def fixture():
    rows = [dict(event='start', schemaVersion=1, scope='P5F_IPHONE_PERFORMANCE'),
            dict(event='begin', network='unsatisfied', thermal='nominal', lowPower=False)]
    for trial in validation.plan():
        epoch = trial['ordinal'] + 1
        rows.append(dict(event='trial', **trial, epoch=epoch, case=PINS[trial['index']], nativeNetwork='unsatisfied', cleanup=True,
            readMicros=100, decodeMicros=1000, firstDrawMicros=1000, decodeDrawMicros=2000,
            rasterMicros=500, buildMicros=100, trialMicros=10000, timingFrameNumber=epoch,
            proof=dict(epoch=epoch, index=trial['index'], crop=False, frameNumber=epoch,
                       logicalWidth=400, logicalHeight=600, dpr=3, fit='contain', filterQuality='low'),
            memory=dict(counter='ios_phys_footprint', valid=True, thermal='nominal', lowPower=False,
                        sampleIntervalMicros=10000, baselineBytes=100000000, loadedBytes=120000000,
                        afterReleaseBytes=100000000, sampledMaxBytes=120000000, samples=3,
                        maxGapMicros=4000, sampleGapsMicros=[4000, 4000], sampledDurationMicros=8000)))
    rows.append(dict(event='seal', completed=144, warmups=12, cleanup=True, sessionMicros=2000000))
    return rows


def encode(rows):
    data = b''
    for sequence, original in enumerate(rows, 1):
        row = dict(original, runNonce=NONCE, sessionId='session', eventSequence=sequence)
        if row['event'] == 'seal':
            row['prefixSha256'] = hashlib.sha256(data).hexdigest()
        data += json.dumps(row, sort_keys=True).encode() + b'\n'
    return data


class P5fPerformanceTest(unittest.TestCase):
    def test_balanced_plan(self):
        rows = validation.plan()
        self.assertEqual(len(rows), 156)
        self.assertEqual(sum(r['warmup'] for r in rows), 12)
        for index in range(12):
            self.assertEqual(sum(r['index'] == index and not r['warmup'] for r in rows), 12)
        self.assertEqual([r['index'] // 3 for r in rows[12:24:3]], [0, 1, 2, 3])
        self.assertEqual([r['index'] // 3 for r in rows[24:36:3]], [3, 2, 1, 0])
        self.assertEqual([r['index'] % 3 for r in rows[12:15]], list(validation.ORDERS[0]))

    def test_complete_session(self):
        result = validation.validate_session(encode(fixture()), NONCE, PINS)
        self.assertEqual(result['status'], 'SEALED_VERIFIED')
        self.assertEqual(result['measuredTrials'], 144)
        self.assertEqual(result['summary']['cases']['8303-WEBP']['samples'], 12)

    def test_missing_duplicate_seal_and_abort_refused(self):
        for change in ('missing', 'duplicate', 'unsealed', 'abort', 'session', 'network', 'geometry'):
            rows = fixture()
            if change == 'missing': rows.pop(10)
            elif change == 'duplicate': rows[10] = copy.deepcopy(rows[9])
            elif change == 'unsealed': rows.pop()
            elif change == 'abort': rows[-1]['event'] = 'abort'
            elif change == 'session': rows[-1]['sessionMicros'] = 600000000
            elif change == 'network': rows[5]['nativeNetwork'] = 'satisfied'
            elif change == 'geometry': rows[15]['proof']['logicalWidth'] = 300
            with self.subTest(change=change), self.assertRaises(ValueError):
                validation.validate_session(encode(rows), NONCE, PINS)

    def test_frames_counters_deadlines_and_missing_values_refused(self):
        mutations = [lambda r: r.update(timingFrameNumber=999), lambda r: r.update(epoch=999),
            lambda r: r['proof'].update(index=999), lambda r: r.update(trialMicros=10000000),
            lambda r: r.update(decodeMicros=None), lambda r: r.update(rasterMicros=float('nan')),
            lambda r: r['memory'].update(valid=False), lambda r: r['memory'].update(baselineBytes=None),
            lambda r: r['memory'].update(samples=0), lambda r: r['memory'].update(maxGapMicros=0),
            lambda r: r['memory'].update(sampledMaxBytes=512*1024**2+1),
            lambda r: r['memory'].update(lowPower=True), lambda r: r['memory'].update(thermal='fair')]
        for mutate in mutations:
            rows = fixture(); mutate(rows[2])
            with self.subTest(mutate=mutate), self.assertRaises(ValueError):
                validation.validate_session(encode(rows), NONCE, PINS)

    def test_tampered_seal_nonce_and_truncated_stream_refused(self):
        data = encode(fixture())
        for bad, nonce in ((data.replace(b'"readMicros": 100', b'"readMicros": 101', 1), NONCE), (data, 'b'*32), (data[:-1], NONCE)):
            with self.assertRaises(ValueError): validation.validate_session(bad, nonce, PINS)

    def test_nested_durations_cannot_exceed_trial_and_equality_is_allowed(self):
        for key in ('rasterMicros', 'buildMicros', 'sampledDurationMicros'):
            for delta in (0, 1):
                rows = fixture()
                row = rows[2]
                target = row['memory'] if key == 'sampledDurationMicros' else row
                target[key] = row['trialMicros'] + delta
                with self.subTest(key=key, delta=delta):
                    if delta:
                        with self.assertRaises(ValueError): validation.validate_session(encode(rows), NONCE, PINS)
                    else:
                        self.assertEqual(validation.validate_session(encode(rows), NONCE, PINS)['status'], 'SEALED_VERIFIED')

    def test_each_pofuduk_decode_and_decode_draw_budget_below_equal_above(self):
        for asset in ('8303', '8306'):
            for metric, threshold in (('decodeMicros', 1200), ('decodeDrawMicros', 2400)):
                for delta in (-1, 0, 1):
                    rows = fixture()[14:-1]
                    for row in rows:
                        if row['case']['caseId'] == asset + '-WEBP': row[metric] = threshold + delta
                    result = validation.summarize(rows, PINS)['pofudukLocalPilot'][asset]
                    with self.subTest(asset=asset, metric=metric, delta=delta):
                        self.assertEqual(result['decodeDrawWithin20Percent'], delta <= 0)

    def test_each_pofuduk_memory_budget_both_branches_below_equal_above(self):
        for asset in ('8303', '8306'):
            for original in (40 * 1024 ** 2, 120 * 1024 ** 2):
                threshold = max(8 * 1024 ** 2, original * .1)
                for delta in (-1, 0, 1):
                    rows = fixture()[14:-1]
                    for row in rows:
                        if row['case']['caseId'] == asset + '-original': row['memory']['sampledMaxBytes'] = original
                        if row['case']['caseId'] == asset + '-WEBP': row['memory']['sampledMaxBytes'] = original + threshold + delta
                    result = validation.summarize(rows, PINS)['pofudukLocalPilot'][asset]
                    with self.subTest(asset=asset, original=original, delta=delta):
                        self.assertEqual(result['memoryDeltaBytes'], threshold + delta)
                        self.assertEqual(result['memoryWithinPilotBudget'], delta <= 0)

    def test_paired_round_deltas_match_original_in_same_round(self):
        rows = fixture()[14:-1]
        for row in rows:
            row['decodeMicros'] = 1000 + row['round'] * 10 + (0, 40, 70)[row['index'] % 3]
            row['decodeDrawMicros'] = row['decodeMicros'] + row['firstDrawMicros']
        result = validation.summarize(rows, PINS)
        for asset in ('1950', '1952', '8303', '8306'):
            for arm, delta in (('PNG', 40), ('WEBP', 70)):
                pairs = result['pairedRoundDeltas'][asset][arm]
                self.assertEqual(len(pairs), 12)
                self.assertEqual([p['round'] for p in pairs], list(range(12)))
                self.assertTrue(all(p['decodeMicros'] == delta and p['decodeDrawMicros'] == delta for p in pairs))

    def test_vsync_first_draw_is_reported_but_does_not_add_pilot_gate(self):
        rows = fixture()[14:-1]
        for row in rows:
            if row['index'] % 3 == 2:
                row['firstDrawMicros'] = 1300
                row['decodeMicros'] = 700
                row['decodeDrawMicros'] = 2000
        summary = validation.summarize(rows, PINS)
        self.assertEqual(summary['cases']['8303-WEBP']['metrics']['firstDrawMicros']['median'], 1300)
        self.assertTrue(summary['pofudukLocalPilot']['8303']['decodeDrawWithin20Percent'])
        self.assertTrue(summary['pofudukLocalPilot']['8306']['decodeDrawWithin20Percent'])

    @unittest.skipUnless(shutil.which('swiftc'), 'Mevcut Swift SDK yok')
    def test_real_native_sampler_stops_on_changed_conditions_and_refuses_seal(self):
        fixtures = Path(__file__).parent/'p5f_native_test'
        with tempfile.TemporaryDirectory(prefix='p5f-native-performance-') as folder:
            root = Path(folder)
            commands = []
            for module in ('UIKit','Flutter'):
                commands.append(['swiftc','-module-cache-path',str(root/'cache'),'-emit-library','-emit-module',
                    '-module-name',module,str(fixtures/(module+'.swift')),'-o',str(root/('lib'+module+'.dylib')),
                    '-emit-module-path',str(root/(module+'.swiftmodule'))])
            commands.append(['swiftc','-module-cache-path',str(root/'cache'),'-I',str(root),'-L',str(root),
                '-lUIKit','-lFlutter','-Xlinker','-rpath','-Xlinker',str(root),
                str(host.HOST/'P5fPerformance.swift'),str(host.HOST/'P5fLifetime.swift'),str(host.HOST/'P5fIdleLease.swift'),
                str(fixtures/'main.swift'),'-o',str(root/'performance-test')])
            for command in commands:
                result=subprocess.run(command,capture_output=True,text=True,timeout=60)
                self.assertEqual(result.returncode,0,result.stderr)
            documents=root/'documents';documents.mkdir()
            result=subprocess.run([str(root/'performance-test'),str(documents)],capture_output=True,text=True,timeout=15)
            self.assertEqual(result.returncode,0,result.stderr)
            self.assertIn('native-performance-pass',result.stdout)

    @unittest.skipUnless(shutil.which('swiftc'), 'Mevcut Swift SDK yok')
    def test_native_lifetime_allows_offline_preparation_and_rejects_started_interruptions(self):
        with tempfile.TemporaryDirectory(prefix='p5f-native-lifetime-') as folder:
            root = Path(folder)
            main = root / 'main.swift'
            main.write_text('''import Foundation
var lifetime = P5fLifetime()
// İlk NWPath satisfied ve uçak modu için Ayarlar geçişi ölçümü başlatmaz.
precondition(!lifetime.invalidate("network_interrupted"))
precondition(!lifetime.invalidate("background_interrupted"))
precondition(lifetime.invalidReason == nil && !lifetime.begun)
try lifetime.begin(permitted: true, nominal: true, lowPower: false)
precondition(lifetime.begun)
precondition(lifetime.invalidate("network_interrupted"))
precondition(lifetime.invalidate("background_interrupted"))
precondition(lifetime.invalidReason == "network_interrupted")
do { try lifetime.seal(); fatalError("interrupted sealed") } catch {}
for condition in [(false, true, false), (true, false, false), (true, true, true)] {
    var blocked = P5fLifetime()
    do { try blocked.begin(permitted: condition.0, nominal: condition.1, lowPower: condition.2); fatalError("bad begin") } catch {}
    precondition(!blocked.begun)
}
var complete = P5fLifetime()
try complete.begin(permitted: true, nominal: true, lowPower: false)
try complete.seal()
precondition(!complete.invalidate("background_interrupted"))
precondition(complete.sealed && complete.invalidReason == nil)
// Her terminal yolunda eski false/true değeri geri gelir; tekrar release etkisizdir.
for prior in [false, true] {
    for terminal in ["seal", "abort", "invalid", "error"] {
        var idleValue = prior
        var writes = 0
        let lease = P5fIdleLease(read: { idleValue }, write: { idleValue = $0; writes += 1 })
        try lease.acquire()
        precondition(idleValue && writes == 1)
        do { try lease.acquire(); fatalError("double acquire") } catch {}
        if terminal == "invalid" {
            var running = P5fLifetime()
            try running.begin(permitted: true, nominal: true, lowPower: false)
            if running.invalidate("background") { lease.release() }
        } else { lease.release() }
        precondition(idleValue == prior && writes == 2)
        lease.release()
        precondition(idleValue == prior && writes == 2)
    }
}
print("native-lifetime-pass")
''')
            binary = root / 'lifetime-test'
            result = subprocess.run(['swiftc', '-module-cache-path', str(root/'cache'),
                str(host.HOST/'P5fLifetime.swift'), str(host.HOST/'P5fIdleLease.swift'), str(main), '-o', str(binary)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            result = subprocess.run([str(binary)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('native-lifetime-pass', result.stdout)

    def test_backup_requires_two_equal_nonempty_copies(self):
        with tempfile.TemporaryDirectory() as folder:
            run = Path(folder)
            for name, data in (('backup-first', b'one'), ('backup-verification', b'two')):
                (run/name).mkdir(); (run/name/'file').write_bytes(data)
            with patch.object(host, 'device_json'), self.assertRaisesRegex(ValueError, 'qa_backup'):
                host.backup(run)

    def test_old_evidence_same_size_change_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'evidence'; path.write_bytes(b'first')
            old = {str(path): host.file_hash(path)}
            path.write_bytes(b'other')
            with patch.object(host.p5d, 'repo_hashes', return_value={}), self.assertRaisesRegex(ValueError, 'old_evidence'):
                host.preserved({}, {}, old)

    def test_old_symlink_is_hashed_without_following_or_mutating_target(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)/'p5e-related'; root.mkdir(); target = root/'target'; target.write_bytes(b'kept')
            link = root/'old-link'; link.symlink_to('target')
            with patch.object(host, 'QA_ROOT', Path(folder)), patch.object(host, 'PINNED_EVIDENCE_ROOTS', ('p5e-related',)):
                old = host.protected_outputs()
            self.assertEqual(old[str(link)]['symlinkTarget'], 'target')
            with patch.object(host.p5d, 'repo_hashes', return_value={}):
                self.assertTrue(host.preserved({}, {}, old)['unchanged'])
                link.unlink(); link.symlink_to('changed')
                with self.assertRaisesRegex(ValueError, 'symlink_changed'): host.preserved({}, {}, old)
            self.assertEqual(target.read_bytes(), b'kept')

    def test_pinned_baseline_file_and_symlink_are_refused_before_any_hash(self):
        for kind in ('file', 'symlink'):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as folder:
                root = Path(folder); baseline = root/'p5e-required'
                if kind == 'file': baseline.write_bytes(b'cannot replace a baseline')
                else:
                    target = root/'target'; target.mkdir(); baseline.symlink_to(target, target_is_directory=True)
                with patch.object(host,'QA_ROOT',root), patch.object(host,'PINNED_EVIDENCE_ROOTS',('p5e-required',)), patch.object(host,'file_hash') as hashed:
                    with self.assertRaisesRegex(ValueError,'pinned_evidence_directory_required'): host.protected_outputs()
                    hashed.assert_not_called()

    def test_install_boundary_blocks_same_nonce_after_success_or_transport_uncertainty(self):
        for uncertain in (False, True):
            with self.subTest(uncertain=uncertain), tempfile.TemporaryDirectory() as folder:
                root = Path(folder); attempt = root/'install-attempt-first'; attempt.mkdir()
                result = {'installation':'NOT_RUN'}
                response = {'installedApplications':[{'bundleID':host.PACKAGE,'installationURL':'file:///P5eOffline.app/'}]}
                with patch.object(host,'device_json',side_effect=RuntimeError('transport') if uncertain else None,return_value=response) as device:
                    if uncertain:
                        with self.assertRaisesRegex(RuntimeError,'transport'): host.install_app(attempt,root/'app',result)
                    else: host.install_app(attempt,root/'app',result)
                    self.assertEqual(result['installation'],'OUTCOME_UNKNOWN' if uncertain else 'INSTALLED')
                    second = root/'install-attempt-second'; second.mkdir()
                    with self.assertRaisesRegex(ValueError,'nonce_installation_already_started'):
                        host.install_app(second,root/'app',{})
                    device.assert_called_once()

    def test_legacy_installed_unknown_and_unfinished_install_logs_block_retry(self):
        for state in ('INSTALLED','OUTCOME_UNKNOWN','missing_result','conflicting_not_run'):
            with self.subTest(state=state), tempfile.TemporaryDirectory() as folder:
                root=Path(folder).resolve();run=root/'p5f-iphone-aaaaaaaaaaaa';run.mkdir()
                (run/'result.private.json').write_text(json.dumps({'status':'BUILD_VERIFIED','installation':'NOT_RUN'}))
                attempt=run/'install-attempt-first';attempt.mkdir()
                if state in ('missing_result','conflicting_not_run'):
                    (attempt/'install.private.log').write_bytes(b'')
                    if state=='conflicting_not_run': (attempt/'result.private.json').write_text('{"installation":"NOT_RUN"}')
                else: (attempt/'result.private.json').write_text(json.dumps({'installation':state}))
                with patch.object(host,'QA_ROOT',root), patch.object(host,'device_json') as device:
                    with self.assertRaisesRegex(ValueError,'nonce_installation_already_started'): host.install_verified(run)
                    device.assert_not_called()
                self.assertEqual(len(list(run.glob('install-attempt-*'))),1)

    def test_only_marked_generated_venv_is_excluded_and_recorded(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            generated = root/'p5e-generated'/'.venv'; generated.mkdir(parents=True)
            (generated/'pyvenv.cfg').write_text('home = /existing/python')
            (generated/'dependency').write_bytes(b'cache')
            unmarked = root/'p5e-evidence'/'.venv'; unmarked.mkdir(parents=True)
            (unmarked/'report.json').write_bytes(b'evidence')
            exclusions = []
            with patch.object(host, 'QA_ROOT', root), patch.object(host, 'PINNED_EVIDENCE_ROOTS', ('p5e-generated','p5e-evidence')): old = host.protected_outputs(exclusions)
            self.assertNotIn(str(generated/'dependency'), old)
            self.assertIn(str(unmarked/'report.json'), old)
            self.assertEqual(len(exclusions), 1)
            self.assertEqual(exclusions[0]['path'], str(generated))
            self.assertEqual(exclusions[0]['markerSha256'], host.file_hash(generated/'pyvenv.cfg'))

    def test_only_related_p5_archives_are_read_and_binaries_are_protected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            unrelated = root/'iphone05-archive'; unrelated.mkdir(); (unrelated/'secret').write_bytes(b'unrelated')
            related = root/'p5e-related'; related.mkdir(); (related/'session.jsonl').write_bytes(b'sealed')
            binary = related/'derived-data/Build/Products/Release-iphoneos/P5eOffline.app/executable'
            binary.parent.mkdir(parents=True); binary.write_bytes(b'binary')
            scope = {}
            real_hash = host.file_hash
            def checked_hash(path):
                self.assertFalse(Path(path).is_relative_to(unrelated))
                return real_hash(path)
            with patch.object(host, 'QA_ROOT', root), patch.object(host, 'PINNED_EVIDENCE_ROOTS', ('p5e-related',)), patch.object(host, 'file_hash', side_effect=checked_hash):
                old = host.protected_outputs(scope_record=scope)
            self.assertIn(str(related/'session.jsonl'), old)
            self.assertIn(str(binary), old)
            self.assertIn('iphone05-archive', scope['excludedHistoricalEntries'])
            self.assertEqual(scope['policy'], 'DECISION_PINNED_P5_BASELINES_PLUS_SMALL_RECORDS')

    def test_rejected_run_keeps_small_records_but_never_reads_large_raw_or_partial_binary(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            accepted = root/'p5e-accepted'; accepted.mkdir(); (accepted/'report.json').write_bytes(b'accepted')
            rejected = root/'p5d-rejected'; rejected.mkdir(); (rejected/'result.private.json').write_bytes(b'rejected')
            raw = rejected/'raw'; raw.mkdir(); (raw/'frame.rgba').write_bytes(b'raw')
            partial = rejected/'derived-data'; partial.mkdir(); (partial/'executable').write_bytes(b'partial')
            scope = {}; real_hash = host.file_hash
            def checked_hash(path):
                self.assertFalse(Path(path).is_relative_to(raw))
                self.assertFalse(Path(path).is_relative_to(partial))
                return real_hash(path)
            with patch.object(host, 'QA_ROOT', root), patch.object(host, 'PINNED_EVIDENCE_ROOTS', ('p5e-accepted',)), patch.object(host, 'file_hash', side_effect=checked_hash):
                old = host.protected_outputs(scope_record=scope)
            self.assertIn(str(rejected/'result.private.json'), old)
            self.assertNotIn(str(raw/'frame.rgba'), old)
            self.assertEqual(scope['excludedRawScopes'][0]['writePolicy'], 'NO_WRITES')
            self.assertEqual(set(scope['excludedRawScopes'][0]['excludedDirectories']), {'raw','derived-data'})

    def test_all_123_p5c_media_files_are_pinned_beyond_selected_12(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); sources = []
            for asset in range(41):
                files = {}
                for arm in ('original', 'PNG', 'WEBP'):
                    name = f'{asset}-{arm}.fixture'; data = f'{asset}:{arm}'.encode()
                    (root/name).write_bytes(data)
                    files[arm] = dict(file=name, sha256=host.digest(data), bytes=len(data))
                sources.append(dict(sourceFile=files['original']['file'], sourceSha256=files['original']['sha256'],
                                    sourceBytes=files['original']['bytes'], candidates={k:files[k] for k in ('PNG','WEBP')}))
            report = root/'report.json'; report.write_text(json.dumps(dict(sources=sources)))
            with patch.object(host.p5d, 'REPORT_SHA', host.file_hash(report)):
                old = host.p5c_report_inventory(report)
                self.assertEqual(len(old), 124)
                (root/'40-WEBP.fixture').write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError, 'full_media_pin'): host.p5c_report_inventory(report)

    def test_both_install_flows_check_fresh_lock_after_hashes_before_mutation(self):
        for flow in ('build-and-install', 'install-verified'):
            for locked_at_boundary in (False, True):
                with self.subTest(flow=flow, locked=locked_at_boundary), tempfile.TemporaryDirectory() as folder:
                    root = Path(folder).resolve()
                    run = root/'p5f-iphone-aaaaaaaaaaaa'; run.mkdir()
                    app = run/'derived-data/Build/Products/Release-iphoneos/P5eOffline.app'
                    (run/'result.private.json').write_text(json.dumps(dict(status='BUILD_VERIFIED', installation='NOT_RUN',
                        bundlePath=str(app), bundleInventory={}, bundleAudit={}, bundleBytes=10)))
                    (run/'source-before.private.json').write_text(json.dumps(dict(repository={}, inputs={})))
                    (run/'old-output-before.private.json').write_text('{}')
                    (run/'snapshot-binding.private.json').write_text('{}')
                    failed = run/'install-attempt-premutation'; failed.mkdir()
                    (failed/'result.private.json').write_text('{"installation":"NOT_RUN","reason":"device_unlock_required"}')
                    profile = root/'profile'; profile.write_bytes(b'existing')
                    events = []
                    def device(output, label, args):
                        if args[:3] == ['device', 'info', 'lockState']:
                            events.append(label)
                            return {'passcodeRequired': locked_at_boundary and label == 'lock-before-install'}
                        return {}
                    def preserved(*args): events.append('preservation'); return {'unchanged': True}
                    def backup(*args): events.append('backup'); return {'verified': True}
                    def install(output, binary, result): events.append('install'); result['installation'] = 'INSTALLED'
                    def prepare(output, *args):
                        mobile = output/'host'; mobile.mkdir(); return mobile, 'nonce'
                    def protected(*args, scope_record=None, **kwargs):
                        scope_record['policy'] = 'TEST'; return {}
                    with ExitStack() as patches:
                        for target, value in [('QA_ROOT',root),('preserved',preserved),('backup',backup),
                            ('device_json',device),('install_app',install),('prepare',prepare),('protected_outputs',protected),
                            ('signing',lambda *args:(profile,{},'identity')),('validate_bundle',lambda *args:{}),
                            ('validate_device',lambda *args:None),('space',lambda *args:{}),('inventory',lambda *args:{}),
                            ('snapshot_binding',lambda *args:{}),('source_closure',lambda:{}),('command',lambda *args,**kwargs:b'')]:
                            patches.enter_context(patch.object(host,target,value))
                        patches.enter_context(patch.object(host.p5d,'fixture_cases',return_value=([],{})))
                        patches.enter_context(patch.object(host.p5d,'repo_hashes',return_value={}))
                        patches.enter_context(patch.object(host.shutil,'disk_usage',return_value=SimpleNamespace(free=10**12)))
                        _, result = host.install_verified(run) if flow == 'install-verified' else host.run_all(root/'report.json',True)
                    boundary = events.index('lock-before-install')
                    self.assertEqual(events[boundary-1], 'preservation')
                    self.assertLess(events.index('backup'),boundary)
                    if locked_at_boundary:
                        self.assertNotIn('install',events)
                        self.assertEqual(result['installation'],'NOT_RUN')
                        self.assertIn('device_unlock_required',result['reason'])
                    else:
                        self.assertEqual(events[boundary+1],'install')
                        self.assertEqual(result['installation'],'INSTALLED')
                        self.assertEqual(result['status'],'AWAITING_OFFLINE_PERFORMANCE')

    def test_source_closure_preserves_offline_composition(self):
        sources = host.source_closure()
        self.assertIn('lib/features/reading_images/p5f_performance/presentation/p5f_cubit.dart', sources)
        self.assertFalse(any('firebase' in name or name.endswith('/main.dart') for name in sources))

if __name__ == '__main__': unittest.main()
