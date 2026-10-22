import 'package:equatable/equatable.dart';

enum ReadingImageRole { readingCover, readingDetail, readingPage }

/// Yayın kimliğidir; erişim hakkı veya dosya doğrulama sonucu değildir.
final class ReadingImageIdentity extends Equatable {
  const ReadingImageIdentity({
    required this.contentId,
    required this.language,
    required this.experience,
    required this.revision,
    required this.manifestDigest,
    required this.jobId,
  });

  final int contentId;
  final String language, experience, revision, manifestDigest, jobId;

  @override
  List<Object?> get props => [
    contentId,
    language,
    experience,
    revision,
    manifestDigest,
    jobId,
  ];
}

final class ReadingImageBinding extends Equatable {
  const ReadingImageBinding({
    required this.role,
    required this.localizationId,
    required this.assetId,
    this.pageId,
    this.pageNumber = 0,
  });

  final ReadingImageRole role;
  final int localizationId, assetId, pageNumber;
  final int? pageId;

  @override
  List<Object?> get props => [
    role,
    localizationId,
    assetId,
    pageId,
    pageNumber,
  ];
}

/// URL içermez. Sabitlenmiş nesnenin yalnız descriptor metadata'sını taşır.
final class ReadingImageRaster extends Equatable {
  const ReadingImageRaster({
    required this.provider,
    required this.objectKey,
    required this.generation,
    required this.mime,
    required this.byteSize,
    required this.sha256,
    required this.width,
    required this.height,
  });

  final String provider, objectKey, generation, mime, sha256;
  final int byteSize, width, height;

  @override
  List<Object?> get props => [
    provider,
    objectKey,
    generation,
    mime,
    byteSize,
    sha256,
    width,
    height,
  ];
}

final class ReadingImageSource extends Equatable {
  const ReadingImageSource({required this.binding, required this.raster});

  final ReadingImageBinding binding;
  final ReadingImageRaster raster;

  @override
  List<Object?> get props => [binding, raster];
}

/// Profil seçimi çağırana aittir; burada üretim varsayılanı bulunmaz.
final class ReadingImageProfile extends Equatable {
  const ReadingImageProfile({
    required this.fingerprint,
    required this.width,
    required this.height,
    required this.allowGrowth,
    this.formatPolicy = 'PNG_FIT',
  });

  final String fingerprint;
  final int width, height;
  final bool allowGrowth;
  final String formatPolicy;

  @override
  List<Object?> get props => [
    fingerprint,
    width,
    height,
    allowGrowth,
    formatPolicy,
  ];
}

final class ReadingImageRendition extends Equatable {
  const ReadingImageRendition({
    required this.renditionKey,
    required this.sourceSha256,
    required this.profileFingerprint,
    required this.raster,
  });

  final String renditionKey, sourceSha256, profileFingerprint;
  final ReadingImageRaster raster;

  @override
  List<Object?> get props => [
    renditionKey,
    sourceSha256,
    profileFingerprint,
    raster,
  ];
}

final class ReadingImageEntry extends Equatable {
  ReadingImageEntry({
    required this.source,
    required List<ReadingImageRendition> renditions,
  }) : renditions = List.unmodifiable(renditions);

  final ReadingImageSource source;
  final List<ReadingImageRendition> renditions;

  @override
  List<Object?> get props => [source, renditions];
}

final class ReadingImageManifest {
  ReadingImageManifest({
    required this.identity,
    required List<ReadingImageEntry> images,
  }) : images = List.unmodifiable(images);

  final ReadingImageIdentity identity;
  final List<ReadingImageEntry> images;
}

/// Tam kaynak sırası ve rol başına beklenen profil kümesi açıkça sağlanır.
final class ReadingImageManifestContext {
  ReadingImageManifestContext({
    required this.identity,
    required List<ReadingImageSource> sources,
    required Map<ReadingImageRole, List<ReadingImageProfile>> profiles,
  }) : sources = List.unmodifiable(sources),
       profiles = Map.unmodifiable({
         for (final entry in profiles.entries)
           entry.key: List<ReadingImageProfile>.unmodifiable(entry.value),
       });

  final ReadingImageIdentity identity;
  final List<ReadingImageSource> sources;
  final Map<ReadingImageRole, List<ReadingImageProfile>> profiles;
}
