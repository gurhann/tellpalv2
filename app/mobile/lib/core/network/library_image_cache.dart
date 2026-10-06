import 'dart:async';
import 'package:file/file.dart' show File;
import 'package:flutter/foundation.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:http/http.dart' as http;
import 'package:injectable/injectable.dart';
import '../../app/library_configuration.dart';
import 'draining_cache_info_repository.dart';
import 'library_cache_store.dart';
import 'library_cache_lifecycle.dart';
import 'library_transfer_file_service.dart';

/// Account/access-scoped thumbnails; CacheManager remains the storage owner.
@lazySingleton
class LibraryImageCache extends CacheManager with ImageCacheManager {
  LibraryImageCache(LibraryConfiguration configuration)
    : this._configured(
        _withLifecycle(
          Config(
            'library-${configuration.context.accessIdentity.hashCode.abs()}',
            stalePeriod: const Duration(minutes: 10),
            maxNrOfCacheObjects: 80,
          ),
          configuration.artworkWait,
        ),
      );

  @visibleForTesting
  LibraryImageCache.forTesting(
    Config configuration, {
    Duration artworkWait = const Duration(seconds: 10),
    http.Client Function()? clientFactory,
  }) : this._configured(
         _withLifecycle(
           configuration,
           artworkWait,
           delegate: clientFactory == null ? configuration.fileService : null,
           clientFactory: clientFactory,
         ),
       );

  // Explicit configuration couples the store and file lifecycle wrappers.
  // ignore: use_super_parameters
  LibraryImageCache._configured(Config configuration)
    : _repository = configuration.repo as DrainingCacheInfoRepository,
      _files = configuration.fileSystem as LibraryCacheFiles,
      _transport = configuration.fileService as LibraryTransferFileService,
      _admission = LibraryCacheAdmission(
        Duration(
          microseconds:
              (configuration.fileService as LibraryTransferFileService)
                  .totalWait
                  .inMicroseconds ~/
              4,
        ),
      ),
      // Package documents this public seam for custom stores; vendor sources stay intact.
      // ignore: invalid_use_of_visible_for_testing_member
      super.custom(configuration, cacheStore: LibraryCacheStore(configuration));

  final DrainingCacheInfoRepository _repository;
  final LibraryCacheFiles _files;
  final LibraryTransferFileService _transport;
  final LibraryCacheAdmission _admission;
  // Includes metadata, transfer, publication and cleanup, even after cancel.
  static const _maximumOwnedAttempts = 90;
  final _operations = <_Operation>{};

  @visibleForTesting
  int get ownedAttemptCount => _operations.length;
  final _delivered = <String, FileInfo>{};
  final _warmHits = <String, (String, FileInfo)>{};
  final _promotions = <String, Future<void>>{};
  final _storage = <Future<void>>{};
  final _promotionFiles = <File>{};
  final _retired = <String, (File, Timer)>{};
  final _retirementCleanup = <String, Future<void>>{};
  int _retirementReservations = 0;
  int _nextOwner = 0;
  bool _closed = false;
  Future<void>? _closing;

  static Config _withLifecycle(
    Config configuration,
    Duration artworkWait, {
    FileService? delegate,
    http.Client Function()? clientFactory,
  }) => Config(
    configuration.cacheKey,
    stalePeriod: configuration.stalePeriod,
    maxNrOfCacheObjects: configuration.maxNrOfCacheObjects,
    repo: DrainingCacheInfoRepository(configuration.repo),
    fileSystem: LibraryCacheFiles(configuration.fileSystem),
    fileService: LibraryTransferFileService(
      artworkWait: artworkWait,
      delegate: delegate,
      clientFactory: clientFactory,
    ),
  );

