import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_app.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_configuration.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/core/preview/domain/entity/library_preview.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/reading_image_consumer_ownership.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_state.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_widget.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/screen/story_detail_screen.dart';

// Yalnız test binding'in HTTP400 override'ını kaldırır; transport aynıdır.
class _RealHttp extends HttpOverrides {}

// Üretilmiş DI kaydını değiştirmeden normal widget'ın Cubit'ini gözler.
class _Consumers extends BlocObserver {
  final created = <ReadingImageConsumerCubit>[];
  @override
  void onCreate(BlocBase<dynamic> bloc) {
    super.onCreate(bloc);
    if (bloc is ReadingImageConsumerCubit) created.add(bloc);
  }
}

void main() {
  final selected = Platform.environment.containsKey('TELLPAL_PACKAGE3_ORIGIN');
  testWidgets(
    'gerçek Spring → normal LibraryApp detail/sayfa JPEG-WebP ve gecikmeli ayrılma',
    (tester) => HttpOverrides.runWithHttpOverrides(() async {
      final directory = Directory(
        Platform.environment['TELLPAL_PACKAGE3_RUN']!,
      );
      final expected =
          jsonDecode(File('${directory.path}/expected.json').readAsStringSync())
              as Map<String, dynamic>;
      final origin = Uri.parse(
        Platform.environment['TELLPAL_PACKAGE3_ORIGIN']!,
      );
      expect(origin.host, '127.0.0.1');
      final token = Platform.environment['TELLPAL_PACKAGE3_TOKEN']!;
      final previousObserver = Bloc.observer;
      final observer = _Consumers();
      Bloc.observer = observer;
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = const Size(400, 800);
      addTearDown(() async {
        tester.view.resetDevicePixelRatio();
        tester.view.resetPhysicalSize();
        Bloc.observer = previousObserver;
        await libraryGetIt.reset();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
      });
      configureLibraryDependencies(
        LibraryConfiguration(
          baseUrl: origin,
          idToken: token,
          context: const LibraryContext('tr', 'synthetic-consumer'),
        ),
        readingImages: ReadingImageConsumerConfiguration(
          origin: origin,
          accountScope: 'synthetic-consumer',
          token: () => token,
          accountChanges: const Stream<void>.empty(),
          environment: 'test',
          maxEncodedBytes: 1000000,
          maxCacheBytes: 4000000,
          maxSourcePixels: 500000,
          maxDecodePixels: 500000,
          requestWait: const Duration(seconds: 10),
          statusEvery: const Duration(seconds: 20),
          maxLease: const Duration(seconds: 30),
        ),
      );
      final router = libraryGetIt<LibraryRouter>();
      final ownership = libraryGetIt<ReadingImageConsumerOwnership>();
      final renders = <Map<String, dynamic>>[];
      debugPrint('Paket 3: normal LibraryApp kuruluyor');
      await tester.pumpWidget(const LibraryApp());
      debugPrint('Paket 3: normal LibraryApp kuruldu');
      for (final raw in expected['cases'] as List) {
        final item = raw as Map<String, dynamic>;
        final id = item['contentId'] as int;
        for (final page in [0, 1]) {
          debugPrint('Paket 3: ${item['sourceFormat']} page=$page route');
          final before = observer.created.length;
          if (page == 0) {
            unawaited(
              router.replaceAll([
                StoryDetailRoute(
                  categoryId: expected['categoryId'] as int,
                  contentId: id,
                  uiLanguage: 'tr',
                  contentLanguage: 'tr',
                ),
              ]),
            );
          } else {
            unawaited(
              router.replaceAll([
                ReadingImagePageRoute(
                  contentId: id,
                  language: 'tr',
                  pageNumber: 1,
                ),
              ]),
            );
          }
          await _until(tester, () => observer.created.length > before);
          final consumer = observer.created.last;
          await _until(
            tester,
            () => consumer.state.status == ReadingImageConsumerStatus.loaded,
            reason: () => 'consumer=${consumer.state.status}',
          );
          if (page == 0) expect(find.byType(StoryDetailScreen), findsOneWidget);
          expect(find.byType(ReadingImageConsumerWidget), findsOneWidget);
          expect(find.byType(CachedNetworkImage), findsOneWidget);
          expect(ownership.activeOwners, 1);
          debugPrint('Paket 3: gerçek engine decoded');
          final state = consumer.state;
          final selection = state.selection!;
          final wire = (item['outputs'] as List)
              .cast<Map<String, dynamic>>()
              .singleWhere(
                (o) =>
                    o['role'] ==
                    (page == 0 ? 'READING_DETAIL' : 'READING_PAGE'),
              );
          expect(selection.rendition.renditionKey, wire['renditionKey']);
          Uint8List? bytes;
          unawaited(() async {
            await consumer.cache.settleOwnedWork();
            final file = await consumer.cache.getFileFromCache(
              state.immutableKey!,
            );
            expect(file, isNotNull);
            bytes = await file!.file.readAsBytes();
          }());
          await _until(tester, () => bytes != null);
          final payload = bytes!;
          expect(payload.length, wire['byteSize']);
          expect(sha256.convert(payload).toString(), wire['sha256']);
          final actualImages = tester
              .widgetList<RawImage>(find.byType(RawImage))
              .where((image) => image.image != null)
              .toList();
          expect(actualImages, isNotEmpty);
          final decoded = actualImages.last.image!;
          expect(decoded.width, selection.geometry.decodeWidth);
          expect(decoded.height, selection.geometry.decodeHeight);
          debugPrint('Paket 3: cache byte/SHA okundu');
          renders.add({
            'sourceFormat': item['sourceFormat'],
            'role': wire['role'],
            'contentId': id,
            'mime': wire['mime'],
            'sha256': sha256.convert(payload).toString(),
            'byteSize': payload.length,
            'renditionKey': selection.rendition.renditionKey,
            'identity': state.admission!.admission.query,
            'httpAndMagicMimeValidated': true,
            'decodeWidth': decoded.width,
            'decodeHeight': decoded.height,
            'cachedNetworkImage': true,
          });
          unawaited(router.replaceAll([const LibraryRoute()]));
          await _until(
            tester,
            () => ownership.activeOwners == 0 && consumer.isClosed,
          );
          expect(find.byType(ReadingImageConsumerWidget), findsNothing);
          expect(consumer.isClosed, isTrue);
          debugPrint('Paket 3: owner gerçek cleanup sonrası boş');
        }
      }
      // Aynı gerçek transport, yeni route/owner: storage yanıtı kontrollü bekler.
      File('${directory.path}/delay-arm').writeAsStringSync('arm');
      final before = observer.created.length;
      final id = (expected['cases'] as List).last['contentId'] as int;
      unawaited(
        router.replaceAll([
          ReadingImagePageRoute(contentId: id, language: 'tr', pageNumber: 1),
        ]),
      );
      await _until(tester, () => observer.created.length > before);
      final delayed = observer.created.last;
      await _until(
        tester,
        () => File('${directory.path}/delay-started').existsSync(),
      );
      expect(ownership.activeOwners, 1);
      expect(delayed.state.status, isNot(ReadingImageConsumerStatus.loaded));
      unawaited(router.replaceAll([const LibraryRoute()]));
      await _until(
        tester,
        () => find.byType(ReadingImageConsumerWidget).evaluate().isEmpty,
      );
      expect(find.byType(CachedNetworkImage), findsNothing);
      await _until(
        tester,
        () => ownership.activeOwners == 0 && delayed.isClosed,
      );
      final waiting = File('${directory.path}/delay-waiting');
      final serverFinished = File('${directory.path}/delay-finished.json');
      expect(waiting.readAsStringSync(), expected['runId']);
      expect(serverFinished.existsSync(), isFalse);
      File('${directory.path}/client-cleanup.json').writeAsStringSync(
        jsonEncode({
          'runId': expected['runId'],
          'activeOwners': ownership.activeOwners,
          'closed': delayed.isClosed,
          'serverWaitingObserved': true,
        }),
      );
      File(
        '${directory.path}/delay-release',
      ).writeAsStringSync(expected['runId'] as String);
      await _until(tester, serverFinished.existsSync);
      final serverDelay =
          jsonDecode(serverFinished.readAsStringSync()) as Map<String, dynamic>;
      expect(serverDelay['runId'], expected['runId']);
      expect(serverDelay['outcome'], 'RELEASED');
      expect(serverDelay['timedOut'], isFalse);
      expect(serverDelay['clientCleanupObserved'], isTrue);
      expect(waiting.existsSync(), isFalse);
      await tester.pumpWidget(const SizedBox());
      await _pump(tester);
      File('${directory.path}/flutter-result.json').writeAsStringSync(
        const JsonEncoder.withIndent('  ').convert({
          'status': 'PASS',
          'runId': expected['runId'],
          'normalGeneratedDi': true,
          'normalLibraryAppCapability': true,
          'renders': renders,
          'delayedDeparture': {
            'realStorageDelayObserved': true,
            'scope': 'client_transport_cache_provider_cleanup',
            'serverStorageReleasedAfterClientCleanup': true,
            'serverWaitingAtClientCleanup': true,
            'serverDelay': serverDelay,
            'ownerDuringTransfer': 1,
            'paintRemoved': true,
            'released': ownership.activeOwners == 0,
            'closed': delayed.isClosed,
          },
        }),
      );
    }, _RealHttp()),
    skip: !selected,
    timeout: const Timeout(Duration(seconds: 150)),
  );
}

Future<void> _pump(WidgetTester tester) async {
  await tester.runAsync(
    () => Future<void>.delayed(const Duration(milliseconds: 20)),
  );
  await tester.pump(const Duration(milliseconds: 20));
}

Future<void> _until(
  WidgetTester tester,
  bool Function() ready, {
  String Function()? reason,
}) async {
  final watch = Stopwatch()..start();
  while (!ready() && watch.elapsed < const Duration(seconds: 12)) {
    await _pump(tester);
  }
  expect(ready(), isTrue, reason: reason?.call());
}
