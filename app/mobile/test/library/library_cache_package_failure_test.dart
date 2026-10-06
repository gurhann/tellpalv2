import 'dart:async';
import 'dart:convert';
import 'dart:io' as io;
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';

final _bytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);

class _Repository extends NonStoringObjectProvider {
  final objects = <String, CacheObject>{};
  Object? openError;
  int closeCalls = 0;
  Object? readError;
  int readCalls = 0;
  int? failAtCall;
  Object? publicationError;
  bool closed = false;
  int afterClose = 0;
  int _id = 0;
  Completer<CacheObject?>? pendingRead;
  void check() {
    if (closed) {
      afterClose++;
      throw StateError('Closed repository');
    }
  }

  @override
  Future<bool> open() async {
    check();
    if (openError != null) throw openError!;
    return true;
  }

  @override
  Future<CacheObject?> get(String key) async {
    check();
    readCalls++;
    if (readError != null && (failAtCall == null || readCalls == failAtCall)) {
      throw readError!;
    }
    if (pendingRead != null) return pendingRead!.future;
    return objects[key];
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) async {
    check();
    if (publicationError != null && object.key.startsWith('immutable')) {
      throw publicationError!;
    }
    return objects[object.key] = CacheObject.fromMap(
      object.copyWith(id: objects[object.key]?.id ?? ++_id).toMap(),
    );
  }

  @override
  Future<List<CacheObject>> getAllObjects() async {
    check();
    return objects.values.toList();
  }

  @override
  Future<List<CacheObject>> getObjectsOverCapacity(int capacity) async {
    check();
    return objects.values
        .take((objects.length - capacity).clamp(0, objects.length))
        .toList();
  }

  @override
  Future<List<CacheObject>> getOldObjects(Duration age) async {
    check();
    return objects.values
        .where((o) => o.touched!.isBefore(DateTime.now().subtract(age)))
        .toList();
  }

  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    check();
    objects.removeWhere((_, o) => ids.contains(o.id));
    return ids.length;
  }

  @override
  Future<bool> close() async {
    check();
    closeCalls++;
    closed = true;
    return true;
  }
}

class _Files implements FileSystem {
  _Files() {
    fs = MemoryFileSystem(
      opHandle: (path, operation) {
        if (operation == FileSystemOp.write &&
            path.endsWith('.jpg') &&
            writeFailure) {
          fs.file(path).createSync();
          throw const io.FileSystemException('Injected initial write failure');
        }
        if (operation == FileSystemOp.delete && deletionFailures != 0) {
          if (deletionFailures > 0) deletionFailures--;
          throw const io.FileSystemException(
            'Injected physical deletion failure',
          );
        }
      },
    );
  }
  late final MemoryFileSystem fs;
  bool writeFailure = false;
  int deletionFailures = 0;
  int creates = 0;
  @override
  Future<files.File> createFile(String name) async {
    creates++;
    return fs.file('/$name');
  }
}

class _FaultIoFiles implements FileSystem {
  _FaultIoFiles(this.delegate);
  final FileSystem delegate;
  int failures = 0;
  int creates = 0;
  @override
  Future<files.File> createFile(String name) async {
    creates++;
    return _DeleteFile(await delegate.createFile(name), this);
  }
}

class _DeleteFile extends files.ForwardingFileSystemEntity<files.File, io.File>
    with files.ForwardingFile {
  _DeleteFile(this.delegate, this.owner);
  @override
  final files.File delegate;
  final _FaultIoFiles owner;
  @override
  files.FileSystem get fileSystem => delegate.fileSystem;
  @override
  files.File wrapFile(io.File file) => _DeleteFile(file as files.File, owner);
  @override
  files.Directory wrapDirectory(io.Directory directory) =>
      fileSystem.directory(directory.path);
  @override
  files.Link wrapLink(io.Link link) => fileSystem.link(link.path);
  @override
  Future<files.File> delete({bool recursive = false}) async {
    if (owner.failures != 0) {
      if (owner.failures > 0) owner.failures--;
      throw const io.FileSystemException('Injected real IO deletion failure');
    }
    await delegate.delete(recursive: recursive);
    return this;
  }
}

