import { useParams } from "react-router-dom";

import { RoutePlaceholder } from "@/components/layout/route-placeholder";

export function CategoryDetailRoute() {
  const { categoryId = "unknown" } = useParams();

  return (
    <RoutePlaceholder
      eyebrow="Categories"
      title={`Category #${categoryId}`}
      description="This route will host localization editing and curation management for a single category."
      highlights={[
        "Base metadata editor",
        "Localized category payloads",
        "Category curation workspace",
      ]}
    />
  );
}
