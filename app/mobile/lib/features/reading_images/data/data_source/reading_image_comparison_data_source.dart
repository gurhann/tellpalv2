import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:injectable/injectable.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';

@injectable
class ReadingImageComparisonDataSource {
  const ReadingImageComparisonDataSource(this.configuration);
  final ReadingImageComparisonConfiguration configuration;
  Future<Map<String, dynamic>> json(
    String path,
    Map<String, String> query,
    ReadingComparisonCancellation cancellation,
  ) async =>
      jsonDecode(
            utf8.decode(
              await read(path, query, cancellation, 65536),
              allowMalformed: false,
            ),
          )
          as Map<String, dynamic>;
  Future<Uint8List> read(
    String path,
    Map<String, String> query,
    ReadingComparisonCancellation cancellation,
    int limit,
  ) async {
    if (!configuration.mediaEnabled) {
      throw StateError('run_configuration_required');
    }
    final client = HttpClient()..autoUncompress = false;
    final timeout = Timer(
      Duration(milliseconds: configuration.limits!.transferMillis),
      () {
        cancellation.cancel();
        client.close(force: true);
      },
    );
    cancellation.onCancel = () => client.close(force: true);
    try {
      cancellation.check();
      final request = await client.getUrl(
        configuration.baseUrl!
            .resolve('/api/test/reading-image-comparison/$path')
            .replace(queryParameters: query),
      );
      request.followRedirects = false;
      request.headers.set(
        HttpHeaders.authorizationHeader,
        'Bearer ${configuration.account!.token()}',
      );
      request.headers.set(HttpHeaders.cacheControlHeader, 'no-store');
      request.headers.set(HttpHeaders.acceptEncodingHeader, 'identity');
      final response = await request.close();
      final cache =
          response.headers
              .value(HttpHeaders.cacheControlHeader)
              ?.replaceAll(' ', '')
              .split(',')
              .toSet() ??
          {};
      final expectedMime = path == 'bytes' ? 'image/png' : 'application/json';
      final encoding = response.headers.value(
        HttpHeaders.contentEncodingHeader,
      );
      if ((encoding != null && encoding.trim().toLowerCase() != 'identity') ||
          response.headers.contentType?.mimeType != expectedMime ||
          response.statusCode != 200 ||
          !cache.containsAll({'private', 'no-store'}) ||
          response.contentLength == 0 ||
          response.contentLength > limit) {
        throw StateError('http_${response.statusCode}_or_cache_or_length');
      }
      final buffer = BytesBuilder(copy: false);
      var count = 0;
      await for (final chunk in response) {
        cancellation.check();
        count += chunk.length;
        if (count > limit ||
            (response.contentLength >= 0 && count > response.contentLength)) {
          throw StateError('encoded_limit');
        }
        buffer.add(chunk);
      }
      cancellation.check();
      if (response.contentLength >= 0 && count != response.contentLength) {
        throw StateError('truncated_body');
      }
      return buffer.takeBytes();
    } finally {
      timeout.cancel();
      cancellation.onCancel = null;
      client.close(force: true);
    }
  }
}
