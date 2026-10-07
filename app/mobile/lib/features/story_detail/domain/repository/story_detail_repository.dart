import 'package:dartz/dartz.dart';
import '../../../../core/error/failure.dart';
import '../entity/story_detail.dart';

abstract interface class StoryDetailRepository {
  Future<Either<Failure, StoryDetail>> fetch(StoryDetailIdentity identity);
}
