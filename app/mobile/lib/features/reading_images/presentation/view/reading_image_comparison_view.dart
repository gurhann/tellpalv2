import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/library_theme.dart';
import '../../../../core/presentation/widgets/reading_image_viewport.dart';
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
                  child: IndexedStack(
                    index: state.canonicalSource ? 1 : 0,
                    alignment: Alignment.center,
                    children: const [
                      Text('Canonical PNG kaynak göster'),
                      Text('Rendition göster'),
                    ],
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
                  Widget buildSlot(
                    BuildContext context,
                    ReadingImageViewportGeometry geometry,
                  ) {
                    final fit = geometry.fit == BoxFit.cover
                        ? ReadingImageFit.cover
                        : ReadingImageFit.contain;
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                      if (mounted && state.status != 'error') {
                        context.read<ReadingImageComparisonCubit>().select(
                          id: widget.contentId,
                          entryIndex: state.entryIndex == 0
                              ? 1
                              : state.entryIndex,
                          canonicalSource: state.canonicalSource,
                          width: geometry.slot.width,
                          height: geometry.slot.height,
                          dpr: geometry.dpr,
                          fit: fit,
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
                                      : 'Ağsız geometri: ${geometry.slot.width.toStringAsFixed(1)} × ${geometry.slot.height.toStringAsFixed(1)} • DPR ${geometry.dpr} • ${fit.name}',
                                  style: AppTypography.label,
                                ),
                              ),
                            )
                          : ReadingImageComparisonRaster(
                              raster: state.raster!,
                              fit: geometry.fit,
                            ),
                    );
                  }

                  return SingleChildScrollView(
                    child: Align(
                      alignment: Alignment.topCenter,
                      child: SizedBox(
                        width: area.maxWidth,
                        child: cover
                            ? ReadingImageViewport.detail(builder: buildSlot)
                            : SizedBox(
                                height: area.maxHeight,
                                child: ReadingImageViewport(
                                  fit: BoxFit.contain,
                                  builder: buildSlot,
                                ),
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
