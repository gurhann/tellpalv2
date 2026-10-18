import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../core/presentation/widgets/reading_page_frame.dart';
import '../../features/reading_images/domain/entity/reading_image_manifest.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../reading_image_comparison_router.dart';
import 'consumer_image_slot.dart';

@RoutePage()
class ReadingImageConsumerReaderScreen extends StatefulWidget {
  const ReadingImageConsumerReaderScreen({
    super.key,
    @PathParam('contentId') required this.contentId,
    @PathParam('pageId') required this.pageId,
  });

  final int contentId, pageId;

  @override
  State<ReadingImageConsumerReaderScreen> createState() =>
      _ConsumerReaderState();
}

class _ConsumerReaderState extends State<ReadingImageConsumerReaderScreen> {
  int? _pageNumber;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _resolvePage();
  }

  @override
  void didUpdateWidget(ReadingImageConsumerReaderScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.pageId != widget.pageId) {
      _pageNumber = null;
      _resolvePage();
    }
  }

  void _resolvePage() {
    final cubit = context.read<ReadingImageComparisonCubit>();
    final pages = cubit.admittedComparison?.manifest.images.where(
      (e) =>
          e.source.binding.role == ReadingImageRole.readingPage &&
          e.source.binding.pageId == widget.pageId,
    );
    if (_pageNumber == null) {
      _pageNumber = pages?.singleOrNull?.source.binding.pageNumber;
      if (_pageNumber == null) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && !cubit.isClosed) cubit.retire('consumer_binding');
        });
      }
    }
  }

  void _changePage(int pageId) {
    context.read<ReadingImageComparisonCubit>().changeChoice(0, false);
    context.router.replace(
      ReadingImageConsumerReaderRoute(
        contentId: widget.contentId,
        pageId: pageId,
      ),
    );
  }

  @override
  Widget build(BuildContext context) =>
      BlocBuilder<ReadingImageComparisonCubit, ReadingImageComparisonState>(
        builder: (context, state) {
          final pages =
              context
                  .read<ReadingImageComparisonCubit>()
                  .admittedComparison
                  ?.manifest
                  .images
                  .where(
                    (e) =>
                        e.source.binding.role == ReadingImageRole.readingPage,
                  )
                  .map((e) => e.source.binding)
                  .toList() ??
              const <ReadingImageBinding>[];
          final index = pages.indexWhere((p) => p.pageId == widget.pageId);
          return Scaffold(
            appBar: AppBar(
              title: Text('QA deneme hikâyesi #${widget.contentId}'),
              leading: IconButton(
                tooltip: 'QA detail ekranına dön',
                onPressed: () {
                  final cubit = context.read<ReadingImageComparisonCubit>();
                  if (cubit.state.status != 'error') {
                    cubit.changeChoice(0, false);
                  }
                  context.router.maybePop();
                },
                icon: const Icon(Icons.arrow_back),
              ),
            ),
            body: ReadingPageFrame(
              header: Text('Sayfa ${_pageNumber ?? "?"}/2'),
              imageBuilder: (context, geometry) => ReadingConsumerImageSlot(
                contentId: widget.contentId,
                role: ReadingImageRole.readingPage,
                pageId: widget.pageId,
                pageNumber: _pageNumber ?? -1,
                geometry: geometry,
              ),
              audio: const Padding(
                padding: EdgeInsets.all(12),
                child: Text('Bu QA denemesinde ses kapalı'),
              ),
              controls: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  OutlinedButton.icon(
                    onPressed: index <= 0
                        ? null
                        : () => _changePage(pages[index - 1].pageId!),
                    icon: const Icon(Icons.chevron_left),
                    label: const Text('Önceki'),
                  ),
                  OutlinedButton.icon(
                    onPressed: index < 0 || index + 1 >= pages.length
                        ? null
                        : () => _changePage(pages[index + 1].pageId!),
                    icon: const Icon(Icons.chevron_right),
                    label: const Text('Sonraki'),
                  ),
                ],
              ),
            ),
          );
        },
      );
}
