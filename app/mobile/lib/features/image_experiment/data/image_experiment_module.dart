import 'package:injectable/injectable.dart';
import '../../../app/image_experiment_configuration.dart';
import '../../../app/image_experiment_router.dart';
import '../../../core/diagnostics/image_experiment_recorder.dart';
import '../../../core/network/image_experiment_http_client.dart';
import '../../../core/network/image_experiment_image_cache.dart';

@module
abstract class ImageExperimentModule {
  @lazySingleton
  ImageExperimentRecorder get recorder => ImageExperimentRecorder();
  @lazySingleton
  ImageExperimentRouter get router => ImageExperimentRouter();
  @lazySingleton
  ImageExperimentTransport transport(
    ImageExperimentConfiguration configuration,
    ImageExperimentRecorder recorder,
  ) => ImageExperimentTransport(configuration, recorder);
  @lazySingleton
  ImageExperimentImageCache cache(
    ImageExperimentConfiguration configuration,
    ImageExperimentTransport transport,
    ImageExperimentRecorder recorder,
  ) => ImageExperimentImageCache(configuration, transport, recorder);
}
