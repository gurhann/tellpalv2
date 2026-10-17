import 'package:auto_route/auto_route.dart';
import 'package:flutter/widgets.dart';
import '../features/reading_images/presentation/screen/reading_image_comparison_screen.dart';
part 'reading_image_comparison_router.gr.dart';

@AutoRouterConfig(
  replaceInRouteName: 'Screen,Route',
  generateForDir: ['lib/features/reading_images'],
)
class ReadingImageComparisonRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(
      page: ReadingImageComparisonRoute.page,
      path: '/comparison/:contentId',
    ),
  ];
}
