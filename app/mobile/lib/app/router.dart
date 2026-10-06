import 'package:auto_route/auto_route.dart';
import 'package:injectable/injectable.dart';
import '../features/library/presentation/screen/library_screen.dart';
part 'router.gr.dart';

@lazySingleton
@AutoRouterConfig(replaceInRouteName: 'Screen,Route')
class LibraryRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(page: LibraryRoute.page, initial: true),
  ];
}
