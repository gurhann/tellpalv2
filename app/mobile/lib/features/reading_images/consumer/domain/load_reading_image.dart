import 'dart:typed_data';
import '../../domain/entity/reading_image_manifest.dart';
import 'package:dartz/dartz.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import 'reading_image_consumer_repository.dart';
import 'reading_image_admission.dart';

@injectable
class LoadReadingImage {
  const LoadReadingImage(this.repository);
  final ReadingImageConsumerRepository repository;
  Future<Either<Failure, Uint8List>> bytes(
    ReadingImageAdmission admission,
    ReadingImageBinding binding,
    ReadingImageSelection selection,
    ReadingImageCancellation cancellation,
  ) => repository.bytes(admission, binding, selection, cancellation);
  Future<Either<Failure, AdmittedReadingImage>> call(
    int id,
    String language,
    ReadingImageCancellation cancellation,
  ) => repository.admit(id, language, cancellation);
  Future<Either<Failure, ReadingImageAdmission>> status(
    ReadingImageAdmission anchor,
    ReadingImageCancellation cancellation,
  ) => repository.status(anchor, cancellation);
}
