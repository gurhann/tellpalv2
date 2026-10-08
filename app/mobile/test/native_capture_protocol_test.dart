import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import '../integration_test/native_capture_protocol.dart';
import '../test_driver/native_capture_writer.dart';

Uint8List png(int length) {
  final bytes = Uint8List(length);
  bytes.setAll(0, [137, 80, 78, 71, 13, 10, 26, 10]);
  return bytes;
}

void main() {
  test(
    'explicit iOS envelope streams an unchanged larger payload while default device and host reject it',
    () async {
      final directory = await Directory.systemTemp.createTemp(
        'story12-ios-capture-',
      );
      addTearDown(() => directory.delete(recursive: true));
      final bytes = png(NativeCaptureProtocol.maxBytes + 19);
      await expectLater(
        NativeCaptureProtocol().publish('default-overflow', bytes),
        throwsStateError,
      );
      final protocol = NativeCaptureProtocol(
        maxCaptureBytes: NativeCaptureProtocol.iosMaxBytes,
      );
      final pending = protocol.publish('ios-large', bytes);
      var acknowledged = false;
      final completed = pending.then((receipt) {
        acknowledged = true;
        return receipt;
      });
      final manifest = Map<String, dynamic>.from(await protocol.next);
      var defaultFetches = 0;
      await expectLater(
        NativeCaptureWriter(directory).persist(manifest, (_, _) async {
          defaultFetches++;
          return '';
        }),
        throwsStateError,
      );
      expect(defaultFetches, 0);
      expect(await directory.list().toList(), isEmpty);
      final writer = NativeCaptureWriter(
        directory,
        maxCaptureBytes: NativeCaptureProtocol.iosMaxBytes,
      );
      final receipt = await writer.persist(
        manifest,
        (sequence, index) async => protocol.chunk(sequence, index),
      );
      final output = File('${directory.path}/ios-large.png');
      expect(await output.length(), bytes.length);
      expect(
        sha256.convert(await output.readAsBytes()).toString(),
        sha256.convert(bytes).toString(),
      );
      expect((await output.stat()).mode & 0x1ff, 0x180);
      expect(receipt['chunkCount'], 33);
      expect(acknowledged, false);
      await expectLater(
        protocol.publish('cannot-replace-ios', png(8)),
        throwsStateError,
      );
      expect(
        protocol.chunk(receipt['sequence'] as int, 0),
        base64Encode(bytes.sublist(0, NativeCaptureProtocol.chunkBytes)),
      );
      protocol.acknowledge(
        receipt['sequence'] as int,
        receipt['sha256'] as String,
      );
      receipt['hostAcknowledged'] = true;
      expect(writer.matchesInventory([await completed]), true);
      await writer.writeIndex();
      final index =
          jsonDecode(
                await File(
                  '${directory.path}/capture-index.json',
                ).readAsString(),
              )
              as Map<String, dynamic>;
      expect(index['transport'], contains('16MiB PNG; 256KiB chunks'));
      expect(index['captures'], [receipt]);
    },
  );
  test(
    'iOS admits the exact envelope and rejects overflow before replacing ownership or host IO',
    () async {
      final directory = await Directory.systemTemp.createTemp(
        'story12-ios-boundary-',
      );
      addTearDown(() => directory.delete(recursive: true));
      final protocol = NativeCaptureProtocol(
        maxCaptureBytes: NativeCaptureProtocol.iosMaxBytes,
      );
      final writer = NativeCaptureWriter(
        directory,
        maxCaptureBytes: NativeCaptureProtocol.iosMaxBytes,
      );
      final pending = protocol.publish(
        'ios-exact-bound',
        png(NativeCaptureProtocol.iosMaxBytes),
      );
      final manifest = Map<String, dynamic>.from(await protocol.next);
      await expectLater(
        protocol.publish(
          'ios-overflow',
          png(NativeCaptureProtocol.iosMaxBytes + 1),
        ),
        throwsStateError,
      );
      var overflowFetches = 0;
      await expectLater(
        writer.persist(
          {
            ...manifest,
            'screenshotName': 'host-overflow',
            'byteSize': NativeCaptureProtocol.iosMaxBytes + 1,
            'chunkCount': 65,
          },
          (_, _) async {
            overflowFetches++;
            return '';
          },
        ),
        throwsStateError,
      );
      expect(overflowFetches, 0);
      expect(writer.receipts, isEmpty);
      expect(await directory.list().toList(), isEmpty);
      final receipt = await writer.persist(
        manifest,
        (sequence, index) async => protocol.chunk(sequence, index),
      );
      expect(receipt['chunkCount'], 64);
      expect(
        await File('${directory.path}/ios-exact-bound.png').length(),
        NativeCaptureProtocol.iosMaxBytes,
      );
      protocol.acknowledge(
        receipt['sequence'] as int,
        receipt['sha256'] as String,
      );
      receipt['hostAcknowledged'] = true;
      expect(writer.matchesInventory([await pending]), true);
    },
  );
  test('capture endpoints reject every unsupported envelope', () {
    for (final size in [
      -1,
      0,
      NativeCaptureProtocol.maxBytes + 1,
      12 * 1024 * 1024,
      NativeCaptureProtocol.iosMaxBytes + 1,
    ]) {
      expect(
        () => NativeCaptureProtocol(maxCaptureBytes: size),
        throwsArgumentError,
      );
      expect(
        () => NativeCaptureWriter(Directory.systemTemp, maxCaptureBytes: size),
        throwsArgumentError,
      );
    }
  });
  test(
    'successive capture reset retains ordered receipts regardless of JSON map key order',
    () async {
      final directory = await Directory.systemTemp.createTemp(
        'story12-sequential-capture-',
      );
      addTearDown(() => directory.delete(recursive: true));
      final protocol = NativeCaptureProtocol(),
          writer = NativeCaptureWriter(directory);
      final device = <Map<String, Object?>>[];
      for (var capture = 0; capture < 3; capture++) {
        final pending = protocol.publish(
          'successive-$capture',
          png(16 + capture),
        );
        final manifest = await protocol.next;
        expect(manifest['sequence'], capture + 1);
        final receipt = await writer.persist(
          Map<String, dynamic>.from(manifest),
          (sequence, index) async => protocol.chunk(sequence, index),
        );
        protocol.acknowledge(
          receipt['sequence'] as int,
          receipt['sha256'] as String,
        );
        receipt['hostAcknowledged'] = true;
        device.add(await pending);
        expect(() => protocol.chunk(capture + 1, 0), throwsStateError);
      }
      expect(writer.matchesInventory(device), true);
      expect(
        writer.matchesInventory(
          device
              .map(
                (receipt) => Map.fromEntries(receipt.entries.toList().reversed),
              )
              .toList(),
        ),
        true,
      );
      expect(writer.matchesInventory(device.reversed.toList()), false);
      expect(writer.matchesInventory(device.sublist(1)), false);
      expect(
        writer.matchesInventory([
          ...device.take(2),
          {...device.last, 'hostAcknowledged': false},
        ]),
        false,
      );
    },
  );
  test(
    'one owned multi-chunk PNG is verified and persisted before capture acknowledgement',
    () async {
      final directory = await Directory.systemTemp.createTemp(
        'story12-host-capture-',
      );
      addTearDown(() => directory.delete(recursive: true));
      final protocol = NativeCaptureProtocol(),
          writer = NativeCaptureWriter(directory);
      final bytes = png(NativeCaptureProtocol.chunkBytes + 19);
      final pending = protocol.publish('detail-test-1.3', bytes);
      final manifest = await protocol.next;
      final receipt = await writer.persist(
        Map<String, dynamic>.from(manifest),
        (sequence, index) async => protocol.chunk(sequence, index),
      );
      expect(
        File('${directory.path}/detail-test-1.3.png').readAsBytesSync(),
        bytes,
      );
      expect(receipt['sha256'], sha256.convert(bytes).toString());
      protocol.acknowledge(
        receipt['sequence'] as int,
        receipt['sha256'] as String,
      );
      expect((await pending)['hostAcknowledged'], true);
      expect(
        jsonDecode(
          File('${directory.path}/capture-index.json').readAsStringSync(),
        )['captures'],
        hasLength(1),
      );
    },
  );
  test(
    'oversized captures and concurrent publication cannot replace the owned capture',
    () async {
      final protocol = NativeCaptureProtocol();
      await expectLater(
        protocol.publish(
          'overflow',
          Uint8List(NativeCaptureProtocol.maxBytes + 1),
        ),
        throwsStateError,
      );
      final pending = protocol.publish('current', png(8));
      final manifest = await protocol.next;
      await expectLater(
        protocol.publish('replacement', png(8)),
        throwsStateError,
      );
      expect(() => protocol.chunk(999, 0), throwsStateError);
      expect(
        () => protocol.chunk(manifest['sequence'] as int, 1),
        throwsStateError,
      );
      protocol.acknowledge(
        manifest['sequence'] as int,
        manifest['sha256'] as String,
      );
      await pending;
    },
  );
  test(
    'wrong or missing acknowledgement fails the capture rather than accepting evidence',
    () async {
      final protocol = NativeCaptureProtocol();
      final pending = protocol.publish('wrong-checksum', png(8));
      final manifest = await protocol.next;
      final failed = expectLater(pending, throwsStateError);
      protocol.acknowledge(manifest['sequence'] as int, 'wrong');
      await failed;
      await expectLater(
        protocol.publish(
          'missing-host',
          png(8),
          acknowledgementWait: const Duration(milliseconds: 1),
        ),
        throwsA(isA<TimeoutException>()),
      );
      expect(
        () => protocol.chunk(manifest['sequence'] as int, 0),
        throwsStateError,
      );
    },
  );
  test(
    'host rejects malformed chunks and checksum mismatch without a completed PNG receipt',
    () async {
      final directory = await Directory.systemTemp.createTemp(
        'story12-rejected-capture-',
      );
      addTearDown(() => directory.delete(recursive: true));
      final writer = NativeCaptureWriter(directory),
          protocol = NativeCaptureProtocol();
      final pending = protocol.publish('corrupted-host-transfer', png(16));
      final manifest = await protocol.next;
      final wrong = png(16)..[15] = 1;
      await expectLater(
        writer.persist(
          Map<String, dynamic>.from(manifest),
          (_, _) async => base64Encode(wrong),
        ),
        throwsStateError,
      );
      expect(writer.receipts, isEmpty);
      expect(
        File('${directory.path}/corrupted-host-transfer.png').existsSync(),
        false,
      );
      final failed = expectLater(pending, throwsStateError);
      protocol.reject(manifest['sequence'] as int);
      await failed;
    },
  );
}
