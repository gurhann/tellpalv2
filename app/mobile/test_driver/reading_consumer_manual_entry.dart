import 'dart:async';
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
  WidgetsFlutterBinding.ensureInitialized();
  final container = configureManualConsumerQa(GetIt.asNewInstance());
  final coordinator = container<ManualConsumerQaCoordinator>(
    param1: ManualQaPrivateProtocol(await getApplicationSupportDirectory()),
  );
  Bloc.observer = coordinator.probe;
  runApp(
    BlocProvider.value(value: coordinator, child: const ManualConsumerQaView()),
  );
  unawaited(coordinator.initialize());
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
