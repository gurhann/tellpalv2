import 'package:dartz/dartz.dart';
import '../entity/reading_image_comparison.dart';
import '../entity/reading_image_manifest.dart';

abstract interface class ReadingImageComparisonRepository {
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation cancellation,
  );
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison comparison,
    ReadingComparisonCancellation cancellation,
  );
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison comparison,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonicalSource,
    ReadingComparisonCancellation cancellation,
  );
}
