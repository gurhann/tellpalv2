import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/datasource/story_detail_datasource.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/repository_impl/story_detail_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/repository/story_detail_repository.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;
import 'library_live_test.dart' show LibraryTestBinding;

/// Device-local injection wraps actual isolated-backend metadata and verified source bytes.
/// Outcomes are controlled failures, never evidence of production/provider natural expiry.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'native injected detail transport and artwork failure recovery',
    (tester) async {
      final records = <Map<String, Object?>>[];
      binding.reportData ??= {};
      binding.reportData!['story12InjectedFaults'] = records;
      final settledCaptures = <Map<String, Object?>>[];
      binding.reportData!['story12SettledFaultCaptures'] = settledCaptures;
      final semantics = tester.ensureSemantics();
      HttpServer? proxy;
      AuthenticatedTransport? transport;
      final silentSockets = <Socket>{};
      final upstreamClients = <HttpClient>{};
      var mode = 'healthy', metadataRequests = 0, heroRequests = 0;
      Uint8List? realHeroBytes;
      Map<String, dynamic>? baseline;
      try {
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() predicate) async {
          final deadline = DateTime.now().add(const Duration(seconds: 45));
          while (!predicate()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native injected-state deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        Future<void> capture(String name) async {
          final surface = find.byType(StoryDetailView);
          await until(() {
            final elements = surface.evaluate().toList();
            if (elements.length != 1) return false;
            final route = ModalRoute.of(elements.single);
            return route?.isCurrent == true &&
                route?.animation?.status == AnimationStatus.completed &&
                tester.getTopLeft(surface).dx.abs() <= 0.001;
          });
          for (var frame = 0; frame < 8; frame++) {
            await tester.pump(const Duration(milliseconds: 16));
          }
          final route = ModalRoute.of(tester.element(surface));
          expect(route?.isCurrent, true);
          expect(route?.animation?.status, AnimationStatus.completed);
          final origin = tester.getTopLeft(surface).dx;
          expect(origin, closeTo(0, 0.001));
          await binding.takeScreenshot(name);
          settledCaptures.add({
            'screenshotName': name,
            'routeAnimationCompleted': true,
            'horizontalOrigin': origin,
            'captureFramesFlushed': 8,
          });
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final config = libraryGetIt<LibraryConfiguration>();
        final library = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        final router = libraryGetIt<LibraryRouter>();
        await until(() => library.state.status != LibraryStatus.loading);
        expect(library.state.status, LibraryStatus.loaded);
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        Future<Uint8List> readUpstream(
          Uri uri, {
          bool authenticated = false,
          int ceiling = 20971520,
        }) async {
          final client = HttpClient()
            ..connectionTimeout = const Duration(seconds: 15);
          upstreamClients.add(client);
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
            if (response.statusCode != 200) {
              throw StateError('Actual fixture upstream request failed');
            }
            if (response.contentLength > ceiling) {
              throw StateError('Actual fixture upstream exceeds ceiling');
            }
            final bytes = BytesBuilder(copy: false);
            await for (final chunk in response.timeout(
              const Duration(seconds: 15),
            )) {
              bytes.add(chunk);
              if (bytes.length > ceiling) {
                throw StateError('Actual fixture body exceeds ceiling');
              }
            }
            return bytes.takeBytes();
          } finally {
            upstreamClients.remove(client);
            client.close(force: true);
          }
        }

        proxy = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        final localBase = Uri.parse('http://127.0.0.1:${proxy.port}');
        proxy.listen((request) async {
          try {
            if (request.uri.path == '/hero') {
              heroRequests++;
              if (mode == 'hero_expired') {
                request.response.statusCode = 403;
                await request.response.close();
                return;
              }
              if (mode == 'hero_silent') {
                final socket = await request.response.detachSocket(
                  writeHeaders: false,
                );
                silentSockets.add(socket);
                socket.write(
                  'HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: ${realHeroBytes!.length}\r\nConnection: close\r\n\r\n',
                );
                socket.add(realHeroBytes.sublist(0, 1));
                await socket.flush();
                socket.listen(
                  (_) {},
                  onDone: () {
                    silentSockets.remove(socket);
                    socket.destroy();
                  },
                  onError: (_) {
                    silentSockets.remove(socket);
                    socket.destroy();
                  },
                );
                return;
              }
              request.response.headers.contentType = ContentType(
                'image',
                'jpeg',
              );
              request.response.headers.set(
                HttpHeaders.cacheControlHeader,
                'no-store',
              );
              if (mode == 'hero_oversized_header') {
                final socket = await request.response.detachSocket(
                  writeHeaders: false,
                );
                socket.write(
                  'HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: 20971521\r\nConnection: close\r\n\r\n',
                );
                await socket.flush();
                socket.destroy();
                return;
              }
              final bytes = mode == 'hero_corrupt'
                  ? realHeroBytes!.sublist(0, 100)
                  : realHeroBytes!;
              request.response.contentLength = bytes.length;
              request.response.add(bytes);
              await request.response.close();
              return;
            }
            metadataRequests++;
            if (mode == 'metadata_network' || mode == 'metadata_timeout') {
              final socket = await request.response.detachSocket(
                writeHeaders: false,
              );
              if (mode == 'metadata_network') {
                socket.destroy();
                return;
              }
              silentSockets.add(socket);
              socket.listen(
                (_) {},
                onDone: () {
                  silentSockets.remove(socket);
                  socket.destroy();
                },
                onError: (_) {
                  silentSockets.remove(socket);
                  socket.destroy();
                },
              );
              return;
            }
            if (mode == 'metadata_503') {
              request.response.statusCode = 503;
              request.response.headers.contentType = ContentType.json;
              request.response.write('{"type":"controlled_native_failure"}');
              await request.response.close();
              return;
            }
            final payload = await readUpstream(
              config.baseUrl.resolve(request.uri.toString()),
              authenticated: true,
              ceiling: 1048576,
            );
            final dto =
                jsonDecode(utf8.decode(payload)) as Map<String, dynamic>;
            if (mode == 'hero_absent') {
              dto['hero'] = null;
            } else {
              final hero = dto['hero'] as Map<String, dynamic>;
              hero['url'] = localBase
                  .resolve('/hero')
                  .replace(queryParameters: {'attempt': '$metadataRequests'})
                  .toString();
              if (mode == 'hero_expired') {
                hero['expiresAt'] = DateTime.now()
                    .toUtc()
                    .subtract(const Duration(seconds: 1))
                    .toIso8601String();
              }
            }
            request.response.headers.contentType = ContentType.json;
            request.response.write(jsonEncode(dto));
            await request.response.close();
          } catch (_) {
            // Only fixed diagnostic text can enter the native runner/evidence.
            request.response.statusCode = 502;
            try {
              await request.response.close();
            } catch (_) {}
          }
        });
        transport = AuthenticatedTransport(
          LibraryConfiguration(
            baseUrl: localBase,
            idToken: config.idToken,
            context: config.context,
          ),
        );
        libraryGetIt.unregister<StoryDetailRepository>();
        libraryGetIt.registerFactory<StoryDetailRepository>(
          () => StoryDetailRepositoryImpl(StoryDetailDatasource(transport!)),
        );
        StoryDetailCubit active() => tester
            .element(find.byType(StoryDetailView))
            .read<StoryDetailCubit>();
        for (final language in ['tr', 'en', 'pt']) {
          LibraryDevHarness.scaleNotifier.value = 1.3;
          unawaited(
            library.load(
              context: LibraryContext(
                language,
                library.state.context.accessIdentity,
              ),
            ),
          );
          await until(() => library.state.status != LibraryStatus.loading);
          expect(library.state.status, LibraryStatus.loaded);
          await tester.pumpAndSettle();
          final row = library.state.catalog!.rows.firstWhere(
            (row) =>
                row.contentLanguage == language &&
                row.items.any((item) => item.isFree),
          );
          final item = row.items.firstWhere((item) => item.isFree);
          final query = config.baseUrl
              .resolve('/api/test/library/stories/${item.contentId}')
              .replace(
                queryParameters: {
                  'categoryId': '${row.categoryId}',
                  'lang': language,
                  'contentLang': language,
                  'experience': 'READING',
                },
              );
          baseline =
              jsonDecode(
                    utf8.decode(
                      await readUpstream(
                        query,
                        authenticated: true,
                        ceiling: 1048576,
                      ),
                    ),
                  )
                  as Map<String, dynamic>;
          final hero = baseline['hero'] as Map<String, dynamic>;
          realHeroBytes = await readUpstream(Uri.parse(hero['url'] as String));
          expect(realHeroBytes.length, hero['byteSize']);
          expect(
            sha256.convert(realHeroBytes).toString(),
            hero['checksumSha256'],
          );
          for (final fault in [
            'metadata_network',
            'metadata_timeout',
            'metadata_503',
            'hero_absent',
            'hero_corrupt',
            'hero_oversized_header',
            'hero_silent',
            'hero_expired',
          ]) {
            debugPrint(
              'Native injected case $language/$fault content ${item.contentId}',
            );
            // Cold detail-only cache prevents a healthy immutable warm hit hiding an injected fault.
            await libraryGetIt<StoryDetailImageCache>().close();
            await libraryGetIt.unregister<StoryDetailImageCache>();
            libraryGetIt.registerLazySingleton<StoryDetailImageCache>(
              () => StoryDetailImageCache(config),
              dispose: (cache) => cache.close(),
            );
            mode = fault;
            final beforeMetadata = metadataRequests, beforeHero = heroRequests;
            final started = DateTime.now();
            unawaited(
              router.push(
                StoryDetailRoute(
                  categoryId: row.categoryId,
                  contentId: item.contentId,
                  uiLanguage: language,
                  contentLanguage: language,
                ),
              ),
            );
            await tester.pump();
            await until(
              () => find.byType(StoryDetailView).evaluate().isNotEmpty,
            );
            final cubit = active();
            final strings = StoryDetailStrings(language);
            if (fault == 'metadata_timeout') {
              expect(cubit.state.status, StoryDetailStatus.loading);
              expect(cubit.state.detail, isNull);
              expect(find.text(strings.loading), findsOneWidget);
              await capture('detail-injected-$language-loading');
            }
            if (fault.startsWith('metadata')) {
              await until(
                () => cubit.state.status != StoryDetailStatus.loading,
              );
              expect(cubit.state.status, StoryDetailStatus.error);
              expect(cubit.state.detail, isNull);
              expect(
                cubit.state.failure!.kind,
                fault == 'metadata_timeout'
                    ? FailureKind.timeout
                    : FailureKind.network,
              );
              await until(
                () =>
                    find.text(strings.error).evaluate().length == 1 &&
                    find
                            .widgetWithText(FilledButton, strings.retry)
                            .evaluate()
                            .length ==
                        1,
              );
              expect(find.text(strings.error), findsOneWidget);
              expect(
                tester.getSize(find.byTooltip(strings.back)).shortestSide,
                greaterThanOrEqualTo(48),
              );
              expect(
                tester
                    .getSize(find.widgetWithText(FilledButton, strings.retry))
                    .height,
                greaterThanOrEqualTo(48),
              );
              await until(() => silentSockets.isEmpty);
              await capture('detail-injected-$language-$fault');
              expect(metadataRequests - beforeMetadata, 1);
              mode = 'healthy';
              await tester.tap(
                find.widgetWithText(FilledButton, strings.retry),
              );
            } else {
              await until(
                () => cubit.state.status != StoryDetailStatus.loading,
              );
              expect(cubit.state.status, StoryDetailStatus.loaded);
              expect(cubit.state.detail!.contentId, item.contentId);
              expect(cubit.state.detail!.title, baseline['title']);
              await until(
                () => cubit.state.heroStatus != StoryHeroStatus.loading,
              );
              expect(cubit.state.heroStatus, StoryHeroStatus.error);
              await until(
                () =>
                    find.text(strings.artworkError).evaluate().length == 1 &&
                    find
                            .widgetWithText(TextButton, strings.retryArtwork)
                            .evaluate()
                            .length ==
                        1,
              );
              expect(find.text(strings.artworkError), findsOneWidget);
              expect(
                tester
                    .getSize(
                      find.widgetWithText(TextButton, strings.retryArtwork),
                    )
                    .height,
                greaterThanOrEqualTo(48),
              );
              expect(find.byType(StoryDetailHero), findsOneWidget);
              await until(() => silentSockets.isEmpty);
              await capture('detail-injected-$language-$fault');
              expect(metadataRequests - beforeMetadata, 1);
              final retained = cubit.state.detail;
              mode = 'healthy';
              await tester.tap(
                find.widgetWithText(TextButton, strings.retryArtwork),
              );
              await tester.pump();
              if (cubit.state.recoveringArtwork) {
                expect(cubit.state.detail, same(retained));
              }
            }
            await until(
              () =>
                  cubit.state.status == StoryDetailStatus.loaded &&
                  !cubit.state.recoveringArtwork &&
                  cubit.state.heroStatus != StoryHeroStatus.loading,
            );
            expect(cubit.state.heroStatus, StoryHeroStatus.loaded);
            expect(cubit.state.identity!.contentId, item.contentId);
            expect(cubit.state.identity!.uiLanguage, language);
            expect(cubit.state.identity!.contentLanguage, language);
            expect(cubit.state.detail!.title, baseline['title']);
            expect(cubit.state.detail!.isFree, baseline['isFree']);
            expect(cubit.state.detail!.isLocked, baseline['isLocked']);
            expect(metadataRequests - beforeMetadata, 2);
            final image = find.descendant(
              of: find.byType(StoryDetailHero),
              matching: find.byType(RawImage),
            );
            await until(
              () =>
                  image.evaluate().isNotEmpty &&
                  tester.widget<RawImage>(image.first).image != null,
            );
            final bitmap = tester.widget<RawImage>(image.first).image!;
            expect(
              bitmap.width * bitmap.height * 4,
              lessThanOrEqualTo(16 * 1024 * 1024),
            );
            await capture('detail-injected-$language-$fault-recovered');
            await tester.tap(find.byTooltip(strings.back));
            await tester.pumpAndSettle();
            expect(cubit.isClosed, true);
            expect(router.stack, hasLength(1));
            expect(library.state.context.language, language);
            await until(() => silentSockets.isEmpty);
            expect(tester.takeException(), isNull);
            records.add({
              'uiLanguage': language,
              'contentLanguage': language,
              'contentId': item.contentId,
              'categoryId': row.categoryId,
              'textScale': 1.3,
              'fault': fault,
              'injected': true,
              'actualProviderNaturalExpiry': false,
              'realSourceSha256': hero['checksumSha256'],
              'realSourceByteSize': realHeroBytes.length,
              'metadataRequests': metadataRequests - beforeMetadata,
              'heroRequests': heroRequests - beforeHero,
              'explicitUiRetry': true,
              'freshMetadataCompletedBeforeHero':
                  !cubit.state.recoveringArtwork,
              'sameIdentityRecovered': true,
              'metadataRetainedDuringArtworkRecovery': !fault.startsWith(
                'metadata',
              ),
              'silentNativeSocketsSettled': silentSockets.isEmpty,
              'decodedRecovery': {
                'width': bitmap.width,
                'height': bitmap.height,
              },
              'elapsedMilliseconds': DateTime.now()
                  .difference(started)
                  .inMilliseconds,
            });
          }
        }
        binding.reportData!['platform'] = Platform.operatingSystem;
        binding.reportData!['operatingSystemVersion'] =
            Platform.operatingSystemVersion;
        binding.reportData!['injectionScope'] =
            'Device-local controlled transport and hero failures over actual isolated backend metadata/source bytes; no provider natural-expiry or physical/AX acceptance';
      } finally {
        transport?.close();
        for (final client in upstreamClients.toList()) {
          client.close(force: true);
        }
        for (final socket in silentSockets.toList()) {
          socket.destroy();
        }
        await proxy?.close(force: true);
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 15)),
  );
}
