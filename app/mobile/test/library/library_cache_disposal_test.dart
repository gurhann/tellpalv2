import 'dart:async';
import 'dart:io' show FileSystemException;
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/network/draining_cache_info_repository.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/core/network/library_cache_lifecycle.dart';

class _Files implements FileSystem {
  _Files() {
    _files = MemoryFileSystem(
      opHandle: (path, operation) {
        if (operation == FileSystemOp.delete && failures != 0) {
          deleteAttempts++;
          if (failures > 0) failures--;
          throw const FileSystemException('Injected file cleanup failure');
        }
      },
    );
  }
  late final MemoryFileSystem _files;
  int failures = 0;
  int deleteAttempts = 0;
  int creates = 0;
  @override
  Future<files.File> createFile(String name) async {
    creates++;
    return _files.file('/$name');
  }
}

class _Delegate extends NonStoringObjectProvider {
  bool closed = false;
  final calls = <String>[];
  int closeCount = 0;
  Completer<CacheObject?>? read;
  Completer<dynamic>? write;
  Completer<List<CacheObject>>? capacity;
  Object? enumerationError;
  void touch(String method) {
    if (closed) throw StateError('Closed storage delegate accessed: $method');
    calls.add(method);
  }

  @override
  Future<bool> open() async {
    touch('open');
    return true;
  }

  @override
  Future<CacheObject?> get(String key) async {
    touch('get');
    return read == null ? null : await read!.future;
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) async {
    touch('write');
    return write == null ? null : await write!.future;
  }

  @override
  Future<List<CacheObject>> getObjectsOverCapacity(int capacity) async {
    touch('capacity');
    return this.capacity == null ? [] : await this.capacity!.future;
  }

  @override
  Future<List<CacheObject>> getOldObjects(Duration age) async {
    touch('old');
    return [];
  }

  @override
  Future<List<CacheObject>> getAllObjects() async {
    touch('all');
    if (enumerationError != null) throw enumerationError!;
    return [];
  }

  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    touch('deleteAll');
    return 0;
  }

  @override
  Future<bool> close() async {
    touch('close');
    closed = true;
    closeCount++;
    return true;
  }
}

CacheObject object() => CacheObject(
  'test-artwork',
  key: 'key',
  relativePath: 'artwork.jpg',
  validTill: DateTime.now(),
);
LibraryImageCache _cache(_Delegate delegate) => LibraryImageCache.forTesting(
  Config(
    'test-disposal',
    stalePeriod: const Duration(minutes: 10),
    maxNrOfCacheObjects: 80,
    repo: delegate,
    fileSystem: _Files(),
  ),
);

