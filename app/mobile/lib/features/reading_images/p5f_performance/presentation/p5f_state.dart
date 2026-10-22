import 'package:freezed_annotation/freezed_annotation.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
part 'p5f_state.freezed.dart';

@freezed
abstract class P5fState with _$P5fState {
  const factory P5fState({
    @Default('initial') String status,
    @Default(0) int completed,
    @Default(0) int index,
    @Default(0) int epoch,
    ReadingComparisonRaster? raster,
    String? error,
  }) = _P5fState;
}
