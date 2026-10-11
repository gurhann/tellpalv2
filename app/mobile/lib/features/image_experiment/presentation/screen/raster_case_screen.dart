import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/image_experiment_composition.dart';
import '../cubit/raster_case_cubit.dart';
import '../view/raster_case_view.dart';

@RoutePage()
class RasterCaseScreen extends StatelessWidget {
  const RasterCaseScreen({
    super.key,
    @PathParam('caseId') required this.caseId,
  });
  final String caseId;
  @override
  Widget build(BuildContext context) => BlocProvider(
    create: (_) =>
        ImageExperimentResolver.of(context).get<RasterCaseCubit>()
          ..load(caseId),
    child: const RasterCaseView(),
  );
}
