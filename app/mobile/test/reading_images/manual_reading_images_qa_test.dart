import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa_assets.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa_cubit.dart';
import 'package:tellpal_mobile_poc/app/manual_reading_images_qa.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'comparison_test_support.dart';
import 'fixture.dart';

class _Assets extends LocalLosslessQaAssets {
  int initializes = 0;
  final indices = <int>[];
  Completer<void>? gate;
  bool fail = false;

  @override
  Future<void> initialize() async {
    initializes++;
  }

  @override
  Future<LocalLosslessQaDecoded> load(
    int index,
    ReadingComparisonCancellation cancellation,
  ) async {
    indices.add(index);
    await gate?.future;
    if (fail) throw const FormatException('synthetic_corruption');
    // Küçük raster yalnız widget sahipliği ve iptal testi içindir.
    final recorder = ui.PictureRecorder();
    ui.Canvas(recorder).drawColor(const Color(0xFFFFFFFF), ui.BlendMode.src);
    final picture = recorder.endRecording();
    final image = await picture.toImage(2, 2);
    final data = (await image.toByteData(format: ui.ImageByteFormat.png))!;
    final bytes = data.buffer.asUint8List(
      data.offsetInBytes,
      data.lengthInBytes,
    );
    image.dispose();
    picture.dispose();
    final owned = await ReadingComparisonRaster.decode(
      bytes,
      raster(width: 2, height: 2, bytes: bytes.length),
      2,
      2,
      limits(pixels: 4),
      cancellation,
    );
    return LocalLosslessQaDecoded(owned, {'caseIndex': index});
  }
}

Future<void> _open(WidgetTester tester, LocalLosslessQaCubit cubit) async {
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox());
    await tester.pump();
  });
  tester.view.physicalSize = const Size(822, 1828);
  tester.view.devicePixelRatio = 2;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
  await tester.pumpWidget(ManualReadingImagesQaApp(cubit: cubit));
  await tester.pumpAndSettle();
}

Future<void> _next(WidgetTester tester, LocalLosslessQaCubit cubit) async {
  await tester.runAsync(() async {
    await tester.tap(find.byKey(const ValueKey('manual-image-next')));
    await Future<void>.delayed(const Duration(milliseconds: 20));
  });
  for (var frame = 0; frame < 100; frame++) {
    await tester.runAsync(() async {
      await Future<void>.delayed(const Duration(milliseconds: 5));
    });
    await tester.pump();
    if (cubit.state.error != null ||
        cubit.state.status == 'stopped' ||
        (cubit.state.status == 'loaded' &&
            cubit.snapshot()['paintedEpoch'] == cubit.state.epoch)) {
      return;
    }
  }
  throw StateError('synthetic_frame_deadline');
}

void main() {
  tearDown(() {
    expect(ReadingComparisonRaster.activeBuffers, 0);
    expect(ReadingComparisonRaster.activeDescriptors, 0);
    expect(ReadingComparisonRaster.activeCodecs, 0);
    expect(ReadingComparisonRaster.activeImages, 0);
  });

  test('manuel giriş açık native debug seçimi gerektirir', () {
    for (final enabled in [false, true]) {
      for (final debug in [false, true]) {
        for (final web in [false, true]) {
          expect(
            manualReadingImagesAllowed(
              enabled: enabled,
              debug: debug,
              web: web,
            ),
            enabled && debug && !web,
          );
        }
      }
    }
  });

  testWidgets(
    'Başlat bekler; altı dosya sıralı boyanır ve Bitir kaynağı bırakır',
    (tester) async {
      final assets = _Assets();
      final active = LocalLosslessQaCubit(assets: assets);
      await _open(tester, active);
      expect(assets.initializes, 0);
      expect(assets.indices, isEmpty);
      expect(find.text('Başlat'), findsOneWidget);
      for (var index = 0; index < 6; index++) {
        await _next(tester, active);
        expect(
          active.snapshot()['completedCases'],
          index + 1,
          reason: active.snapshot().toString(),
        );
        final cases = active.snapshot()['cases'] as List;
        final geometry = (cases.last as Map)['widget'] as Map;
        expect(geometry['physicalWidth'], 822);
        expect(geometry['physicalHeight'], 1334);
        expect(geometry['pixelAligned'], true);
        expect(ReadingComparisonRaster.activeImages, 1);
      }
      expect(assets.indices, [0, 1, 2, 3, 4, 5]);
      expect(assets.initializes, 1);
      await _next(tester, active);
      expect(active.state.status, 'stopped');
      expect(active.snapshot()['allCasesPassed'], true);
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets('hızlı dokunma ikinci işi açmaz; arka plan geç işi iptal eder', (
    tester,
  ) async {
    final assets = _Assets()..gate = Completer<void>();
    final cubit = LocalLosslessQaCubit(assets: assets);
    await _open(tester, cubit);
    await tester.tap(find.byKey(const ValueKey('manual-image-next')));
    await tester.tap(find.byKey(const ValueKey('manual-image-next')));
    await tester.pump();
    expect(assets.indices, [0]);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    await tester.pump();
    expect(cubit.state.error, 'background');
    await tester.runAsync(() async {
      assets.gate!.complete();
      await Future<void>.delayed(const Duration(milliseconds: 30));
    });
    expect(cubit.state.raster, isNull);
    expect(cubit.snapshot()['completedCases'], 0);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets(
    'alan Başlat öncesi yerleşir; başladıktan sonra değişirse durur',
    (tester) async {
      final assets = _Assets();
      final cubit = LocalLosslessQaCubit(assets: assets);
      await _open(tester, cubit);
      tester.view.physicalSize = const Size(1080, 2400);
      await tester.pumpAndSettle();
      tester.view.physicalSize = const Size(822, 1828);
      await tester.pumpAndSettle();
      expect(cubit.state.status, 'initial');
      expect(cubit.state.error, isNull);
      expect(assets.initializes, 0);
      await _next(tester, cubit);
      expect(cubit.snapshot()['completedCases'], 1);
      tester.view.physicalSize = const Size(824, 1830);
      await tester.pumpAndSettle();
      expect(cubit.state.error, 'viewport_changed');
      expect(cubit.state.raster, isNull);
      expect(assets.indices, [0]);
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets('bozuk dosyada hata görünür; fallback veya tekrar yapılmaz', (
    tester,
  ) async {
    final assets = _Assets()..fail = true;
    final cubit = LocalLosslessQaCubit(assets: assets);
    await _open(tester, cubit);
    await _next(tester, cubit);
    expect(cubit.state.error, 'fixture_decode_refused');
    expect(find.textContaining('Görsel doğrulanamadı'), findsOneWidget);
    await _next(tester, cubit);
    expect(assets.indices, [0]);
    await tester.pumpWidget(const SizedBox());
  });
}
