import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_json.dart';
import 'fixture.dart';

void main() {
  test(
    'admitted immutable fixture detaches runtime token from serializable state',
    () {
      final wire = FixtureWire();
      final config = wire.configuration();
      expect(config.cases.length, 1);
      expect(config.credentials.isActive, true);
      expect(config.toString(), isNot(contains('synthetic-project')));
      expect(
        config.credentials.toString(),
        isNot(contains(wire.envelope['idToken'])),
      );
      expect(
        config.cases.values.single.toString(),
        isNot(contains('synthetic')),
      );
      config.credentials.revoke();
      expect(() => config.objectUri('synthetic'), throwsStateError);
    },
  );
  test('recursive duplicate keys and trailing JSON refuse before decoding', () {
    for (final raw in [
      '{"scope":1,"scope":2}',
      '{"x":{"a":1,"a":2}}',
      '{"x":1}{}',
      '[1,]',
      '{"x":1e999}',
    ]) {
      expect(() => decodeImageExperimentJson(raw), throwsFormatException);
    }
    expect(
      decodeImageExperimentJson('{"escaped\\"key": [1,true,null]}'),
      isA<Map>(),
    );
  });
  for (final change in <String, void Function(FixtureWire)>{
    'unknown envelope': (w) => w.envelope['rawSecret'] = 'forbidden',
    'wrong uid': (w) => w.envelope['expectedUid'] = 'other',
    'wrong session': (w) => w.envelope['sessionId'] = 'other',
    'wrong pin': (w) => w.envelope['brokerCertificateSha256'] = '0' * 64,
    'wrong public hash': (w) => w.envelope['publicManifestSha256'] = '0' * 64,
    'http origin': (w) {
      w.envelope['brokerOrigin'] = 'http://localhost:8443';
      w.metadata['brokerOrigin'] = w.envelope['brokerOrigin'];
    },
    'userinfo': (w) {
      w.envelope['brokerOrigin'] = 'https://secret@localhost:8443';
      w.metadata['brokerOrigin'] = w.envelope['brokerOrigin'];
    },
    'expired': (w) => w.envelope['expiresAt'] = w.now.millisecondsSinceEpoch,
    'future issued': (w) =>
        w.envelope['issuedAt'] = w.now.millisecondsSinceEpoch + 1,
    'excess lifetime': (w) =>
        w.envelope['expiresAt'] = w.now.millisecondsSinceEpoch + 180001,
    'bool generation': (w) => w.envelope['generation'] = true,
    'fraction schema': (w) => w.envelope['schemaVersion'] = 1.0,
    'ordinary app': (w) =>
        w.metadata['expectedAppId'] = 'com.tellpal.tellpal_mobile_poc',
    'wrong project': (w) => w.metadata['firebaseProjectId'] = 'other',
    'oversize token': (w) => w.envelope['idToken'] = 'x' * 8193,
    'launcher deadline': (w) =>
        w.metadata['deadline'] = w.now.millisecondsSinceEpoch - 1,
    'unknown dispatched case': (w) => w.metadata['caseId'] = 'missing',
  }.entries) {
    test('bootstrap refuses ${change.key} with fixed reason', () {
      final wire = FixtureWire();
      change.value(wire);
      expect(
        () => wire.configuration(),
        throwsA(
          isA<FormatException>().having(
            (e) => e.message,
            'code',
            'bootstrap_refused',
          ),
        ),
      );
    });
  }
  for (final change in <String, void Function(Map<String, dynamic>)>{
    'path in public': (e) => e['filePath'] = '/private/path',
    'duplicate case': (e) => e['caseId'] = 'invalid/id',
    'unsupported mime': (e) => e['mime'] = 'image/gif',
    'oversize': (e) => e['byteSize'] = 20971521,
    'boolean dimensions': (e) => e['width'] = true,
    'pixel excess': (e) {
      e['width'] = 8192;
      e['height'] = 8192;
    },
    'invalid alignment': (e) => e['alignmentX'] = 1.1,
    'invalid dpr': (e) => e['dpr'] = 0,
    'absent purpose': (e) => e.remove('purposeAcceptanceReceipt'),
  }.entries) {
    test('public manifest refuses ${change.key}', () {
      final manifest = fixtureManifest();
      change.value((manifest['entries'] as List).single);
      expect(
        () => FixtureWire(manifest: manifest).configuration(),
        throwsFormatException,
      );
    });
  }
  test(
    'public manifest exact bytes and duplicate keys are independently bound',
    () {
      final manifest = jsonEncode(fixtureManifest());
      final wire = FixtureWire(
        rawManifest: manifest.replaceFirst(
          '"schemaVersion":1',
          '"schemaVersion":1,"schemaVersion":1',
        ),
      );
      expect(() => wire.configuration(), throwsFormatException);
    },
  );
}
