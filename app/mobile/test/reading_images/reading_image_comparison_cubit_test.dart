import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/repository/reading_image_comparison_repository.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/load_reading_image_comparison.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/use_case/select_reading_image_rendition.dart';
import 'package:tellpal_mobile_poc/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart';
import 'comparison_test_support.dart';
import 'fixture.dart';

class PendingRepository implements ReadingImageComparisonRepository {
  int calls = 0;
  final first =
      Completer<Either<ReadingComparisonFailure, ReadingImageComparison>>();
  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation c,
  ) {
    calls++;
    return first.future;
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison value,
    ReadingComparisonCancellation c,
  ) async => Right(value);
  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonical,
    ReadingComparisonCancellation c,
  ) async => const Left(ReadingComparisonFailure('unavailable'));
}

void choose(ReadingImageComparisonCubit c, int page) => c.select(
  id: 71,
  entryIndex: page,
  canonicalSource: false,
  width: 100,
  height: 100,
  dpr: 1,
  fit: ReadingImageFit.contain,
);

class HoldingRepository extends PendingRepository {
  int leaseMillis = 1000;
  final bytesEntered = Completer<void>();
  final pendingBytes =
      Completer<Either<ReadingComparisonFailure, ReadingComparisonBytes>>();
  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation c,
  ) async {
    final f = Fixture();
    return Right(ReadingImageComparison(f.manifest, f.context, 1, leaseMillis));
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison value,
    ReadingComparisonCancellation c,
  ) async => const Left(ReadingComparisonFailure('withdrawn'));
  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonical,
    ReadingComparisonCancellation c,
  ) {
    if (!bytesEntered.isCompleted) {
      bytesEntered.complete();
    }
    return pendingBytes.future;
  }
}

class StatusHoldingRepository extends HoldingRepository {
  final statusEntered = Completer<void>();
  final pendingStatus =
      Completer<Either<ReadingComparisonFailure, ReadingImageComparison>>();
  ReadingComparisonCancellation? statusCancellation, bytesCancellation;
  ReadingImageComparison? statusAdmission;
  int statusCalls = 0, bytesCalls = 0, manifestCalls = 0;

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation c,
  ) {
    manifestCalls++;
    return super.manifest(id, c);
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> status(
    ReadingImageComparison value,
    ReadingComparisonCancellation c,
  ) {
    statusCalls++;
    statusCancellation = c;
    statusAdmission = value;
    if (!statusEntered.isCompleted) statusEntered.complete();
    return pendingStatus.future;
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonical,
    ReadingComparisonCancellation c,
  ) {
    bytesCalls++;
    bytesCancellation = c;
    return super.bytes(value, binding, rendition, canonical, c);
  }
}

/// Aynı metadata nesnesi her admission'da döner; istek yaşamı ayrıca korunmalıdır.
class AdmissionStatusRepository extends StatusHoldingRepository {
  AdmissionStatusRepository({int leaseMillis = 10000}) {
    final f = Fixture();
    sharedAdmission = ReadingImageComparison(
      f.manifest,
      f.context,
      1,
      leaseMillis,
    );
  }
  late final ReadingImageComparison sharedAdmission;
  final mediaWork =
      <Completer<Either<ReadingComparisonFailure, ReadingComparisonBytes>>>[];
  final mediaCancellations = <ReadingComparisonCancellation>[];

  @override
  Future<Either<ReadingComparisonFailure, ReadingImageComparison>> manifest(
    int id,
    ReadingComparisonCancellation c,
  ) async {
    manifestCalls++;
    return Right(sharedAdmission);
  }

  @override
  Future<Either<ReadingComparisonFailure, ReadingComparisonBytes>> bytes(
    ReadingImageComparison value,
    ReadingImageBinding binding,
    ReadingImageRendition rendition,
    bool canonical,
    ReadingComparisonCancellation c,
  ) {
    bytesCalls++;
    mediaCancellations.add(c);
    final work =
        Completer<Either<ReadingComparisonFailure, ReadingComparisonBytes>>();
    mediaWork.add(work);
    return work.future;
  }

  void releaseMedia(int index) => mediaWork[index].complete(
    const Left(ReadingComparisonFailure('cancelled')),
  );

