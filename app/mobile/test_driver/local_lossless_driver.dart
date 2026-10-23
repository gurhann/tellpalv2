import 'dart:convert';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:flutter_driver/flutter_driver.dart';
import 'native_runtime_guard.dart';

const _cases = [
  (
    9778,
    'PNG',
    1765704,
    '7744c016ca5a8df9700d0ec6e93ec40955e1afafcd810b527c7611deaefff7bd',
    'e2326ced29ddab4e358ed8032bbb89e8a0cfcd19a7b1ca2b12d6454961082f0e',
  ),
  (
    9778,
    'WEBP',
    1017010,
    'cef354623c5c66876965ad690ffc121ae299bfe1fc55f95217082f2ddd6dbc86',
    'e2326ced29ddab4e358ed8032bbb89e8a0cfcd19a7b1ca2b12d6454961082f0e',
  ),
  (
    9816,
    'PNG',
    1658329,
    '5b345614b0d89f9946eddb89996dc0d3c1bcc9162a16922c4efe11b40ed0392b',
    'c5124e71d39360b4e01cf64723eb5fd19a204ee799c020a8e640afbb03c2c00d',
  ),
  (
    9816,
    'WEBP',
    993082,
    '49d7d9a7a9b47a4bf91b4e2a39317b280889fa68becc88152adfd6a95c399d9d',
    'c5124e71d39360b4e01cf64723eb5fd19a204ee799c020a8e640afbb03c2c00d',
  ),
  (
    9818,
    'PNG',
    1688375,
    '54c331c1007c6552f5f590e3e315664d27c17c2e5efe3ec41927417d6697904b',
    '0994cceae1759dd7be81b0e97a0aeaf8b0a6ea7e512fe13202974c285eae4491',
  ),
  (
    9818,
    'WEBP',
    1010290,
    '7914fddd392f23819f8d5b6c68b8faa97ee6da80ad21e522981feb5ace296b49',
    '0994cceae1759dd7be81b0e97a0aeaf8b0a6ea7e512fe13202974c285eae4491',
  ),
];

void _require(bool condition) {
  if (!condition) throw StateError('local_lossless_driver_refused');
}

String localLosslessDriverRunNonce(Map<String, String> environment) {
  final value = environment['LOCAL_LOSSLESS_RUN_NONCE'];
  _require(
    value != null &&
        value.length == 64 &&
        RegExp(r'^[a-f0-9]{64}$').hasMatch(value),
  );
  return value!;
}

/// Bağlantı adresi yalnız flutter drive'ın açık runtime girdisinden gelir.
String localLosslessDriverVmServiceUrl(Map<String, String> environment) {
  final value = environment['VM_SERVICE_URL'];
  _require(value != null && value.isNotEmpty && value == value.trim());
  final uri = Uri.tryParse(value!);
  _require(
    uri != null &&
        ['http', 'ws'].contains(uri.scheme) &&
        ['127.0.0.1', '::1', 'localhost'].contains(uri.host) &&
        uri.hasPort &&
        uri.port > 0 &&
        uri.port <= 65535 &&
        uri.userInfo.isEmpty &&
        uri.fragment.isEmpty,
  );
  return value;
}

class _RuntimeBindingMismatch implements Exception {
  const _RuntimeBindingMismatch();
}

void requireLocalLosslessRuntimeNonce(
  Map<String, dynamic> snapshot,
  String expected,
) {
  localLosslessDriverRunNonce({'LOCAL_LOSSLESS_RUN_NONCE': expected});
  if (snapshot['runNonce'] != expected) {
    throw const _RuntimeBindingMismatch();
  }
}

Future<Map<String, dynamic>> _request(
  FlutterDriver driver,
  String command,
  String runNonce, {
  bool requireOk = true,
}) async {
  final encoded = await driver.requestData(
    command,
    timeout: const Duration(seconds: 45),
  );
  _require(encoded.length <= 65536);
  final value = jsonDecode(encoded);
  if (value is! Map<String, dynamic>) throw const _RuntimeBindingMismatch();
  requireLocalLosslessRuntimeNonce(value, runNonce);
  _require(!requireOk || value['ok'] == true);
  return value;
}

