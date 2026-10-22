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
import 'package:tellpal_mobile_poc/app/p5e_offline/p5e_router.dart' as _i907;
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_repository.dart'
    as _i667;
import 'package:tellpal_mobile_poc/features/reading_images/p5e_offline/presentation/p5e_cubit.dart'
    as _i373;
import 'package:tellpal_mobile_poc/p5e_offline_bindings/p5e_module.dart'
    as _i166;

const String _p5eOffline = 'p5eOffline';

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt initP5e({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    final p5eModule = _$P5eModule();
    gh.lazySingleton<_i667.P5dRepository>(
      () => p5eModule.repository,
      registerFor: {_p5eOffline},
    );
    gh.lazySingleton<_i907.P5eRouter>(
      () => p5eModule.router,
      registerFor: {_p5eOffline},
    );
    gh.factory<_i373.P5eCubit>(
      () => p5eModule.cubit(gh<_i667.P5dRepository>()),
      registerFor: {_p5eOffline},
    );
    return this;
  }
}

class _$P5eModule extends _i166.P5eModule {}
