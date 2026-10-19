import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'app/local_lossless_qa_cubit.dart';
import 'app/local_lossless_qa_injection.dart';
import 'app/manual_reading_images_qa.dart';

/// Ürün girişinden bağımsız, yalnız açık debug seçimiyle çalışan yerel ekran.
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  const enabled = bool.fromEnvironment('TELLPAL_MANUAL_IMAGE_QA');
  if (!manualReadingImagesAllowed(
    enabled: enabled,
    debug: kDebugMode,
    web: kIsWeb,
  )) {
    runApp(
      const MaterialApp(
        home: Scaffold(body: Center(child: Text('Ağsız QA girişi kapalı.'))),
      ),
    );
    return;
  }
  final container = configureLocalLosslessQa(GetIt.asNewInstance());
  runApp(ManualReadingImagesQaApp(cubit: container<LocalLosslessQaCubit>()));
}
