import 'dart:async';
import 'dart:convert';
import 'package:dartz/dartz.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_bootstrap.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/domain/entity/raster_case.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/domain/repository/raster_case_repository.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/domain/use_case/get_raster_case.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/raster_case_cubit.dart';
import 'package:tellpal_mobile_poc/features/image_experiment/presentation/cubit/raster_case_state.dart';
import 'fixture.dart';

class _Repository implements RasterCaseRepository {
  final pending = Completer<Either<Failure, RasterCase?>>();
  @override
  Future<Either<Failure, RasterCase?>> getCase(String caseId) => pending.future;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('synthetic-image-experiment');
  tearDown(
    () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null),
  );
  test('native envelope is taken once and revoke is idempotent', () async {
    final wire = FixtureWire();
    var takes = 0, revokes = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'revoke') {
            revokes++;
            expect(call.arguments, {'result': 'refused'});
            return null;
          }
          takes++;
          return {
            'envelope': jsonEncode(wire.envelope),
            'metadata': wire.metadata,
          };
        });
    final bootstrap = ImageExperimentBootstrap(channel: channel);
    expect((await bootstrap.takeSession()).cases.length, 1);
    await expectLater(bootstrap.takeSession(), throwsFormatException);
    await bootstrap.revoke();
    await bootstrap.revoke();
    expect(takes, 1);
    expect(revokes, 1);
  });
  test(
    'late native callback after revocation cannot admit a container',
    () async {
      final wire = FixtureWire();
      final pending = Completer<dynamic>();
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            channel,
            (call) => call.method == 'revoke' ? Future.value() : pending.future,
          );
      final bootstrap = ImageExperimentBootstrap(channel: channel);
      final take = bootstrap.takeSession();
      await bootstrap.revoke();
      pending.complete({
        'envelope': jsonEncode(wire.envelope),
        'metadata': wire.metadata,
      });
      await expectLater(take, throwsFormatException);
    },
  );
  test('departed Cubit rejects late repository callback', () async {
    final configuration = FixtureWire().configuration();
    final repository = _Repository();
    final cubit = RasterCaseCubit(GetRasterCase(repository), configuration);
    final load = cubit.load('synthetic');
    await cubit.close();
    repository.pending.complete(Right(configuration.cases.values.single));
    await load;
    expect(cubit.state.status, RasterCaseStatus.loading);
  });
  test(
    'revoked session replaces loading state and rejects late success',
    () async {
      final configuration = FixtureWire().configuration();
      final repository = _Repository();
      final cubit = RasterCaseCubit(GetRasterCase(repository), configuration);
      final load = cubit.load('synthetic');
      configuration.credentials.revoke();
      repository.pending.complete(Right(configuration.cases.values.single));
      await load;
      cubit.success(1);
      expect(cubit.state.status, RasterCaseStatus.refused);
      await cubit.close();
    },
  );
}
