import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

import '../../test_driver/reading_consumer_qa_probe.dart';
import 'fixture.dart';
import 'reading_comparison_probe_test.dart' show jwt, png, RealLocalHttp;
import 'reading_image_comparison_cubit_test.dart' show PendingRepository;

Map<String, dynamic> config(int port) {
  final start = DateTime.now().millisecondsSinceEpoch;
  return {
    'action': 'configure',
    'runId': consumerQaRunId,
    'proposalSha256': consumerQaProposalSha,
    'uid': 'jLRypTmoBrhhX21HsDJojurcKTC3',
    'contentId': 94,
    'port': port,
    'token': jwt(),
    'windowStartedAtEpochMillis': start,
    'windowDeadlineEpochMillis': start + 180000,
  };
}

Map<String, String> identityQuery() => {
  'contentId': '94',
  'language': 'tr',
  'liveGeneration': '4',
  'revision':
      '3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d',
  'manifestDigest':
      'd4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2',
  'jobId': 'bb9ae40b-e5ab-4054-885e-0b1fc14e841e',
};

const keys = [
  '8d3c882f7d82a7f4e0f80894b04322407494dc96eb4e26bb3408de0e501a3c45',
  'a02aac0b9869606049e1714b72faa459fef9913363e3801e8da62c02f5dbab92',
  '02d8b7d9015ec2cb1a930552392bac42e33c951c7232bad8ff076075c23c1226',
  '8d3c882f7d82a7f4e0f80894b04322407494dc96eb4e26bb3408de0e501a3c45',
];
Map<String, String> mediaQuery(int order) => {
  ...identityQuery(),
  'canonicalSource': 'false',
  'renditionKey': keys[order],
  'role': order == 0 || order == 3 ? 'READING_DETAIL' : 'READING_PAGE',
  'pageNumber': order == 0 || order == 3 ? '0' : '$order',
};

class _NativeGate extends ReadingComparisonWorkGate {
  _NativeGate(this.zone);
  final Zone zone;
  @override
  Future<void> run(Future<void> Function() work) => zone.run(() => super.run(work));
}

class _PaintCubit extends ReadingImageComparisonCubit {
  _PaintCubit(PendingRepository repository)
    : super(
        LoadReadingImageComparison(repository),
        repository,
        const ReadingImageComparisonConfiguration.geometry(),
        ReadingComparisonWorkGate(),
        const ReadingComparisonClock(),
      );
  void loaded(ReadingComparisonRaster raster, int epoch) => emit(
    ReadingImageComparisonState(status: 'loaded', raster: raster, epoch: epoch),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'sabit kimlik ve kesin tek pencere dışındaki runtime config reddedilir',
    () {
      final input = config(12345);
      final parsed = parseConsumerQaConfiguration(input);
      expect(parsed.limits!.maxPeakBytes, 134217728);
      for (final wrong in [
        {...input, 'contentId': 71},
        {...input, 'contentId': 94.0},
        {...input, 'proposalSha256': 'changed'},
        {...input, 'token': jwt(uid: 'other')},
        {
          ...input,
          'windowDeadlineEpochMillis': input['windowStartedAtEpochMillis'],
        },
        {
          ...input,
          'windowStartedAtEpochMillis': 1,
          'windowDeadlineEpochMillis': 180001,
        },
        {...input, 'extra': input['token']},
      ]) {
        expect(() => parseConsumerQaConfiguration(wrong), throwsA(anything));
      }
      parsed.account!.credentials.revoke();
    },
  );

  test(
    'HTTP öncesi dört sıralı medya ve tek manifest/status tavanı kesilir',
    () {
      final configuration = parseConsumerQaConfiguration(config(12345));
      final events = <Map<String, Object?>>[];
      final source = ObservedConsumerQaSource(configuration, events.add);
      source.reserve('manifest', {'contentId': '94', 'language': 'tr'});
      for (var i = 0; i < 4; i++) { source.reserve('bytes', mediaQuery(i)); }
      for (var i = 0; i < 18; i++) { source.reserve('status', identityQuery()); }
      expect(events.length, 23);
      expect(() => source.reserve('bytes', mediaQuery(0)), throwsA(anything));
      expect(events.length, 23);
      configuration.account!.credentials.revoke();
    },
  );

  test(
    'rol/page/identity/fallback/izinsiz endpoint çağrı öncesi reddedilir',
    () {
      final configuration = parseConsumerQaConfiguration(config(12345));
      for (final query in [
        {...mediaQuery(0), 'role': 'READING_PAGE'},
        {...mediaQuery(0), 'pageNumber': '1'},
        {...mediaQuery(0), 'jobId': 'changed'},
        {...mediaQuery(0), 'canonicalSource': 'true'},
        {...mediaQuery(0), 'pageId': '1427'},
      ]) {
        final events = <Map<String, Object?>>[];
        final source = ObservedConsumerQaSource(configuration, events.add);
        expect(() => source.reserve('bytes', query), throwsA(anything));
        expect(events, isEmpty);
        expect(source.failed, true);
      }
      final source = ObservedConsumerQaSource(configuration, (_) {});
      expect(() => source.reserve('audio', identityQuery()), throwsA(anything));
      configuration.account!.credentials.revoke();
    },
  );

