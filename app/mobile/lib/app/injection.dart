import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'library_configuration.dart';
import 'injection.config.dart';

final libraryGetIt = GetIt.asNewInstance();
@InjectableInit(ignoreUnregisteredTypes: [LibraryConfiguration])
GetIt _initialize(GetIt container) => container.init();
void configureLibraryDependencies(LibraryConfiguration configuration) {
  libraryGetIt.registerSingleton<LibraryConfiguration>(configuration);
  _initialize(libraryGetIt);
}
