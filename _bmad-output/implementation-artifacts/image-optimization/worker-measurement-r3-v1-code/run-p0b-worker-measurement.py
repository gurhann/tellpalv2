#!/usr/bin/env python3
"""Execute the approved local canonical-reference worker protocol in isolation."""
import argparse
import ctypes
import gc
import importlib.util
import json
import os
from pathlib import Path
import re
import resource
import selectors
import signal
import struct
import subprocess
import sys
import time

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('worker_measurement_verifier',HERE/'verify-p0b-worker-measurement.py')
M=importlib.util.module_from_spec(spec); spec.loader.exec_module(M)
V=M.V
LIMITS=M.LIMITS


def exclusive(path,raw):
    V.no_symlinks(path)
    fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'wb') as stream:
        stream.write(raw); stream.flush(); os.fsync(stream.fileno())
    os.chmod(path,0o400)


def receipt(path,value): exclusive(path,V.canonical(value)+b'\n')


def mkdir(path):
    V.no_symlinks(path); path.mkdir(mode=0o700,exist_ok=False)


class LimitedWriter:
    def __init__(self,stream): self.stream=stream; self.total=0
    def write(self,value):
        if self.total+len(value)>LIMITS['output_bytes']: raise V.GateFailure('output_byte_limit')
        written=self.stream.write(value); self.total+=written; return written
    def flush(self): return self.stream.flush()


class TaskInfo(ctypes.Structure):
    _fields_=[(name,ctypes.c_uint64) for name in ('virtual','resident','total_user','total_system','threads_user','threads_system')]+[(name,ctypes.c_int32) for name in ('policy','faults','pageins','cow_faults','messages_sent','messages_received','syscalls_mach','syscalls_unix','csw','threadnum','running','priority')]


class ProcSampler:
    def __init__(self):
        if ctypes.sizeof(TaskInfo)!=96: raise V.GateFailure('proc_taskinfo_abi')
        self.lib=ctypes.CDLL('/usr/lib/libproc.dylib',use_errno=True)
        self.lib.proc_pidinfo.argtypes=[ctypes.c_int,ctypes.c_int,ctypes.c_uint64,ctypes.c_void_p,ctypes.c_int]
        self.lib.proc_pidinfo.restype=ctypes.c_int
    def __call__(self,pid):
        row=TaskInfo(); size=self.lib.proc_pidinfo(pid,4,0,ctypes.byref(row),96)
        if size!=96: return {'rss_bytes':None,'threads':None,'error':'proc_pidinfo_return_size_'+str(size)}
        return {'rss_bytes':int(row.resident),'threads':int(row.threadnum),'error':None}


def footprint(root):
    """Each tree deduplicates hardlinks; categories never include another category."""
    result={'logical_bytes':0,'allocated_bytes':0}; seen=set()
    if not root.exists(): return result
    paths=[root] if root.is_file() else root.rglob('*')
    for path in paths:
        V.no_symlinks(path)
        if not path.is_file(): continue
        stat=path.stat(); key=(stat.st_dev,stat.st_ino)
        if key in seen: continue
        seen.add(key); result['logical_bytes']+=stat.st_size; result['allocated_bytes']+=stat.st_blocks*512
    return result


def disk_snapshot(directory,batch):
    # Assign every inode to exactly one category; retained_batch is a separate total.
    result={key:{'logical_bytes':0,'allocated_bytes':0} for key in ('output','tmp','evidence','retained_batch')}
    seen=set()
    paths=sorted((p for p in batch.rglob('*') if p.is_file()),key=lambda p: (0 if p==directory/'output.bin' else 1 if p.is_relative_to(directory/'tmp') else 2,str(p)))
    for path in paths:
        V.no_symlinks(path); stat=path.stat(); key=(stat.st_dev,stat.st_ino)
        if key in seen: continue
        seen.add(key); values={'logical_bytes':stat.st_size,'allocated_bytes':stat.st_blocks*512}
        category='output' if path==directory/'output.bin' else 'tmp' if path.is_relative_to(directory/'tmp') else 'evidence' if path.is_relative_to(directory) else None
        for unit,value in values.items():
            result['retained_batch'][unit]+=value
            if category: result[category][unit]+=value
    return result


