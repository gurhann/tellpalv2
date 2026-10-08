import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../../diagnostics/library_diagnostics.dart';
import '../../../theme/library_theme.dart';
import '../../../network/library_image_cache.dart';
import '../../domain/entity/library_preview.dart';
import '../../../artwork/artwork_cubit.dart';

class PreviewCard extends StatelessWidget {
  const PreviewCard({
    super.key,
    required this.item,
    required this.artworkContext,
    required this.metadataAttempt,
    required this.lockedLabel,
    required this.retryArtworkLabel,
    required this.createArtwork,
    required this.cache,
    required this.diagnostics,
    this.isCatalogCurrent,
    this.onRefreshExpiredArtwork,
    this.onTap,
    this.onRefreshArtwork,
  });
  final Future<void> Function()? onTap;
  final Future<void> Function()? onRefreshArtwork;
  final LibraryPreview item;
  final LibraryContext artworkContext;
  final int metadataAttempt;
  final String lockedLabel;
  final String retryArtworkLabel;
  final ArtworkCubit Function() createArtwork;
  final LibraryImageCache Function() cache;
  final LibraryDiagnostics Function() diagnostics;
  final bool Function(ArtworkIdentity)? isCatalogCurrent;
  final void Function(int, {required bool automatic})? onRefreshExpiredArtwork;
  @override
  Widget build(BuildContext context) {
    final cover = MediaQuery.sizeOf(context).shortestSide >= 600
        ? (item.covers.tablet ?? item.covers.phone)
        : item.covers.phone;
    final identity = ArtworkIdentity(
      artworkContext,
      metadataAttempt,
      item.contentId,
      cover?.assetId,
    );
    return BlocProvider(
      key: ValueKey(identity),
      create: (_) => createArtwork()..start(identity, available: cover != null),
      child: _CardArtwork(
        item: item,
        cover: cover,
        identity: identity,
        lockedLabel: lockedLabel,
        retryArtworkLabel: retryArtworkLabel,
        cache: cache,
        diagnostics: diagnostics,
        isCatalogCurrent: isCatalogCurrent,
        onRefreshExpiredArtwork: onRefreshExpiredArtwork,
        onTap: onTap,
        onRefreshArtwork: onRefreshArtwork,
      ),
    );
  }
}

class _CardArtwork extends StatefulWidget {
  const _CardArtwork({
    required this.item,
    required this.cover,
    required this.identity,
    required this.lockedLabel,
    required this.retryArtworkLabel,
    required this.cache,
    required this.diagnostics,
    this.isCatalogCurrent,
    this.onRefreshExpiredArtwork,
    this.onTap,
    this.onRefreshArtwork,
  });
  final Future<void> Function()? onTap;
  final Future<void> Function()? onRefreshArtwork;
  final LibraryPreview item;
  final LibraryCover? cover;
  final ArtworkIdentity identity;
  final String lockedLabel;
  final String retryArtworkLabel;
  final LibraryImageCache Function() cache;
  final LibraryDiagnostics Function() diagnostics;
  final bool Function(ArtworkIdentity)? isCatalogCurrent;
  final void Function(int, {required bool automatic})? onRefreshExpiredArtwork;
  @override
  State<_CardArtwork> createState() => _CardArtworkState();
}

class _CardArtworkState extends State<_CardArtwork> {
  bool _navigating = false;
  Future<void> _open() async {
    if (_navigating || widget.onTap == null) return;
    _navigating = true;
    try {
      await widget.onTap!();
    } finally {
      _navigating = false;
    }
  }

  static int _nextOwner = 0;
  final int _owner = ++_nextOwner;
  LibraryPreview get item => widget.item;
  LibraryCover? get cover => widget.cover;
  ArtworkIdentity get identity => widget.identity;
  String get _cacheKey =>
      '${identity.context.accessIdentity}:${identity.context.language}:${identity.contentId}:${identity.assetId}';
  String _transferKey(int attempt) => LibraryImageCache.transferKey(
    _cacheKey,
    '${identity.catalogAttempt}:$_owner:$attempt',
  );

  bool _current(ArtworkCubit artwork, int attempt) =>
      mounted &&
      !artwork.isClosed &&
      artwork.state.identity == identity &&
      artwork.state.attempt == attempt &&
      artwork.state.status != ArtworkStatus.error;

  Future<void> _retryArtwork(BuildContext context) async {
    final artwork = context.read<ArtworkCubit>();
    if (widget.onRefreshArtwork != null) {
      try {
        await widget.cache().discardFailedTransfer(
          _transferKey(artwork.state.attempt),
        );
      } catch (error) {
        widget.diagnostics().error(error);
        return;
      }
      if (!context.mounted) return;
      await widget.onRefreshArtwork!();
      return;
    }
    final capturedAttempt = artwork.state.attempt;
    if (artwork.state.identity != identity ||
        artwork.state.status != ArtworkStatus.error ||
        !(widget.isCatalogCurrent?.call(identity) ?? false)) {
      return;
    }
    try {
      await widget.cache().discardFailedTransfer(_transferKey(capturedAttempt));
    } catch (error) {
      widget.diagnostics().error(error);
      return;
    }
    if (!context.mounted ||
        artwork.isClosed ||
        artwork.state.identity != identity ||
        artwork.state.attempt != capturedAttempt ||
        artwork.state.status != ArtworkStatus.error ||
        !(widget.isCatalogCurrent?.call(identity) ?? false)) {
      return;
    }
    if (!cover!.expiresAt.isAfter(DateTime.now().toUtc())) {
      widget.onRefreshExpiredArtwork?.call(
        identity.catalogAttempt,
        automatic: false,
      );
      return;
    }
    artwork.start(identity, available: true);
  }

