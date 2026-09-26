import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tellpal_mobile_poc/main.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('survives repeated navigation and app lifecycle changes', (
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
    await _waitUntilReady(tester, 1);

    for (var round = 0; round < 3; round++) {
      for (var page = 2; page <= 14; page++) {
        await tester.tap(find.text('Sonraki'));
        await _waitUntilReady(tester, page);
      }

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      await tester.pump(const Duration(seconds: 2));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump(const Duration(seconds: 1));
      expect(find.textContaining('Sayfa 14/14'), findsOneWidget);
      expect(find.textContaining('yüklenemedi'), findsNothing);

      for (var page = 13; page >= 1; page--) {
        await tester.tap(find.text('Önceki'));
        await _waitUntilReady(tester, page);
      }
    }
  });
}

Future<void> _waitUntilReady(WidgetTester tester, int pageNumber) async {
  await _waitFor(tester, find.textContaining('Sayfa $pageNumber/14'));
  await _waitFor(tester, find.byTooltip('Duraklat'));
  expect(find.textContaining('yüklenemedi'), findsNothing);
  await tester.tap(find.byTooltip('Duraklat'));
  await tester.pump();
  expect(find.byTooltip('Oynat'), findsOneWidget);
}

Future<void> _waitFor(WidgetTester tester, Finder finder) async {
  for (var attempt = 0; attempt < 30; attempt++) {
    await tester.pump(const Duration(seconds: 1));
    if (finder.evaluate().isNotEmpty) return;
  }
  expect(finder, findsOneWidget);
}
