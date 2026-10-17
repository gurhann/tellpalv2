import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
part 'reading_image_comparison_model.g.dart';

@JsonSerializable(createToJson: false)
class ReadingImageComparisonModel extends ReadingImageComparison {
  ReadingImageComparisonModel({
    required this.identity,
    required this.descriptor,
    required int leaseMillis,
  }) : super(
         _manifest(identity, descriptor),
         _context(identity, descriptor),
         identity['liveGeneration'] as int,
         leaseMillis,
       );
  final Map<String, dynamic> identity, descriptor;
  factory ReadingImageComparisonModel.fromJson(Map<String, dynamic> json) =>
      _$ReadingImageComparisonModelFromJson(json);
}

ReadingImageRole _role(String value) => switch (value) {
  'READING_COVER' => ReadingImageRole.readingCover,
  'READING_DETAIL' => ReadingImageRole.readingDetail,
  'READING_PAGE' => ReadingImageRole.readingPage,
  _ => throw const FormatException('role'),
};
ReadingImageIdentity _identity(Map<String, dynamic> v) => ReadingImageIdentity(
  contentId: v['contentId'] as int,
  language: v['language'] as String,
  experience: 'READING',
  revision: v['revision'] as String,
  manifestDigest: v['manifestDigest'] as String,
  jobId: v['jobId'] as String,
);
ReadingImageRaster _raster(Map<String, dynamic> v) => ReadingImageRaster(
  provider: v['provider'] as String,
  objectKey: v['objectKey'] as String,
  generation: v['generation'] as String,
  mime: v['mime'] as String,
  byteSize: v['byteSize'] as int,
  sha256: v['sha256'] as String,
  width: v['width'] as int,
  height: v['height'] as int,
);
ReadingImageSource _source(Map<String, dynamic> v) {
  final b = v['binding'] as Map<String, dynamic>;
  return ReadingImageSource(
    binding: ReadingImageBinding(
      role: _role(b['role'] as String),
      localizationId: b['localizationId'] as int,
      assetId: b['assetId'] as int,
      pageId: b['pageId'] as int?,
      pageNumber: b['pageNumber'] as int,
    ),
    raster: _raster(v['raster'] as Map<String, dynamic>),
  );
}

ReadingImageManifest _manifest(
  Map<String, dynamic> i,
  Map<String, dynamic> d,
) => ReadingImageManifest(
  identity: _identity(i),
  images: (d['images'] as List<dynamic>).map((raw) {
    final e = raw as Map<String, dynamic>;
    return ReadingImageEntry(
      source: _source(e['source'] as Map<String, dynamic>),
      renditions: (e['renditions'] as List<dynamic>).map((raw) {
        final r = raw as Map<String, dynamic>;
        return ReadingImageRendition(
          renditionKey: r['renditionKey'] as String,
          sourceSha256: r['sourceSha256'] as String,
          profileFingerprint: r['profileFingerprint'] as String,
          raster: _raster(r['raster'] as Map<String, dynamic>),
        );
      }).toList(),
    );
  }).toList(),
);
ReadingImageManifestContext _context(
  Map<String, dynamic> i,
  Map<String, dynamic> d,
) => ReadingImageManifestContext(
  identity: _identity(i),
  sources: (d['images'] as List<dynamic>)
      .map(
        (raw) => _source(
          (raw as Map<String, dynamic>)['source'] as Map<String, dynamic>,
        ),
      )
      .toList(),
  profiles: (d['profiles'] as Map<String, dynamic>).map(
    (key, value) => MapEntry(
      _role(key),
      (value as List<dynamic>).map((raw) {
        final p = raw as Map<String, dynamic>;
        return ReadingImageProfile(
          fingerprint: p['fingerprint'] as String,
          width: p['width'] as int,
          height: p['height'] as int,
          allowGrowth: p['allowGrowth'] as bool,
        );
      }).toList(),
    ),
  ),
);
