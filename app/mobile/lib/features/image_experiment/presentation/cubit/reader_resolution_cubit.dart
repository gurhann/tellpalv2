import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../../../../core/diagnostics/image_experiment_recorder.dart';
import '../../../../story_reader_images.dart';
import '../view/reader_resolution_images.dart';
import 'reader_resolution_state.dart';
import 'reader_resolution_observation.dart';

/// Owns one finite reader session; native samples stay serial and use one clock.
@injectable
class ReaderResolutionCubit extends Cubit<ReaderResolutionState> {
  ReaderResolutionCubit(this.configuration, this.images, this.recorder)
    : super(const ReaderResolutionState()) {
    _revocation = configuration.credentials.revocations.listen(
      (_) => refuse('session_inactive'),
    );
    images.onFailure = () => refuse('layout_unavailable');
  }
  final ImageExperimentConfiguration configuration;
  final ReaderResolutionImages images;
  final ImageExperimentRecorder recorder;
  final controller = StoryReaderController();
  final _timers = <Timer>[];
  StreamSubscription<void>? _revocation;
  bool _started = false, _ended = false;
  void Function(bool)? _finish;
  Future<bool> Function(Map<String, Object?>)? _publishQuality;
  Future<bool> Function()? _clearQuality;
  Future<void> _qualityWork = Future.value();
  Future<void>? _closing;
  late final observation = ReaderResolutionObservation(
    configuration,
    recorder,
    () => active,
  );
  bool get active => !_ended && !isClosed && configuration.credentials.isActive;
  Map<String, Object?> get _binding => images.binding;

  void start({
    required void Function(bool) finish,
    Future<bool> Function(Map<String, Object?>)? publishQuality,
    Future<bool> Function()? clearQuality,
  }) {
    if (_started) return;
    _started = true;
    _finish = finish;
    _publishQuality = publishQuality;
    _clearQuality = clearQuality;
    final remaining = configuration.credentials.expiresAt.difference(
      DateTime.now(),
    );
    if (!configuration.isReader ||
        configuration.mode != 'release' ||
        !active ||
        remaining <=
            Duration(seconds: configuration.readerQuality ? 25 : 140)) {
      refuse('phase_budget_refused');
      return;
    }
    _timers.add(
      Timer(
        Duration(seconds: configuration.readerQuality ? 20 : 140),
        () => refuse('reader_incomplete'),
      ),
    );
    if (configuration.readerQuality) {
      _beginReader();
      observation.start();
      return;
    }
    _setPhase('baseline');
    observation.start();
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.baseline));
    _later(const Duration(seconds: 20), _beginReader);
  }

  void _later(Duration duration, void Function() work) => _timers.add(
    Timer(duration, () {
      if (active) work();
    }),
  );
  void _beginReader() {
    if (!active) return;
    _setPhase('reader');
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.reading));
    if (configuration.readerQuality) {
      _later(const Duration(seconds: 8), () => unawaited(_qualitySelect(1)));
      _later(const Duration(seconds: 16), () => unawaited(_qualityComplete()));
      return;
    }
    for (var index = 1; index < 6; index++) {
      final page = index;
      _later(Duration(seconds: index * 4), () => controller.selectPage(page));
    }
    for (var step = 0; step < 5; step++) {
      final page = 4 - step;
      _later(
        Duration(seconds: 24 + step * 3),
        () => controller.selectPage(page),
      );
    }
    for (var step = 0; step < 6; step++) {
      final page = const [5, 0, 4, 1, 3, 2][step];
      _later(
        Duration(milliseconds: 40000 + step * 100),
        () => controller.selectPage(page),
      );
    }
    _later(const Duration(seconds: 43), () => controller.selectPage(5));
    _later(
      const Duration(seconds: 15),
      () => unawaited(observation.background()),
    );
    _later(const Duration(seconds: 52), () => unawaited(_naturalExit()));
  }

  void pagePainted(Map<String, Object?> value) {
    if (!active || !configuration.readerQuality) return;
    _qualityWork = _qualityWork.then((_) async {
      if (!active ||
          !await (_publishQuality?.call({
                ...value,
                'sessionSha256': ImageExperimentRecorder.hash(
                  configuration.sessionId,
                ),
              }) ??
              Future.value(false))) {
        refuse('layout_unavailable');
      }
    });
  }

  Future<void> _qualitySelect(int page) async {
    await _qualityWork;
    if (!active) return;
    if (!await (_clearQuality?.call() ?? Future.value(false))) {
      refuse('cleanup_unavailable');
      return;
    }
    if (active) controller.selectPage(page);
  }

  Future<void> retireQualityBinding() async {
    if (!configuration.readerQuality || _clearQuality == null) return;
    await _qualityWork;
    if (!await _clearQuality!()) throw StateError('cleanup_unavailable');
  }

  Future<void> _qualityComplete() async {
    await _qualityWork;
    if (!active) return;
    if (!images.paintedPages.containsAll([1, 2]) ||
        !await (_clearQuality?.call() ?? Future.value(false))) {
      refuse('reader_incomplete');
      return;
    }
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.evicted));
    await WidgetsBinding.instance.endOfFrame;
    await images.evict();
    _complete();
  }

  Future<void> _naturalExit() async {
    if (!active) return;
    if (images.paintedPages.length != 6) {
      refuse('reader_incomplete');
      return;
    }
    await observation.lifecycle();
    _setPhase('natural_retention');
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.natural));
    await WidgetsBinding.instance.endOfFrame;
    if (!active) return;
    await images.snapshot('natural_retention');
    _later(const Duration(seconds: 30), () => unawaited(_evict()));
  }

  Future<void> _evict() async {
    if (!active) return;
    try {
      await images.snapshot('natural_retention');
      _setPhase('owned_eviction');
      await images.evict().timeout(const Duration(seconds: 10));
      if (!active) return;
      recorder.record({
        ..._binding,
        'event': 'release_complete',
        'ownership': 'released',
      });
      await images.snapshot('owned_eviction');
      emit(const ReaderResolutionState(status: ReaderResolutionStatus.evicted));
      _later(const Duration(seconds: 10), _complete);
    } catch (_) {
      refuse('cleanup_unavailable');
    }
  }

  void _complete() {
    if (!active) return;
    _setPhase('complete');
    _ended = true;
    _cancelTimers();
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.complete));
    _finish?.call(true);
  }

  void refuse(String reason) {
    if (_ended || isClosed) return;
    recorder.record({..._binding, 'event': 'failure', 'reason': reason});
    _ended = true;
    _cancelTimers();
    images.cancel();
    emit(const ReaderResolutionState(status: ReaderResolutionStatus.refused));
    _finish?.call(false);
  }

  void _setPhase(String phase) {
    observation.phase(phase);
    recorder.record({..._binding, 'event': 'phase', 'phase': phase});
  }

  void _cancelTimers() {
    for (final timer in _timers) {
      timer.cancel();
    }
    observation.close();
  }

  // The coalesced owner below calls BlocBase.close after its dependencies settle.
  @override
  // ignore: must_call_super
  Future<void> close() => _closing ??= _close();
  Future<void> _close() async {
    _ended = true;
    _cancelTimers();
    Object? failure;
    StackTrace? failureStack;
    Future<void> settle(Future<void> Function() action) async {
      try {
        await action();
      } catch (error, stack) {
        failure ??= error;
        failureStack ??= stack;
      }
    }

    await settle(retireQualityBinding);
    await settle(() async => _revocation?.cancel());
    await settle(images.close);
    await settle(super.close);
    if (failure != null) Error.throwWithStackTrace(failure!, failureStack!);
  }
}