  @override
  Widget build(BuildContext context) => ClipRRect(
    borderRadius: BorderRadius.circular(16),
    child: Stack(
      fit: StackFit.expand,
      children: [
        Semantics(
          container: true,
          label:
              '${item.title}, ${item.contentLanguage}${item.isLocked ? ', ${widget.lockedLabel}' : ''}',
          button: true,
          enabled: widget.onTap != null,
          onTap: widget.onTap == null ? null : _open,
          child: ExcludeSemantics(
            child: GestureDetector(
              onTap: widget.onTap == null ? null : _open,
              child: BlocBuilder<ArtworkCubit, ArtworkState>(
                builder: (context, artwork) {
                  if (cover == null || artwork.status == ArtworkStatus.error) {
                    return _ArtworkError(title: item.title);
                  }
                  final capturedAttempt = artwork.attempt;
                  final decodeWidth =
                      MediaQuery.sizeOf(context).shortestSide >= 600
                      ? 600
                      : 360;
                  return CachedNetworkImage(
                    key: ValueKey(_transferKey(capturedAttempt)),
                    imageUrl: cover!.url,
                    cacheManager: widget.cache(),
                    cacheKey: _transferKey(capturedAttempt),
                    memCacheWidth: decodeWidth,
                    // Server-verified thumbnails already fit 360x480/600x800.
                    fit: BoxFit.cover,
                    placeholder: (_, _) =>
                        const ColoredBox(color: AppColors.placeholder),
                    imageBuilder: (context, provider) {
                      scheduleMicrotask(() {
                        if (context.mounted) {
                          final artwork = context.read<ArtworkCubit>();
                          if (!_current(artwork, capturedAttempt) ||
                              artwork.state.status != ArtworkStatus.loading) {
                            return;
                          }
                          artwork.success(identity, capturedAttempt);
                          unawaited(
                            widget.cache().rememberValidated(
                              _cacheKey,
                              _transferKey(capturedAttempt),
                              isCurrent: () =>
                                  _current(artwork, capturedAttempt),
                            ),
                          );
                        }
                      });
                      // Share OctoImage's bounded decoder key instead of loading
                      // the same source again under an unresized provider key.
                      return Image(
                        image: ResizeImage.resizeIfNeeded(
                          decodeWidth,
                          null,
                          provider,
                        ),
                        fit: BoxFit.cover,
                        excludeFromSemantics: true,
                      );
                    },
                    errorWidget: (context, _, error) {
                      scheduleMicrotask(() {
                        if (!context.mounted) {
                          return;
                        }
                        final accepted = context.read<ArtworkCubit>().fail(
                          identity,
                          capturedAttempt,
                        );
                        if (accepted) {
                          widget.diagnostics().error(error);
                        }
                        if (accepted &&
                            widget.onRefreshArtwork == null &&
                            !cover!.expiresAt.isAfter(DateTime.now().toUtc())) {
                          widget.onRefreshExpiredArtwork?.call(
                            identity.catalogAttempt,
                            automatic: true,
                          );
                        }
                      });
                      return _ArtworkError(title: item.title);
                    },
                  );
                },
              ),
            ),
          ),
        ),
        if (item.isLocked)
          Positioned(
            left: 12,
            top: 12,
            child: ExcludeSemantics(
              child: Container(
                width: 32,
                height: 32,
                padding: const EdgeInsets.all(5),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: AppColors.placeholder, width: 2),
                ),
                child: SvgPicture.asset(
                  'assets/library/lock_glyph.svg',
                  colorFilter: const ColorFilter.mode(
                    AppColors.primary,
                    BlendMode.srcIn,
                  ),
                ),
              ),
            ),
          ),
        BlocSelector<ArtworkCubit, ArtworkState, bool>(
          selector: (state) => state.status == ArtworkStatus.error,
          builder: (context, failed) => failed && cover != null
              ? Positioned(
                  bottom: 0,
                  left: 0,
                  child: SizedBox(
                    width: 48,
                    height: 48,
                    child: IconButton(
                      tooltip: widget.retryArtworkLabel,
                      onPressed: () => _retryArtwork(context),
                      icon: const Icon(Icons.refresh, color: AppColors.ink),
                    ),
                  ),
                )
              : const SizedBox.shrink(),
        ),
      ],
    ),
  );
}

class _ArtworkError extends StatelessWidget {
  const _ArtworkError({required this.title});
  final String title;
  @override
  Widget build(BuildContext context) => ColoredBox(
    color: AppColors.placeholder,
    child: LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxHeight < 200) {
          final verticalPadding = (constraints.maxHeight * 0.32).clamp(
            0.0,
            40.0,
          );
          return Padding(
            padding: EdgeInsets.symmetric(
              horizontal: 12,
              vertical: verticalPadding,
            ),
            child: Center(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: AppTypography.label,
              ),
            ),
          );
        }
        return Padding(
          padding: const EdgeInsets.fromLTRB(12, 48, 12, 48),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.broken_image_outlined,
                color: AppColors.muted,
                size: 28,
              ),
              const SizedBox(height: 8),
              Text(
                title,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: AppTypography.label,
              ),
            ],
          ),
        );
      },
    ),
  );
}
