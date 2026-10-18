import 'package:auto_route/auto_route.dart';
import 'package:flutter/widgets.dart';
import '../features/reading_images/presentation/screen/reading_image_comparison_screen.dart';
import 'reading_image_consumer_qa/flow_screen.dart';
import 'reading_image_consumer_qa/detail_screen.dart';
import 'reading_image_consumer_qa/reader_screen.dart';
part 'reading_image_comparison_router.gr.dart';

@AutoRouterConfig(
  replaceInRouteName: 'Screen,Route',
  generateForDir: [
    'lib/features/reading_images',
    'lib/app/reading_image_consumer_qa',
  ],
)
class ReadingImageComparisonRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(
      page: ReadingImageComparisonRoute.page,
      path: '/comparison/:contentId',
    ),
    AutoRoute(
      page: ReadingImageConsumerFlowRoute.page,
      path: '/consumer-qa/:contentId',
      children: [
        AutoRoute(
          page: ReadingImageConsumerDetailRoute.page,
          path: 'detail/:contentId',
        ),
        AutoRoute(
          page: ReadingImageConsumerReaderRoute.page,
          path: 'reader/:contentId/:pageId',
        ),
      ],
    ),
  ];
}
