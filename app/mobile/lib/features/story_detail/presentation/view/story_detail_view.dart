import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../../../app/router.dart';
import '../../../../app/library_configuration.dart';
import '../../../../app/injection.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/theme/library_theme.dart';
import '../../../../core/presentation/widgets/reading_detail_artwork_frame.dart';
import '../../../../core/preview/domain/entity/library_preview.dart';
import '../../../../core/artwork/artwork_cubit.dart';
import '../../../../core/network/library_image_cache.dart';
import '../../../../core/diagnostics/library_diagnostics.dart';
import '../../../../core/preview/presentation/widgets/preview_card.dart';
import '../../domain/entity/story_detail.dart';
import '../cubit/story_detail_cubit.dart';
import '../cubit/story_detail_state.dart';
import '../story_detail_strings.dart';
import '../widgets/story_detail_hero.dart';

class StoryDetailView extends StatefulWidget {
  const StoryDetailView({super.key});
  @override
  State<StoryDetailView> createState() => _StoryDetailViewState();
}

class _StoryDetailViewState extends State<StoryDetailView> {
  bool _navigating = false;
  Future<void> _open(StoryDetailState state, int id) async {
    if (_navigating ||
        !state.visible ||
        state.status != StoryDetailStatus.loaded) {
      return;
    }
    _navigating = true;
    try {
      await context.router.push(
        StoryDetailRoute(
          categoryId: state.identity!.categoryId,
          contentId: id,
          uiLanguage: state.identity!.uiLanguage,
          contentLanguage: state.identity!.contentLanguage,
          experience: state.identity!.experience,
        ),
      );
    } finally {
      _navigating = false;
    }
  }

