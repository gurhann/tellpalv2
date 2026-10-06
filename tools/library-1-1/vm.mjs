/** Ephemeral VM service injection/observation. Credentials and debugger URLs are never printed. */
import fs from 'node:fs';
const WebSocket=globalThis.WebSocket;
if(Number(process.versions.node.split('.')[0])<22||typeof WebSocket!=='function')throw Error('Node.js 22 or newer is required');
const args=Object.fromEntries(process.argv.slice(2).map((x,i,a)=>x.startsWith('--')?[x.slice(2),a[i+1]]:null).filter(Boolean));
let uri=args['vm-uri'];
if(args['vm-log']){const log=fs.readFileSync(args['vm-log'],'utf8');const matches=[...log.matchAll(/(?:Dart VM Service[^\n]*|The Dart VM service[^\n]*|VM service[^\n]*)?(https?:\/\/127\.0\.0\.1:\d+\/[^\s]+)/gi)];if(!matches.length)throw Error('VM service not yet available');uri=matches[0][1];}
if(!uri)throw Error('Provide --vm-uri or --vm-log');
uri=uri.replace(/^http/,'ws').replace(/\/$/,'')+'/ws';
let socket;
try{socket=new WebSocket(uri);}catch{throw Error('VM connection failed');}
let next=0;const pending=new Map();
socket.addEventListener('message',event=>{let message;try{message=JSON.parse(event.data);}catch{for(const entry of pending.values())entry.reject(Error('Invalid VM response'));pending.clear();socket.close();return;}const resolve=pending.get(message.id);if(resolve){pending.delete(message.id);if(message.error)resolve.reject(Error('VM method failed'));else resolve.resolve(message.result);}});
await new Promise((resolve,reject)=>{socket.addEventListener('open',resolve,{once:true});socket.addEventListener('error',()=>reject(Error('VM connection failed')),{once:true});});
function rpc(method,params={}){return new Promise((resolve,reject)=>{const id=++next;pending.set(id,{resolve,reject});socket.send(JSON.stringify({jsonrpc:'2.0',id,method,params}));setTimeout(()=>{if(pending.delete(id))reject(Error('VM method deadline reached'));},20000).unref();});}
const vm=await rpc('getVM');const isolate=vm.isolates.find(x=>x.name==='main')??vm.isolates[0];const params={isolateId:isolate.id};
let result;
if(args.mode==='configure')result=await rpc('ext.tellpal.library.configure',{...params,baseUrl:args['base-url'],idToken:fs.readFileSync(args.token,'utf8').trim(),language:args.language??'tr',accessIdentity:'existing-authorized-fixture'});
else if(args.mode==='context')result=await rpc('ext.tellpal.library.context',{...params,language:args.language??'tr',textScale:args.scale??'1'});
else result=await rpc('ext.tellpal.library.observe',params);
const redacted=typeof result.json==='string'?JSON.parse(result.json):result;
if(args.output)fs.writeFileSync(args.output,JSON.stringify(redacted,null,2)+'\n');
else console.log(JSON.stringify(redacted));
socket.close();
