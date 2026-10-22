import 'dart:async';
import 'dart:ui' as ui;
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get_it/get_it.dart';
import 'package:tellpal_mobile_poc/app/p5e_offline/p5e_injection.dart';
import 'package:tellpal_mobile_poc/app/p5e_offline/p5e_router.dart';
import 'package:tellpal_mobile_poc/app/p5e_offline_qa.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5e_offline/presentation/p5e_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5e_offline/presentation/p5e_screen.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

class _Repository implements P5dRepository {
  final reads = <int>[];
  final pending = <Completer<Either<P5dFailure, P5dEncoded>>>[];
  bool fail = false;
  Completer<Either<P5dFailure, Unit>>? initializeGate;
  @override
  Future<Either<P5dFailure, Unit>> initialize() async => initializeGate != null
      ? await initializeGate!.future
      : fail
      ? const Left(P5dFailure('manifest_refused'))
      : const Right(unit);
  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) {
    reads.add(index);
    final value = Completer<Either<P5dFailure, P5dEncoded>>();
    pending.add(value);
    return value.future;
  }
}

Future<Uint8List> _png() async {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawColor(const ui.Color(0xff112233), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(1712, 2778);
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    return data!.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  } finally {
    image.dispose();
    picture.dispose();
  }
}

Future<void> _tick() => Future<void>.delayed(Duration.zero);

Future<void> _visitAll(
  P5eCubit cubit,
  _Repository repo,
  Uint8List bytes,
) async {
  await cubit.initialize();
  cubit.network('unsatisfied');
  await _tick();
  repo.pending.last.complete(Right(P5dEncoded(bytes, 0, 0)));
  while (cubit.state.status == 'loading') {
    await _tick();
  }
  await cubit.presented(cubit.state.epoch, {});
  for (var index = 0; index < 12; index++) {
    for (final crop in [false, true]) {
      if (index == 0 && !crop) continue;
      final work = cubit.select(index, crop: crop);
      await _tick();
      repo.pending.last.complete(Right(P5dEncoded(bytes, 0, 0)));
      await work;
      await cubit.presented(cubit.state.epoch, {});
    }
  }
}

Future<ReadingComparisonRaster> _fixtureRaster(Uint8List bytes) =>
    ReadingComparisonRaster.decode(
      bytes,
      ReadingImageRaster(
        provider: 'offline',
        objectKey: 'fixture',
        generation: 'test',
        mime: 'image/png',
        byteSize: bytes.length,
        sha256: 'test',
        width: 1712,
        height: 2778,
      ),
      1712,
      2778,
      p5eLimits,
      ReadingComparisonCancellation(),
    );

int _patternColor(int x, int y) =>
    0xff000000 |
    ((x * 31 + 17) << 16) |
    ((y * 37 + 19) << 8) |
    (x * 13 + y * 11 + 23);

