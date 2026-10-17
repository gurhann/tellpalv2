import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_app.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_injection.dart';
import 'package:tellpal_mobile_poc/core/theme/library_theme.dart';
import 'package:tellpal_mobile_poc/features/reading_images/data/data_source/reading_image_comparison_data_source.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';

const comparisonRunId = 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09';
const comparisonPlanSha =
    'f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3';

void _require(bool condition) {
  if (!condition) throw const FormatException('probe_refused');
}

/// Sırlar yalnız runtime mesajından gelir. Sabit sayılar bu tek onaylı denemeye aittir.
ReadingImageComparisonConfiguration parseComparisonConfiguration(
  Map<String, dynamic> input,
) {
  _require(
    input.keys.toSet().difference({
          'action',
          'runId',
          'planSha256',
          'uid',
          'contentId',
          'port',
          'token',
        }).isEmpty &&
        input.length == 7,
  );
  _require(
    input['action'] == 'configure' &&
        input['runId'] == comparisonRunId &&
        input['planSha256'] == comparisonPlanSha &&
        input['uid'] == imageExperimentQaUid,
  );
  _require(
    input['contentId'] is int &&
        input['contentId'] > 0 &&
        ![71, 93, 112].contains(input['contentId']),
  );
  _require(
    input['port'] is int && input['port'] >= 1024 && input['port'] <= 65535,
  );
  final token = input['token'];
  _require(
    token is String &&
        token.length <= 16384 &&
        RegExp(
          r'^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$',
        ).hasMatch(token),
  );
  final claims =
      jsonDecode(
            utf8.decode(
              base64Url.decode(
                base64Url.normalize((token as String).split('.')[1]),
              ),
            ),
          )
          as Map<String, dynamic>;
  final now = DateTime.now();
  _require(
    claims['aud'] == 'tellpal-ee0dd' &&
        claims['iss'] == 'https://securetoken.google.com/tellpal-ee0dd' &&
        claims['sub'] == imageExperimentQaUid &&
        claims['exp'] is int &&
        claims['iat'] is int &&
        claims['iat'] <= now.millisecondsSinceEpoch ~/ 1000 &&
        (claims['user_id'] == null ||
            claims['user_id'] == imageExperimentQaUid),
  );
  final expiry = DateTime.fromMillisecondsSinceEpoch(
    claims['exp'] * 1000,
    isUtc: true,
  );
  _require(expiry.difference(now).inSeconds > 180);
  return ReadingImageComparisonConfiguration.media(
    origin: Uri(scheme: 'http', host: '127.0.0.1', port: input['port']),
    credentials: ReadingComparisonAccount(
      uid: imageExperimentQaUid,
      generation: 0,
      credentials: ImageExperimentCredentials(
        token,
        now.add(const Duration(seconds: 180)),
      ),
    ),
    profile: ReadingComparisonLimits(
      runId: comparisonRunId,
      maxEncodedBytes: 8388608,
      maxBufferBytes: 25165824,
      maxDecodePixels: 4800000,
      maxDecodedBytes: 20971520,
      maxPeakBytes: 134217728,
      maxOwnedImages: 1,
      transferMillis: 45000,
      decodeMillis: 10000,
      statusEveryMillis: 10000,
      maxStatusChecks: 18,
      maxControlMillis: 180000,
    ),
  );
}

class ReadingComparisonWaiting extends StatelessWidget {
  const ReadingComparisonWaiting({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    theme: AppTypography.theme(),
    home: const Scaffold(
      body: Center(child: Text('QA çalıştırıcısı bekleniyor')),
    ),
  );
}

/// Mevcut HTTP okuyucusunu aynen kullanır; gövdeyi veya query'yi saklamaz.
class ObservedReadingComparisonSource extends ReadingImageComparisonDataSource {
  const ObservedReadingComparisonSource(super.configuration, this.record);
  final void Function(Map<String, Object?>) record;
  @override
  Future<Uint8List> read(
    String path,
    Map<String, String> query,
    ReadingComparisonCancellation cancellation,
    int limit,
  ) async {
    final watch = Stopwatch()..start();
    final safePath = ['manifest', 'status', 'bytes'].contains(path)
        ? path
        : 'refused';
    try {
      final bytes = await super.read(path, query, cancellation, limit);
      record({
        'kind': 'http',
        'endpoint': safePath,
        'ok': true,
        'payloadBytes': bytes.length,
        'elapsedMillis': watch.elapsedMilliseconds,
      });
      return bytes;
    } catch (_) {
      record({
        'kind': 'http',
        'endpoint': safePath,
        'ok': false,
        'elapsedMillis': watch.elapsedMilliseconds,
      });
      rethrow;
    }
  }
}

/// Cubit ve native kaynak sahipliğini gözler; uygulama davranışını değiştirmez.
class ReadingComparisonProbe extends BlocObserver {
  ReadingComparisonProbe({this.initializeDependencies});
  final FutureOr<void> Function(ReadingImageComparisonConfiguration)?
  initializeDependencies;
  Future<void>? _stopping;
  final List<Map<String, Object?>> _events = [];
  ReadingImageComparisonCubit? _cubit;
  ReadingImageComparisonConfiguration? _configuration;
  Timer? _deadline;
  bool _configured = false, _stopped = false;
  int _choice = 0;
  final Stopwatch _renderWatch = Stopwatch();
  final Stopwatch _postTransferWatch = Stopwatch();
  static const _choices = [
    (1, false),
    (1, true),
    (2, true),
    (2, false),
    (3, false),
    (3, true),
  ];

