import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/library_configuration.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/diagnostics/library_diagnostics.dart';
import '../../domain/entity/library_catalog.dart';
import '../../domain/use_case/get_library.dart';
import 'library_state.dart';

@injectable
class LibraryCubit extends Cubit<LibraryState> {
  LibraryCubit(this._getLibrary, this._configuration, this._diagnostics)
    : super(LibraryState(context: _configuration.context));
  final GetLibrary _getLibrary;
  final LibraryConfiguration _configuration;
  final LibraryDiagnostics _diagnostics;
  int _attempt = 0;
  bool _expiryRefreshUsed = false;
  int? _renewalAttempt;

  Future<void> load({LibraryContext? context}) async {
    if (isClosed) return;
    _expiryRefreshUsed = false;
    _renewalAttempt = null;
    final current = context ?? state.context;
    final attempt = ++_attempt;
    emit(LibraryState(context: current, attempt: attempt));
    final stopwatch = Stopwatch()..start();
    final result = await _fetch(current);
    if (!_current(current, attempt)) return;
    // Invalidate failed requests even when their transport cannot be cancelled.
    if (result.isLeft()) _attempt++;
    result.fold(
      (failure) =>
          emit(state.copyWith(status: LibraryStatus.error, failure: failure)),
      (catalog) => emit(
        state.copyWith(
          status: catalog.rows.isEmpty
              ? LibraryStatus.empty
              : LibraryStatus.loaded,
          catalog: catalog,
        ),
      ),
    );
    _diagnostics.catalog(
      current.language,
      stopwatch.elapsedMilliseconds,
      state.status.name,
    );
  }

  Future<Either<Failure, LibraryCatalog>> _fetch(LibraryContext context) =>
      _getLibrary(context).timeout(
        _configuration.catalogWait,
        onTimeout: () => left(const Failure(FailureKind.timeout)),
      );

  bool _current(LibraryContext context, int attempt) =>
      !isClosed && attempt == _attempt && context == state.context;

  void refreshExpiredArtwork(int catalogAttempt, {bool automatic = true}) {
    if (isClosed ||
        catalogAttempt != state.attempt ||
        state.status != LibraryStatus.loaded ||
        (automatic && _expiryRefreshUsed) ||
        _renewalAttempt != null) {
      return;
    }
    if (automatic) {
      _expiryRefreshUsed = true;
    }
    final attempt = ++_attempt;
    _renewalAttempt = attempt;
    unawaited(_refreshArtwork(state, attempt));
  }

  Future<void> _refreshArtwork(LibraryState snapshot, int attempt) async {
    final stopwatch = Stopwatch()..start();
    final result = await _fetch(snapshot.context);
    if (_renewalAttempt == attempt) _renewalAttempt = null;
    if (!_current(snapshot.context, attempt) ||
        state.attempt != snapshot.attempt) {
      return;
    }
    final outcome = result.fold(
      (failure) => 'artwork-refresh-${failure.kind.name}',
      (catalog) {
        if (catalog.rows.isEmpty ||
            catalog.requestedLanguage != snapshot.context.language ||
            catalog.audience != snapshot.catalog!.audience) {
          return 'artwork-refresh-invalid';
        }
        emit(
          snapshot.copyWith(
            attempt: attempt,
            catalog: _renewCovers(snapshot.catalog!, catalog),
          ),
        );
        return 'artwork-refresh-loaded';
      },
    );
    _diagnostics.catalog(
      snapshot.context.language,
      stopwatch.elapsedMilliseconds,
      outcome,
    );
  }

  // Renewal updates only matching artwork; the visible editorial snapshot stays pinned.
  LibraryCatalog _renewCovers(
    LibraryCatalog current,
    LibraryCatalog refreshed,
  ) {
    final rows = {for (final row in refreshed.rows) row.categoryId: row};
    return LibraryCatalog(
      schemaVersion: current.schemaVersion,
      requestedLanguage: current.requestedLanguage,
      audience: current.audience,
      previewLimit: current.previewLimit,
      rows: current.rows.map((row) {
        final fresh = rows[row.categoryId];
        final items = {
          if (fresh?.contentLanguage == row.contentLanguage)
            for (final item in fresh!.items) item.contentId: item,
        };
        return LibraryRow(
          categoryId: row.categoryId,
          slug: row.slug,
          name: row.name,
          description: row.description,
          displayOrder: row.displayOrder,
          contentLanguage: row.contentLanguage,
          items: row.items
              .map(
                (item) => LibraryPreview(
                  contentId: item.contentId,
                  externalKey: item.externalKey,
                  type: item.type,
                  experienceType: item.experienceType,
                  contentLanguage: item.contentLanguage,
                  title: item.title,
                  ageRange: item.ageRange,
                  durationMinutes: item.durationMinutes,
                  isFree: item.isFree,
                  isLocked: item.isLocked,
                  covers: LibraryCovers(
                    phone:
                        items[item.contentId]?.covers.phone ??
                        item.covers.phone,
                    tablet:
                        items[item.contentId]?.covers.tablet ??
                        item.covers.tablet,
                  ),
                ),
              )
              .toList(),
        );
      }).toList(),
    );
  }

  @override
  Future<void> close() {
    _renewalAttempt = null;
    _attempt++;
    return super.close();
  }
}
