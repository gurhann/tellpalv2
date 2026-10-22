import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../domain/reading_image_admission.dart';
part 'reading_image_consumer_state.freezed.dart';

enum ReadingImageConsumerStatus { loading, admitted, loaded, error, suspended }

@freezed
abstract class ReadingImageConsumerState with _$ReadingImageConsumerState {
  const factory ReadingImageConsumerState({
    @Default(0) int attempt,
    @Default(ReadingImageConsumerStatus.loading)
    ReadingImageConsumerStatus status,
    AdmittedReadingImage? admission,
    ReadingImageSelection? selection,
    String? immutableKey,
    String? transferKey,
    String? url,
  }) = _ReadingImageConsumerState;
}
