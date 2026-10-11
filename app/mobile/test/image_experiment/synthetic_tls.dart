import 'dart:io';
import 'dart:typed_data';

/// Temporary synthetic certificate material; no runtime credential is loaded.
class SyntheticTlsIdentity {
  SyntheticTlsIdentity._(this.directory, this.der, this.context);
  final Directory directory;
  final Uint8List der;
  final SecurityContext context;
  String get certificatePath => '${directory.path}/cert.pem';
  String get keyPath => '${directory.path}/key.pem';
  static Future<SyntheticTlsIdentity> create() async {
    final directory = await Directory.systemTemp.createTemp(
      'tellpal-synthetic-tls-',
    );
    final key = '${directory.path}/key.pem',
        certificate = '${directory.path}/cert.pem';
    try {
      final generated = await Process.run('/usr/bin/openssl', [
        'req',
        '-x509',
        '-newkey',
        'rsa:2048',
        '-nodes',
        '-keyout',
        key,
        '-out',
        certificate,
        '-subj',
        '/CN=localhost',
        '-addext',
        'subjectAltName=DNS:localhost',
        '-addext',
        'basicConstraints=critical,CA:TRUE',
        '-addext',
        'extendedKeyUsage=serverAuth',
        '-addext',
        'keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign',
        '-days',
        '1',
      ]);
      if (generated.exitCode != 0) {
        throw StateError('synthetic_certificate_refused');
      }
      final converted = await Process.run('/usr/bin/openssl', [
        'x509',
        '-in',
        certificate,
        '-outform',
        'DER',
        '-out',
        '${directory.path}/cert.der',
      ]);
      if (converted.exitCode != 0) {
        throw StateError('synthetic_certificate_refused');
      }
      return SyntheticTlsIdentity._(
        directory,
        await File('${directory.path}/cert.der').readAsBytes(),
        SecurityContext()
          ..useCertificateChain(certificate)
          ..usePrivateKey(key),
      );
    } catch (_) {
      await directory.delete(recursive: true);
      rethrow;
    }
  }

  Future<void> close() => directory.delete(recursive: true);
}
