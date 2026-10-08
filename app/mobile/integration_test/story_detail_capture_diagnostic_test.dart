import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import 'package:tellpal_mobile_poc/main_library_dev.dart' as app;

import 'library_live_test.dart' show LibraryTestBinding;
import 'native_capture_protocol.dart';

/// Measures the SDK's actual PNG envelope without admitting acceptance evidence.
/// The original native matrix and its fixed capture bounds remain unchanged.
void main() {
  final binding = LibraryTestBinding();
  testWidgets(
    'diagnostic only: actual native screenshot scalar envelope',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        unawaited(app.runLibrary(installErrorHandlers: false));
        Future<void> until(bool Function() ready) async {
          final deadline = DateTime.now().add(const Duration(seconds: 60));
          while (!ready()) {
            if (DateTime.now().isAfter(deadline)) {
              fail('Native capture diagnostic state deadline reached');
            }
            await tester.pump(const Duration(milliseconds: 100));
          }
        }

        await until(() {
          final home = find.byType(LibraryView);
          return home.evaluate().isNotEmpty &&
              tester.element(home).read<LibraryCubit>().state.status ==
                  LibraryStatus.loaded;
        });
        unawaited(
          libraryGetIt<LibraryRouter>().push(
            StoryDetailRoute(
              categoryId: 2,
              contentId: 2,
              uiLanguage: 'tr',
              contentLanguage: 'tr',
            ),
          ),
        );
        await until(() {
          final view = find.byType(StoryDetailView);
          if (view.evaluate().isEmpty) return false;
          final state = tester.element(view).read<StoryDetailCubit>().state;
          return state.status == StoryDetailStatus.loaded &&
              state.heroStatus == StoryHeroStatus.loaded;
        });
        for (var frame = 0; frame < 8; frame++) {
          await tester.pump(const Duration(milliseconds: 16));
        }
        final context = tester.element(find.byType(StoryDetailView));
        final size = MediaQuery.sizeOf(context);
        final data = await binding.callbackManager.takeScreenshot(
          'diagnostic-only-first-loaded-tr-capture',
        );
        final bytes = data['bytes'] as List<int>;
        const signature = [137, 80, 78, 71, 13, 10, 26, 10];
        final png =
            bytes.length >= 8 &&
            List.generate(8, (index) => bytes[index]).join(',') ==
                signature.join(',');
        final header = png && bytes.length >= 33
            ? ByteData.sublistView(Uint8List.fromList(bytes.take(33).toList()))
            : null;
        binding.reportData ??= {};
        binding.reportData!['nativeCaptureScalarDiagnostic'] = {
          'diagnosticOnly': true,
          'acceptanceCapturePublished': false,
          'setup':
              'development route; original matrix proves its real card tap',
          'categoryId': 2,
          'contentId': 2,
          'uiLanguage': 'tr',
          'contentLanguage': 'tr',
          'logicalWidth': size.width,
          'logicalHeight': size.height,
          'devicePixelRatio': MediaQuery.devicePixelRatioOf(context),
          'nativeByteSize': bytes.length,
          'pngSignatureMatches': png,
          'withinOriginal8MiBBound':
              bytes.length >= 8 &&
              bytes.length <= NativeCaptureProtocol.maxBytes,
          'pngWidth': header?.getUint32(16),
          'pngHeight': header?.getUint32(20),
          'pngBitDepth': header?.getUint8(24),
          'pngColorType': header?.getUint8(25),
          'pngInterlace': header?.getUint8(28),
          'originalBoundsChanged': false,
          'nativePixelsOrBytesAltered': false,
        };
      } finally {
        semantics.dispose();
      }
    },
    timeout: const Timeout(Duration(minutes: 5)),
  );
}
