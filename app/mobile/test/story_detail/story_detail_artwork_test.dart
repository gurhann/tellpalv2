import 'dart:async';
import 'dart:io' as io;
import 'dart:typed_data';
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:tellpal_mobile_poc/core/network/library_transfer_file_service.dart';
import 'package:tellpal_mobile_poc/core/network/library_cache_lifecycle.dart';
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';

class MemoryFiles implements FileSystem {
  final system = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => system.file('/$name');
}

class BlockingRepository extends NonStoringObjectProvider {
  final read = Completer<CacheObject?>();
  @override
  Future<CacheObject?> get(String key) => read.future;
}

class BlockingValidationCache extends StoryDetailImageCache {
  BlockingValidationCache(super.configuration) : super.forTesting();
  final validation = Completer<void>();
  @override
  Future<void> validateResponse(FileInfo response, String key) =>
      validation.future;
}

class EncodedClient extends http.BaseClient {
  EncodedClient(this.bytes, {this.declared});
  final List<int> bytes;
  final int? declared;
  bool closed = false;
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async =>
      http.StreamedResponse(
        Stream.value(bytes),
        200,
        contentLength: declared,
        headers: {'content-type': 'image/jpeg'},
      );
  @override
  void close() {
    closed = true;
  }
}

void main() {
  test(
    'profile forwarding file alternate mutation APIs cannot bypass write admission',
    () async {
      final files = MemoryFiles();
      final lifecycle = LibraryCacheFiles(
        files,
        capacity: 20,
        maximumBytes: 104857600,
        reservedFileBytes: 20971520,
      );
      final file = await runZoned(
        () => lifecycle.createFile('admitted.jpg'),
        zoneValues: {LibraryCacheFiles.ownerKey: 'owner'},
      );
      await file.writeAsString('admitted');
      await expectLater(file.copy('/unowned.jpg'), throwsUnsupportedError);
      expect(() => file.copySync('/unowned-sync.jpg'), throwsUnsupportedError);
      await expectLater(file.rename('/moved.jpg'), throwsUnsupportedError);
      expect(() => file.renameSync('/moved-sync.jpg'), throwsUnsupportedError);
      expect(() => file.writeAsBytesSync([1]), throwsUnsupportedError);
      expect(() => file.writeAsStringSync('unowned'), throwsUnsupportedError);
      expect(() => file.createSync(), throwsUnsupportedError);
      expect(() => file.open(mode: io.FileMode.write), throwsUnsupportedError);
      expect(
        () => file.openSync(mode: io.FileMode.append),
        throwsUnsupportedError,
      );
      expect(
        () => file.openWrite(mode: io.FileMode.append),
        throwsUnsupportedError,
      );
      await expectLater(
        file.writeAsBytes([1], mode: io.FileMode.append),
        throwsUnsupportedError,
      );
      expect(await file.readAsString(), 'admitted');
      expect(files.system.directory('/').listSync(), hasLength(1));
      await lifecycle.release('owner', {});
      await lifecycle.clearAndClose();
    },
  );
  test(
    'cancelled checksum/file validation retains all four owned slots until it settles',
    () async {
      final cache = BlockingValidationCache(
        Config(
          'detail-validation-slots',
          repo: NonStoringObjectProvider(),
          fileSystem: MemoryFiles(),
          maxNrOfCacheObjects: 20,
        ),
      );
      final immutable = 'account:tr:1:${'a' * 64}';
      await cache.putFile('', Uint8List.fromList([1]), key: immutable);
      final subscriptions = List.generate(
        4,
        (i) => cache
            .getFileStream(
              'http://example.invalid',
              key: '$immutable::library-transfer::$i',
            )
            .listen((_) {}),
      );
      for (var i = 0; i < 10; i++) {
        await Future<void>.delayed(Duration.zero);
      }
      expect(cache.ownedAttemptCount, 4);
      for (final subscription in subscriptions) {
        await subscription.cancel();
      }
      expect(cache.ownedAttemptCount, 4);
      await expectLater(
        cache.getFileStream('http://example.invalid', key: 'overflow').toList(),
        throwsStateError,
      );
      cache.validation.complete();
      for (var i = 0; i < 30 && cache.ownedAttemptCount > 0; i++) {
        await Future<void>.delayed(Duration.zero);
      }
      expect(cache.ownedAttemptCount, 0);
      await cache.close();
    },
  );
  test(
    'encoded transport accepts twenty MiB and releases clients on body/header overflow',
    () async {
      for (final length in [20971520, 20971521]) {
        for (final declared in [false, true]) {
          final client = EncodedClient(
            List.filled(length, 0),
            declared: declared ? length : null,
          );
          final service = LibraryTransferFileService(
            artworkWait: const Duration(seconds: 10),
            maximumTransferBytes: 20971520,
            clientFactory: () => client,
          );
          final response = service.get(
            'http://example.invalid/hero',
            headers: {LibraryTransferFileService.ownershipHeader: 'attempt'},
          );
          if (length > 20971520 && declared) {
            await expectLater(response, throwsStateError);
          } else {
            final stream = (await response).content;
            if (length > 20971520) {
              await expectLater(stream.toList(), throwsStateError);
            } else {
              expect(
                (await stream.toList()).fold<int>(
                  0,
                  (n, chunk) => n + chunk.length,
                ),
                20971520,
              );
            }
          }
          expect(client.closed, true);
          service.close();
        }
      }
    },
  );
  test(
    'hero targets preserve source aspect and bound phone/tablet decoded pixels',
    () {
      for (final dimensions in [(512, 909), (8000, 5000), (10, 4000000)]) {
        final hero = StoryHero(
          assetId: 1,
          checksumSha256: 'a' * 64,
          mimeType: 'image/jpeg',
          byteSize: 20,
          url: 'https://example.invalid',
          expiresAt: DateTime.now(),
          width: dimensions.$1,
          height: dimensions.$2,
        );
        for (final tablet in [false, true]) {
          final size = storyHeroDecodeSize(hero, tablet);
          expect(size.$1, lessThanOrEqualTo(tablet ? 1600 : 1080));
          expect(size.$2, lessThanOrEqualTo(2048));
          expect(size.$1 * size.$2 * 4, lessThanOrEqualTo(16777216));
        }
      }
    },
  );
  test(
    'disk admission atomically counts concurrent reserved files near byte ceiling',
    () async {
      final delegate = MemoryFiles();
      final owned = LibraryCacheFiles(
        delegate,
        capacity: 20,
        maximumBytes: 104857600,
        reservedFileBytes: 20971520,
      );
      final retained = await runZoned(
        () => owned.createFile('retained'),
        zoneValues: {LibraryCacheFiles.ownerKey: 'old'},
      );
      await retained.writeAsBytes(List.filled(20971520, 0));
      await owned.release('old', {'retained'});
      final attempts = await Future.wait(
        List.generate(
          5,
          (index) => runZoned(
            () => owned
                .createFile('pending-$index')
                .then((file) async {
                  await file.writeAsBytes([0]);
                  return file;
                })
                .then<Object>((file) => file, onError: (Object error) => error),
            zoneValues: {LibraryCacheFiles.ownerKey: 'attempt-$index'},
          ),
        ),
      );
      expect(attempts.whereType<files.File>(), hasLength(4));
      expect(attempts.whereType<StateError>(), hasLength(1));
      for (var index = 0; index < 5; index++) {
        await owned.release('attempt-$index', {});
      }
      await owned.clearAndClose();
    },
  );
  test(
    'retained files count toward twenty-file capacity independently of byte allowance',
    () async {
      final owned = LibraryCacheFiles(
        MemoryFiles(),
        capacity: 20,
        maximumBytes: 104857600,
        reservedFileBytes: 20971520,
      );
      for (var index = 0; index < 20; index++) {
        final file = await runZoned(
          () => owned.createFile('$index'),
          zoneValues: {LibraryCacheFiles.ownerKey: '$index'},
        );
        await file.writeAsBytes([0]);
        await owned.release('$index', {'$index'});
      }
      await expectLater(
        runZoned(
          () => owned
              .createFile('overflow')
              .then((file) => file.writeAsBytes([0])),
          zoneValues: {LibraryCacheFiles.ownerKey: 'overflow'},
        ),
        throwsStateError,
      );
      await owned.clearAndClose();
    },
  );
  test(
    'cancelled unsettled metadata owns four profile slots until storage actually settles',
    () async {
      final repository = BlockingRepository();
      final cache = StoryDetailImageCache.forTesting(
        Config(
          'detail-owned-slots',
          repo: repository,
          fileSystem: MemoryFiles(),
          maxNrOfCacheObjects: 20,
        ),
      );
      final subscriptions = List.generate(
        4,
        (i) => cache
            .getFileStream(
              'http://example.invalid/$i',
              key: 'account:tr:$i:${'a' * 64}',
            )
            .listen((_) {}),
      );
      await Future<void>.delayed(Duration.zero);
      expect(cache.ownedAttemptCount, 4);
      for (final subscription in subscriptions) {
        await subscription.cancel();
      }
      expect(cache.ownedAttemptCount, 4);
      await expectLater(
        cache.getFileStream('http://example.invalid/5', key: '5').toList(),
        throwsStateError,
      );
      repository.read.complete(null);
      for (var i = 0; i < 20 && cache.ownedAttemptCount > 0; i++) {
        await Future<void>.delayed(Duration.zero);
      }
      expect(cache.ownedAttemptCount, 0);
      await cache.close();
    },
  );
}
