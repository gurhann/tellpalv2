import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/repository/reading_image_comparison_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'fixture.dart';
import 'comparison_test_support.dart';

Future<Uint8List> png(int size) async {
  final recorder = ui.PictureRecorder();
  final canvas = ui.Canvas(recorder);
  canvas.drawColor(const ui.Color(0xFFFFFFFF), ui.BlendMode.src);
  final picture = recorder.endRecording();
  final image = await picture.toImage(size, size);
  final data = (await image.toByteData(
    format: ui.ImageByteFormat.png,
  ))!.buffer.asUint8List();
  image.dispose();
  picture.dispose();
  return data;
}

class NativeRepository implements ReadingImageComparisonRepository {
  NativeRepository(this.sourceBytes, this.outputBytes) {
    current = admission();
  }
  final Uint8List sourceBytes, outputBytes;
  late ReadingImageComparison current;
  int byteCalls = 0;
  int leaseMillis = 1000;
  void Function()? beforeBytes;
  ReadingImageComparison admission({int generation = 1, String? revision}) {
    final p = profile(width: 2, height: 2, allowGrowth: true);
    final f = Fixture(
      source: raster(
        width: 4,
        height: 4,
        bytes: sourceBytes.length,
        sha: sha256.convert(sourceBytes).toString(),
      ),
      profiles: [p],
    );
    final images = [
      for (final s in f.sources)
        ReadingImageEntry(
          source: s,
          renditions: [
            rendition(
              s,
              p,
              bytes: outputBytes.length,
              sha: sha256.convert(outputBytes).toString(),
            ),
          ],
        ),
    ];
    final m = seal(images, publication: identity(revision: revision));
    return ReadingImageComparison(
      m,
      ReadingImageManifestContext(
        identity: m.identity,
        sources: f.sources,
        profiles: f.context.profiles,
      ),
      generation,
      leaseMillis,
    );
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation c,
  ) async => Right(current);
  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison value,
    ReadingComparisonCancellation c,
  ) async => Right(current);
  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding b,
    ReadingImageRendition r,
    bool canonical,
    ReadingComparisonCancellation c,
  ) async {
    beforeBytes?.call();
    byteCalls++;
    return Right(canonical ? sourceBytes : outputBytes);
  }
}

Future<void> loaded(
  ReadingImageComparisonCubit c, {
  bool canonical = true,
  int id = 71,
  double size = 1,
}) async {
  final next = c.stream.firstWhere(
    (s) => s.status == 'loaded' || s.status == 'error',
  );
  c.select(
    id: id,
    entryIndex: 3,
    canonicalSource: canonical,
    width: size,
    height: size,
    dpr: 1,
    fit: ReadingImageFit.contain,
  );
  await next.timeout(const Duration(seconds: 3));
}

