import 'dart:typed_data';
import 'reading_image_manifest.dart';

class ReadingImageComparison {
  const ReadingImageComparison(
    this.manifest,
    this.context,
    this.liveGeneration,
    this.leaseMillis,
  );
  final ReadingImageManifest manifest;
  final ReadingImageManifestContext context;
  final int liveGeneration, leaseMillis;
}

/// İptal yeni kapasite açmaz; çağıran eski Future'ın gerçekten bitmesini bekler.
class ReadingComparisonCancellation {
  bool cancelled = false;
  void Function()? onCancel;
  void cancel() {
    cancelled = true;
    onCancel?.call();
  }

  void check() {
    if (cancelled) throw StateError('cancelled');
  }
}

class ReadingComparisonFailure {
  const ReadingComparisonFailure(this.code);
  final String code;
}

typedef ReadingComparisonBytes = Uint8List;