def encode_worker(request,path):
    meter=M.Meter(request); meter.event('bootstrap_start'); M.check_request(request,path)
    pair=request['pair']; width,height=pair['geometry']['width'],pair['geometry']['height']
    reference=Path(request['reference']); expected=len(V.P0A.MAGIC)+8+width*height*4
    V.no_symlinks(reference)
    if reference.stat().st_size!=expected: raise V.GateFailure('reference_length')
    output=V.safe_batch(Path(request['output']),[Path(v) for v in request['inputs'].values()],new=True)
    settings={k:v for k,v in pair[request['arm']+'_settings'].items() if k not in ('mode','profile_handling')}
    meter.event('bootstrap_end'); meter.wait('idle')
    def read():
        with reference.open('rb') as stream: raw=stream.read(expected+1)
        if len(raw)!=expected: raise V.GateFailure('reference_length')
        return raw
    raw=meter.phase('canonical_read',read)
    def project():
        if V.sha(raw)!=pair['reference_pixel_sha256'] or raw[:len(V.P0A.MAGIC)]!=V.P0A.MAGIC: raise V.GateFailure('reference_hash_or_magic')
        if struct.unpack('>II',raw[len(V.P0A.MAGIC):len(V.P0A.MAGIC)+8])!=(width,height): raise V.GateFailure('reference_geometry')
        pixels=raw[len(V.P0A.MAGIC)+8:]
        if any(a!=255 for a in memoryview(pixels)[3::4]): raise V.GateFailure('reference_not_opaque')
        rgba=V.Image.frombytes('RGBA',(width,height),pixels)
        try: return rgba.convert('RGB')
        finally: rgba.close()
    image=meter.phase('projection',project)
    def encode():
        fd=os.open(output,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'wb') as stream:
            image.save(LimitedWriter(stream),**settings); stream.flush(); os.fsync(stream.fileno())
    meter.phase('encoding',encode)
    os.chmod(output,0o400)
    def cleanup():
        nonlocal raw,image
        image.close(); image=None; raw=None; gc.collect()
    meter.finish(cleanup); print(json.dumps({'status':'PASS'}))


