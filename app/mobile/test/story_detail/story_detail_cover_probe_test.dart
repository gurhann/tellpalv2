import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:file/local.dart';
import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'package:tellpal_mobile_poc/core/preview/presentation/widgets/preview_card.dart';
import '../../integration_test/story_detail_cover_probe.dart';
import '../library/library_artwork_test.dart' show ControlledImageCache;
import '../library/library_fixture.dart' show configuration;

void main() {
  testWidgets(
    'cover probe scopes duplicate content, observes real decode and preserves offscreen/error candidates',
    (tester) async {
      tester.view.physicalSize = const Size(411, 914);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final temporary = (await tester.runAsync(
        () => Directory.systemTemp.createTemp('detail-cover-probe'),
      ))!;
      final file = (await tester.runAsync(
        () => File('${temporary.path}/real.png').writeAsBytes(
          base64Decode(
            'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
          ),
        ),
      ))!;
      final cache = ControlledImageCache(temporary.path);
      libraryGetIt.registerSingleton<LibraryImageCache>(cache);
      libraryGetIt.registerSingleton<LibraryDiagnostics>(LibraryDiagnostics());
      libraryGetIt.registerFactory<ArtworkCubit>(
        () => ArtworkCubit(configuration()),
      );
      addTearDown(() async {
        await libraryGetIt.reset();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
        await tester.runAsync(() => temporary.delete(recursive: true));
      });
      LibraryCard card(int id) => LibraryCard(
        item: LibraryPreview(
          contentId: id,
          externalKey: 'probe-$id',
          type: 'STORY',
          experienceType: 'READING',
          contentLanguage: 'en',
          title: 'Probe $id',
          isFree: true,
          isLocked: false,
          covers: LibraryCovers(
            phone: LibraryCover(
              assetId: id,
              kind: 'LISTING_COVER',
              mimeType: 'image/png',
              url: 'https://private.invalid/source?id=$id&secret=never-publish',
              expiresAt: DateTime.now().add(const Duration(hours: 1)),
            ),
          ),
        ),
        state: LibraryState(context: configuration().context, attempt: 7),
        strings: const LibraryStrings('en'),
      );
      const foreignKey = ValueKey('foreign-detail'),
          activeKey = ValueKey('active-detail');
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                SizedBox(
                  key: foreignKey,
                  width: 180,
                  height: 200,
                  child: card(1),
                ),
                SizedBox(
                  key: activeKey,
                  height: 914 * .230 + 16,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: 5,
                    separatorBuilder: (_, _) => const SizedBox(width: 16),
                    itemBuilder: (_, index) => SizedBox(
                      width: (914 * .230 + 16) * .75,
                      child: card(index + 1),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
      await tester.pump();
      final probe = StoryDetailCoverProbe(tester, find.byKey(activeKey));
      final cards = tester.widgetList<PreviewCard>(probe.cards).toList();
      expect(cards.map((card) => card.item.contentId), [1, 2, 3]);
      final local = cards.first;
      expect(probe.artworkState(local)?.status, ArtworkStatus.loading);
      expect(probe.target(local), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (w) => w is LibraryCard && w.item.contentId == 1,
        ),
        findsNWidgets(2),
      );
      Future<void> decode(Finder scope) async {
        final image = tester.widget<CachedNetworkImage>(
          find.descendant(of: scope, matching: find.byType(CachedNetworkImage)),
        );
        final transfer = cache.keys.indexOf(image.cacheKey!);
        expect(transfer, greaterThanOrEqualTo(0));
        await tester.runAsync(() async {
          cache.transfers[transfer].add(
            FileInfo(
              const LocalFileSystem().file(file.path),
              FileSource.Online,
              DateTime.now().add(const Duration(minutes: 1)),
              'https://private.invalid/never-publish',
            ),
          );
          unawaited(cache.transfers[transfer].close());
        });
        for (var frame = 0; frame < 20; frame++) {
          await tester.pump();
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
        }
        await tester.pumpAndSettle();
      }

      await decode(find.byKey(foreignKey));
      expect(probe.decoded(local), findsNothing);
      final globalDuplicate = find.byWidgetPredicate(
        (widget) => widget is LibraryCard && widget.item.contentId == 1,
      );
      expect(
        find.descendant(
          of: globalDuplicate,
          matching: find.byWidgetPredicate(
            (widget) => widget is RawImage && widget.image != null,
          ),
        ),
        findsOneWidget,
      );
      await decode(probe.target(local));
      expect(probe.decoded(local), findsOneWidget);
      expect(probe.artworkState(local)?.status, ArtworkStatus.loaded);
      expect(tester.widget<RawImage>(probe.decoded(local)).image!.width, 2);
      final before = probe.snapshot(cards.last);
      expect(before['globalCardCount'], 4);
      expect(before['mountedCardCount'], 4);
      expect(before['scopedCardCount'], 3);
      final partial = (before['cards'] as List)[2];
      expect(partial['intersectsViewports'], true);
      expect((partial['bounds'] as List).first, closeTo(387.33, .01));
      expect((before['cards'] as List).last['intersectsViewports'], false);
      // Cached offscreen candidates are observed separately; visible failure remains an error.
      await tester.pump(const Duration(seconds: 16));
      await tester.pump();
      final failure = probe.snapshot(cards.last);
      final failed = (failure['cards'] as List).last;
      expect(failure['waitingTargetCount'], 1);
      expect(failure['waitingContentId'], 3);
      expect((failure['cards'] as List)[2]['artworkStatus'], 'error');
      expect(failed['contentId'], 4);
      expect(failed['artworkStatus'], 'error');
      expect(failed['decodedImageCount'], 0);
      expect(probe.decoded(cards.last), findsNothing);
      expect(probe.artworkState(cards.last)?.status, ArtworkStatus.error);
      expect(jsonEncode(failure), isNot(contains('private.invalid')));
      expect(jsonEncode(failure), isNot(contains('never-publish')));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
