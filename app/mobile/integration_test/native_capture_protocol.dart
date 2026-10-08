import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;
import 'package:crypto/crypto.dart';

/// Native counterpart of the SDK's requestData screenshot acknowledgement seam.
/// Owns one bounded PNG until the host verifies and persists every chunk.
class NativeCaptureProtocol {
  NativeCaptureProtocol({this.maxCaptureBytes = maxBytes}) {
    if (maxCaptureBytes != maxBytes && maxCaptureBytes != iosMaxBytes) {
      throw ArgumentError.value(maxCaptureBytes, 'maxCaptureBytes');
    }
  }

  final int maxCaptureBytes;
  static const iosMaxBytes = 16 * 1024 * 1024;
  static const maxBytes = 8 * 1024 * 1024;
  static const chunkBytes = 256 * 1024;
  static final validName = RegExp(r'^[A-Za-z][A-Za-z0-9._-]{0,159}$');
  Completer<Map<String, Object?>> _available = Completer();
  _Capture? _active;
  int _sequence = 0;

  Future<Map<String, Object?>> get next => _available.future;

  Future<Map<String, Object?>> publish(
    String name,
    List<int> bytes, {
    Duration acknowledgementWait = const Duration(seconds: 60),
  }) async {
    if (!validName.hasMatch(name) || name.contains('..')) {
      throw StateError('Native capture name rejected');
    }
    if (bytes.length < 8 || bytes.length > maxCaptureBytes) {
      throw StateError('Native capture byte bound exceeded');
    }
    if (_active != null) throw StateError('Native capture already owned');
    final capture = _Capture(++_sequence, name, bytes);
    _active = capture;
    _available.complete(capture.manifest);
    try {
      final accepted = await capture.acknowledged.future.timeout(
        acknowledgementWait,
      );
      if (!accepted) throw StateError('Host rejected native capture');
      return {...capture.manifest, 'hostAcknowledged': true};
    } finally {
      _active = null;
      _available = Completer();
    }
  }

  String chunk(int sequence, int index) {
    final capture = _require(sequence);
    if (index < 0 || index >= capture.chunkCount) {
      throw StateError('Native capture chunk rejected');
    }
    final start = index * chunkBytes;
    return base64Encode(
      capture.bytes.sublist(
        start,
        math.min(start + chunkBytes, capture.bytes.length),
      ),
    );
  }

  void acknowledge(int sequence, String checksum) {
    final capture = _require(sequence);
    if (capture.acknowledged.isCompleted) {
      throw StateError('Native capture acknowledgement repeated');
    }
    capture.acknowledged.complete(checksum == capture.checksum);
  }

  void reject(int sequence) {
    final capture = _require(sequence);
    if (!capture.acknowledged.isCompleted) capture.acknowledged.complete(false);
  }

  _Capture _require(int sequence) {
    final capture = _active;
    if (capture == null || capture.sequence != sequence) {
      throw StateError('Native capture sequence rejected');
    }
    return capture;
  }
}

class _Capture {
  _Capture(this.sequence, this.name, this.bytes)
    : checksum = sha256.convert(bytes).toString();
  final int sequence;
  final String name, checksum;
  final List<int> bytes;
  final acknowledged = Completer<bool>();
  int get chunkCount =>
      (bytes.length + NativeCaptureProtocol.chunkBytes - 1) ~/
      NativeCaptureProtocol.chunkBytes;
  Map<String, Object?> get manifest => {
    'sequence': sequence,
    'screenshotName': name,
    'byteSize': bytes.length,
    'sha256': checksum,
    'chunkCount': chunkCount,
  };
}
