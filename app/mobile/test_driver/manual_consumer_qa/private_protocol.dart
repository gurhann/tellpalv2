import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import '../reading_consumer_qa_probe.dart';
import '../reading_resource_observation.dart';

const manualScope = ConsumerQaExpectedScope.p4b5Manual;
const manualPhases = [
  'baseline',
  'paint1',
  'paint2',
  'paint3',
  'paint4',
  'beforeBackground',
  'background',
  'resumed',
  'closedUi',
  'stopped',
];
void manualRequire(bool value) {
  if (!value) throw const FormatException('manual_qa_refused');
}

enum ManualQaDiagnosticStage {
  support,
  injection,
  mount,
  platform,
  process,
  directories,
  permissions,
  baseline,
  failureMarker,
}

enum ManualQaDiagnosticCode {
  started,
  complete,
  failed,
  unavailable,
  supportTypeMismatch,
  supportTypeReadFailed,
  supportCanonicalMismatch,
  supportResolveFailed,
}

/// Yalnız sabit aşama/kodlar logcat'e gider; hata metni ve yollar taşınmaz.
class ManualQaDiagnostics {
  ManualQaDiagnostics([void Function(String)? output])
    : _output = output ?? debugPrint;
  final void Function(String) _output;
  ManualQaDiagnosticStage _stage = ManualQaDiagnosticStage.support;
  int _count = 0;

  void emit(ManualQaDiagnosticStage stage, ManualQaDiagnosticCode code) {
    _stage = stage;
    if (_count++ >= 16) return;
    try {
      _output('MANUAL_QA_DIAGNOSTIC stage=${stage.name} code=${code.name}');
    } catch (_) {
      // Tanı hedefinin hatası özel dosya korumalarını değiştirmez.
    }
  }

  void failed() => emit(_stage, ManualQaDiagnosticCode.failed);
}

/// Testler yalnız platform/süreç/izin araç girdilerini değiştirir.
class ManualQaPlatform {
  const ManualQaPlatform();
  bool get android => Platform.isAndroid;
  int get processId => pid;
  Future<String> processStat() => File('/proc/self/stat').readAsString();
  Future<void> permissions(String path, String mode) async {
    final result = await Process.run('chmod', [mode, path]);
    manualRequire(result.exitCode == 0);
  }
}

/// Platform filesDir iznine dokunmaz; yalnız oluşturduğu alt yolları sahiplenir.
class ManualQaPrivateProtocol {
  ManualQaPrivateProtocol(
    this.support, {
    this.platform = const ManualQaPlatform(),
    this.scope = manualScope,
    ManualQaDiagnostics? diagnostics,
  }) : diagnostics = diagnostics ?? ManualQaDiagnostics() {
    manualRequire(scope.manual);
  }
  final Directory support;
  final ConsumerQaExpectedScope scope;
  final ManualQaPlatform platform;
  final ManualQaDiagnostics diagnostics;
  late final Directory root = Directory('${support.path}/manual-consumer-qa');
  late final Directory receipts = Directory('${root.path}/receipts');
  late final Directory secrets = Directory('${root.path}/startup');
  late final Directory captures = Directory('${root.path}/captures');
  final String nonce = List.generate(
    32,
    (_) => Random.secure().nextInt(256).toRadixString(16).padLeft(2, '0'),
  ).join();
  late final String startTicks, lifetimeId;
  String previousSha = '0' * 64;
  int ordinal = 0, totalBytes = 0, captureCount = 0;
  bool consumed = false, initialized = false, failureWritten = false;

