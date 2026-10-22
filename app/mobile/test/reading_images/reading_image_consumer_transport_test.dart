import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/data/reading_image_consumer_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/reading_image_admission.dart';
import 'consumer_fixture.dart';

void main() {
  test(
    'gerçek HTTP metadata chunked; rendition MIME/length/no-store ve redirect sınırı',
    () async {
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      var mode = 'json';
      var followed = 0;
      final subscription = server.listen((request) async {
        expect(
          request.headers.value(HttpHeaders.authorizationHeader),
          'Bearer ephemeral',
        );
        expect(
          request.headers.value(HttpHeaders.acceptEncodingHeader),
          'identity',
        );
        request.response.headers.set(
          HttpHeaders.cacheControlHeader,
          'private,no-store',
        );
        if (request.uri.path.endsWith('/follow')) followed++;
        if (mode == 'redirect') {
          request.response.statusCode = 302;
          request.response.headers.set(HttpHeaders.locationHeader, '/follow');
        } else if (mode == 'json') {
          request.response.headers.contentType = ContentType.json;
          request.response.write(jsonEncode({'bounded': true}));
        } else {
          request.response.headers.contentType = ContentType(
            mode == 'mime' ? 'text' : 'image',
            mode == 'mime' ? 'plain' : 'jpeg',
          );
          if (mode == 'cache') {
            request.response.headers.set(
              HttpHeaders.cacheControlHeader,
              'public,max-age=1000',
            );
          }
          if (mode == 'encoding') {
            request.response.headers.set(
              HttpHeaders.contentEncodingHeader,
              'gzip',
            );
          }
          request.response.contentLength = jpegBytes.length;
          request.response.add(jpegBytes);
        }
        await request.response.close();
      });
      final source = ReadingImageConsumerDataSource(
        consumerConfiguration(
          origin: Uri.parse('http://127.0.0.1:${server.port}'),
        ),
      );
      try {
        expect(await source.json('manifest', {}, ReadingImageCancellation()), {
          'bounded': true,
        });
        mode = 'bytes';
        expect(
          await source.read(
            'bytes',
            {},
            ReadingImageCancellation(),
            1048576,
            'image/jpeg',
          ),
          jpegBytes,
        );
        for (final refusal in ['redirect', 'mime', 'cache', 'encoding']) {
          mode = refusal;
          await expectLater(
            source.read(
              'bytes',
              {},
              ReadingImageCancellation(),
              1048576,
              'image/jpeg',
            ),
            throwsA(isA<HttpException>()),
          );
        }
        expect(followed, 0);
        mode = 'bytes';
        await expectLater(
          source.read('bytes', {}, ReadingImageCancellation(), 1, 'image/jpeg'),
          throwsA(isA<HttpException>()),
        );
      } finally {
        await subscription.cancel();
        await server.close(force: true);
      }
    },
  );
  for (final phase in ['header', 'body']) {
    for (final external in [false, true]) {
      test(
        'gerçek HTTP açık $phase ${external ? 'external cancellation' : 'requestWait'} ile kesilir ve yeni istek çalışır',
        () async {
          final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
          final received = Completer<HttpRequest>();
          final bodyFlushed = Completer<void>();
          final subscription = server.listen((request) async {
            request.response.headers.contentType = ContentType('image', 'jpeg');
            request.response.headers.set(
              HttpHeaders.cacheControlHeader,
              'private,no-store',
            );
            request.response.contentLength = jpegBytes.length;
            if (request.uri.queryParameters['hold'] == 'true') {
              received.complete(request);
              if (phase == 'body') {
                request.response.add(jpegBytes.take(1).toList());
                await request.response.flush();
                bodyFlushed.complete();
              }
            } else {
              request.response.add(jpegBytes);
              await request.response.close();
            }
          });
          final source = ReadingImageConsumerDataSource(
            consumerConfiguration(
              origin: Uri.parse('http://127.0.0.1:${server.port}'),
              requestWait: Duration(milliseconds: external ? 2000 : 100),
            ),
          );
          final cancellation = ReadingImageCancellation();
          Timer? cancelTimer;
          try {
            final watch = Stopwatch()..start();
            final pending = source.read(
              'bytes',
              {'hold': 'true'},
              cancellation,
              1048576,
              'image/jpeg',
            );
            final failed = expectLater(pending, throwsA(anything));
            final held = await received.future;
            if (phase == 'body') await bodyFlushed.future;
            if (external) {
              cancelTimer = Timer(
                const Duration(milliseconds: 30),
                cancellation.cancel,
              );
            }
            await failed.timeout(const Duration(seconds: 1));
            expect(watch.elapsed, lessThan(const Duration(seconds: 1)));
            // Server henüz yanıtı bitirmeden istemci işi gerçekten sonlanmıştır.
            expect(
              await source.read(
                'bytes',
                {},
                ReadingImageCancellation(),
                1048576,
                'image/jpeg',
              ),
              jpegBytes,
            );
            // Sunucudaki açık yanıtın da kaynaklarını kapat.
            try {
              await held.response.close();
            } catch (_) {}
          } finally {
            cancelTimer?.cancel();
            await subscription.cancel();
            await server.close(force: true);
          }
        },
      );
    }
  }
}
