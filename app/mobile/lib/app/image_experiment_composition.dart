import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import '../core/network/image_experiment_http_client.dart';
import '../core/diagnostics/image_experiment_recorder.dart';
import '../core/network/image_experiment_image_cache.dart';
import 'image_experiment_bootstrap.dart';
import 'image_experiment_configuration.dart';
import 'image_experiment_router.dart';
import 'image_experiment_composition.config.dart';

@InjectableInit(
  initializerName: 'initImageExperiment',
  generateForDir: [
    'lib/features/image_experiment',
    'lib/features/image_experiment/data',
  ],
  ignoreUnregisteredTypes: [ImageExperimentConfiguration],
)
GetIt _initializeImageExperiment(GetIt container) =>
    container.initImageExperiment();

class ImageExperimentComposition {
  ImageExperimentComposition._(
    this.container,
    this.bootstrap,
    this.configuration,
  );
  @visibleForTesting
  ImageExperimentComposition.forTesting(
    this.container,
    this.bootstrap,
    this.configuration,
  );
  final GetIt container;
  final ImageExperimentBootstrap bootstrap;
  final ImageExperimentConfiguration configuration;
  Future<void>? _closing;
  bool _endRequested = false;
  bool _verifiedDraw = false;
  void requestTeardown({bool verifiedDraw = false}) {
    if (_endRequested) return;
    _endRequested = true;
    _verifiedDraw = verifiedDraw;
    unawaited(
      container<ImageExperimentTransport>().close().catchError((_) {
        configuration.credentials.revoke();
      }),
    );
  }

  static Future<ImageExperimentComposition> create(
    ImageExperimentConfiguration configuration,
    ImageExperimentBootstrap bootstrap,
  ) async {
    final container = GetIt.asNewInstance();
    container.registerSingleton<ImageExperimentConfiguration>(configuration);
    _initializeImageExperiment(container);
    final result = ImageExperimentComposition._(
      container,
      bootstrap,
      configuration,
    );
    try {
      container<ImageExperimentImageCache>();
      await container<ImageExperimentTransport>().admitStatus();
      return result;
    } catch (_) {
      await result.close();
      throw const FormatException('composition_refused');
    }
  }

  Future<void> close() => _closing ??= _close();
  Future<void> _close() async {
    final recorder = container<ImageExperimentRecorder>();
    final transport = container<ImageExperimentTransport>();
    final elapsed = Stopwatch()..start();
    var failed = false;

    Future<void> attempt(
      Future<void> Function() action, {
      int maximumMilliseconds = 1000,
    }) async {
      try {
        final remaining = 15000 - elapsed.elapsedMilliseconds;
        final budget = remaining < maximumMilliseconds
            ? remaining
            : maximumMilliseconds;
        await action().timeout(Duration(milliseconds: budget > 0 ? budget : 1));
      } catch (_) {
        failed = true;
        recorder.record({
          'event': 'failure',
          'reason': 'cleanup_unavailable',
          'generation': configuration.generation,
          'ownership': 'unavailable',
        });
      }
    }

    await attempt(transport.close, maximumMilliseconds: 2000);
    configuration.credentials.revoke();
    await attempt(() async {
      container<ImageExperimentRouter>().dispose();
    });
    await attempt(
      () => container<ImageExperimentImageCache>().close(),
      maximumMilliseconds: 7000,
    );
    await attempt(configuration.credentials.close);
    await attempt(() => container.reset());
    var ledgerStored = false;
    await attempt(() async {
      final ledger = await recorder.export();
      ledgerStored = await bootstrap.storeLedger(ledger);
      if (!ledgerStored) {
        throw const FormatException('cleanup_unavailable');
      }
    }, maximumMilliseconds: 2000);
    await attempt(() async {
      final result =
          _verifiedDraw &&
              transport.authenticatedRevokeObserved &&
              ledgerStored &&
              !failed
          ? 'settled'
          : 'refused';
      if (!await bootstrap.revoke(result: result)) {
        throw const FormatException('cleanup_unavailable');
      }
    }, maximumMilliseconds: 2000);
    if (failed) throw StateError('cleanup_unavailable');
  }
}

class ImageExperimentResolver extends InheritedWidget {
  const ImageExperimentResolver({
    super.key,
    required this.composition,
    required super.child,
  });
  final ImageExperimentComposition composition;
  T get<T extends Object>() => composition.container<T>();
  static ImageExperimentResolver of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<ImageExperimentResolver>()!;
  @override
  bool updateShouldNotify(ImageExperimentResolver oldWidget) =>
      composition != oldWidget.composition;
}
