import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/library_configuration.dart';
import '../../../../core/error/failure.dart';
import '../../domain/entity/story_detail.dart';
import '../../domain/use_case/get_story_detail.dart';
import 'story_detail_state.dart';

@injectable
class StoryDetailCubit extends Cubit<StoryDetailState> {
  StoryDetailCubit(this._getDetail, this._configuration)
    : super(const StoryDetailState());
  final GetStoryDetail _getDetail;
  final LibraryConfiguration _configuration;
  int _attempt = 0;
  int _heroAttempt = 0;
  Timer? _artworkTimer;

  Future<void> load(
    StoryDetailIdentity identity, {
    bool artworkOnly = false,
  }) async {
    if (isClosed || (artworkOnly && state.recoveringArtwork)) return;
    final attempt = ++_attempt;
    _artworkTimer?.cancel();
    final retained =
        artworkOnly &&
        state.identity == identity &&
        state.status == StoryDetailStatus.loaded;
    emit(
      retained
          ? state.copyWith(
              attempt: attempt,
              recoveringArtwork: true,
              heroStatus: StoryHeroStatus.loading,
              heroAttempt: ++_heroAttempt,
            )
          : StoryDetailState(
              identity: identity,
              attempt: attempt,
              heroAttempt: ++_heroAttempt,
              visible: state.visible,
            ),
    );
    final result = await _getDetail(identity).timeout(
      _configuration.catalogWait,
      onTimeout: () => left(const Failure(FailureKind.timeout)),
    );
    if (isClosed || _attempt != attempt || state.identity != identity) return;
    result.fold(
      (failure) {
        if (failure.kind == FailureKind.unavailable ||
            failure.kind == FailureKind.unauthorized ||
            failure.kind == FailureKind.forbidden) {
          emit(
            state.copyWith(
              status: failure.kind == FailureKind.unavailable
                  ? StoryDetailStatus.unavailable
                  : StoryDetailStatus.error,
              detail: null,
              failure: failure,
              recoveringArtwork: false,
            ),
          );
        } else if (retained) {
          emit(
            state.copyWith(
              heroStatus: StoryHeroStatus.error,
              failure: failure,
              recoveringArtwork: false,
            ),
          );
        } else {
          emit(
            state.copyWith(
              status: StoryDetailStatus.error,
              failure: failure,
              recoveringArtwork: false,
            ),
          );
        }
      },
      (detail) {
        emit(
          state.copyWith(
            status: StoryDetailStatus.loaded,
            detail: detail,
            failure: null,
            heroAttempt: ++_heroAttempt,
            heroStatus: !state.visible
                ? StoryHeroStatus.suspended
                : detail.hero == null
                ? StoryHeroStatus.error
                : StoryHeroStatus.loading,
            recoveringArtwork: false,
          ),
        );
        _startArtworkWait();
      },
    );
  }

  void _startArtworkWait() {
    _artworkTimer?.cancel();
    if (state.heroStatus != StoryHeroStatus.loading) return;
    final attempt = state.heroAttempt;
    _artworkTimer = Timer(
      _configuration.artworkWait,
      () => artworkFailed(attempt),
    );
  }

  void artworkLoaded(int attempt) {
    if (isClosed ||
        !state.visible ||
        attempt != state.heroAttempt ||
        state.heroStatus != StoryHeroStatus.loading) {
      return;
    }
    _artworkTimer?.cancel();
    emit(state.copyWith(heroStatus: StoryHeroStatus.loaded));
  }

  void artworkFailed(int attempt) {
    if (isClosed ||
        !state.visible ||
        attempt != state.heroAttempt ||
        state.heroStatus != StoryHeroStatus.loading) {
      return;
    }
    _artworkTimer?.cancel();
    emit(state.copyWith(heroStatus: StoryHeroStatus.error));
  }

  void setVisible(bool visible) {
    if (isClosed || state.visible == visible) return;
    _artworkTimer?.cancel();
    emit(
      state.copyWith(
        visible: visible,
        heroAttempt: ++_heroAttempt,
        heroStatus: !visible
            ? StoryHeroStatus.suspended
            : state.detail?.hero == null
            ? StoryHeroStatus.error
            : StoryHeroStatus.loading,
      ),
    );
    _startArtworkWait();
  }

  void toggleNarration(bool value) {
    if (!isClosed) emit(state.copyWith(narration: value));
  }

  @override
  Future<void> close() {
    _attempt++;
    _artworkTimer?.cancel();
    return super.close();
  }
}
