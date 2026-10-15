import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../../../../core/diagnostics/image_experiment_recorder.dart';
import '../../../../core/network/image_experiment_image_cache.dart';
import '../../../../story_manifest.dart';
import '../../../../story_reader_images.dart';

/// All three reader paths share the same six verified provider objects.
@lazySingleton
class ReaderResolutionImages implements StoryReaderImages {
  ReaderResolutionImages(this.configuration, this.cache, this.recorder) {
    _revocation = configuration.credentials.revocations.listen((_) => cancel());
  }
  final ImageExperimentConfiguration configuration;
  final ImageExperimentImageCache cache;
  final ImageExperimentRecorder recorder;
  final _geometryReady = Completer<void>();
  final _providers = <int, ImageExperimentProvider>{};
  final _admissions = <int, Future<ImageExperimentProvider>>{};
  final paintedPages = <int>{};
  StreamSubscription<void>? _revocation;
  String? _geometryHash;
  StoryReaderGeometry? _geometry;
  bool _cancelled = false;
  int _selected = 0, _selectionGeneration = 0;
  void Function()? onFailure;
  void Function(Map<String, Object?>)? onPainted;
  Map<String, Object?> get binding => {
    'generation': configuration.generation,
    'sessionSha256': ImageExperimentRecorder.hash(configuration.sessionId),
  };
  bool get active => !_cancelled && configuration.credentials.isActive;
  @override
  void selected(StoryPage page, int generation) {
    _selected = page.pageNumber - 1;
    _selectionGeneration = generation;
    recorder.record({
      ...binding,
      'event': 'reader_selection',
      'pageIndex': _selected,
      'selectionGeneration': generation,
    });
  }

  @override
  void observeLayout(StoryReaderGeometry geometry) {
    if (!active) return;
    final facts = <String, Object?>{
      'observedSlotWidth': geometry.slot.width,
      'observedSlotHeight': geometry.slot.height,
      'observedDpr': geometry.dpr,
      'textScaleFactor': geometry.textScale,
      'paddingLeft': geometry.padding.left,
      'paddingTop': geometry.padding.top,
      'paddingRight': geometry.padding.right,
      'paddingBottom': geometry.padding.bottom,
      'viewInsetLeft': geometry.viewInsets.left,
      'viewInsetTop': geometry.viewInsets.top,
      'viewInsetRight': geometry.viewInsets.right,
      'viewInsetBottom': geometry.viewInsets.bottom,
      'orientation': geometry.slot.width > geometry.slot.height
          ? 'landscape'
          : 'portrait',
    };
    if (facts.values.whereType<num>().any((v) => !v.isFinite) ||
        geometry.slot.isEmpty ||
        geometry.dpr <= 0 ||
        geometry.textScale <= 0) {
      _refuse();
      return;
    }
    final hash = ImageExperimentRecorder.hash(jsonEncode(facts));
    final fixture = configuration.cases.values.first;
    if ((_geometryHash != null && _geometryHash != hash) ||
        (fixture.profileRevision != 'reader_discover_r1' &&
            (fixture.logicalSlotWidth != geometry.slot.width ||
                fixture.logicalSlotHeight != geometry.slot.height ||
                fixture.dpr != geometry.dpr))) {
      _refuse();
      return;
    }
    if (_geometryHash != null) return;
    _geometryHash = hash;
    _geometry = geometry;
    recorder.record({
      ...binding,
      ...facts,
      'event': 'reader_geometry',
      'settingsSha256': hash,
    });
    if (!_geometryReady.isCompleted) _geometryReady.complete();
  }

  Future<ImageExperimentProvider> _admit(StoryPage page) async {
    await _geometryReady.future.timeout(const Duration(seconds: 10));
    if (!active ||
        page.pageNumber < 1 ||
        page.pageNumber > configuration.readerPageCount) {
      throw StateError('session_inactive');
    }
    return _admissions.putIfAbsent(page.pageNumber, () async {
      final value = cache.provider(
        configuration.cases['reader_page_${page.pageNumber}']!,
        'reader_session',
      );
      _providers[page.pageNumber] = value;
      await cache.requireAbsentKeys(value);
      if (!active) throw StateError('session_inactive');
      return value;
    });
  }

  @override
  Future<ImageProvider> prepare(StoryPage page) async =>
      (await _admit(page)).provider;
  @override
  Widget display(BuildContext context, StoryPage page) =>
      _ReaderResolutionImage(
        key: ValueKey(page.pageNumber),
        images: this,
        page: page,
        selected: () => active && _selected == page.pageNumber - 1,
      );
  void _refuse() {
    if (_cancelled) return;
    recorder.record({
      ...binding,
      'event': 'failure',
      'reason': 'layout_unavailable',
    });
    cancel();
    scheduleMicrotask(() => onFailure?.call());
  }

  void cancel() {
    _cancelled = true;
    if (!_geometryReady.isCompleted) {
      // Complete normally so every pending caller observes the same inactive guard.
      _geometryReady.complete();
    }
  }

  Future<void> snapshot(String phase) async {
    for (final value in _providers.values) {
      final key = await value.provider.obtainKey(ImageConfiguration.empty);
      final status = PaintingBinding.instance.imageCache.statusForKey(key);
      recorder.record({
        ...binding,
        'event': 'cache_state',
        'phase': phase,
        'caseSha256': ImageExperimentRecorder.hash(value.fixture.caseId),
        'providerSha256': value.immutableKey,
        'attemptSha256': value.attemptSha256,
        'decodedLiveCount': status.live ? 1 : 0,
        'decodedPendingCount': status.pending ? 1 : 0,
        'decodedKeepAliveCount': status.keepAlive ? 1 : 0,
        'decodedLruBytes': PaintingBinding.instance.imageCache.currentSizeBytes,
        'ownership': 'owned',
        'reason': 'internal_settlement_unavailable',
      });
    }
  }

