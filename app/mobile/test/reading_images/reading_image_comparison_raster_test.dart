import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'fixture.dart';
import 'comparison_test_support.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';

Future<Uint8List> png() async {
  final recorder = ui.PictureRecorder();
  final canvas = ui.Canvas(recorder);
  canvas.drawColor(const ui.Color(0xFFFFFFFF), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(4, 4);
  final data = await image.toByteData(format: ui.ImageByteFormat.png);
  image.dispose();
  picture.dispose();
  return data!.buffer.asUint8List();
}

void main() {
  test('F4 Timer Duration dönüşümünü aşan pozitif süre admission alamaz', () {
    for (final value in [0x80000000, 0x4000000000000000]) {
      expect(
        () => ReadingComparisonLimits(
          runId: 'duration',
          maxOwnedImages: 1,
          maxEncodedBytes: 100,
          maxBufferBytes: 300,
          maxDecodePixels: 100,
          maxDecodedBytes: 400,
          maxPeakBytes: 2300,
          transferMillis: value,
          decodeMillis: 1,
          statusEveryMillis: 1,
          maxStatusChecks: 1,
          maxControlMillis: 1,
        ),
        throwsArgumentError,
      );
    }
  });
  test(
    'native int64 sınırındaki bellek toplamları taşma ile admission alamaz',
    () {
      expect(
        () => ReadingComparisonLimits(
          runId: 'overflow',
          maxOwnedImages: 1,
          maxEncodedBytes: 1,
          maxBufferBytes: 3,
          maxDecodePixels: 0x4000000000000000,
          maxDecodedBytes: 0x7fffffffffffffff,
          maxPeakBytes: 0x7fffffffffffffff,
          transferMillis: 1,
          decodeMillis: 1,
          statusEveryMillis: 1,
          maxStatusChecks: 1,
          maxControlMillis: 1,
        ),
        throwsArgumentError,
      );
    },
  );
  TestWidgetsFlutterBinding.ensureInitialized();
  tearDown(() {
    expect(ReadingComparisonRaster.activeBuffers, 0);
    expect(ReadingComparisonRaster.activeDescriptors, 0);
    expect(ReadingComparisonRaster.activeCodecs, 0);
    expect(ReadingComparisonRaster.activeImages, 0);
  });
  test(
    'gerçek native codec decode edilir ve sahip olunan image bırakılır',
    () async {
      final bytes = await png();
      final image = await ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        ReadingComparisonCancellation(),
      );
      expect(image.image!.width, 2);
      image.dispose();
      expect(image.image, isNull);
      image.dispose();
    },
  );
  test(
    'gerçek header metadata/piksel bütçesini aşınca codec ve kaynak bırakılır',
    () async {
      final bytes = await png();
      await expectLater(
        ReadingComparisonRaster.decode(
          bytes,
          raster(width: 2, height: 2),
          2,
          2,
          limits(pixels: 4),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
      final cancellation = ReadingComparisonCancellation();
      final operation = ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        cancellation,
      );
      cancellation.cancel();
      await expectLater(operation, throwsStateError);
      await expectLater(
        ReadingComparisonRaster.decode(
          Uint8List.fromList([1, 2, 3]),
          raster(width: 4, height: 4),
          2,
          2,
          limits(pixels: 16),
          ReadingComparisonCancellation(),
        ),
        throwsA(anything),
      );
    },
  );
}
