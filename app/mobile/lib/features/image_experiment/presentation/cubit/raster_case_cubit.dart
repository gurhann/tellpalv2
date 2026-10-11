import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../../domain/use_case/get_raster_case.dart';
import 'raster_case_state.dart';

@injectable
class RasterCaseCubit extends Cubit<RasterCaseState> {
  RasterCaseCubit(this._getCase, this._configuration)
    : super(const RasterCaseState()) {
    _revocations = _configuration.credentials.revocations.listen(
      (_) => refuse(),
    );
  }
  final GetRasterCase _getCase;
  final ImageExperimentConfiguration _configuration;
  StreamSubscription<void>? _revocations;
  Timer? _timer;
  int _attempt = 0;
  bool current(int attempt) =>
      !isClosed && attempt == _attempt && _configuration.credentials.isActive;
  Future<void> load(String caseId) async {
    final attempt = ++_attempt;
    _timer?.cancel();
    if (!_configuration.credentials.isActive) {
      refuse();
      return;
    }
    emit(RasterCaseState(status: RasterCaseStatus.loading, attempt: attempt));
    final result = await _getCase(caseId);
    if (!current(attempt)) return;
    result.fold(
      (_) => emit(
        RasterCaseState(status: RasterCaseStatus.error, attempt: attempt),
      ),
      (fixture) {
        if (fixture == null) {
          emit(
            RasterCaseState(status: RasterCaseStatus.empty, attempt: attempt),
          );
          return;
        }
        emit(
          RasterCaseState(
            status: RasterCaseStatus.loading,
            fixture: fixture,
            attempt: attempt,
          ),
        );
        _timer = Timer(const Duration(seconds: 10), () => fail(attempt));
      },
    );
  }

  void success(int attempt) {
    if (!current(attempt) || state.status != RasterCaseStatus.loading) return;
    _timer?.cancel();
    emit(state.copyWith(status: RasterCaseStatus.loaded));
  }

  void fail(int attempt) {
    if (!current(attempt) || state.status != RasterCaseStatus.loading) return;
    _timer?.cancel();
    _attempt++;
    emit(state.copyWith(status: RasterCaseStatus.error));
  }

  void refuse() {
    if (isClosed || state.status == RasterCaseStatus.refused) return;
    _attempt++;
    _timer?.cancel();
    emit(const RasterCaseState(status: RasterCaseStatus.refused));
  }

  @override
  Future<void> close() async {
    _attempt++;
    _timer?.cancel();
    await _revocations?.cancel();
    await super.close();
  }
}
