import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'reading_resource_observation.dart';
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
import 'package:tellpal_mobile_poc/app/reading_image_consumer_qa/consumer_image_slot.dart';

const consumerQaRunId = 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09';
const consumerQaProposalSha =
    '079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6';

/// Beklenen kapsam APK girişinde seçilir; runtime mesajından öğrenilmez.
class ConsumerQaExpectedScope {
  const ConsumerQaExpectedScope._(
    this.proposalSha256,
    this.windowMillis,
    this.maxStatusChecks,
    this.measurementId,
  );
  static const legacy = ConsumerQaExpectedScope._(
    consumerQaProposalSha,
    180000,
    18,
    null,
  );
  static const p4b5 = ConsumerQaExpectedScope._(
    'e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833',
    120000,
    12,
    'p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8',
  );
  final String proposalSha256;
  final int windowMillis;
  final int maxStatusChecks;
  final String? measurementId;
}

void _require(bool condition) {
  if (!condition) throw const FormatException('probe_refused');
}

/// Sırlar yalnız runtime mesajından gelir. Sabit sayılar bu tek onaylı denemeye aittir.
ReadingImageComparisonConfiguration parseConsumerQaConfiguration(
  Map<String, dynamic> input, {
  ConsumerQaExpectedScope expectedScope = ConsumerQaExpectedScope.legacy,
}) {
  _require(
    input.keys.toSet().difference({
          if (expectedScope.measurementId != null) 'measurementId',
          'action',
          'runId',
          'proposalSha256',
          'uid',
          'contentId',
          'port',
          'token',
          'windowStartedAtEpochMillis',
          'windowDeadlineEpochMillis',
        }).isEmpty &&
        input.length == (expectedScope.measurementId == null ? 9 : 10),
  );
  _require(
    input['action'] == 'configure' &&
        input['runId'] == consumerQaRunId &&
        input['proposalSha256'] == expectedScope.proposalSha256 &&
        (expectedScope.measurementId == null ||
            input['measurementId'] == expectedScope.measurementId) &&
        input['uid'] == imageExperimentQaUid,
  );
  _require(input['contentId'] is int && input['contentId'] == 94);
  _require(
    input['port'] is int && input['port'] >= 1024 && input['port'] <= 65535,
  );
  final started = input['windowStartedAtEpochMillis'];
  final deadline = input['windowDeadlineEpochMillis'];
  final current = DateTime.now().millisecondsSinceEpoch;
  _require(
    started is int &&
        deadline is int &&
        deadline - started == expectedScope.windowMillis &&
        started <= current &&
        current < deadline,
  );
  final remaining = deadline - current;
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
        DateTime.fromMillisecondsSinceEpoch(deadline, isUtc: true),
      ),
    ),
    profile: ReadingComparisonLimits(
      runId: consumerQaRunId,
      maxEncodedBytes: 8388608,
      maxBufferBytes: 25165824,
      maxDecodePixels: 4800000,
      maxDecodedBytes: 20971520,
      maxPeakBytes: 134217728,
      maxOwnedImages: 1,
      transferMillis: 45000,
      decodeMillis: 10000,
      statusEveryMillis: 10000,
      maxStatusChecks: expectedScope.maxStatusChecks,
      maxControlMillis: remaining,
    ),
  );
}

class ReadingConsumerQaWaiting extends StatelessWidget {
  const ReadingConsumerQaWaiting({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    theme: AppTypography.theme(),
    home: const Scaffold(
      body: Center(child: Text('QA çalıştırıcısı bekleniyor')),
    ),
  );
}

/// İzin ve sayım gerçek HTTP'den önce yapılır; body/query/token kanıta girmez.
class ObservedConsumerQaSource extends ReadingImageComparisonDataSource {
  ObservedConsumerQaSource(
    super.configuration,
    this.record, {
    this.expectedScope = ConsumerQaExpectedScope.legacy,
  });
  final ConsumerQaExpectedScope expectedScope;
  final void Function(Map<String, Object?>) record;
  int manifests = 0, statuses = 0, media = 0;
  bool failed = false;
  static const _keys = [
    '8d3c882f7d82a7f4e0f80894b04322407494dc96eb4e26bb3408de0e501a3c45',
    'a02aac0b9869606049e1714b72faa459fef9913363e3801e8da62c02f5dbab92',
    '02d8b7d9015ec2cb1a930552392bac42e33c951c7232bad8ff076075c23c1226',
    '8d3c882f7d82a7f4e0f80894b04322407494dc96eb4e26bb3408de0e501a3c45',
  ];
  void reserve(String path, Map<String, String> query) {
    _require(!failed);
    try {
      _require(query['contentId'] == '94' && query['language'] == 'tr');
      if (path == 'manifest') {
        _require(manifests == 0 && query.length == 2);
        manifests++;
      } else {
        _require(
          query['liveGeneration'] == '4' &&
              query['revision'] ==
                  '3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d' &&
              query['manifestDigest'] ==
                  'd4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2' &&
              query['jobId'] == 'bb9ae40b-e5ab-4054-885e-0b1fc14e841e',
        );
        if (path == 'status') {
          _require(
            statuses < expectedScope.maxStatusChecks && query.length == 6,
          );
          statuses++;
        } else {
          _require(
            path == 'bytes' &&
                media < 4 &&
                query.length == 10 &&
                query['canonicalSource'] == 'false' &&
                query['renditionKey'] == _keys[media] &&
                query['role'] ==
                    (media == 0 || media == 3
                        ? 'READING_DETAIL'
                        : 'READING_PAGE') &&
                query['pageNumber'] ==
                    (media == 0 || media == 3 ? '0' : '$media'),
          );
          media++;
        }
      }
      _require(
        manifests + statuses + media <= 5 + expectedScope.maxStatusChecks,
      );
      record({
        'kind': 'httpReservation',
        'endpoint': path,
        'ordinal': manifests + statuses + media,
      });
    } catch (_) {
      failed = true;
      rethrow;
    }
  }

