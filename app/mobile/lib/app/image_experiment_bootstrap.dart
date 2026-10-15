import 'dart:async';
import 'package:flutter/services.dart';
import 'image_experiment_configuration.dart';

class ImageExperimentBootstrap {
  ImageExperimentBootstrap({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel('tellpal/image_experiment');
  final MethodChannel _channel;
  bool _taken = false;
  bool _revoked = false;
  bool _diagnosticReported = false;
  void reportRefusal(ImageExperimentFixtureStage stage) {
    if (_diagnosticReported) return;
    _diagnosticReported = true;
    // Diagnostic failure never delays or changes the original revoke/fallback path.
    unawaited(
      _channel
          .invokeMethod<void>('reportDiagnostic', stage.code)
          .timeout(const Duration(milliseconds: 200))
          .catchError((Object _) {}),
    );
  }

  Future<ImageExperimentConfiguration> takeSession() async {
    if (_taken || _revoked) {
      reportRefusal(ImageExperimentFixtureStage.dartNativeHandoff);
      throw const FormatException('bootstrap_refused');
    }
    _taken = true;
    var stage = ImageExperimentFixtureStage.dartNativeHandoff;
    try {
      final response = await _channel
          .invokeMapMethod<String, dynamic>('takeSession')
          .timeout(const Duration(seconds: 30));
      stage = ImageExperimentFixtureStage.dartNativeResponse;
      if (_revoked ||
          response == null ||
          response.keys.toSet().difference({
            'envelope',
            'metadata',
          }).isNotEmpty) {
        throw const FormatException('bootstrap_refused');
      }
      final raw = response.remove('envelope') as String;
      final metadata = Map<String, dynamic>.from(response['metadata'] as Map);
      stage = ImageExperimentFixtureStage.dartConfiguration;
      return ImageExperimentConfiguration.parse(
        raw,
        metadata,
        observeRefusal: reportRefusal,
      );
    } catch (_) {
      reportRefusal(stage);
      await revoke();
      throw const FormatException('bootstrap_refused');
    }
  }

  Future<bool> clearQualityBinding() async {
    try {
      return await _channel
              .invokeMethod<bool>('clearQualityBinding')
              .timeout(const Duration(milliseconds: 500)) ??
          false;
    } catch (_) {
      return false;
    }
  }

  Future<bool> publishQualityBinding(Map<String, Object?> value) async {
    if (_revoked) return false;
    try {
      return await _channel
              .invokeMethod<bool>('publishQualityBinding', value)
              .timeout(const Duration(milliseconds: 500)) ??
          false;
    } catch (_) {
      return false;
    }
  }

  Future<bool> storeLedger(String ledger) async {
    try {
      return await _channel
              .invokeMethod<bool>('storeLedger', ledger)
              .timeout(const Duration(seconds: 2)) ??
          false;
    } catch (_) {
      return false;
    }
  }

  Future<bool> revoke({String result = 'refused'}) async {
    if (_revoked) return true;
    _revoked = true;
    try {
      await _channel
          .invokeMethod<void>('revoke', {
            'result': result == 'settled' ? 'settled' : 'refused',
          })
          .timeout(const Duration(seconds: 2));
      return true;
    } catch (_) {
      return false;
    }
  }
}
