import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import '../features/image_experiment/data/model/raster_case_model.dart';
import 'image_experiment_json.dart';

enum ImageExperimentFixtureStage {
  dartNativeHandoff('DART_NATIVE_HANDOFF'),
  dartNativeResponse('DART_NATIVE_RESPONSE'),
  dartConfiguration('DART_CONFIGURATION'),
  dartMode('DART_MODE'),
  dartComposition('DART_COMPOSITION'),
  compositionRegistration('DART_COMPOSITION_REGISTRATION'),
  compositionCache('DART_COMPOSITION_CACHE'),
  compositionStatus('DART_COMPOSITION_STATUS'),
  statusAdmission('DART_STATUS_ADMISSION'),
  statusClient('DART_STATUS_CLIENT'),
  statusValidation('DART_STATUS_VALIDATION'),
  clientPreconditions('DART_CLIENT_PRECONDITIONS'),
  clientConstruction('DART_CLIENT_CONSTRUCTION'),
  trustContext('DART_TRUST_CONTEXT'),
  certificateImport('DART_CERTIFICATE_IMPORT'),
  certificateImportDerFailPemPass('DART_CERT_IMPORT_DER_FAIL_PEM_PASS'),
  certificateImportDerPassPemFail('DART_CERT_IMPORT_DER_PASS_PEM_FAIL'),
  certificateImportBothFail('DART_CERT_IMPORT_BOTH_FAIL'),
  certificateImportBothPass('DART_CERT_IMPORT_BOTH_PASS'),
  certificateImportUnavailable('DART_CERT_IMPORT_UNAVAILABLE'),
  requestPreconditions('DART_REQUEST_PRECONDITIONS'),
  tlsConnect('DART_TLS_CONNECT'),
  tlsHandshake('DART_TLS_HANDSHAKE'),
  tlsPinValidity('DART_TLS_PIN_VALIDITY'),
  outgoingRequest('DART_OUTGOING_REQUEST'),
  responseValidation('DART_RESPONSE_VALIDATION'),
  responseBody('DART_RESPONSE_BODY'),
  configurationEnvelope('DART_CONFIG_ENVELOPE'),
  configurationFields('DART_CONFIG_FIELDS'),
  configurationIdentity('DART_CONFIG_IDENTITY'),
  configurationIds('DART_CONFIG_IDS'),
  configurationLauncher('DART_CONFIG_LAUNCHER'),
  configurationTime('DART_CONFIG_TIME'),
  configurationEnvironment('DART_CONFIG_ENVIRONMENT'),
  configurationOrigin('DART_CONFIG_ORIGIN'),
  configurationApp('DART_CONFIG_APP'),
  configurationHashes('DART_CONFIG_HASHES'),
  configurationCertificate('DART_CONFIG_CERTIFICATE'),
  configurationToken('DART_CONFIG_TOKEN'),
  configurationClaims('DART_CONFIG_CLAIMS'),
  configurationManifestHash('DART_CONFIG_MANIFEST_HASH'),
  configurationManifest('DART_CONFIG_MANIFEST'),
  configurationBindings('DART_CONFIG_BINDINGS'),
  configurationCapacity('DART_CONFIG_CAPACITY'),
  configurationFixture('DART_CONFIG_FIXTURE'),
  configurationGeometry('DART_CONFIG_GEOMETRY'),
  configurationSelection('DART_CONFIG_SELECTION');

  const ImageExperimentFixtureStage(this.code);
  final String code;
}

typedef ImageExperimentRefusalObserver =
    void Function(ImageExperimentFixtureStage);

/// Passive per-entry observation; observer errors cannot change the original operation.
class ImageExperimentRefusalObservation {
  static const zoneKey = #imageExperimentRefusalStage;
  static ImageExperimentRefusalObserver? current([
    ImageExperimentRefusalObserver? observer,
  ]) {
    if (observer != null) return observer;
    final value = Zone.current[zoneKey];
    return value is ImageExperimentRefusalObserver ? value : null;
  }

