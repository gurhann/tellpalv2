import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/widgets.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa_assets.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa_cubit.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa_view.dart';
import 'package:tellpal_mobile_poc/app/local_lossless_qa/pins.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'comparison_test_support.dart';
import 'fixture.dart';
import '../../test_driver/local_lossless_driver.dart' as host;

Map<String, dynamic> _manifest() => {
  'schemaVersion': 1,
  'evidenceSha256': localLosslessEvidenceSha256,
  'cases': jsonDecode(jsonEncode(localLosslessCases)),
};

class _MemoryBundle extends CachingAssetBundle {
  _MemoryBundle({Map<String, dynamic>? manifest})
    : values = {
        localLosslessManifestAsset: Uint8List.fromList(
          utf8.encode(jsonEncode(manifest ?? _manifest())),
        ),
      };
  final Map<String, Uint8List> values;
  final Map<String, Future<ByteData>> delayed = {};
  final List<String> reads = [];

  @override
  Future<ByteData> load(String key) async {
    reads.add(key);
    if (delayed.containsKey(key)) return delayed[key]!;
    final bytes = values[key];
    if (bytes == null) throw StateError('synthetic_missing_asset');
    return ByteData.sublistView(bytes);
  }
}

class _ControlledAssets extends LocalLosslessQaAssets {
  _ControlledAssets(this.decode);
  final Future<LocalLosslessQaDecoded> Function(
    int,
    ReadingComparisonCancellation,
  )
  decode;
  int loadCalls = 0, activeLoads = 0, maxActiveLoads = 0;
  final entered = Completer<void>();

  @override
  Future<void> initialize() async {}

  @override
  Future<LocalLosslessQaDecoded> load(
    int index,
    ReadingComparisonCancellation cancellation,
  ) async {
    loadCalls++;
    activeLoads++;
    if (activeLoads > maxActiveLoads) maxActiveLoads = activeLoads;
    if (!entered.isCompleted) entered.complete();
    try {
      return await decode(index, cancellation);
    } finally {
      activeLoads--;
    }
  }
}

