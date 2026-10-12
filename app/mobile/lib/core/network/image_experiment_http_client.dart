import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:http/http.dart' as http;
import '../../app/image_experiment_configuration.dart';
import '../../app/image_experiment_json.dart';
import '../diagnostics/image_experiment_recorder.dart';

/// Discarded, connection-free contexts compare formats only after an original import fails.
class ImageExperimentCertificateImportDiagnostic {
  static const maximumBytes = 16384;
  static const maximumMicroseconds = 200000;

  static void observe(
    void Function() originalImport,
    Uint8List der, {
    ImageExperimentRefusalObserver? observer,
  }) {
    try {
      originalImport();
    } catch (_) {
      final observation = ImageExperimentRefusalObservation.current(observer);
      if (observation != null) {
        ImageExperimentRefusalObservation.report(compare(der), observation);
      }
      rethrow;
    }
  }

  static ImageExperimentFixtureStage compare(
    Uint8List der, {
    void Function(List<int>)? importCertificateForTesting,
    int Function()? elapsedMicrosecondsForTesting,
  }) {
    final elapsed = Stopwatch()..start();
    bool withinBound() =>
        (elapsedMicrosecondsForTesting?.call() ?? elapsed.elapsedMicroseconds) <
        maximumMicroseconds;
    try {
      if (der.isEmpty || der.length > maximumBytes || !withinBound()) {
        return ImageExperimentFixtureStage.certificateImportUnavailable;
      }
      final immutable = Uint8List.fromList(der).asUnmodifiableView();
      final encoded = base64Encode(immutable);
      final lines = [
        for (var offset = 0; offset < encoded.length; offset += 64)
          encoded.substring(
            offset,
            offset + 64 < encoded.length ? offset + 64 : encoded.length,
          ),
      ];
      final pem = utf8.encode(
        '-----BEGIN CERTIFICATE-----\n${lines.join('\n')}\n-----END CERTIFICATE-----\n',
      );
      bool imports(List<int> bytes) {
        final probe = importCertificateForTesting == null
            ? SecurityContext(withTrustedRoots: false)
            : null;
        try {
          if (importCertificateForTesting != null) {
            importCertificateForTesting(bytes);
          } else {
            probe!.setTrustedCertificatesBytes(bytes);
          }
          return true;
        } catch (_) {
          return false;
        }
      }

      if (!withinBound()) {
        return ImageExperimentFixtureStage.certificateImportUnavailable;
      }
      final derPasses = imports(immutable);
      if (!withinBound()) {
        return ImageExperimentFixtureStage.certificateImportUnavailable;
      }
      final pemPasses = imports(pem);
      if (!withinBound()) {
        return ImageExperimentFixtureStage.certificateImportUnavailable;
      }
      return derPasses
          ? (pemPasses
                ? ImageExperimentFixtureStage.certificateImportBothPass
                : ImageExperimentFixtureStage.certificateImportDerPassPemFail)
          : (pemPasses
                ? ImageExperimentFixtureStage.certificateImportDerFailPemPass
                : ImageExperimentFixtureStage.certificateImportBothFail);
    } catch (_) {
      return ImageExperimentFixtureStage.certificateImportUnavailable;
    }
  }
}

/// Owns status observation, the expiry clock and every per-transfer client.
class ImageExperimentTransport {
  ImageExperimentTransport(
    this.configuration,
    this.recorder, {
    ImageExperimentRefusalObserver? observeRefusal,
  }) : _observeRefusal = ImageExperimentRefusalObservation.current(
         observeRefusal,
       ) {
    _revocations = configuration.credentials.revocations.listen(
      (_) => _invalidate('session_inactive'),
    );
    _expiry = Timer(
      configuration.credentials.expiresAt.difference(DateTime.now()),
      () => _invalidate('session_inactive'),
    );
  }
  final ImageExperimentRefusalObserver? _observeRefusal;
  final ImageExperimentConfiguration configuration;
  final ImageExperimentRecorder recorder;
  static const observationZoneKey = #imageExperimentObservation;
  final _clients = <ImageExperimentHttpClient>{};
  StreamSubscription<void>? _revocations;
  Timer? _expiry, _poll;
  bool _polling = false, _closed = false, _closingStarted = false;
  Future<void>? _closing;
  bool _authenticatedRevokeObserved = false;
  bool get authenticatedRevokeObserved => _authenticatedRevokeObserved;
  http.Client newClient() => ImageExperimentRefusalObservation.construct(
    ImageExperimentFixtureStage.clientPreconditions,
    () {
      configuration.credentials.requireActive();
      if (_closingStarted) throw StateError('session_inactive');
      return _newOwnedClient();
    },
    observer: _observeRefusal,
  );

