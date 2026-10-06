import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';
import 'package:tellpal_mobile_poc/features/library/data/repository_impl/library_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';

Map<String, dynamic> fixture() {
  Map<String, dynamic> item(int id) => {
    'contentId': id,
    'externalKey': 'story-$id',
    'type': 'STORY',
    'experienceType': 'READING',
    'contentLanguage': 'tr',
    'title': 'Title',
    'ageRange': null,
    'durationMinutes': null,
    'isFree': false,
    'isLocked': true,
    'covers': {'phone': null, 'tablet': null},
  };
  return {
    'schemaVersion': 1,
    'requestedLanguage': 'tr',
    'audience': 'NON_SUBSCRIBER',
    'previewLimit': 5,
    'rows': [
      {
        'categoryId': 1,
        'slug': 'row',
        'name': 'Row',
        'description': null,
        'displayOrder': 0,
        'contentLanguage': 'tr',
        'items': [for (var id = 1; id <= 5; id++) item(id)],
      },
    ],
  };
}

void main() {
  for (final invalid in [
    'requested language',
    'item language',
    'duplicate category',
    'preview overflow',
    'fractional schemaVersion',
    'fractional previewLimit',
    'fractional categoryId',
    'fractional displayOrder',
    'fractional contentId',
    'fractional ageRange',
    'fractional durationMinutes',
    'fractional phone assetId',
    'fractional tablet assetId',
  ]) {
    test(
      'real HTTP DTO $invalid returns invalidResponse and Cubit error',
      () async {
        final payload = fixture();
        final rows = payload['rows'] as List;
        if (invalid == 'requested language') {
          payload['requestedLanguage'] = 'en';
        }
        if (invalid == 'item language') {
          rows[0]['items'][0]['contentLanguage'] = 'en';
        }
        if (invalid == 'duplicate category') {
          rows.add({...rows[0] as Map<String, dynamic>, 'displayOrder': 1});
        }
        if (invalid == 'preview overflow') {
          rows[0]['items'].add({
            ...rows[0]['items'][0] as Map<String, dynamic>,
            'contentId': 6,
          });
        }
        if (invalid.startsWith('fractional ')) {
          final field = invalid.substring('fractional '.length);
          if (field == 'schemaVersion' || field == 'previewLimit') {
            payload[field] = 1.9;
          } else if (field == 'categoryId' || field == 'displayOrder') {
            rows[0][field] = 1.9;
          } else if (field.endsWith('assetId')) {
            final variant = field.split(' ').first;
            rows[0]['items'][0]['covers'] = <String, dynamic>{
              ...rows[0]['items'][0]['covers'] as Map,
              variant: {
                'assetId': 1.9,
                'kind': 'LISTING_COVER',
                'mimeType': 'image/jpeg',
                'url': 'https://example.invalid/cover.jpg',
                'expiresAt': DateTime.now()
                    .add(const Duration(minutes: 10))
                    .toIso8601String(),
              },
            };
          } else {
            rows[0]['items'][0][field] = 1.9;
          }
        }
        final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        var requests = 0;
        server.listen((request) async {
          requests++;
          expect(request.uri.path, '/api/test/library');
          expect(request.uri.queryParameters['lang'], 'tr');
          request.response.headers.contentType = ContentType.json;
          request.response.write(jsonEncode(payload));
          await request.response.close();
        });
        final config = LibraryConfiguration(
          baseUrl: Uri.parse('http://127.0.0.1:${server.port}'),
          idToken: 'ephemeral-test-token',
          context: const LibraryContext('tr', 'test-account'),
        );
        final transport = AuthenticatedTransport(config);
        final repository = LibraryRepositoryImpl(transport);
        final result = await repository.fetch(config.context);
        expect(
          result.fold((failure) => failure.kind, (_) => null),
          FailureKind.invalidResponse,
        );
        final cubit = LibraryCubit(
          GetLibrary(repository),
          config,
          LibraryDiagnostics(),
        );
        await cubit.load();
        expect(cubit.state.status, LibraryStatus.error);
        expect(cubit.state.failure?.kind, FailureKind.invalidResponse);
        expect(cubit.state.catalog, isNull);
        expect(requests, 2);
        await cubit.close();
        transport.close();
        await server.close(force: true);
      },
    );
  }
}
