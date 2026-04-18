import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TaskRail } from "@/components/workspace/task-rail";

describe("TaskRail", () => {
  it("keeps detail rails compact and operational", () => {
    render(
      <TaskRail
        title="Readiness rail"
        description="Operational checks before publication."
        variant="detail"
        stats={[
          { label: "Visibility", value: "2 / 3", tone: "success" },
          { label: "Processing", value: "3 / 3", tone: "success" },
          { label: "Story pages", value: "2 pages", tone: "default" },
          { label: "Overflow", value: "Should not render", tone: "warning" },
        ]}
      >
        <div>Nested child content</div>
      </TaskRail>,
    );

    expect(
      screen.getByRole("heading", { name: /readiness rail/i }),
    ).toBeVisible();
    expect(screen.getByText("Visibility")).toBeVisible();
    expect(screen.getByText("Processing")).toBeVisible();
    expect(screen.getByText("Story pages")).toBeVisible();
    expect(screen.queryByText("Overflow")).not.toBeInTheDocument();
    expect(screen.queryByText("Nested child content")).not.toBeInTheDocument();
  });
});
