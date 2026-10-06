// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'library_catalog_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LibraryCatalogModel _$LibraryCatalogModelFromJson(Map<String, dynamic> json) =>
    LibraryCatalogModel(
      schemaVersion: _integer(json['schemaVersion']),
      requestedLanguage: json['requestedLanguage'] as String,
      audience: json['audience'] as String,
      previewLimit: _integer(json['previewLimit']),
      rows: LibraryCatalogModel._rows(json['rows'] as List),
    );

LibraryRowModel _$LibraryRowModelFromJson(Map<String, dynamic> json) =>
    LibraryRowModel(
      categoryId: _integer(json['categoryId']),
      slug: json['slug'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      displayOrder: _integer(json['displayOrder']),
      contentLanguage: json['contentLanguage'] as String,
      items: LibraryRowModel._items(json['items'] as List),
    );

LibraryPreviewModel _$LibraryPreviewModelFromJson(Map<String, dynamic> json) =>
    LibraryPreviewModel(
      contentId: _integer(json['contentId']),
      externalKey: json['externalKey'] as String,
      type: json['type'] as String,
      experienceType: json['experienceType'] as String,
      contentLanguage: json['contentLanguage'] as String,
      title: json['title'] as String,
      ageRange: _optionalInteger(json['ageRange']),
      durationMinutes: _optionalInteger(json['durationMinutes']),
      isFree: json['isFree'] as bool,
      isLocked: json['isLocked'] as bool,
      covers: LibraryPreviewModel._covers(
        json['covers'] as Map<String, dynamic>,
      ),
    );

LibraryCoversModel _$LibraryCoversModelFromJson(Map<String, dynamic> json) =>
    LibraryCoversModel(
      phone: LibraryCoversModel._cover(json['phone'] as Map<String, dynamic>?),
      tablet: LibraryCoversModel._cover(
        json['tablet'] as Map<String, dynamic>?,
      ),
    );

LibraryCoverModel _$LibraryCoverModelFromJson(Map<String, dynamic> json) =>
    LibraryCoverModel(
      assetId: _integer(json['assetId']),
      kind: json['kind'] as String,
      mimeType: json['mimeType'] as String,
      url: json['url'] as String,
      expiresAt: DateTime.parse(json['expiresAt'] as String),
    );
