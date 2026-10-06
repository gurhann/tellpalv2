#!/usr/bin/env python3
"""Local acceptance fixture: real artwork upload/preparation, controlled SQL eligibility, redacted HTTP evidence.
Reads private runtime credentials and ID token. Never writes tokens or signed URLs to repository artifacts.
"""
import argparse, hashlib, json, os, pathlib, subprocess, sys, time, urllib.request, urllib.error, uuid
from datetime import datetime, timezone

LANGUAGES = ("tr", "en", "pt")
PREFIX = "library-1-1-"
parser = argparse.ArgumentParser()
parser.add_argument("--runtime", type=pathlib.Path, required=True)
parser.add_argument("--token", type=pathlib.Path, required=True)
parser.add_argument("--artwork", type=pathlib.Path, required=True, help="Existing TellPal assets/images/landing directory")
parser.add_argument("--output", type=pathlib.Path, required=True, help="Redacted evidence JSON")
parser.add_argument("--private-output", type=pathlib.Path, required=True, help="Private /tmp catalog JSON directory")
parser.add_argument("--verify-only", action="store_true")
parser.add_argument("--add-omitted-cases", action="store_true", help="Add missing selected-language and draft-category omission cases to the existing disposable fixture")
args = parser.parse_args()
runtime = json.loads(args.runtime.read_text())
if runtime["db_name"] != "tellpal_library" or runtime["db_user"] != "tellpal_library":
    raise SystemExit("Refusing a database outside the disposable Library fixture")
base = "http://127.0.0.1:8082"
private = args.private_output.resolve()
if not str(private).startswith("/private/tmp/"):
    raise SystemExit("Private catalogs must remain outside the repository under /private/tmp")
private.mkdir(parents=True, exist_ok=True); private.chmod(0o700)

def sql(statement):
    env = dict(os.environ, PGPASSWORD=runtime["db_password"])
    result = subprocess.run(["/opt/homebrew/bin/psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-h", "127.0.0.1", "-p", "5442", "-U", runtime["db_user"], "-d", runtime["db_name"]], input=statement, text=True, capture_output=True, env=env)
    if result.returncode: raise RuntimeError("Disposable fixture SQL failed; inspect privately")
    return result.stdout.strip()
def quote(value): return "'" + str(value).replace("'", "''") + "'"
def http(path, method="GET", body=None, bearer=None, content_type="application/json"):
    data = None if body is None else body if isinstance(body,bytes) else json.dumps(body).encode()
    request = urllib.request.Request(base+path, data=data, method=method, headers={"Content-Type":content_type, **({"Authorization":"Bearer "+bearer} if bearer else {})})
    start = time.monotonic()
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            result = json.load(response); return response.status, result, round((time.monotonic()-start)*1000)
    except urllib.error.HTTPError as error:
        raise RuntimeError("Local HTTP status "+str(error.code)+" at "+path.split("?")[0]) from None
admin = http("/api/admin/auth/login", "POST", {"username":runtime["admin_username"],"password":runtime["admin_password"]})[1]["accessToken"]
def upload(file):
    data=file.read_bytes();boundary="library"+uuid.uuid4().hex
    body=(f"--{boundary}\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\nORIGINAL_IMAGE\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"checksumSha256\"\r\n\r\n{hashlib.sha256(data).hexdigest()}\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{file.name}\"\r\nContent-Type: image/jpeg\r\n\r\n").encode()+data+f"\r\n--{boundary}--\r\n".encode()
    asset=http("/api/admin/media/uploads","POST",body,admin,"multipart/form-data; boundary="+boundary)[1]
    if asset["provider"] != "FIREBASE_STORAGE" or not asset["objectPath"].startswith("/local-library-1-1/"): raise RuntimeError("Storage isolation mismatch")
    return asset["assetId"]

