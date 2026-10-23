import 'dart:convert';
import 'dart:io';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import '../../test_driver/manual_consumer_qa/coordinator.dart';
import '../../test_driver/manual_consumer_qa/private_protocol.dart';
import '../../test_driver/reading_consumer_manual_entry.dart';
import '../../test_driver/reading_consumer_qa_probe.dart';
import '../../test_driver/reading_resource_observation.dart';

class _StartupPlatform extends ManualQaPlatform {
  var permissionCalls = 0;
  @override
  bool get android => true;
  @override
  int get processId => 123;
  @override
  Future<String> processStat() async =>
      '123 (synthetic) ${List.generate(20, (i) => i == 19 ? '456' : '0').join(' ')}';
  @override
  Future<void> permissions(String path, String mode) async {
    permissionCalls++;
    await super.permissions(path, mode);
  }
}

/// Yalnız resolve hata kolunu sağlar; tür kontrolü gerçek dosya sistemindedir.
class _ResolveFailureDirectory implements Directory {
  _ResolveFailureDirectory(this.path);
  @override
  final String path;
  @override
  Directory get absolute => Directory(path).absolute;
  @override
  Future<String> resolveSymbolicLinks() async =>
      throw FileSystemException('STARTUP_PRIVATE_SECRET', path);
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void _safeDiagnostics(List<String> diagnostics, String path) {
  final stages = ManualQaDiagnosticStage.values.map((e) => e.name).join('|');
  final codes = ManualQaDiagnosticCode.values.map((e) => e.name).join('|');
  expect(diagnostics.length, lessThanOrEqualTo(16));
  for (final diagnostic in diagnostics) {
    expect(
      diagnostic,
      matches('^MANUAL_QA_DIAGNOSTIC stage=($stages) code=($codes)\$'),
    );
    expect(diagnostic, isNot(contains('STARTUP_PRIVATE_SECRET')));
    expect(diagnostic, isNot(contains(path)));
  }
}

Future<Map<String, String>> _tree(Directory directory) async {
  final result = <String, String>{};
  await for (final entity in directory.list(
    recursive: true,
    followLinks: false,
  )) {
    result[entity.path] = switch (entity) {
      File() => 'file:${await entity.readAsString()}',
      Link() => 'link:${await entity.target()}',
      _ => 'directory',
    };
  }
  return result;
}

void main() {
  for (final selected in [
    ConsumerQaExpectedScope.p4b5Manual,
    ConsumerQaExpectedScope.p4b5ManualRetry,
  ]) {
    testWidgets(
      '${selected.measurementId} ham 0771 support yolu gerçek bootstrap ile baseline üretir',
      (tester) async {
        final oldObserver = Bloc.observer;
        final diagnostics = <String>[];
        final platform = _StartupPlatform();
        final before = readingOwnedResources();
        Directory? directory;
        ManualConsumerQaCoordinator? coordinator;
        var httpClients = 0;
        try {
          await tester.runAsync(() async {
            // Yalnız geçici üst dizin canonical yapılır; yeni child ham kalır.
            directory = await Directory(
              await Directory.systemTemp.resolveSymbolicLinks(),
            ).createTemp('manual-startup-');
            final chmod = await Process.run('chmod', ['771', directory!.path]);
            expect(chmod.exitCode, 0);
            coordinator = await HttpOverrides.runZoned(
              () => bootstrapManualConsumerQa(
                scope: selected,
                resolveSupport: () async => directory!,
                platform: platform,
                mount: (_) {},
                diagnostic: diagnostics.add,
              ),
              createHttpClient: (_) {
                httpClients++;
                throw StateError('Başlangıçta ağ işi yasak');
              },
            );
            final protocol = coordinator!.protocol;
            expect(coordinator!.state.stage, 'waiting');
            expect(protocol.support.path, directory!.path);
            expect(protocol.initialized, true);
            expect(protocol.consumed, false);
            expect(platform.permissionCalls, 5);
            expect((await directory!.stat()).mode & 0x1ff, 0x1f9);
            final baseline = File('${protocol.receipts.path}/01-baseline.json');
            final value = jsonDecode(await baseline.readAsString()) as Map;
            expect(value['measurementId'], selected.measurementId);
            expect(value['phase'], 'baseline');
            expect(value['sample']['configured'], false);
            expect(value['sample']['events'], isEmpty);
            expect((await baseline.stat()).mode & 0x1ff, 0x180);
            expect(await protocol.secrets.list().toList(), isEmpty);
            expect(
              diagnostics,
              contains('MANUAL_QA_DIAGNOSTIC stage=baseline code=complete'),
            );
          });
          expect(httpClients, 0);
          expect(readingOwnedResources(), before);
          _safeDiagnostics(diagnostics, directory!.path);
        } finally {
          await tester.runAsync(() async {
            await coordinator?.close();
            await directory?.delete(recursive: true);
          });
          Bloc.observer = oldObserver;
        }
      },
    );

    for (final entry in {
      'missing': ManualQaDiagnosticCode.supportTypeMismatch,
      'file': ManualQaDiagnosticCode.supportTypeMismatch,
      'finalLink': ManualQaDiagnosticCode.supportTypeMismatch,
      'intermediateLink': ManualQaDiagnosticCode.supportCanonicalMismatch,
      'canonicalMismatch': ManualQaDiagnosticCode.supportCanonicalMismatch,
      'typeReadError': ManualQaDiagnosticCode.supportTypeReadFailed,
      'resolveError': ManualQaDiagnosticCode.supportResolveFailed,
    }.entries) {
      testWidgets(
        '${selected.measurementId} ${entry.key} sabit tanı ile yazmadan terminal reddedilir',
        (tester) async {
          final oldObserver = Bloc.observer;
          final beforeResources = readingOwnedResources();
          final diagnostics = <String>[];
          final platform = _StartupPlatform();
          Directory? directory;
          ManualConsumerQaCoordinator? coordinator;
          var httpClients = 0;
          try {
            await tester.runAsync(() async {
              directory = await Directory(
                await Directory.systemTemp.resolveSymbolicLinks(),
              ).createTemp('manual-startup-');
              final actual = await Directory(
                '${directory!.path}/support',
              ).create();
              Directory supplied = actual;
              switch (entry.key) {
                case 'missing':
                  supplied = Directory('${directory!.path}/missing');
                case 'file':
                  final file = File('${directory!.path}/wrong-type');
                  await file.writeAsString('STARTUP_PRIVATE_SECRET');
                  supplied = Directory(file.path);
                case 'finalLink':
                  final link = Link('${directory!.path}/final-link');
                  await link.create(actual.path);
                  supplied = Directory(link.path);
                case 'intermediateLink':
                  final link = Link('${directory!.path}/parent-link');
                  await link.create(directory!.path);
                  supplied = Directory('${link.path}/support');
                case 'canonicalMismatch':
                  supplied = Directory('${actual.path}/../support');
                case 'resolveError':
                  supplied = _ResolveFailureDirectory(actual.path);
              }
              final beforeTree = await _tree(directory!);
              final parentMode = (await directory!.stat()).mode;
              final supportMode = (await actual.stat()).mode;
              Future<ManualConsumerQaCoordinator?> start() =>
                  HttpOverrides.runZoned(
                    () => bootstrapManualConsumerQa(
                      scope: selected,
                      resolveSupport: () async => supplied,
                      platform: platform,
                      mount: (_) {},
                      diagnostic: diagnostics.add,
                    ),
                    createHttpClient: (_) {
                      httpClients++;
                      throw StateError('Başlangıçta ağ işi yasak');
                    },
                  );
              coordinator = entry.key == 'typeReadError'
                  ? await IOOverrides.runZoned(
                      start,
                      fseGetType: (path, followLinks) async {
                        expect(path, supplied.path);
                        expect(followLinks, false);
                        throw FileSystemException(
                          'STARTUP_PRIVATE_SECRET',
                          path,
                        );
                      },
                    )
                  : await start();
              final protocol = coordinator!.protocol;
              expect(coordinator!.state.stage, 'failed');
              expect(protocol.initialized, false);
              expect(protocol.consumed, false);
              expect(protocol.ordinal, 0);
              expect(protocol.failureWritten, false);
              expect(platform.permissionCalls, 0);
              expect(coordinator!.probe.snapshot()['configured'], false);
              expect(coordinator!.probe.snapshot()['cubitCount'], 0);
              expect(coordinator!.probe.snapshot()['events'], isEmpty);
              expect(await _tree(directory!), beforeTree);
              expect((await directory!.stat()).mode, parentMode);
              expect((await actual.stat()).mode, supportMode);
              expect(
                diagnostics,
                containsAllInOrder([
                  'MANUAL_QA_DIAGNOSTIC stage=directories code=${entry.value.name}',
                  'MANUAL_QA_DIAGNOSTIC stage=directories code=failed',
                  'MANUAL_QA_DIAGNOSTIC stage=failureMarker code=unavailable',
                ]),
              );
              expect(
                diagnostics.any((value) => value.contains('stage=baseline')),
                false,
              );
            });
            expect(httpClients, 0);
            expect(readingOwnedResources(), beforeResources);
            _safeDiagnostics(diagnostics, directory!.path);
          } finally {
            await tester.runAsync(() async {
              await coordinator?.close();
              await directory?.delete(recursive: true);
            });
            Bloc.observer = oldObserver;
          }
        },
      );
    }
  }
}
