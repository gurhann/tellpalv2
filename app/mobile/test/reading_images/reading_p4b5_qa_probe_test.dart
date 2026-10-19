import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'fixture.dart';
import 'comparison_test_support.dart' show limits;
import 'reading_comparison_probe_test.dart' show png, RealLocalHttp;
import 'reading_image_comparison_cubit_test.dart' show PendingRepository;
import 'package:flutter_test/flutter_test.dart';
import '../../test_driver/reading_consumer_qa_probe.dart';
import 'reading_consumer_qa_probe_test.dart' as old;

Map<String, dynamic> p4b5Config(int port) {
  final input = old.config(port);
  input['proposalSha256'] = ConsumerQaExpectedScope.p4b5.proposalSha256;
  input['measurementId'] = ConsumerQaExpectedScope.p4b5.measurementId;
  input['windowDeadlineEpochMillis'] =
      (input['windowStartedAtEpochMillis'] as int) + 120000;
  return input;
}

class _PhaseCubit extends ReadingImageComparisonCubit {
  _PhaseCubit(PendingRepository repository)
    : super(
        LoadReadingImageComparison(repository),
        repository,
        const ReadingImageComparisonConfiguration.geometry(),
        ReadingComparisonWorkGate(),
        const ReadingComparisonClock(),
      );
  void loaded(ReadingComparisonRaster owned, int epoch) {
    final fixture = Fixture(source: raster(width: 4, height: 4));
    emit(
      ReadingImageComparisonState(
        status: 'loaded',
        raster: owned,
        epoch: epoch,
        comparison: ReadingImageComparison(
          fixture.manifest,
          fixture.context,
          1,
          180000,
        ),
        geometry: const ReadingImageGeometry(
          physicalWidth: 2,
          physicalHeight: 2,
          requiredWidth: 2,
          requiredHeight: 2,
          decodeWidth: 2,
          decodeHeight: 2,
        ),
      ),
    );
  }

