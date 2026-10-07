import 'package:flutter/material.dart';
import 'package:auto_route/auto_route.dart';
import '../../../../app/router.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../../../core/theme/library_theme.dart';
import '../../domain/entity/library_catalog.dart';
import '../cubit/library_cubit.dart';
import '../cubit/library_state.dart';
import '../library_strings.dart';
import '../widgets/library_card.dart';

class LibraryView extends StatefulWidget {
  const LibraryView({super.key, this.detailEnabled = false});
  final bool detailEnabled;
  @override
  State<LibraryView> createState() => _LibraryViewState();
}

class _LibraryViewState extends State<LibraryView> {
  bool _navigating = false;
  Future<void> _open(
    LibraryState state,
    LibraryRow row,
    LibraryPreview item,
  ) async {
    if (_navigating ||
        !widget.detailEnabled ||
        state.status != LibraryStatus.loaded) {
      return;
    }
    _navigating = true;
    try {
      await context.router.push(
        StoryDetailRoute(
          categoryId: row.categoryId,
          contentId: item.contentId,
          uiLanguage: state.context.language,
          contentLanguage: item.contentLanguage,
          experience: item.experienceType,
        ),
      );
    } finally {
      _navigating = false;
    }
  }

  @override
  Widget build(BuildContext context) => BlocBuilder<LibraryCubit, LibraryState>(
    builder: (context, state) {
      final strings = LibraryStrings(state.context.language);
      return Scaffold(
        bottomNavigationBar: _LibraryNavigation(strings: strings),
        body: SafeArea(
          bottom: false,
          child: ListView.builder(
            key: const PageStorageKey('library-scroll'),
            padding: const EdgeInsets.only(bottom: 24),
            itemCount:
                2 +
                (state.status == LibraryStatus.loaded
                    ? state.catalog!.rows.length
                    : 1),
            itemBuilder: (context, index) {
              if (index == 0) {
                return _LibraryHeader(
                  strings: strings,
                  subscriber: state.catalog?.audience == 'SUBSCRIBER',
                );
              }
              if (index == 1) return const SizedBox(height: 24);
              if (state.status == LibraryStatus.loaded) {
                return _LibraryRow(
                  row: state.catalog!.rows[index - 2],
                  state: state,
                  strings: strings,
                  onOpen: widget.detailEnabled
                      ? (item) =>
                            _open(state, state.catalog!.rows[index - 2], item)
                      : null,
                );
              }
              return _CatalogOutcome(state: state, strings: strings);
            },
          ),
        ),
      );
    },
  );
}

