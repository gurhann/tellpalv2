import 'package:flutter/material.dart';
import 'package:auto_route/auto_route.dart';
import '../core/theme/library_theme.dart';
import 'injection.dart';
import 'router.dart';
import 'library_dev_harness.dart';
import 'reading_image_consumer_configuration.dart';
import '../core/presentation/reading_artwork_capability.dart';
import '../features/reading_images/consumer/presentation/reading_image_consumer_widget.dart';

class LibraryApp extends StatelessWidget {
  const LibraryApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'TellPal Library',
    debugShowCheckedModeBanner: false,
    theme: AppTypography.theme(),
    builder: (context, child) => ValueListenableBuilder<double?>(
      valueListenable: LibraryDevHarness.scaleNotifier,
      builder: (context, scale, _) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: scale == null
              ? MediaQuery.textScalerOf(context)
              : TextScaler.linear(scale),
        ),
        child: libraryGetIt.isRegistered<ReadingImageConsumerConfiguration>()
            ? ReadingArtworkCapability(
                builder: (_, request) => ReadingImageConsumerWidget(
                  key: ValueKey(
                    '${request.contentId}:${request.language}:${request.attempt}',
                  ),
                  request: request,
                ),
                child: child!,
              )
            : child!,
      ),
    ),
    routerConfig: libraryGetIt<LibraryRouter>().config(
      navigatorObservers: () => [AutoRouteObserver()],
    ),
  );
}
