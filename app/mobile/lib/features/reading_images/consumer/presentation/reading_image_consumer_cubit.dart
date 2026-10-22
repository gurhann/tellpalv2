import 'dart:async';
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/reading_image_consumer_configuration.dart';
import '../../../../core/network/library_image_cache.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../domain/load_reading_image.dart';
import '../domain/reading_image_consumer_ownership.dart';
import '../domain/reading_image_admission.dart';
import 'reading_image_memory_cache.dart';
import 'reading_image_consumer_state.dart';

@injectable
class ReadingImageConsumerCubit extends Cubit<ReadingImageConsumerState> {
  ReadingImageConsumerCubit(this.loadImage, this.config, this.ownership)
    : super(const ReadingImageConsumerState()) {
    _account = config.accountChanges.listen((_) => suspend());
  }
  final LoadReadingImage loadImage;
  final ReadingImageConsumerConfiguration config;
  final ReadingImageConsumerOwnership ownership;
  ReadingImageMemoryCache? _cache;
  ReadingImageMemoryCache get cache => _cache!;
  ReadingImageConsumerPermit? _permit;
  Future<void> _retireCache() async {
    await Future.wait(_polls.toList());
    await Future.wait(_providers.toList());
    await Future.wait(_decodes.toList());
    await _cache?.close();
    _cache = null;
    _retirementFailed = false;
    _permit?.release();
    _permit = null;
  }

  ReadingImageCancellation _cancel = ReadingImageCancellation();
  ReadingImageAdmission? _anchor;
  Timer? _lease, _status;
  StreamSubscription<void>? _account;
  final _polls = <Future<void>>{};
  final _decodes = <Future<void>>{};
  final _providers = <Future<void>>{};
  Future<void>? _closing;
  void ownProvider(Future<void> done) {
    _providers.add(done);
    unawaited(
      done.then(
        (_) => _providers.remove(done),
        onError: (Object _, StackTrace _) => _providers.remove(done),
      ),
    );
  }

  Future<void> _work = Future.value();
  Future<void> _cleanup = Future.value();
  static int _nextOwner = 0;
  final int _owner = ++_nextOwner;
  int _epoch = 0;
  bool _retired = false;
  bool _retirementFailed = false;
  int? _id;
  String? _language;
  ReadingImageRole? _role;
  int? _page;
  double? _width, _height, _dpr;
  ReadingImageFit? _fit;
  int _decodeWidthLimit = 1080;
  bool current(int attempt) =>
      !isClosed &&
      !_retired &&
      attempt == _epoch &&
      !_cancel.cancelled &&
      (state.status == ReadingImageConsumerStatus.admitted ||
          state.status == ReadingImageConsumerStatus.loaded);
  void ownDecode(Future<void> done) {
    _decodes.add(done);
    unawaited(
      done.then(
        (_) => _decodes.remove(done),
        onError: (Object _, StackTrace _) => _decodes.remove(done),
      ),
    );
  }

  Future<void> load({
    required int contentId,
    required String language,
    required ReadingImageRole role,
    required int pageNumber,
    required double width,
    required double height,
    required double dpr,
    required ReadingImageFit fit,
    int maxDecodeWidth = 1080,
  }) {
    if (isClosed || _retired) return Future.value();
    _id = contentId;
    _language = language;
    _role = role;
    _page = pageNumber;
    _width = width;
    _height = height;
    _dpr = dpr;
    _fit = fit;
    _decodeWidthLimit = maxDecodeWidth;
    final previous = _work;
    final previousCleanup = _cleanup;
    final previousTransfer = state.transferKey;
    final attempt = ++_epoch;
    _cancel.cancel();
    _lease?.cancel();
    _status?.cancel();
    emit(ReadingImageConsumerState(attempt: attempt));
    final cancel = ReadingImageCancellation();
    _cancel = cancel;
    return _work = _load(
      previous,
      previousCleanup,
      previousTransfer,
      cancel,
      attempt,
      contentId,
      language,
      role,
      pageNumber,
      width,
      height,
      dpr,
      fit,
      maxDecodeWidth,
    );
  }

