import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import '../../test_driver/manual_consumer_qa/coordinator.dart';
import '../../test_driver/manual_consumer_qa/private_protocol.dart';
import '../../test_driver/reading_consumer_qa_probe.dart';
import 'reading_p4b5_qa_probe_test.dart' show p4b5Config;
import 'reading_consumer_qa_probe_test.dart' show identityQuery, mediaQuery;

class _Protocol extends ManualQaPrivateProtocol {
  _Protocol() : super(Directory('/unused-synthetic'));
  final phases = <String>[];
  int cleanups = 0, failures = 0;
  bool delivered = false;
  bool acknowledge = true;
  @override
  Future<void> validateCaptures({int? pendingPaint}) async {}
  @override
  Future<bool> capture(int paint, int epoch, String receiptSha) async =>
      acknowledge;
  @override
  Future<void> initialize() async {}
  @override
  Future<Map<String, dynamic>?> consume() async {
    if (delivered) return null;
    delivered = true;
    return p4b5Config(12345, manualScope);
  }

  @override
  Future<void> receipt(String phase, Map<String, Object?> sample) async {
    expectSync(phase, manualPhases[phases.length]);
    phases.add(phase);
  }

  @override
  Future<void> cleanup() async {
    cleanups++;
  }

  @override
  Future<void> failure() async {
    failures++;
  }
}

/// FSM testinde transport yerine sınır gözlemi verilir; gerçek probe testleri ayrıca korunur.
class _Probe extends ReadingConsumerQaProbe {
  _Probe() : super(expectedScope: manualScope);
  int configurations = 0, stops = 0, epoch = 0;
  String status = 'loaded';
  bool configured = false;
  @override
  Map<String, Object?> snapshot() => {
    'ok': true,
    'configured': configured,
    'status': status,
    'liveRasterReadyForPaint': true,
    'currentEpoch': epoch,
    'activeBuffers': 0,
    'activeDescriptors': 0,
    'activeCodecs': 0,
    'activeImages': 0,
    'resourceObservation': {'reservedSlots': 0, 'reservedDecodedBytes': '0'},
  };
  @override
  Future<String> handle(String? message) async {
    configurations++;
    configured = true;
    return jsonEncode({'ok': true});
  }

  @override
  Future<Map<String, Object?>> painted(int expectedEpoch) async {
    expectSync(expectedEpoch, epoch);
    return {...snapshot(), 'paintedEpoch': epoch};
  }

  @override
  Future<void> stop() async {
    stops++;
  }
}

Future<void> tick(WidgetTester tester) async {
  await tester.pump(const Duration(milliseconds: 60));
  await tester.pump();
}

