import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/presentation/widgets/reading_image_viewport.dart';
import '../../p5d_offline/domain/p5d_pins.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import 'p5f_cubit.dart';
import 'p5f_state.dart';

class P5fScreen extends StatefulWidget {
  const P5fScreen({super.key});
  @override
  State<P5fScreen> createState() => _P5fScreenState();
}

class _P5fScreenState extends State<P5fScreen> with WidgetsBindingObserver {
  static const channel = MethodChannel('tellpal.p5f.performance');
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    channel.setMethodCallHandler((call) async {
      if (!mounted) return;
      context.read<P5fCubit>().interrupt(
        call.arguments as String? ?? 'native_invalidated',
      );
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) =>
      context.read<P5fCubit>().lifecycle(state == AppLifecycleState.resumed);
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    channel.setMethodCallHandler(null);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('P5f hız ve bellek')),
    body: SafeArea(
      child: BlocBuilder<P5fCubit, P5fState>(
        builder: (context, state) => Column(
          children: [
            Text('Ölçülen deneme: ${state.completed}/144'),
            if (state.status == 'ready')
              const Text(
                'Uçak modunu açın ve Wi-Fi’ı kapatın. Düşük güç modu kapalı olmalı. Ölçüm boyunca uygulamayı önde tutun.',
              ),
            if (state.status == 'ready')
              TextButton(
                onPressed: context.read<P5fCubit>().start,
                child: const Text('Çevrimdışı ölçümü başlat'),
              ),
            if (state.status == 'finished')
              const Text('144 ölçüm ve temizlik mühürlendi.'),
            if (state.status == 'error')
              Text(
                'Ölçüm geçersiz: ${state.error}. Yeni QA oturumu gerekiyor.',
              ),
            if (state.status == 'initial')
              const Text('SHA ve ICC doğrulanıyor…'),
            Expanded(
              child: state.raster == null
                  ? const SizedBox.expand()
                  : _Raster(state: state),
            ),
          ],
        ),
      ),
    ),
  );
}

class _Raster extends StatelessWidget {
  const _Raster({required this.state});
  final P5fState state;
  @override
  Widget build(BuildContext context) {
    Widget viewport(
      BuildContext context,
      ReadingImageViewportGeometry geometry,
    ) => CustomPaint(
      foregroundPainter: _Frame(
        state.epoch,
        state.index,
        geometry,
        context.read<P5fCubit>().painted,
      ),
      child: ReadingImageComparisonRaster(
        raster: state.raster!,
        fit: geometry.fit,
      ),
    );
    if (p5dPins[state.index]['role'] == 'cover') {
      return Align(
        alignment: Alignment.topCenter,
        child: ReadingImageViewport.detail(builder: viewport),
      );
    }
    return ReadingImageViewport(fit: BoxFit.contain, builder: viewport);
  }
}

/// Zaman damgası alanları birbirinden çıkarılmaz; engine frame kimliği eşleştirilir.
class _Frame extends CustomPainter {
  _Frame(this.epoch, this.index, this.geometry, this.onPaint);
  final int epoch, index;
  final ReadingImageViewportGeometry geometry;
  final void Function(Map<String, Object?>) onPaint;
  bool _recorded = false;
  @override
  void paint(Canvas canvas, Size size) {
    if (_recorded) return;
    _recorded = true;
    final proof = <String, Object?>{
      'epoch': epoch,
      'index': index,
      'crop': false,
      'frameNumber':
          SchedulerBinding.instance.platformDispatcher.frameData.frameNumber,
      'logicalWidth': size.width,
      'logicalHeight': size.height,
      'dpr': geometry.dpr,
      'fit': geometry.fit.name,
      'filterQuality': 'low',
    };
    onPaint(proof);
  }

  @override
  bool shouldRepaint(covariant _Frame oldDelegate) =>
      oldDelegate.epoch != epoch;
}
