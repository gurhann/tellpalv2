import 'package:flutter/widgets.dart';

/// Yalnız elle ölçüm girişinin kontrol kapısı; credential taşımaz.
abstract interface class ManualConsumerQaPolicy {
  String get instruction;
  bool allows(String action);
  bool accept(String action);
  void lifecycle(AppLifecycleState state);
  Future<void> closed();
}

class ManualConsumerQaScope extends InheritedWidget {
  const ManualConsumerQaScope({
    super.key,
    required this.policy,
    required this.revision,
    required super.child,
  });
  final ManualConsumerQaPolicy policy;
  final int revision;
  static ManualConsumerQaPolicy? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<ManualConsumerQaScope>()
      ?.policy;
  @override
  bool updateShouldNotify(ManualConsumerQaScope oldWidget) =>
      revision != oldWidget.revision;
}