Future<Map<String, dynamic>> _waitReady(
  FlutterDriver driver,
  String runNonce,
) async {
  final elapsed = Stopwatch()..start();
  while (elapsed.elapsed < const Duration(seconds: 10)) {
    final remaining = const Duration(seconds: 10) - elapsed.elapsed;
    final value = await _request(driver, 'status', runNonce).timeout(remaining);
    if (value['ready'] == true) return value;
    await Future<void>.delayed(const Duration(milliseconds: 250));
  }
  throw StateError('local_lossless_ready_deadline');
}

bool _emptyTransientHandles(Map<String, dynamic> value) =>
    value['activeBuffers'] == 0 &&
    value['activeDescriptors'] == 0 &&
    value['activeCodecs'] == 0;

void _validatePaint(Map<String, dynamic> snapshot, int index) {
  final cases = snapshot['cases'];
  _require(
    snapshot['enabled'] == true &&
        snapshot['profileMode'] == true &&
        snapshot['stopped'] == false &&
        snapshot['status'] == 'loaded' &&
        snapshot['caseIndex'] == index &&
        snapshot['epoch'] is int &&
        snapshot['paintedEpoch'] == snapshot['epoch'] &&
        snapshot['completedCases'] == index + 1 &&
        cases is List &&
        cases.length == index + 1 &&
        _emptyTransientHandles(snapshot) &&
        snapshot['activeImages'] == 1,
  );
  final sample = (cases as List)[index];
  _require(sample is Map);
  final expected = _cases[index];
  final widget = sample['widget'];
  _require(
    sample['assetId'] == expected.$1 &&
        sample['format'] == expected.$2 &&
        sample['encodedBytes'] == expected.$3 &&
        sample['encodedSha256'] == expected.$4 &&
        sample['decodedWidth'] == 822 &&
        sample['decodedHeight'] == 1334 &&
        sample['rgbBytes'] == 3289644 &&
        sample['rgbSha256'] == expected.$5 &&
        sample['rgbMatchesPin'] == true &&
        sample['paintCompleted'] == true &&
        widget is Map,
  );
  for (final key in [
    'assetReadMicros',
    'checksumMicros',
    'decodeMicros',
    'rgbReadbackMicros',
  ]) {
    _require(sample[key] is int && sample[key] >= 0);
  }
  final geometry = widget as Map;
  final width = geometry['logicalWidth'];
  final height = geometry['logicalHeight'];
  final dpr = geometry['dpr'];
  final left = geometry['physicalLeft'];
  final top = geometry['physicalTop'];
  _require(
    width is num &&
        width.isFinite &&
        height is num &&
        height.isFinite &&
        dpr is num &&
        dpr.isFinite &&
        dpr > 0 &&
        (width * dpr - 822).abs() < 0.001 &&
        (height * dpr - 1334).abs() < 0.001 &&
        geometry['physicalWidth'] == 822 &&
        geometry['physicalHeight'] == 1334 &&
        geometry['pixelAligned'] == true &&
        left is num &&
        left.isFinite &&
        (left - left.round()).abs() < 0.001 &&
        top is num &&
        top.isFinite &&
        (top - top.round()).abs() < 0.001,
  );
}

Future<void> _writeBytesExclusive(File file, List<int> bytes) async {
  await file.create(exclusive: true);
  await privateMode(file.path, '600');
  await file
      .writeAsBytes(bytes, flush: true)
      .timeout(const Duration(seconds: 10));
}

Future<void> _writeJsonExclusive(File file, Object value) =>
    _writeBytesExclusive(
      file,
      utf8.encode('${const JsonEncoder.withIndent('  ').convert(value)}\n'),
    );

void _validatePng(List<int> bytes) {
  const signature = [137, 80, 78, 71, 13, 10, 26, 10];
  _require(bytes.length >= 24 && bytes.length <= 16 * 1024 * 1024);
  for (var index = 0; index < signature.length; index++) {
    _require(bytes[index] == signature[index]);
  }
}