/// Küçük sentetik raster yalnız sahiplik ve iptal testinin native handle'ıdır.
Future<LocalLosslessQaDecoded> _decoded(int index) async {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawColor(const ui.Color(0xFFFFFFFF), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(2, 2);
  late Uint8List bytes;
  try {
    final data = (await image.toByteData(format: ui.ImageByteFormat.png))!;
    bytes = data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  } finally {
    image.dispose();
    picture.dispose();
  }
  final owned = await ReadingComparisonRaster.decode(
    bytes,
    raster(width: 2, height: 2, bytes: bytes.length),
    2,
    2,
    limits(pixels: 4),
    ReadingComparisonCancellation(),
  );
  return LocalLosslessQaDecoded(owned, {
    'caseIndex': index,
    'caseId': localLosslessCases[index]['caseId'],
  });
}

Future<LocalLosslessQaCubit> _ready(_ControlledAssets assets) async {
  final cubit = LocalLosslessQaCubit(assets: assets);
  await cubit.initialize();
  cubit.setViewport(width: 390, height: 700, dpr: 3);
  return cubit;
}

void _markPainted(LocalLosslessQaCubit cubit) =>
    cubit.markPainted(cubit.state.epoch, const {
      'logicalWidth': 274.0,
      'logicalHeight': 1334 / 3,
      'dpr': 3.0,
      'physicalWidth': 822.0,
      'physicalHeight': 1334.0,
      'pixelAligned': true,
    });

void _expectNoHandles() {
  expect(ReadingComparisonRaster.activeBuffers, 0);
  expect(ReadingComparisonRaster.activeDescriptors, 0);
  expect(ReadingComparisonRaster.activeCodecs, 0);
  expect(ReadingComparisonRaster.activeImages, 0);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  tearDown(_expectNoHandles);

  test('offline QA yalnız açık native profile kapısından geçer', () {
    for (final enabled in [false, true]) {
      for (final profileMode in [false, true]) {
        for (final isWeb in [false, true]) {
          expect(
            localLosslessQaAllowed(
              enabled: enabled,
              profileMode: profileMode,
              isWeb: isWeb,
            ),
            enabled && profileMode && !isWeb,
          );
        }
      }
    }
  });

  test(
    'run nonce tam 64 lowercase hex olmalı ve tüm snapshotlara bağlanır',
    () {
      final nonce = List.filled(64, 'a').join();
      expect(validLocalLosslessQaRunNonce(nonce), true);
      expect(
        host.localLosslessDriverRunNonce({'LOCAL_LOSSLESS_RUN_NONCE': nonce}),
        nonce,
      );
      expect(
        () => host.requireLocalLosslessRuntimeNonce({'runNonce': nonce}, nonce),
        returnsNormally,
      );
      for (final invalid in [
        '',
        'a',
        '${nonce}0',
        nonce.substring(1),
        nonce.toUpperCase(),
        '$nonce\n',
        ' $nonce',
        '${nonce.substring(1)}g',
      ]) {
        expect(validLocalLosslessQaRunNonce(invalid), false);
        expect(
          () => host.localLosslessDriverRunNonce({
            'LOCAL_LOSSLESS_RUN_NONCE': invalid,
          }),
          throwsStateError,
        );
        expect(
          () => host.requireLocalLosslessRuntimeNonce({
            'runNonce': invalid,
          }, nonce),
          throwsA(isA<Exception>()),
        );
      }
      expect(() => host.localLosslessDriverRunNonce({}), throwsStateError);
      expect(
        () => host.requireLocalLosslessRuntimeNonce({}, nonce),
        throwsA(isA<Exception>()),
      );
      expect(
        () => host.requireLocalLosslessRuntimeNonce({
          'runNonce': List.filled(64, 'b').join(),
        }, nonce),
        throwsA(isA<Exception>()),
      );
    },
  );

  test(
    'VM adresi yalnız açık flutter drive yerel ortam girdisinden alınır',
    () {
      for (final valid in [
        'http://127.0.0.1:12345/auth=/',
        'http://[::1]:12345/auth=/',
        'ws://localhost:12345/auth=/ws',
      ]) {
        expect(
          host.localLosslessDriverVmServiceUrl({'VM_SERVICE_URL': valid}),
          valid,
        );
      }
      for (final environment in <Map<String, String>>[
        {},
        {'DART_VM_SERVICE_URL': 'http://127.0.0.1:12345/'},
        {'VM_SERVICE_URL': ''},
        {'VM_SERVICE_URL': ' '},
        {'VM_SERVICE_URL': 'http://127.0.0.1/'},
        {'VM_SERVICE_URL': 'http://127.0.0.1:0/'},
        {'VM_SERVICE_URL': 'http://127.0.0.1:70000/'},
        {'VM_SERVICE_URL': 'http://remote.example:12345/'},
        {'VM_SERVICE_URL': 'file:///private/tmp/socket'},
        {'VM_SERVICE_URL': 'http://user@127.0.0.1:12345/'},
        {'VM_SERVICE_URL': 'http://127.0.0.1:12345/#fragment'},
        {'VM_SERVICE_URL': 'http://127.0.0.1:12345/\n'},
      ]) {
        expect(
          () => host.localLosslessDriverVmServiceUrl(environment),
          throwsStateError,
        );
      }
    },
  );

  test('manifest yalnız mevcut altı sabit fixture pinini kabul eder', () async {
    final valid = LocalLosslessQaAssets(bundle: _MemoryBundle());
    await valid.initialize();
    for (final key in [
      'caseId',
      'assetId',
      'asset',
      'format',
      'bytes',
      'sha256',
      'width',
      'height',
      'rgbBytes',
      'rgbSha256',
    ]) {
      final input = _manifest();
      final first = (input['cases'] as List).first as Map;
      first[key] = first[key] is int ? (first[key] as int) + 1 : 'unknown';
      final bundle = _MemoryBundle(manifest: input);
      final assets = LocalLosslessQaAssets(bundle: bundle);
      await expectLater(
        assets.initialize(),
        throwsFormatException,
        reason: '$key değişimi fixture kapsamını genişletemez',
      );
      expect(bundle.reads, [localLosslessManifestAsset]);
      await expectLater(
        assets.load(0, ReadingComparisonCancellation()),
        throwsFormatException,
      );
    }
  });

  test(
    'eksik, ek, sırası bozuk ve yanlış kanıt manifesti reddedilir',
    () async {
      final badInputs = <Map<String, dynamic>>[
        {..._manifest(), 'schemaVersion': 2},
        {..._manifest(), 'evidenceSha256': 'changed'},
        {..._manifest(), 'extra': true},
        {..._manifest()}..remove('evidenceSha256'),
        {..._manifest(), 'cases': []},
        {..._manifest(), 'cases': localLosslessCases.reversed.toList()},
        {
          ..._manifest(),
          'cases': [...localLosslessCases, localLosslessCases.first],
        },
      ];
      for (final input in badInputs) {
        final assets = LocalLosslessQaAssets(
          bundle: _MemoryBundle(manifest: input),
        );
        await expectLater(assets.initialize(), throwsFormatException);
        await expectLater(
          assets.load(0, ReadingComparisonCancellation()),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'bilinmeyen fixture ve initialize öncesi yükleme asset okumaz',
    () async {
      final bundle = _MemoryBundle();
      final assets = LocalLosslessQaAssets(bundle: bundle);
      await expectLater(
        assets.load(0, ReadingComparisonCancellation()),
        throwsFormatException,
      );
      expect(bundle.reads, isEmpty);
      await assets.initialize();
      for (final index in [-1, 6, 100]) {
        await expectLater(
          assets.load(index, ReadingComparisonCancellation()),
          throwsFormatException,
        );
      }
      expect(bundle.reads, [localLosslessManifestAsset]);
    },
  );

  test('bozuk format, SHA ve byte boyutu decode öncesi reddedilir', () async {
    for (final mutation in ['format', 'sha', 'size']) {
      final bundle = _MemoryBundle();
      final assets = LocalLosslessQaAssets(bundle: bundle);
      await assets.initialize();
      final bytes = Uint8List(mutation == 'size' ? 8 : 1765704);
      if (mutation != 'format') {
        bytes.setRange(0, 8, [137, 80, 78, 71, 13, 10, 26, 10]);
      }
      bundle.values['assets/local_lossless/9778.png'] = bytes;
      await expectLater(
        assets.load(0, ReadingComparisonCancellation()),
        throwsFormatException,
      );
      _expectNoHandles();
    }
  });

  test('iptal asset okumasından önce ve sonra native iş açmaz', () async {
    final bundle = _MemoryBundle();
    final assets = LocalLosslessQaAssets(bundle: bundle);
    await assets.initialize();
    final before = ReadingComparisonCancellation()..cancel();
    await expectLater(assets.load(0, before), throwsStateError);
    expect(bundle.reads, [localLosslessManifestAsset]);
    final payload = Completer<ByteData>();
    bundle.delayed['assets/local_lossless/9778.png'] = payload.future;
    final during = ReadingComparisonCancellation();
    final loading = assets.load(0, during);
    during.cancel();
    payload.complete(ByteData(8));
    await expectLater(loading, throwsStateError);
  });

  test('alan yetmezse ve DPR geçersizse sessiz ölçekleme yapılmaz', () async {
    for (final viewport in [
      (273.0, 700.0, 3.0),
      (390.0, 444.0, 3.0),
      (390.0, 700.0, 0.0),
      (double.nan, 700.0, 3.0),
      (390.0, double.infinity, 3.0),
    ]) {
      final assets = _ControlledAssets((index, _) => _decoded(index));
      final cubit = LocalLosslessQaCubit(assets: assets);
      try {
        await cubit.initialize();
        cubit.setViewport(
          width: viewport.$1,
          height: viewport.$2,
          dpr: viewport.$3,
        );
        await expectLater(cubit.show(0), throwsStateError);
        expect(cubit.state.status, 'error');
        expect(assets.loadCalls, 0);
        expect(cubit.snapshot()['allCasesPassed'], false);
      } finally {
        await cubit.close();
      }
    }
  });

  test(
    'altı seri durum paint olmadan ilerlemez, eski raster önce bırakılır',
    () async {
      ReadingComparisonRaster? previous;
      final assets = _ControlledAssets((index, _) async {
        expect(previous?.image, isNull);
        return _decoded(index);
      });
      final cubit = await _ready(assets);
      final cache = PaintingBinding.instance.imageCache;
      final cacheState = (
        cache.currentSize,
        cache.currentSizeBytes,
        cache.liveImageCount,
        cache.pendingImageCount,
      );
      try {
        for (var index = 0; index < 6; index++) {
          await cubit.show(index);
          expect(cubit.state.status, 'loaded');
          expect(ReadingComparisonRaster.activeImages, 1);
          expect(cubit.snapshot()['completedCases'], index);
          expect(cubit.snapshot()['allCasesPassed'], false);
          previous = cubit.state.raster;
          await expectLater(cubit.show(index + 1), throwsStateError);
          _markPainted(cubit);
          expect(cubit.snapshot()['completedCases'], index + 1);
        }
        expect(cubit.snapshot()['allCasesPassed'], true);
        await expectLater(cubit.show(6), throwsStateError);
        await expectLater(cubit.show(0), throwsStateError);
        await cubit.stop();
        expect(cubit.snapshot()['stopped'], true);
        expect(previous!.image, isNull);
        _expectNoHandles();
        expect((
          cache.currentSize,
          cache.currentSizeBytes,
          cache.liveImageCount,
          cache.pendingImageCount,
        ), cacheState);
        expect(assets.maxActiveLoads, 1);
      } finally {
        await cubit.close();
      }
    },
  );

  for (final cause in ['background', 'stop', 'close']) {
    test(
      '$cause sırasında geç native sonuç gösterilmez ve sahiplik sıfırlanır',
      () async {
        final result = Completer<LocalLosslessQaDecoded>();
        final assets = _ControlledAssets((_, _) => result.future);
        final cubit = await _ready(assets);
        final first = cubit.show(0);
        await assets.entered.future;
        await expectLater(cubit.show(0), throwsStateError);
        expect(assets.loadCalls, 1);
        Future<void>? shuttingDown;
        if (cause == 'background') {
          cubit.visibility(false);
          cubit.visibility(true);
        } else if (cause == 'stop') {
          shuttingDown = cubit.stop();
        } else {
          shuttingDown = cubit.close();
        }
        var finished = false;
        shuttingDown?.then((_) => finished = true);
        await Future<void>.delayed(Duration.zero);
        if (shuttingDown != null) {
          expect(
            finished,
            false,
            reason: 'stop tamamlanmadan gecikmiş native iş bırakılmalıdır',
          );
        }
        final lateResult = await _decoded(0);
        result.complete(lateResult);
        await first;
        await shuttingDown;
        try {
          expect(lateResult.raster.image, isNull);
          expect(cubit.state.raster, isNull);
          expect(cubit.snapshot()['completedCases'], 0);
          expect(cubit.snapshot()['allCasesPassed'], false);
          expect(assets.maxActiveLoads, 1);
          await expectLater(cubit.show(0), throwsStateError);
        } finally {
          await cubit.close();
        }
      },
    );
  }

  test(
    'stale paint ve hizasız geometri PASS üretmez, loaded raster bırakılır',
    () async {
      for (final stale in [true, false]) {
        final cubit = await _ready(
          _ControlledAssets((index, _) => _decoded(index)),
        );
        try {
          await cubit.show(0);
          final owned = cubit.state.raster!;
          expect(
            () => cubit.markPainted(cubit.state.epoch + (stale ? 1 : 0), {
              'physicalWidth': 822.0,
              'physicalHeight': 1334.0,
              'pixelAligned': stale,
            }),
            throwsStateError,
          );
          expect(cubit.state.status, 'error');
          expect(cubit.snapshot()['completedCases'], 0);
          expect(cubit.snapshot()['allCasesPassed'], false);
          expect(owned.image, isNull);
        } finally {
          await cubit.close();
        }
      }
    },
  );

  test('yükleme hatası sticky ret olur; stop tekrarı yeni iş açmaz', () async {
    final assets = _ControlledAssets(
      (_, _) => Future.error(StateError('synthetic')),
    );
    final cubit = await _ready(assets);
    try {
      await cubit.show(0);
      expect(cubit.state.status, 'error');
      expect(cubit.state.raster, isNull);
      await expectLater(cubit.show(0), throwsStateError);
      final stop = cubit.stop();
      expect(identical(stop, cubit.stop()), true);
      await stop;
      expect(assets.loadCalls, 1);
      expect(cubit.snapshot()['stopped'], true);
      expect(cubit.snapshot()['allCasesPassed'], false);
    } finally {
      await cubit.close();
    }
  });

  testWidgets('paint tamamlanmış frame ve aynı raster epochunu bekler', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    final assets = _ControlledAssets((index, _) => _decoded(index));
    final cubit = LocalLosslessQaCubit(assets: assets);
    await cubit.initialize();
    try {
      await tester.pumpWidget(LocalLosslessQaApp(cubit: cubit));
      await tester.pumpAndSettle();
      expect(cubit.snapshot()['ready'], true);
      await tester.runAsync(() => cubit.show(0));
      expect(cubit.state.status, 'loaded');
      final painting = paintLocalLosslessQa(cubit);
      expect(cubit.snapshot()['completedCases'], 0);
      await tester.pump();
      await tester.pump();
      await painting;
      expect(cubit.snapshot()['completedCases'], 1);
      final sample = (cubit.snapshot()['cases'] as List).single as Map;
      final geometry = sample['widget'] as Map;
      expect(geometry['physicalWidth'], 822);
      expect(geometry['physicalHeight'], 1334);
      expect(geometry['pixelAligned'], true);
      expect(find.byType(RawImage), findsNothing);
      final stale = expectLater(paintLocalLosslessQa(cubit), throwsStateError);
      cubit.visibility(false);
      await tester.pump();
      await tester.pump();
      await stale;
      expect(cubit.snapshot()['allCasesPassed'], false);
      expect(cubit.state.raster, isNull);
    } finally {
      await cubit.close();
      await tester.pumpWidget(const SizedBox());
    }
  });

  testWidgets(
    'paralel driver komutu aktif işi iptal eder; stop kuyruğu boşaltır',
    (tester) async {
      final result = Completer<LocalLosslessQaDecoded>();
      final assets = _ControlledAssets((_, _) => result.future);
      final cubit = await _ready(assets);
      final control = LocalLosslessQaControl(cubit);
      try {
        final first = control.handle('show:0');
        await assets.entered.future;
        final refusal = jsonDecode(await control.handle('show:0')) as Map;
        expect(refusal['ok'], false);
        expect(cubit.state.status, 'error');
        expect(assets.loadCalls, 1);
        final stopping = control.handle('stop');
        final lateResult = (await tester.runAsync(() => _decoded(0)))!;
        result.complete(lateResult);
        expect((jsonDecode(await first) as Map)['ok'], false);
        await tester.pump();
        final stopped = jsonDecode(await stopping) as Map;
        expect(stopped['stopped'], true);
        expect(stopped['completedCases'], 0);
        expect(stopped['activeImages'], 0);
        expect(lateResult.raster.image, isNull);
        expect(assets.maxActiveLoads, 1);
      } finally {
        await cubit.close();
      }
    },
  );
}
