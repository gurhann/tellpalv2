import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'library_configuration.dart';
import 'reading_image_consumer_configuration.dart';
import 'injection.config.dart';

final libraryGetIt = GetIt.asNewInstance();
@InjectableInit(
  generateForDir: [
    'lib/features/library',
    'lib/features/story_detail',
    'lib/features/reading_images/consumer',
    'lib/core',
    'lib/app/normal',
  ],
  ignoreUnregisteredTypes: [
    LibraryConfiguration,
    ReadingImageConsumerConfiguration,
  ],
)
GetIt _initialize(GetIt container) => container.init();
void configureLibraryDependencies(
  LibraryConfiguration configuration, {
  ReadingImageConsumerConfiguration? readingImages,
}) {
  if (readingImages != null) {
    libraryGetIt.registerSingleton<ReadingImageConsumerConfiguration>(
      readingImages,
    );
  }
  libraryGetIt.registerSingleton<LibraryConfiguration>(configuration);
  _initialize(libraryGetIt);
}
