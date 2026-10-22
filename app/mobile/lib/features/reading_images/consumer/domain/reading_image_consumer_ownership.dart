import 'dart:async';
import 'package:injectable/injectable.dart';
import 'reading_image_admission.dart';

/// Route/Cubit değişse de gerçek cleanup bitmeden yeni medya sahibi açılmaz.
@lazySingleton
class ReadingImageConsumerOwnership {
  ReadingImageConsumerPermit? _owner;
  int get activeOwners => _owner == null ? 0 : 1;
  Future<ReadingImageConsumerPermit> acquire({
    required ReadingImageCancellation cancellation,
    required Duration wait,
  }) async {
    final elapsed = Stopwatch()..start();
    final cancelled = Completer<void>();
    void cancel() {
      if (!cancelled.isCompleted) cancelled.complete();
    }

    cancellation.add(cancel);
    try {
      while (_owner != null) {
        cancellation.check();
        final remaining = wait - elapsed.elapsed;
        if (remaining <= Duration.zero) {
          throw TimeoutException('consumer_capacity_wait');
        }
        await Future.any([
          _owner!._released.future,
          cancelled.future,
        ]).timeout(remaining);
      }
      cancellation.check();
      return _owner = ReadingImageConsumerPermit._(this);
    } finally {
      cancellation.remove(cancel);
    }
  }
}

class ReadingImageConsumerPermit {
  ReadingImageConsumerPermit._(this._gate);
  final ReadingImageConsumerOwnership _gate;
  final _released = Completer<void>();
  void release() {
    if (identical(_gate._owner, this)) {
      _gate._owner = null;
      _released.complete();
    }
  }
}
