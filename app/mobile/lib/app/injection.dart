import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'library_configuration.dart';
import 'injection.config.dart';
export 'reading_image_comparison_injection.dart';

final libraryGetIt = GetIt.asNewInstance();
@InjectableInit(
  generateForDir: [
    'lib/features/library',
    'lib/features/story_detail',
    'lib/core',
    'lib/app',
  ],
  ignoreUnregisteredTypes: [LibraryConfiguration],
)
GetIt _initialize(GetIt container) => container.init();
void configureLibraryDependencies(LibraryConfiguration configuration) {
  libraryGetIt.registerSingleton<LibraryConfiguration>(configuration);
  _initialize(libraryGetIt);
}
