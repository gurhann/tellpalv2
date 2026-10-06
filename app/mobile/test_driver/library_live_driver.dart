import 'dart:convert';
import 'dart:io';
import 'package:flutter_driver/flutter_driver.dart';
import 'package:integration_test/common.dart';

Future<void> main() async {
  final tokenPath = Platform.environment['LIBRARY_ID_TOKEN_FILE'];
  final base = Platform.environment['LIBRARY_BASE_URL'];
  final output = Platform.environment['LIBRARY_NATIVE_EVIDENCE_DIR'];
  if (tokenPath == null || base == null || output == null) {
    throw StateError('Private runtime environment required');
  }
  final driver = await FlutterDriver.connect(
    printCommunication: false,
    logCommunicationToFile: false,
  );
  final configured = await driver.requestData(
    'configure:${jsonEncode({'baseUrl': base, 'idToken': File(tokenPath).readAsStringSync().trim()})}',
  );
  if (configured != 'configured') {
    throw StateError('Runtime injection rejected');
  }
  final response = Response.fromJson(
    await driver.requestData(null, timeout: const Duration(minutes: 10)),
  );
  await driver.close();
  final data = response.data ?? {};
  final dir = Directory(output)..createSync(recursive: true);
  final screenshots = data.remove('screenshots') as List<dynamic>?;
  for (final screenshot in screenshots ?? []) {
    final value = screenshot as Map<String, dynamic>;
    File(
      '${dir.path}/${value['screenshotName']}.png',
    ).writeAsBytesSync(List<int>.from(value['bytes'] as List));
  }
  File('${dir.path}/evidence.json').writeAsStringSync(
    '${const JsonEncoder.withIndent('  ').convert({'passed': response.allTestsPassed, ...data})}\n',
  );
  stdout.writeln(
    response.allTestsPassed
        ? 'Native Library acceptance passed'
        : 'Native Library acceptance failed',
  );
  exit(response.allTestsPassed ? 0 : 1);
}
