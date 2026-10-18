import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../core/presentation/widgets/reading_detail_artwork_frame.dart';
import '../../features/reading_images/domain/entity/reading_image_manifest.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../reading_image_comparison_router.dart';
import 'consumer_image_slot.dart';
import 'flow_screen.dart';

@RoutePage()
class ReadingImageConsumerDetailScreen extends StatefulWidget {
  const ReadingImageConsumerDetailScreen({
    super.key,
    @PathParam('contentId') required this.contentId,
  });

  final int contentId;

  @override
  State<ReadingImageConsumerDetailScreen> createState() =>
      _ConsumerDetailState();
}

class _ConsumerDetailState extends State<ReadingImageConsumerDetailScreen>
    with AutoRouteAwareStateMixin<ReadingImageConsumerDetailScreen> {
  bool _covered = false;

  @override
  void didPushNext() {
    _covered = true;
    final cubit = context.read<ReadingImageComparisonCubit>();
    if (cubit.state.status != 'error') cubit.changeChoice(0, false);
    if (mounted) setState(() {});
  }

  @override
  void didPopNext() {
    _covered = false;
    final cubit = context.read<ReadingImageComparisonCubit>();
    if (cubit.state.status != 'error') cubit.changeChoice(0, false);
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) =>
      BlocBuilder<ReadingImageComparisonCubit, ReadingImageComparisonState>(
        builder: (context, state) {
          final comparison = state.comparison;
          final ready =
              !_covered &&
              state.status == 'loaded' &&
              comparison?.manifest.identity.contentId == widget.contentId &&
              comparison?.manifest.images.length == 4;
          return Scaffold(
            body: ListView.builder(
              padding: EdgeInsets.only(
                bottom: MediaQuery.paddingOf(context).bottom + 24,
              ),
              itemCount: 2,
              itemBuilder: (context, index) => index == 0
                  ? ReadingDetailArtworkFrame(
                      semanticLabel: 'QA deneme hikâyesi #${widget.contentId}',
                      artworkBuilder: (context, geometry) =>
                          ReadingConsumerImageSlot(
                            contentId: widget.contentId,
                            role: ReadingImageRole.readingDetail,
                            geometry: geometry,
                            visible: !_covered,
                          ),
                      overlays: [
                        Positioned(
                          top: MediaQuery.paddingOf(context).top + 16,
                          left: 16,
                          child: IconButton.filled(
                            tooltip: 'QA akışını kapat',
                            onPressed: ReadingImageConsumerFlowScope.of(
                              context,
                            ).close,
                            icon: const Icon(Icons.arrow_back),
                          ),
                        ),
                      ],
                    )
                  : Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        children: [
                          Text('QA deneme hikâyesi #${widget.contentId}'),
                          FilledButton(
                            onPressed: !ready
                                ? null
                                : () {
                                    final page = comparison!.manifest.images
                                        .firstWhere(
                                          (e) =>
                                              e.source.binding.role ==
                                              ReadingImageRole.readingPage,
                                        )
                                        .source
                                        .binding;
                                    context.router.push(
                                      ReadingImageConsumerReaderRoute(
                                        contentId: widget.contentId,
                                        pageId: page.pageId!,
                                      ),
                                    );
                                  },
                            child: const Text('QA sayfalarını aç'),
                          ),
                        ],
                      ),
                    ),
            ),
          );
        },
      );
}
