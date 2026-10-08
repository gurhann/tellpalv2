import 'dart:async';
import 'dart:convert';
import 'dart:io' as io;
import 'package:file/file.dart' as files;
import 'package:file/file.dart' show File;
import 'package:flutter_cache_manager/flutter_cache_manager.dart';

/// Tracks package-created partial files as well as completed cache entries.
class LibraryCacheFiles implements FileSystem {
  LibraryCacheFiles(
    this.delegate, {
    this.capacity = 80,
    this.maximumBytes,
    this.reservedFileBytes = 0,
  });
  final int capacity;
  final int? maximumBytes;
  final int reservedFileBytes;
  final FileSystem delegate;
  static final ownerKey = Object();
  final _files = <String, Map<String, File>>{};
  final _failed = <String>{};
  bool _closed = false;
  final _metadata = <String, File>{};
  final _pending = <String, String>{};
  final _metadataFailures = <String>{};
  Future<void> Function(String name)? beforeWrite;
  Set<String> Function()? protectedPaths;
  Set<String> get writingNames =>
      _files.values.expand((files) => files.keys).toSet();
  int get physicalOwnerCount => {
    ..._metadata.keys,
    ...writingNames,
    ..._pending.keys,
    ..._metadataFailures,
  }.length;
  bool ownsName(String name) =>
      _metadata.containsKey(name) ||
      writingNames.contains(name) ||
      _pending.containsKey(name);

  Future<void> _admissionTail = Future.value();
  @override
  Future<File> createFile(String name) {
    // The package also resolves absent old paths for fire-and-forget deletion.
    // A path handle is not a disk write and cannot consume transfer admission.
    return (() async {
      final file = await delegate.createFile(name);
      if (!_closed && await file.exists()) {
        _metadata.putIfAbsent(name, () => file);
      } else {
        _metadata.remove(name);
      }
      return _AdmittedArtworkFile(
        file,
        this,
        name,
        Zone.current[ownerKey] as String?,
      );
    })();
  }

  Future<void> _admitWrite(String name, File file, String? owner) {
    final completion = Completer<void>();
    final previous = _admissionTail;
    _admissionTail = completion.future;
    return (() async {
      await previous;
      try {
        if (_closed) throw StateError('Library image storage is closed');
        if (owner == null) throw StateError('Artwork write owner required');
        if (_files[owner]?.containsKey(name) == true) return;
        await _retryFailed();
        await beforeWrite?.call(name);
        final retained = <String, File>{..._metadata};
        for (final files in _files.values) {
          retained.addAll(files);
        }
        if (!retained.containsKey(name) &&
            retained.length + _pending.length >= capacity) {
          throw StateError('Artwork retained file capacity reached');
        }
        var bytes = _pending.length * reservedFileBytes;
        for (final entry in retained.entries) {
          if (entry.key == name) continue;
          final writing = _files.values.any(
            (files) => files.containsKey(entry.key),
          );
          bytes += writing
              ? reservedFileBytes
              : await entry.value.exists()
              ? await entry.value.length()
              : 0;
        }
        if (maximumBytes != null && bytes + reservedFileBytes > maximumBytes!) {
          throw StateError('Artwork disk admission is full');
        }
        // Admission is serialized and recorded before the underlying writer opens.
        (_files[owner] ??= {})[name] = file;
      } finally {
        completion.complete();
      }
    })();
  }

  Future<void> release(String owner, Set<String?> retained) async {
    Object? failure;
    StackTrace? trace;
    for (final name
        in _pending.keys.where((n) => _pending[n] == owner).toList()) {
      try {
        if (_closed) {
          throw StateError('Unresolved Library image file ownership');
        }
        final file = await delegate.createFile(name);
        (_files[owner] ??= {})[name] = file;
        _pending.remove(name);
      } catch (error, stack) {
        failure ??= error;
        trace ??= stack;
      }
    }
    final files = _files[owner];
    for (final entry in files?.entries.toList() ?? <MapEntry<String, File>>[]) {
      try {
        if (retained.contains(entry.key)) {
          _metadata[entry.key] = entry.value;
        } else {
          if (await entry.value.exists()) await entry.value.delete();
          _metadata.remove(entry.key);
        }
        files!.remove(entry.key);
        _failed.remove(entry.value.path);
      } catch (error, stack) {
        _failed.add(entry.value.path);
        failure ??= error;
        trace ??= stack;
      }
    }
    if (files?.isEmpty ?? false) _files.remove(owner);
    if (failure != null) Error.throwWithStackTrace(failure, trace!);
  }

