import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_bootstrap.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/core/network/image_experiment_http_client.dart';
import 'synthetic_tls.dart';

class _NoHttp extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) =>
      throw StateError('unexpected_http_client');
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const unavailable = ImageExperimentFixtureStage.certificateImportUnavailable;
  final classifications = <(bool, bool), ImageExperimentFixtureStage>{
    (false, true): ImageExperimentFixtureStage.certificateImportDerFailPemPass,
    (true, false): ImageExperimentFixtureStage.certificateImportDerPassPemFail,
    (false, false): ImageExperimentFixtureStage.certificateImportBothFail,
    (true, true): ImageExperimentFixtureStage.certificateImportBothPass,
  };

  test(
    'real local DER and PEM contexts match independent import outcomes without a client or input mutation',
    () async {
      final identity = await SyntheticTlsIdentity.create();
      try {
        final originalPin = sha256.convert(identity.der).toString();
        final pem = await File(identity.certificatePath).readAsBytes();
        bool imports(List<int> bytes) {
          final context = SecurityContext(withTrustedRoots: false);
          try {
            context.setTrustedCertificatesBytes(bytes);
            return true;
          } catch (_) {
            return false;
          }
        }

        final outcomes = (imports(identity.der), imports(pem));
        expect(outcomes.$2, true);
        final result = HttpOverrides.runWithHttpOverrides(
          () =>
              ImageExperimentCertificateImportDiagnostic.compare(identity.der),
          _NoHttp(),
        );
        expect(result, classifications[outcomes]);
        expect(sha256.convert(identity.der).toString(), originalPin);
      } finally {
        await identity.close();
      }
    },
  );

  test(
    'all four fixed outcomes use exactly two bounded single-certificate inputs from one immutable DER',
    () {
      final der = Uint8List.fromList(List.generate(150, (index) => index));
      for (final entry in classifications.entries) {
        final inputs = <List<int>>[];
        final result = ImageExperimentCertificateImportDiagnostic.compare(
          der,
          importCertificateForTesting: (bytes) {
            inputs.add(bytes);
            if (!(inputs.length == 1 ? entry.key.$1 : entry.key.$2)) {
              throw StateError('private-marker');
            }
          },
          elapsedMicrosecondsForTesting: () => 0,
        );
        expect(result, entry.value);
        expect(inputs.length, 2);
        expect(inputs.first, der);
        expect(() => inputs.first[0] = 255, throwsUnsupportedError);
        final text = utf8.decode(inputs.last);
        final lines = text.split('\n');
        expect(lines.first, '-----BEGIN CERTIFICATE-----');
        expect(lines[lines.length - 2], '-----END CERTIFICATE-----');
        expect(lines.last, '');
        expect(
          lines
              .skip(1)
              .take(lines.length - 3)
              .every((line) => line.length <= 64),
          true,
        );
        expect(base64Decode(lines.skip(1).take(lines.length - 3).join()), der);
        expect(result.code.length, lessThanOrEqualTo(48));
        expect(RegExp(r'^DART_[A-Z_]+$').hasMatch(result.code), true);
        expect(result.code, isNot(contains('private-marker')));
      }
    },
  );

  test(
    'byte and monotonic bounds stop further imports and classify unavailable without error data',
    () {
      var calls = 0;
      void importer(List<int> _) => calls++;
      for (final der in [Uint8List(0), Uint8List(16385)]) {
        expect(
          ImageExperimentCertificateImportDiagnostic.compare(
            der,
            importCertificateForTesting: importer,
          ),
          unavailable,
        );
        expect(calls, 0);
      }
      final der = Uint8List.fromList([1, 2, 3]);
      for (final times in [
        [200000],
        [0, 200000],
        [0, 0, 200000],
        [0, 0, 0, 200000],
      ]) {
        calls = 0;
        var checks = 0;
        expect(
          ImageExperimentCertificateImportDiagnostic.compare(
            der,
            importCertificateForTesting: importer,
            elapsedMicrosecondsForTesting: () => times[checks++],
          ),
          unavailable,
        );
        expect(calls, times.length < 3 ? 0 : times.length - 2);
      }
      calls = 0;
      expect(
        ImageExperimentCertificateImportDiagnostic.compare(
          Uint8List(16384),
          importCertificateForTesting: importer,
          elapsedMicrosecondsForTesting: () => 0,
        ),
        ImageExperimentFixtureStage.certificateImportBothPass,
      );
      expect(calls, 2);
      expect(
        ImageExperimentCertificateImportDiagnostic.compare(
          der,
          elapsedMicrosecondsForTesting: () =>
              throw StateError('private-marker'),
        ),
        unavailable,
      );
    },
  );

  test(
    'real malformed DER and PEM imports both fail and preserve the exact original error and stack despite observer failure',
    () {
      final der = Uint8List.fromList([1, 2, 3]);
      expect(
        ImageExperimentCertificateImportDiagnostic.compare(der),
        ImageExperimentFixtureStage.certificateImportBothFail,
      );
      final original = StateError('private-marker');
      final originalStack = StackTrace.fromString('private-stack-marker');
      var calls = 0;
      final stages = <ImageExperimentFixtureStage>[];
      Object? error;
      StackTrace? stack;
      try {
        ImageExperimentCertificateImportDiagnostic.observe(
          () {
            calls++;
            Error.throwWithStackTrace(original, originalStack);
          },
          der,
          observer: (stage) {
            stages.add(stage);
            throw StateError('observer-private-marker');
          },
        );
      } catch (caught, trace) {
        error = caught;
        stack = trace;
      }
      expect(error, same(original));
      expect(stack.toString(), originalStack.toString());
      expect(calls, 1);
      expect(stages, [ImageExperimentFixtureStage.certificateImportBothFail]);
    },
  );

  test(
    'successful original real import stays silent and never runs a differential observer',
    () async {
      final identity = await SyntheticTlsIdentity.create();
      try {
        final pem = await File(identity.certificatePath).readAsBytes();
        final originalContext = SecurityContext(withTrustedRoots: false);
        final stages = <ImageExperimentFixtureStage>[];
        var calls = 0;
        ImageExperimentCertificateImportDiagnostic.observe(
          () {
            calls++;
            originalContext.setTrustedCertificatesBytes(pem);
          },
          identity.der,
          observer: stages.add,
        );
        expect(calls, 1);
        expect(stages, isEmpty);
      } finally {
        await identity.close();
      }
    },
  );

  test(
    'unknown scoped observers preserve original refusal and surrounding observation state',
    () {
      final original = StateError('private-marker');
      runZoned(
        () {
          expect(ImageExperimentRefusalObservation.current(), null);
          expect(
            () => ImageExperimentCertificateImportDiagnostic.observe(
              () => throw original,
              Uint8List.fromList([1, 2, 3]),
            ),
            throwsA(same(original)),
          );
        },
        zoneValues: {
          ImageExperimentRefusalObservation.zoneKey: 'private-marker',
        },
      );
      expect(ImageExperimentRefusalObservation.current(), null);
    },
  );

  test(
    'first differential code uses existing private channel without generic overwrite or extra revoke',
    () async {
      const channel = MethodChannel('synthetic-certificate-diagnostic');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return null;
      });
      addTearDown(() => messenger.setMockMethodCallHandler(channel, null));
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      final original = StateError('private-marker');
      expect(
        () => ImageExperimentCertificateImportDiagnostic.observe(
          () => throw original,
          Uint8List.fromList([1, 2, 3]),
          observer: bootstrap.reportRefusal,
        ),
        throwsA(same(original)),
      );
      bootstrap.reportRefusal(ImageExperimentFixtureStage.certificateImport);
      await bootstrap.revoke();
      await bootstrap.revoke();
      await Future<void>.delayed(Duration.zero);
      expect(calls.map((call) => call.method), ['reportDiagnostic', 'revoke']);
      expect(calls.first.arguments, 'DART_CERT_IMPORT_BOTH_FAIL');
      expect(
        jsonEncode(calls.map((call) => call.arguments).toList()),
        isNot(contains('private-marker')),
      );
    },
  );
}
