import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
  WorkspaceMetricCard,
  WorkspaceStatusPill,
} from "@/components/workspace/workspace-primitives";

describe("workspace primitives", () => {
  it("renders status pill, metric card, info card, and key value grid content", () => {
    render(
      <div>
        <WorkspaceStatusPill tone="success">Ready</WorkspaceStatusPill>
        <WorkspaceMetricCard
          label="Selected locale"
          value="English"
          detail="Published and visible"
          tone="accent"
        />
        <WorkspaceInfoCard
          title="Readiness rail"
          description="Keeps operational posture compact."
        >
          <WorkspaceKeyValueGrid
            items={[
              { label: "Visibility", value: "2 / 3", tone: "success" },
              { label: "Processing", value: "1 / 3", tone: "warning" },
            ]}
          />
        </WorkspaceInfoCard>
      </div>,
    );

    expect(screen.getByText("Ready")).toBeVisible();
    expect(screen.getByText("Selected locale")).toBeVisible();
    expect(screen.getByText("English")).toBeVisible();
    expect(screen.getByRole("heading", { name: /readiness rail/i })).toBeVisible();
    expect(screen.getByText("Visibility")).toBeVisible();
    expect(screen.getByText("2 / 3")).toBeVisible();
    expect(screen.getByText("Processing")).toBeVisible();
    expect(screen.getByText("1 / 3")).toBeVisible();
  });
});
