import 'dart:async';
import 'dart:io';
import 'package:tellpal_mobile_poc/app/router.dart';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_configuration.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/use_case/get_story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/cubit/story_detail_cubit.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/widgets/story_detail_hero.dart';
import 'package:tellpal_mobile_poc/features/story_detail/presentation/story_detail_strings.dart';
import '../story_detail/story_detail_fixture.dart' as story;
import '../library/library_fixture.dart' as library_fixture;
import 'package:dartz/dartz.dart' show right;
import 'package:tellpal_mobile_poc/app/injection.dart';
import 'package:tellpal_mobile_poc/core/presentation/reading_artwork_capability.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/validate_reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/data/reading_image_consumer_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/data/reading_image_consumer_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/reading_image_admission.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/reading_image_consumer_ownership.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/load_reading_image.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_state.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/presentation/reading_image_consumer_widget.dart';
import 'consumer_fixture.dart';
import 'fixture.dart' as descriptor;
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest_digest.dart';

class Source extends ReadingImageConsumerDataSource {
  Source(this.fixture, {ReadingImageConsumerConfiguration? configuration})
    : super(configuration ?? consumerConfiguration());
  final ConsumerFixture fixture;
  int admissions = 0, reads = 0, statusChecks = 0;
  bool denied = false, corrupt = false;
  String? readPage;
  Completer<void>? block, statusBlock, bytesBlock;
  @override
  Future<Map<String, dynamic>> json(
    String path,
    Map<String, String> query,
    ReadingImageCancellation cancellation,
  ) async {
    if (path == 'manifest') {
      admissions++;
    } else {
      statusChecks++;
    }
    await (path == 'status' ? statusBlock : block)?.future;
    cancellation.check();
    if (denied) throw StateError('denied');
    return fixture.json;
  }

  @override
  Future<Uint8List> read(
    String path,
    Map<String, String> query,
    ReadingImageCancellation cancellation,
    int limit,
    String mime,
  ) async {
    reads++;
    readPage = query['pageNumber'];
    await bytesBlock?.future;
    cancellation.check();
    return corrupt ? Uint8List.fromList([1, 2, 3]) : fixture.bytes;
  }
}

ReadingImageConsumerCubit cubit(
  Source source, {
  ReadingImageConsumerOwnership? gate,
}) {
  final config = source.configuration;
  return ReadingImageConsumerCubit(
    LoadReadingImage(ReadingImageConsumerRepositoryImpl(source, config)),
    config,
    gate ?? ReadingImageConsumerOwnership(),
  );
}

Future<void> load(
  ReadingImageConsumerCubit c, {
  double dpr = 1,
  int page = 0,
}) => c.load(
  contentId: 71,
  language: 'tr',
  role: page == 0
      ? ReadingImageRole.readingDetail
      : ReadingImageRole.readingPage,
  pageNumber: page,
  width: 4,
  height: 4,
  dpr: dpr,
  fit: ReadingImageFit.contain,
);
Future<void> settle(WidgetTester tester) async {
  for (var n = 0; n < 35; n++) {
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 5)),
    );
    await tester.pump();
  }
}

