import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../core/theme/library_theme.dart';
import '../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'local_lossless_qa_cubit.dart';

class LocalLosslessQaApp extends StatefulWidget {
  const LocalLosslessQaApp({super.key, required this.cubit});
  final LocalLosslessQaCubit cubit;
  @override
  State<LocalLosslessQaApp> createState() => _LocalLosslessQaAppState();
}

class _LocalLosslessQaAppState extends State<LocalLosslessQaApp> {
  late final _router = RootStackRouter.build(
    routes: [
      NamedRouteDef(
        name: 'LocalLosslessQaRoute',
        path: '/',
        initial: true,
        builder: (_, _) => BlocProvider.value(
          value: widget.cubit,
          child: const _LocalLosslessQaView(),
        ),
      ),
    ],
  );
  @override
  void dispose() {
    _router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'TellPal yerel görsel QA',
    theme: AppTypography.theme(),
    debugShowCheckedModeBanner: false,
    routerConfig: _router.config(),
  );
}

class _LocalLosslessQaView extends StatelessWidget {
  const _LocalLosslessQaView();
  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Column(
        children: [
          const SizedBox(
            height: 56,
            child: Center(
              child: Text(
                'Yerel PNG / WebP · 1:1 fiziksel piksel',
                style: AppTypography.label,
              ),
            ),
          ),
          Expanded(
            child: LayoutBuilder(
              builder: (context, constraints) {
                final dpr = MediaQuery.devicePixelRatioOf(context);
                final cubit = context.read<LocalLosslessQaCubit>();
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (!cubit.isClosed) {
                    cubit.setViewport(
                      width: constraints.maxWidth,
                      height: constraints.maxHeight,
                      dpr: dpr,
                    );
                  }
                });
                return BlocBuilder<
                  LocalLosslessQaCubit,
                  ReadingImageComparisonState
                >(
                  builder: (context, state) {
                    final raster = state.raster;
                    if (raster == null) {
                      return Center(
                        child: Text(
                          state.error == null
                              ? 'QA çalıştırıcısı bekleniyor'
                              : 'Yerel QA reddedildi',
                          style: AppTypography.description,
                        ),
                      );
                    }
                    final width = 822 / dpr, height = 1334 / dpr;
                    final left =
                        ((constraints.maxWidth * dpr - 822) / 2).round() / dpr;
                    final top =
                        ((constraints.maxHeight * dpr - 1334) / 2).round() /
                        dpr;
                    return Stack(
                      children: [
                        Positioned(
                          left: left,
                          top: top,
                          width: width,
                          height: height,
                          child: ReadingImageComparisonRaster(
                            key: const ValueKey('local-lossless-raster'),
                            raster: raster,
                            fit: BoxFit.contain,
                          ),
                        ),
                      ],
                    );
                  },
                );
              },
            ),
          ),
          BlocSelector<
            LocalLosslessQaCubit,
            ReadingImageComparisonState,
            String
          >(
            selector: (state) => '${state.entryIndex + 1}/6 · ${state.status}',
            builder: (context, label) => SizedBox(
              height: 44,
              child: Center(child: Text(label, style: AppTypography.label)),
            ),
          ),
        ],
      ),
    ),
  );
}

class LocalLosslessQaRefused extends StatelessWidget {
  const LocalLosslessQaRefused({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    theme: AppTypography.theme(),
    home: const Scaffold(
      body: Center(
        child: Text(
          'Yerel QA yalnız açık profile girişinde çalışır.',
          style: AppTypography.description,
        ),
      ),
    ),
  );
}

/// Tam frame bittiğinde aynı owned raster ve gerçek widget geometrisi doğrulanır.
Future<void> paintLocalLosslessQa(LocalLosslessQaCubit cubit) async {
  final binding = WidgetsBinding.instance;
  final epoch = cubit.state.epoch, raster = cubit.state.raster;
  final image = raster?.image;
  if (image == null || cubit.state.status != 'loaded') {
    throw StateError('paint_refused');
  }
  await binding.endOfFrame.timeout(const Duration(seconds: 5));
  RenderBox? box;
  double? dpr;
  void visit(Element element) {
    if (element.widget.key == const ValueKey('local-lossless-raster')) {
      final render = element.findRenderObject();
      if (render is RenderBox && render.attached && render.hasSize) {
        box = render;
        dpr = MediaQuery.devicePixelRatioOf(element);
      }
    }
    element.visitChildElements(visit);
  }

  binding.rootElement?.visitChildElements(visit);
  if (box == null || dpr == null) throw StateError('paint_slot_refused');
  box!.markNeedsPaint();
  binding.scheduleFrame();
  await binding.endOfFrame.timeout(const Duration(seconds: 5));
  if (cubit.state.epoch != epoch ||
      cubit.state.status != 'loaded' ||
      !identical(cubit.state.raster, raster) ||
      !identical(raster!.image, image) ||
      !box!.attached ||
      binding.lifecycleState != AppLifecycleState.resumed) {
    throw StateError('paint_retired');
  }
  final origin = box!.localToGlobal(Offset.zero);
  final w = box!.size.width * dpr!, h = box!.size.height * dpr!;
  final left = origin.dx * dpr!, top = origin.dy * dpr!;
  bool integral(double value) =>
      (value - value.roundToDouble()).abs() < 0.00001;
  cubit.markPainted(epoch, {
    'logicalWidth': box!.size.width,
    'logicalHeight': box!.size.height,
    'logicalLeft': origin.dx,
    'logicalTop': origin.dy,
    'dpr': dpr,
    'physicalWidth': integral(w) ? w.roundToDouble() : w,
    'physicalHeight': integral(h) ? h.roundToDouble() : h,
    'physicalLeft': left,
    'physicalTop': top,
    'pixelAligned': integral(left) && integral(top),
    'fit': 'contain',
  });
}
