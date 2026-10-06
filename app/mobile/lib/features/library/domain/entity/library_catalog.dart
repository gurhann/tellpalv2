import 'package:equatable/equatable.dart';

class LibraryContext extends Equatable {
  const LibraryContext(this.language, this.accessIdentity);
  final String language;
  final String accessIdentity;
  @override
  List<Object> get props => [language, accessIdentity];
}

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

class LibraryPreview {
  const LibraryPreview({
    required this.contentId,
    required this.externalKey,
    required this.type,
    required this.experienceType,
    required this.contentLanguage,
    required this.title,
    this.ageRange,
    this.durationMinutes,
    required this.isFree,
    required this.isLocked,
    required this.covers,
  });
  final int contentId;
  final String externalKey;
  final String type;
  final String experienceType;
  final String contentLanguage;
  final String title;
  final int? ageRange;
  final int? durationMinutes;
  final bool isFree;
  final bool isLocked;
  final LibraryCovers covers;
}

class LibraryCovers {
  const LibraryCovers({this.phone, this.tablet});
  final LibraryCover? phone;
  final LibraryCover? tablet;
}

class LibraryCover {
  const LibraryCover({
    required this.assetId,
    required this.kind,
    required this.mimeType,
    required this.url,
    required this.expiresAt,
  });
  final int assetId;
  final String kind;
  final String mimeType;
  final String url;
  final DateTime expiresAt;
}