  http.Client _newOwnedClient() {
    var stage = ImageExperimentFixtureStage.clientPreconditions;
    try {
      configuration.credentials.requireActive();
      if (_closed) throw StateError('session_inactive');
      late ImageExperimentHttpClient client;
      stage = ImageExperimentFixtureStage.clientConstruction;
      client = ImageExperimentHttpClient(
        configuration,
        recorder,
        onClose: () => _clients.remove(client),
        observeRefusal: _observeRefusal,
        observation: Map<String, Object?>.from(
          Zone.current[observationZoneKey] as Map? ?? const {},
        ),
      );
      _clients.add(client);
      return client;
    } catch (_) {
      ImageExperimentRefusalObservation.report(stage, _observeRefusal);
      rethrow;
    }
  }

  Future<void> admitStatus() async {
    try {
      if (_closed || _closingStarted) throw StateError('session_inactive');
      await _checkStatus();
      if (_closed || _closingStarted) throw StateError('session_inactive');
      _poll ??= Timer.periodic(const Duration(seconds: 2), (_) {
        if (!_polling && !_closed && !_closingStarted) {
          unawaited(_checkStatus().catchError((_) {}));
        }
      });
    } catch (_) {
      ImageExperimentRefusalObservation.report(
        ImageExperimentFixtureStage.statusAdmission,
        _observeRefusal,
      );
      rethrow;
    }
  }

  Future<void> _checkStatus() async {
    if (_polling || _closed || _closingStarted) return;
    _polling = true;
    final elapsed = Stopwatch()..start();
    http.Client? client;
    var stage = ImageExperimentFixtureStage.statusClient;
    try {
      client = newClient();
      stage = ImageExperimentFixtureStage.statusAdmission;
      final response = await client
          .get(
            configuration.brokerOrigin.replace(
              path: '/experiment/v1/session/status',
            ),
          )
          .timeout(const Duration(seconds: 10));
      if (_closingStarted) return;
      stage = ImageExperimentFixtureStage.statusValidation;
      if (response.bodyBytes.length > 1024 || response.statusCode != 200) {
        throw StateError('status_unavailable');
      }
      final value =
          decodeImageExperimentJson(utf8.decode(response.bodyBytes))
              as Map<String, dynamic>;
      if (value.keys.toSet().difference({'status', 'reason'}).isNotEmpty ||
          value['status'] != 'admitted') {
        throw StateError('status_revoked');
      }
      configuration.credentials.requireActive();
      recorder.record({
        'event': 'status',
        'reason': 'none',
        'generation': configuration.generation,
        'observerOverheadMicroseconds': elapsed.elapsedMicroseconds,
      });
    } catch (_) {
      // This observer is intentionally retired before the authenticated revoke.
      if (_closingStarted) return;
      ImageExperimentRefusalObservation.report(stage, _observeRefusal);
      _invalidate('status_unavailable');
      throw StateError('session_inactive');
    } finally {
      client?.close();
      _polling = false;
    }
  }

  void _invalidate(String reason) {
    if (_closed) return;
    _closed = true;
    _poll?.cancel();
    _expiry?.cancel();
    for (final client in _clients.toList()) {
      client.close();
    }
    configuration.credentials.revoke();
    recorder.record({
      'event': 'revoke',
      'reason': reason,
      'generation': configuration.generation,
    });
  }

  Future<void> close() => _closing ??= _close();

  Future<void> _close() async {
    _closingStarted = true;
    _poll?.cancel();
    // Keep the expiry/revocation guards live, but retire all prior request owners.
    for (final client in _clients.toList()) {
      client.close();
    }
    if (!_closed && configuration.credentials.isActive) {
      http.Client? client;
      try {
        client = _newOwnedClient();
        final request = http.Request(
          'POST',
          configuration.brokerOrigin.replace(
            path: '/experiment/v1/session/revoke',
          ),
        );
        final response = await client
            .send(request)
            .timeout(const Duration(seconds: 2));
        final bytes = await response.stream.toBytes().timeout(
          const Duration(seconds: 2),
        );
        final value =
            decodeImageExperimentJson(utf8.decode(bytes))
                as Map<String, dynamic>;
        if (value.length != 2 ||
            value['status'] != 'revoked' ||
            value['reason'] != 'app_revoked') {
          throw const FormatException('cleanup_unavailable');
        }
        _authenticatedRevokeObserved = true;
      } catch (_) {
        recorder.record({
          'event': 'failure',
          'reason': 'status_unavailable',
          'generation': configuration.generation,
        });
      } finally {
        client?.close();
      }
    }
    _invalidate('session_inactive');
    await _revocations?.cancel();
  }
}

