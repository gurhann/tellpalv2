import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import '../library/library_fixture.dart' show configuration;
import 'story_detail_fixture.dart';

void main() {
  test(
    'reordered and disposed detail callbacks cannot replace selected identity',
    () async {
      final repo = ControlledDetailRepository(),
          cubit = StoryDetailCubit(
            GetStoryDetail(ControlledDetailRepository()),
            configuration(),
          );
      await cubit.close();
      final current = StoryDetailCubit(GetStoryDetail(repo), configuration());
      final old = current.load(identity()),
          fresh = current.load(identity(id: 2, ui: 'tr', lang: 'en'));
      repo.responses[1].complete(right(detail(id: 2, ui: 'tr', lang: 'en')));
      await fresh;
      repo.responses[0].complete(right(detail()));
      await old;
      expect(current.state.detail!.contentId, 2);
      expect(current.state.detail!.contentLanguage, 'en');
      final pending = current.load(identity(id: 3));
      await current.close();
      repo.responses[2].complete(right(detail(id: 3)));
      await pending;
      expect(current.state.detail, isNull);
    },
  );
  test(
    'explicit retry uses current context and bounded timeout ignores late result',
    () async {
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(
        GetStoryDetail(repo),
        configuration(wait: const Duration(milliseconds: 20)),
      );
      await cubit.load(identity(ui: 'pt', lang: 'en'));
      expect(cubit.state.status, StoryDetailStatus.error);
      expect(cubit.state.failure!.kind, FailureKind.timeout);
      final retry = cubit.load(cubit.state.identity!);
      repo.responses[1].complete(right(detail(ui: 'pt', lang: 'en')));
      await retry;
      repo.responses[0].complete(right(detail(id: 8)));
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state.detail!.contentId, 1);
      expect(repo.calls[1], repo.calls[0]);
      await cubit.close();
    },
  );
  test(
    'unavailable clears old metadata while artwork recovery preserves current metadata',
    () async {
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), configuration());
      final initial = cubit.load(identity());
      repo.responses[0].complete(right(detail()));
      await initial;
      cubit.toggleNarration(false);
      final recovery = cubit.load(identity(), artworkOnly: true);
      await cubit.load(identity(), artworkOnly: true);
      expect(repo.calls, hasLength(2));
      repo.responses[1].complete(left(const Failure(FailureKind.network)));
      await recovery;
      expect(cubit.state.status, StoryDetailStatus.loaded);
      expect(cubit.state.heroStatus, StoryHeroStatus.error);
      expect(cubit.state.narration, false);
      final unavailable = cubit.load(identity(), artworkOnly: true);
      repo.responses[2].complete(left(const Failure(FailureKind.unavailable)));
      await unavailable;
      expect(cubit.state.status, StoryDetailStatus.unavailable);
      expect(cubit.state.detail, isNull);
      await cubit.close();
    },
  );
  test(
    'hidden routes release artwork state and preserve metadata/narration with stale image rejection',
    () async {
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), configuration());
      final initial = cubit.load(identity());
      repo.responses[0].complete(right(detail()));
      await initial;
      cubit.toggleNarration(false);
      final old = cubit.state.heroAttempt;
      cubit.setVisible(false);
      cubit.artworkLoaded(old);
      expect(cubit.state.heroStatus, StoryHeroStatus.suspended);
      expect(cubit.state.narration, false);
      expect(cubit.state.detail!.contentId, 1);
      cubit.setVisible(true);
      cubit.artworkLoaded(old);
      expect(cubit.state.heroStatus, StoryHeroStatus.error);
      await cubit.close();
    },
  );
}
