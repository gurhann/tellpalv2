import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import '../test_driver/native_runtime_guard.dart';

String syntheticToken(String uid, int expiry) =>
    'synthetic.${base64Url.encode(utf8.encode(jsonEncode({'sub': uid, 'exp': expiry})))}.unsigned';

void main() {
  test(
    'real provider descriptor rejects wrong UID, context, checksum, unexpired URL or provider; private file mode is required',
    () async {
      final now = DateTime.utc(2026, 10, 7);
      final descriptor = <String, dynamic>{
        'firebaseUid': 'selected',
        'categoryId': 2,
        'contentId': 3,
        'requestedLanguage': 'tr',
        'contentLanguage': 'tr',
        'experience': 'READING',
        'assetId': 3,
        'checksumSha256': List.filled(64, 'a').join(),
        'byteSize': 42692,
        'mimeType': 'image/jpeg',
        'expiresAt': now.subtract(const Duration(seconds: 1)).toIso8601String(),
        'expiredUrl':
            'https://storage.googleapis.com/synthetic/object?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Expires=12&X-Goog-Signature=abc123',
      };
      requireProviderExpiryDescriptor(descriptor, 'selected', now);
      for (final change in <Map<String, dynamic>>[
        {'firebaseUid': 'other'},
        {'categoryId': 3},
        {'contentId': 9},
        {'contentLanguage': 'en'},
        {'requestedLanguage': 'en'},
        {'experience': 'LISTENING'},
        {'assetId': 9},
        {'checksumSha256': 'invalid'},
        {'byteSize': 20971521},
        {'expiresAt': now.add(const Duration(seconds: 1)).toIso8601String()},
        {'expiredUrl': 'https://example.invalid/object'},
        {'unexpected': true},
      ]) {
        expect(
          () => requireProviderExpiryDescriptor(
            {...descriptor, ...change},
            'selected',
            now,
          ),
          throwsStateError,
        );
      }
      final directory = await preparePrivateOutput(
        await Directory.systemTemp.createTemp('story12-provider-guard-'),
      );
      addTearDown(() => directory.delete(recursive: true));
      final file = File('${directory.path}/descriptor.json');
      await writePrivateJson(file, descriptor);
      expect(
        await readProviderExpiryDescriptor(file, 'selected', now),
        descriptor,
      );
      await privateMode(file.path, '644');
      await expectLater(
        readProviderExpiryDescriptor(file, 'selected', now),
        throwsStateError,
      );
      await privateMode(file.path, '600');
      final link = Link('${directory.path}/descriptor-link');
      await link.create(file.path);
      await expectLater(
        readProviderExpiryDescriptor(File(link.path), 'selected', now),
        throwsStateError,
      );
    },
  );
  test(
    'local guard rejects different selected UID and expired or malformed token',
    () {
      final now = DateTime.utc(2026, 10, 7),
          seconds = DateTime.utc(2026, 10, 7).millisecondsSinceEpoch ~/ 1000;
      requireSelectedToken(
        syntheticToken('selected', seconds + 120),
        'selected',
        now,
      );
      expect(
        () => requireSelectedToken(
          syntheticToken('other', seconds + 120),
          'selected',
          now,
        ),
        throwsStateError,
      );
      expect(
        () => requireSelectedToken(
          syntheticToken('selected', seconds),
          'selected',
          now,
        ),
        throwsStateError,
      );
      expect(
        () => requireSelectedToken('malformed', 'selected', now),
        throwsStateError,
      );
    },
  );
  test(
    'private output rejects repository and symlink escapes and restricts file modes',
    () async {
      await expectLater(
        preparePrivateOutput(Directory.current),
        throwsStateError,
      );
      final temp = await Directory.systemTemp.createTemp(
        'story12-private-output-',
      );
      addTearDown(() => temp.delete(recursive: true));
      final link = Link('${temp.path}/escape');
      await link.create(Directory.current.path);
      await expectLater(
        preparePrivateOutput(Directory('${link.path}/unused')),
        throwsStateError,
      );
      final directory = await preparePrivateOutput(
        Directory('${temp.path}/accepted'),
      );
      final file = File('${directory.path}/guard.json');
      await writePrivateJson(file, {'guard': true});
      expect((await directory.stat()).mode & 0x1ff, 0x1c0);
      expect((await file.stat()).mode & 0x1ff, 0x180);
      final fileLink = Link('${directory.path}/linked.json');
      await fileLink.create(file.path);
      await expectLater(
        writePrivateJson(File(fileLink.path), {'overwrite': true}),
        throwsStateError,
      );
      expect(jsonDecode(await file.readAsString()), {'guard': true});
    },
  );
}