  void finishStatus(String outcome) {
    switch (outcome) {
      case 'failure':
        pendingStatus.complete(
          const Left(ReadingComparisonFailure('withdrawn')),
        );
      case 'exception':
        pendingStatus.completeError(StateError('status_exception'));
      case 'live':
        pendingStatus.complete(
          Right(
            ReadingImageComparison(
              sharedAdmission.manifest,
              sharedAdmission.context,
              2,
              10000,
            ),
          ),
        );
      case 'invalid':
        pendingStatus.complete(
          Right(
            ReadingImageComparison(
              ReadingImageManifest(
                identity: sharedAdmission.manifest.identity,
                images: const [],
              ),
              sharedAdmission.context,
              1,
              10000,
            ),
          ),
        );
      case 'success':
        pendingStatus.complete(Right(sharedAdmission));
    }
  }
}

Future<void> settleStatus() => Future<void>.delayed(Duration.zero);

void main() {
  for (final selection in ['page', 'geometry', 'choice']) {
    for (final outcome in [
      'failure',
      'exception',
      'live',
      'invalid',
      'success',
    ]) {
      test('P4b3 aynı admission seçim=$selection status=$outcome', () async {
        final repo = AdmissionStatusRepository();
        final config = media(Uri.parse('http://127.0.0.1:1'));
        final clock = ManualComparisonClock();
        final gate = ReadingComparisonWorkGate();
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          gate,
          clock,
        );
        try {
          choose(cubit, 2);
          await settleStatus();
          clock.advance(1000);
          await repo.statusEntered.future;
          final epoch = cubit.state.epoch;
          switch (selection) {
            case 'page':
              choose(cubit, 3);
            case 'geometry':
              cubit.select(
                id: 71,
                entryIndex: 2,
                canonicalSource: false,
                width: 120,
                height: 80,
                dpr: 1.25,
                fit: ReadingImageFit.cover,
              );
            case 'choice':
              cubit.changeChoice(3, true);
              cubit.select(
                id: 71,
                entryIndex: 3,
                canonicalSource: true,
                width: 100,
                height: 100,
                dpr: 1,
                fit: ReadingImageFit.contain,
              );
          }
          expect(cubit.state.epoch, greaterThan(epoch));
          expect(repo.statusCancellation!.cancelled, isFalse);
          expect(repo.mediaCancellations.first.cancelled, isTrue);
          expect(repo.bytesCalls, 1);
          repo.releaseMedia(0);
          await settleStatus();
          expect(repo.bytesCalls, 2);
          final selected = cubit.state;
          final admission = cubit.admittedComparison;
          repo.finishStatus(outcome);
          await settleStatus();
          if (outcome == 'success') {
            expect(identical(cubit.state, selected), isTrue);
            expect(identical(cubit.admittedComparison, admission), isTrue);
            expect(repo.mediaCancellations.last.cancelled, isFalse);
          } else {
            expect(
              cubit.state.error,
              outcome == 'failure' || outcome == 'exception'
                  ? 'status_refused'
                  : 'live_changed',
            );
            expect(cubit.admittedComparison, isNull);
            expect(cubit.state.raster, isNull);
            expect(repo.mediaCancellations.last.cancelled, isTrue);
          }
          var gateReleased = false;
          final drained = gate.run(() async {
            gateReleased = true;
          });
          await settleStatus();
          expect(
            gateReleased,
            isFalse,
            reason: 'Eski medya bitmeden kapasite açılmaz',
          );
          expect(
            (repo.manifestCalls, repo.bytesCalls, repo.statusCalls),
            (1, 2, 1),
          );
          cubit.retire('test_cleanup');
          repo.releaseMedia(1);
          await drained;
        } finally {
          if (!repo.pendingStatus.isCompleted) repo.finishStatus('success');
          for (final work in repo.mediaWork) {
            if (!work.isCompleted) {
              work.complete(const Left(ReadingComparisonFailure('cancelled')));
            }
          }
          await cubit.close();
          await config.account!.credentials.close();
        }
      });
    }
  }

  for (final outcome in ['success', 'failure', 'exception', 'live']) {
    test(
      'P4b3 aynı nesneli readmission eski status=$outcome sonucunu reddeder',
      () async {
        final repo = AdmissionStatusRepository();
        final config = media(Uri.parse('http://127.0.0.1:1'));
        final clock = ManualComparisonClock();
        final cubit = ReadingImageComparisonCubit(
          LoadReadingImageComparison(repo),
          repo,
          config,
          ReadingComparisonWorkGate(),
          clock,
        );
        try {
          choose(cubit, 2);
          await settleStatus();
          clock.advance(1000);
          await repo.statusEntered.future;
          final oldAdmission = cubit.admittedComparison;
          cubit.retry();
          expect(repo.statusCancellation!.cancelled, isTrue);
          choose(cubit, 3);
          repo.releaseMedia(0);
          await settleStatus();
          expect(repo.manifestCalls, 2);
          expect(identical(cubit.admittedComparison, oldAdmission), isTrue);
          final selected = cubit.state;
          repo.finishStatus(outcome);
          await settleStatus();
          expect(identical(cubit.state, selected), isTrue);
          expect(identical(cubit.admittedComparison, oldAdmission), isTrue);
          expect(repo.mediaCancellations.last.cancelled, isFalse);
          expect((repo.bytesCalls, repo.statusCalls), (2, 1));
        } finally {
          for (final work in repo.mediaWork) {
            if (!work.isCompleted) {
              work.complete(const Left(ReadingComparisonFailure('cancelled')));
            }
          }
          if (!repo.pendingStatus.isCompleted) repo.finishStatus('success');
          await cubit.close();
          await config.account!.credentials.close();
        }
      },
    );
  }

  for (final lifecycle in ['account', 'background', 'lease']) {
    for (final outcome in ['success', 'failure', 'exception']) {
      test(
        'P4b3 $lifecycle sonrası eski status=$outcome state canlandırmaz',
        () async {
          final repo = AdmissionStatusRepository(leaseMillis: 2000);
          final config = media(Uri.parse('http://127.0.0.1:1'));
          final clock = ManualComparisonClock();
          final cubit = ReadingImageComparisonCubit(
            LoadReadingImageComparison(repo),
            repo,
            config,
            ReadingComparisonWorkGate(),
            clock,
          );
          try {
            choose(cubit, 2);
            await settleStatus();
            clock.advance(1000);
            await repo.statusEntered.future;
            switch (lifecycle) {
              case 'account':
                config.account!.credentials.revoke();
              case 'background':
                cubit.visibility(false);
              case 'lease':
                clock.advance(1000);
            }
            expect(
              cubit.state.error,
              {
                'account': 'account_changed',
                'background': 'background',
                'lease': 'lease_expired',
              }[lifecycle],
            );
            final retired = cubit.state;
            expect(repo.statusCancellation!.cancelled, isTrue);
            repo.finishStatus(outcome);
            await settleStatus();
            expect(identical(cubit.state, retired), isTrue);
            expect(cubit.admittedComparison, isNull);
            expect(cubit.state.raster, isNull);
            expect(
              (repo.manifestCalls, repo.bytesCalls, repo.statusCalls),
              (1, 1, 1),
            );
          } finally {
            if (!repo.pendingStatus.isCompleted) repo.finishStatus('success');
            repo.releaseMedia(0);
            await cubit.close();
            await config.account!.credentials.close();
          }
        },
      );
    }
  }

  for (final outcome in ['success', 'failure', 'exception']) {
    for (final statusFirst in [true, false]) {
      test(
        'ortak close status ve medyayı bekler: $outcome, statusFirst=$statusFirst',
        () async {
          final repo = StatusHoldingRepository()..leaseMillis = 10000;
          final config = media(Uri.parse('http://127.0.0.1:1'));
          final clock = ManualComparisonClock();
          final cubit = ReadingImageComparisonCubit(
            LoadReadingImageComparison(repo),
            repo,
            config,
            ReadingComparisonWorkGate(),
            clock,
          );
          final states = <String>[];
          final subscription = cubit.stream.listen((s) => states.add(s.status));
          choose(cubit, 2);
          await repo.bytesEntered.future;
          clock.advance(1000);
          await repo.statusEntered.future;
          expect(repo.statusCalls, 1);
          expect(repo.statusCancellation!.cancelled, isFalse);
          final closing = cubit.close();
          final secondClose = cubit.close();
          expect(identical(closing, secondClose), isTrue);
          expect(repo.statusCancellation!.cancelled, isTrue);
          expect(repo.bytesCancellation!.cancelled, isTrue);
          var closed = false;
          final observedClose = closing.then((_) => closed = true);
          final closedState = cubit.state;

          // İptal edilmiş zamanlayıcının sıraya alınmış callback'i de geç gelebilir.
          final callbacks = List<ManualComparisonTimer>.of(clock.timers);
          choose(cubit, 3);
          cubit.changeChoice(1, true);
          cubit.retry();
          cubit.visibility(false);
          cubit.visibility(true);
          config.account!.credentials.revoke();
          for (final timer in callbacks) {
            timer.callback(timer);
          }
          clock.advance(10000);
          await Future<void>.delayed(Duration.zero);
          expect(identical(cubit.state, closedState), isTrue);
          expect(closed, isFalse);
          expect(cubit.isClosed, isFalse);

          void finishStatus() {
            switch (outcome) {
              case 'success':
                repo.pendingStatus.complete(Right(repo.statusAdmission!));
              case 'failure':
                repo.pendingStatus.complete(
                  const Left(ReadingComparisonFailure('late_refusal')),
                );
              case 'exception':
                repo.pendingStatus.completeError(StateError('late_status'));
            }
          }

          void finishMedia() => repo.pendingBytes.complete(
            const Left(ReadingComparisonFailure('cancelled')),
          );
          if (statusFirst) {
            finishStatus();
          } else {
            finishMedia();
          }
          await Future<void>.delayed(Duration.zero);
          expect(closed, isFalse);
          expect(cubit.isClosed, isFalse);
          if (statusFirst) {
            finishMedia();
          } else {
            finishStatus();
          }
          await observedClose;
          await secondClose;
          expect(cubit.isClosed, isTrue);
          expect(cubit.state.error, 'closed');
          expect(cubit.admittedComparison, isNull);
          expect(cubit.state.raster, isNull);
          expect(states.where((s) => s == 'loaded'), isEmpty);
          expect(
            (repo.manifestCalls, repo.bytesCalls, repo.statusCalls),
            (1, 1, 1),
          );
          choose(cubit, 2);
          cubit.changeChoice(2, false);
          cubit.retry();
          cubit.visibility(true);
          expect(identical(cubit.state, closedState), isTrue);
          await subscription.cancel();
          await config.account!.credentials.close();
        },
      );
    }
  }

  test(
    'status başlamadan close mevcut medya işinin sonlanmasını bekler',
    () async {
      final repo = StatusHoldingRepository()..leaseMillis = 10000;
      final config = media(Uri.parse('http://127.0.0.1:1'));
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        ReadingComparisonWorkGate(),
        ManualComparisonClock(),
      );
      choose(cubit, 2);
      await repo.bytesEntered.future;
      var closed = false;
      final closing = cubit.close().then((_) => closed = true);
      await Future<void>.delayed(Duration.zero);
      expect(repo.statusCalls, 0);
      expect(closed, isFalse);
      repo.pendingBytes.complete(
        const Left(ReadingComparisonFailure('cancelled')),
      );
      await closing;
      expect(cubit.isClosed, isTrue);
      await config.account!.credentials.close();
    },
  );

  test(
    'retire ile iptal edilmiş status da close drain kapsamındadır',
    () async {
      final repo = StatusHoldingRepository()..leaseMillis = 10000;
      final config = media(Uri.parse('http://127.0.0.1:1'));
      final clock = ManualComparisonClock();
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        ReadingComparisonWorkGate(),
        clock,
      );
      choose(cubit, 2);
      await repo.bytesEntered.future;
      clock.advance(1000);
      await repo.statusEntered.future;
      cubit.visibility(false);
      expect(repo.statusCancellation!.cancelled, isTrue);
      repo.pendingBytes.complete(
        const Left(ReadingComparisonFailure('cancelled')),
      );
      var closed = false;
      final closing = cubit.close().then((_) => closed = true);
      await Future<void>.delayed(Duration.zero);
      expect(closed, isFalse);
      repo.pendingStatus.complete(Right(repo.statusAdmission!));
      await closing;
      expect(cubit.state.error, 'closed');
      await config.account!.credentials.close();
    },
  );

  test(
    'lease süresi pending decode/transfer neslini kapatır; yeni LIVE otomatik alınmaz',
    () async {
      final repo = HoldingRepository()..leaseMillis = 10;
      final config = media(Uri.parse('http://127.0.0.1:1'));
      final clock = ManualComparisonClock();
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        ReadingComparisonWorkGate(),
        clock,
      );
      choose(cubit, 2);
      await Future<void>.delayed(Duration.zero);
      expect(repo.bytesEntered.isCompleted, isTrue);
      clock.advance(9);
      expect(cubit.state.error, isNull);
      clock.advance(1);
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state.error, 'lease_expired');
      repo.pendingBytes.complete(
        const Left(ReadingComparisonFailure('cancelled')),
      );
      await Future<void>.delayed(Duration.zero);
      await cubit.close();
      await config.account!.credentials.close();
    },
  );
  test(
    'görünür status hatası pending byte işini eski nesil yapar ve tekrar denemez',
    () async {
      final repo = HoldingRepository();
      final profile = ReadingComparisonLimits(
        runId: 'synthetic',
        maxOwnedImages: 1,
        maxEncodedBytes: 2000000,
        maxBufferBytes: 6000000,
        maxDecodePixels: 10000000,
        maxDecodedBytes: 40000000,
        maxPeakBytes: 206000000,
        transferMillis: 1000,
        decodeMillis: 1000,
        statusEveryMillis: 10,
        maxStatusChecks: 2,
        maxControlMillis: 1000,
      );
      final config = media(Uri.parse('http://127.0.0.1:1'), profile: profile);
      final clock = ManualComparisonClock();
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        ReadingComparisonWorkGate(),
        clock,
      );
      choose(cubit, 2);
      await Future<void>.delayed(Duration.zero);
      expect(repo.bytesEntered.isCompleted, isTrue);
      clock.advance(9);
      expect(cubit.state.error, isNull);
      clock.advance(1);
      await Future<void>.delayed(Duration.zero);
      expect(cubit.state.error, 'status_refused');
      repo.pendingBytes.complete(
        const Left(ReadingComparisonFailure('cancelled')),
      );
      await Future<void>.delayed(Duration.zero);
      await cubit.close();
      await config.account!.credentials.close();
    },
  );
  test('çalışma ayarları yokken ağ çağrısı yapılmaz', () async {
    final repo = PendingRepository();
    final cubit = ReadingImageComparisonCubit(
      LoadReadingImageComparison(repo),
      repo,
      const ReadingImageComparisonConfiguration.geometry(),
      ReadingComparisonWorkGate(),
      const ReadingComparisonClock(),
    );
    choose(cubit, 2);
    expect(repo.calls, 0);
    expect(cubit.state.status, 'empty');
    await cubit.close();
  });
  test(
    'eski Future bitmeden yeni iş kapasite alamaz ve geç admission atılır',
    () async {
      final repo = PendingRepository(),
          config = media(Uri.parse('http://127.0.0.1:1'));
      final gate = ReadingComparisonWorkGate();
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        gate,
        const ReadingComparisonClock(),
      );
      choose(cubit, 2);
      await Future<void>.delayed(Duration.zero);
      expect(repo.calls, 1);
      choose(cubit, 3);
      await Future<void>.delayed(Duration.zero);
      expect(repo.calls, 1);
      cubit.visibility(false);
      final fixture = Fixture();
      repo.first.complete(
        Right(
          ReadingImageComparison(fixture.manifest, fixture.context, 1, 1000),
        ),
      );
      await cubit.close();
      expect(repo.calls, 1);
      expect(cubit.state.raster, isNull);
      await config.account!.credentials.close();
    },
  );
  test(
    'hesap revoke anında görsel sahipliğini ve pending admission neslini kapatır',
    () async {
      final repo = PendingRepository(),
          config = media(Uri.parse('http://127.0.0.1:1'));
      final cubit = ReadingImageComparisonCubit(
        LoadReadingImageComparison(repo),
        repo,
        config,
        ReadingComparisonWorkGate(),
        const ReadingComparisonClock(),
      );
      choose(cubit, 2);
      await Future<void>.delayed(Duration.zero);
      config.account!.credentials.revoke();
      expect(cubit.state.error, 'account_changed');
      repo.first.complete(const Left(ReadingComparisonFailure('cancelled')));
      await cubit.close();
      await config.account!.credentials.close();
    },
  );
}

