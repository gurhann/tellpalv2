import 'package:dartz/dartz.dart';
import '../../../../core/error/failure.dart';
import '../entity/library_catalog.dart';

abstract interface class LibraryRepository {
  Future<Either<Failure, LibraryCatalog>> fetch(LibraryContext context);
}
