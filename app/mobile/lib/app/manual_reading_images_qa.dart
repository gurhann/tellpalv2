import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../core/theme/library_theme.dart';
import '../features/reading_images/presentation/cubit/reading_image_comparison_state.dart';
import '../features/reading_images/presentation/widgets/reading_image_comparison_raster.dart';
import 'local_lossless_qa_cubit.dart';
import 'local_lossless_qa_view.dart';

bool manualReadingImagesAllowed({
  required bool enabled,
  required bool debug,
  required bool web,
}) => enabled && debug && !web;

const _labels = [
  'Kapak · PNG',
  'Kapak · kayıpsız WebP',
  'Sayfa 1 · PNG',
  'Sayfa 1 · kayıpsız WebP',
  'Sayfa 2 · PNG',
  'Sayfa 2 · kayıpsız WebP',
];

/// Mevcut ağsız Cubit'i elle ilerletir; otomasyon veya HTTP girişi açmaz.
class ManualReadingImagesQaApp extends StatefulWidget {
  const ManualReadingImagesQaApp({super.key, required this.cubit});
  final LocalLosslessQaCubit cubit;

  @override
  State<ManualReadingImagesQaApp> createState() => _ManualAppState();
}

class _ManualAppState extends State<ManualReadingImagesQaApp>
    with WidgetsBindingObserver {
  bool _started = false, _busy = false;
  ({double width, double height, double dpr})? _viewport;
  late final _router = RootStackRouter.build(
    routes: [
      NamedRouteDef(
        name: 'ManualReadingImagesQaRoute',
        path: '/',
        initial: true,
        builder: (_, _) => BlocProvider.value(
          value: widget.cubit,
          child: _ManualView(advance: _advance, viewport: _setViewport),
        ),
      ),
    ],
  );

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  Future<void> _advance() async {
    final cubit = widget.cubit;
    if (_busy ||
        cubit.isClosed ||
        cubit.state.error != null ||
        cubit.state.status == 'stopped') {
      return;
    }
    _busy = true;
    try {
      if (!_started) {
        final viewport = _viewport;
        if (viewport == null) return;
        cubit.setViewport(
          width: viewport.width,
          height: viewport.height,
          dpr: viewport.dpr,
        );
        if (cubit.state.error != null) return;
        _started = true;
        await cubit.initialize();
      }
      if (!mounted || cubit.isClosed || cubit.state.error != null) return;
      final index = cubit.snapshot()['completedCases'] as int;
      if (index == 6) {
        await cubit.stop();
        return;
      }
      await cubit.show(index);
      if (!mounted || cubit.isClosed || cubit.state.status != 'loaded') return;
      await paintLocalLosslessQa(cubit);
    } catch (_) {
      if (!cubit.isClosed && cubit.state.status != 'stopped') {
        cubit.refuse('manual_display_refused');
      }
    } finally {
      _busy = false;
    }
  }

  void _setViewport(double width, double height, double dpr) {
    _viewport = (width: width, height: height, dpr: dpr);
    // Başlangıç inset'leri yerleşebilir. Ölçü yalnız Başlat'ta dondurulur.
    if (_started && !widget.cubit.isClosed) {
      widget.cubit.setViewport(width: width, height: height, dpr: dpr);
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (_started && state != AppLifecycleState.resumed) {
      widget.cubit.visibility(false);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    unawaited(widget.cubit.close());
    _router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'TellPal ağsız görsel kontrolü',
    theme: AppTypography.theme(),
    debugShowCheckedModeBanner: false,
    routerConfig: _router.config(),
  );
}

class _ManualView extends StatelessWidget {
  const _ManualView({required this.advance, required this.viewport});
  final Future<void> Function() advance;
  final void Function(double width, double height, double dpr) viewport;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Column(
        children: [
          const SizedBox(
            height: 48,
            child: Center(
              child: Text('Ağsız görsel kontrolü', style: AppTypography.label),
            ),
          ),
          Expanded(
            child: LayoutBuilder(
              builder: (context, constraints) {
                final dpr = MediaQuery.devicePixelRatioOf(context);
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (context.mounted) {
                    viewport(constraints.maxWidth, constraints.maxHeight, dpr);
                  }
                });
                return BlocBuilder<
                  LocalLosslessQaCubit,
                  ReadingImageComparisonState
                >(
                  builder: (context, state) {
                    final raster = state.raster;
                    if (raster == null) {
                      final text = state.error != null
                          ? _errorText(state.error!)
                          : state.status == 'stopped'
                          ? 'Kontrol bitti. Gözlemlerini sohbette paylaş.'
                          : state.status == 'loading'
                          ? 'Görsel açılıyor…'
                          : 'Başlat’a dokun.\nAltı görseli sırayla incele.\nBaşladıktan sonra süre 3 dakika.';
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Text(
                            text,
                            textAlign: TextAlign.center,
                            style: AppTypography.description,
                          ),
                        ),
                      );
                    }
                    return Stack(
                      children: [
                        Positioned(
                          left:
                              ((constraints.maxWidth * dpr - 822) / 2).round() /
                              dpr,
                          top:
                              ((constraints.maxHeight * dpr - 1334) / 2)
                                  .round() /
                              dpr,
                          width: 822 / dpr,
                          height: 1334 / dpr,
                          child: ReadingImageComparisonRaster(
                            key: const ValueKey('local-lossless-raster'),
                            raster: raster,
                            fit: BoxFit.contain,
                          ),
                        ),
                      ],
                    );
                  },
                );
              },
            ),
          ),
          SizedBox(
            height: 100,
            child: BlocBuilder<LocalLosslessQaCubit, ReadingImageComparisonState>(
              builder: (context, state) {
                final loaded = state.status == 'loaded';
                final initial = state.status == 'initial';
                final enabled = state.error == null && (initial || loaded);
                return Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      loaded
                          ? '${state.entryIndex + 1}/6 · ${_labels[state.entryIndex]}'
                          : 'Yerel dosyalar · 822 × 1334 · 1:1 piksel',
                      style: AppTypography.label,
                    ),
                    FilledButton(
                      key: const ValueKey('manual-image-next'),
                      onPressed: enabled ? advance : null,
                      child: Text(
                        initial
                            ? 'Başlat'
                            : loaded
                            ? state.entryIndex == 5
                                  ? 'Bitir'
                                  : 'Sonraki: ${_labels[state.entryIndex + 1]}'
                            : 'Kontrol durdu',
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    ),
  );

  static String _errorText(String error) => switch (error) {
    'viewport_refused' || 'viewport_changed' =>
      'Görsel bu alana 1:1 sığmıyor.\nEkranı dik tutup uygulamayı yeniden aç.',
    'qa_deadline' =>
      '3 dakikalık kontrol süresi doldu.\nUygulamayı yeniden açabilirsin.',
    'background' =>
      'Kontrol arka plana geçince durdu.\nUygulamayı yeniden açabilirsin.',
    _ => 'Görsel doğrulanamadı veya gösterilemedi.\nBu ekranı sohbette bildir.',
  };
}
