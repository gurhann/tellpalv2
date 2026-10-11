import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entity/raster_case.dart';
part 'raster_case_state.freezed.dart';

enum RasterCaseStatus { initial, loading, error, empty, loaded, refused }

@freezed
abstract class RasterCaseState with _$RasterCaseState {
  const factory RasterCaseState({
    @Default(RasterCaseStatus.initial) RasterCaseStatus status,
    RasterCase? fixture,
    @Default(0) int attempt,
  }) = _RasterCaseState;
}
