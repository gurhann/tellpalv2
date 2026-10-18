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

/// generated route for
/// [ReadingImageConsumerDetailScreen]
class ReadingImageConsumerDetailRoute
    extends PageRouteInfo<ReadingImageConsumerDetailRouteArgs> {
  ReadingImageConsumerDetailRoute({
    Key? key,
    required int contentId,
    List<PageRouteInfo>? children,
  }) : super(
         ReadingImageConsumerDetailRoute.name,
         args: ReadingImageConsumerDetailRouteArgs(
           key: key,
           contentId: contentId,
         ),
         rawPathParams: {'contentId': contentId},
         initialChildren: children,
       );

  static const String name = 'ReadingImageConsumerDetailRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final pathParams = data.inheritedPathParams;
      final args = data.argsAs<ReadingImageConsumerDetailRouteArgs>(
        orElse: () => ReadingImageConsumerDetailRouteArgs(
          contentId: pathParams.getInt('contentId'),
        ),
      );
      return ReadingImageConsumerDetailScreen(
        key: args.key,
        contentId: args.contentId,
      );
    },
  );
}

class ReadingImageConsumerDetailRouteArgs {
  const ReadingImageConsumerDetailRouteArgs({
    this.key,
    required this.contentId,
  });

  final Key? key;

  final int contentId;

  @override
  String toString() {
    return 'ReadingImageConsumerDetailRouteArgs{key: $key, contentId: $contentId}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ReadingImageConsumerDetailRouteArgs) return false;
    return key == other.key && contentId == other.contentId;
  }

  @override
  int get hashCode => key.hashCode ^ contentId.hashCode;
}

/// generated route for
/// [ReadingImageConsumerFlowScreen]
class ReadingImageConsumerFlowRoute
    extends PageRouteInfo<ReadingImageConsumerFlowRouteArgs> {
  ReadingImageConsumerFlowRoute({
    Key? key,
    required int contentId,
    List<PageRouteInfo>? children,
  }) : super(
         ReadingImageConsumerFlowRoute.name,
         args: ReadingImageConsumerFlowRouteArgs(
           key: key,
           contentId: contentId,
         ),
         rawPathParams: {'contentId': contentId},
         initialChildren: children,
       );

  static const String name = 'ReadingImageConsumerFlowRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final pathParams = data.inheritedPathParams;
      final args = data.argsAs<ReadingImageConsumerFlowRouteArgs>(
        orElse: () => ReadingImageConsumerFlowRouteArgs(
          contentId: pathParams.getInt('contentId'),
        ),
      );
      return ReadingImageConsumerFlowScreen(
        key: args.key,
        contentId: args.contentId,
      );
    },
  );
}

class ReadingImageConsumerFlowRouteArgs {
  const ReadingImageConsumerFlowRouteArgs({this.key, required this.contentId});

  final Key? key;

  final int contentId;

  @override
  String toString() {
    return 'ReadingImageConsumerFlowRouteArgs{key: $key, contentId: $contentId}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ReadingImageConsumerFlowRouteArgs) return false;
    return key == other.key && contentId == other.contentId;
  }

  @override
  int get hashCode => key.hashCode ^ contentId.hashCode;
}

/// generated route for
/// [ReadingImageConsumerReaderScreen]
class ReadingImageConsumerReaderRoute
    extends PageRouteInfo<ReadingImageConsumerReaderRouteArgs> {
  ReadingImageConsumerReaderRoute({
    Key? key,
    required int contentId,
    required int pageId,
    List<PageRouteInfo>? children,
  }) : super(
         ReadingImageConsumerReaderRoute.name,
         args: ReadingImageConsumerReaderRouteArgs(
           key: key,
           contentId: contentId,
           pageId: pageId,
         ),
         rawPathParams: {'contentId': contentId, 'pageId': pageId},
         initialChildren: children,
       );

  static const String name = 'ReadingImageConsumerReaderRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final pathParams = data.inheritedPathParams;
      final args = data.argsAs<ReadingImageConsumerReaderRouteArgs>(
        orElse: () => ReadingImageConsumerReaderRouteArgs(
          contentId: pathParams.getInt('contentId'),
          pageId: pathParams.getInt('pageId'),
        ),
      );
      return ReadingImageConsumerReaderScreen(
        key: args.key,
        contentId: args.contentId,
        pageId: args.pageId,
      );
    },
  );
}

class ReadingImageConsumerReaderRouteArgs {
  const ReadingImageConsumerReaderRouteArgs({
    this.key,
    required this.contentId,
    required this.pageId,
  });

  final Key? key;

  final int contentId;

  final int pageId;

  @override
  String toString() {
    return 'ReadingImageConsumerReaderRouteArgs{key: $key, contentId: $contentId, pageId: $pageId}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ReadingImageConsumerReaderRouteArgs) return false;
    return key == other.key &&
        contentId == other.contentId &&
        pageId == other.pageId;
  }

  @override
  int get hashCode => key.hashCode ^ contentId.hashCode ^ pageId.hashCode;
}
