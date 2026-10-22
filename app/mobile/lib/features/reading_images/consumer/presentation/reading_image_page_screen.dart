import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import '../../../../core/presentation/reading_artwork_capability.dart';
import '../../../../app/injection.dart';
import '../../../../app/reading_image_consumer_configuration.dart';
import '../../../../core/presentation/widgets/reading_image_viewport.dart';
import 'reading_image_consumer_widget.dart';

/// Bağımsız sayfa bileşeni; reader/session veya ses davranışı eklemez.
@RoutePage()
class ReadingImagePageScreen extends StatelessWidget {
  const ReadingImagePageScreen({
    super.key,
    required this.contentId,
    required this.language,
    required this.pageNumber,
  });
  final int contentId, pageNumber;
  final String language;
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(),
    body: !libraryGetIt.isRegistered<ReadingImageConsumerConfiguration>()
        ? const SizedBox.shrink()
        : ReadingImageViewport(
            fit: BoxFit.contain,
            builder: (context, geometry) => ReadingImageConsumerWidget(
              request: ReadingArtworkRequest(
                contentId: contentId,
                language: language,
                pageNumber: pageNumber,
                fit: geometry.fit,
                visible: true,
                attempt: 0,
                onLoaded: () {},
                onError: () {},
              ),
            ),
          ),
  );
}
