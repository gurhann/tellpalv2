import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/injection.dart';
import '../../../../app/library_dev_harness.dart';
import '../../../../core/diagnostics/library_diagnostics.dart';
import '../cubit/library_cubit.dart';
import '../view/library_view.dart';

@RoutePage()
class LibraryScreen extends StatelessWidget {
  const LibraryScreen({super.key});
  @override
  Widget build(BuildContext context) => BlocProvider(
    create: (_) {
      final cubit = libraryGetIt<LibraryCubit>();
      LibraryDevHarness.bind(cubit, libraryGetIt<LibraryDiagnostics>());
      cubit.load();
      return cubit;
    },
    child: const PopScope(canPop: false, child: LibraryView()),
  );
}
