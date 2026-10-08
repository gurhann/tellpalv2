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