  Future<void> retry() => load(
    contentId: _id!,
    language: _language!,
    role: _role!,
    pageNumber: _page!,
    width: _width!,
    height: _height!,
    dpr: _dpr!,
    fit: _fit!,
    maxDecodeWidth: _decodeWidthLimit,
  );
  Future<void> _load(
    Future<void> previous,
    Future<void> previousCleanup,
    String? previousTransfer,
    ReadingImageCancellation cancel,
    int attempt,
    int id,
    String language,
    ReadingImageRole role,
    int page,
    double width,
    double height,
    double dpr,
    ReadingImageFit fit,
    int maxDecodeWidth,
  ) async {
    try {
      await previous;
      await previousCleanup;
      await Future.wait(_polls.toList());
      await Future.wait(_providers.toList());
      await Future.wait(_decodes.toList());
      if (_retirementFailed) await _retireCache();
      await _cache?.settleOwnedWork();
      if (previousTransfer != null) {
        await _cache?.discardFailedTransfer(previousTransfer, failed: false);
      }
      if (isClosed || _retired || attempt != _epoch || cancel.cancelled) return;
      _permit ??= await ownership.acquire(
        cancellation: cancel,
        wait: config.requestWait,
      );
      if (isClosed || _retired || attempt != _epoch || cancel.cancelled) return;
      _cache ??= ReadingImageMemoryCache.create(config, loadImage);
      final result = await loadImage(id, language, cancel);
      if (isClosed || _retired || attempt != _epoch || cancel.cancelled) return;
      await result.fold((_) async => _fail(attempt), (admitted) async {
        if (_anchor != null && !_anchor!.sameIdentity(admitted.admission)) {
          _fail(attempt);
          return;
        }
        _anchor ??= admitted.admission;
        final matches = admitted.manifest.descriptor.images.where(
          (e) =>
              e.source.binding.role == role &&
              e.source.binding.pageNumber == page,
        );
        if (matches.length != 1) {
          _fail(attempt);
          return;
        }
        final entry = matches.single;
        final selected = const SelectReadingImageRendition()(
          manifest: admitted.manifest,
          binding: entry.source.binding,
          acceptedProfileFingerprints: admitted
              .admission
              .context
              .profiles[role]!
              .map((p) => p.fingerprint)
              .toSet(),
          logicalWidth: width,
          logicalHeight: height,
          devicePixelRatio: dpr,
          fit: fit,
          maxDecodePixels: config.maxDecodePixels,
        );
        selected.fold((_) => _fail(attempt), (selection) {
          if (maxDecodeWidth <= 0 ||
              maxDecodeWidth > 1600 ||
              selection.geometry.decodeWidth > maxDecodeWidth ||
              selection.geometry.decodeHeight > 2048) {
            _fail(attempt);
            return;
          }
          final identity = admitted.admission.manifest.identity;
          final immutable = sha256
              .convert(
                utf8.encode(
                  jsonEncode([
                    config.accountScope,
                    identity.contentId,
                    identity.language,
                    admitted.admission.liveGeneration,
                    identity.revision,
                    identity.manifestDigest,
                    identity.jobId,
                    role.name,
                    page,
                    selection.rendition.renditionKey,
                    selection.rendition.raster.sha256,
                  ]),
                ),
              )
              .toString();
          final transfer = LibraryImageCache.transferKey(
            immutable,
            '$_owner:$attempt',
          );
          final query = {
            ...admitted.admission.query,
            'role': switch (role) {
              ReadingImageRole.readingCover => 'READING_COVER',
              ReadingImageRole.readingDetail => 'READING_DETAIL',
              ReadingImageRole.readingPage => 'READING_PAGE',
            },
            'pageNumber': '$page',
            'renditionKey': selection.rendition.renditionKey,
          };
          final url = config.origin
              .resolve('/api/library/reading-images/bytes')
              .replace(queryParameters: query)
              .toString();
          cache.admit(
            admission: admitted.admission,
            binding: entry.source.binding,
            selection: selection,
            transfer: transfer,
            url: url,
            cancellation: cancel,
            isCurrent: () => current(attempt),
          );
          emit(
            ReadingImageConsumerState(
              attempt: attempt,
              status: ReadingImageConsumerStatus.admitted,
              admission: admitted,
              selection: selection,
              immutableKey: immutable,
              transferKey: transfer,
              url: url,
            ),
          );
          _lease = Timer(admitted.remaining, () => suspend());
          _poll(attempt, admitted.admission, cancel);
        });
      });
    } catch (_) {
      _retirementFailed = _permit != null;
      _fail(attempt);
    }
  }

  void _poll(
    int attempt,
    ReadingImageAdmission anchor,
    ReadingImageCancellation cancel,
  ) {
    _status = Timer(config.statusEvery, () async {
      if (!current(attempt)) return;
      final request = loadImage.status(anchor, cancel);
      final settled = request.then<void>(
        (_) {},
        onError: (Object _, StackTrace _) {},
      );
      _polls.add(settled);
      try {
        final result = await request;
        if (!current(attempt)) return;
        result.fold((_) => suspend(), (_) => _poll(attempt, anchor, cancel));
      } catch (_) {
        if (current(attempt)) suspend();
      } finally {
        _polls.remove(settled);
      }
      return;
    });
  }

  void decoded(int attempt) {
    if (!current(attempt) ||
        state.status == ReadingImageConsumerStatus.loaded) {
      return;
    }
    emit(state.copyWith(status: ReadingImageConsumerStatus.loaded));
    unawaited(
      cache.rememberValidated(
        state.immutableKey!,
        state.transferKey!,
        isCurrent: () => current(attempt),
      ),
    );
  }

  void failed(int attempt) {
    if (current(attempt)) _fail(attempt);
  }

  void _scheduleRetirement() {
    final work = _work;
    final previous = _cleanup;
    _cleanup = () async {
      await previous;
      await work;
      try {
        await _retireCache();
      } catch (_) {
        _retirementFailed = true;
      }
    }();
  }

  void _fail(int attempt) {
    if (isClosed || attempt != _epoch) return;
    _cancel.cancel();
    _lease?.cancel();
    _status?.cancel();
    emit(
      ReadingImageConsumerState(
        attempt: attempt,
        status: ReadingImageConsumerStatus.error,
      ),
    );
    _scheduleRetirement();
  }

  void suspend() {
    if (isClosed) return;
    _cancel.cancel();
    _lease?.cancel();
    _status?.cancel();
    ++_epoch;
    emit(
      ReadingImageConsumerState(
        attempt: _epoch,
        status: ReadingImageConsumerStatus.suspended,
      ),
    );
    _scheduleRetirement();
  }

  @override
  Future<void> close() async {
    final closing = _closing ??= _close();
    try {
      await closing;
    } finally {
      if (identical(_closing, closing)) _closing = null;
    }
    if (!isClosed) await super.close();
  }

  Future<void> _close() async {
    if (!_retired) {
      _retired = true;
      _cancel.cancel();
      _lease?.cancel();
      _status?.cancel();
      ++_epoch;
      await _account?.cancel();
      await _work;
      await _cleanup;
    }
    await _retireCache();
  }
}
