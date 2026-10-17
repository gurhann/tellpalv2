import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../widgets/reading_image_comparison_raster.dart';
part 'reading_image_comparison_state.freezed.dart';

@freezed
abstract class ReadingImageComparisonState with _$ReadingImageComparisonState {
  const factory ReadingImageComparisonState({
    @Default('initial') String status,
    String? error,
    ReadingImageComparison? comparison,
    ReadingComparisonRaster? raster,
    ReadingImageGeometry? geometry,
    @Default(0) int entryIndex,
    @Default(false) bool canonicalSource,
    @Default(0) int epoch,
  }) = _ReadingImageComparisonState;
}
