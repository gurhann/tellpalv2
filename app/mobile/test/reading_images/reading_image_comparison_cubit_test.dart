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

void main() {
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
