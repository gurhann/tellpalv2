import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
// Yalnız ayrı QA profile paketi mevcut dev dependency'yi kullanır.
// ignore: depend_on_referenced_packages
import 'package:flutter_driver/driver_extension.dart';
import 'package:get_it/get_it.dart';
import 'local_lossless_qa_cubit.dart';
import 'local_lossless_qa_injection.dart';
import 'local_lossless_qa_view.dart';

const localLosslessQaEnabled = bool.fromEnvironment(
  'TELLPAL_LOCAL_LOSSLESS_QA',
);
const localLosslessQaRunNonce = String.fromEnvironment(
  'TELLPAL_LOCAL_LOSSLESS_RUN',
);

bool validLocalLosslessQaRunNonce(String value) =>
    value.length == 64 && RegExp(r'^[a-f0-9]{64}$').hasMatch(value);

bool localLosslessQaAllowed({
  required bool enabled,
  required bool profileMode,
  required bool isWeb,
}) => enabled && profileMode && !isWeb;

/// Bu giriş native admission veya normal authenticated akışa dokunmaz.
Future<void> runLocalLosslessQa() async {
  if (!validLocalLosslessQaRunNonce(localLosslessQaRunNonce) ||
      !localLosslessQaAllowed(
        enabled: localLosslessQaEnabled,
        profileMode: kProfileMode,
        isWeb: kIsWeb,
      )) {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(const LocalLosslessQaRefused());
    return;
  }
  final container = configureLocalLosslessQa(GetIt.asNewInstance());
  final cubit = container<LocalLosslessQaCubit>();
  final control = LocalLosslessQaControl(cubit);
  enableFlutterDriverExtension(
    handler: control.handle,
    silenceErrors: true,
    enableTextEntryEmulation: false,
  );
  WidgetsBinding.instance.addObserver(control);
  runApp(LocalLosslessQaApp(cubit: cubit));
  await cubit.initialize();
}

class LocalLosslessQaControl extends WidgetsBindingObserver {
  LocalLosslessQaControl(this.cubit);
  final LocalLosslessQaCubit cubit;
  bool _commandActive = false;

  Map<String, Object?> snapshot() => {
    ...cubit.snapshot(),
    'enabled': localLosslessQaEnabled,
    'profileMode': kProfileMode,
    'runNonce': localLosslessQaRunNonce,
    'renderer': 'unobserved',
    'httpTransferMeasured': false,
    'osBackgroundTested': false,
  };

  Future<String> handle(String? message) async {
    try {
      if (message == 'status') return jsonEncode(snapshot());
      if (message == 'stop') {
        await cubit.stop();
        WidgetsBinding.instance.scheduleFrame();
        await WidgetsBinding.instance.endOfFrame.timeout(
          const Duration(seconds: 5),
        );
        return jsonEncode(snapshot());
      }
      if (_commandActive ||
          message == null ||
          !RegExp(r'^show:[0-5]$').hasMatch(message)) {
        throw StateError('command_refused');
      }
      _commandActive = true;
      try {
        await cubit.show(int.parse(message.substring(5)));
        await paintLocalLosslessQa(cubit);
        return jsonEncode(snapshot());
      } finally {
        _commandActive = false;
      }
    } catch (_) {
      cubit.refuse('command_refused');
      return jsonEncode({...snapshot(), 'ok': false});
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) cubit.visibility(false);
  }
}
