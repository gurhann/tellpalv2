import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'package:flutter/material.dart';
import '../features/library/presentation/cubit/library_cubit.dart';
import '../core/diagnostics/library_diagnostics.dart';
import 'package:flutter/foundation.dart';
import 'library_configuration.dart';
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
