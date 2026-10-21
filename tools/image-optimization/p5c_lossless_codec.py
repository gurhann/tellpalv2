"""P5c'nin renk dönüşümü yapmayan decode, encode ve tam eşitlik kontrolleri."""
from hashlib import sha256
from io import BytesIO
import time
import warnings

from PIL import Image, ImageFile, PngImagePlugin

from p5b_image_probe import MAX_PIXELS
from p5b_source_io import require

DIMENSIONS = (1712, 2778)
ICC_SHA256 = "20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab"
SETTINGS = {"PNG": {"compress_level": 9},
            "WEBP": {"lossless": True, "quality": 100, "method": 6, "exact": True}}


def digest(data):
    return sha256(data).hexdigest()


def metadata(image):
    """Ham EXIF/XMP ve ICC byte'larını birbirinden ayrı gözler."""
    xmp = image.info.get("xmp", image.info.get("XML:com.adobe.xmp"))
    if isinstance(xmp, str):
        xmp = xmp.encode("utf-8")
    return {"icc": image.info.get("icc_profile"), "exif": image.info.get("exif"), "xmp": xmp}


def metadata_summary(values):
    return {key: {"present": value is not None, "bytes": len(value) if value is not None else 0,
                  "sha256": digest(value) if value is not None else None}
            for key, value in values.items()}


def decode_source(data, pilot=None, dimensions=DIMENSIONS):
    """Tek, opak RGB/RGBA raster ister; orientation/profil farkını düzeltmeden reddeder."""
    with warnings.catch_warnings():
        warnings.simplefilter("error")
        require(not ImageFile.LOAD_TRUNCATED_IMAGES, "TRUNCATED_DECODING_ENABLED")
        with Image.open(BytesIO(data)) as image:
            require(image.width * image.height <= MAX_PIXELS, "PIXEL_LIMIT")
            require(image.size == tuple(dimensions), "SOURCE_DIMENSIONS_MISMATCH")
            require(getattr(image, "n_frames", 1) == 1, "SOURCE_MULTIFRAME")
            require(image.format in ("PNG", "JPEG"), "SOURCE_FORMAT_INVALID")
            require(image.mode in ("RGB", "RGBA"), "SOURCE_MODE_INVALID")
            require(274 not in image.getexif(), "SOURCE_ORIENTATION_PRESENT")
            values = metadata(image)
            if pilot is not None:
                expected = ("JPEG", "RGB") if pilot == 18 else ("PNG", "RGBA")
                require((image.format, image.mode) == expected, "SOURCE_FORMAT_MODE_MISMATCH")
                require(values["icc"] is None if pilot == 18 else
                        values["icc"] is not None and len(values["icc"]) == 536 and
                        digest(values["icc"]) == ICC_SHA256, "SOURCE_ICC_MISMATCH")
            image.load()
            alpha = image.getchannel("A") if image.mode == "RGBA" else None
            if alpha is None and "transparency" in image.info:
                alpha = image.convert("RGBA").getchannel("A")
            require(alpha is None or alpha.getextrema() == (255, 255), "SOURCE_TRANSPARENT_PIXELS")
            pixels = image.convert("RGB").tobytes()
            record = {"format": image.format, "mode": image.mode, "dimensions": list(image.size),
                      "rgbBytes": len(pixels), "rgbSha256": digest(pixels), "frames": 1,
                      "alphaRemoved": image.mode == "RGBA", "allAlpha255": alpha is not None,
                      "orientation": None, "metadata": metadata_summary(values),
                      "colorInterpretation": "ICC_PRESERVED" if values["icc"] else "UNSPECIFIED"}
        with Image.open(BytesIO(data)) as structure:
            structure.verify()
    return pixels, values, record


