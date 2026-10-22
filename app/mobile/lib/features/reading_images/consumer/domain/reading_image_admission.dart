import '../../domain/entity/reading_image_manifest.dart';
import '../../domain/use_case/validate_reading_image_manifest.dart';

class ReadingImageAdmission {
  ReadingImageAdmission({
    required this.accountScope,
    required this.manifest,
    required this.context,
    required this.liveGeneration,
    required this.leaseMillis,
  });
  final String accountScope;
  final ReadingImageManifest manifest;
  final ReadingImageManifestContext context;
  final int liveGeneration, leaseMillis;
  Map<String, String> get query => {
    'accountScope': accountScope,
    'contentId': '${manifest.identity.contentId}',
    'language': manifest.identity.language,
    'liveGeneration': '$liveGeneration',
    'revision': manifest.identity.revision,
    'manifestDigest': manifest.identity.manifestDigest,
    'jobId': manifest.identity.jobId,
  };
  bool sameIdentity(ReadingImageAdmission other) =>
      accountScope == other.accountScope &&
      liveGeneration == other.liveGeneration &&
      manifest.identity == other.manifest.identity;
}

class AdmittedReadingImage {
  AdmittedReadingImage(this.admission, this.manifest, this.remaining);
  final ReadingImageAdmission admission;
  final ValidatedReadingImageManifest manifest;
  final Duration remaining;
}

/// İptal sürmekte olan I/O'nun sahipliğini bitirmez.
class ReadingImageCancellation {
  bool cancelled = false;
  final _callbacks = <void Function()>{};
  void add(void Function() callback) {
    if (cancelled) {
      callback();
    } else {
      _callbacks.add(callback);
    }
  }

  void remove(void Function() callback) => _callbacks.remove(callback);
  void cancel() {
    cancelled = true;
    for (final callback in _callbacks.toList()) {
      callback();
    }
  }

  void check() {
    if (cancelled) throw StateError('consumer_cancelled');
  }
}
