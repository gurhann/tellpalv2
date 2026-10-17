import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/reading_image_comparison_configuration.dart';
import '../../domain/entity/reading_image_comparison.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/repository/reading_image_comparison_repository.dart';
import '../../domain/use_case/load_reading_image_comparison.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../../domain/use_case/validate_reading_image_manifest.dart';
import '../widgets/reading_image_comparison_raster.dart';
import 'reading_image_comparison_state.dart';

@injectable
class ReadingImageComparisonCubit extends Cubit<ReadingImageComparisonState> {
  ReadingImageComparisonCubit(
    this.load,
    this.repository,
    this.configuration,
    this.workGate,
    this.clock,
  ) : super(const ReadingImageComparisonState()) {
    _account = configuration.account?.changes.listen((_) {
      retire('account_changed');
    });
  }
  final ReadingComparisonWorkGate workGate;
  final ReadingComparisonClock clock;
  final LoadReadingImageComparison load;
  final ReadingImageComparisonRepository repository;
  final ReadingImageComparisonConfiguration configuration;
  StreamSubscription<void>? _account;
  ReadingComparisonCancellation? _cancellation, _statusCancellation;
  Future<void> _tail = Future.value();
  Timer? _poll, _lease, _control;
  int _epoch = 0, _checks = 0;
  bool _checking = false, _visible = true;
  ReadingImageComparison? _admission;
  ReadingImageIdentity? _anchor;
  int? _anchorGeneration, _routeContentId;
  String? _slot;

  void retire(String reason) {
    _epoch++;
    _slot = null;
    _admission = null;
    _cancellation?.cancel();
    _statusCancellation?.cancel();
    _poll?.cancel();
    _lease?.cancel();
    _control?.cancel();
    state.raster?.dispose();
    if (!isClosed) {
      emit(
        ReadingImageComparisonState(
          status: 'error',
          entryIndex: state.entryIndex,
          canonicalSource: state.canonicalSource,
          error: reason,
          epoch: _epoch,
        ),
      );
    }
  }

  void visibility(bool visible) {
    _visible = visible;
    if (!visible) {
      retire('background');
    } else {
      retire('readmission_required');
    }
  }

  void select({
    required int id,
    required int entryIndex,
    required bool canonicalSource,
    required double width,
    required double height,
    required double dpr,
    required ReadingImageFit fit,
  }) {
    if (!_visible || isClosed) return;
    if (configuration.mediaEnabled) {
      if ((_routeContentId != null && _routeContentId != id) ||
          (_admission != null &&
              _admission!.manifest.identity.contentId != id)) {
        retire('content_changed');
        return;
      }
      _routeContentId ??= id;
    }
    final slot = '$id/$entryIndex/$canonicalSource/$width/$height/$dpr/$fit';
    if (slot == _slot) return;
    _slot = slot;
    final epoch = ++_epoch;
    _cancellation?.cancel();
    state.raster?.dispose();
    emit(
      ReadingImageComparisonState(
        status: configuration.mediaEnabled ? 'loading' : 'empty',
        entryIndex: entryIndex,
        canonicalSource: canonicalSource,
        epoch: epoch,
      ),
    );
    if (!configuration.mediaEnabled) return;
    final cancellation = ReadingComparisonCancellation();
    _cancellation = cancellation;
    // Yeni iş, iptal edilmiş codec/HTTP Future'ı gerçekten sonlanınca başlar.
    _tail = workGate.run(
      () => _render(
        epoch,
        cancellation,
        id,
        entryIndex,
        canonicalSource,
        width,
        height,
        dpr,
        fit,
      ),
    );
  }

