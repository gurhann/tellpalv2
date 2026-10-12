#!/usr/bin/env python3
"""Read-only admission of the experiment's Apple-native XCTest runner linkage."""
import argparse
import os
from pathlib import Path
import plistlib
import re
import stat
import struct
import sys
import time


class NativeLinkageRefusal(Exception):
    pass


APPLE_FRAMEWORKS = frozenset(('UIKit', 'Foundation', 'CryptoKit', 'Security', 'CoreFoundation'))
XCTEST_FRAMEWORKS = frozenset(('XCTest', 'XCTestCore', 'XCTestSupport', 'XCUIAutomation', 'XCTAutomationSupport'))
XCTEST_DYLIBS = frozenset(('libXCTestSwiftSupport.dylib',))
SWIFT_DYLIB = re.compile(r'libswift[A-Za-z0-9_]+\.dylib')
DYLIB_COMMANDS = frozenset((0xC, 0x20, 0x80000018, 0x8000001F, 0x80000023))
# These carry layout/link metadata, not another install name or search path.
METADATA_COMMANDS = frozenset((0x2, 0xB, 0x16, 0x19, 0x1B, 0x1D, 0x22, 0x25,
    0x26, 0x29, 0x2A, 0x2C, 0x2E, 0x31, 0x32, 0x80000022, 0x80000028,
    0x80000033, 0x80000034))
APPLE_RPATHS = frozenset(('/usr/lib/swift', '@loader_path/Frameworks', '@executable_path/Frameworks',
    '/System/Developer/Library/Frameworks', '/System/Developer/usr/lib',
    '/Developer/Library/Frameworks', '/Developer/usr/lib'))
MAX_IMAGE_BYTES = 256 * 1024 * 1024
MAX_COMMAND_BYTES = 1024 * 1024
MAX_COMMANDS = 4096


def require(condition):
    if not condition:
        raise NativeLinkageRefusal()


def allowed_dependency(name):
    """Permit exact Apple frameworks and system/Swift/XCTest runtime install names."""
    if name in {f'/System/Library/Frameworks/{item}.framework/{item}' for item in APPLE_FRAMEWORKS}:
        return True
    if name == '/System/Library/PrivateFrameworks/AssertionServices.framework/AssertionServices':
        return True
    if re.fullmatch(r'/usr/lib/[A-Za-z0-9_.+-]+\.dylib', name):
        return True
    if name.startswith('/usr/lib/swift/') and SWIFT_DYLIB.fullmatch(name[len('/usr/lib/swift/'):]):
        return True
    for prefix in ('@rpath/', '@executable_path/Frameworks/', '@loader_path/Frameworks/'):
        if name.startswith(prefix):
            suffix = name[len(prefix):]
            return suffix in {f'{item}.framework/{item}' for item in XCTEST_FRAMEWORKS} or \
                suffix in XCTEST_DYLIBS or SWIFT_DYLIB.fullmatch(suffix) is not None
    return False


def parse_load_commands(header, commands, *, file_size, expected_filetype):
    """Parse only bounded thin arm64 Mach-O headers; never trust tool output text."""
    require(len(header) == 32)
    magic, cpu, subtype, filetype, count, size, _flags, _reserved = struct.unpack('<8I', header)
    require(magic == 0xFEEDFACF and cpu == 0x0100000C and subtype in (0, 2) and filetype == expected_filetype)
    require(1 <= count <= MAX_COMMANDS and 8 <= size <= MAX_COMMAND_BYTES and
        len(commands) == size and 32 + size <= file_size)
    names, offset = [], 0
    for _ in range(count):
        require(offset + 8 <= size)
        command, length = struct.unpack_from('<2I', commands, offset)
        require(length >= 8 and length % 8 == 0 and offset + length <= size)
        if command in DYLIB_COMMANDS or command in (0xD, 0xE, 0x8000001C):
            minimum = 24 if command in DYLIB_COMMANDS or command == 0xD else 12
            require(length >= minimum)
            name_offset = struct.unpack_from('<I', commands, offset + 8)[0]
            require(minimum <= name_offset < length)
            start = offset + name_offset
            end = commands.find(b'\0', start, offset + length)
            require(end > start)
            try:
                name = commands[start:end].decode('ascii')
            except UnicodeError:
                raise NativeLinkageRefusal() from None
            if command in DYLIB_COMMANDS:
                require(allowed_dependency(name) and name not in names)
                names.append(name)
            elif command == 0xD:  # Embedded support dylibs must also identify themselves as Apple runtime.
                require(expected_filetype == 6 and allowed_dependency(name))
            elif command == 0xE:
                require(expected_filetype == 2 and name == '/usr/lib/dyld')
            else:
                require(name in APPLE_RPATHS)
        else:
            # Unknown commands, including library injection and obsolete FVMLIB loads, refuse.
            require(command in METADATA_COMMANDS)
        offset += length
    require(offset == size and names)
    return tuple(names)


def image_slices(descriptor, file_size):
    """Inspect every slice; Xcode's device runner can use a one-slice fat wrapper."""
    header = os.pread(descriptor, 8, 0)
    require(len(header) == 8)
    if header[:4] == b'\xcf\xfa\xed\xfe':
        return ((0, file_size),)
    require(header[:4] == b'\xca\xfe\xba\xbe')
    count = struct.unpack_from('>I', header, 4)[0]
    require(1 <= count <= 2)
    table = os.pread(descriptor, 20 * count, 8)
    require(len(table) == 20 * count)
    slices, subtypes = [], set()
    for index in range(count):
        cpu, subtype, offset, size, alignment = struct.unpack_from('>5I', table, index * 20)
        require(cpu == 0x0100000C and subtype in (0, 2) and subtype not in subtypes and
            0 <= alignment <= 20 and offset >= 8 + len(table) and offset % (1 << alignment) == 0 and
            size >= 40 and offset + size <= file_size)
        require(all(offset + size <= previous or previous + length <= offset for previous, length in slices))
        slice_header = os.pread(descriptor, 12, offset)
        require(len(slice_header) == 12 and struct.unpack('<3I', slice_header) == (0xFEEDFACF, cpu, subtype))
        slices.append((offset, size))
        subtypes.add(subtype)
    return tuple(slices)


