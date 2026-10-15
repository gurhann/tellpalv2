import 'dart:convert';
import 'dart:ui' as ui;
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_target_probe.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart';
import '../../../app/mobile/test/image_experiment/fixture.dart';

class _Files implements FileSystem {
  final fs = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => fs.file('/$name');
}

// Only the storage lookup is substituted. The production absence guard executes.
class _StorageCache extends ImageExperimentImageCache {
  _StorageCache(
    super.configuration,
    super.transport,
    super.recorder, {
    required super.storageConfiguration,
  });
  final disk = <String, FileInfo>{};
  @override
  Future<FileInfo?> getFileFromCache(
    String key, {
    bool ignoreMemCache = false,
  }) async => disk[key];
}

class _Completer extends ImageStreamCompleter {
  void complete() {
    final picture = ui.PictureRecorder();
    ui.Canvas(picture).drawColor(const Color(0xff123456), BlendMode.src);
    final recording = picture.endRecording();
    final image = recording.toImageSync(1, 1);
    recording.dispose();
    setImage(ImageInfo(image: image));
  }
}

class _CountingImage extends MemoryImage {
  _CountingImage() : super(fixtureBytes);
  final _resolutions = <int>[0];
  int get resolutions => _resolutions.single;
  @override
  void resolveStreamForKey(
    ImageConfiguration configuration,
    ImageStream stream,
    MemoryImage key,
    ImageErrorListener handleError,
  ) {
    _resolutions[0]++;
    super.resolveStreamForKey(configuration, stream, key, handleError);
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  for (final present in [
    'immutable disk',
    'transfer disk',
    'pending',
    'live',
    'keepAlive',
    'absent',
  ]) {
    testWidgets('real requireAbsentKeys checks $present', (tester) async {
      final configuration = FixtureWire().configuration();
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(configuration, recorder);
      final storage = _Files();
      final cache = _StorageCache(
        configuration,
        transport,
        recorder,
        storageConfiguration: Config(
          'external-absence-$present',
          repo: NonStoringObjectProvider(),
          fileSystem: storage,
        ),
      );
      final provider = _CountingImage();
      final value = ImageExperimentProvider(
        provider,
        'a' * 64,
        'transfer',
        'b' * 64,
        configuration.cases.values.single,
      );
      final imageCache = PaintingBinding.instance.imageCache;
      imageCache.clear();
      imageCache.clearLiveImages();
      final key = await provider.obtainKey(ImageConfiguration.empty);
      _Completer? completer;
      ImageStreamListener? listener;
      if (present.endsWith('disk')) {
        final file = storage.fs.file('/cached')..writeAsBytesSync(fixtureBytes);
        cache.disk[present == 'immutable disk'
            ? value.immutableKey
            : value.transferKey] = FileInfo(
          file,
          FileSource.Cache,
          DateTime.now().add(const Duration(minutes: 1)),
          '',
        );
      } else if (present != 'absent') {
        completer = _Completer();
        imageCache.putIfAbsent(key, () => completer!);
        if (present == 'pending') {
          imageCache.clearLiveImages();
        } else {
          listener = ImageStreamListener((info, _) => info.dispose());
          completer.addListener(listener);
          completer.complete();
          if (present == 'live') {
            imageCache.evict(key, includeLive: false);
          } else {
            completer.removeListener(listener);
            listener = null;
          }
        }
        final status = imageCache.statusForKey(key);
        expect(status.pending, present == 'pending');
        expect(status.live, present == 'live');
        expect(status.keepAlive, present == 'keepAlive');
      }
      if (present == 'absent') {
        await cache.requireAbsentKeys(value);
      } else {
        await expectLater(
          cache.requireAbsentKeys(value),
          throwsA(
            isA<FormatException>().having(
              (error) => error.message,
              'reason',
              'cache_keys_present',
            ),
          ),
        );
      }
      final records = (jsonDecode(await recorder.export())['records'] as List)
          .cast<Map>();
      final absence = records
          .where((row) => row['cacheState'] == 'namespace_keys_absent')
          .toList();
      expect(absence.length, present == 'absent' ? 1 : 0);
      if (absence.isNotEmpty) {
        expect(absence.single['providerSha256'], value.immutableKey);
        expect(absence.single['attemptSha256'], value.attemptSha256);
        expect(
          absence.single['caseSha256'],
          ImageExperimentRecorder.hash(value.fixture.caseId),
        );
      }
      expect(provider.resolutions, 0);
      if (listener != null) completer!.removeListener(listener);
      imageCache.clear();
      imageCache.clearLiveImages();
      await tester.runAsync(() async {
        await cache.close();
        await transport.close();
        await configuration.credentials.close();
      });
    });
  }

  testWidgets('real cold probe refuses present keys before resolving target', (
    tester,
  ) async {
    final configuration = FixtureWire().configuration();
    final recorder = ImageExperimentRecorder();
    final transport = ImageExperimentTransport(configuration, recorder);
    final storage = _Files();
    final cache = _StorageCache(
      configuration,
      transport,
      recorder,
      storageConfiguration: Config(
        'external-probe-absence',
        repo: NonStoringObjectProvider(),
        fileSystem: storage,
      ),
    );
    final provider = _CountingImage();
    final value = ImageExperimentProvider(
      provider,
      'a' * 64,
      'transfer',
      'b' * 64,
      configuration.cases.values.single,
    );
    final file = storage.fs.file('/cached')..writeAsBytesSync(fixtureBytes);
    cache.disk[value.immutableKey] = FileInfo(
      file,
      FileSource.Cache,
      DateTime.now(),
      '',
    );
    var failures = 0, decoded = 0, draws = 0;
    Future<void>? release;
    await tester.pumpWidget(
      MaterialApp(
        home: ImageExperimentTargetProbe(
          value: value,
          cache: cache,
          recorder: recorder,
          generation: 0,
          current: () => true,
          requireAbsentKeys: true,
          onDecoded: () => decoded++,
          onFailure: () => failures++,
          onDraw: () => draws++,
          onRelease: (future) => release = future,
        ),
      ),
    );
    await tester.pump();
    await tester.pump();
    expect(failures, 1);
    expect(provider.resolutions, 0);
    expect(decoded, 0);
    expect(draws, 0);
    final records = (jsonDecode(await recorder.export())['records'] as List)
        .cast<Map>();
    expect(
      records.where((row) => row['cacheState'] == 'namespace_keys_absent'),
      isEmpty,
    );
    expect(
      records.where((row) => row['event'] == 'failure').single['reason'],
      'decode_refused',
    );
    expect(
      records.where(
        (row) => ['decoded', 'draw', 'layout'].contains(row['event']),
      ),
      isEmpty,
    );
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    await tester.runAsync(() async {
      await release;
      await cache.close();
      await transport.close();
      await configuration.credentials.close();
    });
  });
}
