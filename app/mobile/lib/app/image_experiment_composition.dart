import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import '../core/network/image_experiment_http_client.dart';
import '../core/diagnostics/image_experiment_recorder.dart';
import '../core/network/image_experiment_image_cache.dart';
import 'image_experiment_bootstrap.dart';
import 'image_experiment_phase_coordinator.dart';
import '../features/image_experiment/presentation/cubit/raster_case_cubit.dart';
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
  late final phases = ImageExperimentPhaseCoordinator(
    configuration,
    container<ImageExperimentRecorder>(),
  );
  Map<String, Object?>? _pendingQualityBinding;
  Future<bool>? _qualityPublication;
  Future<void>? _qualityInvalidation;
  final _targetReleases = <Future<void>>[];
  void ownTargetRelease(Future<void> release) {
    _targetReleases.add(release);
  }

  void startCase(RasterCaseCubit cubit, String caseId) {
    if (phases.enabled && phases.quality) {
      cubit.retireBeforeRefusal(invalidateQualityBinding);
    }
    phases.start(
      beforeRelease: invalidateQualityBinding,
      load: () {
        container<ImageExperimentRecorder>().record({
          'event': 'dispatch',
          'generation': configuration.generation,
          'sessionSha256': ImageExperimentRecorder.hash(
            configuration.sessionId,
          ),
          'caseSha256': ImageExperimentRecorder.hash(caseId),
        });
        unawaited(cubit.load(caseId));
      },
      release: () async {
        await invalidateQualityBinding();
        cubit.releaseTarget();
        await WidgetsBinding.instance.endOfFrame;
        if (_targetReleases.isEmpty) throw StateError('cleanup_unavailable');
        await Future.wait(_targetReleases);
      },
      finish: () => requestTeardown(),
    );
  }

  void targetPainted() {
    _verifiedDraw = true;
    if (phases.enabled) {
      phases.painted();
      if (phases.quality &&
          phases.phase == 'hold' &&
          _pendingQualityBinding != null) {
        _qualityPublication = bootstrap.publishQualityBinding({
          ..._pendingQualityBinding!,
          'sessionSha256': ImageExperimentRecorder.hash(
            configuration.sessionId,
          ),
        });
        unawaited(
          _qualityPublication!.then((stored) {
            if (!stored && !_endRequested && _closing == null) {
              container<ImageExperimentRecorder>().record({
                'event': 'failure',
                'reason': 'layout_unavailable',
                'generation': configuration.generation,
              });
              requestTeardown();
            }
          }),
        );
      }
      return;
    }
    requestTeardown(verifiedDraw: true);
  }

  void publishQualityBinding(Map<String, Object?> value) {
    if (phases.enabled &&
        phases.quality &&
        phases.active &&
        phases.phase == 'load') {
      _pendingQualityBinding = Map.unmodifiable(value);
    }
  }

  Future<void> invalidateQualityBinding() {
    if (!phases.enabled || !phases.quality) return Future.value();
    return _qualityInvalidation ??= _invalidateQualityBinding();
  }

  Future<void> _invalidateQualityBinding() async {
    _pendingQualityBinding = null;
    await _qualityPublication;
    if (!await bootstrap.clearQualityBinding()) {
      throw StateError('cleanup_unavailable');
    }
  }

  bool _endRequested = false;
  bool _verifiedDraw = false;
  void requestTeardown({bool verifiedDraw = false}) {
    if (_endRequested) return;
    _endRequested = true;
    _verifiedDraw = _verifiedDraw || verifiedDraw;
    phases.cancel();
    unawaited(
      () async {
        try {
          await invalidateQualityBinding();
        } catch (_) {
          container<ImageExperimentRecorder>().record({
            'event': 'failure',
            'reason': 'cleanup_unavailable',
            'generation': configuration.generation,
          });
        }
        await container<ImageExperimentTransport>().close();
      }().catchError((_) {
        configuration.credentials.revoke();
      }),
    );
  }

  static Future<ImageExperimentComposition> create(
    ImageExperimentConfiguration configuration,
    ImageExperimentBootstrap bootstrap, {
    ImageExperimentRefusalObserver? observeRefusal,
  }) => runZoned(
    () async {
      final observer = ImageExperimentRefusalObservation.current(
        observeRefusal,
      );
      final container = ImageExperimentRefusalObservation.construct(
        ImageExperimentFixtureStage.compositionRegistration,
        () {
          final container = GetIt.asNewInstance();
          container.registerSingleton<ImageExperimentConfiguration>(
            configuration,
          );
          _initializeImageExperiment(container);
          return container;
        },
        observer: observer,
      );
      final result = ImageExperimentComposition._(
        container,
        bootstrap,
        configuration,
      );
      var stage = ImageExperimentFixtureStage.compositionCache;
      try {
        ImageExperimentRefusalObservation.construct(
          stage,
          () => container<ImageExperimentImageCache>(),
          observer: observer,
        );
        stage = ImageExperimentFixtureStage.compositionStatus;
        await container<ImageExperimentTransport>().admitStatus();
        return result;
      } catch (_) {
        ImageExperimentRefusalObservation.report(stage, observer);
        await result.close();
        throw const FormatException('composition_refused');
      }
    },
    zoneValues: {
      ImageExperimentRefusalObservation.zoneKey:
          ImageExperimentRefusalObservation.current(observeRefusal),
    },
  );

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

    await attempt(invalidateQualityBinding);
    await attempt(phases.close);
    await attempt(transport.close, maximumMilliseconds: 2000);
    configuration.credentials.revoke();
    await attempt(() async {
      container<ImageExperimentRouter>().dispose();
    });
    await attempt(() async {
      await Future.wait(_targetReleases);
    }, maximumMilliseconds: 2000);
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
              (!phases.enabled || phases.phase == 'complete') &&
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
