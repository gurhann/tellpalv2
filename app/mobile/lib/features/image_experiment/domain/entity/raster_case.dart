import 'dart:math' as math;
import 'package:equatable/equatable.dart';

/// Immutable admitted fixture facts; credentials and file paths never enter domain state.
class RasterCase extends Equatable {
  const RasterCase({
    required this.caseId,
    required this.role,
    required this.arm,
    required this.sourceSha256,
    required this.referenceSha256,
    required this.outputSha256,
    required this.mime,
    required this.byteSize,
    required this.width,
    required this.height,
    required this.encoderRevision,
    required this.profileRevision,
    required this.logicalSlotWidth,
    required this.logicalSlotHeight,
    required this.dpr,
    required this.fit,
    required this.alignmentX,
    required this.alignmentY,
    required this.purposeAcceptanceReceipt,
  });
  final String caseId,
      role,
      arm,
      sourceSha256,
      referenceSha256,
      outputSha256,
      mime,
      encoderRevision,
      profileRevision,
      fit,
      purposeAcceptanceReceipt;
  final int byteSize, width, height;
  final double logicalSlotWidth, logicalSlotHeight, dpr, alignmentX, alignmentY;

  RasterGeometry get geometry {
    final pw = (logicalSlotWidth * dpr).ceil();
    final ph = (logicalSlotHeight * dpr).ceil();
    if (pw <= 0 || ph <= 0) throw const FormatException('fixture_geometry');
    final scale = fit == 'contain'
        ? math.min(pw / width, ph / height)
        : math.max(pw / width, ph / height);
    final bounded = math.min(1.0, scale);
    final dw = math.min(width, (width * bounded).ceil());
    final dh = math.min(height, (height * bounded).ceil());
    if (dw * dh > 16777216) throw const FormatException('decode_capacity');
    return RasterGeometry(
      physicalWidth: pw,
      physicalHeight: ph,
      requiredWidth: (width * scale).ceil(),
      requiredHeight: (height * scale).ceil(),
      decodeWidth: dw,
      decodeHeight: dh,
      sourceShortage: scale > 1,
    );
  }

  @override
  List<Object?> get props => [
    caseId,
    role,
    arm,
    sourceSha256,
    referenceSha256,
    outputSha256,
    mime,
    byteSize,
    width,
    height,
    encoderRevision,
    profileRevision,
    logicalSlotWidth,
    logicalSlotHeight,
    dpr,
    fit,
    alignmentX,
    alignmentY,
    purposeAcceptanceReceipt,
  ];
  @override
  String toString() => 'RasterCase(admitted)';
}

class RasterGeometry {
  const RasterGeometry({
    required this.physicalWidth,
    required this.physicalHeight,
    required this.requiredWidth,
    required this.requiredHeight,
    required this.decodeWidth,
    required this.decodeHeight,
    required this.sourceShortage,
  });
  final int physicalWidth,
      physicalHeight,
      requiredWidth,
      requiredHeight,
      decodeWidth,
      decodeHeight;
  final bool sourceShortage;
}