def webp_chunks(data, dimensions):
    """ICC'li VP8X dahil RIFF yapısını inceler; kayıplı VP8 ve animasyonu reddeder."""
    require(len(data) >= 26 and data[:4] == b"RIFF" and data[8:12] == b"WEBP" and
            int.from_bytes(data[4:8], "little") + 8 == len(data), "WEBP_RIFF_INVALID")
    offset, chunks, bodies = 12, [], {}
    while offset < len(data):
        require(offset + 8 <= len(data), "WEBP_CHUNK_TRUNCATED")
        kind, length = data[offset:offset + 4], int.from_bytes(data[offset + 4:offset + 8], "little")
        end = offset + 8 + length
        require(end + length % 2 <= len(data), "WEBP_CHUNK_TRUNCATED")
        require(length % 2 == 0 or data[end] == 0, "WEBP_PADDING_INVALID")
        require(kind not in bodies, "WEBP_DUPLICATE_CHUNK")
        chunks.append(kind)
        bodies[kind] = data[offset + 8:end]
        offset = end + length % 2
    require(b"VP8L" in bodies and not set(chunks) - {b"VP8X", b"ICCP", b"VP8L", b"EXIF", b"XMP "},
            "WEBP_VP8L_REQUIRED")
    raster = bodies[b"VP8L"]
    require(len(raster) >= 5 and raster[0] == 0x2f, "WEBP_VP8L_INVALID")
    bits = int.from_bytes(raster[1:5], "little")
    require(bits >> 28 == 0 and ((bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1) == tuple(dimensions),
            "WEBP_RASTER_INVALID")
    flags = (32 if b"ICCP" in bodies else 0) | (8 if b"EXIF" in bodies else 0) | (4 if b"XMP " in bodies else 0)
    expected = ([b"VP8X"] if flags else []) + ([b"ICCP"] if flags & 32 else []) + [b"VP8L"]
    expected += ([b"EXIF"] if flags & 8 else []) + ([b"XMP "] if flags & 4 else [])
    require(chunks == expected, "WEBP_CHUNK_ORDER_INVALID")
    if flags:
        header = bodies[b"VP8X"]
        require(len(header) == 10 and header[:4] == bytes([flags, 0, 0, 0]) and
                (int.from_bytes(header[4:7], "little") + 1,
                 int.from_bytes(header[7:10], "little") + 1) == tuple(dimensions), "WEBP_VP8X_INVALID")
    return [kind.decode("ascii") for kind in chunks]


def validate_candidate(data, format_name, pixels, source_metadata, dimensions=DIMENSIONS):
    """Başarı için hash yerine bütün RGB ve ham ICC byte'larını karşılaştırır."""
    chunks = webp_chunks(data, dimensions) if format_name == "WEBP" else None
    with Image.open(BytesIO(data)) as structure:
        structure.verify()
    started = time.perf_counter()
    with Image.open(BytesIO(data)) as image:
        require(image.format == format_name and image.size == tuple(dimensions) and
                image.mode == "RGB" and getattr(image, "n_frames", 1) == 1, "CANDIDATE_FORMAT_OR_GEOMETRY")
        image.load()
        decode_millis = (time.perf_counter() - started) * 1000
        require(image.tobytes() == pixels, "CANDIDATE_RGB_MISMATCH")
        observed = metadata(image)
        require(observed["icc"] == source_metadata["icc"], "CANDIDATE_ICC_MISMATCH")
        require(274 not in image.getexif(), "CANDIDATE_ORIENTATION_PRESENT")
    return {"format": format_name, "bytes": len(data), "sha256": digest(data),
            "dimensions": list(dimensions), "allRgbBytesEqual": True, "allIccBytesEqual": True,
            "rgbBytesCompared": len(pixels), "rgbSha256": digest(pixels), "webpChunks": chunks,
            "decodeOpenLoadMillis": decode_millis, "metadata": metadata_summary(observed),
            "metadataEqual": {key: observed[key] == source_metadata[key] for key in observed}}


class BudgetBuffer(BytesIO):
    """Encoder'ın çıktı bütçesini bellekte de aşmasını önler."""
    def __init__(self, limit):
        super().__init__()
        self.limit = limit

    def write(self, data):
        require(max(len(self.getbuffer()), self.tell() + len(data)) <= self.limit, "OUTPUT_BUDGET_EXCEEDED")
        return super().write(data)


def encode_candidate(pixels, values, format_name, budget, dimensions=DIMENSIONS):
    options = dict(SETTINGS[format_name])
    if values["icc"] is not None:
        options["icc_profile"] = values["icc"]
    if values["exif"] is not None:
        options["exif"] = values["exif"]
    if values["xmp"] is not None:
        if format_name == "WEBP":
            options["xmp"] = values["xmp"]
        else:
            pnginfo = PngImagePlugin.PngInfo()
            pnginfo.add_itxt("XML:com.adobe.xmp", values["xmp"].decode("utf-8"))
            options["pnginfo"] = pnginfo
    Image.init()
    with Image.frombytes("RGB", tuple(dimensions), pixels) as image, BudgetBuffer(budget) as output:
        started = time.perf_counter()
        image.save(output, format=format_name, **options)
        elapsed = (time.perf_counter() - started) * 1000
        return output.getvalue(), elapsed
