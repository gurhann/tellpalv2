import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import '../features/reading_images/p5d_offline/presentation/p5d_cubit.dart';
import '../features/reading_images/p5d_offline/presentation/p5d_frame_proof.dart';
import 'p5d_offline/p5d_injection.dart';
import 'p5d_offline/p5d_router.dart';

const p5dChannel = MethodChannel('tellpal.p5d.offline');

/// Yalnız ayrı release Android host'u bu giriş noktasını çalıştırır.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (!kReleaseMode ||
      kIsWeb ||
      !Platform.isAndroid ||
      !const bool.fromEnvironment('TELLPAL_P5D_OFFLINE')) {
    runApp(const MaterialApp(home: Text('p5d_entry_refused')));
    return;
  }
  final native = await p5dChannel.invokeMapMethod<String, Object?>(
    'environment',
  );
  if (native?['packageId'] != 'com.tellpal.p5d.offline' ||
      native?['internetPermission'] != false ||
      native?['outputDirectory'] is! String) {
    throw StateError('host_refused');
  }
  final container = configureP5d(GetIt.asNewInstance());
  final cubit = container<P5dCubit>();
  attachP5dControl(cubit, native!['outputDirectory']! as String);
  runApp(
    BlocProvider.value(
      value: cubit,
      child: MaterialApp.router(
        title: 'P5d çevrimdışı karşılaştırma',
        debugShowCheckedModeBanner: false,
        routerConfig: container<P5dRouter>().config(),
      ),
    ),
  );
  await cubit.initialize();
}

P5dControl attachP5dControl(P5dCubit cubit, String output) {
  final controller = P5dControl(cubit, output);
  WidgetsBinding.instance.addObserver(controller);
  p5dChannel.setMethodCallHandler(controller.handle);
  return controller;
}

class P5dControl extends WidgetsBindingObserver {
  P5dControl(this.cubit, this.output);
  final P5dCubit cubit;
  final String output;
  Future<Object?> handle(MethodCall call) async {
    switch (call.method) {
      case 'status':
        return cubit.snapshot();
      case 'show':
        final args = call.arguments as Map;
        try {
          await cubit.show(args['index'] as int, args['sample'] as int, output);
        } catch (error) {
          throw PlatformException(
            code: 'p5d_show_failed',
            message: error.runtimeType.toString(),
            details: cubit.lastFailure,
          );
        }
        return cubit.snapshot();
      case 'crop':
        final crop = call.arguments as bool;
        final epoch = cubit.state.epoch, index = cubit.state.index;
        cubit.crop(crop);
        final deadline = DateTime.now().add(const Duration(seconds: 5));
        do {
          WidgetsBinding.instance.scheduleFrame();
          await WidgetsBinding.instance.endOfFrame;
          if (cubit.state.status != 'loaded') {
            throw StateError('presentation_cancelled');
          }
          if (p5dProofMatches(cubit.frameProof, epoch, index, crop)) {
            return cubit.frameProof;
          }
          await Future<void>.delayed(const Duration(milliseconds: 16));
        } while (DateTime.now().isBefore(deadline));
        throw StateError('paint_ack_timeout');
      case 'release':
        await cubit.release();
        return cubit.snapshot();
      case 'stop':
        await cubit.stop();
        return cubit.snapshot();
      default:
        throw PlatformException(code: 'command_refused');
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) unawaited(cubit.stop());
  }
}
