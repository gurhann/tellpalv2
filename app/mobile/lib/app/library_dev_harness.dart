import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'package:flutter/material.dart';
import '../features/library/presentation/cubit/library_cubit.dart';
import '../core/diagnostics/library_diagnostics.dart';
import 'package:flutter/foundation.dart';
import 'library_configuration.dart';
import 'router.dart';
import 'injection.dart';
import '../features/story_detail/presentation/cubit/story_detail_cubit.dart';
import '../features/library/domain/entity/library_catalog.dart';

/// Debugger/integration-test injection only. Tokens never enter UI, a file or compile-time defines.
class LibraryDevHarness {
  static final _configuration = Completer<LibraryConfiguration>();
  static void install(LibraryConfiguration configuration) {
    if (kReleaseMode) throw StateError('Development entry is unavailable');
    if (!_configuration.isCompleted) _configuration.complete(configuration);
  }

  static final scaleNotifier = ValueNotifier<double?>(null);
  static void bind(LibraryCubit cubit, LibraryDiagnostics diagnostics) {
    if (kReleaseMode) return;
    bindDetail(null);
    registerExtension('ext.tellpal.library.observe', (_, parameters) async {
      final state = cubit.state;
      return ServiceExtensionResponse.result(
        jsonEncode({
          'status': state.status.name,
          'language': state.context.language,
          'attempt': state.attempt,
          'audience': state.catalog?.audience,
          'rows': state.catalog?.rows
              .map(
                (r) => {
                  'categoryId': r.categoryId,
                  'contentLanguage': r.contentLanguage,
                  'items': r.items
                      .map(
                        (i) => {
                          'contentId': i.contentId,
                          'contentLanguage': i.contentLanguage,
                          'isFree': i.isFree,
                          'isLocked': i.isLocked,
                          'phoneAssetId': i.covers.phone?.assetId,
                          'tabletAssetId': i.covers.tablet?.assetId,
                        },
                      )
                      .toList(),
                },
              )
              .toList(),
          'diagnostics': diagnostics.events,
          'imageCacheBytes':
              PaintingBinding.instance.imageCache.currentSizeBytes,
          'imageCacheEntries': PaintingBinding.instance.imageCache.currentSize,
        }),
      );
    });
    registerExtension('ext.tellpal.library.context', (_, parameters) async {
      final language = parameters['language'];
      final scale = double.tryParse(parameters['textScale'] ?? '1');
      if (!['tr', 'en', 'pt'].contains(language) ||
          ![1.0, 1.3].contains(scale)) {
        return ServiceExtensionResponse.result(jsonEncode({'changed': false}));
      }
      scaleNotifier.value = scale!;
      await cubit.load(
        context: LibraryContext(language!, cubit.state.context.accessIdentity),
      );
      return ServiceExtensionResponse.result(jsonEncode({'changed': true}));
    });
  }

  static final _detailRoutes = <StoryDetailCubit>[];
  static bool _detailExtensionInstalled = false;
  static final _detailNavigationPending = <String>{};
  static void bindDetail(StoryDetailCubit? cubit) {
    if (kReleaseMode) return;
    if (cubit != null) _detailRoutes.add(cubit);
    if (_detailExtensionInstalled) return;
    _detailExtensionInstalled = true;
    registerExtension('ext.tellpal.library.detail', (_, parameters) async {
      final active = _detailRoutes
          .where((route) => !route.isClosed && route.state.visible)
          .lastOrNull;
      final state = active?.state;
      return ServiceExtensionResponse.result(
        jsonEncode({
          'status': state?.status.name,
          'heroStatus': state?.heroStatus.name,
          'attempt': state?.attempt,
          'narration': state?.narration,
          'categoryId': state?.identity?.categoryId,
          'contentId': state?.identity?.contentId,
          'uiLanguage': state?.identity?.uiLanguage,
          'contentLanguage': state?.identity?.contentLanguage,
          'title': state?.detail?.title,
          'audience': state?.detail?.audience,
          'isFree': state?.detail?.isFree,
          'isLocked': state?.detail?.isLocked,
          'heroAssetId': state?.detail?.hero?.assetId,
          'recommendations': state?.detail?.recommendations
              .map(
                (item) => {
                  'contentId': item.contentId,
                  'contentLanguage': item.contentLanguage,
                  'isLocked': item.isLocked,
                },
              )
              .toList(),
          'imageCacheBytes':
              PaintingBinding.instance.imageCache.currentSizeBytes,
          'imageCacheEntries': PaintingBinding.instance.imageCache.currentSize,
        }),
      );
    });
    registerExtension('ext.tellpal.library.detailAction', (
      _,
      parameters,
    ) async {
      final active = _detailRoutes
          .where((route) => !route.isClosed && route.state.visible)
          .lastOrNull;
      switch (parameters['action']) {
        case 'open':
          final category = int.tryParse(parameters['categoryId'] ?? ''),
              id = int.tryParse(parameters['contentId'] ?? '');
          final actionKey =
              '${active?.state.identity?.contentId}:$category:$id:${parameters['uiLanguage']}:${parameters['contentLanguage']}';
          if (_detailNavigationPending.contains(actionKey) ||
              category == null ||
              category <= 0 ||
              id == null ||
              id <= 0 ||
              !['tr', 'en', 'pt'].contains(parameters['uiLanguage']) ||
              !['tr', 'en', 'pt'].contains(parameters['contentLanguage'])) {
            return ServiceExtensionResponse.result(
              jsonEncode({'accepted': false}),
            );
          }
          _detailNavigationPending.add(actionKey);
          unawaited(
            libraryGetIt<LibraryRouter>()
                .push(
                  StoryDetailRoute(
                    categoryId: category,
                    contentId: id,
                    uiLanguage: parameters['uiLanguage']!,
                    contentLanguage: parameters['contentLanguage']!,
                  ),
                )
                .whenComplete(() => _detailNavigationPending.remove(actionKey)),
          );
        case 'back':
          await libraryGetIt<LibraryRouter>().maybePop();
        case 'retry':
          if (active?.state.identity != null) {
            unawaited(
              active!.load(
                active.state.identity!,
                artworkOnly: active.state.detail != null,
              ),
            );
          }
        case 'narration':
          active?.toggleNarration(parameters['value'] == 'true');
        default:
          return ServiceExtensionResponse.result(
            jsonEncode({'accepted': false}),
          );
      }
      return ServiceExtensionResponse.result(jsonEncode({'accepted': true}));
    });
  }

  static void unbindDetail(StoryDetailCubit cubit) =>
      _detailRoutes.remove(cubit);

  static Future<LibraryConfiguration> waitForConfiguration() {
    if (kReleaseMode) throw StateError('Development entry is unavailable');
    registerExtension('ext.tellpal.library.configure', (_, parameters) async {
      try {
        install(
          LibraryConfiguration(
            baseUrl: Uri.parse(parameters['baseUrl'] ?? ''),
            idToken: parameters['idToken'] ?? '',
            context: LibraryContext(
              parameters['language'] ?? 'en',
              parameters['accessIdentity'] ?? '',
            ),
          ),
        );
        return ServiceExtensionResponse.result(
          jsonEncode({'configured': true}),
        );
      } catch (_) {
        return ServiceExtensionResponse.result(
          jsonEncode({'configured': false}),
        );
      }
    });
    return _configuration.future;
  }
}
