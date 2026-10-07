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
import 'package:tellpal_mobile_poc/app/library_configuration.dart' as _i863;
import 'package:tellpal_mobile_poc/app/router.dart' as _i785;
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart'
    as _i240;
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart'
    as _i881;
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart'
    as _i557;
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart'
    as _i774;
import 'package:tellpal_mobile_poc/features/library/data/repository_impl/library_repository_impl.dart'
    as _i486;
import 'package:tellpal_mobile_poc/features/library/domain/repository/library_repository.dart'
    as _i431;
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart'
    as _i274;
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart'
    as _i1069;
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart'
    as _i489;
import 'package:tellpal_mobile_poc/features/story_detail/data/datasource/story_detail_datasource.dart'
    as _i432;
import 'package:tellpal_mobile_poc/features/story_detail/data/repository_impl/story_detail_repository_impl.dart'
    as _i785;
import 'package:tellpal_mobile_poc/features/story_detail/domain/repository/story_detail_repository.dart'
    as _i950;
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart'
    as _i923;
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart'
    as _i577;

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt init({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    gh.lazySingleton<_i785.LibraryRouter>(() => _i785.LibraryRouter());
    gh.lazySingleton<_i240.LibraryDiagnostics>(
      () => _i240.LibraryDiagnostics(),
    );
    gh.lazySingleton<_i557.LibraryImageCache>(
      () => _i557.LibraryImageCache(gh<_i863.LibraryConfiguration>()),
      dispose: (i) => i.close(),
    );
    gh.lazySingleton<_i774.StoryDetailImageCache>(
      () => _i774.StoryDetailImageCache(gh<_i863.LibraryConfiguration>()),
      dispose: (i) => i.close(),
    );
    gh.lazySingleton<_i881.AuthenticatedTransport>(
      () => _i881.AuthenticatedTransport(gh<_i863.LibraryConfiguration>()),
      dispose: (i) => i.close(),
    );
    gh.factory<_i1069.ArtworkCubit>(
      () => _i1069.ArtworkCubit(gh<_i863.LibraryConfiguration>()),
    );
    gh.factory<_i431.LibraryRepository>(
      () => _i486.LibraryRepositoryImpl(gh<_i881.AuthenticatedTransport>()),
    );
    gh.factory<_i432.StoryDetailDatasource>(
      () => _i432.StoryDetailDatasource(gh<_i881.AuthenticatedTransport>()),
    );
    gh.factory<_i950.StoryDetailRepository>(
      () => _i785.StoryDetailRepositoryImpl(gh<_i432.StoryDetailDatasource>()),
    );
    gh.factory<_i274.GetLibrary>(
      () => _i274.GetLibrary(gh<_i431.LibraryRepository>()),
    );
    gh.factory<_i489.LibraryCubit>(
      () => _i489.LibraryCubit(
        gh<_i274.GetLibrary>(),
        gh<_i863.LibraryConfiguration>(),
        gh<_i240.LibraryDiagnostics>(),
      ),
    );
    gh.factory<_i923.GetStoryDetail>(
      () => _i923.GetStoryDetail(gh<_i950.StoryDetailRepository>()),
    );
    gh.factory<_i577.StoryDetailCubit>(
      () => _i577.StoryDetailCubit(
        gh<_i923.GetStoryDetail>(),
        gh<_i863.LibraryConfiguration>(),
      ),
    );
    return this;
  }
}
