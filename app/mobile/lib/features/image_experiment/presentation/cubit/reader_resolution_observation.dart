import 'dart:async';
import 'dart:convert';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../../../../core/diagnostics/image_experiment_recorder.dart';

/// Bounded observer ownership is separate from the reader's route and image owners.
class ReaderResolutionObservation {
  ReaderResolutionObservation(this.configuration, this.recorder, this.isActive);
  final ImageExperimentConfiguration configuration;
  final ImageExperimentRecorder recorder;
  final bool Function() isActive;
  final _channel = const MethodChannel('tellpal/image_experiment');
  bool _closed = false, _readingSample = false, _timingsOwned = false;
  String _phase = 'idle';
  int? _lastSample;
  int _frames = 0,
      _buildTotal = 0,
      _rasterTotal = 0,
      _buildMax = 0,
      _rasterMax = 0;
  Timer? _sampleTimer;
  bool get active => !_closed && isActive();
  Map<String, Object?> get _binding => {
    'generation': configuration.generation,
    'sessionSha256': ImageExperimentRecorder.hash(configuration.sessionId),
  };
  void start() {
    unawaited(_facts());
    if (configuration.readerQuality) return;
    SchedulerBinding.instance.addTimingsCallback(_timings);
    _timingsOwned = true;
    unawaited(_sample());
  }

  void phase(String value) {
    _flushFrames();
    _phase = value;
  }

  void _timings(List<FrameTiming> frames) {
    if (!active) return;
    for (final frame in frames) {
      final build = frame.buildDuration.inMicroseconds,
          raster = frame.rasterDuration.inMicroseconds;
      _frames++;
      _buildTotal += build;
      _rasterTotal += raster;
      if (build > _buildMax) _buildMax = build;
      if (raster > _rasterMax) _rasterMax = raster;
    }
  }

  void _flushFrames() {
    if (_phase == 'idle' || configuration.readerQuality) return;
    recorder.record({
      ..._binding,
      'event': 'frame_aggregate',
      'phase': _phase,
      'frameCount': _frames,
      'frameBuildTotalMicroseconds': _buildTotal,
      'frameRasterTotalMicroseconds': _rasterTotal,
      'frameBuildMaxMicroseconds': _buildMax,
      'frameRasterMaxMicroseconds': _rasterMax,
      if (_frames == 0) 'reason': 'frame_unavailable',
    });
    _frames = _buildTotal = _rasterTotal = _buildMax = _rasterMax = 0;
  }

  Future<void> _sample() async {
    if (!active || _readingSample) return;
    _readingSample = true;
    final start = recorder.monotonicMicroseconds, phase = _phase;
    final cadence = _lastSample == null ? null : start - _lastSample!;
    _lastSample = start;
    var timedOut = false;
    Map<String, Object?> memory;
    try {
      final response = await _channel
          .invokeMapMethod<String, dynamic>('sampleMemory')
          .timeout(const Duration(milliseconds: 750));
      memory = _memory(response);
    } on TimeoutException {
      timedOut = true;
      memory = _missing('sample_timeout');
    } catch (_) {
      memory = _missing('sample_failed');
    }
    final end = recorder.monotonicMicroseconds;
    _readingSample = false;
    if (!active) return;
    recorder.record({
      ..._binding,
      ...memory,
      'event': 'memory_sample',
      'phase': phase,
      'sampleStartMicroseconds': start,
      'sampleEndMicroseconds': end,
      'sampleCadenceMicroseconds': cadence,
      'observerOverheadMicroseconds': end - start,
    });
    if (timedOut) return;
    _sampleTimer = Timer(
      Duration(microseconds: (1000000 - (end - start)).clamp(0, 1000000)),
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
  static Map<String, Object?> _memory(Map<String, dynamic>? value) {
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
        !const {
          'ios_phys_footprint',
          'android_total_pss',
          'unavailable',
        }.contains(value['memoryCounter']) ||
        !const {
          'none',
          'mach_result',
          'mach_count',
          'missing_counter',
          'sample_failed',
          'session_inactive',
        }.contains(value['memoryReason']) ||
        !const {
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
        ].any((k) => value[k] != null && (value[k] is! int || value[k] < 0)) ||
        (value['memoryReason'] == 'none') != (value['memoryBytes'] is int) ||
        (value['memoryCounter'] == 'unavailable' &&
            value['memoryBytes'] != null)) {
      return _missing('sample_failed');
    }
    return Map<String, Object?>.from(value);
  }

  Future<void> _facts() async {
    try {
      final value = await _channel
          .invokeMapMethod<String, dynamic>('readDeviceFacts')
          .timeout(const Duration(seconds: 1));
      if (!active ||
          value == null ||
          value['schemaVersion'] != 1 ||
          !const {'ios', 'android'}.contains(value['platform'])) {
        throw StateError('unavailable');
      }
      recorder.record({
        ..._binding,
        'event': 'device_facts',
        'platform': value['platform'],
        'deviceFactsSha256': ImageExperimentRecorder.hash(jsonEncode(value)),
        if (value['model'] is String)
          'modelSha256': ImageExperimentRecorder.hash(value['model']),
        if (value['abis'] is List)
          'abisSha256': ImageExperimentRecorder.hash(jsonEncode(value['abis'])),
        'apiLevel': value['apiLevel'],
        'memTotalBytes': value['memTotalBytes'],
        'memoryClassMiB': value['memoryClassMiB'],
        'lowRamDevice': value['lowRamDevice'],
        'isPhysicalDevice': value['isPhysicalDevice'],
        'qemu': const {'0', '1'}.contains(value['qemu'])
            ? value['qemu']
            : 'unavailable',
        'deviceReason':
            (value['unavailableReasons'] is Map &&
                (value['unavailableReasons'] as Map).isEmpty)
            ? 'none'
            : 'unavailable',
      });
    } catch (_) {
      if (active) {
        recorder.record({
          ..._binding,
          'event': 'failure',
          'reason': 'device_facts_unavailable',
        });
      }
    }
  }

  Future<void> background() async {
    try {
      if (await _channel
              .invokeMethod<bool>('requestReaderBackground')
              .timeout(const Duration(seconds: 1)) !=
          true) {
        throw StateError('unavailable');
      }
    } catch (_) {
      if (active) {
        recorder.record({
          ..._binding,
          'event': 'failure',
          'reason': 'lifecycle_unavailable',
        });
      }
    }
  }

  Future<void> lifecycle() async {
    try {
      final value = await _channel
          .invokeMapMethod<String, dynamic>('readReaderLifecycle')
          .timeout(const Duration(seconds: 1));
      if (value == null ||
          value['schemaVersion'] != 1 ||
          value['backgroundCount'] is! int ||
          value['returnCount'] is! int ||
          value['backgroundCount'] < 1 ||
          value['returnCount'] < 1 ||
          value['backgrounded'] != false) {
        throw StateError('unavailable');
      }
      recorder.record({
        ..._binding,
        'event': 'lifecycle',
        for (final key in [
          'backgroundCount',
          'returnCount',
          'backgrounded',
          'backgroundEpochMilliseconds',
          'returnedEpochMilliseconds',
        ])
          key: value[key],
      });
    } catch (_) {
      if (active) {
        recorder.record({
          ..._binding,
          'event': 'failure',
          'reason': 'lifecycle_unavailable',
        });
      }
    }
  }

  void close() {
    _closed = true;
    _sampleTimer?.cancel();
    if (_timingsOwned) {
      _timingsOwned = false;
      SchedulerBinding.instance.removeTimingsCallback(_timings);
    }
  }
}
