import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../entity/raster_case.dart';
import '../repository/raster_case_repository.dart';

@injectable
class GetRasterCase {
  const GetRasterCase(this._repository);
  final RasterCaseRepository _repository;
  Future<Either<Failure, RasterCase?>> call(String caseId) =>
      _repository.getCase(caseId);
}
