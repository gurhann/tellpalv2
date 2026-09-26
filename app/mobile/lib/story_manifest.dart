class StoryManifest {
  const StoryManifest({
    required this.contentId,
    required this.languageCode,
    required this.revision,
    required this.pages,
  });

  final int contentId;
  final String languageCode;
  final String revision;
  final List<StoryPage> pages;

  factory StoryManifest.fromJson(Map<String, dynamic> json) {
    if (_requiredInt(json, 'schemaVersion') != 1) {
      throw const FormatException('Desteklenmeyen medya manifesti sürümü.');
    }
    final rawPages = json['pages'];
    if (rawPages is! List || rawPages.isEmpty) {
      throw const FormatException('Medya manifestinde sayfa yok.');
    }
    final pages = rawPages
        .map((value) => StoryPage.fromJson(_object(value)))
        .toList();
    for (var index = 0; index < pages.length; index++) {
      if (pages[index].pageNumber != index + 1) {
        throw const FormatException(
          'Medya manifestinin sayfa sırası geçersiz.',
        );
      }
    }
    return StoryManifest(
      contentId: _requiredInt(json, 'contentId'),
      languageCode: _requiredString(json, 'languageCode'),
      revision: _requiredString(json, 'revision'),
      pages: List.unmodifiable(pages),
    );
  }
}

class StoryPage {
  const StoryPage({
    required this.pageId,
    required this.pageNumber,
    required this.width,
    required this.height,
    required this.illustration,
    required this.audio,
  });

  final int pageId;
  final int pageNumber;
  final int width;
  final int height;
  final MediaGrant illustration;
  final MediaGrant? audio;

  factory StoryPage.fromJson(Map<String, dynamic> json) {
    final rawAudio = json['audio'];
    final width = _requiredInt(json, 'width');
    final height = _requiredInt(json, 'height');
    if (width <= 0 || height <= 0) {
      throw const FormatException('Geçersiz görsel boyutları.');
    }
    return StoryPage(
      pageId: _requiredInt(json, 'pageId'),
      pageNumber: _requiredInt(json, 'pageNumber'),
      width: width,
      height: height,
      illustration: MediaGrant.fromJson(_object(json['illustration'])),
      audio: rawAudio == null ? null : MediaGrant.fromJson(_object(rawAudio)),
    );
  }
}

class MediaGrant {
  const MediaGrant({
    required this.assetId,
    required this.mimeType,
    required this.url,
    required this.expiresAt,
  });

  final int assetId;
  final String mimeType;
  final Uri url;
  final DateTime expiresAt;

  bool expiresWithin(Duration window, {DateTime? now}) =>
      expiresAt.isBefore((now ?? DateTime.now().toUtc()).add(window));

  bool get expiresSoon => expiresWithin(const Duration(minutes: 1));

  factory MediaGrant.fromJson(Map<String, dynamic> json) {
    final rawUrl = _requiredString(json, 'downloadUrl');
    final url = Uri.tryParse(rawUrl);
    final expiresAt = DateTime.tryParse(
      _requiredString(json, 'downloadUrlExpiresAt'),
    );
    if (url == null ||
        !{'http', 'https'}.contains(url.scheme) ||
        url.host.isEmpty ||
        expiresAt == null) {
      throw const FormatException('Medya erişim bilgisi geçersiz.');
    }
    return MediaGrant(
      assetId: _requiredInt(json, 'assetId'),
      mimeType: _requiredString(json, 'mimeType'),
      url: url,
      expiresAt: expiresAt.toUtc(),
    );
  }
}

Map<String, dynamic> _object(Object? value) {
  if (value is Map<String, dynamic>) return value;
  throw const FormatException('Medya manifestinin biçimi geçersiz.');
}

int _requiredInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is int) return value;
  throw FormatException('Medya manifestinde $key eksik.');
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is String && value.isNotEmpty) return value;
  throw FormatException('Medya manifestinde $key eksik.');
}
