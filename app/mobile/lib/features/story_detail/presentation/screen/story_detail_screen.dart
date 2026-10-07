import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../../../../app/library_dev_harness.dart';
import '../../domain/entity/story_detail.dart';
import '../cubit/story_detail_cubit.dart';
import '../view/story_detail_view.dart';

@RoutePage()
class StoryDetailScreen extends StatefulWidget {
  const StoryDetailScreen({
    super.key,
    required this.categoryId,
    required this.contentId,
    required this.uiLanguage,
    required this.contentLanguage,
    this.experience = 'READING',
  });
  final int categoryId, contentId;
  final String uiLanguage, contentLanguage, experience;
  @override
  State<StoryDetailScreen> createState() => _StoryDetailScreenState();
}

class _StoryDetailScreenState extends State<StoryDetailScreen>
    with AutoRouteAwareStateMixin<StoryDetailScreen> {
  late final StoryDetailCubit _cubit;
  StoryDetailIdentity get _identity => StoryDetailIdentity(
    categoryId: widget.categoryId,
    contentId: widget.contentId,
    uiLanguage: widget.uiLanguage,
    contentLanguage: widget.contentLanguage,
    experience: widget.experience,
  );
  @override
  void initState() {
    super.initState();
    _cubit = libraryGetIt<StoryDetailCubit>()..load(_identity);
    LibraryDevHarness.bindDetail(_cubit);
  }

  @override
  void didUpdateWidget(covariant StoryDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (StoryDetailIdentity(
          categoryId: oldWidget.categoryId,
          contentId: oldWidget.contentId,
          uiLanguage: oldWidget.uiLanguage,
          contentLanguage: oldWidget.contentLanguage,
          experience: oldWidget.experience,
        ) !=
        _identity) {
      _cubit.load(_identity);
    }
  }

  @override
  void didPushNext() => _cubit.setVisible(false);
  @override
  void didPopNext() => _cubit.setVisible(true);
  @override
  void dispose() {
    LibraryDevHarness.unbindDetail(_cubit);
    _cubit.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) =>
      BlocProvider.value(value: _cubit, child: const StoryDetailView());
}
