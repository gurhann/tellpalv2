#!/usr/bin/env python3
"""Build a self-contained private viewer from verified local experiment files."""

import argparse
import base64
import hashlib
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--html", type=Path, required=True)
    args = parser.parse_args()
    if args.html.exists():
        parser.error("Refusing to overwrite an existing viewer")
    result = json.loads(args.results.read_text())
    pictures = {}
    root = args.output_root.resolve()
    for item in result["sources"] + result["results"]:
        path = (root / item["file"]).resolve()
        if not path.is_relative_to(root):
            raise ValueError("Image path escapes experiment output")
        payload = path.read_bytes()
        if hashlib.sha256(payload).hexdigest() != item["sha256"]:
            raise ValueError(f"Image checksum mismatch: {item['file']}")
        pictures[item["file"]] = "data:image/jpeg;base64," + base64.b64encode(payload).decode()
    data = json.dumps({"result": result, "pictures": pictures}, ensure_ascii=False).replace("<", "\\u003c")
    args.html.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    args.html.write_text(TEMPLATE.replace("__DATA__", data), encoding="utf-8")
    args.html.chmod(0o600)
    print(json.dumps({"viewer": str(args.html), "bytes": args.html.stat().st_size}))


TEMPLATE = r'''<!doctype html>
<html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>TellPal · Görsel karşılaştırması</title>
<style>
:root{color-scheme:light;font-family:system-ui,sans-serif;color:#172333;background:#f3f5f8}*{box-sizing:border-box}body{margin:0}main{max-width:1400px;margin:auto;padding:28px}h1{font-size:28px;margin:0 0 8px}h2{font-size:18px}p{line-height:1.55}.intro{max-width:900px;color:#49566a}.note{border-left:4px solid #bf7b18;background:#fff6e6;padding:14px 18px;margin:20px 0}.controls{display:flex;flex-wrap:wrap;gap:16px;background:white;padding:18px;border:1px solid #dce2e9;border-radius:12px;align-items:end}label{font-weight:600;font-size:13px;display:grid;gap:7px}select,input,button{font:inherit;accent-color:#1256a0}select{padding:9px;border:1px solid #b7c4d4;border-radius:6px;max-width:250px}input[type=range]{width:155px}select:focus-visible,input:focus-visible{outline:3px solid #78b8ff;outline-offset:3px}.stats{padding:14px 0;min-height:48px;line-height:1.6;font-variant-numeric:tabular-nums}.pair,.crops{display:grid;grid-template-columns:1fr 1fr;gap:18px}.panel{min-width:0;border:1px solid #dce2e9;border-radius:12px;background:white;overflow:hidden}.panel h2{padding:16px;margin:0;border-bottom:1px solid #dce2e9}.viewport{overflow:auto;height:660px;padding:16px;text-align:center;background:repeating-conic-gradient(#f3f4f6 0% 25%,#fff 0% 50%) 50% / 20px 20px}.viewport img{max-width:none;display:block;margin:auto}.caption{padding:12px 16px;font-size:13px;color:#49566a;overflow-wrap:anywhere}.crop{padding:18px;overflow:auto;text-align:center}canvas{background:#e9edf2;max-width:none;image-rendering:auto}.detail{font-size:13px;color:#49566a}table{width:100%;border-collapse:collapse;background:white;font-size:13px}th,td{padding:10px;border-bottom:1px solid #dce2e9;text-align:right}th:first-child,td:first-child{text-align:left}thead{background:#e7edf4}td.regression{color:#a12b20;font-weight:600}.table-wrap{overflow:auto}.footer{font-size:13px;color:#49566a;padding:18px 0}details{margin-top:22px}summary{cursor:pointer;font-weight:600;padding:12px 0}@media(max-width:740px){main{padding:16px}.pair,.crops{grid-template-columns:1fr}.viewport{height:550px}h1{font-size:23px}}
</style><main>
<h1>Görsel karşılaştırması</h1>
<p class="intro">Yedi gerçek küçük kapak; mevcut Java JPEG dönüştürücüleriyle 63 kaynak/çıktı çifti. Orijinaller korunur. İki görüntü aynı aday geometrisinde gösterilir; bu ekran gerçek telefon/tablet kalite onayı vermez.</p>
<div class="note"><strong>Henüz üretim profili seçilmedi.</strong> Kaynakların en uzun kenarı 909 px. Büyük kapaklar, okuyucu sayfaları, alfa, döndürme ve animasyon bu karşılaştırmada yok. ICC kaydı ile görsel renk doğruluğu ayrı değerlendirilir.</div>
<div class="controls">
<label>Kaynak<select id="sample" aria-label="Kaynak"></select></label>
<label>Kullanım<select id="role" aria-label="Kullanım"><option value="listing_phone">Liste · telefon (360×480 sınır)</option><option value="listing_tablet">Liste · tablet (600×800 sınır)</option><option value="source_resolution_proxy">Kaynak çözünürlüğü · detay adayı</option></select></label>
<label>JPEG kalitesi<select id="quality" aria-label="JPEG kalitesi"></select></label>
<label>Soldaki karşılaştırma<select id="baseline" aria-label="Soldaki karşılaştırma"><option value="source">Orijinal kaynak</option><option value="control">Aynı liste profili · mevcut q85</option></select></label>
<label>Gösterim ölçeği <span id="zoomLabel"></span><input id="zoom" type="range" min="50" max="200" step="25" value="100"></label>
</div>
<p id="sampleContext" class="detail"></p><div id="stats" class="stats" aria-live="polite"></div>
<div class="pair">
<article class="panel"><h2 id="leftTitle">Orijinal</h2><div class="viewport"><img id="left" alt="Karşılaştırma referansı"></div><div class="caption" id="leftMeta"></div></article>
<article class="panel"><h2 id="rightTitle">Aday</h2><div class="viewport"><img id="right" alt="Seçili JPEG adayı"></div><div class="caption" id="rightMeta"></div></article>
</div>
<p class="detail" id="scaleNote"></p>
<details open><summary>Ayrıntı karşılaştırması</summary>
<div class="controls"><label>Yatay merkez<input id="cropX" type="range" min="0" max="100" value="50"></label><label>Dikey merkez<input id="cropY" type="range" min="0" max="100" value="42"></label></div>
<p class="detail">Her iki görüntü önce adayın piksel boyutuna aynı tarayıcıda çizilir; aynı konumdan en fazla 192×192 örnek piksel 2 kat gösterilir. Bu yardımcı görünüm, kontrollü renk referansı veya cihaz testi değildir.</p>
<div class="crops"><div class="panel crop"><canvas id="cropLeft" aria-label="Referans ayrıntısı"></canvas></div><div class="panel crop"><canvas id="cropRight" aria-label="Aday ayrıntısı"></canvas></div></div>
</details>
<details><summary>Seçili kaynağın bütün dosya sonuçları</summary><div class="table-wrap"><table><thead><tr><th>Aday</th><th>Piksel</th><th>Byte</th><th>Kaynağa göre değişim</th><th>q85 kontrole göre</th></tr></thead><tbody id="rows"></tbody></table></div></details>
<p class="footer">Yüzdelerde pozitif değer küçülmeyi, negatif değer büyümeyi gösterir. Dosya boyutları yerel doğrulanmış byte sayılarıdır; toplam ağ trafiği, depolama veya mobil bellek ölçümü değildir. Kaynak/profil, hash, tekrar süreleri ve sınırlar JSON kanıtında kayıtlıdır.</p>
</main><script type="application/json" id="data">__DATA__</script><script>
const {result,pictures}=JSON.parse(document.getElementById('data').textContent);
const $=id=>document.getElementById(id),fmt=n=>n.toLocaleString('tr-TR'),percent=n=>n==null?'—':`${n.toFixed(1)}%`;
for(const s of result.sources){const o=document.createElement('option');o.value=s.id;o.textContent=`${s.id} · ${s.width}×${s.height}`;$('sample').append(o)}
const delta=n=>`${Math.abs(n).toFixed(1)}% ${n<0?'büyüme':n>0?'küçülme':'değişim'}`;
let current;
function choices(){const old=$('quality').value;$('quality').replaceChildren();const list=result.results.filter(r=>r.sample_id===$('sample').value&&r.role===$('role').value);for(const r of list){const o=document.createElement('option');o.value=r.quality;o.textContent=`q${r.quality}`;$('quality').append(o)}$('quality').value=list.some(r=>String(r.quality)===old)?old:'85';if(!$('quality').value)$('quality').selectedIndex=0;const sourceRole=$('role').value==='source_resolution_proxy';$('baseline').options[1].disabled=sourceRole;if(sourceRole)$('baseline').value='source';update()}
function info(r){const icc=r.metadata.icc;return `${fmt(r.bytes)} byte · ${r.width}×${r.height} · ICC ${icc.present?icc.description||'mevcut':'yok'} · SHA ${r.sha256.slice(0,12)}…`}
async function update(){const s=result.sources.find(s=>s.id===$('sample').value),r=result.results.find(r=>r.sample_id===s.id&&r.role===$('role').value&&String(r.quality)===$('quality').value),b=$('baseline').value==='control'?result.results.find(x=>x.sample_id===s.id&&x.role===r.role&&x.quality===85):s;current={s,r,b};$('sampleContext').textContent=s.id==='en-story11-p3'?'Yalnız yerel Display P3 renk kontrolü; Firebase kaynak eşleşmesi iddia edilmiyor.':'Story 1.2 kaynak byte kanıtıyla eşleşen küçük kapak kontrolü.';$('leftTitle').textContent=b===s?'Orijinal kaynak':'Mevcut liste kontrolü · q85';$('rightTitle').textContent=`JPEG adayı · q${r.quality}`;$('leftMeta').textContent=info(b);$('rightMeta').textContent=info(r);$('stats').textContent=`Kaynak → aday: ${fmt(s.bytes)} → ${fmt(r.bytes)} byte · ${delta(r.source_saved_percent)}. ${r.control_saved_percent==null?'':`Aynı liste q85 kontrolüne göre: ${delta(r.control_saved_percent)}.`}`;$('left').src=pictures[b.file];$('right').src=pictures[r.file];const scale=Number($('zoom').value)/100;for(const id of ['left','right']){$(id).style.width=(r.width*scale)+'px';$(id).style.height=(r.height*scale)+'px'}$('zoomLabel').textContent=`${$('zoom').value}%`;$('scaleNote').textContent=`100% = adayın ${r.width}×${r.height} pikseli kadar CSS alanı; kaynak da aynı alana ölçeklenir. Tarayıcı DPR: ${window.devicePixelRatio}. Büyütme kaynakta yeni ayrıntı oluşturmaz.`;$('rows').replaceChildren();for(const x of result.results.filter(x=>x.sample_id===s.id)){const tr=document.createElement('tr');for(const [i,t] of [`${x.role} q${x.quality}`,`${x.width}×${x.height}`,fmt(x.bytes),percent(x.source_saved_percent),percent(x.control_saved_percent)].entries()){const td=document.createElement('td');td.textContent=t;if((i===3&&x.source_saved_percent<0)||(i===4&&x.control_saved_percent<0))td.className='regression';tr.append(td)}$('rows').append(tr)}await Promise.all([$('left').decode(),$('right').decode()]);if(current.r===r)crop()}
function crop(){if(!current||!$('left').complete||!$('right').complete)return;const {r}=current,side=Math.min(192,r.width,r.height),x=Math.max(0,Math.min(r.width-side,r.width*Number($('cropX').value)/100-side/2)),y=Math.max(0,Math.min(r.height-side,r.height*Number($('cropY').value)/100-side/2));for(const [imageId,canvasId] of [['left','cropLeft'],['right','cropRight']]){const normalized=document.createElement('canvas');normalized.width=r.width;normalized.height=r.height;normalized.getContext('2d').drawImage($(imageId),0,0,r.width,r.height);const c=$(canvasId);c.width=side;c.height=side;c.style.width=side*2+'px';c.style.height=side*2+'px';c.getContext('2d').drawImage(normalized,x,y,side,side,0,0,side,side)}}
for(const id of ['sample','role'])$(id).addEventListener('change',choices);for(const id of ['quality','baseline','zoom'])$(id).addEventListener('input',update);for(const id of ['cropX','cropY'])$(id).addEventListener('input',crop);choices();
</script></html>'''


if __name__ == "__main__":
    main()
