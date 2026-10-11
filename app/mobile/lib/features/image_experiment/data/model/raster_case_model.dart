import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/raster_case.dart';
part 'raster_case_model.g.dart';

@JsonSerializable()
class RasterCaseModel extends RasterCase {
  const RasterCaseModel({
    required super.caseId,
    required super.role,
    required super.arm,
    required super.sourceSha256,
    required super.referenceSha256,
    required super.outputSha256,
    required super.mime,
    required super.byteSize,
    required super.width,
    required super.height,
    required super.encoderRevision,
    required super.profileRevision,
    required super.logicalSlotWidth,
    required super.logicalSlotHeight,
    required super.dpr,
    required super.fit,
    required super.alignmentX,
    required super.alignmentY,
    required super.purposeAcceptanceReceipt,
  });
  factory RasterCaseModel.fromJson(Map<String, dynamic> json) =>
      _$RasterCaseModelFromJson(json);
  Map<String, dynamic> toJson() => _$RasterCaseModelToJson(this);
}
