import 'dart:async';
import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/core/theme/library_theme.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_image_viewport.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/view/reading_image_comparison_view.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'reading_image_comparison_cubit_test.dart' show PendingRepository;
import 'reading_image_comparison_loaded_test.dart' show NativeRepository, png;
import 'comparison_test_support.dart' show media;
import 'fixture.dart';
import 'reading_image_selection_test.dart'
    show select, success, failure, requiredSize;

// Widget'ın fake saati yerine native Future'lar runAsync zone'unda sonlanır.
class _NativeWorkGate extends ReadingComparisonWorkGate {
  _NativeWorkGate(this.nativeZone);
  final Zone nativeZone;

  @override
  Future<void> run(Future<void> Function() work) =>
      nativeZone.run(() => super.run(work));
}

class _MeasuredCubit extends ReadingImageComparisonCubit {
  _MeasuredCubit(PendingRepository repo)
    : super(
        LoadReadingImageComparison(repo),
        repo,
        const ReadingImageComparisonConfiguration.geometry(),
        ReadingComparisonWorkGate(),
        const ReadingComparisonClock(),
      );
  ({Size slot, double dpr, ReadingImageFit fit})? measured;

  @override
  void select({
    required int id,
    required int entryIndex,
    required bool canonicalSource,
    required double width,
    required double height,
    required double dpr,
    required ReadingImageFit fit,
  }) {
    measured = (slot: Size(width, height), dpr: dpr, fit: fit);
    super.select(
      id: id,
      entryIndex: entryIndex,
      canonicalSource: canonicalSource,
      width: width,
      height: height,
      dpr: dpr,
      fit: fit,
    );
  }
}

