import { useParams } from "react-router-dom";

import { RoutePlaceholder } from "@/components/layout/route-placeholder";

export function ContentDetailRoute() {
  const { contentId = "unknown" } = useParams();

  return (
    <RoutePlaceholder
      eyebrow="Contents"
      title={`Content #${contentId}`}
      description="This route will host metadata editing, localization tabs, publication actions, and processing visibility."
      highlights={[
        "Base metadata editor",
        "Localization workspace per language",
        "Publish and archive actions",
      ]}
    />
  );
}
