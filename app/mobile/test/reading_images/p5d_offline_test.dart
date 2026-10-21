import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:dartz/dartz.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get_it/get_it.dart';
import 'package:tellpal_mobile_poc/app/p5d_offline/p5d_injection.dart';
import 'package:tellpal_mobile_poc/app/p5d_offline/p5d_router.dart';
import 'package:tellpal_mobile_poc/app/p5d_offline_qa.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/data/p5d_manifest_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/data/p5d_icc.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_pins.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_text_crops.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_readback.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_frame_proof.dart';
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_frame_timing.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

class _Bundle extends CachingAssetBundle {
  _Bundle(this.manifest, [this.assets = const {}]);
  final Object manifest;
  final Map<String, Uint8List> assets;
  final assetReads = <String>[];
  @override
  Future<ByteData> load(String key) async {
    if (key != 'assets/p5d/manifest.json') {
      assetReads.add(key);
      final bytes = assets[key];
      if (bytes == null) throw StateError('missing_fixture');
      return ByteData.sublistView(bytes);
    }
    return ByteData.sublistView(
      Uint8List.fromList(utf8.encode(jsonEncode(manifest))),
    );
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

class _Repository implements P5dRepository {
  final pending = Completer<Either<P5dFailure, P5dEncoded>>();
  @override
  Future<Either<P5dFailure, Unit>> initialize() async => const Right(unit);
  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) => pending.future;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test('ara örnek readback ölçer raw checksum ve yazım üretmez', () async {
    final recorder = ui.PictureRecorder();
    ui.Canvas(recorder).drawColor(const ui.Color(0xff112233), ui.BlendMode.src);
    final picture = recorder.endRecording();
    final image = await picture.toImage(1, 1);
    try {
      final result = await p5dReadback(
        image,
        '/missing-directory',
        'sample',
        ReadingComparisonCancellation(),
        persist: false,
      );
      final raw = result['rawRgba'] as Map;
      expect(raw['status'], 'MEASURED');
      expect(raw['bytes'], 4);
      expect(raw['readbackMicros'], isNonNegative);
      expect(raw['checksumStatus'], 'NOT_MEASURED');
      expect(raw['checksumMicros'], isNull);
      expect(raw['sha256'], isNull);
      expect(raw['file'], isNull);
      expect(raw['writeMicros'], isNull);
      expect(raw['writeStatus'], 'NOT_RUN');
    } finally {
      image.dispose();
      picture.dispose();
    }
  });
  test('dosya sistemi hatası türü ve OS kodu kaybolmaz', () {
    final diagnostic = p5dFailureDiagnostic(
      const FileSystemException(
        'write',
        '/private',
        OSError('No space left on device', 28),
      ),
    );
    expect(diagnostic['type'], 'FileSystemException');
    expect(diagnostic['osErrorCode'], 28);
    expect(diagnostic.containsKey('path'), isFalse);
  });
  test('yazı kesiti her triplette aynı kalır ve tam boyutun içindedir', () {
    expect(p5dTextCrops, {
      1950: (x: 400, y: 760),
      1952: (x: 440, y: 2160),
      8303: (x: 240, y: 1410),
      8306: (x: 240, y: 240),
    });
    for (var index = 0; index < 12; index++) {
      final roi = p5dTextCrop(index);
      expect(roi, p5dTextCrop(index ~/ 3 * 3));
      expect(roi.x + 1200, lessThanOrEqualTo(1712));
      expect(roi.y + 600, lessThanOrEqualTo(2778));
    }
  });
  test(
    'timing gerçek frame numarası ve epoch ile eşleşir zaman alanları ayrı tutulur',
    () {
      final frame = ui.FrameTiming(
        vsyncStart: 100,
        buildStart: 110,
        buildFinish: 120,
        rasterStart: 130,
        rasterFinish: 140,
        rasterFinishWallTime: 150,
        frameNumber: 42,
      );
      final proof = <String, Object?>{
        'epoch': 7,
        'index': 2,
        'crop': false,
        'frameNumber': 42,
        'vsyncMicros': 100,
      };
      expect(p5dTimingMatches(proof, frame, 7, 2), isTrue);
      expect(p5dTimingMatches(proof, frame, 8, 2), isFalse);
      expect(
        p5dTimingMatches({...proof, 'frameNumber': 41}, frame, 7, 2),
        isFalse,
      );
      expect(
        p5dTimingMatches({...proof, 'vsyncMicros': 99}, frame, 7, 2),
        isTrue,
      );
      expect(p5dTimingMatches({...proof, 'crop': true}, frame, 7, 2), isFalse);
      final timings = P5dFrameTimings();
      expect(timings.matching(proof, 7, 2), isNull);
      timings.close();
    },
  );
  test('frame işareti yanlış mode eski epoch ve farklı kaynağı reddeder', () {
    final proof = <String, Object?>{'epoch': 7, 'index': 2, 'crop': true};
    expect(p5dProofMatches(proof, 7, 2, true), isTrue);
    expect(p5dProofMatches(proof, 8, 2, true), isFalse);
    expect(p5dProofMatches(proof, 7, 2, false), isFalse);
    expect(p5dProofMatches(proof, 7, 3, true), isFalse);
    expect(p5dProofMatches(null, 7, 2, true), isFalse);
  });
  test('on iki pin dört kaynak ve üç formatı tam boyutta eşler', () {
    expect(p5dPins.length, 12);
    expect(p5dPins.map((p) => p['assetId']).toSet(), {1950, 1952, 8303, 8306});
    for (var i = 0; i < 12; i += 3) {
      expect(p5dPins.skip(i).take(3).map((p) => p['arm']), [
        'original',
        'PNG',
        'WEBP',
      ]);
      expect(p5dPins.skip(i).take(3).map((p) => p['role']).toSet().length, 1);
    }
    expect(
      p5dPins.every((p) => p['width'] == 1712 && p['height'] == 2778),
      isTrue,
    );
    expect(p5dLimits.maxEncodedBytes, 32 * 1024 * 1024);
    expect(p5dLimits.maxPeakBytes, 192 * 1024 * 1024);
  });
  test('ayrı DI yalnız P5d repository Cubit ve router sağlar', () async {
    final container = configureP5d(GetIt.asNewInstance());
    expect(container<P5dRepository>(), isA<P5dManifestRepository>());
    final cubit = container<P5dCubit>();
    expect(cubit, isA<P5dCubit>());
    await cubit.close();
    expect(container<P5dRouter>().routes.length, 1);
    await container.reset();
  });
  test('bozuk manifest asset okunmadan reddedilir', () async {
    for (final manifest in [
      {'schemaVersion': 1, 'reportSha256': 'changed', 'cases': p5dPins},
      {
        'schemaVersion': 1,
        'reportSha256': p5dReportSha256,
        'cases': p5dPins.reversed.toList(),
      },
    ]) {
      final bundle = _Bundle(manifest);
      final repository = P5dManifestRepository(bundle: bundle);
      expect((await repository.initialize()).isLeft(), isTrue);
      expect(bundle.assetReads, isEmpty);
      expect((await repository.read(0)).isLeft(), isTrue);
      expect(bundle.assetReads, isEmpty);
    }
    final bundle = _Bundle({
      'schemaVersion': 1,
      'reportSha256': p5dReportSha256,
      'cases': p5dPins,
    });
    expect(
      (await P5dManifestRepository(bundle: bundle).initialize()).isLeft(),
      isTrue,
    );
    expect(bundle.assetReads, [p5dPins.first['asset']]);
  });
  test(
    'geçerli fixture initialize olur test seam üretim pinlerini değiştirmez',
    () async {
      final bytes = await _tinyPng();
      final pins = <Map<String, Object?>>[
        {
          'asset': 'fixture.png',
          'format': 'PNG',
          'bytes': bytes.length,
          'sha256': sha256.convert(bytes).toString(),
          'iccSha256': null,
        },
      ];
      final manifest = {
        'schemaVersion': 1,
        'reportSha256': 'test-report',
        'cases': pins,
      };
      final bundle = _Bundle(manifest, {'fixture.png': bytes});
      final repository = P5dManifestRepository.testing(
        bundle: bundle,
        pins: pins,
        reportSha256: 'test-report',
      );
      expect((await repository.initialize()).isRight(), isTrue);
      expect(bundle.assetReads, ['fixture.png']);
      expect((await repository.read(0)).isRight(), isTrue);
      final productionBundle = _Bundle(manifest, {'fixture.png': bytes});
      expect(
        (await P5dManifestRepository(
          bundle: productionBundle,
        ).initialize()).isLeft(),
        isTrue,
      );
      expect(productionBundle.assetReads, isEmpty);
    },
  );
  test('bare VP8L kabulü yerine RIFF ve kayıpsız chunk şartı aranır', () {
    expect(
      () => p5dIccSha256(Uint8List.fromList([0x2f]), 'WEBP'),
      throwsFormatException,
    );
    final data = Uint8List(26);
    data.setRange(0, 4, ascii.encode('RIFF'));
    data.setRange(8, 16, ascii.encode('WEBPVP8L'));
    ByteData.sublistView(data).setUint32(4, 18, Endian.little);
    ByteData.sublistView(data).setUint32(16, 5, Endian.little);
    data[20] = 0x2f;
    expect(p5dIccSha256(data, 'WEBP'), isNull);
    data.setRange(12, 16, ascii.encode('VP8 '));
    expect(() => p5dIccSha256(data, 'WEBP'), throwsFormatException);
  });
  testWidgets(
    'kayıtlı bootstrap observer lifecycle ile bekleyen işi iptal eder',
    (tester) async {
      final bytes = await tester.runAsync(_tinyPng);
      final originalLifecycle =
          tester.binding.lifecycleState ?? ui.AppLifecycleState.resumed;
      for (final lifecycle in [
        ui.AppLifecycleState.paused,
        ui.AppLifecycleState.inactive,
      ]) {
        tester.binding.handleAppLifecycleStateChanged(
          ui.AppLifecycleState.resumed,
        );
        final repository = _Repository();
        final cubit = P5dCubit(repository);
        await cubit.initialize();
        final controller = attachP5dControl(cubit, '/private/tmp');
        try {
          final work = cubit.show(0, 0, '/private/tmp');
          final failed = expectLater(
            work,
            throwsA(
              isA<StateError>().having(
                (e) => e.message,
                'message',
                'cancelled',
              ),
            ),
          );
          tester.binding.handleAppLifecycleStateChanged(lifecycle);
          repository.pending.complete(Right(P5dEncoded(bytes!, 0, 0)));
          await failed;
          await tester.pump();
          expect(cubit.state.status, 'stopped');
          expect(cubit.state.raster, isNull);
          final snapshot = cubit.snapshot();
          for (final key in [
            'activeImages',
            'activeBuffers',
            'activeDescriptors',
            'activeCodecs',
          ]) {
            expect(snapshot[key], 0);
          }
          expect(snapshot['humanQualityAcceptance'], 'NOT_RUN');
        } finally {
          tester.binding.removeObserver(controller);
          p5dChannel.setMethodCallHandler(null);
          await cubit.close();
          tester.binding.handleAppLifecycleStateChanged(originalLifecycle);
        }
      }
    },
  );
  test(
    'eşzamanlı seçim önceki işi geçersiz kılar ve ikinci decode başlatmaz',
    () async {
      final repository = _Repository();
      final cubit = P5dCubit(repository);
      await cubit.initialize();
      final first = cubit.show(0, 0, '/private/tmp');
      await expectLater(cubit.show(0, 0, '/private/tmp'), throwsStateError);
      repository.pending.complete(const Left(P5dFailure('cancelled')));
      await expectLater(first, throwsStateError);
      expect(cubit.state.error, 'command_refused');
      expect(ReadingComparisonRaster.activeImages, 0);
      await cubit.close();
    },
  );
}
