import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'library_fixture.dart';

void main() {
  test(
    'successful renewal pins changed editorial snapshot and adopts only matching row/language/content covers and expiry',
    () async {
      LibraryCover cover(int id, String variant, DateTime expiry) =>
          LibraryCover(
            assetId: id,
            kind: 'LISTING_COVER',
            mimeType: 'image/jpeg',
            url: 'https://example.invalid/$id-$variant.jpg',
            expiresAt: expiry,
          );
      final oldExpiry = DateTime.now().subtract(const Duration(minutes: 1));
      final newExpiry = DateTime.now().add(const Duration(minutes: 10));
      LibraryPreview item(int id, String language, {bool fresh = false}) =>
          LibraryPreview(
            contentId: id,
            externalKey: '${fresh ? 'changed' : 'old'}-$id',
            type: 'STORY',
            experienceType: 'READING',
            contentLanguage: language,
            title: '${fresh ? 'Changed' : 'Pinned'} title $id',
            ageRange: fresh ? 9 : 4,
            durationMinutes: fresh ? 8 : 3,
            isFree: fresh,
            isLocked: !fresh,
            covers: LibraryCovers(
              phone: cover(
                (fresh ? 1000 : 100) + id,
                'phone',
                fresh ? newExpiry : oldExpiry,
              ),
              tablet: cover(
                (fresh ? 2000 : 200) + id,
                'tablet',
                fresh ? newExpiry : oldExpiry,
              ),
            ),
          );
      LibraryRow row(
        int id,
        String language,
        List<int> ids, {
        bool fresh = false,
      }) => LibraryRow(
        categoryId: id,
        slug: '${fresh ? 'changed' : 'old'}-$id',
        name: '${fresh ? 'Changed' : 'Pinned'} row $id',
        description: '${fresh ? 'Changed' : 'Pinned'} description',
        displayOrder: fresh ? 10 - id : id,
        contentLanguage: language,
        items: ids.map((id) => item(id, language, fresh: fresh)).toList(),
      );
      final original = LibraryCatalog(
        schemaVersion: 1,
        requestedLanguage: 'tr',
        audience: 'NON_SUBSCRIBER',
        previewLimit: 5,
        rows: [
          row(1, 'tr', [11, 12, 13]),
          row(2, 'en', [21]),
          row(3, 'tr', [31]),
        ],
      );
      final refreshed = LibraryCatalog(
        schemaVersion: 1,
        requestedLanguage: 'tr',
        audience: 'NON_SUBSCRIBER',
        previewLimit: 5,
        rows: [
          row(4, 'tr', [41], fresh: true),
          row(2, 'pt', [21], fresh: true),
          row(1, 'tr', [13, 99, 11], fresh: true),
        ],
      );
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final load = cubit.load();
      repo.responses[0].complete(right(original));
      await load;
      final attempt = cubit.state.attempt;
      cubit.refreshExpiredArtwork(attempt);
      expect(cubit.state.catalog, same(original));
      repo.responses[1].complete(right(refreshed));
      await Future<void>.delayed(Duration.zero);
      final renewed = cubit.state.catalog!;
      expect(cubit.state.status, LibraryStatus.loaded);
      expect(cubit.state.attempt, greaterThan(attempt));
      expect(renewed.rows.map((row) => row.categoryId), [1, 2, 3]);
      for (var index = 0; index < original.rows.length; index++) {
        final before = original.rows[index];
        final after = renewed.rows[index];
        expect(
          [
            after.slug,
            after.name,
            after.description,
            after.displayOrder,
            after.contentLanguage,
          ],
          [
            before.slug,
            before.name,
            before.description,
            before.displayOrder,
            before.contentLanguage,
          ],
        );
        expect(
          after.items.map((item) => item.contentId),
          before.items.map((item) => item.contentId),
        );
        for (var itemIndex = 0; itemIndex < before.items.length; itemIndex++) {
          final old = before.items[itemIndex];
          final current = after.items[itemIndex];
          expect(
            [
              current.externalKey,
              current.title,
              current.ageRange,
              current.durationMinutes,
              current.isFree,
              current.isLocked,
              current.contentLanguage,
            ],
            [
              old.externalKey,
              old.title,
              old.ageRange,
              old.durationMinutes,
              old.isFree,
              old.isLocked,
              old.contentLanguage,
            ],
          );
          final matches = after.categoryId == 1 && current.contentId != 12;
          expect(
            current.covers.phone!.assetId,
            (matches ? 1000 : 100) + current.contentId,
          );
          expect(
            current.covers.tablet!.assetId,
            (matches ? 2000 : 200) + current.contentId,
          );
          expect(
            current.covers.phone!.expiresAt,
            matches ? newExpiry : oldExpiry,
          );
          expect(
            current.covers.tablet!.expiresAt,
            matches ? newExpiry : oldExpiry,
          );
          if (matches) {
            expect(
              current.covers.phone!.url,
              contains('/${1000 + current.contentId}-phone.jpg'),
            );
            expect(
              current.covers.tablet!.url,
              contains('/${2000 + current.contentId}-tablet.jpg'),
            );
          }
        }
      }
      expect(renewed.previewLimit, original.previewLimit);
      await cubit.close();
    },
  );
  test(
    'same-context retry rejects an earlier success and error in reversed order',
    () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final first = cubit.load();
      final second = cubit.load();
      repo.responses[1].complete(right(catalog('tr')));
      await second;
      repo.responses[0].complete(left(const Failure(FailureKind.network)));
      await first;
      expect(cubit.state.status, LibraryStatus.loaded);
      expect(cubit.state.attempt, 2);
      final third = cubit.load();
      final fourth = cubit.load();
      repo.responses[3].complete(left(const Failure(FailureKind.network)));
      await fourth;
      repo.responses[2].complete(right(catalog('tr')));
      await third;
      expect(cubit.state.status, LibraryStatus.error);
      await cubit.close();
    },
  );
  test(
    'language and access context change reject obsolete responses and close rejects callbacks',
    () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final old = cubit.load();
      final current = cubit.load(
        context: const LibraryContext('pt', 'another-account'),
      );
      repo.responses[1].complete(right(catalog('pt', empty: true)));
      await current;
      repo.responses[0].complete(right(catalog('tr')));
      await old;
      expect(cubit.state.status, LibraryStatus.empty);
      expect(cubit.state.context.language, 'pt');
      final disposed = cubit.load();
      await cubit.close();
      repo.responses[2].complete(right(catalog('pt')));
      await disposed;
      expect(cubit.state.status, LibraryStatus.loading);
    },
  );
  test(
    'deadline leaves loading, retry has new attempt and expired completion is ignored',
    () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(wait: const Duration(milliseconds: 15)),
        LibraryDiagnostics(),
      );
      final expired = cubit.load();
      await expired;
      expect(cubit.state.failure!.kind, FailureKind.timeout);
      final retry = cubit.load();
      repo.responses[1].complete(right(catalog('tr')));
      await retry;
      repo.responses[0].complete(left(const Failure(FailureKind.network)));
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state.status, LibraryStatus.loaded);
      expect(cubit.state.attempt, 3);
      await cubit.close();
    },
  );
  test(
    'expired signed artwork allows only one automatic refresh per current catalog',
    () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final initial = cubit.load();
      repo.responses[0].complete(right(catalog('tr')));
      await initial;
      cubit.refreshExpiredArtwork(1);
      expect(repo.calls, hasLength(2));
      repo.responses[1].complete(right(catalog('tr')));
      await Future<void>.delayed(Duration.zero);
      cubit.refreshExpiredArtwork(2);
      expect(repo.calls, hasLength(2));
      await cubit.close();
    },
  );
  for (final outcome in ['network', 'timeout', 'empty', 'invalid']) {
    test(
      'background artwork refresh $outcome retains the exact successful catalog',
      () async {
        final repo = ControlledRepository();
        final cubit = LibraryCubit(
          GetLibrary(repo),
          configuration(wait: const Duration(milliseconds: 20)),
          LibraryDiagnostics(),
        );
        final original = catalog('tr');
        final load = cubit.load();
        repo.responses[0].complete(right(original));
        await load;
        final before = cubit.state;
        final changes = <LibraryState>[];
        final subscription = cubit.stream.listen(changes.add);
        cubit.refreshExpiredArtwork(before.attempt);
        expect(cubit.state, same(before));
        if (outcome == 'network') {
          repo.responses[1].complete(left(const Failure(FailureKind.network)));
        }
        if (outcome == 'empty') {
          repo.responses[1].complete(right(catalog('tr', empty: true)));
        }
        if (outcome == 'invalid') {
          repo.responses[1].complete(right(catalog('en')));
        }
        await Future<void>.delayed(const Duration(milliseconds: 30));
        expect(cubit.state, same(before));
        expect(cubit.state.catalog, same(original));
        expect(changes, isEmpty);
        cubit.refreshExpiredArtwork(before.attempt);
        expect(repo.calls, hasLength(2));
        if (outcome == 'timeout') {
          repo.responses[1].complete(right(catalog('tr')));
        }
        await Future<void>.delayed(Duration.zero);
        expect(cubit.state, same(before));
        await subscription.cancel();
        await cubit.close();
      },
    );
  }
  test(
    'manual expired-cover retry remains recoverable after automatic failure and is single-flight',
    () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final initial = cubit.load();
      repo.responses[0].complete(right(catalog('tr')));
      await initial;
      final snapshot = cubit.state;
      cubit.refreshExpiredArtwork(snapshot.attempt);
      repo.responses[1].complete(left(const Failure(FailureKind.network)));
      await Future<void>.delayed(Duration.zero);
      cubit.refreshExpiredArtwork(snapshot.attempt);
      expect(repo.calls, hasLength(2));
      cubit.refreshExpiredArtwork(snapshot.attempt, automatic: false);
      cubit.refreshExpiredArtwork(snapshot.attempt, automatic: false);
      expect(repo.calls, hasLength(3));
      expect(cubit.state, same(snapshot));
      repo.responses[2].complete(right(catalog('tr', empty: true)));
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state, same(snapshot));
      cubit.refreshExpiredArtwork(snapshot.attempt, automatic: false);
      expect(repo.calls, hasLength(4));
      repo.responses[3].complete(right(catalog('tr')));
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state.status, LibraryStatus.loaded);
      expect(cubit.state.attempt, greaterThan(snapshot.attempt));
      cubit.refreshExpiredArtwork(cubit.state.attempt);
      expect(repo.calls, hasLength(4));
      await cubit.close();
    },
  );
  for (final replacement in ['retry', 'context', 'disposal']) {
    test('obsolete automatic refresh cannot replace $replacement', () async {
      final repo = ControlledRepository();
      final cubit = LibraryCubit(
        GetLibrary(repo),
        configuration(),
        LibraryDiagnostics(),
      );
      final initial = cubit.load();
      repo.responses[0].complete(right(catalog('tr')));
      await initial;
      cubit.refreshExpiredArtwork(cubit.state.attempt);
      if (replacement == 'disposal') {
        await cubit.close();
      } else {
        final load = cubit.load(
          context: replacement == 'context'
              ? const LibraryContext('pt', 'another-account')
              : null,
        );
        repo.responses[2].complete(left(const Failure(FailureKind.network)));
        await load;
      }
      final authoritative = cubit.state;
      repo.responses[1].complete(right(catalog('tr')));
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state, same(authoritative));
      if (!cubit.isClosed) await cubit.close();
    });
  }
  test(
    'artwork attempt rejects old success, failure and timeout across retry, context and disposal',
    () async {
      final cubit = ArtworkCubit(
        configuration(wait: const Duration(milliseconds: 15)),
      );
      const a = ArtworkIdentity(LibraryContext('tr', 'a'), 1, 1, 1);
      const b = ArtworkIdentity(LibraryContext('en', 'b'), 2, 1, 2);
      final first = cubit.start(a, available: true);
      final retry = cubit.start(a, available: true);
      expect(cubit.fail(a, first), isFalse);
      cubit.success(a, first);
      expect(cubit.state.status, ArtworkStatus.loading);
      cubit.success(a, retry);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(cubit.state.status, ArtworkStatus.loaded);
      final current = cubit.start(b, available: true);
      cubit.success(a, retry);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(cubit.state.status, ArtworkStatus.error);
      cubit.success(b, current);
      expect(cubit.state.status, ArtworkStatus.error);
      final latest = cubit.start(b, available: true);
      await cubit.close();
      cubit.success(b, latest);
      cubit.fail(b, latest);
    },
  );
}
