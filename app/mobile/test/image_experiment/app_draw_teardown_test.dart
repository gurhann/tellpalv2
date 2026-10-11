import 'dart:convert';
import 'dart:io';
import 'package:file/file.dart' as files;
import 'package:file/memory.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get_it/get_it.dart';
import 'package:http/http.dart' as http;
import 'package:tellpal_mobile_poc/app/image_experiment_app.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_bootstrap.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_composition.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_composition.config.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_image_cache.dart';
import 'fixture.dart';
import 'synthetic_tls.dart';

class _RealHttp extends HttpOverrides {}

class _RealTransport extends ImageExperimentTransport {
  _RealTransport(super.configuration, super.recorder);
  @override
  http.Client newClient() =>
      HttpOverrides.runWithHttpOverrides(super.newClient, _RealHttp());
  @override
  Future<void> admitStatus() =>
      HttpOverrides.runWithHttpOverrides(super.admitStatus, _RealHttp());
  @override
  Future<void> close() =>
      HttpOverrides.runWithHttpOverrides(super.close, _RealHttp());
}

class _Files implements FileSystem {
  final fs = MemoryFileSystem();
  @override
  Future<files.File> createFile(String name) async => fs.file('/$name');
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets(
    'actual routed view draw authenticates revoke and settles all owners',
    (tester) async {
      const channel = MethodChannel('synthetic-draw-teardown');
      final identity = (await tester.runAsync(SyntheticTlsIdentity.create))!;
      final server = (await tester.runAsync(
        () => HttpServer.bindSecure(
          InternetAddress.loopbackIPv4,
          0,
          identity.context,
        ),
      ))!;
      final configuration = FixtureWire(
        cert: identity.der,
        origin: 'https://localhost:${server.port}',
      ).configuration();
      final paths = <String>[];
      String? outcome;
      final token = configuration.credentials.requireActive();
      final subscription = (await tester.runAsync(
        () async => server.listen((request) async {
          paths.add(request.uri.path);
          expect(
            request.headers.value(HttpHeaders.authorizationHeader),
            'Bearer $token',
          );
          expect(
            request.headers.value('X-Experiment-Session'),
            configuration.sessionId,
          );
          if (request.uri.path.endsWith('/revoke')) {
            expect(request.contentLength, 2);
            expect(
              request.headers.value(HttpHeaders.transferEncodingHeader),
              null,
            );
            expect(
              request.headers.value(HttpHeaders.contentTypeHeader),
              'application/json',
            );
            expect(await utf8.decoder.bind(request).join(), '{}');
            request.response.headers.contentType = ContentType.json;
            request.response.write(
              jsonEncode({'status': 'revoked', 'reason': 'app_revoked'}),
            );
          } else if (request.uri.path.endsWith('/status')) {
            request.response.headers.contentType = ContentType.json;
            request.response.write(
              jsonEncode({'status': 'admitted', 'reason': 'none'}),
            );
          } else {
            expect(request.uri.path, configuration.objectUri('synthetic').path);
            request.response.headers.contentType = ContentType('image', 'png');
            request.response.contentLength = fixtureBytes.length;
            request.response.add(fixtureBytes);
          }
          await request.response.close();
        }),
      ))!;
      final container = GetIt.asNewInstance();
      container.registerSingleton<ImageExperimentConfiguration>(configuration);
      container.initImageExperiment();
      container.unregister<ImageExperimentImageCache>();
      container.unregister<ImageExperimentTransport>();
      final recorder = container<ImageExperimentRecorder>();
      final transport = _RealTransport(configuration, recorder);
      container.registerSingleton<ImageExperimentTransport>(transport);
      final cache = (await tester.runAsync(
        () async => ImageExperimentImageCache(
          configuration,
          transport,
          recorder,
          storageConfiguration: Config(
            'synthetic-routed-draw',
            repo: NonStoringObjectProvider(),
            fileSystem: _Files(),
          ),
        ),
      ))!;
      container.registerSingleton<ImageExperimentImageCache>(cache);
      final composition = ImageExperimentComposition.forTesting(
        container,
        ImageExperimentBootstrap(channel: channel),
        configuration,
      );
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            if (call.method == 'storeLedger') {
              final rows =
                  (jsonDecode(call.arguments as String)['records'] as List)
                      .cast<Map>();
              expect(rows.any((row) => row['event'] == 'draw'), true);
              return true;
            }
            outcome = (call.arguments as Map)['result'] as String;
            return null;
          });
      try {
        await tester.runAsync(transport.admitStatus);
        await tester.pumpWidget(ImageExperimentApp(composition: composition));
        for (var step = 0; step < 100 && outcome == null; step++) {
          await tester.runAsync(() async {
            await Future<void>.delayed(const Duration(milliseconds: 20));
          });
          await tester.pump(const Duration(milliseconds: 20));
        }
        expect(outcome, 'settled');
        expect(transport.authenticatedRevokeObserved, true);
        expect(
          paths.where((path) => path.endsWith('/objects/synthetic')).length,
          1,
        );
        expect(paths.where((path) => path.endsWith('/revoke')).length, 1);
        expect(find.text('Experiment entry refused'), findsOneWidget);
        expect(container.isRegistered<ImageExperimentConfiguration>(), false);
        expect(container.isRegistered<ImageExperimentImageCache>(), false);
        expect(tester.takeException(), null);
      } finally {
        await tester.pumpWidget(const SizedBox());
        await tester.runAsync(() async {
          await composition.close().catchError((_) {});
          await subscription.cancel();
          await server.close(force: true);
          await identity.close();
        });
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
      }
    },
  );
}
