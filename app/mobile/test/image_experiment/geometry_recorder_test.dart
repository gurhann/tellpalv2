import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/data/model/raster_case_model.dart';
import 'fixture.dart';

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