  /// Remove using the configured filesystem, retaining the handle on failure.
  Future<void> retainMetadataFile(String name) async {
    _metadata[name] = _metadata[name] ?? await createFile(name);
  }

  Future<void> removeMetadataFile(String name) async {
    final file = _metadata[name] ?? await createFile(name);
    _metadata[name] = file;
    _metadataFailures.add(name);
    try {
      if (await file.exists()) await file.delete();
      _metadata.remove(name);
      _metadataFailures.remove(name);
    } catch (_) {
      _metadataFailures.add(name);
      rethrow;
    }
  }

  Future<void> clearAndClose() async {
    _closed = true;
    Object? failure;
    StackTrace? trace;
    for (final name in _metadataFailures.toList()) {
      try {
        await removeMetadataFile(name);
      } catch (error, stack) {
        failure ??= error;
        trace ??= stack;
      }
    }
    // Each pass visits its initial finite snapshot once; failures retain ownership.
    for (final owner in {..._files.keys, ..._pending.values}) {
      try {
        await release(owner, const {});
      } catch (error, stack) {
        failure ??= error;
        trace ??= stack;
      }
    }
    if (failure != null) Error.throwWithStackTrace(failure, trace!);
  }

  Future<void> _retryFailed() async {
    for (final name in _metadataFailures.toList()) {
      try {
        await removeMetadataFile(name);
      } catch (_) {}
    }
    for (final files in _files.values.toList()) {
      for (final entry in files.entries.toList()) {
        if (!_failed.contains(entry.value.path)) continue;
        try {
          if (await entry.value.exists()) await entry.value.delete();
          _metadata.remove(entry.key);
          files.remove(entry.key);
          _failed.remove(entry.value.path);
        } catch (_) {
          // Capacity remains occupied until deletion actually succeeds.
        }
      }
    }
    _files.removeWhere((_, files) => files.isEmpty);
  }
}

/// Defers physical file/byte admission to the package's awaited write channel.
class _AdmittedArtworkFile
    extends files.ForwardingFileSystemEntity<File, io.File>
    with files.ForwardingFile {
  _AdmittedArtworkFile(this.delegate, this.lifecycle, this.name, this.owner);
  @override
  final File delegate;
  final LibraryCacheFiles lifecycle;
  final String name;
  final String? owner;
  @override
  files.FileSystem get fileSystem => delegate.fileSystem;
  @override
  File wrapFile(io.File file) =>
      _AdmittedArtworkFile(file as File, lifecycle, name, owner);
  @override
  files.Directory wrapDirectory(io.Directory directory) =>
      fileSystem.directory(directory.path);
  @override
  files.Link wrapLink(io.Link link) => fileSystem.link(link.path);
  @override
  io.IOSink openWrite({
    io.FileMode mode = io.FileMode.write,
    Encoding encoding = utf8,
  }) {
    if (mode != io.FileMode.write) {
      throw UnsupportedError('Artwork append writes are not supported');
    }
    return io.IOSink(_ArtworkWriteConsumer(this, mode, encoding));
  }

  @override
  Future<File> writeAsBytes(
    List<int> bytes, {
    io.FileMode mode = io.FileMode.write,
    bool flush = false,
  }) async {
    if (mode != io.FileMode.write) {
      throw UnsupportedError('Artwork append writes are not supported');
    }
    if (lifecycle.maximumBytes != null &&
        bytes.length > lifecycle.reservedFileBytes) {
      throw StateError('Artwork exceeds encoded byte bound');
    }
    await lifecycle._admitWrite(name, delegate, owner);
    await delegate.writeAsBytes(bytes, mode: mode, flush: flush);
    return this;
  }

  @override
  Future<File> writeAsString(
    String contents, {
    io.FileMode mode = io.FileMode.write,
    Encoding encoding = utf8,
    bool flush = false,
  }) => writeAsBytes(encoding.encode(contents), mode: mode, flush: flush);
  @override
  void writeAsBytesSync(
    List<int> bytes, {
    io.FileMode mode = io.FileMode.write,
    bool flush = false,
  }) => throw UnsupportedError('Artwork synchronous writes are not supported');
  @override
  void writeAsStringSync(
    String contents, {
    io.FileMode mode = io.FileMode.write,
    Encoding encoding = utf8,
    bool flush = false,
  }) => throw UnsupportedError('Artwork synchronous writes are not supported');
  @override
  Future<File> create({bool recursive = false, bool exclusive = false}) async {
    await lifecycle._admitWrite(name, delegate, owner);
    await delegate.create(recursive: recursive, exclusive: exclusive);
    return this;
  }

  @override
  void createSync({bool recursive = false, bool exclusive = false}) =>
      throw UnsupportedError('Artwork synchronous creation is not supported');
  @override
  Future<files.RandomAccessFile> open({io.FileMode mode = io.FileMode.read}) {
    if (mode != io.FileMode.read) {
      throw UnsupportedError('Artwork random writes are not supported');
    }
    return delegate.open(mode: mode);
  }

  @override
  files.RandomAccessFile openSync({io.FileMode mode = io.FileMode.read}) {
    if (mode != io.FileMode.read) {
      throw UnsupportedError('Artwork random writes are not supported');
    }
    return delegate.openSync(mode: mode);
  }

  @override
  Future<File> copy(String newPath) async =>
      throw UnsupportedError('Artwork copying is not supported');
  @override
  File copySync(String newPath) =>
      throw UnsupportedError('Artwork copying is not supported');
  @override
  Future<File> rename(String newPath) async =>
      throw UnsupportedError('Artwork renaming is not supported');
  @override
  File renameSync(String newPath) =>
      throw UnsupportedError('Artwork renaming is not supported');

  @override
  Future<File> delete({bool recursive = false}) async {
    await delegate.delete(recursive: recursive);
    lifecycle._metadata.remove(name);
    lifecycle._metadataFailures.remove(name);
    lifecycle._failed.remove(delegate.path);
    for (final owned in lifecycle._files.values) {
      owned.remove(name);
    }
    lifecycle._files.removeWhere((_, owned) => owned.isEmpty);
    return this;
  }
}