class _LibraryHeader extends StatelessWidget {
  const _LibraryHeader({required this.strings, required this.subscriber});
  final LibraryStrings strings;
  final bool subscriber;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 24, 16, 0),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(strings.welcome, style: AppTypography.greeting),
        const SizedBox(height: 16),
        if (!subscriber) ...[
          _DisabledControl(
            label: strings.premium,
            child: Container(
              height: 42,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(8),
                gradient: const LinearGradient(
                  colors: [AppColors.bannerStart, AppColors.bannerEnd],
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  SvgPicture.asset(
                    'assets/library/premium_start.svg',
                    width: 22,
                    height: 18,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      strings.premium,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.section.copyWith(
                        color: AppColors.white,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
        ],
        _DisabledControl(
          label: strings.search,
          child: Container(
            height: 40,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              border: Border.all(color: AppColors.placeholder),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                SvgPicture.asset(
                  'assets/library/icon_search.svg',
                  width: 24,
                  height: 24,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    strings.search,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.description.copyWith(
                      color: AppColors.subtle,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    ),
  );
}

class _DisabledControl extends StatelessWidget {
  const _DisabledControl({required this.label, required this.child});
  final String label;
  final Widget child;
  @override
  Widget build(BuildContext context) => Semantics(
    container: true,
    label: label,
    button: true,
    enabled: false,
    child: ExcludeSemantics(
      child: SizedBox(height: 48, child: Center(child: child)),
    ),
  );
}

class _LibraryRow extends StatelessWidget {
  const _LibraryRow({
    required this.row,
    required this.state,
    required this.strings,
    this.onOpen,
  });
  final Future<void> Function(LibraryPreview)? onOpen;
  final LibraryRow row;
  final LibraryState state;
  final LibraryStrings strings;
  @override
  Widget build(BuildContext context) {
    final height = MediaQuery.sizeOf(context).height * .230 + 16;
    final width = height * .75;
    return Padding(
      padding: const EdgeInsets.only(bottom: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Expanded(child: Text(row.name, style: AppTypography.section)),
                const SizedBox(width: 8),
                _DisabledControl(
                  label: '${strings.all}: ${row.name}',
                  child: Row(
                    children: [
                      Text(
                        strings.all,
                        style: AppTypography.description.copyWith(
                          color: AppColors.primary,
                        ),
                      ),
                      const Icon(Icons.arrow_forward, color: AppColors.primary),
                    ],
                  ),
                ),
              ],
            ),
          ),
          if (row.description != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: Text(row.description!, style: AppTypography.description),
            ),
          SizedBox(
            height: height,
            child: ListView.separated(
              key: PageStorageKey('row-${row.categoryId}'),
              padding: const EdgeInsets.symmetric(horizontal: 16),
              scrollDirection: Axis.horizontal,
              itemCount: row.items.length,
              separatorBuilder: (_, _) => const SizedBox(width: 16),
              itemBuilder: (context, index) => SizedBox(
                width: width,
                child: LibraryCard(
                  key: ValueKey(
                    '${state.context.accessIdentity}:${state.context.language}:${state.attempt}:${row.items[index].contentId}',
                  ),
                  item: row.items[index],
                  state: state,
                  strings: strings,
                  onTap: onOpen == null
                      ? null
                      : () => onOpen!(row.items[index]),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CatalogOutcome extends StatelessWidget {
  const _CatalogOutcome({required this.state, required this.strings});
  final LibraryState state;
  final LibraryStrings strings;
  @override
  Widget build(BuildContext context) {
    if (state.status == LibraryStatus.loading) {
      return Semantics(
        label: strings.loading,
        liveRegion: true,
        child: ExcludeSemantics(
          child: ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: 2,
            itemBuilder: (context, index) => const _SkeletonRow(),
          ),
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          Text(
            state.status == LibraryStatus.empty ? strings.empty : strings.error,
            style: AppTypography.description,
            textAlign: TextAlign.center,
          ),
          if (state.status == LibraryStatus.error)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: SizedBox(
                height: 48,
                child: FilledButton(
                  onPressed: () => context.read<LibraryCubit>().load(),
                  child: Text(strings.retry),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SkeletonRow extends StatelessWidget {
  const _SkeletonRow();
  @override
  Widget build(BuildContext context) {
    final height = MediaQuery.sizeOf(context).height * .230 + 16;
    return Padding(
      padding: const EdgeInsets.only(bottom: 24),
      child: Column(
        children: [
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 12, 120, 16),
            child: SizedBox(
              height: 24,
              width: double.infinity,
              child: ColoredBox(color: AppColors.placeholder),
            ),
          ),
          SizedBox(
            height: height,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: 5,
              separatorBuilder: (_, _) => const SizedBox(width: 16),
              itemBuilder: (context, index) => Container(
                width: height * .75,
                decoration: BoxDecoration(
                  color: AppColors.placeholder,
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _LibraryNavigation extends StatelessWidget {
  const _LibraryNavigation({required this.strings});
  final LibraryStrings strings;
  @override
  Widget build(BuildContext context) => ColoredBox(
    color: AppColors.canvas,
    child: SafeArea(
      top: false,
      child: Row(
        children: [
          _NavigationItem(
            name: strings.library,
            asset: 'icon_navigate_bar_read.svg',
            selected: true,
          ),
          _NavigationItem(
            name: strings.sleep,
            asset: 'icon_navigate_bar_rest.svg',
            selected: false,
          ),
          _NavigationItem(
            name: strings.profile,
            asset: 'icon_navigate_bar_profile.svg',
            selected: false,
          ),
        ],
      ),
    ),
  );
}

class _NavigationItem extends StatelessWidget {
  const _NavigationItem({
    required this.name,
    required this.asset,
    required this.selected,
  });
  final String name;
  final String asset;
  final bool selected;
  @override
  Widget build(BuildContext context) => Expanded(
    child: Semantics(
      label: name,
      button: true,
      selected: selected,
      enabled: selected,
      onTap: selected ? () {} : null,
      child: ExcludeSemantics(
        child: InkWell(
          onTap: selected ? () {} : null,
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 72),
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SvgPicture.asset(
                    'assets/library/$asset',
                    width: 28,
                    height: 28,
                    colorFilter: ColorFilter.mode(
                      selected ? AppColors.primary : AppColors.subtle,
                      BlendMode.srcIn,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    name,
                    style: AppTypography.label.copyWith(
                      color: selected ? AppColors.primary : AppColors.subtle,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );
}