/// Altı sabit durumu seri yürütür; her ekranı tamamlanan paint sonrasında alır.
Future<void> main() async {
  final runNonce = localLosslessDriverRunNonce(Platform.environment);
  final vmServiceUrl = localLosslessDriverVmServiceUrl(Platform.environment);
  final output = Platform.environment['LOCAL_LOSSLESS_EVIDENCE_DIR'];
  _require(output != null && output.isNotEmpty);
  final directory = await preparePrivateOutput(Directory(output!));
  final evidenceFile = File('${directory.path}/evidence.json');
  _require(!await evidenceFile.exists());
  FlutterDriver? driver;
  Map<String, dynamic>? initial, stopped;
  Object? failure;
  var stage = 'connect';
  String? failureStage;
  var runtimeBound = false;
  final observations = <Map<String, Object?>>[];
  final captures = <Map<String, Object?>>[];
  try {
    driver = await FlutterDriver.connect(
      dartVmServiceUrl: vmServiceUrl,
      printCommunication: false,
      logCommunicationToFile: false,
    );
    stage = 'initial_status';
    initial = await _waitReady(driver, runNonce);
    runtimeBound = true;
    _require(
      initial['enabled'] == true &&
          initial['profileMode'] == true &&
          initial['stopped'] == false &&
          initial['completedCases'] == 0 &&
          initial['cases'] is List &&
          (initial['cases'] as List).isEmpty &&
          _emptyTransientHandles(initial) &&
          initial['activeImages'] == 0,
    );
    for (var index = 0; index < _cases.length; index++) {
      stage = 'show_$index';
      final painted = await _request(driver, 'show:$index', runNonce);
      observations.add({'caseIndex': index, 'painted': painted});
      _validatePaint(painted, index);
      stage = 'screenshot_$index';
      final bytes = await driver.screenshot().timeout(
        const Duration(seconds: 20),
      );
      _validatePng(bytes);
      final afterCapture = await _request(driver, 'status', runNonce);
      _validatePaint(afterCapture, index);
      _require(afterCapture['epoch'] == painted['epoch']);
      final expected = _cases[index];
      final name =
          '${index + 1}-${expected.$1}-${expected.$2.toLowerCase()}.png';
      await _writeBytesExclusive(File('${directory.path}/$name'), bytes);
      captures.add({
        'caseIndex': index,
        'screenshotName': name,
        'byteSize': bytes.length,
        'sha256': sha256.convert(bytes).toString(),
        'afterPaintedEpoch': painted['paintedEpoch'],
      });
      await _writeJsonExclusive(
        File('${directory.path}/${index + 1}-observation.json'),
        {
          'painted': painted,
          'afterCapture': afterCapture,
          'capture': captures.last,
        },
      );
    }
  } catch (error) {
    failure = error;
    failureStage = stage;
    if (error is _RuntimeBindingMismatch) runtimeBound = false;
  } finally {
    if (driver != null) {
      if (runtimeBound) {
        try {
          stopped = await _request(driver, 'stop', runNonce, requireOk: false);
          _require(
            stopped['stopped'] == true &&
                _emptyTransientHandles(stopped) &&
                stopped['activeImages'] == 0,
          );
          _require(stopped['ok'] == true);
        } catch (error) {
          failure ??= error;
          failureStage ??= 'stop';
        }
      }
      try {
        await driver.close().timeout(const Duration(seconds: 10));
      } catch (error) {
        failure ??= error;
        failureStage ??= 'close';
      }
    }
  }
  final passed =
      failure == null &&
      observations.length == 6 &&
      captures.length == 6 &&
      stopped?['completedCases'] == 6 &&
      stopped?['cases'] is List &&
      (stopped!['cases'] as List).length == 6;
  await _writeJsonExclusive(evidenceFile, {
    'status': passed ? 'PASS' : 'FAIL',
    'passed': passed,
    'runNonce': runNonce,
    'vmServiceUrlSha256': sha256.convert(utf8.encode(vmServiceUrl)).toString(),
    'vmServiceUrlSource': 'flutter_drive_environment',
    'initial': initial,
    'observations': observations,
    'captures': captures,
    'stopped': stopped,
    'osBackgroundTest': 'NOT_RUN',
    'httpTransferMeasured': false,
    'humanQualityApproval': false,
    'theoreticalRgbaBytes': 4386192,
    'rssScope': 'PROCESS_CURRENT_RSS_SAMPLES_ONLY_NOT_PEAK_OR_GPU',
    if (failure != null) 'hostFailureType': failure.runtimeType.toString(),
    'hostFailureStage': ?failureStage,
  });
  stdout.writeln(
    passed ? 'Yerel kayıpsız cihaz QA: PASS' : 'Yerel kayıpsız cihaz QA: FAIL',
  );
  exitCode = passed ? 0 : 1;
}
