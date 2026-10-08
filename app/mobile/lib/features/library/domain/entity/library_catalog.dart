import '../../../../core/preview/domain/entity/library_preview.dart';
export '../../../../core/preview/domain/entity/library_preview.dart';

class LibraryCatalog {
  LibraryCatalog({
    required this.schemaVersion,
    required this.requestedLanguage,
    required this.audience,
    required this.previewLimit,
    required List<LibraryRow> rows,
  }) : rows = List.unmodifiable(rows);
  final int schemaVersion;
  final String requestedLanguage;
  final String audience;
  final int previewLimit;
  final List<LibraryRow> rows;
}

class LibraryRow {
  LibraryRow({
    required this.categoryId,
    required this.slug,
    required this.name,
    this.description,
    required this.displayOrder,
    required this.contentLanguage,
    required List<LibraryPreview> items,
  }) : items = List.unmodifiable(items);
  final int categoryId;
  final String slug;
  final String name;
  final String? description;
  final int displayOrder;
  final String contentLanguage;
  final List<LibraryPreview> items;
}
