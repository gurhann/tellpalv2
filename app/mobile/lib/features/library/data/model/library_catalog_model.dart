import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/library_catalog.dart';
part 'library_catalog_model.g.dart';

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
class LibraryCatalogModel extends LibraryCatalog {
  LibraryCatalogModel({
    required super.schemaVersion,
    required super.requestedLanguage,
    required super.audience,
    required super.previewLimit,
    required super.rows,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get schemaVersion => super.schemaVersion;
  @override
  @JsonKey(fromJson: _integer)
  int get previewLimit => super.previewLimit;
  @override
  @JsonKey(fromJson: _rows)
  List<LibraryRow> get rows => super.rows;
  factory LibraryCatalogModel.fromJson(Map<String, dynamic> json) =>
      _$LibraryCatalogModelFromJson(json);
  static List<LibraryRow> _rows(List<dynamic> value) => value
      .map((j) => LibraryRowModel.fromJson(j as Map<String, dynamic>))
      .toList();
}

@JsonSerializable(createToJson: false)
class LibraryRowModel extends LibraryRow {
  LibraryRowModel({
    required super.categoryId,
    required super.slug,
    required super.name,
    super.description,
    required super.displayOrder,
    required super.contentLanguage,
    required super.items,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get categoryId => super.categoryId;
  @override
  @JsonKey(fromJson: _integer)
  int get displayOrder => super.displayOrder;
  @override
  @JsonKey(fromJson: _items)
  List<LibraryPreview> get items => super.items;
  factory LibraryRowModel.fromJson(Map<String, dynamic> json) =>
      _$LibraryRowModelFromJson(json);
  static List<LibraryPreview> _items(List<dynamic> value) => value
      .map((j) => LibraryPreviewModel.fromJson(j as Map<String, dynamic>))
      .toList();
}

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
