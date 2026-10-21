import 'dart:async';
import 'dart:io';
import 'dart:ui' show FramePhase;
import 'package:flutter/scheduler.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../presentation/widgets/reading_image_comparison_raster.dart';
import '../domain/p5d_pins.dart';
import '../domain/p5d_repository.dart';
import 'p5d_readback.dart';
import 'p5d_frame_proof.dart';
import 'p5d_frame_timing.dart';
import 'p5d_state.dart';

class P5dCubit extends Cubit<P5dState> {
  P5dCubit(this.repository) : super(const P5dState());
  final P5dRepository repository;
  final _timings = P5dFrameTimings();
  ReadingComparisonCancellation? _cancel;
  Future<void>? _work;
  int _epoch = 0, _next = 0;
  bool _stopped = false;
  final samples = <Map<String, Object?>>[];
  Map<String, Object?>? viewport, frameProof, lastFailure;

  Future<void> initialize() async {
    final result = await repository.initialize();
    if (_stopped || isClosed) return;
    result.fold(
      (f) => refuse(f.code),
      (_) => emit(const P5dState(status: 'ready')),
    );
  }

  void refuse(String reason) {
    _epoch++;
    _cancel?.cancel();
    state.raster?.dispose();
    if (!isClosed) {
      emit(P5dState(status: 'error', error: reason, epoch: _epoch));
    }
  }

  Future<void> show(int index, int sample, String directory) {
    if (_stopped ||
        state.error != null ||
        _work != null ||
        index * 4 + sample != _next ||
        index < 0 ||
        index >= 12 ||
        sample < 0 ||
        sample > 3 ||
        !['ready', 'loaded'].contains(state.status)) {
      refuse('command_refused');
      return Future.error(StateError('command_refused'));
    }
    state.raster?.dispose();
    viewport = null;
    frameProof = null;
    final epoch = ++_epoch;
    _cancel = ReadingComparisonCancellation();
    emit(P5dState(status: 'loading', index: index, epoch: epoch));
    return _work = _load(
      index,
      sample,
      directory,
      epoch,
      _cancel!,
    ).whenComplete(() => _work = null);
  }

  Future<void> _load(
    int index,
    int sample,
    String directory,
    int epoch,
    ReadingComparisonCancellation cancellation,
  ) async {
    ReadingComparisonRaster? owned;
    try {
      final before = ProcessInfo.currentRss;
      final loaded = await _decode(index, cancellation);
      owned = loaded.$1;
      cancellation.check();
      final readback = await p5dReadback(
        owned.image!,
        directory,
        '${p5dPins[index]['caseId']}-$sample',
        cancellation,
        persist: sample == 3,
      );
      cancellation.check();
      if (_stopped || epoch != _epoch || isClosed) return;
      emit(
        P5dState(status: 'loaded', index: index, epoch: epoch, raster: owned),
      );
      owned = null;
      final clock = Stopwatch()..start();
      final deadline = DateTime.now().add(const Duration(seconds: 4));
      Map<String, Object?>? proof;
      FrameTiming? frame;
      do {
        cancellation.check();
        SchedulerBinding.instance.scheduleFrame();
        await SchedulerBinding.instance.endOfFrame;
        cancellation.check();
        if (proof == null && p5dProofMatches(frameProof, epoch, index, false)) {
          proof = Map<String, Object?>.of(frameProof!);
        }
        if (proof != null) frame = _timings.matching(proof, epoch, index);
        if (frame != null) break;
        await Future<void>.delayed(const Duration(milliseconds: 16));
      } while (DateTime.now().isBefore(deadline));
      if (viewport == null || proof == null) {
        throw StateError('paint_proof_missing');
      }
      final frames = <FrameTiming>[?frame];
      samples.add({
        'caseId': p5dPins[index]['caseId'],
        'index': index,
        'sample': sample,
        'warmup': sample == 0,
        ...loaded.$2,
        ...readback,
        'rssBeforeBytes': before,
        'rssLoadedBytes': ProcessInfo.currentRss,
        'frameWaitMicros': clock.elapsedMicroseconds,
        'frameTimingStatus': frame != null
            ? 'MEASURED'
            : (_timings.recent.isEmpty
                  ? 'NO_TIMING_CALLBACK'
                  : 'FRAME_MATCH_FAILED'),
        'epoch': epoch,
        'frameTimingCandidates': _timings.diagnostic(proof),
        'paintFrameNumber': proof['frameNumber'],
        'paintVsyncMicros': proof['vsyncMicros'],
        'viewport': viewport,
        'frames': [
          for (final f in frames)
            {
              'frameNumber': f.frameNumber,
              'vsyncMicros': f.timestampInMicroseconds(FramePhase.vsyncStart),
              'epoch': epoch,
              'frameworkVsyncDeltaMicros':
                  f.timestampInMicroseconds(FramePhase.vsyncStart) -
                  (proof['vsyncMicros'] as int),
              'buildMicros': f.buildDuration.inMicroseconds,
              'rasterMicros': f.rasterDuration.inMicroseconds,
              'totalSpanMicros': f.totalSpan.inMicroseconds,
            },
        ],
      });
      _next++;
    } catch (error) {
      lastFailure = p5dFailureDiagnostic(error);
      if (epoch == _epoch && !_stopped) {
        refuse('decode_readback_or_paint_refused');
      }
      rethrow;
    } finally {
      owned?.dispose();
    }
  }

