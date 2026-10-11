// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'raster_case_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RasterCaseModel _$RasterCaseModelFromJson(Map<String, dynamic> json) =>
    RasterCaseModel(
      caseId: json['caseId'] as String,
      role: json['role'] as String,
      arm: json['arm'] as String,
      sourceSha256: json['sourceSha256'] as String,
      referenceSha256: json['referenceSha256'] as String,
      outputSha256: json['outputSha256'] as String,
      mime: json['mime'] as String,
      byteSize: (json['byteSize'] as num).toInt(),
      width: (json['width'] as num).toInt(),
      height: (json['height'] as num).toInt(),
      encoderRevision: json['encoderRevision'] as String,
      profileRevision: json['profileRevision'] as String,
      logicalSlotWidth: (json['logicalSlotWidth'] as num).toDouble(),
      logicalSlotHeight: (json['logicalSlotHeight'] as num).toDouble(),
      dpr: (json['dpr'] as num).toDouble(),
      fit: json['fit'] as String,
      alignmentX: (json['alignmentX'] as num).toDouble(),
      alignmentY: (json['alignmentY'] as num).toDouble(),
      purposeAcceptanceReceipt: json['purposeAcceptanceReceipt'] as String,
    );

Map<String, dynamic> _$RasterCaseModelToJson(RasterCaseModel instance) =>
    <String, dynamic>{
      'caseId': instance.caseId,
      'role': instance.role,
      'arm': instance.arm,
      'sourceSha256': instance.sourceSha256,
      'referenceSha256': instance.referenceSha256,
      'outputSha256': instance.outputSha256,
      'mime': instance.mime,
      'encoderRevision': instance.encoderRevision,
      'profileRevision': instance.profileRevision,
      'fit': instance.fit,
      'purposeAcceptanceReceipt': instance.purposeAcceptanceReceipt,
      'byteSize': instance.byteSize,
      'width': instance.width,
      'height': instance.height,
      'logicalSlotWidth': instance.logicalSlotWidth,
      'logicalSlotHeight': instance.logicalSlotHeight,
      'dpr': instance.dpr,
      'alignmentX': instance.alignmentX,
      'alignmentY': instance.alignmentY,
    };
