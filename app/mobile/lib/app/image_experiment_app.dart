import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import '../core/theme/library_theme.dart';
import '../story_reader_theme.dart';
import 'image_experiment_composition.dart';
import 'image_experiment_router.dart';

class ImageExperimentApp extends StatefulWidget {
  const ImageExperimentApp({super.key, required this.composition});
  final ImageExperimentComposition composition;
  @override
  State<ImageExperimentApp> createState() => _ImageExperimentAppState();
}

class _ImageExperimentAppState extends State<ImageExperimentApp>
    with WidgetsBindingObserver {
  StreamSubscription<void>? _revocations;
  bool _refused = false;
  bool _refusing = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final credentials = widget.composition.configuration.credentials;
    _revocations = credentials.revocations.listen((_) => _refuse());
    // Broadcast revocation can precede mounting; subscription alone cannot replay it.
    if (!credentials.isActive) _refuse();
  }

  void _refuse() {
    if (!mounted || _refusing) return;
    _refusing = true;
    if ((!widget.composition.phases.enabled ||
            !widget.composition.phases.quality) &&
        !widget.composition.configuration.readerQuality) {
      setState(() => _refused = true);
      unawaited(
        WidgetsBinding.instance.endOfFrame
            .then((_) => widget.composition.close())
            .catchError((_) {}),
      );
      return;
    }
    unawaited(
      () async {
        try {
          await widget.composition.invalidateQualityBinding();
        } catch (_) {}
        if (!mounted) return;
        setState(() => _refused = true);
        // Native quality evidence is retired before the route can paint a placeholder.
        await WidgetsBinding.instance.endOfFrame;
        await widget.composition.close();
      }().catchError((_) {}),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.detached) {
      unawaited(widget.composition.close().catchError((_) {}));
    }
  }

  @override
  Widget build(BuildContext context) => _refused
      ? MaterialApp(
          theme: AppTypography.theme(),
          home: const Scaffold(
            body: Center(
              child: Text(
                'Experiment entry refused',
                style: AppTypography.description,
              ),
            ),
          ),
        )
      : ImageExperimentResolver(
          composition: widget.composition,
          child: MaterialApp.router(
            title: 'TellPal Image Experiment',
            debugShowCheckedModeBanner: false,
            theme: widget.composition.configuration.isReader
                ? storyReaderTheme()
                : AppTypography.theme(),
            routerConfig: widget.composition
                .container<ImageExperimentRouter>()
                .config(
                  deepLinkBuilder: (_) => DeepLink.single(
                    widget.composition.configuration.isReader
                        ? const ReaderResolutionRoute()
                        : RasterCaseRoute(
                            caseId:
                                widget.composition.configuration.selectedCaseId,
                          ),
                  ),
                ),
          ),
        );
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    unawaited(_revocations?.cancel());
    unawaited(widget.composition.close().catchError((_) {}));
    super.dispose();
  }
}
