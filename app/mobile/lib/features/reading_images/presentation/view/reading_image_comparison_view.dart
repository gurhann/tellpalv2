import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/library_theme.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../cubit/reading_image_comparison_cubit.dart';
import '../cubit/reading_image_comparison_state.dart';
import '../widgets/reading_image_comparison_raster.dart';

class ReadingImageComparisonView extends StatefulWidget {
  const ReadingImageComparisonView({super.key, required this.contentId});
  final int contentId;
  @override
  State<ReadingImageComparisonView> createState() => _ComparisonViewState();
}

class _ComparisonViewState extends State<ReadingImageComparisonView>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    context.read<ReadingImageComparisonCubit>().visibility(
      state == AppLifecycleState.resumed,
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(
    BuildContext context,
  ) => BlocBuilder<ReadingImageComparisonCubit, ReadingImageComparisonState>(
    builder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('Okuma görsel karşılaştırması')),
      body: SafeArea(
        child: Column(
          children: [
            const Text(
              'QA • canonical PNG kaynak / doğrulanmış rendition',
              style: AppTypography.label,
            ),
            Wrap(
              children: [
                for (final entry in const [
                  (1, 'Kapak'),
                  (2, 'Sayfa 1'),
                  (3, 'Sayfa 2'),
                ])
                  TextButton(
                    onPressed: () =>
                        _change(context, entry.$1, state.canonicalSource),
                    child: Text(entry.$2),
                  ),
                TextButton(
                  onPressed: () => _change(
                    context,
                    state.entryIndex,
                    !state.canonicalSource,
                  ),
                  child: Text(
                    state.canonicalSource
                        ? 'Rendition göster'
                        : 'Canonical PNG kaynak göster',
                  ),
                ),
              ],
            ),
            if (state.status == 'error')
              Text(
                'Karşılaştırma durdu: ${state.error}',
                style: AppTypography.label,
              ),
            if (state.status == 'error')
              TextButton(
                onPressed: () =>
                    context.read<ReadingImageComparisonCubit>().retry(),
                child: const Text('Yeniden admission'),
              ),
            Expanded(
              child: LayoutBuilder(
                builder: (context, area) {
                  final cover = state.entryIndex < 2;
                  final width = area.maxWidth;
                  final height = cover
                      ? width /
                            (MediaQuery.sizeOf(context).shortestSide >= 600
                                ? 1.1
                                : .9)
                      : area.maxHeight;
                  return SingleChildScrollView(
                    child: Align(
                      alignment: Alignment.topCenter,
                      child: SizedBox(
                        width: width,
                        height: height,
                        child: LayoutBuilder(
                          builder: (context, slot) {
                            WidgetsBinding.instance.addPostFrameCallback((_) {
                              if (mounted && state.status != 'error') {
                                context
                                    .read<ReadingImageComparisonCubit>()
                                    .select(
                                      id: widget.contentId,
                                      entryIndex: state.entryIndex == 0
                                          ? 1
                                          : state.entryIndex,
                                      canonicalSource: state.canonicalSource,
                                      width: slot.maxWidth,
                                      height: slot.maxHeight,
                                      dpr: MediaQuery.devicePixelRatioOf(
                                        context,
                                      ),
                                      fit: cover
                                          ? ReadingImageFit.cover
                                          : ReadingImageFit.contain,
                                    );
                              }
                            });
                            return ClipRect(
                              key: const ValueKey('reading-comparison-slot'),
                              child: state.raster == null
                                  ? ColoredBox(
                                      color: AppColors.placeholder,
                                      child: Center(
                                        child: Text(
                                          state.status == 'loading'
                                              ? 'Yükleniyor'
                                              : 'Ağsız geometri: ${slot.maxWidth.toStringAsFixed(1)} × ${slot.maxHeight.toStringAsFixed(1)} • DPR ${MediaQuery.devicePixelRatioOf(context)} • ${cover ? 'cover' : 'contain'}',
                                          style: AppTypography.label,
                                        ),
                                      ),
                                    )
                                  : ReadingImageComparisonRaster(
                                      raster: state.raster!,
                                      fit: cover
                                          ? BoxFit.cover
                                          : BoxFit.contain,
                                    ),
                            );
                          },
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    ),
  );
  void _change(BuildContext context, int index, bool canonical) {
    final cubit = context.read<ReadingImageComparisonCubit>();
    cubit.changeChoice(index, canonical);
  }
}
