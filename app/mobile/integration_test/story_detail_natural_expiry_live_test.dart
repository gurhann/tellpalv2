import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/datasource/story_detail_datasource.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/repository_impl/story_detail_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/repository/story_detail_repository.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;
import '../test_driver/native_runtime_guard.dart'
    show requireProviderExpiryDescriptor;
import 'library_live_test.dart' show LibraryTestBinding;

/// Only the first actual metadata hero descriptor is replaced by a genuinely expired provider URL.
/// No GCS response is simulated; a separate empty device-local profile avoids warm-hit masking.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'real provider ExpiredToken renders artwork error and real retry renews same-context hero',
    (tester) async {
      HttpServer? proxy;
      AuthenticatedTransport? transport;
      final clients = <HttpClient>{};
      final semantics = tester.ensureSemantics();
      final setup = <String, Object?>{'phase': 'starting'};
      final proxyObservations = <Map<String, Object?>>[];
      binding.reportData ??= {};
      binding.reportData!['realProviderSetupDiagnostics'] = setup;
      binding.reportData!['realProviderProxyDiagnostics'] = proxyObservations;
      try {
        LibraryDevHarness.scaleNotifier.value = 1.3;
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() predicate) async {
          final deadline = DateTime.now().add(const Duration(seconds: 60));
          while (!predicate()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native real expiry state deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        Future<void> capture(String name) async {
          for (var frame = 0; frame < 8; frame++) {
            await tester.pump(const Duration(milliseconds: 16));
          }
          await binding.takeScreenshot(name);
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final descriptor = binding.providerExpiryDescriptor;
        expect(
          descriptor,
          isNotNull,
          reason: 'Private real provider descriptor required',
        );
        requireProviderExpiryDescriptor(
          descriptor!,
          'jLRypTmoBrhhX21HsDJojurcKTC3',
          DateTime.now().toUtc(),
        );
        final config = libraryGetIt<LibraryConfiguration>();
        final library = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        await until(() => library.state.status != LibraryStatus.loading);
        expect(library.state.status, LibraryStatus.loaded);
        expect(library.state.context.language, 'tr');
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        Future<(int, Uint8List)> read(
          Uri uri, {
          bool authenticated = false,
          int ceiling = 1048576,
        }) async {
          final client = HttpClient()
            ..connectionTimeout = const Duration(seconds: 15);
          clients.add(client);
          try {
            final request = await client
                .getUrl(uri)
                .timeout(const Duration(seconds: 15));
            if (authenticated) {
              request.headers.set(
                HttpHeaders.authorizationHeader,
                'Bearer ${config.idToken}',
              );
            }
            final response = await request.close().timeout(
              const Duration(seconds: 15),
            );
            final bytes = BytesBuilder(copy: false);
            await for (final chunk in response.timeout(
              const Duration(seconds: 15),
            )) {
              bytes.add(chunk);
              if (bytes.length > ceiling) {
                throw StateError('Real provider test response exceeded bound');
              }
            }
            return (response.statusCode, bytes.takeBytes());
          } catch (_) {
            throw StateError('Real provider test request failed');
          } finally {
            clients.remove(client);
            client.close(force: true);
          }
        }

        final uri = config.baseUrl
            .resolve('/api/test/library/stories/3')
            .replace(
              queryParameters: {
                'categoryId': '2',
                'lang': 'tr',
                'contentLang': 'tr',
                'experience': 'READING',
              },
            );
        Map<String, dynamic> decode((int, Uint8List) response) {
          // Proxy callbacks run while the widget test awaits a guarded pump.
          expectSync(response.$1, 200);
          return jsonDecode(utf8.decode(response.$2)) as Map<String, dynamic>;
        }

        void sameContext(Map<String, dynamic> dto) {
          expectSync(dto['categoryId'], descriptor['categoryId']);
          expectSync(dto['contentId'], descriptor['contentId']);
          expectSync(dto['requestedLanguage'], descriptor['requestedLanguage']);
          expectSync(dto['contentLanguage'], descriptor['contentLanguage']);
          expectSync(dto['experienceType'], descriptor['experience']);
          final hero = dto['hero'] as Map<String, dynamic>;
          for (final field in [
            'assetId',
            'checksumSha256',
            'byteSize',
            'mimeType',
          ]) {
            expectSync(hero[field], descriptor[field]);
          }
        }

        final baseline = decode(await read(uri, authenticated: true));
        sameContext(baseline);
        final expired = await read(
          Uri.parse(descriptor['expiredUrl'] as String),
        );
        expect(expired.$1, 400);
        expect(
          utf8.decode(expired.$2).contains('<Code>ExpiredToken</Code>'),
          isTrue,
        );
        setup.addAll({
          'phase': 'actual-provider-expiry-verified',
          'providerStatus': expired.$1,
          'providerCode': 'ExpiredToken',
        });
        var metadataRequests = 0;
        Map<String, dynamic>? renewed;
        proxy = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        proxy.listen((request) async {
          final observation = <String, Object?>{
            'phase': 'received',
            'categoryId': int.tryParse(
              request.uri.queryParameters['categoryId'] ?? '',
            ),
            'contentId': int.tryParse(request.uri.pathSegments.last),
            'requestedLanguageMatches':
                request.uri.queryParameters['lang'] == 'tr',
            'contentLanguageMatches':
                request.uri.queryParameters['contentLang'] == 'tr',
            'experienceMatches':
                request.uri.queryParameters['experience'] == 'READING',
          };
          if (proxyObservations.length < 8) proxyObservations.add(observation);
          try {
            expectSync(request.uri.path, uri.path);
            expectSync(request.uri.queryParameters, uri.queryParameters);
            observation['phase'] = 'request-context-matched';
            final response = await read(
              config.baseUrl.resolve(request.uri.toString()),
              authenticated: true,
            );
            observation['upstreamStatus'] = response.$1;
            final dto = decode(response);
            sameContext(dto);
            observation['phase'] = 'upstream-context-matched';
            metadataRequests++;
            observation['metadataRequests'] = metadataRequests;
            if (metadataRequests == 1) {
              final hero = dto['hero'] as Map<String, dynamic>;
              hero['url'] = descriptor['expiredUrl'];
              hero['expiresAt'] = descriptor['expiresAt'];
            } else {
              renewed = dto;
            }
            request.response.headers.contentType = ContentType.json;
            request.response.write(jsonEncode(dto));
            await request.response.close();
            observation['phase'] = 'response-delivered';
          } catch (error) {
            observation['failureClass'] = error.runtimeType.toString();
            request.response.statusCode = 502;
            try {
              await request.response.close();
            } catch (_) {}
          }
        });
        transport = AuthenticatedTransport(
          LibraryConfiguration(
            baseUrl: Uri.parse('http://127.0.0.1:${proxy.port}'),
            idToken: config.idToken,
            context: config.context,
          ),
        );
        libraryGetIt.unregister<StoryDetailRepository>();
        libraryGetIt.registerFactory<StoryDetailRepository>(
          () => StoryDetailRepositoryImpl(StoryDetailDatasource(transport!)),
        );
        await libraryGetIt.unregister<StoryDetailImageCache>();
        libraryGetIt.registerLazySingleton<StoryDetailImageCache>(
          () => StoryDetailImageCache.forTesting(
            Config(
              'detail-natural-provider-${DateTime.now().microsecondsSinceEpoch}',
              stalePeriod: const Duration(minutes: 10),
              maxNrOfCacheObjects: 20,
            ),
          ),
          dispose: (cache) => cache.close(),
        );
        final row = library.state.catalog!.rows.singleWhere(
          (row) => row.categoryId == 2,
        );
        expect(row.contentLanguage, 'tr');
        expect(
          row.items.singleWhere((item) => item.contentId == 3).contentLanguage,
          'tr',
        );
        final vertical = find.byKey(const PageStorageKey('library-scroll'));
        final horizontal = find.byKey(const PageStorageKey('row-2'));
        for (var seek = 0; seek < 20 && horizontal.evaluate().isEmpty; seek++) {
          await tester.drag(vertical, const Offset(0, -180));
          await tester.pumpAndSettle();
        }
        expect(horizontal, findsOneWidget);
        await tester.ensureVisible(horizontal);
        final scroll = find
            .descendant(of: horizontal, matching: find.byType(Scrollable))
            .first;
        tester.state<ScrollableState>(scroll).position.jumpTo(0);
        await tester.pumpAndSettle();
        final card = find.descendant(
          of: horizontal,
          matching: find.byWidgetPredicate(
            (widget) => widget is LibraryCard && widget.item.contentId == 3,
          ),
        );
        expect(card, findsOneWidget);
        await tester.ensureVisible(card);
        await tester.tap(card);
        await tester.pump();
        await until(() => find.byType(StoryDetailView).evaluate().isNotEmpty);
        final cubit = tester
            .element(find.byType(StoryDetailView))
            .read<StoryDetailCubit>();
        await until(() => cubit.state.status != StoryDetailStatus.loading);
        setup.addAll({
          'phase': 'selected-metadata-settled',
          'status': cubit.state.status.name,
          'heroStatus': cubit.state.heroStatus.name,
          'failureKind': cubit.state.failure?.kind.name,
          'metadataRequests': metadataRequests,
        });
        expect(cubit.state.status, StoryDetailStatus.loaded);
        await until(() => cubit.state.heroStatus != StoryHeroStatus.loading);
        expect(cubit.state.heroStatus, StoryHeroStatus.error);
        expect(cubit.state.detail!.title, baseline['title']);
        expect(metadataRequests, 1);
        const strings = StoryDetailStrings('tr');
        final retry = find.widgetWithText(TextButton, strings.retryArtwork);
        await until(
          () =>
              find.text(strings.artworkError).evaluate().length == 1 &&
              retry.evaluate().length == 1,
        );
        expect(tester.getSize(retry).height, greaterThanOrEqualTo(48));
        await capture('detail-real-provider-expired-tr-ui-1.3-error');
        final retained = cubit.state.detail;
        await tester.tap(retry);
        await tester.pump();
        if (cubit.state.recoveringArtwork) {
          expect(cubit.state.detail, same(retained));
        }
        await until(
          () =>
              cubit.state.status == StoryDetailStatus.loaded &&
              cubit.state.heroStatus == StoryHeroStatus.loaded,
        );
        expect(metadataRequests, 2);
        expect(renewed, isNotNull);
        expect(cubit.state.identity!.categoryId, 2);
        expect(cubit.state.identity!.contentId, 3);
        expect(cubit.state.identity!.uiLanguage, 'tr');
        expect(cubit.state.identity!.contentLanguage, 'tr');
        expect(cubit.state.detail!.title, baseline['title']);
        expect(cubit.state.detail!.isFree, baseline['isFree']);
        expect(cubit.state.detail!.isLocked, baseline['isLocked']);
        final fresh = decode(await read(uri, authenticated: true));
        sameContext(fresh);
        final hero = fresh['hero'] as Map<String, dynamic>;
        expect(hero['url'] != descriptor['expiredUrl'], isTrue);
        expect(
          DateTime.parse(hero['expiresAt'] as String).isAfter(DateTime.now()),
          isTrue,
        );
        final source = await read(
          Uri.parse(hero['url'] as String),
          ceiling: 20971520,
        );
        expect(source.$1, 200);
        expect(source.$2.length, descriptor['byteSize']);
        expect(
          sha256.convert(source.$2).toString(),
          descriptor['checksumSha256'],
        );
        final raw = find.descendant(
          of: find.byType(StoryDetailHero),
          matching: find.byType(RawImage),
        );
        await until(() {
          if (raw.evaluate().isEmpty ||
              tester.widget<RawImage>(raw.first).image == null) {
            return false;
          }
          RenderObject? render = tester.renderObject(raw.first);
          while (render != null) {
            if (render is RenderAnimatedOpacity && render.opacity.value < 1) {
              return false;
            }
            if (render is RenderOpacity && render.opacity < 1) return false;
            render = render.parent;
          }
          return true;
        });
        final image = tester.widget<RawImage>(raw.first).image!;
        expect(
          image.width * image.height * 4,
          lessThanOrEqualTo(16 * 1024 * 1024),
        );
        await capture('detail-real-provider-expired-tr-ui-1.3-recovered');
        await tester.tap(find.byTooltip(strings.back));
        await tester.pumpAndSettle();
        expect(cubit.isClosed, true);
        expect(libraryGetIt<LibraryRouter>().stack, hasLength(1));
        await until(
          () => libraryGetIt<StoryDetailImageCache>().ownedAttemptCount == 0,
        );
        expect(clients, isEmpty);
        expect(tester.takeException(), isNull);
        binding.reportData ??= {};
        binding.reportData!['story12NaturalProviderExpiry'] = {
          'platform': Platform.operatingSystem,
          'operatingSystemVersion': Platform.operatingSystemVersion,
          'textScale': 1.3,
          'firebaseUid': descriptor['firebaseUid'],
          'categoryId': 2,
          'contentId': 3,
          'uiLanguage': 'tr',
          'contentLanguage': 'tr',
          'experience': 'READING',
          'audience': baseline['audience'],
          'assetId': descriptor['assetId'],
          'checksumSha256': descriptor['checksumSha256'],
          'byteSize': descriptor['byteSize'],
          'providerStatus': 400,
          'providerErrorCode': 'ExpiredToken',
          'providerResponseSimulated': false,
          'initialMetadataHeroOnlySubstituted': true,
          'isolatedEmptyHeroProfile': true,
          'existingHomeAndDetailCachesReset': false,
          'realCardTap': true,
          'actualRenderedArtworkError': true,
          'metadataRetained': true,
          'realRetry': true,
          'retryMetadataRequests': metadataRequests,
          'freshSameContextHttpStatus': 200,
          'freshHeroHttpStatus': 200,
          'freshHeroSourceChecksumMatched': true,
          'decodedHero': {'width': image.width, 'height': image.height},
          'renderedHeroOpacityVerified': true,
          'captureFramesFlushed': 8,
          'closedDetailAfterBack': true,
          'ownedHeroAttemptsAfterBack': 0,
          'independentUpstreamClientsSettled': true,
          'platformAccessibilityInspection': 'not executed by this target',
        };
      } finally {
        for (final client in clients) {
          client.close(force: true);
        }
        transport?.close();
        await proxy?.close(force: true);
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 5)),
  );
}
