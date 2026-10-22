import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../../../../core/presentation/reading_artwork_capability.dart';
import '../../../../core/theme/library_theme.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import 'reading_image_consumer_cubit.dart';
import 'reading_image_consumer_state.dart';

/// Fresh admission'dan önce provider kurulmaz; yalnız mevcut attempt çizilebilir.
class ReadingImageConsumerWidget extends StatefulWidget {
  const ReadingImageConsumerWidget({super.key, required this.request});
  final ReadingArtworkRequest request;
  @override
  State<ReadingImageConsumerWidget> createState() =>
      _ReadingImageConsumerWidgetState();
}

class _ReadingImageConsumerWidgetState extends State<ReadingImageConsumerWidget>
    with WidgetsBindingObserver {
  late final ReadingImageConsumerCubit _cubit;
  (int, String, int, BoxFit, double, double, double)? _slot;
  ImageProvider? _provider;
  String? _key;
  Completer<void>? _providerReleased;
  bool _foreground = true;
  bool get _eligible =>
      mounted &&
      _foreground &&
      widget.request.visible &&
      (ModalRoute.of(context)?.isCurrent ?? true);
  @override
  void initState() {
    super.initState();
    _foreground =
        WidgetsBinding.instance.lifecycleState == null ||
        WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed;
    WidgetsBinding.instance.addObserver(this);
    _cubit = libraryGetIt<ReadingImageConsumerCubit>();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    setState(() => _foreground = state == AppLifecycleState.resumed);
    if (!_foreground) {
      _evict();
      _cubit.suspend();
    }
  }

  @override
  void didUpdateWidget(covariant ReadingImageConsumerWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.request.visible) _cubit.suspend();
  }

  void _evict() {
    final provider = _provider;
    final released = _providerReleased;
    _provider = null;
    _providerReleased = null;
    _key = null;
    if (provider != null) {
      unawaited(() async {
        await provider.evict();
        WidgetsBinding.instance.scheduleFrame();
        await WidgetsBinding.instance.endOfFrame;
        if (released != null && !released.isCompleted) released.complete();
      }());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _evict();
    unawaited(_cubit.close());
    super.dispose();
  }

  void _observeDecode(ImageProvider provider, int attempt) {
    final done = Completer<void>();
    _cubit.ownDecode(done.future);
    final stream = provider.resolve(ImageConfiguration.empty);
    late ImageStreamListener listener;
    void finish() {
      stream.removeListener(listener);
      if (!done.isCompleted) done.complete();
    }

    listener = ImageStreamListener((info, _) {
      info.dispose();
      finish();
    }, onError: (Object error, StackTrace? stack) => finish());
    stream.addListener(listener);
  }

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final request = widget.request;
      final routeActive = ModalRoute.of(context)?.isCurrent ?? true;
      final next = (
        request.contentId,
        request.language,
        request.pageNumber,
        request.fit,
        constraints.maxWidth,
        constraints.maxHeight,
        MediaQuery.devicePixelRatioOf(context),
      );
      if (next != _slot && request.visible && routeActive && _foreground) {
        _slot = next;
        scheduleMicrotask(() {
          if (_eligible && _slot == next) {
            unawaited(
              _cubit.load(
                contentId: next.$1,
                language: next.$2,
                role: next.$3 == 0
                    ? ReadingImageRole.readingDetail
                    : ReadingImageRole.readingPage,
                pageNumber: next.$3,
                maxDecodeWidth: MediaQuery.sizeOf(context).shortestSide >= 600
                    ? 1600
                    : 1080,
                width: next.$5,
                height: next.$6,
                dpr: next.$7,
                fit: next.$4 == BoxFit.contain
                    ? ReadingImageFit.contain
                    : ReadingImageFit.cover,
              ),
            );
          }
        });
      }
      return BlocBuilder<ReadingImageConsumerCubit, ReadingImageConsumerState>(
        bloc: _cubit,
        builder: (context, state) {
          if (!request.visible || !routeActive || !_foreground) {
            _evict();
            if (state.status != ReadingImageConsumerStatus.suspended) {
              scheduleMicrotask(() {
                if (mounted && _cubit.state.attempt == state.attempt) {
                  _cubit.suspend();
                }
              });
            }
            return const ColoredBox(color: AppColors.placeholder);
          }
          if (!_cubit.current(state.attempt)) {
            _evict();
            if (state.status == ReadingImageConsumerStatus.error ||
                state.status == ReadingImageConsumerStatus.suspended) {
              return ColoredBox(
                color: AppColors.placeholder,
                child: Center(
                  child: TextButton(
                    onPressed: () {
                      if (_eligible) unawaited(_cubit.retry());
                    },
                    child: request.retryLabel == null
                        ? const Icon(Icons.refresh)
                        : Text(request.retryLabel!),
                  ),
                ),
              );
            }
            return const ColoredBox(color: AppColors.placeholder);
          }
          final selection = state.selection!;
          final size = selection.geometry;
          if (_key != state.transferKey) {
            _evict();
            _key = state.transferKey;
            _provider = ResizeImage.resizeIfNeeded(
              size.decodeWidth,
              size.decodeHeight,
              CachedNetworkImageProvider(
                state.url!,
                cacheKey: state.transferKey,
                cacheManager: _cubit.cache,
              ),
            );
            _providerReleased = Completer<void>();
            _cubit.ownProvider(_providerReleased!.future);
            _observeDecode(_provider!, state.attempt);
          }
          final attempt = state.attempt;
          return CachedNetworkImage(
            key: ValueKey(state.transferKey),
            imageUrl: state.url!,
            cacheKey: state.transferKey,
            cacheManager: _cubit.cache,
            memCacheWidth: size.decodeWidth,
            memCacheHeight: size.decodeHeight,
            fit: request.fit,
            fadeInDuration: Duration.zero,
            fadeOutDuration: Duration.zero,
            useOldImageOnUrlChange: false,
            placeholder: (_, _) =>
                const ColoredBox(color: AppColors.placeholder),
            imageBuilder: (context, provider) {
              final bounded = ResizeImage.resizeIfNeeded(
                size.decodeWidth,
                size.decodeHeight,
                provider,
              );
              scheduleMicrotask(() {
                if (_eligible && _cubit.current(attempt)) {
                  _cubit.decoded(attempt);
                  request.onLoaded();
                }
              });
              return _eligible && _cubit.current(attempt)
                  ? Image(
                      image: bounded,
                      fit: request.fit,
                      excludeFromSemantics: true,
                    )
                  : const SizedBox.shrink();
            },
            errorWidget: (context, _, error) {
              scheduleMicrotask(() {
                if (_eligible && _cubit.current(attempt)) {
                  _cubit.failed(attempt);
                  request.onError();
                }
              });
              return const ColoredBox(color: AppColors.placeholder);
            },
          );
        },
      );
    },
  );
}
