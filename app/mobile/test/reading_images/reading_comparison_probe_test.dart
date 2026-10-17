import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter/material.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import 'reading_image_comparison_cubit_test.dart' show PendingRepository;
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import '../../test_driver/reading_comparison_probe.dart';
import 'fixture.dart';
import 'reading_image_comparison_http_test.dart' as http_fixture;

class RealLocalHttp extends HttpOverrides {}

String jwt({String uid = imageExperimentQaUid}) {
  String encode(Object value) =>
      base64Url.encode(utf8.encode(jsonEncode(value))).replaceAll('=', '');
  final seconds = DateTime.now().millisecondsSinceEpoch ~/ 1000;
  return '${encode({'alg': 'synthetic'})}.${encode({'sub': uid, 'aud': 'tellpal-ee0dd', 'iss': 'https://securetoken.google.com/tellpal-ee0dd', 'iat': seconds, 'exp': seconds + 3600})}.synthetic';
}

Map<String, dynamic> configuration(int port) => {
  'action': 'configure',
  'runId': comparisonRunId,
  'planSha256': comparisonPlanSha,
  'uid': imageExperimentQaUid,
  'contentId': 94,
  'port': port,
  'token': jwt(),
};

Future<Uint8List> png(int width, int height) async {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawColor(const ui.Color(0xFFFFFFFF), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(width, height);
  try {
    return (await image.toByteData(
      format: ui.ImageByteFormat.png,
    ))!.buffer.asUint8List();
  } finally {
    image.dispose();
    picture.dispose();
  }
}

class PaintProbeCubit extends ReadingImageComparisonCubit {
  PaintProbeCubit(PendingRepository repository)
    : super(
        LoadReadingImageComparison(repository),
        repository,
        const ReadingImageComparisonConfiguration.geometry(),
        ReadingComparisonWorkGate(),
        const ReadingComparisonClock(),
      );
  void loaded(ReadingComparisonRaster raster, int epoch) => emit(
    ReadingImageComparisonState(status: 'loaded', raster: raster, epoch: epoch),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'tek onaylı run ve kaynak politikası dışındaki runtime mesajları reddedilir',
    () {
      final input = configuration(12345);
      final parsed = parseComparisonConfiguration(input);
      expect(parsed.limits!.maxPeakBytes, 134217728);
      expect(
        parsed.account!.credentials.expiresAt
            .difference(DateTime.now())
            .inSeconds,
        inInclusiveRange(178, 180),
      );
      parsed.account!.credentials.revoke();
      for (final wrong in [
        {...input, 'token': jwt(uid: 'another-account')},
        {...input, 'contentId': 71},
        {...input, 'contentId': 93},
        {...input, 'planSha256': 'changed'},
        {...input, 'port': 443},
        {...input, 'origin': 'https://other.example'},
      ]) {
        expect(() => parseComparisonConfiguration(wrong), throwsA(anything));
      }
    },
  );

  test(
    'bozuk ve fazladan alanlı komut hata cevabına secret yansıtmaz',
    () async {
      final probe = ReadingComparisonProbe();
      final token = jwt();
      final result = await probe.handle(
        jsonEncode({...configuration(12345), 'extra': token}),
      );
      expect(jsonDecode(result), {'ok': false, 'code': 'probe_refused'});
      expect(result, isNot(contains(token)));
      expect(jsonDecode(await probe.handle('{broken')), {
        'ok': false,
        'code': 'probe_refused',
      });
      expect(
        jsonDecode(
          await probe.handle(
            '{"action":"select","entryIndex":1,"canonicalSource":true}',
          ),
        )['ok'],
        false,
      );
    },
  );

  testWidgets(
    'aynı probe ikinci configure kabul etmez; stop credential ve timer sahipliğini bırakır',
    (tester) async {
      final probe = ReadingComparisonProbe();
      final input = configuration(12345);
      expect(jsonDecode(await probe.handle(jsonEncode(input)))['ok'], true);
      expect(jsonDecode(await probe.handle(jsonEncode(input)))['ok'], false);
      final stopped = jsonDecode(await probe.handle('{"action":"stop"}'));
      expect(stopped['stopped'], true);
      expect(stopped['activeImages'], 0);
      expect(jsonDecode(await probe.handle(jsonEncode(input)))['ok'], false);
      await tester.pumpWidget(const ReadingComparisonWaiting());
      await comparisonGetIt.reset();
    },
  );

  testWidgets('kısmi DI hatasında credential kapanır ve timer kalmaz', (
    tester,
  ) async {
    ReadingImageComparisonConfiguration? captured;
    var streamClosed = false;
    final probe = ReadingComparisonProbe(
      initializeDependencies: (configuration) {
        captured = configuration;
        configuration.account!.credentials.revocations.listen(
          (_) {},
          onDone: () {
            streamClosed = true;
          },
        );
        comparisonGetIt.registerSingleton<ReadingImageComparisonConfiguration>(
          configuration,
        );
        throw StateError('synthetic_partial_di');
      },
    );
    final result = jsonDecode(
      await probe.handle(jsonEncode(configuration(12345))),
    );
    expect(result, {'ok': false, 'code': 'probe_refused'});
    expect(captured!.account!.credentials.isActive, false);
    await tester.pump();
    expect(streamClosed, true);
    expect(probe.snapshot()['deadlineActive'], false);
    expect(probe.snapshot()['credentialActive'], false);
    expect(probe.snapshot()['stopped'], true);
    expect(jsonDecode(await probe.handle('{"action":"stop"}'))['ok'], true);
    await tester.pumpWidget(const SizedBox());
    await comparisonGetIt.reset();
  });

  testWidgets(
    'paint makbuzu gerçek frame bekler ve arada değişen epoch reddedilir',
    (tester) async {
      final config = parseComparisonConfiguration(configuration(12345));
      final bytes = (await tester.runAsync(() => png(2, 2)))!;
      final rasterValue = (await tester.runAsync(
        () => ReadingComparisonRaster.decode(
          bytes,
          raster(width: 2, height: 2, bytes: bytes.length),
          2,
          2,
          config.limits!,
          ReadingComparisonCancellation(),
        ),
      ))!;
      final previous = Bloc.observer;
      final probe = ReadingComparisonProbe();
      Bloc.observer = probe;
      final cubit = PaintProbeCubit(PendingRepository());
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      try {
        cubit.loaded(rasterValue, 7);
        await tester.pumpWidget(
          Directionality(
            textDirection: TextDirection.ltr,
            child: Center(
              child: SizedBox(
                width: 30,
                height: 30,
                child: ReadingImageComparisonRaster(
                  raster: rasterValue,
                  fit: BoxFit.contain,
                ),
              ),
            ),
          ),
        );
        var completed = false;
        final painting = probe.handle('{"action":"paint","epoch":7}').then((
          value,
        ) {
          completed = true;
          return value;
        });
        expect(
          completed,
          false,
          reason: 'loaded durumu tamamlanmış paint değildir',
        );
        await tester.pump();
        final painted = jsonDecode(await painting);
        expect(painted['ok'], true);
        expect(painted['paintedEpoch'], 7);
        expect(painted['activeImages'], 1);
        final stale = probe.handle('{"action":"paint","epoch":7}');
        cubit.loaded(rasterValue, 8);
        await tester.pump();
        expect(jsonDecode(await stale), {'ok': false, 'code': 'probe_refused'});
        await tester.pumpWidget(const SizedBox());
        final absent = jsonDecode(
          await probe.handle('{"action":"paint","epoch":8}'),
        );
        expect(
          absent['ok'],
          false,
          reason: 'ağaçta çizilecek raster olmadan makbuz yoktur',
        );
      } finally {
        await cubit.close();
        rasterValue.dispose();
        await config.account!.credentials.close();
        Bloc.observer = previous;
      }
    },
  );

  test(
    'gerçek HTTP ve codec: payload ölçülür, rendition kimliği ve native sahiplik korunur',
    () async {
      final sourceBytes = await png(1712, 2778);
      final outputBytes = await png(822, 1334);
      final fixture = Fixture(
        source: raster(
          width: 1712,
          height: 2778,
          bytes: sourceBytes.length,
          sha: sha256.convert(sourceBytes).toString(),
          objectKey: 'private/source.png',
        ),
        profiles: [profile(width: 823, height: 1334)],
      );
      final manifest = seal([
        for (final entry in fixture.manifest.images)
          ReadingImageEntry(
            source: entry.source,
            renditions: [
              rendition(
                entry.source,
                profile(width: 823, height: 1334),
                bytes: outputBytes.length,
                sha: sha256.convert(outputBytes).toString(),
                objectKey: 'private/rendition.png',
              ),
            ],
          ),
      ], publication: identity(contentId: 94));
      final envelope = http_fixture.envelope(fixture);
      (envelope['identity'] as Map)['contentId'] = 94;
      (envelope['identity'] as Map)['manifestDigest'] =
          manifest.identity.manifestDigest;
      final images = (envelope['descriptor'] as Map)['images'] as List;
      for (var i = 0; i < images.length; i++) {
        (images[i]['renditions'] as List).single['raster'] = http_fixture
            .rasterJson(manifest.images[i].renditions.single.raster);
      }
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      final input = configuration(server.port);
      final config = parseComparisonConfiguration(input);
      var mediaRequests = 0;
      server.listen((request) async {
        expect(
          request.headers.value('authorization'),
          'Bearer ${input['token']}',
        );
        final media = request.uri.path.endsWith('/bytes');
        final bytes = media ? outputBytes : utf8.encode(jsonEncode(envelope));
        if (media) mediaRequests++;
        request.response.headers.set('Cache-Control', 'private,no-store');
        request.response.headers.contentType = media
            ? ContentType('image', 'png')
            : ContentType.json;
        request.response.contentLength = bytes.length;
        request.response.add(bytes);
        await request.response.close();
      });
      final previous = Bloc.observer;
      final probe = ReadingComparisonProbe();
      Bloc.observer = probe;
      configureReadingImageComparisonDependencies(config);
      await comparisonGetIt.unregister<ReadingImageComparisonDataSource>();
      comparisonGetIt.registerFactory<ReadingImageComparisonDataSource>(
        () => ObservedReadingComparisonSource(config, probe.record),
      );
      final cubit = HttpOverrides.runWithHttpOverrides(
        () => comparisonGetIt<ReadingImageComparisonCubit>(),
        RealLocalHttp(),
      );
      try {
        final completed = cubit.stream.firstWhere(
          (state) => state.status == 'loaded' || state.status == 'error',
        );
        HttpOverrides.runWithHttpOverrides(
          () => cubit.select(
            id: 94,
            entryIndex: 1,
            canonicalSource: false,
            width: 411,
            height: 456.66666666666663,
            dpr: 2,
            fit: ReadingImageFit.cover,
          ),
          RealLocalHttp(),
        );
        final loaded = await completed.timeout(const Duration(seconds: 10));
        expect(loaded.status, 'loaded');
        final snapshot = probe.snapshot();
        final events = snapshot['events'] as List;
        final measured = events
            .where((e) => e['kind'] == 'http' && e['endpoint'] == 'bytes')
            .single;
        expect(measured['payloadBytes'], outputBytes.length);
        final rendered = events.where((e) => e['status'] == 'loaded').single;
        expect(rendered['decodedWidth'], 822);
        expect(rendered['decodedHeight'], 1334);
        expect(rendered['role'], 'readingDetail');
        expect(rendered['requiredHeight'], 1334);
        expect(
          rendered['displaySha256'],
          sha256.convert(outputBytes).toString(),
        );
        expect(mediaRequests, 1);
        final exported = jsonEncode(snapshot);
        expect(exported, isNot(contains(input['token'] as String)));
        expect(exported, isNot(contains('objectKey')));
        expect(exported, isNot(contains('private/source.png')));
        cubit.visibility(false);
        expect(ReadingComparisonRaster.activeImages, 0);
        cubit.visibility(true);
        expect(cubit.state.error, 'readmission_required');
        expect(mediaRequests, 1);
      } finally {
        await cubit.close();
        await config.account!.credentials.close();
        await comparisonGetIt.reset();
        Bloc.observer = previous;
        await server.close(force: true);
      }
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.activeBuffers, 0);
      expect(ReadingComparisonRaster.activeDescriptors, 0);
      expect(ReadingComparisonRaster.activeCodecs, 0);
    },
  );
}
