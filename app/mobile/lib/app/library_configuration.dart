import '../core/preview/domain/entity/library_preview.dart';

/// Ephemeral development configuration; secrets are never serialized or persisted.
class LibraryConfiguration {
  LibraryConfiguration({
    required this.baseUrl,
    required this.idToken,
    required this.context,
    this.catalogWait = const Duration(seconds: 15),
    this.artworkWait = const Duration(seconds: 10),
  }) {
    if (!['http', 'https'].contains(baseUrl.scheme) ||
        baseUrl.host.isEmpty ||
        baseUrl.userInfo.isNotEmpty ||
        idToken.isEmpty ||
        !['tr', 'en', 'pt'].contains(context.language) ||
        context.accessIdentity.isEmpty ||
        catalogWait <= Duration.zero ||
        artworkWait <= Duration.zero) {
      throw ArgumentError('Invalid Library development configuration');
    }
  }
  final Uri baseUrl;
  final String idToken;
  final LibraryContext context;
  final Duration catalogWait;
  final Duration artworkWait;
}