  @override
  Widget build(
    BuildContext context,
  ) => BlocBuilder<StoryDetailCubit, StoryDetailState>(
    builder: (context, state) {
      final strings = StoryDetailStrings(state.identity?.uiLanguage ?? 'tr');
      if (state.status != StoryDetailStatus.loaded) {
        return Scaffold(
          appBar: AppBar(
            leading: IconButton(
              style: IconButton.styleFrom(
                minimumSize: const Size(48, 48),
                padding: const EdgeInsets.all(12),
                visualDensity: VisualDensity.standard,
              ),
              tooltip: strings.back,
              onPressed: () => context.router.maybePop(),
              icon: const Icon(Icons.arrow_back),
            ),
          ),
          body: SafeArea(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (state.status == StoryDetailStatus.loading)
                      const CircularProgressIndicator(),
                    const SizedBox(height: 16),
                    Text(
                      state.status == StoryDetailStatus.loading
                          ? strings.loading
                          : state.status == StoryDetailStatus.unavailable
                          ? strings.unavailable
                          : state.failure?.kind == FailureKind.unauthorized ||
                                state.failure?.kind == FailureKind.forbidden
                          ? strings.admission
                          : strings.error,
                      style: AppTypography.description,
                      textAlign: TextAlign.center,
                    ),
                    if (state.status == StoryDetailStatus.error)
                      Padding(
                        padding: const EdgeInsets.only(top: 16),
                        child: FilledButton(
                          onPressed: () => context
                              .read<StoryDetailCubit>()
                              .load(state.identity!),
                          child: Text(strings.retry),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        );
      }
      final detail = state.detail!;
      return Scaffold(
        body: ListView.builder(
          key: PageStorageKey('detail-${state.identity}'),
          padding: EdgeInsets.only(
            bottom: MediaQuery.paddingOf(context).bottom + 24,
          ),
          itemCount:
              5 +
              detail.credits.length +
              (detail.recommendations.isEmpty ? 0 : 1),
          itemBuilder: (context, index) {
            if (index == 0) return _HeroArea(state: state, strings: strings);
            if (index == 1) {
              return _DeferredAction(detail: detail, strings: strings);
            }
            if (index == 2) return _Narration(state: state, strings: strings);
            if (index == 3) {
              return detail.summary == null || detail.summary!.trim().isEmpty
                  ? const SizedBox.shrink()
                  : Padding(
                      padding: const EdgeInsets.fromLTRB(16, 24, 16, 24),
                      child: Text(
                        detail.summary!,
                        style: AppTypography.description,
                      ),
                    );
            }
            if (index == 4) return const SizedBox(height: 16);
            if (index < 5 + detail.credits.length) {
              return _Credit(
                credit: detail.credits[index - 5],
                strings: strings,
              );
            }
            return _Recommendations(
              state: state,
              strings: strings,
              onOpen: (id) => _open(state, id),
            );
          },
        ),
      );
    },
  );
}

class _HeroArea extends StatelessWidget {
  const _HeroArea({required this.state, required this.strings});
  final StoryDetailState state;
  final StoryDetailStrings strings;
  @override
  Widget build(BuildContext context) {
    final detail = state.detail!;
    return ReadingDetailArtworkFrame(
      semanticLabel: detail.title,
      blur:
          state.visible &&
          detail.isLocked &&
          state.heroStatus == StoryHeroStatus.loaded,
      artworkBuilder: (context, geometry) => state.visible
          ? StoryDetailHero(
              key: ValueKey('${state.attempt}:${state.heroAttempt}'),
              state: state,
              strings: strings,
              fit: geometry.fit,
            )
          : const ColoredBox(color: AppColors.placeholder),
      overlays: [
        if (detail.isLocked)
          Positioned(
            top: MediaQuery.paddingOf(context).top + 16,
            right: 16,
            child: Semantics(
              container: true,
              label: strings.locked,
              child: Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: AppColors.canvas,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.lock_outline, color: AppColors.primary),
              ),
            ),
          ),
        Positioned(
          top: MediaQuery.paddingOf(context).top + 16,
          left: 16,
          child: Material(
            color: AppColors.canvas,
            shape: const CircleBorder(),
            child: IconButton(
              style: IconButton.styleFrom(
                minimumSize: const Size(48, 48),
                padding: const EdgeInsets.all(12),
                visualDensity: VisualDensity.standard,
              ),
              tooltip: strings.back,
              onPressed: () => context.router.maybePop(),
              icon: const Icon(Icons.arrow_back, color: AppColors.primary),
            ),
          ),
        ),
        Positioned(
          left: 16,
          right: 16,
          bottom: 8,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Align(
                  alignment: Alignment.bottomLeft,
                  child: detail.durationMinutes == null
                      ? const SizedBox.shrink()
                      : _Pill(
                          icon: Icons.schedule,
                          label: strings.minutes(detail.durationMinutes!),
                        ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Align(
                  alignment: Alignment.bottomRight,
                  child: detail.ageRange == null
                      ? const SizedBox.shrink()
                      : _Pill(
                          icon: Icons.child_care,
                          label: strings.age(detail.ageRange!),
                        ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.icon, required this.label});
  final IconData icon;
  final String label;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
    decoration: BoxDecoration(
      color: AppColors.canvas,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: AppColors.primary),
        const SizedBox(width: 8),
        Flexible(child: Text(label, style: AppTypography.label)),
      ],
    ),
  );
}

class _DeferredAction extends StatelessWidget {
  const _DeferredAction({required this.detail, required this.strings});
  final StoryDetail detail;
  final StoryDetailStrings strings;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
    child: Center(
      child: Semantics(
        button: true,
        enabled: false,
        label: detail.isLocked ? strings.premium : strings.start,
        hint: strings.deferred,
        child: ExcludeSemantics(
          child: Container(
            constraints: const BoxConstraints(minHeight: 48, maxWidth: 343),
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(48),
              gradient: const LinearGradient(
                colors: [AppColors.primary, AppColors.bannerEnd],
              ),
            ),
            child: Text(
              detail.isLocked ? strings.premium : strings.start,
              textAlign: TextAlign.center,
              style: AppTypography.section.copyWith(color: AppColors.white),
            ),
          ),
        ),
      ),
    ),
  );
}

class _Narration extends StatelessWidget {
  const _Narration({required this.state, required this.strings});
  final StoryDetailState state;
  final StoryDetailStrings strings;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 12),
    child: Center(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: AppColors.placeholder,
          border: Border.all(color: AppColors.primary),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                ExcludeSemantics(
                  child: SvgPicture.asset(
                    'assets/library/story_detail_narration_wave.svg',
                    width: 32,
                    height: 32,
                    colorFilter: const ColorFilter.mode(
                      AppColors.storyNarrationAccent,
                      BlendMode.srcIn,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Flexible(
                  child: ExcludeSemantics(
                    child: Text(
                      strings.narration,
                      style: AppTypography.section,
                    ),
                  ),
                ),
              ],
            ),
            Semantics(
              label: strings.narration,
              child: Switch(
                value: state.narration,
                onChanged: context.read<StoryDetailCubit>().toggleNarration,
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _Credit extends StatelessWidget {
  const _Credit({required this.credit, required this.strings});
  final StoryCredit credit;
  final StoryDetailStrings strings;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
    child: LayoutBuilder(
      builder: (context, box) =>
          box.maxWidth < 420 && MediaQuery.textScalerOf(context).scale(1) > 1.1
          ? Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _CreditRoleLabel(credit: credit, strings: strings),
                Text(
                  credit.displayName,
                  style: AppTypography.description.copyWith(
                    color: AppColors.primary,
                  ),
                ),
              ],
            )
          : Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: _CreditRoleLabel(credit: credit, strings: strings),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    credit.displayName,
                    textAlign: TextAlign.end,
                    style: AppTypography.description.copyWith(
                      color: AppColors.primary,
                    ),
                  ),
                ),
              ],
            ),
    ),
  );
}

