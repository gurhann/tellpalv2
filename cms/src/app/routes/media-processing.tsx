import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function MediaProcessingRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Media Processing"
      title="Processing Console"
      description="This route will host processing job lookup, status views, and schedule/retry controls."
      highlights={[
        "Recent job history",
        "Localization status lookup",
        "Schedule and retry workflows",
      ]}
    />
  )
}
