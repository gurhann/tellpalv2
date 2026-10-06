import 'dart:ui' show SemanticsAction, Tristate;
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/theme/library_theme.dart';
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'library_fixture.dart';

void main() {
  setUpAll(() async {
    final loader = FontLoader('Lexend')
      ..addFont(rootBundle.load('assets/library/Lexend-Regular.ttf'))
      ..addFont(rootBundle.load('assets/library/Lexend-Medium.ttf'))
      ..addFont(rootBundle.load('assets/library/Lexend-SemiBold.ttf'));
    await loader.load();
    await (FontLoader(
      'MaterialIcons',
    )..addFont(rootBundle.load('fonts/MaterialIcons-Regular.otf'))).load();
  });
  tearDown(() async {
    await libraryGetIt.reset();
  });
  for (final language in ['tr', 'en', 'pt']) {
    testWidgets(
      'landscape long-title artwork error fits $language at enlarged text',
      (tester) async {
        const size = Size(914, 411);
        tester.view.physicalSize = size;
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final semantics = tester.ensureSemantics();
        final config = configuration(language: language);
        libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
        final repo = ControlledRepository();
        final cubit = LibraryCubit(
          GetLibrary(repo),
          config,
          LibraryDiagnostics(),
        );
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
                child: const LibraryView(),
              ),
            ),
          ),
        );
        final load = cubit.load();
        repo.responses[0].complete(right(catalog(language)));
        await load;
        await tester.pumpAndSettle();
        final item = catalog(language).rows.first.items.first;
        expect(
          find.bySemanticsLabel('${item.title}, ${item.contentLanguage}'),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
        semantics.dispose();
      },
    );
  }
  for (final size in [const Size(411, 914), const Size(800, 1280)]) {
    for (final language in ['tr', 'en', 'pt']) {
      for (final scale in [1.0, 1.3]) {
        testWidgets(
          'long titles, missing artwork and disabled semantics $language $size scale $scale',
          (tester) async {
            tester.view.physicalSize = size;
            tester.view.devicePixelRatio = 1;
            addTearDown(tester.view.resetPhysicalSize);
            addTearDown(tester.view.resetDevicePixelRatio);
            final semantics = tester.ensureSemantics();

            final config = configuration(language: language);
            libraryGetIt.registerFactory<ArtworkCubit>(
              () => ArtworkCubit(config),
            );
            final repo = ControlledRepository();
            final cubit = LibraryCubit(
              GetLibrary(repo),
              config,
              LibraryDiagnostics(),
            );
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
                    child: const RepaintBoundary(
                      key: ValueKey('library-golden'),
                      child: LibraryView(),
                    ),
                  ),
                ),
              ),
            );
            final load = cubit.load();
            repo.responses[0].complete(right(catalog(language)));
            await load;
            await tester.pumpAndSettle();
            final strings = LibraryStrings(language);
            for (final label in [
              strings.sleep,
              strings.profile,
              strings.premium,
              strings.search,
            ]) {
              final node = tester.getSemantics(
                find.bySemanticsLabel(label).first,
              );
              expect(node.flagsCollection.isEnabled, Tristate.isFalse);
              expect(
                node.getSemanticsData().hasAction(SemanticsAction.tap),
                isFalse,
              );
            }
            final first = catalog(language).rows.first;
            final card = first.items.first;
            for (final label in [
              '${strings.all}: ${first.name}',
              '${card.title}, ${card.contentLanguage}',
            ]) {
              final control = find.bySemanticsLabel(label).first;
              final node = tester.getSemantics(control);
              expect(node.flagsCollection.isEnabled, Tristate.isFalse);
              expect(
                node.getSemanticsData().hasAction(SemanticsAction.tap),
                isFalse,
              );
              expect(tester.getRect(control).height, greaterThanOrEqualTo(48));
              await tester.tap(control, warnIfMissed: false);
              await tester.pump();
            }
            expect(tester.takeException(), isNull);
            await expectLater(
              find.byKey(const ValueKey('library-golden')),
              matchesGoldenFile(
                'goldens/library-$language-${size.width.toInt()}-$scale.png',
              ),
            );
            final vertical = find.byKey(const PageStorageKey('library-scroll'));
            await tester.drag(vertical, const Offset(0, -350));
            await tester.pumpAndSettle();
            final scroll = tester
                .state<ScrollableState>(
                  find
                      .descendant(
                        of: vertical,
                        matching: find.byType(Scrollable),
                      )
                      .first,
                )
                .position
                .pixels;
            await tester.tap(find.bySemanticsLabel(strings.library).last);
            await tester.pump();
            expect(
              tester
                  .state<ScrollableState>(
                    find
                        .descendant(
                          of: vertical,
                          matching: find.byType(Scrollable),
                        )
                        .first,
                  )
                  .position
                  .pixels,
              scroll,
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
  testWidgets(
    'loading, empty, failure and retry remain distinct and localized',
    (tester) async {
      final config = configuration(language: 'pt');
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        config,
        LibraryDiagnostics(),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTypography.theme(),
          home: BlocProvider.value(value: cubit, child: const LibraryView()),
        ),
      );
      expect(
        find.bySemanticsLabel(const LibraryStrings('pt').loading),
        findsOneWidget,
      );
      final empty = cubit.load();
      repo.responses[0].complete(right(catalog('pt', empty: true)));
      await empty;
      await tester.pump();
      expect(find.text(const LibraryStrings('pt').empty), findsOneWidget);
      final error = cubit.load();
      repo.responses[1].complete(left(const Failure(FailureKind.network)));
      await error;
      await tester.pump();
      expect(find.text(const LibraryStrings('pt').error), findsOneWidget);
      await tester.tap(find.text(const LibraryStrings('pt').retry));
      await tester.pump();
      expect(repo.calls, hasLength(3));
      expect(
        find.bySemanticsLabel(const LibraryStrings('pt').loading),
        findsOneWidget,
      );
      repo.responses[2].complete(right(catalog('pt', empty: true)));
      await tester.pump();
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
    },
  );
}
