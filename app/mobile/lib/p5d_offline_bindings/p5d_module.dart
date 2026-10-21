import 'package:injectable/injectable.dart';
import '../app/p5d_offline/p5d_router.dart';
import '../features/reading_images/p5d_offline/data/p5d_manifest_repository.dart';
import '../features/reading_images/p5d_offline/domain/p5d_repository.dart';
import '../features/reading_images/p5d_offline/presentation/p5d_cubit.dart';

/// Normal uygulamanın injectable tarama köklerinin dışında kalır.
@module
abstract class P5dModule {
  @LazySingleton(env: ['p5dOffline'])
  P5dRepository get repository => P5dManifestRepository();
  @LazySingleton(env: ['p5dOffline'])
  P5dRouter get router => P5dRouter();
  @Injectable(env: ['p5dOffline'])
  P5dCubit cubit(P5dRepository repository) => P5dCubit(repository);
}
