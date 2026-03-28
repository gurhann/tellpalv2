import { RoutePlaceholder } from "@/components/layout/route-placeholder";

export function ContentsIndexRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Contents"
      title="Content Studio"
      description="This route will host content list, filters, and create/edit entry points."
      highlights={[
        "List and filter content records",
        "Create and update content metadata",
        "Navigate to localized detail workspaces",
      ]}
    />
  );
}
