import 'package:auto_route/auto_route.dart';
import 'package:flutter/widgets.dart';
import 'package:injectable/injectable.dart';
import '../features/library/presentation/screen/library_screen.dart';
import '../features/story_detail/presentation/screen/story_detail_screen.dart';
import '../features/reading_images/consumer/presentation/reading_image_page_screen.dart';
part 'router.gr.dart';

@lazySingleton
@AutoRouterConfig(
  replaceInRouteName: 'Screen,Route',
  generateForDir: [
    'lib/features/library',
    'lib/features/story_detail',
    'lib/features/reading_images/consumer',
  ],
)
class LibraryRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(page: LibraryRoute.page, initial: true),
    AutoRoute(page: StoryDetailRoute.page),
    AutoRoute(page: ReadingImagePageRoute.page),
  ];
}
