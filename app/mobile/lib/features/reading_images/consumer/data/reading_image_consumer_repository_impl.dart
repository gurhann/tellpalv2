import 'dart:typed_data';
import 'package:dartz/dartz.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../../../../app/reading_image_consumer_configuration.dart';
import '../../domain/use_case/validate_reading_image_manifest.dart';
import '../domain/reading_image_admission.dart';
import '../domain/reading_image_consumer_repository.dart';
import 'reading_image_admission_model.dart';
import 'reading_image_consumer_data_source.dart';

@Injectable(as: ReadingImageConsumerRepository)
class ReadingImageConsumerRepositoryImpl
    implements ReadingImageConsumerRepository {
  ReadingImageConsumerRepositoryImpl(this.source, this.config);
  final ReadingImageConsumerDataSource source;
  final ReadingImageConsumerConfiguration config;
  @override
  Future<Either<Failure, Uint8List>> bytes(
    ReadingImageAdmission admission,
    ReadingImageBinding binding,
    ReadingImageSelection selection,
    ReadingImageCancellation cancellation,
  ) async {
    try {
      final entry = admission.manifest.images.singleWhere(
        (e) =>
            e.source.binding == binding &&
            e.renditions.contains(selection.rendition),
      );
      final selectedBinding = entry.source.binding;
      final query = {
        ...admission.query,
        'role': switch (selectedBinding.role) {
          ReadingImageRole.readingCover => 'READING_COVER',
          ReadingImageRole.readingDetail => 'READING_DETAIL',
          ReadingImageRole.readingPage => 'READING_PAGE',
        },
        'pageNumber': '${selectedBinding.pageNumber}',
        'renditionKey': selection.rendition.renditionKey,
      };
      return Right(
        await source.read(
          'bytes',
          query,
          cancellation,
          config.maxEncodedBytes,
          selection.rendition.raster.mime,
        ),
      );
    } catch (_) {
      return const Left(Failure(FailureKind.unavailable));
    }
  }

  @override
  Future<Either<Failure, AdmittedReadingImage>> admit(
    int contentId,
    String language,
    ReadingImageCancellation cancellation,
  ) async {
    final elapsed = Stopwatch()..start();
    try {
      final admission = ReadingImageAdmissionModel.fromJson(
        await source.json('manifest', {
          'contentId': '$contentId',
          'language': language,
        }, cancellation),
      );
      cancellation.check();
      if (admission.accountScope != config.accountScope ||
          admission.manifest.identity.contentId != contentId ||
          admission.manifest.identity.language != language ||
          admission.liveGeneration <= 0 ||
          admission.leaseMillis <= 0 ||
          admission.leaseMillis > config.maxLease.inMilliseconds) {
        throw const FormatException('consumer_admission');
      }
      final remaining =
          Duration(milliseconds: admission.leaseMillis) - elapsed.elapsed;
      if (remaining <= Duration.zero) {
        throw const FormatException('consumer_expired');
      }
      return const ValidateReadingImageManifest()(
        manifest: admission.manifest,
        expected: admission.context,
      ).fold(
        (_) => const Left(Failure(FailureKind.invalidResponse)),
        (manifest) =>
            Right(AdmittedReadingImage(admission, manifest, remaining)),
      );
    } catch (_) {
      return const Left(Failure(FailureKind.unavailable));
    }
  }

  @override
  Future<Either<Failure, ReadingImageAdmission>> status(
    ReadingImageAdmission anchor,
    ReadingImageCancellation cancellation,
  ) async {
    try {
      final admission = ReadingImageAdmissionModel.fromJson(
        await source.json('status', anchor.query, cancellation),
      );
      cancellation.check();
      if (!anchor.sameIdentity(admission)) {
        throw const FormatException('consumer_identity_changed');
      }
      return const ValidateReadingImageManifest()(
        manifest: admission.manifest,
        expected: anchor.context,
      ).fold(
        (_) => const Left(Failure(FailureKind.invalidResponse)),
        (_) => Right(admission),
      );
    } catch (_) {
      return const Left(Failure(FailureKind.unavailable));
    }
  }
}
