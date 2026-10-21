import 'package:auto_route/auto_route.dart';
import '../../features/reading_images/p5d_offline/presentation/p5d_screen.dart';

/// Ayrı PageInfo normal uygulamanın route generator'ına katılmaz.
class P5dRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(
      page: PageInfo('P5dOffline', builder: (_) => const P5dScreen()),
      initial: true,
    ),
  ];
}
