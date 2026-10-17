import 'package:tellpal_mobile_poc/app/reading_image_comparison_configuration.dart';
import 'package:tellpal_mobile_poc/app/image_experiment_configuration.dart';

ReadingComparisonLimits limits({
  int transferMillis = 1000,
  int pixels = 10000000,
}) => ReadingComparisonLimits(
  runId: 'synthetic',
  maxOwnedImages: 1,
  maxEncodedBytes: 2000000,
  maxBufferBytes: 6000000,
  maxDecodePixels: pixels,
  maxDecodedBytes: pixels * 4,
  maxPeakBytes: 6000000 + pixels * 20,
  transferMillis: transferMillis,
  decodeMillis: 1000,
  statusEveryMillis: 1000,
  maxStatusChecks: 2,
  maxControlMillis: 3000,
);
ReadingImageComparisonConfiguration media(
  Uri origin, {
  ReadingComparisonLimits? profile,
}) => ReadingImageComparisonConfiguration.media(
  origin: origin,
  credentials: ReadingComparisonAccount(
    uid: imageExperimentQaUid,
    generation: 1,
    credentials: ImageExperimentCredentials(
      'synthetic-local-http-token',
      DateTime.now().add(const Duration(minutes: 1)),
    ),
  ),
  profile: profile ?? limits(),
);
