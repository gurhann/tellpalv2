import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function ContributorsRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Contributors"
      title="Contributor Management"
      description="This route will host contributor listing, create/rename flows, and content assignment tools."
      highlights={[
        "Contributor list view",
        "Create and rename actions",
        "Content assignment workflows",
      ]}
    />
  )
}
