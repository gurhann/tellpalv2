import 'dart:async';
import 'package:flutter/services.dart';
import '../core/diagnostics/image_experiment_recorder.dart';
import 'image_experiment_configuration.dart';

/// Owns finite experiment phases; the transport keeps status polling during baseline.
class ImageExperimentPhaseCoordinator {
  ImageExperimentPhaseCoordinator(
    this.configuration,
    this.recorder, {
    MethodChannel? channel,
    DateTime Function()? now,
  }) : _channel = channel ?? const MethodChannel('tellpal/image_experiment'),
       _now = now ?? DateTime.now {
    _revocations = configuration.credentials.revocations.listen(
      (_) => cancel(),
    );
  }
  final ImageExperimentConfiguration configuration;
  final ImageExperimentRecorder recorder;
  final MethodChannel _channel;
  final DateTime Function() _now;
  StreamSubscription<void>? _revocations;
  Timer? _phaseTimer, _sampleTimer;
  bool _cancelled = false, _started = false, _sampling = false;
  String phase = 'idle';
  void Function()? _finish;
  Future<void> Function()? _release, _beforeRelease;
  int? _lastSampleStart;
  bool get enabled => const {
    'native_comparison_r1',
    'native_quality_r1',
  }.contains(configuration.batchId);
  bool get quality => configuration.batchId == 'native_quality_r1';
  bool get active => !_cancelled && configuration.credentials.isActive;
  Map<String, Object?> get _binding => {
    'generation': configuration.generation,
    'sessionSha256': ImageExperimentRecorder.hash(configuration.sessionId),
  };

  void start({
    required void Function() load,
    required Future<void> Function() release,
    required void Function() finish,
    Future<void> Function()? beforeRelease,
  }) {
    if (_started) return;
    _started = true;
    if (!enabled) {
      load();
      return;
    }
    _finish = finish;
    _release = release;
    _beforeRelease = beforeRelease;
    final fixture = configuration.cases[configuration.selectedCaseId];
    final remaining = configuration.credentials.expiresAt.difference(_now());
    if (!active ||
        configuration.mode != 'release' ||
        fixture?.purposeAcceptanceReceipt != configuration.batchId ||
        remaining <= Duration(seconds: quality ? 46 : 105)) {
      _fail('phase_budget_refused');
      return;
    }
    if (quality) {
      _beginLoad(load);
      return;
    }
    _setPhase('baseline');
    _sample();
    _phaseTimer = Timer(const Duration(seconds: 30), () => _beginLoad(load));
  }

  void _setPhase(String value) {
    phase = value;
    recorder.record({..._binding, 'event': 'phase', 'phase': value});
  }

  void _beginLoad(void Function() load) {
    if (!active) return;
    _setPhase('load');
    _phaseTimer = Timer(
      const Duration(seconds: 10),
      () => _fail('load_timeout'),
    );
    load();
  }

  void painted() {
    if (!enabled || !active || phase != 'load') return;
    _phaseTimer?.cancel();
    _setPhase('hold');
    _phaseTimer = Timer(
      const Duration(seconds: 10),
      () => unawaited(_releaseTarget()),
    );
  }

  Future<void> _releaseTarget() async {
    if (!active) return;
    try {
      await _beforeRelease?.call().timeout(const Duration(seconds: 1));
      if (!active) return;
      _setPhase('releasing');
      await _release!().timeout(const Duration(seconds: 10));
      if (!active) return;
      recorder.record({
        ..._binding,
        'event': 'release_complete',
        'ownership': 'released',
      });
      if (quality) {
        _complete();
        return;
      }
      _setPhase('post_release');
      _phaseTimer = Timer(const Duration(seconds: 30), _complete);
    } catch (_) {
      _fail('cleanup_unavailable');
    }
  }

  void _complete() {
    if (!active) return;
    _setPhase('complete');
    cancel();
    _finish?.call();
  }

  void _fail(String reason) {
    recorder.record({..._binding, 'event': 'failure', 'reason': reason});
    cancel();
    _finish?.call();
  }

  /// Schedule against Dart call starts, never overlap native reads or retry a timed-out read.
  Future<void> _sample() async {
    if (!active || quality || _sampling) return;
    _sampling = true;
    final start = recorder.monotonicMicroseconds;
    final capturedPhase = phase;
    final cadence = _lastSampleStart == null ? null : start - _lastSampleStart!;
    _lastSampleStart = start;
    var timedOut = false;
    Map<String, Object?> memory;
    try {
      final response = await _channel
          .invokeMapMethod<String, dynamic>('sampleMemory')
          .timeout(const Duration(milliseconds: 500));
      memory = _validatedMemory(response);
    } on TimeoutException {
      timedOut = true;
      memory = _missing('sample_timeout');
    } catch (_) {
      memory = _missing('sample_failed');
    }
    final end = recorder.monotonicMicroseconds;
    _sampling = false;
    if (!active) return;
    recorder.record({
      ..._binding,
      ...memory,
      'event': 'memory_sample',
      'phase': capturedPhase,
      'sampleStartMicroseconds': start,
      'sampleEndMicroseconds': end,
      'sampleCadenceMicroseconds': cadence,
      'observerOverheadMicroseconds': end - start,
    });
    if (timedOut) return; // A native callback could still be running.
    _sampleTimer = Timer(
      Duration(microseconds: (500000 - (end - start)).clamp(0, 500000)),
      () => unawaited(_sample()),
    );
  }

  static Map<String, Object?> _missing(String reason) => {
    'memoryBytes': null,
    'memoryCounter': 'unavailable',
    'memoryReason': reason,
    'nativeReadMicroseconds': null,
    'nativeResultCode': null,
    'nativeResultCount': null,
    'thermalState': 'unavailable',
  };
  static Map<String, Object?> _validatedMemory(Map<String, dynamic>? value) {
    const keys = {
      'memoryBytes',
      'memoryCounter',
      'memoryReason',
      'nativeReadMicroseconds',
      'nativeResultCode',
      'nativeResultCount',
      'thermalState',
    };
    if (value == null ||
        value.keys.toSet().difference(keys).isNotEmpty ||
        keys.difference(value.keys.toSet()).isNotEmpty ||
        !{
          'ios_phys_footprint',
          'android_total_pss',
          'unavailable',
        }.contains(value['memoryCounter']) ||
        !{
          'none',
          'mach_result',
          'mach_count',
          'missing_counter',
          'sample_failed',
          'session_inactive',
        }.contains(value['memoryReason']) ||
        !{
          'nominal',
          'fair',
          'serious',
          'critical',
          'unavailable',
        }.contains(value['thermalState']) ||
        [
          'memoryBytes',
          'nativeReadMicroseconds',
          'nativeResultCode',
          'nativeResultCount',
        ].any(
          (key) =>
              value[key] != null &&
              (value[key] is! int || (value[key] as int) < 0),
        ) ||
        (value['memoryReason'] == 'none') != (value['memoryBytes'] is int) ||
        (value['memoryCounter'] == 'unavailable' &&
            value['memoryBytes'] != null)) {
      return _missing('sample_failed');
    }
    return Map<String, Object?>.from(value);
  }

  void cancel() {
    _cancelled = true;
    _phaseTimer?.cancel();
    _sampleTimer?.cancel();
  }

  Future<void> close() async {
    cancel();
    await _revocations?.cancel();
  }
}
