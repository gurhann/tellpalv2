import { useParams } from "react-router-dom"

import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function StoryPagesRoute() {
  const { contentId = "unknown" } = useParams()

  return (
    <RoutePlaceholder
      eyebrow="Story Pages"
      title={`Story pages for content #${contentId}`}
      description="This route will host page ordering, media references, and localized page payload editing."
      highlights={[
        "Page structure management",
        "Illustration and audio references",
        "Per-page localization editing",
      ]}
    />
  )
}