class ManualComparisonClock extends ReadingComparisonClock {
  int elapsed = 0;
  final timers = <ManualComparisonTimer>[];
  @override
  Timer once(Duration duration, void Function() callback) {
    final timer = ManualComparisonTimer(
      elapsed + duration.inMilliseconds,
      0,
      (_) => callback(),
    );
    timers.add(timer);
    return timer;
  }

  @override
  Timer periodic(Duration duration, void Function(Timer) callback) {
    final timer = ManualComparisonTimer(
      elapsed + duration.inMilliseconds,
      duration.inMilliseconds,
      callback,
    );
    timers.add(timer);
    return timer;
  }

  void advance(int millis) {
    final target = elapsed + millis;
    while (true) {
      final due =
          timers.where((t) => t.isActive && t.deadline <= target).toList()
            ..sort((a, b) => a.deadline.compareTo(b.deadline));
      if (due.isEmpty) break;
      final timer = due.first;
      elapsed = timer.deadline;
      timer.tick++;
      if (timer.period == 0) {
        timer.cancel();
      } else {
        timer.deadline += timer.period;
      }
      timer.callback(timer);
    }
    elapsed = target;
  }
}

class ManualComparisonTimer implements Timer {
  ManualComparisonTimer(this.deadline, this.period, this.callback);
  int deadline;
  final int period;
  final void Function(Timer) callback;
  @override
  bool isActive = true;
  @override
  int tick = 0;
  @override
  void cancel() {
    isActive = false;
  }
}
