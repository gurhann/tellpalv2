/** Real controlled-detail HTTP/VM probes. Tokens, signed URLs and debugger URLs never enter evidence. */
import fs from 'node:fs';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
// Network errors can contain signed URLs. Emit a fixed failure without a stack or response body.
const failClosed=()=>{process.stderr.write('Controlled detail probe failed; no sensitive error data emitted\n');process.exit(1);};
process.on('uncaughtException',failClosed);
process.on('unhandledRejection',failClosed);
const args=Object.fromEntries(process.argv.slice(2).map((value,index,all)=>value.startsWith('--')?[value.slice(2),all[index+1]]:null).filter(Boolean));
const base=args['base-url']??'http://127.0.0.1:8082';
const endpoint=new URL(base);
if(!['127.0.0.1','localhost','::1'].includes(endpoint.hostname)||!['http:','https:'].includes(endpoint.protocol))throw Error('Only an isolated local backend is admitted');
if(!args.token||!args.output)throw Error('Provide private --token and redacted --output');
const token=fs.readFileSync(args.token,'utf8').trim();
if(!token)throw Error('Private token file is empty');
const account=JSON.parse(fs.readFileSync(fileURLToPath(new URL('../mobile-test-account.json',import.meta.url)),'utf8'));
const identity=JSON.parse(Buffer.from(token.split('.')[1]??'', 'base64url').toString('utf8'));
// This selection guard supplements the backend's real Firebase verification.
if(identity.sub!==account.firebaseUid)throw Error('Only the designated test account is admitted');
async function readBytes(response,maximum){
 const reader=response.body.getReader();const chunks=[];let total=0;
 try{for(;;){const next=await reader.read();if(next.done)break;total+=next.value.length;if(total>maximum)throw Error('Response byte bound exceeded');chunks.push(next.value);}return Buffer.concat(chunks,total);}
 finally{await reader.cancel();}
}
async function request(path,bearer=token){
 const response=await fetch(new URL(path,base),{headers:bearer?{Authorization:`Bearer ${bearer}`}:{},signal:AbortSignal.timeout(15000)});
 const bytes=await readBytes(response,1048576);let body;try{body=JSON.parse(bytes);}catch{throw Error('HTTP JSON contract failed');}
 return {status:response.status,body};
}
function requireValue(condition,message){if(!condition)throw Error(message);}
const language=args.language??'tr';
requireValue(['tr','en','pt'].includes(language),'Unsupported UI language');
const home=await request(`/api/test/library?lang=${language}`);
requireValue(home.status===200,'Current Library admission failed');
const row=args.category?home.body.rows.find(item=>item.categoryId===Number(args.category)):home.body.rows[0];
requireValue(row,'Current category is unavailable');
const contentId=Number(args.content??row.items[0].contentId),categoryId=row.categoryId,contentLanguage=row.contentLanguage;
const path=`/api/test/library/stories/${contentId}?`+new URLSearchParams({categoryId:String(categoryId),lang:language,contentLang:contentLanguage,experience:'READING'});
const start=Date.now();const selected=await request(path);const metadataMs=Date.now()-start;
requireValue(selected.status===200,'Current curated detail did not return success');
const detail=selected.body;
requireValue(detail.schemaVersion===1&&detail.contentId===contentId&&detail.categoryId===categoryId&&detail.requestedLanguage===language&&detail.contentLanguage===contentLanguage&&detail.experienceType==='READING'&&detail.type==='STORY','Exact detail identity failed');
requireValue(detail.isLocked===(!detail.isFree&&detail.audience!=='SUBSCRIBER'),'Server access presentation failed');
const ids=new Set([contentId]);
for(const item of detail.recommendations){requireValue(!ids.has(item.contentId)&&item.contentLanguage===contentLanguage&&item.type==='STORY'&&item.experienceType==='READING','Recommendation identity failed');ids.add(item.contentId);}
let artwork=null;
if(detail.hero){
 const timer=Date.now();const response=await fetch(detail.hero.url,{signal:AbortSignal.timeout(10000)});
 requireValue(response.status===200,'Actual hero delivery failed');
 requireValue((Number(response.headers.get('content-length'))||0)<=20971520,'Actual hero headers exceed byte bound');
 const bytes=await readBytes(response,20971520),sha=crypto.createHash('sha256').update(bytes).digest('hex');
 requireValue(bytes.length===detail.hero.byteSize&&sha===detail.hero.checksumSha256,'Actual current-source checksum/size failed');
 artwork={assetId:detail.hero.assetId,checksumSha256:sha,mimeType:detail.hero.mimeType,byteSize:bytes.length,width:detail.hero.width,height:detail.hero.height,transferMs:Date.now()-timer};
}
const faults=[];
for(const [name,probe,bearer,status,errorCode] of [['missing-bearer',path,null,401,'firebase_auth_error'],['invalid-bearer',path,'invalid-fixture-token',401,'firebase_auth_error'],['unsupported-experience',path.replace('experience=READING','experience=AUDIO_STORY'),token,400,'invalid_request'],['missing-language',path.replace(`lang=${language}&`,''),token,400,'invalid_request'],['unavailable-story',path.replace(`/stories/${contentId}`, '/stories/9223372036854775807'),token,404,'content_not_found']]){
 const result=await request(probe,bearer);requireValue(result.status===status&&result.body.errorCode===errorCode,'HTTP fault mapping failed');faults.push({name,status,errorCode});
}
let native=null;
if(args['vm-log']){
 requireValue(typeof WebSocket==='function','Node 22 or newer is required for VM probes');
 const log=fs.readFileSync(args['vm-log'],'utf8');const urls=[...log.matchAll(/https?:\/\/127\.0\.0\.1:\d+\/[^\s]+/g)];requireValue(urls.length>0,'VM service is unavailable');
 const socket=new WebSocket(urls[0][0].replace(/^http/,'ws').replace(/\/$/,'')+'/ws');
 let sequence=0;const pending=new Map();
 socket.addEventListener('message',event=>{const message=JSON.parse(event.data);const entry=pending.get(message.id);if(!entry)return;pending.delete(message.id);message.error?entry.reject(Error('VM method failed')):entry.resolve(message.result);});
 await new Promise((resolve,reject)=>{socket.addEventListener('open',resolve,{once:true});socket.addEventListener('error',()=>reject(Error('VM connection failed')),{once:true});setTimeout(()=>reject(Error('VM connection deadline reached')),15000).unref();});
 function rpc(method,parameters={}){return new Promise((resolve,reject)=>{const id=++sequence;pending.set(id,{resolve,reject});socket.send(JSON.stringify({jsonrpc:'2.0',id,method,params:parameters}));setTimeout(()=>{if(pending.delete(id))reject(Error('VM deadline reached'));},20000).unref();});}
 try{
  const vm=await rpc('getVM'),isolate=vm.isolates.find(item=>item.name==='main')??vm.isolates[0],params={isolateId:isolate.id};
  await rpc('ext.tellpal.library.configure',{...params,baseUrl:base,idToken:token,language,accessIdentity:'existing-authorized-fixture'});
  const admissionDeadline=Date.now()+15000;
  for(;;){try{await rpc('ext.tellpal.library.observe',params);break;}catch{if(Date.now()>admissionDeadline)throw Error('Library extension admission deadline reached');await new Promise(resolve=>setTimeout(resolve,100));}}
  await rpc('ext.tellpal.library.context',{...params,language,textScale:args.scale??'1.0'});
  await rpc('ext.tellpal.library.detailAction',{...params,action:'open',categoryId:String(categoryId),contentId:String(contentId),uiLanguage:language,contentLanguage});
  const deadline=Date.now()+20000;
  for(;;){const result=await rpc('ext.tellpal.library.detail',params);native=typeof result.json==='string'?JSON.parse(result.json):result;
   if(native.status==='loaded'&&native.heroStatus!=='loading')break;if(Date.now()>deadline)throw Error('Native detail deadline reached');await new Promise(resolve=>setTimeout(resolve,100));}
  requireValue(native.contentId===contentId&&native.categoryId===categoryId&&native.uiLanguage===language&&native.contentLanguage===contentLanguage&&native.title===detail.title&&native.audience===detail.audience&&native.isFree===detail.isFree&&native.isLocked===detail.isLocked,'Native/current-HTTP identity/access comparison failed');
  requireValue(JSON.stringify(native.recommendations)===JSON.stringify(detail.recommendations.map(item=>({contentId:item.contentId,contentLanguage:item.contentLanguage,isLocked:item.isLocked}))),'Native/current-HTTP recommendation order failed');
  requireValue(detail.hero?native.heroStatus==='loaded'&&native.heroAssetId===detail.hero.assetId:native.heroStatus==='error','Native hero outcome failed');
  await rpc('ext.tellpal.library.detailAction',{...params,action:'back'});
 }finally{socket.close();}
}
const evidence={executed:true,recordedAt:new Date().toISOString(),baselineCommit:execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),uiLanguage:language,contentLanguage,categoryId,contentId,audience:detail.audience,isFree:detail.isFree,isLocked:detail.isLocked,title:detail.title,ageRange:detail.ageRange,durationMinutes:detail.durationMinutes,summaryPresent:detail.summary!=null,credits:detail.credits,recommendations:detail.recommendations.map(item=>({contentId:item.contentId,contentLanguage:item.contentLanguage,isLocked:item.isLocked})),metadataMs,artwork,faults,native,notCovered:['verified-non-allowlisted-UID','both-server-audiences-unless-separately-run','withdrawn-localization-or-mapping','natural-URL-expiry','native-back-scroll-and-recommendation-interaction','matched-reference-captures','platform-accessibility-inspection','physical-device-memory-and-latency']};
fs.writeFileSync(args.output,JSON.stringify(evidence,null,2)+'\n',{mode:0o600});
fs.chmodSync(args.output,0o600);
console.log('Redacted detail probe evidence saved');
