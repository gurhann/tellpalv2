export type MockupLocaleState = {
  languageCode: string;
  title: string;
  description: string;
  statusLabel: string;
  isPublished: boolean;
  isVisibleToMobile?: boolean;
  isProcessingComplete?: boolean;
  hasBodyText?: boolean;
  hasCover?: boolean;
  hasAudio?: boolean;
  hasIllustration?: boolean;
  note: string;
};

export type MockupContributorAssignment = {
  id: string;
  role: string;
  name: string;
  languageCode: string;
  creditName: string;
  order: number;
  note: string;
};

export type MockupContentSummary = {
  id: string;
  externalKey: string;
  typeLabel: string;
  active: boolean;
  ageRange: number;
  pageCount: number | null;
  locales: MockupLocaleState[];
  summaryLabel: string;
  note: string;
  isDemo: boolean;
};

export type MockupContent = MockupContentSummary & {
  readinessSummary: string;
  workingAgreements: string[];
  contributorAssignments: MockupContributorAssignment[];
  primaryAction: string;
};

export type MockupCategoryCurationItem = {
  id: string;
  contentTitle: string;
  contentType: string;
  languageCode: string;
  displayOrder: number;
  statusLabel: string;
  reason: string;
};

export type MockupCategorySummary = {
  id: string;
  slug: string;
  typeLabel: string;
  premium: boolean;
  active: boolean;
  locales: MockupLocaleState[];
  note: string;
  isDemo: boolean;
};

export type MockupCategory = MockupCategorySummary & {
  curationItems: MockupCategoryCurationItem[];
  guardrails: string[];
};

export type MockupStoryPage = {
  id: string;
  pageNumber: number;
  summary: string;
  note: string;
  localizations: MockupLocaleState[];
};
