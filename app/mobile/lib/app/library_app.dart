import 'package:flutter/material.dart';
import '../core/theme/library_theme.dart';
import 'injection.dart';
import 'router.dart';
import 'library_dev_harness.dart';

class LibraryApp extends StatelessWidget {
  const LibraryApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'TellPal Library',
    debugShowCheckedModeBanner: false,
    theme: AppTypography.theme(),
    builder: (context, child) => ValueListenableBuilder<double?>(
      valueListenable: LibraryDevHarness.scaleNotifier,
      builder: (context, scale, _) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: scale == null
              ? MediaQuery.textScalerOf(context)
              : TextScaler.linear(scale),
        ),
        child: child!,
      ),
    ),
    routerConfig: libraryGetIt<LibraryRouter>().config(),
  );
}
