import 'dart:ui' show ImageFilter;

import 'package:flutter/widgets.dart';

import 'reading_image_viewport.dart';

/// Detail görselinin sunumudur; medya ve kontroller çağıranın sahipliğinde kalır.
class ReadingDetailArtworkFrame extends StatelessWidget {
  const ReadingDetailArtworkFrame({
    super.key,
    required this.semanticLabel,
    required this.artworkBuilder,
    this.blur = false,
    this.overlays = const [],
  });

  final String semanticLabel;
  final Widget Function(BuildContext, ReadingImageViewportGeometry)
  artworkBuilder;
  final bool blur;
  final List<Widget> overlays;

  @override
  Widget build(BuildContext context) => Semantics(
    label: semanticLabel,
    header: true,
    child: ClipRRect(
      borderRadius: const BorderRadius.only(
        bottomLeft: Radius.circular(16),
        bottomRight: Radius.circular(16),
      ),
      child: ReadingImageViewport.detail(
        builder: (context, geometry) => Stack(
          fit: StackFit.expand,
          children: [
            ImageFiltered(
              enabled: blur,
              imageFilter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
              child: artworkBuilder(context, geometry),
            ),
            ...overlays,
          ],
        ),
      ),
    ),
  );
}
