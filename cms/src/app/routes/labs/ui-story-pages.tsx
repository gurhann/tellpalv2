import { UiLabPage } from "@/components/workspace/ui-lab-page";

export function UiStoryPagesLabRoute() {
  return (
    <UiLabPage
      topic="Story Page Editor Prototypes"
      description="Three coded directions for story structure editing and per-language page payload management."
      recommendation="Variant A keeps the editor fast on laptop heights, while the production implementation also borrows Variant C's readiness rail density for page-level completeness."
      winnerHref="/labs/mockups/contents/demo-content/story-pages"
      winnerLabel="Open Variant A mockup"
      variants={[
        {
          id: "A",
          name: "Variant A - Editorial Workspace",
          summary:
            "Page table stays primary; the page editor opens as a large scroll-safe modal with locale tabs.",
          highlights: [
            "Best match for the current page editing mental model.",
            "Keeps create, edit, and delete on the same screen without nested scroll traps.",
            "Supports low-height laptop screens reliably.",
          ],
          winner: true,
        },
        {
          id: "B",
          name: "Variant B - Guided Studio",
          summary:
            "Walks editors through structure first, then locale completeness per page.",
          highlights: [
            "Improves completeness for teams with inconsistent story-page habits.",
            "Feels slower when the primary goal is quick copy or asset correction.",
            "Best as an optional training or onboarding mode.",
          ],
        },
        {
          id: "C",
          name: "Variant C - Operations Hybrid",
          summary:
            "More compact registry with heavier readiness summaries and drawer-based editing.",
          highlights: [
            "Very strong readiness signaling across many pages.",
            "Drawer editing can feel cramped once narrative copy grows.",
            "Useful as a pattern source for summary density, not as the main editor.",
          ],
        },
      ]}
      scoreRows={[
        { criterion: "Task clarity", variantA: 5, variantB: 4, variantC: 4 },
        {
          criterion: "Scroll simplicity",
          variantA: 5,
          variantB: 4,
          variantC: 3,
        },
        {
          criterion: "Modal usability",
          variantA: 5,
          variantB: 4,
          variantC: 3,
        },
        {
          criterion: "Time to first action",
          variantA: 5,
          variantB: 3,
          variantC: 4,
        },
        {
          criterion: "Perceived complexity",
          variantA: 4,
          variantB: 4,
          variantC: 3,
        },
        {
          criterion: "Screen consistency",
          variantA: 5,
          variantB: 4,
          variantC: 4,
        },
      ]}
    />
  );
}
