import 'dart:convert';
import 'package:file/memory.dart';
import 'package:file/file.dart' as files;
import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_target_probe.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart';
import 'fixture.dart';

class _Files implements FileSystem {
  final fs = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => fs.file('/$name');
}

class _ProbeCache extends ImageExperimentImageCache {
  _ProbeCache(
    super.configuration,
    super.transport,
    super.recorder, {
    required super.storageConfiguration,
  });
  bool admitted = true;
  int releases = 0;
  @override
  bool verified(ImageExperimentProvider value) => admitted;
  @override
  Future<void> remember(
    ImageExperimentProvider value,
    bool Function() current,
  ) async {}
  @override
  Future<void> release(
    ImageExperimentProvider value, {
    bool failed = false,
  }) async {
    releases++;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('changed SHA never admits a warm cache success', (tester) async {
    final configuration = FixtureWire().configuration();
    final recorder = ImageExperimentRecorder();
    final transport = ImageExperimentTransport(configuration, recorder);
    final files = _Files();
    final cache = ImageExperimentImageCache(
      configuration,
      transport,
      recorder,
      storageConfiguration: Config(
        'synthetic-validation',
        repo: NonStoringObjectProvider(),
        fileSystem: files,
      ),
    );
    final value = cache.provider(configuration.cases.values.single, 'one');
    final rebuilt = cache.provider(configuration.cases.values.single, 'one');
    expect(rebuilt.transferKey, value.transferKey);
    final file = files.fs.file('/fixture')..writeAsBytesSync(fixtureBytes);
    await tester.runAsync(
      () => cache.validateResponse(
        FileInfo(
          file,
          FileSource.Cache,
          DateTime.now().add(const Duration(seconds: 10)),
          '',
        ),
        value.transferKey,
      ),
    );
    expect(cache.verified(value), true);
    final records = (jsonDecode(await recorder.export())['records'] as List)
        .cast<Map>();
    expect(
      records
          .where((e) => e['event'] == 'ownership' && e['ownership'] == 'owned')
          .length,
      1,
    );
    final owned = records.firstWhere((e) => e['event'] == 'ownership');
    expect(owned['logicalSlotWidth'], 1.0);
    expect(owned['dpr'], 1.0);
    expect(owned['fit'], 'contain');
    expect(owned['sourceWidth'], 1);
    expect(owned['requiredWidth'], 1);
    expect(owned['decodeWidth'], 1);
    expect(owned['clockDomain'], 'dart_monotonic');
    expect(owned['clockUnit'], 'microseconds');
    file.writeAsBytesSync(List<int>.filled(fixtureBytes.length, 1));
    await expectLater(
      cache.validateResponse(
        FileInfo(file, FileSource.Cache, DateTime.now(), ''),
        value.transferKey,
      ),
      throwsFormatException,
    );
    await tester.runAsync(() async {
      await cache.close();
      await transport.close();
      await configuration.credentials.close();
    });
  });
  for (final kind in ['dimensions', 'MIME']) {
    testWidgets('changed $kind never admits exact bytes as cache success', (
      tester,
    ) async {
      final manifest = fixtureManifest();
      final entry =
          (manifest['entries'] as List).single as Map<String, dynamic>;
      if (kind == 'dimensions') {
        entry['width'] = 2;
      } else {
        entry['mime'] = 'image/jpeg';
      }
      final configuration = FixtureWire(manifest: manifest).configuration();
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(configuration, recorder);
      final files = _Files();
      final cache = ImageExperimentImageCache(
        configuration,
        transport,
        recorder,
        storageConfiguration: Config(
          'synthetic-$kind',
          repo: NonStoringObjectProvider(),
          fileSystem: files,
        ),
      );
      final value = cache.provider(configuration.cases.values.single, 'one');
      final file = files.fs.file('/fixture')..writeAsBytesSync(fixtureBytes);
      await tester.runAsync(() async {
        await expectLater(
          cache.validateResponse(
            FileInfo(file, FileSource.Cache, DateTime.now(), ''),
            value.transferKey,
          ),
          throwsFormatException,
        );
        expect(cache.verified(value), false);
        await cache.close();
        await transport.close();
        await configuration.credentials.close();
      });
    });
  }
  testWidgets(
    'current target records actual draw but leaves rasterized and physical claims null',
    (tester) async {
      final configuration = FixtureWire().configuration();
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(configuration, recorder);
      final files = _Files();
      final cache = _ProbeCache(
        configuration,
        transport,
        recorder,
        storageConfiguration: Config(
          'synthetic-probe',
          repo: NonStoringObjectProvider(),
          fileSystem: files,
        ),
      );
      final value = ImageExperimentProvider(
        MemoryImage(fixtureBytes),
        'a' * 64,
        'owned',
        'b' * 64,
        configuration.cases.values.single,
      );
      var current = true, successes = 0, failures = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ImageExperimentTargetProbe(
              value: value,
              cache: cache,
              recorder: recorder,
              generation: 0,
              current: () => current,
              onDecoded: () => successes++,
              onFailure: () => failures++,
            ),
          ),
        ),
      );
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 50));
      });
      await tester.pumpAndSettle();
      expect(successes, 1);
      expect(failures, 0);
      var records = (jsonDecode(await recorder.export())['records'] as List)
          .cast<Map>();
      expect(records.any((e) => e['event'] == 'decoded'), true);
      expect(records.any((e) => e['event'] == 'draw'), true);
      expect(
        records
            .where((e) => e['event'] == 'draw')
            .every((e) => e['firstRasterizedTargetFrame'] == null),
        true,
      );
      current = false;
      await tester.pumpWidget(const SizedBox());
      await tester.pump();
      expect(cache.releases, 1);
      records = (jsonDecode(await recorder.export())['records'] as List)
          .cast<Map>();
      expect(records.last['ownership'], 'released');
      await tester.runAsync(() async {
        await cache.close();
        await transport.close();
        await configuration.credentials.close();
      });
    },
  );
}
