import 'dart:async';
import 'dart:convert';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_bootstrap.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/main_image_experiment.dart' as entry;
import 'fixture.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('synthetic-fixture-diagnostic');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test(
    'configuration observer stays silent on success and retains original refusal despite observer failure',
    () {
      final wire = FixtureWire();
      final stages = <ImageExperimentFixtureStage>[];
      final admitted = ImageExperimentConfiguration.parse(
        jsonEncode(wire.envelope),
        wire.metadata,
        now: () => wire.now,
        observeRefusal: stages.add,
      );
      expect(admitted.cases.length, 1);
      expect(stages, isEmpty);
      wire.envelope['expectedUid'] = 'private-marker';
      expect(
        () => ImageExperimentConfiguration.parse(
          jsonEncode(wire.envelope),
          wire.metadata,
          now: () => wire.now,
          observeRefusal: (stage) {
            stages.add(stage);
            throw StateError('private-marker');
          },
        ),
        throwsA(
          isA<FormatException>().having(
            (error) => error.message,
            'public refusal',
            'bootstrap_refused',
          ),
        ),
      );
      expect(stages, [ImageExperimentFixtureStage.configurationIdentity]);
      admitted.credentials.revoke();
    },
  );

  test(
    'configuration diagnostics expose only finite stages without envelope or error data',
    () {
      final cases = <ImageExperimentFixtureStage, void Function(FixtureWire)>{
        ImageExperimentFixtureStage.configurationFields: (wire) =>
            wire.envelope['private-marker'] = 'secret',
        ImageExperimentFixtureStage.configurationTime: (wire) =>
            wire.envelope['generation'] = true,
        ImageExperimentFixtureStage.configurationToken: (wire) =>
            wire.envelope['idToken'] = 'private-marker',
        ImageExperimentFixtureStage.configurationClaims: (wire) =>
            wire.metadata['firebaseProjectId'] = 'private-marker',
        ImageExperimentFixtureStage.configurationSelection: (wire) =>
            wire.metadata['caseId'] = 'private-marker',
      };
      for (final value in cases.entries) {
        final wire = FixtureWire();
        value.value(wire);
        final stages = <ImageExperimentFixtureStage>[];
        expect(
          () => ImageExperimentConfiguration.parse(
            jsonEncode(wire.envelope),
            wire.metadata,
            now: () => wire.now,
            observeRefusal: stages.add,
          ),
          throwsFormatException,
        );
        expect(stages, [value.key]);
        expect(stages.single.code, isNot(contains('private-marker')));
      }
      for (final stage in ImageExperimentFixtureStage.values) {
        expect(stage.code.length, lessThanOrEqualTo(48));
        expect(RegExp(r'^DART_[A-Z_]+$').hasMatch(stage.code), true);
      }
    },
  );

  for (final phase in ['native', 'response', 'configuration']) {
    test(
      'bootstrap $phase failure reports one fixed stage and keeps once-only revoke',
      () async {
        final wire = FixtureWire();
        final calls = <MethodCall>[];
        messenger.setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          if (call.method == 'reportDiagnostic') return true;
          if (call.method == 'revoke') return null;
          if (phase == 'native') {
            throw PlatformException(
              code: 'private-marker',
              details: {'token': 'private-marker'},
            );
          }
          if (phase == 'response') {
            return {'envelope': 'private-marker', 'metadata': false};
          }
          wire.envelope['expectedUid'] = 'private-marker';
          return {
            'envelope': jsonEncode(wire.envelope),
            'metadata': wire.metadata,
          };
        });
        final bootstrap = ImageExperimentBootstrap(channel: channel);
        await expectLater(
          bootstrap.takeSession(),
          throwsA(
            isA<FormatException>().having(
              (error) => error.message,
              'public refusal',
              'bootstrap_refused',
            ),
          ),
        );
        await bootstrap.revoke();
        await expectLater(bootstrap.takeSession(), throwsFormatException);
        await Future<void>.delayed(Duration.zero);
        expect(calls.where((call) => call.method == 'takeSession').length, 1);
        expect(calls.where((call) => call.method == 'revoke').length, 1);
        final diagnostic = calls.singleWhere(
          (call) => call.method == 'reportDiagnostic',
        );
        expect(diagnostic.arguments, switch (phase) {
          'native' => 'DART_NATIVE_HANDOFF',
          'response' => 'DART_NATIVE_RESPONSE',
          _ => 'DART_CONFIG_IDENTITY',
        });
        expect(diagnostic.arguments, isNot(contains('private-marker')));
      },
    );
  }

  test(
    'unavailable diagnostic channel never waits before original revoke or changes refusal',
    () async {
      final pendingDiagnostic = Completer<dynamic>();
      final revocations = <Object?>[];
      messenger.setMockMethodCallHandler(channel, (call) {
        if (call.method == 'reportDiagnostic') return pendingDiagnostic.future;
        if (call.method == 'revoke') {
          revocations.add(call.arguments);
          return Future.value();
        }
        throw PlatformException(code: 'private-marker');
      });
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      await expectLater(
        bootstrap.takeSession().timeout(const Duration(milliseconds: 100)),
        throwsFormatException,
      );
      expect(revocations, [
        {'result': 'refused'},
      ]);
      pendingDiagnostic.completeError(StateError('private-marker'));
      await Future<void>.delayed(Duration.zero);
      expect(await bootstrap.revoke(), true);
      expect(revocations.length, 1);
    },
  );

  testWidgets(
    'debug mode refusal diagnoses mode and preserves fallback without native admission',
    (tester) async {
      const native = MethodChannel('tellpal/image_experiment');
      final calls = <MethodCall>[];
      final priorFlutterError = FlutterError.onError;
      final priorErrorWidget = ErrorWidget.builder;
      final priorDispatcherError = PlatformDispatcher.instance.onError;
      addTearDown(() {
        messenger.setMockMethodCallHandler(native, null);
        FlutterError.onError = priorFlutterError;
        ErrorWidget.builder = priorErrorWidget;
        PlatformDispatcher.instance.onError = priorDispatcherError;
      });
      messenger.setMockMethodCallHandler(native, (call) async {
        calls.add(call);
        return null;
      });
      await entry.main();
      FlutterError.onError = priorFlutterError;
      ErrorWidget.builder = priorErrorWidget;
      PlatformDispatcher.instance.onError = priorDispatcherError;
      await tester.pump();
      expect(find.text('Experiment entry refused'), findsOneWidget);
      expect(calls.map((call) => call.method), ['reportDiagnostic', 'revoke']);
      expect(calls.first.arguments, 'DART_MODE');
      expect(calls.last.arguments, {'result': 'refused'});
    },
  );
}
