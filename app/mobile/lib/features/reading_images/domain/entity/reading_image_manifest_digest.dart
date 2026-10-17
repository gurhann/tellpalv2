import 'dart:convert';

import 'package:crypto/crypto.dart';

import 'reading_image_manifest.dart';

/// P2 ReadingWork.Manifest metadata kimliği. Gerçek dosya byte'larını okumaz.
abstract final class ReadingImageManifestDigest {
  static String digest(ReadingImageManifest manifest) =>
      _hash('tellpal.reading.manifest.v1', canonical(manifest));

  static String canonical(ReadingImageManifest manifest) => _canonical({
    'revision': manifest.identity.revision,
    'snapshot': {
      'contentId': manifest.identity.contentId,
      'language': manifest.identity.language,
      'bindings': [
        for (final image in manifest.images) _binding(image.source.binding),
      ],
      'errors': <Object?>[],
    },
    'outputs': [
      for (final image in manifest.images)
        for (final rendition in image.renditions)
          {
            'binding': _binding(image.source.binding),
            'sourceSha256': rendition.sourceSha256,
            'profileFingerprint': rendition.profileFingerprint,
            'renditionKey': rendition.renditionKey,
            'provider': rendition.raster.provider,
            'stored': {
              'path': rendition.raster.objectKey,
              'version': rendition.raster.generation,
              'mime': rendition.raster.mime,
              'bytes': rendition.raster.byteSize,
            },
            'sha256': rendition.raster.sha256,
            'width': rendition.raster.width,
            'height': rendition.raster.height,
          },
    ],
  });

  static String renditionKey(
    ReadingImageSource source,
    ReadingImageProfile profile,
  ) => _hash(
    'tellpal.reading.rendition.v1',
    _canonical({
      'schema': 1,
      'role': _role(source.binding.role),
      'sourceSha256': source.raster.sha256,
      'profileSha256': profile.fingerprint,
    }),
  );

  static Map<String, Object?> _binding(ReadingImageBinding binding) => {
    'role': _role(binding.role),
    'localizationId': binding.localizationId,
    'pageId': binding.pageId,
    'pageNumber': binding.pageNumber,
    'assetId': binding.assetId,
  };

  static String _role(ReadingImageRole role) => switch (role) {
    ReadingImageRole.readingCover => 'READING_COVER',
    ReadingImageRole.readingDetail => 'READING_DETAIL',
    ReadingImageRole.readingPage => 'READING_PAGE',
  };

  static String _hash(String domain, String canonical) =>
      sha256.convert(utf8.encode('$domain\n$canonical')).toString();

  static String _canonical(Object? value) {
    if (value == null) return 'null';
    if (value is String) return _string(value);
    if (value is int || value is bool) return value.toString();
    if (value is List<Object?>) return '[${value.map(_canonical).join(',')}]';
    if (value is Map<String, Object?>) {
      final keys = value.keys.toList()..sort();
      return '{${keys.map((key) => '${_string(key)}:${_canonical(value[key])}').join(',')}}';
    }
    throw const FormatException('Desteklenmeyen P2 canonical metadata tipi');
  }

  static String _string(String value) {
    final result = StringBuffer('"');
    for (final c in value.codeUnits) {
      if (c > 127) {
        throw const FormatException('P2 kimlik metadata ASCII olmalı');
      }
      result.write(switch (c) {
        34 => r'\"',
        92 => r'\\',
        8 => r'\b',
        12 => r'\f',
        10 => r'\n',
        13 => r'\r',
        9 => r'\t',
        < 32 || 127 => '\\u${c.toRadixString(16).padLeft(4, '0')}',
        _ => String.fromCharCode(c),
      });
    }
    return '${result.toString()}"';
  }
}
