import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/repository/reading_image_comparison_repository_impl.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../../test_driver/manual_consumer_qa/coordinator.dart';
import '../../test_driver/manual_consumer_qa/private_protocol.dart';
import '../../test_driver/reading_consumer_manual_entry.dart'
    show ManualConsumerQaView;
import '../../test_driver/reading_consumer_qa_probe.dart';
import 'manual_consumer_protocol_test.dart'
    show SyntheticManualQaPlatform, temporary, writePrivate;
import 'reading_image_consumer_qa_test.dart'
    show ConsumerTestLoopback, consumerTestGate;
import 'reading_p4b5_qa_probe_test.dart' show p4b5Config;

class _HttpOverrides extends HttpOverrides {}

Future<void> native(WidgetTester tester, Future<void> Function() task) async {
  await tester.runAsync(
    () => HttpOverrides.runWithHttpOverrides(task, _HttpOverrides()),
  );
}

Future<void> until(WidgetTester tester, bool Function() done) async {
  for (var i = 0; i < 250 && !done(); i++) {
    await native(
      tester,
      () => Future<void>.delayed(const Duration(milliseconds: 10)),
    );
    await tester.pump(const Duration(milliseconds: 20));
  }
  expect(done(), true);
  await tester.pump();
}

void main() {
  for (final selected in [
    ConsumerQaExpectedScope.p4b5Manual,
    ConsumerQaExpectedScope.p4b5ManualRetry,
  ]) {
    for (final outcome in [
      'complete',
      'captureLifecycle',
      'retry',
      'cleanupFailure',
      'lostAck',
    ]) {
      testWidgets(
        '${selected.measurementId} gerçek entry view router protocol $outcome paint ACK ve lifecycle sınırını korur',
        (tester) async {
          tester.view.physicalSize = const Size(822, 1828);
          tester.view.devicePixelRatio = 2;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);
          final oldObserver = Bloc.observer;
          final server = ConsumerTestLoopback();
          late ManualConsumerQaCoordinator coordinator;
          late ManualQaPrivateProtocol protocol;
          late Directory directory;
          late ReadingImageComparisonCubit cubit;
          late Zone nativeZone;
          await native(tester, () async {
            nativeZone = Zone.current;
            directory = await temporary();
            protocol = ManualQaPrivateProtocol(
              directory,
              platform: const SyntheticManualQaPlatform(),
              scope: selected,
            );
            await server.start(contentId: 94);
            server.descriptor['leaseMillis'] = 180000;
          });
          final probe = ReadingConsumerQaProbe(
            expectedScope: selected,
            mount: (widget) => coordinator.mountApp(widget),
            initializeDependencies: (configuration) async {
              nativeZone.run(() {
                final repo = ReadingImageComparisonRepositoryImpl(
                  ReadingImageComparisonDataSource(configuration),
                  configuration,
                );
                final gate = consumerTestGate(nativeZone);
                comparisonGetIt.registerSingleton<ReadingComparisonWorkGate>(
                  gate,
                );
                cubit = ReadingImageComparisonCubit(
                  LoadReadingImageComparison(repo),
                  repo,
                  configuration,
                  gate,
                  const ReadingComparisonClock(),
                );
                comparisonGetIt.registerFactory<ReadingImageComparisonCubit>(
                  () => cubit,
                );
              });
            },
          );
          coordinator = ManualConsumerQaCoordinator(protocol, probe);
          Bloc.observer = probe;
          tester.binding.handleAppLifecycleStateChanged(
            AppLifecycleState.resumed,
          );
          try {
            await tester.pumpWidget(
              BlocProvider.value(
                value: coordinator,
                child: const ManualConsumerQaView(),
              ),
            );
            await native(tester, coordinator.initialize);
            await native(tester, () async {
              final configure = p4b5Config(server.server.port, selected);
              server.expectedToken = configure['token'] as String;
              await writePrivate(
                File('${protocol.secrets.path}/handoff.owner'),
                protocol.nonce,
              );
              await writePrivate(
                File('${protocol.secrets.path}/configure.json'),
                {...protocol.identity(), 'configure': configure},
              );
            });
            await until(tester, () => coordinator.state.stage == 'ready');
            expect(probe.snapshot()['cubitCount'], 0);
            await tester.tap(find.text('Başlat'));
            await until(
              tester,
              () => coordinator.state.stage == 'capturePending',
            );
            expect(coordinator.allows('page1'), false);
            final firstCount = server.delivered.length;
            await tester.pump(const Duration(milliseconds: 250));
            expect(server.delivered.length, firstCount);
            expect(
              find.text('Ekran kaydı bekleniyor. Ekranda kalın.'),
              findsOneWidget,
            );
            if (outcome == 'lostAck') {
              await tester.pump(const Duration(milliseconds: 120001));
              await until(tester, () => coordinator.state.stage == 'failed');
              expect(protocol.captureCount, 0);
              expect(server.delivered.length, 1);
              await native(tester, () async {
                expect(
                  await File(
                    '${protocol.receipts.path}/terminal-failure.json',
                  ).exists(),
                  true,
                );
                expect(
                  await File(
                    '${protocol.receipts.path}/10-stopped.json',
                  ).exists(),
                  false,
                );
              });
              return;
            }
            if (outcome == 'captureLifecycle') {
              tester.binding.handleAppLifecycleStateChanged(
                AppLifecycleState.inactive,
              );
              await until(tester, () => coordinator.state.stage == 'failed');
              await native(tester, () async {
                expect(
                  await File(
                    '${protocol.receipts.path}/terminal-failure.json',
                  ).exists(),
                  true,
                );
                expect(
                  await File(
                    '${protocol.receipts.path}/10-stopped.json',
                  ).exists(),
                  false,
                );
              });
              expect(server.delivered.length, 1);
              return;
            }
            Future<void> acknowledge() async {
              final paint = protocol.captureCount + 1;
              final epoch = probe.snapshot()['currentEpoch'];
              await native(
                tester,
                () => writePrivate(
                  File(
                    '${protocol.captures.path}/capture-${paint.toString().padLeft(2, '0')}.json',
                  ),
                  {
                    ...protocol.identity(),
                    'phase': 'paint$paint',
                    'epoch': epoch,
                    'receiptSha256': protocol.previousSha,
                    'pngSha256': 'a' * 64,
                  },
                ),
              );
              await until(
                tester,
                () =>
                    coordinator.state.stage == 'painted' ||
                    coordinator.state.stage == 'beforeBackground',
              );
            }

            await acknowledge();
            if (outcome == 'retry') {
              cubit.retire('synthetic');
              await tester.pump();
              expect(
                tester
                    .widget<TextButton>(
                      find.widgetWithText(TextButton, 'Yeniden admission'),
                    )
                    .onPressed,
                isNull,
              );
              await until(tester, () => coordinator.state.stage == 'failed');
              expect(
                server.requests
                    .where((u) => u.path.endsWith('/manifest'))
                    .length,
                1,
              );
              return;
            }
            for (final label in [
              'QA sayfalarını aç',
              'Sonraki',
              'QA detail ekranına dön',
            ]) {
              final control = label == 'QA detail ekranına dön'
                  ? find.byTooltip(label)
                  : find.text(label);
              await tester.ensureVisible(control);
              await tester.tap(control);
              await until(tester, () {
                if (coordinator.state.stage == 'failed') {
                  throw StateError(
                    '$label: ${cubit.state.status}/${cubit.state.error} receipts=${protocol.ordinal} media=${server.delivered.length}',
                  );
                }
                return coordinator.state.stage == 'capturePending';
              });
              expect(coordinator.allows('page2'), false);
              await acknowledge();
            }
            expect(
              find.text('Ekran kaydı tamam. Şimdi Home ile arka plana alın.'),
              findsOneWidget,
            );
            expect(protocol.captureCount, 4);
            expect(server.delivered.length, 4);
            final requests = server.requests.length;
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.inactive,
            );
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.hidden,
            );
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.paused,
            );
            await until(tester, () => coordinator.state.stage == 'background');
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.hidden,
            );
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.inactive,
            );
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.resumed,
            );
            await until(tester, () => coordinator.state.stage == 'resumed');
            expect(cubit.state.error, 'readmission_required');
            expect(
              tester
                  .widget<TextButton>(
                    find.widgetWithText(TextButton, 'Yeniden admission'),
                  )
                  .onPressed,
              isNull,
            );
            if (outcome == 'cleanupFailure') {
              await native(
                tester,
                () => writePrivate(
                  File('${protocol.secrets.path}/handoff.owner'),
                  'other-owner',
                ),
              );
            }
            await tester.tap(find.byTooltip('QA akışını kapat'));
            if (outcome == 'cleanupFailure') {
              await until(tester, () => coordinator.state.stage == 'failed');
              expect(protocol.ordinal, 9);
              expect(probe.snapshot()['credentialActive'], false);
              expect(probe.snapshot()['activeImages'], 0);
              await native(tester, () async {
                expect(
                  await File(
                    '${protocol.receipts.path}/terminal-failure.json',
                  ).exists(),
                  true,
                );
                expect(
                  await File(
                    '${protocol.receipts.path}/10-stopped.json',
                  ).exists(),
                  false,
                );
              });
              return;
            }
            await until(tester, () => coordinator.state.stage == 'stopped');
            expect(protocol.ordinal, 10);
            expect(server.requests.length, requests);
            expect(probe.snapshot()['credentialActive'], false);
            expect(probe.snapshot()['activeImages'], 0);
            await native(tester, () async {
              expect(await protocol.secrets.list().toList(), isEmpty);
              expect(
                await File(
                  '${protocol.receipts.path}/terminal-failure.json',
                ).exists(),
                false,
              );
              for (var i = 0; i < manualPhases.length; i++) {
                final file = File(
                  '${protocol.receipts.path}/${(i + 1).toString().padLeft(2, '0')}-${manualPhases[i]}.json',
                );
                expect(
                  jsonDecode(await file.readAsString())['phase'],
                  manualPhases[i],
                );
              }
            });
          } finally {
            var closeDone = false;
            final closing = coordinator.close().whenComplete(
              () => closeDone = true,
            );
            await until(tester, () => closeDone);
            await closing;
            await tester.pumpWidget(const SizedBox());
            await comparisonGetIt.reset();
            await native(tester, () async {
              await server.close();
              await directory.delete(recursive: true);
            });
            Bloc.observer = oldObserver;
            tester.binding.handleAppLifecycleStateChanged(
              AppLifecycleState.resumed,
            );
          }
        },
      );
    }
  }
}
