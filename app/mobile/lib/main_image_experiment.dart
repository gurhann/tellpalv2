import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'app/image_experiment_app.dart';
import 'app/image_experiment_bootstrap.dart';
import 'app/image_experiment_composition.dart';
import 'app/image_experiment_configuration.dart';
import 'app/local_lossless_qa.dart';
import 'core/theme/library_theme.dart';

Future<void> main() async {
  if (localLosslessQaEnabled) {
    await runLocalLosslessQa();
    return;
  }
  WidgetsFlutterBinding.ensureInitialized();
  FlutterError.onError = (_) {};
  ErrorWidget.builder = (_) => const ColoredBox(color: AppColors.placeholder);
  PlatformDispatcher.instance.onError = (error, stack) => true;
  final bootstrap = ImageExperimentBootstrap();
  var stage = ImageExperimentFixtureStage.dartMode;
  try {
    if (kDebugMode || kIsWeb) throw const FormatException('mode_refused');
    stage = ImageExperimentFixtureStage.dartNativeHandoff;
    final configuration = await bootstrap.takeSession();
    stage = ImageExperimentFixtureStage.dartMode;
    if (configuration.mode != (kProfileMode ? 'profile' : 'release')) {
      throw const FormatException('mode_refused');
    }
    stage = ImageExperimentFixtureStage.dartComposition;
    final composition = await ImageExperimentComposition.create(
      configuration,
      bootstrap,
      observeRefusal: bootstrap.reportRefusal,
    );
    if (!configuration.isReader) {
      PaintingBinding.instance.imageCache.maximumSize = 48;
    }
    PaintingBinding.instance.imageCache.maximumSizeBytes =
        (configuration.isReader ? 24 : 32) * 1024 * 1024;
    runApp(ImageExperimentApp(composition: composition));
  } catch (_) {
    bootstrap.reportRefusal(stage);
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
