import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  RegistryToolbar,
  RegistryToolbarGroup,
} from "@/components/data/registry-toolbar";

describe("RegistryToolbar", () => {
  it("renders named slots with summary after filters", () => {
    render(
      <RegistryToolbar
        ariaLabel="Content registry filters"
        search={<input aria-label="Search content" />}
        filters={
          <>
            <RegistryToolbarGroup label="Content type">
              <button type="button">All types</button>
            </RegistryToolbarGroup>
            <RegistryToolbarGroup label="State">
              <button type="button">All states</button>
            </RegistryToolbarGroup>
          </>
        }
        summaryTitle="All types | All states | 3 / 3 records"
        summaryDescription="Search, type, and state filters narrow the registry immediately."
      />,
    );

    const region = screen.getByRole("region", {
      name: /content registry filters/i,
    });
    const searchSlot = region.querySelector('[data-slot="registry-toolbar-search"]');
    const filtersSlot = region.querySelector(
      '[data-slot="registry-toolbar-filters"]',
    );
    const summarySlot = region.querySelector(
      '[data-slot="registry-toolbar-summary"]',
    );

    expect(screen.getByText("Content type")).toBeInTheDocument();
    expect(screen.getByText("State")).toBeInTheDocument();
    expect(
      screen.getByText(/all types \| all states \| 3 \/ 3 records/i),
    ).toBeInTheDocument();
    expect(searchSlot).not.toBeNull();
    expect(filtersSlot).not.toBeNull();
    expect(summarySlot).not.toBeNull();
    expect(
      filtersSlot?.compareDocumentPosition(summarySlot as Node),
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });
});