void main() {
  test(
    'partial cleanup retains transient failure and close retries deletion',
    () async {
      final files = _Files()..failures = 1;
      final lifecycle = LibraryCacheFiles(files);
      final partial = await runZoned(
        () => lifecycle.createFile('partial'),
        zoneValues: {LibraryCacheFiles.ownerKey: 'owner'},
      );
      await partial.writeAsString('unfinished');
      await expectLater(
        lifecycle.release('owner', {}),
        throwsA(isA<FileSystemException>()),
      );
      expect(await partial.exists(), isTrue);
      await lifecycle.clearAndClose();
      expect(await partial.exists(), isFalse);
    },
  );
  test(
    'persistent partial failures bound ownership and storage admission; closed cleanup can recover',
    () async {
      final files = _Files()..failures = -1;
      final lifecycle = LibraryCacheFiles(files);
      for (var index = 0; index < 80; index++) {
        final partial = await runZoned(
          () => lifecycle.createFile('partial-$index'),
          zoneValues: {LibraryCacheFiles.ownerKey: '$index'},
        );
        await partial.writeAsString('unfinished');
        await expectLater(
          lifecycle.release('$index', {}),
          throwsA(isA<FileSystemException>()),
        );
      }
      final before = files.creates;
      for (var index = 0; index < 3; index++) {
        await expectLater(
          runZoned(
            () => lifecycle.createFile('rejected-$index'),
            zoneValues: {LibraryCacheFiles.ownerKey: 'new'},
          ),
          throwsStateError,
        );
      }
      expect(files.creates, before);
      expect(
        files.deleteAttempts,
        320,
      ); // 80 initial + 3 bounded 80-file passes.
      await expectLater(
        lifecycle.clearAndClose(),
        throwsA(isA<FileSystemException>()),
      );
      expect(files._files.directory('/').listSync(), hasLength(80));
      files.failures = 0;
      await lifecycle.clearAndClose();
      expect(files._files.directory('/').listSync(), isEmpty);
      await expectLater(lifecycle.createFile('late'), throwsStateError);
      expect(files.creates, before);
    },
  );
  testWidgets(
    'enumeration failure closes repository and repeated close has no delegate access',
    (tester) async {
      final error = StateError('Injected enumeration failure');
      final delegate = _Delegate()..enumerationError = error;
      final manager = _cache(delegate);
      final closing = manager.close();
      expect(manager.close(), same(closing));
      await expectLater(closing, throwsA(same(error)));
      expect(delegate.closed, isTrue);
      expect(delegate.closeCount, 1);
      final calls = List<String>.of(delegate.calls);
      await manager.close();
      await tester.pump(const Duration(seconds: 11));
      expect(delegate.calls, calls);
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'file deletion failure still closes repository; retry keeps local ownership',
    () async {
      final delegate = _Delegate();
      final files = _Files();
      final manager = LibraryImageCache.forTesting(
        Config('cleanup-delete', repo: delegate, fileSystem: files),
      );
      final partial = await runZoned(
        () => (manager.config.fileSystem as LibraryCacheFiles).createFile(
          'partial',
        ),
        zoneValues: {LibraryCacheFiles.ownerKey: 'owner'},
      );
      await partial.writeAsString('unfinished');
      files.failures = -1;
      await expectLater(manager.close(), throwsA(isA<FileSystemException>()));
      expect(delegate.closed, isTrue);
      expect(await partial.exists(), isTrue);
      final calls = List<String>.of(delegate.calls);
      await expectLater(manager.close(), throwsA(isA<FileSystemException>()));
      expect(delegate.calls, calls);
      files.failures = 0;
      await manager.close();
      expect(await partial.exists(), isFalse);
      expect(delegate.closeCount, 1);
      expect(delegate.calls, calls);
    },
  );
  testWidgets(
    'actual scheduled CacheStore cleanup after disposal never calls closed storage',
    (tester) async {
      final delegate = _Delegate();
      final manager = _cache(delegate);
      expect(
        await manager.getFileFromCache('missing'),
        isNull,
      ); // Schedules the package's 10s cleanup.
      await manager.close();
      expect(delegate.closed, isTrue);
      final before = List<String>.of(delegate.calls);
      await tester.pump(const Duration(seconds: 11));
      expect(delegate.calls, before);
      expect(delegate.closeCount, 1);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'cleanup query admitted before disposal completes safely; late callbacks cannot reach closed storage',
    (tester) async {
      final delegate = _Delegate()..capacity = Completer<List<CacheObject>>();
      final manager = _cache(delegate);
      await manager.getFileFromCache('missing');
      await tester.pump(const Duration(seconds: 10));
      expect(delegate.calls, contains('capacity'));
      final closing = manager.close();
      await tester.pump();
      expect(delegate.closed, isFalse);
      delegate.capacity!.complete([]);
      await closing;
      await tester.pump();
      expect(delegate.closed, isTrue);
      final callsAtClose = List<String>.of(delegate.calls);
      await tester.pump(const Duration(seconds: 11));
      expect(delegate.calls, callsAtClose);
      expect(delegate.calls.last, 'close');
      expect(delegate.closeCount, 1);
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'disposal drains admitted read and write, declines new work and coalesces close',
    () async {
      final delegate = _Delegate()
        ..read = Completer<CacheObject?>()
        ..write = Completer<dynamic>();
      final repository = DrainingCacheInfoRepository(delegate);
      await repository.open();
      final reading = repository.get('pending');
      final writing = repository.updateOrInsert(object());
      final closing = repository.close();
      expect(repository.close(), same(closing));
      expect(await repository.get('new'), isNull);
      expect(await repository.getAllObjects(), isEmpty);
      expect(await repository.deleteAll([1]), 0);
      expect(delegate.calls, ['open', 'get', 'write']);
      expect(delegate.closed, isFalse);
      final ready = object();
      delegate.read!.complete(ready);
      expect(await reading, same(ready));
      expect(delegate.closed, isFalse);
      delegate.write!.complete(ready);
      expect(await writing, same(ready));
      expect(await closing, isTrue);
      expect(delegate.closeCount, 1);
      expect(await repository.open(), isFalse);
      expect(
        await repository.getOldObjects(const Duration(minutes: 10)),
        isEmpty,
      );
      expect(await repository.getObjectsOverCapacity(80), isEmpty);
      expect(await repository.updateOrInsert(object()), isNull);
      expect(delegate.calls, ['open', 'get', 'write', 'close']);
    },
  );
  test(
    'admitted storage errors propagate and still release the close drain',
    () async {
      final delegate = _Delegate()..write = Completer<dynamic>();
      final repository = DrainingCacheInfoRepository(delegate);
      final writing = repository.updateOrInsert(object());
      final error = StateError('Admitted storage failure');
      final observed = expectLater(writing, throwsA(same(error)));
      final closing = repository.close();
      expect(delegate.closed, isFalse);
      delegate.write!.completeError(error);
      await observed;
      expect(await closing, isTrue);
      expect(delegate.closeCount, 1);
    },
  );
}
