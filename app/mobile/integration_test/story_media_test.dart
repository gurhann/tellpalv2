import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tellpal_mobile_poc/main.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('loads image and audio on all protected story pages', (
    tester,
  ) async {
    const token = String.fromEnvironment('POC_TEST_TOKEN');
    expect(
      token,
      isNotEmpty,
      reason: 'Pass POC_TEST_TOKEN with --dart-define.',
    );

    await tester.pumpWidget(const TellPalMediaPoc());
    await tester.enterText(find.byType(TextField).at(1), token);
    await tester.tap(find.text('Hikâyeyi aç'));
    await tester.pump();

    for (var pageNumber = 1; pageNumber <= 14; pageNumber++) {
      await _waitFor(tester, find.textContaining('Sayfa $pageNumber/14'));
      await _waitFor(tester, find.byTooltip('Duraklat'));
      expect(find.textContaining('yüklenemedi'), findsNothing);

      await tester.tap(find.byTooltip('Duraklat'));
      await tester.pump();
      expect(find.byTooltip('Oynat'), findsOneWidget);

      if (pageNumber < 14) {
        await tester.tap(find.text('Sonraki'));
      }
    }
  });
}

Future<void> _waitFor(WidgetTester tester, Finder finder) async {
  for (var attempt = 0; attempt < 30; attempt++) {
    await tester.pump(const Duration(seconds: 1));
    if (finder.evaluate().isNotEmpty) return;
  }
  expect(finder, findsOneWidget);
}
