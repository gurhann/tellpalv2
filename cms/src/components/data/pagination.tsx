import { Button } from "@/components/ui/button";
import { useI18n } from "@/i18n/locale-provider";

type PaginationProps = {
  page: number;
  pageSize: number;
  totalItems: number;
  isLoading?: boolean;
  onPageChange: (page: number) => void;
};

export function Pagination({
  page,
  pageSize,
  totalItems,
  isLoading = false,
  onPageChange,
}: PaginationProps) {
  const { locale } = useI18n();
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  return (
    <nav
      aria-label={locale === "tr" ? "Sayfalama" : "Pagination"}
      className="flex items-center justify-between gap-3"
    >
      <p className="text-sm text-muted-foreground">
        {locale === "tr"
          ? `Sayfa ${page + 1} / ${totalPages}`
          : `Page ${page + 1} / ${totalPages}`}
      </p>
      <div className="flex gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={page === 0 || isLoading}
          onClick={() => onPageChange(page - 1)}
        >
          {locale === "tr" ? "Önceki" : "Previous"}
        </Button>
        <Button
          type="button"
          variant="outline"
          disabled={page + 1 >= totalPages || isLoading}
          onClick={() => onPageChange(page + 1)}
        >
          {locale === "tr" ? "Sonraki" : "Next"}
        </Button>
      </div>
    </nav>
  );
}
