import 'package:flutter/widgets.dart';

/// Feature sınırından yalnız kimlik ve gerçek görsel slotu geçer.
class ReadingArtworkRequest {
  const ReadingArtworkRequest({
    required this.contentId,
    required this.language,
    required this.fit,
    required this.visible,
    required this.attempt,
    required this.onLoaded,
    required this.onError,
    this.pageNumber = 0,
    this.retryLabel,
  });
  final int contentId, attempt, pageNumber;
  final String language;
  final String? retryLabel;
  final BoxFit fit;
  final bool visible;
  final VoidCallback onLoaded, onError;
}

class ReadingArtworkCapability extends InheritedWidget {
  const ReadingArtworkCapability({
    super.key,
    required this.builder,
    required super.child,
  });
  final Widget Function(BuildContext, ReadingArtworkRequest) builder;
  static ReadingArtworkCapability? maybeOf(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<ReadingArtworkCapability>();
  @override
  bool updateShouldNotify(ReadingArtworkCapability oldWidget) =>
      builder != oldWidget.builder;
}
