import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:file/file.dart' as files;
import 'package:file/local.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/io_client.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';

class _RealHttp extends HttpOverrides {}

class _TrackedClient extends IOClient {
  _TrackedClient(super.client);
  bool closed = false;
  @override
  void close() {
    closed = true;
    super.close();
  }
}

class _Files implements FileSystem {
  _Files(this.directory);
  final Directory directory;
  final files.FileSystem system = const LocalFileSystem();
  Completer<void>? entered;
  Completer<void>? release;
  int calls = 0;
  @override
  Future<files.File> createFile(String name) async {
    calls++;
    if (release != null) {
      if (!entered!.isCompleted) entered!.complete();
      await release!.future;
    }
    return system.file('${directory.path}/$name');
  }
}

class _Repository extends NonStoringObjectProvider {
  final objects = <String, CacheObject>{};
  bool closed = false;
  int writes = 0;
  Completer<void>? entered;
  Completer<void>? release;
  @override
  Future<CacheObject?> get(String key) async {
    if (closed) throw StateError('Closed repository');
    return objects[key];
  }

  @override
  Future<dynamic> updateOrInsert(CacheObject object) async {
    if (closed) throw StateError('Closed repository');
    writes++;
    if (release != null) {
      if (!entered!.isCompleted) entered!.complete();
      await release!.future;
    }
    return objects[object.key] = object.copyWith(
      id: object.id ?? objects.length + 1,
    );
  }

  @override
  Future<List<CacheObject>> getAllObjects() async {
    if (closed) throw StateError('Closed repository');
    return objects.values.toList();
  }

  @override
  Future<int> deleteAll(Iterable<int> ids) async {
    objects.removeWhere((_, object) => ids.contains(object.id));
    return ids.length;
  }

  @override
  Future<bool> close() async {
    closed = true;
    return true;
  }
}

class _Harness {
  _Harness(
    this.server,
    this.directory,
    this.repository,
    this.files,
    this.cache,
  );
  final HttpServer server;
  final Directory directory;
  final _Repository repository;
  final _Files files;
  final LibraryImageCache cache;
  final requests = <String>[];
  final clients = <_TrackedClient>[];
  bool recoverHeaders = false;
  final progress = <String, int>{};
  final arrived = StreamController<String>.broadcast();
  final timers = <Timer>[];
  Uri url(String path) => Uri.parse('http://127.0.0.1:${server.port}/$path');
  static Future<_Harness> create({
    Duration wait = const Duration(milliseconds: 600),
  }) async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final directory = await Directory.systemTemp.createTemp(
      'library-transport-',
    );
    final repository = _Repository();
    final files = _Files(directory);
    late _Harness harness;
    final cache = LibraryImageCache.forTesting(
      Config(
        'real-transfer',
        repo: repository,
        fileSystem: files,
        stalePeriod: const Duration(minutes: 10),
        maxNrOfCacheObjects: 80,
      ),
      artworkWait: wait,
      clientFactory: () {
        final client = _TrackedClient(
          HttpOverrides.runWithHttpOverrides(() => HttpClient(), _RealHttp()),
        );
        harness.clients.add(client);
        return client;
      },
    );
    harness = _Harness(server, directory, repository, files, cache);
    server.listen((request) async {
      final path = request.uri.path;
      harness.requests.add(path);
      harness.arrived.add(path);
      expect(request.headers.value('x-library-local-transfer'), isNull);
      try {
        if (path.startsWith('/headers') && !harness.recoverHeaders) return;
        request.response.bufferOutput = false;
        request.response.headers.contentType = ContentType('image', 'jpeg');
        request.response.headers.set('cache-control', 'max-age=600');
        if (path.startsWith('/body')) {
          request.response.add([1]);
          await request.response.flush();
          return;
        }
        if (path.startsWith('/trickle')) {
          request.response.add([1]);
          await request.response.flush();
          harness.timers.add(
            Timer.periodic(const Duration(milliseconds: 30), (timer) async {
              try {
                request.response.add([2]);
                await request.response.flush();
              } catch (_) {
                timer.cancel();
              }
            }),
          );
          return;
        }
        if (path.startsWith('/delayed')) {
          await Future<void>.delayed(const Duration(milliseconds: 900));
        }
        request.response.add([1, 2, 3, 4]);
        await request.response.close();
      } catch (_) {
        /* The bounded client deliberately aborts the old socket. */
      }
    });
    return harness;
  }

  Future<List<FileResponse>> load(String path, String attempt) => cache
      .getFileStream(
        url(path).toString(),
        key: LibraryImageCache.transferKey('account:en:content:asset', attempt),
        withProgress: true,
      )
      .map((event) {
        if (event is DownloadProgress) {
          progress[attempt] = (progress[attempt] ?? 0) + 1;
        }
        return event;
      })
      .toList();
  Future<void> dispose() async {
    await cache.close();
    for (final timer in timers) {
      timer.cancel();
    }
    await server.close(force: true);
    await arrived.close();
    await directory.delete(recursive: true);
  }
}

