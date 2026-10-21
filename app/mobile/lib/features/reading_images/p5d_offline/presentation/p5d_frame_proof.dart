import 'package:flutter/widgets.dart';
import 'package:flutter/scheduler.dart';
import '../domain/p5d_text_crops.dart';

bool p5dProofMatches(
  Map<String, Object?>? proof,
  int epoch,
  int index,
  bool crop,
) =>
    proof != null &&
    proof['epoch'] == epoch &&
    proof['index'] == index &&
    proof['crop'] == crop;

/// Aynı Flutter frame'inin küçük vektör işareti; görsel alanının ölçüsünü değiştirmez.
class P5dFrameProof extends StatelessWidget {
  const P5dFrameProof({
    super.key,
    required this.epoch,
    required this.index,
    required this.crop,
    required this.onPaint,
    required this.child,
  });
  final int epoch, index;
  final bool crop;
  final ValueChanged<Map<String, Object?>> onPaint;
  final Widget child;
  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) => CustomPaint(
      foregroundPainter: _ProofPainter(
        epoch,
        index,
        crop,
        MediaQuery.devicePixelRatioOf(context),
        () => (context.findRenderObject()! as RenderBox).localToGlobal(
          Offset.zero,
        ),
        onPaint,
      ),
      child: child,
    ),
  );
}

class _ProofPainter extends CustomPainter {
  const _ProofPainter(
    this.epoch,
    this.index,
    this.crop,
    this.dpr,
    this.origin,
    this.onPaint,
  );
  final int epoch, index;
  final bool crop;
  final double dpr;
  final Offset Function() origin;
  final ValueChanged<Map<String, Object?>> onPaint;
  @override
  void paint(Canvas canvas, Size size) {
    final width = (size.width * dpr).floor(),
        height = (size.height * dpr).floor();
    final x = width - 12, y = height - 12;
    final code =
        ((epoch * 73 % 256) << 16) |
        ((epoch * 151 % 256) << 8) |
        (crop ? 61 : 197);
    final colors = [0x000000, 0xffffff, code, code ^ 0xffffff];
    for (var cell = 0; cell < 4; cell++) {
      canvas.drawRect(
        Rect.fromLTWH(
          (x + cell % 2 * 4) / dpr,
          (y + cell ~/ 2 * 4) / dpr,
          4 / dpr,
          4 / dpr,
        ),
        Paint()
          ..color = Color(0xff000000 | colors[cell])
          ..isAntiAlias = false,
      );
    }
    final offset = origin();
    onPaint({
      'frameNumber':
          SchedulerBinding.instance.platformDispatcher.frameData.frameNumber,
      'vsyncMicros':
          SchedulerBinding.instance.currentSystemFrameTimeStamp.inMicroseconds,
      'epoch': epoch,
      'index': index,
      'crop': crop,
      'dpr': dpr,
      'markerX': (offset.dx * dpr).round() + x,
      'markerY': (offset.dy * dpr).round() + y,
      'markerCellPixels': 4,
      'markerColors': colors,
      'logicalWidth': size.width,
      'logicalHeight': size.height,
      if (crop)
        'roi': {
          'sourceX': p5dTextCrop(index).x,
          'sourceY': p5dTextCrop(index).y,
          'sourceWidth': width.clamp(0, 1200),
          'sourceHeight': height.clamp(0, 600),
          'destinationPhysicalWidth': width.clamp(0, 1200),
          'destinationPhysicalHeight': height.clamp(0, 600),
          'globalPhysicalLeft': (offset.dx * dpr).round(),
          'globalPhysicalTop': (offset.dy * dpr).round(),
          'physicalPixelsPerSourcePixel': 1,
        },
    });
  }

  @override
  bool shouldRepaint(covariant _ProofPainter old) =>
      old.epoch != epoch ||
      old.index != index ||
      old.crop != crop ||
      old.dpr != dpr;
}
