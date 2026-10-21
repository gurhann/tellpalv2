import 'package:freezed_annotation/freezed_annotation.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
part 'p5d_state.freezed.dart';

@freezed
abstract class P5dState with _$P5dState {
  const factory P5dState({
    @Default('initial') String status,
    @Default(0) int epoch,
    @Default(0) int index,
    @Default(false) bool crop,
    ReadingComparisonRaster? raster,
    String? error,
  }) = _P5dState;
}
