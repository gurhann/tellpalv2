// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'library_preview_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

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
