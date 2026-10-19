import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_driver/driver_extension.dart';
import 'reading_consumer_qa_probe.dart';

/// Yalnız ayrı debug APK'da çalışır; normal giriş ve router değişmez.
void main() {
  if (!kDebugMode) {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(const ReadingConsumerQaWaiting());
    return;
  }
  final probe = ReadingConsumerQaProbe(
    expectedScope: ConsumerQaExpectedScope.p4b5,
  );
  enableFlutterDriverExtension(
    handler: probe.handle,
    silenceErrors: true,
    enableTextEntryEmulation: false,
  );
  Bloc.observer = probe;
  runApp(const ReadingConsumerQaWaiting());
}
