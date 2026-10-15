import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_phase_coordinator.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'fixture.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('synthetic-phases');
  ImageExperimentConfiguration config(
    String batch, {
    String? purpose,
    int budget = 120000,
  }) {
    final manifest = fixtureManifest()..['batchId'] = batch;
    (manifest['entries'] as List).single['purposeAcceptanceReceipt'] =
        purpose ?? batch;
    final wire = FixtureWire(manifest: manifest);
    wire.envelope['batchId'] = batch;
    wire.metadata['batchId'] = batch;
    wire.metadata['expectedAppId'] =
        'com.tellpal.tellpal_mobile_poc.imageexperiment.release';
    wire.envelope['expiresAt'] = wire.now.millisecondsSinceEpoch + budget;
    return wire.configuration();
  }

  Future<List<Map<String, dynamic>>> rows(
    ImageExperimentRecorder recorder,
  ) async => (jsonDecode(await recorder.export())['records'] as List)
      .cast<Map<String, dynamic>>();
  tearDown(
    () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null),
  );
  for (final quality in [false, true]) {
    testWidgets(
      'owned ${quality ? "quality" : "primary"} phases wait for exact release completion',
      (tester) async {
        var reads = 0, loads = 0, finishes = 0;
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (_) async {
              reads++;
              return {
                'memoryBytes': 1024,
                'memoryCounter': 'ios_phys_footprint',
                'memoryReason': 'none',
                'nativeReadMicroseconds': 2,
                'nativeResultCode': 0,
                'nativeResultCount': 96,
                'thermalState': 'nominal',
              };
            });
        final recorder = ImageExperimentRecorder();
        final coordinator = ImageExperimentPhaseCoordinator(
          config(quality ? 'native_quality_r1' : 'native_comparison_r1'),
          recorder,
          channel: channel,
        );
        final released = Completer<void>();
        coordinator.start(
          load: () => loads++,
          release: () => released.future,
          finish: () => finishes++,
        );
        if (!quality) {
          expect(loads, 0);
          await tester.pump(const Duration(seconds: 29));
          expect(loads, 0);
          await tester.pump(const Duration(seconds: 1));
        }
        expect(loads, 1);
        coordinator.painted();
        await tester.pump(const Duration(seconds: 9));
        expect(coordinator.phase, 'hold');
        await tester.pump(const Duration(seconds: 1));
        expect(coordinator.phase, 'releasing');
        expect(finishes, 0);
        released.complete();
        await tester.pump();
        expect(coordinator.phase, quality ? 'complete' : 'post_release');
        if (!quality) {
          await tester.pump(const Duration(seconds: 29));
          expect(finishes, 0);
          await tester.pump(const Duration(seconds: 1));
        }
        expect(finishes, 1);
        expect(reads, quality ? 0 : greaterThan(0));
        final events = (await tester.runAsync(() => rows(recorder)))!;
        expect(
          events.where((row) => row['event'] == 'release_complete').length,
          1,
        );
        await tester.runAsync(coordinator.close);
      },
    );
  }
  testWidgets(
    'revocation and explicit close cancel baseline and pending late sample',
    (tester) async {
      final response = Completer<Map<String, dynamic>>();
      var reads = 0, loads = 0;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (_) {
            reads++;
            return response.future;
          });
      final configuration = config('native_comparison_r1');
      final recorder = ImageExperimentRecorder();
      final coordinator = ImageExperimentPhaseCoordinator(
        configuration,
        recorder,
        channel: channel,
      );
      coordinator.start(
        load: () => loads++,
        release: () async {},
        finish: () {},
      );
      await tester.pump();
      configuration.credentials.revoke();
      response.complete({'arbitrary': 'secret'});
      await tester.pump(const Duration(seconds: 60));
      expect(reads, 1);
      expect(loads, 0);
      expect(
        ((await tester.runAsync(
          () => rows(recorder),
        ))!).any((row) => row['event'] == 'memory_sample'),
        false,
      );
      await tester.runAsync(coordinator.close);
    },
  );
  testWidgets('timed out native read is not overlapped or retried', (
    tester,
  ) async {
    var reads = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (_) {
          reads++;
          return Completer<dynamic>().future;
        });
    final recorder = ImageExperimentRecorder();
    final coordinator = ImageExperimentPhaseCoordinator(
      config('native_comparison_r1'),
      recorder,
      channel: channel,
    );
    coordinator.start(load: () {}, release: () async {}, finish: () {});
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pump(const Duration(seconds: 2));
    expect(reads, 1);
    final sample = ((await tester.runAsync(
      () => rows(recorder),
    ))!).singleWhere((row) => row['event'] == 'memory_sample');
    expect(sample['memoryBytes'], null);
    expect(sample['memoryReason'], 'sample_timeout');
    await tester.runAsync(coordinator.close);
  });
  testWidgets(
    'watchdog begins before repository lookup and terminates unpainted load',
    (tester) async {
      final recorder = ImageExperimentRecorder();
      var finishes = 0;
      final coordinator = ImageExperimentPhaseCoordinator(
        config('native_quality_r1'),
        recorder,
        channel: channel,
      );
      coordinator.start(
        load: () {},
        release: () async {},
        finish: () => finishes++,
      );
      await tester.pump(const Duration(seconds: 10));
      coordinator.painted();
      expect(finishes, 1);
      expect(
        ((await tester.runAsync(() => rows(recorder)))!).last['reason'],
        'load_timeout',
      );
      await tester.runAsync(coordinator.close);
    },
  );
  for (final badPurpose in [false, true]) {
    testWidgets(
      'insufficient budget or mismatched purpose refuses before dispatch $badPurpose',
      (tester) async {
        final recorder = ImageExperimentRecorder();
        var loads = 0, finishes = 0;
        final coordinator = ImageExperimentPhaseCoordinator(
          config(
            'native_comparison_r1',
            purpose: badPurpose ? 'legacy' : null,
            budget: badPurpose ? 120000 : 99999,
          ),
          recorder,
          channel: channel,
        );
        coordinator.start(
          load: () => loads++,
          release: () async {},
          finish: () => finishes++,
        );
        expect(loads, 0);
        expect(finishes, 1);
        expect(
          ((await tester.runAsync(() => rows(recorder)))!).single['reason'],
          'phase_budget_refused',
        );
        await tester.runAsync(coordinator.close);
      },
    );
  }
  testWidgets('legacy batch immediately dispatches without phase sampling', (
    tester,
  ) async {
    var loads = 0;
    final recorder = ImageExperimentRecorder();
    final coordinator = ImageExperimentPhaseCoordinator(
      config('synthetic'),
      recorder,
      channel: channel,
    );
    coordinator.start(load: () => loads++, release: () async {}, finish: () {});
    expect(loads, 1);
    expect((await tester.runAsync(() => rows(recorder)))!, isEmpty);
    await tester.runAsync(coordinator.close);
  });
}
