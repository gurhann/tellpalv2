import { render, screen } from "@testing-library/react";

import { assetViewModels } from "@/features/assets/test/fixtures";

import { AssetTable } from "./asset-table";

describe("AssetTable", () => {
  it("renders required recent asset columns", () => {
    render(<AssetTable assets={assetViewModels} />);

    expect(screen.getByRole("columnheader", { name: /asset/i })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /kind/i })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /provider/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /media type/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /mime type/i }),
    ).toBeVisible();
    expect(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    ).toBeVisible();
    expect(screen.getByText("Phone Thumbnail")).toBeVisible();
    expect(screen.getByText("Firebase Storage")).toBeVisible();
  });

  it("renders an empty state when there are no assets", () => {
    render(<AssetTable assets={[]} />);

    expect(
      screen.getByRole("heading", { name: /no media assets yet/i }),
    ).toBeVisible();
  });
});
