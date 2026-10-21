import 'dart:ui' as ui;
import 'package:flutter/scheduler.dart';
import 'p5d_frame_proof.dart';

bool p5dTimingMatches(
  Map<String, Object?>? proof,
  ui.FrameTiming timing,
  int epoch,
  int index,
) =>
    p5dProofMatches(proof, epoch, index, false) &&
    proof!['frameNumber'] == timing.frameNumber;

/// Release callback'i oturum boyunca açık kalır; her ölçüm gerçek paint frame'ine bağlanır.
class P5dFrameTimings {
  P5dFrameTimings() {
    SchedulerBinding.instance.addTimingsCallback(_record);
  }
  final _frames = <int, ui.FrameTiming>{};
  bool _closed = false;
  void _record(List<ui.FrameTiming> values) {
    for (final frame in values) {
      _frames[frame.frameNumber] = frame;
    }
    while (_frames.length > 512) {
      _frames.remove(_frames.keys.first);
    }
  }

  List<Map<String, int>> get recent => [
    for (final frame in _frames.values.toList().reversed.take(8))
      {
        'frameNumber': frame.frameNumber,
        'vsyncMicros': frame.timestampInMicroseconds(ui.FramePhase.vsyncStart),
      },
  ];

  Map<String, Object?> diagnostic(Map<String, Object?> proof) {
    final byNumber = _frames[proof['frameNumber']];
    ui.FrameTiming? nearest;
    final timestamp = proof['vsyncMicros'] as int;
    for (final candidate in _frames.values) {
      if (nearest == null ||
          (candidate.timestampInMicroseconds(ui.FramePhase.vsyncStart) -
                      timestamp)
                  .abs() <
              (nearest.timestampInMicroseconds(ui.FramePhase.vsyncStart) -
                      timestamp)
                  .abs()) {
        nearest = candidate;
      }
    }
    Map<String, int>? row(ui.FrameTiming? frame) => frame == null
        ? null
        : {
            'frameNumber': frame.frameNumber,
            'engineVsyncMicros': frame.timestampInMicroseconds(
              ui.FramePhase.vsyncStart,
            ),
            'frameworkVsyncDeltaMicros':
                frame.timestampInMicroseconds(ui.FramePhase.vsyncStart) -
                timestamp,
          };
    return {
      'callbackFramesRetained': _frames.length,
      'frameNumberCandidate': row(byNumber),
      'nearestTimestampCandidate': row(nearest),
    };
  }

  ui.FrameTiming? matching(Map<String, Object?> proof, int epoch, int index) {
    final frame = _frames[proof['frameNumber']];
    return frame != null && p5dTimingMatches(proof, frame, epoch, index)
        ? frame
        : null;
  }

  void close() {
    if (_closed) return;
    _closed = true;
    SchedulerBinding.instance.removeTimingsCallback(_record);
    _frames.clear();
  }
}
