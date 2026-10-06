import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:dartz/dartz.dart';
import 'package:file/local.dart';
import 'package:file/file.dart' as files;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/domain/use_case/get_library.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'library_fixture.dart' hide catalog;
import 'library_fixture.dart' as fixtures;

LibraryCatalog fixtureCatalog(String language) => fixtures.catalog(language);

class TestCacheFileSystem implements FileSystem {
  TestCacheFileSystem(this.directory);
  final String directory;
  @override
  Future<files.File> createFile(String name) async =>
      const LocalFileSystem().file('$directory/$name');
}

class ControlledImageCache extends LibraryImageCache {
  ControlledImageCache(String directory)
    : super.forTesting(
        Config(
          'widget-test-images',
          repo: NonStoringObjectProvider(),
          fileSystem: TestCacheFileSystem(directory),
        ),
      );
  final keys = <String>[];
  final transfers = <StreamController<FileResponse>>[];
  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    keys.add(key!);
    final response = StreamController<FileResponse>();
    transfers.add(response);
    return response.stream;
  }

  @override
  Future<void> removeFile(String key) async {}
  @override
  Future<void> rememberValidated(
    String immutableKey,
    String transferKey, {
    required bool Function() isCurrent,
  }) async {}
}

LibraryPreview preview() => LibraryPreview(
  contentId: 1,
  externalKey: 'story',
  type: 'STORY',
  experienceType: 'READING',
  contentLanguage: 'tr',
  title: 'Story title',
  isFree: false,
  isLocked: true,
  covers: LibraryCovers(phone: cover(1), tablet: cover(2)),
);
LibraryCover cover(int id) => LibraryCover(
  assetId: id,
  kind: 'LISTING_COVER',
  mimeType: 'image/jpeg',
  url: 'http://test.invalid/artwork/$id',
  expiresAt: DateTime.now().add(const Duration(hours: 1)),
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late File decoded;
  late Directory temporary;
  setUp(() async {
    temporary = await Directory.systemTemp.createTemp('library-artwork-test');
    decoded = await File('${temporary.path}/valid.png').writeAsBytes(
      base64Decode(
        'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
      ),
    );
  });
  tearDown(() async {
    await libraryGetIt.reset();
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
    await temporary.delete(recursive: true);
  });
  Future<(LibraryCubit, ControlledImageCache)> mount(
    WidgetTester tester,
    Size size,
  ) async {
    final config = configuration(wait: const Duration(seconds: 2));
    final cache = ControlledImageCache(temporary.path);
    libraryGetIt.registerSingleton<LibraryImageCache>(cache);
    libraryGetIt.registerSingleton<LibraryDiagnostics>(LibraryDiagnostics());
    libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
    final repository = ControlledRepository();
    final catalog = LibraryCubit(
      GetLibrary(repository),
      config,
      LibraryDiagnostics(),
    );
    final load = catalog.load();
    repository.responses[0].complete(right(fixtureCatalog('tr')));
    await load;
    Future<void> render(Size selected) => tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: MediaQueryData(size: selected),
          child: BlocProvider.value(
            value: catalog,
            child: SizedBox(
              width: 180,
              height: 240,
              child: LibraryCard(
                item: preview(),
                state: catalog.state,
                strings: const LibraryStrings('tr'),
              ),
            ),
          ),
        ),
      ),
    );
    await render(size);
    await tester.pump();
    return (catalog, cache);
  }

  ArtworkCubit artwork(WidgetTester tester) =>
      tester.element(find.byType(CachedNetworkImage)).read<ArtworkCubit>();
  Future<void> success(
    WidgetTester tester,
    ControlledImageCache cache,
    int transfer,
  ) async {
    await tester.runAsync(() async {
      cache.transfers[transfer].add(
        FileInfo(
          const LocalFileSystem().file(decoded.path),
          FileSource.Online,
          DateTime.now().add(const Duration(minutes: 10)),
          'http://test.invalid/artwork',
        ),
      );
      unawaited(cache.transfers[transfer].close());
    });
    // File read, engine decode and the widget frame advance in distinct schedulers.
    for (var frame = 0; frame < 20; frame++) {
      await tester.pump();
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 10)),
      );
    }
    await tester.pumpAndSettle(const Duration(milliseconds: 10));
  }

  testWidgets(
    'same-asset retry starts a separate provider transfer before old success completes',
    (tester) async {
      final (catalog, cache) = await mount(tester, const Size(411, 914));
      final expired = artwork(tester);
      await tester.pump(const Duration(milliseconds: 2005));
      expect(expired.state.status, ArtworkStatus.error);
      await tester.tap(find.byTooltip(const LibraryStrings('tr').retryArtwork));
      await tester.pump();
      await tester.pump();
      expect(cache.transfers, hasLength(2));
      expect(cache.keys.toSet(), hasLength(2));
      final current = artwork(tester);
      await success(tester, cache, 0);
      expect(current.state.status, ArtworkStatus.loading);
      await success(tester, cache, 1);
      expect(current.state.status, ArtworkStatus.loaded);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      await catalog.close();
    },
  );
  testWidgets(
    'same-asset retry rejects old transfer error after replacement loaded',
    (tester) async {
      final (catalog, cache) = await mount(tester, const Size(411, 914));
      await tester.pump(const Duration(milliseconds: 2005));
      await tester.tap(find.byTooltip(const LibraryStrings('tr').retryArtwork));
      await tester.pump();
      await tester.pump();
      final current = artwork(tester);
      await success(tester, cache, 1);
      cache.transfers[0].addError(const HttpException('Obsolete transfer'));
      await tester.pump();
      await tester.pump();
      expect(current.state.status, ArtworkStatus.loaded);
      expect(
        find.byTooltip(const LibraryStrings('tr').retryArtwork),
        findsNothing,
      );
      await tester.pumpWidget(const SizedBox());
      await catalog.close();
    },
  );
  testWidgets(
    'short enlarged long-title error preserves lock and 48dp retry without overflow',
    (tester) async {
      final (catalog, cache) = await mount(tester, const Size(914, 411));
      final semantics = tester.ensureSemantics();
      final original = preview();
      const title =
          'A very long localized story title retained in the short landscape artwork placeholder';
      final item = LibraryPreview(
        contentId: original.contentId,
        externalKey: original.externalKey,
        type: original.type,
        experienceType: original.experienceType,
        contentLanguage: original.contentLanguage,
        title: title,
        isFree: false,
        isLocked: true,
        covers: original.covers,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: MediaQuery(
            data: const MediaQueryData(
              size: Size(914, 411),
              textScaler: TextScaler.linear(1.3),
            ),
            child: BlocProvider.value(
              value: catalog,
              child: Center(
                child: SizedBox(
                  width: 83,
                  height: 110,
                  child: LibraryCard(
                    item: item,
                    state: catalog.state,
                    strings: const LibraryStrings('tr'),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 2005));
      final retry = find.byTooltip(const LibraryStrings('tr').retryArtwork);
      expect(tester.getSize(retry), const Size(48, 48));
      expect(
        find.bySemanticsLabel(
          '$title, tr, ${const LibraryStrings('tr').locked}',
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      await tester.tap(retry);
      await tester.pump();
      expect(cache.transfers.length, greaterThanOrEqualTo(2));
      await tester.pumpWidget(const SizedBox());
      await catalog.close();
      semantics.dispose();
    },
  );
  for (final sizes in [
    [const Size(411, 914), const Size(800, 1280)],
    [const Size(800, 1280), const Size(411, 914)],
  ]) {
    testWidgets(
      'resize ${sizes.first.width} to ${sizes.last.width} replaces asset owner and keeps timeout/retry current',
      (tester) async {
        final (catalog, cache) = await mount(tester, sizes.first);
        final old = artwork(tester);
        final previousId = old.state.identity!.assetId;
        await tester.pumpWidget(
          MaterialApp(
            home: MediaQuery(
              data: MediaQueryData(size: sizes.last),
              child: BlocProvider.value(
                value: catalog,
                child: SizedBox(
                  width: 180,
                  height: 240,
                  child: LibraryCard(
                    item: preview(),
                    state: catalog.state,
                    strings: const LibraryStrings('tr'),
                  ),
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        final selected = artwork(tester);
        expect(old.isClosed, isTrue);
        expect(selected.state.identity!.assetId, isNot(previousId));
        expect(cache.transfers, hasLength(2));
        await success(tester, cache, 0);
        expect(selected.state.status, ArtworkStatus.loading);
        await tester.pump(const Duration(milliseconds: 2005));
        expect(selected.state.status, ArtworkStatus.error);
        await tester.tap(
          find.byTooltip(const LibraryStrings('tr').retryArtwork),
        );
        await tester.pump();
        await tester.pump();
        expect(cache.transfers, hasLength(3));
        await success(tester, cache, 2);
        expect(selected.state.status, ArtworkStatus.loaded);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        await catalog.close();
      },
    );
  }
}
