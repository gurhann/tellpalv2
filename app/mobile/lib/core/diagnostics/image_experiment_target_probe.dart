import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import '../network/image_experiment_image_cache.dart';
import '../theme/library_theme.dart';
import 'image_experiment_recorder.dart';

/// One listener resolves the final bounded provider. The renderer receives its own clone.
class ImageExperimentTargetProbe extends StatefulWidget {
  const ImageExperimentTargetProbe({
    super.key,
    required this.value,
    required this.cache,
    required this.recorder,
    required this.generation,
    required this.current,
    required this.onDecoded,
    required this.onFailure,
    this.onDraw,
  });
  final ImageExperimentProvider value;
  final ImageExperimentImageCache cache;
  final ImageExperimentRecorder recorder;
  final int generation;
  final bool Function() current;
  final void Function() onDecoded, onFailure;
  final void Function()? onDraw;
  @override
  State<ImageExperimentTargetProbe> createState() =>
      _ImageExperimentTargetProbeState();
}

class _ImageExperimentTargetProbeState
    extends State<ImageExperimentTargetProbe> {
  ImageStream? _stream;
  ImageStreamListener? _listener;
  ImageInfo? _info;
  bool _failed = false;
  bool _drawCompleted = false;
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_stream != null) return;
    _stream = widget.value.provider.resolve(
      createLocalImageConfiguration(context),
    );
    _listener = ImageStreamListener(
      _decoded,
      onError: (Object _, StackTrace? stack) => _failure(),
    );
    _stream!.addListener(_listener!);
  }

  void _decoded(ImageInfo image, bool synchronous) {
    if (!mounted || !widget.current()) {
      image.dispose();
      return;
    }
    if (!widget.cache.verified(widget.value)) {
      image.dispose();
      _failure();
      return;
    }
    final previous = _info;
    _info = image.clone();
    previous?.dispose();
    widget.recorder.record({
      'event': 'decoded',
      'caseSha256': ImageExperimentRecorder.hash(widget.value.fixture.caseId),
      'providerSha256': widget.value.immutableKey,
      'attemptSha256': widget.value.attemptSha256,
      'actualDecodeWidth': image.image.width,
      'actualDecodeHeight': image.image.height,
      'generation': widget.generation,
      'ownership': 'owned',
    });
    image.dispose();
    unawaited(
      widget.cache
          .remember(widget.value, () => mounted && widget.current())
          .catchError((_) {
            widget.recorder.record({
              'event': 'failure',
              'reason': 'invalid_fixture',
              'generation': widget.generation,
            });
          }),
    );
    // A synchronous image listener may run during build; notify after the frame.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !widget.current()) return;
      setState(() {});
      widget.onDecoded();
    });
    WidgetsBinding.instance.ensureVisualUpdate();
  }

  void _failure() {
    if (!mounted || !widget.current() || _failed) return;
    _failed = true;
    widget.recorder.record({
      'event': 'failure',
      'reason': 'decode_refused',
      'generation': widget.generation,
      'providerSha256': widget.value.immutableKey,
      'attemptSha256': widget.value.attemptSha256,
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && widget.current()) widget.onFailure();
    });
    WidgetsBinding.instance.ensureVisualUpdate();
  }

  @override
  Widget build(BuildContext context) {
    final fixture = widget.value.fixture;
    return SizedBox(
      width: fixture.logicalSlotWidth,
      height: fixture.logicalSlotHeight,
      child: _info == null || _failed || !widget.current()
          ? const ColoredBox(color: AppColors.placeholder)
          : _TargetImage(
              image: _info!.image,
              fit: fixture.fit == 'contain' ? BoxFit.contain : BoxFit.cover,
              alignment: Alignment(fixture.alignmentX, fixture.alignmentY),
              current: () =>
                  mounted &&
                  widget.current() &&
                  (ModalRoute.of(context)?.isCurrent ?? false),
              draw: (frame) {
                widget.recorder.record({
                  'event': 'draw',
                  'frameNumber': frame,
                  'providerSha256': widget.value.immutableKey,
                  'attemptSha256': widget.value.attemptSha256,
                  'generation': widget.generation,
                  'firstRasterizedTargetFrame': null,
                  'reason': frame < 0
                      ? 'frame_unavailable'
                      : 'unverified_visibility',
                  'physicalPresentationVerified': false,
                });
                if (!_drawCompleted && widget.onDraw != null) {
                  _drawCompleted = true;
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (mounted && widget.current()) widget.onDraw!();
                  });
                  WidgetsBinding.instance.ensureVisualUpdate();
                }
              },
            ),
    );
  }

  @override
  void dispose() {
    if (_listener != null) _stream?.removeListener(_listener!);
    _info?.dispose();
    _info = null;
    // Only the exact provider and transfer are owned by this departed route.
    unawaited(widget.value.provider.evict());
    unawaited(
      widget.cache.release(widget.value, failed: _failed).catchError((_) {}),
    );
    widget.recorder.record({
      'event': 'ownership',
      'ownership': 'released',
      'generation': widget.generation,
      'providerSha256': widget.value.immutableKey,
      'attemptSha256': widget.value.attemptSha256,
    });
    super.dispose();
  }
}

class _TargetImage extends LeafRenderObjectWidget {
  const _TargetImage({
    required this.image,
    required this.fit,
    required this.alignment,
    required this.current,
    required this.draw,
  });
  final ui.Image image;
  final BoxFit fit;
  final Alignment alignment;
  final bool Function() current;
  final void Function(int) draw;
  @override
  RenderObject createRenderObject(BuildContext context) => _TargetRenderImage(
    image: image.clone(),
    fit: fit,
    alignment: alignment,
    current: current,
    draw: draw,
  );
  @override
  void updateRenderObject(
    BuildContext context,
    covariant _TargetRenderImage renderObject,
  ) {
    renderObject
      ..image = image.clone()
      ..fit = fit
      ..alignment = alignment
      ..current = current
      ..draw = draw;
  }
}

class _TargetRenderImage extends RenderImage {
  _TargetRenderImage({
    required super.image,
    required super.fit,
    required super.alignment,
    required this.current,
    required this.draw,
  });
  bool Function() current;
  void Function(int) draw;
  @override
  void paint(PaintingContext context, Offset offset) {
    if (!attached || image == null || !current() || size.isEmpty) return;
    super.paint(context, offset);
    draw(ui.PlatformDispatcher.instance.frameData.frameNumber);
  }
}
