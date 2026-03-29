const languageLabels = {
  tr: "Turkish",
  en: "English",
  es: "Spanish",
  pt: "Portuguese",
  de: "German",
} as const;

export function normalizeLanguageCode(code: string) {
  return code.trim().toLowerCase();
}

export function resolveLanguageLabel(code: string) {
  const normalized = normalizeLanguageCode(code);

  if (!normalized) {
    return "";
  }

  return (
    languageLabels[normalized as keyof typeof languageLabels] ??
    normalized.toUpperCase()
  );
}

export function mapLanguage(code: string) {
  const normalizedCode = normalizeLanguageCode(code);

  return {
    code: normalizedCode,
    label: resolveLanguageLabel(normalizedCode),
  };
}