  Future<void> initialize() async {
    diagnostics.emit(
      ManualQaDiagnosticStage.platform,
      ManualQaDiagnosticCode.started,
    );
    manualRequire(!initialized && platform.android);
    diagnostics.emit(
      ManualQaDiagnosticStage.process,
      ManualQaDiagnosticCode.started,
    );
    final stat = await platform.processStat();
    startTicks = stat.substring(stat.lastIndexOf(') ') + 2).split(' ')[19];
    manualRequire(RegExp(r'^[1-9][0-9]*$').hasMatch(startTicks));
    lifetimeId =
        (readingOwnedResources()['resourceObservation'] as Map)['lifetimeId']
            as String;
    diagnostics.emit(
      ManualQaDiagnosticStage.directories,
      ManualQaDiagnosticCode.started,
    );
    final FileSystemEntityType supportType;
    try {
      supportType = await FileSystemEntity.type(
        support.path,
        followLinks: false,
      );
    } catch (_) {
      diagnostics.emit(
        ManualQaDiagnosticStage.directories,
        ManualQaDiagnosticCode.supportTypeReadFailed,
      );
      rethrow;
    }
    if (supportType != FileSystemEntityType.directory) {
      diagnostics.emit(
        ManualQaDiagnosticStage.directories,
        ManualQaDiagnosticCode.supportTypeMismatch,
      );
      manualRequire(false);
    }
    // Canonical yol eşitliği symlink'li ara bileşenleri de reddeder.
    final String canonicalSupport;
    try {
      canonicalSupport = await support.resolveSymbolicLinks();
    } catch (_) {
      diagnostics.emit(
        ManualQaDiagnosticStage.directories,
        ManualQaDiagnosticCode.supportResolveFailed,
      );
      rethrow;
    }
    if (canonicalSupport != support.absolute.path) {
      diagnostics.emit(
        ManualQaDiagnosticStage.directories,
        ManualQaDiagnosticCode.supportCanonicalMismatch,
      );
      manualRequire(false);
    }
    diagnostics.emit(
      ManualQaDiagnosticStage.permissions,
      ManualQaDiagnosticCode.started,
    );
    for (final directory in [root, receipts, secrets, captures]) {
      manualRequire(
        await FileSystemEntity.type(directory.path, followLinks: false) ==
            FileSystemEntityType.notFound,
      );
      await directory.create();
      await platform.permissions(directory.path, '700');
      await _private(directory.path, directory: true);
    }
    initialized = true;
    diagnostics.emit(
      ManualQaDiagnosticStage.permissions,
      ManualQaDiagnosticCode.complete,
    );
  }

  Map<String, Object?> identity() => {
    'measurementId': scope.measurementId,
    'proposalSha256': scope.proposalSha256,
    'budgetRunId': consumerQaRunId,
    'nonce': nonce,
    'pid': platform.processId,
    'processStartTicks': startTicks,
    'lifetimeId': lifetimeId,
    'supportDirectory': support.path,
  };

  Future<void> _private(String path, {bool directory = false}) async {
    manualRequire(
      await FileSystemEntity.type(path, followLinks: false) ==
          (directory
              ? FileSystemEntityType.directory
              : FileSystemEntityType.file),
    );
    manualRequire(
      await File(path).resolveSymbolicLinks() == File(path).absolute.path,
    );
    manualRequire(
      (await FileStat.stat(path)).mode & 0x1ff == (directory ? 0x1c0 : 0x180),
    );
  }

  Future<void> _directories() async {
    manualRequire(
      initialized &&
          await support.resolveSymbolicLinks() == support.absolute.path,
    );
    for (final directory in [root, receipts, secrets, captures]) {
      await _private(directory.path, directory: true);
    }
  }

  Future<Map<String, dynamic>?> consume() async {
    manualRequire(!consumed);
    await _directories();
    final file = File('${secrets.path}/configure.json');
    if (await FileSystemEntity.type(file.path, followLinks: false) ==
        FileSystemEntityType.notFound) {
      return null;
    }
    consumed = true;
    await _private(file.path);
    final owner = File('${secrets.path}/handoff.owner');
    await _private(owner.path);
    manualRequire(
      await owner.readAsString() == nonce && await file.length() <= 20480,
    );
    final value = jsonDecode(await file.readAsString()) as Map<String, dynamic>;
    manualRequire(
      value.length == identity().length + 1 &&
          identity().entries.every((e) => value[e.key] == e.value),
    );
    final configuration = value['configure'] as Map<String, dynamic>;
    final validated = parseConsumerQaConfiguration(
      configuration,
      expectedScope: scope,
    );
    await validated.account!.credentials.close();
    return configuration;
  }

  Future<void> receipt(String phase, Map<String, Object?> sample) async {
    manualRequire(
      ordinal < manualPhases.length && manualPhases[ordinal] == phase,
    );
    final bytes = utf8.encode(
      jsonEncode({
        ...identity(),
        'ordinal': ordinal + 1,
        'phase': phase,
        'previousSha256': previousSha,
        'recordedAtEpochMillis': DateTime.now().millisecondsSinceEpoch,
        'sample': sample,
      }),
    );
    manualRequire(
      bytes.length <= 1048576 && totalBytes + bytes.length <= 12582912,
    );
    await _publish(
      File(
        '${receipts.path}/${(ordinal + 1).toString().padLeft(2, '0')}-$phase.json',
      ),
      bytes,
    );
    previousSha = sha256.convert(bytes).toString();
    totalBytes += bytes.length;
    ordinal++;
  }

