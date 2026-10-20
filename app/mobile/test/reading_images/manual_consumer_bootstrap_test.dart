import 'dart:io';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import '../../test_driver/manual_consumer_qa/coordinator.dart';
import '../../test_driver/manual_consumer_qa/private_protocol.dart';
import '../../test_driver/reading_consumer_manual_entry.dart';
import '../../test_driver/reading_resource_observation.dart';
import 'manual_consumer_protocol_test.dart'
    show SyntheticManualQaPlatform, temporary;

class _EarlyFailurePlatform extends SyntheticManualQaPlatform {
  const _EarlyFailurePlatform(this.failure);
  final String failure;
  @override
  bool get android => failure != 'platform';
  @override
  Future<String> processStat() async {
    if (failure == 'proc') {
      throw const FileSystemException('REPAIR_SECRET', '/untrusted-secret');
    }
    if (failure == 'stat') return 'REPAIR_SECRET';
    return super.processStat();
  }

  @override
  Future<void> permissions(String path, String mode) async {
    if (failure == 'permission') {
      throw FileSystemException('REPAIR_SECRET', path);
    }
    await super.permissions(path, mode);
  }
}

void expectSafeDiagnostics(List<String> values, Directory directory) {
  expect(values.length, lessThanOrEqualTo(16));
  final stages = ManualQaDiagnosticStage.values
      .map((value) => value.name)
      .join('|');
  final codes = ManualQaDiagnosticCode.values
      .map((value) => value.name)
      .join('|');
  for (final value in values) {
    expect(
      value,
      matches('^MANUAL_QA_DIAGNOSTIC stage=($stages) code=($codes)\$'),
    );
    expect(value, isNot(contains('REPAIR_SECRET')));
    expect(value, isNot(contains(directory.path)));
  }
}