  void drain() {
    state.raster?.dispose();
    emit(const ReadingImageComparisonState(status: 'background'));
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'yeni scope APK sabitinden seçilir; eski parser varsayılanı korunur',
    () {
      final legacy = parseConsumerQaConfiguration(old.config(12345));
      expect(legacy.limits!.maxStatusChecks, 18);
      final parsed = parseConsumerQaConfiguration(
        p4b5Config(12345),
        expectedScope: ConsumerQaExpectedScope.p4b5,
      );
      expect(parsed.limits!.maxStatusChecks, 12);
      expect(parsed.limits!.maxControlMillis, lessThanOrEqualTo(120000));
      for (final wrong in [
        old.config(12345),
        {...p4b5Config(12345), 'measurementId': consumerQaRunId},
        {...p4b5Config(12345), 'proposalSha256': consumerQaProposalSha},
        {
          ...p4b5Config(12345),
          'windowDeadlineEpochMillis':
              p4b5Config(12345)['windowStartedAtEpochMillis'] + 180000,
        },
        {...p4b5Config(12345), 'statusLimit': 18},
      ]) {
        expect(
          () => parseConsumerQaConfiguration(
            wrong,
            expectedScope: ConsumerQaExpectedScope.p4b5,
          ),
          throwsA(anything),
        );
      }
      expect(
        () => parseConsumerQaConfiguration(p4b5Config(12345)),
        throwsA(anything),
      );
      legacy.account!.credentials.revoke();
      parsed.account!.credentials.revoke();
    },
  );
  test('P4b5 gerçek HTTP öncesi en çok 12 status ve toplam 17 ayırır', () {
    final configuration = parseConsumerQaConfiguration(
      p4b5Config(12345),
      expectedScope: ConsumerQaExpectedScope.p4b5,
    );
    final events = <Map<String, Object?>>[];
    final source = ObservedConsumerQaSource(
      configuration,
      events.add,
      expectedScope: ConsumerQaExpectedScope.p4b5,
    );
    source.reserve('manifest', {'contentId': '94', 'language': 'tr'});
    for (var index = 0; index < 4; index++) {
      source.reserve('bytes', old.mediaQuery(index));
    }
    for (var index = 0; index < 12; index++) {
      source.reserve('status', old.identityQuery());
    }
    expect(events.length, 17);
    expect(
      () => source.reserve('status', old.identityQuery()),
      throwsA(anything),
    );
    expect(source.failed, true);
    expect(events.length, 17);
    configuration.account!.credentials.revoke();
  });
  testWidgets('yanlış ilk configure aynı P4b5 probe ile tekrar açılamaz', (
    tester,
  ) async {
    final probe = ReadingConsumerQaProbe(
      expectedScope: ConsumerQaExpectedScope.p4b5,
    );
    expect(
      jsonDecode(await probe.handle(jsonEncode(old.config(12345))))['ok'],
      false,
    );
    expect(
      jsonDecode(await probe.handle(jsonEncode(p4b5Config(12345))))['ok'],
      false,
    );
    final snapshot = jsonDecode(await probe.handle('{"action":"snapshot"}'));
    for (final flag in [
      'ok',
      'configured',
      'stopped',
      'cubitClosed',
      'credentialActive',
      'deadlineActive',
    ]) {
      expect(snapshot[flag], isA<bool>());
    }
    expect(snapshot['configured'], false);
    await probe.stop();
    await tester.pump();
  });
  testWidgets(
    'gerçek loopback ve dört native paint ham JSON phase kabulüne gider',
    (tester) async {
      final prior = Bloc.observer;
      final probe = ReadingConsumerQaProbe(
        expectedScope: ConsumerQaExpectedScope.p4b5,
        initializeDependencies: (_) async {},
      );
      final records = <Map<String, Object?>>[];
      void capture(String phase) => records.add({
        'phase': phase,
        'sample': jsonDecode(jsonEncode(probe.snapshot())),
      });
      capture('baseline');
      late HttpServer server;
      late Uint8List bytes;
      await tester.runAsync(() async {
        bytes = await png(4, 4);
        server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        server.listen((request) async {
          request.response.headers.contentType = ContentType('image', 'png');
          request.response.contentLength = bytes.length;
          request.response.add(bytes);
          await request.response.close();
        });
        expect(
          jsonDecode(
            await probe.handle(jsonEncode(p4b5Config(server.port))),
          )['ok'],
          true,
        );
      });
      Bloc.observer = probe;
      final cubit = _PhaseCubit(PendingRepository());
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      try {
        for (var index = 0; index < 4; index++) {
          if (index > 0) cubit.drain();
          late ReadingComparisonRaster owned;
          await tester.runAsync(() async {
            probe.record({
              'kind': 'httpReservation',
              'endpoint': 'bytes',
              'ordinal': index + 1,
            });
            final client = HttpOverrides.runWithHttpOverrides(
              () => HttpClient(),
              RealLocalHttp(),
            );
            try {
              final request = await client.getUrl(
                Uri.parse('http://127.0.0.1:${server.port}/bytes'),
              );
              final response = await request.close();
              final received = await response.fold<List<int>>(
                [],
                (all, part) => all..addAll(part),
              );
              probe.record({
                'kind': 'http',
                'endpoint': 'bytes',
                'ok': response.statusCode == 200,
                'payloadBytes': received.length,
              });
              owned = await ReadingComparisonRaster.decode(
                Uint8List.fromList(received),
                raster(width: 4, height: 4, bytes: received.length),
                2,
                2,
                limits(pixels: 16),
                ReadingComparisonCancellation(),
              );
            } finally {
              client.close(force: true);
            }
          });
          cubit.loaded(owned, index + 1);
          await tester.pumpWidget(
            Directionality(
              textDirection: TextDirection.ltr,
              child: Center(
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: ReadingImageComparisonRaster(
                    raster: owned,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
            ),
          );
          final paint = probe.handle('{"action":"paint","epoch":${index + 1}}');
          await tester.pump();
          records.add({
            'phase': 'paint${index + 1}',
            'sample': jsonDecode(await paint),
          });
        }
        final before = probe.snapshot();
        expect(before['observedLifecycle'], 'resumed');
        expect(before['liveRasterAttached'], true);
        expect(before['currentEpoch'], 4);
        expect(before['lastPaintedEpoch'], 4);
        capture('beforeBackground');
        cubit.drain();
        tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
        expect(probe.snapshot()['observedLifecycle'], 'paused');
        expect(probe.snapshot()['liveRasterAttached'], false);
        capture('background');
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
        capture('resumed');
        await cubit.close();
        capture('closedUi');
        final stopped = jsonDecode(await probe.handle('{"action":"stop"}'));
        records.add({'phase': 'stopped', 'sample': stopped});
        // Ham JSON özel test logundan Python'a geçer; yeni IPC veya storage yoktur.
        // ignore: avoid_print
        print(
          'P4B5_RESOURCE_JSON:${jsonEncode({'chain': 'loopback-native', 'records': records})}',
        );
      } finally {
        await probe.stop();
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
        Bloc.observer = prior;
        await tester.runAsync(() => server.close(force: true));
      }
    },
  );
}