void main() {
  testWidgets(
    'yüklü QA view kapak/sayfa ve iki kolda ortak rect ve selector fit değerini rastera taşır',
    (tester) async {
      tester.view.physicalSize = const Size(487.5, 1055);
      tester.view.devicePixelRatio = 1.25;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final config = media(Uri.parse('http://127.0.0.1:1'));
      late NativeRepository repo;
      late Fixture fixture;
      late ReadingImageComparisonCubit cubit;
      await tester.runAsync(() async {
        repo = NativeRepository(await png(1200), await png(1000));
        final output = profile(width: 1000, height: 1000);
        fixture = Fixture(
          source: raster(
            width: 1200,
            height: 1200,
            bytes: repo.sourceBytes.length,
            sha: sha256.convert(repo.sourceBytes).toString(),
          ),
          profiles: [output],
        );
        final manifest = seal([
          for (final source in fixture.sources)
            ReadingImageEntry(
              source: source,
              renditions: [
                rendition(
                  source,
                  output,
                  bytes: repo.outputBytes.length,
                  sha: sha256.convert(repo.outputBytes).toString(),
                ),
              ],
            ),
        ]);
        repo.current = ReadingImageComparison(
          manifest,
          ReadingImageManifestContext(
            identity: manifest.identity,
            sources: fixture.sources,
            profiles: fixture.context.profiles,
          ),
          1,
          repo.leaseMillis,
        );
        cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          _NativeWorkGate(Zone.current),
          const ReadingComparisonClock(),
        );
      });
      Future<void> waitLoaded() async {
        await tester.pump();
        for (
          var attempt = 0;
          attempt < 50 && cubit.state.status == 'loading';
          attempt++
        ) {
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump();
        }
        expect(cubit.state.status, 'loaded');
        await tester.pump();
      }

      try {
        await tester.pumpWidget(
          MaterialApp(
            theme: AppTypography.theme(),
            home: BlocProvider<ReadingImageComparisonCubit>.value(
              value: cubit,
              child: const ReadingImageComparisonView(contentId: 71),
            ),
          ),
        );
        await waitLoaded();
        var expectedByteCalls = 1;
        for (final choice in [
          (label: 'Kapak', index: 1),
          (label: 'Sayfa 1', index: 2),
          (label: 'Sayfa 2', index: 3),
        ]) {
          await tester.tap(find.text(choice.label));
          await waitLoaded();
          expectedByteCalls++;
          final viewport = find.byType(ReadingImageViewport);
          final rect = tester.getRect(viewport);
          final expectedFit = choice.index == 1 ? BoxFit.cover : BoxFit.contain;
          for (final canonical in [false, true]) {
            if (canonical) {
              await tester.tap(find.text('Canonical PNG kaynak göster'));
              await waitLoaded();
              expectedByteCalls++;
            }
            final widget = tester.widget<ReadingImageComparisonRaster>(
              find.byType(ReadingImageComparisonRaster),
            );
            expect(widget.fit, expectedFit);
            expect(
              tester.widget<ReadingImageViewport>(viewport).fit,
              expectedFit,
            );
            expect(
              tester.getRect(find.byType(ReadingImageComparisonRaster)),
              rect,
            );
            expect(
              tester.getRect(
                find.byKey(const ValueKey('reading-comparison-slot')),
              ),
              rect,
            );
            expect(cubit.state.entryIndex, choice.index);
            expect(cubit.state.canonicalSource, canonical);
            expect(identical(widget.raster, cubit.state.raster), true);
            final binding =
                repo.current.manifest.images[choice.index].source.binding;
            final expected = success(
              const SelectReadingImageRendition()(
                manifest:
                    const ValidateReadingImageManifest()(
                      manifest: repo.current.manifest,
                      expected: repo.current.context,
                    ).fold(
                      (failure) => throw StateError(failure.name),
                      (value) => value,
                    ),
                binding: binding,
                acceptedProfileFingerprints: repo
                    .current
                    .context
                    .profiles[binding.role]!
                    .map((p) => p.fingerprint)
                    .toSet(),
                logicalWidth: rect.width,
                logicalHeight: rect.height,
                devicePixelRatio: 1.25,
                fit: expectedFit == BoxFit.cover
                    ? ReadingImageFit.cover
                    : ReadingImageFit.contain,
                maxDecodePixels: config.limits!.maxDecodePixels,
              ),
            );
            final actual = cubit.state.geometry!;
            expect(
              (actual.physicalWidth, actual.physicalHeight),
              (
                expected.geometry.physicalWidth,
                expected.geometry.physicalHeight,
              ),
            );
            expect(
              (actual.requiredWidth, actual.requiredHeight),
              (
                expected.geometry.requiredWidth,
                expected.geometry.requiredHeight,
              ),
            );
            expect(
              (actual.decodeWidth, actual.decodeHeight),
              (expected.geometry.decodeWidth, expected.geometry.decodeHeight),
            );
            expect(
              (widget.raster.image!.width, widget.raster.image!.height),
              canonical
                  ? (1200, 1200)
                  : (actual.decodeWidth, actual.decodeHeight),
            );
            expect(repo.byteCalls, expectedByteCalls);
          }
          await tester.tap(find.text('Rendition göster'));
          await waitLoaded();
          expectedByteCalls++;
          expect(
            tester
                .widget<ReadingImageComparisonRaster>(
                  find.byType(ReadingImageComparisonRaster),
                )
                .fit,
            expectedFit,
          );
          expect(
            tester.getRect(find.byType(ReadingImageComparisonRaster)),
            rect,
          );
        }
        expect(tester.takeException(), isNull);
      } finally {
        await tester.pumpWidget(const SizedBox());
        await tester.runAsync(() async {
          await cubit.close().timeout(const Duration(seconds: 3));
          await config.account!.credentials.close();
        });
      }
      expect(ReadingComparisonRaster.activeImages, 0);
      expect(ReadingComparisonRaster.activeBuffers, 0);
      expect(ReadingComparisonRaster.activeDescriptors, 0);
      expect(ReadingComparisonRaster.activeCodecs, 0);
    },
  );

  for (final scale in [1.0, 1.5]) {
    testWidgets('kol değişimi sayfa alanını korur; metin ölçeği $scale', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.625;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final repo = PendingRepository();
      final cubit = _MeasuredCubit(repo);
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTypography.theme(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: TextScaler.linear(scale)),
            child: child!,
          ),
          home: BlocProvider<ReadingImageComparisonCubit>.value(
            value: cubit,
            child: const ReadingImageComparisonView(contentId: 0),
          ),
        ),
      );
      await tester.pumpAndSettle();
      final slot = find.byKey(const ValueKey('reading-comparison-slot'));
      for (final page in ['Kapak', 'Sayfa 1', 'Sayfa 2']) {
        await tester.tap(find.text(page));
        await tester.pumpAndSettle();
        final before = tester.getRect(slot);
        expect(cubit.measured!.slot.width, closeTo(before.width, .000001));
        expect(cubit.measured!.slot.height, closeTo(before.height, .000001));
        expect(cubit.measured!.dpr, 2.625);
        expect(
          cubit.measured!.fit,
          page == 'Kapak' ? ReadingImageFit.cover : ReadingImageFit.contain,
        );
        expect(tester.getRect(find.byType(ReadingImageViewport)), before);
        final fixture = Fixture(profiles: [profile(width: 2400, height: 1600)]);
        final g = cubit.measured!;
        final actual = success(
          select(
            fixture,
            width: g.slot.width,
            height: g.slot.height,
            dpr: g.dpr,
            fit: g.fit,
          ),
        );
        expect(
          (actual.geometry.requiredWidth, actual.geometry.requiredHeight),
          requiredSize(
            2400,
            1600,
            (g.slot.width * g.dpr).ceil(),
            (g.slot.height * g.dpr).ceil(),
            g.fit,
          ),
        );
        final insufficient = select(
          Fixture(profiles: [profile(width: 100, height: 100)]),
          width: g.slot.width,
          height: g.slot.height,
          dpr: g.dpr,
          fit: g.fit,
        );
        expect(
          failure(insufficient),
          ReadingImageSelectionFailure.renditionShortage,
        );
        await tester.tap(find.text('Canonical PNG kaynak göster'));
        await tester.pumpAndSettle();
        expect(find.text('Rendition göster'), findsOneWidget);
        expect(tester.getRect(slot), before, reason: '$page kaynak kolu');
        await tester.tap(find.text('Rendition göster'));
        await tester.pumpAndSettle();
        expect(tester.getRect(slot), before, reason: '$page rendition kolu');
      }
      expect(repo.calls, 0);
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
    });
  }
  testWidgets(
    'QA pencere, DPR ve metin ölçeği değişince yeni rect seçiciye gider',
    (tester) async {
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final repo = PendingRepository();
      final cubit = _MeasuredCubit(repo);
      for (final sample in [
        (size: const Size(390, 844), dpr: 1.25, scale: 1.0),
        (size: const Size(800, 1280), dpr: 2.0, scale: 1.5),
        (size: const Size(390, 844), dpr: 3.0, scale: 1.3),
      ]) {
        tester.view.physicalSize = sample.size * sample.dpr;
        tester.view.devicePixelRatio = sample.dpr;
        await tester.pumpWidget(
          MaterialApp(
            theme: AppTypography.theme(),
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: TextScaler.linear(sample.scale)),
              child: child!,
            ),
            home: BlocProvider<ReadingImageComparisonCubit>.value(
              value: cubit,
              child: const ReadingImageComparisonView(contentId: 0),
            ),
          ),
        );
        await tester.pumpAndSettle();
        for (final choice in ['Kapak', 'Sayfa 1']) {
          await tester.tap(find.text(choice));
          await tester.pumpAndSettle();
          final rect = tester.getRect(
            find.byKey(const ValueKey('reading-comparison-slot')),
          );
          expect(cubit.measured!.slot.width, closeTo(rect.width, .000001));
          expect(cubit.measured!.slot.height, closeTo(rect.height, .000001));
          expect(cubit.measured!.dpr, sample.dpr);
          expect(
            cubit.measured!.fit,
            choice == 'Kapak' ? ReadingImageFit.cover : ReadingImageFit.contain,
          );
          if (choice == 'Kapak') {
            expect(
              rect.size.aspectRatio,
              closeTo(sample.size.shortestSide >= 600 ? 1.1 : .9, .0001),
            );
          }
        }
        expect(tester.takeException(), isNull);
      }
      expect(repo.calls, 0);
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
    },
  );

  testWidgets('ağsız geometri aynı slotta kaynak ve rendition kolunu değiştirir', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final repo = PendingRepository();
    final cubit = ReadingImageComparisonCubit(
      LoadReadingImageComparison(repo),
      repo,
      const ReadingImageComparisonConfiguration.geometry(),
      ReadingComparisonWorkGate(),
      const ReadingComparisonClock(),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider<ReadingImageComparisonCubit>.value(
          value: cubit,
          child: const ReadingImageComparisonView(contentId: 0),
        ),
      ),
    );
    await tester.pump();
    expect(find.textContaining('Ağsız geometri:'), findsOneWidget);
    final before = tester.getSize(
      find.byKey(const ValueKey('reading-comparison-slot')),
    );
    expect(before.width, 390);
    expect(before.height, closeTo(390 / .9, .01));
    debugPrint(
      'P4a2 synthetic cover: ${before.width} x ${before.height}, DPR 3, cover, center',
    );
    await tester.tap(find.text('Canonical PNG kaynak göster'));
    await tester.pump();
    await tester.pump();
    expect(
      tester.getSize(find.byKey(const ValueKey('reading-comparison-slot'))),
      before,
    );
    expect(repo.calls, 0);
    await tester.tap(find.text('Sayfa 1'));
    await tester.pump();
    await tester.pump();
    expect(find.textContaining('contain'), findsOneWidget);
    debugPrint(
      'P4a2 synthetic page: ${tester.getSize(find.byKey(const ValueKey('reading-comparison-slot')))}, DPR 3, contain, center',
    );
    await tester.pumpWidget(const SizedBox());
    await cubit.close();
  });
}
