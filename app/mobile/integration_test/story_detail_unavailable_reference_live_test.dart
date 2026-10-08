import 'dart:async';
import 'dart:io';

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
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;

import 'library_live_test.dart' show LibraryTestBinding;

/// Additional real-404 reference captures; the original native matrix is intact.
/// SDK images are captured only after the actual route has finished entering.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'settled localized real unavailable references',
    (tester) async {
      final semantics = tester.ensureSemantics();
      final records = <Map<String, Object?>>[];
      binding.reportData ??= {};
      binding.reportData!['story12SettledUnavailableReferences'] = records;
      try {
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() ready) async {
          final deadline = DateTime.now().add(const Duration(seconds: 60));
          while (!ready()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Settled unavailable reference deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        await until(() => find.byType(LibraryView).evaluate().isNotEmpty);
        final library = tester
            .element(find.byType(LibraryView))
            .read<LibraryCubit>();
        await until(() => library.state.status != LibraryStatus.loading);
        expect(library.state.status, LibraryStatus.loaded);
        final router = libraryGetIt<LibraryRouter>();
        final config = libraryGetIt<LibraryConfiguration>();
        if (Platform.isAndroid) await binding.convertFlutterSurfaceToImage();
        for (final language in ['tr', 'en', 'pt']) {
          for (final scale in [1.0, 1.3]) {
            LibraryDevHarness.scaleNotifier.value = scale;
            final client = HttpClient()
              ..connectionTimeout = const Duration(seconds: 15);
            try {
              final uri = config.baseUrl
                  .resolve('/api/test/library/stories/2147483647')
                  .replace(
                    queryParameters: {
                      'categoryId': '1',
                      'lang': language,
                      'contentLang': language,
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
              expect(response.statusCode, HttpStatus.notFound);
              var byteCount = 0;
              await for (final chunk in response.timeout(
                const Duration(seconds: 15),
              )) {
                byteCount += chunk.length;
                expect(byteCount, lessThanOrEqualTo(1024 * 1024));
              }
            } finally {
              client.close(force: true);
            }
            unawaited(
              router.push(
                StoryDetailRoute(
                  categoryId: 1,
                  contentId: 2147483647,
                  uiLanguage: language,
                  contentLanguage: language,
                ),
              ),
            );
            final surface = find.byType(StoryDetailView);
            await until(() {
              final elements = surface.evaluate().toList();
              if (elements.length != 1) return false;
              final route = ModalRoute.of(elements.single);
              return route?.isCurrent == true &&
                  route?.animation?.status == AnimationStatus.completed &&
                  tester.getTopLeft(surface).dx.abs() <= 0.001 &&
                  elements.single.read<StoryDetailCubit>().state.status ==
                      StoryDetailStatus.unavailable;
            });
            final state = tester
                .element(surface)
                .read<StoryDetailCubit>()
                .state;
            expect(state.detail, isNull);
            final strings = StoryDetailStrings(language);
            expect(find.text(strings.unavailable), findsOneWidget);
            expect(find.text(strings.retry), findsNothing);
            final back = find.byTooltip(strings.back);
            expect(back, findsOneWidget);
            final backSize = tester.getSize(back);
            expect(backSize.width, greaterThanOrEqualTo(48));
            expect(backSize.height, greaterThanOrEqualTo(48));
            for (var frame = 0; frame < 8; frame++) {
              await tester.pump(const Duration(milliseconds: 16));
            }
            final route = ModalRoute.of(tester.element(surface));
            expect(route?.isCurrent, true);
            expect(route?.animation?.status, AnimationStatus.completed);
            final origin = tester.getTopLeft(surface).dx;
            expect(origin, closeTo(0, 0.001));
            final name =
                'detail-settled-$language-$scale-unavailable-real-http';
            await binding.takeScreenshot(name);
            await tester.tap(back);
            await tester.pumpAndSettle();
            expect(surface, findsNothing);
            expect(
              tester.element(find.byType(LibraryView)).read<LibraryCubit>(),
              same(library),
            );
            records.add({
              'screenshotName': name,
              'language': language,
              'textScale': scale,
              'independentHttpStatus': HttpStatus.notFound,
              'routeAnimationCompleted': true,
              'horizontalOrigin': origin,
              'captureFramesFlushed': 8,
              'backWidth': backSize.width,
              'backHeight': backSize.height,
              'libraryOwnerRetainedAfterBack': true,
            });
          }
        }
        expect(records, hasLength(6));
      } finally {
        LibraryDevHarness.scaleNotifier.value = null;
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 5)),
  );
}
