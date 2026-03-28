import type { LucideIcon } from "lucide-react"
import {
  BookOpenText,
  FolderKanban,
  Image,
  ShieldCheck,
  Sparkles,
  Users,
  Workflow,
} from "lucide-react"

export type CmsNavigationItem = {
  label: string
  description: string
  path: string
  icon: LucideIcon
}

export const cmsNavigationItems: CmsNavigationItem[] = [
  {
    label: "Contents",
    description: "Editorial records and localization flows.",
    path: "/contents",
    icon: BookOpenText,
  },
  {
    label: "Categories",
    description: "Category metadata and curation workspaces.",
    path: "/categories",
    icon: FolderKanban,
  },
  {
    label: "Media",
    description: "Asset registry and media metadata.",
    path: "/media",
    icon: Image,
  },
  {
    label: "Media Processing",
    description: "Packaging state and retry operations.",
    path: "/media-processing",
    icon: Workflow,
  },
  {
    label: "Contributors",
    description: "Credits, names, roles, and assignments.",
    path: "/contributors",
    icon: Users,
  },
  {
    label: "Free Access",
    description: "Access keys and grant visibility.",
    path: "/free-access",
    icon: Sparkles,
  },
]

export function getRouteMeta(pathname: string) {
  if (pathname.startsWith("/contents/") && pathname.endsWith("/story-pages")) {
    return {
      title: "Story Pages",
      eyebrow: "Contents",
      description: "Manage story page structure and localized page payloads.",
    }
  }

  if (pathname.startsWith("/contents/")) {
    return {
      title: "Content Detail",
      eyebrow: "Contents",
      description: "Edit core metadata, localizations, and publication actions.",
    }
  }

  if (pathname.startsWith("/categories/")) {
    return {
      title: "Category Detail",
      eyebrow: "Categories",
      description: "Manage category metadata, localizations, and curation.",
    }
  }

  const directMatch = cmsNavigationItems.find((item) =>
    pathname === item.path || pathname.startsWith(`${item.path}/`)
  )

  if (directMatch) {
    return {
      title: directMatch.label,
      eyebrow: "TellPal CMS",
      description: directMatch.description,
    }
  }

  return {
    title: "Workspace",
    eyebrow: "TellPal CMS",
    description: "Admin route skeleton for the CMS workspace.",
  }
}

export const cmsShieldIcon = ShieldCheck
