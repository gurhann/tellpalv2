import 'package:flutter/widgets.dart';

/// Son görsel alanını ve o yerleşimin DPR/fit değerlerini taşır.
@immutable
class ReadingImageViewportGeometry {
  const ReadingImageViewportGeometry({
    required this.slot,
    required this.dpr,
    required this.fit,
  });

  final Size slot;
  final double dpr;
  final BoxFit fit;
}

/// Tüketicinin ayırdığı son alanı ölçer; medya veya rendition seçmez.
class ReadingImageViewport extends StatelessWidget {
  const ReadingImageViewport({
    super.key,
    required this.fit,
    required this.builder,
  }) : _detail = false;

  /// Detail ve QA kapağı aynı uyarlanabilir oranı kullanır.
  const ReadingImageViewport.detail({super.key, required this.builder})
    : fit = BoxFit.cover,
      _detail = true;

  final BoxFit fit;
  final Widget Function(BuildContext, ReadingImageViewportGeometry) builder;
  final bool _detail;

  @override
  Widget build(BuildContext context) {
    final viewport = LayoutBuilder(
      builder: (context, constraints) => SizedBox.expand(
        child: builder(
          context,
          ReadingImageViewportGeometry(
            slot: constraints.biggest,
            dpr: MediaQuery.devicePixelRatioOf(context),
            fit: fit,
          ),
        ),
      ),
    );
    return _detail
        ? AspectRatio(
            aspectRatio: MediaQuery.sizeOf(context).shortestSide >= 600
                ? 1.1
                : .9,
            child: viewport,
          )
        : viewport;
  }
}
