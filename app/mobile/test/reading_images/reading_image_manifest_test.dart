import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';

import 'fixture.dart';

void main() {
  const validate = ValidateReadingImageManifest();

  test('tam descriptor doğrulanır; kaynak ve çıktılar salt okunurdur', () {
    final f = Fixture();
    final result = validate(manifest: f.manifest, expected: f.context);
    expect(result.isRight(), isTrue);
    final verified = result.getOrElse(() => throw StateError('doğrulama'));
    expect(verified.descriptor, same(f.manifest));
    expect(() => verified.descriptor.images.clear(), throwsUnsupportedError);
    expect(
      () => verified.descriptor.images.first.renditions.clear(),
      throwsUnsupportedError,
    );
  });

  test('koleksiyonlar iç içe kopyalanır', () {
    final f = Fixture();
    final sources = f.sources.toList();
    final profiles = [profile()];
    final byRole = {for (final role in ReadingImageRole.values) role: profiles};
    final context = ReadingImageManifestContext(
      identity: identity(),
      sources: sources,
      profiles: byRole,
    );
    final renditions = f.manifest.images.first.renditions.toList();
    final entry = ReadingImageEntry(
      source: sources.first,
      renditions: renditions,
    );
    final entries = [entry];
    final descriptor = ReadingImageManifest(
      identity: identity(),
      images: entries,
    );
    sources.clear();
    profiles.clear();
    byRole.clear();
    renditions.clear();
    entries.clear();
    expect(context.sources.length, 4);
    expect(context.profiles.length, 3);
    expect(context.profiles.values.every((p) => p.length == 1), isTrue);
    expect(descriptor.images.single.renditions.length, 1);
    expect(() => context.profiles.clear(), throwsUnsupportedError);
    expect(() => context.profiles.values.first.clear(), throwsUnsupportedError);
    expect(() => context.sources.clear(), throwsUnsupportedError);
  });

  final mismatchedIdentities = [
    identity(contentId: 72),
    identity(language: 'en'),
    identity(revision: hash('1')),
    identity(digest: hash('2')),
    identity(jobId: '00000000-1234-1234-1234-123456789abc'),
  ];
  for (var i = 0; i < mismatchedIdentities.length; i++) {
    test('yanlış beklenen kimlik reddedilir: $i', () {
      final f = Fixture();
      final changed = ReadingImageManifest(
        identity: mismatchedIdentities[i],
        images: f.manifest.images,
      );
      expect(
        validate(
          manifest: changed,
          expected: f.context,
        ).swap().getOrElse(() => throw StateError('red')),
        ReadingImageManifestFailure.contextMismatch,
      );
    });
  }

  for (final invalid in [
    identity(contentId: 0),
    identity(language: 'TR'),
    identity(experience: 'LISTENING'),
    identity(revision: 'bad'),
    identity(digest: hash('G')),
    identity(jobId: ''),
  ]) {
    test('bozuk kimlik metadata reddedilir: ${invalid.props}', () {
      final f = Fixture();
      expect(
        validate(
          manifest: ReadingImageManifest(
            identity: invalid,
            images: f.manifest.images,
          ),
          expected: f.context,
        ).isLeft(),
        isTrue,
      );
    });
  }

  test('eksik, yinelenen ve sırasız roller/sayfalar kısmi başarı olamaz', () {
    final f = Fixture();
    final entries = f.manifest.images;
    final invalidSets = [
      entries.sublist(1),
      entries.sublist(0, 3),
      <ReadingImageEntry>[],
      [entries[0], entries[1], entries[2], entries[2]],
      [entries[0], entries[1], entries[3], entries[2]],
      [entries[1], entries[0], entries[2], entries[3]],
    ];
    for (final images in invalidSets) {
      expect(
        validate(
          manifest: ReadingImageManifest(
            identity: f.manifest.identity,
            images: images,
          ),
          expected: f.context,
        ).isLeft(),
        isTrue,
      );
    }
  });

  final invalidBindings = <String, List<ReadingImageSource> Function(Fixture)>{
    'aynı pageId ve farklı pageNumber': (f) => [
      ...f.sources.take(3),
      ReadingImageSource(
        binding: binding(
          ReadingImageRole.readingPage,
          pageId: f.sources[2].binding.pageId,
          pageNumber: 2,
        ),
        raster: raster(),
      ),
    ],
    'sırasız pageNumber': (f) => [
      f.sources[0],
      f.sources[1],
      f.sources[3],
      f.sources[2],
    ],
    'yanlış sayfa rolü': (f) => [
      ...f.sources.take(3),
      ReadingImageSource(
        binding: binding(ReadingImageRole.readingDetail),
        raster: raster(),
      ),
    ],
    'kapak ve detail farklı kaynak': (f) => [
      f.sources[0],
      ReadingImageSource(
        binding: f.sources[1].binding,
        raster: raster(sha: hash('9')),
      ),
      ...f.sources.skip(2),
    ],
  };
  for (final entry in invalidBindings.entries) {
    test(
      'F3 aynı bozuk descriptor ve context incompleteBindings: ${entry.key}',
      () {
        final f = Fixture();
        final sources = entry.value(f);
        final manifest = seal([
          for (final source in sources)
            ReadingImageEntry(
              source: source,
              renditions: [rendition(source, profile())],
            ),
        ]);
        final expected = ReadingImageManifestContext(
          identity: manifest.identity,
          sources: sources,
          profiles: f.context.profiles,
        );
        expect(
          validate(
            manifest: manifest,
            expected: expected,
          ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
          ReadingImageManifestFailure.incompleteBindings,
        );
      },
    );
  }

  for (final bytes in [1000000, 1000001]) {
    test(
      'F4 allowGrowth false/true aynı $bytes byte çıktısını reddeder/kabul eder',
      () {
        for (final allowGrowth in [false, true]) {
          final p = profile(allowGrowth: allowGrowth);
          final f = Fixture(profiles: [p], outputBytes: {p.fingerprint: bytes});
          final result = validate(manifest: f.manifest, expected: f.context);
          if (allowGrowth) {
            expect(result.isRight(), isTrue);
          } else {
            expect(
              result.swap().getOrElse(
                () => throw StateError('Red bekleniyordu'),
              ),
              ReadingImageManifestFailure.renditionMismatch,
            );
          }
        }
      },
    );
  }

  test('F6 context ve descriptor aynı zz dilini taşısa da invalidIdentity', () {
    final f = Fixture();
    final manifest = seal(
      f.manifest.images,
      publication: identity(language: 'zz'),
    );
    final expected = ReadingImageManifestContext(
      identity: manifest.identity,
      sources: f.sources,
      profiles: f.context.profiles,
    );
    expect(
      validate(
        manifest: manifest,
        expected: expected,
      ).swap().getOrElse(() => throw StateError('Red bekleniyordu')),
      ReadingImageManifestFailure.invalidIdentity,
    );
  });

  test('F6 backend canonical dil kümesinin tamamı kabul edilir', () {
    final f = Fixture();
    for (final language in ['tr', 'en', 'es', 'pt', 'de']) {
      final manifest = seal(
        f.manifest.images,
        publication: identity(language: language),
      );
      final expected = ReadingImageManifestContext(
        identity: manifest.identity,
        sources: f.sources,
        profiles: f.context.profiles,
      );
      expect(
        validate(manifest: manifest, expected: expected).isRight(),
        isTrue,
      );
    }
  });

  test('kaynak SHA, nesne sürümü ve bağ değişikliği reddedilir', () {
    final f = Fixture();
    for (final changed in [
      ReadingImageSource(
        binding: f.sources.last.binding,
        raster: raster(sha: hash('9')),
      ),
      ReadingImageSource(
        binding: f.sources.last.binding,
        raster: raster(generation: '124'),
      ),
      ReadingImageSource(
        binding: binding(
          ReadingImageRole.readingPage,
          pageNumber: 2,
          pageId: 99,
        ),
        raster: raster(),
      ),
    ]) {
      final entries = f.manifest.images.toList();
      entries[3] = ReadingImageEntry(
        source: changed,
        renditions: entries[3].renditions,
      );
      expect(
        validate(
          manifest: ReadingImageManifest(
            identity: f.manifest.identity,
            images: entries,
          ),
          expected: f.context,
        ).isLeft(),
        isTrue,
      );
    }
  });

  test('bozuk kaynak metadata beklenen bağlamda da olsa reddedilir', () {
    for (final source in [
      raster(width: 0),
      raster(height: -1),
      raster(bytes: 0),
      raster(sha: 'bad'),
      raster(mime: 'text/plain'),
      raster(provider: ''),
      raster(objectKey: ''),
      raster(generation: ''),
    ]) {
      final f = Fixture(source: source);
      expect(
        validate(manifest: f.manifest, expected: f.context).isLeft(),
        isTrue,
      );
    }
  });

  test(
    'yanlış/eksik profil, hash, MIME, byte, kaynak, kimlik ve floor-fit reddedilir',
    () {
      final f = Fixture();
      final source = f.sources.last;
      final p = profile();
      final badOutputs = [
        rendition(source, p, fingerprint: hash('8')),
        rendition(source, p, sourceSha: hash('8')),
        rendition(source, p, key: hash('8')),
        rendition(source, p, sha: 'bad'),
        rendition(source, p, bytes: 0),
        rendition(source, p, bytes: source.raster.byteSize),
        rendition(source, p, mime: 'image/jpeg'),
        rendition(source, p, provider: 'OTHER'),
        rendition(source, p, width: 1199),
        rendition(source, p, height: 799),
        rendition(source, p, width: 0),
      ];
      for (final outputs in [
        <ReadingImageRendition>[],
        ...badOutputs.map((r) => [r]),
        [
          f.manifest.images.last.renditions.first,
          f.manifest.images.last.renditions.first,
        ],
      ]) {
        final entries = f.manifest.images.toList();
        entries[3] = ReadingImageEntry(source: source, renditions: outputs);
        expect(
          validate(
            manifest: ReadingImageManifest(
              identity: f.manifest.identity,
              images: entries,
            ),
            expected: f.context,
          ).isLeft(),
          isTrue,
        );
      }
    },
  );

  test('geçersiz veya yinelenen profil ve yetersiz kaynak reddedilir', () {
    for (final profiles in [
      <ReadingImageProfile>[],
      [profile(width: 0)],
      [profile(fingerprint: 'bad')],
      [profile(), profile()],
      [profile(width: 2600)],
    ]) {
      final f = Fixture(profiles: profiles);
      expect(
        validate(manifest: f.manifest, expected: f.context).isLeft(),
        isTrue,
      );
    }
  });

  test('floor-fit oran yuvarlaması bağımsız tamsayı hesabına uyar', () {
    final f = Fixture(
      source: raster(width: 2000, height: 3000),
      profiles: [profile(width: 901, height: 901)],
    );
    expect(f.manifest.images.first.renditions.single.raster.width, 600);
    expect(f.manifest.images.first.renditions.single.raster.height, 901);
    expect(
      validate(manifest: f.manifest, expected: f.context).isRight(),
      isTrue,
    );
  });
}
