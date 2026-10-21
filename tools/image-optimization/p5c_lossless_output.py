"""P5c'nin exclusive özel çıktıları, çıktı bütçesi ve çevrimdışı galerisi."""
import html
import json
import os
from pathlib import Path
import stat

from p5b_source_io import ROOT, open_regular, require

OUTPUT_BUDGET = 1024 ** 3


def create_private_directory(path):
    """Yeni dizin ayırır; repo içi, symlink ve var olan yol reddedilir."""
    path = Path(path).absolute()
    require(".." not in path.parts and not path.is_relative_to(ROOT), "PRIVATE_PATH_REQUIRED")
    parent = os.open("/", os.O_RDONLY | os.O_DIRECTORY)
    try:
        for part in path.parts[1:-1]:
            child = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent)
            os.close(parent)
            parent = child
        os.mkdir(path.name, mode=0o700, dir_fd=parent)
        descriptor = os.open(path.name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent)
        try:
            require(stat.S_IMODE(os.fstat(descriptor).st_mode) == 0o700, "PRIVATE_DIRECTORY_MODE")
        finally:
            os.close(descriptor)
    finally:
        os.close(parent)
    return path


def exclusive_bytes(path, data, budget):
    require(len(data) <= budget, "OUTPUT_BUDGET_EXCEEDED")
    descriptor = open_regular(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL)
    with os.fdopen(descriptor, "wb") as output:
        output.write(data)
    return len(data)


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8")


def gallery(records):
    """Üç biçimi aynı CSS ölçüsünde gösterir; script ve dış kaynak kullanmaz."""
    sections = []
    for record in records:
        asset = record["assetId"]
        roles = ", ".join(role["role"] + (f" {role['pageNumber']}" if role["pageNumber"] else "")
                          for role in record["roles"])
        figures = []
        images = [("Kaynak", record["sourceFile"], record["sourceBytes"])]
        images += [(format_name, candidate["file"], candidate["bytes"])
                   for format_name, candidate in record["candidates"].items()]
        for name, filename, byte_count in images:
            figures.append(f'<figure><figcaption>{name} · {byte_count:,} byte</figcaption>'
                           f'<a href="{html.escape(filename, quote=True)}">'
                           f'<img src="{html.escape(filename, quote=True)}" width="1712" height="2778" '
                           f'loading="lazy" alt="#{asset} {name}"></a></figure>')
        sections.append(f'<section><h2>#{record["pilotContentId"]}/TR · asset {asset}</h2>'
                        f'<p>{html.escape(roles)}</p><div class="images">{"".join(figures)}</div></section>')
    return ('<!doctype html><html lang="tr"><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            '<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; img-src \'self\'; '
            'style-src \'unsafe-inline\'; base-uri \'none\'; form-action \'none\'">'
            '<title>P5c yerel kayıpsız karşılaştırma</title><style>'
            'body{font:16px system-ui;margin:24px;color:#222;background:#fafafa}'
            '.images{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}'
            'figure{margin:0}img{display:block;width:100%;height:auto}section{margin-bottom:48px}'
            'figcaption{margin:8px 0}a{color:inherit}</style>'
            '<h1>P5c: Kaynak / PNG / kayıpsız WebP</h1>'
            '<p>Her sütun aynı ölçektedir. Görsele tıklayarak 1712×2778 dosyayı açın. '
            'Tüm dosyalar bu özel dizindedir; dış kaynak yoktur. ICC profilini tarayıcı yorumlar. '
            'Yerel inceleme üretim, P2, cihaz veya fiziksel kalite kabulü değildir.</p>'
            + "".join(sections) + '</html>\n').encode("utf-8")


def totals(records):
    result = []
    for pilot in (18, 62):
        group = [record for record in records if record["pilotContentId"] == pilot]
        source_bytes = sum(record["sourceBytes"] for record in group)
        formats = {}
        for format_name in ("PNG", "WEBP"):
            candidates = [record["candidates"][format_name] for record in group if format_name in record["candidates"]]
            encoded_bytes = sum(candidate["bytes"] for candidate in candidates)
            formats[format_name] = {"files": len(candidates), "bytes": encoded_bytes,
                                   "savedBytes": source_bytes - encoded_bytes,
                                   "savedPercent": (1 - encoded_bytes / source_bytes) * 100 if source_bytes else None,
                                   "growingFiles": [record["assetId"] for record in group
                                                    if format_name in record["candidates"] and
                                                    record["candidates"][format_name]["bytes"] > record["sourceBytes"]]}
        result.append({"pilotContentId": pilot, "languageCode": "tr", "uniqueSources": len(group),
                       "roleBindings": sum(len(record["roles"]) for record in group),
                       "sourceBytes": source_bytes, "formats": formats})
    return result
