import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'package:path_provider/path_provider.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_qa/manual_qa_policy.dart';
import 'package:tellpal_mobile_poc/core/theme/library_theme.dart';
import 'manual_consumer_qa/coordinator.dart';
import 'manual_consumer_qa/injection.dart';
import 'manual_consumer_qa/private_protocol.dart';

Future<void> main() async {
  await bootstrapManualConsumerQa();
}

/// Main ve testler aynı DI, coordinator ve özel protokol başlangıcını kullanır.
Future<ManualConsumerQaCoordinator?> bootstrapManualConsumerQa({
  Future<Directory> Function()? resolveSupport,
  ManualQaPlatform platform = const ManualQaPlatform(),
  void Function(Widget)? mount,
  void Function(String)? diagnostic,
}) async {
  WidgetsFlutterBinding.ensureInitialized();
  final diagnostics = ManualQaDiagnostics(diagnostic);
  ManualConsumerQaCoordinator? coordinator;
  try {
    diagnostics.emit(
      ManualQaDiagnosticStage.support,
      ManualQaDiagnosticCode.started,
    );
    final support = await (resolveSupport ?? getApplicationSupportDirectory)();
    diagnostics.emit(
      ManualQaDiagnosticStage.support,
      ManualQaDiagnosticCode.complete,
    );
    diagnostics.emit(
      ManualQaDiagnosticStage.injection,
      ManualQaDiagnosticCode.started,
    );
    final container = configureManualConsumerQa(GetIt.asNewInstance());
    coordinator = container<ManualConsumerQaCoordinator>(
      param1: ManualQaPrivateProtocol(
        support,
        platform: platform,
        diagnostics: diagnostics,
      ),
    );
    Bloc.observer = coordinator.probe;
    diagnostics.emit(
      ManualQaDiagnosticStage.mount,
      ManualQaDiagnosticCode.started,
    );
    (mount ?? runApp)(
      BlocProvider.value(
        value: coordinator,
        child: const ManualConsumerQaView(),
      ),
    );
    diagnostics.emit(
      ManualQaDiagnosticStage.mount,
      ManualQaDiagnosticCode.complete,
    );
    await coordinator.initialize();
    return coordinator;
  } catch (_) {
    diagnostics.failed();
    await coordinator?.fail();
    return coordinator;
  }
}

class ManualConsumerQaView extends StatelessWidget {
  const ManualConsumerQaView({super.key});
  @override
  Widget build(BuildContext context) =>
      BlocBuilder<ManualConsumerQaCoordinator, ManualConsumerQaState>(
        builder: (context, state) {
          final coordinator = context.read<ManualConsumerQaCoordinator>();
          return ManualConsumerQaScope(
            policy: coordinator,
            revision: state.revision,
            child:
                coordinator.app ??
                MaterialApp(
                  theme: AppTypography.theme(),
                  home: Scaffold(
                    body: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(coordinator.instruction),
                          FilledButton(
                            onPressed: state.stage == 'ready'
                                ? coordinator.start
                                : null,
                            child: const Text('Başlat'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
          );
        },
      );
}