def signature(info):
    return (info.st_dev, info.st_ino, info.st_mode, info.st_size, info.st_mtime_ns, info.st_ctime_ns)


def checked_path(path, *, directory):
    require(path.is_absolute() and path.resolve(strict=True) == path)
    info = path.lstat()
    require(stat.S_ISDIR(info.st_mode) if directory else stat.S_ISREG(info.st_mode))
    return info


def image_dependencies(path, *, expected_filetype, deadline):
    require(time.monotonic() < deadline)
    before = checked_path(path, directory=False)
    require(32 <= before.st_size <= MAX_IMAGE_BYTES)
    descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    try:
        opened = os.fstat(descriptor)
        require(signature(before) == signature(opened))
        names = []
        for offset, file_size in image_slices(descriptor, opened.st_size):
            header = os.pread(descriptor, 32, offset)
            require(len(header) == 32)
            size = struct.unpack_from('<I', header, 20)[0]
            require(8 <= size <= MAX_COMMAND_BYTES and 32 + size <= file_size)
            commands = os.pread(descriptor, size, offset + 32)
            names.extend(parse_load_commands(header, commands, file_size=file_size, expected_filetype=expected_filetype))
        require(signature(opened) == signature(os.fstat(descriptor)) and time.monotonic() < deadline)
        return tuple(names)
    finally:
        os.close(descriptor)


def bundle_info(path):
    info = checked_path(path, directory=False)
    require(1 <= info.st_size <= 64 * 1024)
    descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    try:
        require(signature(info) == signature(os.fstat(descriptor)))
        data = os.read(descriptor, 64 * 1024 + 1)
        require(len(data) == info.st_size and signature(info) == signature(os.fstat(descriptor)))
        values = plistlib.loads(data)
        require(type(values) is dict)
        return values, signature(info)
    finally:
        os.close(descriptor)


def verify_runner(runner, *, deadline=None):
    deadline = time.monotonic() + 10 if deadline is None else deadline
    require(time.monotonic() < deadline and runner.name == 'ImageExperimentUITests-Runner.app')
    initial = checked_path(runner, directory=True)
    info, runner_info_signature = bundle_info(runner / 'Info.plist')
    identities = {f'com.tellpal.tellpalMobilePoc.imageexperiment.{mode}.uitests.xctrunner' for mode in ('profile', 'release')}
    require(info.get('CFBundleIdentifier') in identities and info.get('CFBundleExecutable') == 'ImageExperimentUITests-Runner')
    plugin = runner / 'PlugIns/ImageExperimentUITests.xctest'
    plugin_signature = signature(checked_path(plugin, directory=True))
    test, test_info_signature = bundle_info(plugin / 'Info.plist')
    require(test.get('CFBundleIdentifier') == info['CFBundleIdentifier'].removesuffix('.xctrunner') and
        test.get('CFBundleExecutable') == 'ImageExperimentUITests')
    images = [(runner / 'ImageExperimentUITests-Runner', 2), (plugin / 'ImageExperimentUITests', 8)]
    entries, signatures = 0, [(runner / 'Info.plist', runner_info_signature, False),
        (plugin, plugin_signature, True), (plugin / 'Info.plist', test_info_signature, False)]
    def traversal_error(_error):
        raise NativeLinkageRefusal()
    for directory, directories, files in os.walk(runner, followlinks=False, onerror=traversal_error):
        require(time.monotonic() < deadline)
        for name in directories + files:
            entries += 1
            require(entries <= 65536)
            path = Path(directory) / name
            metadata = path.lstat()
            require(stat.S_ISDIR(metadata.st_mode) or stat.S_ISREG(metadata.st_mode))
            signatures.append((path, signature(metadata), stat.S_ISDIR(metadata.st_mode)))
            if name.endswith('.framework'):
                framework = name.removesuffix('.framework')
                require(stat.S_ISDIR(metadata.st_mode) and framework in XCTEST_FRAMEWORKS)
                images.append((path / framework, 6))
            elif name.endswith('.dylib'):
                require(stat.S_ISREG(metadata.st_mode) and (name in XCTEST_DYLIBS or SWIFT_DYLIB.fullmatch(name)))
                images.append((path, 6))
            elif name.endswith('.xctest'):
                require(path == plugin)
            elif name.endswith(('.app', '.appex')):
                raise NativeLinkageRefusal()
    require(len(images) <= 128)
    dependencies = [image_dependencies(path, expected_filetype=filetype, deadline=deadline) for path, filetype in images]
    for path, initial_signature, directory in signatures:
        require(time.monotonic() < deadline and signature(checked_path(path, directory=directory)) == initial_signature)
    require(signature(initial) == signature(runner.lstat()) and time.monotonic() < deadline)
    return {'status': 'verified', 'reason': 'apple_native_only', 'imageCount': len(images),
        'dependencyCount': sum(map(len, dependencies))}


class SafeParser(argparse.ArgumentParser):
    def error(self, _message):
        raise NativeLinkageRefusal()


def main():
    parser = SafeParser(add_help=False)
    parser.add_argument('--runner', required=True)
    arguments = parser.parse_args()
    verify_runner(Path(arguments.runner))
    print('NATIVE_TEST_LINKAGE_PASS')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        print('NATIVE_TEST_LINKAGE_REFUSED')
        sys.exit(1)
