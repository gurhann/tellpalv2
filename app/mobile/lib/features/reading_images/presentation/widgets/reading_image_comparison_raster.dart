import 'dart:async';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/widgets.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';

/// Sahibi açık tek native görüntü. Global/disk ImageCache kullanılmaz.
class ReadingComparisonRaster {
  ReadingComparisonRaster._(this.image, this._reservation);
  static int activeBuffers = 0,
      activeDescriptors = 0,
      activeCodecs = 0,
      activeImages = 0;
  static _RasterReservation? _owner;
  static int get reservedSlots => _owner == null ? 0 : 1;
  static BigInt get reservedDecodedBytes => _owner?.bytes ?? BigInt.zero;
  final _RasterReservation _reservation;
  static int _bufferCreationAttempts = 0;
  static int get bufferCreationAttempts => _bufferCreationAttempts;
  ui.Image? image;
  void dispose() {
    if (image != null) {
      image!.dispose();
      image = null;
      activeImages--;
      _reservation.release();
    }
  }

  static Future<ReadingComparisonRaster> decode(
    Uint8List bytes,
    ReadingImageRaster metadata,
    int width,
    int height,
    ReadingComparisonLimits limits,
    ReadingComparisonCancellation cancellation,
  ) async {
    _RasterReservation? reservation;
    ui.ImmutableBuffer? buffer;
    ui.ImageDescriptor? descriptor;
    ui.Codec? codec;
    ui.Image? image;
    final watch = Stopwatch()..start();
    try {
      cancellation.check();
      if (bytes.length > limits.maxEncodedBytes ||
          BigInt.from(bytes.length) * BigInt.from(3) >
              BigInt.from(limits.maxBufferBytes)) {
        throw StateError('buffer_limit');
      }
      final outputBytes =
          BigInt.from(width) * BigInt.from(height) * BigInt.from(4);
      final declaredPixels =
          BigInt.from(metadata.width) * BigInt.from(metadata.height);
      if (width <= 0 ||
          height <= 0 ||
          width > metadata.width ||
          height > metadata.height ||
          declaredPixels > BigInt.from(limits.maxDecodePixels) ||
          outputBytes > BigInt.from(limits.maxDecodedBytes) ||
          BigInt.from(bytes.length) * BigInt.from(3) +
                  declaredPixels * BigInt.from(16) +
                  outputBytes >
              BigInt.from(limits.maxPeakBytes)) {
        throw StateError('decode_resource_or_metadata');
      }
      if (_owner != null) throw StateError('owned_image_limit');
      reservation = _RasterReservation(outputBytes);
      _owner = reservation;
      _bufferCreationAttempts++;
      buffer = await ui.ImmutableBuffer.fromUint8List(bytes);
      activeBuffers++;
      cancellation.check();
      descriptor = await ui.ImageDescriptor.encoded(buffer);
      activeDescriptors++;
      cancellation.check();
      final actualPixels =
          BigInt.from(descriptor.width) * BigInt.from(descriptor.height);
      final outputPixels = BigInt.from(width) * BigInt.from(height);
      if (descriptor.width != metadata.width ||
          descriptor.height != metadata.height ||
          actualPixels > BigInt.from(limits.maxDecodePixels) ||
          width <= 0 ||
          height <= 0 ||
          width > descriptor.width ||
          height > descriptor.height ||
          outputPixels * BigInt.from(4) > BigInt.from(limits.maxDecodedBytes) ||
          BigInt.from(bytes.length) * BigInt.from(3) +
                  actualPixels * BigInt.from(16) +
                  outputPixels * BigInt.from(4) >
              BigInt.from(limits.maxPeakBytes)) {
        throw StateError('decode_resource_or_metadata');
      }
      codec = await descriptor.instantiateCodec(
        targetWidth: width,
        targetHeight: height,
      );
      activeCodecs++;
      cancellation.check();
      if (codec.frameCount != 1) throw StateError('single_frame_required');
      image = (await codec.getNextFrame()).image;
      activeImages++;
      if (activeImages > limits.maxOwnedImages) {
        throw StateError('owned_image_limit');
      }
      cancellation.check();
      if (watch.elapsedMilliseconds > limits.decodeMillis ||
          image.width != width ||
          image.height != height) {
        throw StateError('decode_deadline_or_dimensions');
      }
      final owned = ReadingComparisonRaster._(image, reservation);
      reservation = null;
      image = null;
      return owned;
    } finally {
      if (image != null) {
        image.dispose();
        activeImages--;
      }
      if (codec != null) {
        codec.dispose();
        activeCodecs--;
      }
      if (descriptor != null) {
        descriptor.dispose();
        activeDescriptors--;
      }
      if (buffer != null) {
        buffer.dispose();
        activeBuffers--;
      }
      reservation?.release();
    }
  }
}

/// Token yalnız kendi rezervasyonunu bırakır; eski dispose yeni sahibine dokunamaz.
class _RasterReservation {
  _RasterReservation(this.bytes);
  final BigInt bytes;
  bool _released = false;
  void release() {
    if (_released) return;
    _released = true;
    if (identical(ReadingComparisonRaster._owner, this)) {
      ReadingComparisonRaster._owner = null;
    }
  }
}

class ReadingImageComparisonRaster extends StatelessWidget {
  const ReadingImageComparisonRaster({
    super.key,
    required this.raster,
    required this.fit,
  });
  final ReadingComparisonRaster raster;
  final BoxFit fit;
  @override
  Widget build(BuildContext context) => CustomPaint(
    painter: _BorrowedReadingPainter(raster, fit),
    size: Size.infinite,
  );
}

/// RenderImage'ın ui.Image clone sahipliğini oluşturmaz. Dispose sonrası eski painter boş çizer.
class _BorrowedReadingPainter extends CustomPainter {
  const _BorrowedReadingPainter(this.raster, this.fit);
  final ReadingComparisonRaster raster;
  final BoxFit fit;
  @override
  void paint(Canvas canvas, Size size) {
    final image = raster.image;
    if (image == null) return;
    paintImage(
      canvas: canvas,
      rect: Offset.zero & size,
      image: image,
      fit: fit,
      alignment: Alignment.center,
      filterQuality: FilterQuality.low,
    );
  }

  @override
  bool shouldRepaint(covariant _BorrowedReadingPainter old) =>
      old.raster != raster || old.fit != fit;
}