void main() {
  testWidgets(
    'tek Başlat ve dört çizimden sonra background resume close stop sırası',
    (tester) async {
      final protocol = _Protocol();
      final probe = _Probe();
      final coordinator = ManualConsumerQaCoordinator(protocol, probe);
      await coordinator.initialize();
      await tick(tester);
      expect(coordinator.state.stage, 'ready');
      expect(probe.configurations, 0);
      await coordinator.start();
      await tick(tester);
      await tick(tester);
      expect(
        coordinator.state.stage,
        'painted',
        reason: protocol.phases.toString(),
      );
      expect(coordinator.allows('page1'), true);
      for (final action in ['page1', 'page2', 'detail']) {
        expect(coordinator.accept(action), true);
        probe.epoch++;
        await tick(tester);
        await tick(tester);
      }
      expect(protocol.phases, manualPhases.take(6));
      expect(coordinator.allows('close'), false);
      coordinator.lifecycle(AppLifecycleState.hidden);
      await tester.pump();
      coordinator.lifecycle(AppLifecycleState.paused);
      await tester.pump();
      coordinator.lifecycle(AppLifecycleState.resumed);
      await tester.pump();
      expect(coordinator.accept('close'), true);
      await coordinator.closed();
      expect(protocol.phases, manualPhases);
      expect(coordinator.state.stage, 'stopped');
      expect(probe.configurations, 1);
      expect(protocol.failures, 0);
      await coordinator.close();
    },
  );

  testWidgets('erken veya ikinci Başlat yeni configure açmadan terminaldir', (
    tester,
  ) async {
    for (final early in [true, false]) {
      final protocol = _Protocol();
      final probe = _Probe();
      final coordinator = ManualConsumerQaCoordinator(protocol, probe);
      await coordinator.initialize();
      if (!early) {
        await tick(tester);
        await coordinator.start();
      }
      await coordinator.start();
      expect(coordinator.state.stage, 'failed');
      expect(probe.configurations, early ? 0 : 1);
      expect(protocol.failures, 1);
      await coordinator.close();
    }
  });

  testWidgets('yanlış gezinme ve erken arka plan yeni admission açmaz', (
    tester,
  ) async {
    for (final background in [false, true]) {
      final protocol = _Protocol();
      final probe = _Probe();
      final coordinator = ManualConsumerQaCoordinator(protocol, probe);
      await coordinator.initialize();
      await tick(tester);
      await coordinator.start();
      if (background) {
        coordinator.lifecycle(AppLifecycleState.paused);
      } else {
        expect(coordinator.accept('page2'), false);
      }
      await tester.pump();
      expect(coordinator.state.stage, 'failed');
      expect(probe.configurations, 1);
      await coordinator.close();
    }
  });

  testWidgets('aynı epoch ikinci çizim sayılmaz ve medya hatası terminaldir', (
    tester,
  ) async {
    final protocol = _Protocol();
    final probe = _Probe();
    final coordinator = ManualConsumerQaCoordinator(protocol, probe);
    await coordinator.initialize();
    await tick(tester);
    await coordinator.start();
    await tick(tester);
    await tick(tester);
    expect(coordinator.accept('page1'), true);
    await tick(tester);
    expect(protocol.phases, ['baseline', 'paint1']);
    probe.status = 'error';
    await tick(tester);
    expect(coordinator.state.stage, 'failed');
    expect(probe.configurations, 1);
    await coordinator.close();
  });

  testWidgets('128 olay taşması sessiz kayıt silmek yerine terminaldir', (
    tester,
  ) async {
    var overflow = 0;
    final probe = ReadingConsumerQaProbe(
      expectedScope: manualScope,
      mount: (_) {},
      onOverflow: () => overflow++,
    );
    for (var i = 0; i < 129; i++) {
      probe.record({'kind': 'synthetic', 'ordinal': i});
    }
    await tester.pump();
    expect(probe.snapshot()['ok'], false);
    expect((probe.snapshot()['events'] as List).length, 128);
    expect(overflow, 1);
    await probe.stop();
  });

  testWidgets('120 saniyelik pencere beklerken uzamaz ve cleanup terminaldir', (
    tester,
  ) async {
    final protocol = _Protocol();
    final probe = _Probe();
    final coordinator = ManualConsumerQaCoordinator(protocol, probe);
    await coordinator.initialize();
    await tick(tester);
    await tester.pump(const Duration(milliseconds: 120001));
    await tester.pump();
    expect(coordinator.state.stage, 'failed');
    expect(probe.configurations, 0);
    expect(protocol.failures, 1);
    expect(protocol.cleanups, 1);
    await coordinator.start();
    expect(probe.configurations, 0);
    await coordinator.close();
  });

  test('manual kimlik token pencere ve beşinci medya HTTP öncesi kesilir', () {
    final valid = p4b5Config(12345, manualScope);
    final configuration = parseConsumerQaConfiguration(
      valid,
      expectedScope: manualScope,
    );
    for (final invalid in [
      {...valid, 'measurementId': 'old'},
      {...valid, 'proposalSha256': 'wrong'},
      {...valid, 'token': 'wrong'},
      {...valid, 'windowDeadlineEpochMillis': 1},
    ]) {
      expect(
        () => parseConsumerQaConfiguration(invalid, expectedScope: manualScope),
        throwsA(anything),
      );
    }
    final events = <Map<String, Object?>>[];
    final source = ObservedConsumerQaSource(
      configuration,
      events.add,
      expectedScope: manualScope,
    );
    source.reserve('manifest', {'contentId': '94', 'language': 'tr'});
    for (var i = 0; i < 4; i++) {
      source.reserve('bytes', mediaQuery(i));
    }
    for (var i = 0; i < 12; i++) {
      source.reserve('status', identityQuery());
    }
    expect(events.length, 17);
    expect(() => source.reserve('bytes', mediaQuery(0)), throwsA(anything));
    expect(events.length, 17);
    configuration.account!.credentials.revoke();
  });
}
