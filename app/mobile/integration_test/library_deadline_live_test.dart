import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'native_catalog_transport_probe.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;

/// Native-only, ephemeral transfer faults retain actual authenticated catalog and JPEG bytes.
/// Signed URLs and credentials exist only in process memory and never enter report data.
class _TransferFaults {
  _TransferFaults(this._upstream);
  final Uri _upstream;
  final HttpClient _client = HttpClient();
  final Map<String, Uri> _covers = {};
  HttpServer? _server;
  Duration nextCatalogDelay = const Duration(milliseconds: 16500);
  Duration artworkDelay = Duration.zero;
  int catalogRequests = 0;
  int nextCatalogStatus = 200;
  int? expireNextAssetId;
  final deniedCoverKeys = <String>{};
  int artworkRequests = 0;
  int delayedCatalogCompletions = 0;
  int delayedArtworkCompletions = 0;
  int disconnectedConsumers = 0;
  int? corruptAssetId;
  int corruptTransfers = 0;
  final Map<int, int> requestsByAsset = {};
  final List<String> upstreamFailures = [];
  bool _closed = false;

  Future<Uri> start() async {
    _server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    _server!.listen((request) => unawaited(_handle(request)));
    return Uri.parse('http://127.0.0.1:${_server!.port}');
  }

  Future<void> _handle(HttpRequest request) async {
    var upstreamComplete = false;
    try {
      if (request.uri.path == '/api/test/library') {
        final sequence = ++catalogRequests;
        final expiringAsset = expireNextAssetId;
        expireNextAssetId = null;
        final forcedStatus = nextCatalogStatus;
        nextCatalogStatus = 200;
        if (forcedStatus != 200) {
          request.response.statusCode = forcedStatus;
          request.response.headers.contentType = ContentType.json;
          request.response.write(jsonEncode({'status': forcedStatus}));
          await request.response.close();
          return;
        }
        final delay = nextCatalogDelay;
        nextCatalogDelay = Duration.zero;
        final upstream = await _client.getUrl(
          _upstream
              .resolve('/api/test/library')
              .replace(queryParameters: request.uri.queryParameters),
        );
        final authorization = request.headers.value(
          HttpHeaders.authorizationHeader,
        );
        if (authorization != null) {
          upstream.headers.set(HttpHeaders.authorizationHeader, authorization);
        }
        final response = await upstream.close();
        final body = await response.transform(utf8.decoder).join();
        upstreamComplete = true;
        request.response.statusCode = response.statusCode;
        request.response.headers.contentType = ContentType.json;
        if (response.statusCode == 200) {
          final catalog = jsonDecode(body) as Map<String, dynamic>;
          for (final row in catalog['rows'] as List) {
            for (final item in row['items'] as List) {
              for (final kind in ['phone', 'tablet']) {
                final cover = item['covers'][kind] as Map<String, dynamic>?;
                if (cover == null) continue;
                final key = '$sequence-${cover['assetId']}-$kind';
                _covers[key] = Uri.parse(cover['url'] as String);
                if (cover['assetId'] == expiringAsset) {
                  deniedCoverKeys.add(key);
                  cover['expiresAt'] = DateTime.now()
                      .toUtc()
                      .subtract(const Duration(seconds: 1))
                      .toIso8601String();
                }
                cover['url'] = 'http://127.0.0.1:${_server!.port}/artwork/$key';
              }
            }
          }
          if (expiringAsset != null) nextCatalogStatus = 503;
          if (delay > Duration.zero) await Future<void>.delayed(delay);
          if (_closed) return;
          request.response.write(jsonEncode(catalog));
          await request.response.close();
          if (delay > Duration.zero) delayedCatalogCompletions++;
        } else {
          request.response.write(body);
          await request.response.close();
        }
      } else if (request.uri.path.startsWith('/artwork/')) {
        artworkRequests++;
        final key = request.uri.pathSegments.last;
        final assetId = int.parse(key.split('-')[1]);
        requestsByAsset.update(
          assetId,
          (count) => count + 1,
          ifAbsent: () => 1,
        );
        if (deniedCoverKeys.contains(key)) {
          request.response.statusCode = 403;
          await request.response.close();
          return;
        }
        if (assetId == corruptAssetId) {
          request.response.headers.contentType = ContentType('image', 'jpeg');
          request.response.headers.set(
            HttpHeaders.cacheControlHeader,
            'max-age=600',
          );
          request.response.add([0xff, 0xd8, 0x00, 0x01, 0xff, 0xd9]);
          await request.response.close();
          corruptTransfers++;
          return;
        }
        final delay = artworkDelay;
        final url = _covers[request.uri.pathSegments.last];
        if (url == null) throw const FormatException('Unknown test cover');
        if (delay > Duration.zero) await Future<void>.delayed(delay);
        if (_closed) return;
        final upstream = await _client.getUrl(url);
        final response = await upstream.close();
        request.response.statusCode = response.statusCode;
        request.response.headers.contentType = response.headers.contentType;
        final bytes = <int>[];
        await for (final chunk in response) {
          bytes.addAll(chunk);
          if (bytes.length > 1048576) {
            throw const FormatException(
              'Artwork fixture exceeds transfer bound',
            );
          }
        }
        upstreamComplete = true;
        request.response.add(bytes);
        await request.response.close();
        if (delay > Duration.zero) delayedArtworkCompletions++;
      } else {
        request.response.statusCode = 404;
        await request.response.close();
      }
    } catch (error) {
      if (_closed) return;
      if (upstreamComplete &&
          (error is SocketException || error is HttpException)) {
        disconnectedConsumers++;
        return;
      }
      upstreamFailures.add(error.runtimeType.toString());
      try {
        request.response.statusCode = 502;
        await request.response.close();
      } catch (_) {
        // Timed-out consumers may have closed their local transfer.
      }
    }
  }

