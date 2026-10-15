import 'package:auto_route/auto_route.dart';
import 'package:flutter/widgets.dart';
import '../features/image_experiment/presentation/screen/raster_case_screen.dart';
import '../features/image_experiment/presentation/screen/reader_resolution_screen.dart';
part 'image_experiment_router.gr.dart';

@AutoRouterConfig(
  replaceInRouteName: 'Screen,Route',
  generateForDir: ['lib/features/image_experiment'],
)
class ImageExperimentRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(page: RasterCaseRoute.page, path: '/fixture/:caseId'),
    AutoRoute(page: ReaderResolutionRoute.page, path: '/reader'),
  ];
}