  Future<void> evict() async {
    for (final value in _providers.values) {
      await value.provider.evict();
      await cache.release(value);
      await cache.removeFile(value.transferKey);
      await cache.removeFile(value.immutableKey);
    }
  }

  Future<void> close() async {
    cancel();
    await _revocation?.cancel();
  }
}

class _ReaderResolutionImage extends StatefulWidget {
  const _ReaderResolutionImage({
    super.key,
    required this.images,
    required this.page,
    required this.selected,
  });
  final ReaderResolutionImages images;
  final StoryPage page;
  final bool Function() selected;
  @override
  State<_ReaderResolutionImage> createState() => _ReaderResolutionImageState();
}

class _ReaderResolutionImageState extends State<_ReaderResolutionImage> {
  ImageStream? _stream;
  ImageStreamListener? _listener;
  ImageInfo? _info;
  ImageExperimentProvider? _value;
  bool _started = false, _painted = false;
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_started) return;
    _started = true;
    unawaited(_resolve());
  }

  Future<void> _resolve() async {
    try {
      final value = await widget.images._admit(widget.page);
      if (!mounted || !widget.selected()) return;
      _value = value;
      _stream = value.provider.resolve(createLocalImageConfiguration(context));
      _listener = ImageStreamListener((image, _) {
        if (!mounted || !widget.selected()) {
          image.dispose();
          return;
        }
        final fixture = value.fixture;
        final expectedWidth = widget.images.configuration.readerFullDecode
            ? fixture.width
            : fixture.geometry.decodeWidth;
        final expectedHeight = widget.images.configuration.readerFullDecode
            ? fixture.height
            : fixture.geometry.decodeHeight;
        if (!widget.images.cache.verified(value) ||
            image.image.width != expectedWidth ||
            image.image.height != expectedHeight) {
          image.dispose();
          widget.images._refuse();
          return;
        }
        _info?.dispose();
        _info = image.clone();
        image.dispose();
        widget.images.recorder.record({
          ...widget.images.binding,
          'event': 'decoded',
          'pageIndex': widget.page.pageNumber - 1,
          'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
          'providerSha256': value.immutableKey,
          'attemptSha256': value.attemptSha256,
          'actualDecodeWidth': _info!.image.width,
          'actualDecodeHeight': _info!.image.height,
        });
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && widget.selected()) setState(() {});
        });
        WidgetsBinding.instance.ensureVisualUpdate();
      }, onError: (Object _, StackTrace? stack) => widget.images._refuse());
      _stream!.addListener(_listener!);
    } catch (_) {
      if (mounted && widget.selected()) widget.images._refuse();
    }
  }

  @override
  Widget build(BuildContext context) => _info == null || !widget.selected()
      ? const Center(child: CircularProgressIndicator())
      : _ReaderPaint(
          image: _info!.image,
          current: () => mounted && widget.selected(),
          painted: () {
            if (_painted) return;
            _painted = true;
            widget.images.paintedPages.add(widget.page.pageNumber);
            widget.images.recorder.record({
              ...widget.images.binding,
              'event': 'page_paint',
              'pageIndex': widget.page.pageNumber - 1,
              'selectionGeneration': widget.images._selectionGeneration,
              'caseSha256': ImageExperimentRecorder.hash(
                _value!.fixture.caseId,
              ),
              'providerSha256': _value!.immutableKey,
              'attemptSha256': _value!.attemptSha256,
              'frameNumber':
                  ui.PlatformDispatcher.instance.frameData.frameNumber,
              'physicalPresentationVerified': false,
              'reason': 'unverified_visibility',
            });
            widget.images.onPainted?.call({
              'caseSha256': ImageExperimentRecorder.hash(
                _value!.fixture.caseId,
              ),
              'providerSha256': _value!.immutableKey,
              'attemptSha256': _value!.attemptSha256,
              'generation': widget.images.configuration.generation,
              'observedSlotWidth': widget.images._geometry!.slot.width,
              'observedSlotHeight': widget.images._geometry!.slot.height,
              'observedDpr': widget.images._geometry!.dpr,
              'actualDecodeWidth': _info!.image.width,
              'actualDecodeHeight': _info!.image.height,
            });
          },
        );
  @override
  void dispose() {
    if (_listener != null) _stream?.removeListener(_listener!);
    _info?.dispose();
    // Natural route exit drops listeners/clones only. QA eviction is a later phase.
    super.dispose();
  }
}

class _ReaderPaint extends LeafRenderObjectWidget {
  const _ReaderPaint({
    required this.image,
    required this.current,
    required this.painted,
  });
  final ui.Image image;
  final bool Function() current;
  final void Function() painted;
  @override
  RenderObject createRenderObject(BuildContext context) => _ReaderRenderImage(
    image: image.clone(),
    current: current,
    painted: painted,
  );
  @override
  void updateRenderObject(
    BuildContext context,
    covariant _ReaderRenderImage renderObject,
  ) {
    renderObject
      ..image = image.clone()
      ..current = current
      ..painted = painted;
  }
}

class _ReaderRenderImage extends RenderImage {
  _ReaderRenderImage({
    required super.image,
    required this.current,
    required this.painted,
  }) : super(fit: BoxFit.contain);
  bool Function() current;
  void Function() painted;
  @override
  void paint(PaintingContext context, Offset offset) {
    if (!attached || image == null || !current() || size.isEmpty) return;
    super.paint(context, offset);
    painted();
  }
}