  void record(Map<String, Object?> event) {
    if (_events.length >= 128) _events.removeAt(0);
    _events.add(Map.unmodifiable(event));
    if (event['kind'] == 'http' &&
        event['endpoint'] == 'bytes' &&
        event['ok'] == true) {
      _postTransferWatch
        ..reset()
        ..start();
    }
  }

  @override
  void onCreate(BlocBase bloc) {
    super.onCreate(bloc);
    if (bloc is ReadingImageComparisonCubit) _cubit = bloc;
  }

  @override
  void onChange(BlocBase bloc, Change change) {
    super.onChange(bloc, change);
    if (bloc is! ReadingImageComparisonCubit ||
        change.nextState is! ReadingImageComparisonState) {
      return;
    }
    final state = change.nextState as ReadingImageComparisonState;
    if (state.status == 'loading') {
      _renderWatch
        ..reset()
        ..start();
    }
    final event = <String, Object?>{
      'kind': 'state',
      'status': state.status,
      'entryIndex': state.entryIndex,
      'canonicalSource': state.canonicalSource,
      'epoch': state.epoch,
      ...ownedResources(),
    };
    final comparison = state.comparison;
    if (state.status == 'loaded' &&
        comparison != null &&
        state.geometry != null) {
      final entry = comparison.manifest.images[state.entryIndex];
      final id = comparison.manifest.identity;
      final candidate = entry.renditions.single;
      final raster = state.canonicalSource
          ? entry.source.raster
          : candidate.raster;
      final geometry = state.geometry!;
      event.addAll({
        'contentId': id.contentId,
        'language': id.language,
        'revision': id.revision,
        'manifestDigest': id.manifestDigest,
        'jobId': id.jobId,
        'liveGeneration': comparison.liveGeneration,
        'role': entry.source.binding.role.name,
        'pageNumber': entry.source.binding.pageNumber,
        'renditionKey': candidate.renditionKey,
        'profileFingerprint': candidate.profileFingerprint,
        'sourceSha256': entry.source.raster.sha256,
        'displaySha256': raster.sha256,
        'encodedWidth': raster.width,
        'encodedHeight': raster.height,
        'expectedPayloadBytes': raster.byteSize,
        'decodedWidth': state.raster?.image?.width,
        'decodedHeight': state.raster?.image?.height,
        'physicalWidth': geometry.physicalWidth,
        'physicalHeight': geometry.physicalHeight,
        'requiredWidth': geometry.requiredWidth,
        'requiredHeight': geometry.requiredHeight,
        'renderElapsedMillis': _renderWatch.elapsedMilliseconds,
        'postTransferToLoadedUpperBoundMillis':
            _postTransferWatch.elapsedMilliseconds,
      });
    }
    record(event);
  }

  Map<String, Object?> ownedResources() => {
    'activeBuffers': ReadingComparisonRaster.activeBuffers,
    'activeDescriptors': ReadingComparisonRaster.activeDescriptors,
    'activeCodecs': ReadingComparisonRaster.activeCodecs,
    'activeImages': ReadingComparisonRaster.activeImages,
  };

  Map<String, Object?> snapshot() {
    Map<String, Object?>? slot;
    void visit(Element element) {
      if (element.widget.key == const ValueKey('reading-comparison-slot')) {
        final box = element.findRenderObject();
        if (box is RenderBox && box.hasSize) {
          slot = {
            'logicalWidth': box.size.width,
            'logicalHeight': box.size.height,
            'dpr': MediaQuery.devicePixelRatioOf(element),
          };
        }
      }
      element.visitChildElements(visit);
    }

    WidgetsBinding.instance.rootElement?.visitChildElements(visit);
    return {
      'ok': true,
      'configured': _configured,
      'stopped': _stopped,
      'credentialActive':
          _configuration?.account?.credentials.isActive ?? false,
      'deadlineActive': _deadline?.isActive ?? false,
      'status': _cubit?.state.status,
      'slot': slot,
      'events': List.of(_events),
      ...ownedResources(),
    };
  }

