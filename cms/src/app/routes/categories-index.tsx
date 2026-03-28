import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function CategoriesIndexRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Categories"
      title="Category Studio"
      description="This route will host category listing, filters, and create/edit entry points."
      highlights={[
        "List category records",
        "Edit premium and active states",
        "Open category localization and curation detail views",
      ]}
    />
  )
}
