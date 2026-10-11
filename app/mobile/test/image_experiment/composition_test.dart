import 'dart:async';
import 'package:file/memory.dart';
import 'package:file/file.dart' as files;
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:get_it/get_it.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_app.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_bootstrap.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_composition.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_composition.config.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/domain/entity/raster_case.dart';
import 'fixture.dart';

class _Files implements FileSystem {
  final fs = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => fs.file('/$name');
}

class _Transport extends ImageExperimentTransport {
  _Transport(super.configuration, super.recorder);
  @override
  bool get authenticatedRevokeObserved => true;
  @override
  Future<void> close() async {
    configuration.credentials.revoke();
    await super.close();
  }
}

class _Cache extends ImageExperimentImageCache {
  _Cache(super.configuration, super.transport, super.recorder)
    : super(
        storageConfiguration: Config(
          'synthetic-app',
          repo: NonStoringObjectProvider(),
          fileSystem: _Files(),
        ),
      );
  int releases = 0, closes = 0;
  bool failClose = false;
  bool stallClose = false;
  @override
  ImageExperimentProvider provider(RasterCase fixture, String attempt) =>
      ImageExperimentProvider(
        MemoryImage(fixtureBytes),
        'a' * 64,
        'attempt-$attempt',
        'b' * 64,
        fixture,
      );
  @override
  bool verified(ImageExperimentProvider value) => true;
  @override
  Future<void> remember(
    ImageExperimentProvider value,
    bool Function() current,
  ) async {}
  @override
  Future<void> release(
    ImageExperimentProvider value, {
    bool failed = false,
  }) async {
    releases++;
  }

  Future<void> closeOwned() => super.close();

