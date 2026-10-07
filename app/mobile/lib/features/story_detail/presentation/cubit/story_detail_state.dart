import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../../core/error/failure.dart';
import '../../domain/entity/story_detail.dart';
part 'story_detail_state.freezed.dart';

enum StoryDetailStatus { loading, loaded, error, unavailable }

enum StoryHeroStatus { loading, loaded, error, suspended }

@freezed
abstract class StoryDetailState with _$StoryDetailState {
  const factory StoryDetailState({
    StoryDetailIdentity? identity,
    @Default(0) int attempt,
    @Default(StoryDetailStatus.loading) StoryDetailStatus status,
    StoryDetail? detail,
    Failure? failure,
    @Default(true) bool narration,
    @Default(true) bool visible,
    @Default(StoryHeroStatus.loading) StoryHeroStatus heroStatus,
    @Default(0) int heroAttempt,
    @Default(false) bool recoveringArtwork,
  }) = _StoryDetailState;
}
