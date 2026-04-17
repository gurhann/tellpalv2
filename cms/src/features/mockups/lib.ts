import type { AppLocale } from "@/i18n/locale-provider";
import type { MockupLocaleState } from "@/features/mockups/types";

export function getMockupLanguageLabel(
  languageCode: string,
  appLocale: AppLocale,
) {
  switch (languageCode) {
    case "tr":
      return appLocale === "tr" ? "Turkce" : "Turkish";
    case "en":
    default:
      return appLocale === "tr" ? "Ingilizce" : "English";
  }
}

export function getReadinessTone(localeState: MockupLocaleState) {
  if (localeState.isPublished && localeState.isProcessingComplete !== false) {
    return "success" as const;
  }

  if (
    localeState.hasAudio === false ||
    localeState.hasIllustration === false ||
    localeState.hasBodyText === false
  ) {
    return "warning" as const;
  }

  return "info" as const;
}

export function countVisibleLocales(locales: MockupLocaleState[]) {
  return locales.filter((localeState) => localeState.isVisibleToMobile).length;
}

export function countProcessingComplete(locales: MockupLocaleState[]) {
  return locales.filter((localeState) => localeState.isProcessingComplete).length;
}

export function countIllustrationReady(locales: MockupLocaleState[]) {
  return locales.filter((localeState) => localeState.hasIllustration).length;
}
