import 'dart:async';
import 'dart:convert';
import 'package:crypto/crypto.dart';
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

  /// Separate cache profiles share the verified package lifecycle without altering home defaults.
  LibraryImageCache.profile(
    Config configuration, {
    Duration artworkWait = const Duration(seconds: 10),
    http.Client Function()? clientFactory,
    FileService? delegate,
    int maximumOwnedAttempts = 4,
    int maximumFiles = 20,
    int maximumBytes = 104857600,
    int maximumTransferBytes = 20971520,
  }) : this._configured(
         _withLifecycle(
           configuration,
           artworkWait,
           delegate: delegate,
           clientFactory: clientFactory,
           maximumFiles: maximumFiles,
           maximumBytes: maximumBytes,
           maximumTransferBytes: maximumTransferBytes,
         ),
         maximumOwnedAttempts: maximumOwnedAttempts,
         fileLimit: maximumFiles,
       );

  // Explicit configuration couples the store and file lifecycle wrappers.
  // ignore: use_super_parameters
  LibraryImageCache._configured(
    Config configuration, {
    int maximumOwnedAttempts = 90,
    int fileLimit = 80,
  }) : _maximumOwnedAttempts = maximumOwnedAttempts,
       _fileLimit = fileLimit,
       _repository = configuration.repo as DrainingCacheInfoRepository,
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
       super.custom(
         configuration,
         cacheStore: LibraryCacheStore(configuration),
       ) {
    _transport.failureObserver = (owner, event) {
      if (failureObserver != null) _transportFailures[owner] = event;
    };
    _files.protectedPaths = () => {
      ..._protectedReaderPaths,
      ..._failedFiles.values.map((failure) => failure.$1.path),
    };
  }

  final DrainingCacheInfoRepository _repository;
  final LibraryCacheFiles _files;
  final LibraryTransferFileService _transport;
  final LibraryCacheAdmission _admission;
  // Includes metadata, transfer, publication and cleanup, even after cancel.
  final int _maximumOwnedAttempts;
  final int _fileLimit;
  final _operations = <_Operation>{};
  final _transportFailures = <String, Map<String, Object?>>{};
  final _flights = <(String, String, String, bool), _SharedRead>{};

  @visibleForTesting
  int get ownedAttemptCount => _operations.length;

  /// Native tests may observe failure stage without changing admission or errors.
  /// Raw identities, sources, messages and response bodies never enter the event.
  @visibleForTesting
  void Function(Map<String, Object?>)? failureObserver;

  @visibleForTesting
  static String failureIdentitySha256(String identity) =>
      sha256.convert(utf8.encode(identity)).toString();

  void _observeFailure(
    String key,
    String phase,
    Object error,
    Stopwatch elapsed, {
    Map<String, Object?> transport = const {},
  }) {
    final observer = failureObserver;
    if (observer == null) return;
    final split = key.lastIndexOf(_separator);
    try {
      observer(
        Map.unmodifiable({
          'transferSha256': failureIdentitySha256(key),
          'identitySha256': failureIdentitySha256(
            split < 0 ? key : key.substring(0, split),
          ),
          'phase': phase,
          'failureClass': error.runtimeType.toString(),
          'operationMilliseconds': elapsed.elapsedMilliseconds,
          ...transport,
          if (error is TimeoutException && error.duration != null)
            'timeoutMilliseconds': error.duration!.inMilliseconds,
          if (error is HttpExceptionWithStatus) 'httpStatus': error.statusCode,
        }),
      );
    } catch (_) {
      // Test observation must never replace the actual consumer failure.
    }
  }

  final _delivered = <String, FileInfo>{};
  final _pendingSharedFiles = <String, FileInfo>{};
  final _warmHits = <String, (String, FileInfo)>{};
  final _promotions = <String, Future<void>>{};
  final _storage = <Future<void>>{};
  final _promotionFiles = <File>{};
  final _decodingFiles = <File>{};
  final _failedFiles = <String, (File, Object, StackTrace)>{};
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
    int maximumFiles = 80,
    int? maximumBytes,
    int? maximumTransferBytes,
  }) => Config(
    configuration.cacheKey,
    stalePeriod: configuration.stalePeriod,
    maxNrOfCacheObjects: configuration.maxNrOfCacheObjects,
    repo: DrainingCacheInfoRepository(configuration.repo),
    fileSystem: LibraryCacheFiles(
      configuration.fileSystem,
      capacity: maximumFiles,
      maximumBytes: maximumBytes,
      reservedFileBytes: maximumTransferBytes ?? 0,
    ),
    fileService: LibraryTransferFileService(
      artworkWait: artworkWait,
      delegate: delegate,
      clientFactory: clientFactory,
      maximumTransferBytes: maximumTransferBytes,
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
    operation.output.onCancel = () => _cancelOperation(operation);
    _operations.add(operation);
    operation.finished = runZoned(
      () => _read(operation, url, headers, withProgress),
      zoneValues: {LibraryCacheFiles.ownerKey: operation.owner},
    );
    return operation.output.stream;
  }

  Future<void> _shareCold(
    _Operation operation,
    String url,
    Map<String, String>? headers,
    bool withProgress,
  ) async {
    final split = operation.key.lastIndexOf(_separator);
    final sortedHeaders = Map.fromEntries(
      (headers ?? const <String, String>{}).entries.toList()
        ..sort((a, b) => a.key.compareTo(b.key)),
    );
    // Warm reads validate independently. Only an exact unfinished cold source
    // shares package IO; renewed URLs and another namespace cannot join it.
    final identity = (
      operation.key.substring(0, split),
      url,
      jsonEncode(sortedHeaders),
      withProgress,
    );
    var shared = _flights[identity];
    if (shared == null || shared.source.cancelled.isCompleted) {
      shared = _SharedRead(
        identity,
        _Operation(operation.key, operation.owner),
      );
      _flights[identity] = shared;
      shared.consumers.add(operation);
      operation.shared = shared;
      final selected = shared;
      shared.source.failureKeys = () => selected.consumers
          .where((consumer) => !consumer.cancelled.isCompleted)
          .map((consumer) => consumer.key);
      shared.source.output.stream.listen(
        (response) => _publishShared(selected, response),
        onError: (Object error, StackTrace stack) {
          for (final consumer in selected.consumers) {
            if (!consumer.cancelled.isCompleted) {
              consumer.output.addError(error, stack);
            }
          }
        },
        onDone: () => unawaited(_settleShared(selected)),
      );
      shared.source.finished = runZoned(
        () => _read(
          selected.source,
          url,
          headers,
          withProgress,
          sharedProducer: true,
        ),
        zoneValues: {LibraryCacheFiles.ownerKey: selected.source.owner},
      );
    } else {
      shared.consumers.add(operation);
      operation.shared = shared;
      if (shared.latest != null) {
        _publishConsumer(shared, operation, shared.latest!);
      }
    }
    await shared.settled.future;
  }

  void _cancelOperation(_Operation operation) {
    operation.cancel();
    final shared = operation.shared;
    if (shared == null) {
      _transport.cancel(operation.owner);
    } else if (shared.consumers.every(
      (consumer) => consumer.cancelled.isCompleted,
    )) {
      shared.source.cancel();
      _transport.cancel(shared.source.owner);
    }
  }

  void _publishShared(_SharedRead shared, FileResponse response) {
    shared.latest = response;
    for (final consumer in shared.consumers) {
      _publishConsumer(shared, consumer, response);
    }
  }

  void _publishConsumer(
    _SharedRead shared,
    _Operation consumer,
    FileResponse response,
  ) {
    if (consumer.cancelled.isCompleted) return;
    if (response is FileInfo) {
      // Each decoder owns its own callback key, even when cold IO is shared.
      _delivered[consumer.key] = response;
      while (_delivered.length > _fileLimit) {
        _delivered.remove(_delivered.keys.first);
      }
    }
    consumer.output.add(response);
  }

  Future<void> _settleShared(_SharedRead shared) async {
    await shared.source.finished;
    if (identical(_flights[shared.identity], shared)) {
      _flights.remove(shared.identity);
    }
    if (!shared.settled.isCompleted) shared.settled.complete();
  }

  void _observeOperationFailure(
    _Operation operation,
    String phase,
    Object error,
    Stopwatch elapsed,
  ) {
    for (final key in operation.failureKeys?.call() ?? [operation.key]) {
      _observeFailure(
        key,
        phase,
        error,
        elapsed,
        transport: {
          ...?_transportFailures[operation.owner],
          if (operation.failureKeys != null)
            'producerSha256': failureIdentitySha256(operation.key),
        },
      );
    }
  }

  Future<void> _read(
    _Operation operation,
    String url,
    Map<String, String>? headers,
    bool withProgress, {
    bool sharedProducer = false,
  }) async {
    bool admitted = false;
    var phase = 'warm-lookup';
    final elapsed = Stopwatch()..start();
    try {
      final key = operation.key;
      final split = key.lastIndexOf(_separator);
      _delivered.remove(key);
      _warmHits.remove(key);
      if (split >= 0 && !sharedProducer) {
        final immutable = key.substring(0, split);
        final cached = await getFileFromCache(immutable);
        if (_closed || operation.cancelled.isCompleted) return;
        if (cached != null && cached.validTill.isAfter(DateTime.now())) {
          phase = 'warm-validation';
          await _validateFile(cached, key, immutable);
          if (_closed || operation.cancelled.isCompleted) return;
          _warmHits[key] = (immutable, cached);
          if (_warmHits.length > _fileLimit) {
            _warmHits.remove(_warmHits.keys.first);
          }
          operation.output.add(cached);
          return;
        }
      }
      // Surface an admitted lookup failure before CacheManager's fallback path
      // can silently treat its failed first lookup as a miss.
      if (!sharedProducer) {
        phase = 'attempt-lookup';
        await getFileFromCache(key);
        if (_closed || operation.cancelled.isCompleted) return;
        if (split >= 0) {
          await _shareCold(operation, url, headers, withProgress);
          return;
        }
      }
      phase = 'capacity-cleanup';
      await _cleanupCapacity(reserveFile: true);
      phase = 'admission';
      await _admission.enter(operation.cancelled.future);
      admitted = true;
      if (_closed || operation.cancelled.isCompleted) return;
      // The ownership marker is stripped by FileService before the HTTP request.
      phase = 'package-source';
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
          phase = 'file-publication';
          await (store as LibraryCacheStore).drain();
          (store as LibraryCacheStore).rethrowFailure(operation.owner);
          if (_closed || operation.cancelled.isCompleted) continue;
          // A stale attempt is an unavailable source, not a current hero. Let
          // the package finish its bounded renewal before checksum validation.
          if (split >= 0 &&
              response.source == FileSource.Cache &&
              !response.validTill.isAfter(DateTime.now())) {
            phase = 'package-source';
            continue;
          }
          phase = 'file-validation';
          await _validateFile(response, key, key);
          if (_closed || operation.cancelled.isCompleted) continue;
        }
        if (response is FileInfo && split >= 0) {
          if (!response.validTill.isAfter(DateTime.now())) continue;
          if (sharedProducer) {
            // Protect publication before async fan-out, without inventing a
            // decoder owner for a consumer that has already cancelled.
            _pendingSharedFiles[operation.owner] = response;
          } else {
            _delivered[key] = response;
            if (_delivered.length > _fileLimit) {
              _delivered.remove(_delivered.keys.first);
            }
          }
        }
        operation.output.add(response);
        phase = 'package-source';
      }
    } catch (error, stack) {
      (store as LibraryCacheStore).discardReportedFailure(operation.owner);
      if (!_closed && !operation.cancelled.isCompleted) {
        _observeOperationFailure(operation, phase, error, elapsed);
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
        if (!_closed) await _cleanupFailedFiles();
      } catch (error, stack) {
        if (!_closed && !operation.cancelled.isCompleted) {
          _observeOperationFailure(operation, 'settlement', error, elapsed);
          operation.output.addError(error, stack);
        }
      } finally {
        if (admitted) _admission.leave();
        _transportFailures.remove(operation.owner);
        _pendingSharedFiles.remove(operation.owner);
        operation.shared?.consumers.remove(operation);
        operation.shared = null;
        _operations.remove(operation);
        unawaited(operation.output.close());
      }
    }
  }

  Future<void> _validateFile(
    FileInfo response,
    String key,
    String cacheKey,
  ) async {
    final failure = _failedFiles[cacheKey];
    if (failure?.$1.path == response.file.path) {
      Error.throwWithStackTrace(failure!.$2, failure.$3);
    }
    _decodingFiles.add(response.file);
    try {
      await validateResponse(response, key);
    } catch (error, stack) {
      // Keep physical ownership until all admitted readers/writers have settled.
      _failedFiles[cacheKey] = (response.file, error, stack);
      rethrow;
    } finally {
      _decodingFiles.remove(response.file);
    }
  }

  Set<String> get _protectedReaderPaths => {
    ..._delivered.values.map((value) => value.file.path),
    ..._pendingSharedFiles.values.map((value) => value.file.path),
    ..._warmHits.values.map((value) => value.$2.file.path),
    ..._decodingFiles.map((value) => value.path),
    ..._promotionFiles.map((value) => value.path),
    ..._retired.values.map((value) => value.$1.path),
  };

  Future<void> _cleanupFailedFiles() async {
    for (final entry in _failedFiles.entries.toList()) {
      final file = entry.value.$1;
      if (_protectedReaderPaths.contains(file.path) ||
          _files.writingNames.any((name) => file.path.endsWith('/$name'))) {
        continue;
      }
      final objects = await _repository.getAllObjects();
      final current = objects
          .where((object) => object.key == entry.key)
          .firstOrNull;
      if (current != null && file.path.endsWith('/${current.relativePath}')) {
        // Recheck after asynchronous metadata enumeration; another admitted
        // reader may still own the same physical source.
        if (_protectedReaderPaths.contains(file.path) ||
            _files.writingNames.contains(current.relativePath)) {
          continue;
        }
        final shared = objects.any(
          (object) =>
              object.key != entry.key &&
              object.relativePath == current.relativePath,
        );
        if (shared) {
          await (store as LibraryCacheStore).removeCachedReference(current);
        } else {
          await (store as LibraryCacheStore).removeCachedFile(current);
        }
      } else if (!_protectedReaderPaths.contains(file.path) &&
          !_files.writingNames.any((name) => file.path.endsWith('/$name')) &&
          !objects.any(
            (object) => file.path.endsWith('/${object.relativePath}'),
          )) {
        if (await file.exists()) await file.delete();
      }
      if (identical(_failedFiles[entry.key], entry.value)) {
        _failedFiles.remove(entry.key);
      }
    }
  }

  /// Profile validation shares the operation's slot until its actual file work settles.
  @protected
  Future<void> validateResponse(FileInfo response, String key) async {}

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
          await _restoreProfileFiles();
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

  /// Retry settles its old writer and evicts only the failed source it read.
  Future<void> discardFailedTransfer(String key, {bool failed = true}) {
    if (_closed) return Future.value();
    return _trackStorage(() => _discardFailedTransfer(key, failed: failed));
  }

  Future<void> _discardFailedTransfer(
    String key, {
    required bool failed,
  }) async {
    final operations = _operations.where((entry) => entry.key == key).toList();
    for (final operation in operations) {
      _cancelOperation(operation);
    }
    // A cancelled package writer must settle before same-key retry can publish.
    await Future.wait(operations.map((operation) => operation.finished));
    if (_closed) return;
    final delivered = _delivered.remove(key);
    if (failed && delivered != null) {
      _failedFiles[key] = (
        delivered.file,
        StateError('Artwork decoding failed'),
        StackTrace.current,
      );
    }
    final hit = _warmHits.remove(key);
    if (failed && hit != null) {
      _failedFiles[hit.$1] = (
        hit.$2.file,
        StateError('Artwork decoding failed'),
        StackTrace.current,
      );
    }
    await _cleanupFailedFiles();
  }

  Future<void> _promote(
    String immutableKey,
    FileInfo delivered,
    bool Function() isCurrent,
  ) async {
    _decodingFiles.add(delivered.file);
    File? previousFile;
    try {
      final shared = await getFileFromCache(immutableKey);
      previousFile = shared?.file;
      if (previousFile != null) _decodingFiles.add(previousFile);
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
        if (_retired.length + _retirementReservations >= _fileLimit) return;
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
    } finally {
      _decodingFiles.remove(delivered.file);
      if (previousFile != null) _decodingFiles.remove(previousFile);
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

  Future<void> _cleanupCapacity({bool reserveFile = false}) async {
    await _cleanupFailedFiles();
    await _restoreProfileFiles();
    if (_promotionFiles.length >= _fileLimit) {
      throw StateError('Library image publication cleanup capacity reached');
    }
    if (_retired.length + _retirementReservations >= _fileLimit ||
        (reserveFile && _files.physicalOwnerCount >= _fileLimit)) {
      if (_retired.isNotEmpty) await _releaseRetired(_retired.keys.first);
      if (_retired.length + _retirementReservations >= _fileLimit) {
        throw StateError('Library image retirement capacity reached');
      }
    }
  }

  Future<void> _restoreProfileFiles() async {
    // Account for persisted profile bytes across app restarts before any new write.
    for (final object in await _repository.getAllObjects()) {
      await _files.createFile(object.relativePath);
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
        _cancelOperation(operation);
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
  _SharedRead? shared;
  Iterable<String> Function()? failureKeys;
  void cancel() {
    if (!cancelled.isCompleted) cancelled.complete();
  }
}

class _SharedRead {
  _SharedRead(this.identity, this.source);
  final (String, String, String, bool) identity;
  final _Operation source;
  final consumers = <_Operation>[];
  final settled = Completer<void>();
  FileResponse? latest;
}
