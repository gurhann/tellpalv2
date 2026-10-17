import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest_digest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';

String hash(String digit) => List.filled(64, digit).join();

ReadingImageIdentity identity({
  int contentId = 71,
  String language = 'tr',
  String experience = 'READING',
  String? revision,
  String? digest,
  String jobId = '12345678-1234-1234-1234-123456789abc',
}) => ReadingImageIdentity(
  contentId: contentId,
  language: language,
  experience: experience,
  revision: revision ?? hash('a'),
  manifestDigest: digest ?? hash('b'),
  jobId: jobId,
);

ReadingImageManifest seal(
  List<ReadingImageEntry> images, {
  ReadingImageIdentity? publication,
}) {
  final original = publication ?? identity();
  final draft = ReadingImageManifest(identity: original, images: images);
  return ReadingImageManifest(
    identity: ReadingImageIdentity(
      contentId: original.contentId,
      language: original.language,
      experience: original.experience,
      revision: original.revision,
      manifestDigest: ReadingImageManifestDigest.digest(draft),
      jobId: original.jobId,
    ),
    images: images,
  );
}

ReadingImageRaster raster({
  int width = 2400,
  int height = 1600,
  int bytes = 1000000,
  String mime = 'image/png',
  String? sha,
  String provider = 'FIREBASE_STORAGE',
  String objectKey = 'test/source.png',
  String generation = '123',
}) => ReadingImageRaster(
  provider: provider,
  objectKey: objectKey,
  generation: generation,
  mime: mime,
  byteSize: bytes,
  sha256: sha ?? hash('c'),
  width: width,
  height: height,
);

ReadingImageProfile profile({
  int width = 1200,
  int height = 800,
  String? fingerprint,
  bool allowGrowth = false,
}) => ReadingImageProfile(
  fingerprint: fingerprint ?? hash('d'),
  width: width,
  height: height,
  allowGrowth: allowGrowth,
);

ReadingImageBinding binding(
  ReadingImageRole role, {
  int? pageId,
  int? pageNumber,
  int? assetId,
  int localizationId = 7,
}) => ReadingImageBinding(
  role: role,
  localizationId: localizationId,
  assetId: assetId ?? (role == ReadingImageRole.readingPage ? 9 : 8),
  pageId: pageId ?? (role == ReadingImageRole.readingPage ? 10 : null),
  pageNumber: pageNumber ?? (role == ReadingImageRole.readingPage ? 1 : 0),
);

ReadingImageRendition rendition(
  ReadingImageSource source,
  ReadingImageProfile p, {
  int? width,
  int? height,
  int bytes = 100000,
  String? key,
  String? sourceSha,
  String? fingerprint,
  String? sha,
  String mime = 'image/png',
  String provider = 'FIREBASE_STORAGE',
  String? objectKey,
  String generation = '123',
}) {
  final roles = ['READING_COVER', 'READING_DETAIL', 'READING_PAGE'];
  final data = jsonEncode({
    'profileSha256': p.fingerprint,
    'role': roles[source.binding.role.index],
    'schema': 1,
    'sourceSha256': source.raster.sha256,
  });
  final keyHash = sha256
      .convert(utf8.encode('tellpal.reading.rendition.v1\n$data'))
      .toString();
  final s = source.raster;
  final sw = BigInt.from(s.width);
  final sh = BigInt.from(s.height);
  final pw = BigInt.from(p.width);
  final ph = BigInt.from(p.height);
  final widthLimits = pw * sh <= ph * sw;
  final expectedWidth = widthLimits ? p.width : (sw * ph ~/ sh).toInt();
  final expectedHeight = widthLimits ? (sh * pw ~/ sw).toInt() : p.height;
  return ReadingImageRendition(
    renditionKey: key ?? keyHash,
    sourceSha256: sourceSha ?? s.sha256,
    profileFingerprint: fingerprint ?? p.fingerprint,
    raster: raster(
      width: width ?? expectedWidth,
      height: height ?? expectedHeight,
      bytes: bytes,
      sha: sha ?? hash('e'),
      mime: mime,
      provider: provider,
      objectKey: objectKey ?? 'test/$keyHash.png',
      generation: generation,
    ),
  );
}

final class Fixture {
  Fixture({
    ReadingImageRaster? source,
    List<ReadingImageProfile>? profiles,
    int pages = 2,
    Map<String, int> outputBytes = const {},
  }) {
    final actualProfiles = profiles ?? [profile()];
    sources = [
      ReadingImageSource(
        binding: binding(ReadingImageRole.readingCover),
        raster: source ?? raster(),
      ),
      ReadingImageSource(
        binding: binding(ReadingImageRole.readingDetail),
        raster: source ?? raster(),
      ),
      for (var i = 1; i <= pages; i++)
        ReadingImageSource(
          binding: binding(
            ReadingImageRole.readingPage,
            pageId: 10 + i,
            pageNumber: i,
          ),
          raster: source ?? raster(),
        ),
    ];
    manifest = seal([
      for (final s in sources)
        ReadingImageEntry(
          source: s,
          renditions: [
            for (final p in actualProfiles)
              rendition(s, p, bytes: outputBytes[p.fingerprint] ?? 100000),
          ],
        ),
    ]);
    context = ReadingImageManifestContext(
      identity: manifest.identity,
      sources: sources,
      profiles: {
        for (final role in ReadingImageRole.values) role: actualProfiles,
      },
    );
  }

  late final List<ReadingImageSource> sources;
  late final ReadingImageManifestContext context;
  late final ReadingImageManifest manifest;

  ValidatedReadingImageManifest get validated =>
      const ValidateReadingImageManifest()(
        manifest: manifest,
        expected: context,
      ).fold(
        (failure) => throw StateError('Fixture: $failure'),
        (value) => value,
      );
}
