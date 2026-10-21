"""P5b özel girdilerini sınırlı ve salt okunur dosya tanıtıcılarıyla açar."""
import os
from pathlib import Path
import stat

MAX_FILE_BYTES = 64 * 1024 * 1024
ROOT = Path(__file__).absolute().parents[2]


class PreflightError(ValueError):
    """Özel yolları veya dosya içeriğini taşımayan hata kodu."""


def require(condition, code):
    if not condition:
        raise PreflightError(code)


def open_regular(path, flags=os.O_RDONLY):
    """Üst dizinler dahil symlink izlemez; FIFO açılışında beklemez."""
    path = Path(path).absolute()
    require(".." not in path.parts and not path.is_relative_to(ROOT), "PRIVATE_PATH_REQUIRED")
    parent = os.open("/", os.O_RDONLY | os.O_DIRECTORY)
    try:
        for component in path.parts[1:-1]:
            child = os.open(component, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent)
            os.close(parent)
            parent = child
        descriptor = os.open(path.name, flags | os.O_NOFOLLOW | os.O_NONBLOCK, 0o600, dir_fd=parent)
        if not stat.S_ISREG(os.fstat(descriptor).st_mode):
            os.close(descriptor)
            raise PreflightError("REGULAR_FILE_REQUIRED")
        return descriptor
    except OSError:
        raise PreflightError("FILE_UNAVAILABLE_OR_SYMLINK") from None
    finally:
        os.close(parent)


def read_bounded(path, limit=MAX_FILE_BYTES):
    descriptor = open_regular(path)
    try:
        with os.fdopen(descriptor, "rb") as handle:
            before = os.fstat(handle.fileno())
            require(0 < before.st_size <= limit, "FILE_BYTE_LIMIT_OR_EMPTY")
            data = handle.read(limit + 1)
            after = os.fstat(handle.fileno())
    except OSError:
        raise PreflightError("FILE_READ_FAILED") from None
    require(0 < len(data) <= limit, "FILE_BYTE_LIMIT_OR_EMPTY")
    require((before.st_size, before.st_mtime_ns, before.st_ctime_ns) ==
            (after.st_size, after.st_mtime_ns, after.st_ctime_ns) and len(data) == after.st_size,
            "FILE_CHANGED_DURING_READ")
    return data


def write_report(path, report_bytes):
    # O_EXCL var olan kaynak, hardlink ve raporların üzerine yazılmasını önler.
    descriptor = open_regular(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL)
    with os.fdopen(descriptor, "wb") as handle:
        handle.write(report_bytes)
