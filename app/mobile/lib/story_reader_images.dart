import 'package:flutter/widgets.dart';
import 'story_manifest.dart';

/// Optional owned media seam; ordinary reader requests keep their existing path.
abstract interface class StoryReaderImages {
  Future<ImageProvider> prepare(StoryPage page);
  Widget display(BuildContext context, StoryPage page);
  void observeLayout(StoryReaderGeometry geometry);
  void selected(StoryPage page, int generation);
}

class StoryReaderGeometry {
  const StoryReaderGeometry({
    required this.slot,
    required this.dpr,
    required this.textScale,
    required this.padding,
    required this.viewInsets,
  });
  final Size slot;
  final double dpr, textScale;
  final EdgeInsets padding, viewInsets;
}

/// Selection still runs through the real reader's generation and audio guards.
class StoryReaderController {
  void Function(int)? _select;
  void selectPage(int index) => _select?.call(index);
  void attach(void Function(int) select) => _select = select;
  void detach() => _select = null;
}
