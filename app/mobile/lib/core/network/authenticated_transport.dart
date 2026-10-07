import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:injectable/injectable.dart';
import '../../app/library_configuration.dart';

/// One app-owned transport; admitted requests own cancellable native sockets.
/// Failures contain status or fixed text, never URLs or credentials.
@lazySingleton
class AuthenticatedTransport {
  AuthenticatedTransport(this._configuration);
  final LibraryConfiguration _configuration;
  final _requests = <_CatalogRequest>{};
  bool _closed = false;
  Duration get totalWait => Duration(
    microseconds: _configuration.catalogWait.inMicroseconds * 4 ~/ 5,
  );
  Duration get headerWait => Duration(
    microseconds: _configuration.catalogWait.inMicroseconds * 2 ~/ 5,
  );
  Duration get idleWait => headerWait;

  Future<Map<String, dynamic>> library(String language) =>
      _get('/api/test/library', {'lang': language});

  Future<Map<String, dynamic>> storyDetail({
    required int categoryId,
    required int contentId,
    required String uiLanguage,
    required String contentLanguage,
    required String experience,
  }) => _get('/api/test/library/stories/$contentId', {
    'categoryId': '$categoryId',
    'lang': uiLanguage,
    'contentLang': contentLanguage,
    'experience': experience,
  });

  Future<Map<String, dynamic>> _get(
    String path,
    Map<String, String> parameters,
  ) async {
    if (_closed) throw StateError('Library transport is closed');
    if (_requests.length >= 8) {
      throw StateError('Library transport admission is full');
    }
    final operation = _CatalogRequest(headerWait);
    _requests.add(operation);
    operation.timer = Timer(totalWait, operation.expire);
    try {
      final request = await operation
          .wait(
            operation.client
                .getUrl(
                  _configuration.baseUrl
                      .resolve(path)
                      .replace(queryParameters: parameters),
                )
                .then((request) {
                  operation.request = request;
                  if (operation.cancelled.isCompleted) request.abort();
                  return request;
                }),
          )
          .timeout(
            headerWait,
            onTimeout: () {
              operation.expire();
              throw TimeoutException('Catalog response headers expired');
            },
          );
      request.headers.set(
        HttpHeaders.authorizationHeader,
        'Bearer ${_configuration.idToken}',
      );
      final response = await operation
          .wait(request.close())
          .timeout(
            headerWait,
            onTimeout: () {
              operation.expire();
              throw TimeoutException('Catalog response headers expired');
            },
          );
      final bytes = BytesBuilder(copy: false);
      final body = Completer<void>();
      operation.subscription = response
          .timeout(idleWait)
          .listen(
            (chunk) {
              if (body.isCompleted) return;
              operation.received += chunk.length;
              // Discarded non-success bodies have the same finite byte bound.
              if (operation.received > 1048576) {
                body.completeError(
                  const FormatException('Catalog exceeds transport bound'),
                );
                operation.abort();
                return;
              }
              if (response.statusCode == 200) bytes.add(chunk);
            },
            onError: (Object error, StackTrace stack) {
              if (!body.isCompleted) body.completeError(error, stack);
            },
            onDone: () {
              if (!body.isCompleted) body.complete();
            },
            cancelOnError: true,
          );
      await operation.wait(body.future);
      if (response.statusCode != 200) {
        throw CatalogHttpFailure(response.statusCode);
      }
      final value = jsonDecode(utf8.decode(bytes.takeBytes()));
      if (value is! Map<String, dynamic>) {
        throw const FormatException('Catalog shape is invalid');
      }
      return value;
    } finally {
      operation.timer?.cancel();
      operation.abort();
      await operation.subscription?.cancel();
      _requests.remove(operation);
    }
  }

  @disposeMethod
  void close() {
    _closed = true;
    for (final operation in _requests.toList()) {
      operation.expire();
    }
  }
}

class _CatalogRequest {
  _CatalogRequest(Duration connectionWait)
    : client = HttpClient()..connectionTimeout = connectionWait;
  final HttpClient client;
  final cancelled = Completer<void>();
  HttpClientRequest? request;
  StreamSubscription<List<int>>? subscription;
  Timer? timer;
  int received = 0;
  Future<T> wait<T>(Future<T> pending) => Future.any([
    pending,
    cancelled.future.then<T>(
      (_) => throw TimeoutException('Catalog transfer expired'),
    ),
  ]);
  void expire() {
    if (!cancelled.isCompleted) cancelled.complete();
    abort();
  }

  void abort() {
    request?.abort();
    client.close(force: true);
  }
}

class CatalogHttpFailure implements Exception {
  const CatalogHttpFailure(this.status);
  final int status;
}
