import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/data/model/raster_case_model.dart';
import 'fixture.dart';

const _legacyCounters = [
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
];

void main() {
  test('both decode axes use one scale and retain shortage demand', () {
    final entry = fixtureEntry()
      ..['width'] = 100
      ..['height'] = 50
      ..['logicalSlotWidth'] = 200.0
      ..['logicalSlotHeight'] = 200.0
      ..['dpr'] = 2.0;
    final contain = RasterCaseModel.fromJson(entry).geometry;
    expect(contain.requiredWidth, 400);
    expect(contain.requiredHeight, 200);
    expect(contain.decodeWidth, 100);
    expect(contain.decodeHeight, 50);
    expect(contain.sourceShortage, true);
    entry['fit'] = 'cover';
    final cover = RasterCaseModel.fromJson(entry).geometry;
    expect(cover.requiredWidth, 800);
    expect(cover.requiredHeight, 400);
    entry
      ..['logicalSlotWidth'] = 20.0
      ..['logicalSlotHeight'] = 20.0
      ..['dpr'] = 1.0
      ..['fit'] = 'contain';
    final small = RasterCaseModel.fromJson(entry).geometry;
    expect(small.decodeWidth, 20);
    expect(small.decodeHeight, 10);
  });
  test(
    'legacy recorder preserves all ten unavailable counter placeholders',
    () async {
      final recorder = ImageExperimentRecorder();
      recorder.record({'event': 'status', 'reason': 'none'});
      final row =
          (jsonDecode(await recorder.export())['records'] as List).single
              as Map<String, dynamic>;
      for (final counter in _legacyCounters) {
        expect(row.containsKey(counter), true, reason: counter);
        expect(row[counter], null, reason: counter);
      }
      expect(row['counterReason'], 'missing_counter');
    },
  );
  test(
    'compact recorder retains explicit nulls and reasons without inferred counters',
    () async {
      final recorder = ImageExperimentRecorder(
        includeLegacyCounterPlaceholders: false,
      );
      recorder.record({
        'event': 'memory_sample',
        'memoryBytes': null,
        'memoryCounter': 'unavailable',
        'memoryReason': 'sample_timeout',
        'decodedLiveCount': null,
        'counterReason': 'internal_settlement_unavailable',
      });
      recorder.record({'event': 'status', 'reason': 'none'});
      final rows = jsonDecode(await recorder.export())['records'] as List;
      final missing = rows.first as Map<String, dynamic>;
      for (final counter in ['memoryBytes', 'decodedLiveCount']) {
        expect(missing.containsKey(counter), true, reason: counter);
        expect(missing[counter], null, reason: counter);
      }
      expect(missing['memoryReason'], 'sample_timeout');
      expect(missing['counterReason'], 'internal_settlement_unavailable');
      expect(missing.containsKey('dartHeapBytes'), false);
      expect(rows.last['counterReason'], 'missing_counter');
    },
  );
  test(
    'complete synthetic reader trace retains all 418 events within both caps',
    () async {
      final recorder = ImageExperimentRecorder(
        includeLegacyCounterPlaceholders: false,
      );
      final trace = _syntheticReaderTrace();
      expect(trace.length, 418);
      for (final row in trace) {
        recorder.record(row);
      }
      final raw = await recorder.export();
      final value = jsonDecode(raw) as Map<String, dynamic>;
      final rows = (value['records'] as List).cast<Map<String, dynamic>>();
      expect(value['droppedCount'], 0);
      expect(rows.length, trace.length);
      expect(utf8.encode(raw).length, lessThan(250000));
      for (var index = 0; index < trace.length; index++) {
        expect(rows[index], containsPair('counterReason', 'missing_counter'));
        for (final entry in trace[index].entries) {
          expect(rows[index], containsPair(entry.key, entry.value));
        }
      }
      expect(
        rows.where((row) => row['event'] == 'phase').map((row) => row['phase']),
        [
          'baseline',
          'reader',
          'natural_retention',
          'owned_eviction',
          'complete',
        ],
      );
      expect(rows.where((row) => row['event'] == 'memory_sample').length, 112);
      expect(
        rows.singleWhere((row) => row['memoryReason'] == 'sample_timeout'),
        containsPair('memoryBytes', null),
      );
      expect(rows.where((row) => row['event'] == 'ownership').length, 6);
      expect(rows.where((row) => row['event'] == 'cache_state').length, 24);
      expect(rows.where((row) => row['event'] == 'release_complete').length, 1);
    },
  );
  test('compact recorder still drops the 513th small event', () async {
    final recorder = ImageExperimentRecorder(
      includeLegacyCounterPlaceholders: false,
    );
    for (var index = 0; index < 513; index++) {
      recorder.record({'event': 'draw', 'frameNumber': index});
    }
    final raw = await recorder.export();
    final value = jsonDecode(raw);
    expect((value['records'] as List).length, 512);
    expect(value['droppedCount'], 1);
    expect(utf8.encode(raw).length, lessThanOrEqualTo(250000));
  });
  test(
    'compact recorder still enforces the byte cap before 512 rows',
    () async {
      final recorder = ImageExperimentRecorder(
        includeLegacyCounterPlaceholders: false,
      );
      for (var index = 0; index < 512; index++) {
        recorder.record({
          'event': 'ownership',
          'ownership': 'owned',
          'generation': index,
          for (final key in [
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
          ])
            key: 'a' * 64,
        });
      }
      final raw = await recorder.export();
      final value = jsonDecode(raw);
      final count = (value['records'] as List).length;
      expect(count, inExclusiveRange(0, 512));
      expect(value['droppedCount'], 512 - count);
      expect(utf8.encode(raw).length, lessThanOrEqualTo(250000));
    },
  );
  test(
    'recorder refuses raw values and caps sanitized export with explicit loss',
    () async {
      final recorder = ImageExperimentRecorder();
      recorder.record({
        'event': 'request',
        'url': 'https://secret',
        'idToken': 'credential',
      });
      recorder.record({'event': 'failure', 'reason': 'raw-server-error'});
      for (var n = 0; n < 600; n++) {
        recorder.record({
          'event': 'draw',
          'frameNumber': n,
          'firstRasterizedTargetFrame': null,
          'reason': 'unverified_visibility',
          'physicalPresentationVerified': false,
        });
      }
      final raw = await recorder.export();
      final value = jsonDecode(raw);
      final count = (value['records'] as List).length;
      expect(count, inInclusiveRange(1, 512));
      expect(value['droppedCount'], 600 - count);
      expect(utf8.encode(raw).length, lessThanOrEqualTo(250000));
      expect(raw, isNot(contains('secret')));
      expect(raw, isNot(contains('credential')));
      expect(
        (value['records'] as List).first['firstRasterizedTargetFrame'],
        null,
      );
    },
  );
}

