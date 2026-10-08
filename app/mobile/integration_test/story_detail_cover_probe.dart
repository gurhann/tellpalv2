import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tellpal_mobile_poc/core/artwork/artwork_cubit.dart';
import 'package:tellpal_mobile_poc/core/network/library_image_cache.dart';
import 'package:tellpal_mobile_poc/core/preview/presentation/widgets/preview_card.dart';

/// Test-only observations whitelist scalar identity, layout and decoded state.
/// No source URL, cache key, token, exception message or response body is read.
class StoryDetailCoverProbe {
  StoryDetailCoverProbe(this.tester, this.scope);
  final WidgetTester tester;
  final Finder scope;

  Finder get cards =>
      find.descendant(of: scope, matching: find.byType(PreviewCard));

  Finder get mountedCards => find.descendant(
    of: scope,
    matching: find.byType(PreviewCard, skipOffstage: false),
    skipOffstage: false,
  );

  Finder target(PreviewCard card, {bool includeCached = false}) =>
      find.descendant(
        of: scope,
        skipOffstage: !includeCached,
        matching: find.byWidgetPredicate(
          (widget) =>
              widget is PreviewCard &&
              widget.item.contentId == card.item.contentId &&
              widget.item.contentLanguage == card.item.contentLanguage &&
              widget.item.covers.phone?.assetId ==
                  card.item.covers.phone?.assetId &&
              widget.item.covers.tablet?.assetId ==
                  card.item.covers.tablet?.assetId,
          skipOffstage: !includeCached,
        ),
      );

  Finder decoded(PreviewCard card) => find.descendant(
    of: target(card),
    matching: find.byWidgetPredicate(
      (widget) => widget is RawImage && widget.image != null,
    ),
  );

  ArtworkState? artworkState(PreviewCard card) {
    final states = find.descendant(
      of: target(card),
      matching: find.byWidgetPredicate(
        (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
      ),
    );
    return states.evaluate().isEmpty
        ? null
        : states.evaluate().first.read<ArtworkCubit>().state;
  }

  Map<String, Object?> snapshot(PreviewCard? waitingFor) => {
    'globalCardCount': find.byType(PreviewCard).evaluate().length,
    'scopeCount': scope.evaluate().length,
    'scopedCardCount': cards.evaluate().length,
    'mountedCardCount': mountedCards.evaluate().length,
    'waitingContentId': waitingFor?.item.contentId,
    'waitingContentLanguage': waitingFor?.item.contentLanguage,
    'waitingPhoneAssetId': waitingFor?.item.covers.phone?.assetId,
    'waitingTabletAssetId': waitingFor?.item.covers.tablet?.assetId,
    if (waitingFor != null)
      'waitingTargetCount': target(waitingFor).evaluate().length,
    'cards': tester
        .widgetList<PreviewCard>(mountedCards)
        .take(10)
        .map(_card)
        .toList(),
  };

  Map<String, Object?> _card(PreviewCard card) {
    final selected = target(card, includeCached: true);
    final states = find.descendant(
      of: selected,
      skipOffstage: false,
      matching: find.byWidgetPredicate(
        (widget) => widget is BlocBuilder<ArtworkCubit, ArtworkState>,
        skipOffstage: false,
      ),
    );
    final artwork = states.evaluate().isEmpty
        ? null
        : states.evaluate().first.read<ArtworkCubit>().state;
    final images = find.descendant(
      of: selected,
      skipOffstage: false,
      matching: find.byType(RawImage, skipOffstage: false),
    );
    final decodedImages = tester
        .widgetList<RawImage>(images)
        .where((image) => image.image != null);
    final render = tester.renderObject<RenderBox>(selected.first);
    final bounds = render.localToGlobal(Offset.zero) & render.size;
    var clip =
        Offset.zero & (tester.view.physicalSize / tester.view.devicePixelRatio);
    var parent = render.parent;
    while (parent != null) {
      if (parent is RenderViewportBase) {
        clip = clip.intersect(parent.localToGlobal(Offset.zero) & parent.size);
      }
      parent = parent.parent;
    }
    final intersection = bounds.intersect(clip);
    final tablet =
        MediaQuery.sizeOf(tester.element(selected.first)).shortestSide >= 600;
    final cover = tablet
        ? (card.item.covers.tablet ?? card.item.covers.phone)
        : card.item.covers.phone;
    return {
      'cacheIdentitySha256': LibraryImageCache.failureIdentitySha256(
        '${card.artworkContext.accessIdentity}:${card.artworkContext.language}:${card.item.contentId}:${cover?.assetId}',
      ),
      'contentId': card.item.contentId,
      'contentLanguage': card.item.contentLanguage,
      'phoneAssetId': card.item.covers.phone?.assetId,
      'tabletAssetId': card.item.covers.tablet?.assetId,
      'contextLanguage': artwork?.identity?.context.language,
      'catalogAttempt': artwork?.identity?.catalogAttempt,
      'artworkAttempt': artwork?.attempt,
      'artworkStatus': artwork?.status.name,
      'networkImageCount': find
          .descendant(
            of: selected,
            matching: find.byType(CachedNetworkImage, skipOffstage: false),
            skipOffstage: false,
          )
          .evaluate()
          .length,
      'rawImageCount': images.evaluate().length,
      'decodedImageCount': decodedImages.length,
      'decodedImages': decodedImages
          .take(2)
          .map(
            (raw) => {'width': raw.image!.width, 'height': raw.image!.height},
          )
          .toList(),
      'bounds': [bounds.left, bounds.top, bounds.width, bounds.height],
      'intersectsViewports': intersection.width > 0 && intersection.height > 0,
    };
  }
}
