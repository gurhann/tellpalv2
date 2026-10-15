import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import 'story_manifest.dart';
import 'story_manifest_client.dart';
import 'story_reader_images.dart';

class StoryPlayerPage extends StatefulWidget {
  const StoryPlayerPage({
    super.key,
    required this.client,
    required this.contentId,
    required this.languageCode,
    this.grantRefreshWindow = const Duration(minutes: 1),
    this.images,
    this.controller,
  });

  final StoryManifestSource client;
  final int contentId;
  final String languageCode;
  final Duration grantRefreshWindow;
  final StoryReaderImages? images;
  final StoryReaderController? controller;

  @override
  State<StoryPlayerPage> createState() => _StoryPlayerPageState();
}

class _StoryPlayerPageState extends State<StoryPlayerPage> {
  final AudioPlayer _player = AudioPlayer();
  StreamSubscription<PlayerState>? _playerStateSubscription;
  Future<void> _audioTransition = Future<void>.value();
  StoryManifest? _manifest;
  String? _manifestError;
  String? _mediaError;
  int _pageIndex = 0;
  int _selectionGeneration = 0;
  int? _loadedGeneration;
  int? _completedGeneration;
  double? _seekPreviewMs;
  bool _loadingManifest = true;
  bool _loadingMedia = false;
  bool _refreshing = false;

  @override
  void initState() {
    super.initState();
    widget.controller?.attach(_selectPage);
    _playerStateSubscription = _player.playerStateStream.listen(_onPlayerState);
    unawaited(_loadManifest());
  }

  @override
  void dispose() {
    _selectionGeneration++;
    widget.controller?.detach();
    _playerStateSubscription?.cancel();
    widget.client.close();
    unawaited(_player.dispose());
    super.dispose();
  }

  Future<void> _loadManifest() async {
    setState(() {
      _loadingManifest = true;
      _manifestError = null;
    });
    try {
      final manifest = await widget.client.fetch(
        contentId: widget.contentId,
        languageCode: widget.languageCode,
      );
      if (!mounted) return;
      setState(() {
        _manifest = manifest;
        _loadingManifest = false;
      });
      _selectPage(0);
    } on StoryManifestException catch (error) {
      if (!mounted) return;
      setState(() {
        _manifestError = error.message;
        _loadingManifest = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _manifestError = 'Medya manifesti açılamadı.';
        _loadingManifest = false;
      });
    }
  }

  void _selectPage(int index, {bool autoplay = true}) {
    final manifest = _manifest;
    if (manifest == null || index < 0 || index >= manifest.pages.length) return;
    final generation = ++_selectionGeneration;
    widget.images?.selected(manifest.pages[index], generation);
    setState(() {
      _pageIndex = index;
      _loadedGeneration = null;
      _mediaError = null;
      _loadingMedia = true;
      _seekPreviewMs = null;
    });
    _audioTransition = _audioTransition.then(
      (_) => _prepareSelectedPage(generation, autoplay: autoplay),
    );
  }

  Future<void> _prepareSelectedPage(
    int generation, {
    required bool autoplay,
  }) async {
    if (!mounted || generation != _selectionGeneration) return;
    try {
      await _refreshIfNeeded(generation);
      if (!mounted || generation != _selectionGeneration) return;

      final manifest = _manifest!;
      final page = manifest.pages[_pageIndex];
      var imageFailed = false;
      final provider =
          await (widget.images?.prepare(page) ??
              Future<ImageProvider>.value(
                NetworkImage(page.illustration.url.toString()),
              ));
      if (!mounted || generation != _selectionGeneration) return;
      await precacheImage(
        provider,
        context,
        onError: (error, stackTrace) => imageFailed = true,
      ).timeout(const Duration(seconds: 15));
      if (imageFailed) {
        throw const StoryManifestException('Görsel yüklenemedi.');
      }
      if (!mounted || generation != _selectionGeneration) return;

      await _player.stop();
      if (!mounted || generation != _selectionGeneration) return;
      if (page.audio != null) {
        await _player
            .setUrl(page.audio!.url.toString())
            .timeout(const Duration(seconds: 15));
      }
      if (!mounted || generation != _selectionGeneration) return;

      setState(() {
        _loadedGeneration = generation;
        _loadingMedia = false;
      });
      _prefetchNextImage();
      if (autoplay && page.audio != null) unawaited(_playSafely());
    } catch (_) {
      if (!mounted || generation != _selectionGeneration) return;
      setState(() {
        _loadingMedia = false;
        _mediaError =
            'Sayfanın görseli veya sesi yüklenemedi. Bağlantıları yenileyip tekrar deneyin.';
      });
    }
  }

