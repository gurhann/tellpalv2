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
import 'package:tellpal_mobile_poc/app/local_lossless_qa/assets.dart' as _i702;
import 'package:tellpal_mobile_poc/app/local_lossless_qa/cubit.dart' as _i836;

const String _localLosslessQa = 'localLosslessQa';

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt initLocalLosslessQa({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    gh.lazySingleton<_i702.LocalLosslessQaAssets>(
      () => _i702.LocalLosslessQaAssets.create(),
      registerFor: {_localLosslessQa},
    );
    gh.factory<_i836.LocalLosslessQaCubit>(
      () =>
          _i836.LocalLosslessQaCubit(assets: gh<_i702.LocalLosslessQaAssets>()),
      registerFor: {_localLosslessQa},
    );
    return this;
  }
}