def seed():
    if int(sql("select count(*) from contents where external_key not like 'library-1-1-%'")): raise RuntimeError("Disposable database contains unrelated content")
    if int(sql("select count(*) from categories")): raise RuntimeError("Fixture already exists: use --verify-only")
    cover_ids={}
    for lang in LANGUAGES:
        for i in range(1,8):
            file=args.artwork/lang/f"story{i}.jpg"
            if not file.is_file(): raise RuntimeError("Declared-language source artwork is absent: "+lang+" story"+str(i))
            cover_ids[lang,i]=upload(file)
    names={"alpha":{"tr":"Harika Maceralar","en":"Wonderful Adventures","pt":"Aventuras maravilhosas"},"zeta":{"tr":"Birlikte Okuyalım","en":"Read Together","pt":"Vamos ler juntos"},"english":{"tr":"İngilizce Hikâyeler","en":"English Stories","pt":"Histórias em inglês"},"promo":{"tr":"Premium Hikâyeleri Keşfet","en":"Discover Premium Stories","pt":"Descubra histórias Premium"},"empty":{"tr":"Boş satır","en":"Empty row","pt":"Linha vazia"},"hidden":{"tr":"Gizli satır","en":"Hidden row","pt":"Linha oculta"}}
    categories={}
    for key, localized in names.items():
        cid=int(sql("insert into categories(slug,type,is_active) values("+quote(PREFIX+key)+",'STORY',"+("false" if key=="hidden" else "true")+") returning id"));categories[key]=cid
        for lang,name in localized.items(): sql(f"insert into category_localizations(category_id,language_code,name,description,status,published_at) values({cid},{quote(lang)},{quote(name)},{quote({'tr':'Hayal gücünü keşfeden, birlikte okunacak hikâyeler.','en':'Stories to explore imagination and read together.','pt':'Histórias para explorar a imaginação e ler juntos.'}[lang])},'PUBLISHED',now())")
    stories={};jobs=[]
    for i in range(10):
        key=PREFIX+"story-"+str(i);cid=int(sql("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY',"+quote(key)+",true,5,0) returning id"));stories[i]=cid
        languages=LANGUAGES if i<8 else ('en',)
        for lang in languages:
            title={"tr":"Gökyüzüne Ulaşan Küçük Kahramanın Unutulmaz Macerası","en":"The Little Hero's Unforgettable Adventure to the Sky","pt":"A inesquecível aventura do pequeno herói que alcançou o céu"}[lang]+" "+str(i+1)
            source=cover_ids[lang,i%7+1]
            sql(f"insert into content_localizations(content_id,language_code,title,cover_media_id,status,processing_status,published_at) values({cid},{quote(lang)},{quote(title)},{source},'PUBLISHED',{'PENDING' if i==0 else 'COMPLETED'!r},now())")
            http(f"/api/admin/contents/{cid}/localizations/{lang}/listing-cover-processing","POST",{},admin);jobs.append((cid,lang))
    for lang in LANGUAGES:
        for lane,ids in [('alpha',range(8)),('zeta',range(1,4)),('promo',[1,2]),('hidden',[1])]:
            for order,i in enumerate(ids): sql(f"insert into category_contents(category_id,content_id,language_code,display_order) values({categories[lane]},{stories[i]},{quote(lang)},{order})")
    for lane in ['zeta','english']:
        for order,i in enumerate([8,9]): sql(f"insert into category_contents(category_id,content_id,language_code,display_order) values({categories[lane]},{stories[i]},'en',{order+(3 if lane=='zeta' else 0)})")
    for lang in LANGUAGES: sql(f"insert into content_free_access(access_key,content_id,language_code) values('default',{stories[1]},{quote(lang)})")
    for lang in LANGUAGES:
        snapshot=http('/api/admin/categories/home?lang='+lang,bearer=admin)[1]
        order=['empty','hidden','zeta','alpha','english','promo'] if lang=='tr' else ['alpha','empty','zeta','promo','english','hidden']
        by_id={v:k for k,v in categories.items()}
        rows=[{'categoryId':r['categoryId'],'displayOrder':order.index(by_id[r['categoryId']]),'audience':'NON_SUBSCRIBER' if by_id[r['categoryId']]=='promo' else 'ALL','contentLanguage':'en' if by_id[r['categoryId']]=='english' else None} for r in snapshot['rows']]
        http('/api/admin/categories/home?lang='+lang,'PUT',{'expectedRevision':snapshot['revision'],'rows':rows},admin)
    deadline=time.monotonic()+180
    while True:
        pending=int(sql("select count(*) from asset_processing where processing_kind='LISTING_COVER' and status<>'COMPLETED'"))
        failed=int(sql("select count(*) from asset_processing where processing_kind='LISTING_COVER' and status='FAILED'"))
        if failed: raise RuntimeError("Real cover preparation failed: inspect job state privately")
        if not pending: break
        if time.monotonic()>deadline: raise RuntimeError("Cover preparation deadline reached")
        time.sleep(2)
