import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import '../../p5d_offline/domain/p5d_pins.dart';
import '../../p5d_offline/domain/p5d_repository.dart';
import 'p5e_state.dart';

typedef P5eRecorder = Future<void> Function(Map<String, Object?> entry);
typedef P5eDecoder =
    Future<ReadingComparisonRaster> Function(
      Uint8List bytes,
      ReadingImageRaster metadata,
      int width,
      int height,
      ReadingComparisonLimits limits,
      ReadingComparisonCancellation cancellation,
    );

/// Epoch iptali ve seri kuyruk, eski seçimin yeni ekrana ulaşmasını engeller.
class P5eCubit extends Cubit<P5eState> {
  P5eCubit(P5dRepository repository, P5eRecorder record)
    : this._(
        repository,
        record,
        Duration(milliseconds: p5eLimits.decodeMillis),
        ReadingComparisonRaster.decode,
      );
  P5eCubit._(this.repository, this.record, this._deadline, this._decode)
    : super(const P5eState());

  @visibleForTesting
  factory P5eCubit.testing(
    P5dRepository repository,
    P5eRecorder record, {
    required Duration deadline,
    P5eDecoder? decode,
  }) {
    if (kReleaseMode ||
        deadline <= Duration.zero ||
        deadline > Duration(milliseconds: p5eLimits.decodeMillis)) {
      throw StateError('test_configuration_refused');
    }
    return P5eCubit._(
      repository,
      record,
      deadline,
      decode ?? ReadingComparisonRaster.decode,
    );
  }
  final P5dRepository repository;
  final P5eRecorder record;
  final Duration _deadline;
  final P5eDecoder _decode;
  Future<void> _tail = Future.value();
  Future<void>? _sealing;
  ReadingComparisonCancellation? _cancel;
  bool _ready = false, _active = true, _terminal = false;
  int _epoch = 0;
  final _seen = <String>{};

  Future<void> initialize() async {
    if (isClosed || _terminal) return;
    try {
      final result = await _bounded(repository.initialize());
      if (isClosed || _terminal) return;
      result.fold((f) => _fail(f.code), (_) {
        _ready = true;
        emit(state.copyWith(status: 'blocked'));
      });
    } catch (_) {
      if (!isClosed && !_terminal) _fail('manifest_deadline_or_read_refused');
    }
  }

  /// Süre sınırı SDK işini fiziksel olarak iptal etmez; geç raster ayrıca bırakılır.
  Future<T> _bounded<T>(
    Future<T> operation, {
    ReadingComparisonCancellation? cancellation,
    void Function(T)? disposeLate,
  }) {
    var expired = false;
    final tracked = operation.then((value) {
      if (expired) disposeLate?.call(value);
      return value;
    });
    return tracked.timeout(
      _deadline,
      onTimeout: () {
        expired = true;
        cancellation?.cancel();
        throw TimeoutException('p5e_operation_deadline');
      },
    );
  }

  void network(String value) {
    if (isClosed) return;
    if (![
      'unknown',
      'satisfied',
      'unsatisfied',
      'requiresConnection',
    ].contains(value)) {
      value = 'unknown';
    }
    if (state.status == 'sealing' &&
        value == 'unsatisfied' &&
        state.network == value) {
      return;
    }
    _invalidate();
    if (_seen.isNotEmpty &&
        value != 'unsatisfied' &&
        state.status != 'finished') {
      _fail('offline_session_interrupted');
      return;
    }
    if (!_ready && state.status == 'error') return;
    if (state.status == 'finished') return;
    emit(state.copyWith(network: value, status: 'blocked', raster: null));
    if (value == 'unsatisfied' && _active && _ready) {
      unawaited(select(state.index, crop: state.crop));
    }
  }

  Future<void> lifecycle(bool active) async {
    _active = active;
    _invalidate();
    if (!active && _seen.isNotEmpty && state.status != 'finished') {
      _fail('background_session_interrupted');
      await _tail;
      return;
    }
    if (state.status == 'error' || state.status == 'finished') return;
    if (!isClosed) {
      emit(state.copyWith(status: 'blocked', network: 'unknown', raster: null));
    }
    await _tail;
  }

  Future<void> select(int index, {bool? crop}) {
    if (isClosed ||
        _terminal ||
        _sealing != null ||
        !_ready ||
        !_active ||
        state.network != 'unsatisfied') {
      return Future.value();
    }
    if (index < 0 || index >= p5dPins.length) {
      _fail('selection_refused');
      return Future.value();
    }
    _invalidate();
    final epoch = _epoch;
    final cancellation = ReadingComparisonCancellation();
    _cancel = cancellation;
    final cropMode = crop ?? state.crop;
    emit(
      state.copyWith(
        status: 'loading',
        index: index,
        crop: cropMode,
        epoch: epoch,
        raster: null,
        error: null,
      ),
    );
    _tail = _tail.then((_) => _load(index, cropMode, epoch, cancellation));
    return _tail;
  }

