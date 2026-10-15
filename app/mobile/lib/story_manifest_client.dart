import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'story_manifest.dart';

abstract interface class StoryManifestSource {
  Future<StoryManifest> fetch({
    required int contentId,
    required String languageCode,
    String? revision,
  });
  void close();
}

class StoryManifestClient implements StoryManifestSource {
  StoryManifestClient({required this.baseUrl, required this.bearerToken}) {
    _httpClient.connectionTimeout = const Duration(seconds: 8);
  }

  final Uri baseUrl;
  final String bearerToken;
  final HttpClient _httpClient = HttpClient();

  @override
  Future<StoryManifest> fetch({
    required int contentId,
    required String languageCode,
    String? revision,
  }) async {
    final queryParameters = {'lang': languageCode};
    if (revision != null) queryParameters['revision'] = revision;
    final uri = baseUrl.replace(
      path: '/api/test/story-page-media/$contentId',
      queryParameters: queryParameters,
    );
    try {
      final request = await _httpClient
          .getUrl(uri)
          .timeout(const Duration(seconds: 10));
      request.headers.set(
        HttpHeaders.authorizationHeader,
        'Bearer $bearerToken',
      );
      request.headers.set(HttpHeaders.acceptHeader, 'application/json');
      final response = await request.close().timeout(
        const Duration(seconds: 15),
      );
      if (response.statusCode != HttpStatus.ok) {
        await response.drain<void>();
        throw StoryManifestException(switch (response.statusCode) {
          HttpStatus.unauthorized =>
            'Token geçersiz veya backend test kimliğini kabul etmiyor.',
          HttpStatus.forbidden =>
            'Bu kullanıcı medya testi izin listesinde değil.',
          HttpStatus.notFound =>
            'Bu hikâye için hazır medya manifesti bulunamadı.',
          _ => 'Backend medya isteğini tamamlayamadı (${response.statusCode}).',
        });
      }
      final body = await response
          .transform(utf8.decoder)
          .join()
          .timeout(const Duration(seconds: 15));
      final decoded = jsonDecode(body);
      if (decoded is! Map<String, dynamic>) {
        throw const FormatException('Medya manifestinin biçimi geçersiz.');
      }
      final manifest = StoryManifest.fromJson(decoded);
      if (manifest.contentId != contentId ||
          manifest.languageCode != languageCode) {
        throw const FormatException(
          'Medya manifesti istenen hikâyeye ait değil.',
        );
      }
      return manifest;
    } on SocketException {
      throw const StoryManifestException(
        'Backend’e bağlanılamadı. Adresi ve ağı kontrol edin.',
      );
    } on TimeoutException {
      throw const StoryManifestException(
        'Backend yanıtı zaman aşımına uğradı.',
      );
    } on HandshakeException {
      throw const StoryManifestException('Backend TLS bağlantısı kurulamadı.');
    } on FormatException catch (error) {
      throw StoryManifestException(error.message);
    }
  }

  @override
  void close() => _httpClient.close(force: true);
}

class StoryManifestException implements Exception {
  const StoryManifestException(this.message);

  final String message;

  @override
  String toString() => message;
}
