import 'dart:convert';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import '../../test_driver/manual_consumer_qa/private_protocol.dart';
import 'reading_p4b5_qa_probe_test.dart' show p4b5Config;

class SyntheticManualQaPlatform extends ManualQaPlatform {
  const SyntheticManualQaPlatform();
  @override
  bool get android => true;
  @override
  int get processId => 123;
  @override
  Future<String> processStat() async =>
      '123 (synthetic) ${List.generate(20, (i) => i == 19 ? '456' : '0').join(' ')}';
}

Future<Directory> temporary() async {
  final directory = await Directory.systemTemp.createTemp('manual-protocol-');
  return Directory(await directory.resolveSymbolicLinks());
}

Future<void> writePrivate(File file, Object value) async {
  final part = File('${file.path}.part');
  await part.writeAsString(
    value is String ? value : jsonEncode(value),
    flush: true,
  );
  await const SyntheticManualQaPlatform().permissions(part.path, '600');
  await part.rename(file.path);
}

Future<void> configure(
  ManualQaPrivateProtocol protocol, {
  String? field,
  Object? value,
}) async {
  await writePrivate(
    File('${protocol.secrets.path}/handoff.owner'),
    protocol.nonce,
  );
  final input = {
    ...protocol.identity(),
    'configure': p4b5Config(12345, manualScope),
  };
  if (field != null) input[field] = value;
  await writePrivate(File('${protocol.secrets.path}/configure.json'), input);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'Gerçek protokol 0771 filesDir iznini korur ve tek kayıt tüketir',
    () async {
      final directory = await temporary();
      try {
        await const SyntheticManualQaPlatform().permissions(
          directory.path,
          '771',
        );
        final protocol = ManualQaPrivateProtocol(
          directory,
          platform: const SyntheticManualQaPlatform(),
        );
        await protocol.initialize();
        expect((await directory.stat()).mode & 0x1ff, 0x1f9);
        await protocol.receipt('baseline', {'synthetic': true});
        final baseline = File('${protocol.receipts.path}/01-baseline.json');
        expect((await baseline.stat()).mode & 0x1ff, 0x180);
        expect(
          jsonDecode(await baseline.readAsString())['nonce'],
          protocol.nonce,
        );
        await configure(protocol);
        expect(
          (await protocol.consume())!['measurementId'],
          manualScope.measurementId,
        );
        await expectLater(protocol.consume(), throwsFormatException);
        await protocol.cleanup();
        expect(await protocol.secrets.list().toList(), isEmpty);
        expect((await directory.stat()).mode & 0x1ff, 0x1f9);
      } finally {
        await directory.delete(recursive: true);
      }
    },
  );
  test(
    'nonce PID startTicks lifetime scope ve proposal configure öncesi reddedilir',
    () async {
      for (final entry in {
        'nonce': 'b' * 64,
        'pid': 999,
        'processStartTicks': '999',
        'lifetimeId': 'b' * 32,
        'measurementId': 'old',
        'proposalSha256': 'b' * 64,
        'budgetRunId': 'old',
        'supportDirectory': '/elsewhere',
      }.entries) {
        final directory = await temporary();
        try {
          final protocol = ManualQaPrivateProtocol(
            directory,
            platform: const SyntheticManualQaPlatform(),
          );
          await protocol.initialize();
          await configure(protocol, field: entry.key, value: entry.value);
          await expectLater(
            protocol.consume(),
            throwsFormatException,
            reason: entry.key,
          );
          await protocol.cleanup();
          expect(await protocol.secrets.list().toList(), isEmpty);
        } finally {
          await directory.delete(recursive: true);
        }
      }
    },
  );
  test(
    'Yanlış QA izni symlink ve eski dizin yeniden kullanımı reddedilir',
    () async {
      for (final mutation in ['permission', 'symlink', 'existing']) {
        final directory = await temporary();
        try {
          final protocol = ManualQaPrivateProtocol(
            directory,
            platform: const SyntheticManualQaPlatform(),
          );
          await protocol.initialize();
          if (mutation == 'permission') {
            await const SyntheticManualQaPlatform().permissions(
              protocol.secrets.path,
              '755',
            );
            await expectLater(protocol.consume(), throwsFormatException);
          } else if (mutation == 'symlink') {
            await protocol.secrets.delete();
            await Link(protocol.secrets.path).create(directory.path);
            await expectLater(protocol.consume(), throwsFormatException);
          } else {
            await expectLater(
              ManualQaPrivateProtocol(
                directory,
                platform: const SyntheticManualQaPlatform(),
              ).initialize(),
              throwsFormatException,
            );
          }
        } finally {
          await directory.delete(recursive: true);
        }
      }
    },
  );
  test(
    'Paint ACK tam kimliği epoch receipt ve PNG hashini bağlar; değiştirme kesilir',
    () async {
      for (final mutation in [
        'valid',
        'phase',
        'epoch',
        'receiptSha256',
        'nonce',
        'pid',
      ]) {
        final directory = await temporary();
        try {
          final protocol = ManualQaPrivateProtocol(
            directory,
            platform: const SyntheticManualQaPlatform(),
          );
          await protocol.initialize();
          await protocol.receipt('baseline', {});
          await protocol.receipt('paint1', {'paintedEpoch': 4});
          final receiptSha = protocol.previousSha;
          expect(await protocol.capture(1, 4, receiptSha), false);
          final ack = {
            ...protocol.identity(),
            'phase': 'paint1',
            'epoch': 4,
            'receiptSha256': receiptSha,
            'pngSha256': 'a' * 64,
          };
          if (mutation != 'valid') {
            ack[mutation] = mutation == 'epoch' || mutation == 'pid'
                ? 999
                : 'wrong';
          }
          final file = File('${protocol.captures.path}/capture-01.json');
          await writePrivate(file, ack);
          if (mutation == 'valid') {
            expect(await protocol.capture(1, 4, receiptSha), true);
            await protocol.validateCaptures();
            await expectLater(
              protocol.capture(1, 4, receiptSha),
              throwsFormatException,
            );
            ack['pngSha256'] = sha256.convert([1]).toString();
            await writePrivate(file, ack);
            await expectLater(
              protocol.validateCaptures(),
              throwsFormatException,
            );
          } else {
            await expectLater(
              protocol.capture(1, 4, receiptSha),
              throwsFormatException,
              reason: mutation,
            );
          }
        } finally {
          await directory.delete(recursive: true);
        }
      }
    },
  );
  test('Sıra dışı ACK ve nonce sahibi olmayan cleanup reddedilir', () async {
    final directory = await temporary();
    try {
      final protocol = ManualQaPrivateProtocol(
        directory,
        platform: const SyntheticManualQaPlatform(),
      );
      await protocol.initialize();
      await writePrivate(File('${protocol.captures.path}/capture-02.json'), {});
      await expectLater(
        protocol.capture(1, 4, 'a' * 64),
        throwsFormatException,
      );
      await configure(protocol);
      await writePrivate(
        File('${protocol.secrets.path}/handoff.owner'),
        'other',
      );
      await expectLater(protocol.cleanup(), throwsFormatException);
      expect(
        await File('${protocol.secrets.path}/configure.json').exists(),
        true,
      );
      await protocol.failure();
      expect(
        jsonDecode(
          await File(
            '${protocol.receipts.path}/terminal-failure.json',
          ).readAsString(),
        )['status'],
        'FAILED_NO_RETRY',
      );
      expect(
        await File('${protocol.receipts.path}/10-stopped.json').exists(),
        false,
      );
    } finally {
      await directory.delete(recursive: true);
    }
  });
}