  Future<void> close() async {
    _closed = true;
    _client.close(force: true);
    await _server?.close(force: true);
    _covers.clear();
  }
}

class _DeadlineBinding extends IntegrationTestWidgetsFlutterBinding {
  _TransferFaults? faults;
  @override
  Future<Map<String, dynamic>> callback(Map<String, String> params) async {
    if (params['command'] == 'request_data' &&
        params['message']?.startsWith('configure:') == true) {
      try {
        final value =
            jsonDecode(params['message']!.substring(10))
                as Map<String, dynamic>;
        faults = _TransferFaults(Uri.parse(value['baseUrl'] as String));
        final proxy = await faults!.start();
        LibraryDevHarness.install(
          LibraryConfiguration(
            baseUrl: proxy,
            idToken: value['idToken'] as String,
            context: const LibraryContext('tr', 'existing-authorized-fixture'),
          ),
        );
        return {
          'isError': false,
          'response': {'message': 'configured'},
        };
      } catch (_) {
        return {
          'isError': false,
          'response': {'message': 'configuration-rejected'},
        };
      }
    }
    return super.callback(params);
  }
}

void main() {
  final binding = _DeadlineBinding();
  testWidgets(
    'actual native transfers enforce 15s catalog and 4s artwork headers with current-attempt retry',
    (tester) async {
      final semanticsHandle = tester.ensureSemantics();
      unawaited(app.runLibrary(installErrorHandlers: false));
      Future<void> until(bool Function() predicate) async {
        final deadline = DateTime.now().add(const Duration(seconds: 35));
        while (!predicate()) {
          if (DateTime.now().isAfter(deadline)) {
            fail('Native deadline test did not settle');
          }
          await tester.pump(const Duration(milliseconds: 100));
        }
      }

      Future<void> settleRenderedImages() async {
        await until(() {
          final rendered = find.byType(RawImage).evaluate();
          return rendered.isNotEmpty &&
              rendered.every(
                (element) => (element.widget as RawImage).image != null,
              );
        });
        await tester.pumpAndSettle();
      }

      Iterable<ArtworkCubit> artworks() => find
          .byWidgetPredicate(
            (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
          )
          .evaluate()
          .map((element) => element.read<ArtworkCubit>());
      try {
        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final cubit = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        final configuration = libraryGetIt<LibraryConfiguration>();
        expect(configuration.catalogWait, const Duration(seconds: 15));
        expect(configuration.artworkWait, const Duration(seconds: 10));
        final faults = binding.faults!;
        binding.reportData = {};
        final nativeCatalogTransport = await verifyNativeCatalogTransport(
          configuration,
        );
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        await until(() => cubit.state.status != LibraryStatus.loading);
        expect(cubit.state.status, LibraryStatus.error);
        expect(cubit.state.failure!.kind, FailureKind.timeout);
        final expiredCatalogAttempt = cubit.state.attempt;
        await tester.pump();
        await binding.takeScreenshot('library-catalog-timeout');
        expect(find.text(const LibraryStrings('tr').error), findsOneWidget);
        final catalogRetry = cubit.load();
        await until(() => cubit.state.status != LibraryStatus.loading);
        await catalogRetry;
        expect(cubit.state.status, LibraryStatus.loaded);
        expect(cubit.state.context, configuration.context);
        expect(cubit.state.attempt, greaterThan(expiredCatalogAttempt));
        final recoveredCatalogAttempt = cubit.state.attempt;
        expect(cubit.state.catalog!.rows.map((row) => row.categoryId), [
          3,
          2,
          1,
          4,
        ]);
        await until(() => faults.delayedCatalogCompletions == 1);
        expect(cubit.state.attempt, recoveredCatalogAttempt);
        expect(cubit.state.status, LibraryStatus.loaded);
        await until(
          () =>
              artworks().isNotEmpty &&
              artworks().every(
                (cubit) => cubit.state.status == ArtworkStatus.loaded,
              ),
        );

        // Expire actual artwork transfers while preserving the successful real catalog.
        await libraryGetIt<LibraryImageCache>().emptyCache();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
        faults.artworkDelay = const Duration(milliseconds: 20000);
        // Separate controlled contexts make each fault a genuinely cold transfer.
        final artworkLoad = cubit.load(
          context: const LibraryContext(
            'tr',
            'existing-authorized-fixture:delayed-artwork',
          ),
        );
        await until(() => cubit.state.status == LibraryStatus.loaded);
        await artworkLoad;
        await tester.pump();
        await until(
          () =>
              artworks().isNotEmpty &&
              artworks().every(
                (cubit) => cubit.state.status == ArtworkStatus.loading,
              ),
        );
        final currentArtwork = artworks().first;
        final initialIdentity = currentArtwork.state.identity!;
        final oldImageAttempt = currentArtwork.state.attempt;
        final currentCatalog = cubit.state.catalog!;
        final currentCatalogAttempt = cubit.state.attempt;
        final item = currentCatalog.rows.first.items.first;
        final label =
            '${item.title}, ${item.contentLanguage}${item.isLocked ? ', ${const LibraryStrings('tr').locked}' : ''}';
        final card = find.bySemanticsLabel(label).first;
        final geometry = tester.getRect(card);
        final stopwatch = Stopwatch()..start();
        await until(() => currentArtwork.state.status == ArtworkStatus.error);
        stopwatch.stop();
        // Contract: 4s native header bound precedes the 10s presentation fallback.
        expect(stopwatch.elapsedMilliseconds, greaterThanOrEqualTo(3500));
        expect(stopwatch.elapsedMilliseconds, lessThan(5500));
        expect(cubit.state.status, LibraryStatus.loaded);
        expect(cubit.state.catalog, same(currentCatalog));
        expect(tester.getRect(card), geometry);
        expect(find.text(item.title), findsWidgets);
        expect(currentArtwork.state.identity, initialIdentity);

        await tester.pump();
        await binding.takeScreenshot('library-artwork-timeout');
        faults.artworkDelay = Duration.zero;
        final requestsBeforeTimedRetry =
            faults.requestsByAsset[initialIdentity.assetId]!;
        final lateCompletionsBeforeRetry = faults.delayedArtworkCompletions;
        final retryWindow = Stopwatch()..start();
        final retryButton = find
            .byTooltip(const LibraryStrings('tr').retryArtwork)
            .first;
        await tester.tap(retryButton);
        await tester.pump();
        await until(() => currentArtwork.state.attempt > oldImageAttempt);
        expect(currentArtwork.state.attempt, greaterThan(oldImageAttempt));
        final retryImageAttempt = currentArtwork.state.attempt;
        await until(
          () =>
              faults.requestsByAsset[initialIdentity.assetId]! >
              requestsBeforeTimedRetry,
        );
        retryWindow.stop();
        expect(faults.delayedArtworkCompletions, lateCompletionsBeforeRetry);
        final freshRetryStartedBeforeLateCompletion = true;
        await until(() => currentArtwork.state.status == ArtworkStatus.loaded);
        expect(faults.delayedArtworkCompletions, lateCompletionsBeforeRetry);
        await until(() => faults.delayedArtworkCompletions > 0);
        currentArtwork.fail(initialIdentity, oldImageAttempt);
        currentArtwork.success(initialIdentity, oldImageAttempt);
        expect(currentArtwork.state.status, ArtworkStatus.loaded);
        expect(currentArtwork.state.attempt, retryImageAttempt);
        expect(cubit.state.attempt, currentCatalogAttempt);
        expect(cubit.state.status, LibraryStatus.loaded);
        expect(tester.getRect(card), geometry);
        expect(faults.upstreamFailures, isEmpty);
        await tester.pump();
        for (var frame = 0; frame < 8; frame++) {
          await tester.pump(const Duration(milliseconds: 16));
        }
        await tester.pumpAndSettle();
        await settleRenderedImages();
        await binding.takeScreenshot('library-artwork-retry');

        // Corrupt exactly one declared cover; neighboring cards keep their real JPEGs.
        await libraryGetIt<LibraryImageCache>().emptyCache();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
        final tablet =
            MediaQuery.sizeOf(
              tester.element(find.byType(LibraryView)),
            ).shortestSide >=
            600;
        final corruptCover = tablet
            ? (item.covers.tablet ?? item.covers.phone)!
            : item.covers.phone!;
        faults.corruptAssetId = corruptCover.assetId;
        final corruptLoad = cubit.load(
          context: const LibraryContext(
            'tr',
            'existing-authorized-fixture:corrupt-artwork',
          ),
        );
        await until(() => cubit.state.status == LibraryStatus.loaded);
        await corruptLoad;
        await tester.pump();
        await until(() => artworks().isNotEmpty);
        final corruptArtwork = artworks().first;
        final corruptIdentity = corruptArtwork.state.identity!;
        final corruptAttempt = corruptArtwork.state.attempt;
        final corruptCatalog = cubit.state.catalog!;
        final corruptCatalogAttempt = cubit.state.attempt;
        final corruptGeometry = tester.getRect(card);
        await until(() => corruptArtwork.state.status == ArtworkStatus.error);
        await until(
          () => artworks()
              .skip(1)
              .every((artwork) => artwork.state.status == ArtworkStatus.loaded),
        );
        expect(faults.corruptTransfers, 1);
        expect(
          artworks().where(
            (artwork) => artwork.state.status == ArtworkStatus.error,
          ),
          hasLength(1),
        );
        expect(cubit.state.status, LibraryStatus.loaded);
        expect(cubit.state.catalog, same(corruptCatalog));
        expect(
          cubit.state.catalog!.rows.first.items.first.isLocked,
          item.isLocked,
        );
        expect(tester.getRect(card), corruptGeometry);
        expect(find.text(item.title), findsWidgets);
        await tester.pump();
        await tester.pumpAndSettle();
        await settleRenderedImages();
        await binding.takeScreenshot('library-artwork-corrupt');
        final requestsBeforeCorruptRetry =
            faults.requestsByAsset[corruptCover.assetId]!;
        faults.corruptAssetId = null;
        await tester.tap(
          find.byTooltip(const LibraryStrings('tr').retryArtwork).first,
        );
        await tester.pump();
        await until(() => corruptArtwork.state.attempt > corruptAttempt);
        expect(corruptArtwork.state.attempt, greaterThan(corruptAttempt));
        final recoveredCorruptAttempt = corruptArtwork.state.attempt;
        await until(() => corruptArtwork.state.status != ArtworkStatus.loading);
        expect(corruptArtwork.state.status, ArtworkStatus.loaded);
        expect(
          faults.requestsByAsset[corruptCover.assetId]!,
          greaterThan(requestsBeforeCorruptRetry),
        );
        corruptArtwork.fail(corruptIdentity, corruptAttempt);
        corruptArtwork.success(corruptIdentity, corruptAttempt);
        expect(corruptArtwork.state.status, ArtworkStatus.loaded);
        expect(corruptArtwork.state.attempt, recoveredCorruptAttempt);
        expect(cubit.state.attempt, corruptCatalogAttempt);
        expect(cubit.state.catalog, same(corruptCatalog));
        expect(tester.getRect(card), corruptGeometry);
        expect(faults.upstreamFailures, isEmpty);
        for (var frame = 0; frame < 8; frame++) {
          await tester.pump(const Duration(milliseconds: 16));
        }
        await tester.pumpAndSettle();
        await settleRenderedImages();
        await binding.takeScreenshot('library-artwork-corrupt-retry');
        // An expired cover 403 triggers one background renewal; its real GET 503
        // retains the successful catalog/card error, then explicit retry recovers.
        await libraryGetIt<LibraryImageCache>().emptyCache();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
        faults.expireNextAssetId = corruptCover.assetId;
        final renewalLoad = cubit.load(
          context: const LibraryContext(
            'tr',
            'existing-authorized-fixture:expired-artwork',
          ),
        );
        await until(() => cubit.state.status == LibraryStatus.loaded);
        await renewalLoad;
        final renewalCatalog = cubit.state.catalog!;
        final expiredItem = renewalCatalog.rows.first.items.first;
        final expiredUrl = tablet
            ? expiredItem.covers.tablet!.url
            : expiredItem.covers.phone!.url;

        final renewalAttempt = cubit.state.attempt;
        await tester.pump();
        final renewalGeometry = tester.getRect(card);
        await until(
          () => libraryGetIt<LibraryDiagnostics>().events.any(
            (event) => event['outcome'] == 'artwork-refresh-network',
          ),
        );
        expect(cubit.state.status, LibraryStatus.loaded);
        expect(cubit.state.catalog, same(renewalCatalog));
        expect(cubit.state.attempt, renewalAttempt);
        expect(tester.getRect(card), renewalGeometry);
        expect(find.text(item.title), findsWidgets);
        expect(artworks().first.state.status, ArtworkStatus.error);
        await until(
          () => artworks()
              .skip(1)
              .every((a) => a.state.status == ArtworkStatus.loaded),
        );
        await tester.pumpAndSettle();
        await settleRenderedImages();
        await binding.takeScreenshot('library-refresh-failure-retains-catalog');
        final requestsBeforeExplicitRefresh = faults.catalogRequests;
        await tester.tap(
          find.byTooltip(const LibraryStrings('tr').retryArtwork).first,
        );
        await tester.pump();
        await until(() => cubit.state.attempt > renewalAttempt);
        await until(
          () =>
              artworks().isNotEmpty &&
              artworks().every((a) => a.state.status == ArtworkStatus.loaded),
        );
        expect(faults.catalogRequests, requestsBeforeExplicitRefresh + 1);
        final refreshedItem = cubit.state.catalog!.rows.first.items.first;
        final refreshedCover = tablet
            ? refreshedItem.covers.tablet!
            : refreshedItem.covers.phone!;
        expect(refreshedCover.url, isNot(expiredUrl));
        expect(refreshedCover.expiresAt.isAfter(DateTime.now()), isTrue);
        final probe = HttpClient();
        final rejectedOld = await (await probe.getUrl(
          Uri.parse(expiredUrl),
        )).close();
        expect(rejectedOld.statusCode, 403);
        await rejectedOld.drain<void>();
        probe.close(force: true);

        expect(cubit.state.status, LibraryStatus.loaded);
        expect(tester.getRect(card), renewalGeometry);
        expect(faults.upstreamFailures, isEmpty);
        await tester.pumpAndSettle();
        await settleRenderedImages();
        await binding.takeScreenshot('library-refresh-explicit-retry');
        binding.reportData!.addAll({
          'platform': Platform.operatingSystem,
          'nativeCatalogTransport': nativeCatalogTransport,
          'network':
              'in-process loopback fault proxy to real authenticated backend and Firebase JPEGs',
          'catalogWaitMilliseconds': configuration.catalogWait.inMilliseconds,
          'catalogObservedWaitMilliseconds': libraryGetIt<LibraryDiagnostics>()
              .events
              .firstWhere(
                (event) => event['kind'] == 'catalog',
              )['milliseconds'],
          'catalogNativeHeaderWaitMilliseconds': 6000,
          'catalogNativeBodyIdleWaitMilliseconds': 6000,
          'catalogNativeTotalWaitMilliseconds': 12000,
          'oldCoverUrlPermanentlyRejected': true,
          'freshCoverUrlAndExpiryAdopted': true,
          'selectedCoverVariant': tablet ? 'tablet' : 'phone',
          'artworkWaitMilliseconds': configuration.artworkWait.inMilliseconds,
          'artworkNativeHeaderWaitMilliseconds': 4000,
          'artworkNativeBodyIdleWaitMilliseconds': 4000,
          'artworkNativeTotalWaitMilliseconds': 8000,
          'artworkAdmissionWaitMilliseconds': 2000,
          'observedArtworkErrorCause': 'native-header-deadline',
          'catalogDelayMilliseconds': 16500,
          'artworkDelayMilliseconds': 20000,
          'artworkObservedWaitMilliseconds': stopwatch.elapsedMilliseconds,
          'catalogRequests': faults.catalogRequests,
          'artworkRequests': faults.artworkRequests,
          'lateCatalogCompletions': faults.delayedCatalogCompletions,
          'lateArtworkCompletions': faults.delayedArtworkCompletions,
          'disconnectedConsumers': faults.disconnectedConsumers,
          'corruptCoverAssetId': corruptCover.assetId,
          'corruptTransfers': faults.corruptTransfers,
          'corruptCoverRequestsBeforeRetry': requestsBeforeCorruptRetry,
          'corruptCoverRequestsAfterRetry':
              faults.requestsByAsset[corruptCover.assetId],
          'corruptRetryAttempt': recoveredCorruptAttempt,
          'otherCoversLoaded': artworks()
              .skip(1)
              .every((artwork) => artwork.state.status == ArtworkStatus.loaded),
          'catalogRetryAttempt': recoveredCatalogAttempt,
          'artworkRetryAttempt': retryImageAttempt,
          'timedRetryRequestsBefore': requestsBeforeTimedRetry,
          'timedRetryRequestsAfter':
              faults.requestsByAsset[initialIdentity.assetId],
          'freshRetryStartedBeforeLateCompletion':
              freshRetryStartedBeforeLateCompletion,
          'freshRetryStartMilliseconds': retryWindow.elapsedMilliseconds,
          'signedRefreshFailureRetainsCatalog': true,
          'signedRefreshFailureRetainsGeometry': true,
          'expiredCoverHttpStatus': 403,
          'automaticRenewalHttpStatus': 503,
          'explicitRenewalRetryRecovered': true,
          'orderedRows': currentCatalog.rows
              .map((row) => row.categoryId)
              .toList(),
        });
      } finally {
        await tester.pumpWidget(const SizedBox.shrink());
        await libraryGetIt.reset();
        await binding.faults?.close();
        semanticsHandle.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 4)),
  );
}
