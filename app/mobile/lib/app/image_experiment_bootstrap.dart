import 'dart:async';
import 'package:flutter/services.dart';
import 'image_experiment_configuration.dart';

class ImageExperimentBootstrap {
  ImageExperimentBootstrap({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel('tellpal/image_experiment');
  final MethodChannel _channel;
  bool _taken = false;
  bool _revoked = false;
  Future<ImageExperimentConfiguration> takeSession() async {
    if (_taken || _revoked) throw const FormatException('bootstrap_refused');
    _taken = true;
    try {
      final response = await _channel
          .invokeMapMethod<String, dynamic>('takeSession')
          .timeout(const Duration(seconds: 30));
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
      return ImageExperimentConfiguration.parse(raw, metadata);
    } catch (_) {
      await revoke();
      throw const FormatException('bootstrap_refused');
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
