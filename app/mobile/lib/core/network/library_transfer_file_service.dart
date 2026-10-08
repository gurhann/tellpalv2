import 'dart:async';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:http/http.dart' as http;

/// Bounds the actual transport, independently of the presentation deadline.
/// Each native transfer owns its client so expiry/closure releases its socket.
class LibraryTransferFileService extends FileService {
  LibraryTransferFileService({
    required Duration artworkWait,
    FileService? delegate,
    http.Client Function()? clientFactory,
    this.maximumTransferBytes,
  }) : totalWait = Duration(microseconds: artworkWait.inMicroseconds * 4 ~/ 5),
       headerWait = Duration(microseconds: artworkWait.inMicroseconds * 2 ~/ 5),
       idleWait = Duration(microseconds: artworkWait.inMicroseconds * 2 ~/ 5),
       _delegate = delegate,
       _clientFactory = clientFactory ?? http.Client.new;

  static const ownershipHeader = 'x-library-local-transfer';
  final int? maximumTransferBytes;
  final Duration totalWait;
  final Duration headerWait;
  final Duration idleWait;
  final FileService? _delegate;
  final http.Client Function() _clientFactory;
  final _transfers = <String, _Transfer>{};
  bool _closed = false;
  void Function(String owner, Map<String, Object?> event)? failureObserver;

  void cancel(String owner) => _transfers[owner]?.cancel();
  void close() {
    _closed = true;
    for (final transfer in _transfers.values.toList()) {
      transfer.cancel();
    }
  }

  @override
  Future<FileServiceResponse> get(
    String url, {
    Map<String, String>? headers,
  }) async {
    if (_closed) throw StateError('Library image transport is closed');
    final outgoing = Map<String, String>.of(headers ?? {});
    final owner = outgoing.remove(ownershipHeader)!;
    final client = _delegate == null ? _clientFactory() : null;
    final transfer = _Transfer(client, (phase, limit, elapsed) {
      try {
        failureObserver?.call(
          owner,
          Map.unmodifiable({
            'transportPhase': phase,
            'transportMilliseconds': elapsed,
            'transportLimitMilliseconds': limit.inMilliseconds,
          }),
        );
      } catch (_) {
        // Optional observations cannot replace or delay transport failure.
      }
    });
    _transfers[owner] = transfer;
    transfer.timer = Timer(totalWait, () {
      transfer.observeFailure('total', totalWait);
      transfer.cancel();
      _transfers.remove(owner);
    });
    try {
      final response =
          await Future.any([
            (_delegate ?? HttpFileService(httpClient: client)).get(
              url,
              headers: outgoing,
            ),
            transfer.cancelled.future.then<FileServiceResponse>(
              (_) => throw TimeoutException('Artwork transfer expired'),
            ),
          ]).timeout(
            headerWait,
            onTimeout: () {
              transfer.observeFailure('response-headers', headerWait);
              transfer.cancel();
              throw TimeoutException('Artwork response headers expired');
            },
          );
      if (transfer.cancelled.isCompleted) {
        throw TimeoutException('Artwork transfer expired');
      }
      if (![200, 202, 304].contains(response.statusCode)) {
        transfer.finish();
        _transfers.remove(owner);
      }
      if (maximumTransferBytes != null &&
          (response.contentLength ?? 0) > maximumTransferBytes!) {
        throw StateError('Artwork exceeds encoded byte bound');
      }
      return _Response(
        response,
        transfer,
        idleWait,
        () => _transfers.remove(owner),
        maximumTransferBytes,
      );
    } catch (_) {
      transfer.finish();
      _transfers.remove(owner);
      rethrow;
    }
  }
}

class _Transfer {
  _Transfer(this.client, this._observe);
  final void Function(String, Duration, int) _observe;
  final _elapsed = Stopwatch()..start();
  bool _observed = false;
  void observeFailure(String phase, Duration limit) {
    if (_observed) return;
    _observed = true;
    _observe(phase, limit, _elapsed.elapsedMilliseconds);
  }

  final http.Client? client;
  final cancelled = Completer<void>();
  Timer? timer;
  void cancel() {
    if (!cancelled.isCompleted) cancelled.complete();
    client?.close();
  }

  void finish() {
    timer?.cancel();
    client?.close();
  }
}

class _Response implements FileServiceResponse {
  _Response(
    this._response,
    this._transfer,
    this._idleWait,
    this._finished,
    this._maximumBytes,
  );
  final int? _maximumBytes;
  final FileServiceResponse _response;
  final _Transfer _transfer;
  final Duration _idleWait;
  final void Function() _finished;
  @override
  Stream<List<int>> get content {
    late StreamController<List<int>> controller;
    StreamSubscription<List<int>>? subscription;
    bool ended = false;
    int received = 0;
    Future<void> finish([Object? error, StackTrace? stack]) async {
      if (ended) return;
      ended = true;
      if (error != null) controller.addError(error, stack);
      await subscription?.cancel();
      _transfer.finish();
      _finished();
      await controller.close();
    }

    controller = StreamController<List<int>>(
      onListen: () {
        if (_transfer.cancelled.isCompleted) {
          unawaited(finish(TimeoutException('Artwork transfer expired')));
          return;
        }
        subscription = _response.content
            .timeout(_idleWait)
            .listen(
              (chunk) {
                received += chunk.length;
                if (_maximumBytes != null && received > _maximumBytes) {
                  _transfer.cancel();
                  unawaited(
                    finish(StateError('Artwork exceeds encoded byte bound')),
                  );
                } else if (!ended) {
                  controller.add(chunk);
                }
              },
              onError: (Object error, StackTrace stack) {
                if (error is TimeoutException &&
                    !_transfer.cancelled.isCompleted) {
                  _transfer.observeFailure('response-body-idle', _idleWait);
                }
                unawaited(
                  finish(
                    _transfer.cancelled.isCompleted
                        ? TimeoutException('Artwork transfer expired')
                        : error,
                    stack,
                  ),
                );
              },
              onDone: () => unawaited(finish()),
            );
        unawaited(
          _transfer.cancelled.future.then(
            (_) => finish(TimeoutException('Artwork transfer expired')),
          ),
        );
      },
      onCancel: () async {
        _transfer.cancel();
        await subscription?.cancel();
        _transfer.finish();
        _finished();
      },
    );
    return controller.stream;
  }

  @override
  int? get contentLength => _response.contentLength;
  @override
  int get statusCode => _response.statusCode;
  @override
  DateTime get validTill {
    final maximum = DateTime.now().add(const Duration(minutes: 10));
    return _response.validTill.isBefore(maximum)
        ? _response.validTill
        : maximum;
  }

  @override
  String? get eTag => _response.eTag;
  @override
  String get fileExtension => _response.fileExtension;
}
