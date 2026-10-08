import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:dartz/dartz.dart';
import 'package:file/file.dart' as files;
import 'package:file/local.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/io_client.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/library_diagnostics.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/cubit/library_state.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/library_strings.dart';
import 'package:tellpal_mobile_poc/features/library/presentation/widgets/library_card.dart';
import 'package:tellpal_mobile_poc/core/preview/presentation/widgets/preview_card.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import '../library/library_fixture.dart' show configuration;
import 'story_detail_fixture.dart';

final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);

class _Files implements FileSystem {
  _Files(this.directory);
  final String directory;
  @override
  Future<files.File> createFile(String name) async =>
      const LocalFileSystem().file('$directory/$name');
}

class _Repository extends NonStoringObjectProvider {
  final objects = <String, CacheObject>{};
  int nextId = 0;
  @override
  Future<CacheObject?> get(String key) async => objects[key];
  @override
  Future<dynamic> updateOrInsert(CacheObject object) async =>
      objects[object.key] = object.copyWith(
        id: object.id ?? objects[object.key]?.id ?? ++nextId,
      );
  @override
  Future<List<CacheObject>> getAllObjects() async => objects.values.toList();
  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    final before = objects.length;
    objects.removeWhere((_, object) => ids.contains(object.id));
    return before - objects.length;
  }
}

class _RealHttp extends HttpOverrides {}

class _ObservedRealCache extends LibraryImageCache {
  _ObservedRealCache(super.configuration, {super.clientFactory})
    : super.forTesting(artworkWait: settlementTimeout);

  static const settlementTimeout = Duration(seconds: 10);
  int promotionsStarted = 0;
  int promotionsCompleted = 0;
  Completer<void>? promotionGate;

  @override
  Future<void> rememberValidated(
    String immutableKey,
    String transferKey, {
    required bool Function() isCurrent,
  }) async {
    promotionsStarted++;
    try {
      final gate = promotionGate;
      if (gate != null) await gate.future;
      await super.rememberValidated(
        immutableKey,
        transferKey,
        isCurrent: isCurrent,
      );
    } finally {
      promotionsCompleted++;
    }
  }
}

class _Load {
  _Load(this.url, this.key);
  final String url, key;
  final response = StreamController<FileResponse>();
}

class _ControlledCache extends LibraryImageCache {
  _ControlledCache(String directory)
    : super.forTesting(
        Config(
          'rec-controlled',
          repo: NonStoringObjectProvider(),
          fileSystem: _Files(directory),
        ),
      );
  final loads = <_Load>[];
  final remembered = <String>[];
  int cleanupFailures = 0;
  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    final load = _Load(url, key!);
    loads.add(load);
    return load.response.stream;
  }

  @override
  Future<void> rememberValidated(
    String immutableKey,
    String transferKey, {
    required bool Function() isCurrent,
  }) async {
    if (isCurrent()) remembered.add(transferKey);
  }

  @override
  Future<void> discardFailedTransfer(String key, {bool failed = true}) async {
    if (cleanupFailures > 0) {
      cleanupFailures--;
      throw const FileSystemException('Controlled cleanup failure');
    }
  }
}

