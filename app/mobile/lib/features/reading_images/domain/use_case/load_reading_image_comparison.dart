import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../entity/reading_image_comparison.dart';
import '../repository/reading_image_comparison_repository.dart';
import 'validate_reading_image_manifest.dart';

@injectable
class LoadReadingImageComparison {
  const LoadReadingImageComparison(this.repository);
  final ReadingImageComparisonRepository repository;
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> call(
    int id,
    ReadingComparisonCancellation cancellation,
  ) async {
    final result = await repository.manifest(id, cancellation);
    return result.flatMap(
      (value) =>
          const ValidateReadingImageManifest()(
            manifest: value.manifest,
            expected: value.context,
          ).fold(
            (failure) => Left(ReadingComparisonFailure(failure.name)),
            (_) => Right(value),
          ),
    );
  }
}
