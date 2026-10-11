import 'package:injectable/injectable.dart';
import '../../../../app/image_experiment_configuration.dart';
import '../model/raster_case_model.dart';

@injectable
class RasterCaseDatasource {
  const RasterCaseDatasource(this._configuration);
  final ImageExperimentConfiguration _configuration;
  Future<RasterCaseModel?> getCase(String caseId) async {
    _configuration.credentials.requireActive();
    final value = _configuration.cases[caseId];
    if (value == null) throw const FormatException('fixture_unknown');
    return value;
  }
}
