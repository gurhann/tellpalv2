import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'reading_image_comparison_configuration.dart';
import 'reading_image_comparison_injection.config.dart';

final comparisonGetIt = GetIt.asNewInstance();
@InjectableInit(
  initializerName: 'initReadingComparison',
  generateForDir: ['lib/features/reading_images'],
  ignoreUnregisteredTypes: [ReadingImageComparisonConfiguration],
)
GetIt _initializeReadingComparison(GetIt container) =>
    container.initReadingComparison();
void configureReadingImageComparisonDependencies(
  ReadingImageComparisonConfiguration configuration,
) {
  comparisonGetIt.registerSingleton<ReadingImageComparisonConfiguration>(
    configuration,
  );
  _initializeReadingComparison(comparisonGetIt);
}
