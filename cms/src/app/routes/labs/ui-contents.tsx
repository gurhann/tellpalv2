import { UiLabPage } from "@/components/workspace/ui-lab-page";

export function UiContentsLabRoute() {
  return (
    <UiLabPage
      topic="Content Studio Prototypes"
      description="Three coded directions for the content registry and detail workspace. These variants focus on editorial flow, localization control, and story-page handoff."
      recommendation="Variant A wins for production because it keeps the shell calm, makes the primary task obvious, and leaves the right rail available for readiness and story-page context."
      variants={[
        {
          id: "A",
          name: "Variant A - Editorial Workspace",
          summary:
            "Split layout with a quiet registry, single primary editor lane, and right-side readiness rail.",
          highlights: [
            "Primary work stays in one column; readiness and story actions stay in the rail.",
            "Create and detail flows share the same modal contract and action density.",
            "Best fit for daily editor work with low cognitive overhead.",
          ],
          winner: true,
        },
        {
          id: "B",
          name: "Variant B - Guided Studio",
          summary:
            "Step-driven flow: metadata, localization, assets, publish, then story pages.",
          highlights: [
            "Strong onboarding and guardrails for infrequent operators.",
            "Best when publication errors come from missed prerequisites.",
            "Adds more navigation and step switching than the winning shell.",
          ],
        },
        {
          id: "C",
          name: "Variant C - Operations Hybrid",
          summary:
            "Table-first workspace with heavier side drawers and denser status density.",
          highlights: [
            "Fast for bulk review and registry-heavy work.",
            "Useful pattern for asset and operations surfaces.",
            "Feels busier than needed for content authorship tasks.",
          ],
        },
      ]}
      scoreRows={[
        { criterion: "Task clarity", variantA: 5, variantB: 4, variantC: 3 },
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
          variantA: 5,
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
