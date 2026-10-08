import 'dart:ui' show Tristate;
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/theme/library_theme.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import '../library/library_fixture.dart' show configuration;
import 'story_detail_fixture.dart';

void main() {
  tearDown(() => libraryGetIt.reset());
  testWidgets(
    'very long unavailable-artwork title at 1.3 keeps full semantics and reachable retry inside the hero',
    (tester) async {
      const size = Size(411, 914);
      tester.view.physicalSize = size;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final config = configuration();
      libraryGetIt.registerSingleton<LibraryConfiguration>(config);
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
      final load = cubit.load(identity());
      final title = 'A very long complete localized story title ' * 60;
      repo.responses[0].complete(
        right(StoryDetailModel.fromJson({...payload(), 'title': title})),
      );
      await load;
      final semantics = tester.ensureSemantics();
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTypography.theme(),
          home: MediaQuery(
            data: const MediaQueryData(
              size: size,
              textScaler: TextScaler.linear(1.3),
            ),
            child: BlocProvider.value(
              value: cubit,
              child: const StoryDetailView(),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      expect(
        tester
            .getSemantics(find.byType(StoryDetailHero))
            .getSemanticsData()
            .label,
        contains(title),
      );
      final heroRect = tester.getRect(find.byType(StoryDetailHero));
      final retry = find.widgetWithText(
        TextButton,
        const StoryDetailStrings('tr').retryArtwork,
      );
      final retryRect = tester.getRect(retry);
      expect(heroRect.contains(retryRect.topLeft), isTrue);
      expect(heroRect.contains(retryRect.bottomRight), isTrue);
      expect(retryRect.height, greaterThanOrEqualTo(48));
      await tester.tap(retry);
      expect(repo.calls, hasLength(2));
      repo.responses.last.complete(left(const Failure(FailureKind.network)));
      await tester.pump();
      await tester.pumpWidget(const SizedBox());
      semantics.dispose();
      await cubit.close();
    },
  );
  for (final size in [const Size(411, 914), const Size(800, 1280)]) {
    for (final language in ['tr', 'en', 'pt']) {
      for (final scale in [1.0, 1.3]) {
        for (final locked in [false, true]) {
          testWidgets(
            'adaptive $size $language $scale locked=$locked retains metadata and deferred action semantics',
            (tester) async {
              tester.view.physicalSize = size;
              tester.view.devicePixelRatio = 1;
              addTearDown(tester.view.resetPhysicalSize);
              addTearDown(tester.view.resetDevicePixelRatio);
              final config = configuration(language: language);
              libraryGetIt.registerSingleton<LibraryConfiguration>(config);
              libraryGetIt.registerFactory<ArtworkCubit>(
                () => ArtworkCubit(config),
              );
              final repo = ControlledDetailRepository();
              final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
              final load = cubit.load(identity(ui: language));
              repo.responses[0].complete(
                right(detail(ui: language, locked: locked)),
              );
              await load;
              final semantics = tester.ensureSemantics();
              await tester.pumpWidget(
                MaterialApp(
                  theme: AppTypography.theme(),
                  home: MediaQuery(
                    data: MediaQueryData(
                      size: size,
                      textScaler: TextScaler.linear(scale),
                      padding: const EdgeInsets.only(top: 24, bottom: 24),
                    ),
                    child: BlocProvider.value(
                      value: cubit,
                      child: const StoryDetailView(),
                    ),
                  ),
                ),
              );
              await tester.pump();
              final strings = StoryDetailStrings(language);
              final heroLabel = tester
                  .getSemantics(find.byType(StoryDetailHero))
                  .getSemanticsData()
                  .label;
              expect(
                RegExp(
                  RegExp.escape(cubit.state.detail!.title),
                ).allMatches(heroLabel).length,
                1,
                reason:
                    'Artwork failure announces the retained story title once',
              );
              final lock = find.bySemanticsLabel(strings.locked);
              expect(lock, locked ? findsOneWidget : findsNothing);
              if (locked) {
                expect(tester.getSize(lock), const Size(48, 48));
              }
              expect(
                tester
                    .widget<ImageFiltered>(find.byType(ImageFiltered))
                    .enabled,
                false,
                reason: 'Missing artwork error/title must remain readable',
              );
              final cta = find.bySemanticsLabel(
                locked ? strings.premium : strings.start,
              );
              await tester.ensureVisible(cta);
              await tester.pumpAndSettle();
              expect(
                tester.getSemantics(cta).flagsCollection.isEnabled,
                Tristate.isFalse,
              );
              expect(tester.getSize(cta).height, greaterThanOrEqualTo(48));
              expect(find.text(strings.similar), findsNothing);
              await tester.ensureVisible(find.byType(Switch));
              await tester.pumpAndSettle();
              expect(
                tester
                    .getSemantics(find.byType(Switch))
                    .getSemanticsData()
                    .label,
                strings.narration,
                reason:
                    'The narration control exposes one localized accessible name',
              );
              await tester.tap(find.byType(Switch));
              await tester.pump();
              expect(cubit.state.narration, false);
              expect(repo.calls, hasLength(1));
              await tester.drag(
                find.byType(ListView).first,
                const Offset(0, -500),
              );
              await tester.pumpAndSettle();
              expect(
                find.text('Author Name with a long complete credit'),
                findsOneWidget,
              );
              expect(tester.takeException(), isNull);
              await tester.pumpWidget(const SizedBox());
              await cubit.close();
              semantics.dispose();
            },
          );
        }
      }
    }
  }
  testWidgets(
    'loading, network retry and unavailable keep reachable localized back without stale detail',
    (tester) async {
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(
        GetStoryDetail(repo),
        configuration(language: 'pt'),
      );
      final first = cubit.load(identity(ui: 'pt', lang: 'en'));
      await tester.pumpWidget(
        MaterialApp(
          home: BlocProvider.value(
            value: cubit,
            child: const StoryDetailView(),
          ),
        ),
      );
      const strings = StoryDetailStrings('pt');
      expect(find.text(strings.loading), findsOneWidget);
      expect(
        tester.getSize(find.byTooltip(strings.back)).height,
        greaterThanOrEqualTo(48),
      );
      repo.responses[0].complete(left(const Failure(FailureKind.network)));
      await first;
      await tester.pump();
      expect(find.text(strings.error), findsOneWidget);
      await tester.tap(find.text(strings.retry));
      await tester.pump();
      expect(repo.calls[1], repo.calls[0]);
      expect(find.text(strings.loading), findsOneWidget);
      repo.responses[1].complete(left(const Failure(FailureKind.unavailable)));
      await tester.pumpAndSettle();
      expect(find.text(strings.unavailable), findsOneWidget);
      expect(find.text(strings.retry), findsNothing);
      expect(find.byType(Switch), findsNothing);
      expect(repo.calls, hasLength(2));
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
    },
  );
}
