import 'dart:io';
import 'package:flutter/widgets.dart';
import 'manual_consumer_qa/coordinator.dart';
import 'manual_consumer_qa/private_protocol.dart';
import 'reading_consumer_manual_entry.dart';
import 'reading_consumer_qa_probe.dart';

Future<void> main() async {
  await bootstrapManualConsumerRetryQa();
}

/// Yeni APK aynı başlangıç zincirini yalnız sabit yeni kapsamla açar.
Future<ManualConsumerQaCoordinator?> bootstrapManualConsumerRetryQa({
  Future<Directory> Function()? resolveSupport,
  ManualQaPlatform platform = const ManualQaPlatform(),
  void Function(Widget)? mount,
  void Function(String)? diagnostic,
}) => bootstrapManualConsumerQa(
  scope: ConsumerQaExpectedScope.p4b5ManualRetry,
  resolveSupport: resolveSupport,
  platform: platform,
  mount: mount,
  diagnostic: diagnostic,
);
