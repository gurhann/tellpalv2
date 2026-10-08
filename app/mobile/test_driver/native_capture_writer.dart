import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'package:crypto/crypto.dart';
import '../integration_test/native_capture_protocol.dart';
import 'native_runtime_guard.dart';

/// Persists and verifies one native PNG through bounded chunks before acknowledging it.
class NativeCaptureWriter {
  NativeCaptureWriter(
    this.directory, {
    this.maxCaptureBytes = NativeCaptureProtocol.maxBytes,
  }) {
    if (maxCaptureBytes != NativeCaptureProtocol.maxBytes &&
        maxCaptureBytes != NativeCaptureProtocol.iosMaxBytes) {
      throw ArgumentError.value(maxCaptureBytes, 'maxCaptureBytes');
    }
  }
  final int maxCaptureBytes;
  final Directory directory;
  final receipts = <Map<String, Object?>>[];

  bool matchesInventory(List<dynamic> received) {
    if (received.length != receipts.length) return false;
    for (var index = 0; index < received.length; index++) {
      final actual = received[index], expected = receipts[index];
      if (actual is! Map ||
          actual.length != expected.length ||
          expected.keys.any((key) => actual[key] != expected[key])) {
        return false;
      }
    }
    return true;
  }

  Future<Map<String, Object?>> persist(
    Map<String, dynamic> manifest,
    Future<String> Function(int sequence, int index) fetch,
  ) async {
    final sequence = manifest['sequence'],
        name = manifest['screenshotName'],
        size = manifest['byteSize'],
        count = manifest['chunkCount'],
        checksum = manifest['sha256'];
    if (sequence is! int ||
        sequence <= 0 ||
        name is! String ||
        !NativeCaptureProtocol.validName.hasMatch(name) ||
        name.contains('..') ||
        size is! int ||
        size < 8 ||
        size > maxCaptureBytes ||
        count is! int ||
        count !=
            (size + NativeCaptureProtocol.chunkBytes - 1) ~/
                NativeCaptureProtocol.chunkBytes ||
        checksum is! String ||
        !RegExp(r'^[a-f0-9]{64}$').hasMatch(checksum) ||
        receipts.any((receipt) => receipt['screenshotName'] == name)) {
      throw StateError('Native capture manifest rejected');
    }
    await preparePrivateOutput(directory).timeout(const Duration(seconds: 10));
    final partial = File('${directory.path}/$name.png.part');
    if (await partial.exists() ||
        await File('${directory.path}/$name.png').exists()) {
      throw StateError('Native capture destination already exists');
    }
    await preparePrivateFile(partial);
    final file = await partial
        .open(mode: FileMode.write)
        .timeout(const Duration(seconds: 10));
    final digest = _DigestSink();
    final hash = sha256.startChunkedConversion(digest);
    final elapsed = Stopwatch()..start();
    var closed = false;
    try {
      for (var index = 0; index < count; index++) {
        if (elapsed.elapsed > const Duration(seconds: 60)) {
          throw StateError('Native capture transfer deadline reached');
        }
        final encoded = await fetch(
          sequence,
          index,
        ).timeout(const Duration(seconds: 30));
        if (encoded.length >
            ((NativeCaptureProtocol.chunkBytes + 2) ~/ 3) * 4) {
          throw StateError('Native capture encoded chunk exceeded bound');
        }
        final bytes = base64Decode(encoded);
        if (bytes.length !=
            math.min(
              NativeCaptureProtocol.chunkBytes,
              size - index * NativeCaptureProtocol.chunkBytes,
            )) {
          throw StateError('Native capture chunk length differs');
        }
        if (index == 0 && !_png(bytes)) {
          throw StateError('Native capture is not PNG');
        }
        hash.add(bytes);
        await file.writeFrom(bytes).timeout(const Duration(seconds: 10));
      }
      hash.close();
      if (digest.value.toString() != checksum) {
        throw StateError('Native capture checksum differs');
      }
      await file.flush().timeout(const Duration(seconds: 10));
      await file.close().timeout(const Duration(seconds: 10));
      closed = true;
      await partial
          .rename('${directory.path}/$name.png')
          .timeout(const Duration(seconds: 10));
      final receipt = <String, Object?>{
        'sequence': sequence,
        'screenshotName': name,
        'byteSize': size,
        'sha256': checksum,
        'chunkCount': count,
      };
      receipts.add(receipt);
      await writeIndex();
      return receipt;
    } finally {
      if (!closed) await file.close().timeout(const Duration(seconds: 10));
    }
  }

  Future<void> writeIndex() => writePrivateJson(
    File('${directory.path}/capture-index.json'),
    {
      'transport':
          'one-owned-native-capture; ${maxCaptureBytes ~/ (1024 * 1024)}MiB PNG; '
          '256KiB chunks; SHA256 verified before acknowledgement',
      'captures': receipts,
    },
  );

  static bool _png(List<int> bytes) {
    const signature = [137, 80, 78, 71, 13, 10, 26, 10];
    for (var index = 0; index < signature.length; index++) {
      if (bytes[index] != signature[index]) return false;
    }
    return true;
  }
}

class _DigestSink implements Sink<Digest> {
  Digest? value;
  @override
  void add(Digest data) => value = data;
  @override
  void close() {}
}
