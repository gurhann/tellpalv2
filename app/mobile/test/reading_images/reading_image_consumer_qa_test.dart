import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_app.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_qa/consumer_image_slot.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_qa/flow_screen.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_image_viewport.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_detail_artwork_frame.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_page_frame.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/model/reading_image_comparison_model.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/repository/reading_image_comparison_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

import 'comparison_test_support.dart';
import 'fixture.dart';
import 'reading_image_comparison_cubit_test.dart' show ManualComparisonClock;
import 'reading_image_comparison_http_test.dart' show envelope;
import 'reading_image_comparison_loaded_test.dart' show png;
import 'reading_image_selection_test.dart' show requiredSize;

class _LoopbackHttpOverrides extends HttpOverrides {}

Future<T?> _native<T>(WidgetTester tester, Future<T> Function() body) =>
    tester.runAsync(
      () => HttpOverrides.runWithHttpOverrides(body, _LoopbackHttpOverrides()),
    );

class _NativeGate extends ReadingComparisonWorkGate {
  _NativeGate(this.zone);
  final Zone zone;
  @override
  Future<void> run(Future<void> Function() work) =>
      zone.run(() => super.run(work));
}

/// Tüm byte'lar sentetiktir. Sunucu yalnız loopback üzerinde ve test boyunca yaşar.
class _Loopback {
  late HttpServer server;
  late Uint8List output;
  late Map<String, Object?> descriptor;
  final requests = <Uri>[];
  final delivered = <Map<String, String>>[];
  int payloadBytes = 0;
  int manifestStatus = 200;
  bool corrupt = false;
  Completer<void>? holdBytes, bytesEntered;

  Future<void> start() async {
    final source = await png(1200);
    output = await png(1000);
    final f = Fixture(
      source: raster(
        width: 1200,
        height: 1200,
        bytes: source.length,
        sha: sha256.convert(source).toString(),
      ),
      profiles: [profile(width: 1000, height: 1000, allowGrowth: true)],
      outputBytes: {hash('d'): output.length},
    );
    final images = [
      for (final s in f.sources)
        ReadingImageEntry(
          source: s,
          renditions: [
            rendition(
              s,
              f.context.profiles[s.binding.role]!.single,
              bytes: output.length,
              sha: sha256.convert(output).toString(),
            ),
          ],
        ),
    ];
    final manifest = seal(images);
    descriptor = envelope(f);
    (descriptor['identity'] as Map<String, Object?>)['manifestDigest'] =
        manifest.identity.manifestDigest;
    final jsonImages =
        (descriptor['descriptor'] as Map<String, Object?>)['images'] as List;
    for (final image in jsonImages) {
      final rendition = (image['renditions'] as List).single;
      rendition['raster']['sha256'] = sha256.convert(output).toString();
    }
    final decoded = ReadingImageComparisonModel.fromJson(
      jsonDecode(jsonEncode(descriptor)) as Map<String, dynamic>,
    );
    expect(
      const ValidateReadingImageManifest()(
        manifest: decoded.manifest,
        expected: decoded.context,
      ).isRight(),
      isTrue,
      reason: 'Sentetik HTTP fixture geçerli olmalı',
    );
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((request) async {
      try {
        requests.add(request.uri);
        expect(request.headers.value('cache-control'), 'no-store');
        expect(request.headers.value('accept-encoding'), 'identity');
        expect(
          request.headers.value('authorization'),
          'Bearer synthetic-local-http-token',
        );
        request.response.headers.set('Cache-Control', 'private,no-store');
        final bytesPath = request.uri.path.endsWith('/bytes');
        if (bytesPath) {
          delivered.add(request.uri.queryParameters);
          bytesEntered?.complete();
          await holdBytes?.future;
          request.response.headers.contentType = ContentType('image', 'png');
          final data = corrupt ? Uint8List(output.length) : output;
          request.response.contentLength = data.length;
          request.response.add(data);
          payloadBytes += data.length;
        } else {
          request.response.statusCode = manifestStatus;
          request.response.headers.contentType = ContentType.json;
          final data = utf8.encode(jsonEncode(descriptor));
          request.response.contentLength = data.length;
          request.response.add(data);
        }
        await request.response.close();
      } on HttpException {
        // İptal edilen test isteğinin soketi kapanmış olabilir.
      } on SocketException {
        // İptal edilen test isteğinin soketi kapanmış olabilir.
      }
    });
  }

