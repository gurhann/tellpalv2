import 'dart:async';
import 'dart:io';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../features/reading_images/domain/entity/reading_image_comparison.dart';
import '../../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../../features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'assets.dart';
import 'pins.dart';

/// Ayrı QA kompozisyonu tek native işi ve raster sahipliğini tutar.
@Injectable(env: ['localLosslessQa'])
class LocalLosslessQaCubit extends Cubit<ReadingImageComparisonState> {
  LocalLosslessQaCubit({required this.assets})
    : super(const ReadingImageComparisonState());
  final LocalLosslessQaAssets assets;
  final List<Map<String, Object?>> _cases = [];
  ReadingComparisonCancellation? _cancellation;
  Future<void>? _work, _stopping;
  Timer? _deadline;
  Map<String, Object?>? _viewport;
  Object? _rssStop;
  bool _initialized = false, _visible = true, _stopped = false;
  String? _failure;
  int _epoch = 0;

  Future<void> initialize() async {
    try {
      await assets.initialize();
      if (_stopped || isClosed) return;
      _initialized = true;
      _deadline = Timer(
        const Duration(minutes: 3),
        () => refuse('qa_deadline'),
      );
      _emit('ready');
    } catch (_) {
      refuse('fixture_manifest_refused');
    }
  }

  void setViewport({
    required double width,
    required double height,
    required double dpr,
  }) {
    if (_stopped || _failure != null || isClosed) return;
    if (!width.isFinite ||
        !height.isFinite ||
        !dpr.isFinite ||
        dpr <= 0 ||
        width * dpr < 822 ||
        height * dpr < 1334) {
      refuse('viewport_refused');
      return;
    }
    final next = {
      'availableLogicalWidth': width,
      'availableLogicalHeight': height,
      'dpr': dpr,
    };
    if (_viewport != null && _viewport.toString() != next.toString()) {
      refuse('viewport_changed');
      return;
    }
    _viewport = next;
  }

  Future<void> show(int index) {
    if (!_initialized ||
        _viewport == null ||
        _stopped ||
        !_visible ||
        _failure != null ||
        _work != null ||
        index != _cases.length ||
        index < 0 ||
        index >= localLosslessCases.length ||
        _cases.any((item) => item['paintCompleted'] != true)) {
      return Future.error(StateError('command_refused'));
    }
    state.raster?.dispose();
    final epoch = ++_epoch;
    final cancellation = ReadingComparisonCancellation();
    _cancellation = cancellation;
    _emit('loading', index: index);
    return _work = _load(
      index,
      epoch,
      cancellation,
    ).whenComplete(() => _work = null);
  }

  Future<void> _load(
    int index,
    int epoch,
    ReadingComparisonCancellation cancellation,
  ) async {
    ReadingComparisonRaster? raster;
    try {
      final before = _rss();
      final decoded = await assets.load(index, cancellation);
      raster = decoded.raster;
      cancellation.check();
      if (!_owns(epoch)) return;
      _cases.add({
        ...decoded.measurement,
        'epoch': epoch,
        'paintCompleted': false,
        'rssBefore': before,
        'rssLoaded': _rss(),
      });
      emit(
        ReadingImageComparisonState(
          status: 'loaded',
          raster: raster,
          epoch: epoch,
          entryIndex: index,
          canonicalSource: localLosslessCases[index]['format'] == 'PNG',
        ),
      );
      raster = null;
    } catch (_) {
      if (_owns(epoch)) refuse('fixture_decode_refused');
    } finally {
      raster?.dispose();
    }
  }

  void markPainted(int epoch, Map<String, Object?> widget) {
    if (!_owns(epoch) ||
        state.status != 'loaded' ||
        _cases.isEmpty ||
        widget['physicalWidth'] != 822.0 ||
        widget['physicalHeight'] != 1334.0 ||
        widget['pixelAligned'] != true) {
      refuse('paint_refused');
      throw StateError('paint_refused');
    }
    _cases.last = {
      ..._cases.last,
      'paintCompleted': true,
      'paintedEpoch': epoch,
      'widget': Map<String, Object?>.unmodifiable(widget),
    };
  }

  bool _owns(int epoch) =>
      !isClosed && !_stopped && _visible && _failure == null && epoch == _epoch;

  void visibility(bool visible) {
    _visible = visible;
    if (!visible) refuse('background');
  }

  void refuse(String reason) {
    _failure ??= reason;
    _epoch++;
    _cancellation?.cancel();
    state.raster?.dispose();
    _emit('error');
  }

  void _emit(String status, {int? index}) {
    if (isClosed) return;
    emit(
      ReadingImageComparisonState(
        status: status,
        error: _failure,
        entryIndex: index ?? state.entryIndex,
        epoch: _epoch,
      ),
    );
  }

  Future<void> stop() => _stopping ??= _stop();
  Future<void> _stop() async {
    _stopped = true;
    _deadline?.cancel();
    _epoch++;
    _cancellation?.cancel();
    state.raster?.dispose();
    _emit('stopped');
    await _work;
    _rssStop = _rss();
  }

  Map<String, Object?> snapshot() => {
    'ok': _failure == null,
    'status': state.status,
    'error': _failure,
    'caseCount': 6,
    'caseIndex': state.entryIndex,
    'epoch': state.epoch,
    'paintedEpoch': _cases.isEmpty ? null : _cases.last['paintedEpoch'],
    'ready': _initialized && _viewport != null,
    'stopped': _stopped,
    'completedCases': _cases
        .where((item) => item['paintCompleted'] == true)
        .length,
    'cases': List<Map<String, Object?>>.unmodifiable(_cases),
    'allCasesPassed':
        _failure == null &&
        _cases.length == 6 &&
        _cases.every((item) => item['paintCompleted'] == true),
    'rssStop': _rssStop,
    'rssKind': 'instantaneous_process_rss_bytes',
    'rssLoadedScope': 'after_rgb_readback_and_checksum_includes_validation_overhead',
    'activeBuffers': ReadingComparisonRaster.activeBuffers,
    'activeDescriptors': ReadingComparisonRaster.activeDescriptors,
    'activeCodecs': ReadingComparisonRaster.activeCodecs,
    'activeImages': ReadingComparisonRaster.activeImages,
  };

  static Map<String, Object?> _rss() {
    try {
      return {'available': true, 'bytes': ProcessInfo.currentRss};
    } catch (_) {
      return {'available': false};
    }
  }

  @override
  Future<void> close() async {
    await stop();
    await super.close();
  }
}
