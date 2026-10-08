import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/library_catalog.dart';
import '../../../../core/preview/data/model/library_preview_model.dart';
export '../../../../core/preview/data/model/library_preview_model.dart';
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
