import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../../domain/entity/raster_case.dart';
import '../../domain/repository/raster_case_repository.dart';
import '../datasource/raster_case_datasource.dart';

@Injectable(as: RasterCaseRepository)
class RasterCaseRepositoryImpl implements RasterCaseRepository {
  const RasterCaseRepositoryImpl(this._datasource);
  final RasterCaseDatasource _datasource;
  @override
  Future<Either<Failure, RasterCase?>> getCase(String caseId) async {
    try {
      return Right(await _datasource.getCase(caseId));
    } on StateError {
      return const Left(Failure(FailureKind.unauthorized));
    } catch (_) {
      return const Left(Failure(FailureKind.invalidResponse));
    }
  }
}
