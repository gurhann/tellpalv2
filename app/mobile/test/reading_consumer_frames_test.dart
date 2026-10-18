import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_detail_artwork_frame.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_image_viewport.dart';
import 'package:tellpal_mobile_poc/core/presentation/widgets/reading_page_frame.dart';

void main() {
  for (final size in [const Size(390, 844), const Size(800, 1280)]) {
    testWidgets('detail ortak oran, clip, blur ve semantics: $size', (
      tester,
    ) async {
      tester.view.physicalSize = size;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final semantics = tester.ensureSemantics();
      ReadingImageViewportGeometry? observed;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: ReadingDetailArtworkFrame(
                semanticLabel: 'Gerçek hikâye',
                blur: true,
                artworkBuilder: (_, geometry) {
                  observed = geometry;
                  return const ColoredBox(color: Colors.white);
                },
                overlays: const [
                  Positioned(left: 16, bottom: 8, child: Text('Overlay')),
                ],
              ),
            ),
          ),
        ),
      );
      final rect = tester.getRect(find.byType(ReadingImageViewport));
      expect(rect.width, size.width);
      expect(
        rect.size.aspectRatio,
        closeTo(size.shortestSide >= 600 ? 1.1 : .9, .0001),
      );
      expect(observed!.slot, rect.size);
      expect(observed!.fit, BoxFit.cover);
      expect(
        tester.widget<ImageFiltered>(find.byType(ImageFiltered)).enabled,
        isTrue,
      );
      final clip = tester.widget<ClipRRect>(find.byType(ClipRRect));
      expect(
        clip.borderRadius,
        const BorderRadius.only(
          bottomLeft: Radius.circular(16),
          bottomRight: Radius.circular(16),
        ),
      );
      expect(find.bySemanticsLabel(RegExp('Gerçek hikâye')), findsOneWidget);
      expect(tester.getRect(find.text('Overlay')).left, rect.left + 16);
      expect(tester.getRect(find.text('Overlay')).bottom, rect.bottom - 8);
      semantics.dispose();
    });
  }

  testWidgets(
    'reader yuvaları ve 1×1 raster intrinsic boyutu/merkezi korunur',
    (tester) async {
      late Uint8List bytes;
      await tester.runAsync(() async {
        final recorder = ui.PictureRecorder();
        ui.Canvas(recorder).drawColor(Colors.white, BlendMode.src);
        final picture = recorder.endRecording();
        final image = await picture.toImage(1, 1);
        bytes = (await image.toByteData(
          format: ui.ImageByteFormat.png,
        ))!.buffer.asUint8List();
        image.dispose();
        picture.dispose();
      });
      ReadingImageViewportGeometry? measured;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ReadingPageFrame(
              header: const Text('Sayfa 1/2'),
              imageBuilder: (_, geometry) {
                measured = geometry;
                return Center(
                  child: Image.memory(bytes, key: const ValueKey('one-pixel')),
                );
              },
              progress: const SizedBox(height: 4),
              error: const Text('Hata'),
              audio: const SizedBox(height: 48, child: Text('Ses')),
              controls: const SizedBox(
                width: double.infinity,
                height: 48,
                child: Text('Kontroller'),
              ),
            ),
          ),
        ),
      );
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 30)),
      );
      await tester.pump();
      final viewport = tester.getRect(find.byType(ReadingImageViewport));
      final image = tester.getRect(find.byKey(const ValueKey('one-pixel')));
      expect(image.size, const Size(1, 1));
      expect(image.center, viewport.center);
      expect(measured!.slot, viewport.size);
      expect(measured!.fit, BoxFit.contain);
      expect(tester.getRect(find.text('Sayfa 1/2')).top, 8);
      expect(tester.getRect(find.text('Hata')).top, viewport.bottom + 12);
      expect(tester.getRect(find.text('Kontroller')).left, 16);
      expect(tester.getRect(find.text('Kontroller')).bottom, 584);
      expect(tester.takeException(), isNull);
    },
  );
}
