import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/image_experiment_composition.dart';
import '../../../../story_manifest_client.dart';
import '../../../../story_player_page.dart';
import '../cubit/reader_resolution_cubit.dart';
import '../cubit/reader_resolution_state.dart';

@RoutePage()
class ReaderResolutionScreen extends StatelessWidget {
  const ReaderResolutionScreen({super.key});
  @override
  Widget build(BuildContext context) => BlocProvider(
    create: (_) {
      final composition = ImageExperimentResolver.of(context).composition;
      final cubit = composition.container<ReaderResolutionCubit>();
      composition.startReader(cubit);
      return cubit;
    },
    child: const _ReaderResolutionView(),
  );
}

class _ReaderResolutionView extends StatelessWidget {
  const _ReaderResolutionView();
  @override
  Widget build(
    BuildContext context,
  ) => BlocSelector<ReaderResolutionCubit, ReaderResolutionState, bool>(
    selector: (state) => state.status == ReaderResolutionStatus.reading,
    builder: (context, reading) {
      if (!reading) return const Scaffold(body: SizedBox.expand());
      final cubit = context.read<ReaderResolutionCubit>();
      final composition = ImageExperimentResolver.of(context).composition;
      return StoryPlayerPage(
        client: composition.container<StoryManifestSource>(),
        contentId: 71,
        languageCode: 'tr',
        images: cubit.images,
        controller: cubit.controller,
        // A finite experiment grant is never renewed or replaced by a backend request.
        grantRefreshWindow: Duration.zero,
      );
    },
  );
}
