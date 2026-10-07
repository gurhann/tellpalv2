import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/entity/story_detail.dart';
import 'package:tellpal_mobile_poc/features/story_detail/domain/repository/story_detail_repository.dart';
import 'package:tellpal_mobile_poc/features/story_detail/data/model/story_detail_model.dart';

StoryDetailIdentity identity({
  int id = 1,
  String ui = 'tr',
  String lang = 'tr',
}) => StoryDetailIdentity(
  categoryId: 2,
  contentId: id,
  uiLanguage: ui,
  contentLanguage: lang,
);
Map<String, dynamic> payload({
  int id = 1,
  String ui = 'tr',
  String lang = 'tr',
  bool locked = false,
  bool recommendations = false,
}) => {
  'schemaVersion': 1,
  'categoryId': 2,
  'contentId': id,
  'externalKey': 'story-$id',
  'type': 'STORY',
  'experienceType': 'READING',
  'requestedLanguage': ui,
  'contentLanguage': lang,
  'audience': 'NON_SUBSCRIBER',
  'title': 'Selected story $id with a long title',
  'ageRange': 5,
  'durationMinutes': 8,
  'summary': 'A long localized story summary. ' * 12,
  'credits': [
    {
      'contributorId': 1,
      'role': 'AUTHOR',
      'displayName': 'Author Name with a long complete credit',
    },
  ],
  'isFree': !locked,
  'isLocked': locked,
  'hero': null,
  'recommendations': recommendations
      ? [
          {
            'contentId': id + 1,
            'externalKey': 'story-${id + 1}',
            'type': 'STORY',
            'experienceType': 'READING',
            'contentLanguage': lang,
            'title': 'Recommendation ${id + 1}',
            'ageRange': null,
            'durationMinutes': null,
            'isFree': false,
            'isLocked': true,
            'covers': {'phone': null, 'tablet': null},
          },
        ]
      : [],
};
StoryDetail detail({
  int id = 1,
  String ui = 'tr',
  String lang = 'tr',
  bool locked = false,
  bool recommendations = false,
}) => StoryDetailModel.fromJson(
  payload(
    id: id,
    ui: ui,
    lang: lang,
    locked: locked,
    recommendations: recommendations,
  ),
);

class ControlledDetailRepository implements StoryDetailRepository {
  final calls = <StoryDetailIdentity>[];
  final responses = <Completer<Either<Failure, StoryDetail>>>[];
  @override
  Future<Either<Failure, StoryDetail>> fetch(StoryDetailIdentity identity) {
    calls.add(identity);
    final response = Completer<Either<Failure, StoryDetail>>();
    responses.add(response);
    return response.future;
  }
}
