import 'package:injectable/injectable.dart';
import '../../../app/image_experiment_configuration.dart';
import '../../../story_manifest.dart';
import '../../../story_manifest_client.dart';

/// Synthetic QA identities map only to the admitted six local PNG objects.
@LazySingleton(as: StoryManifestSource)
class ReaderManifestSource implements StoryManifestSource {
  ReaderManifestSource(this.configuration);
  final ImageExperimentConfiguration configuration;
  static const qaContentId = 71;
  static const qaRevision = 'qa-reader-resolution-r1';
  bool _closed = false;
  @override
  Future<StoryManifest> fetch({
    required int contentId,
    required String languageCode,
    String? revision,
  }) async {
    configuration.credentials.requireActive();
    if (_closed ||
        !configuration.isReader ||
        contentId != qaContentId ||
        languageCode != 'tr' ||
        (revision != null && revision != qaRevision)) {
      throw const StoryManifestException('Deney manifesti kullanılamıyor.');
    }
    return StoryManifest(
      contentId: qaContentId,
      languageCode: 'tr',
      revision: qaRevision,
      pages: List.unmodifiable([
        for (var index = 0; index < configuration.readerPageCount; index++)
          StoryPage(
            pageId: 160001 + index,
            pageNumber: index + 1,
            width: configuration.cases['reader_page_${index + 1}']!.width,
            height: configuration.cases['reader_page_${index + 1}']!.height,
            illustration: MediaGrant(
              assetId: 160101 + index,
              mimeType: 'image/png',
              url: configuration.objectUri('reader_page_${index + 1}'),
              expiresAt: configuration.credentials.expiresAt,
            ),
            audio: null,
          ),
      ]),
    );
  }

  @override
  void close() => _closed = true;
}
