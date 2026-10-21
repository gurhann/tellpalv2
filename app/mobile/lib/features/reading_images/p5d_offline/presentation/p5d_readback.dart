import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import '../../domain/entity/reading_image_comparison.dart';

/// Her readback yazıldıktan sonra bırakılır; iki tam byte tamponu tutulmaz.
Future<Map<String, Object?>> p5dReadback(
  ui.Image image,
  String directory,
  String name,
  ReadingComparisonCancellation cancellation, {
  bool persist = true,
}) async {
  final result = <String, Object?>{'colorSpace': image.colorSpace.name};
  for (final kind in [
    ui.ImageByteFormat.rawRgba,
    ui.ImageByteFormat.rawExtendedRgba128,
  ]) {
    final clock = Stopwatch()..start();
    final dynamicData = await _supportedReadback(image, kind);
    cancellation.check();
    final readbackMicros = clock.elapsedMicroseconds;
    if (dynamicData == null && kind == ui.ImageByteFormat.rawExtendedRgba128) {
      result[kind.name] = {'status': 'UNSUPPORTED'};
      continue;
    }
    final data = dynamicData;
    if (data == null ||
        data.lengthInBytes !=
            image.width *
                image.height *
                (kind == ui.ImageByteFormat.rawRgba ? 4 : 16)) {
      throw StateError('readback_refused');
    }
    String? hash;
    int? hashMicros, writeMicros;
    final filename = '$name.${kind.name}.raw';
    if (persist) {
      final bytes = data.buffer.asUint8List(
        data.offsetInBytes,
        data.lengthInBytes,
      );
      final hashClock = Stopwatch()..start();
      hash = sha256.convert(bytes).toString();
      hashMicros = hashClock.elapsedMicroseconds;
      final writeClock = Stopwatch()..start();
      await File('$directory/$filename').writeAsBytes(bytes, flush: true);
      writeMicros = writeClock.elapsedMicroseconds;
    }
    cancellation.check();
    result[kind.name] = {
      'status': 'MEASURED',
      'file': persist ? filename : null,
      'bytes': data.lengthInBytes,
      'sha256': hash,
      'readbackMicros': readbackMicros,
      'checksumStatus': persist ? 'MEASURED' : 'NOT_MEASURED',
      'checksumMicros': hashMicros,
      'writeStatus': persist ? 'MEASURED' : 'NOT_RUN',
      'writeMicros': writeMicros,
    };
  }
  return result;
}

Future<ByteData?> _supportedReadback(
  ui.Image image,
  ui.ImageByteFormat format,
) async {
  try {
    return await image.toByteData(format: format);
  } on UnsupportedError {
    if (format == ui.ImageByteFormat.rawExtendedRgba128) return null;
    rethrow;
  }
}
