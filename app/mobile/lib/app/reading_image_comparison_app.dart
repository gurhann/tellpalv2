import 'package:flutter/material.dart';
import 'package:auto_route/auto_route.dart';
import '../core/theme/library_theme.dart';
import 'reading_image_comparison_router.dart';

class ReadingImageComparisonApp extends StatefulWidget {
  const ReadingImageComparisonApp({
    super.key,
    required this.contentId,
    this.consumerQa = false,
  });
  final int contentId;
  final bool consumerQa;
  @override
  State<ReadingImageComparisonApp> createState() => _ComparisonAppState();
}

class _ComparisonAppState extends State<ReadingImageComparisonApp> {
  final router = ReadingImageComparisonRouter();
  @override
  void dispose() {
    router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'TellPal QA',
    theme: AppTypography.theme(),
    routerConfig: router.config(
      navigatorObservers: () => [AutoRouteObserver()],
      deepLinkBuilder: (_) => DeepLink([
        if (widget.consumerQa)
          ReadingImageConsumerFlowRoute(
            contentId: widget.contentId,
            children: [
              ReadingImageConsumerDetailRoute(contentId: widget.contentId),
            ],
          )
        else
          ReadingImageComparisonRoute(contentId: widget.contentId),
      ]),
    ),
  );
}