class _ArtworkWriteConsumer implements StreamConsumer<List<int>> {
  _ArtworkWriteConsumer(this.file, this.mode, this.encoding);
  final _AdmittedArtworkFile file;
  final io.FileMode mode;
  final Encoding encoding;
  @override
  Future<void> addStream(Stream<List<int>> stream) async {
    try {
      await file.lifecycle._admitWrite(file.name, file.delegate, file.owner);
    } catch (_) {
      // Release the actual response/socket even when no physical writer was admitted.
      await stream.listen(null).cancel();
      rethrow;
    }
    var received = 0;
    await stream
        .map((chunk) {
          received += chunk.length;
          if (file.lifecycle.maximumBytes != null &&
              received > file.lifecycle.reservedFileBytes) {
            throw StateError('Artwork exceeds encoded byte bound');
          }
          return chunk;
        })
        .pipe(file.delegate.openWrite(mode: mode, encoding: encoding));
  }

  @override
  Future<void> close() async {}
}

/// Keeps package download admission finite without replacing its storage pipeline.
class LibraryCacheAdmission {
  LibraryCacheAdmission(this.queueWait);
  final Duration queueWait;
  int _active = 0;
  bool _closed = false;
  final _queue = <Completer<void>>[];
  Future<void> enter(Future<void> cancelled) async {
    if (_closed) throw StateError('Library image cache is closed');
    if (_active < 10) {
      _active++;
      return;
    }
    if (_queue.length >= 80) throw StateError('Library image queue is full');
    final slot = Completer<void>();
    _queue.add(slot);
    try {
      await Future.any([
        slot.future,
        cancelled.then(
          (_) => throw StateError('Library image attempt cancelled'),
        ),
      ]).timeout(queueWait);
    } catch (_) {
      if (!_queue.remove(slot)) leave();
      rethrow;
    }
    if (_closed) {
      leave();
      throw StateError('Library image cache is closed');
    }
  }

  void leave() {
    if (_queue.isNotEmpty && !_closed) {
      _queue.removeAt(0).complete();
    } else {
      _active--;
    }
  }

  void close() {
    _closed = true;
    for (final slot in _queue) {
      slot.completeError(StateError('Library image cache is closed'));
    }
  }
}