def supervise(command,directory,batch,attempt_id,stage,*,sampler=None,child_seconds=None,batch_deadline=None):
    """Drain bounded pipes and reap exactly once with wait4; never Popen.poll."""
    sampler=sampler or ProcSampler(); start=time.perf_counter_ns(); usage=resource.getrusage(resource.RUSAGE_SELF)
    lifecycle={'attempt_id':attempt_id,'stage':stage,'pid':None,'supervisor_pid':os.getpid(),'start_ns':start,'end_ns':None,
               'status':'spawn_failure','terminal':None,'term_sent':False,'kill_sent':False,'deadline_expired':False,'deadline_ns':start+int((child_seconds or LIMITS['child_seconds'])*10**9),'term_ns':None,'kill_ns':None,'units':M.UNITS}
    paths={suffix:directory/(stage+'.'+suffix) for suffix in ('stdout','stderr','events.jsonl','samples.jsonl')}
    streams={suffix:os.fdopen(os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'wb',buffering=0) for suffix,path in paths.items()}
    counts={suffix:0 for suffix in streams}; events=[]; samples=[]; stdout=bytearray(); event_buffer=b''; selector=selectors.DefaultSelector()
    child=None; terminal=None; failure=None; sent_at=None; endpoint=None; next_tick=0; observer_wall=0; completed_event=False; reaped_at=None
    def write(key,raw):
        cap=LIMITS['log_bytes'] if key in ('stdout','stderr') else LIMITS['telemetry_bytes']
        if counts[key]+len(raw)>cap: raise V.GateFailure('bounded_stream_overflow')
        streams[key].write(raw); counts[key]+=len(raw)
    def capture(kind,planned,event_seq=None,tick=None):
        nonlocal observer_wall,terminal,reaped_at
        if len(samples)>=LIMITS['samples']: raise V.GateFailure('sample_record_overflow')
        before=time.perf_counter_ns(); observation=sampler(child.pid); captured=time.perf_counter_ns()
        row={'attempt_id':attempt_id,'stage':stage,'pid':child.pid,'supervisor_pid':os.getpid(),'seq':len(samples),
             'kind':kind,'planned_ns':planned,'t_ns':captured,'lateness_ns':max(0,captured-planned),
             'capture_cost_ns':captured-before,'event_seq':event_seq,'tick':tick,**observation,'disk':disk_snapshot(directory,batch)}
        observer_wall+=time.perf_counter_ns()-before; samples.append(row); write('samples.jsonl',V.canonical(row)+b'\n')
        if row['disk']['retained_batch']['logical_bytes']>LIMITS['batch_bytes']: raise V.GateFailure('disk_cap')
        if observation['error']:
            reaped,status,rusage=os.wait4(child.pid,os.WNOHANG)
            if reaped:
                terminal={'pid':reaped,'exit_code':os.WEXITSTATUS(status) if os.WIFEXITED(status) else None,'signal':os.WTERMSIG(status) if os.WIFSIGNALED(status) else None,'user_seconds':rusage.ru_utime,'system_seconds':rusage.ru_stime,'maxrss_bytes':rusage.ru_maxrss,'reaped_ns':time.perf_counter_ns()}
                lifecycle['terminal']=terminal; reaped_at=time.perf_counter_ns(); child.returncode=terminal['exit_code'] if terminal['signal'] is None else -terminal['signal']
                if terminal['exit_code']==0: raise V.GateFailure('live_sample_failed')
            else: raise V.GateFailure('live_sample_failed')
    def terminate(reason):
        nonlocal failure,sent_at
        if failure is None: failure=reason
        if child is not None and terminal is None and not lifecycle['term_sent']:
            try: os.killpg(child.pid,signal.SIGTERM)
            except ProcessLookupError: pass
            lifecycle['term_sent']=True; sent_at=time.perf_counter_ns(); lifecycle['term_ns']=sent_at
    read_fd=write_fd=None
    try:
        read_fd,write_fd=os.pipe()
        env=dict(os.environ,TMPDIR=str(directory/'tmp'),P0B_EVENT_FD=str(write_fd))
        try:
            child=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,pass_fds=(write_fd,),start_new_session=True)
        except OSError:
            write('stderr',b'subprocess_spawn_failure\n')
        finally: os.close(write_fd); write_fd=None
        if child:
            lifecycle['pid']=child.pid; lifecycle['status']='failure'
            for file,key in ((child.stdout,'stdout'),(child.stderr,'stderr'),(read_fd,'events.jsonl')):
                fd=file if isinstance(file,int) else file.fileno(); os.set_blocking(fd,False); selector.register(fd,selectors.EVENT_READ,key)
            deadline=start+int((child_seconds or LIMITS['child_seconds'])*10**9)
            while terminal is None or selector.get_map():
                now=time.perf_counter_ns()
                if reaped_at and now-reaped_at>2*10**9 and selector.get_map():
                    terminate('descendant_pipe_linger'); break
                if terminal is None:
                    if batch_deadline is not None and now>=batch_deadline: terminate('batch_deadline')
                    elif now>=deadline:
                        lifecycle['deadline_expired']=True; terminate('timeout')
                    if sent_at and not lifecycle['kill_sent'] and now-sent_at>=2*10**9:
                        try: os.killpg(child.pid,signal.SIGKILL)
                        except ProcessLookupError: pass
                        lifecycle['kill_sent']=True; lifecycle['kill_ns']=time.perf_counter_ns()
                    if failure is None and not completed_event:
                        try:
                            if now>=start+next_tick*10**9:
                                capture('periodic',start+next_tick*10**9,tick=next_tick); next_tick+=1
                            if endpoint and terminal is None and now>=endpoint[1]:
                                capture(endpoint[0],endpoint[1]); child.stdin.write(b'continue\n'); child.stdin.flush(); endpoint=None
                        except Exception as exc: terminate(str(exc) if isinstance(exc,V.GateFailure) else 'instrumentation_failure')
                for key,_ in selector.select(0.01):
                    try: raw=os.read(key.fd,65536)
                    except BlockingIOError: continue
                    if not raw: selector.unregister(key.fd); continue
                    try:
                        write(key.data,raw)
                        if key.data=='stdout': stdout.extend(raw)
                        if key.data=='events.jsonl':
                            event_buffer+=raw
                            while b'\n' in event_buffer:
                                line,event_buffer=event_buffer.split(b'\n',1)
                                def unique(pairs):
                                    out={}
                                    for k,v in pairs:
                                        if k in out: raise V.GateFailure('duplicate_event_key')
                                        out[k]=v
                                    return out
                                event=json.loads(line,object_pairs_hook=unique,parse_constant=lambda _: (_ for _ in ()).throw(V.GateFailure('nonfinite_event')))
                                if set(event)!={'attempt_id','stage','pid','seq','name','phase','t_ns','user_seconds','system_seconds'}: raise V.GateFailure('event_fields')
                                for field in ('seq','pid','t_ns'): M.number(event[field],integer=True)
                                for field in ('user_seconds','system_seconds'): M.number(event[field])
                                names={'bootstrap_start','bootstrap_end','idle_start','idle_end','phase_start','phase_end','settle_start','settle_end','complete'}
                                phases={'canonical_read','projection','encoding','cleanup'} if stage=='encode' else {'output_verification','cleanup'}
                                if event['name'] not in names or (event['phase'] not in phases if event['name'] in ('phase_start','phase_end') else event['phase'] is not None): raise V.GateFailure('malformed_event')
                                if events and (event['t_ns']<events[-1]['t_ns'] or event['user_seconds']<events[-1]['user_seconds'] or event['system_seconds']<events[-1]['system_seconds']): raise V.GateFailure('event_order')
                                if len(events)>=LIMITS['events']: raise V.GateFailure('event_record_overflow')
                                if event['seq']!=len(events) or event['pid']!=child.pid or event['attempt_id']!=attempt_id or event['stage']!=stage: raise V.GateFailure('event_ownership_or_replay')
                                if not start<=event['t_ns']<=time.perf_counter_ns(): raise V.GateFailure('clock_domain_mismatch')
                                events.append(event)
                                if terminal is None and failure is None:
                                    capture('boundary',event['t_ns'],event_seq=event['seq'])
                                    if terminal is None and event['name']!='phase_start': child.stdin.write(b'event\n'); child.stdin.flush()
                                    if event['name']=='complete': completed_event=True
                                    if event['name']=='idle_start': endpoint=('idle_endpoint',event['t_ns']+5*10**9)
                                    elif event['name']=='settle_start': endpoint=('settled_endpoint',next(e['t_ns'] for e in reversed(events) if e['name']=='phase_end' and e['phase']=='cleanup')+30*10**9)
                    except Exception as exc: terminate(str(exc) if isinstance(exc,V.GateFailure) else 'malformed_event')
                if terminal is None:
                    reaped,status,rusage=os.wait4(child.pid,os.WNOHANG)
                    if reaped:
                        terminal={'pid':reaped,'exit_code':os.WEXITSTATUS(status) if os.WIFEXITED(status) else None,
                                  'signal':os.WTERMSIG(status) if os.WIFSIGNALED(status) else None,
                                  'user_seconds':rusage.ru_utime,'system_seconds':rusage.ru_stime,'maxrss_bytes':rusage.ru_maxrss,'reaped_ns':time.perf_counter_ns()}
                        child.returncode=terminal['exit_code'] if terminal['signal'] is None else -terminal['signal']
                        lifecycle['terminal']=terminal; reaped_at=time.perf_counter_ns()
            lifecycle['status']='timeout' if failure=='timeout' else 'instrumentation_failure' if failure else 'success' if terminal['exit_code']==0 else 'failure'
            if terminal['exit_code']==0 and failure is None:
                # A successful process must have all endpoints/stages, checked before retry decisions.
                M.derive_child(events,samples,dict(lifecycle,end_ns=time.perf_counter_ns(),collector_user_seconds=0,collector_system_seconds=0,observer_wall_ns=observer_wall),attempt_id,stage)
    except KeyboardInterrupt:
        terminate('cancellation')
    except Exception as exc:
        terminate(str(exc) if isinstance(exc,V.GateFailure) else 'instrumentation_failure')
    finally:
        if child and terminal is None:
            terminate(failure or 'instrumentation_failure')
            while True:
                pid,status,rusage=os.wait4(child.pid,os.WNOHANG)
                if pid: break
                if time.perf_counter_ns()-sent_at>=2*10**9:
                    try: os.killpg(child.pid,signal.SIGKILL)
                    except ProcessLookupError: pass
                    lifecycle['kill_sent']=True; lifecycle['kill_ns']=time.perf_counter_ns()
                time.sleep(0.01)
            lifecycle['terminal']={'pid':pid,'exit_code':os.WEXITSTATUS(status) if os.WIFEXITED(status) else None,'signal':os.WTERMSIG(status) if os.WIFSIGNALED(status) else None,'user_seconds':rusage.ru_utime,'system_seconds':rusage.ru_stime,'maxrss_bytes':rusage.ru_maxrss,'reaped_ns':time.perf_counter_ns()}
            child.returncode=lifecycle['terminal']['exit_code'] if lifecycle['terminal']['signal'] is None else -lifecycle['terminal']['signal']
        if failure and failure!='timeout': lifecycle['status']='instrumentation_failure'
        lifecycle['end_ns']=time.perf_counter_ns(); after=resource.getrusage(resource.RUSAGE_SELF)
        lifecycle.update(collector_user_seconds=after.ru_utime-usage.ru_utime,collector_system_seconds=after.ru_stime-usage.ru_stime,observer_wall_ns=observer_wall,failure=failure)
        selector.close()
        if read_fd is not None: os.close(read_fd)
        if write_fd is not None: os.close(write_fd)
        if child:
            for stream in (child.stdin,child.stdout,child.stderr): stream.close()
        for key,stream in streams.items(): stream.flush(); os.fsync(stream.fileno()); stream.close(); os.chmod(paths[key],0o400)
        receipt(directory/(stage+'.lifecycle.json'),lifecycle)
        if (directory/'output.bin').exists(): V.no_symlinks(directory/'output.bin'); os.chmod(directory/'output.bin',0o400)
    files={path.name:V.file_identity(path,path.name) for path in paths.values()}
    path=directory/(stage+'.lifecycle.json'); files[path.name]=V.file_identity(path,path.name)
    return {'status':lifecycle['status'],'files':files,'failure':failure,'stdout':bytes(stdout)}


