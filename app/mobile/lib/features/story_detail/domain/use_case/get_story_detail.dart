import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../entity/story_detail.dart';
import '../repository/story_detail_repository.dart';

@injectable
class GetStoryDetail {
  const GetStoryDetail(this._repository);
  final StoryDetailRepository _repository;
  Future<Either<Failure, StoryDetail>> call(StoryDetailIdentity identity) =>
      _repository.fetch(identity);
}
