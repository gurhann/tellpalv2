import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../p5d_offline/domain/p5d_pins.dart';
import '../../p5d_offline/presentation/p5d_frame_timing.dart';
import '../../p5e_offline/presentation/p5e_cubit.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import '../domain/p5f_plan.dart';
import '../domain/p5f_repository.dart';
import 'p5f_state.dart';

typedef P5fNative =
    Future<Map<String, Object?>> Function(
      String method,
      Map<String, Object?> values,
    );

/// Tek seri raster sahibi; geç decode sonucu ve başarısız oturum ayrıca temizlenir.
class P5fCubit extends Cubit<P5fState> {
  P5fCubit(P5fRepository repository, P5fNative native)
    : this._(
        repository,
        native,
        ReadingComparisonRaster.decode,
        const Duration(seconds: 10),
        const Duration(minutes: 10),
        P5dFrameTimings.new,
      );
  P5fCubit._(
    this.repository,
    this.native,
    this._decode,
    this.trialLimit,
    this.sessionLimit,
    this._createTimings,
  ) : super(const P5fState());
  @visibleForTesting
  factory P5fCubit.testing(
    P5fRepository repository,
    P5fNative native, {
    P5eDecoder? decode,
    P5dFrameTimings Function()? createTimings,
    Duration trialLimit = const Duration(seconds: 10),
    Duration sessionLimit = const Duration(minutes: 10),
  }) {
    if (kReleaseMode ||
        trialLimit <= Duration.zero ||
        trialLimit > const Duration(seconds: 10) ||
        sessionLimit <= Duration.zero ||
        sessionLimit > const Duration(minutes: 10)) {
      throw StateError('test_configuration_refused');
    }
    return P5fCubit._(
      repository,
      native,
      decode ?? ReadingComparisonRaster.decode,
      trialLimit,
      sessionLimit,
      createTimings ?? P5dFrameTimings.new,
    );
  }
  final P5fRepository repository;
  final P5fNative native;
  final P5eDecoder _decode;
  final P5dFrameTimings Function() _createTimings;
  final Duration trialLimit, sessionLimit;
  final _sessionClock = Stopwatch();
  Stopwatch? _drawClock;
  ReadingComparisonCancellation? _cancel;
  Completer<Map<String, Object?>>? _paint;
  P5dFrameTimings? _frames;
  Future<void>? _run;
  bool _valid = true, _ready = false, _running = false;
  int _epoch = 0;

  Future<void> initialize() async {
    try {
      final result = await repository.initialize().timeout(
        const Duration(seconds: 10),
      );
      if (!_valid || isClosed) return;
      result.fold((failure) => interrupt(failure.code), (_) {
        _ready = true;
        emit(state.copyWith(status: 'ready'));
      });
    } catch (_) {
      interrupt('preflight_refused');
    }
  }

  Future<void> start() => _run ??= _execute();
  Future<void> _execute() async {
    if (!_ready || !_valid || isClosed) return;
    _running = true;
    _sessionClock.start();
    _frames = _createTimings();
    final timer = Timer(sessionLimit, () => interrupt('session_deadline'));
    try {
      await native('begin', {}).timeout(trialLimit);
      for (final trial in p5fPlan()) {
        _check();
        await _trial(trial);
      }
      _check();
      _assertReleased();
      await native('seal', {
        'completed': 144,
        'warmups': 12,
        'cleanup': true,
      }).timeout(trialLimit);
      _check();
      _running = false;
      emit(state.copyWith(status: 'finished', raster: null));
    } catch (_) {
      interrupt('trial_or_native_refused');
      try {
        await native('abort', {
          'reason': state.error ?? 'aborted',
          'cleanup': _released(),
        }).timeout(trialLimit);
      } catch (_) {
        /* Başarı üretilemez. */
      }
    } finally {
      timer.cancel();
      _running = false;
      _frames?.close();
      _frames = null;
    }
  }

