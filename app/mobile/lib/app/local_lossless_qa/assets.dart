import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import '../../features/reading_images/domain/entity/reading_image_comparison.dart';
import '../../features/reading_images/domain/entity/reading_image_manifest.dart';
import '../../features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import '../reading_image_comparison_configuration.dart';
import 'pins.dart';

const localLosslessEvidenceSha256 =
    '16adae98244f6821cdb15f81dcca4a7640c2075cbeaa2cbecfc34786a1e78100';
const localLosslessManifestAsset = 'assets/local_lossless/manifest.json';

class LocalLosslessQaDecoded {
  const LocalLosslessQaDecoded(this.raster, this.measurement);
  final ReadingComparisonRaster raster;
  final Map<String, Object?> measurement;
}

/// Altı sabit dosya dışında giriş kabul etmez. Byte'lar cache'e gönderilmez.
@LazySingleton(env: ['localLosslessQa'])
class LocalLosslessQaAssets {
  LocalLosslessQaAssets({AssetBundle? bundle}) : bundle = bundle ?? rootBundle;
  @factoryMethod
  static LocalLosslessQaAssets create() => LocalLosslessQaAssets();
  final AssetBundle bundle;
  bool _initialized = false;

  Future<void> initialize() async {
    _initialized = false;
    final bytes = await bundle.load(localLosslessManifestAsset);
    final input = jsonDecode(
      utf8.decode(
        bytes.buffer.asUint8List(bytes.offsetInBytes, bytes.lengthInBytes),
      ),
    );
    if (input is! Map<String, dynamic> ||
        input.length != 3 ||
        input['schemaVersion'] != 1 ||
        input['evidenceSha256'] != localLosslessEvidenceSha256 ||
        !_matchesCases(input['cases'])) {
      throw const FormatException('fixture_manifest_refused');
    }
    _initialized = true;
  }

  static bool _matchesCases(Object? input) {
    if (input is! List || input.length != localLosslessCases.length) {
      return false;
    }
    for (var i = 0; i < input.length; i++) {
      final item = input[i], expected = localLosslessCases[i];
      if (item is! Map ||
          item.length != expected.length ||
          expected.entries.any((entry) => item[entry.key] != entry.value)) {
        return false;
      }
    }
    return true;
  }

  Future<LocalLosslessQaDecoded> load(
    int index,
    ReadingComparisonCancellation cancellation,
  ) async {
    if (!_initialized || index < 0 || index >= localLosslessCases.length) {
      throw const FormatException('fixture_refused');
    }
    final fixture = localLosslessCases[index];
    cancellation.check();
    var watch = Stopwatch()..start();
    final data = await bundle.load(fixture['asset'] as String);
    final bytes = data.buffer.asUint8List(
      data.offsetInBytes,
      data.lengthInBytes,
    );
    final readMicros = watch.elapsedMicroseconds;
    cancellation.check();
    watch = Stopwatch()..start();
    final hash = sha256.convert(bytes).toString();
    final format = _format(bytes);
    if (bytes.length != fixture['bytes'] ||
        hash != fixture['sha256'] ||
        format != fixture['format']) {
      throw const FormatException('fixture_bytes_refused');
    }
    final checksumMicros = watch.elapsedMicroseconds;
    cancellation.check();
    ReadingComparisonRaster? raster;
    try {
      watch = Stopwatch()..start();
      raster = await ReadingComparisonRaster.decode(
        bytes,
        ReadingImageRaster(
          provider: 'local',
          objectKey: fixture['asset'] as String,
          generation: 'fixed',
          mime: format == 'PNG' ? 'image/png' : 'image/webp',
          byteSize: bytes.length,
          sha256: hash,
          width: 822,
          height: 1334,
        ),
        822,
        1334,
        _limits,
        cancellation,
      );
      final decodeMicros = watch.elapsedMicroseconds;
      cancellation.check();
      watch = Stopwatch()..start();
      final rgba = await raster.image!.toByteData(
        format: ui.ImageByteFormat.rawRgba,
      );
      cancellation.check();
      if (rgba == null || rgba.lengthInBytes != 4386192) {
        throw const FormatException('rgba_refused');
      }
      final pixels = rgba.buffer.asUint8List(
        rgba.offsetInBytes,
        rgba.lengthInBytes,
      );
      final rgb = Uint8List(3289644);
      for (int source = 0, target = 0; source < pixels.length; source += 4) {
        if (pixels[source + 3] != 255) {
          throw const FormatException('alpha_refused');
        }
        rgb[target++] = pixels[source];
        rgb[target++] = pixels[source + 1];
        rgb[target++] = pixels[source + 2];
      }
      final rgbHash = sha256.convert(rgb).toString();
      if (rgbHash != fixture['rgbSha256']) {
        throw const FormatException('rgb_pin_refused');
      }
      final result = LocalLosslessQaDecoded(raster, {
        'caseIndex': index,
        'caseId': fixture['caseId'],
        'assetId': fixture['assetId'],
        'asset': fixture['asset'],
        'format': format,
        'encodedBytes': bytes.length,
        'encodedSha256': hash,
        'decodedWidth': raster.image!.width,
        'decodedHeight': raster.image!.height,
        'rgbBytes': rgb.length,
        'rgbSha256': rgbHash,
        'rgbMatchesPin': true,
        'assetReadMicros': readMicros,
        'checksumMicros': checksumMicros,
        'decodeMicros': decodeMicros,
        'rgbReadbackMicros': watch.elapsedMicroseconds,
        'theoreticalRgbaBytes': 4386192,
        'httpTransferMeasured': false,
      });
      raster = null;
      return result;
    } finally {
      raster?.dispose();
    }
  }

  static String _format(Uint8List b) {
    if (b.length >= 8 && b.take(8).join(',') == '137,80,78,71,13,10,26,10') {
      return 'PNG';
    }
    if (b.length >= 21 &&
        ascii.decode(b.sublist(0, 4), allowInvalid: true) == 'RIFF' &&
        ascii.decode(b.sublist(8, 16), allowInvalid: true) == 'WEBPVP8L' &&
        b[20] == 0x2f &&
        ByteData.sublistView(b).getUint32(4, Endian.little) == b.length - 8) {
      return 'WEBP';
    }
    throw const FormatException('format_refused');
  }

  static final _limits = ReadingComparisonLimits(
    runId: 'local-lossless-fixed-six',
    maxEncodedBytes: 1765704,
    maxBufferBytes: 5297112,
    maxDecodePixels: 1096548,
    maxDecodedBytes: 4386192,
    maxPeakBytes: 28000000,
    maxOwnedImages: 1,
    transferMillis: 10000,
    decodeMillis: 10000,
    statusEveryMillis: 10000,
    maxStatusChecks: 1,
    maxControlMillis: 180000,
  );
}
