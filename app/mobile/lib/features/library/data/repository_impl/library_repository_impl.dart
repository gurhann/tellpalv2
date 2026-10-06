import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/network/authenticated_transport.dart';
import '../../domain/entity/library_catalog.dart';
import '../../domain/repository/library_repository.dart';
import '../model/library_catalog_model.dart';

@Injectable(as: LibraryRepository)
class LibraryRepositoryImpl implements LibraryRepository {
  const LibraryRepositoryImpl(this._transport);
  final AuthenticatedTransport _transport;
  @override
  Future<Either<Failure, LibraryCatalog>> fetch(LibraryContext context) async {
    try {
      final catalog = LibraryCatalogModel.fromJson(
        await _transport.library(context.language),
      );
      if (catalog.schemaVersion != 1 ||
          catalog.requestedLanguage != context.language ||
          catalog.previewLimit < 1 ||
          catalog.previewLimit > 100 ||
          !['NON_SUBSCRIBER', 'SUBSCRIBER'].contains(catalog.audience)) {
        throw const FormatException('Catalog contract mismatch');
      }
      final categories = <int>{};
      var previousOrder = -1;
      for (final row in catalog.rows) {
        if (!categories.add(row.categoryId) ||
            row.displayOrder <= previousOrder ||
            row.items.isEmpty ||
            row.items.length > catalog.previewLimit ||
            !['tr', 'en', 'pt'].contains(row.contentLanguage)) {
          throw const FormatException('Row contract mismatch');
        }
        previousOrder = row.displayOrder;
        final ids = <int>{};
        for (final item in row.items) {
          if (!ids.add(item.contentId) ||
              item.contentLanguage != row.contentLanguage ||
              item.type != 'STORY' ||
              item.experienceType != 'READING') {
            throw const FormatException('Preview contract mismatch');
          }
          for (final cover in [item.covers.phone, item.covers.tablet]) {
            if (cover != null &&
                (cover.assetId <= 0 ||
                    cover.mimeType != 'image/jpeg' ||
                    !['http', 'https'].contains(Uri.parse(cover.url).scheme))) {
              throw const FormatException('Cover contract mismatch');
            }
          }
        }
      }
      return right(catalog);
    } on CatalogHttpFailure catch (e) {
      return left(
        Failure(
          e.status == 401
              ? FailureKind.unauthorized
              : e.status == 403
              ? FailureKind.forbidden
              : FailureKind.network,
        ),
      );
    } on TimeoutException {
      return left(const Failure(FailureKind.timeout));
    } on FormatException {
      return left(const Failure(FailureKind.invalidResponse));
    } on TypeError {
      return left(const Failure(FailureKind.invalidResponse));
    } catch (_) {
      return left(const Failure(FailureKind.network));
    }
  }
}