class _CreditRoleLabel extends StatelessWidget {
  const _CreditRoleLabel({required this.credit, required this.strings});
  final StoryCredit credit;
  final StoryDetailStrings strings;
  @override
  Widget build(BuildContext context) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      ExcludeSemantics(
        child: SvgPicture.asset(
          'assets/library/story_detail_${switch (credit.role) {
            'AUTHOR' => 'pen',
            'NARRATOR' => 'microphone',
            'ILLUSTRATOR' => 'palette',
            _ => 'music_notes',
          }}.svg',
          width: 24,
          height: 24,
          colorFilter: const ColorFilter.mode(
            AppColors.primary,
            BlendMode.srcIn,
          ),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: Text(
          strings.role(credit.role),
          style: AppTypography.description,
        ),
      ),
    ],
  );
}

class _Recommendations extends StatelessWidget {
  const _Recommendations({
    required this.state,
    required this.strings,
    required this.onOpen,
  });
  final StoryDetailState state;
  final StoryDetailStrings strings;
  final Future<void> Function(int) onOpen;
  @override
  Widget build(BuildContext context) {
    final detail = state.detail!;
    final height = MediaQuery.sizeOf(context).height * .230 + 16;
    final artworkContext = LibraryContext(
      state.identity!.uiLanguage,
      libraryGetIt<LibraryConfiguration>().context.accessIdentity,
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
          child: Text(strings.similar, style: AppTypography.section),
        ),
        SizedBox(
          height: height,
          child: ListView.separated(
            key: PageStorageKey('recommendations-${detail.contentId}'),
            padding: const EdgeInsets.symmetric(horizontal: 16),
            scrollDirection: Axis.horizontal,
            itemCount: detail.recommendations.length,
            separatorBuilder: (_, _) => const SizedBox(width: 16),
            itemBuilder: (context, index) => SizedBox(
              width: height * .75,
              child: PreviewCard(
                item: detail.recommendations[index],
                artworkContext: artworkContext,
                // Retained descriptors keep their owner until fresh metadata commits.
                metadataAttempt: state.metadataAttempt,
                lockedLabel: strings.locked,
                retryArtworkLabel: strings.retryArtwork,
                createArtwork: () => libraryGetIt<ArtworkCubit>(),
                cache: () => libraryGetIt<LibraryImageCache>(),
                diagnostics: () => libraryGetIt<LibraryDiagnostics>(),
                onTap: () => onOpen(detail.recommendations[index].contentId),
                onRefreshArtwork: () => context.read<StoryDetailCubit>().load(
                  state.identity!,
                  artworkOnly: true,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
