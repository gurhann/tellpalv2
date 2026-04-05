import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import {
  enMessages,
  trMessages,
  type MessageValue,
  type TranslationKey,
  type TranslationParams,
} from "@/i18n/messages";

export type AppLocale = "en" | "tr";

type I18nContextValue = {
  locale: AppLocale;
  setLocale: (locale: AppLocale) => void;
  t: (key: TranslationKey, params?: TranslationParams) => string;
  formatDateTime: (
    value: string | number | Date,
    options?: Intl.DateTimeFormatOptions,
  ) => string;
  formatNumber: (value: number, options?: Intl.NumberFormatOptions) => string;
  formatBytes: (value: number) => string;
};

const LOCALE_STORAGE_KEY = "tellpal.cms.locale";

const localeMessages = {
  en: enMessages,
  tr: trMessages,
} as const;

function resolveSupportedLocale(input: string | null | undefined): AppLocale {
  if (!input) {
    return "en";
  }

  return input.trim().toLowerCase().startsWith("tr") ? "tr" : "en";
}

function interpolate(template: string, params: TranslationParams | undefined) {
  if (!params) {
    return template;
  }

  return template.replace(/\{(\w+)\}/g, (_, token: string) => {
    const value = params[token];
    return value === undefined || value === null ? "" : String(value);
  });
}

function createTranslator(locale: AppLocale) {
  const fallbackMessages = localeMessages.en;
  const messages = localeMessages[locale];

  return (key: TranslationKey, params?: TranslationParams) => {
    const value = (messages[key] ?? fallbackMessages[key]) as MessageValue;

    if (typeof value === "function") {
      return value(params);
    }

    return interpolate(value, params);
  };
}

function resolveInitialLocale(): AppLocale {
  if (typeof window === "undefined") {
    return "en";
  }

  const storedLocale = window.localStorage.getItem(LOCALE_STORAGE_KEY);
  if (storedLocale) {
    return resolveSupportedLocale(storedLocale);
  }

  return resolveSupportedLocale(window.navigator.language);
}

const defaultTranslator = createTranslator("en");

const I18nContext = createContext<I18nContextValue>({
  locale: "en",
  setLocale: () => undefined,
  t: defaultTranslator,
  formatDateTime: (value, options) =>
    new Intl.DateTimeFormat("en", options).format(new Date(value)),
  formatNumber: (value, options) =>
    new Intl.NumberFormat("en", options).format(value),
  formatBytes: (value) =>
    `${new Intl.NumberFormat("en").format(value)} ${defaultTranslator("app.bytesUnit")}`,
});

type LocaleProviderProps = {
  children: ReactNode;
};

export function LocaleProvider({ children }: LocaleProviderProps) {
  const [locale, setLocale] = useState<AppLocale>(resolveInitialLocale);

  useEffect(() => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo<I18nContextValue>(() => {
    const t = createTranslator(locale);

    return {
      locale,
      setLocale,
      t,
      formatDateTime: (input, options) =>
        new Intl.DateTimeFormat(locale, options).format(new Date(input)),
      formatNumber: (input, options) =>
        new Intl.NumberFormat(locale, options).format(input),
      formatBytes: (input) =>
        `${new Intl.NumberFormat(locale).format(input)} ${t("app.bytesUnit")}`,
    };
  }, [locale]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useI18n() {
  return useContext(I18nContext);
}

// eslint-disable-next-line react-refresh/only-export-components
export function getStoredLocaleKey() {
  return LOCALE_STORAGE_KEY;
}
