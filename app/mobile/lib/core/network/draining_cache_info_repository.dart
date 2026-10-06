import 'dart:async';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';

/// Stops repository admission and drains existing operations before closing storage.
/// Package cleanup callbacks arriving later observe an unavailable cache, not closed SQL.
class DrainingCacheInfoRepository implements CacheInfoRepository {
  DrainingCacheInfoRepository(this._delegate);
  final CacheInfoRepository _delegate;
  bool _accepting = true;
  int _active = 0;
  Completer<void>? _drained;
  Future<bool>? _closing;
  Future<bool>? _opening;

  Future<T> _run<T>(Future<T> Function() operation, T unavailable) async {
    if (!_accepting) return unavailable;
    _active++;
    try {
      return await operation();
    } finally {
      _active--;
      if (_active == 0) {
        _drained?.complete();
        _drained = null;
      }
    }
  }

  /// Waits for admitted metadata writes before cache enumeration or shutdown.
  Future<void> whenIdle() => _active == 0
      ? Future<void>.value()
      : (_drained ??= Completer<void>()).future;

  @override
  Future<bool> close() => _closing ??= _drainAndClose();

  Future<bool> _drainAndClose() async {
    _accepting = false;
    await whenIdle();
    return _delegate.close();
  }

  @override
  Future<bool> open() => !_accepting
      ? Future.value(false)
      : (_opening ??= _run(_delegate.open, false));
  @override
  Future<bool> exists() => _run(_delegate.exists, false);
  @override
  Future<CacheObject?> get(String key) async {
    return _run(() => _delegate.get(key), null);
  }

  @override
  Future<List<CacheObject>> getAllObjects() =>
      _run(_delegate.getAllObjects, const []);
  @override
  Future<List<CacheObject>> getObjectsOverCapacity(int capacity) =>
      _run(() => _delegate.getObjectsOverCapacity(capacity), const []);
  @override
  Future<List<CacheObject>> getOldObjects(Duration maxAge) =>
      _run(() => _delegate.getOldObjects(maxAge), const []);
  @override
  Future<dynamic> updateOrInsert(CacheObject object) =>
      _run<dynamic>(() => _delegate.updateOrInsert(object), null);
  @override
  Future<CacheObject> insert(
    CacheObject object, {
    bool setTouchedToNow = true,
  }) => _run(
    () => _delegate.insert(object, setTouchedToNow: setTouchedToNow),
    object,
  );
  @override
  Future<int> update(CacheObject object, {bool setTouchedToNow = true}) =>
      _run(() => _delegate.update(object, setTouchedToNow: setTouchedToNow), 0);
  @override
  Future<int> delete(int id) async {
    return _run(() => _delegate.delete(id), 0);
  }

  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    return _run(() => _delegate.deleteAll(ids), 0);
  }

  @override
  Future<void> deleteDataFile() => _run<void>(_delegate.deleteDataFile, null);
}
