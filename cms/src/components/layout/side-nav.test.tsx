import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { localMockupNavigationItems } from "@/app/navigation";
import { SideNav } from "@/components/layout/side-nav";
import { LocaleProvider } from "@/i18n/locale-provider";

describe("SideNav", () => {
  it("shows the local mockup area and opens the mockup index in development", () => {
    render(
      <MemoryRouter>
        <LocaleProvider>
          <SideNav />
        </LocaleProvider>
      </MemoryRouter>,
    );

    expect(localMockupNavigationItems).toHaveLength(1);
    expect(screen.getByRole("link", { name: /mockups/i })).toHaveAttribute(
      "href",
      "/labs/mockups",
    );
  });
});
