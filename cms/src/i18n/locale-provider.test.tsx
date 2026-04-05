import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import {
  LocaleProvider,
  getStoredLocaleKey,
  useI18n,
} from "@/i18n/locale-provider";

function LocaleProbe() {
  const { locale, setLocale, t, formatBytes } = useI18n();

  return (
    <div>
      <p>{locale}</p>
      <p>{t("app.language")}</p>
      <p>{formatBytes(1200)}</p>
      <button type="button" onClick={() => setLocale("tr")}>
        to-tr
      </button>
    </div>
  );
}

describe("LocaleProvider", () => {
  beforeEach(() => {
    window.localStorage.clear();
    Object.defineProperty(window.navigator, "language", {
      configurable: true,
      value: "en-US",
    });
    document.documentElement.lang = "en";
  });

  it("uses localStorage preference before navigator language", () => {
    window.localStorage.setItem(getStoredLocaleKey(), "tr");
    Object.defineProperty(window.navigator, "language", {
      configurable: true,
      value: "en-US",
    });

    render(
      <LocaleProvider>
        <LocaleProbe />
      </LocaleProvider>,
    );

    expect(screen.getByText("tr")).toBeInTheDocument();
    expect(screen.getByText("Dil")).toBeInTheDocument();
    expect(document.documentElement.lang).toBe("tr");
  });

  it("falls back to navigator language and persists locale changes", () => {
    Object.defineProperty(window.navigator, "language", {
      configurable: true,
      value: "tr-TR",
    });

    render(
      <LocaleProvider>
        <LocaleProbe />
      </LocaleProvider>,
    );

    expect(screen.getByText("tr")).toBeInTheDocument();
    expect(screen.getByText("1.200 bayt")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "to-tr" }));

    expect(window.localStorage.getItem(getStoredLocaleKey())).toBe("tr");
    expect(document.documentElement.lang).toBe("tr");
  });
});