void main() {
  testWidgets(
    'Gerçek entry bootstrap DI coordinator protocol credential beklemeden boş baseline üretir',
    (tester) async {
      final oldObserver = Bloc.observer;
      final diagnostics = <String>[];
      late Directory directory;
      ManualConsumerQaCoordinator? coordinator;
      Widget? mounted;
      var httpClients = 0;
      final before = readingOwnedResources();
      try {
        await tester.runAsync(() async {
          directory = await temporary();
          coordinator = await HttpOverrides.runZoned(
            () => bootstrapManualConsumerQa(
              resolveSupport: () async => directory,
              platform: const SyntheticManualQaPlatform(),
              mount: (widget) => mounted = widget,
              diagnostic: diagnostics.add,
            ),
            createHttpClient: (_) {
              httpClients++;
              throw StateError('Ağ işi başlangıçta yasak');
            },
          );
        });
        expect(coordinator, isNotNull);
        expect(coordinator!.state.stage, 'waiting');
        expect(identical(Bloc.observer, coordinator!.probe), true);
        await tester.pumpWidget(mounted!);
        expect(find.byType(ManualConsumerQaView), findsOneWidget);
        expect(
          tester
              .element(find.byType(ManualConsumerQaView))
              .read<ManualConsumerQaCoordinator>(),
          same(coordinator),
        );
        await tester.runAsync(() async {
          final protocol = coordinator!.protocol;
          expect(protocol.initialized, true);
          expect(protocol.consumed, false);
          final baseline = File('${protocol.receipts.path}/01-baseline.json');
          final value = jsonDecode(await baseline.readAsString()) as Map;
          final sample = value['sample'] as Map;
          expect(value['phase'], 'baseline');
          expect(sample['cubitCount'], 0);
          expect(sample['events'], isEmpty);
          for (final flag in [
            'configured',
            'credentialActive',
            'deadlineActive',
            'stopped',
            'cubitClosed',
          ]) {
            expect(sample[flag], false, reason: flag);
          }
          for (final count in [
            'activeBuffers',
            'activeDescriptors',
            'activeCodecs',
            'activeImages',
          ]) {
            expect(sample[count], 0, reason: count);
          }
          final resources = sample['resourceObservation'] as Map;
          expect(resources['bufferCreationAttempts'], '0');
          expect(resources['reservedSlots'], 0);
          expect(resources['reservedDecodedBytes'], '0');
          expect(await protocol.secrets.list().toList(), isEmpty);
          expect((await baseline.stat()).mode & 0x1ff, 0x180);
          for (final path in [
            protocol.root,
            protocol.receipts,
            protocol.secrets,
            protocol.captures,
          ]) {
            expect((await path.stat()).mode & 0x1ff, 0x1c0);
          }
        });
        expect(httpClients, 0);
        expect(readingOwnedResources(), before);
        expect(
          diagnostics,
          contains('MANUAL_QA_DIAGNOSTIC stage=baseline code=complete'),
        );
        expectSafeDiagnostics(diagnostics, directory);
      } finally {
        await tester.pumpWidget(const SizedBox());
        await tester.runAsync(() async {
          await coordinator?.close();
          await directory.delete(recursive: true);
        });
        Bloc.observer = oldObserver;
      }
    },
  );

  for (final failure in [
    'resolver',
    'platform',
    'proc',
    'stat',
    'permission',
    'symlink',
    'missing',
    'mount',
  ]) {
    testWidgets(
      'Gerçek bootstrap erken $failure hatasını sırsız gösterir ve güvensiz yola yazmaz',
      (tester) async {
        final oldObserver = Bloc.observer;
        late Directory directory;
        ManualConsumerQaCoordinator? coordinator;
        final diagnostics = <String>[];
        var httpClients = 0;
        final before = readingOwnedResources();
        try {
          await tester.runAsync(() async {
            directory = await temporary();
            final link = Link('${directory.path}/untrusted-link');
            if (failure == 'symlink') await link.create(directory.path);
            coordinator = await HttpOverrides.runZoned(
              () => bootstrapManualConsumerQa(
                resolveSupport: () async {
                  if (failure == 'resolver') {
                    throw StateError('REPAIR_SECRET ${directory.path}');
                  }
                  if (failure == 'symlink') {
                    return Directory(link.path);
                  }
                  if (failure == 'missing') {
                    return Directory('${directory.path}/absent');
                  }
                  return directory;
                },
                platform: _EarlyFailurePlatform(failure),
                mount: (_) {
                  if (failure == 'mount') throw StateError('REPAIR_SECRET');
                },
                diagnostic: diagnostics.add,
              ),
              createHttpClient: (_) {
                httpClients++;
                throw StateError('Ağ işi başlangıçta yasak');
              },
            );
            if (failure == 'resolver') {
              expect(coordinator, isNull);
            } else {
              expect(coordinator!.state.stage, 'failed');
              expect(coordinator!.protocol.initialized, false);
              expect(coordinator!.probe.snapshot()['cubitCount'], 0);
              expect(coordinator!.probe.snapshot()['events'], isEmpty);
              expect(
                diagnostics,
                contains(
                  'MANUAL_QA_DIAGNOSTIC stage=failureMarker code=unavailable',
                ),
              );
            }
            final stage = switch (failure) {
              'resolver' => 'support',
              'proc' || 'stat' => 'process',
              'symlink' || 'missing' => 'directories',
              'permission' => 'permissions',
              _ => failure,
            };
            expect(
              diagnostics,
              contains('MANUAL_QA_DIAGNOSTIC stage=$stage code=failed'),
            );
            final files = await directory
                .list(recursive: true, followLinks: false)
                .toList();
            expect(files.whereType<File>(), isEmpty);
          });
          expect(httpClients, 0);
          expect(readingOwnedResources(), before);
          expectSafeDiagnostics(diagnostics, directory);
        } finally {
          await tester.runAsync(() async {
            await coordinator?.close();
            await directory.delete(recursive: true);
          });
          Bloc.observer = oldObserver;
        }
      },
    );
  }
}
