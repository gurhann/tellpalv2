import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/image_experiment_composition.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../../../../core/diagnostics/image_experiment_recorder.dart';
import '../../../../core/diagnostics/image_experiment_target_probe.dart';
import '../../../../core/network/image_experiment_image_cache.dart';
import '../../../../core/theme/library_theme.dart';
import '../cubit/raster_case_cubit.dart';
import '../cubit/raster_case_state.dart';

class RasterCaseView extends StatelessWidget {
  const RasterCaseView({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text(
        'TellPal Image Experiment',
        style: AppTypography.section,
      ),
    ),
    body: BlocBuilder<RasterCaseCubit, RasterCaseState>(
      builder: (context, state) {
        final fixture = state.fixture;
        if ((state.status == RasterCaseStatus.loading ||
                state.status == RasterCaseStatus.loaded) &&
            fixture != null) {
          final resolver = ImageExperimentResolver.of(context);
          final cache = resolver.get<ImageExperimentImageCache>();
          final configuration = resolver.get<ImageExperimentConfiguration>();
          final cubit = context.read<RasterCaseCubit>();
          final value = cache.provider(fixture, '${state.attempt}');
          return Center(
            child: ImageExperimentTargetProbe(
              key: ValueKey(value.transferKey),
              value: value,
              cache: cache,
              recorder: resolver.get<ImageExperimentRecorder>(),
              generation: configuration.generation,
              current: () => cubit.current(state.attempt),
              onDecoded: () => cubit.success(state.attempt),
              onFailure: () => cubit.fail(state.attempt),
              onDraw: () =>
                  resolver.composition.requestTeardown(verifiedDraw: true),
            ),
          );
        }
        if (state.status == RasterCaseStatus.error ||
            state.status == RasterCaseStatus.empty) {
          final composition = ImageExperimentResolver.of(context).composition;
          WidgetsBinding.instance.addPostFrameCallback(
            (_) => composition.requestTeardown(),
          );
          WidgetsBinding.instance.ensureVisualUpdate();
        }
        final text = switch (state.status) {
          RasterCaseStatus.initial => 'Fixture entry ready',
          RasterCaseStatus.loading => 'Loading admitted fixture',
          RasterCaseStatus.error => 'Fixture unavailable',
          RasterCaseStatus.empty => 'No admitted fixture',
          RasterCaseStatus.loaded => 'Fixture unavailable',
          RasterCaseStatus.refused => 'Experiment entry refused',
        };
        return Center(child: Text(text, style: AppTypography.description));
      },
    ),
  );
}
