import 'dart:async';
import 'dart:ui' as ui;
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get_it/get_it.dart';
import 'package:tellpal_mobile_poc/app/p5e_offline/p5e_injection.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5e_offline/presentation/p5e_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_frame_timing.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5f_performance/domain/p5f_plan.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5f_performance/domain/p5f_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5f_performance/data/p5f_manifest_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5f_performance/presentation/p5f_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5f_performance/presentation/p5f_screen.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

class _Repository implements P5fRepository {
  final pending = Completer<Either<P5dFailure, P5dEncoded>>();
  bool initialized = false;
  int reads = 0;
  @override
  Future<Either<P5dFailure, Unit>> initialize() async {
    initialized = true;
    return const Right(unit);
  }

  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) {
    reads++;
    return pending.future;
  }
}

class _BrokenBundle extends CachingAssetBundle {
  @override
  Future<ByteData> load(String key) async => throw StateError('bad_asset');
}

class _ImmediateRepository implements P5fRepository {
  _ImmediateRepository(this.bytes);
  final Uint8List bytes;
  final reads = <int>[];
  @override
  Future<Either<P5dFailure, Unit>> initialize() async => const Right(unit);
  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) async {
    reads.add(index);
    return Right(P5dEncoded(bytes, 1, 0));
  }
}

class _MatchedTimings extends P5dFrameTimings {
  bool closed = false;
  final proofs = <Map<String, Object?>>[];
  @override
  ui.FrameTiming? matching(Map<String, Object?> proof, int epoch, int index) {
    proofs.add(Map.of(proof));
    final frame = ui.FrameTiming(
      vsyncStart: 100,
      buildStart: 110,
      buildFinish: 120,
      rasterStart: 130,
      rasterFinish: 140,
      rasterFinishWallTime: 999999,
      frameNumber: proof['frameNumber']! as int,
    );
    return p5dTimingMatches(proof, frame, epoch, index) ? frame : null;
  }

  @override
  void close() {
    closed = true;
    super.close();
  }
}

Future<Uint8List> _tinyPng() async {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawColor(const ui.Color(0xff112233), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(1, 1);
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    return data!.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  } finally {
    image.dispose();
    picture.dispose();
  }
}

Future<void> _tick() => Future<void>.delayed(Duration.zero);

