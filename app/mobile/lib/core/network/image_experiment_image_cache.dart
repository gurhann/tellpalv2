import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import '../../app/image_experiment_configuration.dart';
import '../../features/image_experiment/domain/entity/raster_case.dart';
import '../diagnostics/image_experiment_recorder.dart';
import 'image_experiment_http_client.dart';
import 'library_image_cache.dart';

String _digestFields(Iterable<String> fields) {
  final out = BytesBuilder();
  for (final field in fields) {
    final value = utf8.encode(field);
    out.add((ByteData(4)..setUint32(0, value.length)).buffer.asUint8List());
    out.add(value);
  }
  return sha256.convert(out.takeBytes()).toString();
}

class ImageExperimentImageCache extends LibraryImageCache {
  ImageExperimentImageCache(
    this.configuration,
    this.transport,
    this.recorder, {
    Config? storageConfiguration,
  }) : namespace = namespaceFor(configuration),
       super.profile(
         storageConfiguration ??
             Config(
               'image-experiment-${namespaceFor(configuration)}',
               stalePeriod: const Duration(seconds: 180),
               maxNrOfCacheObjects: 20,
             ),
         clientFactory: transport.newClient,
         maximumOwnedAttempts: 4,
         maximumFiles: 20,
         maximumBytes: 104857600,
         maximumTransferBytes: 20971520,
       );
  final ImageExperimentConfiguration configuration;
  final ImageExperimentTransport transport;
  final ImageExperimentRecorder recorder;
  final String namespace;
  final _identities = <String, RasterCase>{};
  final _verified = <String>{};
  static String namespaceFor(ImageExperimentConfiguration c) => _digestFields([
    imageExperimentScope,
    c.batchId,
    c.sessionId,
    '${c.generation}',
    c.accessAlias,
    for (final fixture in c.cases.values) ...[
      fixture.role,
      fixture.sourceSha256,
      fixture.profileRevision,
    ],
  ]);
  String identity(RasterCase fixture) => _digestFields([
    namespace,
    for (final field in fixture.props) '$field',
    '${fixture.geometry.decodeWidth}',
    '${fixture.geometry.decodeHeight}',
  ]);
  ImageExperimentProvider provider(RasterCase fixture, String attempt) {
    configuration.credentials.requireActive();
    if (configuration.cases[fixture.caseId] != fixture) {
      throw const FormatException('fixture_unknown');
    }
    final immutable = identity(fixture);
    final transfer = LibraryImageCache.transferKey(immutable, attempt);
    if (!_identities.containsKey(transfer) &&
        _identities.keys
                .where((k) => k.contains('::library-transfer::'))
                .length >=
            4) {
      throw const FormatException('attempt_capacity');
    }
    final newlyOwned = !_identities.containsKey(transfer);
    _identities[immutable] = fixture;
    _identities[transfer] = fixture;
    final geometry = fixture.geometry;
    if (newlyOwned) {
      recorder.record({
        'event': 'ownership',
        'ownership': 'owned',
        'role': fixture.role,
        'sourceSha256': fixture.sourceSha256,
        'referenceSha256': fixture.referenceSha256,
        'scopeSha256': ImageExperimentRecorder.hash(
          '$imageExperimentScope/${configuration.batchId}',
        ),
        'settingsSha256': ImageExperimentRecorder.hash(
          jsonEncode([
            fixture.encoderRevision,
            fixture.logicalSlotWidth,
            fixture.logicalSlotHeight,
            fixture.dpr,
            fixture.fit,
            fixture.alignmentX,
            fixture.alignmentY,
          ]),
        ),
        'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
        'providerSha256': immutable,
        'attemptSha256': ImageExperimentRecorder.hash(transfer),
        'generation': configuration.generation,
        'sourceWidth': fixture.width,
        'sourceHeight': fixture.height,
        'logicalSlotWidth': fixture.logicalSlotWidth,
        'logicalSlotHeight': fixture.logicalSlotHeight,
        'dpr': fixture.dpr,
        'fit': fixture.fit,
        'alignmentX': fixture.alignmentX,
        'alignmentY': fixture.alignmentY,
        'requiredWidth': geometry.requiredWidth,
        'requiredHeight': geometry.requiredHeight,
        'decodeWidth': geometry.decodeWidth,
        'decodeHeight': geometry.decodeHeight,
        'sourceShortage': geometry.sourceShortage,
      });
    }
    final base = CachedNetworkImageProvider(
      configuration.objectUri(fixture.caseId).toString(),
      cacheManager: this,
      cacheKey: transfer,
    );
    final provider = ResizeImage(
      base,
      width: fixture.geometry.decodeWidth,
      height: fixture.geometry.decodeHeight,
      allowUpscaling: false,
    );
    return ImageExperimentProvider(
      provider,
      immutable,
      transfer,
      ImageExperimentRecorder.hash(transfer),
      fixture,
    );
  }

  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    final fixture = _identities[key];
    if (fixture == null || key == null) {
      return Stream.error(const FormatException('fixture_unknown'));
    }
    return runZoned(
      () => super.getFileStream(
        url,
        key: key,
        headers: headers,
        withProgress: withProgress,
      ),
      zoneValues: {
        ImageExperimentTransport.observationZoneKey: {
          'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
          'providerSha256': identity(fixture),
          'attemptSha256': ImageExperimentRecorder.hash(key),
          'role': fixture.role,
        },
      },
    );
  }

  @override
  Future<void> validateResponse(FileInfo response, String key) async {
    configuration.credentials.requireActive();
    _verified.remove(key);
    final fixture = _identities[key];
    if (fixture == null) throw const FormatException('fixture_unknown');
    if (await response.file.length() != fixture.byteSize) {
      throw const FormatException('fixture_length');
    }
    final bytes = await response.file.readAsBytes().timeout(
      const Duration(seconds: 10),
    );
    configuration.credentials.requireActive();
    if (bytes.length != fixture.byteSize ||
        sha256.convert(bytes).toString() != fixture.outputSha256 ||
        _mime(bytes) != fixture.mime) {
      throw const FormatException('fixture_integrity');
    }
    ui.ImmutableBuffer? buffer;
    ui.ImageDescriptor? descriptor;
    try {
      buffer = await ui.ImmutableBuffer.fromUint8List(bytes);
      descriptor = await ui.ImageDescriptor.encoded(buffer);
      configuration.credentials.requireActive();
      if (descriptor.width != fixture.width ||
          descriptor.height != fixture.height) {
        throw const FormatException('fixture_dimensions');
      }
      _verified.add(key);
      recorder.record({
        'event': 'encoded_verified',
        'encodedVerified': true,
        'caseSha256': ImageExperimentRecorder.hash(fixture.caseId),
        'providerSha256': identity(fixture),
        'attemptSha256': ImageExperimentRecorder.hash(key),
        'generation': configuration.generation,
      });
    } finally {
      descriptor?.dispose();
      buffer?.dispose();
    }
  }

  static String? _mime(Uint8List bytes) {
    if (bytes.length >= 8 &&
        bytes[0] == 137 &&
        bytes[1] == 80 &&
        bytes[2] == 78 &&
        bytes[3] == 71 &&
        bytes[4] == 13 &&
        bytes[5] == 10 &&
        bytes[6] == 26 &&
        bytes[7] == 10) {
      return 'image/png';
    }
    if (bytes.length >= 3 &&
        bytes[0] == 255 &&
        bytes[1] == 216 &&
        bytes[2] == 255) {
      return 'image/jpeg';
    }
    if (bytes.length >= 12 &&
        ascii.decode(bytes.sublist(0, 4), allowInvalid: true) == 'RIFF' &&
        ascii.decode(bytes.sublist(8, 12), allowInvalid: true) == 'WEBP') {
      return 'image/webp';
    }
    return null;
  }

  Future<void> requireAbsentKeys(ImageExperimentProvider value) async {
    final key = await value.provider.obtainKey(ImageConfiguration.empty);
    final status = PaintingBinding.instance.imageCache.statusForKey(key);
    final immutableFile = await getFileFromCache(value.immutableKey);
    final transferFile = await getFileFromCache(value.transferKey);
    configuration.credentials.requireActive();
    if (immutableFile != null ||
        transferFile != null ||
        status.live ||
        status.pending ||
        status.keepAlive) {
      throw const FormatException('cache_keys_present');
    }
    recorder.record({
      'event': 'cache_state',
      'cacheState': 'namespace_keys_absent',
      'reason': 'none',
      'ownership': 'owned',
      'generation': configuration.generation,
      'caseSha256': ImageExperimentRecorder.hash(value.fixture.caseId),
      'providerSha256': value.immutableKey,
      'attemptSha256': value.attemptSha256,
    });
  }

  bool verified(ImageExperimentProvider value) =>
      _verified.contains(value.transferKey);
  Future<void> remember(
    ImageExperimentProvider value,
    bool Function() current,
  ) => rememberValidated(
    value.immutableKey,
    value.transferKey,
    isCurrent: current,
  );
  Future<void> release(
    ImageExperimentProvider value, {
    bool failed = false,
  }) async {
    try {
      await discardFailedTransfer(value.transferKey, failed: failed);
    } finally {
      _identities.remove(value.transferKey);
      _verified.remove(value.transferKey);
    }
  }

  /// Private package retirement/storage counters are unavailable; never infer settlement from LRU.
  Future<String> prepareState(
    String command,
    ImageExperimentProvider value, {
    required bool hasOwnedListener,
  }) async {
    if (!{
      'network-cold',
      'disk-warm/decoded-cold',
      'decoded-warm',
    }.contains(command)) {
      throw const FormatException('cache_command');
    }
    final key = await value.provider.obtainKey(ImageConfiguration.empty);
    final status = PaintingBinding.instance.imageCache.statusForKey(key);
    final encoded = await getFileFromCache(value.immutableKey);
    if (encoded != null) await validateResponse(encoded, value.immutableKey);
    // The requested state requires proof for all owner families, unavailable on this cache seam.
    recorder.record({
      'event': 'cache_state',
      'cacheState': 'unavailable',
      'ownership': 'unavailable',
      'reason': 'internal_settlement_unavailable',
      'decodedLiveCount': status.live ? 1 : 0,
      'decodedPendingCount': status.pending ? 1 : 0,
      'encodedVerified': encoded != null,
      'generation': configuration.generation,
    });
    return 'unavailable';
  }
}

class ImageExperimentProvider {
  const ImageExperimentProvider(
    this.provider,
    this.immutableKey,
    this.transferKey,
    this.attemptSha256,
    this.fixture,
  );
  final ImageProvider provider;
  final String immutableKey, transferKey, attemptSha256;
  final RasterCase fixture;
  @override
  String toString() => 'ImageExperimentProvider(admitted)';
}