  @override
  Future<Uint8List> read(
    String path,
    Map<String, String> query,
    ReadingComparisonCancellation cancellation,
    int limit,
  ) async {
    reserve(path, query);
    final watch = Stopwatch()..start();
    try {
      final bytes = await super.read(path, query, cancellation, limit);
      record({
        'kind': 'http',
        'endpoint': path,
        'ok': true,
        'payloadBytes': bytes.length,
        'elapsedMillis': watch.elapsedMilliseconds,
      });
      return bytes;
    } catch (_) {
      failed = true;
      record({
        'kind': 'http',
        'endpoint': path,
        'ok': false,
        'elapsedMillis': watch.elapsedMilliseconds,
      });
      rethrow;
    }
  }
}

/// Cubit ve native kaynak sahipliğini gözler; uygulama davranışını değiştirmez.
class ReadingConsumerQaProbe extends BlocObserver {
  ReadingConsumerQaProbe({
    this.initializeDependencies,
    this.expectedScope = ConsumerQaExpectedScope.legacy,
  });
  final ConsumerQaExpectedScope expectedScope;
  bool _configureAttempted = false;
  final FutureOr<void> Function(ReadingImageComparisonConfiguration)?
  initializeDependencies;
  int? _lastPaintedEpoch;
  Future<void>? _stopping;
  final List<Map<String, Object?>> _events = [];
  ReadingImageComparisonCubit? _cubit;
  int _cubitCount = 0;
  ReadingImageComparisonConfiguration? _configuration;
  Timer? _deadline;
  bool _configured = false, _stopped = false;
  final Stopwatch _renderWatch = Stopwatch();
  final Stopwatch _postTransferWatch = Stopwatch();
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
    if (bloc is ReadingImageComparisonCubit) {
      _cubitCount++;
      _require(_cubitCount == 1);
      _cubit = bloc;
    }
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
        'pageId': entry.source.binding.pageId,
        'decodeWidth': geometry.decodeWidth,
        'decodeHeight': geometry.decodeHeight,
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

  Map<String, Object?> ownedResources() => readingOwnedResources();

  Map<String, Object?> snapshot() {
    Map<String, Object?>? slot;
    var liveRasterAttached = false;
    final currentRaster = _cubit?.state.raster;
    void visit(Element element) {
      if (element.widget case ReadingImageComparisonRaster widget) {
        final render = element.findRenderObject();
        if (identical(widget.raster, currentRaster) &&
            currentRaster?.image != null &&
            render is RenderBox &&
            render.attached &&
            render.hasSize &&
            !render.size.isEmpty &&
            (ModalRoute.of(element)?.isCurrent ?? true)) {
          liveRasterAttached = true;
        }
      }
      if (element.widget.key == const ValueKey('reading-consumer-slot') &&
          (ModalRoute.of(element)?.isCurrent ?? true)) {
        final box = element.findRenderObject();
        if (box is RenderBox && box.hasSize) {
          slot = {
            'logicalWidth': box.size.width,
            'logicalHeight': box.size.height,
            'dpr': MediaQuery.devicePixelRatioOf(element),
            'fit': element
                .findAncestorWidgetOfExactType<ReadingConsumerImageSlot>()
                ?.geometry
                .fit
                .name,
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
      'cubitClosed': _cubit?.isClosed ?? false,
      'cubitCount': _cubitCount,
      'slot': slot,
      'events': List.of(_events),
      if (expectedScope.measurementId != null) ...{
        'observedLifecycle': WidgetsBinding.instance.lifecycleState?.name,
        'currentEpoch': _cubit?.state.epoch,
        'lastPaintedEpoch': _lastPaintedEpoch,
        'liveRasterAttached': liveRasterAttached,
      },
      ...ownedResources(),
    };
  }

  Future<String> handle(String? message) async {
    try {
      _require(message != null && message.length <= 20000);
      final input = jsonDecode(message!) as Map<String, dynamic>;
      switch (input['action']) {
        case 'configure':
          _require(
            !_configured &&
                !_stopped &&
                (expectedScope.measurementId == null || !_configureAttempted),
          );
          _configureAttempted = true;
          final configuration = parseConsumerQaConfiguration(
            input,
            expectedScope: expectedScope,
          );
          _configured = true;
          _configuration = configuration;
          _deadline = Timer(
            configuration.account!.credentials.expiresAt.difference(
              DateTime.now(),
            ),
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
                () => ObservedConsumerQaSource(
                  configuration,
                  record,
                  expectedScope: expectedScope,
                ),
              );
            }
            _require(!_stopped && configuration.account!.credentials.isActive);
            runApp(
              ReadingImageComparisonApp(
                contentId: input['contentId'] as int,
                consumerQa: true,
              ),
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
    _lastPaintedEpoch = epoch;
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
      await _cubit?.close();
      runApp(const ReadingConsumerQaWaiting());
      if (comparisonGetIt.isRegistered<ReadingComparisonWorkGate>()) {
        await comparisonGetIt<ReadingComparisonWorkGate>().run(() async {});
      }
    } finally {
      await configuration?.account?.credentials.close();
    }
  }
}
