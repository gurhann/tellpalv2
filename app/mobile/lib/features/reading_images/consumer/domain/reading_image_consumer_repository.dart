import 'dart:typed_data';
import '../../domain/entity/reading_image_manifest.dart';
import 'package:dartz/dartz.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../../../../core/error/failure.dart';
import 'reading_image_admission.dart';

abstract interface class ReadingImageConsumerRepository {
  Future<Either<Failure, Uint8List>> bytes(
    ReadingImageAdmission admission,
    ReadingImageBinding binding,
    ReadingImageSelection selection,
    ReadingImageCancellation cancellation,
  );
  Future<Either<Failure, AdmittedReadingImage>> admit(
    int contentId,
    String language,
    ReadingImageCancellation cancellation,
  );
  Future<Either<Failure, ReadingImageAdmission>> status(
    ReadingImageAdmission anchor,
    ReadingImageCancellation cancellation,
  );
}
