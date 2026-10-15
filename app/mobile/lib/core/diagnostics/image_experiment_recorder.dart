import 'dart:convert';
import 'package:crypto/crypto.dart';

/// Finite private ledger. Unknown fields and arbitrary strings are refused.
class ImageExperimentRecorder {
  ImageExperimentRecorder({bool includeLegacyCounterPlaceholders = true})
    : _includeLegacyCounterPlaceholders = includeLegacyCounterPlaceholders;

  final bool _includeLegacyCounterPlaceholders;
  final _clock = Stopwatch()..start();
  final _records = <Map<String, Object?>>[];
  // Reserve framing, separators and the explicit loss counter inside the byte cap.
  int _bytes = 128;
  int droppedCount = 0;
  static const _hashKeys = {
    'caseSha256',
    'providerSha256',
    'attemptSha256',
    'sessionSha256',
    'sourceSha256',
    'referenceSha256',
    'settingsSha256',
    'scopeSha256',
    'modelSha256',
    'abisSha256',
    'deviceFactsSha256',
  };
  static const _intKeys = {
    'generation',
    'monotonicMicroseconds',
    'requestCount',
    'retryCount',
    'bodyBytes',
    'sourceWidth',
    'sourceHeight',
    'requiredWidth',
    'requiredHeight',
    'decodeWidth',
    'decodeHeight',
    'actualDecodeWidth',
    'actualDecodeHeight',
    'frameNumber',
    'dartHeapBytes',
    'dartExternalBytes',
    'decodedLruBytes',
    'decodedLiveCount',
    'decodedPendingCount',
    'processBytes',
    'nativeBytes',
    'graphicsBytes',
    'diskBytes',
    'transferBytes',
    'firstRasterizedTargetFrame',
    'observerOverheadMicroseconds',
    'memoryBytes',
    'nativeReadMicroseconds',
    'nativeResultCode',
    'nativeResultCount',
    'sampleStartMicroseconds',
    'sampleEndMicroseconds',
    'sampleCadenceMicroseconds',
    'pageIndex',
    'selectionGeneration',
    'decodedKeepAliveCount',
    'frameCount',
    'frameBuildTotalMicroseconds',
    'frameRasterTotalMicroseconds',
    'frameBuildMaxMicroseconds',
    'frameRasterMaxMicroseconds',
    'apiLevel',
    'memTotalBytes',
    'memoryClassMiB',
    'backgroundCount',
    'returnCount',
    'backgroundEpochMilliseconds',
    'returnedEpochMilliseconds',
  };
  static const _doubleKeys = {
    'logicalSlotWidth',
    'logicalSlotHeight',
    'dpr',
    'alignmentX',
    'alignmentY',
    'observedSlotWidth',
    'observedSlotHeight',
    'observedDpr',
    'textScaleFactor',
    'paddingLeft',
    'paddingTop',
    'paddingRight',
    'paddingBottom',
    'viewInsetLeft',
    'viewInsetTop',
    'viewInsetRight',
    'viewInsetBottom',
  };
  static const _boolKeys = {
    'encodedVerified',
    'sourceShortage',
    'physicalPresentationVerified',
    'lowRamDevice',
    'isPhysicalDevice',
    'backgrounded',
  };
  static const _strings = <String, Set<String>>{
    'phase': {
      'idle',
      'baseline',
      'load',
      'hold',
      'releasing',
      'post_release',
      'complete',
      'reader',
      'natural_retention',
      'owned_eviction',
    },
    'memoryCounter': {'ios_phys_footprint', 'android_total_pss', 'unavailable'},
    'memoryReason': {
      'none',
      'mach_result',
      'mach_count',
      'missing_counter',
      'sample_failed',
      'session_inactive',
      'sample_timeout',
    },
    'thermalState': {'nominal', 'fair', 'serious', 'critical', 'unavailable'},
    'clockDomain': {'dart_monotonic'},
    'clockUnit': {'microseconds'},
    'fit': {'contain', 'cover'},
    'orientation': {'portrait', 'landscape'},
    'platform': {'ios', 'android'},
    'deviceReason': {
      'none',
      'unsupported',
      'read_failed',
      'invalid_value',
      'unavailable',
    },
    'qemu': {'0', '1', 'unavailable'},
    'role': {'isolated_raster_fixture'},
    'event': {
      'request',
      'body',
      'encoded_verified',
      'decoded',
      'draw',
      'failure',
      'revoke',
      'cache_state',
      'status',
      'status_request',
      'status_body',
      'ownership',
      'phase',
      'memory_sample',
      'release_complete',
      'layout',
      'dispatch',
      'reader_geometry',
      'reader_selection',
      'page_paint',
      'frame_aggregate',
      'device_facts',
      'lifecycle',
    },
    'ownership': {'owned', 'settling', 'settled', 'released', 'unavailable'},
    'reason': {
      'none',
      'invalid_fixture',
      'session_inactive',
      'status_unavailable',
      'status_revoked',
      'missing_counter',
      'unverified_visibility',
      'frame_unavailable',
      'timing_unavailable',
      'clock_unmapped',
      'retained_layer_ambiguity',
      'route_departed',
      'stale_attempt',
      'internal_settlement_unavailable',
      'export_capacity',
      'pin_refused',
      'transport_refused',
      'decode_refused',
      'cleanup_unavailable',
      'phase_budget_refused',
      'load_timeout',
      'layout_unavailable',
      'lifecycle_unavailable',
      'device_facts_unavailable',
      'reader_incomplete',
    },
    'counterReason': {
      'missing_counter',
      'unverified_visibility',
      'frame_unavailable',
      'timing_unavailable',
      'clock_unmapped',
      'retained_layer_ambiguity',
      'internal_settlement_unavailable',
    },
    'cacheState': {
      'namespace_keys_absent',
      'network-cold',
      'disk-warm/decoded-cold',
      'decoded-warm',
      'unavailable',
    },
  };
  static String hash(String value) =>
      sha256.convert(utf8.encode(value)).toString();
  int get monotonicMicroseconds => _clock.elapsedMicroseconds;
  void record(Map<String, Object?> input) {
    try {
      final row = <String, Object?>{
        'monotonicMicroseconds': monotonicMicroseconds,
        'clockDomain': 'dart_monotonic',
        'clockUnit': 'microseconds',
        if (_includeLegacyCounterPlaceholders)
          for (final counter in [
            'dartHeapBytes',
            'dartExternalBytes',
            'decodedLruBytes',
            'decodedLiveCount',
            'decodedPendingCount',
            'processBytes',
            'nativeBytes',
            'graphicsBytes',
            'diskBytes',
            'transferBytes',
          ])
            counter: null,
        'counterReason': 'missing_counter',
      };
      for (final entry in input.entries) {
        final key = entry.key, value = entry.value;
        if (_hashKeys.contains(key)) {
          if (value is! String || !RegExp(r'^[a-f0-9]{64}$').hasMatch(value)) {
            return;
          }
        } else if (_intKeys.contains(key)) {
          if (value != null &&
              (value is! int || value < (key == 'frameNumber' ? -1 : 0))) {
            return;
          }
        } else if (_doubleKeys.contains(key)) {
          if (value is! num || !value.isFinite) return;
        } else if (_boolKeys.contains(key)) {
          if (value is! bool &&
              !(value == null &&
                  const {'lowRamDevice', 'isPhysicalDevice'}.contains(key))) {
            return;
          }
        } else if (_strings.containsKey(key)) {
          if (value is! String || !_strings[key]!.contains(value)) return;
        } else {
          return;
        }
        row[key] = value;
      }
      final size = utf8.encode(jsonEncode(row)).length;
      if (_records.length >= 512 || _bytes + size > 250000) {
        droppedCount++;
        return;
      }
      _records.add(Map.unmodifiable(row));
      _bytes += size + 1;
    } catch (_) {
      /* Observation cannot affect the consumer. */
    }
  }

  Future<String> export() => Future<String>.sync(
    () => jsonEncode({
      'schemaVersion': 1,
      'records': List.unmodifiable(_records),
      'droppedCount': droppedCount,
    }),
  ).timeout(const Duration(seconds: 2));
  @override
  String toString() => 'ImageExperimentRecorder(sanitized)';
}