  @override
  Future<void> close() async {
    closes++;
    if (stallClose) return Completer<void>().future;
    await super.close();
    if (failClose) throw StateError('synthetic');
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('synthetic-composition');
  tearDown(
    () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null),
  );
  ({
    ImageExperimentComposition composition,
    _Cache cache,
    GetIt container,
    ImageExperimentRecorder recorder,
  })
  setup({
    bool failNative = false,
    bool failCache = false,
    bool stallCache = false,
    bool confirmedRemote = false,
  }) {
    final configuration = FixtureWire().configuration();
    final container = GetIt.asNewInstance();
    container.registerSingleton<ImageExperimentConfiguration>(configuration);
    container.initImageExperiment();
    container.unregister<ImageExperimentImageCache>();
    final recorder = container<ImageExperimentRecorder>();
    if (confirmedRemote) {
      container.unregister<ImageExperimentTransport>();
      container.registerSingleton<ImageExperimentTransport>(
        _Transport(configuration, recorder),
      );
    }
    final cache =
        _Cache(configuration, container<ImageExperimentTransport>(), recorder)
          ..failClose = failCache
          ..stallClose = stallCache;
    container.registerSingleton<ImageExperimentImageCache>(cache);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'storeLedger') return true;
          if (failNative) throw PlatformException(code: 'synthetic-only');
          return null;
        });
    return (
      composition: ImageExperimentComposition.forTesting(
        container,
        ImageExperimentBootstrap(channel: channel),
        configuration,
      ),
      cache: cache,
      container: container,
      recorder: recorder,
    );
  }

  for (final draw in [false, true]) {
    test(
      'native settled result requires verified current draw and successful owner cleanup draw=$draw',
      () async {
        final value = setup(confirmedRemote: true);
        String? result;
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (call) async {
              if (call.method == 'storeLedger') return true;
              result = (call.arguments as Map)['result'] as String;
              return null;
            });
        value.composition.requestTeardown(verifiedDraw: draw);
        await value.composition.close();
        expect(result, draw ? 'settled' : 'refused');
        expect(
          value.container.isRegistered<ImageExperimentConfiguration>(),
          false,
        );
      },
    );
  }
  testWidgets(
    'revocation removes fixture route before owned container teardown',
    (tester) async {
      final value = (await tester.runAsync(() async => setup()))!;
      await tester.pumpWidget(
        ImageExperimentApp(composition: value.composition),
      );
      await tester.pumpAndSettle();
      expect(find.text('TellPal Image Experiment'), findsOneWidget);
      value.composition.configuration.credentials.revoke();
      await tester.pumpAndSettle();
      expect(find.text('Experiment entry refused'), findsOneWidget);
      expect(value.cache.releases, 1);
      var closed = false;
      final closing = value.composition.close().then((_) => closed = true);
      for (var step = 0; step < 20 && !closed; step++) {
        await tester.runAsync(() async {
          await Future<void>.delayed(const Duration(milliseconds: 20));
        });
        await tester.pump(const Duration(milliseconds: 20));
      }
      expect(closed, true);
      await closing;
      expect(value.cache.closes, 1);
      expect(
        value.container.isRegistered<ImageExperimentConfiguration>(),
        false,
      );
      expect(tester.takeException(), null);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'revoked before mounting renders refusal and releases all owners',
    (tester) async {
      final value = (await tester.runAsync(() async => setup()))!;
      value.composition.configuration.credentials.revoke();
      String? outcome;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'storeLedger') return true;
            outcome = (call.arguments as Map)['result'] as String;
            return null;
          });
      await tester.pumpWidget(
        ImageExperimentApp(composition: value.composition),
      );
      for (var step = 0; step < 30 && outcome == null; step++) {
        await tester.runAsync(() async {
          await Future<void>.delayed(const Duration(milliseconds: 10));
        });
        await tester.pump(const Duration(milliseconds: 20));
      }
      expect(find.text('Experiment entry refused'), findsOneWidget);
      expect(find.text('TellPal Image Experiment'), findsNothing);
      expect(value.cache.releases, 0);
      expect(value.cache.closes, 1);
      expect(outcome, 'refused');
      expect(value.container.isRegistered<ImageExperimentImageCache>(), false);
      expect(
        value.container.isRegistered<ImageExperimentConfiguration>(),
        false,
      );
      expect(tester.takeException(), null);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'never completing cache cannot prevent native revoke or container release',
    (tester) async {
      final value = (await tester.runAsync(
        () async => setup(stallCache: true),
      ))!;
      var revokes = 0, storedFailure = false;
      String? outcome;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'revoke') {
              revokes++;
              outcome = (call.arguments as Map)['result'] as String;
            }
            if (call.method == 'storeLedger') {
              storedFailure = (call.arguments as String).contains(
                'cleanup_unavailable',
              );
              return true;
            }
            return null;
          });
      value.composition.configuration.credentials.revoke();
      var finished = false;
      Object? failure;
      final closing = value.composition.close().then<void>(
        (_) {
          finished = true;
        },
        onError: (Object error) {
          failure = error;
          finished = true;
        },
      );
      for (var step = 0; step < 40 && !finished; step++) {
        await tester.runAsync(() async {
          await Future<void>.delayed(const Duration(milliseconds: 10));
        });
        await tester.pump(const Duration(milliseconds: 500));
      }
      expect(finished, true);
      await closing;
      expect(failure, isStateError);
      expect(revokes, 1);
      expect(storedFailure, true);
      expect(outcome, 'refused');
      expect(
        value.container.isRegistered<ImageExperimentConfiguration>(),
        false,
      );
      await tester.runAsync(value.cache.closeOwned);
    },
  );
  test(
    'failed native revoke and cache close still release container and retain fixed failure',
    () async {
      final value = setup(failNative: true, failCache: true);
      value.composition.configuration.credentials.revoke();
      final first = value.composition.close();
      final second = value.composition.close();
      expect(identical(first, second), true);
      await expectLater(first, throwsStateError);
      expect(value.cache.closes, 1);
      expect(
        value.container.isRegistered<ImageExperimentConfiguration>(),
        false,
      );
      final records =
          jsonDecode(await value.recorder.export())['records'] as List;
      expect(records.any((e) => e['reason'] == 'cleanup_unavailable'), true);
    },
  );
}
