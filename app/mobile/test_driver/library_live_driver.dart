import 'dart:convert';
import 'dart:io';
import 'package:flutter_driver/flutter_driver.dart';
import 'package:integration_test/common.dart';
import 'native_capture_writer.dart';
import 'native_runtime_guard.dart';
import '../integration_test/native_capture_protocol.dart';

Future<void> main() async {
  final tokenPath = Platform.environment['LIBRARY_ID_TOKEN_FILE'];
  final base = Platform.environment['LIBRARY_BASE_URL'];
  final output = Platform.environment['LIBRARY_NATIVE_EVIDENCE_DIR'];
  if (tokenPath == null || base == null || output == null) {
    throw StateError('Private runtime environment required');
  }
  var writer = NativeCaptureWriter(
    await preparePrivateOutput(Directory(output)),
  );
  FlutterDriver? driver;
  Response? response;
  Object? failure;
  try {
    final selection =
        jsonDecode(
              await File.fromUri(
                Platform.script.resolve(
                  '../../../tools/mobile-test-account.json',
                ),
              ).readAsString(),
            )
            as Map<String, dynamic>;
    final token = (await File(tokenPath).readAsString()).trim();
    requireSelectedToken(
      token,
      selection['firebaseUid'] as String,
      DateTime.now().toUtc(),
    );
    final descriptorPath = Platform.environment['LIBRARY_PROVIDER_EXPIRY_FILE'];
    final descriptor = descriptorPath == null
        ? null
        : await readProviderExpiryDescriptor(
            File(descriptorPath),
            selection['firebaseUid'] as String,
            DateTime.now().toUtc(),
          );
    driver = await FlutterDriver.connect(
      printCommunication: false,
      logCommunicationToFile: false,
    );
    final configured = await driver.requestData(
      'configure:${jsonEncode({'baseUrl': base, 'idToken': token, 'providerExpiryDescriptor': ?descriptor})}',
    );
    if (configured == 'configured:capture-stream-v1' ||
        configured == 'configured:capture-stream-v1-ios16') {
      if (configured == 'configured:capture-stream-v1-ios16') {
        writer = NativeCaptureWriter(
          writer.directory,
          maxCaptureBytes: NativeCaptureProtocol.iosMaxBytes,
        );
      }
      while (true) {
        final event =
            jsonDecode(
                  await driver.requestData(
                    'capture-next',
                    timeout: const Duration(minutes: 20),
                  ),
                )
                as Map<String, dynamic>;
        if (event['kind'] == 'finished') break;
        if (event['kind'] != 'capture') {
          throw StateError('Native capture event rejected');
        }
        final manifest = Map<String, dynamic>.from(event)..remove('kind');
        final receipt = await writer.persist(manifest, (sequence, index) async {
          final packet =
              jsonDecode(
                    await driver!.requestData(
                      'capture-chunk:$sequence:$index',
                      timeout: const Duration(seconds: 30),
                    ),
                  )
                  as Map<String, dynamic>;
          if (packet['kind'] != 'chunk' ||
              packet['sequence'] != sequence ||
              packet['index'] != index) {
            throw StateError('Native capture chunk identity differs');
          }
          return packet['base64'] as String;
        });
        final ack =
            jsonDecode(
                  await driver.requestData(
                    'capture-ack:${receipt['sequence']}:${receipt['sha256']}',
                    timeout: const Duration(seconds: 30),
                  ),
                )
                as Map<String, dynamic>;
        if (ack['kind'] != 'acknowledged' ||
            ack['sequence'] != receipt['sequence']) {
          throw StateError('Native capture acknowledgement rejected');
        }
        receipt['hostAcknowledged'] = true;
        await writer.writeIndex();
      }
      response = Response.fromJson(
        await driver.requestData(null, timeout: const Duration(seconds: 60)),
      );
      final received =
          response.data?['streamedScreenshots'] as List<dynamic>? ?? [];
      if (!writer.matchesInventory(received)) {
        throw StateError('Native capture receipt inventory differs');
      }
    } else if (configured == 'configured') {
      // Existing deadline bindings retain their original SDK final-response protocol.
      response = Response.fromJson(
        await driver.requestData(null, timeout: const Duration(minutes: 20)),
      );
      final screenshots =
          response.data?.remove('screenshots') as List<dynamic>? ?? [];
      for (final screenshot in screenshots) {
        final value = screenshot as Map<String, dynamic>;
        final name = value['screenshotName'];
        if (name is! String ||
            !NativeCaptureProtocol.validName.hasMatch(name) ||
            name.contains('..')) {
          throw StateError('Native capture name rejected');
        }
        final file = File('${writer.directory.path}/$name.png');
        await preparePrivateFile(file);
        await file.writeAsBytes(
          List<int>.from(value['bytes'] as List),
          flush: true,
        );
      }
    } else {
      throw StateError('Runtime injection rejected');
    }
  } catch (error) {
    // Never print RPC payloads, temporary credentials, URLs or exception stacks.
    failure = error;
  } finally {
    try {
      await driver?.close().timeout(const Duration(seconds: 10));
    } catch (_) {}
  }
  final passed = failure == null && response?.allTestsPassed == true;
  await writePrivateJson(File('${writer.directory.path}/evidence.json'), {
    'passed': passed,
    ...?response?.data,
    'hostCaptureReceipts': writer.receipts,
    if (failure != null)
      'hostTransportFailureType': failure.runtimeType.toString(),
  });
  stdout.writeln(
    passed
        ? 'Native Library acceptance passed'
        : 'Native Library acceptance failed',
  );
  exit(passed ? 0 : 1);
}
