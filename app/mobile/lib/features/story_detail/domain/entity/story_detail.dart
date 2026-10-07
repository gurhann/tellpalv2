import 'package:equatable/equatable.dart';
import '../../../library/domain/entity/library_catalog.dart';

class StoryDetailIdentity extends Equatable {
  const StoryDetailIdentity({
    required this.categoryId,
    required this.contentId,
    required this.uiLanguage,
    required this.contentLanguage,
    this.experience = 'READING',
  });
  final int categoryId, contentId;
  final String uiLanguage, contentLanguage, experience;
  @override
  List<Object> get props => [
    categoryId,
    contentId,
    uiLanguage,
    contentLanguage,
    experience,
  ];
}

class StoryDetail {
  StoryDetail({
    required this.schemaVersion,
    required this.categoryId,
    required this.contentId,
    required this.externalKey,
    required this.type,
    required this.experienceType,
    required this.requestedLanguage,
    required this.contentLanguage,
    required this.audience,
    required this.title,
    this.ageRange,
    this.durationMinutes,
    this.summary,
    required List<StoryCredit> credits,
    required this.isFree,
    required this.isLocked,
    this.hero,
    required List<LibraryPreview> recommendations,
  }) : credits = List.unmodifiable(credits),
       recommendations = List.unmodifiable(recommendations);
  final int schemaVersion, categoryId, contentId;
  final String externalKey,
      type,
      experienceType,
      requestedLanguage,
      contentLanguage,
      audience,
      title;
  final int? ageRange, durationMinutes;
  final String? summary;
  final List<StoryCredit> credits;
  final bool isFree, isLocked;
  final StoryHero? hero;
  final List<LibraryPreview> recommendations;
}

class StoryCredit {
  const StoryCredit({
    required this.contributorId,
    required this.role,
    required this.displayName,
  });
  final int contributorId;
  final String role, displayName;
}

class StoryHero {
  const StoryHero({
    required this.assetId,
    required this.checksumSha256,
    required this.mimeType,
    required this.byteSize,
    required this.url,
    required this.expiresAt,
    required this.width,
    required this.height,
  });
  final int assetId, byteSize, width, height;
  final String checksumSha256, mimeType, url;
  final DateTime expiresAt;
}