  static const _separator = '::library-transfer::';
  static String transferKey(String immutableKey, String attempt) =>
      '$immutableKey$_separator$attempt';

  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    if (_closed) {
      return Stream.error(StateError('Library image cache is closed'));
    }
    // Reserve ownership synchronously before any metadata/file work starts.
    // Slow uncancellable reads retain their slot until they genuinely settle.
    if (_operations.length >= _maximumOwnedAttempts) {
      return Stream.error(StateError('Library image attempt capacity reached'));
    }
    final operation = _Operation(key ?? url, '${++_nextOwner}');
    operation.output.onCancel = () {
      operation.cancel();
      _transport.cancel(operation.owner);
    };
    _operations.add(operation);
    operation.finished = runZoned(
      () => _read(operation, url, headers, withProgress),
      zoneValues: {LibraryCacheFiles.ownerKey: operation.owner},
    );
    return operation.output.stream;
  }

  Future<void> _read(
    _Operation operation,
    String url,
    Map<String, String>? headers,
    bool withProgress,
  ) async {
    bool admitted = false;
    try {
      final key = operation.key;
      final split = key.lastIndexOf(_separator);
      _delivered.remove(key);
      _warmHits.remove(key);
      if (split >= 0) {
        final immutable = key.substring(0, split);
        final cached = await getFileFromCache(immutable);
        if (_closed || operation.cancelled.isCompleted) return;
        if (cached != null && cached.validTill.isAfter(DateTime.now())) {
          _warmHits[key] = (immutable, cached);
          if (_warmHits.length > 80) _warmHits.remove(_warmHits.keys.first);
          operation.output.add(cached);
          return;
        }
      }
      // Surface an admitted lookup failure before CacheManager's fallback path
      // can silently treat its failed first lookup as a miss.
      await getFileFromCache(key);
      if (_closed || operation.cancelled.isCompleted) return;
      await _cleanupCapacity();
      await _admission.enter(operation.cancelled.future);
      admitted = true;
      if (_closed || operation.cancelled.isCompleted) return;
      // The ownership marker is stripped by FileService before the HTTP request.
      await for (final response in super.getFileStream(
        url,
        key: key,
        headers: {
          ...?headers,
          LibraryTransferFileService.ownershipHeader: operation.owner,
        },
        withProgress: withProgress,
      )) {
        if (_closed || operation.cancelled.isCompleted) continue;
        if (response is FileInfo) {
          await (store as LibraryCacheStore).drain();
          (store as LibraryCacheStore).rethrowFailure(operation.owner);
          if (_closed || operation.cancelled.isCompleted) continue;
        }
        if (response is FileInfo && split >= 0) {
          if (!response.validTill.isAfter(DateTime.now())) continue;
          _delivered[key] = response;
          if (_delivered.length > 80) _delivered.remove(_delivered.keys.first);
        }
        operation.output.add(response);
      }
    } catch (error, stack) {
      (store as LibraryCacheStore).discardReportedFailure(operation.owner);
      if (!_closed && !operation.cancelled.isCompleted) {
        operation.output.addError(error, stack);
      }
    } finally {
      // Consume the complete package stream even if the image provider unsubscribes.
      // Publication starts before FileInfo and must finish before file cleanup.
      try {
        await (store as LibraryCacheStore).drain();
        (store as LibraryCacheStore).rethrowFailure(operation.owner);
        await _repository.whenIdle();
        await _files.release(
          operation.owner,
          (await _repository.getAllObjects())
              .map((entry) => entry.relativePath)
              .toSet(),
        );
      } catch (error, stack) {
        if (!_closed && !operation.cancelled.isCompleted) {
          operation.output.addError(error, stack);
        }
      } finally {
        if (admitted) _admission.leave();
        _operations.remove(operation);
        unawaited(operation.output.close());
      }
    }
  }

  @override
  Future<File> getSingleFile(
    String url, {
    String? key,
    Map<String, String>? headers,
  }) async =>
      (await getFileStream(
                url,
                key: key,
                headers: headers,
              ).firstWhere((value) => value is FileInfo)
              as FileInfo)
          .file;

  @override
  Future<FileInfo> downloadFile(
    String url, {
    String? key,
    Map<String, String>? authHeaders,
    bool force = false,
  }) async =>
      await getFileStream(
            url,
            key: key,
            headers: authHeaders,
          ).firstWhere((value) => value is FileInfo)
          as FileInfo;

  @override
  Future<FileInfo?> getFileFromCache(
    String key, {
    bool ignoreMemCache = false,
  }) async {
    if (_closed) return null;
    return _trackStorage(
      () => super.getFileFromCache(key, ignoreMemCache: ignoreMemCache),
    );
  }

  @override
  Future<FileInfo?> getFileFromMemory(String key) => _closed
      ? Future.value()
      : _trackStorage(() => super.getFileFromMemory(key));

  @override
  Future<File> putFile(
    String url,
    Uint8List fileBytes, {
    String? key,
    String? eTag,
    Duration maxAge = const Duration(days: 30),
    String fileExtension = 'file',
  }) {
    if (_closed) {
      return Future.error(StateError('Library image cache is closed'));
    }
    return _ownPublication(
      () => super.putFile(
        url,
        fileBytes,
        key: key,
        eTag: eTag,
        maxAge: maxAge,
        fileExtension: fileExtension,
      ),
    );
  }

  @override
  Future<File> putFileStream(
    String url,
    Stream<List<int>> source, {
    String? key,
    String? eTag,
    Duration maxAge = const Duration(days: 30),
    String fileExtension = 'file',
  }) {
    if (_closed) {
      return Future.error(StateError('Library image cache is closed'));
    }
    return _ownPublication(
      () => super.putFileStream(
        url,
        source,
        key: key,
        eTag: eTag,
        maxAge: maxAge,
        fileExtension: fileExtension,
      ),
    );
  }

  Future<File> _ownPublication(Future<File> Function() operation) {
    final owner = 'publication-${++_nextOwner}';
    return _trackStorage(
      () => runZoned(() async {
        try {
          final file = await operation();
          await (store as LibraryCacheStore).drain();
          (store as LibraryCacheStore).rethrowFailure(owner);
          return file;
        } catch (_) {
          (store as LibraryCacheStore).discardReportedFailure(owner);
          rethrow;
        } finally {
          await _repository.whenIdle();
          await _files.release(
            owner,
            (await _repository.getAllObjects())
                .map((o) => o.relativePath)
                .toSet(),
          );
        }
      }, zoneValues: {LibraryCacheFiles.ownerKey: owner}),
    );
  }

  Future<T> _trackStorage<T>(Future<T> Function() operation) async {
    final drained = Completer<void>();
    _storage.add(drained.future);
    try {
      final result = await operation();
      await (store as LibraryCacheStore).drain();
      await _repository.whenIdle();
      return result;
    } finally {
      _storage.remove(drained.future);
      drained.complete();
    }
  }

  Future<void> rememberValidated(
    String immutableKey,
    String transferKey, {
    required bool Function() isCurrent,
  }) async {
    _warmHits.remove(transferKey);
    final delivered = _delivered.remove(transferKey);
    if (delivered == null || _closed || !isCurrent()) return;
    while (_promotions.containsKey(immutableKey)) {
      await _promotions[immutableKey];
      if (_closed || !isCurrent()) return;
    }
    final promotion = _promote(immutableKey, delivered, isCurrent);
    _promotions[immutableKey] = promotion;
    try {
      await promotion;
    } finally {
      _promotions.remove(immutableKey);
    }
  }

  /// Retry cancels its old transfer and evicts only the failed warm file it read.
  Future<void> discardFailedTransfer(String key) {
    if (_closed) return Future.value();
    return _trackStorage(() => _discardFailedTransfer(key));
  }

  Future<void> _discardFailedTransfer(String key) async {
    _delivered.remove(key);
    for (final operation
        in _operations.where((entry) => entry.key == key).toList()) {
      operation.cancel();
      _transport.cancel(operation.owner);
    }
    final hit = _warmHits.remove(key);
    if (_closed || hit == null) return;
    final current = await getFileFromCache(hit.$1);
    if (!_closed && current?.file.path == hit.$2.file.path) {
      await removeFile(hit.$1);
      if (await current!.file.exists()) await current.file.delete();
    }
  }

  Future<void> _promote(
    String immutableKey,
    FileInfo delivered,
    bool Function() isCurrent,
  ) async {
    try {
      final shared = await getFileFromCache(immutableKey);
      if (_closed || !isCurrent()) return;
      final bytes = await delivered.file.readAsBytes();
      if (_closed || !isCurrent()) return;
      if (shared != null) {
        if (shared.validTill.isAfter(DateTime.now())) return;
        final metadata = await store.retrieveCacheData(immutableKey);
        final previousBytes = await shared.file.readAsBytes();
        if (metadata == null || _closed || !isCurrent()) return;
        if (listEquals(bytes, previousBytes)) {
          // Equality with the actually decoded bytes validates this shared file.
          await (store as LibraryCacheStore).publish(
            metadata.copyWith(
              validTill: DateTime.now().add(const Duration(minutes: 10)),
            ),
          );
          return;
        }
        final transferred = (await _repository.getAllObjects())
            .where(
              (entry) => delivered.file.path.endsWith('/${entry.relativePath}'),
            )
            .firstOrNull;
        if (transferred == null || _closed || !isCurrent()) return;
        await _cleanupCapacity();
        if (_closed || !isCurrent()) return;
        if (_retired.length + _retirementReservations >= 80) return;
        _retirementReservations++;
        try {
          // Swap metadata ownership, never rewrite a file a decoder is reading.
          // The existing transfer record retains the old file within the same
          // 80-object/10-minute cache budget, rather than creating orphan history.
          _promotionFiles.add(delivered.file);
          try {
            await (store as LibraryCacheStore).publish(
              transferred.copyWith(
                relativePath: metadata.relativePath,
                validTill: DateTime.now().add(const Duration(minutes: 10)),
              ),
            );
            await (store as LibraryCacheStore).publish(
              metadata.copyWith(
                relativePath: transferred.relativePath,
                length: bytes.length,
                validTill: DateTime.now().add(const Duration(minutes: 10)),
              ),
            );
          } catch (_) {
            // CacheStore updates its memory view before persistence. Restore both
            // original records on partial failure; retain explicit file ownership
            // even if a storage failure also prevents that best-effort rollback.
            try {
              await (store as LibraryCacheStore).publish(transferred);
            } catch (_) {}
            try {
              await (store as LibraryCacheStore).publish(metadata);
            } catch (_) {}
            rethrow;
          } finally {
            await _repository.whenIdle();
            final retained = (await _repository.getAllObjects()).any(
              (entry) => delivered.file.path.endsWith('/${entry.relativePath}'),
            );
            if (!retained && await delivered.file.exists()) {
              await delivered.file.delete();
            }
            _promotionFiles.remove(delivered.file);
          }
          _retired[transferred.key]?.$2.cancel();
          _retired[transferred.key] = (
            shared.file,
            Timer(
              const Duration(minutes: 10),
              () => unawaited(_retireFromTimer(transferred.key)),
            ),
          );
        } finally {
          _retirementReservations--;
        }
        return;
      }
      await putFile(
        '',
        bytes,
        key: immutableKey,
        fileExtension: 'jpg',
        maxAge: const Duration(minutes: 10),
      );
      await _repository.whenIdle();
    } catch (_) {
      // Promotion cannot turn successfully decoded artwork into a display error.
    }
  }

  Future<void> _releaseRetired(String key) async {
    final current = _retirementCleanup[key];
    if (current != null) return current;
    final retired = _retired[key];
    if (retired == null) return;
    retired.$2.cancel();
    final cleanup = _trackStorage(() async {
      if (!_closed) await removeFile(key);
      if (await retired.$1.exists()) await retired.$1.delete();
      if (identical(_retired[key], retired)) _retired.remove(key);
    });
    _retirementCleanup[key] = cleanup;
    try {
      await cleanup;
    } finally {
      _retirementCleanup.remove(key);
    }
  }

  Future<void> _retireFromTimer(String key) async {
    try {
      await _releaseRetired(key);
    } catch (_) {
      // The retained record is retried by bounded admission/close cleanup.
    }
  }

  Future<void> _cleanupCapacity() async {
    if (_promotionFiles.length >= 80) {
      throw StateError('Library image publication cleanup capacity reached');
    }
    if (_retired.length + _retirementReservations >= 80) {
      if (_retired.isNotEmpty) await _releaseRetired(_retired.keys.first);
      if (_retired.length + _retirementReservations >= 80) {
        throw StateError('Library image retirement capacity reached');
      }
    }
  }

  @disposeMethod
  Future<void> close() {
    if (_closing != null) return _closing!;
    final closing = _close();
    _closing = closing;
    // Observe completion without creating an unhandled error future. A later
    // call can retry retained local cleanup after the repository is closed.
    unawaited(
      closing.then(
        (_) => _closing = null,
        onError: (Object _, StackTrace _) {
          _closing = null;
        },
      ),
    );
    return closing;
  }

  Future<void> _close() async {
    final firstClose = !_closed;
    _closed = true;
    Object? failure;
    StackTrace? trace;
    Future<void> attempt(Future<void> Function() action) async {
      try {
        await action();
      } catch (error, stack) {
        failure ??= error;
        trace ??= stack;
      }
    }

    for (final retired in _retired.values) {
      retired.$2.cancel();
    }
    if (firstClose) {
      _delivered.clear();
      _warmHits.clear();
      _admission.close();
      for (final operation in _operations) {
        operation.cancel();
      }
      _transport.close();
      (store as LibraryCacheStore).stopCleanup();
      try {
        await attempt(
          () => Future.wait(
            _operations.map((operation) => operation.finished).toList(),
          ),
        );
        await attempt(() => Future.wait(_promotions.values.toList()));
        await attempt(() => Future.wait(_storage.toList()));
        await attempt(_repository.whenIdle);
        bool capturedFiles = false;
        await attempt(() async {
          bool complete = true;
          for (final object in await _repository.getAllObjects()) {
            try {
              final file = await _files.createFile(object.relativePath);
              // Explicit ownership survives metadata deletion or cleanup failure.
              _promotionFiles.add(file);
            } catch (error, stack) {
              complete = false;
              failure ??= error;
              trace ??= stack;
            }
          }
          capturedFiles = complete;
        });
        // If enumeration/path resolution failed, persisted metadata retains its
        // files. Removing those rows would discard our only cleanup ownership.
        if (capturedFiles) await attempt(emptyCache);
        await _cleanupClosedFiles(attempt);
      } finally {
        await attempt(() => super.dispose());
      }
    } else {
      // No delegate/metadata access is permitted on post-disposal retry.
      await _cleanupClosedFiles(attempt);
    }
    if (failure != null) Error.throwWithStackTrace(failure!, trace!);
  }

  Future<void> _cleanupClosedFiles(
    Future<void> Function(Future<void> Function()) attempt,
  ) async {
    for (final key in _retired.keys.toList()) {
      await attempt(() => _releaseRetired(key));
    }
    for (final file in _promotionFiles.toList()) {
      await attempt(() async {
        if (await file.exists()) await file.delete();
        _promotionFiles.remove(file);
      });
    }
    await attempt(_files.clearAndClose);
  }

  @override
  Future<void> dispose() => close();
}

class _Operation {
  _Operation(this.key, this.owner);
  final String key;
  final String owner;
  final cancelled = Completer<void>();
  final output = StreamController<FileResponse>();
  late Future<void> finished;
  void cancel() {
    if (!cancelled.isCompleted) cancelled.complete();
  }
}
