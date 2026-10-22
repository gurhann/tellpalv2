import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';

/// Dosya ve metadata ömrü yalnız cache sahibinin bellek kapsamındadır.
class MemoryImageCacheFiles implements FileSystem {
  final files.FileSystem memory = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => memory.file('/$name');
}

class MemoryImageCacheRepository implements CacheInfoRepository {
  final _objects = <String, CacheObject>{};
  var _nextId = 0;
  bool _closed = false;
  void _check() {
    if (_closed) throw StateError('memory_cache_closed');
  }

  @override
  Future<bool> open() async {
    _check();
    return true;
  }

  @override
  Future<bool> exists() async {
    _check();
    return false;
  }

  @override
  Future<CacheObject?> get(String key) async {
    _check();
    return _objects[key];
  }

  @override
  Future<List<CacheObject>> getAllObjects() async {
    _check();
    return _objects.values.toList();
  }

  @override
  Future<CacheObject> insert(
    CacheObject object, {
    bool setTouchedToNow = true,
  }) async {
    _check();
    final value = CacheObject.fromMap({
      ...object.toMap(setTouchedToNow: setTouchedToNow),
      CacheObject.columnId: ++_nextId,
    });
    _objects[value.key] = value;
    return value;
  }

  @override
  Future<int> update(CacheObject object, {bool setTouchedToNow = true}) async {
    _check();
    if (!_objects.containsKey(object.key)) return 0;
    _objects[object.key] = CacheObject.fromMap(
      object.toMap(setTouchedToNow: setTouchedToNow),
    );
    return 1;
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) async {
    _check();
    final existing = _objects[object.key];
    if (existing == null) return insert(object);
    return update(object.copyWith(id: existing.id));
  }

  @override
  Future<int> delete(int id) => deleteAll([id]);
  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    _check();
    final keys = _objects.values
        .where((o) => ids.contains(o.id))
        .map((o) => o.key)
        .toList();
    for (final key in keys) {
      _objects.remove(key);
    }
    return keys.length;
  }

  @override
  Future<List<CacheObject>> getObjectsOverCapacity(int capacity) async {
    final values = await getAllObjects();
    values.sort(
      (a, b) => (b.touched ?? DateTime.fromMillisecondsSinceEpoch(0)).compareTo(
        a.touched ?? DateTime.fromMillisecondsSinceEpoch(0),
      ),
    );
    return values.skip(capacity).toList();
  }

  @override
  Future<List<CacheObject>> getOldObjects(Duration maxAge) async {
    final cutoff = DateTime.now().subtract(maxAge);
    return (await getAllObjects())
        .where((o) => o.touched?.isBefore(cutoff) ?? false)
        .toList();
  }

  @override
  Future<void> deleteDataFile() async {
    _objects.clear();
  }

  @override
  Future<bool> close() async {
    _objects.clear();
    _closed = true;
    return true;
  }
}
