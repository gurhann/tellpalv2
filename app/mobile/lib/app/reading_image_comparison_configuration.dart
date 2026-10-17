import 'dart:async';
import 'image_experiment_configuration.dart';

/// Profil seçimi ayrı çalışma kararıdır; sayısal varsayılan içermez.
class ReadingComparisonLimits {
  static const maxSupportedMillis = 0x7fffffff;
  ReadingComparisonLimits({
    required this.runId,
    required this.maxEncodedBytes,
    required this.maxBufferBytes,
    required this.maxDecodePixels,
    required this.maxDecodedBytes,
    required this.maxPeakBytes,
    required this.maxOwnedImages,
    required this.transferMillis,
    required this.decodeMillis,
    required this.statusEveryMillis,
    required this.maxStatusChecks,
    required this.maxControlMillis,
  }) {
    if (maxOwnedImages != 1 ||
        runId.isEmpty ||
        [
          maxEncodedBytes,
          maxBufferBytes,
          maxDecodePixels,
          maxDecodedBytes,
          maxPeakBytes,
          transferMillis,
          decodeMillis,
          statusEveryMillis,
          maxStatusChecks,
          maxControlMillis,
        ].any((n) => n <= 0) ||
        BigInt.from(maxBufferBytes) <
            BigInt.from(maxEncodedBytes) * BigInt.from(3) ||
        BigInt.from(maxDecodedBytes) <
            BigInt.from(maxDecodePixels) * BigInt.from(4) ||
        BigInt.from(maxPeakBytes) <
            BigInt.from(maxBufferBytes) +
                BigInt.from(maxDecodePixels) * BigInt.from(16) +
                BigInt.from(maxDecodedBytes) ||
        [
          transferMillis,
          decodeMillis,
          statusEveryMillis,
          maxControlMillis,
        ].any((n) => n > maxSupportedMillis) ||
        statusEveryMillis > maxControlMillis) {
      throw ArgumentError('explicit_comparison_limits');
    }
  }
  final String runId;
  final int maxOwnedImages;
  final int maxEncodedBytes,
      maxBufferBytes,
      maxDecodePixels,
      maxDecodedBytes,
      maxPeakBytes,
      transferMillis,
      decodeMillis,
      statusEveryMillis,
      maxStatusChecks,
      maxControlMillis;
}

/// Mevcut geçici geliştirme credential sağlayıcısını kullanır. Revoke/account/token
/// nesli değişince eski sağlayıcı kapatılmalı; izlenemeyen sabit token kabul edilmez.
class ReadingComparisonAccount {
  ReadingComparisonAccount({
    required this.uid,
    required this.generation,
    required this.credentials,
  });
  final String uid;
  final int generation;
  final ImageExperimentCredentials credentials;
  Stream<void> get changes => credentials.revocations;
  String token() {
    if (uid != imageExperimentQaUid || generation < 0) {
      throw StateError('account_refused');
    }
    return credentials.requireActive();
  }
}

class ReadingImageComparisonConfiguration {
  const ReadingImageComparisonConfiguration.geometry()
    : baseUrl = null,
      account = null,
      limits = null;
  ReadingImageComparisonConfiguration.media({
    required Uri origin,
    required ReadingComparisonAccount credentials,
    required ReadingComparisonLimits profile,
  }) : baseUrl = origin,
       account = credentials,
       limits = profile {
    if (!['http', 'https'].contains(origin.scheme) ||
        origin.host.isEmpty ||
        origin.userInfo.isNotEmpty ||
        origin.hasQuery ||
        origin.hasFragment ||
        (origin.path.isNotEmpty && origin.path != '/')) {
      throw ArgumentError('comparison_origin');
    }
    credentials.token();
  }
  final Uri? baseUrl;
  final ReadingComparisonAccount? account;
  final ReadingComparisonLimits? limits;
  bool get mediaEnabled => baseUrl != null && account != null && limits != null;
}

/// Debugger/test kompozisyon noktası; token dosya veya compile-time define'a yazılmaz.
class ReadingImageComparisonHarness {
  static ReadingImageComparisonConfiguration configuration =
      const ReadingImageComparisonConfiguration.geometry();
  static int contentId = 0;
}
