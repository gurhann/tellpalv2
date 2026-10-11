import 'package:dartz/dartz.dart';
import '../../../../core/error/failure.dart';
import '../entity/raster_case.dart';

abstract interface class RasterCaseRepository {
  Future<Either<Failure, RasterCase?>> getCase(String caseId);
}
