import 'dart:math';

import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

// Kimlik ve sayaç aynı isolate yaşamına aittir; yeni probe bunları yenilemez.
final String _lifetimeId = List.generate(
  16,
  (_) => Random.secure().nextInt(256).toRadixString(16).padLeft(2, '0'),
).join();

/// Yalnız mevcut getter'ları okur; kapasite veya native iş yönetmez.
Map<String, Object?> readingOwnedResources() => {
  'activeBuffers': ReadingComparisonRaster.activeBuffers,
  'activeDescriptors': ReadingComparisonRaster.activeDescriptors,
  'activeCodecs': ReadingComparisonRaster.activeCodecs,
  'activeImages': ReadingComparisonRaster.activeImages,
  'resourceObservation': {
    'version': 1,
    'lifetimeId': _lifetimeId,
    'reservedSlots': ReadingComparisonRaster.reservedSlots,
    'reservedDecodedBytes': ReadingComparisonRaster.reservedDecodedBytes
        .toString(),
    'bufferCreationAttempts': ReadingComparisonRaster.bufferCreationAttempts
        .toString(),
  },
};
