import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../cubit/reading_image_comparison_cubit.dart';
import '../view/reading_image_comparison_view.dart';

@RoutePage()
class ReadingImageComparisonScreen extends StatefulWidget {
  const ReadingImageComparisonScreen({
    super.key,
    @PathParam('contentId') required this.contentId,
  });
  final int contentId;
  @override
  State<ReadingImageComparisonScreen> createState() => _ComparisonScreenState();
}

class _ComparisonScreenState extends State<ReadingImageComparisonScreen>
    with AutoRouteAwareStateMixin<ReadingImageComparisonScreen> {
  late final cubit = comparisonGetIt<ReadingImageComparisonCubit>();
  @override
  void didPushNext() {
    cubit.visibility(false);
  }

  @override
  void didPopNext() {
    cubit.visibility(true);
  }

  @override
  void dispose() {
    unawaited(cubit.close());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => BlocProvider.value(
    value: cubit,
    child: ReadingImageComparisonView(contentId: widget.contentId),
  );
}
