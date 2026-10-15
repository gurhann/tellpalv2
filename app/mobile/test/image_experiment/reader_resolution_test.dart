import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/data/reader_manifest_source.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/domain/entity/raster_case.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/reader_resolution_cubit.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/reader_resolution_state.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/view/reader_resolution_images.dart';
import 'package:tellpal_mobile_poc/story_player_page.dart';
import 'package:tellpal_mobile_poc/story_reader_images.dart';
import 'fixture.dart';

class _Files implements FileSystem {
  final fs = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => fs.file('/$name');
}

class _LookupGateRepository extends NonStoringObjectProvider {
  Completer<void>? lookupGate;
  @override
  Future<CacheObject?> get(String key) async {
    await lookupGate?.future;
    return super.get(key);
  }
}

class _Transport extends ImageExperimentTransport {
  _Transport(super.configuration, super.recorder);
  @override
  Future<void> close() async {
    configuration.credentials.revoke();
    await super.close();
  }
}

class _Cache extends ImageExperimentImageCache {
  _Cache(super.configuration, super.transport, super.recorder)
    : super(
        storageConfiguration: Config(
          'reader-test',
          repo: NonStoringObjectProvider(),
          fileSystem: _Files(),
        ),
      );
  int providers = 0, absences = 0, releases = 0;
  final providerValues = <String, ImageProvider>{};
  Completer<void>? admission;
  @override
  ImageExperimentProvider provider(RasterCase fixture, String attempt) {
    providers++;
    final provider = MemoryImage(fixtureBytes);
    providerValues[fixture.caseId] = provider;
    return ImageExperimentProvider(
      provider,
      ImageExperimentRecorder.hash(fixture.caseId),
      'transfer-${fixture.caseId}',
      ImageExperimentRecorder.hash(attempt),
      fixture,
    );
  }

  @override
  Future<void> requireAbsentKeys(ImageExperimentProvider value) async {
    absences++;
    await admission?.future;
  }

  @override
  bool verified(ImageExperimentProvider value) => true;
  @override
  Future<void> release(
    ImageExperimentProvider value, {
    bool failed = false,
  }) async {
    releases++;
  }

  @override
  Future<void> removeFile(String key) async {}
  @override
  Future<void> close() async {
    await transport.close();
    await super.close();
  }
}

class _ClosingImages extends ReaderResolutionImages {
  _ClosingImages(super.configuration, super.cache, super.recorder);
  int closes = 0, cancellations = 0;
  Object? closeError;
  @override
  void cancel() {
    cancellations++;
    super.cancel();
  }

  @override
  Future<void> close() async {
    closes++;
    await super.close();
    if (closeError != null) throw closeError!;
  }
}

class _ClosingCubit extends ReaderResolutionCubit {
  _ClosingCubit(super.configuration, super.images, super.recorder);
  int refusals = 0;
  @override
  void refuse(String reason) {
    refusals++;
    super.refuse(reason);
  }
}

class _PngTransport extends _Transport {
  _PngTransport(super.configuration, super.recorder, this.bytes);
  final Uint8List bytes;
  final requests = <Uri>[];
  @override
  http.Client newClient() => MockClient((request) async {
    requests.add(request.url);
    return http.Response.bytes(
      bytes,
      200,
      headers: {'content-type': 'image/png', 'cache-control': 'max-age=60'},
    );
  });
}

Future<Uint8List> _largerPng() async {
  final recording = ui.PictureRecorder();
  final canvas = Canvas(recording);
  canvas.drawRect(
    const Rect.fromLTWH(0, 0, 12, 8),
    Paint()..color = Colors.red,
  );
  final picture = recording.endRecording();
  final image = await picture.toImage(12, 8);
  try {
    return (await image.toByteData(
      format: ui.ImageByteFormat.png,
    ))!.buffer.asUint8List();
  } finally {
    image.dispose();
    picture.dispose();
  }
}

Future<ImageInfo> _decoded(ImageProvider provider) async {
  final complete = Completer<ImageInfo>();
  final stream = provider.resolve(ImageConfiguration.empty);
  final listener = ImageStreamListener(
    (image, _) {
      if (!complete.isCompleted) complete.complete(image);
    },
    onError: (Object error, StackTrace? stack) {
      if (!complete.isCompleted) complete.completeError(error, stack);
    },
  );
  stream.addListener(listener);
  try {
    return await complete.future.timeout(const Duration(seconds: 5));
  } finally {
    stream.removeListener(listener);
  }
}