  testWidgets(
    'ikinci configure/select kapalı; stop credential ve timer bırakır',
    (tester) async {
      final probe = ReadingConsumerQaProbe();
      final message = jsonEncode(config(12345));
      expect(jsonDecode(await probe.handle(message))['ok'], true);
      expect(jsonDecode(await probe.handle(message))['ok'], false);
      expect(
        jsonDecode(
          await probe.handle(
            '{"action":"select","entryIndex":2,"canonicalSource":false}',
          ),
        )['ok'],
        false,
      );
      final stopped = jsonDecode(await probe.handle('{"action":"stop"}'));
      expect(stopped['credentialActive'], false);
      expect(stopped['deadlineActive'], false);
      expect(stopped['stopped'], true);
      await tester.pumpWidget(const SizedBox());
      await comparisonGetIt.reset();
    },
  );

  testWidgets(
    'gerçek router/loopback: bozuk retained payload fallback açmadan durur',
    (tester) async {
      tester.view.physicalSize = const Size(822, 1828);
      tester.view.devicePixelRatio = 2;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final prior = Bloc.observer;
      late ReadingConsumerQaProbe probe;
      probe = ReadingConsumerQaProbe(initializeDependencies: (configuration) async {
        configureReadingImageComparisonDependencies(configuration);
        await comparisonGetIt.unregister<ReadingImageComparisonDataSource>();
        comparisonGetIt.registerFactory<ReadingImageComparisonDataSource>(() => ObservedConsumerQaSource(configuration, probe.record));
        await comparisonGetIt.unregister<ReadingComparisonWorkGate>();
        comparisonGetIt.registerSingleton<ReadingComparisonWorkGate>(_NativeGate(Zone.current));
      });
      Bloc.observer = probe;
      late HttpServer server;
      var calls = 0;
      await tester.runAsync(() async {
        server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
        final envelope =
            jsonDecode(
                  await File(
                    '../../be/src/test/resources/p4/consumer-retained-ready.json',
                  ).readAsString(),
                )
                as Map;
        envelope['leaseMillis'] = 180000;
        server.listen((request) async {
          calls++;
          final media = request.uri.path.endsWith('/bytes');
          expect(request.headers.value('cache-control'), 'no-store');
          expect(request.headers.value('accept-encoding'), 'identity');
          final bytes = media
              ? Uint8List(3292721)
              : utf8.encode(jsonEncode(envelope));
          request.response.headers.set('Cache-Control', 'private,no-store');
          request.response.headers.contentType = media
              ? ContentType('image', 'png')
              : ContentType.json;
          request.response.contentLength = bytes.length;
          request.response.add(bytes);
          await request.response.close();
        });
        await HttpOverrides.runWithHttpOverrides(() async {
          expect(
            jsonDecode(
              await probe.handle(jsonEncode(config(server.port))),
            )['ok'],
            true,
          );
        }, RealLocalHttp());
      });
      try {
        for (var i = 0; i < 100 && probe.snapshot()['status'] != 'error'; i++) {
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump(const Duration(milliseconds: 30));
        }
        expect(probe.snapshot()['status'], 'error');
        expect(calls, 2);
        expect(probe.snapshot()['cubitCount'], 1);
        expect(probe.snapshot()['activeImages'], 0);
        expect(find.textContaining('QA görseli durdu'), findsOneWidget);
        expect(
          (probe.snapshot()['events'] as List)
              .where((e) => e['kind'] == 'http' && e['endpoint'] == 'bytes')
              .length,
          1,
        );
      } finally {
        await tester.runAsync(probe.stop);
        await tester.pumpWidget(const SizedBox());
        await comparisonGetIt.reset();
        Bloc.observer = prior;
        await tester.runAsync(() => server.close(force: true));
      }
    },
  );

  testWidgets(
    'native codec paint aynı epoch/raster ister; eski epoch makbuz olmaz',
    (tester) async {
      final configuration = parseConsumerQaConfiguration(config(12345));
      final bytes = (await tester.runAsync(() => png(2, 2)))!;
      final owned = (await tester.runAsync(
        () => ReadingComparisonRaster.decode(
          bytes,
          raster(width: 2, height: 2, bytes: bytes.length),
          2,
          2,
          configuration.limits!,
          ReadingComparisonCancellation(),
        ),
      ))!;
      final prior = Bloc.observer;
      final probe = ReadingConsumerQaProbe();
      Bloc.observer = probe;
      final cubit = _PaintCubit(PendingRepository());
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      try {
        cubit.loaded(owned, 7);
        await tester.pumpWidget(
          Directionality(
            textDirection: TextDirection.ltr,
            child: Center(
              child: SizedBox(
                width: 30,
                height: 30,
                child: ReadingImageComparisonRaster(
                  raster: owned,
                  fit: BoxFit.contain,
                ),
              ),
            ),
          ),
        );
        final painting = probe.handle('{"action":"paint","epoch":7}');
        await tester.pump();
        expect(jsonDecode(await painting)['paintedEpoch'], 7);
        final stale = probe.handle('{"action":"paint","epoch":7}');
        cubit.loaded(owned, 8);
        await tester.pump();
        expect(jsonDecode(await stale)['ok'], false);
      } finally {
        await tester.pumpWidget(const SizedBox());
        await cubit.close();
        owned.dispose();
        await configuration.account!.credentials.close();
        Bloc.observer = prior;
      }
    },
  );
}
