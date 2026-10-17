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
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart'
    as _i230;
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart'
    as _i599;
import 'package:tellpal_mobile_poc/features/reading_images/data/repository/reading_image_comparison_repository_impl.dart'
    as _i162;
import 'package:tellpal_mobile_poc/features/reading_images/domain/repository/reading_image_comparison_repository.dart'
    as _i148;
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart'
    as _i349;
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart'
    as _i550;

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt initReadingComparison({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    gh.lazySingleton<_i550.ReadingComparisonWorkGate>(
      () => _i550.ReadingComparisonWorkGate(),
    );
    gh.lazySingleton<_i550.ReadingComparisonClock>(
      () => const _i550.ReadingComparisonClock(),
    );
    gh.factory<_i599.ReadingImageComparisonDataSource>(
      () => _i599.ReadingImageComparisonDataSource(
        gh<_i230.ReadingImageComparisonConfiguration>(),
      ),
    );
    gh.factory<_i148.ReadingImageComparisonRepository>(
      () => _i162.ReadingImageComparisonRepositoryImpl(
        gh<_i599.ReadingImageComparisonDataSource>(),
        gh<_i230.ReadingImageComparisonConfiguration>(),
      ),
    );
    gh.factory<_i349.LoadReadingImageComparison>(
      () => _i349.LoadReadingImageComparison(
        gh<_i148.ReadingImageComparisonRepository>(),
      ),
    );
    gh.factory<_i550.ReadingImageComparisonCubit>(
      () => _i550.ReadingImageComparisonCubit(
        gh<_i349.LoadReadingImageComparison>(),
        gh<_i148.ReadingImageComparisonRepository>(),
        gh<_i230.ReadingImageComparisonConfiguration>(),
        gh<_i550.ReadingComparisonWorkGate>(),
        gh<_i550.ReadingComparisonClock>(),
      ),
    );
    return this;
  }
}
