import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/library_preview.dart';
part 'library_preview_model.g.dart';

int _integer(Object? value) {
  if (value is int) return value;
  if (value is double &&
      value.isFinite &&
      value == value.truncateToDouble() &&
      value >= -9223372036854775808.0 &&
      value < 9223372036854775808.0) {
    return value.toInt();
  }
  throw const FormatException('Library integer field is invalid');
}

int? _optionalInteger(Object? value) => value == null ? null : _integer(value);

@JsonSerializable(createToJson: false)
class LibraryPreviewModel extends LibraryPreview {
  const LibraryPreviewModel({
    required super.contentId,
    required super.externalKey,
    required super.type,
    required super.experienceType,
    required super.contentLanguage,
    required super.title,
    super.ageRange,
    super.durationMinutes,
    required super.isFree,
    required super.isLocked,
    required super.covers,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get contentId => super.contentId;
  @override
  @JsonKey(fromJson: _optionalInteger)
  int? get ageRange => super.ageRange;
  @override
  @JsonKey(fromJson: _optionalInteger)
  int? get durationMinutes => super.durationMinutes;
  @override
  @JsonKey(fromJson: _covers)
  LibraryCovers get covers => super.covers;
  factory LibraryPreviewModel.fromJson(Map<String, dynamic> json) =>
      _$LibraryPreviewModelFromJson(json);
  static LibraryCovers _covers(Map<String, dynamic> value) =>
      LibraryCoversModel.fromJson(value);
}

@JsonSerializable(createToJson: false)
class LibraryCoversModel extends LibraryCovers {
  const LibraryCoversModel({super.phone, super.tablet});
  @override
  @JsonKey(fromJson: _cover)
  LibraryCover? get phone => super.phone;
  @override
  @JsonKey(fromJson: _cover)
  LibraryCover? get tablet => super.tablet;
  factory LibraryCoversModel.fromJson(Map<String, dynamic> json) =>
      _$LibraryCoversModelFromJson(json);
  static LibraryCover? _cover(Map<String, dynamic>? value) =>
      value == null ? null : LibraryCoverModel.fromJson(value);
}

@JsonSerializable(createToJson: false)
class LibraryCoverModel extends LibraryCover {
  const LibraryCoverModel({
    required super.assetId,
    required super.kind,
    required super.mimeType,
    required super.url,
    required super.expiresAt,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get assetId => super.assetId;
  factory LibraryCoverModel.fromJson(Map<String, dynamic> json) =>
      _$LibraryCoverModelFromJson(json);
}
