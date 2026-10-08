import 'dart:async';
import 'dart:convert';
import 'dart:io' as io;
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:file/file.dart' show File;
import 'package:http/io_client.dart';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/core/network/library_cache_store.dart';
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart';

class _NativeHttpOverrides extends io.HttpOverrides {}

class _Repository extends CacheObjectProvider {
  _Repository(String path) : super(path: path);
  bool rejectDelete = false;
  @override
  Future<int> deleteAll(Iterable<int> ids) => rejectDelete
      ? Future.error(
          StateError('Injected SQLite failed-source deletion failure'),
        )
      : super.deleteAll(ids);
}

class _GatedHeroCache extends StoryDetailImageCache {
  _GatedHeroCache(super.configuration) : super.forTesting();
  final started = Completer<void>();
  final release = Completer<void>();
  int calls = 0;
  @override
  Future<void> validateResponse(FileInfo response, String key) async {
    if (++calls == 1) {
      started.complete();
      await release.future;
    }
    await super.validateResponse(response, key);
  }
}

final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);
final _checksum = sha256.convert(_png).toString();
final _identity = 'qa:tr:1:$_checksum';
final _goodIdentity = 'qa:tr:2:$_checksum';
final _key = LibraryImageCache.transferKey(_identity, '1:2');

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  sqfliteFfiInit();
  databaseFactory = databaseFactoryFfi;
  const channel = MethodChannel('plugins.flutter.io/path_provider');
  late io.Directory directory;
  late IOFileSystem files;
  late _Repository repository;
  late io.HttpServer server;
  late String url;
  var requests = 0;
  Completer<void>? responseRelease;
  LibraryImageCache? cache;
  Future<void> idle() async {
    final until = DateTime.now().add(const Duration(seconds: 3));
    while (cache!.ownedAttemptCount != 0) {
      if (DateTime.now().isAfter(until)) {
        fail('Real package attempt did not settle');
      }
      await Future<void>.delayed(const Duration(milliseconds: 1));
    }
  }

  Future<void> reopenWithSeed({required bool expired}) async {
    await repository.open();
    await (await files.createFile('bad.png')).writeAsBytes(List.filled(100, 0));
    await repository.insert(
      CacheObject(
        url,
        key: _key,
        relativePath: 'bad.png',
        length: 100,
        validTill: DateTime.now().add(Duration(minutes: expired ? -1 : 10)),
      ),
    );
    await (await files.createFile('good.png')).writeAsBytes(_png);
    await repository.insert(
      CacheObject(
        url,
        key: _goodIdentity,
        relativePath: 'good.png',
        length: _png.length,
        validTill: DateTime.now().add(const Duration(minutes: 10)),
      ),
    );
    await repository.close();
    repository = _Repository('${directory.path}/detail.db');
    cache = StoryDetailImageCache.forTesting(
      Config(
        'detail',
        maxNrOfCacheObjects: 20,
        stalePeriod: const Duration(minutes: 10),
        repo: repository,
        fileSystem: files,
      ),
      clientFactory: () =>
          IOClient(_NativeHttpOverrides().createHttpClient(null)),
    );
  }

  Future<void> assertHealthy(File filesFile) async {
    final bytes = await filesFile.readAsBytes();
    expect(sha256.convert(bytes).toString(), _checksum);
    final codec = await ui.instantiateImageCodec(Uint8List.fromList(bytes));
    final frame = await codec.getNextFrame();
    expect(frame.image.width, 2);
    expect(frame.image.height, 2);
    frame.image.dispose();
    codec.dispose();
  }

  setUp(() async {
    requests = 0;
    responseRelease = null;
    directory = await io.Directory.systemTemp.createTemp(
      'story12-expired-cache-',
    );
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (_) async => directory.path);
    files = IOFileSystem('detail');
    repository = _Repository('${directory.path}/detail.db');
    server = await io.HttpServer.bind(io.InternetAddress.loopbackIPv4, 0);
    url = 'http://127.0.0.1:${server.port}/hero.png';
    server.listen((request) async {
      requests++;
      await responseRelease?.future;
      request.response.headers.set(
        io.HttpHeaders.contentTypeHeader,
        'image/png',
      );
      request.response.headers.set(
        io.HttpHeaders.cacheControlHeader,
        'max-age=600',
      );
      request.response.contentLength = _png.length;
      request.response.add(_png);
      await request.response.close();
    });
  });
  tearDown(() async {
    repository.rejectDelete = false;
    if (responseRelease != null && !responseRelease!.isCompleted) {
      responseRelease!.complete();
    }
    final gated = cache;
    if (gated is _GatedHeroCache && !gated.release.isCompleted) {
      gated.release.complete();
    }
    await cache?.close();
    cache = null;
    await server.close(force: true);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
    await directory.delete(recursive: true);
  });
  test(
    'real SQLite timer cleanup preserves shared cold bytes while another delivered consumer awaits actual decode',
    () async {
      responseRelease = Completer<void>();
      cache = LibraryImageCache.forTesting(
        Config(
          'detail',
          stalePeriod: const Duration(minutes: 10),
          maxNrOfCacheObjects: 80,
          repo: repository,
          fileSystem: files,
        ),
        clientFactory: () =>
            IOClient(_NativeHttpOverrides().createHttpClient(null)),
      );
      final store = cache!.store as LibraryCacheStore;
      // Delay the first real cleanup until both consumer FileInfos have arrived.
      store.cleanupRunMinInterval = const Duration(milliseconds: 250);
      final firstKey = LibraryImageCache.transferKey(
        _goodIdentity,
        'shared-first',
      );
      final secondKey = LibraryImageCache.transferKey(
        _goodIdentity,
        'shared-second',
      );
      final first = cache!.getSingleFile(url, key: firstKey);
      final second = cache!.getSingleFile(url, key: secondKey);
      while (requests == 0) {
        await Future<void>.delayed(const Duration(milliseconds: 1));
      }
      responseRelease!.complete();
      final delivered = await Future.wait([first, second]);
      expect(requests, 1);
      expect(delivered[0].path, delivered[1].path);
      await assertHealthy(delivered[0]);
      await cache!.rememberValidated(
        _goodIdentity,
        firstKey,
        isCurrent: () => true,
      );
      await cache!.discardFailedTransfer(firstKey, failed: false);
      final transfer = (await repository.getAllObjects()).singleWhere(
        (entry) => entry.key == firstKey,
      );
      await repository.update(
        CacheObject(
          transfer.url,
          id: transfer.id,
          key: transfer.key,
          relativePath: transfer.relativePath,
          validTill: transfer.validTill,
          length: transfer.length,
          eTag: transfer.eTag,
          touched: DateTime.now().subtract(const Duration(days: 1)),
        ),
        setTouchedToNow: false,
      );
      await Future<void>.delayed(const Duration(milliseconds: 350));
      await store.drain();
      expect(
        await delivered[1].exists(),
        isTrue,
        reason:
            'The other current decoder still owns these actual shared bytes',
      );
      await assertHealthy(delivered[1]);
      await cache!.rememberValidated(
        _goodIdentity,
        secondKey,
        isCurrent: () => true,
      );
      final warm = await cache!.getSingleFile(
        url,
        key: LibraryImageCache.transferKey(_goodIdentity, 'after-shared'),
      );
      await assertHealthy(warm);
      expect(requests, 1);
      await idle();
      expect((await repository.getAllObjects()).length, lessThanOrEqualTo(80));
    },
  );
  test(
    'real SQLite timer cleanup preserves concurrent warm validation until the gated reader actually decodes',
    () async {
      await repository.open();
      await (await files.createFile('good.png')).writeAsBytes(_png);
      await repository.insert(
        CacheObject(
          url,
          key: _goodIdentity,
          relativePath: 'good.png',
          length: _png.length,
          validTill: DateTime.now().add(const Duration(minutes: 10)),
          touched: DateTime.now().subtract(const Duration(days: 1)),
        ),
        setTouchedToNow: false,
      );
      final gated = _GatedHeroCache(
        Config(
          'detail',
          stalePeriod: const Duration(minutes: 10),
          maxNrOfCacheObjects: 20,
          repo: repository,
          fileSystem: files,
        ),
      );
      cache = gated;
      final store = gated.store as LibraryCacheStore;
      store.cleanupRunMinInterval = const Duration(milliseconds: 40);
      final firstKey = LibraryImageCache.transferKey(
        _goodIdentity,
        'gated-warm',
      );
      final first = gated.getSingleFile(url, key: firstKey);
      await gated.started.future;
      final secondKey = LibraryImageCache.transferKey(
        _goodIdentity,
        'other-warm',
      );
      final second = await gated.getSingleFile(url, key: secondKey);
      await assertHealthy(second);
      await gated.discardFailedTransfer(secondKey, failed: false);
      await Future<void>.delayed(const Duration(milliseconds: 100));
      await store.drain();
      expect(await (await files.createFile('good.png')).exists(), isTrue);
      gated.release.complete();
      await assertHealthy(await first);
      await idle();
      expect(requests, 0);
      expect(await repository.get(_goodIdentity), isNotNull);
    },
  );
  test(
    'real SQLite reopened expired corrupt attempt renews healthy HTTP bytes before SHA/decode and preserves independent immutable data',
    () async {
      await reopenWithSeed(expired: true);
      final file = await cache!.getSingleFile(url, key: _key);
      await assertHealthy(file);
      await idle();
      expect(requests, 1);
      final good = await cache!.getSingleFile(
        url,
        key: LibraryImageCache.transferKey(_goodIdentity, 'warm'),
      );
      await assertHealthy(good);
      await idle();
      expect(requests, 1);
      expect(await (await files.createFile('good.png')).readAsBytes(), _png);
      expect(
        directory
            .listSync(recursive: true)
            .whereType<io.File>()
            .where((file) => file.path.endsWith('.png'))
            .length,
        lessThanOrEqualTo(20),
      );
    },
  );
  test(
    'real SQLite current corrupt attempt fails SHA, evicts only failed transfer and same-key explicit retry fetches healthy source',
    () async {
      await reopenWithSeed(expired: false);
      await expectLater(
        cache!.getSingleFile(url, key: _key),
        throwsFormatException,
      );
      await cache!.discardFailedTransfer(_key);
      await idle();
      expect(await repository.get(_key), isNull);
      expect(await (await files.createFile('bad.png')).exists(), isFalse);
      expect(await (await files.createFile('good.png')).readAsBytes(), _png);
      final file = await cache!.getSingleFile(url, key: _key);
      await assertHealthy(file);
      await idle();
      expect(requests, 1);
      expect(await repository.get(_goodIdentity), isNotNull);
      final reopened = await databaseFactoryFfi.openDatabase(
        '${directory.path}/detail.db',
        options: OpenDatabaseOptions(readOnly: true, singleInstance: false),
      );
      final rows = await reopened.query('cacheObject');
      expect(
        rows.where((row) => row['key'] == _key).single['relativePath'],
        isNot('bad.png'),
      );
      expect(
        rows.where((row) => row['key'] == _goodIdentity).single['relativePath'],
        'good.png',
      );
      await reopened.close();
      expect(cache!.ownedAttemptCount, 0);
    },
  );
  test(
    'real SQLite failed immutable cleanup waits for parallel reader and preserves another key sharing the physical file',
    () async {
      await repository.open();
      await (await files.createFile(
        'bad.png',
      )).writeAsBytes(List.filled(100, 0));
      final validTill = DateTime.now().add(const Duration(minutes: 10));
      await repository.insert(
        CacheObject(
          url,
          key: _identity,
          relativePath: 'bad.png',
          length: 100,
          validTill: validTill,
        ),
      );
      await repository.insert(
        CacheObject(
          url,
          key: 'independent-alias',
          relativePath: 'bad.png',
          length: 100,
          validTill: validTill,
        ),
      );
      await repository.close();
      repository = _Repository('${directory.path}/detail.db');
      final gated = _GatedHeroCache(
        Config(
          'detail',
          maxNrOfCacheObjects: 20,
          stalePeriod: const Duration(minutes: 10),
          repo: repository,
          fileSystem: files,
        ),
      );
      cache = gated;
      final first = gated.getSingleFile(
        url,
        key: LibraryImageCache.transferKey(_identity, 'first'),
      );
      final firstError = expectLater(first, throwsFormatException);
      await gated.started.future;
      await expectLater(
        gated.getSingleFile(
          url,
          key: LibraryImageCache.transferKey(_identity, 'second'),
        ),
        throwsFormatException,
      );
      expect(await repository.get(_identity), isNotNull);
      expect(await (await files.createFile('bad.png')).exists(), isTrue);
      gated.release.complete();
      await firstError;
      await idle();
      expect(await repository.get(_identity), isNull);
      expect(await repository.get('independent-alias'), isNotNull);
      expect(await (await files.createFile('bad.png')).exists(), isTrue);
      expect(requests, 0);
    },
  );
  test(
    'real SQLite normal warm cancellation preserves valid immutable bytes for next route',
    () async {
      await reopenWithSeed(expired: false);
      final key = LibraryImageCache.transferKey(_goodIdentity, 'normal-back');
      await assertHealthy(await cache!.getSingleFile(url, key: key));
      await idle();
      await cache!.discardFailedTransfer(key, failed: false);
      expect(await repository.get(_goodIdentity), isNotNull);
      expect(await (await files.createFile('good.png')).readAsBytes(), _png);
      final nextKey = LibraryImageCache.transferKey(
        _goodIdentity,
        'next-route',
      );
      await assertHealthy(await cache!.getSingleFile(url, key: nextKey));
      await idle();
      expect(requests, 0);
      expect(cache!.ownedAttemptCount, 0);
    },
  );
  test(
    'real SQLite failed-source deletion failure retains ownership and bytes until explicit cleanup retry succeeds',
    () async {
      await reopenWithSeed(expired: false);
      repository.rejectDelete = true;
      await expectLater(
        cache!.getSingleFile(url, key: _key),
        throwsFormatException,
      );
      await idle();
      await expectLater(cache!.discardFailedTransfer(_key), throwsStateError);
      expect(await repository.get(_key), isNotNull);
      expect(await (await files.createFile('bad.png')).exists(), isTrue);
      expect(await (await files.createFile('good.png')).readAsBytes(), _png);
      expect(requests, 0);
      repository.rejectDelete = false;
      await cache!.discardFailedTransfer(_key);
      expect(await repository.get(_key), isNull);
      expect(await (await files.createFile('bad.png')).exists(), isFalse);
      await assertHealthy(await cache!.getSingleFile(url, key: _key));
      await idle();
      expect(requests, 1);
      expect(cache!.ownedAttemptCount, 0);
    },
  );
}
