import 'package:freezed_annotation/freezed_annotation.dart';
part 'reader_resolution_state.freezed.dart';

enum ReaderResolutionStatus {
  initial,
  baseline,
  reading,
  natural,
  evicted,
  complete,
  refused,
}

@freezed
abstract class ReaderResolutionState with _$ReaderResolutionState {
  const factory ReaderResolutionState({
    @Default(ReaderResolutionStatus.initial) ReaderResolutionStatus status,
  }) = _ReaderResolutionState;
}
