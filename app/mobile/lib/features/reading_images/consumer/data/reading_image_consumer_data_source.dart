import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:injectable/injectable.dart';
import '../../../../app/reading_image_consumer_configuration.dart';
import '../domain/reading_image_admission.dart';

@injectable
class ReadingImageConsumerDataSource {
  ReadingImageConsumerDataSource(this.configuration);
  final ReadingImageConsumerConfiguration configuration;
  Future<Map<String, dynamic>> json(
    String path,
    Map<String, String> query,
    ReadingImageCancellation cancellation,
  ) async =>
      jsonDecode(
            utf8.decode(
              await read(
                path,
                query,
                cancellation,
                1048576,
                'application/json',
              ),
            ),
          )
          as Map<String, dynamic>;
  Future<Uint8List> read(
    String path,
    Map<String, String> query,
    ReadingImageCancellation cancellation,
    int limit,
    String mime,
  ) async {
    final client = HttpClient()..autoUncompress = false;
    void abort() => client.close(force: true);
    cancellation.add(abort);
    final timer = Timer(configuration.requestWait, abort);
    try {
      cancellation.check();
      final token = configuration.token();
      if (token.isEmpty) throw StateError('consumer_credential_missing');
      final request = await client.getUrl(
        configuration.origin
            .resolve('/api/library/reading-images/$path')
            .replace(queryParameters: query),
      );
      request.followRedirects = false;
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      request.headers.set(HttpHeaders.acceptEncodingHeader, 'identity');
      request.headers.set(HttpHeaders.cacheControlHeader, 'no-store');
      final response = await request.close();
      final directives =
          response.headers
              .value(HttpHeaders.cacheControlHeader)
              ?.replaceAll(' ', '')
              .toLowerCase()
              .split(',')
              .toSet() ??
          {};
      final encoding = response.headers.value(
        HttpHeaders.contentEncodingHeader,
      );
      if (response.statusCode != 200 ||
          response.headers.contentType?.mimeType != mime ||
          !directives.containsAll({'private', 'no-store'}) ||
          (encoding != null && encoding != 'identity') ||
          (response.contentLength == 0 ||
              (path == 'bytes' && response.contentLength < 0)) ||
          response.contentLength > limit) {
        throw HttpException('consumer_http_${response.statusCode}');
      }
      final buffer = BytesBuilder(copy: false);
      var count = 0;
      await for (final chunk in response) {
        cancellation.check();
        count += chunk.length;
        if (count > limit ||
            (response.contentLength >= 0 && count > response.contentLength)) {
          throw const FormatException('consumer_byte_limit');
        }
        buffer.add(chunk);
      }
      cancellation.check();
      if (response.contentLength >= 0 && count != response.contentLength) {
        throw const FormatException('consumer_truncated');
      }
      return buffer.takeBytes();
    } finally {
      timer.cancel();
      cancellation.remove(abort);
      abort();
    }
  }
}
