import 'dart:async';
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

  int get _ownedCount =>
      _files.values.fold<int>(0, (n, f) => n + f.length) +
      _pending.length +
      _metadataFailures.length;

  Future<void> _admissionTail = Future.value();
  @override
  Future<File> createFile(String name) {
    if (maximumBytes == null) return _createFile(name);
    final completion = Completer<void>();
    final previous = _admissionTail;
    _admissionTail = completion.future;
    return (() async {
      await previous;
      try {
        return await _createFile(name);
      } finally {
        completion.complete();
      }
    })();
  }

  Future<File> _createFile(String name) async {
    if (_closed) throw StateError('Library image storage is closed');
    final owner = Zone.current[ownerKey] as String?;
    final known =
        _metadata[name] ??
        _files.values.map((f) => f[name]).whereType<File>().firstOrNull;
    if (known != null) return known;
    if (owner != null && !_pending.containsKey(name)) {
      if (_ownedCount >= capacity) await _retryFailed();
      if (_ownedCount >= capacity) {
        throw StateError('Library image cleanup capacity reached');
      }
      if (maximumBytes != null) {
        final retained = <String, File>{..._metadata};
        for (final files in _files.values) {
          retained.addAll(files);
        }
        if (retained.length + _pending.length >= capacity) {
          throw StateError('Artwork retained file capacity reached');
        }
        var bytes = _pending.length * reservedFileBytes;
        for (final entry in retained.entries) {
          final owned = _files.values.any(
            (files) => files.containsKey(entry.key),
          );
          bytes += owned
              ? reservedFileBytes
              : await entry.value.exists()
              ? await entry.value.length()
              : 0;
        }
        if (bytes + reservedFileBytes > maximumBytes!) {
          throw StateError('Artwork disk admission is full');
        }
      }
      // Reserve ownership before the delegate can physically create anything.
      _pending[name] = owner;
    }
    final file = await delegate.createFile(name);
    final reserved = _pending.remove(name);
    if (reserved != null) (_files[reserved] ??= {})[name] = file;
    return file;
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
        } else if (await entry.value.exists()) {
          await entry.value.delete();
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
