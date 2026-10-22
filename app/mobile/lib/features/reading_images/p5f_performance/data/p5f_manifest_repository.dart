import 'package:dartz/dartz.dart';
import 'package:flutter/services.dart';
import '../../p5d_offline/data/p5d_manifest_repository.dart';
import '../../p5d_offline/domain/p5d_pins.dart';
import '../../p5d_offline/domain/p5d_repository.dart';
import '../domain/p5f_repository.dart';

/// SHA/ICC yalnız ön doğrulamadadır; zamanlı okuma sıcak asset yükünü ölçer.
class P5fManifestRepository implements P5fRepository {
  P5fManifestRepository({AssetBundle? bundle}) : bundle = bundle ?? rootBundle;
  final AssetBundle bundle;
  bool _ready = false;
  @override
  Future<Either<P5dFailure, Unit>> initialize() async {
    _ready = false;
    final result = await P5dManifestRepository(bundle: bundle).initialize();
    _ready = result.isRight();
    return result;
  }

  @override
  Future<Either<P5dFailure, P5dEncoded>> read(int index) async {
    if (!_ready || index < 0 || index >= p5dPins.length) {
      return const Left(P5dFailure('preflight_required'));
    }
    try {
      final clock = Stopwatch()..start();
      final data = await bundle.load(p5dPins[index]['asset']! as String);
      final elapsed = clock.elapsedMicroseconds;
      if (data.lengthInBytes != p5dPins[index]['bytes']) {
        return const Left(P5dFailure('asset_size_changed'));
      }
      return Right(
        P5dEncoded(
          data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
          elapsed,
          0,
        ),
      );
    } catch (_) {
      return const Left(P5dFailure('asset_read_refused'));
    }
  }
}
