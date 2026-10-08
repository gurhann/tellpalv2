import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:cached_network_image/cached_network_image.dart';
import 'package:crypto/crypto.dart';
import 'package:dartz/dartz.dart';
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/core/network/story_detail_image_cache.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_state.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/view/story_detail_view.dart';
import '../library/library_fixture.dart' show configuration;
import 'story_detail_fixture.dart';

final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGP4z8AARAwQCgAf7gP9i18U1AAAAABJRU5ErkJggg==',
);

StoryDetail _heroDetail(String url) => StoryDetailModel.fromJson({
  ...payload(),
  'hero': {
    'assetId': 1,
    'checksumSha256': sha256.convert(_png).toString(),
    'byteSize': _png.length,
    'mimeType': 'image/png',
    'url': url,
    'expiresAt': DateTime.now()
        .add(const Duration(minutes: 10))
        .toIso8601String(),
    'width': 2,
    'height': 2,
  },
});

class _Files implements FileSystem {
  final memory = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => memory.file('/$name');
}

class _Load {
  _Load(this.url, this.key);
  final String url;
  final String? key;
  final response = StreamController<FileResponse>();
}

class _HeroCache extends StoryDetailImageCache {
  _HeroCache()
    : super.forTesting(
        Config(
          'recovery-provider-test',
          repo: NonStoringObjectProvider(),
          fileSystem: _Files(),
        ),
      );
  final loads = <_Load>[];
  final remembered = <String>[];
  final discarded = <String>[];
  final memory = MemoryFileSystem();
  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    final load = _Load(url, key);
    loads.add(load);
    return load.response.stream;
  }

  void succeed(_Load load) {
    final file = memory.file('/${loads.indexOf(load)}.png')
      ..writeAsBytesSync(_png);
    load.response.add(
      FileInfo(
        file,
        FileSource.Online,
        DateTime.now().add(const Duration(minutes: 10)),
        load.url,
      ),
    );
    unawaited(load.response.close());
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
    if (failed) discarded.add(key);
  }

  @override
  Future<void> close() async {
    for (final load in loads) {
      unawaited(load.response.close());
    }
    await super.close();
  }
}

class _DecodeGateBinding extends AutomatedTestWidgetsFlutterBinding {
  Completer<void>? started;
  Completer<void>? release;
  Completer<void>? decoded;
  @override
  Future<ui.Codec> instantiateImageCodecWithSize(
    ui.ImmutableBuffer buffer, {
    ui.TargetImageSizeCallback? getTargetSize,
  }) async {
    final gate = release;
    final completion = decoded;
    if (gate != null) {
      started!.complete();
      await gate.future;
    }
    final codec = await super.instantiateImageCodecWithSize(
      buffer,
      getTargetSize: getTargetSize,
    );
    if (gate != null) completion!.complete();
    return codec;
  }
}

