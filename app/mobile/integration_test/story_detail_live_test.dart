import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:ui' show SemanticsAction, Tristate;

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/core/preview/presentation/widgets/preview_card.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;

import 'library_live_test.dart' show LibraryTestBinding;
import 'story_detail_cover_probe.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/core/artwork/artwork_cubit.dart';

/// Native evidence uses the real card/route callbacks and a separate current HTTP read.
/// The host injects the designated account's temporary token in memory.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'real story detail card, recommendation, back and native image matrix',
    (tester) async {
      final semantics = tester.ensureSemantics();
      final records = <Map<String, Object?>>[];
      final observationClock = Stopwatch()..start();
      binding.reportData ??= {};
      binding.reportData!['story12PartialCases'] = records;
      final explicitCoverRecoveries = <Map<String, Object?>>[];
      binding.reportData!['story12ExplicitCoverRecoveries'] =
          explicitCoverRecoveries;
      try {
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() predicate) async {
          final deadline = DateTime.now().add(const Duration(seconds: 60));
          while (!predicate()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native detail state deadline reached');
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

        double offset(Finder list) => tester
            .state<ScrollableState>(
              find
                  .descendant(of: list, matching: find.byType(Scrollable))
                  .first,
            )
            .position
            .pixels;

        Future<StoryDetail> freshDetail(StoryDetailIdentity identity) async {
          final config = libraryGetIt<LibraryConfiguration>();
          final client = HttpClient()
            ..connectionTimeout = const Duration(seconds: 15);
          try {
            final uri = config.baseUrl
                .resolve('/api/test/library/stories/${identity.contentId}')
                .replace(
                  queryParameters: {
                    'categoryId': '${identity.categoryId}',
                    'lang': identity.uiLanguage,
                    'contentLang': identity.contentLanguage,
                    'experience': identity.experience,
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
            final bytes = <int>[];
            await for (final chunk in response.timeout(
              const Duration(seconds: 15),
            )) {
              bytes.addAll(chunk);
              if (bytes.length > 1024 * 1024) {
                fail('Independent metadata body exceeded limit');
              }
            }
            return StoryDetailModel.fromJson(
              jsonDecode(utf8.decode(bytes)) as Map<String, dynamic>,
            );
          } finally {
            client.close(force: true);
          }
        }

        // URLs and expiries intentionally never enter comparison failure output or evidence.
        Map<String, Object?> detailRecord(StoryDetail value) => {
          'schemaVersion': value.schemaVersion,
          'categoryId': value.categoryId,
          'contentId': value.contentId,
          'externalKey': value.externalKey,
          'type': value.type,
          'experience': value.experienceType,
          'uiLanguage': value.requestedLanguage,
          'contentLanguage': value.contentLanguage,
          'audience': value.audience,
          'title': value.title,
          'ageRange': value.ageRange,
          'durationMinutes': value.durationMinutes,
          'summary': value.summary,
          'isFree': value.isFree,
          'isLocked': value.isLocked,
          'credits': value.credits
              .map(
                (credit) => {
                  'contributorId': credit.contributorId,
                  'role': credit.role,
                  'displayName': credit.displayName,
                },
              )
              .toList(),
          'hero': value.hero == null
              ? null
              : {
                  'assetId': value.hero!.assetId,
                  'checksum': value.hero!.checksumSha256,
                  'mimeType': value.hero!.mimeType,
                  'byteSize': value.hero!.byteSize,
                  'width': value.hero!.width,
                  'height': value.hero!.height,
                },
          'recommendations': value.recommendations
              .map(
                (item) => {
                  'contentId': item.contentId,
                  'title': item.title,
                  'language': item.contentLanguage,
                  'isFree': item.isFree,
                  'isLocked': item.isLocked,
                  'phoneAssetId': item.covers.phone?.assetId,
                  'tabletAssetId': item.covers.tablet?.assetId,
                },
              )
              .toList(),
        };

        StoryDetailCubit activeDetail() => tester
            .element(find.byType(StoryDetailView))
            .read<StoryDetailCubit>();
        Future<Map<String, int>> loadedHero(StoryDetailCubit detail) async {
          await until(() => detail.state.heroStatus != StoryHeroStatus.loading);
          expect(detail.state.heroStatus, StoryHeroStatus.loaded);
          final raw = find.descendant(
            of: find.byType(StoryDetailHero),
            matching: find.byType(RawImage),
          );
          await until(
            () =>
                raw.evaluate().isNotEmpty &&
                (tester.widget<RawImage>(raw.first).image != null),
          );
          final image = tester.widget<RawImage>(raw.first).image!;
          final tablet =
              tester.view.physicalSize.shortestSide /
                  tester.view.devicePixelRatio >=
              600;
          final expected = storyHeroDecodeSize(
            detail.state.detail!.hero!,
            tablet,
          );
          expect(image.width, lessThanOrEqualTo(expected.$1));
          expect(image.height, lessThanOrEqualTo(expected.$2));
          expect(
            image.width * image.height * 4,
            lessThanOrEqualTo(16 * 1024 * 1024),
          );
          return {
            'width': image.width,
            'height': image.height,
            'bytes': image.width * image.height * 4,
          };
        }

        StoryDetailCoverProbe coverProbe(StoryDetailCubit detail) {
          final activeView = find.byWidget(
            tester.widget<StoryDetailView>(find.byType(StoryDetailView)),
          );
          expect(
            tester.element(activeView).read<StoryDetailCubit>(),
            same(detail),
          );
          return StoryDetailCoverProbe(tester, activeView);
        }

        void observeCovers(
          String phase,
          StoryDetailCubit detail, {
          PreviewCard? waitingFor,
        }) {
          final observations =
              (binding.reportData!['story12CoverWaitDiagnostics'] ??=
                      <Map<String, Object?>>[])
                  as List;
          if (observations.length >= 128) return;
          final events = libraryGetIt<LibraryDiagnostics>().events;
          observations.add({
            'phase': phase,
            'elapsedMilliseconds': observationClock.elapsedMilliseconds,
            'detailContentId': detail.state.identity!.contentId,
            'uiLanguage': detail.state.identity!.uiLanguage,
            'contentLanguage': detail.state.identity!.contentLanguage,
            'detailAttempt': detail.state.attempt,
            'metadataAttempt': detail.state.metadataAttempt,
            ...coverProbe(detail).snapshot(waitingFor),
            'diagnostics': events
                .skip(events.length > 8 ? events.length - 8 : 0)
                .toList(),
          });
        }

        Future<List<Map<String, Object?>>> loadedRecommendationCovers(
          StoryDetailCubit detail, {
          bool allowExplicitRecovery = true,
        }) async {
          if (detail.state.detail!.recommendations.isEmpty) return [];
          final probe = coverProbe(detail);
          final cards = probe.cards;
          await until(() => cards.evaluate().isNotEmpty);
          observeCovers('wait-entered', detail);
          final tablet =
              MediaQuery.sizeOf(tester.element(cards.first)).shortestSide >=
              600;
          final result = <Map<String, Object?>>[];
          final requiredCards = tester.widgetList<PreviewCard>(cards).toList();
          for (final card in requiredCards) {
            final cover = tablet
                ? (card.item.covers.tablet ?? card.item.covers.phone)
                : card.item.covers.phone;
            if (cover == null) {
              result.add({
                'contentId': card.item.contentId,
                'state': 'absent-descriptor',
              });
              continue;
            }
            final raw = probe.decoded(card);
            try {
              await until(
                () =>
                    raw.evaluate().isNotEmpty ||
                    probe.artworkState(card)?.status == ArtworkStatus.error,
              );
            } catch (_) {
              observeCovers('wait-deadline', detail, waitingFor: card);
              rethrow;
            }
            if (raw.evaluate().isEmpty) {
              observeCovers('current-artwork-error', detail, waitingFor: card);
              expect(probe.artworkState(card)?.status, ArtworkStatus.error);
              expect(
                allowExplicitRecovery,
                isTrue,
                reason: 'A second error after the one real retry fails the run',
              );
              final identity = detail.state.identity!;
              final metadataAttempt = detail.state.metadataAttempt;
              final retainedMetadata = detailRecord(detail.state.detail!);
              final recovery = <String, Object?>{
                'selectedContentId': identity.contentId,
                'categoryId': identity.categoryId,
                'uiLanguage': identity.uiLanguage,
                'contentLanguage': identity.contentLanguage,
                'failedRecommendationContentId': card.item.contentId,
                'failedAssetId': cover.assetId,
                'metadataAttemptBefore': metadataAttempt,
                'phase': 'observed-current-artwork-error',
                'classification': 'qualified explicit user-control recovery',
              };
              final index = explicitCoverRecoveries.length;
              explicitCoverRecoveries.add(recovery);
              final prefix =
                  'detail-${identity.uiLanguage}-${identity.contentId}-cover-retry-$index';
              await capture('$prefix-error');
              final retry = find.descendant(
                of: probe.target(card),
                matching: find.byTooltip(
                  StoryDetailStrings(identity.uiLanguage).retryArtwork,
                ),
              );
              expect(retry, findsOneWidget);
              expect(
                tester.getSize(retry).shortestSide,
                greaterThanOrEqualTo(48),
              );
              await tester.ensureVisible(retry);
              await tester.pumpAndSettle();
              expect(retry.hitTestable(), findsOneWidget);
              await tester.tap(retry);
              await tester.pump();
              await until(
                () =>
                    !detail.state.recoveringArtwork &&
                    detail.state.metadataAttempt > metadataAttempt,
              );
              expect(detail.state.identity, identity);
              expect(detail.state.status, StoryDetailStatus.loaded);
              expect(detailRecord(detail.state.detail!), retainedMetadata);
              expect(
                detailRecord(detail.state.detail!),
                detailRecord(await freshDetail(identity)),
              );
              final hero = await loadedHero(detail);
              final covers = await loadedRecommendationCovers(
                detail,
                allowExplicitRecovery: false,
              );
              for (final required in requiredCards) {
                final requiredCover = tablet
                    ? (required.item.covers.tablet ??
                          required.item.covers.phone)
                    : required.item.covers.phone;
                if (requiredCover == null) continue;
                expect(
                  covers.any(
                    (decoded) =>
                        decoded['contentId'] == required.item.contentId &&
                        decoded['contentLanguage'] ==
                            required.item.contentLanguage &&
                        decoded['assetId'] == requiredCover.assetId &&
                        decoded['state'] == 'decoded',
                  ),
                  isTrue,
                  reason:
                      'Every originally required partial/visible cover must decode',
                );
              }
              await capture('$prefix-recovered');
              recovery.addAll({
                'phase': 'fresh-same-context-verified-decoded',
                'realRetryControlTap': true,
                'metadataAttemptAfter': detail.state.metadataAttempt,
                'freshIndependentHttpComparison': true,
                'decodedHero': hero,
                'decodedRecommendationCovers': covers,
              });
              return covers;
            }
            final image = tester.widget<RawImage>(raw.first).image!;
            expect(image.width, lessThanOrEqualTo(tablet ? 600 : 360));
            expect(
              image.width * image.height * 4,
              lessThanOrEqualTo(16 * 1024 * 1024),
            );
            result.add({
              'contentId': card.item.contentId,
              'contentLanguage': card.item.contentLanguage,
              'assetId': cover.assetId,
              'state': 'decoded',
              'width': image.width,
              'height': image.height,
              'bytes': image.width * image.height * 4,
            });
          }
          observeCovers('wait-decoded', detail);
          return result;
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final cacheFailures = <Map<String, Object?>>[];
        binding.reportData!['story12CacheFailures'] = cacheFailures;
        final cache = libraryGetIt<LibraryImageCache>();
        cache.failureObserver = (event) {
          if (cacheFailures.length == 64) cacheFailures.removeAt(0);
          cacheFailures.add(event);
        };
        addTearDown(() => cache.failureObserver = null);
        final library = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        final router = libraryGetIt<LibraryRouter>();
        await until(() => library.state.status != LibraryStatus.loading);
        expect(library.state.status, LibraryStatus.loaded);
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        for (final language in ['tr', 'en', 'pt']) {
          for (final scale in [1.0, 1.3]) {
            LibraryDevHarness.scaleNotifier.value = scale;
            unawaited(
              library.load(
                context: LibraryContext(
                  language,
                  library.state.context.accessIdentity,
                ),
              ),
            );
            await until(() => library.state.status != LibraryStatus.loading);
            expect(library.state.status, LibraryStatus.loaded);
            await tester.pumpAndSettle();
            final catalog = library.state.catalog!;
            final attempt = library.state.attempt;
            final cases = <(String, LibraryRow, LibraryPreview)>[];
            for (final kind in ['free', 'locked', 'mapped']) {
              for (final row in catalog.rows) {
                final eligible = row.items.where(
                  (item) => kind == 'mapped'
                      ? row.contentLanguage != language
                      : row.contentLanguage == language &&
                            (kind == 'free' ? item.isFree : item.isLocked),
                );
                if (eligible.isNotEmpty) {
                  cases.add((kind, row, eligible.first));
                  break;
                }
              }
            }
            expect(cases, isNotEmpty);
            for (final (kind, row, item) in cases) {
              debugPrint(
                'Native detail case $language/$scale/$kind category ${row.categoryId} content ${item.contentId}',
              );
              final vertical = find.byKey(
                const PageStorageKey('library-scroll'),
              );
              await tester.drag(vertical, const Offset(0, 3000));
              await tester.pumpAndSettle();
              final horizontal = find.byKey(
                PageStorageKey('row-${row.categoryId}'),
              );
              for (
                var index = 0;
                index < 20 && horizontal.evaluate().isEmpty;
                index++
              ) {
                await tester.drag(vertical, const Offset(0, -180));
                await tester.pumpAndSettle();
              }
              expect(horizontal, findsOneWidget);
              await tester.ensureVisible(horizontal);
              await tester.pumpAndSettle();
              final rowScrollable = find
                  .descendant(of: horizontal, matching: find.byType(Scrollable))
                  .first;
              tester.state<ScrollableState>(rowScrollable).position.jumpTo(0);
              await tester.pumpAndSettle();
              final card = find.descendant(
                of: horizontal,
                matching: find.byWidgetPredicate(
                  (widget) =>
                      widget is PreviewCard &&
                      widget.item.contentId == item.contentId,
                ),
              );
              for (
                var seek = 0;
                seek < row.items.length * 4 && card.evaluate().isEmpty;
                seek++
              ) {
                await tester.drag(horizontal, const Offset(-100, 0));
                await tester.pumpAndSettle();
              }
              expect(
                card,
                findsOneWidget,
                reason:
                    'Category ${row.categoryId}/card ${item.contentId} must be reachable from row start',
              );
              await tester.ensureVisible(card);
              await tester.pumpAndSettle();
              // Leave a nonzero row position where the selected card permits it.
              if (row.items.length > 1) {
                await tester.drag(horizontal, const Offset(-30, 0));
              }
              await tester.pumpAndSettle();
              final beforeVertical = offset(vertical),
                  beforeHorizontal = offset(horizontal);
              final cardLabel =
                  '${item.title}, ${item.contentLanguage}${item.isLocked ? ', ${LibraryStrings(language).locked}' : ''}';
              final control = find.descendant(
                of: card,
                matching: find.bySemanticsLabel(cardLabel),
              );
              final cardSemantics = tester.getSemantics(control);
              expect(cardSemantics.flagsCollection.isEnabled, Tristate.isTrue);
              expect(
                cardSemantics.getSemanticsData().hasAction(SemanticsAction.tap),
                isTrue,
              );
              expect(tester.getSize(control).height, greaterThanOrEqualTo(48));
              final started = DateTime.now();
              await tester.tap(control);
              await tester.tap(control, warnIfMissed: false);
              await tester.pump();
              await until(
                () => find.byType(StoryDetailView).evaluate().isNotEmpty,
              );
              final source = activeDetail();
              expect(source.state.identity!.contentId, item.contentId);
              expect(source.state.identity!.categoryId, row.categoryId);
              expect(source.state.identity!.uiLanguage, language);
              expect(
                source.state.identity!.contentLanguage,
                item.contentLanguage,
              );
              await until(
                () => source.state.status != StoryDetailStatus.loading,
              );
              expect(source.state.status, StoryDetailStatus.loaded);
              expect(router.stack, hasLength(2));
              observeCovers('before-independent-metadata', source);
              final baseline = await freshDetail(source.state.identity!);
              observeCovers('after-independent-metadata', source);
              expect(
                detailRecord(source.state.detail!),
                detailRecord(baseline),
              );
              final decoded = await loadedHero(source);
              observeCovers('hero-decoded', source);
              await tester.pumpAndSettle();
              final strings = StoryDetailStrings(language);
              final back = find.byTooltip(strings.back);
              expect(
                tester.getSize(back).shortestSide,
                greaterThanOrEqualTo(48),
              );
              final action = find.bySemanticsLabel(
                item.isLocked ? strings.premium : strings.start,
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
              expect(tester.getSize(action).height, greaterThanOrEqualTo(48));
              final prefix = 'detail-$language-$scale-$kind-${item.contentId}';
              await capture('$prefix-loaded');
              observeCovers('loaded-capture-acknowledged', source);
              await tester.ensureVisible(find.byType(Switch));
              await tester.tap(find.byType(Switch));
              await tester.pumpAndSettle();
              expect(source.state.narration, false);
              final detailList = find.byType(ListView).first;
              await tester.drag(detailList, const Offset(0, -1400));
              await tester.pumpAndSettle();
              final recommendationCovers = await loadedRecommendationCovers(
                source,
              );
              final sourceOffset = offset(detailList);
              await capture('$prefix-metadata');
              var nested = false;
              if (source.state.detail!.recommendations.isNotEmpty) {
                final recommendation =
                    source.state.detail!.recommendations.first;
                final recommendationCard = find.byWidgetPredicate(
                  (widget) =>
                      widget is PreviewCard &&
                      widget.item.contentId == recommendation.contentId,
                );
                await tester.ensureVisible(recommendationCard);
                await tester.pumpAndSettle();
                final retainedOffset = offset(detailList);
                await tester.tap(recommendationCard);
                await tester.tap(recommendationCard, warnIfMissed: false);
                await tester.pump();
                await until(
                  () =>
                      find.byType(StoryDetailView).evaluate().length == 1 &&
                      activeDetail() != source,
                );
                final target = activeDetail();
                await until(
                  () => target.state.status != StoryDetailStatus.loading,
                );
                expect(target.state.status, StoryDetailStatus.loaded);
                expect(router.stack, hasLength(3));
                expect(source.state.visible, false);
                expect(source.state.heroStatus, StoryHeroStatus.suspended);
                expect(
                  target.state.identity!.contentId,
                  recommendation.contentId,
                );
                expect(
                  target.state.identity!.contentLanguage,
                  item.contentLanguage,
                );
                expect(
                  detailRecord(target.state.detail!),
                  detailRecord(await freshDetail(target.state.identity!)),
                );
                await loadedHero(target);
                await capture('$prefix-recommendation');
                await tester.tap(find.byTooltip(strings.back));
                await tester.pumpAndSettle();
                expect(target.isClosed, true);
                expect(activeDetail(), same(source));
                expect(source.state.visible, true);
                expect(source.state.narration, false);
                expect(offset(find.byType(ListView).first), retainedOffset);
                expect(router.stack, hasLength(2));
                nested = true;
              } else {
                expect(find.text(strings.similar), findsNothing);
              }
              await tester.drag(
                find.byType(ListView).first,
                const Offset(0, 3000),
              );
              await tester.pumpAndSettle();
              final returnedHero = await loadedHero(source);
              await tester.tap(find.byTooltip(strings.back));
              await tester.pumpAndSettle();
              expect(source.isClosed, true);
              expect(router.stack, hasLength(1));
              expect(
                tester.element(find.byType(LibraryView)).read<LibraryCubit>(),
                same(library),
              );
              expect(library.state.attempt, attempt);
              expect(library.state.catalog, same(catalog));
              expect(offset(vertical), beforeVertical);
              expect(offset(horizontal), beforeHorizontal);
              expect(tester.takeException(), isNull);
              records.add({
                ...detailRecord(baseline),
                'case': kind,
                'textScale': scale,
                'realCardTap': true,
                'repeatedTapSingleRoute': true,
                'freshHttpComparison': true,
                'decodedHero': decoded,
                'restoredHero': returnedHero,
                'nestedRecommendationBack': nested,
                'recommendationCoversAtMetadataCapture': recommendationCovers,
                'sourceDetailScroll': sourceOffset,
                'libraryVerticalScroll': beforeVertical,
                'libraryHorizontalScroll': beforeHorizontal,
                'libraryOwnerRetained': true,
                'narrationPreferenceRestored': true,
                'hiddenHeroSuspended': nested,
                'deferredActionDisabled': true,
                'elapsedMilliseconds': DateTime.now()
                    .difference(started)
                    .inMilliseconds,
                'imageCacheBytesAfterReturn':
                    PaintingBinding.instance.imageCache.currentSizeBytes,
              });
            }
          }
        }
        // A real unavailable identity exercises the defined server/client outcome.
        unawaited(
          router.push(
            StoryDetailRoute(
              categoryId: 1,
              contentId: 2147483647,
              uiLanguage: 'pt',
              contentLanguage: 'pt',
            ),
          ),
        );
        await tester.pump();
        await until(() => find.byType(StoryDetailView).evaluate().isNotEmpty);
        final missing = activeDetail();
        await until(() => missing.state.status != StoryDetailStatus.loading);
        expect(missing.state.status, StoryDetailStatus.unavailable);
        expect(missing.state.detail, isNull);
        expect(
          find.text(const StoryDetailStrings('pt').unavailable),
          findsOneWidget,
        );
        await capture('detail-pt-unavailable-real-http');
        await tester.tap(find.byTooltip(const StoryDetailStrings('pt').back));
        await tester.pumpAndSettle();
        binding.reportData ??= {};
        binding.reportData!['story12'] = {
          'platform': Platform.operatingSystem,
          'operatingSystemVersion': Platform.operatingSystemVersion,
          'logicalSize': {
            'width':
                tester.view.physicalSize.width / tester.view.devicePixelRatio,
            'height':
                tester.view.physicalSize.height / tester.view.devicePixelRatio,
          },
          'physicalPixels': {
            'width': tester.view.physicalSize.width,
            'height': tester.view.physicalSize.height,
          },
          'devicePixelRatio': tester.view.devicePixelRatio,
          'cases': records,
          'unavailableRealHttp': true,
          'gaps': [
            'physical devices',
            'minimum/older OS',
            'natural URL expiry',
            'platform accessibility inspector',
            'measured artwork contrast',
            'approved quantitative memory/latency budgets',
            'native network/timeout/5xx/corrupt-source fault captures',
          ],
        };
      } finally {
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 15)),
  );
}
