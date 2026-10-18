import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_image_viewport.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'reading_images/fixture.dart';
import 'reading_images/reading_image_selection_test.dart'
    show select, success, failure, requiredSize;

void main() {
  for (final detail in [false, true]) {
    testWidgets('son rect/DPR/fit yeniden ölçülür; detail=$detail', (
      tester,
    ) async {
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      ReadingImageViewportGeometry? measured;
      const slotKey = ValueKey('measured-slot');
      for (final sample in [
        (size: const Size(390, 844), dpr: 1.0, scale: 1.0),
        (size: const Size(844, 390), dpr: 1.25, scale: 1.3),
        (size: const Size(800, 1280), dpr: 2.0, scale: 1.5),
        (size: const Size(390, 844), dpr: 3.0, scale: 1.0),
      ]) {
        tester.view.physicalSize = sample.size * sample.dpr;
        tester.view.devicePixelRatio = sample.dpr;
        Widget buildSlot(BuildContext context, ReadingImageViewportGeometry g) {
          measured = g;
          return const SizedBox(key: slotKey);
        }

        final content = Column(
          children: [
            const Text('Dış kontrol'),
            if (detail)
              ReadingImageViewport.detail(builder: buildSlot)
            else
              Expanded(
                child: ReadingImageViewport(
                  fit: BoxFit.contain,
                  builder: buildSlot,
                ),
              ),
          ],
        );
        await tester.pumpWidget(
          MaterialApp(
            home: MediaQuery(
              data: MediaQueryData(
                size: sample.size,
                devicePixelRatio: sample.dpr,
                textScaler: TextScaler.linear(sample.scale),
                padding: const EdgeInsets.only(top: 24, bottom: 16),
                viewInsets: const EdgeInsets.only(bottom: 20),
              ),
              child: Scaffold(
                body: SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: detail
                        ? SingleChildScrollView(child: content)
                        : content,
                  ),
                ),
              ),
            ),
          ),
        );
        final rect = tester.getRect(find.byKey(slotKey));
        expect(measured!.slot.width, closeTo(rect.width, .000001));
        expect(measured!.slot.height, closeTo(rect.height, .000001));
        expect(measured!.dpr, sample.dpr);
        expect(measured!.fit, detail ? BoxFit.cover : BoxFit.contain);
        expect(rect.width, sample.size.width - 24);
        if (detail) {
          expect(
            rect.width / rect.height,
            closeTo(sample.size.shortestSide >= 600 ? 1.1 : .9, .0001),
          );
        }
        expect(tester.takeException(), isNull);
      }
    });
  }

  testWidgets('gerçek alan hazır fixture/seçici ve oracle ile eşleşir', (
    tester,
  ) async {
    for (final source in [(2400, 1600), (1600, 2400)]) {
      final fixture = Fixture(
        source: raster(width: source.$1, height: source.$2),
        profiles: [
          profile(width: 600, height: 600, fingerprint: hash('1')),
          profile(width: 1200, height: 1200, fingerprint: hash('2')),
          profile(width: 1600, height: 1600, fingerprint: hash('3')),
        ],
      );
      for (final fit in [BoxFit.cover, BoxFit.contain]) {
        for (final slot in [const Size(301.2, 200.2), const Size(801, 1200)]) {
          ReadingImageViewportGeometry? geometry;
          await tester.pumpWidget(
            MediaQuery(
              data: const MediaQueryData(devicePixelRatio: 1.25),
              child: Align(
                child: SizedBox.fromSize(
                  size: slot,
                  child: ReadingImageViewport(
                    fit: fit,
                    builder: (_, value) {
                      geometry = value;
                      return const SizedBox();
                    },
                  ),
                ),
              ),
            ),
          );
          final g = geometry!;
          final domainFit = g.fit == BoxFit.cover
              ? ReadingImageFit.cover
              : ReadingImageFit.contain;
          final wanted = requiredSize(
            source.$1,
            source.$2,
            (g.slot.width * g.dpr).ceil(),
            (g.slot.height * g.dpr).ceil(),
            domainFit,
          );
          final result = select(
            fixture,
            width: g.slot.width,
            height: g.slot.height,
            dpr: g.dpr,
            fit: domainFit,
          );
          final candidates = fixture.manifest.images.last.renditions.where(
            (r) => r.raster.width >= wanted.$1 && r.raster.height >= wanted.$2,
          );
          if (candidates.isEmpty) {
            expect(
              failure(result),
              ReadingImageSelectionFailure.renditionShortage,
            );
          } else {
            final selected = success(result);
            expect(selected.rendition, candidates.first);
            expect((
              selected.geometry.requiredWidth,
              selected.geometry.requiredHeight,
            ), wanted);
          }
        }
      }
    }
  });
}
