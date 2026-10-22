import 'package:auto_route/auto_route.dart';
import '../../features/reading_images/p5e_offline/presentation/p5e_screen.dart';

class P5eRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(
      page: PageInfo('P5eOffline', builder: (_) => const P5eScreen()),
      initial: true,
    ),
  ];
}