  Future<void> _refreshIfNeeded(int generation) async {
    final manifest = _manifest!;
    final page = manifest.pages[_pageIndex];
    if (!page.illustration.expiresWithin(widget.grantRefreshWindow) &&
        !(page.audio?.expiresWithin(widget.grantRefreshWindow) ?? false)) {
      return;
    }
    final refreshed = await widget.client.fetch(
      contentId: widget.contentId,
      languageCode: widget.languageCode,
      revision: manifest.revision,
    );
    _validateSameRevision(manifest, refreshed);
    if (mounted && generation == _selectionGeneration) {
      setState(() => _manifest = refreshed);
    }
  }

  Future<void> _refreshGrants() async {
    final manifest = _manifest;
    if (_refreshing || manifest == null) return;
    final autoplay = _player.playing || _mediaError != null;
    setState(() => _refreshing = true);
    try {
      final refreshed = await widget.client.fetch(
        contentId: widget.contentId,
        languageCode: widget.languageCode,
        revision: manifest.revision,
      );
      _validateSameRevision(manifest, refreshed);
      if (!mounted) return;
      setState(() {
        _manifest = refreshed;
        _refreshing = false;
      });
      _selectPage(_pageIndex, autoplay: autoplay);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _refreshing = false;
        _mediaError = 'Bağlantılar yenilenemedi. Backend ve ağı kontrol edin.';
      });
    }
  }

  void _validateSameRevision(StoryManifest previous, StoryManifest refreshed) {
    if (previous.revision != refreshed.revision ||
        previous.pages.length != refreshed.pages.length ||
        !List.generate(previous.pages.length, (index) => index).every(
          (index) =>
              previous.pages[index].pageId == refreshed.pages[index].pageId &&
              previous.pages[index].illustration.assetId ==
                  refreshed.pages[index].illustration.assetId &&
              previous.pages[index].audio?.assetId ==
                  refreshed.pages[index].audio?.assetId,
        )) {
      throw const StoryManifestException(
        'Yenilenen medya farklı bir sürüme ait.',
      );
    }
  }

  void _prefetchNextImage() {
    final manifest = _manifest!;
    if (_pageIndex + 1 >= manifest.pages.length) return;
    final nextPage = manifest.pages[_pageIndex + 1];
    final grant = nextPage.illustration;
    if (grant.expiresSoon) return;
    unawaited(() async {
      try {
        final provider =
            await (widget.images?.prepare(nextPage) ??
                Future<ImageProvider>.value(
                  NetworkImage(grant.url.toString()),
                ));
        if (!mounted) return;
        await precacheImage(provider, context, onError: (error, stackTrace) {});
      } catch (_) {
        // A speculative next page cannot replace the selected-page error state.
      }
    }());
  }

  void _onPlayerState(PlayerState state) {
    if (state.processingState != ProcessingState.completed ||
        _loadedGeneration != _selectionGeneration ||
        _completedGeneration == _selectionGeneration ||
        _manifest == null ||
        _pageIndex + 1 >= _manifest!.pages.length) {
      return;
    }
    _completedGeneration = _selectionGeneration;
    _selectPage(_pageIndex + 1);
  }

  Future<void> _playSafely() async {
    try {
      await _player.play();
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _mediaError =
            'Ses oynatılamadı. Bağlantıları yenileyip tekrar deneyin.',
      );
    }
  }

  Future<void> _togglePlayback() async {
    if (_player.playing) {
      await _player.pause();
      return;
    }
    if (_player.processingState == ProcessingState.completed) {
      await _player.seek(Duration.zero);
    }
    unawaited(_playSafely());
  }

  Future<void> _seekTo(double milliseconds) async {
    setState(() => _seekPreviewMs = null);
    try {
      await _player.seek(Duration(milliseconds: milliseconds.round()));
    } catch (_) {
      if (mounted) setState(() => _mediaError = 'Ses konumu değiştirilemedi.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final manifest = _manifest;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Haberci Nota'),
        actions: [
          if (manifest != null)
            IconButton(
              tooltip: 'Medya bağlantılarını yenile',
              onPressed: _refreshing ? null : _refreshGrants,
              icon: _refreshing
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.refresh),
            ),
        ],
      ),
      body: _loadingManifest
          ? const Center(child: CircularProgressIndicator())
          : manifest == null
          ? _buildManifestError()
          : _buildReader(manifest),
    );
  }

  Widget _buildManifestError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _manifestError ?? 'Medya manifesti açılamadı.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _loadManifest,
              child: const Text('Tekrar dene'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReader(StoryManifest manifest) {
    final page = manifest.pages[_pageIndex];
    return SafeArea(
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              'Sayfa ${page.pageNumber}/${manifest.pages.length} · Test sürümü ${manifest.revision.substring(0, min(8, manifest.revision.length))}',
            ),
          ),
          Expanded(
            child: ColoredBox(
              color: Colors.black,
              child: Center(
                child: widget.images == null
                    ? Image.network(
                        page.illustration.url.toString(),
                        key: ValueKey(page.illustration.url),
                        fit: BoxFit.contain,
                        semanticLabel: 'Hikâye sayfası ${page.pageNumber}',
                        loadingBuilder: (context, child, progress) =>
                            progress == null
                            ? child
                            : const CircularProgressIndicator(),
                        errorBuilder: (context, error, stackTrace) => Center(
                          child: TextButton(
                            onPressed: _refreshGrants,
                            child: const Text(
                              'Görsel açılamadı · bağlantıyı yenile',
                            ),
                          ),
                        ),
                      )
                    : LayoutBuilder(
                        builder: (context, constraints) {
                          final media = MediaQuery.of(this.context);
                          widget.images!.observeLayout(
                            StoryReaderGeometry(
                              slot: constraints.biggest,
                              dpr: media.devicePixelRatio,
                              textScale: media.textScaler.scale(1),
                              padding: media.padding,
                              viewInsets: media.viewInsets,
                            ),
                          );
                          return widget.images!.display(context, page);
                        },
                      ),
              ),
            ),
          ),
          if (_loadingMedia || widget.images != null)
            SizedBox(
              height: 4,
              child: _loadingMedia ? const LinearProgressIndicator() : null,
            ),
          if (_mediaError != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(_mediaError!, textAlign: TextAlign.center),
            ),
          _buildAudioControls(page),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                OutlinedButton.icon(
                  onPressed: _pageIndex == 0
                      ? null
                      : () => _selectPage(_pageIndex - 1),
                  icon: const Icon(Icons.chevron_left),
                  label: const Text('Önceki'),
                ),
                OutlinedButton.icon(
                  onPressed: _pageIndex + 1 >= manifest.pages.length
                      ? null
                      : () => _selectPage(_pageIndex + 1),
                  icon: const Icon(Icons.chevron_right),
                  label: const Text('Sonraki'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAudioControls(StoryPage page) {
    if (page.audio == null) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Text('Bu sayfada ses yok.'),
      );
    }
    return StreamBuilder<PlayerState>(
      stream: _player.playerStateStream,
      builder: (context, playerSnapshot) {
        final playing = playerSnapshot.data?.playing ?? false;
        return Row(
          children: [
            IconButton.filled(
              tooltip: playing ? 'Duraklat' : 'Oynat',
              onPressed:
                  _loadingMedia ||
                      _mediaError != null ||
                      _loadedGeneration != _selectionGeneration
                  ? null
                  : _togglePlayback,
              icon: Icon(playing ? Icons.pause : Icons.play_arrow),
            ),
            Expanded(
              child: StreamBuilder<Duration>(
                stream: _player.positionStream,
                builder: (context, positionSnapshot) {
                  final duration = _player.duration;
                  final durationMs = max(1, duration?.inMilliseconds ?? 0);
                  final positionMs = min(
                    positionSnapshot.data?.inMilliseconds ?? 0,
                    durationMs,
                  );
                  final sliderValue = (_seekPreviewMs ?? positionMs.toDouble())
                      .clamp(0.0, durationMs.toDouble());
                  return Column(
                    children: [
                      Slider(
                        value: sliderValue,
                        max: durationMs.toDouble(),
                        onChanged: duration == null
                            ? null
                            : (value) => setState(() => _seekPreviewMs = value),
                        onChangeEnd: duration == null ? null : _seekTo,
                      ),
                      Text(
                        '${_formatDuration(Duration(milliseconds: positionMs))} / ${_formatDuration(duration ?? Duration.zero)}',
                      ),
                    ],
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}

String _formatDuration(Duration duration) {
  final minutes = duration.inMinutes.toString().padLeft(2, '0');
  final seconds = (duration.inSeconds % 60).toString().padLeft(2, '0');
  return '$minutes:$seconds';
}
