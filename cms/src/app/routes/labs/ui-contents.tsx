import { UiLabPage } from "@/components/workspace/ui-lab-page";

export function UiContentsLabRoute() {
  return (
    <UiLabPage
      topic="Contents Registry Explorations"
      description="Three coded directions for the content registry. This pass treats each canonical content type as its own table, with compact shared controls and verified domain fields. Duration is domain-confirmed but still needs to be projected by the registry API."
      recommendation="Current working direction: canonical type-led tables with compact language and readiness controls. Review the mockup before promoting it to the production Contents route."
      winnerHref="/labs/mockups/contents"
      winnerLabel="Open current mockup"
      variants={[
        {
          id: "A",
          name: "Type-led Registry",
          summary:
            "Content-type tabs that switch between dedicated tables above a compact registry toolbar.",
          highlights: [
            "Each type owns the columns editors need, instead of sharing one generic table.",
            "Language and readiness stay as compact, labeled secondary controls.",
            "Best current fit for the Contents-only exploration; detail remains deferred.",
          ],
          winner: true,
        },
        {
          id: "B",
          name: "Guided Registry",
          summary:
            "A more guided registry flow with progressive filters and stronger onboarding cues.",
          highlights: [
            "Strong onboarding and guardrails for infrequent operators.",
            "Best when publication errors come from missed prerequisites.",
            "Adds more navigation and step switching than the winning shell.",
          ],
        },
        {
          id: "C",
          name: "Operations Hybrid",
          summary:
            "Table-first workspace with heavier drawers and denser status treatment.",
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