  /// ACK gelene kadar UI kapalı kalır; erken/ikinci/başka epoch ACK terminaldir.
  Future<bool> capture(int paint, int epoch, String receiptSha) async {
    await _directories();
    manualRequire(paint == captureCount + 1 && paint >= 1 && paint <= 4);
    final names = (await captures.list().toList())
        .map((e) => e.path.split('/').last)
        .toSet();
    final expectedNames = {
      for (var i = 1; i <= paint; i++)
        'capture-${i.toString().padLeft(2, '0')}.json',
    };
    // Host atomik yayın sırasında yalnız beklenen .part dosyasını bırakabilir.
    manualRequire(
      names.difference({
        ...expectedNames,
        'capture-${paint.toString().padLeft(2, '0')}.json.part',
      }).isEmpty,
    );
    final file = File(
      '${captures.path}/capture-${paint.toString().padLeft(2, '0')}.json',
    );
    if (!names.contains(file.path.split('/').last)) return false;
    await _private(file.path);
    manualRequire(await file.length() <= 20480);
    final raw = await file.readAsBytes();
    final ack = jsonDecode(utf8.decode(raw)) as Map<String, dynamic>;
    manualRequire(
      ack.length == identity().length + 4 &&
          identity().entries.every((e) => ack[e.key] == e.value) &&
          ack['phase'] == 'paint$paint' &&
          ack['epoch'] == epoch &&
          ack['receiptSha256'] == receiptSha &&
          ack['pngSha256'] is String &&
          RegExp(r'^[a-f0-9]{64}$').hasMatch(ack['pngSha256'] as String) &&
          totalBytes + raw.length <= 12582912,
    );
    totalBytes += raw.length;
    captureCount++;
    _captures[file.path] = sha256.convert(raw).toString();
    return true;
  }

  final Map<String, String> _captures = {};
  Future<void> validateCaptures({int? pendingPaint}) async {
    await _directories();
    final allowed = _captures.keys.toSet();
    if (pendingPaint != null) {
      manualRequire(pendingPaint == captureCount + 1);
      final expected =
          '${captures.path}/capture-${pendingPaint.toString().padLeft(2, '0')}.json';
      allowed.addAll([expected, '$expected.part']);
    }
    manualRequire(
      (await captures.list().toList()).every(
        (file) => allowed.contains(file.path),
      ),
    );
    for (final entry in _captures.entries) {
      await _private(entry.key);
      manualRequire(
        sha256.convert(await File(entry.key).readAsBytes()).toString() ==
            entry.value,
      );
    }
    if (captureCount == 4) {
      manualRequire((await captures.list().toList()).length == 4);
    }
  }

  Future<void> _publish(File file, List<int> bytes) async {
    await _directories();
    manualRequire(
      await FileSystemEntity.type(file.path, followLinks: false) ==
          FileSystemEntityType.notFound,
    );
    final temporary = File('${file.path}.part');
    await temporary.create(exclusive: true);
    await platform.permissions(temporary.path, '600');
    await _private(temporary.path);
    await temporary.writeAsBytes(bytes, flush: true);
    manualRequire(
      await FileSystemEntity.type(file.path, followLinks: false) ==
          FileSystemEntityType.notFound,
    );
    await temporary.rename(file.path);
  }

  Future<void> failure() async {
    if (!initialized || failureWritten) {
      diagnostics.emit(
        ManualQaDiagnosticStage.failureMarker,
        ManualQaDiagnosticCode.unavailable,
      );
      return;
    }
    final bytes = utf8.encode(
      jsonEncode({
        ...identity(),
        'status': 'FAILED_NO_RETRY',
        'completedPhases': ordinal,
        'previousSha256': previousSha,
      }),
    );
    manualRequire(totalBytes + bytes.length <= 12582912);
    try {
      await _publish(File('${receipts.path}/terminal-failure.json'), bytes);
      diagnostics.emit(
        ManualQaDiagnosticStage.failureMarker,
        ManualQaDiagnosticCode.complete,
      );
    } catch (_) {
      diagnostics.emit(
        ManualQaDiagnosticStage.failureMarker,
        ManualQaDiagnosticCode.failed,
      );
      rethrow;
    }
    failureWritten = true;
    totalBytes += bytes.length;
  }

  Future<void> cleanup() async {
    if (!initialized) return;
    await _directories();
    final owner = File('${secrets.path}/handoff.owner');
    final paths = [
      '${secrets.path}/configure.json',
      '${secrets.path}/configure.json.part',
    ];
    if (await FileSystemEntity.type(owner.path, followLinks: false) ==
        FileSystemEntityType.notFound) {
      manualRequire((await secrets.list().toList()).isEmpty);
      return;
    }
    await _private(owner.path);
    manualRequire(await owner.readAsString() == nonce);
    for (final path in paths) {
      if (await FileSystemEntity.type(path, followLinks: false) !=
          FileSystemEntityType.notFound) {
        await _private(path);
        await File(path).delete();
      }
    }
    await owner.delete();
    manualRequire((await secrets.list().toList()).isEmpty);
  }
}
