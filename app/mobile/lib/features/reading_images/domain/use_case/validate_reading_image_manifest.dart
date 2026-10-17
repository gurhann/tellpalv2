import 'dart:math' as math;

import 'package:dartz/dartz.dart';

import '../entity/reading_image_manifest.dart';
import '../entity/reading_image_manifest_digest.dart';

enum ReadingImageManifestFailure {
  invalidIdentity,
  contextMismatch,
  incompleteBindings,
  sourceMismatch,
  invalidMetadata,
  profileMismatch,
  renditionMismatch,
  geometryMismatch,
  manifestDigestMismatch,
}

/// Yalnız bu dosyanın doğrulayıcısı oluşturabilir. Byte veya erişim kanıtı değildir.
final class ValidatedReadingImageManifest {
  const ValidatedReadingImageManifest._(this.descriptor);

  final ReadingImageManifest descriptor;
}

final class ValidateReadingImageManifest {
  const ValidateReadingImageManifest();

  Either<ReadingImageManifestFailure, ValidatedReadingImageManifest> call({
    required ReadingImageManifest manifest,
    required ReadingImageManifestContext expected,
  }) {
    if (!_validIdentity(expected.identity) ||
        !_validIdentity(manifest.identity)) {
      return const Left(ReadingImageManifestFailure.invalidIdentity);
    }
    if (manifest.identity != expected.identity) {
      return const Left(ReadingImageManifestFailure.contextMismatch);
    }
    if (!_completeBindings(expected.sources) ||
        !_completeBindings(manifest.images.map((e) => e.source).toList()) ||
        manifest.images.length != expected.sources.length) {
      return const Left(ReadingImageManifestFailure.incompleteBindings);
    }
    if (expected.profiles.length != ReadingImageRole.values.length) {
      return const Left(ReadingImageManifestFailure.profileMismatch);
    }
    for (var i = 0; i < manifest.images.length; i++) {
      final entry = manifest.images[i];
      if (entry.source != expected.sources[i]) {
        return const Left(ReadingImageManifestFailure.sourceMismatch);
      }
      final failure = _validateEntry(
        entry,
        expected.profiles[entry.source.binding.role] ?? const [],
      );
      if (failure != null) return Left(failure);
    }
    try {
      if (ReadingImageManifestDigest.digest(manifest) !=
          expected.identity.manifestDigest) {
        return const Left(ReadingImageManifestFailure.manifestDigestMismatch);
      }
    } on FormatException {
      return const Left(ReadingImageManifestFailure.invalidMetadata);
    }
    return Right(ValidatedReadingImageManifest._(manifest));
  }

  ReadingImageManifestFailure? _validateEntry(
    ReadingImageEntry entry,
    List<ReadingImageProfile> profiles,
  ) {
    if (!_validRaster(entry.source.raster)) {
      return ReadingImageManifestFailure.invalidMetadata;
    }
    if (profiles.isEmpty ||
        profiles.any((p) => !_validProfile(p)) ||
        profiles.map((p) => p.fingerprint).toSet().length != profiles.length ||
        entry.renditions.length != profiles.length) {
      return ReadingImageManifestFailure.profileMismatch;
    }
    final seen = <String>{};
    for (final rendition in entry.renditions) {
      final profile = profiles
          .where((p) => p.fingerprint == rendition.profileFingerprint)
          .firstOption;
      if (profile == null || !seen.add(rendition.profileFingerprint)) {
        return ReadingImageManifestFailure.profileMismatch;
      }
      if (!_validRaster(rendition.raster) ||
          rendition.raster.mime != 'image/png') {
        return ReadingImageManifestFailure.invalidMetadata;
      }
      final source = entry.source.raster;
      if (rendition.sourceSha256 != source.sha256 ||
          rendition.raster.provider != source.provider ||
          rendition.renditionKey !=
              ReadingImageManifestDigest.renditionKey(entry.source, profile) ||
          (!profile.allowGrowth &&
              rendition.raster.byteSize >= source.byteSize)) {
        return ReadingImageManifestFailure.renditionMismatch;
      }
      if (!_floorFit(source, rendition.raster, profile)) {
        return ReadingImageManifestFailure.geometryMismatch;
      }
    }
    return null;
  }

  bool _completeBindings(List<ReadingImageSource> sources) {
    if (sources.length < 3 ||
        sources[0].binding.role != ReadingImageRole.readingCover ||
        sources[1].binding.role != ReadingImageRole.readingDetail) {
      return false;
    }
    final cover = sources[0].binding;
    final detail = sources[1].binding;
    if (cover.assetId != detail.assetId ||
        cover.localizationId != detail.localizationId ||
        sources[0].raster != sources[1].raster) {
      return false;
    }
    final pageIds = <int>{};
    for (var i = 0; i < sources.length; i++) {
      final binding = sources[i].binding;
      if (binding.localizationId <= 0 || binding.assetId <= 0) return false;
      if (i < 2) {
        if (binding.pageId != null || binding.pageNumber != 0) return false;
        continue;
      }
      if (binding.role != ReadingImageRole.readingPage ||
          binding.pageNumber != i - 1 ||
          binding.pageId == null ||
          binding.pageId! <= 0 ||
          !pageIds.add(binding.pageId!)) {
        return false;
      }
    }
    return true;
  }

  bool _validIdentity(ReadingImageIdentity identity) =>
      identity.contentId > 0 &&
      const {'tr', 'en', 'es', 'pt', 'de'}.contains(identity.language) &&
      identity.experience == 'READING' &&
      _hash(identity.revision) &&
      _hash(identity.manifestDigest) &&
      RegExp(
        r'^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$',
      ).hasMatch(identity.jobId);

  bool _validRaster(ReadingImageRaster raster) =>
      const {'FIREBASE_STORAGE', 'LOCAL_STUB'}.contains(raster.provider) &&
      raster.objectKey.trim().isNotEmpty &&
      raster.generation.trim().isNotEmpty &&
      RegExp(r'^image/[a-zA-Z0-9.+-]+$').hasMatch(raster.mime) &&
      raster.byteSize > 0 &&
      raster.width > 0 &&
      raster.height > 0 &&
      _hash(raster.sha256);

  bool _validProfile(ReadingImageProfile profile) =>
      _hash(profile.fingerprint) && profile.width > 0 && profile.height > 0;

  bool _hash(String value) => RegExp(r'^[0-9a-f]{64}$').hasMatch(value);

  bool _floorFit(
    ReadingImageRaster source,
    ReadingImageRaster output,
    ReadingImageProfile profile,
  ) {
    if (source.width < profile.width || source.height < profile.height) {
      return false;
    }
    final scale = math.min(
      profile.width / source.width,
      profile.height / source.height,
    );
    return output.width == math.max(1, (source.width * scale).floor()) &&
        output.height == math.max(1, (source.height * scale).floor());
  }
}

extension on Iterable<ReadingImageProfile> {
  ReadingImageProfile? get firstOption => isEmpty ? null : first;
}
