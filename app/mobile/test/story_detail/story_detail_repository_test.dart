import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/datasource/story_detail_datasource.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/repository_impl/story_detail_repository_impl.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';

import 'story_detail_fixture.dart';

LibraryConfiguration configuration({required Uri baseUrl}) =>
    LibraryConfiguration(
      baseUrl: baseUrl,
      idToken: 'ephemeral-test-token',
      context: const LibraryContext('tr', 'account'),
    );
void main() {
  for (final dimensions in [
    (8000, 5000),
    (40000000, 1),
    (40000001, 1),
    (4611686018427387904, 3),
    (0, 1),
    (1, 0),
  ]) {
    test(
      'real HTTP hero dimensions $dimensions enforce the pixel ceiling without integer overflow',
      () async {
        final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        server.listen((request) async {
          request.response.write(
            jsonEncode({
              ...payload(),
              'hero': {
                'assetId': 1,
                'checksumSha256': 'a' * 64,
                'byteSize': 100,
                'mimeType': 'image/png',
                'url': 'https://example.invalid/hero.png',
                'expiresAt': '2026-10-09T00:00:00Z',
                'width': dimensions.$1,
                'height': dimensions.$2,
              },
            }),
          );
          await request.response.close();
        });
        final transport = AuthenticatedTransport(
          configuration(baseUrl: Uri.parse('http://127.0.0.1:${server.port}')),
        );
        addTearDown(() async {
          transport.close();
          await server.close(force: true);
        });
        final result = await StoryDetailRepositoryImpl(
          StoryDetailDatasource(transport),
        ).fetch(identity());
        expect(
          result.fold((f) => f.kind, (_) => null),
          dimensions == (8000, 5000) || dimensions == (40000000, 1)
              ? null
              : FailureKind.invalidResponse,
        );
      },
    );
  }
  for (final code in [200, 400, 401, 403, 404, 500]) {
    test(
      'real HTTP detail status $code and explicit mapped identity stay typed',
      () async {
        final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        server.listen((request) async {
          expect(request.uri.path, '/api/test/library/stories/1');
          expect(request.uri.queryParameters, {
            'categoryId': '2',
            'lang': 'tr',
            'contentLang': 'en',
            'experience': 'READING',
          });
          request.response.statusCode = code;
          request.response.write(jsonEncode(payload(ui: 'tr', lang: 'en')));
          await request.response.close();
        });
        final transport = AuthenticatedTransport(
          configuration(baseUrl: Uri.parse('http://127.0.0.1:${server.port}')),
        );
        final result = await StoryDetailRepositoryImpl(
          StoryDetailDatasource(transport),
        ).fetch(identity(ui: 'tr', lang: 'en'));
        expect(result.fold((f) => f.kind, (_) => null), switch (code) {
          200 => null,
          400 => FailureKind.invalidResponse,
          401 => FailureKind.unauthorized,
          403 => FailureKind.forbidden,
          404 => FailureKind.unavailable,
          _ => FailureKind.network,
        });
        transport.close();
        await server.close(force: true);
      },
    );
  }
  for (final violation in [
    'contentId',
    'contentLanguage',
    'requestedLanguage',
    'experienceType',
    'isLocked',
    'recommendations',
    'credits',
    'schemaVersion',
  ]) {
    test(
      'rejects $violation contract mismatch rather than substituting metadata',
      () async {
        final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        final data = payload(recommendations: true);
        if (violation == 'recommendations') {
          data['recommendations'][0]['contentId'] = 1;
        } else if (violation == 'credits') {
          data['credits'][0]['role'] = 'UNKNOWN';
        } else {
          data[violation] = switch (violation) {
            'contentId' => 99,
            'isLocked' => true,
            'schemaVersion' => 1.9,
            'experienceType' => 'AUDIO_STORY',
            _ => 'en',
          };
        }
        server.listen((r) async {
          r.response.write(jsonEncode(data));
          await r.response.close();
        });
        final transport = AuthenticatedTransport(
          configuration(baseUrl: Uri.parse('http://127.0.0.1:${server.port}')),
        );
        final result = await StoryDetailRepositoryImpl(
          StoryDetailDatasource(transport),
        ).fetch(identity());
        expect(
          result.fold((f) => f.kind, (_) => null),
          FailureKind.invalidResponse,
        );
        transport.close();
        await server.close(force: true);
      },
    );
  }
}
