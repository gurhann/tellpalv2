import 'dart:convert';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:tellpal_mobile_poc/app/reading_image_consumer_configuration.dart';
import 'package:tellpal_mobile_poc/features/reading_images/domain/entity/reading_image_manifest.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/domain/reading_image_admission.dart';
import 'package:tellpal_mobile_poc/features/reading_images/consumer/data/reading_image_admission_model.dart';
import 'fixture.dart';

final pngBytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAFElEQVR4nGO8k6LBgA0wYRUdtBIAHjsBeE34RiIAAAAASUVORK5CYII=',
);
final jpegBytes = base64Decode(
  '/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAIAAgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwBaKKK+TPuj/9k=',
);
final webpBytes = base64Decode(
  'UklGRh4AAABXRUJQVlA4TBEAAAAvB8ABAAdQsnIXpf+BiOh/AAA=',
);
ReadingImageConsumerConfiguration consumerConfiguration({
  Stream<void> changes = const Stream.empty(),
  Uri? origin,
  int maxLeaseMillis = 60000,
  Duration requestWait = const Duration(seconds: 3),
  Duration statusEvery = const Duration(seconds: 10),
}) => ReadingImageConsumerConfiguration(
  origin: origin ?? Uri.parse('http://localhost:1'),
  accountScope: 'account',
  token: () => 'ephemeral',
  accountChanges: changes,
  environment: 'test',
  maxEncodedBytes: 1048576,
  maxCacheBytes: 4194304,
  maxSourcePixels: 4194304,
  maxDecodePixels: 4194304,
  requestWait: requestWait,
  statusEvery: statusEvery,
  maxLease: Duration(milliseconds: maxLeaseMillis),
);

class ConsumerFixture {
  ConsumerFixture({
    String format = 'PNG',
    int leaseMillis = 30000,
    String policy = 'PRESERVE_JPEG_OR_LOSSLESS_WEBP',
    int pages = 1,
  }) {
    bytes = switch (format) {
      'JPEG' => jpegBytes,
      'WEBP' => webpBytes,
      _ => pngBytes,
    };
    final sourceBytes = format == 'JPEG' ? jpegBytes : pngBytes;
    final p = ReadingImageProfile(
      fingerprint: hash('d'),
      width: 8,
      height: 8,
      allowGrowth: true,
      formatPolicy: policy,
    );
    final sources = [
      for (final role in ReadingImageRole.values)
        ReadingImageSource(
          binding: binding(role),
          raster: raster(
            width: 8,
            height: 8,
            bytes: sourceBytes.length,
            mime: format == 'JPEG' ? 'image/jpeg' : 'image/png',
            sha: sha256.convert(sourceBytes).toString(),
          ),
        ),
      for (var page = 2; page <= pages; page++)
        ReadingImageSource(
          binding: binding(
            ReadingImageRole.readingPage,
            pageId: 10 + page,
            pageNumber: page,
          ),
          raster: raster(
            width: 8,
            height: 8,
            bytes: sourceBytes.length,
            mime: format == 'JPEG' ? 'image/jpeg' : 'image/png',
            sha: sha256.convert(sourceBytes).toString(),
          ),
        ),
    ];
    final manifest = seal([
      for (final s in sources)
        ReadingImageEntry(
          source: s,
          renditions: [
            rendition(
              s,
              p,
              width: 8,
              height: 8,
              bytes: bytes.length,
              sha: sha256.convert(bytes).toString(),
              mime: 'image/${format.toLowerCase()}',
            ),
          ],
        ),
    ]);
    final i = manifest.identity;
    Map<String, Object?> imageRaster(ReadingImageRaster s) => {
      'provider': s.provider,
      'objectKey': s.objectKey,
      'generation': s.generation,
      'mime': s.mime,
      'byteSize': s.byteSize,
      'sha256': s.sha256,
      'width': s.width,
      'height': s.height,
    };
    String roleName(ReadingImageRole role) =>
        ['READING_COVER', 'READING_DETAIL', 'READING_PAGE'][role.index];
    json = {
      'accountScope': 'account',
      'delivery': {
        'leaseMillis': leaseMillis,
        'identity': {
          'contentId': i.contentId,
          'language': i.language,
          'liveGeneration': 1,
          'revision': i.revision,
          'manifestDigest': i.manifestDigest,
          'jobId': i.jobId,
        },
        'descriptor': {
          'images': [
            for (final e in manifest.images)
              {
                'source': {
                  'binding': {
                    'role': roleName(e.source.binding.role),
                    'localizationId': e.source.binding.localizationId,
                    'assetId': e.source.binding.assetId,
                    'pageId': e.source.binding.pageId,
                    'pageNumber': e.source.binding.pageNumber,
                  },
                  'raster': imageRaster(e.source.raster),
                },
                'renditions': [
                  for (final output in e.renditions)
                    {
                      'renditionKey': output.renditionKey,
                      'sourceSha256': output.sourceSha256,
                      'profileFingerprint': output.profileFingerprint,
                      'raster': imageRaster(output.raster),
                    },
                ],
              },
          ],
          'profiles': {
            for (final role in ReadingImageRole.values)
              roleName(role): [
                {
                  'fingerprint': p.fingerprint,
                  'width': p.width,
                  'height': p.height,
                  'allowGrowth': p.allowGrowth,
                  'formatPolicy': p.formatPolicy,
                },
              ],
          },
        },
      },
    };
  }
  late final Uint8List bytes;
  late final Map<String, dynamic> json;
  ReadingImageAdmission get admission =>
      ReadingImageAdmissionModel.fromJson(json);
}
