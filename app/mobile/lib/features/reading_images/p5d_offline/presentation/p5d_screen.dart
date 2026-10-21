import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/presentation/widgets/reading_image_viewport.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import '../domain/p5d_pins.dart';
import '../domain/p5d_text_crops.dart';
import 'p5d_cubit.dart';
import 'p5d_frame_proof.dart';
import 'p5d_state.dart';

class P5dScreen extends StatelessWidget {
  const P5dScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: BlocBuilder<P5dCubit, P5dState>(
        builder: (context, state) {
          if (state.status != 'loaded' || state.raster == null) {
            return Center(child: Text(state.error ?? state.status));
          }
          final cubit = context.read<P5dCubit>();
          if (state.crop) {
            return P5dFrameProof(
              epoch: state.epoch,
              index: state.index,
              crop: true,
              onPaint: cubit.recordFrameProof,
              child: _P5dCrop(raster: state.raster!, index: state.index),
            );
          }
          Widget raster(
            BuildContext context,
            ReadingImageViewportGeometry geometry,
          ) {
            cubit.viewport = {
              'logicalWidth': geometry.slot.width,
              'logicalHeight': geometry.slot.height,
              'dpr': geometry.dpr,
              'fit': geometry.fit.name,
              'filterQuality': FilterQuality.low.name,
            };
            return P5dFrameProof(
              epoch: state.epoch,
              index: state.index,
              crop: false,
              onPaint: cubit.recordFrameProof,
              child: ReadingImageComparisonRaster(
                raster: state.raster!,
                fit: geometry.fit,
              ),
            );
          }

          return p5dPins[state.index]['role'] == 'cover'
              ? Align(
                  alignment: Alignment.topCenter,
                  child: ReadingImageViewport.detail(builder: raster),
                )
              : ReadingImageViewport(fit: BoxFit.contain, builder: raster);
        },
      ),
    ),
  );
}

class _P5dCrop extends StatelessWidget {
  const _P5dCrop({required this.raster, required this.index});
  final int index;
  final ReadingComparisonRaster raster;
  @override
  Widget build(BuildContext context) => CustomPaint(
    painter: _P5dCropPainter(
      raster,
      MediaQuery.devicePixelRatioOf(context),
      index,
    ),
    size: Size.infinite,
  );
}

/// Yazı alanından sabit kesit: bir kaynak pikseli bir ekran pikseline karşılık gelir.
class _P5dCropPainter extends CustomPainter {
  const _P5dCropPainter(this.raster, this.dpr, this.index);
  final int index;
  final ReadingComparisonRaster raster;
  final double dpr;
  @override
  void paint(Canvas canvas, Size size) {
    final image = raster.image;
    if (image == null) return;
    final width = (size.width * dpr).floorToDouble().clamp(0.0, 1200.0);
    final height = (size.height * dpr).floorToDouble().clamp(0.0, 600.0);
    canvas.clipRect(Offset.zero & size);
    canvas.drawImageRect(
      image,
      Rect.fromLTWH(
        p5dTextCrop(index).x.toDouble(),
        p5dTextCrop(index).y.toDouble(),
        width,
        height,
      ),
      Rect.fromLTWH(0, 0, width / dpr, height / dpr),
      Paint()..filterQuality = FilterQuality.none,
    );
  }

  @override
  bool shouldRepaint(covariant _P5dCropPainter old) =>
      old.raster != raster || old.dpr != dpr || old.index != index;
}
