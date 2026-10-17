import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/repository/reading_image_comparison_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'fixture.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'comparison_test_support.dart';

String role(ReadingImageRole r) => switch (r) {
  ReadingImageRole.readingCover => 'READING_COVER',
  ReadingImageRole.readingDetail => 'READING_DETAIL',
  ReadingImageRole.readingPage => 'READING_PAGE',
};
Map<String, Object?> rasterJson(ReadingImageRaster r) => {
  'provider': r.provider,
  'objectKey': r.objectKey,
  'generation': r.generation,
  'mime': r.mime,
  'byteSize': r.byteSize,
  'sha256': r.sha256,
  'width': r.width,
  'height': r.height,
};
Map<String, Object?> envelope(Fixture f) => {
  'identity': {
    'contentId': 71,
    'language': 'tr',
    'liveGeneration': 1,
    'revision': f.manifest.identity.revision,
    'manifestDigest': f.manifest.identity.manifestDigest,
    'jobId': f.manifest.identity.jobId,
  },
  'leaseMillis': 1000,
  'descriptor': {
    'images': [
      for (final e in f.manifest.images)
        {
          'source': {
            'binding': {
              'role': role(e.source.binding.role),
              'localizationId': e.source.binding.localizationId,
              'assetId': e.source.binding.assetId,
              'pageId': e.source.binding.pageId,
              'pageNumber': e.source.binding.pageNumber,
            },
            'raster': rasterJson(e.source.raster),
          },
          'renditions': [
            for (final r in e.renditions)
              {
                'renditionKey': r.renditionKey,
                'sourceSha256': r.sourceSha256,
                'profileFingerprint': r.profileFingerprint,
                'raster': rasterJson(r.raster),
              },
          ],
        },
    ],
    'profiles': {
      for (final e in f.context.profiles.entries)
        role(e.key): [
          for (final p in e.value)
            {
              'fingerprint': p.fingerprint,
              'width': p.width,
              'height': p.height,
              'allowGrowth': p.allowGrowth,
            },
        ],
    },
  },
};
void main() {
  test(
    'F13 gerçek gzip response reddedilir; identity payload için otomatik açma kapalıdır',
    () async {
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      server.listen((request) async {
        expect(
          request.headers.value(HttpHeaders.acceptEncodingHeader),
          'identity',
        );
        final bytes = gzip.encode([1, 2, 3]);
        request.response.headers.set('Cache-Control', 'private,no-store');
        request.response.headers.contentType = ContentType('image', 'png');
        request.response.headers.set(HttpHeaders.contentEncodingHeader, 'gzip');
        request.response.contentLength = bytes.length;
        request.response.add(bytes);
        await request.response.close();
      });
      final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
      await expectLater(
        ReadingImageComparisonDataSource(
          config,
        ).read('bytes', {}, ReadingComparisonCancellation(), 1000),
        throwsStateError,
      );
      await server.close(force: true);
      await config.account!.credentials.close();
    },
  );
  test('gerçek HTTP payload SHA eşleşir; yanlış SHA başarı sayılmaz', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final data = [1, 2, 3, 4, 5];
    server.listen((request) async {
      request.response.headers.set('Cache-Control', 'private,no-store');
      request.response.headers.contentType = ContentType('image', 'png');
      request.response.contentLength = data.length;
      request.response.add(data);
      await request.response.close();
    });
    final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
    final fixture = Fixture();
    final repository = ReadingImageComparisonRepositoryImpl(
      ReadingImageComparisonDataSource(config),
      config,
    );
    final old = fixture.manifest.images.first.renditions.first;
    ReadingImageRendition candidate(String checksum) => ReadingImageRendition(
      renditionKey: old.renditionKey,
      sourceSha256: old.sourceSha256,
      profileFingerprint: old.profileFingerprint,
      raster: raster(bytes: 5, sha: checksum),
    );
    final value = ReadingImageComparison(
      fixture.manifest,
      fixture.context,
      1,
      1000,
    );
    expect(
      (await repository.bytes(
        value,
        fixture.manifest.images.first.source.binding,
        candidate(sha256.convert(data).toString()),
        false,
        ReadingComparisonCancellation(),
      )).isRight(),
      isTrue,
    );
    expect(
      (await repository.bytes(
        value,
        fixture.manifest.images.first.source.binding,
        candidate('0' * 64),
        false,
        ReadingComparisonCancellation(),
      )).isLeft(),
      isTrue,
    );
    await server.close(force: true);
    await config.account!.credentials.close();
  });
  test(
    'gerçek HTTP descriptor P4a1 digest ve tüm rol/sayfa profilleriyle doğrulanır',
    () async {
      final fixture = Fixture();
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      server.listen((request) async {
        final bytes = utf8.encode(jsonEncode(envelope(fixture)));
        request.response.headers.contentType = ContentType.json;
        request.response.headers.set('Cache-Control', 'private,no-store');
        request.response.headers.contentType = ContentType('image', 'png');
        request.response.headers.contentType = ContentType.json;
        request.response.contentLength = bytes.length;
        request.response.add(bytes);
        await request.response.close();
      });
      final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
      final repository = ReadingImageComparisonRepositoryImpl(
        ReadingImageComparisonDataSource(config),
        config,
      );
      final result = await LoadReadingImageComparison(repository)(
        71,
        ReadingComparisonCancellation(),
      );
      expect(result.isRight(), isTrue);
      expect(
        result.getOrElse(() => throw StateError('admission')).manifest.identity,
        fixture.manifest.identity,
      );
      await server.close(force: true);
      await config.account!.credentials.close();
    },
  );
  test('gerçek yerel HTTP private no-store ve uzun gövde sınırı', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final task = server.listen((request) async {
      expect(
        request.headers.value('authorization'),
        'Bearer synthetic-local-http-token',
      );
      request.response.headers.set('Cache-Control', 'private,no-store');
      request.response.headers.contentType = ContentType('image', 'png');
      request.response.contentLength = 5;
      request.response.add([1, 2, 3, 4, 5]);
      await request.response.close();
    });
    final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
    final source = ReadingImageComparisonDataSource(config);
    expect(await source.read('bytes', {}, ReadingComparisonCancellation(), 5), [
      1,
      2,
      3,
      4,
      5,
    ]);
    await expectLater(
      source.read('bytes', {}, ReadingComparisonCancellation(), 4),
      throwsStateError,
    );
    await task.cancel();
    await server.close(force: true);
    await config.account!.credentials.close();
  });
  test('gerçek HTTP iptali yavaş gövde Future sonlanmasını bekler', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final opened = Completer<void>();
    server.listen((request) async {
      request.response.headers.set('Cache-Control', 'private,no-store');
      request.response.headers.contentType = ContentType('image', 'png');
      request.response.contentLength = 100;
      request.response.add([1]);
      await request.response.flush();
      opened.complete();
    });
    final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
    final cancellation = ReadingComparisonCancellation();
    final operation = ReadingImageComparisonDataSource(
      config,
    ).read('bytes', {}, cancellation, 100);
    final rejected = expectLater(operation, throwsA(anything));
    await opened.future;
    cancellation.cancel();
    await rejected.timeout(const Duration(seconds: 3));
    await server.close(force: true);
    await config.account!.credentials.close();
  });
  test('eksik no-store yanıtı ve ağsız mod HTTP öncesi reddedilir', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((r) async {
      r.response.contentLength = 1;
      r.response.add([1]);
      await r.response.close();
    });
    final config = media(Uri.parse('http://127.0.0.1:${server.port}'));
    await expectLater(
      ReadingImageComparisonDataSource(
        config,
      ).read('bytes', {}, ReadingComparisonCancellation(), 2),
      throwsStateError,
    );
    await server.close(force: true);
    await config.account!.credentials.close();
  });
}