def bounded(command,directory,name,*,batch_deadline=None):
    """Bound drains, owned-group termination, descendant pipes, and mandatory reap."""
    start=time.perf_counter_ns(); proc=None; selector=selectors.DefaultSelector()
    data={'stdout':bytearray(),'stderr':bytearray()}; terminal=None; sent=None; reaped_at=None; failure=None
    try:
        proc=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,start_new_session=True)
        for stream,key in ((proc.stdout,'stdout'),(proc.stderr,'stderr')):
            os.set_blocking(stream.fileno(),False); selector.register(stream,selectors.EVENT_READ,key)
        while terminal is None or selector.get_map():
            now=time.perf_counter_ns()
            if terminal is None and now-start>=LIMITS['preflight_seconds']*10**9: failure='guard_deadline'
            if terminal is None and batch_deadline is not None and now>=batch_deadline: failure='batch_deadline'
            if reaped_at and now-reaped_at>2*10**9 and selector.get_map(): failure='descendant_pipe_linger'; break
            if failure and sent is None:
                try: os.killpg(proc.pid,signal.SIGTERM)
                except ProcessLookupError: pass
                sent=now
            if terminal is None and sent and now-sent>2*10**9:
                try: os.killpg(proc.pid,signal.SIGKILL)
                except ProcessLookupError: pass
            for key,_ in selector.select(0.02):
                chunk=os.read(key.fileobj.fileno(),65536)
                if not chunk: selector.unregister(key.fileobj); continue
                if len(data[key.data])+len(chunk)<=LIMITS['log_bytes']: data[key.data].extend(chunk)
                else: failure='guard_log_overflow'
            if terminal is None:
                pid,status,usage=os.wait4(proc.pid,os.WNOHANG)
                if pid:
                    terminal={'pid':pid,'exit_code':os.WEXITSTATUS(status) if os.WIFEXITED(status) else None,'signal':os.WTERMSIG(status) if os.WIFSIGNALED(status) else None,
                              'user_seconds':usage.ru_utime,'system_seconds':usage.ru_stime,'maxrss_bytes':usage.ru_maxrss}
                    reaped_at=time.perf_counter_ns(); proc.returncode=terminal['exit_code'] if terminal['signal'] is None else -terminal['signal']
    except BaseException as exc:
        failure='cancellation' if isinstance(exc,KeyboardInterrupt) else 'bounded_guard_failure'
    finally:
        if proc and (terminal is None or failure):
            try: os.killpg(proc.pid,signal.SIGTERM)
            except ProcessLookupError: pass
            grace=time.perf_counter_ns()
            while terminal is None:
                pid,status,usage=os.wait4(proc.pid,os.WNOHANG)
                if pid:
                    terminal={'pid':pid,'exit_code':os.WEXITSTATUS(status) if os.WIFEXITED(status) else None,'signal':os.WTERMSIG(status) if os.WIFSIGNALED(status) else None,
                              'user_seconds':usage.ru_utime,'system_seconds':usage.ru_stime,'maxrss_bytes':usage.ru_maxrss}
                    proc.returncode=terminal['exit_code'] if terminal['signal'] is None else -terminal['signal']; break
                if time.perf_counter_ns()-grace>=2*10**9:
                    try: os.killpg(proc.pid,signal.SIGKILL)
                    except ProcessLookupError: pass
                time.sleep(.01)
            if failure:
                try: os.killpg(proc.pid,signal.SIGKILL)
                except ProcessLookupError: pass
        selector.close()
        if proc: proc.stdout.close(); proc.stderr.close()
        for key,raw in data.items(): exclusive(directory/(name+'.'+key),bytes(raw))
        receipt(directory/(name+'.lifecycle.json'),{'start_ns':start,'end_ns':time.perf_counter_ns(),'terminal':terminal,'failure':failure})
    if failure or terminal is None or terminal['exit_code']!=0: raise V.GateFailure(failure or 'bounded_guard_failed')
    return json.loads(data['stdout'])