  bool _owns(int epoch) => !isClosed && _visible && epoch == _epoch;
  Future<void> _render(
    int epoch,
    ReadingComparisonCancellation cancellation,
    int id,
    int index,
    bool canonical,
    double width,
    double height,
    double dpr,
    ReadingImageFit fit,
  ) async {
    ReadingComparisonRaster? raster;
    Timer? decodeDeadline;
    try {
      if (!_owns(epoch)) return;
      configuration.account!.token();
      if (_admission == null) {
        final result = await load(id, cancellation);
        cancellation.check();
        if (!_owns(epoch)) return;
        final fresh = result.fold((f) => throw StateError(f.code), (a) => a);
        if (fresh.manifest.identity.contentId != id ||
            fresh.manifest.identity.language != 'tr' ||
            fresh.liveGeneration <= 0 ||
            fresh.leaseMillis <= 0 ||
            fresh.leaseMillis > ReadingComparisonLimits.maxSupportedMillis) {
          throw StateError('identity');
        }
        if (_anchor != null &&
            (_anchor != fresh.manifest.identity ||
                _anchorGeneration != fresh.liveGeneration)) {
          throw StateError('live_anchor_changed');
        }
        _anchor ??= fresh.manifest.identity;
        _anchorGeneration ??= fresh.liveGeneration;
        _admission = fresh;
        _controls();
      }
      final admission = _admission!;
      if (index < 0 || index >= admission.manifest.images.length) {
        throw StateError('binding');
      }
      final entry = admission.manifest.images[index];
      final validated = const ValidateReadingImageManifest()(
        manifest: admission.manifest,
        expected: admission.context,
      ).fold((f) => throw StateError(f.name), (m) => m);
      final selected = const SelectReadingImageRendition()(
        manifest: validated,
        binding: entry.source.binding,
        acceptedProfileFingerprints: admission
            .context
            .profiles[entry.source.binding.role]!
            .map((p) => p.fingerprint)
            .toSet(),
        logicalWidth: width,
        logicalHeight: height,
        devicePixelRatio: dpr,
        fit: fit,
        maxDecodePixels: configuration.limits!.maxDecodePixels,
      ).fold((f) => throw StateError(f.name), (s) => s);
      final bytes = (await repository.bytes(
        admission,
        entry.source.binding,
        selected.rendition,
        canonical,
        cancellation,
      )).fold((f) => throw StateError(f.code), (b) => b);
      cancellation.check();
      if (!_owns(epoch)) return;
      final metadata = canonical
          ? entry.source.raster
          : selected.rendition.raster;
      decodeDeadline = clock.once(
        Duration(milliseconds: configuration.limits!.decodeMillis),
        () {
          if (_owns(epoch)) {
            retire('decode_deadline');
          }
        },
      );
      raster = await ReadingComparisonRaster.decode(
        bytes,
        metadata,
        canonical ? metadata.width : selected.geometry.decodeWidth,
        canonical ? metadata.height : selected.geometry.decodeHeight,
        configuration.limits!,
        cancellation,
      );
      cancellation.check();
      if (!_owns(epoch)) return;
      configuration.account!.token();
      emit(
        ReadingImageComparisonState(
          status: 'loaded',
          comparison: admission,
          raster: raster,
          geometry: selected.geometry,
          entryIndex: index,
          canonicalSource: canonical,
          epoch: epoch,
        ),
      );
      raster = null;
    } catch (_) {
      if (_owns(epoch)) retire('media_refused');
    } finally {
      decodeDeadline?.cancel();
      raster?.dispose();
    }
  }

  void _controls() {
    final limits = configuration.limits!;
    _checks = 0;
    final credentialMillis = configuration.account!.credentials.expiresAt
        .difference(clock.now())
        .inMilliseconds;
    final leaseMillis = credentialMillis < _admission!.leaseMillis
        ? credentialMillis
        : _admission!.leaseMillis;
    if (leaseMillis <= 0) {
      retire('credential_expired');
      return;
    }
    _lease = clock.once(
      Duration(milliseconds: leaseMillis),
      () => retire('lease_expired'),
    );
    _control = clock.once(
      Duration(milliseconds: limits.maxControlMillis),
      () => retire('control_duration'),
    );
    _poll = clock.periodic(
      Duration(milliseconds: limits.statusEveryMillis),
      (_) => _check(),
    );
  }

  Future<void> _check() async {
    if (_checking || _admission == null || !_visible) return;
    if (++_checks > configuration.limits!.maxStatusChecks) {
      retire('control_count');
      return;
    }
    _checking = true;
    final admission = _admission!;
    final epoch = _epoch;
    final cancellation = ReadingComparisonCancellation();
    _statusCancellation = cancellation;
    try {
      final result = await repository.status(admission, cancellation);
      if (!_owns(epoch)) return;
      result.fold((_) => retire('status_refused'), (fresh) {
        final valid = const ValidateReadingImageManifest()(
          manifest: fresh.manifest,
          expected: fresh.context,
        ).isRight();
        if (!valid ||
            fresh.liveGeneration != _anchorGeneration ||
            fresh.manifest.identity != _anchor ||
            fresh.liveGeneration != admission.liveGeneration ||
            fresh.manifest.identity != admission.manifest.identity) {
          retire('live_changed');
        }
      });
    } finally {
      _checking = false;
    }
  }

  void changeChoice(int index, bool canonical) {
    _epoch++;
    _slot = null;
    _cancellation?.cancel();
    state.raster?.dispose();
    emit(
      ReadingImageComparisonState(
        status: 'initial',
        entryIndex: index,
        canonicalSource: canonical,
        epoch: _epoch,
      ),
    );
  }

  void retry() {
    final index = state.entryIndex, canonical = state.canonicalSource;
    retire('readmission_required');
    emit(
      ReadingImageComparisonState(
        entryIndex: index,
        canonicalSource: canonical,
        epoch: _epoch,
      ),
    );
  }

  @override
  Future<void> close() async {
    retire('closed');
    await _account?.cancel();
    await _tail;
    await super.close();
  }
}

/// Tüm route örneklerinde eski native iş bitmeden yeni kapasite açılmaz.
@lazySingleton
class ReadingComparisonWorkGate {
  Future<void> _tail = Future.value();
  Future<void> run(Future<void> Function() work) =>
      _tail = _tail.catchError((Object _) {}).then((_) => work());
}

/// Test saati süre sınırlarını değiştirmeden zamanlayıcı olaylarını denetler.
@lazySingleton
class ReadingComparisonClock {
  const ReadingComparisonClock();
  DateTime now() => DateTime.now();
  Timer once(Duration duration, void Function() callback) =>
      Timer(duration, callback);
  Timer periodic(Duration duration, void Function(Timer) callback) =>
      Timer.periodic(duration, callback);
}
