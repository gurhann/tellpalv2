import 'package:equatable/equatable.dart';

class LibraryContext extends Equatable {
  const LibraryContext(this.language, this.accessIdentity);
  final String language;
  final String accessIdentity;
  @override
  List<Object> get props => [language, accessIdentity];
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
