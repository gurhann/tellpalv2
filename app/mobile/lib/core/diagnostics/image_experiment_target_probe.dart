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
    this.requireAbsentKeys = false,
    this.onRelease,
    this.onQualityBinding,
  });
  final ImageExperimentProvider value;
  final ImageExperimentImageCache cache;
  final ImageExperimentRecorder recorder;
  final int generation;
  final bool Function() current;
  final void Function() onDecoded, onFailure;
  final void Function()? onDraw;
  final bool requireAbsentKeys;
  final void Function(Future<void>)? onRelease;
  final void Function(Map<String, Object?>)? onQualityBinding;
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
  bool _resolving = false;
  bool _drawCompleted = false;
  Future<void>? _remembering;
  Size? _observedSize;
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_stream != null || _resolving) return;
    _resolving = true;
    if (widget.requireAbsentKeys) {
      unawaited(_resolveCold());
      return;
    }
    _resolve();
  }

  Future<void> _resolveCold() async {
    try {
      await widget.cache.requireAbsentKeys(widget.value);
      if (!mounted || !widget.current()) return;
      _resolve();
    } catch (_) {
      _failure();
    }
  }

  void _resolve() {
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
    _remembering = widget.cache
        .remember(widget.value, () => mounted && widget.current())
        .catchError((_) {
          widget.recorder.record({
            'event': 'failure',
            'reason': 'invalid_fixture',
            'generation': widget.generation,
          });
        });
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
              observeLayout: (size) {
                _observedSize = size;
              },
              draw: (frame) {
                widget.recorder.record({
                  'event': 'draw',
                  'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
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
                  final size = _observedSize;
                  final binding = <String, Object?>{
                    'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
                    'providerSha256': widget.value.immutableKey,
                    'attemptSha256': widget.value.attemptSha256,
                    'generation': widget.generation,
                    'observedSlotWidth': size?.width ?? 0.0,
                    'observedSlotHeight': size?.height ?? 0.0,
                    'observedDpr': MediaQuery.devicePixelRatioOf(context),
                    'actualDecodeWidth': _info!.image.width,
                    'actualDecodeHeight': _info!.image.height,
                  };
                  widget.recorder.record({...binding, 'event': 'layout'});
                  widget.onQualityBinding?.call(binding);
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
    // Completion includes the listener, renderer frame boundary and exact cache owner.
    final release = _releaseAfterFrame();
    widget.onRelease?.call(release);
    unawaited(release.catchError((_) {}));
    super.dispose();
  }

  Future<void> _releaseAfterFrame() async {
    try {
      await WidgetsBinding.instance.endOfFrame;
      await _remembering;
      await widget.value.provider.evict();
      await widget.cache.release(widget.value, failed: _failed);
      widget.recorder.record({
        'event': 'ownership',
        'ownership': 'released',
        'caseSha256': ImageExperimentRecorder.hash(widget.value.fixture.caseId),
        'generation': widget.generation,
        'providerSha256': widget.value.immutableKey,
        'attemptSha256': widget.value.attemptSha256,
      });
    } catch (_) {
      widget.recorder.record({
        'event': 'failure',
        'reason': 'cleanup_unavailable',
        'generation': widget.generation,
      });
      rethrow;
    }
  }
}

class _TargetImage extends LeafRenderObjectWidget {
  const _TargetImage({
    required this.image,
    required this.fit,
    required this.alignment,
    required this.current,
    required this.draw,
    required this.observeLayout,
  });
  final ui.Image image;
  final BoxFit fit;
  final Alignment alignment;
  final bool Function() current;
  final void Function(int) draw;
  final void Function(Size) observeLayout;
  @override
  RenderObject createRenderObject(BuildContext context) => _TargetRenderImage(
    image: image.clone(),
    fit: fit,
    alignment: alignment,
    current: current,
    draw: draw,
    observeLayout: observeLayout,
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
      ..draw = draw
      ..observeLayout = observeLayout;
  }
}

class _TargetRenderImage extends RenderImage {
  _TargetRenderImage({
    required super.image,
    required super.fit,
    required super.alignment,
    required this.current,
    required this.draw,
    required this.observeLayout,
  });
  bool Function() current;
  void Function(int) draw;
  void Function(Size) observeLayout;
  @override
  void performLayout() {
    super.performLayout();
    observeLayout(size);
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    if (!attached || image == null || !current() || size.isEmpty) return;
    super.paint(context, offset);
    draw(ui.PlatformDispatcher.instance.frameData.frameNumber);
  }
}
