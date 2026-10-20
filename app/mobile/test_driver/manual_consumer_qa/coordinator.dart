import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:injectable/injectable.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_qa/manual_qa_policy.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import '../reading_consumer_qa_probe.dart';
import 'private_protocol.dart';
part 'coordinator.freezed.dart';

@freezed
abstract class ManualConsumerQaState with _$ManualConsumerQaState {
  const factory ManualConsumerQaState({
    @Default('baseline') String stage,
    @Default(0) int revision,
  }) = _ManualConsumerQaState;
}

/// Tek başlatma ve sıralı receipt kapısı; medya işi mevcut probe/Cubit'tedir.
@injectable
class ManualConsumerQaCoordinator extends Cubit<ManualConsumerQaState>
    implements ManualConsumerQaPolicy {
  ManualConsumerQaCoordinator(
    @factoryParam this.protocol,
    @factoryParam ReadingConsumerQaProbe? suppliedProbe,
  ) : super(const ManualConsumerQaState()) {
    probe =
        suppliedProbe ??
        ReadingConsumerQaProbe(
          expectedScope: manualScope,
          mount: mountApp,
          onOverflow: () => unawaited(fail()),
        );
  }
  final ManualQaPrivateProtocol protocol;
  late final ReadingConsumerQaProbe probe;
  Widget? app;
  Map<String, dynamic>? _configuration;
  Timer? _poll, _deadline;
  Future<void> _tail = Future.value();
  bool _busy = false, _started = false, _terminal = false;
  int _paintCount = 0;
  int? _lastEpoch;
  String? _expectedAction;
  String? _captureReceiptSha;
  Future<void>? _failing;

  void mountApp(Widget widget) {
    app = widget;
    _emit();
  }

  void _emit([String? stage]) {
    if (!isClosed) {
      emit(
        state.copyWith(
          stage: stage ?? state.stage,
          revision: state.revision + 1,
        ),
      );
    }
  }

  Future<void> initialize() async {
    try {
      await protocol.initialize();
      protocol.diagnostics.emit(
        ManualQaDiagnosticStage.baseline,
        ManualQaDiagnosticCode.started,
      );
      await protocol.receipt('baseline', probe.snapshot());
      protocol.diagnostics.emit(
        ManualQaDiagnosticStage.baseline,
        ManualQaDiagnosticCode.complete,
      );
      _emit('waiting');
      _poll = Timer.periodic(const Duration(milliseconds: 50), (_) => _tick());
    } catch (_) {
      protocol.diagnostics.failed();
      await fail();
    }
  }

  void _tick() {
    if (_busy || _terminal) return;
    _busy = true;
    _tail = _tail.then((_) async {
      try {
        await protocol.validateCaptures(
          pendingPaint: state.stage == 'capturePending' ? _paintCount : null,
        );
        if (!_started && _configuration == null) {
          _configuration = await protocol.consume();
          if (_configuration != null) {
            final deadline =
                _configuration!['windowDeadlineEpochMillis'] as int;
            _deadline = Timer(
              Duration(
                milliseconds: deadline - DateTime.now().millisecondsSinceEpoch,
              ),
              () => unawaited(fail()),
            );
            _emit('ready');
          }
        } else if (_started && state.stage == 'loading') {
          final sample = probe.snapshot();
          manualRequire(sample['ok'] == true && sample['status'] != 'error');
          if (sample['status'] == 'loaded' &&
              sample['liveRasterReadyForPaint'] == true &&
              sample['currentEpoch'] != _lastEpoch) {
            manualRequire(
              _paintCount < 4 &&
                  sample['currentEpoch'] is int &&
                  (_lastEpoch == null ||
                      (sample['currentEpoch'] as int) > _lastEpoch!),
            );
            // loaded bildirimi build'den önce gelebilir; gerçek slot önce frame'e bağlanır.
            WidgetsBinding.instance.scheduleFrame();
            await WidgetsBinding.instance.endOfFrame;
            manualRequire(!_terminal);
            final painted = await probe.painted(sample['currentEpoch'] as int);
            manualRequire(!_terminal);
            await protocol.receipt('paint${_paintCount + 1}', painted);
            _lastEpoch = sample['currentEpoch'] as int;
            _paintCount++;
            _captureReceiptSha = protocol.previousSha;
            _emit('capturePending');
          }
        } else if (_started && state.stage == 'capturePending') {
          manualRequire(
            probe.snapshot()['status'] == 'loaded' &&
                probe.snapshot()['currentEpoch'] == _lastEpoch,
          );
          if (await protocol.capture(
            _paintCount,
            _lastEpoch!,
            _captureReceiptSha!,
          )) {
            manualRequire(!_terminal);
            _expectedAction = switch (_paintCount) {
              1 => 'page1',
              2 => 'page2',
              3 => 'detail',
              _ => null,
            };
            if (_paintCount == 4) {
              await protocol.receipt('beforeBackground', probe.snapshot());
              manualRequire(!_terminal);
              _emit('beforeBackground');
            } else {
              _emit('painted');
            }
          }
        } else if (_started &&
            ![
              'background',
              'resumed',
              'closing',
              'closedUi',
              'stopped',
            ].contains(state.stage)) {
          if (state.stage != 'beforeBackground' ||
              WidgetsBinding.instance.lifecycleState ==
                  AppLifecycleState.resumed) {
            manualRequire(probe.snapshot()['status'] != 'error');
          }
        }
      } catch (_) {
        await fail();
      } finally {
        _busy = false;
      }
    });
  }

  Future<void> start() async {
    if (_started || state.stage != 'ready' || _configuration == null) {
      await fail();
      return;
    }
    _started = true;
    final configuration = _configuration!;
    _configuration = null;
    _emit('loading');
    try {
      final result = jsonDecode(await probe.handle(jsonEncode(configuration)));
      configuration.clear();
      manualRequire(result['ok'] == true && !_terminal);
    } catch (_) {
      configuration.clear();
      await fail();
    }
  }

  @override
  String get instruction => switch (state.stage) {
    'ready' => 'Hazır. Başlat düğmesine bir kez dokunun.',
    'loading' => 'Görselin çizilmesini bekleyin.',
    'capturePending' => 'Ekran kaydı bekleniyor. Ekranda kalın.',
    'painted' => switch (_expectedAction) {
      'page1' => 'QA sayfalarını aç düğmesine dokunun.',
      'page2' => 'Sonraki düğmesine dokunun.',
      _ => 'QA detail ekranına dön düğmesine dokunun.',
    },
    'beforeBackground' => 'Ekran kaydı tamam. Şimdi Home ile arka plana alın.',
    'background' => 'Aynı uygulamaya geri dönün.',
    'resumed' => 'Geri dönüş kaydedildi. QA akışını kapatın.',
    'stopped' => 'Ölçüm kapandı.',
    'failed' => 'Ölçüm durdu. Tekrar başlatmayın.',
    _ => 'QA hazırlığı bekleniyor.',
  };

  @override
  bool allows(String action) =>
      !_terminal &&
      ((state.stage == 'painted' && _expectedAction == action) ||
          (state.stage == 'resumed' && action == 'close'));
  @override
  bool accept(String action) {
    if (!allows(action)) {
      unawaited(fail());
      return false;
    }
    _expectedAction = null;
    _emit(action == 'close' ? 'closing' : 'loading');
    return true;
  }

  @override
  void lifecycle(AppLifecycleState lifecycle) {
    if (_terminal || !_started) return;
    if (lifecycle == AppLifecycleState.inactive &&
        ['beforeBackground', 'background'].contains(state.stage)) {
      return;
    }
    _tail = _tail.then((_) async {
      try {
        if (lifecycle == AppLifecycleState.hidden ||
            lifecycle == AppLifecycleState.paused) {
          if (state.stage == 'background') return;
          manualRequire(state.stage == 'beforeBackground');
          await _drain();
          await protocol.receipt('background', probe.snapshot());
          _emit('background');
        } else if (lifecycle == AppLifecycleState.resumed) {
          manualRequire(state.stage == 'background');
          await _drain();
          await protocol.receipt('resumed', probe.snapshot());
          _emit('resumed');
        } else {
          await fail();
        }
      } catch (_) {
        await fail();
      }
    });
  }

  Future<void> _drain() async {
    if (comparisonGetIt.isRegistered<ReadingComparisonWorkGate>()) {
      await comparisonGetIt<ReadingComparisonWorkGate>().run(() async {});
    }
    final snapshot = probe.snapshot();
    manualRequire(
      [
        'activeBuffers',
        'activeDescriptors',
        'activeCodecs',
        'activeImages',
      ].every((key) => snapshot[key] == 0),
    );
    final observation = snapshot['resourceObservation'] as Map;
    manualRequire(
      observation['reservedSlots'] == 0 &&
          observation['reservedDecodedBytes'] == '0',
    );
  }

  @override
  Future<void> closed() async {
    try {
      manualRequire(state.stage == 'closing' && !_terminal);
      await _drain();
      await protocol.receipt('closedUi', probe.snapshot());
      _emit('closedUi');
      await probe.stop();
      await _drain();
      await protocol.cleanup();
      manualRequire(!_terminal);
      await protocol.receipt('stopped', probe.snapshot());
      _terminal = true;
      app = null;
      _poll?.cancel();
      _deadline?.cancel();
      _emit('stopped');
    } catch (_) {
      await fail();
    }
  }

  Future<void> fail() =>
      state.stage == 'stopped' ? Future.value() : (_failing ??= _fail());
  Future<void> _fail() async {
    _terminal = true;
    _configuration?.clear();
    _configuration = null;
    _poll?.cancel();
    _deadline?.cancel();
    try {
      await probe.stop();
    } catch (_) {
      // Kapanış hatası terminaldir; cleanup yine bağımsız çalışır.
    } finally {
      try {
        await protocol.failure();
      } catch (_) {
        // Yazılamayan failure marker host için de başarı kanıtı olamaz.
      } finally {
        try {
          await protocol.cleanup();
        } catch (_) {
          // stopped üretilmez; host bağımsız cleanup sonucunu kaydeder.
        } finally {
          app = null;
          _emit('failed');
        }
      }
    }
  }

  @override
  Future<void> close() async {
    await fail();
    await _tail;
    return super.close();
  }
}
