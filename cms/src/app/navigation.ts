import type { LucideIcon } from "lucide-react";
import {
  BookOpenText,
  FolderKanban,
  Image,
  ShieldCheck,
  Sparkles,
  Users,
  Workflow,
} from "lucide-react";

import type { TranslationKey } from "@/i18n/messages";

export type CmsNavigationItem = {
  labelKey: TranslationKey;
  descriptionKey: TranslationKey;
  path: string;
  icon: LucideIcon;
};

type RouteMeta = {
  titleKey: TranslationKey;
  eyebrowKey: TranslationKey;
  descriptionKey: TranslationKey;
};

export const cmsNavigationItems: CmsNavigationItem[] = [
  {
    labelKey: "nav.contents.label",
    descriptionKey: "nav.contents.description",
    path: "/contents",
    icon: BookOpenText,
  },
  {
    labelKey: "nav.categories.label",
    descriptionKey: "nav.categories.description",
    path: "/categories",
    icon: FolderKanban,
  },
  {
    labelKey: "nav.contributors.label",
    descriptionKey: "nav.contributors.description",
    path: "/contributors",
    icon: Users,
  },
  {
    labelKey: "nav.freeAccess.label",
    descriptionKey: "nav.freeAccess.description",
    path: "/free-access",
    icon: Sparkles,
  },
  {
    labelKey: "nav.media.label",
    descriptionKey: "nav.media.description",
    path: "/media",
    icon: Image,
  },
  {
    labelKey: "nav.mediaProcessing.label",
    descriptionKey: "nav.mediaProcessing.description",
    path: "/media-processing",
    icon: Workflow,
  },
];

export function getRouteMeta(pathname: string): RouteMeta {
  if (pathname.startsWith("/labs/mockups")) {
    return {
      titleKey: "route.mockupLabs.title",
      eyebrowKey: "layout.brand",
      descriptionKey: "route.mockupLabs.description",
    };
  }

  if (pathname.startsWith("/labs/ui/")) {
    return {
      titleKey: "route.uiLabs.title",
      eyebrowKey: "layout.brand",
      descriptionKey: "route.uiLabs.description",
    };
  }

  if (pathname.startsWith("/contents/") && pathname.endsWith("/story-pages")) {
    return {
      titleKey: "route.storyPages.title",
      eyebrowKey: "nav.contents.label",
      descriptionKey: "route.storyPages.description",
    };
  }

  if (pathname.startsWith("/contents/")) {
    return {
      titleKey: "route.contentsDetail.title",
      eyebrowKey: "nav.contents.label",
      descriptionKey: "route.contentsDetail.description",
    };
  }

  if (pathname.startsWith("/categories/")) {
    return {
      titleKey: "route.categoryDetail.title",
      eyebrowKey: "nav.categories.label",
      descriptionKey: "route.categoryDetail.description",
    };
  }

  const directMatch = cmsNavigationItems.find(
    (item) => pathname === item.path || pathname.startsWith(`${item.path}/`),
  );

  if (directMatch) {
    return {
      titleKey: directMatch.labelKey,
      eyebrowKey: "layout.brand",
      descriptionKey: directMatch.descriptionKey,
    };
  }

  return {
    titleKey: "route.workspace.title",
    eyebrowKey: "layout.brand",
    descriptionKey: "route.workspace.description",
  };
}

export const cmsShieldIcon = ShieldCheck;
