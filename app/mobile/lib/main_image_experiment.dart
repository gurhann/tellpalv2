import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'app/image_experiment_app.dart';
import 'app/image_experiment_bootstrap.dart';
import 'app/image_experiment_composition.dart';
import 'core/theme/library_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  FlutterError.onError = (_) {};
  ErrorWidget.builder = (_) => const ColoredBox(color: AppColors.placeholder);
  PlatformDispatcher.instance.onError = (error, stack) => true;
  final bootstrap = ImageExperimentBootstrap();
  try {
    if (kDebugMode || kIsWeb) throw const FormatException('mode_refused');
    final configuration = await bootstrap.takeSession();
    if (configuration.mode != (kProfileMode ? 'profile' : 'release')) {
      throw const FormatException('mode_refused');
    }
    final composition = await ImageExperimentComposition.create(
      configuration,
      bootstrap,
    );
    PaintingBinding.instance.imageCache.maximumSize = 48;
    PaintingBinding.instance.imageCache.maximumSizeBytes = 32 * 1024 * 1024;
    runApp(ImageExperimentApp(composition: composition));
  } catch (_) {
    await bootstrap.revoke();
    runApp(
      MaterialApp(
        theme: AppTypography.theme(),
        home: const Scaffold(
          body: Center(
            child: Text(
              'Experiment entry refused',
              style: AppTypography.description,
            ),
          ),
        ),
      ),
    );
  }
}
