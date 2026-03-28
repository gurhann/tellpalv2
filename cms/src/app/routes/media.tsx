import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function MediaRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Media"
      title="Asset Library"
      description="This route will host recent assets, metadata updates, and reusable asset selection flows."
      highlights={[
        "Recent asset list",
        "Asset detail sheet",
        "Metadata editing and URL refresh actions",
      ]}
    />
  )
}