FixtureWire readerWire({
  String batch = 'reader_resolution_a_r1',
  int budget = 175000,
  Uint8List? imageBytes,
}) {
  final quality = batch.startsWith('reader_quality_');
  final discover = batch == 'reader_resolution_a_r1';
  final manifest = fixtureManifest()..['batchId'] = batch;
  manifest['entries'] = [
    for (var index = 0; index < (quality ? 2 : 6); index++)
      {
        ...fixtureEntry(),
        'caseId': 'reader_page_${index + 1}',
        'profileRevision': discover ? 'reader_discover_r1' : 'reader_frozen_r1',
        'purposeAcceptanceReceipt': batch,
        if (imageBytes != null) ...{
          'outputSha256': sha256.convert(imageBytes).toString(),
          'byteSize': imageBytes.length,
          'width': 12,
          'height': 8,
          'logicalSlotWidth': discover ? 1.0 : 3.0,
          'logicalSlotHeight': discover ? 1.0 : 2.0,
        },
      },
  ];
  final wire = FixtureWire(manifest: manifest);
  wire.envelope['batchId'] = batch;
  wire.metadata['batchId'] = batch;
  wire.metadata['caseId'] = 'reader_page_1';
  wire.metadata['expectedAppId'] =
      'com.tellpal.tellpal_mobile_poc.imageexperiment.release';
  wire.envelope['expiresAt'] = wire.now.millisecondsSinceEpoch + budget;
  return wire;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('tellpal/image_experiment');
  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
  Future<List<Map<String, dynamic>>> rows(
    ImageExperimentRecorder recorder,
  ) async => (jsonDecode(await recorder.export())['records'] as List)
      .cast<Map<String, dynamic>>();
  ({
    ImageExperimentConfiguration configuration,
    ImageExperimentRecorder recorder,
    _Cache cache,
    ReaderResolutionImages images,
    ReaderResolutionCubit cubit,
  })
  setup({String batch = 'reader_resolution_a_r1', int budget = 175000}) {
    final configuration = readerWire(
      batch: batch,
      budget: budget,
    ).configuration();
    final recorder = ImageExperimentRecorder();
    final cache = _Cache(
      configuration,
      _Transport(configuration, recorder),
      recorder,
    );
    final images = ReaderResolutionImages(configuration, cache, recorder);
    return (
      configuration: configuration,
      recorder: recorder,
      cache: cache,
      images: images,
      cubit: ReaderResolutionCubit(configuration, images, recorder),
    );
  }

  StoryReaderGeometry geometry({double width = 1}) => StoryReaderGeometry(
    slot: Size(width, 1),
    dpr: 1,
    textScale: 1,
    padding: EdgeInsets.zero,
    viewInsets: EdgeInsets.zero,
  );

  for (final arm in ['a', 'b']) {
    test(
      'real reader cache admits six distinct $arm providers and decodes the arm dimensions',
      () async {
        final bytes = await _largerPng();
        final configuration = readerWire(
          batch: 'reader_resolution_${arm}_r1',
          imageBytes: bytes,
        ).configuration();
        final recorder = ImageExperimentRecorder();
        final transport = _PngTransport(configuration, recorder, bytes);
        final repository = _LookupGateRepository();
        final cache = ImageExperimentImageCache(
          configuration,
          transport,
          recorder,
          storageConfiguration: Config(
            'real-six-reader-$arm',
            repo: repository,
            fileSystem: _Files(),
          ),
        );
        final providers = <ImageExperimentProvider>[];
        try {
          for (final fixture in configuration.cases.values) {
            final value = cache.provider(fixture, 'reader_session');
            await cache.requireAbsentKeys(value);
            providers.add(value);
          }
          expect(providers.length, 6);
          expect(providers.map((v) => v.immutableKey).toSet().length, 6);
          expect(providers.map((v) => v.transferKey).toSet().length, 6);
          expect(
            cache
                .provider(configuration.cases.values.first, 'reader_session')
                .transferKey,
            providers.first.transferKey,
          );
          expect(
            () => cache.provider(configuration.cases.values.first, 'seventh'),
            throwsA(
              isA<FormatException>().having(
                (error) => error.message,
                'capacity refusal',
                'attempt_capacity',
              ),
            ),
          );
          final held = <StreamSubscription<FileResponse>>[];
          final transferErrors = <Object>[];
          final gate = repository.lookupGate = Completer<void>();
          try {
            for (final value in providers) {
              held.add(
                cache
                    .getFileStream(
                      configuration.objectUri(value.fixture.caseId).toString(),
                      key: value.transferKey,
                    )
                    .listen((_) {}, onError: transferErrors.add),
              );
            }
            expect(cache.ownedAttemptCount, 6);
            await expectLater(
              cache
                  .getFileStream(
                    configuration
                        .objectUri(providers.first.fixture.caseId)
                        .toString(),
                    key: providers.first.transferKey,
                  )
                  .toList(),
              throwsStateError,
            );
          } finally {
            for (final subscription in held) {
              await subscription.cancel();
            }
            gate.complete();
            repository.lookupGate = null;
          }
          final settling = Stopwatch()..start();
          while (cache.ownedAttemptCount != 0 &&
              settling.elapsed < const Duration(seconds: 2)) {
            await Future<void>.delayed(const Duration(milliseconds: 1));
          }
          expect(cache.ownedAttemptCount, 0);
          expect(transferErrors, isEmpty);
          expect(transport.requests, isEmpty);
          // Six providers stay admitted together; transfer reservations remain
          // bounded independently and do not require six simultaneous downloads.
          for (final value in providers) {
            final image = await _decoded(value.provider);
            try {
              expect(image.image.width, arm == 'a' ? 12 : 3);
              expect(image.image.height, arm == 'a' ? 8 : 2);
              expect(cache.verified(value), true);
            } finally {
              image.dispose();
            }
          }
          expect(transport.requests.length, 6);
          expect(transport.requests.toSet().length, 6);
          final events = await rows(recorder);
          final owned = events.where((row) => row['event'] == 'ownership');
          expect(owned.length, 6);
          expect(
            owned.every(
              (row) =>
                  row['decodeWidth'] == (arm == 'a' ? 12 : 3) &&
                  row['decodeHeight'] == (arm == 'a' ? 8 : 2),
            ),
            true,
          );
        } finally {
          for (final value in providers) {
            await value.provider.evict();
            await cache.release(value);
          }
          await cache.close();
          await transport.close();
          await configuration.credentials.close();
        }
      },
    );
  }

  test(
    'finite reader matrix permits exactly its pages; legacy remains selected-only',
    () {
      for (final batch in readerExperimentBatches) {
        final config = readerWire(batch: batch).configuration();
        expect(config.cases.length, config.readerQuality ? 2 : 6);
        for (final id in config.cases.keys) {
          expect(config.objectUri(id).path, endsWith('/$id'));
        }
        expect(() => config.objectUri('reader_page_7'), throwsFormatException);
      }
      final selected = readerWire(
        batch: 'reader_resolution_075_r1',
      ).configuration();
      expect(selected.isReader, isTrue);
      expect(selected.readerQuality, isFalse);
      expect(selected.readerFullDecode, isFalse);
      expect(selected.readerPageCount, 6);
      final wire = readerWire();
      wire.metadata['expectedAppId'] =
          'com.tellpal.tellpal_mobile_poc.imageexperiment.profile';
      expect(wire.configuration, throwsFormatException);
      final wrong = readerWire();
      wrong.metadata['caseId'] = 'reader_page_2';
      expect(wrong.configuration, throwsFormatException);
    },
  );
  test(
    'synthetic QA manifest has no audio, expires and refuses after close',
    () async {
      final config = readerWire().configuration();
      final source = ReaderManifestSource(config);
      final manifest = await source.fetch(contentId: 71, languageCode: 'tr');
      expect(manifest.pages.length, 6);
      expect(manifest.pages.every((p) => p.audio == null), true);
      expect(manifest.revision, startsWith('qa-'));
      await expectLater(
        source.fetch(contentId: 19, languageCode: 'tr'),
        throwsA(isA<Exception>()),
      );
      source.close();
      await expectLater(
        source.fetch(contentId: 71, languageCode: 'tr'),
        throwsA(isA<Exception>()),
      );
      await config.credentials.close();
    },
  );
  testWidgets(
    'selected prefetch and display pin one provider; rapid stale selection cannot paint old page',
    (tester) async {
      final value = setup();
      final source = ReaderManifestSource(value.configuration);
      final controller = StoryReaderController();
      await tester.pumpWidget(
        MaterialApp(
          home: StoryPlayerPage(
            client: source,
            contentId: 71,
            languageCode: 'tr',
            images: value.images,
            controller: controller,
            grantRefreshWindow: Duration.zero,
          ),
        ),
      );
      for (var i = 0; i < 15; i++) {
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 20)),
        );
        await tester.pump(const Duration(milliseconds: 20));
      }
      expect(value.images.paintedPages, contains(1));
      final manifest = await source.fetch(contentId: 71, languageCode: 'tr');
      final a = await value.images.prepare(manifest.pages.first);
      final b = await value.images.prepare(manifest.pages.first);
      expect(identical(a, b), true);
      controller.selectPage(5);
      controller.selectPage(1);
      controller.selectPage(4);
      for (var i = 0; i < 15; i++) {
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 20)),
        );
        await tester.pump(const Duration(milliseconds: 20));
      }
      expect(find.textContaining('Sayfa 5/6'), findsOneWidget);
      expect(value.cache.providers, lessThanOrEqualTo(6));
      expect(value.cache.absences, value.cache.providers);
      final events = (await tester.runAsync(() => rows(value.recorder)))!;
      final selections = events
          .where((r) => r['event'] == 'reader_selection')
          .toList();
      expect(selections.last['pageIndex'], 4);
      final after = events.where(
        (r) =>
            r['event'] == 'page_paint' &&
            (r['monotonicMicroseconds'] as int) >
                (selections.last['monotonicMicroseconds'] as int),
      );
      expect(after.every((r) => r['pageIndex'] == 4), true);
      await tester.pumpWidget(const SizedBox());
      await tester.pump();
      expect(
        value.cache.releases,
        0,
      ); // Natural route disposal cannot evict the experiment cache.
      await tester.runAsync(() async {
        await value.images.evict();
        await value.cubit.close();
        await value.cache.close();
      });
      expect(value.cache.releases, greaterThan(0));
      expect(tester.takeException(), null);
    },
  );
  testWidgets(
    'pending geometry admission cancels on revocation and changed frozen geometry refuses',
    (tester) async {
      final value = setup(batch: 'reader_resolution_b_r1');
      final source = ReaderManifestSource(value.configuration);
      final manifest = await source.fetch(contentId: 71, languageCode: 'tr');
      final future = value.images.prepare(manifest.pages.first);
      value.configuration.credentials.revoke();
      await expectLater(future, throwsStateError);
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cache.close);
      final changed = setup(batch: 'reader_resolution_b_r1');
      changed.images.observeLayout(geometry(width: 2));
      await tester.pump();
      expect(changed.cubit.state.status, ReaderResolutionStatus.refused);
      await tester.runAsync(changed.cubit.close);
      await tester.runAsync(changed.cache.close);
    },
  );
  testWidgets(
    'budget refusal dispatches nothing; serial sample timeout is never retried',
    (tester) async {
      final low = setup(budget: 100000);
      var finishes = 0;
      low.cubit.start(finish: (_) => finishes++);
      expect(low.cubit.state.status, ReaderResolutionStatus.refused);
      expect(finishes, 1);
      expect(low.cache.providers, 0);
      await tester.runAsync(low.cubit.close);
      await tester.runAsync(low.cache.close);
      var reads = 0;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'sampleMemory') {
              reads++;
              return Completer<dynamic>().future;
            }
            return null;
          });
      final value = setup();
      value.cubit.start(finish: (_) {});
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 750));
      await tester.pump(const Duration(seconds: 2));
      expect(reads, 1);
      final samples = (await tester.runAsync(
        () => rows(value.recorder),
      ))!.where((r) => r['event'] == 'memory_sample');
      expect(samples.single['memoryBytes'], null);
      expect(samples.single['memoryReason'], 'sample_timeout');
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cache.close);
    },
  );
  testWidgets(
    'baseline revocation retires timers before any reader route selection',
    (tester) async {
      final value = setup();
      value.cubit.start(finish: (_) {});
      await tester.pump(const Duration(seconds: 10));
      value.configuration.credentials.revoke();
      await tester.pump(const Duration(seconds: 100));
      expect(value.cubit.state.status, ReaderResolutionStatus.refused);
      expect(value.cache.providers, 0);
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cache.close);
    },
  );
  testWidgets(
    'natural retention and exact owned eviction have separate bounded phases',
    (tester) async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'sampleMemory') {
              return {
                'memoryBytes': 1024,
                'memoryCounter': 'ios_phys_footprint',
                'memoryReason': 'none',
                'nativeReadMicroseconds': 2,
                'nativeResultCode': 0,
                'nativeResultCount': 96,
                'thermalState': 'nominal',
              };
            }
            if (call.method == 'readReaderLifecycle') {
              return {
                'schemaVersion': 1,
                'backgroundCount': 1,
                'returnCount': 1,
                'backgrounded': false,
                'backgroundEpochMilliseconds': 1,
                'returnedEpochMilliseconds': 2,
              };
            }
            if (call.method == 'requestReaderBackground') return true;
            return null;
          });
      final value = setup();
      bool? complete;
      final manifest = await ReaderManifestSource(
        value.configuration,
      ).fetch(contentId: 71, languageCode: 'tr');
      value.images.observeLayout(geometry());
      for (final page in manifest.pages) {
        await value.images.prepare(page);
      }
      await tester.pumpWidget(const SizedBox());
      value.cubit.start(finish: (result) => complete = result);
      await tester.pump(const Duration(seconds: 20));
      expect(value.cubit.state.status, ReaderResolutionStatus.reading);
      value.images.paintedPages.addAll([1, 2, 3, 4, 5, 6]);
      await tester.pump(const Duration(seconds: 52));
      await tester.pump();
      expect(value.cubit.state.status, ReaderResolutionStatus.natural);
      expect(value.cache.releases, 0);
      await tester.pump(const Duration(seconds: 29));
      expect(value.cache.releases, 0);
      await tester.pump(const Duration(seconds: 1));
      await tester.pump();
      expect(value.cache.releases, 6);
      expect(complete, null);
      await tester.pump(const Duration(seconds: 10));
      expect(complete, true);
      final events = (await tester.runAsync(() => rows(value.recorder)))!;
      expect(
        events.where((r) => r['event'] == 'phase').map((r) => r['phase']),
        [
          'baseline',
          'reader',
          'natural_retention',
          'owned_eviction',
          'complete',
        ],
      );
      final samples = events.where((r) => r['event'] == 'memory_sample');
      expect(samples.every((r) => r['memoryBytes'] == 1024), true);
      expect(
        events
            .where((r) => r['event'] == 'cache_state')
            .map((r) => r['phase'])
            .toSet(),
        containsAll(['natural_retention', 'owned_eviction']),
      );
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cache.close);
    },
  );
  testWidgets(
    'quality clears each previous capture before page change and has no memory sampler',
    (tester) async {
      var reads = 0;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'sampleMemory') reads++;
            return null;
          });
      final value = setup(batch: 'reader_quality_full_r1');
      final order = <String>[];
      bool? complete;
      value.cubit.controller.attach((page) => order.add('select:$page'));
      value.cubit.start(
        finish: (result) => complete = result,
        publishQuality: (_) async {
          order.add('publish');
          return true;
        },
        clearQuality: () async {
          order.add('clear');
          return true;
        },
      );
      value.images.paintedPages.add(1);
      value.cubit.pagePainted({});
      await tester.pump();
      await tester.pump(const Duration(seconds: 8));
      await tester.pump();
      expect(order, ['publish', 'clear', 'select:1']);
      value.images.paintedPages.add(2);
      value.cubit.pagePainted({});
      await tester.pump();
      await tester.pump(const Duration(seconds: 8));
      await tester.pump();
      expect(complete, true);
      expect(reads, 0);
      expect(order, ['publish', 'clear', 'select:1', 'publish', 'clear']);
      await tester.runAsync(value.cubit.close);
      await tester.runAsync(value.cache.close);
    },
  );
  for (final nativeThrows in [false, true]) {
    test(
      'quality close settles image and Cubit owners when native clear ${nativeThrows ? 'throws' : 'returns false'}',
      () async {
        final configuration = readerWire(
          batch: 'reader_quality_full_r1',
        ).configuration();
        final recorder = ImageExperimentRecorder();
        final cache = _Cache(
          configuration,
          _Transport(configuration, recorder),
          recorder,
        );
        final images = _ClosingImages(configuration, cache, recorder);
        final cubit = _ClosingCubit(configuration, images, recorder);
        final nativeError = StateError('Injected native clear failure');
        // A later owner failure cannot mask the original native clearance error.
        if (nativeThrows) images.closeError = StateError('Later image failure');
        var clears = 0;
        cubit.start(
          finish: (_) {},
          clearQuality: () async {
            clears++;
            if (nativeThrows) throw nativeError;
            return false;
          },
        );
        final closed = cubit.close();
        expect(identical(closed, cubit.close()), true);
        await expectLater(
          closed,
          throwsA(
            nativeThrows
                ? same(nativeError)
                : isA<StateError>().having(
                    (error) => error.message,
                    'native refusal',
                    'cleanup_unavailable',
                  ),
          ),
        );
        expect(clears, 1);
        expect(cubit.isClosed, true);
        expect(images.closes, 1);
        expect(images.active, false);
        expect(images.cancellations, 1);
        final cancellations = images.cancellations;
        configuration.credentials.revoke();
        await Future<void>.delayed(Duration.zero);
        expect(cubit.refusals, 0);
        expect(images.cancellations, cancellations);
        expect(clears, 1);
        await cache.close();
        await configuration.credentials.close();
      },
    );
  }
}
