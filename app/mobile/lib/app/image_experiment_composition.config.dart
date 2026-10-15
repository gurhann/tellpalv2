// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// InjectableConfigGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

// ignore_for_file: no_leading_underscores_for_library_prefixes

import 'package:get_it/get_it.dart' as _i174;
import 'package:injectable/injectable.dart' as _i526;
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart'
    as _i633;
import 'package:tellpal_mobile_poc/app/image_experiment_router.dart' as _i360;
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart'
    as _i881;
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart'
    as _i740;
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart'
    as _i517;
import 'package:tellpal_mobile_poc/features/image_experiment/data/datasource/raster_case_datasource.dart'
    as _i1028;
import 'package:tellpal_mobile_poc/features/image_experiment/data/image_experiment_module.dart'
    as _i288;
import 'package:tellpal_mobile_poc/features/image_experiment/data/reader_manifest_source.dart'
    as _i895;
import 'package:tellpal_mobile_poc/features/image_experiment/data/repository_impl/raster_case_repository_impl.dart'
    as _i905;
import 'package:tellpal_mobile_poc/features/image_experiment/domain/repository/raster_case_repository.dart'
    as _i585;
import 'package:tellpal_mobile_poc/features/image_experiment/domain/use_case/get_raster_case.dart'
    as _i531;
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/raster_case_cubit.dart'
    as _i723;
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/reader_resolution_cubit.dart'
    as _i343;
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/view/reader_resolution_images.dart'
    as _i136;
import 'package:tellpal_mobile_poc/story_manifest_client.dart' as _i148;

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt initImageExperiment({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    final imageExperimentModule = _$ImageExperimentModule();
    gh.lazySingleton<_i360.ImageExperimentRouter>(
      () => imageExperimentModule.router,
    );
    gh.factory<_i1028.RasterCaseDatasource>(
      () =>
          _i1028.RasterCaseDatasource(gh<_i633.ImageExperimentConfiguration>()),
    );
    gh.lazySingleton<_i881.ImageExperimentRecorder>(
      () => imageExperimentModule.recorder(
        gh<_i633.ImageExperimentConfiguration>(),
      ),
    );
    gh.lazySingleton<_i148.StoryManifestSource>(
      () =>
          _i895.ReaderManifestSource(gh<_i633.ImageExperimentConfiguration>()),
    );
    gh.factory<_i585.RasterCaseRepository>(
      () => _i905.RasterCaseRepositoryImpl(gh<_i1028.RasterCaseDatasource>()),
    );
    gh.factory<_i531.GetRasterCase>(
      () => _i531.GetRasterCase(gh<_i585.RasterCaseRepository>()),
    );
    gh.factory<_i723.RasterCaseCubit>(
      () => _i723.RasterCaseCubit(
        gh<_i531.GetRasterCase>(),
        gh<_i633.ImageExperimentConfiguration>(),
      ),
    );
    gh.lazySingleton<_i740.ImageExperimentTransport>(
      () => imageExperimentModule.transport(
        gh<_i633.ImageExperimentConfiguration>(),
        gh<_i881.ImageExperimentRecorder>(),
      ),
    );
    gh.lazySingleton<_i517.ImageExperimentImageCache>(
      () => imageExperimentModule.cache(
        gh<_i633.ImageExperimentConfiguration>(),
        gh<_i740.ImageExperimentTransport>(),
        gh<_i881.ImageExperimentRecorder>(),
      ),
    );
    gh.lazySingleton<_i136.ReaderResolutionImages>(
      () => _i136.ReaderResolutionImages(
        gh<_i633.ImageExperimentConfiguration>(),
        gh<_i517.ImageExperimentImageCache>(),
        gh<_i881.ImageExperimentRecorder>(),
      ),
    );
    gh.factory<_i343.ReaderResolutionCubit>(
      () => _i343.ReaderResolutionCubit(
        gh<_i633.ImageExperimentConfiguration>(),
        gh<_i136.ReaderResolutionImages>(),
        gh<_i881.ImageExperimentRecorder>(),
      ),
    );
    return this;
  }
}

class _$ImageExperimentModule extends _i288.ImageExperimentModule {}
