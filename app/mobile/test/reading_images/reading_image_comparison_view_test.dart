import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/view/reading_image_comparison_view.dart';
import 'reading_image_comparison_cubit_test.dart' show PendingRepository;

void main() {
  testWidgets('ağsız geometri aynı slotta kaynak ve rendition kolunu değiştirir', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final repo = PendingRepository();
    final cubit = ReadingImageComparisonCubit(
      LoadReadingImageComparison(repo),
      repo,
      const ReadingImageComparisonConfiguration.geometry(),
      ReadingComparisonWorkGate(),
      const ReadingComparisonClock(),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider.value(
          value: cubit,
          child: const ReadingImageComparisonView(contentId: 0),
        ),
      ),
    );
    await tester.pump();
    expect(find.textContaining('Ağsız geometri:'), findsOneWidget);
    final before = tester.getSize(
      find.byKey(const ValueKey('reading-comparison-slot')),
    );
    expect(before.width, 390);
    expect(before.height, closeTo(390 / .9, .01));
    debugPrint(
      'P4a2 synthetic cover: ${before.width} x ${before.height}, DPR 3, cover, center',
    );
    await tester.tap(find.text('Canonical PNG kaynak göster'));
    await tester.pump();
    await tester.pump();
    expect(
      tester.getSize(find.byKey(const ValueKey('reading-comparison-slot'))),
      before,
    );
    expect(repo.calls, 0);
    await tester.tap(find.text('Sayfa 1'));
    await tester.pump();
    await tester.pump();
    expect(find.textContaining('contain'), findsOneWidget);
    debugPrint(
      'P4a2 synthetic page: ${tester.getSize(find.byKey(const ValueKey('reading-comparison-slot')))}, DPR 3, contain, center',
    );
    await tester.pumpWidget(const SizedBox());
    await cubit.close();
  });
}
