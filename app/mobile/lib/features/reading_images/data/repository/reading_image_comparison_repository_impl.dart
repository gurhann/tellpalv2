import 'package:crypto/crypto.dart';
import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/repository/reading_image_comparison_repository.dart';
import '../data_source/reading_image_comparison_data_source.dart';
import '../model/reading_image_comparison_model.dart';

@Injectable(as: ReadingImageComparisonRepository)
class ReadingImageComparisonRepositoryImpl
    implements ReadingImageComparisonRepository {
  const ReadingImageComparisonRepositoryImpl(this.source, this.configuration);
  final ReadingImageComparisonDataSource source;
  final ReadingImageComparisonConfiguration configuration;
  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation cancellation,
  ) => _capture('manifest', {
    'contentId': '$id',
    'language': 'tr',
  }, cancellation);
  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison value,
    ReadingComparisonCancellation cancellation,
  ) => _capture('status', identityQuery(value), cancellation);
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> _capture(
    String path,
    Map<String, String> query,
    ReadingComparisonCancellation cancellation,
  ) async {
    try {
      return Right(
        ReadingImageComparisonModel.fromJson(
          await source.json(path, query, cancellation),
        ),
      );
    } catch (_) {
      return const Left(ReadingComparisonFailure('admission_refused'));
    }
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonicalSource,
    ReadingComparisonCancellation cancellation,
  ) async {
    try {
      final entry = value.manifest.images.singleWhere(
        (e) => e.source.binding == binding,
      );
      final raster = canonicalSource ? entry.source.raster : rendition.raster;
      final limits = configuration.limits!;
      if (raster.byteSize > limits.maxEncodedBytes) {
        throw StateError('encoded_limit');
      }
      final data = await source.read(
        'bytes',
        {
          ...identityQuery(value),
          'role': switch (binding.role) {
            ReadingImageRole.readingCover => 'READING_COVER',
            ReadingImageRole.readingDetail => 'READING_DETAIL',
            ReadingImageRole.readingPage => 'READING_PAGE',
          },
          'pageNumber': '${binding.pageNumber}',
          'renditionKey': rendition.renditionKey,
          'canonicalSource': '$canonicalSource',
        },
        cancellation,
        limits.maxEncodedBytes,
      );
      if (data.length != raster.byteSize ||
          sha256.convert(data).toString() != raster.sha256) {
        throw StateError('byte_integrity');
      }
      return Right(data);
    } catch (_) {
      return const Left(
        ReadingComparisonFailure('byte_integrity_or_transfer_refused'),
      );
    }
  }

  static Map<String, String> identityQuery(ReadingImageComparison value) {
    final i = value.manifest.identity;
    return {
      'contentId': '${i.contentId}',
      'language': i.language,
      'liveGeneration': '${value.liveGeneration}',
      'revision': i.revision,
      'manifestDigest': i.manifestDigest,
      'jobId': i.jobId,
    };
  }
}
