import 'dart:async';
import 'dart:io' show FileSystemException;
import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';

final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);
const _identity = 'account:en:story2:tablet';
String _key(String attempt) =>
    LibraryImageCache.transferKey(_identity, attempt);

class _Files implements FileSystem {
  _Files() {
    _files = MemoryFileSystem(
      opHandle: (path, operation) {
        if (operation == FileSystemOp.delete &&
            path == failingPath &&
            failures != 0) {
          deleteAttempts++;
          if (failures > 0) failures--;
          throw const FileSystemException(
            'Injected retirement deletion failure',
          );
        }
      },
    );
  }
  late final MemoryFileSystem _files;
  String? failingPath;
  int failures = 0;
  int deleteAttempts = 0;
  @override
  Future<files.File> createFile(String name) async => _files.file('/$name');
}

class _Repository extends NonStoringObjectProvider {
  final objects = <String, CacheObject>{};
  bool closed = false;
  bool failReplacement = false;
  bool failEnumeration = false;
  int callsAfterClose = 0;
  Completer<void>? metadataEntered;
  Completer<void>? metadataRelease;
  void _check() {
    if (closed) {
      callsAfterClose++;
      throw StateError('Access after repository close');
    }
  }

  @override
  Future<CacheObject?> get(String key) async {
    _check();
    return objects[key];
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) async {
    _check();
    if (object.key == _identity &&
        failReplacement &&
        objects[object.key]?.relativePath != object.relativePath) {
      failReplacement = false;
      throw StateError('Injected replacement publication failure');
    }
    if (object.key == _identity && metadataRelease != null) {
      metadataEntered!.complete();
      await metadataRelease!.future;
      _check();
    }
    return objects[object.key] = object.copyWith(
      id: object.id ?? objects.length + 1,
    );
  }

  @override
  Future<List<CacheObject>> getAllObjects() async {
    _check();
    if (failEnumeration) {
      failEnumeration = false;
      throw StateError('Injected enumeration failure');
    }
    return objects.values.toList();
  }

  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    _check();
    objects.removeWhere((_, object) => ids.contains(object.id));
    return ids.length;
  }

  @override
  Future<bool> close() async {
    _check();
    closed = true;
    return true;
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
  Stream<List<int>> get content => Stream.value(_png);
  @override
  int get contentLength => _png.length;
  @override
  int get statusCode => 200;
  @override
  DateTime get validTill => DateTime.now().add(const Duration(minutes: 10));
  @override
  String? get eTag => null;
  @override
  String get fileExtension => '.png';
}

class _Cache extends LibraryImageCache {
  _Cache(_Repository repository, _Service service, [_Files? files])
    : super.forTesting(
        Config(
          'promotion-test',
          stalePeriod: const Duration(minutes: 10),
          maxNrOfCacheObjects: 80,
          repo: repository,
          fileSystem: files ?? _Files(),
          fileService: service,
        ),
      );
  int immutableWrites = 0;
  Completer<void>? writeEntered;
  Completer<void>? writeRelease;
  @override
  Future<files.File> putFile(
    String url,
    Uint8List bytes, {
    String? key,
    String? eTag,
    Duration maxAge = const Duration(days: 30),
    String fileExtension = 'file',
  }) async {
    if (key == _identity) {
      immutableWrites++;
      writeEntered?.complete();
      if (writeRelease != null) await writeRelease!.future;
    }
    return super.putFile(
      url,
      bytes,
      key: key,
      eTag: eTag,
      maxAge: maxAge,
      fileExtension: fileExtension,
    );
  }
}

Future<FileInfo> _delivered(_Cache cache, String key) async =>
    (await cache
            .getFileStream('https://example.invalid/tablet.jpg', key: key)
            .first)
        as FileInfo;