void main() {
  final binding = _DecodeGateBinding();
  tearDown(() async {
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
    await libraryGetIt.reset();
  });
  for (final outcome in ['failure', 'timeout']) {
    testWidgets(
      'recycled loaded hero starts a bounded current consumer and reports $outcome',
      (tester) async {
        final config = configuration();
        libraryGetIt.registerSingleton<LibraryConfiguration>(config);
        final cache = _HeroCache();
        libraryGetIt.registerSingleton<StoryDetailImageCache>(
          cache,
          dispose: (cache) => cache.close(),
        );
        final repo = ControlledDetailRepository();
        final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
        final load = cubit.load(identity());
        repo.responses[0].complete(
          right(_heroDetail('https://example.invalid/recycled-$outcome')),
        );
        await load;
        Widget screen() => MaterialApp(
          home: BlocProvider.value(
            value: cubit,
            child: const StoryDetailView(),
          ),
        );
        await tester.pumpWidget(screen());
        await tester.runAsync(() async => cache.succeed(cache.loads.single));
        for (var i = 0; i < 10; i++) {
          await tester.runAsync(
            () async => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump(const Duration(milliseconds: 100));
          if (cubit.state.heroStatus == StoryHeroStatus.loaded) break;
        }
        expect(cubit.state.heroStatus, StoryHeroStatus.loaded);
        final oldAttempt = cubit.state.heroAttempt;
        await tester.pumpWidget(const SizedBox());
        await tester.pumpWidget(screen());
        await tester.pump();
        await tester.pump();
        expect(cubit.state.heroStatus, StoryHeroStatus.loading);
        expect(cubit.state.heroAttempt, greaterThan(oldAttempt));
        expect(cache.loads, hasLength(2));
        expect(cache.loads.last.key, isNot(cache.loads.first.key));
        cubit.artworkLoaded(oldAttempt);
        cubit.artworkFailed(oldAttempt);
        expect(cubit.state.heroStatus, StoryHeroStatus.loading);
        if (outcome == 'failure') {
          cache.loads.last.response.addError(
            StateError('New consumer source failed'),
          );
          for (
            var i = 0;
            i < 10 && cubit.state.heroStatus != StoryHeroStatus.error;
            i++
          ) {
            await tester.runAsync(
              () async =>
                  Future<void>.delayed(const Duration(milliseconds: 10)),
            );
            await tester.pump(const Duration(milliseconds: 100));
          }
        } else {
          await tester.pump(
            config.artworkWait + const Duration(milliseconds: 1),
          );
          await tester.pump();
        }
        expect(cubit.state.heroStatus, StoryHeroStatus.error);
        await tester.pump();
        final retry = find.widgetWithText(
          TextButton,
          const StoryDetailStrings('tr').retryArtwork,
        );
        expect(retry, findsOneWidget);
        expect(tester.widget<TextButton>(retry).onPressed, isNotNull);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
      },
    );
  }
  testWidgets(
    'disposed hero evicts its admitted pending decoder before a late real codec completes',
    (tester) async {
      final config = configuration();
      libraryGetIt.registerSingleton<LibraryConfiguration>(config);
      final cache = _HeroCache();
      libraryGetIt.registerSingleton<StoryDetailImageCache>(
        cache,
        dispose: (cache) => cache.close(),
      );
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
      final load = cubit.load(identity());
      repo.responses[0].complete(
        right(_heroDetail('https://example.invalid/pending-codec')),
      );
      await load;
      binding.started = Completer<void>();
      binding.release = Completer<void>();
      binding.decoded = Completer<void>();
      addTearDown(() {
        if (!binding.release!.isCompleted) binding.release!.complete();
        binding.release = null;
        binding.started = null;
        binding.decoded = null;
      });
      await tester.pumpWidget(
        MaterialApp(
          home: BlocProvider.value(
            value: cubit,
            child: const StoryDetailView(),
          ),
        ),
      );
      final image = tester.widget<CachedNetworkImage>(
        find.byType(CachedNetworkImage),
      );
      final provider = ResizeImage.resizeIfNeeded(
        image.memCacheWidth,
        image.memCacheHeight,
        CachedNetworkImageProvider(
          image.imageUrl,
          cacheKey: image.cacheKey,
          cacheManager: cache,
        ),
      );
      final key = await provider.obtainKey(ImageConfiguration.empty);
      await tester.runAsync(() async => cache.succeed(cache.loads.single));
      for (var i = 0; i < 20 && !binding.started!.isCompleted; i++) {
        await tester.runAsync(
          () async => Future<void>.delayed(const Duration(milliseconds: 10)),
        );
        await tester.pump();
      }
      expect(binding.started!.isCompleted, isTrue);
      expect(
        PaintingBinding.instance.imageCache.statusForKey(key).pending,
        isTrue,
      );
      await tester.pumpWidget(const SizedBox());
      await tester.pump();
      expect(PaintingBinding.instance.imageCache.containsKey(key), isFalse);
      binding.release!.complete();
      for (var i = 0; i < 20 && !binding.decoded!.isCompleted; i++) {
        await tester.runAsync(
          () async => Future<void>.delayed(const Duration(milliseconds: 10)),
        );
        await tester.pump();
      }
      expect(
        binding.decoded!.isCompleted,
        isTrue,
        reason:
            'The actual engine codec must finish before late-cache assertions',
      );
      await tester.pump();
      expect(PaintingBinding.instance.imageCache.containsKey(key), isFalse);
      expect(
        PaintingBinding.instance.imageCache.statusForKey(key).live,
        isFalse,
      );
      expect(cache.loads, hasLength(1));
      expect(cache.remembered, isEmpty);
      expect(tester.takeException(), isNull);
      await cubit.close();
    },
  );
  test(
    'artwork renewal rejects pending metadata callbacks and starts fresh hero only after same-context success',
    () async {
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), configuration());
      final initial = cubit.load(identity());
      repo.responses[0].complete(
        right(_heroDetail('https://example.invalid/old')),
      );
      await initial;
      final oldAttempt = cubit.state.heroAttempt;
      cubit.artworkFailed(oldAttempt);
      final retained = cubit.state.detail;
      final renewal = cubit.load(identity(), artworkOnly: true);
      final pendingAttempt = cubit.state.heroAttempt;
      expect(cubit.state.recoveringArtwork, isTrue);
      expect(cubit.state.detail, same(retained));
      cubit.artworkLoaded(pendingAttempt);
      expect(cubit.state.heroStatus, isNot(StoryHeroStatus.loaded));
      cubit.setVisible(false);
      cubit.setVisible(true);
      cubit.artworkLoaded(cubit.state.heroAttempt);
      expect(cubit.state.heroStatus, StoryHeroStatus.loading);
      cubit.artworkFailed(cubit.state.heroAttempt);
      expect(cubit.state.recoveringArtwork, isTrue);
      expect(cubit.state.heroStatus, isNot(StoryHeroStatus.error));
      await cubit.load(identity(), artworkOnly: true);
      expect(repo.calls, [identity(), identity()]);
      repo.responses[1].complete(
        right(_heroDetail('https://example.invalid/fresh')),
      );
      await renewal;
      expect(cubit.state.recoveringArtwork, isFalse);
      expect(cubit.state.heroStatus, StoryHeroStatus.loading);
      cubit.artworkLoaded(oldAttempt);
      cubit.artworkLoaded(pendingAttempt);
      expect(cubit.state.heroStatus, StoryHeroStatus.loading);
      cubit.artworkLoaded(cubit.state.heroAttempt);
      expect(cubit.state.heroStatus, StoryHeroStatus.loaded);
      expect(cubit.state.detail!.hero!.url, 'https://example.invalid/fresh');
      await cubit.close();
    },
  );
  for (final failure in [FailureKind.network, FailureKind.unavailable]) {
    test(
      'pending artwork renewal $failure rejects obsolete hero callbacks and preserves only eligible metadata',
      () async {
        final repo = ControlledDetailRepository();
        final cubit = StoryDetailCubit(GetStoryDetail(repo), configuration());
        final initial = cubit.load(identity());
        repo.responses[0].complete(
          right(_heroDetail('https://example.invalid/old')),
        );
        await initial;
        cubit.artworkFailed(cubit.state.heroAttempt);
        final retained = cubit.state.detail;
        final renewal = cubit.load(identity(), artworkOnly: true);
        final pending = cubit.state.heroAttempt;
        repo.responses[1].complete(left(Failure(failure)));
        await renewal;
        cubit.artworkLoaded(pending);
        expect(cubit.state.recoveringArtwork, isFalse);
        if (failure == FailureKind.unavailable) {
          expect(cubit.state.status, StoryDetailStatus.unavailable);
          expect(cubit.state.detail, isNull);
        } else {
          expect(cubit.state.status, StoryDetailStatus.loaded);
          expect(cubit.state.detail, same(retained));
          expect(cubit.state.heroStatus, StoryHeroStatus.error);
        }
        expect(repo.calls, [identity(), identity()]);
        await cubit.close();
      },
    );
  }
  testWidgets(
    'pending renewal installs no old URL provider; fresh metadata yields one decoded current hero and stale callbacks cannot promote',
    (tester) async {
      final config = configuration();
      libraryGetIt.registerSingleton<LibraryConfiguration>(config);
      final cache = _HeroCache();
      libraryGetIt.registerSingleton<StoryDetailImageCache>(
        cache,
        dispose: (cache) => cache.close(),
      );
      final repo = ControlledDetailRepository();
      final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
      final initial = cubit.load(identity());
      repo.responses[0].complete(
        right(_heroDetail('https://example.invalid/widget-old')),
      );
      await initial;
      await tester.pumpWidget(
        MaterialApp(
          home: BlocProvider.value(
            value: cubit,
            child: BlocBuilder<StoryDetailCubit, StoryDetailState>(
              builder: (context, state) => SizedBox(
                width: 300,
                height: 300,
                child: StoryDetailHero(
                  key: ValueKey('${state.attempt}:${state.heroAttempt}'),
                  state: state,
                  strings: const StoryDetailStrings('tr'),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(cache.loads, hasLength(1));
      final oldWidget = tester.widget<CachedNetworkImage>(
        find.byType(CachedNetworkImage),
      );
      cubit.artworkFailed(cubit.state.heroAttempt);
      await tester.pump();
      final renewal = cubit.load(identity(), artworkOnly: true);
      await tester.pump();
      final staleContext = tester.element(find.byType(StoryDetailHero));
      oldWidget.imageBuilder!(
        staleContext,
        const AssetImage('unused-stale-provider'),
      );
      oldWidget.errorWidget!(
        staleContext,
        '',
        StateError('Obsolete image completion'),
      );
      await tester.runAsync(() async => cache.succeed(cache.loads.first));
      await tester.pump();
      expect(cache.loads, hasLength(1));
      expect(find.byType(CachedNetworkImage), findsNothing);
      expect(cubit.state.recoveringArtwork, isTrue);
      expect(cache.remembered, isEmpty);
      expect(cache.discarded, isEmpty);
      repo.responses[1].complete(
        right(_heroDetail('https://example.invalid/widget-fresh')),
      );
      await renewal;
      await tester.pump();
      await tester.pump();
      expect(cache.loads, hasLength(2));
      expect(cache.loads.last.url, 'https://example.invalid/widget-fresh');
      await tester.runAsync(() async => cache.succeed(cache.loads.last));
      // Engine decode and widget frames use different schedulers in widget tests.
      for (
        var i = 0;
        i < 20 && cubit.state.heroStatus != StoryHeroStatus.loaded;
        i++
      ) {
        await tester.pump(const Duration(milliseconds: 16));
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 10)),
        );
      }
      await tester.pump();
      expect(cubit.state.heroStatus, StoryHeroStatus.loaded);
      expect(cache.loads, hasLength(2));
      final decoded = find.byWidgetPredicate(
        (widget) => widget is RawImage && widget.image != null,
      );
      expect(decoded, findsWidgets);
      final raw = tester.widget<RawImage>(decoded.first);
      expect(raw.image!.width, 2);
      expect(raw.image!.height, 2);
      expect(cache.remembered, [cache.loads.last.key]);
      expect(repo.calls, [identity(), identity()]);
      await tester.pumpWidget(const SizedBox());
      await cubit.close();
    },
  );
  for (final failure in [FailureKind.network, FailureKind.unavailable]) {
    testWidgets(
      'failed artwork renewal $failure renders retained error or withdrawn state without restarting old provider',
      (tester) async {
        final config = configuration();
        libraryGetIt.registerSingleton<LibraryConfiguration>(config);
        final cache = _HeroCache();
        libraryGetIt.registerSingleton<StoryDetailImageCache>(
          cache,
          dispose: (cache) => cache.close(),
        );
        final repo = ControlledDetailRepository();
        final cubit = StoryDetailCubit(GetStoryDetail(repo), config);
        final initial = cubit.load(identity());
        repo.responses[0].complete(
          right(_heroDetail('https://example.invalid/withdrawn-old')),
        );
        await initial;
        await tester.pumpWidget(
          MaterialApp(
            home: BlocProvider.value(
              value: cubit,
              child: const StoryDetailView(),
            ),
          ),
        );
        await tester.pump();
        expect(cache.loads, hasLength(1));
        final oldWidget = tester.widget<CachedNetworkImage>(
          find.byType(CachedNetworkImage),
        );
        cubit.artworkFailed(cubit.state.heroAttempt);
        await tester.pump();
        final retained = cubit.state.detail;
        final renewal = cubit.load(identity(), artworkOnly: true);
        await tester.pump();
        expect(find.byType(CachedNetworkImage), findsNothing);
        expect(cache.loads, hasLength(1));
        final retry = tester.widget<TextButton>(
          find.widgetWithText(
            TextButton,
            const StoryDetailStrings('tr').retryArtwork,
          ),
        );
        expect(retry.onPressed, isNull);
        repo.responses[1].complete(left(Failure(failure)));
        await renewal;
        await tester.pump();
        final currentContext = tester.element(find.byType(StoryDetailView));
        oldWidget.imageBuilder!(
          currentContext,
          const AssetImage('unused-old-provider'),
        );
        oldWidget.errorWidget!(
          currentContext,
          '',
          StateError('Old image callback after renewal failure'),
        );
        await tester.runAsync(() async => cache.succeed(cache.loads.first));
        await tester.pump();
        expect(cache.loads, hasLength(1));
        expect(cache.remembered, isEmpty);
        expect(cubit.state.recoveringArtwork, isFalse);
        if (failure == FailureKind.network) {
          expect(cubit.state.detail, same(retained));
          expect(
            find.text(const StoryDetailStrings('tr').artworkError),
            findsOneWidget,
          );
          expect(
            tester
                .widget<TextButton>(
                  find.widgetWithText(
                    TextButton,
                    const StoryDetailStrings('tr').retryArtwork,
                  ),
                )
                .onPressed,
            isNotNull,
          );
        } else {
          expect(cubit.state.detail, isNull);
          expect(find.byType(StoryDetailHero), findsNothing);
          expect(
            find.text(const StoryDetailStrings('tr').unavailable),
            findsOneWidget,
          );
        }
        expect(repo.calls, [identity(), identity()]);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
      },
    );
  }
}