  static void report(
    ImageExperimentFixtureStage stage,
    ImageExperimentRefusalObserver? observer,
  ) {
    try {
      observer?.call(stage);
    } catch (_) {}
  }

  static T construct<T>(
    ImageExperimentFixtureStage stage,
    T Function() operation, {
    ImageExperimentRefusalObserver? observer,
  }) {
    try {
      return operation();
    } catch (_) {
      report(stage, current(observer));
      rethrow;
    }
  }
}

const imageExperimentScope = 'image-optimization-native-fixture-r1';
const imageExperimentQaUid = 'jLRypTmoBrhhX21HsDJojurcKTC3';
final _id = RegExp(r'^[A-Za-z0-9_-]{1,64}$');
final _hash = RegExp(r'^[a-f0-9]{64}$');

/// Detached bounded runtime reference. It is never serialized or installed in Firebase.
class ImageExperimentCredentials {
  ImageExperimentCredentials(
    String token,
    this.expiresAt, {
    DateTime Function()? now,
  }) : _token = token,
       _now = now ?? DateTime.now;
  String? _token;
  final DateTime expiresAt;
  final DateTime Function() _now;
  final _revocations = StreamController<void>.broadcast(sync: true);
  Stream<void> get revocations => _revocations.stream;
  bool get isActive => _token != null && _now().isBefore(expiresAt);
  String requireActive() {
    if (!isActive) {
      revoke();
      throw StateError('session_inactive');
    }
    return _token!;
  }

  void revoke() {
    if (_token == null) return;
    _token = null;
    _revocations.add(null);
  }

  Future<void> close() async {
    revoke();
    await _revocations.close();
  }

  @override
  String toString() => 'ImageExperimentCredentials(redacted)';
}

class ImageExperimentConfiguration {
  ImageExperimentConfiguration._({
    required this.batchId,
    required this.sessionId,
    required this.generation,
    required this.accessAlias,
    required this.brokerOrigin,
    required this.certificateDer,
    required this.certificateSha256,
    required this.manifestSha256,
    required this.publicManifestSha256,
    required this.credentials,
    required this.cases,
    required this.selectedCaseId,
    required this.expectedAppId,
  });
  final String batchId,
      sessionId,
      accessAlias,
      certificateSha256,
      manifestSha256,
      publicManifestSha256;
  final int generation;
  final String selectedCaseId;
  String get mode => expectedAppId.split('.').last;
  final String expectedAppId;
  final Uri brokerOrigin;
  final Uint8List certificateDer;
  final ImageExperimentCredentials credentials;
  final Map<String, RasterCaseModel> cases;

  static ImageExperimentConfiguration parse(
    String raw,
    Map<String, dynamic> metadata, {
    DateTime Function()? now,
    void Function(ImageExperimentFixtureStage)? observeRefusal,
  }) {
    var stage = ImageExperimentFixtureStage.configurationEnvelope;
    try {
      return _parse(
        raw,
        metadata,
        now ?? DateTime.now,
        (value) => stage = value,
      );
    } catch (_) {
      try {
        observeRefusal?.call(stage);
      } catch (_) {}
      throw const FormatException('bootstrap_refused');
    }
  }

