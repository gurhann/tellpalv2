import 'package:auto_route/auto_route.dart';
import '../../features/reading_images/p5e_offline/presentation/p5e_screen.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import '../../features/reading_images/p5f_performance/presentation/p5f_screen.dart';
import '../../features/reading_images/p5f_performance/presentation/p5f_cubit.dart';

class P5eRouter extends RootStackRouter {
  P5eRouter(this.container);
  final GetIt container;
  @override
  List<AutoRoute> get routes => [
    if (const bool.fromEnvironment('TELLPAL_P5F_PERFORMANCE'))
      AutoRoute(
        initial: true,
        path: '/performance',
        page: PageInfo(
          'P5fPerformance',
          builder: (_) => BlocProvider(
            create: (_) => container<P5fCubit>()..initialize(),
            child: const P5fScreen(),
          ),
        ),
      ),
    if (!const bool.fromEnvironment('TELLPAL_P5F_PERFORMANCE'))
      AutoRoute(
        page: PageInfo('P5eOffline', builder: (_) => const P5eScreen()),
        initial: true,
      ),
  ];
}
