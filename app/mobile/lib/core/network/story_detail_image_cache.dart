import 'package:crypto/crypto.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:http/http.dart' as http;
import 'package:injectable/injectable.dart';
import '../../app/library_configuration.dart';
import 'library_image_cache.dart';

/// App-owned hero profile: twenty files, 100 MiB disk, four owned attempts, 20 MiB per transfer.
@lazySingleton
class StoryDetailImageCache extends LibraryImageCache {
  StoryDetailImageCache(LibraryConfiguration configuration)
    : super.profile(
        Config(
          'detail-hero-${configuration.context.accessIdentity.hashCode.abs()}',
          stalePeriod: const Duration(minutes: 10),
          maxNrOfCacheObjects: 20,
        ),
        artworkWait: configuration.artworkWait,
      );
  // Test delegate is explicit so production always owns cancellable native clients.
  // ignore: use_super_parameters
  StoryDetailImageCache.forTesting(
    Config configuration, {
    Duration artworkWait = const Duration(seconds: 10),
    http.Client Function()? clientFactory,
  }) : super.profile(
         configuration,
         artworkWait: artworkWait,
         clientFactory: clientFactory,
         delegate: clientFactory == null ? configuration.fileService : null,
       );

  @override
  @disposeMethod
  Future<void> close() => super.close();

  @override
  Future<void> validateResponse(FileInfo response, String key) async {
    if (await response.file.length() > 20971520) {
      throw const FormatException('Hero encoded byte bound exceeded');
    }
    final checksum = key.split('::library-transfer::').first.split(':').last;
    if (sha256.convert(await response.file.readAsBytes()).toString() !=
        checksum) {
      throw const FormatException('Hero checksum differs from admitted source');
    }
  }
}
