import 'dart:async';
import 'dart:ui';
import 'core/diagnostics/library_diagnostics.dart';
import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'app/injection.dart';
import 'app/library_app.dart';
import 'app/library_dev_harness.dart';

Future<void> main() => runLibrary();

Future<void> runLibrary({bool installErrorHandlers = true}) async {
  WidgetsFlutterBinding.ensureInitialized();
  if (kReleaseMode) {
    runApp(
      const MaterialApp(
        home: Scaffold(
          body: Center(child: Text('Development entry unavailable')),
        ),
      ),
    );
    return;
  }
  if (installErrorHandlers) {
    FlutterError.onError = (details) => developer.log(
      'framework_error type=${details.exception.runtimeType}',
      name: 'library',
    );
    ErrorWidget.builder = (_) => const ColoredBox(color: Color(0xFFE6ECF4));
  }
  runApp(
    const MaterialApp(
      home: Scaffold(
        body: Center(child: Text('Library development harness required')),
      ),
    ),
  );
  final configuration = await LibraryDevHarness.waitForConfiguration();
  configureLibraryDependencies(configuration);
  PaintingBinding.instance.imageCache.maximumSize = 48;
  PaintingBinding.instance.imageCache.maximumSizeBytes = 32 * 1024 * 1024;
  final diagnostics = libraryGetIt<LibraryDiagnostics>();
  if (installErrorHandlers) {
    FlutterError.onError = (details) => diagnostics.error(details.exception);
    PlatformDispatcher.instance.onError = (error, stack) {
      diagnostics.error(error);
      return true;
    };
  }
  runApp(const LibraryApp());
}
