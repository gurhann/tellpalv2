import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/story_detail.dart';
import '../../../../core/preview/domain/entity/library_preview.dart';
import '../../../../core/preview/data/model/library_preview_model.dart';
part 'story_detail_model.g.dart';

int _integer(Object? value) {
  if (value is int) return value;
  throw const FormatException('Detail integer is invalid');
}

int? _optionalInteger(Object? value) => value == null ? null : _integer(value);

@JsonSerializable(createToJson: false)
class StoryDetailModel extends StoryDetail {
  StoryDetailModel({
    required super.schemaVersion,
    required super.categoryId,
    required super.contentId,
    required super.externalKey,
    required super.type,
    required super.experienceType,
    required super.requestedLanguage,
    required super.contentLanguage,
    required super.audience,
    required super.title,
    super.ageRange,
    super.durationMinutes,
    super.summary,
    required super.credits,
    required super.isFree,
    required super.isLocked,
    super.hero,
    required super.recommendations,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get schemaVersion => super.schemaVersion;
  @override
  @JsonKey(fromJson: _integer)
  int get categoryId => super.categoryId;
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
  @JsonKey(fromJson: _credits)
  List<StoryCredit> get credits => super.credits;
  @override
  @JsonKey(fromJson: _hero)
  StoryHero? get hero => super.hero;
  @override
  @JsonKey(fromJson: _recommendations)
  List<LibraryPreview> get recommendations => super.recommendations;
  factory StoryDetailModel.fromJson(Map<String, dynamic> json) =>
      _$StoryDetailModelFromJson(json);
  static List<StoryCredit> _credits(List<dynamic> value) => value
      .map((j) => StoryCreditModel.fromJson(j as Map<String, dynamic>))
      .toList();
  static StoryHero? _hero(Map<String, dynamic>? value) =>
      value == null ? null : StoryHeroModel.fromJson(value);
  static List<LibraryPreview> _recommendations(List<dynamic> value) => value
      .map((j) => LibraryPreviewModel.fromJson(j as Map<String, dynamic>))
      .toList();
}

@JsonSerializable(createToJson: false)
class StoryCreditModel extends StoryCredit {
  StoryCreditModel({
    required super.contributorId,
    required super.role,
    required super.displayName,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get contributorId => super.contributorId;
  factory StoryCreditModel.fromJson(Map<String, dynamic> json) =>
      _$StoryCreditModelFromJson(json);
}

@JsonSerializable(createToJson: false)
class StoryHeroModel extends StoryHero {
  StoryHeroModel({
    required super.assetId,
    required super.checksumSha256,
    required super.mimeType,
    required super.byteSize,
    required super.url,
    required super.expiresAt,
    required super.width,
    required super.height,
  });
  @override
  @JsonKey(fromJson: _integer)
  int get assetId => super.assetId;
  @override
  @JsonKey(fromJson: _integer)
  int get byteSize => super.byteSize;
  @override
  @JsonKey(fromJson: _integer)
  int get width => super.width;
  @override
  @JsonKey(fromJson: _integer)
  int get height => super.height;
  factory StoryHeroModel.fromJson(Map<String, dynamic> json) =>
      _$StoryHeroModelFromJson(json);
}
