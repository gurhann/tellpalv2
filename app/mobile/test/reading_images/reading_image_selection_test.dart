import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';

import 'fixture.dart';

Either<ReadingImageSelectionFailure, ReadingImageSelection> select(
  Fixture f, {
  double width = 500,
  double height = 300,
  double dpr = 1,
  ReadingImageFit fit = ReadingImageFit.contain,
  int budget = 10000000,
  Set<String>? accepted,
  ReadingImageBinding? target,
}) => const SelectReadingImageRendition()(
  manifest: f.validated,
  binding: target ?? f.sources.last.binding,
  acceptedProfileFingerprints:
      accepted ??
      f.context.profiles.values.first.map((p) => p.fingerprint).toSet(),
  logicalWidth: width,
  logicalHeight: height,
  devicePixelRatio: dpr,
  fit: fit,
  maxDecodePixels: budget,
);

ReadingImageSelection success(
  Either<ReadingImageSelectionFailure, ReadingImageSelection> result,
) => result.fold((f) => throw StateError('$f'), (selection) => selection);

ReadingImageSelectionFailure failure(
  Either<ReadingImageSelectionFailure, ReadingImageSelection> result,
) => result.fold((f) => f, (_) => throw StateError('Red bekleniyordu'));

// Geometri oracle'ı ölçeği double ile çarpmaz; iki ekseni tamsayı kesriyle hesaplar.
(int, int) requiredSize(int sw, int sh, int pw, int ph, ReadingImageFit fit) {
  final widthIsSmaller = pw * sh <= ph * sw;
  final useWidth = fit == ReadingImageFit.contain
      ? widthIsSmaller
      : !widthIsSmaller;
  final numerator = useWidth ? pw : ph;
  final denominator = useWidth ? sw : sh;
  return (
    (sw * numerator + denominator - 1) ~/ denominator,
    (sh * numerator + denominator - 1) ~/ denominator,
  );
}