  Uri get origin => Uri.parse('http://127.0.0.1:${server.port}');
  Future<void> close() async {
    if (holdBytes != null && !holdBytes!.isCompleted) holdBytes!.complete();
    await server.close(force: true);
  }
}

Future<void> _wait(WidgetTester tester, bool Function() done) async {
  for (var count = 0; count < 120 && !done(); count++) {
    await _native(
      tester,
      () => Future<void>.delayed(const Duration(milliseconds: 10)),
    );
    await tester.pump(const Duration(milliseconds: 20));
  }
  expect(done(), isTrue, reason: 'Sınırlı yerel bekleme tamamlanmalı');
  await tester.pump(const Duration(milliseconds: 350));
}

void _select(
  ReadingImageComparisonCubit cubit, {
  ReadingImageRole role = ReadingImageRole.readingDetail,
  int? pageId,
  int pageNumber = 0,
  double size = 100,
}) => cubit.select(
  id: 71,
  entryIndex: 0,
  canonicalSource: false,
  width: size,
  height: size,
  dpr: 1,
  fit: ReadingImageFit.contain,
  consumerRole: role,
  pageId: pageId,
  pageNumber: pageNumber,
);

void main() {
  tearDown(() {
    expect(ReadingComparisonRaster.activeImages, 0);
    expect(ReadingComparisonRaster.activeBuffers, 0);
    expect(ReadingComparisonRaster.activeDescriptors, 0);
    expect(ReadingComparisonRaster.activeCodecs, 0);
  });

  testWidgets(
    'gerçek HTTP/codec: detail → iki sayfa → geri → bounded close aynı anchor',
    (tester) async {
      tester.view.physicalSize = const Size(487.5, 1055);
      tester.view.devicePixelRatio = 1.25;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final cacheBefore = PaintingBinding.instance.imageCache.currentSize;
      var audioCalls = 0, diskCalls = 0, factories = 0;
      const audio = MethodChannel('com.ryanheise.just_audio.methods');
      const disk = MethodChannel('plugins.flutter.io/path_provider');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      messenger.setMockMethodCallHandler(audio, (_) async {
        audioCalls++;
        return null;
      });
      messenger.setMockMethodCallHandler(disk, (_) async {
        diskCalls++;
        return null;
      });
      addTearDown(() {
        messenger.setMockMethodCallHandler(audio, null);
        messenger.setMockMethodCallHandler(disk, null);
      });
      final server = _Loopback();
      late ReadingImageComparisonCubit cubit;
      late ReadingImageComparisonConfiguration configuration;
      await _native(tester, () async {
        await server.start();
        configuration = media(server.origin);
        final repository = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repository),
          repository,
          configuration,
          _NativeGate(Zone.current),
          ManualComparisonClock(),
        );
        comparisonGetIt.registerFactory<ReadingImageComparisonCubit>(() {
          factories++;
          return cubit;
        });
      });
      final boundary = GlobalKey();
      final traces = <String>[];
      try {
        await tester.pumpWidget(
          RepaintBoundary(
            key: boundary,
            child: const ReadingImageComparisonApp(
              contentId: 71,
              consumerQa: true,
            ),
          ),
        );
        Future<void> painted(int index) async {
          await _wait(
            tester,
            () =>
                cubit.state.status == 'loaded' &&
                cubit.state.entryIndex == index &&
                find.byType(ReadingImageComparisonRaster).evaluate().length ==
                    1,
          );
          final image = tester.widget<ReadingImageComparisonRaster>(
            find.byType(ReadingImageComparisonRaster),
          );
          final slot = tester.getRect(
            find.ancestor(
              of: find.byType(ReadingImageComparisonRaster),
              matching: find.byType(ReadingImageViewport),
            ),
          );
          expect(
            tester.getRect(find.byType(ReadingImageComparisonRaster)),
            slot,
          );
          final fit = index == 1
              ? ReadingImageFit.cover
              : ReadingImageFit.contain;
          expect(image.fit, index == 1 ? BoxFit.cover : BoxFit.contain);
          final expected = requiredSize(
            1200,
            1200,
            (slot.width * 1.25).ceil(),
            (slot.height * 1.25).ceil(),
            fit,
          );
          expect((
            cubit.state.geometry!.requiredWidth,
            cubit.state.geometry!.requiredHeight,
          ), expected);
          expect(
            (image.raster.image!.width, image.raster.image!.height),
            (
              cubit.state.geometry!.decodeWidth,
              cubit.state.geometry!.decodeHeight,
            ),
          );
          expect(
            image.raster.image!.debugGetOpenHandleStackTraces()!.length,
            1,
          );
          expect(ReadingComparisonRaster.activeImages, 1);
          final shot = await _native(
            tester,
            () =>
                (boundary.currentContext!.findRenderObject()!
                        as RenderRepaintBoundary)
                    .toImage(),
          );
          final pixels = await _native(tester, () => shot!.toByteData());
          final offset =
              (slot.center.dy.floor() * shot!.width + slot.center.dx.floor()) *
              4;
          expect(pixels!.buffer.asUint8List().sublist(offset, offset + 4), [
            255,
            255,
            255,
            255,
          ]);
          shot.dispose();
          final request = server.delivered.last;
          expect(request['canonicalSource'], 'false');
          expect(
            request['role'],
            index == 1 ? 'READING_DETAIL' : 'READING_PAGE',
          );
          expect(request['pageNumber'], '${index == 1 ? 0 : index - 1}');
          expect(
            request['manifestDigest'],
            (server.descriptor['identity'] as Map)['manifestDigest'],
          );
          expect(
            request['renditionKey'],
            cubit
                .state
                .comparison!
                .manifest
                .images[index]
                .renditions
                .single
                .renditionKey,
          );
          traces.add(
            '${request['role']}/${request['pageNumber']}: ${slot.size}, DPR 1.25, ${image.fit}, ${request['renditionKey']}, ${server.output.length} byte',
          );
        }

        await painted(1);
        final anchor = cubit.state.comparison!.manifest.identity;
        final first = cubit.state.raster!.image!;
        await tester.tap(find.text('QA sayfalarını aç'));
        await painted(2);
        expect(first.debugDisposed, isTrue);
        expect(find.text('Bu QA denemesinde ses kapalı'), findsOneWidget);
        await tester.tap(find.text('Sonraki'));
        await painted(3);
        expect(find.text('Sayfa 2/2'), findsOneWidget);
        await tester.tap(find.byTooltip('QA detail ekranına dön'));
        await painted(1);
        expect(cubit.state.comparison!.manifest.identity, anchor);
        expect(factories, 1);
        expect(
          server.requests.where((u) => u.path.endsWith('/manifest')).length,
          1,
        );
        expect(server.delivered.length, 4);
        expect(server.payloadBytes, server.output.length * 4);
        expect(
          server.requests.every(
            (u) => u.path.startsWith('/api/test/reading-image-comparison/'),
          ),
          isTrue,
        );
        expect(audioCalls, 0);
        expect(diskCalls, 0);
        expect(PaintingBinding.instance.imageCache.currentSize, cacheBefore);
        expect(find.byType(Image), findsNothing);
        expect(find.byType(RawImage), findsNothing);
        await tester.tap(find.byTooltip('QA akışını kapat'));
        await _wait(tester, () => cubit.isClosed);
        expect(find.text('QA akışı kapandı'), findsOneWidget);
        for (final trace in traces) {
          debugPrint('P4a4 sentetik paint izi: $trace');
        }
        expect(tester.takeException(), isNull);
      } finally {
        await _native(tester, () async {
          if (!cubit.isClosed) {
            await cubit.close().timeout(const Duration(seconds: 3));
          }
          await configuration.account!.credentials.close();
          await server.close();
          await comparisonGetIt.reset();
        });
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'gerçek QA parent binding paused/resumed olayında rasterı bırakır; açık readmission bekler',
    (tester) async {
      tester.view.physicalSize = const Size(487.5, 1055);
      tester.view.devicePixelRatio = 1.25;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      final server = _Loopback();
      late ManualComparisonClock clock;
      late ReadingImageComparisonConfiguration configuration;
      late ReadingImageComparisonCubit cubit;
      await _native(tester, () async {
        await server.start();
        clock = ManualComparisonClock();
        configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          _NativeGate(Zone.current),
          clock,
        );
        comparisonGetIt.registerFactory<ReadingImageComparisonCubit>(
          () => cubit,
        );
      });
      try {
        await tester.pumpWidget(
          const ReadingImageComparisonApp(contentId: 71, consumerQa: true),
        );
        await _wait(
          tester,
          () => ['loaded', 'error'].contains(cubit.state.status),
        );
        expect(cubit.state.status, 'loaded', reason: cubit.state.error);
        expect(find.byType(ReadingImageConsumerFlowScreen), findsOneWidget);
        expect(find.byType(ReadingImageComparisonRaster), findsOneWidget);
        final firstImage = cubit.state.raster!.image!;
        final anchor = cubit.state.comparison!.manifest.identity;
        final requestCount = server.requests.length;
        final payloadBytes = server.payloadBytes;

        tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
        expect(cubit.state.error, 'background');
        expect(firstImage.debugDisposed, isTrue);
        expect(cubit.state.raster, isNull);
        expect(ReadingComparisonRaster.activeImages, 0);
        tester.binding.scheduleForcedFrame();
        await tester.pump();
        expect(find.byType(ReadingImageComparisonRaster), findsNothing);
        clock.advance(1000);
        await _native(
          tester,
          () => Future<void>.delayed(const Duration(milliseconds: 30)),
        );
        expect(server.requests.length, requestCount);
        expect(server.payloadBytes, payloadBytes);

        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
        await tester.pump();
        expect(cubit.state.error, 'readmission_required');
        expect(find.byType(ReadingImageComparisonRaster), findsNothing);
        expect(find.text('Yeniden admission'), findsOneWidget);
        expect(
          tester
              .widget<FilledButton>(
                find.widgetWithText(FilledButton, 'QA sayfalarını aç'),
              )
              .onPressed,
          isNull,
        );
        clock.advance(1000);
        await _native(
          tester,
          () => Future<void>.delayed(const Duration(milliseconds: 30)),
        );
        await tester.pump();
        expect(
          server.requests.length,
          requestCount,
          reason: 'Resume sessiz admission açmamalı',
        );
        expect(server.payloadBytes, payloadBytes);

        await tester.tap(find.text('Yeniden admission'));
        await _wait(tester, () => cubit.state.status == 'loaded');
        expect(cubit.state.comparison!.manifest.identity, anchor);
        expect(find.byType(ReadingImageComparisonRaster), findsOneWidget);
        expect(identical(cubit.state.raster!.image, firstImage), isFalse);
        expect(
          server.requests.where((u) => u.path.endsWith('/manifest')).length,
          2,
        );
        expect(server.delivered.length, 2);
        expect(tester.takeException(), isNull);
      } finally {
        await _native(tester, () async {
          await cubit.close().timeout(const Duration(seconds: 3));
          await configuration.account!.credentials.close();
          await server.close();
          await comparisonGetIt.reset();
        });
        await tester.pumpWidget(const SizedBox());
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
      }
    },
  );

  testWidgets(
    'gerçek QA parent UI close native gate release bitmeden kapanışı tamamlamaz',
    (tester) async {
      tester.view.physicalSize = const Size(487.5, 1055);
      tester.view.devicePixelRatio = 1.25;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final server = _Loopback();
      late Completer<void> entered;
      late Completer<void> release;
      late Future<void> pendingNative;
      late ReadingComparisonRaster heldRaster;
      late ReadingImageComparisonConfiguration configuration;
      late ReadingImageComparisonCubit cubit;
      var closeRequested = false;
      await _native(tester, () async {
        entered = Completer<void>();
        release = Completer<void>();
        await server.start();
        configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        final gate = _NativeGate(Zone.current);
        pendingNative = gate.run(() async {
          heldRaster = await ReadingComparisonRaster.decode(
            server.output,
            raster(width: 1000, height: 1000),
            1,
            1,
            configuration.limits!,
            ReadingComparisonCancellation(),
          );
          entered.complete();
          try {
            await release.future;
          } finally {
            heldRaster.dispose();
          }
        });
        await entered.future.timeout(const Duration(seconds: 3));
        cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          gate,
          ManualComparisonClock(),
        );
        comparisonGetIt.registerFactory<ReadingImageComparisonCubit>(
          () => cubit,
        );
      });
      try {
        await tester.pumpWidget(
          const ReadingImageComparisonApp(contentId: 71, consumerQa: true),
        );
        await _wait(tester, () => cubit.state.status == 'loading');
        final nativeImage = heldRaster.image!;
        final parentPopScope = find
            .descendant(
              of: find.byType(ReadingImageConsumerFlowScope),
              matching: find.byWidgetPredicate((widget) => widget is PopScope),
            )
            .first;
        expect(ReadingComparisonRaster.activeImages, 1);
        expect(server.requests, isEmpty);
        expect(find.byType(ReadingImageConsumerFlowScreen), findsOneWidget);
        expect(tester.widget<PopScope>(parentPopScope).canPop, isFalse);

        closeRequested = true;
        await tester.tap(find.byTooltip('QA akışını kapat'));
        await tester.pump(const Duration(milliseconds: 350));
        await _native(
          tester,
          () => Future<void>.delayed(const Duration(milliseconds: 30)),
        );
        await tester.pump();
        expect(cubit.state.error, 'closed');
        expect(
          cubit.isClosed,
          isFalse,
          reason: 'Cubit native kuyruğu gerçekten drain etmelidir',
        );
        expect(nativeImage.debugDisposed, isFalse);
        expect(
          find.text('QA akışı kapandı'),
          findsNothing,
          reason: 'Parent _drain await kaldırılırsa bu kontrol bozulmalıdır',
        );
        expect(find.byType(ReadingImageConsumerFlowScreen), findsOneWidget);
        expect(find.byType(ReadingConsumerImageSlot), findsOneWidget);
        expect(tester.widget<PopScope>(parentPopScope).canPop, isFalse);
        expect(server.requests, isEmpty);

        release.complete();
        await _native(
          tester,
          () => pendingNative.timeout(const Duration(seconds: 3)),
        );
        await _wait(tester, () => cubit.isClosed);
        expect(nativeImage.debugDisposed, isTrue);
        expect(ReadingComparisonRaster.activeImages, 0);
        expect(find.text('QA akışı kapandı'), findsOneWidget);
        expect(find.byType(ReadingConsumerImageSlot), findsNothing);
        expect(tester.widget<PopScope>(parentPopScope).canPop, isTrue);
        expect(server.requests, isEmpty);
        expect(tester.takeException(), isNull);
      } finally {
        if (!release.isCompleted) release.complete();
        await _native(
          tester,
          () => pendingNative.timeout(const Duration(seconds: 3)),
        );
        if (closeRequested) {
          await _wait(tester, () => cubit.isClosed);
        }
        await _native(tester, () async {
          if (!cubit.isClosed) {
            await cubit.close().timeout(const Duration(seconds: 3));
          }
          await configuration.account!.credentials.close();
          await server.close();
          await comparisonGetIt.reset();
        });
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets('kapalı consumer QA girişi I/O açmaz', (tester) async {
    final configuration = const ReadingImageComparisonConfiguration.geometry();
    final repository = ReadingImageComparisonRepositoryImpl(
      ReadingImageComparisonDataSource(configuration),
      configuration,
    );
    final cubit = ReadingImageComparisonCubit(
      LoadReadingImageComparison(repository),
      repository,
      configuration,
      ReadingComparisonWorkGate(),
      ManualComparisonClock(),
    );
    comparisonGetIt.registerFactory<ReadingImageComparisonCubit>(() => cubit);
    await tester.pumpWidget(
      const ReadingImageComparisonApp(contentId: 71, consumerQa: true),
    );
    await tester.pumpAndSettle();
    expect(find.text('QA girişi kapalı'), findsOneWidget);
    expect(find.byType(ReadingConsumerImageSlot), findsNothing);
    expect(cubit.state.status, 'initial');
    await tester.pumpWidget(const SizedBox());
    await cubit.close();
    await comparisonGetIt.reset();
  });

  for (final fault in [
    'unauthorized',
    'language',
    'role',
    'order',
    'revision',
    'digest',
    'profile',
    'generation',
    'missing',
    'corrupt',
  ]) {
    testWidgets(
      'HTTP consumer ret matrisi: $fault; original/provider fallback yok',
      (tester) async {
        await _native(tester, () async {
          final server = _Loopback();
          await server.start();
          final identity = server.descriptor['identity'] as Map;
          final images =
              (server.descriptor['descriptor'] as Map)['images'] as List;
          switch (fault) {
            case 'unauthorized':
              server.manifestStatus = 403;
            case 'language':
              identity['language'] = 'en';
            case 'role':
              images[1]['source']['binding']['role'] = 'READING_COVER';
            case 'order':
              images[2]['source']['binding']['pageNumber'] = 2;
            case 'revision':
              identity['revision'] = 'invalid';
            case 'digest':
              identity['manifestDigest'] = hash('0');
            case 'profile':
              images[1]['renditions'][0]['profileFingerprint'] = hash('0');
            case 'generation':
              identity['liveGeneration'] = 0;
            case 'missing':
              images[1]['renditions'] = [];
            case 'corrupt':
              server.corrupt = true;
          }
          final configuration = media(server.origin);
          final repo = ReadingImageComparisonRepositoryImpl(
            ReadingImageComparisonDataSource(configuration),
            configuration,
          );
          final cubit = ReadingImageComparisonCubit(
            LoadReadingImageComparison(repo),
            repo,
            configuration,
            ReadingComparisonWorkGate(),
            ManualComparisonClock(),
          );
          try {
            final stopped = cubit.stream.firstWhere((s) => s.status == 'error');
            _select(cubit);
            await stopped.timeout(const Duration(seconds: 3));
            expect(cubit.state.raster, isNull);
            expect(server.delivered.length, fault == 'corrupt' ? 1 : 0);
            expect(
              server.delivered.every((q) => q['canonicalSource'] == 'false'),
              isTrue,
            );
          } finally {
            await cubit.close();
            await configuration.account!.credentials.close();
            await server.close();
          }
        });
      },
    );
  }

  testWidgets(
    'consumer tam pageId/sıra eşleşmeden byte istemez; shortage tipli kalır',
    (tester) async {
      await _native(tester, () async {
        final server = _Loopback();
        await server.start();
        final configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          ReadingComparisonWorkGate(),
          ManualComparisonClock(),
        );
        try {
          var stopped = cubit.stream.firstWhere((s) => s.status == 'error');
          _select(
            cubit,
            role: ReadingImageRole.readingPage,
            pageId: 11,
            pageNumber: 2,
          );
          await stopped.timeout(const Duration(seconds: 3));
          expect(server.delivered, isEmpty);
          cubit.retry();
          stopped = cubit.stream.firstWhere((s) => s.status == 'error');
          _select(cubit, size: 1100);
          await stopped.timeout(const Duration(seconds: 3));
          expect(cubit.state.error, 'renditionShortage');
          expect(server.delivered, isEmpty);
        } finally {
          await cubit.close();
          await configuration.account!.credentials.close();
          await server.close();
        }
      });
    },
  );

  for (final event in ['account', 'background', 'lease', 'status', 'live']) {
    testWidgets(
      'consumer $event temizliği ve açık yeniden admission anchor koruması',
      (tester) async {
        await _native(tester, () async {
          final server = _Loopback();
          await server.start();
          server.descriptor['leaseMillis'] = 2000;
          final configuration = media(server.origin);
          final repo = ReadingImageComparisonRepositoryImpl(
            ReadingImageComparisonDataSource(configuration),
            configuration,
          );
          final clock = ManualComparisonClock();
          final cubit = ReadingImageComparisonCubit(
            LoadReadingImageComparison(repo),
            repo,
            configuration,
            ReadingComparisonWorkGate(),
            clock,
          );
          try {
            final loaded = cubit.stream.firstWhere((s) => s.status == 'loaded');
            _select(cubit);
            await loaded.timeout(const Duration(seconds: 3));
            final image = cubit.state.raster!.image!;
            switch (event) {
              case 'account':
                configuration.account!.credentials.revoke();
              case 'background':
                cubit.visibility(false);
                cubit.visibility(true);
              case 'lease':
                clock.advance(2000);
              case 'status':
                server.manifestStatus = 403;
                clock.advance(1000);
              case 'live':
                (server.descriptor['identity'] as Map)['liveGeneration'] = 2;
                clock.advance(1000);
            }
            for (
              var wait = 0;
              wait < 60 && cubit.state.status != 'error';
              wait++
            ) {
              await Future<void>.delayed(const Duration(milliseconds: 10));
            }
            expect(cubit.state.status, 'error');
            expect(
              cubit.state.error,
              {
                'account': 'account_changed',
                'background': 'readmission_required',
                'lease': 'lease_expired',
                'status': 'status_refused',
                'live': 'live_changed',
              }[event],
            );
            expect(image.debugDisposed, isTrue);
            expect(cubit.state.raster, isNull);
            final bytes = server.delivered.length;
            await Future<void>.delayed(const Duration(milliseconds: 20));
            expect(
              server.delivered.length,
              bytes,
              reason: 'Sessiz tekrar admission olmaz',
            );
            if (event == 'background' || event == 'lease') {
              // İlk LIVE sonraki açık girişte de aynı kalmak zorundadır.
              (server.descriptor['identity'] as Map)['liveGeneration'] = 2;
              cubit.retry();
              final refused = cubit.stream.firstWhere(
                (s) => s.status == 'error',
              );
              _select(cubit);
              await refused.timeout(const Duration(seconds: 3));
              expect(server.delivered.length, bytes);
              expect(cubit.state.raster, isNull);
            }
          } finally {
            await cubit.close().timeout(const Duration(seconds: 3));
            await configuration.account!.credentials.close();
            await server.close();
          }
        });
      },
    );
  }

  testWidgets(
    'gerçek ortak frame ölçümü: tall/wide, kesirli DPR, resize, text-scale ve shortage',
    (tester) async {
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final server = _Loopback();
      late ReadingImageComparisonConfiguration configuration;
      late ReadingImageComparisonCubit cubit;
      await _native(tester, () async {
        await server.start();
        configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          _NativeGate(Zone.current),
          ManualComparisonClock(),
        );
      });
      try {
        for (final sample in [
          (size: const Size(390, 844), dpr: 1.25, text: 1.0),
          (size: const Size(844, 390), dpr: 1.0, text: 1.5),
          (size: const Size(600, 900), dpr: 1.25, text: 1.3),
          (size: const Size(390, 844), dpr: 2.625, text: 1.0),
        ]) {
          tester.view.physicalSize = sample.size * sample.dpr;
          tester.view.devicePixelRatio = sample.dpr;
          for (final detail in [true, false]) {
            cubit.changeChoice(0, false);
            ReadingImageViewportGeometry? observed;
            Widget image(
              BuildContext context,
              ReadingImageViewportGeometry geometry,
            ) {
              observed = geometry;
              return ReadingConsumerImageSlot(
                contentId: 71,
                role: detail
                    ? ReadingImageRole.readingDetail
                    : ReadingImageRole.readingPage,
                pageId: detail ? null : 11,
                pageNumber: detail ? 0 : 1,
                geometry: geometry,
              );
            }

            final bytesBefore = server.delivered.length;
            await tester.pumpWidget(
              MaterialApp(
                builder: (context, child) => MediaQuery(
                  data: MediaQuery.of(
                    context,
                  ).copyWith(textScaler: TextScaler.linear(sample.text)),
                  child: child!,
                ),
                home: BlocProvider.value(
                  value: cubit,
                  child: Scaffold(
                    body: detail
                        ? SingleChildScrollView(
                            child: ReadingDetailArtworkFrame(
                              semanticLabel: 'QA geometri',
                              artworkBuilder: image,
                            ),
                          )
                        : ReadingPageFrame(
                            header: const Text('Sayfa 1/2'),
                            imageBuilder: image,
                            audio: const Text('Bu QA denemesinde ses kapalı'),
                            controls: const SizedBox(height: 48),
                          ),
                  ),
                ),
              ),
            );
            await _wait(
              tester,
              () => ['loaded', 'error'].contains(cubit.state.status),
            );
            final measured = observed!;
            final rect = tester.getRect(find.byType(ReadingImageViewport));
            expect(measured.slot, rect.size);
            expect(measured.dpr, sample.dpr);
            expect(measured.fit, detail ? BoxFit.cover : BoxFit.contain);
            final required = requiredSize(
              1200,
              1200,
              (rect.width * sample.dpr).ceil(),
              (rect.height * sample.dpr).ceil(),
              detail ? ReadingImageFit.cover : ReadingImageFit.contain,
            );
            if (required.$1 > 1000 || required.$2 > 1000) {
              expect(
                cubit.state.error,
                required.$1 > 1200 || required.$2 > 1200
                    ? 'sourceShortage'
                    : 'renditionShortage',
              );
              expect(server.delivered.length, bytesBefore);
              expect(find.byType(ReadingImageComparisonRaster), findsNothing);
              cubit.retry();
            } else {
              expect(cubit.state.status, 'loaded');
              expect((
                cubit.state.geometry!.requiredWidth,
                cubit.state.geometry!.requiredHeight,
              ), required);
              expect(
                tester
                    .widget<ReadingImageComparisonRaster>(
                      find.byType(ReadingImageComparisonRaster),
                    )
                    .fit,
                measured.fit,
              );
            }
            expect(tester.takeException(), isNull);
          }
        }
      } finally {
        await _native(tester, () async {
          await cubit.close().timeout(const Duration(seconds: 3));
          await configuration.account!.credentials.close();
          await server.close();
        });
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'gerçek bekleyen HTTP kapanışı bounded drain ile raster üretmeden biter',
    (tester) async {
      await _native(tester, () async {
        final server = _Loopback();
        await server.start();
        server.holdBytes = Completer<void>();
        server.bytesEntered = Completer<void>();
        final configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          ReadingComparisonWorkGate(),
          ManualComparisonClock(),
        );
        try {
          _select(cubit);
          await server.bytesEntered!.future.timeout(const Duration(seconds: 3));
          await cubit.close().timeout(const Duration(seconds: 3));
          expect(cubit.isClosed, isTrue);
          expect(cubit.state.raster, isNull);
          expect(server.payloadBytes, 0);
        } finally {
          if (!cubit.isClosed) await cubit.close();
          await configuration.account!.credentials.close();
          await server.close();
        }
      });
    },
  );

  testWidgets(
    'eski native Future kapanmadan yeni HTTP/decode yok; geç epoch boyanmaz',
    (tester) async {
      await _native(tester, () async {
        final server = _Loopback();
        await server.start();
        final configuration = media(server.origin);
        final repo = ReadingImageComparisonRepositoryImpl(
          ReadingImageComparisonDataSource(configuration),
          configuration,
        );
        final gate = ReadingComparisonWorkGate();
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          configuration,
          gate,
          ManualComparisonClock(),
        );
        final release = Completer<void>();
        final entered = Completer<void>();
        final old = gate.run(() async {
          final native = await ReadingComparisonRaster.decode(
            server.output,
            raster(width: 1000, height: 1000),
            1,
            1,
            configuration.limits!,
            ReadingComparisonCancellation(),
          );
          entered.complete();
          try {
            await release.future;
          } finally {
            native.dispose();
          }
        });
        try {
          await entered.future;
          _select(cubit);
          cubit.changeChoice(0, false);
          _select(
            cubit,
            role: ReadingImageRole.readingPage,
            pageId: 12,
            pageNumber: 2,
          );
          await Future<void>.delayed(const Duration(milliseconds: 20));
          expect(server.requests, isEmpty);
          expect(ReadingComparisonRaster.activeImages, 1);
          final loaded = cubit.stream.firstWhere((s) => s.status == 'loaded');
          release.complete();
          await old;
          await loaded.timeout(const Duration(seconds: 3));
          expect(server.delivered.length, 1);
          expect(server.delivered.single['pageNumber'], '2');
          expect(cubit.state.entryIndex, 3);
          expect(ReadingComparisonRaster.activeImages, 1);
        } finally {
          if (!release.isCompleted) release.complete();
          await old;
          await cubit.close().timeout(const Duration(seconds: 3));
          await configuration.account!.credentials.close();
          await server.close();
        }
      });
    },
  );
}
