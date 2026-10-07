import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_app.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/domain/repository/library_repository.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/repository/story_detail_repository.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import '../library/library_fixture.dart' as home;
import 'story_detail_fixture.dart';

class ImmediateLibrary implements LibraryRepository {
  int calls = 0;
  @override
  Future<Either<Failure, LibraryCatalog>> fetch(LibraryContext context) async {
    calls++;
    return right(home.catalog(context.language));
  }
}

class ImmediateDetails implements StoryDetailRepository {
  final calls = <StoryDetailIdentity>[];
  @override
  Future<Either<Failure, StoryDetail>> fetch(
    StoryDetailIdentity identity,
  ) async {
    calls.add(identity);
    return right(
      detail(
        id: identity.contentId,
        ui: identity.uiLanguage,
        lang: identity.contentLanguage,
        recommendations: true,
      ),
    );
  }
}

class EnabledLibraryRouter extends LibraryRouter {
  @override List<AutoRoute> get routes => [
    AutoRoute(page:PageInfo(LibraryRoute.name,builder:(_)=>BlocProvider(create:(_)=>libraryGetIt<LibraryCubit>()..load(),child:const LibraryView(detailEnabled:true))),initial:true),
    AutoRoute(page:StoryDetailRoute.page),
  ];
}
void main() {
  tearDown(() => libraryGetIt.reset());
  testWidgets('enabled Library card carries IDs, reading and both language scalars with one repeated route',(tester)async{
    configureLibraryDependencies(home.configuration());final library=ImmediateLibrary(),details=ImmediateDetails();
    libraryGetIt.unregister<LibraryRepository>();libraryGetIt.registerSingleton<LibraryRepository>(library);
    libraryGetIt.unregister<StoryDetailRepository>();libraryGetIt.registerSingleton<StoryDetailRepository>(details);
    libraryGetIt.unregister<LibraryRouter>();libraryGetIt.registerSingleton<LibraryRouter>(EnabledLibraryRouter());
    await tester.pumpWidget(const LibraryApp());await tester.pumpAndSettle();
    final card=tester.widget<LibraryCard>(find.byType(LibraryCard).first);
    final expected=card.item;unawaited(card.onTap!());unawaited(card.onTap!());await tester.pumpAndSettle();
    expect(details.calls,hasLength(1));expect(details.calls.single.contentId,expected.contentId);expect(details.calls.single.contentLanguage,expected.contentLanguage);expect(details.calls.single.uiLanguage,'tr');expect(details.calls.single.experience,'READING');expect(libraryGetIt<LibraryRouter>().stack,hasLength(2));
    await libraryGetIt<LibraryRouter>().maybePop();await tester.pumpAndSettle();unawaited(card.onTap!());await tester.pumpAndSettle();expect(details.calls,hasLength(2));
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets(
    'nested repeated recommendation selection restores source detail preference/scroll and Library stack/scroll',
    (tester) async {
      tester.view.physicalSize = const Size(411, 914);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      configureLibraryDependencies(home.configuration());
      final catalog = ImmediateLibrary(), details = ImmediateDetails();
      libraryGetIt.unregister<LibraryRepository>();
      libraryGetIt.registerSingleton<LibraryRepository>(catalog);
      libraryGetIt.unregister<StoryDetailRepository>();
      libraryGetIt.registerSingleton<StoryDetailRepository>(details);
      await tester.pumpWidget(const LibraryApp());
      await tester.pumpAndSettle();
      final vertical = find.byKey(const PageStorageKey('library-scroll'));
      await tester.drag(vertical, const Offset(0, -180));
      await tester.pumpAndSettle();
      final libraryOffset = tester
          .state<ScrollableState>(
            find
                .descendant(of: vertical, matching: find.byType(Scrollable))
                .first,
          )
          .position
          .pixels;
      final router = libraryGetIt<LibraryRouter>();
      unawaited(
        router.push(
          StoryDetailRoute(
            categoryId: 2,
            contentId: 1,
            uiLanguage: 'tr',
            contentLanguage: 'en',
          ),
        ),
      );
      await tester.pumpAndSettle();
      final source = tester
          .element(find.byType(StoryDetailView))
          .read<StoryDetailCubit>();
      expect(source.state.detail!.contentLanguage, 'en');
      await tester.ensureVisible(find.byType(Switch));
      await tester.tap(find.byType(Switch));
      await tester.pumpAndSettle();
      expect(source.state.narration, false);
      await tester.drag(find.byType(ListView).first, const Offset(0, -900));
      await tester.pumpAndSettle();
      final sourceOffset = tester
          .state<ScrollableState>(
            find
                .descendant(
                  of: find.byType(ListView).first,
                  matching: find.byType(Scrollable),
                )
                .first,
          )
          .position
          .pixels;
      final card = tester.widget<LibraryCard>(find.byType(LibraryCard).last);
      unawaited(card.onTap!());
      unawaited(card.onTap!());
      await tester.pumpAndSettle();
      expect(router.stack, hasLength(3));
      expect(details.calls, hasLength(2));
      expect(source.state.visible, false);
      expect(source.state.narration, false);
      expect(details.calls.last.contentLanguage, 'en');
      await router.maybePop();
      await tester.pumpAndSettle();
      expect(source.state.visible, true);
      expect(source.state.narration, false);
      expect(
        tester
            .state<ScrollableState>(
              find
                  .descendant(
                    of: find.byType(ListView).first,
                    matching: find.byType(Scrollable),
                  )
                  .first,
            )
            .position
            .pixels,
        sourceOffset,
      );
      unawaited(card.onTap!());
      await tester.pumpAndSettle();
      expect(details.calls, hasLength(3));
      await router.maybePop();
      await tester.pumpAndSettle();
      await router.maybePop();
      await tester.pumpAndSettle();
      expect(router.stack, hasLength(1));
      expect(catalog.calls, 1);
      expect(
        tester
            .state<ScrollableState>(
              find
                  .descendant(of: vertical, matching: find.byType(Scrollable))
                  .first,
            )
            .position
            .pixels,
        libraryOffset,
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