/// Synthetic coverage of a full six-page run, including retained and evicted owners.
List<Map<String, Object?>> _syntheticReaderTrace() {
  final rows = <Map<String, Object?>>[];
  final session = ImageExperimentRecorder.hash('synthetic-reader-session');
  const phases = {
    0: 'baseline',
    20: 'reader',
    72: 'natural_retention',
    102: 'owned_eviction',
    112: 'complete',
  };
  void add(int at, String event, [Map<String, Object?> fields = const {}]) =>
      rows.add({
        'generation': 1,
        'sessionSha256': session,
        'monotonicMicroseconds': at,
        'event': event,
        ...fields,
      });
  for (final phase in phases.entries) {
    add(phase.key * 1000000, 'phase', {'phase': phase.value});
    if (phase.value != 'complete') {
      add((phase.key + 1) * 1000000, 'frame_aggregate', {
        'phase': phase.value,
        'frameCount': 50,
        'frameBuildTotalMicroseconds': 100000,
        'frameRasterTotalMicroseconds': 150000,
        'frameBuildMaxMicroseconds': 5000,
        'frameRasterMaxMicroseconds': 6000,
      });
    }
  }
  for (var second = 0; second < 112; second++) {
    final timeout = second == 36;
    final start = second * 1000000 + 1000;
    add(start + 20, 'memory_sample', {
      'phase': phases.entries.lastWhere((entry) => entry.key <= second).value,
      'memoryBytes': timeout ? null : 123456789,
      'memoryCounter': timeout ? 'unavailable' : 'ios_phys_footprint',
      'memoryReason': timeout ? 'sample_timeout' : 'none',
      'nativeReadMicroseconds': timeout ? null : 10,
      'nativeResultCode': timeout ? null : 0,
      'nativeResultCount': timeout ? null : 96,
      'thermalState': timeout ? 'unavailable' : 'nominal',
      'sampleStartMicroseconds': start,
      'sampleEndMicroseconds': start + 20,
      'sampleCadenceMicroseconds': second == 0 ? null : 1000000,
      'observerOverheadMicroseconds': 20,
    });
  }
  for (var poll = 0; poll < 57; poll++) {
    final start = poll * 2000000;
    add(start + 10, 'status_request', {'requestCount': 1, 'retryCount': 0});
    add(start + 20, 'status_body', {'bodyBytes': 38});
    add(start + 30, 'status', {
      'reason': 'none',
      'observerOverheadMicroseconds': 20,
    });
  }
  for (var selection = 0; selection < 18; selection++) {
    add(20000000 + selection * 1000000, 'reader_selection', {
      'pageIndex': selection % 6,
      'selectionGeneration': selection + 1,
    });
  }
  for (var page = 0; page < 6; page++) {
    final identity = {
      'caseSha256': ImageExperimentRecorder.hash('reader_page_${page + 1}'),
      'providerSha256': ImageExperimentRecorder.hash('provider-$page'),
      'attemptSha256': ImageExperimentRecorder.hash('attempt-$page'),
    };
    final start = 20000000 + page * 4000000;
    add(start, 'ownership', {
      ...identity,
      'ownership': 'owned',
      'role': 'isolated_raster_fixture',
      'sourceSha256': '1' * 64,
      'referenceSha256': '2' * 64,
      'settingsSha256': '3' * 64,
      'scopeSha256': '4' * 64,
      'sourceWidth': 1712,
      'sourceHeight': 2778,
      'logicalSlotWidth': 390.0,
      'logicalSlotHeight': 600.0,
      'dpr': 3.0,
      'fit': 'contain',
      'alignmentX': 0.0,
      'alignmentY': 0.0,
      'requiredWidth': 1110,
      'requiredHeight': 1800,
      'decodeWidth': 1712,
      'decodeHeight': 2778,
      'sourceShortage': false,
    });
    add(start + 10, 'cache_state', {
      ...identity,
      'cacheState': 'namespace_keys_absent',
      'ownership': 'owned',
      'reason': 'none',
    });
    add(start + 20, 'request', {
      ...identity,
      'role': 'isolated_raster_fixture',
      'requestCount': 1,
      'retryCount': 0,
    });
    add(start + 30, 'body', {
      ...identity,
      'role': 'isolated_raster_fixture',
      'bodyBytes': 1234567,
    });
    for (var proof = 0; proof < const [6, 5, 5, 4, 4, 3][page]; proof++) {
      add(start + 40 + proof, 'encoded_verified', {
        ...identity,
        'encodedVerified': true,
      });
    }
    for (var decode = 0; decode < const [4, 4, 4, 3, 2, 2][page]; decode++) {
      add(start + 50 + decode, 'decoded', {
        ...identity,
        'pageIndex': page,
        'actualDecodeWidth': 1712,
        'actualDecodeHeight': 2778,
      });
    }
    for (var paint = 0; paint < const [3, 3, 3, 2, 2, 2][page]; paint++) {
      add(start + 60 + paint, 'page_paint', {
        ...identity,
        'pageIndex': page,
        'selectionGeneration': page + 1,
        'frameNumber': page + paint,
        'physicalPresentationVerified': false,
        'reason': 'unverified_visibility',
      });
    }
    for (final at in [72000100, 102000100, 102000300]) {
      add(at, 'cache_state', {
        ...identity,
        'phase': at == 102000300 ? 'owned_eviction' : 'natural_retention',
        'decodedLiveCount': 0,
        'decodedPendingCount': 0,
        'decodedKeepAliveCount': at == 102000300 ? 0 : 1,
        'decodedLruBytes': at == 102000300 ? 0 : 19023744,
        'ownership': 'owned',
        'reason': 'internal_settlement_unavailable',
      });
    }
  }
  add(1, 'device_facts', {
    'platform': 'ios',
    'deviceFactsSha256': '5' * 64,
    'modelSha256': '6' * 64,
    'abisSha256': '7' * 64,
    'apiLevel': null,
    'memTotalBytes': 4294967296,
    'memoryClassMiB': null,
    'lowRamDevice': null,
    'isPhysicalDevice': true,
    'qemu': 'unavailable',
    'deviceReason': 'unavailable',
  });
  add(20000001, 'reader_geometry', {
    'observedSlotWidth': 390.0,
    'observedSlotHeight': 600.0,
    'observedDpr': 3.0,
    'textScaleFactor': 1.0,
    'paddingLeft': 0.0,
    'paddingTop': 47.0,
    'paddingRight': 0.0,
    'paddingBottom': 34.0,
    'viewInsetLeft': 0.0,
    'viewInsetTop': 0.0,
    'viewInsetRight': 0.0,
    'viewInsetBottom': 0.0,
    'orientation': 'portrait',
    'settingsSha256': '8' * 64,
  });
  add(71999999, 'lifecycle', {
    'backgroundCount': 1,
    'returnCount': 1,
    'backgrounded': false,
    'backgroundEpochMilliseconds': 100000,
    'returnedEpochMilliseconds': 100500,
  });
  add(102000200, 'release_complete', {'ownership': 'released'});
  add(113000000, 'revoke', {'reason': 'session_inactive'});
  rows.sort(
    (a, b) => (a['monotonicMicroseconds'] as int).compareTo(
      b['monotonicMicroseconds'] as int,
    ),
  );
  return rows;
}