  static ImageExperimentConfiguration _parse(
    String raw,
    Map<String, dynamic> metadata,
    DateTime Function() now,
    void Function(ImageExperimentFixtureStage) observeStage,
  ) {
    if (utf8.encode(raw).length > 65536) {
      throw const FormatException('envelope_capacity');
    }
    final envelope = decodeImageExperimentJson(raw) as Map<String, dynamic>;
    observeStage(ImageExperimentFixtureStage.configurationFields);
    const keys = {
      'schemaVersion',
      'scope',
      'batchId',
      'sessionId',
      'generation',
      'issuedAt',
      'expiresAt',
      'expectedUid',
      'accessAlias',
      'backendEnvironment',
      'brokerOrigin',
      'brokerCertificateDerBase64',
      'brokerCertificateSha256',
      'manifestSha256',
      'publicManifestSha256',
      'publicManifestBase64',
      'idToken',
    };
    if (envelope.keys.toSet().difference(keys).isNotEmpty ||
        keys.difference(envelope.keys.toSet()).isNotEmpty) {
      throw const FormatException('envelope_fields');
    }
    observeStage(ImageExperimentFixtureStage.configurationIdentity);
    if (envelope['schemaVersion'] is! int ||
        envelope['schemaVersion'] != 1 ||
        envelope['scope'] != imageExperimentScope ||
        metadata['scope'] != imageExperimentScope ||
        envelope['expectedUid'] != imageExperimentQaUid ||
        metadata['expectedUid'] != imageExperimentQaUid) {
      throw const FormatException('identity');
    }
    observeStage(ImageExperimentFixtureStage.configurationIds);
    for (final key in ['batchId', 'sessionId', 'accessAlias']) {
      if (envelope[key] is! String || !_id.hasMatch(envelope[key])) {
        throw const FormatException('id');
      }
    }
    observeStage(ImageExperimentFixtureStage.configurationLauncher);
    for (final key in [
      'scope',
      'batchId',
      'sessionId',
      'brokerOrigin',
      'brokerCertificateDerBase64',
      'brokerCertificateSha256',
      'manifestSha256',
      'publicManifestSha256',
      'backendEnvironment',
    ]) {
      if (envelope[key] != metadata[key]) {
        throw const FormatException('launcher_mismatch');
      }
    }
    observeStage(ImageExperimentFixtureStage.configurationTime);
    if (envelope['generation'] is! int ||
        envelope['generation'] < 0 ||
        envelope['issuedAt'] is! int ||
        envelope['expiresAt'] is! int ||
        envelope['issuedAt'] < 0 ||
        envelope['expiresAt'] < 0 ||
        metadata['deadline'] is! int ||
        metadata['deadline'] < now().millisecondsSinceEpoch) {
      throw const FormatException('time');
    }
    final issued = envelope['issuedAt'] as int;
    final expires = envelope['expiresAt'] as int;
    final epoch = now().millisecondsSinceEpoch;
    if (issued > epoch ||
        expires <= epoch ||
        expires <= issued ||
        expires - issued > 180000) {
      throw const FormatException('time');
    }
    observeStage(ImageExperimentFixtureStage.configurationEnvironment);
    if (!['local', 'test'].contains(envelope['backendEnvironment'])) {
      throw const FormatException('environment');
    }
    observeStage(ImageExperimentFixtureStage.configurationOrigin);
    final origin = Uri.parse(envelope['brokerOrigin'] as String);
    if (origin.scheme != 'https' ||
        origin.host.isEmpty ||
        origin.userInfo.isNotEmpty ||
        origin.hasQuery ||
        origin.hasFragment ||
        (origin.path.isNotEmpty && origin.path != '/') ||
        origin.port <= 0 ||
        origin.port > 65535) {
      throw const FormatException('origin');
    }
    observeStage(ImageExperimentFixtureStage.configurationApp);
    final appId = metadata['expectedAppId'];
    if (appId is! String ||
        !RegExp(
          r'^com\.tellpal\.(tellpal_mobile_poc|tellpalMobilePoc)\.imageexperiment\.(profile|release)$',
        ).hasMatch(appId)) {
      throw const FormatException('app_identity');
    }
    observeStage(ImageExperimentFixtureStage.configurationHashes);
    for (final key in [
      'brokerCertificateSha256',
      'manifestSha256',
      'publicManifestSha256',
    ]) {
      if (envelope[key] is! String || !_hash.hasMatch(envelope[key])) {
        throw const FormatException('hash');
      }
    }
    observeStage(ImageExperimentFixtureStage.configurationCertificate);
    final cert = base64Decode(envelope['brokerCertificateDerBase64'] as String);
    if (cert.isEmpty ||
        cert.length > 16384 ||
        sha256.convert(cert).toString() !=
            envelope['brokerCertificateSha256']) {
      throw const FormatException('certificate');
    }
    observeStage(ImageExperimentFixtureStage.configurationToken);
    final token = envelope.remove('idToken');
    if (token is! String || utf8.encode(token).length > 8192 || token.isEmpty) {
      throw const FormatException('token');
    }
    final jwtParts = token.split('.');
    if (jwtParts.length != 3) throw const FormatException('token');
    observeStage(ImageExperimentFixtureStage.configurationClaims);
    final claims =
        decodeImageExperimentJson(
              utf8.decode(base64Url.decode(base64Url.normalize(jwtParts[1]))),
            )
            as Map<String, dynamic>;
    final project = metadata['firebaseProjectId'];
    if (project is! String ||
        project.isEmpty ||
        claims['sub'] != imageExperimentQaUid ||
        claims['aud'] != project ||
        claims['iss'] != 'https://securetoken.google.com/$project' ||
        claims['exp'] is! int ||
        claims['exp'] * 1000 <= epoch ||
        expires > claims['exp'] * 1000) {
      throw const FormatException('token_selection');
    }
    observeStage(ImageExperimentFixtureStage.configurationManifestHash);
    final manifestBytes = base64Decode(
      envelope['publicManifestBase64'] as String,
    );
    if (manifestBytes.length > 49152 ||
        sha256.convert(manifestBytes).toString() !=
            envelope['publicManifestSha256']) {
      throw const FormatException('manifest_hash');
    }
    observeStage(ImageExperimentFixtureStage.configurationManifest);
    final manifest =
        decodeImageExperimentJson(
              utf8.decode(manifestBytes, allowMalformed: false),
            )
            as Map<String, dynamic>;
    const manifestKeys = {
      'schemaVersion',
      'scope',
      'batchId',
      'corpusSha256',
      'referenceSha256',
      'sourceSha256',
      'entries',
    };
    if (manifest.keys.toSet().difference(manifestKeys).isNotEmpty ||
        manifestKeys.difference(manifest.keys.toSet()).isNotEmpty ||
        manifest['schemaVersion'] is! int ||
        manifest['schemaVersion'] != 1 ||
        manifest['scope'] != imageExperimentScope ||
        manifest['batchId'] != envelope['batchId']) {
      throw const FormatException('manifest');
    }
    observeStage(ImageExperimentFixtureStage.configurationBindings);
    for (final key in ['corpusSha256', 'referenceSha256', 'sourceSha256']) {
      if (manifest[key] is! String || !_hash.hasMatch(manifest[key])) {
        throw const FormatException('binding');
      }
    }
    observeStage(ImageExperimentFixtureStage.configurationCapacity);
    final entries = manifest['entries'] as List<dynamic>;
    if (entries.isEmpty || entries.length > 20) {
      throw const FormatException('manifest_capacity');
    }
    final cases = <String, RasterCaseModel>{};
    var totalBytes = 0;
    const caseKeys = {
      'caseId',
      'role',
      'arm',
      'sourceSha256',
      'referenceSha256',
      'outputSha256',
      'mime',
      'byteSize',
      'width',
      'height',
      'encoderRevision',
      'profileRevision',
      'logicalSlotWidth',
      'logicalSlotHeight',
      'dpr',
      'fit',
      'alignmentX',
      'alignmentY',
      'purposeAcceptanceReceipt',
    };
    for (final rawEntry in entries) {
      observeStage(ImageExperimentFixtureStage.configurationFixture);
      final entry = rawEntry as Map<String, dynamic>;
      if (entry.keys.toSet().difference(caseKeys).isNotEmpty ||
          caseKeys.difference(entry.keys.toSet()).isNotEmpty) {
        throw const FormatException('fixture_fields');
      }
      for (final key in [
        'caseId',
        'encoderRevision',
        'profileRevision',
        'purposeAcceptanceReceipt',
      ]) {
        if (entry[key] is! String || !_id.hasMatch(entry[key])) {
          throw const FormatException('fixture_identity');
        }
      }
      for (final key in ['sourceSha256', 'referenceSha256', 'outputSha256']) {
        if (entry[key] is! String || !_hash.hasMatch(entry[key])) {
          throw const FormatException('fixture_hash');
        }
      }
      if (entry['role'] != 'isolated_raster_fixture' ||
          !['control', 'candidate'].contains(entry['arm']) ||
          !['image/jpeg', 'image/png', 'image/webp'].contains(entry['mime']) ||
          !['contain', 'cover'].contains(entry['fit'])) {
        throw const FormatException('fixture_kind');
      }
      for (final key in ['byteSize', 'width', 'height']) {
        if (entry[key] is! int || entry[key] <= 0) {
          throw const FormatException('fixture_bounds');
        }
      }
      if (entry['byteSize'] > 20971520 ||
          entry['width'] > 8192 ||
          entry['height'] > 8192 ||
          entry['width'] * entry['height'] > 16777216) {
        throw const FormatException('fixture_bounds');
      }
      observeStage(ImageExperimentFixtureStage.configurationGeometry);
      for (final key in [
        'logicalSlotWidth',
        'logicalSlotHeight',
        'dpr',
        'alignmentX',
        'alignmentY',
      ]) {
        if (entry[key] is! num || !(entry[key] as num).isFinite) {
          throw const FormatException('geometry');
        }
      }
      if (entry['logicalSlotWidth'] <= 0 ||
          entry['logicalSlotHeight'] <= 0 ||
          entry['dpr'] <= 0 ||
          (entry['logicalSlotWidth'] * entry['dpr']) > 2147483647 ||
          (entry['logicalSlotHeight'] * entry['dpr']) > 2147483647 ||
          (entry['alignmentX'] as num).abs() > 1 ||
          (entry['alignmentY'] as num).abs() > 1) {
        throw const FormatException('geometry');
      }
      final value = RasterCaseModel.fromJson(entry);
      value.geometry;
      if (cases.containsKey(value.caseId)) {
        throw const FormatException('duplicate_case');
      }
      cases[value.caseId] = value;
      totalBytes += value.byteSize;
    }
    observeStage(ImageExperimentFixtureStage.configurationSelection);
    if (metadata['caseId'] is! String ||
        !cases.containsKey(metadata['caseId'])) {
      throw const FormatException('case_selection');
    }
    observeStage(ImageExperimentFixtureStage.configurationCapacity);
    if (totalBytes > 104857600) {
      throw const FormatException('manifest_capacity');
    }
    return ImageExperimentConfiguration._(
      batchId: envelope['batchId'],
      sessionId: envelope['sessionId'],
      generation: envelope['generation'],
      accessAlias: envelope['accessAlias'],
      brokerOrigin: origin,
      certificateDer: cert,
      certificateSha256: envelope['brokerCertificateSha256'],
      manifestSha256: envelope['manifestSha256'],
      publicManifestSha256: envelope['publicManifestSha256'],
      credentials: ImageExperimentCredentials(
        token,
        DateTime.fromMillisecondsSinceEpoch(expires, isUtc: true),
        now: now,
      ),
      cases: Map.unmodifiable(cases),
      selectedCaseId: metadata['caseId'],
      expectedAppId: appId,
    );
  }

  Uri objectUri(String caseId) {
    credentials.requireActive();
    if (!cases.containsKey(caseId) || caseId != selectedCaseId) {
      throw const FormatException('fixture_unknown');
    }
    return brokerOrigin.replace(
      path: '/experiment/v1/batches/$batchId/objects/$caseId',
    );
  }

  @override
  String toString() => 'ImageExperimentConfiguration(admitted)';
}
