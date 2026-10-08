from pathlib import Path
import json, os, subprocess, urllib.request, urllib.error, urllib.parse, hashlib, datetime, time, base64, sys, argparse, shutil

# Network failures can include signed URLs; never print exception bodies or stacks.
sys.excepthook=lambda *_: print('Controlled HTTP matrix failed; sensitive error data omitted',file=sys.stderr)
parser=argparse.ArgumentParser(description='Real isolated Story 1.2 HTTP prerequisite matrix')
parser.add_argument('--runtime',type=Path,required=True)
parser.add_argument('--token',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--base-url',default='http://127.0.0.1:8082')
parser.add_argument('--mode',choices=['non-subscriber','subscriber','unlisted'],required=True)
args=parser.parse_args()
repo=Path(__file__).resolve().parents[2]
base=args.base_url.rstrip('/')
parsed=urllib.parse.urlparse(base)
assert parsed.scheme in ['http','https'] and parsed.hostname in ['127.0.0.1','localhost','::1']
assert parsed.port==8082 and not parsed.username and not parsed.password and not parsed.query and not parsed.fragment and not parsed.path
assert not args.token.resolve().is_relative_to(repo) and not args.runtime.resolve().is_relative_to(repo)
assert not args.output.resolve().is_relative_to(repo)
psql=shutil.which('psql')
assert psql,'PostgreSQL client is required'
runtime=json.loads(args.runtime.read_text())
account=json.loads((repo/'tools/mobile-test-account.json').read_text())['firebaseUid']
token=args.token.read_text().strip()
part=token.split('.')[1]
claims=json.loads(base64.urlsafe_b64decode(part+'='*((-len(part))%4)))
assert claims['sub']==account and claims['exp']>time.time()
assert runtime['db_name']=='tellpal_library' and runtime['db_user']=='tellpal_library'
mode=args.mode
records=[]

def sql(query):
 result=subprocess.run([psql,'-X','-qAt','-v','ON_ERROR_STOP=1','-h','127.0.0.1','-p','5442','-U',runtime['db_user'],'-d',runtime['db_name']],input=query,text=True,capture_output=True,env=dict(os.environ,PGPASSWORD=runtime['db_password']))
 if result.returncode: raise RuntimeError('Isolated SQL operation failed')
 return result.stdout.strip()

def quote(value):
 if value is None:return 'NULL'
 if isinstance(value,bool):return 'true' if value else 'false'
 if isinstance(value,int):return str(value)
 return "'"+str(value).replace("'","''")+"'"

def snapshot():
 rows={}
 for table in ['contents','content_localizations','categories','category_localizations','category_contents','content_contributors','contributors','content_free_access','media_assets','listing_cover_revisions']:
  rows[table]=json.loads(sql('select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text),\'[]\'::jsonb) from '+table+' t'))
 return hashlib.sha256(json.dumps(rows,sort_keys=True).encode()).hexdigest()

def request(path,bearer=token):
 request=urllib.request.Request(base+path,headers={'Authorization':'Bearer '+bearer} if bearer else {})
 started=time.monotonic()
 try:
  with urllib.request.urlopen(request,timeout=15) as response:
   raw=response.read(1048577);assert len(raw)<=1048576
   return response.status,json.loads(raw),round((time.monotonic()-started)*1000)
 except urllib.error.HTTPError as error:
  raw=error.read(1048577);assert len(raw)<=1048576
  return error.code,json.loads(raw),round((time.monotonic()-started)*1000)

def path(cid=2,category=1,lang='tr',content_lang=None):
 return f'/api/test/library/stories/{cid}?categoryId={category}&lang={lang}&contentLang={content_lang or lang}&experience=READING'

def check(name,url,expected=200,code=None,bearer=token,validate=None):
 status,body,elapsed=request(url,bearer)
 assert status==expected, name+' unexpected status '+str(status)
 if code:assert body['errorCode']==code,name+' unexpected error code'
 if validate:validate(body)
 record={'name':name,'status':status,'elapsedMs':elapsed}
 if status==200 and 'contentId' in body:
  record.update({k:body[k] for k in ['schemaVersion','categoryId','contentId','requestedLanguage','contentLanguage','audience','title','ageRange','durationMinutes','summary','credits','isFree','isLocked']})
  record['recommendations']=[{'contentId':i['contentId'],'contentLanguage':i['contentLanguage'],'isLocked':i['isLocked']} for i in body['recommendations']]
  record['hero']=None if body['hero'] is None else {k:v for k,v in body['hero'].items() if k not in ['url','expiresAt']}
 else:record['errorCode']=body.get('errorCode')
 records.append(record)
 return body

def require(condition):assert condition

def change_case(name,table,where,changes,url,expected=404,validate=None):
 columns=list(changes)
 original=json.loads(sql('select row_to_json(r) from (select '+','.join(columns)+' from '+table+' where '+where+') r'))
 try:
  sql('update '+table+' set '+','.join(k+'='+quote(v) for k,v in changes.items())+' where '+where)
  check(name,url,expected,'content_not_found' if expected==404 else None,validate=validate)
 finally:
  sql('update '+table+' set '+','.join(k+'='+quote(v) for k,v in original.items())+' where '+where)

def verify_source(body):
 hero=body['hero'];assert hero
 parsed=urllib.parse.urlparse(hero['url']);assert parsed.scheme=='https' and parsed.hostname in ['storage.googleapis.com','firebasestorage.googleapis.com']
 with urllib.request.urlopen(hero['url'],timeout=10) as response:
  raw=response.read(20971521)
  assert len(raw)<=20971520 and response.status==200
  assert len(raw)==hero['byteSize'] and hashlib.sha256(raw).hexdigest()==hero['checksumSha256']
 return {'assetId':hero['assetId'],'byteSize':len(raw),'checksumSha256':hero['checksumSha256'],'mimeType':hero['mimeType'],'width':hero['width'],'height':hero['height']}

before=snapshot()
sources=[]
try:
 assert int(sql("select count(*) from contents where external_key not like 'library-1-1-%'"))==0
 if mode=='unlisted':
  check('verified-designated-UID-outside-allowlist',path(),403)
  check('verified-designated-UID-home-outside-allowlist','/api/test/library?lang=tr',403)
 else:
  subscriber=mode=='subscriber'
  audience='SUBSCRIBER' if subscriber else 'NON_SUBSCRIBER'
  for lang in ['tr','en','pt']:
   b=check(lang+'-free-identity-credits-order',path(lang=lang),validate=lambda d,l=lang: require(d['contentId']==2 and d['contentLanguage']==l and d['requestedLanguage']==l and d['audience']==audience and d['isFree'] and not d['isLocked'] and [c['role'] for c in d['credits']]==['AUTHOR','NARRATOR','ILLUSTRATOR'] and d['credits'][0]['displayName']==('Deniz Yılmaz' if l=='tr' else 'Deniz Yilmaz') and d['credits'][1]['displayName']=='Library Fixture Narrator' and d['credits'][2]['displayName']=='Library Fixture Illustrator' and [r['contentId'] for r in d['recommendations']]==[3,4,5,6,7]))
   sources.append({'language':lang,**verify_source(b)})
   check(lang+'-locked-access',path(3,lang=lang),validate=lambda d:require(not d['isFree'] and d['isLocked']==(not subscriber) and all(r['isLocked']==(not r['isFree'] and not subscriber) for r in d['recommendations'])))
   check(lang+'-beyond-home-preview-full-membership',path(8,lang=lang),validate=lambda d:require(d['contentId']==8 and [r['contentId'] for r in d['recommendations']]==[2,3,4,5,6]))
   check(lang+'-mapped-EN-optional-metadata',path(9,3,lang,'en'),validate=lambda d,l=lang:require(d['requestedLanguage']==l and d['contentLanguage']=='en' and d['summary'] is None and d['durationMinutes'] is None and d['credits']==[] and [r['contentId'] for r in d['recommendations']]==[10]))
  check('ordered-smaller-category',path(2,2),validate=lambda d:require([r['contentId'] for r in d['recommendations']]==[3,4]))
  check('promo-category-server-audience',path(2,4),404 if subscriber else 200,'content_not_found' if subscriber else None)
  for name,url,status,code in [('unsupported-experience',path().replace('READING','AUDIO_STORY'),400,'invalid_request'),('invalid-language',path().replace('lang=tr&','lang=xx&'),400,'invalid_request'),('missing-language',path().replace('lang=tr&',''),400,'invalid_request'),('missing-content-language',path().replace('contentLang=tr&',''),400,'invalid_request'),('invalid-category',path(category=0),400,'invalid_request'),('missing-category',path().replace('categoryId=1&',''),400,'invalid_request'),('nonmember',path(9),404,'content_not_found'),('exact-language-mismatch',path(content_lang='en'),404,'content_not_found'),('pending-processing',path(1),404,'content_not_found'),('unavailable-content',path(9223372036854775807),404,'content_not_found')]:
   check(name,url,status,code)
  check('missing-bearer',path(),401,'firebase_auth_error',bearer=None)
  check('invalid-bearer',path(),401,'firebase_auth_error',bearer='invalid-test-token')
  change_case('withdrawn-localization','content_localizations',"content_id=2 and language_code='tr'",{'status':'DRAFT'},path())
  change_case('inactive-content','contents','id=2',{'is_active':False},path())
  change_case('inactive-category','categories','id=1',{'is_active':False},path())
  change_case('opted-out-category','category_localizations',"category_id=1 and language_code='tr'",{'home_display_order':None},path())
  change_case('withdrawn-category-localization','category_localizations',"category_id=1 and language_code='tr'",{'status':'DRAFT'},path())
  change_case('withdrawn-explicit-mapping','category_localizations',"category_id=3 and language_code='tr'",{'home_content_language':None},path(9,3,'tr','en'))
  change_case('withdrawn-mapped-language-publication','category_localizations',"category_id=3 and language_code='en'",{'status':'DRAFT'},path(9,3,'tr','en'))
  empty_category=3 if subscriber else 4
  excluded_content=10 if subscriber else 3
  selected_content=9 if subscriber else 2
  change_case('empty-filtered-recommendations','contents','id='+str(excluded_content),{'is_active':False},path(selected_content,empty_category,'tr','en' if subscriber else 'tr'),200,lambda d:require(d['recommendations']==[]))
  change_case('absent-cover-metadata-retained','content_localizations',"content_id=2 and language_code='tr'",{'cover_media_id':None},path(),200,lambda d:require(d['hero'] is None and d['summary'] is not None and len(d['credits'])==3))
  asset=int(sql("select cover_media_id from content_localizations where content_id=2 and language_code='tr'"))
  change_case('corrupt-source-provenance-metadata-retained','media_assets','id='+str(asset),{'checksum_sha256':'0'*64},path(),200,lambda d:require(d['hero'] is None and d['contentId']==2))
  change_case('oversized-source-metadata-retained','media_assets','id='+str(asset),{'byte_size':20971521},path(),200,lambda d:require(d['hero'] is None and d['contentId']==2))
finally:
 after=snapshot()
 assert before==after,'Isolated fixture restoration mismatch'
result={'executed':True,'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'firebaseUid':account,'serverFixtureMode':mode,'stubTokensEnabled':False,'backendPort':8082,'postgresPort':5442,'datasetSha256Before':before,'datasetSha256After':after,'datasetRestored':True,'cases':records,'actualSources':sources,'remoteStorageWrites':False,'nativeAcceptance':False}
result['matrixScriptSha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
out=args.output
out.parent.mkdir(parents=True,exist_ok=True)
descriptor=os.open(out,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
with os.fdopen(descriptor,'w') as file:file.write(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
out.chmod(0o600)
print(json.dumps({'mode':mode,'passedCases':len(records),'verifiedActualSources':len(sources),'datasetRestored':True}))
