import type { AppLocale } from "@/i18n/locale-provider";

const languageLabels = {
  tr: {
    en: "Turkish",
    tr: "Türkçe",
  },
  en: {
    en: "English",
    tr: "İngilizce",
  },
  es: {
    en: "Spanish",
    tr: "İspanyolca",
  },
  pt: {
    en: "Portuguese",
    tr: "Portekizce",
  },
  de: {
    en: "German",
    tr: "Almanca",
  },
} as const;

export function normalizeLanguageCode(code: string) {
  return code.trim().toLowerCase();
}

export function getSupportedCmsLanguageOptions(locale: AppLocale = "en") {
  return Object.entries(languageLabels).map(([code, labels]) => ({
    code,
    label: labels[locale],
  }));
}

export const supportedCmsLanguageOptions = getSupportedCmsLanguageOptions();

export function resolveLanguageLabel(code: string, locale: AppLocale = "en") {
  const normalized = normalizeLanguageCode(code);

  if (!normalized) {
    return "";
  }

  return (
    languageLabels[normalized as keyof typeof languageLabels]?.[locale] ??
    normalized.toUpperCase()
  );
}

export function mapLanguage(code: string, locale: AppLocale = "en") {
  const normalizedCode = normalizeLanguageCode(code);

  return {
    code: normalizedCode,
    label: resolveLanguageLabel(normalizedCode, locale),
  };
}
