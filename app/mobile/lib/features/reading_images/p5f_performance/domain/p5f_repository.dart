import 'package:dartz/dartz.dart';
import '../../p5d_offline/domain/p5d_repository.dart';

abstract interface class P5fRepository {
  Future<Either<P5dFailure, Unit>> initialize();
  Future<Either<P5dFailure, P5dEncoded>> read(int index);
}
