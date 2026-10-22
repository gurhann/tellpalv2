import 'dart:async';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:crypto/crypto.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import '../../../../app/reading_image_consumer_configuration.dart';
import '../../../../core/network/library_image_cache.dart';
import '../../../../core/network/memory_image_cache_storage.dart';
import '../../domain/use_case/select_reading_image_rendition.dart';
import '../../domain/entity/reading_image_manifest.dart';
import '../domain/reading_image_admission.dart';
import '../domain/load_reading_image.dart';

/// Aynı package lifecycle; dosya ve metadata yalnız bellekte kalır.
class ReadingImageMemoryCache extends LibraryImageCache {
  ReadingImageMemoryCache._(this.configuration, this._delivery)
    : super.profile(
        Config(
          'reading-consumer-memory',
          repo: MemoryImageCacheRepository(),
          fileSystem: MemoryImageCacheFiles(),
          maxNrOfCacheObjects: 4,
        ),
        artworkWait: configuration.requestWait,
        delegate: _delivery,
        maximumOwnedAttempts: 1,
        maximumFiles: 4,
        maximumBytes: configuration.maxCacheBytes,
        maximumTransferBytes: configuration.maxEncodedBytes,
      );
  factory ReadingImageMemoryCache.create(
    ReadingImageConsumerConfiguration config,
    LoadReadingImage loadImage,
  ) => ReadingImageMemoryCache._(config, _RenditionService(loadImage));
  final ReadingImageConsumerConfiguration configuration;
  final _RenditionService _delivery;
  @override
  Future<void> settleOwnedWork() async {
    await super.settleOwnedWork();
    await _delivery.settle();
  }

  @override
  Future<void> close() async {
    try {
      await super.close();
    } finally {
      await _delivery.settle();
    }
  }

  ReadingImageSelection? _selected;
  String? _transfer;
  bool Function()? _current;
  void admit({
    required ReadingImageAdmission admission,
    required ReadingImageBinding binding,
    required ReadingImageSelection selection,
    required String transfer,
    required String url,
    required ReadingImageCancellation cancellation,
    required bool Function() isCurrent,
  }) {
    _selected = selection;
    _transfer = transfer;
    _current = isCurrent;
    _delivery.admit(
      admission,
      binding,
      selection,
      url,
      cancellation,
      isCurrent,
    );
  }

  @override
  Stream<FileResponse> getFileStream(
    String url, {
    String? key,
    Map<String, String>? headers,
    bool withProgress = false,
  }) {
    if (_current?.call() != true || key != _transfer) {
      return Stream.error(StateError('consumer_fresh_admission_required'));
    }
    return super.getFileStream(
      url,
      key: key,
      headers: headers,
      withProgress: withProgress,
    );
  }

  @override
  Future<void> validateResponse(FileInfo response, String key) async {
    final selected = _selected?.rendition.raster;
    if (selected == null || _current?.call() != true || key != _transfer) {
      throw StateError('consumer_stale');
    }
    if (await response.file.length() != selected.byteSize ||
        selected.byteSize > configuration.maxEncodedBytes) {
      throw const FormatException('consumer_length');
    }
    final bytes = await response.file.readAsBytes();
    if (sha256.convert(bytes).toString() != selected.sha256 ||
        _mime(bytes) != selected.mime) {
      throw const FormatException('consumer_integrity');
    }
    final buffer = await ui.ImmutableBuffer.fromUint8List(bytes);
    ui.ImageDescriptor? descriptor;
    try {
      descriptor = await ui.ImageDescriptor.encoded(buffer);
      if (descriptor.width != selected.width ||
          descriptor.height != selected.height ||
          descriptor.width * descriptor.height >
              configuration.maxSourcePixels) {
        throw const FormatException('consumer_geometry');
      }
      if (_current?.call() != true) throw StateError('consumer_stale');
    } finally {
      descriptor?.dispose();
      buffer.dispose();
    }
  }

  static String _mime(Uint8List bytes) {
    if (bytes.length >= 8 &&
        bytes[0] == 137 &&
        bytes[1] == 80 &&
        bytes[2] == 78 &&
        bytes[3] == 71) {
      return 'image/png';
    }
    if (bytes.length >= 4 && bytes[0] == 255 && bytes[1] == 216) {
      return 'image/jpeg';
    }
    if (bytes.length >= 12 &&
        String.fromCharCodes(bytes.sublist(0, 4)) == 'RIFF' &&
        String.fromCharCodes(bytes.sublist(8, 12)) == 'WEBP') {
      return 'image/webp';
    }
    throw const FormatException('consumer_mime');
  }
}

class _RenditionService extends FileService {
  _RenditionService(this.loadImage);
  final LoadReadingImage loadImage;
  final _pending = <Future<void>>{};
  Future<void> settle() async => Future.wait(_pending.toList());
  ReadingImageAdmission? _admission;
  ReadingImageBinding? _binding;
  ReadingImageSelection? _selection;
  String? _url;
  ReadingImageCancellation? _cancel;
  bool Function()? _current;
  void admit(
    ReadingImageAdmission admission,
    ReadingImageBinding binding,
    ReadingImageSelection selection,
    String url,
    ReadingImageCancellation cancellation,
    bool Function() current,
  ) {
    _admission = admission;
    _binding = binding;
    _selection = selection;
    _url = url;
    _cancel = cancellation;
    _current = current;
  }

  @override
  Future<FileServiceResponse> get(
    String url, {
    Map<String, String>? headers,
  }) async {
    final selection = _selection;
    final admission = _admission;
    final cancellation = _cancel;
    if (url != _url ||
        selection == null ||
        admission == null ||
        cancellation == null ||
        _current?.call() != true) {
      throw StateError('consumer_unadmitted');
    }
    final request = loadImage.bytes(
      admission,
      _binding!,
      selection,
      cancellation,
    );
    final settled = request.then<void>(
      (_) {},
      onError: (Object _, StackTrace _) {},
    );
    _pending.add(settled);
    final result = await request.whenComplete(() => _pending.remove(settled));
    final bytes = result.fold(
      (failure) => throw StateError('consumer_bytes_unavailable'),
      (bytes) => bytes,
    );
    if (_current?.call() != true || cancellation.cancelled) {
      throw StateError('consumer_stale');
    }
    return _MemoryResponse(bytes, selection.rendition.raster.mime);
  }
}

class _MemoryResponse implements FileServiceResponse {
  _MemoryResponse(this.bytes, this.mime);
  final Uint8List bytes;
  final String mime;
  @override
  Stream<List<int>> get content => Stream.value(bytes);
  @override
  int get contentLength => bytes.length;
  @override
  int get statusCode => 200;
  @override
  DateTime get validTill => DateTime.now().add(const Duration(minutes: 10));
  @override
  String? get eTag => null;
  @override
  String get fileExtension => mime.split('/').last;
}
