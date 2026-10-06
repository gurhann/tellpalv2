import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';

import 'probe.config.dart';

part 'probe.freezed.dart';
part 'probe.g.dart';
part 'probe.gr.dart';

final getIt = GetIt.instance;

@InjectableInit()
void configureDependencies() => getIt.init();

class ProbeEntity {
  const ProbeEntity({required this.id});
  final int id;
}

@JsonSerializable()
class ProbeModel extends ProbeEntity {
  const ProbeModel({required super.id});
  factory ProbeModel.fromJson(Map<String, dynamic> json) =>
      _$ProbeModelFromJson(json);
  Map<String, dynamic> toJson() => _$ProbeModelToJson(this);
}

@freezed
sealed class ProbeState with _$ProbeState {
  const factory ProbeState.initial() = ProbeInitial;
  const factory ProbeState.loaded(ProbeEntity item) = ProbeLoaded;
}

@injectable
class ProbeCubit extends Cubit<ProbeState> {
  ProbeCubit() : super(const ProbeState.initial());
}

@RoutePage()
class ProbePage extends StatelessWidget {
  const ProbePage({super.key});
  @override
  Widget build(BuildContext context) => BlocProvider(
    create: (_) => getIt<ProbeCubit>(),
    child: const Text('Toolchain probe'),
  );
}

@lazySingleton
@AutoRouterConfig(replaceInRouteName: 'Page,Route')
class ProbeRouter extends RootStackRouter {
  @override
  List<AutoRoute> get routes => [
    AutoRoute(page: ProbeRoute.page, initial: true),
  ];
}