void main() {
  test(
    'shared actual cold header failure reaches both current consumers and explicit same-key retry owns one fresh source',
    () async {
      final harness = await _Harness.create();
      final observations = <Map<String, Object?>>[];
      harness.cache.failureObserver = observations.add;
      try {
        final first = harness.load('headers-shared', 'first');
        final second = harness.load('headers-shared', 'second');
        await Future.wait([
          expectLater(first, throwsA(isA<TimeoutException>())),
          expectLater(second, throwsA(isA<TimeoutException>())),
        ]);
        expect(harness.requests, ['/headers-shared']);
        expect(harness.clients, hasLength(1));
        expect(harness.clients.single.closed, true);
        await harness.cache.discardFailedTransfer(
          LibraryImageCache.transferKey('account:en:content:asset', 'first'),
        );
        await harness.cache.discardFailedTransfer(
          LibraryImageCache.transferKey('account:en:content:asset', 'second'),
        );
        expect(harness.cache.ownedAttemptCount, 0);
        expect(observations, hasLength(2));
        expect(
          observations.map((e) => e['transferSha256']).toSet(),
          hasLength(2),
        );
        expect(
          observations.map((e) => e['producerSha256']).toSet(),
          hasLength(1),
        );
        expect(
          observations.every((e) => e['transportPhase'] == 'response-headers'),
          true,
        );
        expect(harness.directory.listSync(recursive: true), isEmpty);
        await harness.cache.discardFailedTransfer(
          LibraryImageCache.transferKey('account:en:content:asset', 'first'),
        );
        harness.recoverHeaders = true;
        final recovered = await harness.load('headers-shared', 'first');
        final file = recovered.whereType<FileInfo>().single;
        expect(await file.file.readAsBytes(), [1, 2, 3, 4]);
        expect(harness.requests, ['/headers-shared', '/headers-shared']);
        expect(harness.clients, hasLength(2));
        expect(harness.clients.every((c) => c.closed), true);
        expect(harness.cache.ownedAttemptCount, 0);
      } finally {
        await harness.dispose();
      }
    },
  );

  test(
    'last shared consumer cancellation closes one actual silent socket and settles both owned slots',
    () async {
      final harness = await _Harness.create(wait: const Duration(seconds: 2));
      try {
        final key1 = LibraryImageCache.transferKey(
          'account:en:content:asset',
          'first',
        );
        final key2 = LibraryImageCache.transferKey(
          'account:en:content:asset',
          'second',
        );
        final first = harness.cache
            .getFileStream(harness.url('headers-shared').toString(), key: key1)
            .listen((_) {}, onError: (Object _) {});
        final second = harness.cache
            .getFileStream(harness.url('headers-shared').toString(), key: key2)
            .listen((_) {}, onError: (Object _) {});
        await harness.arrived.stream.first;
        expect(harness.requests, ['/headers-shared']);
        expect(harness.clients, hasLength(1));
        await first.cancel();
        expect(harness.clients.single.closed, false);
        expect(harness.cache.ownedAttemptCount, 2);
        await second.cancel();
        await harness.cache.discardFailedTransfer(key1, failed: false);
        await harness.cache.discardFailedTransfer(key2, failed: false);
        expect(harness.clients.single.closed, true);
        expect(harness.cache.ownedAttemptCount, 0);
        expect(harness.repository.objects, isEmpty);
        expect(harness.directory.listSync(recursive: true), isEmpty);
      } finally {
        await harness.dispose();
      }
    },
  );

  test(
    'same actual URL never shares across account, asset, headers or progress contracts',
    () async {
      final harness = await _Harness.create(wait: const Duration(seconds: 2));
      try {
        final uri = harness.url('ok').toString();
        final responses = await Future.wait([
          harness.cache
              .getFileStream(
                uri,
                key: LibraryImageCache.transferKey(
                  'account:en:content:asset',
                  'one',
                ),
              )
              .toList(),
          harness.cache
              .getFileStream(
                uri,
                key: LibraryImageCache.transferKey(
                  'another-account:en:content:asset',
                  'two',
                ),
              )
              .toList(),
          harness.cache
              .getFileStream(
                uri,
                key: LibraryImageCache.transferKey(
                  'account:en:content:another-asset',
                  'three',
                ),
              )
              .toList(),
          harness.cache
              .getFileStream(
                uri,
                key: LibraryImageCache.transferKey(
                  'account:en:content:asset',
                  'four',
                ),
                headers: {'x-test-source': 'different'},
              )
              .toList(),
          harness.cache
              .getFileStream(
                uri,
                key: LibraryImageCache.transferKey(
                  'account:en:content:asset',
                  'five',
                ),
                withProgress: true,
              )
              .toList(),
        ]);
        expect(harness.requests, hasLength(5));
        expect(harness.clients, hasLength(5));
        for (final response in responses) {
          expect(
            await response.whereType<FileInfo>().single.file.readAsBytes(),
            [1, 2, 3, 4],
          );
        }
        expect(harness.clients.every((c) => c.closed), true);
        expect(harness.cache.ownedAttemptCount, 0);
      } finally {
        await harness.dispose();
      }
    },
  );

  for (final path in ['headers', 'body', 'trickle']) {
    test(
      'real CacheManager bounds $path and removes partial files before close',
      () async {
        final harness = await _Harness.create();
        final observations = <Map<String, Object?>>[];
        harness.cache.failureObserver = (event) {
          observations.add(event);
          if (path == 'headers') throw StateError('Test observer failure');
        };
        final stopwatch = Stopwatch()..start();
        await expectLater(
          harness.load(path, path),
          throwsA(isA<TimeoutException>()),
        );
        expect(stopwatch.elapsed, lessThan(const Duration(seconds: 2)));
        expect(observations, hasLength(1));
        expect(observations.single['phase'], 'package-source');
        expect(observations.single['transportPhase'], switch (path) {
          'headers' => 'response-headers',
          'body' => 'response-body-idle',
          _ => 'total',
        });
        expect(
          observations.single['transportLimitMilliseconds'],
          path == 'trickle' ? 480 : 240,
        );
        expect(observations.single['transportMilliseconds'], isA<int>());
        expect(observations.single['failureClass'], 'TimeoutException');
        expect(
          observations.single['identitySha256'],
          LibraryImageCache.failureIdentitySha256('account:en:content:asset'),
        );
        expect(jsonEncode(observations), isNot(contains('account:en')));
        expect(jsonEncode(observations), isNot(contains('http://')));
        if (path == 'body') {
          expect(harness.progress[path], greaterThanOrEqualTo(1));
        }
        if (path == 'trickle') {
          expect(harness.progress[path], greaterThanOrEqualTo(3));
          expect(
            stopwatch.elapsed,
            greaterThanOrEqualTo(const Duration(milliseconds: 400)),
          );
        }
        await harness.cache.close();
        expect(harness.repository.objects, isEmpty);
        expect(harness.directory.listSync(recursive: true), isEmpty);
        await harness.dispose();
      },
    );
  }
  test(
    'all ten occupied package slots release; current retry fetches independently',
    () async {
      final harness = await _Harness.create(wait: const Duration(seconds: 2));
      final observations = <Map<String, Object?>>[];
      harness.cache.failureObserver = observations.add;
      final errors = <Future<void>>[];
      for (var i = 0; i < 10; i++) {
        errors.add(
          expectLater(
            harness.load('headers/$i', 'old$i'),
            throwsA(isA<TimeoutException>()),
          ),
        );
      }
      while (harness.requests.length < 10) {
        await harness.arrived.stream.first;
      }
      final queued = harness.load('ok', 'queued');
      final queuedError = expectLater(queued, throwsA(isA<TimeoutException>()));
      await queuedError;
      final queuedObservation = observations.singleWhere(
        (event) =>
            event['transferSha256'] ==
            LibraryImageCache.failureIdentitySha256(
              LibraryImageCache.transferKey(
                'account:en:content:asset',
                'queued',
              ),
            ),
      );
      expect(queuedObservation['phase'], 'admission');
      expect(queuedObservation['failureClass'], 'TimeoutException');
      expect(queuedObservation['timeoutMilliseconds'], 400);
      await Future.wait(errors);
      final result = await harness.load('ok', 'retry');
      expect(result.whereType<FileInfo>(), hasLength(1));
      expect(harness.requests.where((path) => path == '/ok'), hasLength(1));
      await harness.dispose();
    },
  );
  test(
    'explicit retry sends request before the old delayed headers and rejects its success',
    () async {
      final harness = await _Harness.create(wait: const Duration(seconds: 3));
      final old = harness.load('delayed', 'old');
      await harness.arrived.stream.first;
      await harness.cache.discardFailedTransfer(
        LibraryImageCache.transferKey('account:en:content:asset', 'old'),
      );
      final watch = Stopwatch()..start();
      final current = await harness.load('ok', 'retry');
      expect(watch.elapsed, lessThan(const Duration(milliseconds: 800)));
      expect(current.whereType<FileInfo>(), hasLength(1));
      expect(await old, isEmpty);
      await Future<void>.delayed(const Duration(milliseconds: 1000));
      expect(harness.repository.objects.keys, everyElement(endsWith('retry')));
      await harness.dispose();
    },
  );
  for (final path in ['headers', 'body', 'trickle']) {
    test(
      'close cancels $path plus queued work; no late file or metadata publication',
      () async {
        final harness = await _Harness.create(wait: const Duration(seconds: 5));
        final ongoing = List.generate(
          12,
          (i) => harness.load('$path/$i', 'old$i'),
        );
        while (harness.requests.length < 10) {
          await harness.arrived.stream.first;
        }
        final watch = Stopwatch()..start();
        await harness.cache.close();
        expect(watch.elapsed, lessThan(const Duration(seconds: 1)));
        await Future.wait(ongoing);
        final calls = harness.files.calls;
        final writes = harness.repository.writes;
        final requests = harness.requests.length;
        expect(harness.repository.objects, isEmpty);
        expect(harness.directory.listSync(recursive: true), isEmpty);
        await expectLater(harness.load('ok', 'after-close'), throwsStateError);
        await Future<void>.delayed(const Duration(milliseconds: 150));
        expect(harness.files.calls, calls);
        expect(harness.repository.writes, writes);
        expect(harness.requests.length, requests);
        await harness.dispose();
      },
    );
  }
  test(
    'close drains admitted package metadata and delayed file creation before cleanup',
    () async {
      final harness = await _Harness.create();
      harness.files.entered = Completer<void>();
      harness.files.release = Completer<void>();
      final loading = harness.load('ok', 'pending');
      await harness.files.entered!.future;
      var done = false;
      final closing = harness.cache.close().then((_) => done = true);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(done, isFalse);
      harness.files.release!.complete();
      await loading;
      await closing;
      expect(harness.repository.closed, isTrue);
      expect(harness.directory.listSync(recursive: true), isEmpty);
      await harness.dispose();
    },
  );
  test(
    'close drains actual package metadata publication before removing files and closing the repository',
    () async {
      final harness = await _Harness.create();
      harness.repository.entered = Completer<void>();
      harness.repository.release = Completer<void>();
      final loading = harness.load('ok', 'publishing');
      await harness.repository.entered!.future;
      var done = false;
      final closing = harness.cache.close().then((_) => done = true);
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(done, isFalse);
      expect(harness.repository.closed, isFalse);
      harness.repository.release!.complete();
      await loading;
      await closing;
      expect(harness.repository.closed, isTrue);
      expect(harness.repository.objects, isEmpty);
      expect(harness.directory.listSync(recursive: true), isEmpty);
      await harness.dispose();
    },
  );
}
