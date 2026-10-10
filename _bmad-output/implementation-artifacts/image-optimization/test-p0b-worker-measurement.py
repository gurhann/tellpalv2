#!/usr/bin/env python3
"""Synthetic resource contracts and isolated tiny save/meter seams; no corpus evidence."""
import copy
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE=Path(__file__).resolve().parent

def load(name,file):
    spec=importlib.util.spec_from_file_location(name,HERE/file); result=importlib.util.module_from_spec(spec); spec.loader.exec_module(result); return result
M=load('measurement','verify-p0b-worker-measurement.py'); R=load('collector','run-p0b-worker-measurement.py')


def synthetic(stage='encode',status='success'):
    names=[('bootstrap_start',None),('bootstrap_end',None),('idle_start',None),('idle_end',None)]
    phases=['canonical_read','projection','encoding'] if stage=='encode' else ['output_verification']
    for phase in phases+['cleanup']: names.extend([('phase_start',phase),('phase_end',phase)])
    names.extend([('settle_start',None),('settle_end',None),('complete',None)])
    events=[]; t=1000000000
    for index,(name,phase) in enumerate(names):
        if name=='idle_end': t+=5*10**9
        elif name=='settle_end': t+=30*10**9
        else: t+=10**6
        events.append({'attempt_id':'synthetic','stage':stage,'pid':10,'seq':index,'name':name,'phase':phase,'t_ns':t,'user_seconds':index*.01,'system_seconds':index*.005})
    end=t+10**6
    disk={category:{'logical_bytes':10,'allocated_bytes':512} for category in ('output','tmp','evidence','retained_batch')}
    samples=[]
    for event in events:
        samples.append({'attempt_id':'synthetic','stage':stage,'pid':10,'supervisor_pid':20,'kind':'boundary','planned_ns':event['t_ns'],'t_ns':event['t_ns']+10,'lateness_ns':10,'capture_cost_ns':10,'event_seq':event['seq'],'tick':None,'rss_bytes':100,'threads':1,'error':None,'disk':copy.deepcopy(disk)})
        if event['name'] in ('idle_start','settle_start'):
            seconds=5 if event['name']=='idle_start' else 30
            endpoint=(event['t_ns'] if seconds==5 else next(e['t_ns'] for e in events if e['name']=='phase_end' and e['phase']=='cleanup'))+seconds*10**9
            samples.append({'attempt_id':'synthetic','stage':stage,'pid':10,'supervisor_pid':20,'kind':'idle_endpoint' if seconds==5 else 'settled_endpoint','planned_ns':endpoint,'t_ns':endpoint,'lateness_ns':0,'capture_cost_ns':10,'event_seq':None,'tick':None,'rss_bytes':100 if seconds==5 else 90,'threads':1,'error':None,'disk':copy.deepcopy(disk)})
    for tick in range((end-10**9)//10**9+1):
        planned=10**9+tick*10**9
        samples.append({'attempt_id':'synthetic','stage':stage,'pid':10,'supervisor_pid':20,'kind':'periodic','planned_ns':planned,'t_ns':planned,'lateness_ns':0,'capture_cost_ns':10,'event_seq':None,'tick':tick,'rss_bytes':100,'threads':1,'error':None,'disk':copy.deepcopy(disk)})
    samples.sort(key=lambda s:s['t_ns'])
    for index,s in enumerate(samples): s['seq']=index
    life={'attempt_id':'synthetic','stage':stage,'pid':10,'supervisor_pid':20,'start_ns':10**9,'end_ns':end,'status':status,'terminal':{'pid':10,'exit_code':0 if status=='success' else 2,'signal':None,'user_seconds':1.0,'system_seconds':.5,'maxrss_bytes':150,'reaped_ns':end},'term_sent':False,'kill_sent':False,'term_ns':None,'kill_ns':None,'deadline_ns':121000000000,'deadline_expired':False,'units':M.UNITS,'collector_user_seconds':.1,'collector_system_seconds':.1,'observer_wall_ns':10}
    return events,samples,life


class Metrics(unittest.TestCase):
    def valid(self,stage='encode'):
        return M.derive_child(*synthetic(stage),'synthetic',stage)
    def reject(self,mutate):
        e,s,l=synthetic(); mutate(e,s,l)
        with self.assertRaises((ValueError,KeyError,TypeError)): M.derive_child(e,s,l,'synthetic','encode')
    def test_complete_encode(self): self.assertTrue(self.valid()['complete'])
    def test_complete_verify(self): self.assertTrue(self.valid('verify')['complete'])
    def test_signed_settled_delta(self): self.assertEqual(self.valid()['settled_minus_idle_rss_bytes'],-10)
    def test_short_phase_has_null_peak(self):
        phase=self.valid()['phases']['encoding']; self.assertEqual(phase['periodic_samples'],0); self.assertIsNone(phase['sampled_peak_rss_bytes'])
    def test_kernel_peak_separate(self): self.assertEqual(self.valid()['kernel_lifetime_high_water_rss_bytes'],150)
    def test_no_overlapping_sum(self): self.assertNotIn('total_memory',self.valid())
    def test_cpu_deltas(self): self.assertAlmostEqual(self.valid()['phases']['encoding']['user_seconds'],.01)
    def test_bool_duration_rejected(self): self.reject(lambda e,s,l:e[0].update(t_ns=True))
    def test_bool_bytes_rejected(self): self.reject(lambda e,s,l:s[0].update(rss_bytes=True))
    def test_nonfinite_rejected(self): self.reject(lambda e,s,l:e[0].update(user_seconds=float('nan')))
    def test_negative_bytes_rejected(self): self.reject(lambda e,s,l:s[0].update(rss_bytes=-1))
    def test_negative_duration_rejected(self): self.reject(lambda e,s,l:e[1].update(t_ns=e[0]['t_ns']-1))
    def test_negative_cpu_rejected(self): self.reject(lambda e,s,l:e[5].update(user_seconds=0))
    def test_units_rejected(self): self.reject(lambda e,s,l:l.update(units={'rss':'KiB'}))
    def test_event_order_rejected(self): self.reject(lambda e,s,l:e.reverse())
    def test_event_replay_rejected(self): self.reject(lambda e,s,l:e[1].update(seq=0))
    def test_wrong_pid_rejected(self): self.reject(lambda e,s,l:e[0].update(pid=11))
    def test_wrong_attempt_rejected(self): self.reject(lambda e,s,l:s[0].update(attempt_id='other'))
    def test_wrong_supervisor_rejected(self): self.reject(lambda e,s,l:s[0].update(supervisor_pid=99))
    def test_missing_event_rejected(self): self.reject(lambda e,s,l:e.pop())
    def test_extra_event_rejected(self): self.reject(lambda e,s,l:e.append(copy.deepcopy(e[-1])))
    def test_missing_idle_rejected(self): self.reject(lambda e,s,l:s.__setitem__(slice(None),[x for x in s if x['kind']!='idle_endpoint']))
    def test_missing_settled_rejected(self): self.reject(lambda e,s,l:s.__setitem__(slice(None),[x for x in s if x['kind']!='settled_endpoint']))
    def test_missing_boundary_rejected(self): self.reject(lambda e,s,l:s.__setitem__(slice(None),[x for x in s if x.get('event_seq')!=4]))
    def test_early_endpoint_rejected(self): self.reject(lambda e,s,l:next(x for x in s if x['kind']=='idle_endpoint').update(t_ns=10**9))
    def test_missing_terminal_rejected(self): self.reject(lambda e,s,l:l.update(terminal=None))
    def test_wrong_terminal_pid_rejected(self): self.reject(lambda e,s,l:l['terminal'].update(pid=11))
    def test_missing_terminal_exit_rejected(self): self.reject(lambda e,s,l:l['terminal'].update(exit_code=None))
    def test_bad_lateness_rejected(self): self.reject(lambda e,s,l:s[0].update(lateness_ns=42))
    def test_delayed_tick_visible(self):
        e,s,l=synthetic(); sample=next(x for x in s if x['kind']=='periodic'); sample.update(t_ns=sample['t_ns']+100,lateness_ns=100)
        s.sort(key=lambda x:x['t_ns'])
        for i,x in enumerate(s):x['seq']=i
        result=M.derive_child(e,s,l,'synthetic','encode'); self.assertEqual(result['coverage']['maximum_lateness_ns'],100)
    def test_missing_tick_rejected(self): self.reject(lambda e,s,l:next(x for x in s if x['kind']=='periodic').update(tick=1))
    def test_all_periodic_records_missing_rejected(self):
        def remove(e,s,l):
            s[:]=[x for x in s if x['kind']!='periodic']
            for i,x in enumerate(s):x['seq']=i
        self.reject(remove)
    def test_periodic_tail_missing_rejected(self):
        def remove(e,s,l):
            last=max(x['tick'] for x in s if x['kind']=='periodic')
            s[:]=[x for x in s if x['kind']!='periodic' or x['tick']<last]
            for i,x in enumerate(s):x['seq']=i
        self.reject(remove)
    def test_delayed_tail_tick_keeps_actual_coverage(self):
        e,s,l=synthetic();periodic=[x for x in s if x['kind']=='periodic'];s.remove(periodic[-1]);last=periodic[-2]
        last['t_ns']=e[-1]['t_ns']-100;last['lateness_ns']=last['t_ns']-last['planned_ns']
        s.sort(key=lambda x:x['t_ns'])
        for i,x in enumerate(s):x['seq']=i
        result=M.derive_child(e,s,l,'synthetic','encode');self.assertTrue(result['complete']);self.assertGreater(result['coverage']['maximum_lateness_ns'],10**9)
    def test_terminal_user_cpu_cannot_precede_last_event(self):
        self.reject(lambda e,s,l:l['terminal'].update(user_seconds=e[-1]['user_seconds']-.001))
    def test_terminal_system_cpu_cannot_precede_last_event(self):
        self.reject(lambda e,s,l:l['terminal'].update(system_seconds=e[-1]['system_seconds']-.001))
    def test_terminal_microsecond_precision_tolerance(self):
        e,s,l=synthetic()
        for key in ('user_seconds','system_seconds'):l['terminal'][key]=e[-1][key]-.000001
        self.assertTrue(M.derive_child(e,s,l,'synthetic','encode')['complete'])
    def test_capture_error_never_zero(self): self.reject(lambda e,s,l:s[0].update(error='unsupported',rss_bytes=0))
    def test_success_missing_sample_rejected(self): self.reject(lambda e,s,l:s.clear())
    def test_disk_negative_rejected(self): self.reject(lambda e,s,l:s[0]['disk']['tmp'].update(logical_bytes=-1))
    def test_disk_bad_unit_rejected(self): self.reject(lambda e,s,l:s[0]['disk']['tmp'].update(kib=1))
    def test_expected_failed_partial_retained(self):
        e,s,l=synthetic(status='failure'); e=e[:2]; s=[x for x in s if x['t_ns']<=e[-1]['t_ns']+10]
        for i,x in enumerate(s): x['seq']=i
        result=M.derive_child(e,s,l,'synthetic','encode'); self.assertFalse(result['complete']); self.assertIsNone(result['idle']['rss_bytes'])
    def test_timeout_requires_supervisor_proof(self): self.reject(lambda e,s,l:l.update(status='timeout'))
    def test_expected_spawn_failure(self):
        _,_,l=synthetic(status='spawn_failure'); l.update(pid=None,terminal=None)
        result=M.derive_child([],[],l,'synthetic','encode'); self.assertFalse(result['complete']); self.assertIsNone(result['kernel_lifetime_high_water_rss_bytes'])
    def test_success_terminal_exit_forgery(self): self.reject(lambda e,s,l:l['terminal'].update(exit_code=2))
    def test_float_terminal_bytes(self): self.reject(lambda e,s,l:l['terminal'].update(maxrss_bytes=150.5))
    def test_success_terminal_late(self): self.reject(lambda e,s,l:l['terminal'].update(reaped_ns=l['deadline_ns']+1))
    def test_bool_terminal_exit(self): self.reject(lambda e,s,l:l['terminal'].update(exit_code=False))
    def test_bool_pid(self): self.reject(lambda e,s,l:l.update(pid=True))
    def test_bool_signal_flag(self): self.reject(lambda e,s,l:l.update(term_sent=0))
    def test_boundary_wrong_event_binding(self): self.reject(lambda e,s,l:next(x for x in s if x['kind']=='boundary').update(event_seq=999))
    def test_boundary_wrong_planned_time(self): self.reject(lambda e,s,l:next(x for x in s if x['kind']=='boundary').update(planned_ns=0))
    def test_cpu_global_order(self): self.reject(lambda e,s,l:e[3].update(user_seconds=0))
    def test_bootstrap_separate(self): self.assertIn('bootstrap_guards',self.valid()['phases'])
    def test_os_threads_separate(self): self.assertEqual(self.valid()['sampled_lifetime_peak_os_threads'],1)
    def test_unavailable_counters_explicit(self): self.assertEqual(M.UNAVAILABLE['python_heap']['reason'],'unsupported')
    def test_unexercised_not_zero(self): self.assertIsNone(M.UNEXERCISED['source_decode_normalization']['duration_ns'])
    def test_abi_96_bytes(self): import ctypes; self.assertEqual(ctypes.sizeof(R.TaskInfo),96)
    def test_native_proc_taskinfo_resident_and_thread_mapping(self):
        import ctypes
        calls=[]
        def native(pid,flavor,arg,pointer,size):
            calls.append((pid,flavor,arg,size));row=ctypes.cast(pointer,ctypes.POINTER(R.TaskInfo)).contents
            row.virtual=987654321;row.resident=1048577;row.threadnum=7;row.running=2;return 96
        sampler=R.ProcSampler.__new__(R.ProcSampler);sampler.lib=__import__('types').SimpleNamespace(proc_pidinfo=native)
        self.assertEqual(sampler(123),{'rss_bytes':1048577,'threads':7,'error':None});self.assertEqual(calls,[(123,4,0,96)])
    def test_native_wrong_return_size_is_null_error(self):
        sampler=R.ProcSampler.__new__(R.ProcSampler);sampler.lib=__import__('types').SimpleNamespace(proc_pidinfo=lambda *args:95)
        self.assertEqual(sampler(123),{'rss_bytes':None,'threads':None,'error':'proc_pidinfo_return_size_95'})
    def test_frozen_environment_drift(self):
        with self.assertRaises(ValueError): M.validate_environment({'os':'Linux'})
    def test_verifier_does_not_import_encoder_or_collector(self):
        source=(HERE/'verify-p0b-worker-measurement.py').read_text(); self.assertNotIn("spec_from_file_location('collector'",source); self.assertNotIn('import run_p0b',source)


class Isolation(unittest.TestCase):
    def setUp(self): self.tmp=tempfile.TemporaryDirectory(dir='/private/tmp'); self.root=Path(self.tmp.name)
    def tearDown(self): self.tmp.cleanup()
    def test_exclusive_receipts(self):
        R.receipt(self.root/'a',{'x':1})
        with self.assertRaises(FileExistsError):R.receipt(self.root/'a',{})
        self.assertEqual((self.root/'a').stat().st_mode&0o222,0)
    def test_symlink_refused(self):
        (self.root/'link').symlink_to(self.root/'a')
        with self.assertRaises(ValueError):R.receipt(self.root/'link',{})
    def test_parent_segment_refused(self):
        with self.assertRaises(ValueError):R.receipt(self.root/'..'/'a',{})
    def test_existing_batch_refused(self):
        with self.assertRaises(ValueError):M.V.safe_batch(self.root,[],new=True)
    def test_overlap_both_directions(self):
        for output,input_ in ((self.root/'a',self.root),(self.root,self.root/'a')):
            with self.assertRaises(ValueError):M.V.safe_batch(output,[input_])
    def test_hardlink_disk_deduplicated(self):
        (self.root/'a').write_bytes(b'123'); os.link(self.root/'a',self.root/'b'); self.assertEqual(R.footprint(self.root)['logical_bytes'],3)
    def test_partial_writer_retained(self):
        with (self.root/'partial').open('wb') as stream:
            writer=R.LimitedWriter(stream); writer.write(b'123')
            with patch.dict(R.LIMITS,output_bytes=3):
                with self.assertRaises(ValueError):writer.write(b'4')
        self.assertEqual((self.root/'partial').read_bytes(),b'123')
    def test_jsonl_duplicate_rejected(self):
        (self.root/'raw').write_bytes(b'{"a":1,"a":2}\n')
        with self.assertRaises(ValueError):M.read_jsonl(self.root/'raw',64)
    def test_jsonl_nonfinite_rejected(self):
        (self.root/'raw').write_bytes(b'{"a":NaN}\n')
        with self.assertRaises(ValueError):M.read_jsonl(self.root/'raw',64)
    def test_jsonl_truncated_tail_preserved(self):
        (self.root/'raw').write_bytes(b'{"a":1}\n{"a":')
        rows,tail=M.read_jsonl(self.root/'raw',64); self.assertEqual(rows,[{'a':1}]); self.assertEqual(tail,'{"a":')
    def test_jsonl_bound(self):
        (self.root/'raw').write_bytes(b'{}\n'*65)
        with self.assertRaises(ValueError):M.read_jsonl(self.root/'raw',64)
    def test_public_privacy(self):
        with self.assertRaises(ValueError):M.V.scan_public({'path':str(self.root)})
    def setup_child(self):
        directory=self.root/'attempt'; R.mkdir(directory); R.mkdir(directory/'tmp'); return directory
    def test_spawn_failure_retained(self):
        directory=self.setup_child(); result=R.supervise(['/no/such/executable'],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'spawn_failure'); self.assertFalse(M.load_child(directory,result,'synthetic','encode')['complete'])
    def test_early_failure_lifecycle_retained(self):
        directory=self.setup_child(); result=R.supervise([sys.executable,'-c','raise SystemExit(2)'],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'failure'); self.assertFalse(M.load_child(directory,result,'synthetic','encode')['complete'])
    def descendant_fixture(self,resistant):
        import signal,time
        directory=self.setup_child();pid_file=self.root/'descendant.pid';pid=None
        script="""import os,signal,time
from pathlib import Path
path=Path(%r)
pid=os.fork()
if pid==0:
    if %r: signal.signal(signal.SIGTERM,signal.SIG_IGN)
    path.write_text(str(os.getpid()))
    while True: time.sleep(.05)
while not path.exists(): time.sleep(.01)
os._exit(2)
"""%(str(pid_file),resistant)
        try:
            original=R.os.wait4;reaps=[]
            def waited(*args):
                value=original(*args)
                if value[0]:reaps.append(value[0])
                return value
            with patch.object(R.os,'wait4',side_effect=waited):
                result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
            pid=int(pid_file.read_text());self.assertEqual(result['failure'],'descendant_pipe_linger')
            lifecycle=M.V.load_json(directory/'encode.lifecycle.json');self.assertTrue(lifecycle['term_sent'])
            if resistant:
                self.assertTrue(lifecycle['kill_sent']);self.assertGreaterEqual(lifecycle['kill_ns']-lifecycle['term_ns'],2*10**9)
            # wait4 must produce one terminal result; a reaped leader is never waited twice.
            self.assertEqual(reaps,[lifecycle['pid']]);self.assertEqual(lifecycle['terminal']['exit_code'],2)
            deadline=time.monotonic()+2
            alive=True
            while time.monotonic()<deadline:
                try:os.kill(pid,0)
                except ProcessLookupError:alive=False;break
                time.sleep(.02)
            self.assertFalse(alive,'owned descendant remained present after cleanup')
            for name in ('stdout','stderr','events.jsonl','samples.jsonl','lifecycle.json'):
                self.assertEqual((directory/('encode.'+name)).stat().st_mode&0o222,0)
        finally:
            if pid is None and pid_file.exists():pid=int(pid_file.read_text())
            if pid is not None:
                try:os.kill(pid,signal.SIGKILL)
                except ProcessLookupError:pass
    def test_reaped_leader_descendant_pipe_holder_terminated(self):self.descendant_fixture(False)
    def test_reaped_leader_term_resistant_descendant_killed(self):self.descendant_fixture(True)
    def test_closed_stdin_handshake_preserves_all_evidence(self):
        directory=self.setup_child()
        script="""import os,json,time,resource
os.close(0)
u=resource.getrusage(resource.RUSAGE_SELF)
e=dict(attempt_id='synthetic',stage='encode',pid=os.getpid(),seq=0,name='bootstrap_start',phase=None,t_ns=time.perf_counter_ns(),user_seconds=u.ru_utime,system_seconds=u.ru_stime)
os.write(int(os.environ['P0B_EVENT_FD']),json.dumps(e).encode()+b'\\n')
time.sleep(1)
"""
        result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'instrumentation_failure')
        for name in ('stdout','stderr','events.jsonl','samples.jsonl','lifecycle.json'):
            path=directory/('encode.'+name);self.assertEqual(path.stat().st_mode&0o222,0);self.assertIn(path.name,result['files'])
        self.assertIsNotNone(M.V.load_json(directory/'encode.lifecycle.json')['terminal'])
        self.assertEqual(M.read_jsonl(directory/'encode.events.jsonl',64)[0][0]['name'],'bootstrap_start')
    def test_sample_pid_loss_confirmed_failure_is_partial(self):
        directory=self.setup_child()
        def gone(pid): __import__('time').sleep(.15); return {'rss_bytes':None,'threads':None,'error':'proc_pidinfo_return_size_0'}
        result=R.supervise([sys.executable,'-c','raise SystemExit(2)'],directory,self.root,'synthetic','encode',sampler=gone)
        self.assertEqual(result['status'],'failure'); measured=M.load_child(directory,result,'synthetic','encode'); self.assertFalse(measured['complete']); self.assertEqual(measured['coverage']['failures'],1)
    def test_live_pid_api_loss_is_instrumentation_stop(self):
        directory=self.setup_child(); result=R.supervise([sys.executable,'-c','import time; time.sleep(10)'],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':None,'threads':None,'error':'proc_pidinfo_return_size_0'})
        self.assertEqual(result['status'],'instrumentation_failure')
    def test_timeout_reaped_and_partial_retained(self):
        directory=self.setup_child(); script="import time; print('partial',flush=True); time.sleep(10)"
        result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None},child_seconds=.1)
        self.assertEqual(result['status'],'timeout'); self.assertIn(b'partial',(directory/'encode.stdout').read_bytes()); self.assertFalse(M.load_child(directory,result,'synthetic','encode')['complete'])
    def test_malformed_events_stop(self):
        directory=self.setup_child(); script="import os; os.write(int(os.environ['P0B_EVENT_FD']),b'not-json\\n')"
        result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'instrumentation_failure')
    def test_log_overflow_stops(self):
        directory=self.setup_child(); script="import os; os.write(1,b'x'*4096)"
        with patch.dict(R.LIMITS,log_bytes=100): result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'instrumentation_failure')
    def test_global_deadline_stops(self):
        directory=self.setup_child(); result=R.supervise([sys.executable,'-c','import time; time.sleep(1)'],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None},batch_deadline=0)
        self.assertEqual(result['status'],'instrumentation_failure'); self.assertEqual(result['failure'],'batch_deadline')
    def test_disk_cap_stops(self):
        directory=self.setup_child()
        with patch.dict(R.LIMITS,batch_bytes=1):
            (directory/'tmp'/'partial').write_bytes(b'123')
            result=R.supervise([sys.executable,'-c','import time; time.sleep(1)'],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['failure'],'disk_cap')
    def test_cross_category_hardlink_disk(self):
        directory=self.setup_child(); (directory/'output.bin').write_bytes(b'123'); os.link(directory/'output.bin',directory/'tmp'/'link')
        snap=R.disk_snapshot(directory,self.root); self.assertEqual(snap['output']['logical_bytes'],3); self.assertEqual(snap['tmp']['logical_bytes'],0); self.assertEqual(snap['evidence']['logical_bytes'],0)
    def test_guard_timeout_reaps(self):
        with patch.dict(R.LIMITS,preflight_seconds=.03):
            with self.assertRaises(ValueError): R.bounded([sys.executable,'-c','import time; time.sleep(10)'],self.root,'guard')
        record=M.V.load_json(self.root/'guard.lifecycle.json'); self.assertEqual(record['failure'],'guard_deadline'); self.assertIsNotNone(record['terminal'])
    def test_guard_global_deadline(self):
        with self.assertRaises(ValueError):R.bounded([sys.executable,'-c','import time; time.sleep(10)'],self.root,'guard',batch_deadline=0)
        self.assertEqual(M.V.load_json(self.root/'guard.lifecycle.json')['failure'],'batch_deadline')
    def witness_fixture(self):
        root=self.root/'witnesses';root.mkdir()
        for name in (*M.CODE_NAMES,M.PROTOCOL.name):
            (root/name).write_bytes((HERE/name).read_bytes());(root/name).chmod(0o444)
        return root,{'measurement_code':M.code_identity()}
    def test_historical_sources_rehashed_against_scope(self):
        root,scope=self.witness_fixture();result=M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
        self.assertEqual(result['measurement_code'],scope['measurement_code']);self.assertEqual(result['measurement_source_kind'],'historical_scope_bound_witnesses')
        self.assertEqual(result['final_verification_code'][0],M.V.file_identity(HERE/'verify-p0b-worker-measurement.py','verify-p0b-worker-measurement.py'))
    def test_unrecognized_historical_scope_rejected(self):
        root,scope=self.witness_fixture()
        with self.assertRaisesRegex(ValueError,'unrecognized_historical_measurement_scope'):M.verification_provenance(scope,root,scope_sha256='0'*64)
    def test_historical_witness_tampering_rejected(self):
        root,scope=self.witness_fixture();path=root/M.CODE_NAMES[0];path.chmod(0o644);path.write_bytes(b'forged');path.chmod(0o444)
        with self.assertRaisesRegex(ValueError,'measurement_code_witness_mismatch'):M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
    def test_git_writable_historical_witness_hash_still_verified(self):
        root,scope=self.witness_fixture();(root/M.CODE_NAMES[0]).chmod(0o644)
        self.assertEqual(M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)['measurement_code'],scope['measurement_code'])
    def test_non_file_historical_witness_rejected(self):
        root,scope=self.witness_fixture();path=root/M.CODE_NAMES[0];path.unlink();path.mkdir()
        with self.assertRaisesRegex(ValueError,'historical_code_witness_file_type'):M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
    def test_extra_historical_witness_file_rejected(self):
        root,scope=self.witness_fixture();(root/'fake').write_text('fake')
        with self.assertRaisesRegex(ValueError,'historical_code_witness_inventory'):M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
    def test_historical_witness_missing_file_rejected(self):
        root,scope=self.witness_fixture();(root/M.CODE_NAMES[0]).unlink()
        with self.assertRaisesRegex(ValueError,'historical_code_witness_inventory'):M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
    def test_historical_witness_symlink_rejected(self):
        root,scope=self.witness_fixture();path=root/M.CODE_NAMES[0];path.unlink();path.symlink_to(HERE/M.CODE_NAMES[0])
        with self.assertRaises(ValueError):M.verification_provenance(scope,root,scope_sha256=M.HISTORICAL_SCOPE_HASH)
    def test_new_worker_guard_does_not_accept_historical_code(self):
        scope={'parent_scope':{},'measurement_code':[{'id':'forged_old_code','sha256':'0'*64,'bytes':1}],'environment':{'synthetic':True}}
        request={'inputs':{},'scope':str(self.root/'scope.json')}
        with patch.object(M.V,'load_json',return_value=scope),patch.object(M.V,'check_integrity'),patch.object(M,'environment',return_value=scope['environment']):
            with self.assertRaisesRegex(ValueError,'prerequisite_integrity_failed'):M.check_request(request,self.root/'request.json')
    def test_historical_option_refused_for_meter_child(self):
        result=subprocess.run([sys.executable,str(HERE/'verify-p0b-worker-measurement.py'),'--meter-request',str(self.root/'absent'),'--historical-code-root',str(self.root)],capture_output=True,text=True)
        self.assertEqual(result.returncode,2);self.assertEqual(json.loads(result.stdout)['diagnostics'][0]['category'],'historical_code_option_final_verification_only')
    def test_tiny_live_meter_save_seam(self):
        directory=self.setup_child(); magic=M.V.P0A.MAGIC; raw=magic+__import__('struct').pack('>II',2,2)+bytes([10,20,30,255])*4
        reference=self.root/'tiny.pixels'; reference.write_bytes(raw)
        pair=copy.deepcopy(M.V.load_json(HERE/'p0a-corpus.json')['trial_plan']['planned_pairs'][0]); pair['geometry']={'width':2,'height':2}; pair['reference_pixel_sha256']=M.V.sha(raw)
        request={'attempt_id':'synthetic','stage':'encode','inputs':{},'pair':pair,'arm':'control','reference':str(reference),'output':str(directory/'output.bin')}
        R.receipt(directory/'request.json',request)
        script="import importlib.util, pathlib; s=importlib.util.spec_from_file_location('r', "+repr(str(HERE/'run-p0b-worker-measurement.py'))+"); r=importlib.util.module_from_spec(s); s.loader.exec_module(r); r.M.check_request=lambda *args: None; p=pathlib.Path("+repr(str(directory/'request.json'))+"); r.encode_worker(r.V.load_json(p),p)"
        result=R.supervise([sys.executable,'-c',script],directory,self.root,'synthetic','encode',sampler=lambda pid:{'rss_bytes':100,'threads':1,'error':None})
        self.assertEqual(result['status'],'success'); measured=M.load_child(directory,result,'synthetic','encode'); self.assertTrue(measured['complete']); self.assertGreaterEqual(measured['settled']['t_ns']-measured['idle']['t_ns'],30*10**9)
        with M.V.Image.open(directory/'output.bin') as image:image.load(); self.assertEqual(image.size,(2,2))
    def test_tiny_actual_frozen_saves(self):
        image=M.V.Image.new('RGB',(2,2),(10,20,30)); corpus=M.V.load_json(HERE/'p0a-corpus.json')
        settings={json.dumps(p[arm+'_settings'],sort_keys=True) for p in corpus['trial_plan']['planned_pairs'] for arm in ('control','candidate')}
        for index,setting in enumerate(settings):
            arguments={k:v for k,v in json.loads(setting).items() if k not in ('mode','profile_handling')}; output=self.root/str(index)
            with output.open('wb') as stream: image.save(R.LimitedWriter(stream),**arguments); stream.flush(); os.fsync(stream.fileno())
            with M.V.Image.open(output) as readback: readback.load(); self.assertEqual(readback.size,(2,2)); self.assertEqual(readback.mode,'RGB')
        image.close()

