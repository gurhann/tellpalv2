import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../../../../core/artwork/artwork_cubit.dart';
import '../../../../core/diagnostics/library_diagnostics.dart';
import '../../../../core/network/library_image_cache.dart';
import '../../../../core/preview/presentation/widgets/preview_card.dart';
import '../../domain/entity/library_catalog.dart';
import '../cubit/library_cubit.dart';
import '../cubit/library_state.dart';
import '../library_strings.dart';

class LibraryCard extends StatelessWidget {
  const LibraryCard({
    super.key,
    required this.item,
    required this.state,
    required this.strings,
    this.onTap,
    this.onRefreshArtwork,
  });
  final LibraryPreview item;
  final LibraryState state;
  final LibraryStrings strings;
  final Future<void> Function()? onTap;
  final Future<void> Function()? onRefreshArtwork;

  @override
  Widget build(BuildContext context) => PreviewCard(
    item: item,
    artworkContext: state.context,
    metadataAttempt: state.attempt,
    lockedLabel: strings.locked,
    retryArtworkLabel: strings.retryArtwork,
    createArtwork: () => libraryGetIt<ArtworkCubit>(),
    cache: () => libraryGetIt<LibraryImageCache>(),
    diagnostics: () => libraryGetIt<LibraryDiagnostics>(),
    onTap: onTap,
    onRefreshArtwork: onRefreshArtwork,
    isCatalogCurrent: (identity) {
      final catalog = context.read<LibraryCubit>();
      return !catalog.isClosed &&
          catalog.state.context == identity.context &&
          catalog.state.attempt == identity.catalogAttempt;
    },
    onRefreshExpiredArtwork: (attempt, {required automatic}) => context
        .read<LibraryCubit>()
        .refreshExpiredArtwork(attempt, automatic: automatic),
  );
}