void main() {
  tearDown(() async {
    await libraryGetIt.reset();
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
  });
  for (final format in ['JPEG', 'WEBP']) {
    test('preserve $format gerçek boyut/digest policy doğrulaması', () {
      final f = ConsumerFixture(format: format).admission;
      expect(
        const ValidateReadingImageManifest()(
          manifest: f.manifest,
          expected: f.context,
        ).isRight(),
        isTrue,
      );
    });
  }
  test('null/eksik policy eski PNG; unknown policy reddedilir', () {
    final f = ConsumerFixture(policy: 'PNG_FIT');
    final descriptor =
        (f.json['delivery'] as Map<String, dynamic>)['descriptor']
            as Map<String, dynamic>;
    for (final values
        in (descriptor['profiles'] as Map<String, dynamic>).values) {
      (values as List).first.remove('formatPolicy');
    }
    expect(
      f.admission.context.profiles.values.first.single.formatPolicy,
      'PNG_FIT',
    );
    for (final values
        in (descriptor['profiles'] as Map<String, dynamic>).values) {
      (values as List).first['formatPolicy'] = 'unknown';
    }
    expect(
      const ValidateReadingImageManifest()(
        manifest: f.admission.manifest,
        expected: f.admission.context,
      ).isLeft(),
      isTrue,
    );
  });
  test('geç admission/callback yeni attempt durumunu dolduramaz', () async {
    final source = Source(ConsumerFixture(policy: 'PNG_FIT'));
    final c = cubit(source);
    source.block = Completer();
    final first = load(c);
    await Future<void>.delayed(Duration.zero);
    final retry = load(c);
    source.block!.complete();
    source.block = null;
    await first;
    await retry;
    expect(c.state.attempt, 2);
    expect(c.state.status, ReadingImageConsumerStatus.admitted);
    c.decoded(1);
    expect(c.state.status, ReadingImageConsumerStatus.admitted);
    await c.close();
  });
  test('fresh admission reddi var olan warm dosyayı çizime açmaz', () async {
    final source = Source(ConsumerFixture(policy: 'PNG_FIT'));
    final c = cubit(source);
    await load(c);
    final cache = c.cache;
    await cache.putFile('', source.fixture.bytes, key: c.state.immutableKey!);
    source.denied = true;
    await c.retry();
    expect(c.state.status, ReadingImageConsumerStatus.error);
    expect(source.reads, 0);
    await c.close();
    expect(source.admissions, 2);
  });
  test(
    'app scope permit native decode ve cache cleanup bitene kadar kalır',
    () async {
      final gate = ReadingImageConsumerOwnership();
      final first = cubit(
        Source(ConsumerFixture(policy: 'PNG_FIT')),
        gate: gate,
      );
      await load(first);
      final decode = Completer<void>();
      first.ownDecode(decode.future);
      first.suspend();
      final second = cubit(
        Source(ConsumerFixture(policy: 'PNG_FIT')),
        gate: gate,
      );
      final waiting = load(second);
      await Future<void>.delayed(Duration.zero);
      expect(second.state.status, ReadingImageConsumerStatus.loading);
      expect(gate.activeOwners, 1);
      decode.complete();
      await first.close();
      await waiting;
      expect(second.state.status, ReadingImageConsumerStatus.admitted);
      await second.close();
    },
  );
  for (final format in ['JPEG', 'WEBP']) {
    testWidgets(
      'CachedNetworkImage gerçek $format decode/promotion ve warm tekrar',
      (tester) async {
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetDevicePixelRatio);
        final source = Source(ConsumerFixture(format: format));
        final c = cubit(source);
        libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
        final request = ReadingArtworkRequest(
          contentId: 71,
          language: 'tr',
          fit: BoxFit.contain,
          visible: true,
          attempt: 0,
          onLoaded: () {},
          onError: () {},
        );
        await tester.pumpWidget(
          MaterialApp(
            home: Center(
              child: SizedBox(
                width: 4,
                height: 4,
                child: ReadingImageConsumerWidget(request: request),
              ),
            ),
          ),
        );
        await settle(tester);
        expect(find.byType(CachedNetworkImage), findsOneWidget);
        expect(c.state.status, ReadingImageConsumerStatus.loaded);
        expect(source.reads, 1);
        final key = c.state.immutableKey!;
        await tester.runAsync(() async {
          expect(await c.cache.getFileFromCache(key), isNotNull);
        });
        unawaited(c.retry());
        await tester.pump();
        await settle(tester);
        expect(c.state.status, ReadingImageConsumerStatus.loaded);
        expect(source.admissions, 2);
        expect(source.reads, 1);
        await tester.pumpWidget(const SizedBox());
        await settle(tester);
        var closed = false;
        unawaited(c.close().then((_) => closed = true));
        await settle(tester);
        expect(closed, isTrue);
      },
    );
  }
  testWidgets('bozuk bytes READY/promotion kazanmaz', (tester) async {
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetDevicePixelRatio);
    final source = Source(ConsumerFixture(policy: 'PNG_FIT'))..corrupt = true;
    final c = cubit(source);
    libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: SizedBox(
            width: 4,
            height: 4,
            child: ReadingImageConsumerWidget(
              request: ReadingArtworkRequest(
                contentId: 71,
                language: 'tr',
                fit: BoxFit.contain,
                visible: true,
                attempt: 0,
                onLoaded: () {},
                onError: () {},
              ),
            ),
          ),
        ),
      ),
    );
    await settle(tester);
    expect(c.state.status, ReadingImageConsumerStatus.error);
    expect(find.byIcon(Icons.refresh), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
    await settle(tester);
    var closed = false;
    unawaited(c.close().then((_) => closed = true));
    await settle(tester);
    expect(closed, isTrue);
  });
  test(
    'gerçek lease Timer görsel admission ve cache sahipliğini temizler',
    () async {
      final source = Source(
        ConsumerFixture(policy: 'PNG_FIT', leaseMillis: 20),
      );
      final c = cubit(source);
      await load(c);
      expect(c.state.status, ReadingImageConsumerStatus.admitted);
      await Future<void>.delayed(const Duration(milliseconds: 50));
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      expect(c.current(c.state.attempt), isFalse);
      await c.close();
    },
  );
  test('page slot çarpı DPR gerçek decode sınırını belirler', () async {
    final source = Source(ConsumerFixture(policy: 'PNG_FIT'));
    final c = cubit(source);
    await load(c, dpr: 2, page: 1);
    expect(c.state.selection!.geometry.decodeWidth, 8);
    expect(c.state.selection!.geometry.decodeHeight, 8);
    await load(c, dpr: 3, page: 1);
    expect(c.state.status, ReadingImageConsumerStatus.error);
    expect(source.reads, 0);
    await c.close();
  });
  testWidgets(
    'örtülen route ve background mevcut çizimi kaldırır; retry fresh admission alır',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetDevicePixelRatio);
      final source = Source(ConsumerFixture(format: 'JPEG'));
      final c = cubit(source);
      libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
      final navigator = GlobalKey<NavigatorState>();
      await tester.pumpWidget(
        MaterialApp(
          navigatorKey: navigator,
          home: Center(
            child: SizedBox(
              width: 4,
              height: 4,
              child: ReadingImageConsumerWidget(
                request: ReadingArtworkRequest(
                  contentId: 71,
                  language: 'tr',
                  fit: BoxFit.contain,
                  visible: true,
                  attempt: 0,
                  onLoaded: () {},
                  onError: () {},
                ),
              ),
            ),
          ),
        ),
      );
      await settle(tester);
      expect(c.state.status, ReadingImageConsumerStatus.loaded);
      unawaited(
        navigator.currentState!.push(
          MaterialPageRoute<void>(builder: (_) => const SizedBox()),
        ),
      );
      await settle(tester);
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      expect(find.byType(CachedNetworkImage), findsNothing);
      navigator.currentState!.pop();
      await settle(tester);
      unawaited(c.retry());
      await settle(tester);
      expect(source.admissions, 2);
      expect(c.state.status, ReadingImageConsumerStatus.loaded);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      await settle(tester);
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      expect(find.byType(CachedNetworkImage), findsNothing);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpWidget(const SizedBox());
      await settle(tester);
      var closed = false;
      unawaited(c.close().then((_) => closed = true));
      await settle(tester);
      expect(closed, isTrue);
    },
  );
  testWidgets(
    'legacy hero null olan detail opt-in ile normal tüketiciyi çağırır',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetDevicePixelRatio);
      final source = Source(ConsumerFixture(format: 'WEBP'));
      final c = cubit(source);
      libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
      final repo = story.ControlledDetailRepository();
      final detail = StoryDetailCubit(
        GetStoryDetail(repo),
        library_fixture.configuration(),
      );
      final pending = detail.load(story.identity(id: 71));
      repo.responses.single.complete(right(story.detail(id: 71)));
      await pending;
      var bindings = 0;
      Widget view({required bool optedIn}) {
        final hero = BlocProvider.value(
          value: detail,
          child: StoryDetailHero(
            state: detail.state,
            strings: const StoryDetailStrings('tr'),
            fit: BoxFit.contain,
          ),
        );
        return MaterialApp(
          home: Center(
            child: SizedBox(
              width: 4,
              height: 4,
              child: optedIn
                  ? ReadingArtworkCapability(
                      builder: (_, request) {
                        bindings++;
                        expect(request.contentId, 71);
                        expect(request.language, 'tr');
                        return ReadingImageConsumerWidget(request: request);
                      },
                      child: hero,
                    )
                  : hero,
            ),
          ),
        );
      }

      await tester.pumpWidget(view(optedIn: true));
      await settle(tester);
      expect(bindings, greaterThan(0));
      expect(source.admissions, 1);
      expect(c.state.status, ReadingImageConsumerStatus.loaded);
      expect(source.reads, 1);
      await tester.pumpWidget(const SizedBox());
      await settle(tester);
      // Varsayılan detail yolu eski widget test grubuyla ayrıca doğrulanır.
      await detail.close();
      var closed = false;
      unawaited(c.close().then((_) => closed = true));
      await settle(tester);
      expect(closed, isTrue);
    },
  );

  test(
    'aynı rendition kaynağını paylaşan üç sayfa doğru binding ile istenir',
    () async {
      final source = Source(ConsumerFixture(format: 'WEBP', pages: 3));
      final c = cubit(source);
      await load(c, page: 3);
      final admission = c.state.admission!.admission;
      final selection = c.state.selection!;
      final binding = admission.manifest.images
          .singleWhere((e) => e.source.binding.pageNumber == 3)
          .source
          .binding;
      expect(
        (await c.loadImage.bytes(
          admission,
          binding,
          selection,
          ReadingImageCancellation(),
        )).isRight(),
        isTrue,
      );
      expect(source.readPage, '3');
      await c.close();
    },
  );
  test('hesap değişimi admission ve çizim yetkisini iptal eder', () async {
    final changes = StreamController<void>();
    final source = Source(
      ConsumerFixture(policy: 'PNG_FIT'),
      configuration: consumerConfiguration(changes: changes.stream),
    );
    final c = cubit(source);
    await load(c);
    changes.add(null);
    await Future<void>.delayed(Duration.zero);
    expect(c.state.status, ReadingImageConsumerStatus.suspended);
    await c.close();
    await changes.close();
  });

  testWidgets('gerçek lease Timer mevcut provider çizimini kaldırır', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetDevicePixelRatio);
    final source = Source(ConsumerFixture(format: 'JPEG', leaseMillis: 50));
    final c = cubit(source);
    libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: SizedBox(
            width: 4,
            height: 4,
            child: ReadingImageConsumerWidget(
              request: ReadingArtworkRequest(
                contentId: 71,
                language: 'tr',
                fit: BoxFit.contain,
                visible: true,
                attempt: 0,
                onLoaded: () {},
                onError: () {},
              ),
            ),
          ),
        ),
      ),
    );
    await settle(tester);
    expect(c.state.status, ReadingImageConsumerStatus.loaded);
    await tester.pump(const Duration(milliseconds: 60));
    await settle(tester);
    expect(c.state.status, ReadingImageConsumerStatus.suspended);
    expect(find.byType(CachedNetworkImage), findsNothing);
    await tester.pumpWidget(const SizedBox());
    await settle(tester);
    var closed = false;
    unawaited(c.close().then((_) => closed = true));
    await settle(tester);
    expect(closed, isTrue);
  });
  testWidgets(
    'admission beklerken örtülen route işi iptal eder ve erken kapasite açmaz',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetDevicePixelRatio);
      final source = Source(ConsumerFixture(format: 'JPEG'))
        ..block = Completer<void>();
      final gate = ReadingImageConsumerOwnership();
      final c = cubit(source, gate: gate);
      libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
      final navigator = GlobalKey<NavigatorState>();
      await tester.pumpWidget(
        MaterialApp(
          navigatorKey: navigator,
          home: Center(
            child: SizedBox(
              width: 4,
              height: 4,
              child: ReadingImageConsumerWidget(
                request: ReadingArtworkRequest(
                  contentId: 71,
                  language: 'tr',
                  fit: BoxFit.contain,
                  visible: true,
                  attempt: 0,
                  onLoaded: () {},
                  onError: () {},
                ),
              ),
            ),
          ),
        ),
      );
      await settle(tester);
      expect(source.admissions, 1);
      expect(c.state.status, ReadingImageConsumerStatus.loading);
      unawaited(
        navigator.currentState!.push(
          MaterialPageRoute<void>(builder: (_) => const SizedBox()),
        ),
      );
      await settle(tester);
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      expect(gate.activeOwners, 1);
      source.block!.complete();
      await settle(tester);
      expect(source.reads, 0);
      expect(gate.activeOwners, 0);
      expect(find.byType(CachedNetworkImage), findsNothing);
      await tester.pumpWidget(const SizedBox());
      await settle(tester);
      var closed = false;
      unawaited(c.close().then((_) => closed = true));
      await settle(tester);
      expect(closed, isTrue);
    },
  );

  for (final closeImmediately in [false, true]) {
    test(
      'load sonrası aynı tur ${closeImmediately ? 'close' : 'suspend/retry'} döngüye girmez',
      () async {
        final c = cubit(Source(ConsumerFixture(format: 'JPEG')));
        final pending = load(c);
        if (closeImmediately) {
          final closing = c.close();
          await Future.wait([
            pending,
            closing,
          ]).timeout(const Duration(seconds: 1));
          expect(c.isClosed, isTrue);
        } else {
          c.suspend();
          await pending.timeout(const Duration(seconds: 1));
          await c.retry().timeout(const Duration(seconds: 1));
          expect(c.state.status, ReadingImageConsumerStatus.admitted);
          await c.close().timeout(const Duration(seconds: 1));
        }
      },
    );
  }

  test(
    'bekleyen status close ve sonraki owner admission için gerçek settlement tutar',
    () async {
      final source = Source(
        ConsumerFixture(format: 'JPEG'),
        configuration: consumerConfiguration(
          statusEvery: const Duration(milliseconds: 10),
        ),
      )..statusBlock = Completer<void>();
      final gate = ReadingImageConsumerOwnership();
      final first = cubit(source, gate: gate);
      await load(first);
      await Future<void>.delayed(const Duration(milliseconds: 30));
      expect(source.statusChecks, 1);
      var closed = false;
      final closing = first.close().then((_) => closed = true);
      final secondSource = Source(ConsumerFixture(format: 'JPEG'));
      final second = cubit(secondSource, gate: gate);
      final waiting = load(second);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(closed, isFalse);
      expect(secondSource.admissions, 0);
      expect(gate.activeOwners, 1);
      source.statusBlock!.complete();
      await closing;
      await waiting;
      expect(first.state.status, isNot(ReadingImageConsumerStatus.loaded));
      expect(secondSource.admissions, 1);
      await second.close();
    },
  );

  test(
    'settlement hatası sonraki retry/close Future zincirini zehirlemez',
    () async {
      final gate = ReadingImageConsumerOwnership();
      final c = cubit(Source(ConsumerFixture(format: 'JPEG')), gate: gate);
      await load(c);
      final failedSettlement = Completer<void>();
      c.ownProvider(failedSettlement.future);
      final retry = c.retry();
      await Future<void>.delayed(Duration.zero);
      expect(gate.activeOwners, 1);
      failedSettlement.completeError(StateError('cleanup_failure'));
      await retry.timeout(const Duration(seconds: 1));
      expect(c.state.status, ReadingImageConsumerStatus.error);
      await c.retry().timeout(const Duration(seconds: 1));
      expect(c.state.status, ReadingImageConsumerStatus.admitted);
      await c.close().timeout(const Duration(seconds: 1));
      expect(gate.activeOwners, 0);
    },
  );

  test(
    'önceki retirement hatası permit tutar; retry gerçek cleanup sonrası ilerler',
    () async {
      final gate = ReadingImageConsumerOwnership();
      final c = cubit(Source(ConsumerFixture(format: 'JPEG')), gate: gate);
      await load(c);
      final failedCleanup = Completer<void>();
      c.ownProvider(failedCleanup.future);
      c.suspend();
      await Future<void>.delayed(Duration.zero);
      failedCleanup.completeError(StateError('provider_cleanup_failure'));
      await Future<void>.delayed(Duration.zero);
      expect(gate.activeOwners, 1);
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      await c.retry().timeout(const Duration(seconds: 1));
      expect(c.state.status, ReadingImageConsumerStatus.admitted);
      expect(gate.activeOwners, 1);
      await c.close().timeout(const Duration(seconds: 1));
      expect(gate.activeOwners, 0);
    },
  );

  test(
    'ikinci owner bekleme iptali ve requestWait tavanı erken permit açmaz',
    () async {
      final gate = ReadingImageConsumerOwnership();
      final first = cubit(Source(ConsumerFixture(format: 'JPEG')), gate: gate);
      await load(first);
      final decode = Completer<void>();
      first.ownDecode(decode.future);
      first.suspend();
      final secondSource = Source(ConsumerFixture(format: 'JPEG'));
      final second = cubit(secondSource, gate: gate);
      final waiting = load(second);
      await Future<void>.delayed(Duration.zero);
      second.suspend();
      await waiting.timeout(const Duration(seconds: 1));
      await second.close();
      expect(secondSource.admissions, 0);
      final thirdSource = Source(
        ConsumerFixture(format: 'JPEG'),
        configuration: consumerConfiguration(
          requestWait: const Duration(milliseconds: 25),
        ),
      );
      final third = cubit(thirdSource, gate: gate);
      await load(third);
      expect(third.state.status, ReadingImageConsumerStatus.error);
      expect(thirdSource.admissions, 0);
      expect(gate.activeOwners, 1);
      await third.close();
      decode.complete();
      await first.close();
      expect(gate.activeOwners, 0);
    },
  );

  test(
    'dış transfer timeout yavaş bytes delegate sahipliğini gerçek bitişe kadar tutar',
    () async {
      final source = Source(
        ConsumerFixture(format: 'JPEG'),
        configuration: consumerConfiguration(
          requestWait: const Duration(milliseconds: 100),
        ),
      )..bytesBlock = Completer<void>();
      final gate = ReadingImageConsumerOwnership();
      final first = cubit(source, gate: gate);
      await load(first);
      final file = first.cache.getSingleFile(
        first.state.url!,
        key: first.state.transferKey,
      );
      await expectLater(file, throwsA(anything));
      expect(source.reads, 1);
      var closed = false;
      final closing = first.close().then((_) => closed = true);
      final secondSource = Source(ConsumerFixture(format: 'JPEG'));
      final second = cubit(secondSource, gate: gate);
      final waiting = load(second);
      await Future<void>.delayed(const Duration(milliseconds: 30));
      expect(closed, isFalse);
      expect(gate.activeOwners, 1);
      expect(secondSource.admissions, 0);
      source.bytesBlock!.complete();
      await closing.timeout(const Duration(seconds: 1));
      await waiting;
      expect(second.state.status, ReadingImageConsumerStatus.admitted);
      await second.close();
    },
  );

  for (final field in ['sha256', 'byteSize']) {
    test(
      'preserve JPEG $field eşitliği geçerli canonical digest olsa da zorunludur',
      () {
        final fixture = ConsumerFixture(format: 'JPEG');
        final images = fixture.json['delivery']['descriptor']['images'] as List;
        final raster = images.last['renditions'].single['raster'] as Map;
        raster[field] = field == 'sha256'
            ? descriptor.hash('e')
            : jpegBytes.length + 1;
        var admission = fixture.admission;
        fixture.json['delivery']['identity']['manifestDigest'] =
            ReadingImageManifestDigest.digest(admission.manifest);
        admission = fixture.admission;
        expect(
          ReadingImageManifestDigest.digest(admission.manifest),
          admission.manifest.identity.manifestDigest,
        );
        expect(
          const ValidateReadingImageManifest()(
            manifest: admission.manifest,
            expected: admission.context,
          ).fold((failure) => failure, (_) => null),
          ReadingImageManifestFailure.renditionMismatch,
        );
      },
    );
  }

  testWidgets('background resize/DPR admission read decode başlatmaz', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetDevicePixelRatio);
    final source = Source(ConsumerFixture(format: 'JPEG'));
    final c = cubit(source);
    libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
    var loaded = 0;
    Widget view(double size) => MaterialApp(
      home: Center(
        child: SizedBox(
          width: size,
          height: size,
          child: ReadingImageConsumerWidget(
            request: ReadingArtworkRequest(
              contentId: 71,
              language: 'tr',
              fit: BoxFit.contain,
              visible: true,
              attempt: 0,
              onLoaded: () => loaded++,
              onError: () {},
            ),
          ),
        ),
      ),
    );
    await tester.pumpWidget(view(4));
    await settle(tester);
    expect(c.state.status, ReadingImageConsumerStatus.loaded);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await settle(tester);
    final before = (source.admissions, source.reads, loaded);
    tester.view.devicePixelRatio = 2;
    await tester.pumpWidget(view(3));
    await settle(tester);
    expect((source.admissions, source.reads, loaded), before);
    expect(c.state.status, ReadingImageConsumerStatus.suspended);
    expect(find.byType(CachedNetworkImage), findsNothing);
    expect(find.byType(TextButton), findsNothing);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpWidget(const SizedBox());
    await settle(tester);
    await c.close();
  });

  testWidgets(
    'loaded widget periyodik status reddinde provider ve permit temizlenir',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetDevicePixelRatio);
      final source = Source(
        ConsumerFixture(format: 'JPEG'),
        configuration: consumerConfiguration(
          statusEvery: const Duration(seconds: 1),
        ),
      );
      final gate = ReadingImageConsumerOwnership();
      final c = cubit(source, gate: gate);
      libraryGetIt.registerFactory<ReadingImageConsumerCubit>(() => c);
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 4,
              height: 4,
              child: ReadingImageConsumerWidget(
                request: ReadingArtworkRequest(
                  contentId: 71,
                  language: 'tr',
                  fit: BoxFit.contain,
                  visible: true,
                  attempt: 0,
                  onLoaded: () {},
                  onError: () {},
                ),
              ),
            ),
          ),
        ),
      );
      await settle(tester);
      expect(c.state.status, ReadingImageConsumerStatus.loaded);
      source.denied = true;
      await tester.pump(const Duration(seconds: 1));
      await settle(tester);
      expect(source.statusChecks, 1);
      expect(c.state.status, ReadingImageConsumerStatus.suspended);
      expect(find.byType(CachedNetworkImage), findsNothing);
      expect(gate.activeOwners, 0);
      await tester.pumpWidget(const SizedBox());
      await settle(tester);
      await c.close();
    },
  );
  test('normal root varsayılan kapalıdır ve QA bağımlılığı içermez', () {
    configureLibraryDependencies(library_fixture.configuration());
    expect(
      libraryGetIt.isRegistered<ReadingImageConsumerConfiguration>(),
      isFalse,
    );
    expect(libraryGetIt<LibraryRouter>(), isA<LibraryRouter>());
    final generated = File('lib/app/injection.config.dart').readAsStringSync();
    expect(generated, isNot(contains('reading_image_comparison')));
    expect(generated, isNot(contains('local_lossless_qa')));
    for (final file in Directory(
      'lib/features/reading_images/consumer/presentation',
    ).listSync().whereType<File>()) {
      expect(
        file.readAsStringSync(),
        isNot(contains("import '../data/")),
        reason: file.path,
      );
    }
  });
  test(
    'platform decode genişliği aşılırsa byte okunmadan reddedilir',
    () async {
      final source = Source(ConsumerFixture(policy: 'PNG_FIT'));
      final c = cubit(source);
      await c.load(
        contentId: 71,
        language: 'tr',
        role: ReadingImageRole.readingDetail,
        pageNumber: 0,
        width: 8,
        height: 8,
        dpr: 1,
        fit: ReadingImageFit.contain,
        maxDecodeWidth: 4,
      );
      expect(c.state.status, ReadingImageConsumerStatus.error);
      expect(source.reads, 0);
      await c.close();
    },
  );

  test(
    'korumalı dosya ve metadata cache disk File/Directory açmadan çalışır',
    () async {
      var diskCalls = 0;
      await IOOverrides.runZoned(
        () async {
          final source = Source(ConsumerFixture(policy: 'PNG_FIT'));
          final c = cubit(source);
          await load(c);
          final file = await c.cache.getSingleFile(
            c.state.url!,
            key: c.state.transferKey,
          );
          expect(await file.readAsBytes(), source.fixture.bytes);
          c.decoded(c.state.attempt);
          await c.cache.settleOwnedWork();
          expect(
            await c.cache.getFileFromCache(c.state.immutableKey!),
            isNotNull,
          );
          await c.close();
        },
        createFile: (path) {
          diskCalls++;
          throw StateError('disk_file_refused');
        },
        createDirectory: (path) {
          diskCalls++;
          throw StateError('disk_directory_refused');
        },
        getSystemTempDirectory: () {
          diskCalls++;
          throw StateError('disk_temp_refused');
        },
      );
      expect(diskCalls, 0);
    },
  );
}
