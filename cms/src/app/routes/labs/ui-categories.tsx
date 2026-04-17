import { UiLabPage } from "@/components/workspace/ui-lab-page";

export function UiCategoriesLabRoute() {
  return (
    <UiLabPage
      topic="Category Studio Prototypes"
      description="Alternative coded layouts for category metadata, localization tabs, and curation management."
      recommendation="Variant A still wins, but the final category rollout borrows Variant B's staged language guidance to make curation prerequisites easier to read."
      winnerHref="/labs/mockups/categories"
      winnerLabel="Open Variant A mockup"
      variants={[
        {
          id: "A",
          name: "Variant A - Editorial Workspace",
          summary:
            "Metadata and localization stay in the main lane; curation context and rules move into a sticky rail.",
          highlights: [
            "Strong balance between category editing and curation oversight.",
            "Published-locale prerequisites are visible without crowding the form.",
            "Matches the content detail shell for faster cross-module learning.",
          ],
          winner: true,
        },
        {
          id: "B",
          name: "Variant B - Guided Studio",
          summary:
            "Turns localization and curation into a staged handoff with explicit prerequisites.",
          highlights: [
            "Excellent for explaining why curation is blocked.",
            "Adds an extra step transition editors must manage.",
            "Useful as a pattern for create-localization flows.",
          ],
        },
        {
          id: "C",
          name: "Variant C - Operations Hybrid",
          summary:
            "Keeps a dense table mindset with curation controls closer to the registry.",
          highlights: [
            "Quick for curation-heavy operators who mostly reorder content.",
            "Metadata editing becomes secondary and less discoverable.",
            "Better suited to future operations tooling than category authoring.",
          ],
        },
      ]}
      scoreRows={[
        { criterion: "Task clarity", variantA: 5, variantB: 5, variantC: 3 },
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
          variantC: 4,
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
          variantC: 2,
        },
        {
          criterion: "Screen consistency",
          variantA: 5,
          variantB: 4,
          variantC: 3,
        },
      ]}
    />
  );
}
