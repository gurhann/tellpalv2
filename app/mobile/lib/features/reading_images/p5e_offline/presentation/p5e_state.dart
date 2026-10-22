import 'package:freezed_annotation/freezed_annotation.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
part 'p5e_state.freezed.dart';

@freezed
abstract class P5eState with _$P5eState {
  const factory P5eState({
    @Default('initial') String status,
    @Default('unknown') String network,
    @Default(0) int index,
    @Default(0) int epoch,
    @Default(0) int visitedViews,
    @Default(false) bool crop,
    ReadingComparisonRaster? raster,
    String? error,
  }) = _P5eState;
}
