import 'dart:io';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';

/// Bounded in-memory diagnostic evidence excludes tokens, URLs, response bodies and user IDs.
@lazySingleton
class LibraryDiagnostics {
  final List<Map<String, Object>> _events = [];
  void catalog(String language, int milliseconds, String outcome) => _record({
    'kind': 'catalog',
    'language': language,
    'milliseconds': milliseconds,
    'outcome': outcome,
  });
  void error(Object error) => _record({
    'kind': 'error',
    'type': error.runtimeType.toString(),
    if (error is HttpExceptionWithStatus) 'httpStatus': error.statusCode,
    if (error is SocketException && error.osError != null)
      'osErrorCode': error.osError!.errorCode,
    if (error is FileSystemException && error.osError != null)
      'osErrorCode': error.osError!.errorCode,
  });
  void _record(Map<String, Object> event) {
    if (_events.length == 100) _events.removeAt(0);
    _events.add(event);
  }

  List<Map<String, Object>> get events => List.unmodifiable(_events);
}
