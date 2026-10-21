import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';

/// Yalnız desteklenen kapların profilini okur; renk dönüşümü yapmaz.
String? p5dIccSha256(Uint8List bytes, String format) {
  final data = ByteData.sublistView(bytes);
  final profiles = <Uint8List>[];
  if (format == 'PNG') {
    if (bytes.length < 8 ||
        bytes.take(8).join(',') != '137,80,78,71,13,10,26,10') {
      throw const FormatException('png_signature');
    }
    for (var p = 8; p + 12 <= bytes.length;) {
      final n = data.getUint32(p);
      if (p + 12 + n > bytes.length) throw const FormatException('png_chunk');
      if (ascii.decode(bytes.sublist(p + 4, p + 8)) == 'iCCP') {
        final payload = bytes.sublist(p + 8, p + 8 + n);
        final zero = payload.indexOf(0);
        if (zero < 1 || zero + 2 >= n || payload[zero + 1] != 0) {
          throw const FormatException('png_icc');
        }
        profiles.add(
          Uint8List.fromList(zlib.decode(payload.sublist(zero + 2))),
        );
      }
      p += n + 12;
    }
  } else if (format == 'WEBP') {
    if (bytes.length < 20 ||
        ascii.decode(bytes.sublist(0, 4)) != 'RIFF' ||
        ascii.decode(bytes.sublist(8, 12)) != 'WEBP' ||
        data.getUint32(4, Endian.little) != bytes.length - 8) {
      throw const FormatException('webp_signature');
    }
    var lossless = false;
    for (var p = 12; p + 8 <= bytes.length;) {
      final name = ascii.decode(bytes.sublist(p, p + 4));
      final n = data.getUint32(p + 4, Endian.little);
      if (p + 8 + n > bytes.length) throw const FormatException('webp_chunk');
      if (name == 'ICCP') profiles.add(bytes.sublist(p + 8, p + 8 + n));
      if (name == 'VP8L') lossless = true;
      if (name == 'VP8 ' || name == 'ANIM' || name == 'ANMF') {
        throw const FormatException('webp_lossless_required');
      }
      p += 8 + n + (n & 1);
    }
    if (!lossless) throw const FormatException('webp_lossless_required');
  } else if (format == 'JPEG') {
    if (bytes.length < 4 || bytes[0] != 255 || bytes[1] != 216) {
      throw const FormatException('jpeg_signature');
    }
    final pieces = <int, Uint8List>{};
    var expected = 0;
    for (var p = 2; p + 4 < bytes.length;) {
      if (bytes[p] != 255) throw const FormatException('jpeg_marker');
      final marker = bytes[p + 1];
      if (marker == 218 || marker == 217) break;
      final n = data.getUint16(p + 2);
      if (n < 2 || p + 2 + n > bytes.length) {
        throw const FormatException('jpeg_segment');
      }
      final payload = bytes.sublist(p + 4, p + 2 + n);
      if (marker == 226 &&
          payload.length >= 14 &&
          ascii.decode(payload.sublist(0, 12), allowInvalid: true) ==
              'ICC_PROFILE\u0000') {
        if (pieces.containsKey(payload[12]) ||
            (expected != 0 && expected != payload[13])) {
          throw const FormatException('jpeg_icc');
        }
        expected = payload[13];
        pieces[payload[12]] = payload.sublist(14);
      }
      p += 2 + n;
    }
    if (pieces.isNotEmpty) {
      if (pieces.length != expected) throw const FormatException('jpeg_icc');
      profiles.add(
        Uint8List.fromList([
          for (var i = 1; i <= expected; i++)
            ...pieces[i] ?? (throw const FormatException('jpeg_icc')),
        ]),
      );
    }
  } else {
    throw const FormatException('format_refused');
  }
  if (profiles.length > 1) throw const FormatException('duplicate_icc');
  if (profiles.isNotEmpty && profiles.single.length > 65536) {
    throw const FormatException('icc_limit');
  }
  return profiles.isEmpty ? null : sha256.convert(profiles.single).toString();
}
