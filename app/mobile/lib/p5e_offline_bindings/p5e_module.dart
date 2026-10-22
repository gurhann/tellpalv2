import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import '../app/p5e_offline/p5e_router.dart';
import '../features/reading_images/p5d_offline/data/p5d_manifest_repository.dart';
import '../features/reading_images/p5d_offline/domain/p5d_repository.dart';
import '../features/reading_images/p5e_offline/presentation/p5e_cubit.dart';

@module
abstract class P5eModule {
  @LazySingleton(env: ['p5eOffline'])
  P5dRepository get repository => P5dManifestRepository();
  @LazySingleton(env: ['p5eOffline'])
  P5eRouter get router => P5eRouter();
  @Injectable(env: ['p5eOffline'])
  P5eCubit cubit(P5dRepository repository) =>
      P5eCubit(repository, (entry) async {
        await const MethodChannel(
          'tellpal.p5e.offline',
        ).invokeMethod<void>('record', entry);
      });
}
