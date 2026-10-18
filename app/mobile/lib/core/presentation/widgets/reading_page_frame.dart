import 'package:flutter/material.dart';

import 'reading_image_viewport.dart';

/// Reader yerleşimi; hazırlık, ses, gezinme ve görsel sahipliği taşımaz.
class ReadingPageFrame extends StatelessWidget {
  const ReadingPageFrame({
    super.key,
    required this.header,
    required this.imageBuilder,
    required this.audio,
    required this.controls,
    this.progress,
    this.error,
  });

  final Widget header, audio, controls;
  final Widget Function(BuildContext, ReadingImageViewportGeometry)
  imageBuilder;
  final Widget? progress, error;

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: header,
        ),
        Expanded(
          child: ColoredBox(
            color: Colors.black,
            child: Center(
              child: ReadingImageViewport(
                fit: BoxFit.contain,
                builder: imageBuilder,
              ),
            ),
          ),
        ),
        ?progress,
        if (error != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: error!,
          ),
        audio,
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
          child: controls,
        ),
      ],
    ),
  );
}
