import 'dart:async';

/// Yalnız açık yerel/test geliştirme kompozisyonu; credential bellekte tutulur.
class ReadingImageConsumerConfiguration {
  ReadingImageConsumerConfiguration({
    required this.origin,
    required this.accountScope,
    required this.token,
    required this.accountChanges,
    required String environment,
    required this.maxEncodedBytes,
    required this.maxCacheBytes,
    required this.maxSourcePixels,
    required this.maxDecodePixels,
    required this.requestWait,
    required this.statusEvery,
    required this.maxLease,
  }) {
    if (!{'local', 'test'}.contains(environment) ||
        !{'http', 'https'}.contains(origin.scheme) ||
        origin.host.isEmpty ||
        origin.userInfo.isNotEmpty ||
        origin.hasQuery ||
        origin.hasFragment ||
        (origin.path.isNotEmpty && origin.path != '/') ||
        accountScope.isEmpty ||
        maxEncodedBytes <= 0 ||
        maxEncodedBytes > 20971520 ||
        maxCacheBytes < maxEncodedBytes * 4 ||
        maxSourcePixels <= 0 ||
        maxDecodePixels <= 0 ||
        maxDecodePixels > 4194304 ||
        [
          requestWait,
          statusEvery,
          maxLease,
        ].any((d) => d <= Duration.zero || d.inMilliseconds > 0x7fffffff) ||
        statusEvery > maxLease) {
      throw ArgumentError('explicit_consumer_configuration');
    }
  }
  final Uri origin;
  final String accountScope;
  final String Function() token;
  final Stream<void> accountChanges;
  final int maxEncodedBytes, maxCacheBytes, maxSourcePixels, maxDecodePixels;
  final Duration requestWait, statusEvery, maxLease;
}
