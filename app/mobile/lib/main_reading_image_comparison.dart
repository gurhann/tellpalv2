import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'app/injection.dart';
import 'app/reading_image_comparison_app.dart';
import 'app/reading_image_comparison_configuration.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  if (kReleaseMode) {
    runApp(
      const MaterialApp(
        home: Scaffold(body: Center(child: Text('QA girişi kapalı'))),
      ),
    );
    return;
  }
  configureReadingImageComparisonDependencies(
    ReadingImageComparisonHarness.configuration,
  );
  runApp(
    ReadingImageComparisonApp(
      contentId: ReadingImageComparisonHarness.contentId,
    ),
  );
}