  Future<String> handle(String? message) async {
    try {
      _require(message != null && message.length <= 20000);
      final input = jsonDecode(message!) as Map<String, dynamic>;
      switch (input['action']) {
        case 'configure':
          _require(!_configured && !_stopped);
          final configuration = parseComparisonConfiguration(input);
          _configured = true;
          _configuration = configuration;
          _deadline = Timer(
            const Duration(seconds: 180),
            () => unawaited(stop()),
          );
          try {
            if (initializeDependencies != null) {
              await initializeDependencies!(configuration);
            } else {
              configureReadingImageComparisonDependencies(configuration);
              await comparisonGetIt
                  .unregister<ReadingImageComparisonDataSource>();
              comparisonGetIt.registerFactory<ReadingImageComparisonDataSource>(
                () => ObservedReadingComparisonSource(configuration, record),
              );
            }
            _require(!_stopped && configuration.account!.credentials.isActive);
            runApp(
              ReadingImageComparisonApp(contentId: input['contentId'] as int),
            );
          } catch (_) {
            await stop();
            rethrow;
          }
          return jsonEncode({'ok': true, 'configured': true});
        case 'paint':
          _require(input.length == 2 && input['epoch'] is int);
          return jsonEncode(await painted(input['epoch'] as int));
        case 'snapshot':
          _require(input.length == 1);
          return jsonEncode(snapshot());
        case 'select':
          _require(
            input.length == 3 &&
                input.keys.toSet().containsAll({
                  'action',
                  'entryIndex',
                  'canonicalSource',
                }),
          );
          _require(
            _configured &&
                !_stopped &&
                _cubit?.state.status == 'loaded' &&
                _choice < 5,
          );
          final next = _choices[_choice + 1];
          _require(
            input['entryIndex'] == next.$1 &&
                input['canonicalSource'] == next.$2,
          );
          _choice++;
          _cubit!.changeChoice(next.$1, next.$2);
          return jsonEncode({'ok': true});
        case 'stop':
          _require(input.length == 1);
          await stop();
          return jsonEncode(snapshot());
        default:
          throw const FormatException('probe_refused');
      }
    } catch (_) {
      return jsonEncode({'ok': false, 'code': 'probe_refused'});
    }
  }

  /// Tamamlanan frame aynı epoch ve aynı owned raster'ı hâlâ taşımalıdır.
  Future<Map<String, Object?>> painted(int epoch) async {
    final cubit = _cubit;
    final raster = cubit?.state.raster;
    final image = raster?.image;
    final binding = WidgetsBinding.instance;
    _require(
      !_stopped &&
          cubit != null &&
          cubit.state.status == 'loaded' &&
          cubit.state.epoch == epoch &&
          image != null &&
          ReadingComparisonRaster.activeImages == 1 &&
          binding.lifecycleState == AppLifecycleState.resumed,
    );
    RenderBox? paintedBox;
    void visit(Element element) {
      if (element.widget case ReadingImageComparisonRaster widget) {
        if (identical(widget.raster, raster)) {
          final render = element.findRenderObject();
          if (render is RenderBox &&
              render.attached &&
              render.hasSize &&
              !render.size.isEmpty) {
            paintedBox = render;
          }
        }
      }
      element.visitChildElements(visit);
    }

    binding.rootElement?.visitChildElements(visit);
    _require(paintedBox != null);
    paintedBox!.markNeedsPaint();
    binding.scheduleFrame();
    await binding.endOfFrame.timeout(const Duration(seconds: 5));
    _require(
      !_stopped &&
          cubit!.state.status == 'loaded' &&
          cubit.state.epoch == epoch &&
          identical(cubit.state.raster, raster) &&
          identical(raster!.image, image) &&
          paintedBox!.attached &&
          !paintedBox!.debugNeedsPaint &&
          ReadingComparisonRaster.activeImages == 1 &&
          binding.lifecycleState == AppLifecycleState.resumed,
    );
    return {...snapshot(), 'paintedEpoch': epoch};
  }

  Future<void> stop() => _stopping ??= _stopOwned();

  Future<void> _stopOwned() async {
    _stopped = true;
    _deadline?.cancel();
    final configuration = _configuration;
    _configuration = null;
    try {
      configuration?.account?.credentials.revoke();
      _cubit?.retire('driver_closed');
      runApp(const ReadingComparisonWaiting());
      if (comparisonGetIt.isRegistered<ReadingComparisonWorkGate>()) {
        await comparisonGetIt<ReadingComparisonWorkGate>().run(() async {});
      }
    } finally {
      await configuration?.account?.credentials.close();
    }
  }
}
