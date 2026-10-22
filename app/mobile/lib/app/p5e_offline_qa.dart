import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import '../features/reading_images/p5e_offline/presentation/p5e_cubit.dart';
import 'p5e_offline/p5e_injection.dart';
import 'p5e_offline/p5e_router.dart';

const p5eChannel = MethodChannel('tellpal.p5e.offline');
const p5ePackage = 'com.tellpal.tellpalMobilePoc.imageexperiment.release';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (!kReleaseMode ||
      kIsWeb ||
      !Platform.isIOS ||
      !const bool.fromEnvironment('TELLPAL_P5E_OFFLINE')) {
    runApp(const MaterialApp(home: Text('P5e başlangıcı reddedildi.')));
    return;
  }
  final refusal = await p5eStartupRefusal(
    expectedNonce: const String.fromEnvironment('TELLPAL_P5E_RUN'),
  );
  if (refusal != null) {
    runApp(refusal);
    return;
  }
  final container = configureP5e(GetIt.asNewInstance());
  final cubit = container<P5eCubit>();
  final control = P5eControl(cubit);
  WidgetsBinding.instance.addObserver(control);
  p5eChannel.setMethodCallHandler(control.handle);
  runApp(
    BlocProvider.value(
      value: cubit,
      child: MaterialApp.router(
        title: 'P5e çevrimdışı karşılaştırma',
        debugShowCheckedModeBanner: false,
        routerConfig: container<P5eRouter>().config(),
      ),
    ),
  );
  if (!const bool.fromEnvironment('TELLPAL_P5F_PERFORMANCE')) {
    await cubit.initialize();
    await control.refresh();
  }
}

/// Native başlangıç hatası veya uyuşmazlığı karşılaştırmayı kapalı tutar.
Future<Widget?> p5eStartupRefusal({required String expectedNonce}) async {
  try {
    final environment = await p5eChannel
        .invokeMapMethod<String, Object?>('environment')
        .timeout(const Duration(seconds: 30));
    if (environment?['packageId'] == p5ePackage &&
        environment?['plugins'] == 0 &&
        environment?['physicalDevice'] == true &&
        environment?['runNonce'] == expectedNonce &&
        expectedNonce.isNotEmpty) {
      return null;
    }
  } catch (_) {
    // Native bağlantı hatası da aynı kapalı ret ekranını kullanır.
  }
  return const MaterialApp(
    home: Scaffold(
      body: SafeArea(
        child: Center(
          child: Text('P5e başlangıcı doğrulanamadı. Karşılaştırma kapalı.'),
        ),
      ),
    ),
  );
}

class P5eControl extends WidgetsBindingObserver {
  P5eControl(this.cubit);
  final P5eCubit cubit;
  bool _active = true;
  Future<void> refresh() async {
    try {
      final network = await p5eChannel
          .invokeMethod<String>('network')
          .timeout(const Duration(seconds: 30));
      if (_active) cubit.network(network ?? 'unknown');
    } catch (_) {
      if (_active) cubit.network('unknown');
    }
  }

  Future<Object?> handle(MethodCall call) async {
    if (call.method != 'networkChanged') {
      throw PlatformException(code: 'command_refused');
    }
    if (_active) cubit.network(call.arguments as String? ?? 'unknown');
    return null;
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _active = state == AppLifecycleState.resumed;
    unawaited(_transition(_active));
  }

  Future<void> _transition(bool active) async {
    await cubit.lifecycle(active);
    if (active && _active) await refresh();
  }

  Future<void> dispose() async {
    _active = false;
    WidgetsBinding.instance.removeObserver(this);
    p5eChannel.setMethodCallHandler(null);
    await cubit.close();
  }
}
