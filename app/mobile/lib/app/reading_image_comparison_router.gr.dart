// dart format width=80
// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// AutoRouterGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

part of 'reading_image_comparison_router.dart';

/// generated route for
/// [ReadingImageComparisonScreen]
class ReadingImageComparisonRoute
    extends PageRouteInfo<ReadingImageComparisonRouteArgs> {
  ReadingImageComparisonRoute({
    Key? key,
    required int contentId,
    List<PageRouteInfo>? children,
  }) : super(
         ReadingImageComparisonRoute.name,
         args: ReadingImageComparisonRouteArgs(key: key, contentId: contentId),
         rawPathParams: {'contentId': contentId},
         initialChildren: children,
       );

  static const String name = 'ReadingImageComparisonRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final pathParams = data.inheritedPathParams;
      final args = data.argsAs<ReadingImageComparisonRouteArgs>(
        orElse: () => ReadingImageComparisonRouteArgs(
          contentId: pathParams.getInt('contentId'),
        ),
      );
      return ReadingImageComparisonScreen(
        key: args.key,
        contentId: args.contentId,
      );
    },
  );
}

class ReadingImageComparisonRouteArgs {
  const ReadingImageComparisonRouteArgs({this.key, required this.contentId});

  final Key? key;

  final int contentId;

  @override
  String toString() {
    return 'ReadingImageComparisonRouteArgs{key: $key, contentId: $contentId}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ReadingImageComparisonRouteArgs) return false;
    return key == other.key && contentId == other.contentId;
  }

  @override
  int get hashCode => key.hashCode ^ contentId.hashCode;
}
