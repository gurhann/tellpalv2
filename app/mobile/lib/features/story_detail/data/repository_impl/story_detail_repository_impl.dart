import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/network/authenticated_transport.dart';
import '../../domain/entity/story_detail.dart';
import '../../domain/repository/story_detail_repository.dart';
import '../datasource/story_detail_datasource.dart';

@Injectable(as: StoryDetailRepository)
class StoryDetailRepositoryImpl implements StoryDetailRepository {
  const StoryDetailRepositoryImpl(this._source);
  final StoryDetailDatasource _source;
  @override
  Future<Either<Failure, StoryDetail>> fetch(
    StoryDetailIdentity identity,
  ) async {
    if (identity.categoryId <= 0 ||
        identity.contentId <= 0 ||
        identity.experience != 'READING' ||
        !['tr', 'en', 'pt'].contains(identity.uiLanguage) ||
        !['tr', 'en', 'pt'].contains(identity.contentLanguage)) {
      return left(const Failure(FailureKind.invalidResponse));
    }
    try {
      final detail = await _source.fetch(identity);
      if (detail.schemaVersion != 1 ||
          detail.categoryId != identity.categoryId ||
          detail.contentId != identity.contentId ||
          detail.requestedLanguage != identity.uiLanguage ||
          detail.contentLanguage != identity.contentLanguage ||
          detail.type != 'STORY' ||
          detail.experienceType != identity.experience ||
          detail.title.trim().isEmpty ||
          !['NON_SUBSCRIBER', 'SUBSCRIBER'].contains(detail.audience) ||
          detail.isLocked !=
              (!detail.isFree && detail.audience != 'SUBSCRIBER') ||
          detail.credits.length > 100 ||
          detail.recommendations.length > 100 ||
          (detail.ageRange != null && detail.ageRange! < 0) ||
          (detail.durationMinutes != null && detail.durationMinutes! < 0)) {
        throw const FormatException('Detail contract mismatch');
      }
      final creditIds = <String>{};
      for (final credit in detail.credits) {
        if (credit.contributorId <= 0 ||
            ![
              'AUTHOR',
              'NARRATOR',
              'ILLUSTRATOR',
              'MUSICIAN',
            ].contains(credit.role) ||
            credit.displayName.trim().isEmpty ||
            !creditIds.add('${credit.contributorId}:${credit.role}')) {
          throw const FormatException('Credit contract mismatch');
        }
      }
      final hero = detail.hero;
      if (hero != null &&
          (hero.assetId <= 0 ||
              !RegExp(r'^[a-f0-9]{64}$').hasMatch(hero.checksumSha256) ||
              !['image/png', 'image/jpeg'].contains(hero.mimeType) ||
              hero.byteSize <= 0 ||
              hero.byteSize > 20971520 ||
              hero.width <= 0 ||
              hero.height <= 0 ||
              hero.width * hero.height > 40000000 ||
              !_url(hero.url))) {
        throw const FormatException('Hero contract mismatch');
      }
      final ids = <int>{identity.contentId};
      for (final item in detail.recommendations) {
        if (item.contentId <= 0 ||
            !ids.add(item.contentId) ||
            item.contentLanguage != identity.contentLanguage ||
            item.type != 'STORY' ||
            item.experienceType != 'READING' ||
            item.title.trim().isEmpty ||
            item.isLocked !=
                (!item.isFree && detail.audience != 'SUBSCRIBER')) {
          throw const FormatException('Recommendation contract mismatch');
        }
        for (final cover in [item.covers.phone, item.covers.tablet]) {
          if (cover != null &&
              (cover.assetId <= 0 ||
                  cover.mimeType != 'image/jpeg' ||
                  !_url(cover.url))) {
            throw const FormatException('Recommendation cover mismatch');
          }
        }
      }
      return right(detail);
    } on CatalogHttpFailure catch (error) {
      return left(
        Failure(
          error.status == 404
              ? FailureKind.unavailable
              : error.status == 401
              ? FailureKind.unauthorized
              : error.status == 403
              ? FailureKind.forbidden
              : error.status == 400
              ? FailureKind.invalidResponse
              : FailureKind.network,
        ),
      );
    } on TimeoutException {
      return left(const Failure(FailureKind.timeout));
    } on FormatException {
      return left(const Failure(FailureKind.invalidResponse));
    } on TypeError {
      return left(const Failure(FailureKind.invalidResponse));
    } catch (_) {
      return left(const Failure(FailureKind.network));
    }
  }

  static bool _url(String value) {
    final url = Uri.tryParse(value);
    return url != null &&
        ['http', 'https'].contains(url.scheme) &&
        url.host.isNotEmpty &&
        url.userInfo.isEmpty;
  }
}
