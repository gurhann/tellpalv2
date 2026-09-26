import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/story_manifest.dart';

void main() {
  test(
    'accepts ordered image and audio grants from the protected response',
    () {
      final manifest = StoryManifest.fromJson(_payload());

      expect(manifest.revision, 'a' * 64);
      expect(manifest.pages.single.pageNumber, 1);
      expect(manifest.pages.single.illustration.url.scheme, 'https');
      expect(manifest.pages.single.audio?.mimeType, 'audio/mpeg');
    },
  );

  test('allows a page without audio', () {
    final payload = _payload();
    final pages = payload['pages'] as List;
    pages[0] = Map<String, dynamic>.from(pages.first as Map)..['audio'] = null;

    expect(StoryManifest.fromJson(payload).pages.single.audio, isNull);
  });

  test('rejects an unsupported schema and an invalid page order', () {
    final unsupported = _payload()..['schemaVersion'] = 2;
    final unordered = _payload();
    (unordered['pages'] as List).first['pageNumber'] = 2;

    expect(() => StoryManifest.fromJson(unsupported), throwsFormatException);
    expect(() => StoryManifest.fromJson(unordered), throwsFormatException);
  });

  test('detects grants that expire inside the refresh window', () {
    final now = DateTime.utc(2030, 1, 1, 12);
    final grant = MediaGrant(
      assetId: 21,
      mimeType: 'image/jpeg',
      url: Uri.parse('https://example.test/media/21'),
      expiresAt: now.add(const Duration(minutes: 10)),
    );

    expect(grant.expiresWithin(const Duration(minutes: 9), now: now), isFalse);
    expect(grant.expiresWithin(const Duration(minutes: 11), now: now), isTrue);
  });
}

Map<String, dynamic> _payload() => {
  'schemaVersion': 1,
  'contentId': 1,
  'languageCode': 'tr',
  'revision': 'a' * 64,
  'pages': <Map<String, dynamic>>[
    {
      'pageId': 10,
      'pageNumber': 1,
      'width': 887,
      'height': 1440,
      'illustration': _grant(21, 'image/jpeg'),
      'audio': _grant(22, 'audio/mpeg'),
    },
  ],
};

Map<String, dynamic> _grant(int id, String mimeType) => {
  'assetId': id,
  'mimeType': mimeType,
  'downloadUrl': 'https://example.test/media/$id',
  'downloadUrlExpiresAt': '2030-01-01T00:00:00Z',
};