void main() {
  tearDown(() {
    expect(ReadingComparisonRaster.reservedSlots, 0);
    expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.zero);
    expect(ReadingComparisonRaster.activeImages, 0);
  });
  testWidgets(
    'P4b2 aynı gate iki Cubit resident owner varken ikinci native çağrı başlamaz',
    (tester) async {
      final config = media(Uri.parse('http://127.0.0.1:1'));
      await tester.runAsync(() async {
        final repo = NativeRepository(await png(4), await png(2));
        final gate = ReadingComparisonWorkGate();
        ReadingImageComparisonCubit owner() => ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          gate,
          const ReadingComparisonClock(),
        );
        final first = owner(), second = owner();
        try {
          await loaded(first);
          final image = first.state.raster!.image!;
          expect(ReadingComparisonRaster.reservedSlots, 1);
          expect(ReadingComparisonRaster.reservedDecodedBytes, BigInt.from(64));
          final before = ReadingComparisonRaster.bufferCreationAttempts;
          await loaded(second);
          expect(second.state.error, 'media_refused');
          expect(repo.byteCalls, 2);
          expect(image.debugDisposed, isFalse);
          expect(ReadingComparisonRaster.bufferCreationAttempts - before, 0);
          first.retire('released');
          await loaded(second);
          expect(second.state.status, 'loaded');
          expect(ReadingComparisonRaster.reservedSlots, 1);
        } finally {
          await first.close();
          await second.close();
          await config.account!.credentials.close();
        }
      });
    },
  );
  testWidgets(
    'F4 büyük server lease değeri Timer öncesi ve byte çağrısından önce reddedilir',
    (tester) async {
      final config = media(Uri.parse('http://127.0.0.1:1'));
      await tester.runAsync(() async {
        final repo = NativeRepository(await png(4), await png(2))
          ..leaseMillis = 0x80000000;
        repo.current = repo.admission();
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          ReadingComparisonWorkGate(),
          const ReadingComparisonClock(),
        );
        await loaded(cubit);
        expect(cubit.state.status, 'error');
        expect(repo.byteCalls, 0);
        await cubit.close();
        await config.account!.credentials.close();
      });
    },
  );
  testWidgets(
    'F2 F6 F7 F8 F9 gerçek PNG loaded source rendition resize ve klonsuz native widget sahipliği',
    (tester) async {
      final config = media(Uri.parse('http://127.0.0.1:1'));
      late NativeRepository repo;
      late ReadingImageComparisonCubit c;
      await tester.runAsync(() async {
        repo = NativeRepository(await png(4), await png(2));
        c = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          ReadingComparisonWorkGate(),
          const ReadingComparisonClock(),
        );
        await loaded(c);
        expect(c.state.status, 'loaded');
        expect(c.state.raster!.image!.width, 4);
      });
      final source = c.state.raster!, sourceImage = c.state.raster!.image!;
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: ReadingImageComparisonRaster(
            raster: source,
            fit: BoxFit.contain,
          ),
        ),
      );
      expect(find.byType(RawImage), findsNothing);
      expect(sourceImage.debugGetOpenHandleStackTraces()!.length, 1);
      repo.beforeBytes = () {
        expect(sourceImage.debugDisposed, isTrue);
        expect(sourceImage.debugGetOpenHandleStackTraces(), isEmpty);
      };
      await tester.runAsync(() async {
        await loaded(c, canonical: false);
        expect(c.state.status, 'loaded');
        expect(c.state.raster!.image!.width, 1);
      });
      final output = c.state.raster!, outputImage = c.state.raster!.image!;
      await tester.pump();
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: ReadingImageComparisonRaster(
            raster: output,
            fit: BoxFit.contain,
          ),
        ),
      );
      expect(outputImage.debugGetOpenHandleStackTraces()!.length, 1);
      repo.beforeBytes = () {
        expect(outputImage.debugDisposed, isTrue);
        expect(outputImage.debugGetOpenHandleStackTraces(), isEmpty);
      };
      await tester.runAsync(() async {
        await loaded(c, canonical: false, size: 2);
        expect(c.state.raster!.image!.width, 2);
      });
      final resized = c.state.raster!.image!;
      c.retire('lease_expired');
      expect(c.state.entryIndex, 3);
      expect(c.state.canonicalSource, isFalse);
      expect(resized.debugDisposed, isTrue);
      repo.beforeBytes = null;
      await tester.runAsync(() async {
        c.retry();
        await loaded(c, canonical: false);
        expect(c.state.status, 'loaded');
        final calls = repo.byteCalls;
        await loaded(c, canonical: false, id: 72);
        expect(c.state.status, 'error');
        expect(repo.byteCalls, calls);
        await c.close();
        await config.account!.credentials.close();
      });
      await tester.pumpWidget(const SizedBox());
      expect(tester.takeException(), isNull);
      expect(ReadingComparisonRaster.activeImages, 0);
    },
  );
  for (final change in ['identity', 'generation']) {
    testWidgets(
      'F2 anchor $change değişimi background retry ve kol değişiminde byte öncesi reddedilir',
      (tester) async {
        final config = media(Uri.parse('http://127.0.0.1:1'));
        await tester.runAsync(() async {
          final repo = NativeRepository(await png(4), await png(2));
          final c = ReadingImageComparisonCubit(
            LoadReadingImageComparison(repo),
            repo,
            config,
            ReadingComparisonWorkGate(),
            const ReadingComparisonClock(),
          );
          await loaded(c);
          expect(c.state.status, 'loaded');
          final calls = repo.byteCalls;
          c.visibility(false);
          c.visibility(true);
          expect(c.state.entryIndex, 3);
          expect(c.state.canonicalSource, isTrue);
          repo.current = repo.admission(
            generation: change == 'generation' ? 2 : 1,
            revision: change == 'identity' ? hash('1') : null,
          );
          c.retry();
          c.changeChoice(3, false);
          await loaded(c, canonical: false);
          expect(c.state.status, 'error');
          expect(repo.byteCalls, calls);
          expect(c.state.raster, isNull);
          await c.close();
          await config.account!.credentials.close();
        });
      },
    );
  }
}