def guard(args,scope,directory,*,batch_deadline=None):
    observed=bounded([sys.executable,str(HERE/'verify-p0b-encoder-pilot.py'),'--integrity-scope',str(Path(scope['parent_batch'])/'scope.json'),*V.cli_inputs(args)],directory,'integrity',batch_deadline=batch_deadline)
    if observed.get('status')!='PASS' or not V.same(M.code_identity(),scope['measurement_code']) or not V.same(M.environment(),scope['environment']): raise V.GateFailure('prerequisite_integrity_failed')
    if not V.same(sdk_provenance(),scope['sdk_provenance']): raise V.GateFailure('prerequisite_integrity_failed')
    for name,digest in scope['parent_output_files'].items():
        if V.stream_hash(V.no_symlinks(Path(scope['parent_batch'])/name))[0]!=digest: raise V.GateFailure('prerequisite_integrity_failed')
    for name,identity in scope['parent_bindings'].items():
        path=Path(scope['parent_batch'])/name if name in ('scope.json','execution-result.json') else HERE/name
        if V.stream_hash(path)[0]!=identity: raise V.GateFailure('prerequisite_integrity_failed')


def sdk_provenance():
    base=Path('/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk')
    paths=('usr/include/libproc.h','usr/include/sys/proc_info.h','usr/include/sys/stat.h','usr/share/man/man2/getrusage.2')
    return [V.file_identity(base/name,name) for name in paths]