Future<ReadingComparisonRaster> _decodeTiny(
  Uint8List bytes,
  ReadingComparisonCancellation cancellation,
) => ReadingComparisonRaster.decode(
  bytes,
  ReadingImageRaster(
    provider: 'test',
    objectKey: 'fixture',
    generation: 'test',
    mime: 'image/png',
    byteSize: bytes.length,
    sha256: 'test',
    width: 1,
    height: 1,
  ),
  1,
  1,
  p5eLimits,
  cancellation,
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  for (final deadline in [false, true]) {
    test(
      'geç seal yanıtı ${deadline ? 'oturum süresini' : 'lifecycle kesintisini'} başarıya çeviremez',
      () async {
        final bytes = await _tinyPng();
        final sealing = Completer<void>(),
            reply = Completer<Map<String, Object?>>();
        final methods = <String>[];
        final cubit = P5fCubit.testing(
          _ImmediateRepository(bytes),
          (method, _) async {
            methods.add(method);
            if (method == 'seal') {
              sealing.complete();
              return reply.future;
            }
            return {};
          },
          createTimings: _MatchedTimings.new,
          decode: (_, _, _, _, _, token) => _decodeTiny(bytes, token),
          sessionLimit: deadline
              ? const Duration(seconds: 1)
              : const Duration(minutes: 10),
        );
        final subscription = cubit.stream.listen((state) {
          if (state.status == 'loaded') {
            cubit.painted({
              'epoch': state.epoch,
              'index': state.index,
              'crop': false,
              'frameNumber': state.epoch,
            });
          }
        });
        try {
          await cubit.initialize();
          final work = cubit.start();
          await sealing.future;
          expect(cubit.state.completed, 144);
          if (deadline) {
            await Future<void>.delayed(const Duration(milliseconds: 1050));
          } else {
            cubit.lifecycle(false);
          }
          expect(cubit.state.status, 'error');
          final error = cubit.state.error;
          expect(
            error,
            deadline ? 'session_deadline' : 'background_interrupted',
          );
          reply.complete({});
          await work;
          expect(cubit.state.status, 'error');
          expect(cubit.state.error, error);
          expect(methods.where((m) => m == 'seal').length, 1);
          expect(methods, contains('abort'));
          expect(ReadingComparisonRaster.reservedSlots, 0);
        } finally {
          await subscription.cancel();
          await cubit.close();
        }
      },
    );
  }
  testWidgets(
    'gerçek ekran raster paint kanıtı bir trialı Cubit üzerinden tamamlar',
    (tester) async {
      final bytes = (await tester.runAsync(_tinyPng))!;
      final raster = (await tester.runAsync(
        () => _decodeTiny(bytes, ReadingComparisonCancellation()),
      ))!;
      final frames = _MatchedTimings(), rows = <Map<String, Object?>>[];
      final releaseRecord = Completer<Map<String, Object?>>();
      final cubit = P5fCubit.testing(
        _ImmediateRepository(bytes),
        (method, values) async {
          if (method == 'recordTrial') {
            rows.add(Map.of(values));
            return releaseRecord.future;
          }
          return {};
        },
        createTimings: () => frames,
        decode: (_, _, _, _, _, _) async => raster,
      );
      await cubit.initialize();
      await tester.pumpWidget(
        BlocProvider.value(
          value: cubit,
          child: const MaterialApp(home: P5fScreen()),
        ),
      );
      await tester.tap(find.text('Çevrimdışı ölçümü başlat'));
      for (
        var tick = 0;
        tick < 20 &&
            find.byType(ReadingImageComparisonRaster).evaluate().isEmpty;
        tick++
      ) {
        await tester.pump(Duration.zero, EnginePhase.layout);
      }
      expect(find.byType(ReadingImageComparisonRaster), findsOneWidget);
      for (var tick = 0; tick < 20 && rows.isEmpty; tick++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(rows.length, 1);
      final row = rows.single, proof = row['proof']! as Map<String, Object?>;
      expect(row['ordinal'], 0);
      expect(row['cleanup'], true);
      expect(proof['epoch'], 1);
      expect(proof['index'], 0);
      expect(proof['crop'], false);
      expect(
        proof['frameNumber'],
        tester.binding.platformDispatcher.frameData.frameNumber,
      );
      expect(row['timingFrameNumber'], proof['frameNumber']);
      expect(proof['logicalWidth'], greaterThan(0));
      expect(proof['logicalHeight'], greaterThan(0));
      expect(proof['filterQuality'], 'low');
      expect(frames.proofs.single, proof);
      expect(raster.image, isNull);
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.reservedSlots, 0);
      cubit.interrupt('test_stop_after_real_paint');
      releaseRecord.complete({});
      await cubit.start();
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
      expect(frames.closed, true);
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'gerçek execute trial finally döngüsü 156 turu temizleyip tek seal üretir',
    () async {
      final bytes = await _tinyPng();
      final repo = _ImmediateRepository(bytes);
      final frames = _MatchedTimings(),
          methods = <String>[],
          rows = <Map<String, Object?>>[];
      final tokens = <ReadingComparisonCancellation>[];
      final rasters = <ReadingComparisonRaster>[];
      final cubit = P5fCubit.testing(
        repo,
        (method, values) async {
          methods.add(method);
          if (method == 'recordTrial') rows.add(Map.of(values));
          if (method == 'finishTrial' ||
              method == 'beginTrial' ||
              method == 'seal') {
            expect(ReadingComparisonRaster.activeImages, 0);
            expect(ReadingComparisonRaster.reservedSlots, 0);
          }
          if (method == 'loaded') {
            expect(ReadingComparisonRaster.activeImages, 1);
          }
          if (method == 'seal') {
            expect(values, {'completed': 144, 'warmups': 12, 'cleanup': true});
            expect(rows.length, 156);
          }
          return {};
        },
        createTimings: () => frames,
        decode: (encoded, metadata, width, height, limits, cancellation) async {
          expect(width, 1712);
          expect(height, 2778);
          expect(identical(limits, p5eLimits), isTrue);
          cancellation.check();
          tokens.add(cancellation);
          final raster = await ReadingComparisonRaster.decode(
            bytes,
            ReadingImageRaster(
              provider: 'test',
              objectKey: 'fixture',
              generation: 'test',
              mime: 'image/png',
              byteSize: bytes.length,
              sha256: 'test',
              width: 1,
              height: 1,
            ),
            1,
            1,
            limits,
            cancellation,
          );
          rasters.add(raster);
          return raster;
        },
      );
      final subscription = cubit.stream.listen((state) {
        if (state.status == 'loaded') {
          cubit.painted({
            'epoch': state.epoch,
            'index': state.index,
            'crop': false,
            'frameNumber': state.epoch + 100,
            'logicalWidth': 390.0,
            'logicalHeight': 600.0,
            'dpr': 3.0,
            'fit': 'contain',
            'filterQuality': 'low',
          });
        }
      });
      try {
        await cubit.initialize();
        await cubit.start();
        expect(cubit.state.status, 'finished');
        expect(cubit.state.completed, 144);
        expect(rows.length, 156);
        expect(rows.where((r) => r['warmup'] == true).length, 12);
        expect(rows.where((r) => r['warmup'] == false).length, 144);
        expect(repo.reads, p5fPlan().map((t) => t.index));
        expect(rows.map((r) => r['ordinal']), List.generate(156, (i) => i));
        expect(rows.every((r) => r['cleanup'] == true), isTrue);
        expect(methods.where((m) => m == 'seal').length, 1);
        expect(methods.where((m) => m == 'begin').length, 1);
        expect(methods, isNot(contains('abort')));
        expect(tokens.length, 156);
        expect(tokens.toSet().length, 156);
        expect(tokens.every((t) => t.cancelled), isTrue);
        expect(rasters.every((r) => r.image == null), isTrue);
        expect(cubit.state.raster, isNull);
        expect(frames.closed, isTrue);
        expect(ReadingComparisonRaster.activeBuffers, 0);
        expect(ReadingComparisonRaster.activeDescriptors, 0);
        expect(ReadingComparisonRaster.activeCodecs, 0);
        expect(ReadingComparisonRaster.activeImages, 0);
        expect(ReadingComparisonRaster.reservedSlots, 0);
      } finally {
        await subscription.cancel();
        await cubit.close();
      }
    },
  );
  test('12 ısınma ve her dosya için 12 dengeli örnek üretir', () {
    final plan = p5fPlan();
    expect(plan.length, 156);
    expect(plan.where((v) => v.warmup).length, 12);
    for (var index = 0; index < 12; index++) {
      final samples = plan.where((v) => !v.warmup && v.index == index).toList();
      expect(samples.length, 12);
      expect(samples.map((v) => v.round).toSet().length, 12);
    }
    for (var round = 0; round < 12; round++) {
      final rows = plan.where((v) => v.round == round).toList();
      expect(rows.take(3).map((v) => v.index % 3), p5fOrders[round % 6]);
      expect(
        rows.map((v) => v.index ~/ 3).toSet().toList(),
        round.isEven ? [0, 1, 2, 3] : [3, 2, 1, 0],
      );
    }
  });
  test(
    'ön doğrulama olmadan asset okunmaz ve bozuk manifest reddedilir',
    () async {
      final repo = P5fManifestRepository(bundle: _BrokenBundle());
      expect((await repo.read(0)).isLeft(), isTrue);
      expect((await repo.initialize()).isLeft(), isTrue);
      expect((await repo.read(0)).isLeft(), isTrue);
    },
  );
  test('get_it P5f Cubit ve repository sağlar', () async {
    final container = configureP5e(GetIt.asNewInstance());
    final cubit = container<P5fCubit>();
    expect(container<P5fRepository>(), isA<P5fManifestRepository>());
    await cubit.close();
    await container.reset();
  });
  test(
    'hazır ekranda ağ ve Ayarlar geçişi oturumu bozmaz; sonra begin çalışır',
    () async {
      final repo = _Repository(), methods = <String>[];
      final cubit = P5fCubit(repo, (method, _) async {
        methods.add(method);
        return {};
      });
      await cubit.initialize();
      cubit.network('satisfied');
      cubit.lifecycle(false);
      cubit.network('unsatisfied');
      cubit.lifecycle(true);
      expect(cubit.state.status, 'ready');
      expect(cubit.state.error, isNull);
      final work = cubit.start();
      await _tick();
      expect(methods, containsAllInOrder(['begin', 'beginTrial']));
      repo.pending.complete(const Left(P5dFailure('test_stop')));
      await work;
      await cubit.close();
    },
  );
  test('trial süresi dolunca iptal edilir ve mühür gönderilmez', () async {
    final repo = _Repository(), methods = <String>[];
    final cubit = P5fCubit.testing(repo, (method, _) async {
      methods.add(method);
      return {};
    }, trialLimit: const Duration(milliseconds: 20));
    await cubit.initialize();
    await cubit.start();
    expect(cubit.state.status, 'error');
    expect(methods, contains('abort'));
    expect(methods, isNot(contains('seal')));
    repo.pending.complete(Right(P5dEncoded(Uint8List(1), 1, 0)));
    await _tick();
    expect(ReadingComparisonRaster.reservedSlots, 0);
    await cubit.close();
  });
  test(
    'ağ veya arka plan kesintisi eski denemeyi başarıya çeviremez',
    () async {
      for (final background in [false, true]) {
        final repo = _Repository(), methods = <String>[];
        final cubit = P5fCubit.testing(repo, (method, _) async {
          methods.add(method);
          return {};
        });
        await cubit.initialize();
        final work = cubit.start();
        await _tick();
        if (background) {
          cubit.lifecycle(false);
        } else {
          cubit.network('satisfied');
        }
        repo.pending.complete(Right(P5dEncoded(Uint8List(1), 1, 0)));
        await work;
        expect(cubit.state.status, 'error');
        expect(cubit.state.completed, 0);
        expect(methods, isNot(contains('recordTrial')));
        expect(methods, isNot(contains('seal')));
        await cubit.close();
      }
    },
  );
  test('native sayaç reddi denemeyi ve mührü engeller', () async {
    final methods = <String>[];
    final cubit = P5fCubit(_Repository(), (method, _) async {
      methods.add(method);
      if (method == 'beginTrial') {
        throw PlatformException(code: 'counter_refused');
      }
      return {};
    });
    await cubit.initialize();
    await cubit.start();
    expect(cubit.state.status, 'error');
    expect(methods, contains('abort'));
    expect(methods, isNot(contains('seal')));
    await cubit.close();
  });
  test('oturum süresi dolunca sessiz örnek eleme yapılmaz', () async {
    final repo = _Repository();
    final cubit = P5fCubit.testing(
      repo,
      (_, _) async => {},
      sessionLimit: const Duration(milliseconds: 10),
      trialLimit: const Duration(milliseconds: 30),
    );
    await cubit.initialize();
    await cubit.start();
    expect(cubit.state.status, 'error');
    expect(cubit.state.error, 'session_deadline');
    await cubit.close();
  });
  test('frame epoch index ve engine numarası birlikte eşleşir', () {
    final timing = ui.FrameTiming(
      vsyncStart: 100,
      buildStart: 110,
      buildFinish: 120,
      rasterStart: 130,
      rasterFinish: 140,
      rasterFinishWallTime: 999999,
      frameNumber: 42,
    );
    final proof = <String, Object?>{
      'epoch': 1,
      'index': 2,
      'crop': false,
      'frameNumber': 42,
    };
    expect(p5dTimingMatches(proof, timing, 1, 2), isTrue);
    expect(
      p5dTimingMatches({...proof, 'frameNumber': 41}, timing, 1, 2),
      isFalse,
    );
    expect(p5dTimingMatches(proof, timing, 2, 2), isFalse);
    expect(p5dTimingMatches(proof, timing, 1, 3), isFalse);
  });
  test('decode süresi dolduktan gelen raster tek kez bırakılır', () async {
    final repo = _Repository();
    final decoded = Completer<ReadingComparisonRaster>();
    final cubit = P5fCubit.testing(
      repo,
      (_, _) async => {},
      decode: (_, _, _, _, _, _) => decoded.future,
      trialLimit: const Duration(milliseconds: 20),
    );
    await cubit.initialize();
    final work = cubit.start();
    await _tick();
    repo.pending.complete(Right(P5dEncoded(Uint8List(1), 1, 0)));
    await work;
    expect(cubit.state.status, 'error');
    final recorder = ui.PictureRecorder();
    ui.Canvas(recorder).drawColor(const ui.Color(0xff112233), ui.BlendMode.src);
    final picture = recorder.endRecording();
    final image = await picture.toImage(1, 1);
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    image.dispose();
    picture.dispose();
    final bytes = data!.buffer.asUint8List(
      data.offsetInBytes,
      data.lengthInBytes,
    );
    final raster = await ReadingComparisonRaster.decode(
      bytes,
      ReadingImageRaster(
        provider: 'test',
        objectKey: 'fixture',
        generation: 'test',
        mime: 'image/png',
        byteSize: bytes.length,
        sha256: 'test',
        width: 1,
        height: 1,
      ),
      1,
      1,
      p5eLimits,
      ReadingComparisonCancellation(),
    );
    expect(ReadingComparisonRaster.activeImages, 1);
    decoded.complete(raster);
    await _tick();
    expect(raster.image, isNull);
    expect(ReadingComparisonRaster.activeImages, 0);
    expect(ReadingComparisonRaster.reservedSlots, 0);
    await cubit.close();
  });
  test('üretim sınırını genişleten test ayarı reddedilir', () {
    expect(
      () => P5fCubit.testing(
        _Repository(),
        (_, _) async => {},
        trialLimit: const Duration(seconds: 11),
      ),
      throwsStateError,
    );
  });
}
