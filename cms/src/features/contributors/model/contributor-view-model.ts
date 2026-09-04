import type {
  AdminContentContributorResponse,
  AdminContributorResponse,
  ContributorRole,
} from "@/features/contributors/api/contributor-admin";
import { mapLanguage } from "@/lib/languages";

export const GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL = "All languages";

const contributorRoleLabels: Record<ContributorRole, string> = {
  AUTHOR: "Author",
  ILLUSTRATOR: "Illustrator",
  NARRATOR: "Narrator",
  MUSICIAN: "Musician",
};

export function getContributorRoleLabel(
  role: ContributorRole,
  locale: "en" | "tr",
) {
  const labels = {
    en: {
      AUTHOR: "Author",
      ILLUSTRATOR: "Illustrator",
      NARRATOR: "Narrator",
      MUSICIAN: "Musician",
    },
    tr: {
      AUTHOR: "Yazar",
      ILLUSTRATOR: "İllüstratör",
      NARRATOR: "Anlatıcı",
      MUSICIAN: "Müzisyen",
    },
  } as const;
  return labels[locale][role];
}

function buildInitials(displayName: string) {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);

  if (parts.length === 0) {
    return "";
  }

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export type ContributorViewModel = {
  id: number;
  displayName: string;
  initials: string;
  roles?: ContributorRole[];
  totalUsageCount?: number;
  usageByRole?: Partial<Record<ContributorRole, number>>;
  updatedAt?: string;
};

export type ContentContributorViewModel = {
  contentId: number;
  contributorId: number;
  displayName: string;
  initials: string;
  role: ContributorRole;
  roleLabel: string;
  languageCode: string | null;
  languageLabel: string;
  creditName: string | null;
  effectiveCreditName: string;
  sortOrder: number;
};

function mapContributorLanguageScope(languageCode: string | null) {
  if (languageCode === null) {
    return {
      code: null,
      label: GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL,
    };
  }

  const language = mapLanguage(languageCode);

  return {
    code: language.code,
    label: language.label,
  };
}

export function mapAdminContributor(
  contributor: AdminContributorResponse,
): ContributorViewModel {
  return {
    id: contributor.contributorId,
    displayName: contributor.displayName,
    initials: buildInitials(contributor.displayName),
    ...(contributor.roles?.length ? { roles: contributor.roles } : {}),
  };
}

export function mapAdminContributorRegistryItem(
  item: import("@/features/contributors/api/contributor-admin").AdminContributorRegistryPage["items"][number],
): ContributorViewModel {
  return {
    id: item.contributorId,
    displayName: item.displayName,
    initials: buildInitials(item.displayName),
    roles: item.roles,
    totalUsageCount: item.totalUsageCount,
    usageByRole: item.usageByRole,
    updatedAt: item.updatedAt,
  };
}

export function mapAdminContributorList(
  contributors: AdminContributorResponse[],
): ContributorViewModel[] {
  return contributors.map(mapAdminContributor);
}

export function mapAdminContentContributor(
  contributor: AdminContentContributorResponse,
): ContentContributorViewModel {
  const language = mapContributorLanguageScope(contributor.languageCode);

  return {
    contentId: contributor.contentId,
    contributorId: contributor.contributorId,
    displayName: contributor.contributorDisplayName,
    initials: buildInitials(contributor.contributorDisplayName),
    role: contributor.role,
    roleLabel: contributorRoleLabels[contributor.role],
    languageCode: language.code,
    languageLabel: language.label,
    creditName: contributor.creditName,
    effectiveCreditName:
      contributor.creditName?.trim() || contributor.contributorDisplayName,
    sortOrder: contributor.sortOrder,
  };
}
