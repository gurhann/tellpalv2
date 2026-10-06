import 'dart:async';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
// CacheManager.custom exposes CacheStore but its package barrel omits the type.
// ignore: implementation_imports
import 'package:flutter_cache_manager/src/cache_store.dart';
import 'draining_cache_info_repository.dart';
import 'library_cache_lifecycle.dart';

/// App-owned bridge for the public CacheManager.custom store seam.
/// All accepted lookups settle; package fire-and-forget writes are drained and
/// their admitted errors are delivered to the owning adapter operation.
class LibraryCacheStore extends CacheStore {
  // Keep the explicit configuration used by all four lifecycle dependencies.
  // ignore: use_super_parameters
  LibraryCacheStore(Config configuration)
    : _repository = configuration.repo as DrainingCacheInfoRepository,
      _files = configuration.fileSystem as LibraryCacheFiles,
      _capacity = configuration.maxNrOfCacheObjects,
      _maxAge = configuration.stalePeriod,
      super(_packageConfiguration(configuration));

  static Config _packageConfiguration(Config configuration) => Config(
    configuration.cacheKey,
    stalePeriod: configuration.stalePeriod,
    maxNrOfCacheObjects: configuration.maxNrOfCacheObjects,
    repo: _PackageInitializationRepository(configuration.repo),
    fileSystem: configuration.fileSystem,
    fileService: configuration.fileService,
  );

  final DrainingCacheInfoRepository _repository;
  final LibraryCacheFiles _files;
  final int _capacity;
  final Duration _maxAge;
  final _writes = <Future<void>>{};
  final _failures = <String, (Object, StackTrace)>{};
  Timer? _cleanup;
  bool _stopping = false;

  Future<T> _ownLookup<T>(Future<T> Function() operation) async {
    final owner = Zone.current[LibraryCacheFiles.ownerKey] as String?;
    // CacheManager catches a failed getFile and falls back to WebHelper. The
    // next package lookup must reject that same attempt before any HTTP work.
    if (owner != null) rethrowFailure(owner);
    try {
      return await operation();
    } catch (error, stack) {
      if (owner != null) _failures.putIfAbsent(owner, () => (error, stack));
      rethrow;
    }
  }

  @override
  Future<FileInfo?> getFile(String key, {bool ignoreMemCache = false}) =>
      _ownLookup(() => super.getFile(key, ignoreMemCache: ignoreMemCache));

  @override
  Future<FileInfo?> getFileFromMemory(String key) =>
      _ownLookup(() => super.getFileFromMemory(key));

  @override
  Future<CacheObject?> retrieveCacheData(
    String key, {
    bool ignoreMemCache = false,
  }) => _ownLookup(() => _retrieve(key));

  Future<CacheObject?> _retrieve(String key) async {
    // Use the configured repository directly instead of the package's
    // onError-less lookup completer. No storage error becomes a cache miss.
    await _repository.open();
    final object = await _repository.get(key);
    if (_stopping) return null;
    _cleanup ??= Timer(cleanupRunMinInterval, () {
      _cleanup = null;
      unawaited(_runCleanup());
    });
    if (object == null) return null;
    final file = await _files.createFile(object.relativePath);
    if (!await file.exists()) {
      if (object.id != null) await _repository.deleteAll([object.id!]);
      return null;
    }
    return object;
  }

  /// Directly awaited publications preserve the caller's original error.
  Future<void> publish(CacheObject object) async {
    // The package initialization future settles neutrally; actual operations
    // still await the original repository future and preserve its exact error.
    await _repository.open();
    await super.putFile(object);
  }

  @override
  Future<void> putFile(CacheObject cacheObject) {
    final owner =
        Zone.current[LibraryCacheFiles.ownerKey] as String? ?? 'package';
    final completion = Completer<void>();
    _writes.add(completion.future);
    return (() async {
      try {
        await publish(cacheObject);
      } catch (error, stack) {
        // WebHelper/CacheManager do not await publication. Keep the error until
        // their owning operation's drain, avoiding an unhandled vendor future.
        _failures.putIfAbsent(owner, () => (error, stack));
        emptyMemoryCache();
      } finally {
        _writes.remove(completion.future);
        completion.complete();
      }
    })();
  }

  /// The original error has reached the adapter consumer; release its record.
  void discardReportedFailure(String owner) => _failures.remove(owner);

  void rethrowFailure(String owner) {
    final failure = _failures.remove(owner);
    if (failure != null) Error.throwWithStackTrace(failure.$1, failure.$2);
  }

  Future<void> drain() async {
    while (_writes.isNotEmpty) {
      await Future.wait(_writes.toList());
    }
  }

  @override
  Future<void> removeCachedFile(CacheObject cacheObject) async {
    // Physical ownership remains in LibraryCacheFiles if deletion fails.
    await _files.retainMetadataFile(cacheObject.relativePath);
    if (cacheObject.id != null) await _repository.deleteAll([cacheObject.id!]);
    emptyMemoryCache();
    await _files.removeMetadataFile(cacheObject.relativePath);
  }

  @override
  Future<void> emptyCache() async {
    for (final object in await _repository.getAllObjects()) {
      await removeCachedFile(object);
    }
  }

  Future<void> _runCleanup() async {
    if (_stopping) return;
    final completion = Completer<void>();
    _writes.add(completion.future);
    try {
      final objects = {
        for (final object in await _repository.getObjectsOverCapacity(
          _capacity,
        ))
          object.key: object,
        for (final object in await _repository.getOldObjects(_maxAge))
          object.key: object,
      };
      for (final object in objects.values) {
        try {
          await removeCachedFile(object);
        } catch (_) {
          // Metadata and explicit physical owner remain; retry on the next
          // bounded cleanup or close. Timer errors cannot escape asynchronously.
        }
      }
    } catch (_) {
      // Enumeration leaves persisted ownership intact for subsequent cleanup.
    } finally {
      _writes.remove(completion.future);
      completion.complete();
    }
  }

  void stopCleanup() {
    _stopping = true;
    _cleanup?.cancel();
    _cleanup = null;
  }
}

/// CacheStore keeps a private success-only continuation of Config.repo.open.
/// Observe its initialization failure here, while the owning store operations
/// await the original opening future so no storage error becomes a cache miss.
class _PackageInitializationRepository extends DrainingCacheInfoRepository {
  // Keep the public parameter name independent of the private delegate field.
  // ignore: use_super_parameters
  _PackageInitializationRepository(CacheInfoRepository repository)
    : super(repository);

  @override
  Future<bool> open() async {
    try {
      return await super.open();
    } catch (_) {
      return false;
    }
  }
}
