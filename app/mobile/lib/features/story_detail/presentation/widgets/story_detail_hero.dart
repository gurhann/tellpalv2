import 'dart:async';
import 'dart:math' as math;
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../../../../app/library_configuration.dart';
import '../../../../core/network/story_detail_image_cache.dart';
import '../../../../core/network/library_image_cache.dart';
import '../../../../core/theme/library_theme.dart';
import '../../../../core/presentation/reading_artwork_capability.dart';
import '../../domain/entity/story_detail.dart';
import '../cubit/story_detail_cubit.dart';
import '../cubit/story_detail_state.dart';
import '../story_detail_strings.dart';

/// Aspect-preserving decoder targets stay below the 16 MiB pixel ceiling.
(int, int) storyHeroDecodeSize(StoryHero hero, bool tablet) {
  final scale = math.min(
    1.0,
    math.min(
      math.min((tablet ? 1600 : 1080) / hero.width, 2048 / hero.height),
      math.sqrt(4194304 / (hero.width * hero.height)),
    ),
  );
  return (
    math.max(1, (hero.width * scale).floor()),
    math.max(1, (hero.height * scale).floor()),
  );
}

class StoryDetailHero extends StatefulWidget {
  const StoryDetailHero({
    super.key,
    required this.state,
    required this.strings,
    this.fit = BoxFit.cover,
  });
  final StoryDetailState state;
  final StoryDetailStrings strings;
  final BoxFit fit;
  @override
  State<StoryDetailHero> createState() => _StoryDetailHeroState();
}

class _StoryDetailHeroState extends State<StoryDetailHero> {
  ImageProvider? _provider;
  String? _transfer;
  StoryDetailImageCache? _cache;
  late final bool _recycledLoadedConsumer;

  @override
  void initState() {
    super.initState();
    _recycledLoadedConsumer = widget.state.heroStatus == StoryHeroStatus.loaded;
    if (_recycledLoadedConsumer) {
      final previousAttempt = widget.state.heroAttempt;
      scheduleMicrotask(() {
        if (!mounted) return;
        context.read<StoryDetailCubit>().restartArtworkConsumer(
          previousAttempt,
        );
      });
    }
  }

  void _releaseTransfer(String key, {bool failed = false}) {
    unawaited(
      _cache!.discardFailedTransfer(key, failed: failed).catchError((Object _) {
        // Failed cleanup retains physical ownership for admission/close retry.
        // The rendered artwork error already reports the failed image to the user.
      }),
    );
  }

  @override
  void dispose() {
    final provider = _provider;
    if (provider != null) unawaited(provider.evict());
    final key = _transfer;
    if (key != null) _releaseTransfer(key);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state, detail = state.detail!, hero = detail.hero;
    final strings = widget.strings;
    final capability = ReadingArtworkCapability.maybeOf(context);
    if (capability != null) {
      return capability.builder(
        context,
        ReadingArtworkRequest(
          contentId: detail.contentId,
          language: detail.contentLanguage,
          fit: widget.fit,
          visible: state.visible,
          attempt: state.heroAttempt,
          retryLabel: strings.retryArtwork,
          onLoaded: () {
            if (context.mounted) {
              context.read<StoryDetailCubit>().artworkLoaded(state.heroAttempt);
            }
          },
          onError: () {},
        ),
      );
    }
    if (!state.visible) return const ColoredBox(color: AppColors.placeholder);
    if (hero == null || state.heroStatus == StoryHeroStatus.error) {
      return ColoredBox(
        color: AppColors.placeholder,
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.broken_image_outlined,
                color: AppColors.muted,
                size: 32,
              ),
              const SizedBox(height: 12),
              Flexible(
                child: ExcludeSemantics(
                  // The surrounding hero header already names the complete title.
                  child: Text(
                    detail.title,
                    style: AppTypography.section,
                    textAlign: TextAlign.center,
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(strings.artworkError, style: AppTypography.description),
              TextButton(
                style: TextButton.styleFrom(minimumSize: const Size(48, 48)),
                onPressed: state.recoveringArtwork
                    ? null
                    : () => context.read<StoryDetailCubit>().load(
                        state.identity!,
                        artworkOnly: true,
                      ),
                child: Text(strings.retryArtwork),
              ),
            ],
          ),
        ),
      );
    }
    // Readable metadata stays on screen while its fresh hero descriptor renews.
    // No provider may retry the retained URL during this metadata phase.
    if (state.recoveringArtwork || _recycledLoadedConsumer) {
      return const ColoredBox(color: AppColors.placeholder);
    }
    _cache = libraryGetIt<StoryDetailImageCache>();
    final account = libraryGetIt<LibraryConfiguration>().context.accessIdentity;
    final immutable =
        '$account:${detail.contentLanguage}:${hero.assetId}:${hero.checksumSha256}';
    final transfer = LibraryImageCache.transferKey(
      immutable,
      '${state.attempt}:${state.heroAttempt}',
    );
    _transfer = transfer;
    final size = storyHeroDecodeSize(
      hero,
      MediaQuery.sizeOf(context).shortestSide >= 600,
    );
    final attempt = state.heroAttempt;
    // Own the exact OctoImage decoder key while source bytes/codec are pending.
    // Waiting until imageBuilder would miss disposal before decode completes.
    _provider = ResizeImage.resizeIfNeeded(
      size.$1,
      size.$2,
      CachedNetworkImageProvider(
        hero.url,
        cacheKey: transfer,
        cacheManager: _cache,
      ),
    );
    return CachedNetworkImage(
      key: ValueKey(transfer),
      imageUrl: hero.url,
      cacheKey: transfer,
      cacheManager: _cache,
      memCacheWidth: size.$1,
      memCacheHeight: size.$2,
      fit: widget.fit,
      placeholder: (_, _) => const ColoredBox(color: AppColors.placeholder),
      imageBuilder: (context, provider) {
        // Match OctoImage's admitted decoder key; the unresized key would start
        // another source load and bypass this hero's decoded pixel target.
        final bounded = ResizeImage.resizeIfNeeded(size.$1, size.$2, provider);
        _provider = bounded;
        scheduleMicrotask(() {
          if (!context.mounted) return;
          final cubit = context.read<StoryDetailCubit>();
          if (cubit.state.heroAttempt != attempt ||
              !cubit.state.visible ||
              cubit.state.recoveringArtwork ||
              cubit.state.status != StoryDetailStatus.loaded ||
              cubit.state.heroStatus != StoryHeroStatus.loading) {
            return;
          }
          cubit.artworkLoaded(attempt);
          unawaited(
            _cache!.rememberValidated(
              immutable,
              transfer,
              isCurrent: () =>
                  mounted &&
                  !cubit.isClosed &&
                  cubit.state.heroAttempt == attempt &&
                  cubit.state.visible &&
                  !cubit.state.recoveringArtwork &&
                  cubit.state.heroStatus == StoryHeroStatus.loaded,
            ),
          );
        });
        return Image(
          image: bounded,
          fit: widget.fit,
          excludeFromSemantics: true,
        );
      },
      errorWidget: (context, _, error) {
        scheduleMicrotask(() {
          if (context.mounted) {
            final cubit = context.read<StoryDetailCubit>();
            if (cubit.state.heroAttempt == attempt &&
                !cubit.state.recoveringArtwork &&
                cubit.state.status == StoryDetailStatus.loaded &&
                cubit.state.heroStatus != StoryHeroStatus.loaded) {
              cubit.artworkFailed(attempt);
              _releaseTransfer(transfer, failed: true);
            }
          }
        });
        return const ColoredBox(color: AppColors.placeholder);
      },
    );
  }
}