void main() {
  for (final dimensions in [
    (source: (1005, 670), output: (375, 250), slot: (375.0, 250.0)),
    (source: (2400, 1600), output: (204, 136), slot: (204.0, 136.0)),
    (source: (11, 77), output: (9, 63), slot: (9.0, 77.0)),
  ]) {
    test(
      'F1 exact ihtiyaç sınırı ${dimensions.source} -> ${dimensions.output}',
      () {
        final f = Fixture(
          source: raster(
            width: dimensions.source.$1,
            height: dimensions.source.$2,
          ),
          profiles: [
            profile(width: dimensions.output.$1, height: dimensions.output.$2),
          ],
        );
        for (final fit in ReadingImageFit.values) {
          final wanted = requiredSize(
            dimensions.source.$1,
            dimensions.source.$2,
            dimensions.slot.$1.toInt(),
            dimensions.slot.$2.toInt(),
            fit,
          );
          final result = select(
            f,
            width: dimensions.slot.$1,
            height: dimensions.slot.$2,
            fit: fit,
          );
          if (wanted.$1 > dimensions.output.$1 ||
              wanted.$2 > dimensions.output.$2) {
            expect(
              failure(result),
              ReadingImageSelectionFailure.renditionShortage,
            );
            continue;
          }
          final actual = success(result);
          expect((
            actual.geometry.requiredWidth,
            actual.geometry.requiredHeight,
          ), wanted);
          expect((
            actual.geometry.decodeWidth,
            actual.geometry.decodeHeight,
          ), dimensions.output);
        }
      },
    );
  }

  test('F1 BigInt çapraz çarpımları int64 taşmasını önler', () {
    final f = Fixture(
      source: raster(width: 8000000000, height: 6000000000),
      profiles: [
        profile(width: 6000000000, height: 4500000000, fingerprint: hash('2')),
        profile(width: 4000000000, height: 3000000000, fingerprint: hash('1')),
      ],
    );
    final result = success(
      select(
        f,
        width: 4000000000,
        height: 1000000000,
        budget: 2000000000000000000,
      ),
    );
    expect(
      (result.geometry.requiredWidth, result.geometry.requiredHeight),
      (1333333334, 1000000000),
    );
    expect(result.rendition.profileFingerprint, hash('1'));
    expect(
      (result.geometry.decodeWidth, result.geometry.decodeHeight),
      (1333333334, 1000000001),
    );
  });

  test('F2 exact decode 218x218 tam bütçede kabul edilir', () {
    final f = Fixture(
      source: raster(width: 2000, height: 2000),
      profiles: [profile(width: 1600, height: 1600)],
    );
    final result = success(select(f, width: 218, height: 218, budget: 47524));
    expect(
      (result.geometry.decodeWidth, result.geometry.decodeHeight),
      (218, 218),
    );
    expect(
      (result.geometry.requiredWidth, result.geometry.requiredHeight),
      (218, 218),
    );
  });

  test('F2 exact decode 218x218 bütçeden tek piksel fazla ise reddedilir', () {
    final f = Fixture(
      source: raster(width: 2000, height: 2000),
      profiles: [profile(width: 1600, height: 1600)],
    );
    expect(
      failure(select(f, width: 218, height: 218, budget: 47523)),
      ReadingImageSelectionFailure.decodeResourceExceeded,
    );
  });

  test('en küçük alan seçilir; byte ve kimlik eşitlik bozucudur', () {
    final profiles = [
      profile(width: 600, height: 400, fingerprint: hash('1')),
      profile(fingerprint: hash('2')),
      profile(width: 1800, height: 1200, fingerprint: hash('3')),
    ];
    final f = Fixture(profiles: profiles.reversed.toList());
    expect(success(select(f)).rendition.profileFingerprint, hash('1'));
    expect(
      success(select(f, width: 900, height: 600)).rendition.profileFingerprint,
      hash('2'),
    );
    final equalArea = Fixture(
      profiles: [
        profile(fingerprint: hash('1')),
        profile(fingerprint: hash('2')),
      ],
      outputBytes: {hash('1'): 200000, hash('2'): 90000},
    );
    expect(success(select(equalArea)).rendition.profileFingerprint, hash('2'));
    final tied = Fixture(
      profiles: [
        profile(fingerprint: hash('1')),
        profile(fingerprint: hash('2')),
      ],
    );
    final keys =
        tied.manifest.images.last.renditions.map((r) => r.renditionKey).toList()
          ..sort();
    expect(success(select(tied)).rendition.renditionKey, keys.first);
  });

  test('yalnız açıkça kabul edilen profil ve tam bağ seçilebilir', () {
    final f = Fixture(
      profiles: [
        profile(width: 600, height: 400, fingerprint: hash('1')),
        profile(),
      ],
    );
    expect(
      success(select(f, accepted: {hash('d')})).rendition.profileFingerprint,
      hash('d'),
    );
    expect(
      failure(select(f, accepted: {})),
      ReadingImageSelectionFailure.renditionShortage,
    );
    expect(
      failure(select(f, accepted: {hash('f')})),
      ReadingImageSelectionFailure.renditionShortage,
    );
    expect(
      failure(
        select(f, target: binding(ReadingImageRole.readingPage, pageId: 999)),
      ),
      ReadingImageSelectionFailure.bindingMismatch,
    );
  });

  test('cover ve contain tall/wide kaynaklarda iki eksenli ihtiyacı korur', () {
    for (final dimensions in [(2400, 1600), (1600, 2400)]) {
      final f = Fixture(
        source: raster(width: dimensions.$1, height: dimensions.$2),
        profiles: [profile(width: 1200, height: 1200)],
      );
      for (final fit in ReadingImageFit.values) {
        final selection = success(select(f, width: 400, height: 300, fit: fit));
        final wanted = requiredSize(
          dimensions.$1,
          dimensions.$2,
          400,
          300,
          fit,
        );
        expect((
          selection.geometry.requiredWidth,
          selection.geometry.requiredHeight,
        ), wanted);
        expect(selection.geometry.decodeWidth, greaterThanOrEqualTo(wanted.$1));
        expect(
          selection.geometry.decodeHeight,
          greaterThanOrEqualTo(wanted.$2),
        );
        expect(
          selection.geometry.decodeWidth,
          lessThanOrEqualTo(selection.rendition.raster.width),
        );
        expect(
          selection.geometry.decodeHeight,
          lessThanOrEqualTo(selection.rendition.raster.height),
        );
      }
    }
  });

  test('fractional DPR önce fiziksel slotu yukarı yuvarlar', () {
    final result = success(
      select(Fixture(), width: 301.2, height: 200.2, dpr: 1.25),
    );
    expect(
      (result.geometry.physicalWidth, result.geometry.physicalHeight),
      (377, 251),
    );
    expect((
      result.geometry.requiredWidth,
      result.geometry.requiredHeight,
    ), requiredSize(2400, 1600, 377, 251, ReadingImageFit.contain));
  });

  test('pencere ve DPR değiştiğinde ihtiyaç yeniden hesaplanır', () {
    final f = Fixture(
      profiles: [
        profile(width: 600, height: 400, fingerprint: hash('1')),
        profile(),
      ],
    );
    final small = success(select(f, width: 300, height: 200));
    final resized = success(select(f, width: 800, height: 600));
    final density = success(select(f, width: 300, height: 200, dpr: 3));
    expect(small.rendition.profileFingerprint, hash('1'));
    expect(resized.rendition.profileFingerprint, hash('d'));
    expect(density.rendition.profileFingerprint, hash('d'));
    expect(density.geometry.requiredWidth, 900);
    expect(resized.geometry.requiredWidth, 800);
  });

  test('tek eksen eksiği, kaynak upscale ve original fallback reddedilir', () {
    final tall = Fixture(
      source: raster(width: 1600, height: 2400),
      profiles: [profile(width: 1200, height: 1200)],
    );
    expect(
      failure(
        select(tall, width: 801, height: 1200, fit: ReadingImageFit.cover),
      ),
      ReadingImageSelectionFailure.renditionShortage,
    );
    final wide = Fixture();
    expect(
      failure(
        select(wide, width: 1200, height: 801, fit: ReadingImageFit.cover),
      ),
      ReadingImageSelectionFailure.renditionShortage,
    );
    expect(
      failure(
        select(wide, width: 2401, height: 1600, fit: ReadingImageFit.cover),
      ),
      ReadingImageSelectionFailure.sourceShortage,
    );
    expect(
      failure(select(wide, width: 2400, height: 1600)),
      ReadingImageSelectionFailure.renditionShortage,
    );
  });

  test('decode açık piksel bütçesine uyar; ihtiyaç bütçeye kırpılmaz', () {
    final f = Fixture();
    final result = success(select(f, width: 600, height: 400, budget: 240000));
    expect(
      (result.geometry.decodeWidth, result.geometry.decodeHeight),
      (600, 400),
    );
    expect(
      failure(select(f, width: 600, height: 400, budget: 239999)),
      ReadingImageSelectionFailure.decodeResourceExceeded,
    );
    expect(
      failure(select(f, budget: 0)),
      ReadingImageSelectionFailure.decodeResourceExceeded,
    );
    expect(
      failure(select(f, budget: -1)),
      ReadingImageSelectionFailure.decodeResourceExceeded,
    );
  });

  test('sonlu olmayan ve pozitif olmayan geometri tipli hatadır', () {
    final f = Fixture();
    for (final value in [
      0.0,
      -1.0,
      double.nan,
      double.infinity,
      double.negativeInfinity,
    ]) {
      expect(
        failure(select(f, width: value)),
        ReadingImageSelectionFailure.invalidGeometry,
      );
      expect(
        failure(select(f, height: value)),
        ReadingImageSelectionFailure.invalidGeometry,
      );
      expect(
        failure(select(f, dpr: value)),
        ReadingImageSelectionFailure.invalidGeometry,
      );
    }
    expect(
      failure(select(f, width: 1e308, dpr: 1e308)),
      ReadingImageSelectionFailure.invalidGeometry,
    );
  });

  test(
    'küçük matris bağımsız geometri ve exhaustive aday oracle ile eşleşir',
    () {
      for (final sourceSize in [(2400, 1600), (1600, 2400)]) {
        final f = Fixture(
          source: raster(width: sourceSize.$1, height: sourceSize.$2),
          profiles: [
            profile(width: 600, height: 600, fingerprint: hash('1')),
            profile(width: 1200, height: 1200, fingerprint: hash('2')),
            profile(width: 1600, height: 1600, fingerprint: hash('3')),
          ],
        );
        for (final slot in [
          (200.0, 300.0),
          (301.2, 200.2),
          (500.0, 400.0),
          (1400.0, 1000.0),
        ]) {
          for (final dpr in [1.0, 1.25, 2.0]) {
            for (final fit in ReadingImageFit.values) {
              final wanted = requiredSize(
                sourceSize.$1,
                sourceSize.$2,
                (slot.$1 * dpr).ceil(),
                (slot.$2 * dpr).ceil(),
                fit,
              );
              final result = select(
                f,
                width: slot.$1,
                height: slot.$2,
                dpr: dpr,
                fit: fit,
              );
              if (wanted.$1 > sourceSize.$1 || wanted.$2 > sourceSize.$2) {
                expect(
                  failure(result),
                  ReadingImageSelectionFailure.sourceShortage,
                );
                continue;
              }
              final all = f.manifest.images.last.renditions;
              ReadingImageRendition? expected;
              for (final candidate in all) {
                if (candidate.raster.width < wanted.$1 ||
                    candidate.raster.height < wanted.$2) {
                  continue;
                }
                final smaller =
                    expected == null ||
                    candidate.raster.width * candidate.raster.height <
                        expected.raster.width * expected.raster.height;
                if (smaller) expected = candidate;
              }
              if (expected == null) {
                expect(
                  failure(result),
                  ReadingImageSelectionFailure.renditionShortage,
                );
                continue;
              }
              final actual = success(result);
              expect(actual.rendition, expected);
              expect((
                actual.geometry.requiredWidth,
                actual.geometry.requiredHeight,
              ), wanted);
              final ratioError =
                  (actual.geometry.decodeWidth * expected.raster.height -
                          actual.geometry.decodeHeight * expected.raster.width)
                      .abs();
              expect(
                ratioError,
                lessThanOrEqualTo(
                  expected.raster.width + expected.raster.height,
                ),
              );
            }
          }
        }
      }
    },
  );
}
