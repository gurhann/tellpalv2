import 'dart:async';

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../reading_image_comparison_injection.dart';

@RoutePage()
class ReadingImageConsumerFlowScreen extends StatefulWidget {
  const ReadingImageConsumerFlowScreen({
    super.key,
    @PathParam('contentId') required this.contentId,
  });

  final int contentId;

  @override
  State<ReadingImageConsumerFlowScreen> createState() =>
      _ReadingImageConsumerFlowState();
}

class _ReadingImageConsumerFlowState
    extends State<ReadingImageConsumerFlowScreen>
    with
        AutoRouteAwareStateMixin<ReadingImageConsumerFlowScreen>,
        WidgetsBindingObserver {
  late final cubit = comparisonGetIt<ReadingImageComparisonCubit>();
  Future<void>? _closing;
  bool _closed = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) =>
      cubit.visibility(state == AppLifecycleState.resumed);

  @override
  void didPushNext() => cubit.visibility(false);

  @override
  void didPopNext() => cubit.visibility(true);

  Future<void> _close() => _closing ??= _drain();

  Future<void> _drain() async {
    await cubit.close();
    if (!mounted) return;
    setState(() => _closed = true);
    await context.router.maybePop();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    if (!cubit.isClosed) unawaited(_closing ??= cubit.close());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => BlocProvider.value(
    value: cubit,
    child: ReadingImageConsumerFlowScope(
      close: _close,
      child: PopScope(
        canPop: _closed,
        onPopInvokedWithResult: (didPop, _) {
          if (!didPop) unawaited(_close());
        },
        child: _closed
            ? const Scaffold(body: Center(child: Text('QA akışı kapandı')))
            : !cubit.configuration.mediaEnabled || widget.contentId <= 0
            ? const Scaffold(body: Center(child: Text('QA girişi kapalı')))
            : const AutoRouter(),
      ),
    ),
  );
}

/// Native işin bitmesini bekleyen kapanışı child sunumuna verir.
class ReadingImageConsumerFlowScope extends InheritedWidget {
  const ReadingImageConsumerFlowScope({
    super.key,
    required this.close,
    required super.child,
  });

  final Future<void> Function() close;

  static ReadingImageConsumerFlowScope of(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<ReadingImageConsumerFlowScope>()!;

  @override
  bool updateShouldNotify(ReadingImageConsumerFlowScope oldWidget) => false;
}
