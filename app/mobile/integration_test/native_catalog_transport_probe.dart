import 'dart:async';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/network/authenticated_transport.dart';

/// Runs actual native sockets independently of the real authenticated fixture.
Future<Map<String, dynamic>> verifyNativeCatalogTransport(
  LibraryConfiguration real,
) async {
  final evidence = <String, dynamic>{};
  for (final mode in [
    'headers',
    'body',
    'error',
    'trickle',
    'retry',
    'dispose',
  ]) {
    final peer = await ServerSocket.bind(InternetAddress.loopbackIPv4, 0);
    final sockets = <Socket>{};
    final timers = <Timer>[];
    var requests = 0;
    var released = 0;
    var success = false;
    peer.listen((socket) {
      sockets.add(socket);
      unawaited(socket.done.catchError((_) {}));
      var read = false;
      socket.listen(
        (_) {
          if (read) return;
          read = true;
          requests++;
          if (success) {
            socket.write('HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\n{}');
            return;
          }
          if (mode == 'headers' || mode == 'retry' || mode == 'dispose') return;
          socket.write(
            'HTTP/1.1 ${mode == 'error' ? '503 Service Unavailable' : '200 OK'}\r\nContent-Length: 999999\r\n\r\n ',
          );
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
    final transport = AuthenticatedTransport(
      LibraryConfiguration(
        baseUrl: Uri.parse('http://127.0.0.1:${peer.port}'),
        idToken: real.idToken,
        context: real.context,
        catalogWait: const Duration(milliseconds: 300),
      ),
    );
    Future<void> until(bool Function() done) async {
      final deadline = DateTime.now().add(const Duration(seconds: 3));
      while (!done()) {
        if (DateTime.now().isAfter(deadline)) {
          fail('Native catalog socket release expired');
        }
        await Future<void>.delayed(const Duration(milliseconds: 5));
      }
    }

    try {
      if (mode == 'dispose') {
        final pending = expectLater(
          transport.library('tr'),
          throwsA(isA<TimeoutException>()),
        );
        await until(() => requests == 1);
        transport.close();
        await pending;
        await until(() => released == 1);
        await expectLater(transport.library('tr'), throwsStateError);
      } else {
        final repeats = mode == 'retry' ? 12 : 1;
        for (var index = 0; index < repeats; index++) {
          await expectLater(
            transport.library('tr'),
            throwsA(isA<TimeoutException>()),
          );
          await until(() => released == index + 1);
        }
        if (mode == 'retry') {
          success = true;
          expect(await transport.library('tr'), isEmpty);
          await until(() => released == 13);
        }
      }
      expect(sockets, isEmpty);
      evidence[mode] = {
        'requests': requests,
        'releasedSockets': released,
        'passed': true,
      };
    } finally {
      transport.close();
      for (final timer in timers) {
        timer.cancel();
      }
      for (final socket in sockets) {
        socket.destroy();
      }
      await peer.close();
    }
  }
  return evidence;
}
