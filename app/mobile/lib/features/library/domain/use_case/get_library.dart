import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../entity/library_catalog.dart';
import '../repository/library_repository.dart';

@injectable
class GetLibrary {
  const GetLibrary(this._repository);
  final LibraryRepository _repository;
  Future<Either<Failure, LibraryCatalog>> call(LibraryContext context) =>
      _repository.fetch(context);
}