  Future<(ReadingComparisonRaster, Map<String, Object?>)> _decode(
    int index,
    ReadingComparisonCancellation cancellation,
  ) async {
    final result = await repository.read(index);
    final encoded = result.fold((f) => throw StateError(f.code), (v) => v);
    cancellation.check();
    final pin = p5dPins[index];
    final watch = Stopwatch()..start();
    final raster = await ReadingComparisonRaster.decode(
      encoded.bytes,
      ReadingImageRaster(
        provider: 'offline',
        objectKey: pin['asset']! as String,
        generation: 'p5c',
        mime: 'image/${(pin['format']! as String).toLowerCase()}',
        byteSize: encoded.bytes.length,
        sha256: pin['sha256']! as String,
        width: 1712,
        height: 2778,
      ),
      1712,
      2778,
      p5dLimits,
      cancellation,
    );
    return (
      raster,
      {
        'assetReadMicros': encoded.readMicros,
        'checksumMicros': encoded.checksumMicros,
        'decodeMicros': watch.elapsedMicroseconds,
        'encodedSha256': pin['sha256'],
        'decodedWidth': 1712,
        'decodedHeight': 2778,
      },
    );
  }

  void recordFrameProof(Map<String, Object?> proof) {
    if (state.status == 'loaded' &&
        p5dProofMatches(proof, state.epoch, state.index, state.crop)) {
      frameProof = proof;
    }
  }

  void crop(bool value) {
    if (state.status != 'loaded' || _work != null) {
      throw StateError('crop_refused');
    }
    emit(state.copyWith(crop: value));
  }

  Future<void> release() async {
    if (_work != null) throw StateError('busy');
    state.raster?.dispose();
    emit(P5dState(status: 'ready', epoch: ++_epoch));
    if (samples.isNotEmpty) {
      samples.last['rssAfterBytes'] = ProcessInfo.currentRss;
    }
  }

  Future<void> stop() async {
    _stopped = true;
    _epoch++;
    _cancel?.cancel();
    state.raster?.dispose();
    if (!isClosed) emit(P5dState(status: 'stopped', epoch: _epoch));
    try {
      await _work;
    } catch (_) {}
    _timings.close();
  }

  Map<String, Object?> snapshot() => {
    'status': state.status,
    'error': state.error,
    'failure': lastFailure,
    'completedSamples': _next,
    'acceptedCases': samples.where((s) => s['sample'] == 3).length,
    'samples': samples,
    'frameProof': frameProof,
    'rssLoadedScope': 'after_readback_checksum_and_optional_file_write',
    'frameWaitScope':
        'bounded_wait_for_batched_release_FrameTiming_not_draw_duration',
    'rssKind': 'instantaneous_process_rss_bytes_not_peak_or_gpu',
    'modeledBufferBudgetBytes': 192 * 1024 * 1024,
    'processRssLimit': false,
    'physicalDisplayP3Acceptance': 'NOT_RUN',
    'humanQualityAcceptance': 'NOT_RUN',
    'httpTransferMeasured': false,
    'activeImages': ReadingComparisonRaster.activeImages,
    'activeBuffers': ReadingComparisonRaster.activeBuffers,
    'activeDescriptors': ReadingComparisonRaster.activeDescriptors,
    'activeCodecs': ReadingComparisonRaster.activeCodecs,
  };

  @override
  Future<void> close() async {
    await stop();
    await super.close();
  }
}

final p5dLimits = ReadingComparisonLimits(
  runId: 'p5d-offline',
  maxEncodedBytes: 32 * 1024 * 1024,
  maxBufferBytes: 96 * 1024 * 1024,
  maxDecodePixels: 4756000,
  maxDecodedBytes: 4756000 * 4,
  maxPeakBytes: 192 * 1024 * 1024,
  maxOwnedImages: 1,
  transferMillis: 30000,
  decodeMillis: 30000,
  statusEveryMillis: 1000,
  maxStatusChecks: 1,
  maxControlMillis: 30000,
);

Map<String, Object?> p5dFailureDiagnostic(Object error) => {
  'type': error.runtimeType.toString(),
  if (error is FileSystemException) 'osErrorCode': error.osError?.errorCode,
  if (error is FileSystemException) 'osMessage': error.osError?.message,
};
