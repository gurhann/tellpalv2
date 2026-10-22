import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/presentation/widgets/reading_image_viewport.dart';
import '../../p5d_offline/domain/p5d_pins.dart';
import '../../p5d_offline/domain/p5d_text_crops.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import 'p5e_cubit.dart';
import 'p5e_state.dart';

class P5eScreen extends StatelessWidget {
  const P5eScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Çevrimdışı görsel karşılaştırması')),
    body: SafeArea(
      child: BlocBuilder<P5eCubit, P5eState>(
        builder: (context, state) {
          final cubit = context.read<P5eCubit>();
          return Column(
            children: [
              DropdownButton<int>(
                value: state.index ~/ 3,
                isExpanded: true,
                items: const [
                  DropdownMenuItem(
                    value: 0,
                    child: Text('Kocaman Bir Sarılma · kapak'),
                  ),
                  DropdownMenuItem(
                    value: 1,
                    child: Text('Kocaman Bir Sarılma · ilk sayfa'),
                  ),
                  DropdownMenuItem(
                    value: 2,
                    child: Text('Pofuduk Mısır Pops · kapak'),
                  ),
                  DropdownMenuItem(
                    value: 3,
                    child: Text('Pofuduk Mısır Pops · ilk sayfa'),
                  ),
                ],
                onChanged: (value) {
                  if (value != null) cubit.select(value * 3 + state.index % 3);
                },
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (var arm = 0; arm < 3; arm++)
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: ChoiceChip(
                        label: Text(const ['Orijinal', 'PNG', 'WebP'][arm]),
                        selected: state.index % 3 == arm,
                        onSelected: (_) =>
                            cubit.select(state.index ~/ 3 * 3 + arm),
                      ),
                    ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ChoiceChip(
                    label: const Text('Tam görünüm'),
                    selected: !state.crop,
                    onSelected: (_) => cubit.select(state.index, crop: false),
                  ),
                  const SizedBox(width: 8),
                  ChoiceChip(
                    label: const Text('1:1 yazı kesiti'),
                    selected: state.crop,
                    onSelected: (_) => cubit.select(state.index, crop: true),
                  ),
                ],
              ),
              Text('Görüntülenen: ${state.visitedViews}/24'),
              Expanded(child: _P5eContent(state: state)),
              TextButton(
                onPressed: state.status == 'loaded' && state.visitedViews == 24
                    ? cubit.finish
                    : null,
                child: const Text('Karşılaştırmayı bitir ve kaydet'),
              ),
            ],
          );
        },
      ),
    ),
  );
}

class _P5eContent extends StatelessWidget {
  const _P5eContent({required this.state});
  final P5eState state;
  @override
  Widget build(BuildContext context) {
    if (state.status == 'sealing') {
      return const Center(child: Text('Oturum kaydediliyor…'));
    }
    if (state.status == 'finished') {
      return const Center(
        child: Text(
          'Oturum kaydedildi. Sonuçlar daha sonra cihazdan alınabilir.',
        ),
      );
    }
    if (state.status == 'error') {
      return const Center(
        child: Text(
          'Görsel veya oturum doğrulanamadı. Uygulamayı yeniden açın.',
        ),
      );
    }
    if (state.status == 'loading' || state.status == 'initial') {
      return const Center(child: Text('Görseller doğrulanıyor ve yükleniyor…'));
    }
    if (state.status != 'loaded' || state.raster == null) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(20),
          child: Text(
            'Karşılaştırma için uçak modunu açın ve Wi-Fi’ı kapatın. Ağ durumu doğrulanınca görsel açılır.',
          ),
        ),
      );
    }
    Widget viewport(
      BuildContext context,
      ReadingImageViewportGeometry geometry,
    ) => CustomPaint(
      key: const ValueKey('p5e-raster'),
      size: Size.infinite,
      painter: _P5ePainter(
        state.raster!,
        state.index,
        state.crop,
        geometry,
        (data) => context.read<P5eCubit>().presented(state.epoch, data),
      ),
    );
    if (!state.crop && p5dPins[state.index]['role'] == 'cover') {
      return Align(
        alignment: Alignment.topCenter,
        child: ReadingImageViewport.detail(builder: viewport),
      );
    }
    return ReadingImageViewport(fit: BoxFit.contain, builder: viewport);
  }
}

/// Kaynak pikseli fiziksel ekran pikseline eşler; bütün formatların alanı aynıdır.
class _P5ePainter extends CustomPainter {
  _P5ePainter(this.raster, this.index, this.crop, this.geometry, this.onPaint);
  final ReadingComparisonRaster raster;
  final int index;
  final bool crop;
  final ReadingImageViewportGeometry geometry;
  final void Function(Map<String, Object?>) onPaint;
  bool _recorded = false;
  @override
  void paint(Canvas canvas, Size size) {
    final image = raster.image;
    if (image == null) return;
    final dpr = geometry.dpr;
    final width = (size.width * dpr).floorToDouble().clamp(0.0, 1200.0);
    final height = (size.height * dpr).floorToDouble().clamp(0.0, 600.0);
    canvas.clipRect(Offset.zero & size);
    if (crop) {
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
    } else {
      paintImage(
        canvas: canvas,
        rect: Offset.zero & size,
        image: image,
        fit: geometry.fit,
        alignment: Alignment.center,
        filterQuality: FilterQuality.low,
      );
    }
    if (_recorded) return;
    _recorded = true;
    final data = <String, Object?>{
      'logicalWidth': size.width,
      'logicalHeight': size.height,
      'dpr': dpr,
      'fit': crop ? '1to1' : geometry.fit.name,
      'filterQuality': crop ? 'none' : 'low',
      if (crop)
        'roi': {
          'x': p5dTextCrop(index).x,
          'y': p5dTextCrop(index).y,
          'width': width,
          'height': height,
          'physicalPixelsPerSourcePixel': 1,
        },
    };
    WidgetsBinding.instance.addPostFrameCallback((_) => onPaint(data));
  }

  @override
  bool shouldRepaint(covariant _P5ePainter old) =>
      old.raster != raster ||
      old.index != index ||
      old.crop != crop ||
      old.geometry.dpr != geometry.dpr;
}
