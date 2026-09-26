import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tellpal_mobile_poc/main.dart';
import 'package:tellpal_mobile_poc/story_manifest.dart';
import 'package:tellpal_mobile_poc/story_manifest_client.dart';
import 'package:tellpal_mobile_poc/story_player_page.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() {
    expect(
      _token,
      isNotEmpty,
      reason: 'Pass POC_TEST_TOKEN with --dart-define.',
    );
  });

  testWidgets('keeps the final page after rapid navigation', (tester) async {
    await tester.pumpWidget(const TellPalMediaPoc());
    await tester.enterText(find.byType(TextField).at(1), _token);
    await tester.tap(find.text('Hikâyeyi aç'));
    await tester.pump();
    await _waitUntilReady(tester, 1);

    await _tapRepeatedly(tester, find.text('Sonraki'), 10);
    await _waitUntilReady(tester, 11);
    await _tapRepeatedly(tester, find.text('Önceki'), 7);
    await _waitUntilReady(tester, 4);
    await _tapRepeatedly(tester, find.text('Sonraki'), 9);
    await _waitUntilReady(tester, 13);
  });

  testWidgets('shows an outage and recovers after the backend returns', (
    tester,
  ) async {
    final client = _ControlledStoryManifestClient();
    await tester.pumpWidget(
      MaterialApp(
        home: StoryPlayerPage(client: client, contentId: 1, languageCode: 'tr'),
      ),
    );
    await _waitUntilReady(tester, 1);

    client.rejectRequests = true;
    await tester.tap(find.byTooltip('Medya bağlantılarını yenile'));
    await _waitFor(
      tester,
      find.text('Bağlantılar yenilenemedi. Backend ve ağı kontrol edin.'),
    );
    expect(find.textContaining('Sayfa 1/14'), findsOneWidget);

    client.rejectRequests = false;
    await tester.tap(find.byTooltip('Medya bağlantılarını yenile'));
    await _waitUntilReady(tester, 1);
    expect(find.textContaining('yenilenemedi'), findsNothing);
  });

  testWidgets('refreshes expiring grants against the pinned revision', (
    tester,
  ) async {
    final client = _ControlledStoryManifestClient();
    await tester.pumpWidget(
      MaterialApp(
        home: StoryPlayerPage(
          client: client,
          contentId: 1,
          languageCode: 'tr',
          grantRefreshWindow: const Duration(hours: 1),
        ),
      ),
    );
    await _waitUntilReady(tester, 1);

    expect(client.requestedRevisions.length, greaterThanOrEqualTo(2));
    expect(client.requestedRevisions.first, isNull);
    expect(client.requestedRevisions[1], client.receivedRevisions.first);
    expect(client.receivedRevisions.toSet(), hasLength(1));
  });
}

const _token = String.fromEnvironment('POC_TEST_TOKEN');

class _ControlledStoryManifestClient extends StoryManifestClient {
  _ControlledStoryManifestClient()
    : super(baseUrl: Uri.parse('http://localhost:8080'), bearerToken: _token);

  bool rejectRequests = false;
  final List<String?> requestedRevisions = [];
  final List<String> receivedRevisions = [];

  @override
  Future<StoryManifest> fetch({
    required int contentId,
    required String languageCode,
    String? revision,
  }) async {
    requestedRevisions.add(revision);
    if (rejectRequests) {
      throw const StoryManifestException(
        'Backend’e bağlanılamadı. Adresi ve ağı kontrol edin.',
      );
    }
    final manifest = await super.fetch(
      contentId: contentId,
      languageCode: languageCode,
      revision: revision,
    );
    receivedRevisions.add(manifest.revision);
    return manifest;
  }
}

Future<void> _tapRepeatedly(
  WidgetTester tester,
  Finder finder,
  int count,
) async {
  for (var index = 0; index < count; index++) {
    await tester.tap(finder);
  }
  await tester.pump();
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