/// Certificate validation and pin checking finish before an HTTP request sees a bearer.
class ImageExperimentHttpClient extends http.BaseClient {
  ImageExperimentHttpClient(
    this.configuration,
    this.recorder, {
    void Function()? onClose,
    String? pinOverrideForTesting,
    this.observation = const {},
    ImageExperimentRefusalObserver? observeRefusal,
  }) : _observeRefusal = ImageExperimentRefusalObservation.current(
         observeRefusal,
       ),
       _onClose = onClose,
       _pin = pinOverrideForTesting ?? configuration.certificateSha256 {
    var stage = ImageExperimentFixtureStage.trustContext;
    try {
      final encoded = base64Encode(configuration.certificateDer);
      final lines = [
        for (var offset = 0; offset < encoded.length; offset += 64)
          encoded.substring(
            offset,
            offset + 64 < encoded.length ? offset + 64 : encoded.length,
          ),
      ];
      final pem =
          '-----BEGIN CERTIFICATE-----\n${lines.join('\n')}\n-----END CERTIFICATE-----\n';
      final context = SecurityContext(withTrustedRoots: false);
      stage = ImageExperimentFixtureStage.certificateImport;
      ImageExperimentCertificateImportDiagnostic.observe(
        () => context.setTrustedCertificatesBytes(utf8.encode(pem)),
        configuration.certificateDer,
        observer: _observeRefusal,
      );
      stage = ImageExperimentFixtureStage.clientConstruction;
      _native = HttpClient(context: context)
        ..connectionTimeout = const Duration(seconds: 10)
        ..findProxy = ((_) => 'DIRECT');
      _native.connectionFactory = (uri, proxyHost, proxyPort) async {
        ImageExperimentRefusalObservation.construct(
          ImageExperimentFixtureStage.requestPreconditions,
          () {
            _validateUri(uri);
            if (_closed || proxyHost != null) {
              throw StateError('transport_refused');
            }
          },
          observer: _observeRefusal,
        );
        late ConnectionTask<SecureSocket> task;
        try {
          task = await SecureSocket.startConnect(
            uri.host,
            uri.port,
            context: context,
          );
        } catch (_) {
          ImageExperimentRefusalObservation.report(
            ImageExperimentFixtureStage.tlsConnect,
            _observeRefusal,
          );
          rethrow;
        }
        _tasks.add(task);
        final socket = task.socket
            .then((socket) {
              _tasks.remove(task);
              final certificate = socket.peerCertificate;
              final now = DateTime.now();
              if (_closed ||
                  certificate == null ||
                  sha256.convert(certificate.der).toString() != _pin ||
                  now.isBefore(certificate.startValidity) ||
                  now.isAfter(certificate.endValidity)) {
                ImageExperimentRefusalObservation.report(
                  ImageExperimentFixtureStage.tlsPinValidity,
                  _observeRefusal,
                );
                socket.destroy();
                throw StateError('pin_refused');
              }
              try {
                configuration.credentials.requireActive();
              } catch (_) {
                ImageExperimentRefusalObservation.report(
                  ImageExperimentFixtureStage.requestPreconditions,
                  _observeRefusal,
                );
                socket.destroy();
                rethrow;
              }
              _sockets.add(socket);
              return socket;
            })
            .catchError((Object error) {
              ImageExperimentRefusalObservation.report(
                ImageExperimentFixtureStage.tlsHandshake,
                _observeRefusal,
              );
              _tasks.remove(task);
              throw StateError('pin_refused');
            });
        return ConnectionTask.fromSocket(socket, task.cancel);
      };
    } catch (_) {
      ImageExperimentRefusalObservation.report(stage, _observeRefusal);
      rethrow;
    }
  }
  final ImageExperimentRefusalObserver? _observeRefusal;
  final ImageExperimentConfiguration configuration;
  final ImageExperimentRecorder recorder;
  final String _pin;
  final Map<String, Object?> observation;
  final void Function()? _onClose;
  late final HttpClient _native;
  final _tasks = <ConnectionTask<SecureSocket>>{};
  final _sockets = <SecureSocket>{};
  final _timers = <Timer>{};
  bool _closed = false;
  void _validateUri(Uri uri) {
    final origin = configuration.brokerOrigin;
    final fixturePrefix =
        '/experiment/v1/batches/${configuration.batchId}/objects/';
    final fixture = uri.path.startsWith(fixturePrefix)
        ? uri.path.substring(fixturePrefix.length)
        : null;
    if (uri.scheme != 'https' ||
        uri.host != origin.host ||
        uri.port != origin.port ||
        uri.userInfo.isNotEmpty ||
        uri.hasQuery ||
        uri.hasFragment ||
        !(uri.path == '/experiment/v1/session/status' ||
            uri.path == '/experiment/v1/session/revoke' ||
            (fixture != null && configuration.cases.containsKey(fixture)))) {
      throw StateError('transport_refused');
    }
  }

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    ImageExperimentRefusalObservation.construct(
      ImageExperimentFixtureStage.requestPreconditions,
      () {
        _validateUri(request.url);
        if (_closed ||
            !(request.method == 'GET' &&
                    request.url.path != '/experiment/v1/session/revoke' ||
                request.method == 'POST' &&
                    request.url.path == '/experiment/v1/session/revoke') ||
            request.headers.isNotEmpty ||
            request.contentLength != null && request.contentLength != 0) {
          throw StateError('transport_refused');
        }
        configuration.credentials.requireActive();
      },
      observer: _observeRefusal,
    );
    late Timer timer;
    timer = Timer(const Duration(seconds: 10), close);
    _timers.add(timer);
    var stage = ImageExperimentFixtureStage.outgoingRequest;
    try {
      // openUrl awaits the pinned connectionFactory before returning this request.
      final outgoing = await _native
          .openUrl(request.method, request.url)
          .timeout(const Duration(seconds: 10));
      stage = ImageExperimentFixtureStage.requestPreconditions;
      if (_closed) throw StateError('session_inactive');
      outgoing.followRedirects = false;
      outgoing.headers.set(
        HttpHeaders.authorizationHeader,
        'Bearer ${configuration.credentials.requireActive()}',
      );
      outgoing.headers.set('X-Experiment-Session', configuration.sessionId);
      outgoing.headers.set(HttpHeaders.acceptEncodingHeader, 'identity');
      recorder.record({
        'event':
            request.url.path == '/experiment/v1/session/status' ||
                request.url.path == '/experiment/v1/session/revoke'
            ? 'status_request'
            : 'request',
        'requestCount': 1,
        'retryCount': 0,
        'generation': configuration.generation,
      });
      if (request.method == 'POST') {
        outgoing.headers.set(HttpHeaders.contentTypeHeader, 'application/json');
        outgoing.contentLength = 2;
        outgoing.write('{}');
      }
      stage = ImageExperimentFixtureStage.outgoingRequest;
      final response = await outgoing.close();
      stage = ImageExperimentFixtureStage.responseValidation;
      final isStatus =
          request.url.path == '/experiment/v1/session/status' ||
          request.url.path == '/experiment/v1/session/revoke';
      final fixture = isStatus
          ? null
          : configuration.cases[request.url.path.split('/').last]!;
      final maximum = isStatus ? 1024 : fixture!.byteSize;
      if (response.statusCode != 200 ||
          response.redirects.isNotEmpty ||
          response.headers.value(HttpHeaders.contentEncodingHeader) != null ||
          (fixture != null &&
              (response.contentLength != fixture.byteSize ||
                  response.headers.contentType?.mimeType != fixture.mime))) {
        close();
        throw StateError('transport_refused');
      }
      var bytes = 0;
      final stream = response.map((chunk) {
        try {
          configuration.credentials.requireActive();
          if (_closed) throw StateError('session_inactive');
          bytes += chunk.length;
          if (bytes > maximum) {
            close();
            throw StateError('transport_refused');
          }
          recorder.record({
            ...observation,
            'event': isStatus ? 'status_body' : 'body',
            'bodyBytes': chunk.length,
            'generation': configuration.generation,
          });
          return chunk;
        } catch (_) {
          ImageExperimentRefusalObservation.report(
            ImageExperimentFixtureStage.responseBody,
            _observeRefusal,
          );
          rethrow;
        }
      });
      return http.StreamedResponse(
        _finish(stream, timer),
        response.statusCode,
        contentLength: response.contentLength < 0
            ? null
            : response.contentLength,
        headers: {
          for (final name in [
            HttpHeaders.contentTypeHeader,
            HttpHeaders.cacheControlHeader,
            HttpHeaders.etagHeader,
          ])
            if (response.headers.value(name) != null)
              name: response.headers.value(name)!,
        },
        request: request,
      );
    } catch (_) {
      ImageExperimentRefusalObservation.report(stage, _observeRefusal);
      timer.cancel();
      _timers.remove(timer);
      close();
      throw StateError('transport_refused');
    }
  }

  Stream<List<int>> _finish(Stream<List<int>> source, Timer timer) async* {
    try {
      yield* source;
    } catch (_) {
      ImageExperimentRefusalObservation.report(
        ImageExperimentFixtureStage.responseBody,
        _observeRefusal,
      );
      rethrow;
    } finally {
      timer.cancel();
      _timers.remove(timer);
    }
  }

  @override
  void close() {
    if (_closed) return;
    _closed = true;
    for (final timer in _timers) {
      timer.cancel();
    }
    _timers.clear();
    for (final task in _tasks) {
      task.cancel();
    }
    _tasks.clear();
    for (final socket in _sockets) {
      socket.destroy();
    }
    _sockets.clear();
    _native.close(force: true);
    _onClose?.call();
  }
}
