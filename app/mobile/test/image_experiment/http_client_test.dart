import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/diagnostics/image_experiment_recorder.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'fixture.dart';

void main() {
  late Directory directory;
  late HttpServer server;
  late Uint8List cert;
  late StreamSubscription<HttpRequest> subscription;
  final received = <Map<String, Object?>>[];
  var behavior = 'success';
  late Completer<void> delayedStatus, revokeStarted, lateStatusReturned;
  final responseOrder = <String>[];
  setUp(() async {
    directory = await Directory.systemTemp.createTemp('tellpal-synthetic-tls-');
    final key = '${directory.path}/key.pem',
        certificate = '${directory.path}/cert.pem',
        der = '${directory.path}/cert.der';
    final generate = await Process.run('/usr/bin/openssl', [
      'req',
      '-x509',
      '-newkey',
      'rsa:2048',
      '-nodes',
      '-keyout',
      key,
      '-out',
      certificate,
      '-subj',
      '/CN=localhost',
      '-addext',
      'subjectAltName=DNS:localhost',
      '-addext',
      'basicConstraints=critical,CA:TRUE',
      '-addext',
      'extendedKeyUsage=serverAuth',
      '-addext',
      'keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign',
      '-days',
      '1',
    ]);
    expect(generate.exitCode, 0);
    final convert = await Process.run('/usr/bin/openssl', [
      'x509',
      '-in',
      certificate,
      '-outform',
      'DER',
      '-out',
      der,
    ]);
    expect(convert.exitCode, 0);
    cert = await File(der).readAsBytes();
    final context = SecurityContext()
      ..useCertificateChain(certificate)
      ..usePrivateKey(key);
    server = await HttpServer.bindSecure(
      InternetAddress.loopbackIPv4,
      0,
      context,
    );
    received.clear();
    behavior = 'success';
    delayedStatus = Completer<void>();
    revokeStarted = Completer<void>();
    lateStatusReturned = Completer<void>();
    responseOrder.clear();
    subscription = server.listen((request) async {
      received.add({
        'path': request.uri.path,
        'authorization': request.headers.value(HttpHeaders.authorizationHeader),
        'length': request.contentLength,
        'type': request.headers.value(HttpHeaders.contentTypeHeader),
        'transferEncoding': request.headers.value(
          HttpHeaders.transferEncodingHeader,
        ),
      });
      if (request.method == 'POST' &&
          (request.contentLength != 2 ||
              request.headers.value(HttpHeaders.contentTypeHeader) !=
                  'application/json' ||
              request.headers.value(HttpHeaders.transferEncodingHeader) !=
                  null)) {
        request.response.statusCode = 400;
        await request.response.close();
        return;
      }
      if (behavior == 'redirect') {
        request.response.statusCode = 302;
        request.response.headers.set('location', 'https://example.invalid/');
      } else if (behavior == 'mime') {
        request.response.headers.contentType = ContentType('text', 'plain');
        request.response.contentLength = fixtureBytes.length;
        request.response.add(fixtureBytes);
      } else if (behavior == 'length') {
        request.response.headers.contentType = ContentType('image', 'png');
        request.response.contentLength = fixtureBytes.length + 1;
        request.response.add([...fixtureBytes, 0]);
      } else if (behavior == 'pending') {
        return;
      } else if (request.uri.path.endsWith('/status')) {
        request.response.headers.contentType = ContentType.json;
        final racing =
            behavior == 'poll-race' &&
            received
                    .where((e) => (e['path'] as String).endsWith('/status'))
                    .length ==
                2;
        if (racing) {
          delayedStatus.complete();
          await revokeStarted.future;
        }
        request.response.write(
          jsonEncode(
            racing
                ? {'status': 'revoked', 'reason': 'app_revoked'}
                : {'status': 'admitted', 'reason': 'none'},
          ),
        );
        if (racing) {
          await request.response.close().catchError((_) {});
          responseOrder.add('retired-status');
          lateStatusReturned.complete();
          return;
        }
      } else if (request.uri.path.endsWith('/revoke')) {
        expect(await utf8.decoder.bind(request).join(), '{}');
        if (behavior == 'poll-race') {
          revokeStarted.complete();
          await lateStatusReturned.future;
          responseOrder.add('revoke');
        }
        request.response.headers.contentType = ContentType.json;
        request.response.write(
          jsonEncode({
            'status': 'revoked',
            'reason': behavior == 'bad-revoke'
                ? 'session_expired'
                : 'app_revoked',
          }),
        );
      } else {
        request.response.headers.contentType = ContentType('image', 'png');
        request.response.contentLength = fixtureBytes.length;
        request.response.add(fixtureBytes);
      }
      await request.response.close();
    });
  });
  tearDown(() async {
    await subscription.cancel();
    await server.close(force: true);
    await directory.delete(recursive: true);
  });
  FixtureWire wire() =>
      FixtureWire(cert: cert, origin: 'https://localhost:${server.port}');
  test('pin mismatch prevents all HTTP bytes and bearer headers', () async {
    final configuration = wire().configuration();
    final client = ImageExperimentHttpClient(
      configuration,
      ImageExperimentRecorder(),
      pinOverrideForTesting: '0' * 64,
    );
    await expectLater(
      client.get(configuration.objectUri('synthetic')),
      throwsStateError,
    );
    expect(received, isEmpty);
    client.close();
    await configuration.credentials.close();
  });
  test(
    'verified leaf and hostname permit only exact admitted fixture request',
    () async {
      final configuration = wire().configuration();
      final client = ImageExperimentHttpClient(
        configuration,
        ImageExperimentRecorder(),
      );
      final response = await client.get(configuration.objectUri('synthetic'));
      expect(response.bodyBytes, fixtureBytes);
      expect(received.single['authorization'], startsWith('Bearer '));
      await expectLater(
        client.get(Uri.parse('https://localhost:${server.port}/arbitrary')),
        throwsStateError,
      );
      await expectLater(
        client.get(
          Uri.parse(
            'http://localhost:${server.port}/experiment/v1/session/status',
          ),
        ),
        throwsStateError,
      );
      expect(received.length, 1);
      client.close();
      await configuration.credentials.close();
    },
  );
  for (final invalid in ['redirect', 'mime', 'length']) {
    test('$invalid response refuses without alternate request', () async {
      behavior = invalid;
      final configuration = wire().configuration();
      final client = ImageExperimentHttpClient(
        configuration,
        ImageExperimentRecorder(),
      );
      await expectLater(
        client.get(configuration.objectUri('synthetic')),
        throwsStateError,
      );
      expect(received.length, 1);
      client.close();
      await configuration.credentials.close();
    });
  }
  test(
    'transport owned status overhead is distinct and remote revoke is bounded',
    () async {
      final configuration = wire().configuration();
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(configuration, recorder);
      await transport.admitStatus();
      await transport.close();
      expect(configuration.credentials.isActive, false);
      expect(transport.authenticatedRevokeObserved, true);
      expect(
        received.map((e) => e['path']),
        contains('/experiment/v1/session/revoke'),
      );
      final records = (jsonDecode(await recorder.export())['records'] as List)
          .cast<Map>();
      expect(records.where((e) => e['event'] == 'request'), isEmpty);
      expect(records.any((e) => e['event'] == 'status_request'), true);
      expect(
        records.any((e) => e['observerOverheadMicroseconds'] is int),
        true,
      );
      await configuration.credentials.close();
    },
  );
  test('strict Python framing accepts the authenticated revoke body', () async {
    final configuration = wire().configuration();
    final transport = ImageExperimentTransport(
      configuration,
      ImageExperimentRecorder(),
    );
    await transport.close();
    expect(transport.authenticatedRevokeObserved, true);
    expect(received.single['length'], 2);
    expect(received.single['type'], 'application/json');
    expect(received.single['transferEncoding'], null);
    await configuration.credentials.close();
  });
  test(
    'retired status cannot cancel authenticated revoke and close is coalesced',
    () async {
      behavior = 'poll-race';
      final configuration = wire().configuration();
      final transport = ImageExperimentTransport(
        configuration,
        ImageExperimentRecorder(),
      );
      await transport.admitStatus();
      final observer = expectLater(transport.admitStatus(), throwsStateError);
      await delayedStatus.future.timeout(const Duration(seconds: 2));
      final first = transport.close(), second = transport.close();
      expect(identical(first, second), true);
      expect(() => transport.newClient(), throwsStateError);
      await first;
      await observer;
      expect(transport.authenticatedRevokeObserved, true);
      expect(responseOrder, ['retired-status', 'revoke']);
      expect(
        received.where((e) => (e['path'] as String).endsWith('/revoke')).length,
        1,
      );
      await configuration.credentials.close();
    },
  );
  test(
    'real Dart transport interoperates with Python FixtureHandler over pinned TLS',
    () async {
      final root = Directory.current.parent.parent.path;
      final wire = FixtureWire(cert: cert);
      final process = await Process.start(
        '$root/tools/image-optimization/.venv/bin/python',
        [
          '-B',
          '$root/app/mobile/test/image_experiment/python_fixture_broker.py',
        ],
      );
      final errors = process.stderr.drain<void>();
      final lines = process.stdout
          .transform(utf8.decoder)
          .transform(const LineSplitter());
      process.stdin.writeln(
        jsonEncode({
          'certificate': '${directory.path}/cert.pem',
          'key': '${directory.path}/key.pem',
          'session': wire.envelope['sessionId'],
          'token': wire.envelope['idToken'],
        }),
      );
      await process.stdin.flush();
      ImageExperimentTransport? transport;
      try {
        final report =
            jsonDecode(await lines.first.timeout(const Duration(seconds: 3)))
                as Map;
        final origin = 'https://localhost:${report['port']}';
        wire.envelope['brokerOrigin'] = origin;
        wire.metadata['brokerOrigin'] = origin;
        final configuration = wire.configuration();
        transport = ImageExperimentTransport(
          configuration,
          ImageExperimentRecorder(),
        );
        await transport.admitStatus();
        await transport.close();
        expect(transport.authenticatedRevokeObserved, true);
        await configuration.credentials.close();
      } finally {
        await transport?.close();
        process.stdin.writeln('stop');
        await process.stdin.close();
        try {
          expect(await process.exitCode.timeout(const Duration(seconds: 3)), 0);
        } on TimeoutException {
          process.kill(ProcessSignal.sigkill);
          await process.exitCode;
          rethrow;
        }
        await errors;
      }
    },
  );
  test(
    'expiry or unrelated final status cannot claim authenticated app revoke',
    () async {
      behavior = 'bad-revoke';
      final configuration = wire().configuration();
      final transport = ImageExperimentTransport(
        configuration,
        ImageExperimentRecorder(),
      );
      await transport.close();
      expect(transport.authenticatedRevokeObserved, false);
      expect(configuration.credentials.isActive, false);
      await configuration.credentials.close();
    },
  );
  test(
    'revocation cancels an owned outstanding response and late callbacks',
    () async {
      behavior = 'pending';
      final configuration = wire().configuration();
      final recorder = ImageExperimentRecorder();
      final transport = ImageExperimentTransport(configuration, recorder);
      final client = transport.newClient();
      final pending = client.get(configuration.objectUri('synthetic'));
      while (received.isEmpty) {
        await Future<void>.delayed(const Duration(milliseconds: 5));
      }
      configuration.credentials.revoke();
      await expectLater(
        pending.timeout(const Duration(seconds: 1)),
        throwsStateError,
      );
      await transport.close();
      await configuration.credentials.close();
    },
  );
}
