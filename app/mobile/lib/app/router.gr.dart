// dart format width=80
// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// AutoRouterGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

part of 'router.dart';

/// generated route for
/// [LibraryScreen]
class LibraryRoute extends PageRouteInfo<void> {
  const LibraryRoute({List<PageRouteInfo>? children})
    : super(LibraryRoute.name, initialChildren: children);

  static const String name = 'LibraryRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      return const LibraryScreen();
    },
  );
}

/// generated route for
/// [StoryDetailScreen]
class StoryDetailRoute extends PageRouteInfo<StoryDetailRouteArgs> {
  StoryDetailRoute({
    Key? key,
    required int categoryId,
    required int contentId,
    required String uiLanguage,
    required String contentLanguage,
    String experience = 'READING',
    List<PageRouteInfo>? children,
  }) : super(
         StoryDetailRoute.name,
         args: StoryDetailRouteArgs(
           key: key,
           categoryId: categoryId,
           contentId: contentId,
           uiLanguage: uiLanguage,
           contentLanguage: contentLanguage,
           experience: experience,
         ),
         initialChildren: children,
       );

  static const String name = 'StoryDetailRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final args = data.argsAs<StoryDetailRouteArgs>();
      return StoryDetailScreen(
        key: args.key,
        categoryId: args.categoryId,
        contentId: args.contentId,
        uiLanguage: args.uiLanguage,
        contentLanguage: args.contentLanguage,
        experience: args.experience,
      );
    },
  );
}

class StoryDetailRouteArgs {
  const StoryDetailRouteArgs({
    this.key,
    required this.categoryId,
    required this.contentId,
    required this.uiLanguage,
    required this.contentLanguage,
    this.experience = 'READING',
  });

  final Key? key;

  final int categoryId;

  final int contentId;

  final String uiLanguage;

  final String contentLanguage;

  final String experience;

  @override
  String toString() {
    return 'StoryDetailRouteArgs{key: $key, categoryId: $categoryId, contentId: $contentId, uiLanguage: $uiLanguage, contentLanguage: $contentLanguage, experience: $experience}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! StoryDetailRouteArgs) return false;
    return key == other.key &&
        categoryId == other.categoryId &&
        contentId == other.contentId &&
        uiLanguage == other.uiLanguage &&
        contentLanguage == other.contentLanguage &&
        experience == other.experience;
  }

  @override
  int get hashCode =>
      key.hashCode ^
      categoryId.hashCode ^
      contentId.hashCode ^
      uiLanguage.hashCode ^
      contentLanguage.hashCode ^
      experience.hashCode;
}
