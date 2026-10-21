"""Pillow gözlemi üretir; P2 kabulünü veya görsel kaliteyi değerlendirmez."""
from io import BytesIO
from hashlib import sha256
import warnings

from PIL import Image, ImageFile, __version__ as PILLOW_VERSION

from p5b_source_io import PreflightError, require

MAX_PIXELS = 40_000_000


def inspect_image(data):
    result = {"p2Result": "NOT_EVALUATED", "fullDecode": False, "structureVerified": False,
              "pillowVersion": PILLOW_VERSION, "format": None, "mime": None, "width": None,
              "height": None, "mode": None, "frames": None, "animated": None,
              "exifPresent": None, "orientation": None, "iccPresent": None,
              "alphaChannel": None, "transparencyMetadata": None, "transparentPixels": None,
              "reviewSignals": [], "errors": []}
    # Çok kareli/diğer biçimler gözlenir fakat bu dar pilot aracıyla decode edilmez.
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error")
            with Image.open(BytesIO(data)) as image:
                width, height = image.size
                result.update(format=image.format, mime=Image.MIME.get(image.format), width=width,
                              height=height, mode=image.mode, frames=getattr(image, "n_frames", 1),
                              animated=bool(getattr(image, "is_animated", False)))
                require(width > 0 and height > 0 and width * height <= MAX_PIXELS, "PIXEL_LIMIT")
                exif = image.getexif()
                icc = image.info.get("icc_profile")
                orientation = exif.get(274)
                result.update(exifPresent="exif" in image.info or bool(exif), exifEntryCount=len(exif),
                              orientation=orientation if type(orientation) is int else None,
                              iccPresent=icc is not None, iccBytes=len(icc) if icc is not None else 0,
                              iccSha256=sha256(icc).hexdigest() if icc is not None else None,
                              alphaChannel="A" in image.getbands(),
                              transparencyMetadata="transparency" in image.info)
                require(image.format in ("PNG", "JPEG"), "UNSUPPORTED_FORMAT")
                require(result["frames"] == 1 and not result["animated"], "MULTIFRAME_UNSUPPORTED")
            with Image.open(BytesIO(data)) as structure:
                structure.verify()
                result["structureVerified"] = True
            with Image.open(BytesIO(data)) as image:
                require(not ImageFile.LOAD_TRUNCATED_IMAGES, "TRUNCATED_DECODING_ENABLED")
                image.load()
                result["fullDecode"] = True
                result["transparentPixels"] = False
                result["alphaExtrema"] = None
                if result["alphaChannel"] or result["transparencyMetadata"]:
                    alpha = image.getchannel("A") if result["alphaChannel"] else image.convert("RGBA").getchannel("A")
                    result["alphaExtrema"] = list(alpha.getextrema())
                    result["transparentPixels"] = result["alphaExtrema"][0] < 255
    except PreflightError as error:
        result["errors"].append(str(error))
    except (Image.DecompressionBombError, Image.DecompressionBombWarning):
        result["errors"].append("PIXEL_LIMIT")
    except Exception:
        result["errors"].append("IMAGE_INSPECTION_FAILED")
    result["reviewSignals"] = [code for observed, code in (
        (result["exifPresent"], "EXIF_PRESENT"),
        (result["orientation"] is not None and result["orientation"] != 1, "NONIDENTITY_ORIENTATION"),
        (result["iccPresent"], "ICC_PRESENT"), (result["alphaChannel"], "ALPHA_CHANNEL_PRESENT"),
        (result["transparencyMetadata"], "TRANSPARENCY_METADATA_PRESENT"),
        (result["transparentPixels"], "TRANSPARENT_PIXELS_PRESENT"),
        (result["mode"] is not None and result["mode"] != "RGB", "NON_RGB_MODE"),
    ) if observed]
    return result
