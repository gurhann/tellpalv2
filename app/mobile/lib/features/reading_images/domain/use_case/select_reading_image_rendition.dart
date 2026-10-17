import 'package:dartz/dartz.dart';

import '../entity/reading_image_manifest.dart';
import 'validate_reading_image_manifest.dart';

enum ReadingImageFit { contain, cover }

enum ReadingImageSelectionFailure {
  invalidGeometry,
  bindingMismatch,
  sourceShortage,
  renditionShortage,
  decodeResourceExceeded,
}

final class ReadingImageGeometry {
  const ReadingImageGeometry({
    required this.physicalWidth,
    required this.physicalHeight,
    required this.requiredWidth,
    required this.requiredHeight,
    required this.decodeWidth,
    required this.decodeHeight,
  });

  final int physicalWidth, physicalHeight, requiredWidth, requiredHeight;
  final int decodeWidth, decodeHeight;
}

final class ReadingImageSelection {
  const ReadingImageSelection({
    required this.rendition,
    required this.geometry,
  });

  final ReadingImageRendition rendition;
  final ReadingImageGeometry geometry;
}

/// Yalnız doğrulanmış descriptor üzerinde seçim yapar; orijinale düşmez.
final class SelectReadingImageRendition {
  const SelectReadingImageRendition();

  Either<ReadingImageSelectionFailure, ReadingImageSelection> call({
    required ValidatedReadingImageManifest manifest,
    required ReadingImageBinding binding,
    required Set<String> acceptedProfileFingerprints,
    required double logicalWidth,
    required double logicalHeight,
    required double devicePixelRatio,
    required ReadingImageFit fit,
    required int maxDecodePixels,
  }) {
    if (![
      logicalWidth,
      logicalHeight,
      devicePixelRatio,
    ].every((value) => value.isFinite && value > 0)) {
      return const Left(ReadingImageSelectionFailure.invalidGeometry);
    }
    if (maxDecodePixels <= 0) {
      return const Left(ReadingImageSelectionFailure.decodeResourceExceeded);
    }
    final matching = manifest.descriptor.images.where(
      (e) => e.source.binding == binding,
    );
    if (matching.isEmpty) {
      return const Left(ReadingImageSelectionFailure.bindingMismatch);
    }
    final entry = matching.first;
    final source = entry.source.raster;
    final slotWidth = logicalWidth * devicePixelRatio;
    final slotHeight = logicalHeight * devicePixelRatio;
    if (!_representable(slotWidth) || !_representable(slotHeight)) {
      return const Left(ReadingImageSelectionFailure.invalidGeometry);
    }
    final physicalWidth = slotWidth.ceil();
    final physicalHeight = slotHeight.ceil();
    final required = _fitDimensions(
      source.width,
      source.height,
      physicalWidth,
      physicalHeight,
      fit,
    );
    // İhtiyaç kaynakla sınırlandırılmaz. Upscale sessizce kabul edilmez.
    if (required.$1 > BigInt.from(source.width) ||
        required.$2 > BigInt.from(source.height)) {
      return const Left(ReadingImageSelectionFailure.sourceShortage);
    }
    final width = required.$1.toInt();
    final height = required.$2.toInt();
    final candidates =
        entry.renditions
            .where(
              (r) =>
                  acceptedProfileFingerprints.contains(r.profileFingerprint) &&
                  r.raster.width >= width &&
                  r.raster.height >= height,
            )
            .toList()
          ..sort(_compare);
    if (candidates.isEmpty) {
      return const Left(ReadingImageSelectionFailure.renditionShortage);
    }
    final selected = candidates.first;
    final raster = selected.raster;
    // Aday iki ekseni karşıladığı için bu tek ölçek 1'i aşamaz.
    final decode = _fitDimensions(
      raster.width,
      raster.height,
      width,
      height,
      ReadingImageFit.cover,
    );
    if (decode.$1 * decode.$2 > BigInt.from(maxDecodePixels)) {
      return const Left(ReadingImageSelectionFailure.decodeResourceExceeded);
    }
    return Right(
      ReadingImageSelection(
        rendition: selected,
        geometry: ReadingImageGeometry(
          physicalWidth: physicalWidth,
          physicalHeight: physicalHeight,
          requiredWidth: width,
          requiredHeight: height,
          decodeWidth: decode.$1.toInt(),
          decodeHeight: decode.$2.toInt(),
        ),
      ),
    );
  }

  // Bu bir decode bütçesi değildir; web ve VM'de güvenli tamsayı dönüşüm sınırıdır.
  bool _representable(double value) =>
      value.isFinite && value > 0 && value <= 9007199254740991;

  // Fiziksel slot tamsayı olduktan sonra ölçek ve ceil yalnız exact kesirle hesaplanır.
  (BigInt, BigInt) _fitDimensions(
    int sourceWidth,
    int sourceHeight,
    int targetWidth,
    int targetHeight,
    ReadingImageFit fit,
  ) {
    final sw = BigInt.from(sourceWidth);
    final sh = BigInt.from(sourceHeight);
    final tw = BigInt.from(targetWidth);
    final th = BigInt.from(targetHeight);
    final comparison = (tw * sh).compareTo(th * sw);
    final useWidth = fit == ReadingImageFit.contain
        ? comparison <= 0
        : comparison >= 0;
    final numerator = useWidth ? tw : th;
    final denominator = useWidth ? sw : sh;
    return (
      (sw * numerator + denominator - BigInt.one) ~/ denominator,
      (sh * numerator + denominator - BigInt.one) ~/ denominator,
    );
  }

  int _compare(ReadingImageRendition a, ReadingImageRendition b) {
    final pixels = _area(
      a.raster.width,
      a.raster.height,
    ).compareTo(_area(b.raster.width, b.raster.height));
    if (pixels != 0) return pixels;
    final bytes = a.raster.byteSize.compareTo(b.raster.byteSize);
    return bytes != 0 ? bytes : a.renditionKey.compareTo(b.renditionKey);
  }

  BigInt _area(int width, int height) =>
      BigInt.from(width) * BigInt.from(height);
}