  Future<void> _trial(P5fTrial trial) async {
    final clock = Stopwatch()..start();
    final cancellation = ReadingComparisonCancellation();
    _cancel = cancellation;
    _epoch++;
    final epoch = _epoch;
    ReadingComparisonRaster? lateOwner;
    var expired = false;
    Future<T> bounded<T>(Future<T> value, {void Function(T)? disposeLate}) =>
        value
            .then((result) {
              if (expired || !_valid) {
                disposeLate?.call(result);
                throw StateError('cancelled');
              }
              return result;
            })
            .timeout(
              trialLimit - clock.elapsed,
              onTimeout: () {
                expired = true;
                cancellation.cancel();
                throw TimeoutException('trial_deadline');
              },
            );
    try {
      await bounded(native('beginTrial', {...trial.toJson(), 'epoch': epoch}));
      final encoded = (await bounded(
        repository.read(trial.index),
      )).fold((f) => throw StateError(f.code), (v) => v);
      _check();
      final pin = p5dPins[trial.index];
      final decodeClock = Stopwatch()..start();
      lateOwner = await bounded<ReadingComparisonRaster>(
        _decode(
          encoded.bytes,
          ReadingImageRaster(
            provider: 'offline',
            objectKey: pin['asset']! as String,
            generation: 'p5c',
            mime: 'image/${(pin['format']! as String).toLowerCase()}',
            byteSize: encoded.bytes.length,
            sha256: pin['sha256']! as String,
            width: 1712,
            height: 2778,
          ),
          1712,
          2778,
          p5eLimits,
          cancellation,
        ),
        disposeLate: (r) => r.dispose(),
      );
      final decodeMicros = decodeClock.elapsedMicroseconds;
      _check();
      await bounded(native('loaded', {'epoch': epoch}));
      _paint = Completer<Map<String, Object?>>();
      _drawClock = Stopwatch()..start();
      emit(
        state.copyWith(
          status: 'loaded',
          index: trial.index,
          epoch: epoch,
          raster: lateOwner,
        ),
      );
      lateOwner = null;
      final proof = await bounded(_paint!.future);
      final drawMicros = proof['firstDrawMicros']! as int;
      ui.FrameTiming? frame;
      while (frame == null) {
        _check();
        frame = _frames!.matching(proof, epoch, trial.index);
        if (frame == null) {
          await bounded(Future<void>.delayed(const Duration(milliseconds: 5)));
        }
      }
      state.raster?.dispose();
      emit(state.copyWith(status: 'running', raster: null));
      _assertReleased();
      final memory = await bounded(native('finishTrial', {'epoch': epoch}));
      _check();
      final row = {
        ...trial.toJson(),
        'epoch': epoch,
        'case': pin,
        'readMicros': encoded.readMicros,
        'decodeMicros': decodeMicros,
        'firstDrawMicros': drawMicros,
        'decodeDrawMicros': decodeMicros + drawMicros,
        'rasterMicros': frame.rasterDuration.inMicroseconds,
        'buildMicros': frame.buildDuration.inMicroseconds,
        'trialMicros': clock.elapsedMicroseconds,
        'proof': proof,
        'timingFrameNumber': frame.frameNumber,
        'memory': memory,
        'cleanup': true,
      };
      await native('recordTrial', row).timeout(trialLimit - clock.elapsed);
      if (!trial.warmup) emit(state.copyWith(completed: state.completed + 1));
    } finally {
      cancellation.cancel();
      if (identical(_cancel, cancellation)) _cancel = null;
      lateOwner?.dispose();
      state.raster?.dispose();
      _paint = null;
      _drawClock = null;
    }
  }

  void painted(Map<String, Object?> proof) {
    if (!_valid ||
        isClosed ||
        state.status != 'loaded' ||
        proof['epoch'] != _epoch ||
        proof['index'] != state.index ||
        _paint == null ||
        _paint!.isCompleted) {
      return;
    }
    _paint!.complete({
      ...proof,
      'firstDrawMicros': _drawClock!.elapsedMicroseconds,
    });
  }

  void interrupt(String reason) {
    if (!_valid || isClosed || state.status == 'finished') return;
    _valid = false;
    _cancel?.cancel();
    state.raster?.dispose();
    if (_paint != null && !_paint!.isCompleted) {
      _paint!.completeError(StateError(reason));
    }
    emit(state.copyWith(status: 'error', error: reason, raster: null));
  }

  void network(String value) {
    if (_running && value != 'unsatisfied') interrupt('network_interrupted');
  }

  void lifecycle(bool active) {
    if (_running && !active) interrupt('background_interrupted');
  }

  void _check() {
    if (!_valid || isClosed || _sessionClock.elapsed >= sessionLimit) {
      throw StateError('session_invalid');
    }
    _cancel?.check();
  }

  bool _released() =>
      ReadingComparisonRaster.activeImages == 0 &&
      ReadingComparisonRaster.activeBuffers == 0 &&
      ReadingComparisonRaster.activeCodecs == 0 &&
      ReadingComparisonRaster.activeDescriptors == 0 &&
      ReadingComparisonRaster.reservedSlots == 0;
  void _assertReleased() {
    if (!_released()) throw StateError('raster_ownership_refused');
  }

  @override
  Future<void> close() async {
    interrupt('closed');
    await _run;
    await super.close();
  }
}
