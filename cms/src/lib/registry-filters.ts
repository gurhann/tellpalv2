const preferredTypeOrder = [
  "Story",
  "Audio Story",
  "Meditation",
  "Lullaby",
];

type RegistryFilterSummaryOptions = {
  locale: "en" | "tr";
  filteredCount: number;
  totalCount: number;
  selectedType: string;
  allTypesLabel: string;
  selectedStateLabel?: string | null;
  selectedAccessLabel?: string | null;
};

export function sortRegistryTypeLabels(typeLabels: string[]) {
  return [...typeLabels].sort((left, right) => {
    const leftIndex = preferredTypeOrder.indexOf(left);
    const rightIndex = preferredTypeOrder.indexOf(right);

    if (leftIndex !== -1 || rightIndex !== -1) {
      if (leftIndex === -1) {
        return 1;
      }

      if (rightIndex === -1) {
        return -1;
      }

      return leftIndex - rightIndex;
    }

    return left.localeCompare(right);
  });
}

export function buildRegistryFilterSummary({
  locale,
  filteredCount,
  totalCount,
  selectedType,
  allTypesLabel,
  selectedStateLabel,
  selectedAccessLabel,
}: RegistryFilterSummaryOptions) {
  const countLabel =
    locale === "tr"
      ? `${filteredCount} / ${totalCount} kayit`
      : `${filteredCount} / ${totalCount} records`;
  const segments = [
    selectedType === "ALL" ? allTypesLabel : selectedType,
    selectedAccessLabel,
    selectedStateLabel,
    countLabel,
  ].filter((segment): segment is string => Boolean(segment));

  return segments.join(" | ");
}
