import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:ui' show SemanticsAction, Tristate;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;

/// Host driver injects credentials in memory after connecting; never build defines or assets.
class LibraryTestBinding extends IntegrationTestWidgetsFlutterBinding {
  @override
  Future<Map<String, dynamic>> callback(Map<String, String> params) async {
    if (params['command'] == 'request_data' &&
        params['message']?.startsWith('configure:') == true) {
      try {
        final value =
            jsonDecode(params['message']!.substring(10))
                as Map<String, dynamic>;
        LibraryDevHarness.install(
          LibraryConfiguration(
            baseUrl: Uri.parse(value['baseUrl'] as String),
            idToken: value['idToken'] as String,
            context: const LibraryContext('tr', 'existing-authorized-fixture'),
          ),
        );
        return {
          'isError': false,
          'response': {'message': 'configured'},
        };
      } catch (_) {
        return {
          'isError': false,
          'response': {'message': 'configuration-rejected'},
        };
      }
    }
    return super.callback(params);
  }
}

void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'real Library matrix matches final CMS fixture, disabled controls retain scroll and cache remains bounded',
    (tester) async {
      final semanticsHandle = tester.ensureSemantics();
      try {
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() predicate) async {
          final deadline = DateTime.now().add(const Duration(seconds: 90));
          while (!predicate()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native Library state deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        Future<void> settleRenderedCovers() async {
          final states = find.byWidgetPredicate(
            (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
          );
          await until(
            () =>
                states.evaluate().isNotEmpty &&
                states.evaluate().every(
                  (element) =>
                      element.read<ArtworkCubit>().state.status !=
                      ArtworkStatus.loading,
                ),
          );
          expect(
            states.evaluate().map(
              (element) => element.read<ArtworkCubit>().state.status,
            ),
            everyElement(ArtworkStatus.loaded),
          );
          await until(() {
            final images = find.byType(RawImage).evaluate();
            return images.isNotEmpty &&
                images.every(
                  (element) => (element.widget as RawImage).image != null,
                );
          });
          await tester.pumpAndSettle();
        }

        Future<Map<String, dynamic>> freshMetadata(String language) async {
          final configuration = libraryGetIt<LibraryConfiguration>();
          final client = HttpClient()
            ..connectionTimeout = const Duration(seconds: 15);
          try {
            final request = await client.getUrl(
              configuration.baseUrl.resolve('/api/test/library?lang=$language'),
            );
            request.headers.set(
              HttpHeaders.authorizationHeader,
              'Bearer ${configuration.idToken}',
            );
            final response = await request.close().timeout(
              const Duration(seconds: 15),
            );
            expect(
              response.statusCode,
              HttpStatus.ok,
              reason: 'Independent current catalog probe',
            );
            final dto =
                jsonDecode(
                      await utf8.decoder
                          .bind(response)
                          .join()
                          .timeout(const Duration(seconds: 15)),
                    )
                    as Map<String, dynamic>;
            return {
              'schemaVersion': dto['schemaVersion'],
              'requestedLanguage': dto['requestedLanguage'],
              'audience': dto['audience'],
              'previewLimit': dto['previewLimit'],
              'rows': (dto['rows'] as List).map((value) {
                final row = value as Map<String, dynamic>;
                return {
                  'categoryId': row['categoryId'],
                  'contentLanguage': row['contentLanguage'],
                  'name': row['name'],
                  'displayOrder': row['displayOrder'],
                  'slug': row['slug'],
                  'description': row['description'],
                  'items': (row['items'] as List).map((value) {
                    final item = value as Map<String, dynamic>;
                    final covers = item['covers'] as Map<String, dynamic>;
                    return {
                      'contentId': item['contentId'],
                      'title': item['title'],
                      'language': item['contentLanguage'],
                      'isFree': item['isFree'],
                      'isLocked': item['isLocked'],
                      'externalKey': item['externalKey'],
                      'type': item['type'],
                      'experienceType': item['experienceType'],
                      'ageRange': item['ageRange'],
                      'durationMinutes': item['durationMinutes'],
                      'phoneAssetId':
                          (covers['phone']
                              as Map<String, dynamic>?)?['assetId'],
                      'tabletAssetId':
                          (covers['tablet']
                              as Map<String, dynamic>?)?['assetId'],
                    };
                  }).toList(),
                };
              }).toList(),
            };
          } finally {
            client.close(force: true);
          }
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final cubit = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        final records = <Map<String, Object?>>[];
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        // Complete bootstrap before the normal matrix; obsolete-transfer races
        // are deliberately exercised by the separate deadline integration target.
        await until(() => cubit.state.status != LibraryStatus.loading);
        expect(cubit.state.status, LibraryStatus.loaded);
        await until(() {
          final artwork = find
              .byWidgetPredicate(
                (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
              )
              .evaluate();
          return artwork.isNotEmpty &&
              artwork.every(
                (element) =>
                    element.read<ArtworkCubit>().state.status !=
                    ArtworkStatus.loading,
              );
        });
        expect(
          find
              .byWidgetPredicate(
                (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
              )
              .evaluate()
              .map((element) => element.read<ArtworkCubit>().state.status),
          everyElement(ArtworkStatus.loaded),
        );
        await tester.pumpAndSettle();
        for (final language in ['tr', 'en', 'pt']) {
          for (final scale in [1.0, 1.3]) {
            LibraryDevHarness.scaleNotifier.value = scale;
            final load = cubit.load(
              context: LibraryContext(
                language,
                cubit.state.context.accessIdentity,
              ),
            );
            await until(() => cubit.state.status != LibraryStatus.loading);
            await load;
            expect(cubit.state.status, LibraryStatus.loaded);
            final current = cubit.state.catalog!;
            expect(
              current.rows.map((r) => r.categoryId),
              current.audience == 'SUBSCRIBER'
                  ? (language == 'tr' ? [3, 2, 1] : [1, 2, 3])
                  : (language == 'tr' ? [3, 2, 1, 4] : [1, 2, 4, 3]),
            );
            for (final row in current.rows) {
              expect(row.items.length, lessThanOrEqualTo(5));
              expect(
                row.items.map((i) => i.contentLanguage),
                everyElement(row.contentLanguage),
              );
              expect(row.items.first.covers.phone, isNotNull);
              if (current.audience == 'SUBSCRIBER') {
                expect(
                  row.items.map((item) => item.isLocked),
                  everyElement(false),
                );
              }
            }
            if (language == 'tr') {
              expect(
                current.rows
                    .firstWhere((r) => r.categoryId == 2)
                    .items
                    .map((i) => i.contentId),
                [2, 3, 4],
              );
            }
            expect(
              current.rows.firstWhere((r) => r.categoryId == 3).contentLanguage,
              'en',
            );
            await until(
              () => find
                  .byWidgetPredicate(
                    (w) => w is BlocBuilder<ArtworkCubit, ArtworkState>,
                  )
                  .evaluate()
                  .every(
                    (e) =>
                        e.read<ArtworkCubit>().state.status !=
                        ArtworkStatus.loading,
                  ),
            );
            expect(
              find
                  .byWidgetPredicate(
                    (w) => w is BlocBuilder<ArtworkCubit, ArtworkState>,
                  )
                  .evaluate()
                  .map((e) => e.read<ArtworkCubit>().state.status),
              everyElement(ArtworkStatus.loaded),
              reason: jsonEncode(libraryGetIt<LibraryDiagnostics>().events),
            );
            await tester.pumpAndSettle();
            final strings = LibraryStrings(language);
            final vertical = find.byKey(const PageStorageKey('library-scroll'));
            for (var repetition = 0; repetition < 5; repetition++) {
              await tester.drag(vertical, const Offset(0, -350));
              await tester.pumpAndSettle();
              await tester.drag(vertical, const Offset(0, 350));
              await tester.pumpAndSettle();
            }
            await tester.drag(vertical, const Offset(0, -200));
            await tester.pumpAndSettle();
            final scrollable = find
                .descendant(of: vertical, matching: find.byType(Scrollable))
                .first;
            final before = tester
                .state<ScrollableState>(scrollable)
                .position
                .pixels;
            await tester.tap(find.bySemanticsLabel(strings.library).last);
            await tester.pump();
            expect(
              tester.state<ScrollableState>(scrollable).position.pixels,
              before,
            );
            await tester.binding.handlePopRoute();
            await tester.pump();
            expect(find.byType(LibraryView), findsOneWidget);
            expect(
              tester.state<ScrollableState>(scrollable).position.pixels,
              before,
            );
            for (final name in [strings.sleep, strings.profile]) {
              await tester.tap(
                find.bySemanticsLabel(name).last,
                warnIfMissed: false,
              );
              await tester.pump();
              expect(cubit.state.context.language, language);
              expect(
                tester.state<ScrollableState>(scrollable).position.pixels,
                before,
              );
            }
            await tester.drag(vertical, const Offset(0, 2000));
            await tester.pumpAndSettle();
            final topScroll = tester
                .state<ScrollableState>(scrollable)
                .position
                .pixels;
            final first = current.rows.first;
            final card = first.items.first;
            for (final name in [
              strings.search,
              if (current.audience != 'SUBSCRIBER') strings.premium,
              '${strings.all}: ${first.name}',
              '${card.title}, ${card.contentLanguage}${card.isLocked ? ', ${strings.locked}' : ''}',
            ]) {
              final control = find.bySemanticsLabel(name).first;
              final semantics = tester.getSemantics(control);
              expect(semantics.flagsCollection.isEnabled, Tristate.isFalse);
              expect(
                semantics.getSemanticsData().hasAction(SemanticsAction.tap),
                isFalse,
              );
              // The layout size avoids floating-point loss when subtracting global offsets.
              expect(tester.getSize(control).height, greaterThanOrEqualTo(48));
              await tester.tap(control, warnIfMissed: false);
              await tester.pump();
              expect(cubit.state.context.language, language);
              expect(cubit.state.status, LibraryStatus.loaded);
              expect(
                tester.state<ScrollableState>(scrollable).position.pixels,
                topScroll,
              );
              expect(tester.view.viewInsets.bottom, 0);
              expect(find.byType(LibraryView), findsOneWidget);
            }
            await settleRenderedCovers();
            final baseline = await freshMetadata(language);
            expect(
              {
                'schemaVersion': current.schemaVersion,
                'requestedLanguage': current.requestedLanguage,
                'audience': current.audience,
                'previewLimit': current.previewLimit,
                'rows': current.rows
                    .map(
                      (row) => {
                        'categoryId': row.categoryId,
                        'contentLanguage': row.contentLanguage,
                        'name': row.name,
                        'displayOrder': row.displayOrder,
                        'slug': row.slug,
                        'description': row.description,
                        'items': row.items
                            .map(
                              (item) => {
                                'contentId': item.contentId,
                                'title': item.title,
                                'language': item.contentLanguage,
                                'isFree': item.isFree,
                                'isLocked': item.isLocked,
                                'externalKey': item.externalKey,
                                'type': item.type,
                                'experienceType': item.experienceType,
                                'ageRange': item.ageRange,
                                'durationMinutes': item.durationMinutes,
                                'phoneAssetId': item.covers.phone?.assetId,
                                'tabletAssetId': item.covers.tablet?.assetId,
                              },
                            )
                            .toList(),
                      },
                    )
                    .toList(),
              },
              baseline,
              reason:
                  'Every native case matches an independent fresh authenticated HTTP response; signed URLs are excluded',
            );
            final selected = tester.getSemantics(
              find.bySemanticsLabel(strings.library).last,
            );
            expect(selected.flagsCollection.isSelected, Tristate.isTrue);
            expect(
              PaintingBinding.instance.imageCache.currentSizeBytes,
              lessThanOrEqualTo(32 * 1024 * 1024),
            );
            expect(
              PaintingBinding.instance.imageCache.currentSize,
              lessThanOrEqualTo(48),
            );
            records.add({
              'language': language,
              'textScale': scale,
              'freshHttpComparison': true,
              'renderedCoverBitmapsVerified': true,
              'audience': current.audience,
              'rows': current.rows
                  .map(
                    (r) => {
                      'categoryId': r.categoryId,
                      'contentLanguage': r.contentLanguage,
                      'items': r.items
                          .map(
                            (i) => {
                              'contentId': i.contentId,
                              'title': i.title,
                              'language': i.contentLanguage,
                              'isFree': i.isFree,
                              'isLocked': i.isLocked,
                              'phoneAssetId': i.covers.phone?.assetId,
                              'tabletAssetId': i.covers.tablet?.assetId,
                            },
                          )
                          .toList(),
                    },
                  )
                  .toList(),
              'imageCacheBytes':
                  PaintingBinding.instance.imageCache.currentSizeBytes,
              'imageCacheEntries':
                  PaintingBinding.instance.imageCache.currentSize,
            });
            // Allow native test pointer overlays to decay before visual evidence.
            for (var frame = 0; frame < 8; frame++) {
              await tester.pump(const Duration(milliseconds: 16));
            }
            await binding.takeScreenshot('library-$language-$scale');
          }
        }
        final diagnostics = libraryGetIt<LibraryDiagnostics>();
        expect(diagnostics.events.where((e) => e['kind'] == 'error'), isEmpty);
        binding.reportData ??= {};
        binding.reportData!['platform'] = Platform.operatingSystem;
        binding.reportData!['size'] = {
          'width':
              tester.view.physicalSize.width / tester.view.devicePixelRatio,
          'height':
              tester.view.physicalSize.height / tester.view.devicePixelRatio,
        };
        binding.reportData!['matrix'] = records;
        binding.reportData!['diagnostics'] = diagnostics.events;
      } finally {
        semanticsHandle.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 10)),
  );
}
