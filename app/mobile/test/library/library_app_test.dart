import 'package:flutter/material.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_app.dart';
import 'package:tellpal_mobile_poc/app/library_dev_harness.dart';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/view/library_view.dart';
import 'library_fixture.dart';

void main() {
  testWidgets(
    'app composition uses platform enlarged text until an explicit harness override',
    (tester) async {
      final config = configuration();
      final repository = ControlledRepository();
      LibraryDevHarness.scaleNotifier.value = null;
      libraryGetIt.registerSingleton<LibraryRouter>(LibraryRouter());
      libraryGetIt.registerSingleton<LibraryDiagnostics>(LibraryDiagnostics());
      libraryGetIt.registerFactory<LibraryCubit>(
        () => LibraryCubit(
          GetLibrary(repository),
          config,
          libraryGetIt<LibraryDiagnostics>(),
        ),
      );
      libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
      tester.platformDispatcher.textScaleFactorTestValue = 1.3;
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
      await tester.pumpWidget(const LibraryApp());
      await tester.pump();
      repository.responses.single.complete(right(catalog('tr', empty: true)));
      await tester.pump();
      final context = tester.element(find.byType(LibraryView));
      expect(MediaQuery.textScalerOf(context).scale(10), 13);
      LibraryDevHarness.scaleNotifier.value = 1.0;
      await tester.pump();
      expect(MediaQuery.textScalerOf(context).scale(10), 10);
      LibraryDevHarness.scaleNotifier.value = null;
      await tester.pump();
      expect(MediaQuery.textScalerOf(context).scale(10), 13);
      await tester.pumpWidget(const SizedBox());
      await libraryGetIt.reset();
    },
  );
}
