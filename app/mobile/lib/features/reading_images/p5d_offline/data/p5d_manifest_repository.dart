import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import '../domain/p5d_pins.dart';
import '../domain/p5d_repository.dart';
import 'p5d_icc.dart';

class P5dManifestRepository implements P5dRepository {
  P5dManifestRepository({AssetBundle? bundle})
    : this._(bundle ?? rootBundle, p5dPins, p5dReportSha256);
  P5dManifestRepository._(this.bundle, this._pins, this._reportSha256);

  @visibleForTesting
  factory P5dManifestRepository.testing({
    required AssetBundle bundle,
    required List<Map<String, Object?>> pins,
    required String reportSha256,
  }) {
    if (kReleaseMode) throw StateError('test_configuration_refused');
    return P5dManifestRepository._(bundle, pins, reportSha256);
  }
  final List<Map<String, Object?>> _pins;
  final String _reportSha256;
  static P5dManifestRepository create() => P5dManifestRepository();
  final AssetBundle bundle;
  bool _ready = false;

  @override
  Future<Either<P5dFailure, Unit>> initialize() async {
    _ready = false;
    try {
      final data = await bundle.load('assets/p5d/manifest.json');
      if (data.lengthInBytes > 32768) {
        throw const FormatException('manifest_limit');
      }
      final input = jsonDecode(
        utf8.decode(
          data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
        ),
      );
      if (input is! Map ||
          input.length != 3 ||
          input['schemaVersion'] != 1 ||
          input['reportSha256'] != _reportSha256 ||
          jsonEncode(input['cases']) != jsonEncode(_pins)) {
        throw const FormatException('manifest_refused');
      }
      // Her dosya gösterimden önce doğrulanır; bozuk girişte kısmi gösterim yoktur.
      _ready = true;
      for (var i = 0; i < _pins.length; i++) {
        final result = await read(i);
        if (result.isLeft()) throw const FormatException('preflight_refused');
      }
      return const Right(unit);
    } catch (_) {
      _ready = false;
      return const Left(P5dFailure('manifest_or_asset_refused'));
    }
  }

  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) async {
    if (!_ready || index < 0 || index >= _pins.length) {
      return const Left(P5dFailure('case_refused'));
    }
    try {
      final pin = _pins[index];
      var clock = Stopwatch()..start();
      final data = await bundle.load(pin['asset']! as String);
      final readMicros = clock.elapsedMicroseconds;
      if (data.lengthInBytes > 32 * 1024 * 1024 ||
          data.lengthInBytes != pin['bytes']) {
        throw const FormatException('encoded_limit');
      }
      final bytes = data.buffer.asUint8List(
        data.offsetInBytes,
        data.lengthInBytes,
      );
      clock = Stopwatch()..start();
      if (sha256.convert(bytes).toString() != pin['sha256'] ||
          p5dIccSha256(bytes, pin['format']! as String) != pin['iccSha256']) {
        throw const FormatException('sha_or_icc_refused');
      }
      return Right(P5dEncoded(bytes, readMicros, clock.elapsedMicroseconds));
    } catch (_) {
      return const Left(P5dFailure('asset_refused'));
    }
  }
}
