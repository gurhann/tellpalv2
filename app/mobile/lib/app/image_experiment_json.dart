import 'dart:convert';

/// Reject duplicate object keys at every depth before the standard decoder runs.
Object? decodeImageExperimentJson(String source) {
  final scanner = _JsonScanner(source);
  scanner.value();
  scanner.whitespace();
  if (scanner.offset != source.length) {
    throw const FormatException('json_refused');
  }
  return jsonDecode(source);
}

class _JsonScanner {
  _JsonScanner(this.source);
  final String source;
  int offset = 0;
  int depth = 0;
  void whitespace() {
    while (offset < source.length && ' \t\r\n'.contains(source[offset])) {
      offset++;
    }
  }

  String string() {
    final start = offset;
    if (offset >= source.length || source[offset++] != '"') {
      throw const FormatException('json_refused');
    }
    while (offset < source.length) {
      final character = source[offset++];
      if (character == '\\') {
        offset++;
      } else if (character == '"') {
        return jsonDecode(source.substring(start, offset)) as String;
      }
    }
    throw const FormatException('json_refused');
  }

  void value() {
    whitespace();
    if (offset >= source.length || ++depth > 32) {
      throw const FormatException('json_refused');
    }
    final character = source[offset];
    if (character == '{') {
      offset++;
      whitespace();
      final keys = <String>{};
      if (offset < source.length && source[offset] == '}') {
        offset++;
        depth--;
        return;
      }
      while (true) {
        whitespace();
        final key = string();
        if (!keys.add(key)) throw const FormatException('json_duplicate');
        whitespace();
        if (offset >= source.length || source[offset++] != ':') {
          throw const FormatException('json_refused');
        }
        value();
        whitespace();
        if (offset >= source.length) {
          throw const FormatException('json_refused');
        }
        if (source[offset++] == '}') break;
        if (source[offset - 1] != ',') {
          throw const FormatException('json_refused');
        }
      }
    } else if (character == '[') {
      offset++;
      whitespace();
      if (offset < source.length && source[offset] == ']') {
        offset++;
        depth--;
        return;
      }
      while (true) {
        value();
        whitespace();
        if (offset >= source.length) {
          throw const FormatException('json_refused');
        }
        if (source[offset++] == ']') break;
        if (source[offset - 1] != ',') {
          throw const FormatException('json_refused');
        }
      }
    } else if (character == '"') {
      string();
    } else {
      final start = offset;
      while (offset < source.length && !' \t\r\n,]}'.contains(source[offset])) {
        offset++;
      }
      if (offset == start) throw const FormatException('json_refused');
      final primitive = jsonDecode(source.substring(start, offset));
      if (primitive is num && !primitive.isFinite) {
        throw const FormatException('json_refused');
      }
    }
    depth--;
  }
}
