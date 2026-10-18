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
    expect(ReadingComparisonRaster.reservedSlots, 0);
    expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.zero);
    expect(ReadingComparisonRaster.activeBuffers, 0);
    expect(ReadingComparisonRaster.activeDescriptors, 0);
    expect(ReadingComparisonRaster.activeCodecs, 0);
    expect(ReadingComparisonRaster.activeImages, 0);
  });
  test(
    'P4b2 pending iptal gerçek native cleanup bitmeden slotu açmaz',
    () async {
      final bytes = await png();
      final cancellation = ReadingComparisonCancellation();
      final before = ReadingComparisonRaster.bufferCreationAttempts;
      final pending = ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        cancellation,
      );
      expect(ReadingComparisonRaster.reservedSlots, 1);
      expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.from(16));
      expect(ReadingComparisonRaster.bufferCreationAttempts - before, 1);
      cancellation.cancel();
      final cancelled = expectLater(pending, throwsStateError);
      expect(ReadingComparisonRaster.reservedSlots, 1);
      await expectLater(
        ReadingComparisonRaster.decode(
          bytes,
          raster(width: 4, height: 4),
          1,
          1,
          limits(pixels: 100),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
      expect(ReadingComparisonRaster.bufferCreationAttempts - before, 1);
      await cancelled;
      expect(ReadingComparisonRaster.reservedSlots, 0);
      expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.zero);
    },
  );
  test(
    'P4b2 resident devir farklı run ve eski double-dispose yeni ownerı korur',
    () async {
      final bytes = await png();
      Future<ReadingComparisonRaster> decode(ReadingComparisonLimits budget) =>
          ReadingComparisonRaster.decode(
            bytes,
            raster(width: 4, height: 4),
            2,
            2,
            budget,
            ReadingComparisonCancellation(),
          );
      final first = await decode(limits(pixels: 16));
      addTearDown(first.dispose);
      final before = ReadingComparisonRaster.bufferCreationAttempts;
      final different = ReadingComparisonLimits(
        runId: 'different',
        maxOwnedImages: 1,
        maxEncodedBytes: 4000000,
        maxBufferBytes: 12000000,
        maxDecodePixels: 100,
        maxDecodedBytes: 400,
        maxPeakBytes: 12002000,
        transferMillis: 1000,
        decodeMillis: 1000,
        statusEveryMillis: 1000,
        maxStatusChecks: 2,
        maxControlMillis: 3000,
      );
      for (final budget in [limits(pixels: 16), different]) {
        await expectLater(decode(budget), throwsStateError);
        expect(ReadingComparisonRaster.reservedSlots, 1);
        expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.from(16));
        expect(first.image!.debugDisposed, isFalse);
        expect(ReadingComparisonRaster.bufferCreationAttempts, before);
      }
      first.dispose();
      final second = await decode(different);
      addTearDown(second.dispose);
      first.dispose();
      expect(ReadingComparisonRaster.reservedSlots, 1);
      expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.from(16));
      expect(second.image!.debugDisposed, isFalse);
      second.dispose();
    },
  );
  test('P4b2 geçersiz hedef ve ön iptal native öncesinde reddedilir', () async {
    final bytes = await png();
    final before = ReadingComparisonRaster.bufferCreationAttempts;
    for (final size in [0, -1, 5, 0x7fffffffffffffff]) {
      await expectLater(
        ReadingComparisonRaster.decode(
          bytes,
          raster(width: 4, height: 4),
          size,
          size,
          limits(pixels: 16),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
    }
    await expectLater(
      ReadingComparisonRaster.decode(
        bytes,
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        ReadingComparisonCancellation()..cancel(),
      ),
      throwsStateError,
    );
    await expectLater(
      ReadingComparisonRaster.decode(
        Uint8List(2000001),
        raster(width: 4, height: 4),
        2,
        2,
        limits(pixels: 16),
        ReadingComparisonCancellation(),
      ),
      throwsStateError,
    );
    expect(ReadingComparisonRaster.bufferCreationAttempts, before);
  });
  test(
    'P4b2 kaynak piksel bütçesi hedef sığsa da native öncesinde reddedilir',
    () async {
      final bytes = await png();
      final before = ReadingComparisonRaster.bufferCreationAttempts;
      await expectLater(
        ReadingComparisonRaster.decode(
          bytes,
          raster(width: 4, height: 4),
          2,
          2,
          limits(pixels: 4),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
      expect(ReadingComparisonRaster.bufferCreationAttempts, before);
      expect(ReadingComparisonRaster.reservedSlots, 0);
      expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.zero);
    },
  );
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
          limits(pixels: 16),
          ReadingComparisonCancellation(),
        ),
        throwsStateError,
      );
      expect(ReadingComparisonRaster.reservedSlots, 0);
      expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.zero);
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