  Future<void> _load(
    int index,
    bool crop,
    int epoch,
    ReadingComparisonCancellation cancellation,
  ) async {
    ReadingComparisonRaster? owned;
    try {
      cancellation.check();
      final encoded = (await _bounded(
        repository.read(index),
        cancellation: cancellation,
      )).fold((f) => throw StateError(f.code), (value) => value);
      cancellation.check();
      final pin = p5dPins[index];
      owned = await _bounded<ReadingComparisonRaster>(
        _decode(
          encoded.bytes,
          ReadingImageRaster(
            provider: 'offline',
            objectKey: pin['asset']! as String,
            generation: 'p5c',
            mime: 'image/${(pin['format']! as String).toLowerCase()}',
            byteSize: encoded.bytes.length,
            sha256: pin['sha256']! as String,
            width: pin['width']! as int,
            height: pin['height']! as int,
          ),
          1712,
          2778,
          p5eLimits,
          cancellation,
        ),
        cancellation: cancellation,
        disposeLate: (raster) => raster.dispose(),
      );
      cancellation.check();
      if (!_current(epoch)) return;
      emit(state.copyWith(status: 'loaded', raster: owned, crop: crop));
      owned = null;
    } catch (error) {
      if (error is TimeoutException && !isClosed && !_terminal) {
        _fail('decode_or_asset_deadline');
      } else if (_current(epoch)) {
        _fail('decode_or_asset_refused');
      }
    } finally {
      owned?.dispose();
    }
  }

  Future<void> presented(int epoch, Map<String, Object?> geometry) async {
    if (!_current(epoch) ||
        state.status != 'loaded' ||
        state.raster?.image == null) {
      return;
    }
    final viewKey = '${state.index}:${state.crop}';
    try {
      await _bounded(
        record({
          'event': 'presentation',
          'epoch': epoch,
          'case': p5dPins[state.index],
          'crop': state.crop,
          'network': state.network,
          'geometry': geometry,
          'decoderColorSpace': state.raster!.image!.colorSpace.name,
          'decodedWidth': state.raster!.image!.width,
          'decodedHeight': state.raster!.image!.height,
        }),
      );
      if (!_current(epoch)) return;
      _seen.add(viewKey);
      if (!isClosed) emit(state.copyWith(visitedViews: _seen.length));
    } catch (_) {
      if (_current(epoch)) _fail('session_record_refused');
    }
  }

  Future<void> finish() {
    final pending = _sealing;
    if (pending != null) return pending;
    if (!_current(state.epoch) ||
        state.status != 'loaded' ||
        _seen.length != 24) {
      return Future.value();
    }
    final epoch = state.epoch;
    emit(state.copyWith(status: 'sealing'));
    return _sealing = _seal(epoch);
  }

  Future<void> _seal(int epoch) async {
    try {
      await _bounded(record({'event': 'seal', 'network': state.network}));
      if (!_current(epoch)) return;
      _ready = false;
      _invalidate();
      if (!isClosed) emit(state.copyWith(status: 'finished', raster: null));
    } catch (_) {
      if (!isClosed && !_terminal) _fail('session_seal_refused');
    }
  }

  bool _current(int epoch) =>
      !isClosed &&
      !_terminal &&
      _active &&
      epoch == _epoch &&
      state.network == 'unsatisfied';
  void _invalidate() {
    _epoch++;
    _cancel?.cancel();
    state.raster?.dispose();
  }

  void _fail(String reason) {
    _ready = false;
    _terminal = true;
    _invalidate();
    if (!isClosed) {
      emit(state.copyWith(status: 'error', error: reason, raster: null));
    }
  }

  @override
  Future<void> close() async {
    _active = false;
    _ready = false;
    _terminal = true;
    _invalidate();
    await Future.wait([_tail, ?_sealing]);
    await super.close();
  }
}

final p5eLimits = ReadingComparisonLimits(
  runId: 'p5e-offline',
  maxEncodedBytes: 32 * 1024 * 1024,
  maxBufferBytes: 96 * 1024 * 1024,
  maxDecodePixels: 4756000,
  maxDecodedBytes: 4756000 * 4,
  maxPeakBytes: 192 * 1024 * 1024,
  maxOwnedImages: 1,
  transferMillis: 30000,
  decodeMillis: 30000,
  statusEveryMillis: 1000,
  maxStatusChecks: 1,
  maxControlMillis: 30000,
);
