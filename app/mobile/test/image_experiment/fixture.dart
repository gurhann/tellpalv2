import 'dart:convert';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';

final fixtureBytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4//8/AAX+Av4N70a4AAAAAElFTkSuQmCC',
);
Map<String, dynamic> fixtureEntry() => {
  'caseId': 'synthetic',
  'role': 'isolated_raster_fixture',
  'arm': 'control',
  'sourceSha256': '1' * 64,
  'referenceSha256': '2' * 64,
  'outputSha256': sha256.convert(fixtureBytes).toString(),
  'mime': 'image/png',
  'byteSize': fixtureBytes.length,
  'width': 1,
  'height': 1,
  'encoderRevision': 'synthetic-r1',
  'profileRevision': 'synthetic-r1',
  'logicalSlotWidth': 1.0,
  'logicalSlotHeight': 1.0,
  'dpr': 1.0,
  'fit': 'contain',
  'alignmentX': 0.0,
  'alignmentY': 0.0,
  'purposeAcceptanceReceipt': 'synthetic-only',
};
Map<String, dynamic> fixtureManifest() => {
  'schemaVersion': 1,
  'scope': imageExperimentScope,
  'batchId': 'synthetic',
  'corpusSha256': '3' * 64,
  'referenceSha256': '2' * 64,
  'sourceSha256': '1' * 64,
  'entries': [fixtureEntry()],
};

class FixtureWire {
  FixtureWire({
    Uint8List? cert,
    String origin = 'https://localhost:8443',
    DateTime? clock,
    Map<String, dynamic>? manifest,
    String? rawManifest,
  }) {
    now = clock ?? DateTime.now();
    final certificate = cert ?? Uint8List.fromList([1, 2, 3]);
    final publicBytes = utf8.encode(
      rawManifest ?? jsonEncode(manifest ?? fixtureManifest()),
    );
    final claims = base64Url.encode(
      utf8.encode(
        jsonEncode({
          'sub': imageExperimentQaUid,
          'aud': 'synthetic-project',
          'iss': 'https://securetoken.google.com/synthetic-project',
          'exp': now.millisecondsSinceEpoch ~/ 1000 + 3600,
        }),
      ),
    );
    envelope = {
      'schemaVersion': 1,
      'scope': imageExperimentScope,
      'batchId': 'synthetic',
      'sessionId': 'synthetic-session',
      'generation': 0,
      'issuedAt': now.millisecondsSinceEpoch - 1,
      'expiresAt': now.millisecondsSinceEpoch + 120000,
      'expectedUid': imageExperimentQaUid,
      'accessAlias': 'synthetic-access',
      'backendEnvironment': 'test',
      'brokerOrigin': origin,
      'brokerCertificateDerBase64': base64Encode(certificate),
      'brokerCertificateSha256': sha256.convert(certificate).toString(),
      'manifestSha256': '4' * 64,
      'publicManifestSha256': sha256.convert(publicBytes).toString(),
      'publicManifestBase64': base64Encode(publicBytes),
      'idToken': 'e30.$claims.synthetic-signature',
    };
    metadata = {
      for (final key in [
        'scope',
        'batchId',
        'sessionId',
        'expectedUid',
        'backendEnvironment',
        'brokerOrigin',
        'brokerCertificateDerBase64',
        'brokerCertificateSha256',
        'manifestSha256',
        'publicManifestSha256',
      ])
        key: envelope[key],
      'deadline': now.millisecondsSinceEpoch + 30000,
      'firebaseProjectId': 'synthetic-project',
      'expectedAppId': 'com.tellpal.tellpal_mobile_poc.imageexperiment.profile',
      'caseId': 'synthetic',
    };
  }
  late final DateTime now;
  late final Map<String, dynamic> envelope, metadata;
  ImageExperimentConfiguration configuration() =>
      ImageExperimentConfiguration.parse(
        jsonEncode(envelope),
        metadata,
        now: () => now,
      );
}
