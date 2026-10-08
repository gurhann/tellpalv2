import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' show SemanticsAction, Tristate;

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'package:tellpal_mobile_poc/core/preview/presentation/widgets/preview_card.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;
import 'library_live_test.dart' show LibraryTestBinding;

/// Parent temporarily withdraws content 10 in the isolated SQL fixture and restores it in finally.
/// This target admits the real category 3/content 9 EN response, with TR UI and no proxy/fake metadata.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'real mapped detail omits empty credits and recommendation area',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        LibraryDevHarness.scaleNotifier.value = 1.3;
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() predicate) async {
          final deadline = DateTime.now().add(const Duration(seconds: 60));
          while (!predicate()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native empty-area state deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        Future<void> capture(String name) async {
          for (var frame = 0; frame < 8; frame++) {
            await tester.pump(const Duration(milliseconds: 16));
          }
          await binding.takeScreenshot(name);
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final library = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        await until(() => library.state.status != LibraryStatus.loading);
        expect(library.state.status, LibraryStatus.loaded);
        expect(library.state.context.language, 'tr');
        final catalog = library.state.catalog!;
        final row = catalog.rows.singleWhere((row) => row.categoryId == 3);
        final item = row.items.singleWhere((item) => item.contentId == 9);
        expect(row.contentLanguage, 'en');
        expect(item.contentLanguage, 'en');
        final config = libraryGetIt<LibraryConfiguration>();
        final client = HttpClient()
          ..connectionTimeout = const Duration(seconds: 15);
        late StoryDetailModel baseline;
        try {
          final uri = config.baseUrl
              .resolve('/api/test/library/stories/9')
              .replace(
                queryParameters: {
                  'categoryId': '3',
                  'lang': 'tr',
                  'contentLang': 'en',
                  'experience': 'READING',
                },
              );
          final request = await client
              .getUrl(uri)
              .timeout(const Duration(seconds: 15));
          request.headers.set(
            HttpHeaders.authorizationHeader,
            'Bearer ${config.idToken}',
          );
          final response = await request.close().timeout(
            const Duration(seconds: 15),
          );
          expect(response.statusCode, HttpStatus.ok);
          final bytes = BytesBuilder(copy: false);
          await for (final chunk in response.timeout(
            const Duration(seconds: 15),
          )) {
            bytes.add(chunk);
            if (bytes.length > 1048576) {
              fail('Independent detail metadata exceeded limit');
            }
          }
          baseline = StoryDetailModel.fromJson(
            jsonDecode(utf8.decode(bytes.takeBytes())) as Map<String, dynamic>,
          );
        } finally {
          client.close(force: true);
        }
        expect(baseline.categoryId, 3);
        expect(baseline.contentId, 9);
        expect(baseline.requestedLanguage, 'tr');
        expect(baseline.contentLanguage, 'en');
        expect(baseline.credits, isEmpty);
        expect(baseline.recommendations, isEmpty);
        expect(baseline.hero, isNotNull);
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        final vertical = find.byKey(const PageStorageKey('library-scroll'));
        final horizontal = find.byKey(const PageStorageKey('row-3'));
        for (var seek = 0; seek < 20 && horizontal.evaluate().isEmpty; seek++) {
          await tester.drag(vertical, const Offset(0, -180));
          await tester.pumpAndSettle();
        }
        expect(horizontal, findsOneWidget);
        await tester.ensureVisible(horizontal);
        final rowScrollable = find
            .descendant(of: horizontal, matching: find.byType(Scrollable))
            .first;
        tester.state<ScrollableState>(rowScrollable).position.jumpTo(0);
        await tester.pumpAndSettle();
        final card = find.descendant(
          of: horizontal,
          matching: find.byWidgetPredicate(
            (widget) => widget is LibraryCard && widget.item.contentId == 9,
          ),
        );
        expect(card, findsOneWidget);
        await tester.ensureVisible(card);
        await tester.tap(card);
        await tester.pump();
        await until(() => find.byType(StoryDetailView).evaluate().isNotEmpty);
        final cubit = tester
            .element(find.byType(StoryDetailView))
            .read<StoryDetailCubit>();
        await until(() => cubit.state.status != StoryDetailStatus.loading);
        expect(cubit.state.status, StoryDetailStatus.loaded);
        expect(cubit.state.identity!.categoryId, 3);
        expect(cubit.state.identity!.contentId, 9);
        expect(cubit.state.identity!.uiLanguage, 'tr');
        expect(cubit.state.identity!.contentLanguage, 'en');
        expect(cubit.state.detail!.credits, isEmpty);
        expect(cubit.state.detail!.recommendations, isEmpty);
        expect(cubit.state.detail!.title, baseline.title);
        expect(cubit.state.detail!.isFree, baseline.isFree);
        expect(cubit.state.detail!.isLocked, baseline.isLocked);
        expect(cubit.state.detail!.hero!.assetId, baseline.hero!.assetId);
        expect(
          cubit.state.detail!.hero!.checksumSha256,
          baseline.hero!.checksumSha256,
        );
        await until(() => cubit.state.heroStatus != StoryHeroStatus.loading);
        expect(cubit.state.heroStatus, StoryHeroStatus.loaded);
        final raw = find.descendant(
          of: find.byType(StoryDetailHero),
          matching: find.byType(RawImage),
        );
        await until(
          () =>
              raw.evaluate().isNotEmpty &&
              tester.widget<RawImage>(raw.first).image != null,
        );
        final bitmap = tester.widget<RawImage>(raw.first).image!;
        expect(
          bitmap.width * bitmap.height * 4,
          lessThanOrEqualTo(16 * 1024 * 1024),
        );
        const strings = StoryDetailStrings('tr');
        final action = find.bySemanticsLabel(
          baseline.isLocked ? strings.premium : strings.start,
        );
        expect(
          tester.getSemantics(action).flagsCollection.isEnabled,
          Tristate.isFalse,
        );
        expect(
          tester
              .getSemantics(action)
              .getSemanticsData()
              .hasAction(SemanticsAction.tap),
          isFalse,
        );
        await capture('detail-empty-mapped-en-tr-ui-1.3-loaded');
        await tester.drag(find.byType(ListView).first, const Offset(0, -3000));
        await tester.pumpAndSettle();
        expect(find.text(strings.similar), findsNothing);
        expect(find.byType(PreviewCard), findsNothing);
        for (final role in ['AUTHOR', 'NARRATOR', 'ILLUSTRATOR', 'MUSICIAN']) {
          expect(find.text(strings.role(role)), findsNothing);
        }
        for (final placeholder in ['-', 'N/A', 'Unknown']) {
          expect(find.text(placeholder), findsNothing);
        }
        await capture(
          'detail-empty-mapped-en-tr-ui-1.3-no-credit-or-recommendation-area',
        );
        // Finish assertions at the loaded detail top; standard test cleanup later unmounts it.
        // Parent uses the existing normal development entry for separate platform AX inspection.
        await tester.drag(find.byType(ListView).first, const Offset(0, 3000));
        await tester.pumpAndSettle();
        expect(libraryGetIt<LibraryRouter>().stack, hasLength(2));
        expect(cubit.state.status, StoryDetailStatus.loaded);
        expect(tester.takeException(), isNull);
        binding.reportData ??= {};
        binding.reportData!['story12EmptyAreas'] = {
          'platform': Platform.operatingSystem,
          'operatingSystemVersion': Platform.operatingSystemVersion,
          'logicalSize': {
            'width':
                tester.view.physicalSize.width / tester.view.devicePixelRatio,
            'height':
                tester.view.physicalSize.height / tester.view.devicePixelRatio,
          },
          'textScale': 1.3,
          'categoryId': 3,
          'contentId': 9,
          'uiLanguage': 'tr',
          'contentLanguage': 'en',
          'audience': baseline.audience,
          'actualBackendNoProxy': true,
          'realCardTap': true,
          'freshHttpEmptyCredits': true,
          'freshHttpEmptyRecommendations': true,
          'nativeCreditsAndRecommendationAreaAbsent': true,
          'roleOrUnknownPlaceholdersAbsent': true,
          'heroAssetId': baseline.hero!.assetId,
          'heroChecksumSha256': baseline.hero!.checksumSha256,
          'decodedHero': {'width': bitmap.width, 'height': bitmap.height},
          'loadedDetailAtEndOfAssertions': true,
          'standardTestCleanupUnmountsSurface': true,
          'platformAccessibilityInspection': 'not executed by this target',
          'sourceAndTemporaryFixtureIdentity':
              'Recorded separately by parent, including SQL finally restoration',
        };
      } finally {
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 5)),
  );
}