class Orchestration(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(dir='/private/tmp'); self.root=Path(self.tmp.name)
        self.parent=self.root/'parent'; R.mkdir(self.parent); R.receipt(self.parent/'scope.json',{'batch_id':'encoder-pilot-r3-v2'}); R.receipt(self.parent/'execution-result.json',{})
        self.args=__import__('argparse').Namespace(source_root=self.root/'sources',reference_root=self.root/'references',preparation_receipt=self.root/'preparation.json',freeze_manifest=self.root/'freeze.json',corpus=HERE/'p0a-corpus.json',protocol=HERE/'p0a-trial-protocol.md',parent_batch=self.parent,output_root=self.root/'batch',batch_id='synthetic-worker-matrix')
        self.parent_result=M.V.load_json(HERE/'p0b-encoder-pilot.json'); self.evidence={'status':'PASS','input_snapshot':[]}
        self.old_hash=R.V.stream_hash
    def tearDown(self): self.tmp.cleanup()
    def stream_hash(self,path):
        if Path(path)==self.parent/'scope.json':return M.PARENT_SCOPE_HASH,0
        return self.old_hash(path)
    def run_fixture(self,mode):
        calls=[]
        old_load=R.V.load_json
        def load_json(path):return self.parent_result if Path(path)==HERE/'p0b-encoder-pilot.json' else old_load(path)
        def bounded(command,directory,name,**kwargs):return self.parent_result if name=='parent-reverification' else self.evidence
        def guard(*args,**kwargs):
            if mode=='integrity':raise R.V.GateFailure('prerequisite_integrity_failed')
        def supervise(command,directory,batch,ident,stage,**kwargs):
            calls.append((ident,stage)); start=__import__('time').perf_counter_ns(); end=start+1
            status='success' if mode=='success' else 'instrumentation_failure' if mode=='instrumentation' else 'failure'
            R.receipt(directory/(stage+'.lifecycle.json'),{'start_ns':start,'end_ns':end,'deadline_ns':start+M.LIMITS['child_seconds']*10**9,'status':status})
            stdout=b''
            if mode=='failure_with_output':R.exclusive(directory/'output.bin',b'synthetic valid partial output inspected through fixture seam')
            if mode=='success':
                if stage=='encode':R.exclusive(directory/'output.bin',b'synthetic output inspected through fixture seam')
                request=M.V.load_json(directory/'request.json')
                observed=next(r['output'] for r in self.parent_result['arms'] if r['pair_id']==request['pair']['id'] and r['arm']==request['arm'])
                stdout=M.V.canonical({'status':'PASS','observation':observed})
            return {'status':status,'files':{},'failure':'malformed_event' if mode=='instrumentation' else None,'stdout':stdout}
        with patch.object(R.M,'environment',return_value={'synthetic':True}),patch.object(R.M,'validate_environment'),patch.object(R,'ProcSampler',return_value=lambda pid:{'rss_bytes':1,'threads':1,'error':None}),patch.object(R,'bounded',side_effect=bounded),patch.object(R,'guard',side_effect=guard),patch.object(R,'supervise',side_effect=supervise),patch.object(R.V,'stream_hash',side_effect=self.stream_hash),patch.object(R.V,'input_snapshot',return_value=[]),patch.object(R.V,'load_json',side_effect=load_json):
            R.run_batch(self.args)
        return R.V.load_json(self.args.output_root/'execution-result.json'),calls
    def verify_fixture(self,*,broken_resource=False):
        def child(directory,record,ident,stage):
            if broken_resource:raise M.V.GateFailure('missing_periodic_sampling')
            if record['status']=='success':
                phases=['canonical_read','projection','encoding'] if stage=='encode' else ['output_verification']
                return {'complete':True,'phases':{name:{'duration_ns':1} for name in phases},'idle':{'rss_bytes':1},'settled':{'rss_bytes':1}}
            return {'complete':False,'phases':{name:{'duration_ns':None} for name in ('canonical_read','projection','encoding')},'idle':{'rss_bytes':None},'settled':{'rss_bytes':None}}
        def inspect(output,reference,pair,arm):
            return next(r['output'] for r in self.parent_result['arms'] if r['pair_id']==pair['id'] and r['arm']==arm)
        scope=M.V.load_json(self.args.output_root/'scope.json')
        old_load=M.V.load_json
        def load_json(path):return self.parent_result if Path(path)==HERE/'p0b-encoder-pilot.json' else old_load(path)
        with patch.object(M,'environment',return_value={'synthetic':True}),patch.object(M,'validate_environment'),patch.object(M.V,'preflight',return_value=self.evidence),patch.object(M.V,'make_scope',return_value=scope['parent_scope']),patch.object(M.V,'verify_result',return_value=self.parent_result),patch.object(M.V,'stream_hash',side_effect=self.stream_hash),patch.object(M,'load_child',side_effect=child),patch.object(M.V,'inspect_output',side_effect=inspect),patch.object(M.V,'load_json',side_effect=load_json):
            return M.verify_batch(self.args.output_root,self.args)
    def test_retry_exhaustion_continues_exact_schedule(self):
        result,calls=self.run_fixture('failure'); self.assertIsNone(result['stop_reason']); self.assertEqual(len(calls),56); self.assertEqual(len(result['arms']),28)
        for row in result['arms']: self.assertEqual(row['status'],'failed'); self.assertEqual([a['kind'] for a in row['attempts']],['initial','retry'])
    def test_missing_output_never_borrows_parent_bytes(self):
        self.run_fixture('failure'); result=self.verify_fixture(); self.assertEqual(result['status'],'INCOMPLETE'); self.assertEqual(result['counts']['actual_attempts'],56); self.assertEqual(result['counts']['output_verified_arms'],0)
        self.assertTrue(all(row['output'] is None for row in result['arms']))
        self.assertTrue(all(all(delta is None for delta in pair['raw_byte_comparisons'].values()) for pair in result['pairs']))
    def test_integrity_stop_retains_all_not_run(self):
        result,calls=self.run_fixture('integrity'); self.assertEqual(result['stop_reason'],'prerequisite_integrity_failed'); self.assertEqual(calls,[]); self.assertTrue(all(row['status']=='not_run' for row in result['arms']))
    def test_instrumentation_stop_never_retries_or_continues(self):
        result,calls=self.run_fixture('instrumentation'); self.assertEqual(result['stop_reason'],'instrumentation_failure'); self.assertEqual(len(calls),1); self.assertEqual(len(result['arms'][0]['attempts']),1); self.assertTrue(all(row['status']=='not_run' for row in result['arms'][1:]))
        verified=self.verify_fixture(); self.assertEqual(verified['counts']['actual_attempts'],1); self.assertEqual(verified['counts']['not_run_arms'],27)
    def test_history_survives_global_verifier_failure(self):
        self.run_fixture('instrumentation'); result=M.retained_failure(self.args.output_root,self.args,'missing_terminal_rusage'); self.assertEqual(result['counts']['actual_attempts'],1); self.assertEqual(result['counts']['not_run_arms'],27)
    def test_good_outputs_survive_broken_resource_records(self):
        self.run_fixture('success');result=self.verify_fixture(broken_resource=True)
        self.assertEqual(result['status'],'INVALID');self.assertEqual(result['counts']['output_verified_arms'],28);self.assertEqual(result['counts']['resource_valid_arms'],0)
        self.assertTrue(all(r['output_valid'] and not r['resource_valid'] and r['measurements'] is None for r in result['arms']))
        self.assertTrue(all(r['attempts'][0]['output_valid'] and not r['attempts'][0]['resource_valid'] for r in result['arms']))
    def test_failed_attempt_good_partial_output_is_separate_from_resource_failure(self):
        self.run_fixture('failure_with_output');result=self.verify_fixture(broken_resource=True)
        self.assertEqual(result['status'],'INVALID');self.assertEqual(result['counts']['output_verified_arms'],0);self.assertEqual(result['counts']['resource_valid_arms'],0)
        self.assertTrue(all(r['output'] is None and not r['output_valid'] for r in result['arms']))
        self.assertTrue(all(a['output_valid'] and 'partial_output' in a and not a['resource_valid'] for r in result['arms'] for a in r['attempts']))
    def test_successful_shrink_and_growth_two_named_denominators(self):
        corpus=M.V.load_json(self.args.corpus);pairs=corpus['trial_plan']['planned_pairs']
        for index,pair in enumerate(pairs[:2]):
            for row in self.parent_result['arms']:
                if row['pair_id']==pair['id']:row['output']['bytes']=100 if row['arm']=='control' else 70 if index==0 else pair['original_byte_denominator']['bytes']+100
        self.run_fixture('success');result=self.verify_fixture();self.assertEqual(result['status'],'PASS')
        for index,pair in enumerate(result['pairs'][:2]):
            delta=pair['raw_byte_comparisons'];self.assertEqual(set(delta),{'normalized-same-geometry-jpeg-q85','original-source'})
            self.assertEqual(delta['normalized-same-geometry-jpeg-q85']['denominator_bytes'],100)
            self.assertEqual(delta['original-source']['denominator_bytes'],pair['original_byte_denominator']['bytes'])
            self.assertNotEqual(delta['original-source']['denominator_bytes'],100)
            for item in delta.values():
                self.assertGreater(item['denominator_bytes'],0);self.assertEqual(item['candidate_minus_denominator_bytes'],item['candidate_bytes']-item['denominator_bytes'])
                self.assertEqual(item['candidate_to_denominator_ratio'],item['candidate_bytes']/item['denominator_bytes'])
                if index==0:self.assertLess(item['candidate_minus_denominator_bytes'],0);self.assertLess(item['candidate_to_denominator_ratio'],1)
                else:self.assertGreater(item['candidate_minus_denominator_bytes'],0);self.assertGreater(item['candidate_to_denominator_ratio'],1)

if __name__=='__main__': unittest.main(verbosity=2)
