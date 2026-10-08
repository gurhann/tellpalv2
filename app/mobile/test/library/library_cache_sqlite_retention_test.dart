import 'dart:async';
import 'dart:convert';
import 'dart:io' as io;
import 'dart:ui' as ui;
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';

final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);

class _Repository extends CacheObjectProvider {
  _Repository(String path) : super(path: path);
  bool rejectDelete = false;
  @override
  Future<int> deleteAll(Iterable<int> ids) {
    if (rejectDelete) {
      return Future.error(StateError('Injected SQL deletion failure'));
    }
    return super.deleteAll(ids);
  }
}

class _Service extends FileService {
  @override
  Future<FileServiceResponse> get(
    String url, {
    Map<String, String>? headers,
  }) async => _Response();
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

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  sqfliteFfiInit();
  databaseFactory = databaseFactoryFfi;
  const channel = MethodChannel('plugins.flutter.io/path_provider');
  late io.Directory root;
  late IOFileSystem files;
  late _Repository repository;
  LibraryImageCache? cache;
  int physicalCount() =>
      io.Directory('${root.path}/home').listSync().whereType<io.File>().length;
  Future<void> seed(int count, {bool expired = false}) async {
    await repository.open();
    for (var index = 0; index < count; index++) {
      final name = 'seed-$index.png';
      final bytes = expired ? Uint8List.fromList([0, 1, 2]) : _png;
      await (await files.createFile(name)).writeAsBytes(bytes);
      await repository.insert(
        CacheObject(
          '',
          key: 'seed-$index',
          relativePath: name,
          validTill: DateTime.now().add(Duration(minutes: expired ? -1 : 10)),
          length: bytes.length,
        ),
      );
    }
  }

  LibraryImageCache openCache() => LibraryImageCache.forTesting(
    Config(
      'home',
      stalePeriod: const Duration(minutes: 10),
      maxNrOfCacheObjects: 80,
      repo: repository,
      fileSystem: files,
      fileService: _Service(),
    ),
  );
  setUp(() async {
    root = await io.Directory.systemTemp.createTemp(
      'story12-sqlite-retention-',
    );
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (_) async => root.path);
    files = IOFileSystem('home');
    repository = _Repository('${root.path}/home.db');
  });
  tearDown(() async {
    repository.rejectDelete = false;
    if (cache != null) await cache!.close();
    cache = null;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
    await root.delete(recursive: true);
  });

  test(
    'real SQLite current-day capacity query returns zero; restarted 82-file profile repairs before each publication and preserves warm reads',
    () async {
      await seed(82);
      expect(await repository.getObjectsOverCapacity(80), isEmpty);
      expect(physicalCount(), 82);
      await repository.close();
      repository = _Repository('${root.path}/home.db');
      cache = openCache();
      final warm = (await cache!.getFileFromCache('seed-81'))!;
      expect(await warm.file.readAsBytes(), _png);
      for (var index = 0; index < 12; index++) {
        await cache!.putFile('', _png, key: 'new-$index');
        expect(physicalCount(), 80);
        expect((await repository.getAllObjects()).length, 80);
        expect(await repository.getObjectsOverCapacity(80), isEmpty);
      }
      expect(await warm.file.readAsBytes(), _png);
    },
  );

  test(
    'real SQLite and IO concurrent transfers, decoded promotions and publications never retain more than 80 physical files',
    () async {
      await seed(60);
      cache = openCache();
      final operations = List.generate(
        24,
        (index) => () async {
          final identity = 'parallel-$index';
          final transfer = LibraryImageCache.transferKey(identity, 'attempt');
          final info =
              (await cache!
                      .getFileStream(
                        'https://example.invalid/image',
                        key: transfer,
                      )
                      .first)
                  as FileInfo;
          expect(physicalCount(), lessThanOrEqualTo(80));
          final codec = await ui.instantiateImageCodec(
            await info.file.readAsBytes(),
          );
          final frame = await codec.getNextFrame();
          expect(frame.image.width, 2);
          frame.image.dispose();
          codec.dispose();
          await cache!.rememberValidated(
            identity,
            transfer,
            isCurrent: () => true,
          );
          expect(physicalCount(), lessThanOrEqualTo(80));
        }(),
      );
      operations.addAll(
        List.generate(
          12,
          (index) => () async {
            await cache!.putFile(
              '',
              Uint8List.fromList(_png),
              key: 'publication-$index',
            );
            expect(physicalCount(), lessThanOrEqualTo(80));
          }(),
        ),
      );
      await Future.wait(operations);
      expect(physicalCount(), 80);
      expect((await repository.getAllObjects()).length, 80);
      expect(await repository.getObjectsOverCapacity(80), isEmpty);
      await cache!.close();
      expect(physicalCount(), 0);
    },
  );

  test(
    'real SQLite concurrent expired replacements retain decoded new files and never exceed 80 physical owners',
    () async {
      await seed(60, expired: true);
      cache = openCache();
      await Future.wait(
        List.generate(
          24,
          (index) => () async {
            final identity = 'seed-$index';
            final transfer = LibraryImageCache.transferKey(identity, 'renewal');
            final info =
                (await cache!
                        .getFileStream(
                          'https://example.invalid/image',
                          key: transfer,
                        )
                        .first)
                    as FileInfo;
            final codec = await ui.instantiateImageCodec(
              await info.file.readAsBytes(),
            );
            final frame = await codec.getNextFrame();
            expect(frame.image.width, 2);
            frame.image.dispose();
            codec.dispose();
            await cache!.rememberValidated(
              identity,
              transfer,
              isCurrent: () => true,
            );
            expect(physicalCount(), lessThanOrEqualTo(80));
            final current = await cache!.getFileFromCache(identity);
            expect(current, isNotNull);
            expect(await current!.file.readAsBytes(), _png);
          }(),
        ),
      );
      expect(physicalCount(), 80);
      expect((await repository.getAllObjects()).length, 80);
      expect(await repository.getObjectsOverCapacity(80), isEmpty);
    },
  );
  test(
    'real SQLite metadata deletion failure keeps 80 physical owners and rejects writer until explicit retry succeeds',
    () async {
      await seed(80);
      cache = openCache();
      repository.rejectDelete = true;
      await expectLater(
        cache!.putFile('', _png, key: 'rejected'),
        throwsStateError,
      );
      expect(physicalCount(), 80);
      expect((await repository.getAllObjects()).length, 80);
      expect(await (await files.createFile('seed-0.png')).readAsBytes(), _png);
      repository.rejectDelete = false;
      await cache!.putFile('', _png, key: 'retry');
      expect(physicalCount(), 80);
      expect((await repository.getAllObjects()).length, 80);
      expect(await cache!.getFileFromCache('retry'), isNotNull);
    },
  );
}
