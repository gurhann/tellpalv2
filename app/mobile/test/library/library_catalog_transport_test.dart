import 'dart:async';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';

class _Loopback {
  late ServerSocket server;
  final sockets = <Socket>{};
  final timers = <Timer>[];
  int requested = 0;
  int released = 0;
  int connections = 0;
  String mode = 'headers';
  String payload = '';
  int statusCode = 200;
  Future<void> start() async {
    server = await ServerSocket.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((socket) {
      connections++;
      sockets.add(socket);
      unawaited(socket.done.catchError((_) {}));
      var received = false;
      socket.listen(
        (bytes) {
          if (received) return;
          received = true;
          requested++;
          if (mode == 'boundary') {
            socket.write(
              'HTTP/1.1 $statusCode Boundary\r\nContent-Length: ${payload.length}\r\n\r\n$payload',
            );
            return;
          }
          if (mode == 'headers') return;
          if (mode == 'success') {
            socket.write('HTTP/1.1 200 OK\r\nContent-Length: 7\r\n\r\n{"x":1}');
            return;
          }
          final status = mode == 'error'
              ? '500 Internal Server Error'
              : '200 OK';
          socket.write('HTTP/1.1 $status\r\nContent-Length: 999999\r\n\r\n ');
          if (mode == 'trickle') {
            timers.add(
              Timer.periodic(const Duration(milliseconds: 15), (_) {
                if (sockets.contains(socket)) socket.write(' ');
              }),
            );
          }
        },
        onError: (_) {},
        onDone: () {
          released++;
          sockets.remove(socket);
          socket.destroy();
        },
      );
    });
  }

  AuthenticatedTransport transport({
    Duration wait = const Duration(milliseconds: 250),
  }) => AuthenticatedTransport(
    LibraryConfiguration(
      baseUrl: Uri.parse('http://127.0.0.1:${server.port}'),
      idToken: 'local-test-token',
      context: const LibraryContext('tr', 'transport-test'),
      catalogWait: wait,
    ),
  );
  Future<void> waitFor(bool Function() condition) async {
    final deadline = DateTime.now().add(const Duration(seconds: 2));
    while (!condition()) {
      if (DateTime.now().isAfter(deadline)) fail('Loopback condition expired');
      await Future<void>.delayed(const Duration(milliseconds: 5));
    }
  }

  Future<void> close() async {
    for (final timer in timers) {
      timer.cancel();
    }
    for (final socket in sockets) {
      socket.destroy();
    }
    await server.close();
  }
}

void main() {
  for (final status in [200, 500]) {
    for (final length in [1048575, 1048576, 1048577]) {
      test(
        'real catalog $status body $length-byte boundary releases its socket',
        () async {
          final peer = _Loopback()
            ..mode = 'boundary'
            ..statusCode = status
            ..payload = '${' ' * (length - 7)}{"x":1}';
          await peer.start();
          final transport = peer.transport(wait: const Duration(seconds: 3));
          if (length > 1048576) {
            await expectLater(transport.library('tr'), throwsFormatException);
          } else if (status != 200) {
            await expectLater(
              transport.library('tr'),
              throwsA(
                isA<CatalogHttpFailure>().having(
                  (error) => error.status,
                  'status',
                  status,
                ),
              ),
            );
          } else {
            expect(await transport.library('tr'), {'x': 1});
          }
          await peer.waitFor(() => peer.released == 1);
          expect(peer.sockets, isEmpty);
          peer.mode = 'success';
          expect(await transport.library('tr'), {'x': 1});
          await peer.waitFor(() => peer.released == 2);
          transport.close();
          await peer.close();
        },
      );
    }
  }
  test(
    'eight native requests occupy admission; ninth opens no socket and released slot admits retry',
    () async {
      final peer = _Loopback();
      await peer.start();
      final transport = peer.transport(wait: const Duration(seconds: 3));
      int? releasedIndex;
      final active = [
        for (var index = 0; index < 8; index++)
          transport.library('tr').then((response) {
            releasedIndex = index;
            return response;
          }),
      ];
      await peer.waitFor(() => peer.requested == 8);
      expect(peer.connections, 8);
      await expectLater(transport.library('tr'), throwsStateError);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(peer.connections, 8);
      peer.sockets.first.write(
        'HTTP/1.1 200 OK\r\nContent-Length: 7\r\n\r\n{"x":1}',
      );
      expect(await Future.any(active), {'x': 1});
      await peer.waitFor(() => peer.released == 1);
      peer.mode = 'success';
      expect(await transport.library('en'), {'x': 1});
      await peer.waitFor(() => peer.released == 2);
      expect(peer.connections, 9);
      final cancelled = active.indexed
          .where((entry) => entry.$1 != releasedIndex)
          .map(
            (entry) => expectLater(entry.$2, throwsA(isA<TimeoutException>())),
          )
          .toList();
      transport.close();
      await Future.wait(cancelled);
      await peer.waitFor(() => peer.released == 9);
      expect(peer.sockets, isEmpty);
      await peer.close();
    },
  );
  for (final mode in ['headers', 'body', 'error', 'trickle']) {
    test('real catalog $mode deadline closes the native socket', () async {
      final peer = _Loopback()..mode = mode;
      await peer.start();
      final transport = peer.transport();
      final clock = Stopwatch()..start();
      await expectLater(
        transport.library('tr'),
        throwsA(isA<TimeoutException>()),
      );
      expect(clock.elapsedMilliseconds, lessThan(350));
      await peer.waitFor(() => peer.released == 1);
      expect(peer.sockets, isEmpty);
      transport.close();
      await peer.close();
    });
  }
  test(
    'repeated expired catalog attempts release resources and current retry succeeds',
    () async {
      final peer = _Loopback();
      await peer.start();
      final transport = peer.transport();
      for (var index = 0; index < 12; index++) {
        await expectLater(
          transport.library('tr'),
          throwsA(isA<TimeoutException>()),
        );
        await peer.waitFor(() => peer.released == index + 1);
      }
      peer.mode = 'success';
      expect(await transport.library('en'), {'x': 1});
      await peer.waitFor(() => peer.released == 13);
      expect(peer.requested, 13);
      expect(peer.sockets, isEmpty);
      transport.close();
      await peer.close();
    },
  );
  test(
    'app disposal cancels simultaneous native requests and rejects new admission',
    () async {
      final peer = _Loopback();
      await peer.start();
      final transport = peer.transport();
      final first = expectLater(
        transport.library('tr'),
        throwsA(isA<TimeoutException>()),
      );
      final second = expectLater(
        transport.library('en'),
        throwsA(isA<TimeoutException>()),
      );
      await peer.waitFor(() => peer.requested == 2);
      transport.close();
      await Future.wait([first, second]);
      await peer.waitFor(() => peer.released == 2);
      await expectLater(transport.library('pt'), throwsStateError);
      expect(peer.sockets, isEmpty);
      await peer.close();
    },
  );
}
