import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import '../features/reading_images/p5f_performance/data/p5f_manifest_repository.dart';
import '../features/reading_images/p5f_performance/domain/p5f_repository.dart';
import '../features/reading_images/p5f_performance/presentation/p5f_cubit.dart';
import '../features/reading_images/p5d_offline/data/p5d_manifest_repository.dart';
import '../features/reading_images/p5d_offline/domain/p5d_repository.dart';
import '../features/reading_images/p5e_offline/presentation/p5e_cubit.dart';

@module
abstract class P5eModule {
  @LazySingleton(env: ['p5eOffline'])
  P5dRepository get repository => P5dManifestRepository();
  @LazySingleton(env: ['p5eOffline'])
  P5fRepository get performanceRepository => P5fManifestRepository();
  @Injectable(env: ['p5eOffline'])
  P5fCubit performanceCubit(P5fRepository repository) =>
      P5fCubit(repository, (method, values) async {
        return await const MethodChannel(
              'tellpal.p5f.performance',
            ).invokeMapMethod<String, Object?>(method, values) ??
            <String, Object?>{};
      });
  @Injectable(env: ['p5eOffline'])
  P5eCubit cubit(P5dRepository repository) =>
      P5eCubit(repository, (entry) async {
        await const MethodChannel(
          'tellpal.p5e.offline',
        ).invokeMethod<void>('record', entry);
      });
}
