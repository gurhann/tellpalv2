import 'package:injectable/injectable.dart';
import '../../../../core/network/authenticated_transport.dart';
import '../../domain/entity/story_detail.dart';
import '../model/story_detail_model.dart';

@injectable
class StoryDetailDatasource {
  const StoryDetailDatasource(this._transport);
  final AuthenticatedTransport _transport;
  Future<StoryDetailModel> fetch(StoryDetailIdentity identity) async =>
      StoryDetailModel.fromJson(
        await _transport.storyDetail(
          categoryId: identity.categoryId,
          contentId: identity.contentId,
          uiLanguage: identity.uiLanguage,
          contentLanguage: identity.contentLanguage,
          experience: identity.experience,
        ),
      );
}
