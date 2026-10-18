import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../core/presentation/widgets/reading_image_viewport.dart';
import '../../core/theme/library_theme.dart';
import '../../features/reading_images/domain/entity/reading_image_manifest.dart';
import '../../features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../../features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

/// Gerçek frame ölçümünü mevcut seçiciye verir; medya sahipliği Cubit'tedir.
class ReadingConsumerImageSlot extends StatefulWidget {
  const ReadingConsumerImageSlot({
    super.key,
    required this.contentId,
    required this.role,
    required this.geometry,
    this.pageId,
    this.pageNumber = 0,
    this.visible = true,
  });

  final int contentId;
  final ReadingImageRole role;
  final int? pageId;
  final int pageNumber;
  final ReadingImageViewportGeometry geometry;
  final bool visible;

  @override
  State<ReadingConsumerImageSlot> createState() => _ConsumerImageSlotState();
}

class _ConsumerImageSlotState extends State<ReadingConsumerImageSlot> {
  @override
  Widget build(BuildContext context) =>
      BlocBuilder<ReadingImageComparisonCubit, ReadingImageComparisonState>(
        builder: (context, state) {
          final cubit = context.read<ReadingImageComparisonCubit>();
          final geometry = widget.geometry;
          final current = ModalRoute.of(context)?.isCurrent ?? true;
          if (widget.visible && current && state.status != 'error') {
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (!mounted ||
                  !widget.visible ||
                  cubit.state.epoch != state.epoch ||
                  cubit.state.status == 'error' ||
                  ModalRoute.of(context)?.isCurrent == false ||
                  cubit.isClosed) {
                return;
              }
              cubit.select(
                id: widget.contentId,
                entryIndex: 0,
                canonicalSource: false,
                width: geometry.slot.width,
                height: geometry.slot.height,
                dpr: geometry.dpr,
                fit: geometry.fit == BoxFit.cover
                    ? ReadingImageFit.cover
                    : ReadingImageFit.contain,
                consumerRole: widget.role,
                pageId: widget.pageId,
                pageNumber: widget.pageNumber,
              );
            });
          }
          final binding = state.comparison?.manifest.images
              .elementAtOrNull(state.entryIndex)
              ?.source
              .binding;
          final owns =
              widget.visible &&
              current &&
              state.status == 'loaded' &&
              binding?.role == widget.role &&
              binding?.pageId == widget.pageId &&
              binding?.pageNumber == widget.pageNumber;
          return ClipRect(
            key: const ValueKey('reading-consumer-slot'),
            child: owns && state.raster != null
                ? ReadingImageComparisonRaster(
                    raster: state.raster!,
                    fit: geometry.fit,
                  )
                : ColoredBox(
                    color: AppColors.placeholder,
                    child: Center(
                      child: state.status == 'error'
                          ? Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  'QA görseli durdu: ${state.error}',
                                  style: AppTypography.label,
                                ),
                                TextButton(
                                  onPressed: cubit.retry,
                                  child: const Text('Yeniden admission'),
                                ),
                              ],
                            )
                          : const Text(
                              'QA görseli yükleniyor',
                              style: AppTypography.label,
                            ),
                    ),
                  ),
          );
        },
      );
}