def add_omitted_cases():
    definitions=[('no-tr',{'tr':'Bu dilde hikâye yok','en':'English-only curation','pt':'Sem histórias neste idioma'},'en',[8,9]),('draft',{'tr':'Yayımlanmamış kategori','en':'Unpublished category','pt':'Categoria não publicada'},'tr',[1])]
    for key,names,lane,story_indices in definitions:
        existing=sql("select id from categories where slug="+quote(PREFIX+key))
        cid=int(existing or sql("insert into categories(slug,type,is_active) values("+quote(PREFIX+key)+",'STORY',true) returning id"))
        for lang,name in names.items():
            status='DRAFT' if key=='draft' and lang=='tr' else 'PUBLISHED'
            published='null' if status=='DRAFT' else 'now()'
            sql(f"insert into category_localizations(category_id,language_code,name,status,published_at) values({cid},{quote(lang)},{quote(name)},{quote(status)},{published}) on conflict(category_id,language_code) do nothing")
        for order,index in enumerate(story_indices):
            content=int(sql("select id from contents where external_key="+quote(PREFIX+'story-'+str(index))))
            sql(f"insert into category_contents(category_id,content_id,language_code,display_order) values({cid},{content},{quote(lane)},{order}) on conflict do nothing")
    snapshot=http('/api/admin/categories/home?lang=tr',bearer=admin)[1]
    next_order=max((r['displayOrder'] for r in snapshot['rows'] if r['displayOrder'] is not None),default=-1)+1
    rows=[]
    for row in snapshot['rows']:
        order=row['displayOrder']
        if row['slug'] in [PREFIX+'no-tr',PREFIX+'draft'] and order is None:order=next_order;next_order+=1
        rows.append({'categoryId':row['categoryId'],'displayOrder':order,'audience':row['audience'],'contentLanguage':row['contentLanguage']})
    http('/api/admin/categories/home?lang=tr','PUT',{'expectedRevision':snapshot['revision'],'rows':rows},admin)
if args.add_omitted_cases:add_omitted_cases()
elif not args.verify_only:
    seed()
    add_omitted_cases()
token=args.token.read_text().strip();evidence={'timestamp':datetime.now(timezone.utc).isoformat(),'contractVersion':1,'backend':'isolated local 8082','storage':'real Firebase / local-library-1-1','identity':'authorized existing Firebase account (redacted)','catalogs':[],'artwork':[],'omittedFixtureCases':{}}
seen=set()
for key in ['empty','hidden','no-tr','draft']:
    id=sql("select id from categories where slug="+quote(PREFIX+key))
    if id:evidence['omittedFixtureCases'][key]=int(id)
for lang in LANGUAGES:
    status,catalog,milliseconds=http('/api/test/library?lang='+lang,bearer=token)
    file=private/f'catalog-{lang}.json';file.write_text(json.dumps(catalog));file.chmod(0o600)
    public=json.loads(json.dumps(catalog))
    for row in public['rows']:
        for item in row['items']:
            for variant in ['phone','tablet']:
                cover=item['covers'].get(variant)
                if cover:
                    url=cover.pop('url');cover.pop('expiresAt',None)
                    if cover['assetId'] not in seen:
                        start=time.monotonic()
                        with urllib.request.urlopen(url,timeout=15) as response:
                            data=response.read();mime=response.headers.get_content_type()
                        if mime!='image/jpeg' or not data.startswith(b'\xff\xd8'): raise RuntimeError('Actual artwork MIME mismatch')
                        image_file=private/f"asset-{cover['assetId']}.jpg";image_file.write_bytes(data)
                        dims=subprocess.run(['sips','-g','pixelWidth','-g','pixelHeight',str(image_file)],capture_output=True,text=True,check=True).stdout
                        width=int(dims.split('pixelWidth: ')[1].split()[0]);height=int(dims.split('pixelHeight: ')[1].split()[0])
                        maximum=(360,480) if variant=='phone' else (600,800)
                        if width>maximum[0] or height>maximum[1]:raise RuntimeError('Actual artwork dimensions exceed profile')
                        evidence['artwork'].append({'assetId':cover['assetId'],'mimeType':mime,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'width':width,'height':height,'milliseconds':round((time.monotonic()-start)*1000)})
                        seen.add(cover['assetId'])
    if lang=='tr' and any(r['categoryId'] in evidence['omittedFixtureCases'].values() for r in public['rows']):raise RuntimeError('Configured ineligible row was disclosed')
    if any(len(r['items'])>5 for r in public['rows']):raise RuntimeError('Preview limit mismatch')
    if any(i['contentLanguage']!=r['contentLanguage'] for r in public['rows'] for i in r['items']):raise RuntimeError('Language substitution')
    forbidden={'audio','audioAsset','pages','video','narration','fullAssets'}
    if any(forbidden.intersection(i) for r in public['rows'] for i in r['items']):raise RuntimeError('Full media disclosed')
    evidence['catalogs'].append({'httpStatus':status,'milliseconds':milliseconds,'catalog':public})
args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(evidence,indent=2,ensure_ascii=False)+'\n')
print(json.dumps({'catalogs':len(evidence['catalogs']),'verifiedArtwork':len(seen),'evidence':str(args.output)},ensure_ascii=False))
