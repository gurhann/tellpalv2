import 'dart:typed_data';
import 'package:dartz/dartz.dart';

class P5dFailure {
  const P5dFailure(this.code);
  final String code;
}

class P5dEncoded {
  const P5dEncoded(this.bytes, this.readMicros, this.checksumMicros);
  final Uint8List bytes;
  final int readMicros, checksumMicros;
}

abstract interface class P5dRepository {
  Future<Either<P5dFailure, Unit>> initialize();
  Future<Either<P5dFailure, P5dEncoded>> read(int index);
}