Future<Uint8List> _patternPng() async {
  final recorder = ui.PictureRecorder();
  final canvas = ui.Canvas(recorder);
  canvas.drawColor(const ui.Color(0xff112233), ui.BlendMode.src);
  for (var y = 0; y < 6; y++) {
    for (var x = 0; x < 8; x++) {
      canvas.drawRect(
        ui.Rect.fromLTWH(400.0 + x, 760.0 + y, 1, 1),
        ui.Paint()
          ..color = ui.Color(_patternColor(x, y))
          ..isAntiAlias = false,
      );
    }
  }
  final picture = recorder.endRecording();
  final image = await picture.toImage(1712, 2778);
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    return data!.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  } finally {
    image.dispose();
    picture.dispose();
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets(
    'native environment handler hatası ve uyuşmazlığı Türkçe ret ekranı verir',
    (tester) async {
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      addTearDown(() => messenger.setMockMethodCallHandler(p5eChannel, null));
      for (final throws in [true, false]) {
        messenger.setMockMethodCallHandler(p5eChannel, (_) async {
          if (throws) {
            throw PlatformException(code: 'native_environment_failed');
          }
          return {
            'packageId': 'other',
            'plugins': 0,
            'physicalDevice': true,
            'runNonce': 'nonce',
          };
        });
        final refusal = await p5eStartupRefusal(expectedNonce: 'nonce');
        expect(refusal, isNotNull);
        await tester.pumpWidget(refusal!);
        expect(
          find.text('P5e başlangıcı doğrulanamadı. Karşılaştırma kapalı.'),
          findsOneWidget,
        );
        expect(find.byType(P5eScreen), findsNothing);
      }
      messenger.setMockMethodCallHandler(
        p5eChannel,
        (_) async => {
          'packageId': p5ePackage,
          'plugins': 0,
          'physicalDevice': true,
          'runNonce': 'nonce',
        },
      );
      expect(await p5eStartupRefusal(expectedNonce: 'nonce'), isNull);
    },
  );

  test(
    'çift finish aynı bekleyen Future ile tek seal üretir; başarı ve hata temizlenir',
    () async {
      final bytes = await _png();
      for (final fails in [false, true]) {
        final repo = _Repository();
        final seal = Completer<void>();
        var sealCalls = 0;
        final cubit = P5eCubit(repo, (entry) {
          if (entry['event'] == 'seal') {
            sealCalls++;
            return seal.future;
          }
          return Future.value();
        });
        await _visitAll(cubit, repo, bytes);
        final first = cubit.finish();
        final second = cubit.finish();
        expect(identical(first, second), isTrue);
        expect(cubit.state.status, 'sealing');
        cubit.network('unsatisfied');
        expect(cubit.state.status, 'sealing');
        expect(sealCalls, 1);
        await cubit.select(0);
        expect(cubit.state.status, 'sealing');
        if (fails) {
          seal.completeError(StateError('disk_full'));
        } else {
          seal.complete();
        }
        await Future.wait([first, second]);
        expect(cubit.state.status, fails ? 'error' : 'finished');
        expect(ReadingComparisonRaster.activeImages, 0);
        await cubit.close();
        expect(ReadingComparisonRaster.reservedSlots, 0);
      }
    },
  );

  test(
    'initialize ve asset read deadline sonsuz işi mantıksal olarak kapatır',
    () async {
      final gate = Completer<Either<P5dFailure, Unit>>();
      final repo = _Repository()..initializeGate = gate;
      final cubit = P5eCubit.testing(
        repo,
        (_) async {},
        deadline: const Duration(milliseconds: 20),
      );
      await cubit.initialize();
      expect(cubit.state.status, 'error');
      await cubit.close().timeout(const Duration(seconds: 1));
      gate.complete(const Right(unit));
      await _tick();
      expect(cubit.isClosed, isTrue);
      expect(repo.reads, isEmpty);

      final reading = _Repository();
      final reader = P5eCubit.testing(
        reading,
        (_) async {},
        deadline: const Duration(milliseconds: 20),
      );
      await reader.initialize();
      reader.network('unsatisfied');
      await _tick();
      final queued = reader.select(1);
      await queued.timeout(const Duration(seconds: 1));
      expect(reader.state.status, 'error');
      expect(reading.reads, [0]);
      await reader.close().timeout(const Duration(seconds: 1));
      reading.pending.single.complete(Right(P5dEncoded(await _png(), 0, 0)));
      await _tick();
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.reservedSlots, 0);
    },
  );

  test(
    'decode deadline close’u sınırlar; geç gerçek raster tek kez bırakılır',
    () async {
      final bytes = await _png();
      final lateRaster = await _fixtureRaster(bytes);
      final delayed = Completer<ReadingComparisonRaster>();
      ReadingComparisonCancellation? cancellation;
      var decodes = 0;
      final repo = _Repository();
      final cubit = P5eCubit.testing(
        repo,
        (_) async {},
        deadline: const Duration(milliseconds: 20),
        decode: (bytes, metadata, width, height, limits, token) {
          decodes++;
          cancellation = token;
          return delayed.future;
        },
      );
      await cubit.initialize();
      cubit.network('unsatisfied');
      await _tick();
      repo.pending.single.complete(Right(P5dEncoded(bytes, 0, 0)));
      while (cubit.state.status == 'loading') {
        await _tick();
      }
      expect(cubit.state.status, 'error');
      expect(cancellation!.cancelled, isTrue);
      expect(cubit.state.raster, isNull);
      await cubit.select(1);
      expect(decodes, 1);
      await cubit.close().timeout(const Duration(seconds: 1));
      expect(ReadingComparisonRaster.activeImages, 1);
      delayed.complete(lateRaster);
      await _tick();
      expect(lateRaster.image, isNull);
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.reservedSlots, 0);
    },
  );

  test(
    'bekleyen asset read sırasında close deadline ile biter ve geç sonuç çizilmez',
    () async {
      final repo = _Repository();
      final cubit = P5eCubit.testing(
        repo,
        (_) async {},
        deadline: const Duration(milliseconds: 20),
      );
      await cubit.initialize();
      cubit.network('unsatisfied');
      await _tick();
      expect(repo.reads, [0]);
      await cubit.close().timeout(const Duration(seconds: 1));
      expect(cubit.isClosed, isTrue);
      repo.pending.single.complete(Right(P5dEncoded(await _png(), 0, 0)));
      await _tick();
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.reservedSlots, 0);
    },
  );

  testWidgets(
    'gerçek P5e painter DPR3 ROI başlangıcı ve 1:1 fiziksel pikseli çizer',
    (tester) async {
      tester.view.devicePixelRatio = 3;
      tester.view.physicalSize = const Size(1170, 2532);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.view.resetPhysicalSize);
      final repo = _Repository();
      late P5eCubit cubit;
      await tester.runAsync(() async {
        cubit = P5eCubit(repo, (_) async {});
        final bytes = await _patternPng();
        await cubit.initialize();
        cubit.network('unsatisfied');
        await _tick();
        repo.pending.single.complete(Right(P5dEncoded(bytes, 0, 0)));
        while (cubit.state.status == 'loading') {
          await _tick();
        }
        final crop = cubit.select(0, crop: true);
        await _tick();
        repo.pending.last.complete(Right(P5dEncoded(bytes, 0, 0)));
        await crop;
      });
      final boundaryKey = GlobalKey();
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            chipTheme: const ChipThemeData(labelStyle: TextStyle(fontSize: 10)),
          ),
          home: BlocProvider.value(
            value: cubit,
            child: RepaintBoundary(key: boundaryKey, child: const P5eScreen()),
          ),
        ),
      );
      await tester.pump();
      final rasterFinder = find.byKey(const ValueKey('p5e-raster'));
      expect(rasterFinder, findsOneWidget);
      final offset =
          tester.getTopLeft(rasterFinder) -
          tester.getTopLeft(find.byKey(boundaryKey));
      final boundary = tester.renderObject<RenderRepaintBoundary>(
        find.byKey(boundaryKey),
      );
      await tester.runAsync(() async {
        final image = await boundary.toImage(pixelRatio: 3);
        try {
          final data = await image.toByteData(
            format: ui.ImageByteFormat.rawRgba,
          );
          int pixel(int x, int y) {
            final i =
                ((offset.dy * 3).round() + y) * image.width * 4 +
                ((offset.dx * 3).round() + x) * 4;
            return (data!.getUint8(i + 3) << 24) |
                (data.getUint8(i) << 16) |
                (data.getUint8(i + 1) << 8) |
                data.getUint8(i + 2);
          }

          for (var y = 0; y < 6; y++) {
            for (var x = 0; x < 8; x++) {
              expect(
                pixel(x, y),
                _patternColor(x, y),
                reason: 'physical $x,$y',
              );
            }
          }
          expect(pixel(8, 0), 0xff112233);
          expect(pixel(0, 6), 0xff112233);
        } finally {
          image.dispose();
        }
      });
      await tester.pumpWidget(const SizedBox());
      await tester.runAsync(cubit.close);
      expect(ReadingComparisonRaster.activeImages, 0);
    },
  );

  test('ayrı DI yalnız P5e container ve route oluşturur', () async {
    final container = configureP5e(GetIt.asNewInstance());
    expect(container<P5eRouter>().routes.single.page.name, 'P5eOffline');
    await container<P5eCubit>().close();
    await container.reset();
  });
  test(
    'manifest reddi ve bilinmeyen/erişilebilir ağ gösterimi kapatır',
    () async {
      final repo = _Repository();
      final cubit = P5eCubit(repo, (_) async {});
      await cubit.initialize();
      await cubit.select(0);
      for (final value in [
        'unknown',
        'satisfied',
        'requiresConnection',
        'invalid',
      ]) {
        cubit.network(value);
        await cubit.select(1);
      }
      expect(repo.reads, isEmpty);
      await cubit.close();
      final refused = P5eCubit(_Repository()..fail = true, (_) async {});
      await refused.initialize();
      refused.network('unsatisfied');
      await refused.select(0);
      expect(refused.state.status, 'error');
      await refused.close();
    },
  );
  test('hızlı seçim eski decode sonucunu boyamaz ve seri çalışır', () async {
    final repo = _Repository();
    final cubit = P5eCubit(repo, (_) async {});
    await cubit.initialize();
    cubit.network('unsatisfied');
    await _tick();
    final latest = cubit.select(2);
    expect(repo.reads, [0]);
    repo.pending.first.complete(Right(P5dEncoded(await _png(), 0, 0)));
    await _tick();
    expect(repo.reads, [0, 2]);
    repo.pending.last.complete(Right(P5dEncoded(await _png(), 0, 0)));
    await latest;
    expect(cubit.state.index, 2);
    expect(cubit.state.status, 'loaded');
    expect(ReadingComparisonRaster.activeImages, 1);
    cubit.network('satisfied');
    expect(cubit.state.raster, isNull);
    expect(ReadingComparisonRaster.activeImages, 0);
    await cubit.close();
    expect(ReadingComparisonRaster.reservedSlots, 0);
  });
  test('gerçek lifecycle observer bekleyen seçimi iptal eder', () async {
    final repo = _Repository();
    final cubit = P5eCubit(repo, (_) async {});
    await cubit.initialize();
    cubit.network('unsatisfied');
    await _tick();
    final control = P5eControl(cubit);
    WidgetsBinding.instance.addObserver(control);
    TestWidgetsFlutterBinding.instance.handleAppLifecycleStateChanged(
      AppLifecycleState.inactive,
    );
    repo.pending.first.complete(Right(P5dEncoded(await _png(), 0, 0)));
    await _tick();
    expect(cubit.state.status, 'blocked');
    expect(cubit.state.network, 'unknown');
    expect(cubit.state.raster, isNull);
    await control.dispose();
    expect(ReadingComparisonRaster.activeImages, 0);
    expect(ReadingComparisonRaster.activeCodecs, 0);
    expect(ReadingComparisonRaster.activeBuffers, 0);
  });
  test(
    '24 doğrulanmış görünüm olmadan mühürlenmez ve sayaç tekrar saymaz',
    () async {
      final repo = _Repository();
      final entries = <Map<String, Object?>>[];
      final cubit = P5eCubit(repo, (entry) async {
        entries.add(entry);
      });
      final bytes = await _png();
      await cubit.initialize();
      cubit.network('unsatisfied');
      await _tick();
      repo.pending.last.complete(Right(P5dEncoded(bytes, 0, 0)));
      while (cubit.state.status == 'loading') {
        await _tick();
      }
      await cubit.presented(cubit.state.epoch, {});
      await cubit.presented(cubit.state.epoch, {});
      expect(cubit.state.visitedViews, 1);
      await cubit.finish();
      expect(entries.where((e) => e['event'] == 'seal'), isEmpty);
      for (var index = 0; index < 12; index++) {
        for (final crop in [false, true]) {
          if (index == 0 && !crop) continue;
          final work = cubit.select(index, crop: crop);
          await _tick();
          repo.pending.last.complete(Right(P5dEncoded(bytes, 0, 0)));
          await work;
          await cubit.presented(cubit.state.epoch, {});
        }
      }
      expect(cubit.state.visitedViews, 24);
      await cubit.finish();
      expect(cubit.state.status, 'finished');
      expect(entries.where((e) => e['event'] == 'seal').length, 1);
      expect(ReadingComparisonRaster.activeImages, 0);
      await cubit.close();
    },
  );
  test('decode ve günlük yazma hatası rasterı bırakır', () async {
    for (final decodeFails in [true, false]) {
      final repo = _Repository();
      final cubit = P5eCubit(repo, (_) async => throw StateError('disk_full'));
      await cubit.initialize();
      cubit.network('unsatisfied');
      await _tick();
      repo.pending.first.complete(
        Right(P5dEncoded(decodeFails ? Uint8List(4) : await _png(), 0, 0)),
      );
      await _tick();
      if (!decodeFails) {
        while (cubit.state.status == 'loading') {
          await _tick();
        }
        await cubit.presented(cubit.state.epoch, {});
      } else {
        while (cubit.state.status == 'loading') {
          await _tick();
        }
      }
      expect(cubit.state.status, 'error');
      expect(cubit.state.raster, isNull);
      await cubit.close();
      expect(ReadingComparisonRaster.reservedSlots, 0);
    }
  });
}
