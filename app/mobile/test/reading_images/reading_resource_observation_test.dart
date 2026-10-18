import 'dart:convert';
import 'dart:isolate';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

import '../../test_driver/reading_comparison_probe.dart';
import '../../test_driver/reading_consumer_qa_probe.dart';
import '../../test_driver/reading_resource_observation.dart';
import 'comparison_test_support.dart';
import 'fixture.dart';
import 'reading_comparison_probe_test.dart' show png;

Map<String, dynamic> _json(Map<String, Object?> value) =>
    jsonDecode(jsonEncode(value)) as Map<String, dynamic>;

void _observed(
  Map<String, dynamic> sample,
  int slots,
  String bytes,
  int attempts,
) {
  expect(sample['resourceObservation'], {
    'version': 1,
    'lifetimeId': readingOwnedResources()['resourceObservation'] is Map
        ? (readingOwnedResources()['resourceObservation'] as Map)['lifetimeId']
        : fail('gözlem map olmalıdır'),
    'reservedSlots': slots,
    'reservedDecodedBytes': bytes,
    'bufferCreationAttempts': '$attempts',
  });
  expect(sample['activeBuffers'], ReadingComparisonRaster.activeBuffers);
  expect(
    sample['activeDescriptors'],
    ReadingComparisonRaster.activeDescriptors,
  );
  expect(sample['activeCodecs'], ReadingComparisonRaster.activeCodecs);
  expect(sample['activeImages'], ReadingComparisonRaster.activeImages);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('P4b4 gerçek pending, iptal ve drain iki probe JSON’unda ayrılır', (
    tester,
  ) async {
    await tester.runAsync(() async {
      final bytes = await png(4, 4);
      final first = await ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        ReadingComparisonCancellation(),
      );
      first.dispose();
      final baseline = ReadingComparisonRaster.bufferCreationAttempts;
      expect(baseline, greaterThan(0));
      final consumer = ReadingConsumerQaProbe();
      final comparison = ReadingComparisonProbe();
      final samples = [_json(consumer.snapshot())];
      final cancellation = ReadingComparisonCancellation();
      final pending = ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        cancellation,
      );
      final pendingSample = _json(consumer.snapshot());
      _observed(pendingSample, 1, '16', baseline + 1);
      expect(pendingSample['activeImages'], 0);
      expect(pendingSample['activeBuffers'], 0);
      expect(comparison.ownedResources(), consumer.ownedResources());
      samples.add(pendingSample);
      cancellation.cancel();
      final cancelled = expectLater(pending, throwsStateError);
      _observed(_json(comparison.snapshot()), 1, '16', baseline + 1);
      await expectLater(
        ReadingComparisonRaster.decode(
          bytes,
          raster(width: 4, height: 4),
          1,
          1,
          limits(pixels: 16),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
      expect(ReadingComparisonRaster.bufferCreationAttempts, baseline + 1);
      await cancelled;
      _observed(_json(comparison.snapshot()), 0, '0', baseline + 1);
      final stopped =
          jsonDecode(await consumer.handle('{"action":"stop"}'))
              as Map<String, dynamic>;
      _observed(stopped, 0, '0', baseline + 1);
      samples.add(stopped);
      // Ham çıktı özel test kaydından Python'a verilir; dosya/IPC eklenmez.
      // ignore: avoid_print
      print(
        'P4_RESOURCE_JSON:${jsonEncode({'chain': 'native-pending', 'samples': samples})}',
      );
    });
    await tester.pumpWidget(const SizedBox());
  });

  test(
    'P4b4 resident ve ikinci ret sayacı değiştirmez; yeni probe yaşamı korur',
    () async {
      final bytes = await png(4, 4);
      final baseline = ReadingComparisonRaster.bufferCreationAttempts;
      final resident = await ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        ReadingComparisonCancellation(),
      );
      try {
        final sample = _json(ReadingConsumerQaProbe().ownedResources());
        _observed(sample, 1, '16', baseline + 1);
        expect(sample['activeImages'], 1);
        await expectLater(
          ReadingComparisonRaster.decode(
            bytes,
            raster(width: 4, height: 4),
            1,
            1,
            limits(pixels: 16),
            ReadingComparisonCancellation(),
          ),
          throwsStateError,
        );
        _observed(
          _json(ReadingComparisonProbe().ownedResources()),
          1,
          '16',
          baseline + 1,
        );
        expect(
          ReadingComparisonProbe().ownedResources(),
          ReadingConsumerQaProbe().ownedResources(),
        );
      } finally {
        resident.dispose();
      }
      _observed(_json(readingOwnedResources()), 0, '0', baseline + 1);
      final local =
          (readingOwnedResources()['resourceObservation'] as Map)['lifetimeId'];
      final other = await Isolate.run(
        () =>
            (readingOwnedResources()['resourceObservation']
                as Map)['lifetimeId'],
      );
      expect(other, isNot(local));
    },
  );
}