Future<void> _decode(FileInfo file) async {
  final codec = await ui.instantiateImageCodec(await file.file.readAsBytes());
  final frame = await codec.getNextFrame();
  expect(frame.image.width, 2);
  frame.image.dispose();
  codec.dispose();
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'failed complete-file enumeration retains persisted ownership while closing repository; close retry never reads delegate',
    () async {
      final repository = _Repository();
      final cache = _Cache(repository, _Service());
      final file = await cache.putFile('', _png, key: _identity);
      final metadata = Map<String, CacheObject>.of(repository.objects);
      repository.failEnumeration = true;
      await expectLater(cache.close(), throwsStateError);
      expect(repository.closed, isTrue);
      expect(repository.objects, metadata);
      expect(await file.exists(), isTrue);
      await cache.close();
      expect(repository.callsAfterClose, 0);
      expect(repository.objects, metadata);
      expect(
        await file.exists(),
        isTrue,
        reason:
            'Persisted metadata still owns files that could not be enumerated before closure',
      );
    },
  );
  for (final persistent in [false, true]) {
    test(
      'retirement capacity retains ${persistent ? 'persistent' : 'transient'} failed ownership and bounds new transfer admission',
      () async {
        final repository = _Repository();
        final service = _Service();
        final fileSystem = _Files();
        final cache = _Cache(repository, service, fileSystem);
        files.File? firstOld;
        // Each replacement retains its old file and decoded new file: 40 pairs
        // occupy the actual 80-file profile, rather than 80 pairs / 160 files.
        for (var index = 0; index < 40; index++) {
          final identity = 'cleanup-$index';
          final transfer = LibraryImageCache.transferKey(identity, 'new');
          await cache.putFile(
            '',
            Uint8List.fromList([0, 1, 2]),
            key: identity,
            maxAge: const Duration(seconds: -1),
          );
          final old = (await cache.getFileFromCache(identity))!.file;
          firstOld ??= old;
          final fresh = await _delivered(cache, transfer);
          await _decode(fresh);
          await cache.rememberValidated(
            identity,
            transfer,
            isCurrent: () => true,
          );
        }
        fileSystem.failingPath = firstOld!.path;
        expect(fileSystem._files.directory('/').listSync().length, 80);
        fileSystem.failures = persistent ? -1 : 1;
        for (var retry = 0; retry < (persistent ? 3 : 1); retry++) {
          await expectLater(
            _delivered(
              cache,
              LibraryImageCache.transferKey('overflow', '$retry'),
            ),
            throwsA(isA<FileSystemException>()),
          );
        }
        expect(service.requests, 40);
        expect(
          fileSystem.deleteAttempts,
          persistent ? inInclusiveRange(3, 6) : 1,
        );
        expect(await firstOld.exists(), isTrue);
        expect(
          repository.objects.values.any(
            (object) => firstOld!.path.endsWith('/${object.relativePath}'),
          ),
          isFalse,
        );
        if (persistent) {
          await expectLater(cache.close(), throwsA(isA<FileSystemException>()));
          expect(repository.closed, isTrue);
          expect(await firstOld.exists(), isTrue);
          await expectLater(cache.close(), throwsA(isA<FileSystemException>()));
          expect(await firstOld.exists(), isTrue);
          fileSystem.failures = 0;
          await cache.close();
        } else {
          await cache.close();
        }
        expect(repository.closed, isTrue);
        expect(await firstOld.exists(), isFalse);
        expect(fileSystem._files.directory('/').listSync(), isEmpty);
      },
    );
  }
  test(
    'timer retirement failure is handled, keeps ownership and close retries it',
    () async {
      final repository = _Repository();
      final files = _Files();
      final cache = _Cache(repository, _Service(), files);
      await cache.putFile(
        '',
        Uint8List.fromList([0, 1, 2]),
        key: _identity,
        maxAge: const Duration(seconds: -1),
      );
      final old = (await cache.getFileFromCache(_identity))!.file;
      final fresh = await _delivered(cache, _key('timer'));
      await _decode(fresh);
      await runZoned(
        () => cache.rememberValidated(
          _identity,
          _key('timer'),
          isCurrent: () => true,
        ),
        zoneSpecification: ZoneSpecification(
          createTimer: (self, parent, zone, duration, callback) =>
              parent.createTimer(
                zone,
                duration == const Duration(minutes: 10)
                    ? const Duration(milliseconds: 20)
                    : duration,
                callback,
              ),
        ),
      );
      files.failingPath = old.path;
      files.failures = 1;
      await Future<void>.delayed(const Duration(milliseconds: 50));
      expect(files.deleteAttempts, 1);
      expect(await old.exists(), isTrue);
      await cache.close();
      expect(await old.exists(), isFalse);
      expect(repository.closed, isTrue);
    },
  );
  test(
    'duplicate decoded transfers coalesce promotion and never rewrite a shared reader file',
    () async {
      final cache = _Cache(_Repository(), _Service());
      await cache.putFile('', _png, key: _key('row1'));
      await cache.putFile('', _png, key: _key('row2'));
      await _decode(await _delivered(cache, _key('row1')));
      await _decode(await _delivered(cache, _key('row2')));
      cache.writeEntered = Completer<void>();
      cache.writeRelease = Completer<void>();
      final first = cache.rememberValidated(
        _identity,
        _key('row1'),
        isCurrent: () => true,
      );
      await cache.writeEntered!.future;
      final second = cache.rememberValidated(
        _identity,
        _key('row2'),
        isCurrent: () => true,
      );
      expect(cache.immutableWrites, 1);
      cache.writeRelease!.complete();
      await Future.wait([first, second]);
      final shared = (await cache.getFileFromCache(_identity))!;
      final reader = await shared.file.open();
      await _decode(await _delivered(cache, _key('row3')));
      await cache.rememberValidated(
        _identity,
        _key('row3'),
        isCurrent: () => true,
      );
      expect(cache.immutableWrites, 1);
      expect(
        (await cache.getFileFromCache(_identity))!.file.path,
        shared.file.path,
      );
      expect(await reader.read(_png.length), _png);
      await reader.close();
      await cache.close();
    },
  );
  test(
    'warm immutable hit cannot promote unrelated stale transfer bytes',
    () async {
      final cache = _Cache(_Repository(), _Service());
      await cache.putFile('', Uint8List.fromList([0, 1, 2]), key: _key('old'));
      await cache.putFile('', _png, key: _identity);
      final warm = await _delivered(cache, _key('old'));
      await _decode(warm);
      await cache.rememberValidated(
        _identity,
        _key('old'),
        isCurrent: () => true,
      );
      expect(cache.immutableWrites, 1);
      expect(
        await (await cache.getFileFromCache(_identity))!.file.readAsBytes(),
        _png,
      );
      await cache.close();
    },
  );
  test(
    'corrupt warm hit is evicted on explicit retry; unrelated transfer failure preserves good shared file',
    () async {
      final service = _Service();
      final cache = _Cache(_Repository(), service);
      await cache.putFile('', Uint8List.fromList([0, 1, 2]), key: _identity);
      final corrupt = await _delivered(cache, _key('bad'));
      await expectLater(_decode(corrupt), throwsException);
      await cache.discardFailedTransfer(_key('bad'));
      expect(await cache.getFileFromCache(_identity), isNull);
      await _decode(await _delivered(cache, _key('retry')));
      expect(service.requests, 1);
      await cache.rememberValidated(
        _identity,
        _key('retry'),
        isCurrent: () => true,
      );
      final good = (await cache.getFileFromCache(_identity))!;
      await cache.discardFailedTransfer(_key('unrelated-network-failure'));
      expect(
        (await cache.getFileFromCache(_identity))!.file.path,
        good.file.path,
      );
      await _decode(good);
      await cache.close();
    },
  );
  test(
    'close drains promotion and package metadata write before emptying storage and declines new promotion',
    () async {
      final repository = _Repository();
      final cache = _Cache(repository, _Service());
      await _decode(await _delivered(cache, _key('active')));
      repository.metadataEntered = Completer<void>();
      repository.metadataRelease = Completer<void>();
      final promotion = cache.rememberValidated(
        _identity,
        _key('active'),
        isCurrent: () => true,
      );
      await repository.metadataEntered!.future;
      final closing = cache.close();
      expect(cache.close(), same(closing));
      await cache.rememberValidated(
        _identity,
        _key('new'),
        isCurrent: () => true,
      );
      expect(repository.closed, isFalse);
      expect(cache.immutableWrites, 1);
      repository.metadataRelease!.complete();
      await promotion;
      await closing;
      expect(repository.closed, isTrue);
      expect(repository.objects, isEmpty);
    },
  );
  test(
    'fresh current decode renews expired shared metadata without rewriting active readers; next owner reuses it',
    () async {
      final service = _Service();
      final cache = _Cache(_Repository(), service);
      await cache.putFile(
        '',
        _png,
        key: _identity,
        maxAge: const Duration(seconds: -1),
      );
      final expired = (await cache.getFileFromCache(_identity))!;
      final reader = await expired.file.open();
      final downloaded = await _delivered(cache, _key('fresh'));
      await _decode(downloaded);
      expect(service.requests, 1);
      await cache.rememberValidated(
        _identity,
        _key('fresh'),
        isCurrent: () => true,
      );
      final renewed = (await cache.getFileFromCache(_identity))!;
      expect(renewed.validTill.isAfter(DateTime.now()), isTrue);
      expect(renewed.file.path, expired.file.path);
      expect(cache.immutableWrites, 1);
      expect(await reader.read(_png.length), _png);
      await reader.close();
      await _decode(await _delivered(cache, _key('next-owner')));
      expect(service.requests, 1);
      await cache.close();
    },
  );
  test(
    'fresh decode recovers corrupt expired shared bytes without rewriting active reader; next owner has no refetch',
    () async {
      final service = _Service();
      final cache = _Cache(_Repository(), service);
      final corrupt = Uint8List.fromList([0, 1, 2]);
      await cache.putFile(
        '',
        corrupt,
        key: _identity,
        maxAge: const Duration(seconds: -1),
      );
      final expired = (await cache.getFileFromCache(_identity))!;
      final reader = await expired.file.open();
      final downloaded = await _delivered(cache, _key('fresh-recovery'));
      await _decode(downloaded);
      await cache.rememberValidated(
        _identity,
        _key('fresh-recovery'),
        isCurrent: () => true,
      );
      final recovered = (await cache.getFileFromCache(_identity))!;
      expect(recovered.file.path, isNot(expired.file.path));
      expect(recovered.validTill.isAfter(DateTime.now()), isTrue);
      expect(await expired.file.readAsBytes(), corrupt);
      expect(await reader.read(corrupt.length), corrupt);
      await reader.close();
      await _decode(await _delivered(cache, _key('next-valid-owner')));
      expect(service.requests, 1);
      await cache.close();
      expect(await expired.file.exists(), isFalse);
    },
  );
  test(
    'partial corrupt-expired metadata publication rolls back and close removes every delivered file',
    () async {
      final repository = _Repository();
      final cache = _Cache(repository, _Service());
      final corrupt = Uint8List.fromList([0, 1, 2]);
      await cache.putFile(
        '',
        corrupt,
        key: _identity,
        maxAge: const Duration(seconds: -1),
      );
      final expired = (await cache.getFileFromCache(_identity))!;
      final downloaded = await _delivered(cache, _key('failed-publication'));
      await _decode(downloaded);
      repository.failReplacement = true;
      await cache.rememberValidated(
        _identity,
        _key('failed-publication'),
        isCurrent: () => true,
      );
      expect(repository.failReplacement, isFalse);
      final shared = (await cache.getFileFromCache(_identity))!;
      expect(shared.file.path, expired.file.path);
      expect(shared.validTill.isAfter(DateTime.now()), isFalse);
      expect(await shared.file.readAsBytes(), corrupt);
      await _decode(
        (await cache.getFileFromCache(_key('failed-publication')))!,
      );
      await cache.close();
      expect(await downloaded.file.exists(), isFalse);
      expect(await expired.file.exists(), isFalse);
      expect(repository.objects, isEmpty);
    },
  );
}
