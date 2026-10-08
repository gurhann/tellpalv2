import 'dart:convert';
import 'dart:io';

/// Local selection/expiry guard only; the backend remains the token verifier.
void requireSelectedToken(String token, String selectedUid, DateTime now) {
  try {
    final parts = token.split('.');
    if (parts.length != 3 || parts.any((part) => part.isEmpty)) {
      throw const FormatException();
    }
    final claims =
        jsonDecode(utf8.decode(base64Url.decode(base64Url.normalize(parts[1]))))
            as Map<String, dynamic>;
    final expires = claims['exp'];
    if (selectedUid.isEmpty ||
        claims['sub'] != selectedUid ||
        expires is! int ||
        expires <= now.millisecondsSinceEpoch ~/ 1000 + 60) {
      throw const FormatException();
    }
  } catch (_) {
    throw StateError('Selected QA token guard rejected');
  }
}

/// Admits only the fixed real-provider expiry fixture; never includes URL data in errors.
void requireProviderExpiryDescriptor(
  Map<String, dynamic> value,
  String uid,
  DateTime now,
) {
  try {
    const fields = {
      'firebaseUid',
      'categoryId',
      'contentId',
      'requestedLanguage',
      'contentLanguage',
      'experience',
      'assetId',
      'checksumSha256',
      'byteSize',
      'mimeType',
      'expiresAt',
      'expiredUrl',
    };
    final url = Uri.parse(value['expiredUrl'] as String);
    final expires = DateTime.parse(value['expiresAt'] as String);
    if (value.keys.toSet().difference(fields).isNotEmpty ||
        value.length != fields.length ||
        value['firebaseUid'] != uid ||
        value['categoryId'] != 2 ||
        value['contentId'] != 3 ||
        value['requestedLanguage'] != 'tr' ||
        value['contentLanguage'] != 'tr' ||
        value['experience'] != 'READING' ||
        value['assetId'] != 3 ||
        !RegExp(
          r'^[a-f0-9]{64}$',
        ).hasMatch(value['checksumSha256'] as String) ||
        value['byteSize'] is! int ||
        (value['byteSize'] as int) <= 0 ||
        (value['byteSize'] as int) > 20971520 ||
        value['mimeType'] != 'image/jpeg' ||
        !expires.isUtc ||
        !expires.isBefore(now) ||
        url.scheme != 'https' ||
        url.host != 'storage.googleapis.com' ||
        url.userInfo.isNotEmpty ||
        url.fragment.isNotEmpty ||
        url.pathSegments.length < 2 ||
        url.queryParameters['X-Goog-Algorithm'] != 'GOOG4-RSA-SHA256' ||
        !RegExp(
          r'^[a-fA-F0-9]+$',
        ).hasMatch(url.queryParameters['X-Goog-Signature'] ?? '') ||
        (int.tryParse(url.queryParameters['X-Goog-Expires'] ?? '') ?? 0) !=
            12) {
      throw const FormatException();
    }
  } catch (_) {
    throw StateError('Real provider expiry descriptor rejected');
  }
}

Future<Map<String, dynamic>> readProviderExpiryDescriptor(
  File file,
  String uid,
  DateTime now,
) async {
  try {
    if (await FileSystemEntity.type(file.path, followLinks: false) !=
        FileSystemEntityType.file) {
      throw const FormatException();
    }
    final resolved = await file.resolveSymbolicLinks();
    final temporary = await Directory.systemTemp.resolveSymbolicLinks();
    if (!resolved.startsWith('/private/tmp/') &&
        !resolved.startsWith('$temporary/')) {
      throw const FormatException();
    }
    final stat = await file.stat();
    if ((stat.mode & 0x1ff) != 0x180 || stat.size > 16384) {
      throw const FormatException();
    }
    final value = jsonDecode(await file.readAsString()) as Map<String, dynamic>;
    requireProviderExpiryDescriptor(value, uid, now);
    return value;
  } catch (_) {
    throw StateError('Private provider descriptor rejected');
  }
}

/// Resolves existing ancestors before creating files, including symlink escapes.
Future<Directory> preparePrivateOutput(Directory requested) async {
  final temporary = await Directory.systemTemp.resolveSymbolicLinks();
  var ancestor = requested.absolute;
  final suffix = <String>[];
  while (!await ancestor.exists()) {
    final uri = ancestor.uri.normalizePath();
    final segments = uri.pathSegments.where((part) => part.isNotEmpty).toList();
    if (segments.isEmpty) throw StateError('Private output directory rejected');
    suffix.insert(0, segments.last);
    ancestor = ancestor.parent;
  }
  final resolvedAncestor = await ancestor.resolveSymbolicLinks();
  final candidate = Directory(
    '$resolvedAncestor/${suffix.join('/')}',
  ).uri.normalizePath().toFilePath();
  bool contained(String root) =>
      candidate.startsWith('$root/') && candidate != '$root/';
  if (!contained('/private/tmp') && !contained(temporary)) {
    throw StateError('Private temporary output required');
  }
  final directory = await Directory(candidate).create(recursive: true);
  await privateMode(directory.path, '700');
  return directory;
}

Future<void> privateMode(String path, String mode) async {
  final result = await Process.run('chmod', [
    mode,
    path,
  ]).timeout(const Duration(seconds: 10));
  if (result.exitCode != 0) throw StateError('Private file mode rejected');
}

Future<void> preparePrivateFile(File file) async {
  final type = await FileSystemEntity.type(file.path, followLinks: false);
  if (type != FileSystemEntityType.notFound &&
      type != FileSystemEntityType.file) {
    throw StateError('Private output file rejected');
  }
  await file.create();
  await privateMode(file.path, '600');
}

Future<void> writePrivateJson(File file, Object value) async {
  await preparePrivateFile(file);
  await file
      .writeAsString(
        '${const JsonEncoder.withIndent('  ').convert(value)}\n',
        flush: true,
      )
      .timeout(const Duration(seconds: 10));
}
