import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get_it/get_it.dart';
import 'package:http/http.dart' as http;
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
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

class _CaptureHttp extends HttpOverrides {
  final contexts = <SecurityContext?>[];
  final clients = <HttpClient>[];
  @override
  HttpClient createHttpClient(SecurityContext? context) {
    contexts.add(context);
    final client = super.createHttpClient(context);
    clients.add(client);
    return client;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late SyntheticTlsIdentity identity;
  late HttpServer server;
  late StreamSubscription<HttpRequest> subscription;
  final received = <String>[];
  final authenticated = <bool>[];
  var admitted = true;
  setUp(() async {
    identity = await SyntheticTlsIdentity.create();
    server = await HttpServer.bindSecure(
      InternetAddress.loopbackIPv4,
      0,
      identity.context,
    );
    received.clear();
    authenticated.clear();
    admitted = true;
    subscription = server.listen((request) async {
      received.add(request.uri.path);
      authenticated.add(
        request.headers
                    .value(HttpHeaders.authorizationHeader)
                    ?.startsWith('Bearer ') ==
                true &&
            request.headers.value('X-Experiment-Session') ==
                'synthetic-session',
      );
      request.response.headers.contentType = ContentType.json;
      request.response.write(
        jsonEncode(
          request.method == 'POST'
              ? {'status': 'revoked', 'reason': 'app_revoked'}
              : {'status': admitted ? 'admitted' : 'revoked', 'reason': 'none'},
        ),
      );
      await request.response.close();
    }, onError: (Object _) {});
  });
  tearDown(() async {
    await subscription.cancel();
    await server.close(force: true);
    await identity.close();
  });
  ImageExperimentConfiguration configuration() => FixtureWire(
    cert: identity.der,
    origin: 'https://localhost:${server.port}',
  ).configuration();

  void realTest(String description, FutureOr<void> Function() body) {
    test(
      description,
      () => HttpOverrides.runWithHttpOverrides(body, _RealHttp()),
    );
  }

  Future<void> withOwnedPlugins(
    Future<void> Function(Directory root) body,
  ) async {
    final root = await Directory.systemTemp.createTemp('tellpal-composition-');
    final previousFactory = databaseFactoryOrNull;
    sqfliteFfiInit();
    final previousDatabasePath = await databaseFactoryFfi.getDatabasesPath();
    databaseFactory = databaseFactoryFfi;
    await databaseFactoryFfi.setDatabasesPath(root.path);
    const pathChannel = MethodChannel('plugins.flutter.io/path_provider');
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(pathChannel, (_) async => root.path);
    try {
      await body(root);
    } finally {
      messenger.setMockMethodCallHandler(pathChannel, null);
      await databaseFactoryFfi.setDatabasesPath(previousDatabasePath);
      databaseFactoryOrNull = previousFactory;
      await root.delete(recursive: true);
    }
  }

  realTest(
    'production create propagates its observer into generated lazy TLS client and retains detailed first refusal',
    () => withOwnedPlugins((root) async {
      final other = await SyntheticTlsIdentity.create();
      final global = GetIt.instance;
      const channel = MethodChannel('synthetic-production-refusal');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return call.method == 'storeLedger' ? true : null;
      });
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      final config = FixtureWire(
        cert: other.der,
        origin: 'https://localhost:${server.port}',
      ).configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final captured = _CaptureHttp();
      try {
        await HttpOverrides.runWithHttpOverrides(() async {
          await expectLater(
            ImageExperimentComposition.create(
              config,
              bootstrap,
              observeRefusal: (stage) {
                stages.add(stage);
                bootstrap.reportRefusal(stage);
              },
            ),
            throwsA(
              isA<FormatException>().having(
                (error) => error.message,
                'original refusal',
                'composition_refused',
              ),
            ),
          );
        }, captured);
        await Future<void>.delayed(Duration.zero);
        expect(stages.first, ImageExperimentFixtureStage.tlsHandshake);
        expect(
          calls
              .where((call) => call.method == 'reportDiagnostic')
              .single
              .arguments,
          'DART_TLS_HANDSHAKE',
        );
        expect(calls.map((call) => call.method), [
          'reportDiagnostic',
          'storeLedger',
          'revoke',
        ]);
        final ledger =
            jsonDecode(
                  calls
                          .singleWhere((call) => call.method == 'storeLedger')
                          .arguments
                      as String,
                )
                as Map<String, dynamic>;
        final records = ledger['records'] as List;
        expect(
          records.where((row) => row['event'] == 'status_request'),
          isEmpty,
        );
        expect(records.where((row) => row['event'] == 'revoke').length, 1);
        expect(records.where((row) => row['event'] == 'failure'), isEmpty);
        expect(ledger['droppedCount'], 0);
        expect(captured.contexts.whereType<SecurityContext>().length, 1);
        expect(config.credentials.isActive, false);
        expect(identical(GetIt.instance, global), true);
        expect(received, isEmpty);
        expect(authenticated, isEmpty);
        expect(
          root.listSync().whereType<File>().any(
            (file) => file.path.endsWith('.db'),
          ),
          true,
        );
        await bootstrap.revoke();
        expect(calls.where((call) => call.method == 'revoke').length, 1);
      } finally {
        messenger.setMockMethodCallHandler(channel, null);
        config.credentials.revoke();
        await other.close();
        for (final client in captured.clients) {
          client.close(force: true);
        }
      }
    }),
  );

  realTest(
    'successful production create status and close stay silent and release generated owners and cache files',
    () => withOwnedPlugins((root) async {
      final global = GetIt.instance;
      const channel = MethodChannel('synthetic-production-success');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return call.method == 'storeLedger' ? true : null;
      });
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      ImageExperimentComposition? composition;
      try {
        composition = await ImageExperimentComposition.create(
          config,
          bootstrap,
          observeRefusal: (stage) {
            stages.add(stage);
            bootstrap.reportRefusal(stage);
          },
        );
        final container = composition.container;
        final transport = container<ImageExperimentTransport>();
        final cache = container<ImageExperimentImageCache>();
        expect(
          container
              .checkLazySingletonInstanceExists<ImageExperimentTransport>(),
          true,
        );
        expect(
          container
              .checkLazySingletonInstanceExists<ImageExperimentImageCache>(),
          true,
        );
        expect(identical(container, global), false);
        expect(identical(GetIt.instance, global), true);
        final owned = await cache.putFile(
          '',
          fixtureBytes,
          key: 'synthetic-owned',
        );
        expect(owned.path.startsWith('${root.path}/'), true);
        expect(await owned.exists(), true);
        expect(stages, isEmpty);
        final closing = composition.close();
        expect(identical(closing, composition.close()), true);
        await closing;
        expect(stages, isEmpty);
        expect(calls.map((call) => call.method), ['storeLedger', 'revoke']);
        expect((calls.last.arguments as Map)['result'], 'refused');
        expect(transport.authenticatedRevokeObserved, true);
        expect(config.credentials.isActive, false);
        expect(container.isRegistered<ImageExperimentTransport>(), false);
        expect(container.isRegistered<ImageExperimentImageCache>(), false);
        expect(container.isRegistered<ImageExperimentConfiguration>(), false);
        expect(await owned.exists(), false);
        await expectLater(
          cache.putFile('', fixtureBytes, key: 'after-close'),
          throwsStateError,
        );
        final ledger =
            jsonDecode(calls.first.arguments as String) as Map<String, dynamic>;
        expect(
          (ledger['records'] as List).where((row) => row['event'] == 'failure'),
          isEmpty,
        );
        expect(ledger['droppedCount'], 0);
        expect(received, [
          '/experiment/v1/session/status',
          '/experiment/v1/session/revoke',
        ]);
        expect(authenticated, [true, true]);
      } finally {
        await composition?.close();
        messenger.setMockMethodCallHandler(channel, null);
        config.credentials.revoke();
      }
    }),
  );

  for (final scenario in ['matching', 'wrong-pin', 'untrusted-peer']) {
    realTest(
      'production single-leaf PEM context preserves pinned TLS before authenticated headers: $scenario',
      () async {
        final other = scenario == 'untrusted-peer'
            ? await SyntheticTlsIdentity.create()
            : null;
        final selectedDer = other?.der ?? identity.der;
        final encoded = base64Encode(selectedDer);
        final lines = [
          for (var offset = 0; offset < encoded.length; offset += 64)
            encoded.substring(
              offset,
              offset + 64 < encoded.length ? offset + 64 : encoded.length,
            ),
        ];
        final singlePem = utf8.encode(
          '-----BEGIN CERTIFICATE-----\n${lines.join('\n')}\n-----END CERTIFICATE-----\n',
        );
        SecurityContext(
          withTrustedRoots: false,
        ).setTrustedCertificatesBytes(singlePem);
        final config = FixtureWire(
          cert: selectedDer,
          origin: 'https://localhost:${server.port}',
        ).configuration();
        final before = List<int>.from(config.certificateDer);
        final stages = <ImageExperimentFixtureStage>[];
        final captured = _CaptureHttp();
        ImageExperimentHttpClient? client;
        try {
          await HttpOverrides.runWithHttpOverrides(() async {
            client = ImageExperimentHttpClient(
              config,
              ImageExperimentRecorder(),
              pinOverrideForTesting: scenario == 'wrong-pin' ? '0' * 64 : null,
              observeRefusal: stages.add,
            );
            final request = client!.get(
              config.brokerOrigin.replace(
                path: '/experiment/v1/session/status',
              ),
            );
            if (scenario == 'matching') {
              expect((await request).statusCode, 200);
              expect(stages, isEmpty);
              expect(received, ['/experiment/v1/session/status']);
              expect(authenticated, [true]);
            } else {
              await expectLater(request, throwsStateError);
              expect(received, isEmpty);
              expect(authenticated, isEmpty);
              expect(
                stages.first,
                scenario == 'wrong-pin'
                    ? ImageExperimentFixtureStage.tlsPinValidity
                    : ImageExperimentFixtureStage.tlsHandshake,
              );
            }
          }, captured);
          expect(captured.contexts.length, 1);
          expect(captured.contexts.single, isNotNull);
          expect(
            identical(captured.contexts.single, SecurityContext.defaultContext),
            false,
          );
          expect(config.certificateDer, before);
        } finally {
          client?.close();
          config.credentials.revoke();
          await other?.close();
        }
      },
    );
  }

  realTest(
    'registration and cache refusal observation preserves generated lazy registration and original errors',
    () async {
      final global = GetIt.instance;
      final container = GetIt.asNewInstance();
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      container.registerSingleton<ImageExperimentConfiguration>(config);
      ImageExperimentRefusalObservation.construct(
        ImageExperimentFixtureStage.compositionRegistration,
        () => container.initImageExperiment(),
        observer: stages.add,
      );
      expect(stages, isEmpty);
      expect(container.isRegistered<ImageExperimentTransport>(), true);
      expect(container.isRegistered<ImageExperimentImageCache>(), true);
      expect(
        container.checkLazySingletonInstanceExists<ImageExperimentTransport>(),
        false,
      );
      expect(
        () => ImageExperimentRefusalObservation.construct(
          ImageExperimentFixtureStage.compositionRegistration,
          () =>
              container.registerSingleton<ImageExperimentConfiguration>(config),
          observer: (stage) {
            stages.add(stage);
            throw StateError('private-marker');
          },
        ),
        throwsArgumentError,
      );
      final original = StateError('private-marker');
      await container.unregister<ImageExperimentImageCache>();
      container.registerFactory<ImageExperimentImageCache>(
        () => throw original,
      );
      expect(
        () => ImageExperimentRefusalObservation.construct(
          ImageExperimentFixtureStage.compositionCache,
          () => container<ImageExperimentImageCache>(),
          observer: stages.add,
        ),
        throwsA(same(original)),
      );
      expect(stages, [
        ImageExperimentFixtureStage.compositionRegistration,
        ImageExperimentFixtureStage.compositionCache,
      ]);
      expect(identical(global, GetIt.instance), true);
      expect(
        container.checkLazySingletonInstanceExists<ImageExperimentTransport>(),
        false,
      );
      config.credentials.revoke();
      await container.reset();
    },
  );

  realTest(
    'unknown zone values and throwing observers cannot change original certificate import refusal',
    () {
      final config = FixtureWire().configuration();
      Object? baselineError;
      try {
        ImageExperimentHttpClient(config, ImageExperimentRecorder());
      } catch (error) {
        baselineError = error;
      }
      expect(baselineError, isNotNull);
      final stages = <ImageExperimentFixtureStage>[];
      Object? observedError;
      try {
        ImageExperimentHttpClient(
          config,
          ImageExperimentRecorder(),
          observeRefusal: (stage) {
            stages.add(stage);
            throw StateError('private-marker');
          },
        );
      } catch (error) {
        observedError = error;
      }
      expect(observedError.runtimeType, baselineError.runtimeType);
      expect(stages, [
        ImageExperimentFixtureStage.certificateImportBothFail,
        ImageExperimentFixtureStage.certificateImport,
      ]);
      runZoned(
        () {
          expect(ImageExperimentRefusalObservation.current(), null);
          expect(
            () => ImageExperimentHttpClient(config, ImageExperimentRecorder()),
            throwsA(isA<TlsException>()),
          );
        },
        zoneValues: {
          ImageExperimentRefusalObservation.zoneKey: 'private-marker',
        },
      );
      config.credentials.revoke();
    },
  );

  realTest(
    'real pinned status and authenticated revoke remain silent on successful passive observation',
    () async {
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final transport = ImageExperimentTransport(
        config,
        ImageExperimentRecorder(),
        observeRefusal: stages.add,
      );
      await transport.admitStatus();
      expect(stages, isEmpty);
      await transport.close();
      expect(transport.authenticatedRevokeObserved, true);
      expect(stages, isEmpty);
      expect(received, [
        '/experiment/v1/session/status',
        '/experiment/v1/session/revoke',
      ]);
    },
  );

  realTest(
    'real wrong pin reports pin validity before bearer and preserves refusal when observer throws',
    () async {
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final client = ImageExperimentHttpClient(
        config,
        ImageExperimentRecorder(),
        pinOverrideForTesting: '0' * 64,
        observeRefusal: (stage) {
          stages.add(stage);
          throw StateError('private-marker');
        },
      );
      await expectLater(
        client.get(
          config.brokerOrigin.replace(path: '/experiment/v1/session/status'),
        ),
        throwsA(
          isA<StateError>().having(
            (error) => error.message,
            'original refusal',
            'transport_refused',
          ),
        ),
      );
      client.close();
      expect(stages.first, ImageExperimentFixtureStage.tlsPinValidity);
      expect(received, isEmpty);
      config.credentials.revoke();
    },
  );

  realTest(
    'real untrusted TLS handshake refuses before any authenticated HTTP request',
    () async {
      final other = await SyntheticTlsIdentity.create();
      try {
        final config = FixtureWire(
          cert: other.der,
          origin: 'https://localhost:${server.port}',
        ).configuration();
        final stages = <ImageExperimentFixtureStage>[];
        final client = ImageExperimentHttpClient(
          config,
          ImageExperimentRecorder(),
          observeRefusal: stages.add,
        );
        await expectLater(
          client.get(
            config.brokerOrigin.replace(path: '/experiment/v1/session/status'),
          ),
          throwsStateError,
        );
        client.close();
        expect(stages.first, ImageExperimentFixtureStage.tlsHandshake);
        expect(received, isEmpty);
        config.credentials.revoke();
      } finally {
        await other.close();
      }
    },
  );

  realTest(
    'request preconditions keep headers and wrong origins refused without a TLS or HTTP attempt',
    () async {
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final client = ImageExperimentHttpClient(
        config,
        ImageExperimentRecorder(),
        observeRefusal: stages.add,
      );
      final request = http.Request(
        'GET',
        config.brokerOrigin.replace(path: '/experiment/v1/session/status'),
      )..headers['authorization'] = 'private-marker';
      await expectLater(client.send(request), throwsStateError);
      await expectLater(
        client.get(
          config.brokerOrigin.replace(
            host: 'unselected.invalid',
            path: '/experiment/v1/session/status',
          ),
        ),
        throwsStateError,
      );
      client.close();
      expect(stages, [
        ImageExperimentFixtureStage.requestPreconditions,
        ImageExperimentFixtureStage.requestPreconditions,
      ]);
      expect(received, isEmpty);
      expect(
        stages.map((stage) => stage.code).join(),
        isNot(contains('private-marker')),
      );
      config.credentials.revoke();
    },
  );

  realTest(
    'initial status invalid response reports status validation and keeps invalidation and zero revoke requests',
    () async {
      admitted = false;
      final config = configuration();
      final stages = <ImageExperimentFixtureStage>[];
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(
        config,
        recorder,
        observeRefusal: stages.add,
      );
      await expectLater(
        transport.admitStatus(),
        throwsA(
          isA<StateError>().having(
            (error) => error.message,
            'original refusal',
            'session_inactive',
          ),
        ),
      );
      await transport.close();
      expect(stages.first, ImageExperimentFixtureStage.statusValidation);
      expect(config.credentials.isActive, false);
      expect(transport.authenticatedRevokeObserved, false);
      expect(received, ['/experiment/v1/session/status']);
    },
  );

  realTest(
    'scoped typed zone observes initial client import before any status request and leaves surrounding zone unchanged',
    () async {
      const channel = MethodChannel('synthetic-transport-diagnostic');
      final calls = <MethodCall>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            calls.add(call);
            return null;
          });
      addTearDown(
        () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null),
      );
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      final config = FixtureWire().configuration();
      final recorder = ImageExperimentRecorder();
      final transport = runZoned(
        () => ImageExperimentTransport(config, recorder),
        zoneValues: {
          ImageExperimentRefusalObservation.zoneKey: bootstrap.reportRefusal,
        },
      );
      expect(ImageExperimentRefusalObservation.current(), null);
      await expectLater(transport.admitStatus(), throwsStateError);
      await transport.close();
      await bootstrap.revoke();
      await Future<void>.delayed(Duration.zero);
      expect(calls.map((call) => call.method), ['reportDiagnostic', 'revoke']);
      expect(calls.first.arguments, 'DART_CERT_IMPORT_BOTH_FAIL');
      final ledger =
          jsonDecode(await recorder.export()) as Map<String, dynamic>;
      expect(
        (ledger['records'] as List).where(
          (row) => row['event'] == 'status_request',
        ),
        isEmpty,
      );
      expect(
        (ledger['records'] as List)
            .where((row) => row['event'] == 'revoke')
            .length,
        1,
      );
      expect(ledger['droppedCount'], 0);
    },
  );
}
