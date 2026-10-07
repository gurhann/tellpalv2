// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'story_detail_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StoryDetailModel _$StoryDetailModelFromJson(Map<String, dynamic> json) =>
    StoryDetailModel(
      schemaVersion: _integer(json['schemaVersion']),
      categoryId: _integer(json['categoryId']),
      contentId: _integer(json['contentId']),
      externalKey: json['externalKey'] as String,
      type: json['type'] as String,
      experienceType: json['experienceType'] as String,
      requestedLanguage: json['requestedLanguage'] as String,
      contentLanguage: json['contentLanguage'] as String,
      audience: json['audience'] as String,
      title: json['title'] as String,
      ageRange: _optionalInteger(json['ageRange']),
      durationMinutes: _optionalInteger(json['durationMinutes']),
      summary: json['summary'] as String?,
      credits: StoryDetailModel._credits(json['credits'] as List),
      isFree: json['isFree'] as bool,
      isLocked: json['isLocked'] as bool,
      hero: StoryDetailModel._hero(json['hero'] as Map<String, dynamic>?),
      recommendations: StoryDetailModel._recommendations(
        json['recommendations'] as List,
      ),
    );

StoryCreditModel _$StoryCreditModelFromJson(Map<String, dynamic> json) =>
    StoryCreditModel(
      contributorId: _integer(json['contributorId']),
      role: json['role'] as String,
      displayName: json['displayName'] as String,
    );

StoryHeroModel _$StoryHeroModelFromJson(Map<String, dynamic> json) =>
    StoryHeroModel(
      assetId: _integer(json['assetId']),
      checksumSha256: json['checksumSha256'] as String,
      mimeType: json['mimeType'] as String,
      byteSize: _integer(json['byteSize']),
      url: json['url'] as String,
      expiresAt: DateTime.parse(json['expiresAt'] as String),
      width: _integer(json['width']),
      height: _integer(json['height']),
    );
