import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest_digest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';

import 'fixture.dart';

void main() {
  const validate = ValidateReadingImageManifest();

  test('F5 gerçek Java P2 canonical golden ve digest birebir eşleşir', () {
    const digest =
        'e2b4ff6ff58310a42e25d300f8f0187ed73d36c8d9ceb718052cda595d4f71e9';
    final f = Fixture(pages: 1);
    final manifest = ReadingImageManifest(
      identity: identity(digest: digest),
      images: [
        for (final source in f.sources)
          ReadingImageEntry(
            source: source,
            renditions: [
              rendition(
                source,
                profile(),
                objectKey: 'test/"q"\\slash\b\f\n\r\t\u0001\u007f.png',
                generation: 'v"\\\t\u001f\u007f',
              ),
            ],
          ),
      ],
    );
    final golden = File(
      'test/reading_images/fixtures/p2_manifest_golden.json',
    ).readAsStringSync();
    expect(ReadingImageManifestDigest.canonical(manifest), golden);
    expect(ReadingImageManifestDigest.digest(manifest), digest);
    final expected = ReadingImageManifestContext(
      identity: manifest.identity,
      sources: f.sources,
      profiles: f.context.profiles,
    );
    expect(validate(manifest: manifest, expected: expected).isRight(), isTrue);
  });

  final outputChanges =
      <String, ReadingImageRendition Function(ReadingImageSource)>{
        'path': (s) =>
            rendition(s, profile(), objectKey: 'test/substituted.png'),
        'version': (s) => rendition(s, profile(), generation: '124'),
        'SHA': (s) => rendition(s, profile(), sha: hash('f')),
        'byte': (s) => rendition(s, profile(), bytes: 99999),
      };
  for (final change in outputChanges.entries) {
    test('F5 sabit digest altında çıktı ${change.key} değişimi reddedilir', () {
      final f = Fixture();
      final entries = f.manifest.images.toList();
      entries[3] = ReadingImageEntry(
        source: f.sources[3],
        renditions: [change.value(f.sources[3])],
      );
      final manifest = ReadingImageManifest(
        identity: f.manifest.identity,
        images: entries,
      );
      expect(
        validate(
          manifest: manifest,
          expected: f.context,
        ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
        ReadingImageManifestFailure.manifestDigestMismatch,
      );
    });
  }

  test(
    'F5 bozuk digest hem context hem descriptor içinde olsa da reddedilir',
    () {
      final f = Fixture();
      final publication = identity(digest: hash('f'));
      final manifest = ReadingImageManifest(
        identity: publication,
        images: f.manifest.images,
      );
      final expected = ReadingImageManifestContext(
        identity: publication,
        sources: f.sources,
        profiles: f.context.profiles,
      );
      expect(
        validate(
          manifest: manifest,
          expected: expected,
        ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
        ReadingImageManifestFailure.manifestDigestMismatch,
      );
    },
  );

  test('F5 çıktı dizisi sırası canonical kimliğin parçasıdır', () {
    final f = Fixture(
      profiles: [
        profile(fingerprint: hash('1')),
        profile(),
      ],
    );
    final manifest = ReadingImageManifest(
      identity: f.manifest.identity,
      images: [
        for (final image in f.manifest.images)
          ReadingImageEntry(
            source: image.source,
            renditions: image.renditions.reversed.toList(),
          ),
      ],
    );
    expect(
      validate(
        manifest: manifest,
        expected: f.context,
      ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
      ReadingImageManifestFailure.manifestDigestMismatch,
    );
  });

  test(
    'F5 snapshot bağı context ile eşleşse de eski digest altında değişemez',
    () {
      final f = Fixture();
      final source = ReadingImageSource(
        binding: binding(
          ReadingImageRole.readingPage,
          pageId: f.sources.last.binding.pageId,
          pageNumber: 2,
          assetId: 99,
        ),
        raster: raster(),
      );
      final entries = f.manifest.images.toList();
      entries[3] = ReadingImageEntry(
        source: source,
        renditions: [rendition(source, profile())],
      );
      final manifest = ReadingImageManifest(
        identity: f.manifest.identity,
        images: entries,
      );
      final expected = ReadingImageManifestContext(
        identity: manifest.identity,
        sources: entries.map((e) => e.source).toList(),
        profiles: f.context.profiles,
      );
      expect(
        validate(
          manifest: manifest,
          expected: expected,
        ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
        ReadingImageManifestFailure.manifestDigestMismatch,
      );
    },
  );

  test('F5 canonical olmayan Unicode çıktı yolu tipli metadata hatasıdır', () {
    final f = Fixture();
    final entries = f.manifest.images.toList();
    entries[3] = ReadingImageEntry(
      source: f.sources[3],
      renditions: [
        rendition(f.sources[3], profile(), objectKey: 'test/değişti.png'),
      ],
    );
    final manifest = ReadingImageManifest(
      identity: f.manifest.identity,
      images: entries,
    );
    expect(
      validate(
        manifest: manifest,
        expected: f.context,
      ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
      ReadingImageManifestFailure.invalidMetadata,
    );
  });

  test('F5 content dil revision snapshot alanları digest içinde korunur', () {
    final f = Fixture();
    for (final publication in [
      identity(contentId: 72, digest: f.manifest.identity.manifestDigest),
      identity(language: 'en', digest: f.manifest.identity.manifestDigest),
      identity(revision: hash('1'), digest: f.manifest.identity.manifestDigest),
    ]) {
      final manifest = ReadingImageManifest(
        identity: publication,
        images: f.manifest.images,
      );
      final expected = ReadingImageManifestContext(
        identity: publication,
        sources: f.sources,
        profiles: f.context.profiles,
      );
      expect(
        validate(
          manifest: manifest,
          expected: expected,
        ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
        ReadingImageManifestFailure.manifestDigestMismatch,
      );
    }
  });
}