StoryDetail _detail(
  String url, {
  String ui = 'tr',
  String lang = 'tr',
  DateTime? expiresAt,
}) => StoryDetailModel.fromJson({
  ...payload(ui: ui, lang: lang),
  'summary': null,
  'credits': [],
  'recommendations': [
    {
      'contentId': 2,
      'externalKey': 'rec-2',
      'type': 'STORY',
      'experienceType': 'READING',
      'contentLanguage': lang,
      'title': 'Recommendation',
      'isFree': true,
      'isLocked': false,
      'covers': {
        'phone': {
          'assetId': 7,
          'kind': 'LISTING_COVER',
          'mimeType': 'image/png',
          'url': url,
          'expiresAt':
              (expiresAt ?? DateTime.now().add(const Duration(minutes: 10)))
                  .toIso8601String(),
        },
        'tablet': null,
      },
    },
  ],
});
void main() {
  late Directory directory;
  late HttpServer server;
  late File decoded;
  final requests = <String>[];
  WidgetTester? currentTester;
  Completer<void>? holdResponse;
  setUp(() async {
    directory = await Directory.systemTemp.createTemp('detail-rec-real');
    decoded = await File('${directory.path}/decoded.png').writeAsBytes(_png);
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    requests.clear();
    holdResponse = null;
    server.listen((request) async {
      requests.add(request.uri.path);
      await holdResponse?.future;
      request.response.headers.set(HttpHeaders.contentTypeHeader, 'image/png');
      request.response.headers.set(
        HttpHeaders.cacheControlHeader,
        'max-age=600',
      );
      request.response.contentLength = _png.length;
      request.response.add(_png);
      await request.response.close();
    });
  });
  tearDown(() async {
    await currentTester!.runAsync(() async {
      await libraryGetIt.reset();
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      await server.close(force: true);
      await directory.delete(recursive: true);
    });
  });
  Future<void> frames(WidgetTester tester) async {
    for (var frame = 0; frame < 30; frame++) {
      await tester.pump();
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 10)),
      );
    }
    await tester.pumpAndSettle();
  }

  Future<void> realFramesUntil(
    WidgetTester tester,
    bool Function() settled,
  ) async {
    final elapsed = Stopwatch()..start();
    while (!settled() &&
        elapsed.elapsed < _ObservedRealCache.settlementTimeout) {
      await tester.pump();
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 10)),
      );
    }
    expect(settled(), isTrue, reason: 'Real decode/publication did not settle');
    await tester.pumpAndSettle();
  }

  Future<void> realArtworkSettled(
    WidgetTester tester,
    _ObservedRealCache cache, {
    required int imageCount,
    required int promotionCount,
  }) => realFramesUntil(
    tester,
    () =>
        find
                .byWidgetPredicate((w) => w is RawImage && w.image != null)
                .evaluate()
                .length ==
            imageCount &&
        cache.promotionsStarted == promotionCount &&
        cache.promotionsCompleted == promotionCount,
  );

  Future<void> mountDetail(WidgetTester tester, StoryDetailCubit cubit) async {
    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider.value(value: cubit, child: const StoryDetailView()),
      ),
    );
    await tester.pumpAndSettle();
    await tester.drag(find.byType(ListView).first, const Offset(0, -900));
    await tester.pumpAndSettle();
  }

  for (final cancelOrigin in [false, true]) {
    testWidgets(
      'overlapping home and detail cancelOrigin=$cancelOrigin share one actual cold HTTP PNG and retain independent current owners',
      (tester) async {
        currentTester = tester;
        final config = configuration(language: 'en');
        libraryGetIt.registerSingleton<LibraryConfiguration>(config);
        libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
        libraryGetIt.registerSingleton<LibraryDiagnostics>(
          LibraryDiagnostics(),
        );
        final cache = _ObservedRealCache(
          Config(
            'overlap-real',
            repo: _Repository(),
            fileSystem: _Files(directory.path),
          ),
          clientFactory: () => IOClient(
            HttpOverrides.runWithHttpOverrides(() => HttpClient(), _RealHttp()),
          ),
        );
        libraryGetIt.registerSingleton<LibraryImageCache>(cache);
        holdResponse = Completer<void>();
        final item = _detail(
          'http://127.0.0.1:${server.port}/overlap',
          ui: 'en',
          lang: 'en',
        ).recommendations.single;
        final immutable =
            '${config.context.accessIdentity}:en:${item.contentId}:${item.covers.phone!.assetId}';
        bool closed = false;
        try {
          Future<void> mount(bool homeVisible) => tester.pumpWidget(
            MaterialApp(
              home: Column(
                children: [
                  homeVisible
                      ? SizedBox(
                          width: 180,
                          height: 240,
                          child: LibraryCard(
                            item: item,
                            state: LibraryState(
                              context: config.context,
                              attempt: 1,
                            ),
                            strings: const LibraryStrings('en'),
                          ),
                        )
                      : const SizedBox(),
                  SizedBox(
                    width: 180,
                    height: 240,
                    child: PreviewCard(
                      key: const ValueKey('current-detail-owner'),
                      item: item,
                      artworkContext: config.context,
                      metadataAttempt: 2,
                      lockedLabel: 'Locked',
                      retryArtworkLabel: 'Retry cover',
                      createArtwork: () => libraryGetIt<ArtworkCubit>(),
                      cache: () => cache,
                      diagnostics: () => libraryGetIt<LibraryDiagnostics>(),
                      onRefreshArtwork: () async {},
                    ),
                  ),
                ],
              ),
            ),
          );
          await mount(true);
          for (var frame = 0; frame < 10; frame++) {
            await tester.pump();
            await tester.runAsync(
              () => Future<void>.delayed(const Duration(milliseconds: 10)),
            );
          }
          expect(requests, ['/overlap']);
          expect(cache.ownedAttemptCount, 2);
          if (cancelOrigin) {
            await mount(false);
            await tester.pump();
            expect(cache.ownedAttemptCount, 2);
          }
          holdResponse!.complete();
          await realArtworkSettled(
            tester,
            cache,
            imageCount: cancelOrigin ? 1 : 2,
            promotionCount: cancelOrigin ? 1 : 2,
          );
          final images = find.byWidgetPredicate(
            (w) => w is RawImage && w.image != null,
          );
          expect(
            images,
            findsNWidgets(cancelOrigin ? 1 : 2),
            reason:
                'Real promotion started=${cache.promotionsStarted} '
                'completed=${cache.promotionsCompleted} '
                'owned=${cache.ownedAttemptCount}',
          );
          expect(
            tester.widgetList<RawImage>(images).map((w) => w.image!.width),
            cancelOrigin ? [2] : [2, 2],
          );
          expect(requests, ['/overlap']);
          final cubits = find
              .byType(CachedNetworkImage)
              .evaluate()
              .map((element) => element.read<ArtworkCubit>())
              .toList();
          expect(cubits, hasLength(cancelOrigin ? 1 : 2));
          expect(
            cubits.map((c) => c.state.identity!.catalogAttempt),
            cancelOrigin ? [2] : [1, 2],
          );
          expect(
            cubits.every((c) => c.state.status == ArtworkStatus.loaded),
            true,
          );
          expect(cache.ownedAttemptCount, 0);
          final warm = await tester.runAsync(
            () => cache.getFileFromCache(immutable),
          );
          expect(warm, isNotNull);
          expect(await tester.runAsync(() => warm!.file.readAsBytes()), _png);
          expect(tester.takeException(), isNull);
        } finally {
          if (!holdResponse!.isCompleted) holdResponse!.complete();
          await tester.pumpWidget(const SizedBox());
          final closing = cache.close().whenComplete(() => closed = true);
          for (var frame = 0; frame < 100 && !closed; frame++) {
            await tester.pump();
            await tester.runAsync(
              () => Future<void>.delayed(const Duration(milliseconds: 10)),
            );
          }
          expect(closed, true);
          await closing;
        }
      },
    );
  }

  for (final languages in [('en', 'en'), ('pt', 'pt'), ('tr', 'en')]) {
    testWidgets(
      'recommendation ${languages.$1}/${languages.$2} reuses actual home warm file and one real decoded source in current UI namespace',
      (tester) async {
        currentTester = tester;
        tester.view.physicalSize = const Size(411, 914);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final config = configuration();
        libraryGetIt.registerSingleton<LibraryConfiguration>(config);
        libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
        libraryGetIt.registerSingleton<LibraryDiagnostics>(
          LibraryDiagnostics(),
        );
        final cache = _ObservedRealCache(
          Config(
            'rec-warm',
            repo: _Repository(),
            fileSystem: _Files(directory.path),
          ),
          clientFactory: () => IOClient(
            HttpOverrides.runWithHttpOverrides(() => HttpClient(), _RealHttp()),
          ),
        );
        if (languages == ('en', 'en')) {
          cache.promotionGate = Completer<void>();
        }
        libraryGetIt.registerSingleton<LibraryImageCache>(
          cache,
          dispose: (cache) => cache.close(),
        );
        StoryDetailCubit? ownedCubit;
        try {
          final url = 'http://127.0.0.1:${server.port}/warm';
          final value = _detail(url, ui: languages.$1, lang: languages.$2);
          final preview = value.recommendations.single;
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: SizedBox(
                  width: 180,
                  height: 240,
                  child: LibraryCard(
                    item: preview,
                    state: LibraryState(
                      context: LibraryContext(
                        languages.$1,
                        config.context.accessIdentity,
                      ),
                      attempt: 9,
                    ),
                    strings: LibraryStrings(languages.$1),
                  ),
                ),
              ),
            ),
          );
          final immutable =
              '${config.context.accessIdentity}:${languages.$1}:2:7';
          if (cache.promotionGate != null) {
            await realFramesUntil(
              tester,
              () =>
                  find
                          .byWidgetPredicate(
                            (w) => w is RawImage && w.image != null,
                          )
                          .evaluate()
                          .length ==
                      1 &&
                  cache.promotionsStarted == 1,
            );
            expect(cache.promotionsCompleted, 0);
            expect(
              await tester.runAsync(() => cache.getFileFromCache(immutable)),
              isNull,
              reason: 'Decoded pixels precede asynchronous warm publication',
            );
            cache.promotionGate!.complete();
          }
          await realArtworkSettled(
            tester,
            cache,
            imageCount: 1,
            promotionCount: 1,
          );
          expect(
            find.byWidgetPredicate((w) => w is RawImage && w.image != null),
            findsOneWidget,
          );
          final warm = await tester.runAsync(
            () => cache.getFileFromCache(immutable),
          );
          expect(
            warm,
            isNotNull,
            reason:
                'Real promotion started=${cache.promotionsStarted} '
                'completed=${cache.promotionsCompleted} '
                'owned=${cache.ownedAttemptCount}',
          );
          expect(await tester.runAsync(() => warm!.file.readAsBytes()), _png);
          expect(requests, ['/warm']);
          final repo = ControlledDetailRepository();
          final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
          ownedCubit = cubit;
          final load = cubit.load(
            identity(ui: languages.$1, lang: languages.$2),
          );
          repo.responses.single.complete(right(value));
          await load;
          await mountDetail(tester, cubit);
          await realArtworkSettled(
            tester,
            cache,
            imageCount: 1,
            promotionCount: 2,
          );
          final card = find.byType(PreviewCard);
          final state = tester
              .element(
                find.descendant(
                  of: card,
                  matching: find.byType(CachedNetworkImage),
                ),
              )
              .read<ArtworkCubit>()
              .state;
          expect(requests, ['/warm']);
          expect(state.identity!.context.language, languages.$1);
          expect(
            state.identity!.context.accessIdentity,
            config.context.accessIdentity,
          );
          expect(
            tester.widget<PreviewCard>(card).item.contentLanguage,
            languages.$2,
          );
          final raw = find.descendant(
            of: card,
            matching: find.byWidgetPredicate(
              (w) => w is RawImage && w.image != null,
            ),
          );
          expect(tester.widget<RawImage>(raw).image!.width, 2);
          expect(requests, ['/warm']);
          expect(cache.ownedAttemptCount, 0);
          expect(tester.takeException(), isNull);
          await tester.pumpWidget(const SizedBox());
          await cubit.close();
        } finally {
          if (cache.promotionGate?.isCompleted == false) {
            cache.promotionGate!.complete();
          }
          await tester.pumpWidget(const SizedBox());
          if (ownedCubit != null && !ownedCubit.isClosed) {
            await ownedCubit.close();
          }
          var closed = false;
          final closing = cache.close().whenComplete(() => closed = true);
          for (var frame = 0; frame < 100 && !closed; frame++) {
            await tester.pump();
            await tester.runAsync(
              () => Future<void>.delayed(const Duration(milliseconds: 10)),
            );
          }
          expect(
            closed,
            true,
            reason:
                'Real package/IO cache must drain before test scheduler exits',
          );
          await closing;
        }
      },
    );
  }
  for (final scenario in [(false, false), (true, false), (true, true)]) {
    final expired = scenario.$1;
    final cleanupFailure = scenario.$2;
    testWidgets(
      'recommendation retry expired=$expired cleanupFailure=$cleanupFailure retains failed card during pending metadata and decodes fresh same-asset URL once',
      (tester) async {
        currentTester = tester;
        tester.view.physicalSize = const Size(411, 914);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final config = configuration();
        libraryGetIt.registerSingleton<LibraryConfiguration>(config);
        libraryGetIt.registerFactory<ArtworkCubit>(() => ArtworkCubit(config));
        libraryGetIt.registerSingleton<LibraryDiagnostics>(
          LibraryDiagnostics(),
        );
        final cache = _ControlledCache(directory.path);
        libraryGetIt.registerSingleton<LibraryImageCache>(
          cache,
          dispose: (cache) => cache.close(),
        );
        final repo = ControlledDetailRepository();
        final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
        final load = cubit.load(identity());
        repo.responses.single.complete(
          right(
            _detail(
              'https://example.invalid/old',
              expiresAt: expired
                  ? DateTime.now().subtract(const Duration(seconds: 1))
                  : null,
            ),
          ),
        );
        await load;
        await mountDetail(tester, cubit);
        expect(cache.loads, hasLength(1));
        final oldFinder = find.byType(CachedNetworkImage);
        final old = tester.widget<CachedNetworkImage>(oldFinder);
        final oldContext = tester.element(oldFinder);
        await tester.runAsync(() async {
          cache.loads.first.response.addError(
            StateError('Controlled actual provider failure'),
          );
        });
        await tester.pump();
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        final rec = find.byType(PreviewCard);
        final retry = find.descendant(
          of: rec,
          matching: find.byTooltip(const LibraryStrings('tr').retryArtwork),
        );
        if (cleanupFailure) {
          cache.cleanupFailures = 1;
          final diagnostics = libraryGetIt<LibraryDiagnostics>();
          final before = diagnostics.events.length;
          await tester.tap(retry);
          await tester.pump();
          await tester.pump();
          expect(tester.takeException(), isNull);
          expect(diagnostics.events.length, before + 1);
          expect(diagnostics.events.last['type'], 'FileSystemException');
          expect(cubit.state.recoveringArtwork, false);
          expect(cubit.state.metadataAttempt, 1);
          expect(repo.calls, hasLength(1));
          expect(cache.loads, hasLength(1));
          expect(retry, findsOneWidget);
        }
        await tester.tap(retry);
        await tester.pump();
        await tester.pump();
        expect(cubit.state.recoveringArtwork, true);
        expect(cubit.state.metadataAttempt, 1);
        expect(repo.calls, hasLength(2));
        expect(cache.loads.map((load) => load.url), [
          'https://example.invalid/old',
        ]);
        expect(
          find.descendant(of: rec, matching: find.byType(CachedNetworkImage)),
          findsNothing,
        );
        old.imageBuilder!(oldContext, const AssetImage('obsolete-provider'));
        await tester.pump();
        expect(cache.remembered, isEmpty);
        repo.responses[1].complete(
          right(_detail('https://example.invalid/fresh')),
        );
        await tester.pump();
        await tester.pump();
        expect(cubit.state.recoveringArtwork, false);
        expect(cubit.state.metadataAttempt, 2);
        expect(cache.loads.map((load) => load.url), [
          'https://example.invalid/old',
          'https://example.invalid/fresh',
        ]);
        final fresh = cache.loads.last;
        old.errorWidget!(
          oldContext,
          '',
          StateError('Obsolete recommendation error'),
        );
        old.imageBuilder!(
          oldContext,
          const AssetImage('obsolete-after-commit'),
        );
        await tester.pump();
        expect(cache.remembered, isEmpty);
        await tester.runAsync(() async {
          fresh.response.add(
            FileInfo(
              const LocalFileSystem().file(decoded.path),
              FileSource.Online,
              DateTime.now().add(const Duration(minutes: 1)),
              fresh.url,
            ),
          );
          unawaited(fresh.response.close());
        });
        await frames(tester);
        final raw = find.descendant(
          of: find.byType(PreviewCard),
          matching: find.byWidgetPredicate(
            (w) => w is RawImage && w.image != null,
          ),
        );
        expect(tester.widget<RawImage>(raw).image!.width, 2);
        expect(cache.loads, hasLength(2));
        expect(cache.remembered, [fresh.key]);
        expect(repo.calls, hasLength(2));
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
      },
    );
  }
}