class _Service extends FileService {
  int requests = 0;
  @override
  Future<FileServiceResponse> get(
    String url, {
    Map<String, String>? headers,
  }) async {
    requests++;
    return _Response();
  }
}

class _Response implements FileServiceResponse {
  @override
  Stream<List<int>> get content => Stream.value(_bytes);
  @override
  int get contentLength => _bytes.length;
  @override
  int get statusCode => 200;
  @override
  DateTime get validTill => DateTime.now().add(const Duration(minutes: 10));
  @override
  String? get eTag => null;
  @override
  String get fileExtension => '.png';
}

LibraryImageCache _cache(
  _Repository repo,
  FileSystem fs, [
  _Service? service,
  int capacity = 80,
]) => LibraryImageCache.forTesting(
  Config(
    'failure-test',
    stalePeriod: const Duration(minutes: 10),
    repo: repo,
    fileSystem: fs,
    fileService: service ?? _Service(),
    maxNrOfCacheObjects: capacity,
  ),
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'actual package opening failure reaches each consumer once without zone error or HTTP and closes once',
    () async {
      final uncaught = <Object>[];
      final finished = Completer<void>();
      runZonedGuarded(() async {
        try {
          final error = StateError('Injected repository opening failure');
          final repo = _Repository()..openError = error;
          final service = _Service();
          final cache = _cache(repo, _Files(), service);
          for (var attempt = 0; attempt < 3; attempt++) {
            final errors = <Object>[];
            final complete = Completer<void>();
            cache
                .getFileStream('unused', key: 'opening-$attempt')
                .listen(
                  (_) => fail('Opening failure must not deliver a file'),
                  onError: (Object value) => errors.add(value),
                  onDone: complete.complete,
                );
            await complete.future;
            expect(errors, [same(error)]);
          }
          expect(service.requests, 0);
          await cache.close();
          await cache.close();
          expect(repo.closeCalls, 1);
          expect(repo.afterClose, 0);
          final healthyRepo = _Repository();
          final healthy = _cache(healthyRepo, _Files(), service);
          expect(
            await healthy.getSingleFile('unused', key: 'healthy'),
            isNotNull,
          );
          await healthy.close();
          expect(service.requests, 1);
          await Future<void>.delayed(const Duration(milliseconds: 20));
          finished.complete();
        } catch (error, stack) {
          finished.completeError(error, stack);
        }
      }, (error, _) => uncaught.add(error));
      await finished.future.timeout(const Duration(seconds: 3));
      expect(uncaught, isEmpty);
    },
  );

  for (final closeWhileBlocked in [false, true]) {
    test(
      'whole attempt capacity bounds blocked reads and cancelled ownership with ${closeWhileBlocked ? "close" : "retry"}',
      () async {
        final gate = Completer<CacheObject?>();
        final repo = _Repository()..pendingRead = gate;
        final service = _Service();
        final cache = _cache(repo, _Files(), service);
        final overflow = <Object>[];
        final subscriptions = <StreamSubscription<FileResponse>>[];
        for (var attempt = 0; attempt < 120; attempt++) {
          subscriptions.add(
            cache
                .getFileStream('unused', key: 'blocked-$attempt')
                .listen(
                  (_) =>
                      fail('Cancelled blocked owner must not deliver a file'),
                  onError: (Object error) => overflow.add(error),
                ),
          );
        }
        await Future<void>.delayed(const Duration(milliseconds: 30));
        expect(repo.readCalls, 90);
        expect(cache.ownedAttemptCount, 90);
        expect(overflow, hasLength(30));
        expect(overflow, everyElement(isA<StateError>()));
        expect(service.requests, 0);
        for (final subscription in subscriptions) {
          await subscription.cancel();
        }
        expect(cache.ownedAttemptCount, 90);
        await expectLater(
          cache.getSingleFile('unused', key: 'still-full'),
          throwsStateError,
        );
        expect(repo.readCalls, 90);
        var closed = false;
        Future<void>? closing;
        if (closeWhileBlocked) {
          closing = cache.close().then((_) {
            closed = true;
          });
          await Future<void>.delayed(const Duration(milliseconds: 30));
          expect(closed, isFalse);
          expect(repo.closed, isFalse);
        }
        repo.pendingRead = null;
        gate.complete(null);
        for (var tick = 0; tick < 100 && cache.ownedAttemptCount != 0; tick++) {
          await Future<void>.delayed(const Duration(milliseconds: 5));
        }
        expect(cache.ownedAttemptCount, 0);
        expect(service.requests, 0);
        if (closeWhileBlocked) {
          await closing!.timeout(const Duration(seconds: 2));
        } else {
          expect(
            await cache.getSingleFile('unused', key: 'current-retry'),
            isNotNull,
          );
          expect(service.requests, 1);
          await cache.close();
        }
        expect(repo.closed, isTrue);
        expect(repo.closeCalls, 1);
        expect(repo.afterClose, 0);
      },
    );
  }

  for (final lookup in [1, 2, 3, 4]) {
    test(
      'actual package admitted lookup #$lookup error rejects fallback before HTTP and permits independent retry',
      () async {
        final error = StateError('Injected admitted lookup #$lookup failure');
        final repo = _Repository()
          ..readError = error
          ..failAtCall = lookup;
        final service = _Service();
        final fs = _Files();
        final cache = _cache(repo, fs, service);
        try {
          await expectLater(
            cache
                .getFileStream(
                  'https://example.invalid/cover',
                  key: LibraryImageCache.transferKey(
                    'immutable-nth-lookup',
                    'first',
                  ),
                )
                .toList()
                .timeout(const Duration(seconds: 1)),
            throwsA(same(error)),
          );
          expect(service.requests, 0);
          expect(repo.objects, isEmpty);
          expect(fs.fs.directory('/').listSync(), isEmpty);
          // The failure is attempt-owned, not a permanent shared miss/error entry.
          final retry = await cache
              .getFileStream(
                'https://example.invalid/cover',
                key: LibraryImageCache.transferKey(
                  'immutable-nth-lookup',
                  'second',
                ),
              )
              .toList()
              .timeout(const Duration(seconds: 1));
          expect(retry.whereType<FileInfo>(), hasLength(1));
          expect(service.requests, 1);
        } finally {
          await cache.close().timeout(const Duration(seconds: 1));
        }
        expect(repo.closed, isTrue);
        expect(repo.afterClose, 0);
      },
    );
  }
  for (final publication in [false, true]) {
    test(
      'real initial immutable ${publication ? 'metadata publication' : 'write'} failure owns partial file through cleanup and close',
      () async {
        final repo = _Repository();
        final fs = _Files();
        final cache = _cache(repo, fs);
        final key = LibraryImageCache.transferKey('immutable-initial', 'first');
        await cache
            .getFileStream('https://example.invalid/cover', key: key)
            .toList();
        if (publication) {
          repo.publicationError = StateError(
            'Injected initial metadata failure',
          );
        } else {
          fs.writeFailure = true;
        }
        fs.deletionFailures = 1;
        await cache.rememberValidated(
          'immutable-initial',
          key,
          isCurrent: () => true,
        );
        expect(
          fs.fs.directory('/').listSync().where((f) => f.path.endsWith('.jpg')),
          hasLength(1),
        );
        expect(repo.objects.containsKey('immutable-initial'), isFalse);
        await cache.close().timeout(const Duration(seconds: 2));
        expect(fs.fs.directory('/').listSync(), isEmpty);
        expect(repo.closed, isTrue);
        expect(repo.afterClose, 0);
      },
    );
  }
  test(
    'persistent failed initial writes retain at most 80 physical owners and reject further creation; close retry recovers',
    () async {
      final repo = _Repository();
      final fs = _Files()
        ..writeFailure = true
        ..deletionFailures = -1;
      final cache = _cache(repo, fs);
      for (var i = 0; i < 80; i++) {
        await expectLater(
          cache.putFile('', _bytes, key: 'immutable-$i', fileExtension: 'jpg'),
          throwsA(isA<io.FileSystemException>()),
        );
      }
      final before = fs.creates;
      for (var i = 0; i < 3; i++) {
        await expectLater(
          cache.putFile('', _bytes, key: 'rejected-$i', fileExtension: 'jpg'),
          throwsStateError,
        );
      }
      expect(fs.creates, before);
      expect(fs.fs.directory('/').listSync(), hasLength(80));
      await expectLater(cache.close(), throwsA(isA<io.FileSystemException>()));
      expect(repo.closed, isTrue);
      fs.deletionFailures = 0;
      await cache.close();
      expect(fs.fs.directory('/').listSync(), isEmpty);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'actual package retrieval and download lookup errors settle with original error, independent retry succeeds and close is finite',
    () async {
      final repo = _Repository();
      final fs = _Files();
      final service = _Service();
      final cache = _cache(repo, fs, service);
      final error = StateError('Admitted metadata get failure');
      repo.readError = error;
      await expectLater(
        cache.store
            .retrieveCacheData('direct')
            .timeout(const Duration(seconds: 1)),
        throwsA(same(error)),
      );
      await expectLater(
        cache
            .getFileStream(
              'https://example.invalid/cover',
              key: LibraryImageCache.transferKey('immutable-read', 'first'),
            )
            .toList()
            .timeout(const Duration(seconds: 1)),
        throwsA(same(error)),
      );
      expect(service.requests, 0);
      repo.readError = null;
      final retried = await cache
          .getFileStream(
            'https://example.invalid/cover',
            key: LibraryImageCache.transferKey('immutable-read', 'second'),
          )
          .toList();
      expect(retried.whereType<FileInfo>(), hasLength(1));
      expect(service.requests, 1);
      repo.readError = error;
      await expectLater(
        cache.getFileFromCache('after-success'),
        throwsA(same(error)),
      );
      await cache.close().timeout(const Duration(seconds: 1));
      expect(repo.closed, isTrue);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'actual package removeFile failure retains physical owner after metadata deletion; close retries without delegate access',
    () async {
      final repo = _Repository();
      final fs = _Files();
      final cache = _cache(repo, fs);
      final file = await cache.putFile('', _bytes, key: 'remove');
      fs.deletionFailures = -1;
      await expectLater(
        cache.removeFile('remove'),
        throwsA(isA<io.FileSystemException>()),
      );
      expect(repo.objects, isEmpty);
      expect(await file.exists(), isTrue);
      await expectLater(cache.close(), throwsA(isA<io.FileSystemException>()));
      expect(repo.closed, isTrue);
      fs.deletionFailures = 0;
      await cache.close();
      expect(await file.exists(), isFalse);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'repeated failed package removals bound physical ownership and storage admission',
    () async {
      final repo = _Repository();
      final fs = _Files();
      final cache = _cache(repo, fs);
      for (var i = 0; i < 80; i++) {
        await cache.putFile('', _bytes, key: 'remove-$i');
        fs.deletionFailures = -1;
        await expectLater(
          cache.removeFile('remove-$i'),
          throwsA(isA<io.FileSystemException>()),
        );
      }
      final before = fs.creates;
      await expectLater(
        cache.putFile('', _bytes, key: 'overflow'),
        throwsStateError,
      );
      expect(fs.creates, before);
      expect(repo.objects, isEmpty);
      expect(fs.fs.directory('/').listSync(), hasLength(80));
      await expectLater(cache.close(), throwsA(isA<io.FileSystemException>()));
      fs.deletionFailures = 0;
      await cache.close();
      expect(fs.fs.directory('/').listSync(), isEmpty);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'accepted package lookup error during disposal settles consumer and finite close without late delegate access',
    () async {
      final repo = _Repository()..pendingRead = Completer<CacheObject?>();
      final cache = _cache(repo, _Files());
      final reading = cache.getFileFromCache('pending');
      final error = StateError('Admitted get failure during close');
      final observed = expectLater(reading, throwsA(same(error)));
      await Future<void>.delayed(Duration.zero);
      final closing = cache.close();
      repo.pendingRead!.completeError(error);
      await observed;
      await closing.timeout(const Duration(seconds: 1));
      expect(repo.closed, isTrue);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'real package transfer metadata publication failure reaches consumer before FileInfo and close cleans bytes',
    () async {
      final repo = _Repository()
        ..publicationError = StateError(
          'Admitted transfer publication failure',
        );
      final fs = _Files();
      final cache = _cache(repo, fs);
      await expectLater(
        cache
            .getFileStream(
              'https://example.invalid/cover',
              key: LibraryImageCache.transferKey(
                'immutable-publication',
                'attempt',
              ),
            )
            .toList(),
        throwsA(same(repo.publicationError)),
      );
      await cache.close();
      expect(fs.fs.directory('/').listSync(), isEmpty);
      expect(repo.closed, isTrue);
    },
  );
  test(
    'actual IOFileSystem eviction failures retain explicit files through metadata deletion and disposed retry',
    () async {
      final directory = await io.Directory.systemTemp.createTemp(
        'library-io-failure-',
      );
      const channel = MethodChannel('plugins.flutter.io/path_provider');
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async => directory.path);
      final repo = _Repository();
      final fs = _FaultIoFiles(IOFileSystem('failed-package-eviction'));
      final cache = _cache(repo, fs, null, 1);
      cache.store.cleanupRunMinInterval = const Duration(milliseconds: 20);
      try {
        final removed = await cache.putFile('', _bytes, key: 'removed');
        fs.failures = -1;
        await expectLater(
          cache.removeFile('removed'),
          throwsA(isA<io.FileSystemException>()),
        );
        expect(await removed.exists(), isTrue);
        expect(repo.objects, isEmpty);
        final evicted = await cache.putFile('', _bytes, key: 'evicted');
        await cache.putFile('', _bytes, key: 'retained');
        await cache.getFileFromCache('missing');
        await Future<void>.delayed(const Duration(milliseconds: 60));
        expect(repo.objects.keys, isNot(contains('evicted')));
        expect(await evicted.exists(), isTrue);
        await expectLater(
          cache.close(),
          throwsA(isA<io.FileSystemException>()),
        );
        expect(repo.closed, isTrue);
        final creates = fs.creates;
        fs.failures = 0;
        await cache.close();
        expect(fs.creates, creates);
        expect(repo.afterClose, 0);
        expect(
          directory.listSync(recursive: true).whereType<io.File>(),
          isEmpty,
        );
      } finally {
        fs.failures = 0;
        await cache.close();
        await directory.delete(recursive: true);
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
      }
    },
  );
  test(
    'real IOFileSystem package removal, age/capacity cleanup and close delete configured physical paths',
    () async {
      final directory = await io.Directory.systemTemp.createTemp('library-io-');
      const channel = MethodChannel('plugins.flutter.io/path_provider');
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async => directory.path);
      final repo = _Repository();
      final fs = IOFileSystem('actual-package-eviction');
      final cache = _cache(repo, fs, null, 1);
      cache.store.cleanupRunMinInterval = const Duration(milliseconds: 20);
      try {
        final removed = await cache.putFile('', _bytes, key: 'removed');
        await cache.removeFile('removed');
        expect(await removed.exists(), isFalse);
        final evicted = await cache.putFile('', _bytes, key: 'over-capacity');
        await cache.putFile('', _bytes, key: 'newest');
        await cache.getFileFromCache('missing');
        await Future<void>.delayed(const Duration(milliseconds: 60));
        expect(repo.objects.keys, contains('newest'));
        expect(repo.objects.keys, isNot(contains('over-capacity')));
        expect(await evicted.exists(), isFalse);
        final aged = await cache.putFile('', _bytes, key: 'aged');
        repo.objects['aged'] = CacheObject.fromMap({
          ...repo.objects['aged']!.toMap(),
          CacheObject.columnTouched: DateTime.now()
              .subtract(const Duration(minutes: 11))
              .millisecondsSinceEpoch,
        });
        await cache.getFileFromCache('missing-age');
        await Future<void>.delayed(const Duration(milliseconds: 60));
        expect(await aged.exists(), isFalse);
        await cache.close();
        expect(
          directory.listSync(recursive: true).whereType<io.File>(),
          isEmpty,
        );
        expect(repo.closed, isTrue);
        expect(repo.afterClose, 0);
      } finally {
        await cache.close();
        await directory.delete(recursive: true);
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
      }
    },
  );
}
