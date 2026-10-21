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
import 'package:tellpal_mobile_poc/app/p5d_offline/p5d_router.dart' as _i177;
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/domain/p5d_repository.dart'
    as _i667;
import 'package:tellpal_mobile_poc/features/reading_images/p5d_offline/presentation/p5d_cubit.dart'
    as _i816;
import 'package:tellpal_mobile_poc/p5d_offline_bindings/p5d_module.dart'
    as _i1029;

const String _p5dOffline = 'p5dOffline';

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt initP5d({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    final p5dModule = _$P5dModule();
    gh.lazySingleton<_i667.P5dRepository>(
      () => p5dModule.repository,
      registerFor: {_p5dOffline},
    );
    gh.lazySingleton<_i177.P5dRouter>(
      () => p5dModule.router,
      registerFor: {_p5dOffline},
    );
    gh.factory<_i816.P5dCubit>(
      () => p5dModule.cubit(gh<_i667.P5dRepository>()),
      registerFor: {_p5dOffline},
    );
    return this;
  }
}

class _$P5dModule extends _i1029.P5dModule {}
