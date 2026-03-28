import { RoutePlaceholder } from "@/components/layout/route-placeholder"

export function FreeAccessRoute() {
  return (
    <RoutePlaceholder
      eyebrow="Free Access"
      title="Access Key Grants"
      description="This route will host grant listing, key filtering, and revoke workflows."
      highlights={[
        "Default key visibility",
        "Grant and revoke actions",
        "Content-linked access entries",
      ]}
    />
  )
}