def run_batch(args):
    if not re.fullmatch(r'[a-z0-9][a-z0-9._-]{0,95}',args.batch_id): raise V.GateFailure('unsafe_batch_identity')
    batch=V.safe_batch(args.output_root,V.input_paths(args)+[args.parent_batch],new=True)
    corpus=V.load_json(args.corpus); schedule=V.make_schedule(corpus); started=time.perf_counter_ns(); deadline=started+LIMITS['batch_seconds']*10**9
    mkdir(batch); mkdir(batch/'attempts'); mkdir(batch/'guards')
    inputs={k:str(getattr(args,k)) for k in ('source_root','reference_root','preparation_receipt','freeze_manifest','corpus','protocol')}
    receipt(batch/'private-inputs.json',inputs); receipt(batch/'planned-ledger.json',{'schedule':schedule})
    rows=[{'pair_id':r['pair_id'],'arm':r['arm'],'status':'not_run','attempts':[]} for r in schedule]
    result={'schema_version':1,'artifact':'p0b-worker-execution','batch_id':args.batch_id,'scope_sha256':None,'arms':rows,'stop_reason':None,'input_snapshot_after':None}
    try:
        env=M.environment(); M.validate_environment(env); sampler=ProcSampler()
        if sampler(os.getpid())['error'] is not None: raise V.GateFailure('rss_api_admission_failed')
        evidence=bounded([sys.executable,str(HERE/'verify-p0b-encoder-pilot.py'),'--preflight',*V.cli_inputs(args)],batch/'guards','preflight',batch_deadline=deadline)
        if evidence.get('status')!='PASS': raise V.GateFailure('prerequisite_failure')
        parent=bounded([sys.executable,str(HERE/'verify-p0b-encoder-pilot.py'),'--batch-root',str(args.parent_batch),*V.cli_inputs(args)],batch/'guards','parent-reverification',batch_deadline=deadline)
        if parent.get('status')!='PASS' or not V.same(parent,V.load_json(HERE/'p0b-encoder-pilot.json')) or V.stream_hash(HERE/'p0b-encoder-pilot.json')[0]!=M.PARENT_PUBLIC_HASH or V.stream_hash(args.parent_batch/'scope.json')[0]!=M.PARENT_SCOPE_HASH:
            raise V.GateFailure('parent_evidence_drift')
        scope={'schema_version':1,'artifact':'p0b-worker-scope','batch_id':args.batch_id,'batch_start_ns':started,
               'parent_batch':str(args.parent_batch),'parent_scope':V.load_json(args.parent_batch/'scope.json'),
               'parent_bindings':{name:V.stream_hash(args.parent_batch/name if name in ('scope.json','execution-result.json') else HERE/name)[0] for name in ('scope.json','execution-result.json','p0b-encoder-pilot.json','p0b-encoder-pilot.md')},
               'parent_output_files':{str(Path('attempts')/next(a['id'] for a in r['attempts'] if a['status']=='success')/'output.bin'):r['output']['sha256'] for r in parent['arms']},
               'measurement_code':M.code_identity(),'environment':env,'sdk_provenance':sdk_provenance(),
               'limits':LIMITS,'units':M.UNITS,'schedule':schedule,'limitations':M.LIMITATIONS,'preflight':evidence}
        receipt(batch/'scope.json',scope); result['scope_sha256']=V.stream_hash(batch/'scope.json')[0]
        parent_outputs={(r['pair_id'],r['arm']):r['output'] for r in parent['arms']}
        deadline=started+LIMITS['batch_seconds']*10**9
        pairs={p['id']:p for p in corpus['trial_plan']['planned_pairs']}
        for index,row in enumerate(rows):
            planned=schedule[index]; pair=pairs[row['pair_id']]
            for attempt_number,ident in enumerate((planned['initial_id'],planned['retry_id'])):
                if time.perf_counter_ns()>=deadline: raise V.GateFailure('batch_deadline')
                guard_directory=batch/'guards'/ident; mkdir(guard_directory)
                guard(args,scope,guard_directory,batch_deadline=deadline)
                if footprint(batch)['logical_bytes']>=LIMITS['batch_bytes']: raise V.GateFailure('disk_cap')
                directory=batch/'attempts'/ident; mkdir(directory); mkdir(directory/'tmp')
                kind='initial' if attempt_number==0 else 'retry'; t_ns=time.perf_counter_ns()
                receipt(directory/'started.json',dict(V.started_receipt(planned,ident,kind,result['scope_sha256']),t_ns=t_ns))
                request=V.expected_request(args,batch,directory,pair,row['arm']); request.update(attempt_id=ident,stage='encode'); receipt(directory/'request.json',request)
                decoded=None
                attempt={'id':ident,'kind':kind,'status':'failed','failure':None,'children':{},'end_ns':None}
                stop=False
                try:
                    encoded=supervise([sys.executable,str(Path(__file__).resolve()),'--encode-request',str(directory/'request.json')],directory,batch,ident,'encode',sampler=sampler,batch_deadline=deadline)
                    attempt['children']['encode']={k:v for k,v in encoded.items() if k!='stdout'}
                    if encoded['status']=='instrumentation_failure': attempt['failure']=encoded['failure'] if encoded['failure'] in ('batch_deadline','disk_cap','cancellation') else 'instrumentation_failure'; stop=True
                    elif encoded['status']!='success': attempt['failure']='encode_'+encoded['status']
                    else:
                        decoded=supervise([sys.executable,str(HERE/'verify-p0b-worker-measurement.py'),'--meter-request',str(directory/'request.json')],directory,batch,ident,'verify',sampler=sampler,batch_deadline=deadline)
                        attempt['children']['verify']={k:v for k,v in decoded.items() if k!='stdout'}
                        if decoded['status']=='instrumentation_failure': attempt['failure']=decoded['failure'] if decoded['failure'] in ('batch_deadline','disk_cap','cancellation') else 'instrumentation_failure'; stop=True
                        elif decoded['status']!='success': attempt['failure']='readback_'+decoded['status']
                        else:
                            observation=json.loads(decoded['stdout'])['observation']
                            if not V.same(observation,parent_outputs[(row['pair_id'],row['arm'])]): attempt['failure']='predecessor_output_mismatch'
                            else: attempt['status']='success'
                    # A child guard failure must never become an ordinary encode retry.
                    for child_result in (encoded,decoded):
                        if child_result:
                            try: category=json.loads(child_result['stdout']).get('category')
                            except (ValueError,TypeError): category=None
                            if category=='prerequisite_integrity_failed': attempt['failure']=category; stop=True
                except KeyboardInterrupt: attempt['failure']='cancellation'; stop=True
                except Exception: attempt['failure']='instrumentation_failure'; stop=True
                attempt['end_ns']=time.perf_counter_ns(); receipt(directory/'completed.json',attempt); row['attempts'].append(attempt); row['status']=attempt['status']
                receipt(directory/'final-disk.json',{'snapshot':disk_snapshot(directory,batch),'t_ns':time.perf_counter_ns(),'excluded_names':['final-disk.json']})
                print(json.dumps({'arm':index+1,'scheduled_arms':28,'attempt':ident,'status':attempt['status'],'failure':attempt['failure']}),flush=True)
                if stop: raise V.GateFailure(attempt['failure'])
                if attempt['status']=='success': break
        final_directory=batch/'guards'/'final'; mkdir(final_directory)
        guard(args,scope,final_directory,batch_deadline=deadline)
        after=bounded([sys.executable,str(HERE/'verify-p0b-encoder-pilot.py'),'--preflight',*V.cli_inputs(args)],final_directory,'postflight',batch_deadline=deadline)
        result['input_snapshot_after']=after['input_snapshot']
        if not V.same(after['input_snapshot'],evidence['input_snapshot']): raise V.GateFailure('prerequisite_integrity_failed')
    except (Exception,KeyboardInterrupt) as exc:
        result['stop_reason']=str(exc) if isinstance(exc,V.GateFailure) else 'cancellation' if isinstance(exc,KeyboardInterrupt) else 'batch_failure:'+type(exc).__name__
        receipt(batch/'stopped.json',{'category':result['stop_reason'],'t_ns':time.perf_counter_ns()})
        # Preserve final observed inputs even after an instrumentation stop when readable.
        try: result['input_snapshot_after']=V.input_snapshot(args,corpus)
        except Exception: pass
    result['end_ns']=time.perf_counter_ns(); result['retained_disk']=footprint(batch)
    receipt(batch/'execution-result.json',result)
    final_files=[V.file_identity(p,str(p.relative_to(batch))) for p in sorted(batch.rglob('*')) if p.is_file()]
    receipt(batch/'final-disk.json',{'snapshot':footprint(batch),'files':final_files,'t_ns':time.perf_counter_ns(),'excluded_names':['final-disk.json']})
    print(json.dumps({'batch_id':args.batch_id,'stop_reason':result['stop_reason'],'attempts':sum(len(r['attempts']) for r in rows)}),flush=True)
    return 0 if result['stop_reason'] is None else 2


def main():
    parser=V.parser(); parser.add_argument('--output-root',type=Path); parser.add_argument('--parent-batch',type=Path); parser.add_argument('--batch-id',default='worker-measurement-r3-v1'); parser.add_argument('--encode-request',type=Path)
    args=parser.parse_args()
    try:
        if args.encode_request: encode_worker(V.load_json(V.private_path(args.encode_request)),args.encode_request); return 0
        return run_batch(args)
    except Exception as exc:
        print(json.dumps({'status':'INVALID','category':str(exc) if isinstance(exc,V.GateFailure) else 'worker_failure:'+type(exc).__name__})); return 2

if __name__=='__main__': raise SystemExit(main())
