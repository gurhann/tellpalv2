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
  });
  final StoryDetailState state;
  final StoryDetailStrings strings;
  @override
  State<StoryDetailHero> createState() => _StoryDetailHeroState();
}

class _StoryDetailHeroState extends State<StoryDetailHero> {
  ImageProvider? _provider;
  String? _transfer;
  StoryDetailImageCache? _cache;
  @override
  void dispose() {
    final provider = _provider;
    if (provider != null) unawaited(provider.evict());
    final key = _transfer;
    if (key != null) unawaited(_cache!.discardFailedTransfer(key));
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state, detail = state.detail!, hero = detail.hero;
    final strings = widget.strings;
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
              Text(
                detail.title,
                style: AppTypography.section,
                textAlign: TextAlign.center,
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
    return CachedNetworkImage(
      key: ValueKey(transfer),
      imageUrl: hero.url,
      cacheKey: transfer,
      cacheManager: _cache,
      memCacheWidth: size.$1,
      memCacheHeight: size.$2,
      fit: BoxFit.cover,
      placeholder: (_, _) => const ColoredBox(color: AppColors.placeholder),
      imageBuilder: (context, provider) {
        _provider = provider;
        scheduleMicrotask(() {
          if (!context.mounted) return;
          final cubit = context.read<StoryDetailCubit>();
          if (cubit.state.heroAttempt != attempt || !cubit.state.visible) {
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
                  cubit.state.visible,
            ),
          );
        });
        return Image(
          image: provider,
          fit: BoxFit.cover,
          excludeFromSemantics: true,
        );
      },
      errorWidget: (context, _, error) {
        scheduleMicrotask(() {
          if (context.mounted) {
            context.read<StoryDetailCubit>().artworkFailed(attempt);
          }
        });
        return const ColoredBox(color: AppColors.placeholder);
      },
    );
  }
}
