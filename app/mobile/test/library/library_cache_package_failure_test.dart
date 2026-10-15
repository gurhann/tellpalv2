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

class _OpeningRepository extends _Repository {
  final opening = Completer<bool>();
  final entered = Completer<void>();
  final beforeOpenError = StateError('Repository enumerated before opening');
  int openCalls = 0, enumerations = 0, publications = 0;
  bool opened = false;

  @override
  Future<bool> open() async {
    check();
    openCalls++;
    if (!entered.isCompleted) entered.complete();
    final result = await opening.future;
    opened = true;
    return result;
  }

  @override
  Future<List<CacheObject>> getAllObjects() {
    enumerations++;
    if (!opened) throw beforeOpenError;
    return super.getAllObjects();
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) {
    publications++;
    return super.updateOrInsert(object);
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

class _DelayedOldPathFiles implements FileSystem {
  _DelayedOldPathFiles(this.delegate);
  final FileSystem delegate;
  final oldPaths = Completer<void>();
  int pendingOldPaths = 0;
  @override
  Future<files.File> createFile(String name) async {
    if (name.endsWith('.file')) {
      pendingOldPaths++;
      await oldPaths.future;
    }
    return delegate.createFile(name);
  }
}

class _HeldBodyResponse extends _Response {
  final listened = Completer<void>();
  late final body = StreamController<List<int>>(
    onListen: () => listened.complete(),
  );
  @override
  Stream<List<int>> get content => body.stream;
}

class _HeldBodyService extends _Service {
  final response = _HeldBodyResponse();
  @override
  Future<FileServiceResponse> get(
    String url, {
    Map<String, String>? headers,
  }) async {
    requests++;
    return response;
  }
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
  for (final cancel in [false, true]) {
    test(
      'real detail package late old-path cleanup after ${cancel ? 'cancellation and ' : ''}close has no uncaught error',
      () async {
        final directory = await io.Directory.systemTemp.createTemp(
          'detail-late-cleanup-',
        );
        const channel = MethodChannel('plugins.flutter.io/path_provider');
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (call) async => directory.path);
        final repo = _Repository();
        final fs = _DelayedOldPathFiles(IOFileSystem('late-detail-paths'));
        final held = _HeldBodyService();
        final service = cancel ? held : _Service();
        final cache = LibraryImageCache.profile(
          Config(
            'late-detail-paths',
            repo: repo,
            fileSystem: fs,
            fileService: service,
            maxNrOfCacheObjects: 20,
          ),
          delegate: service,
        );
        final uncaught = <Object>[], finished = Completer<void>();
        runZonedGuarded(() async {
          try {
            if (cancel) {
              final subscription = cache
                  .getFileStream(
                    'https://example.invalid/late',
                    key: 'cancelled',
                  )
                  .listen((_) {});
              await held.response.listened.future.timeout(
                const Duration(seconds: 2),
              );
              await subscription.cancel();
            } else {
              final result = await cache
                  .getFileStream('https://example.invalid/late', key: 'loaded')
                  .toList();
              expect(result.whereType<FileInfo>(), hasLength(1));
            }
            expect(fs.pendingOldPaths, greaterThan(0));
            await cache.close().timeout(const Duration(seconds: 2));
            fs.oldPaths.complete();
            await Future<void>.delayed(const Duration(milliseconds: 10));
            final late = await cache.config.fileSystem.createFile(
              'declined-late.jpg',
            );
            await expectLater(late.writeAsBytes([1]), throwsStateError);
            await expectLater(
              Stream<List<int>>.value([1]).pipe(late.openWrite()),
              throwsStateError,
            );
            expect(
              directory.listSync(recursive: true).whereType<io.File>(),
              isEmpty,
            );
            expect(uncaught, isEmpty);
            finished.complete();
          } catch (error, stack) {
            finished.completeError(error, stack);
          }
        }, (error, stack) => uncaught.add(error));
        try {
          await finished.future;
        } finally {
          if (!fs.oldPaths.isCompleted) fs.oldPaths.complete();
          await cache.close().timeout(const Duration(seconds: 2));
          await directory.delete(recursive: true);
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, null);
        }
      },
    );
  }
  test(
    'reopened detail profile counts existing physical bytes and permits warm reads at full budget',
    () async {
      final directory = await io.Directory.systemTemp.createTemp(
        'detail-persisted-budget-',
      );
      const channel = MethodChannel('plugins.flutter.io/path_provider');
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async => directory.path);
      final repo = _Repository(),
          service = _Service(),
          disk = IOFileSystem('detail-persisted-budget');
      final bytes = Uint8List(20971520);
      for (var index = 0; index < 5; index++) {
        final name = 'persisted-$index.jpg';
        await (await disk.createFile(name)).writeAsBytes(bytes);
        await repo.updateOrInsert(
          CacheObject(
            'old-source',
            key: 'persisted-$index',
            relativePath: name,
            validTill: DateTime.now().add(const Duration(minutes: 10)),
            length: bytes.length,
          ),
        );
      }
      // Fresh lifecycle ownership starts empty while package metadata/physical files already exist.
      final cache = LibraryImageCache.profile(
        Config(
          'detail-persisted-budget',
          repo: repo,
          fileSystem: disk,
          fileService: service,
          maxNrOfCacheObjects: 20,
        ),
        delegate: service,
      );
      try {
        expect(await cache.getFileFromCache('persisted-0'), isNotNull);
        await expectLater(
          cache
              .getFileStream(
                'https://example.invalid/current',
                key: 'new-transfer',
              )
              .toList()
              .timeout(const Duration(seconds: 2)),
          throwsStateError,
        );
        for (var wait = 0; wait < 20 && cache.ownedAttemptCount != 0; wait++) {
          await Future<void>.delayed(Duration.zero);
        }
        expect(cache.ownedAttemptCount, 0);
        final physical = directory
            .listSync(recursive: true)
            .whereType<io.File>()
            .toList();
        expect(physical, hasLength(5));
        expect(
          physical.fold<int>(0, (total, file) => total + file.lengthSync()),
          104857600,
        );
        expect(repo.objects, hasLength(5));
        expect(await cache.getFileFromCache('persisted-4'), isNotNull);
      } finally {
        await cache.close().timeout(const Duration(seconds: 2));
        await directory.delete(recursive: true);
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
      }
    },
  );
  test(
    'actual detail package old-path removal does not reserve disk and full admission fails through consumer',
    () async {
      final directory = await io.Directory.systemTemp.createTemp(
        'detail-package-capacity-',
      );
      const channel = MethodChannel('plugins.flutter.io/path_provider');
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async => directory.path);
      final repo = _Repository(), service = _Service();
      final cache = LibraryImageCache.profile(
        Config(
          'detail-real-capacity',
          repo: repo,
          fileSystem: IOFileSystem('detail-real-capacity'),
          fileService: service,
          maxNrOfCacheObjects: 20,
        ),
        delegate: service,
      );
      final uncaught = <Object>[];
      final finished = Completer<void>();
      runZonedGuarded(() async {
        try {
          for (var index = 0; index < 4; index++) {
            await cache.putFile(
              '',
              Uint8List(20971520),
              key: 'retained-$index',
            );
          }
          final response = await cache
              .getFileStream('https://example.invalid/detail', key: 'current')
              .toList()
              .timeout(const Duration(seconds: 2));
          expect(response.whereType<FileInfo>(), hasLength(1));
          expect(repo.objects.keys, contains('current'));
          expect(
            directory.listSync(recursive: true).whereType<io.File>(),
            hasLength(5),
          );
          // Fill the remaining byte allowance with a real retained file, then reject another writer.
          await cache.removeFile('current');
          await cache.putFile('', Uint8List(20971520), key: 'last-retained');
          await expectLater(
            cache
                .getFileStream(
                  'https://example.invalid/rejected',
                  key: 'rejected',
                )
                .toList()
                .timeout(const Duration(seconds: 2)),
            throwsStateError,
          );
          for (
            var wait = 0;
            wait < 20 && cache.ownedAttemptCount != 0;
            wait++
          ) {
            await Future<void>.delayed(Duration.zero);
          }
          expect(cache.ownedAttemptCount, 0);
          expect(
            directory.listSync(recursive: true).whereType<io.File>(),
            hasLength(5),
          );
          await cache.removeFile('last-retained');
          final recovered = await cache
              .getFileStream(
                'https://example.invalid/retry',
                key: 'independent-retry',
              )
              .toList()
              .timeout(const Duration(seconds: 2));
          expect(recovered.whereType<FileInfo>(), hasLength(1));
          await Future<void>.delayed(Duration.zero);
          expect(uncaught, isEmpty);
          finished.complete();
        } catch (error, stack) {
          finished.completeError(error, stack);
        }
      }, (error, stack) => uncaught.add(error));
      try {
        await finished.future;
      } finally {
        await cache.close().timeout(const Duration(seconds: 2));
        await directory.delete(recursive: true);
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
      }
    },
  );
  test(
    'profile publication waits for delayed opening before enumeration or writing',
    () async {
      final repo = _OpeningRepository(), files = _Files(), service = _Service();
      final cache = LibraryImageCache.profile(
        Config(
          'delayed-opening-publication',
          repo: repo,
          fileSystem: files,
          fileService: service,
        ),
        delegate: service,
      );
      io.File? published;
      Object? failure;
      var settled = false;
      final publication = cache
          .putFile('', _bytes, key: 'owned')
          .then<void>(
            (file) {
              published = file;
              settled = true;
            },
            onError: (Object error) {
              failure = error;
              settled = true;
            },
          );
      try {
        await repo.entered.future.timeout(const Duration(seconds: 2));
        await Future<void>.delayed(Duration.zero);
        expect(settled, false);
        expect(repo.openCalls, 1);
        expect(repo.enumerations, 0);
        expect(repo.publications, 0);
        expect(files.creates, 0);
        repo.opening.complete(true);
        await publication.timeout(const Duration(seconds: 2));
        expect(failure, null);
        expect(published, isNotNull);
        expect(await published!.readAsBytes(), _bytes);
        expect(repo.objects.keys, ['owned']);
        expect(repo.publications, 1);
        expect(repo.openCalls, 1);
        expect(service.requests, 0);
      } finally {
        if (!repo.opening.isCompleted) repo.opening.complete(true);
        await publication.timeout(const Duration(seconds: 2));
        await cache.close().timeout(const Duration(seconds: 2));
      }
      expect(repo.closeCalls, 1);
      expect(repo.afterClose, 0);
    },
  );
  test(
    'profile publication preserves delayed opening error without enumeration or writing',
    () async {
      final error = StateError('Injected delayed repository opening failure');
      final repo = _OpeningRepository(), files = _Files(), service = _Service();
      final cache = LibraryImageCache.profile(
        Config(
          'rejected-opening-publication',
          repo: repo,
          fileSystem: files,
          fileService: service,
        ),
        delegate: service,
      );
      Object? failure;
      var settled = false;
      final publication = cache
          .putFile('', _bytes, key: 'owned')
          .then<void>(
            (_) => settled = true,
            onError: (Object value) {
              failure = value;
              settled = true;
            },
          );
      try {
        await repo.entered.future.timeout(const Duration(seconds: 2));
        await Future<void>.delayed(Duration.zero);
        expect(settled, false);
        expect(repo.enumerations, 0);
        expect(repo.publications, 0);
        expect(files.creates, 0);
        repo.opening.completeError(error);
        await publication.timeout(const Duration(seconds: 2));
        expect(failure, same(error));
        expect(repo.openCalls, 1);
        expect(repo.enumerations, 0);
        expect(repo.publications, 0);
        expect(files.creates, 0);
        expect(repo.objects, isEmpty);
        expect(service.requests, 0);
      } finally {
        if (!repo.opening.isCompleted) repo.opening.completeError(error);
        await publication.timeout(const Duration(seconds: 2));
        // Failed opening remains a cleanup failure; every owner still closes.
        await expectLater(
          cache.close().timeout(const Duration(seconds: 2)),
          throwsA(same(repo.beforeOpenError)),
        );
      }
      expect(repo.closeCalls, 1);
      expect(repo.afterClose, 0);
    },
  );
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
      for (var i = 0; i < 3; i++) {
        await expectLater(
          cache.putFile('', _bytes, key: 'rejected-$i', fileExtension: 'jpg'),
          throwsStateError,
        );
        expect(fs.fs.directory('/').listSync(), hasLength(80));
      }
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
      await expectLater(
        cache.putFile('', _bytes, key: 'overflow'),
        throwsStateError,
      );
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
