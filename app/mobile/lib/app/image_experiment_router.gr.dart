// dart format width=80
// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// AutoRouterGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

part of 'image_experiment_router.dart';

/// generated route for
/// [RasterCaseScreen]
class RasterCaseRoute extends PageRouteInfo<RasterCaseRouteArgs> {
  RasterCaseRoute({
    Key? key,
    required String caseId,
    List<PageRouteInfo>? children,
  }) : super(
         RasterCaseRoute.name,
         args: RasterCaseRouteArgs(key: key, caseId: caseId),
         rawPathParams: {'caseId': caseId},
         initialChildren: children,
       );

  static const String name = 'RasterCaseRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      final pathParams = data.inheritedPathParams;
      final args = data.argsAs<RasterCaseRouteArgs>(
        orElse: () =>
            RasterCaseRouteArgs(caseId: pathParams.getString('caseId')),
      );
      return RasterCaseScreen(key: args.key, caseId: args.caseId);
    },
  );
}

class RasterCaseRouteArgs {
  const RasterCaseRouteArgs({this.key, required this.caseId});

  final Key? key;

  final String caseId;

  @override
  String toString() {
    return 'RasterCaseRouteArgs{key: $key, caseId: $caseId}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! RasterCaseRouteArgs) return false;
    return key == other.key && caseId == other.caseId;
  }

  @override
  int get hashCode => key.hashCode ^ caseId.hashCode;
}

/// generated route for
/// [ReaderResolutionScreen]
class ReaderResolutionRoute extends PageRouteInfo<void> {
  const ReaderResolutionRoute({List<PageRouteInfo>? children})
    : super(ReaderResolutionRoute.name, initialChildren: children);

  static const String name = 'ReaderResolutionRoute';

  static PageInfo page = PageInfo(
    name,
    builder: (data) {
      return const ReaderResolutionScreen();
    },
  );
}
