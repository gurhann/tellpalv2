import type { ReactNode } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import {
  LanguageBadge,
  type LanguageBadgeTone,
} from "@/components/language/language-badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useI18n } from "@/i18n/locale-provider";
import { cn } from "@/lib/utils";

export type LanguageTabItem = {
  code: string;
  label?: string;
  tone?: LanguageBadgeTone;
  meta?: string;
  description?: string;
  disabled?: boolean;
};

type LanguageTabsProps = {
  items: LanguageTabItem[];
  value: string;
  onValueChange: (value: string) => void;
  renderContent?: (item: LanguageTabItem) => ReactNode;
  listLabel?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  className?: string;
  listClassName?: string;
  contentClassName?: string;
};

export function LanguageTabs({
  items,
  value,
  onValueChange,
  renderContent,
  listLabel,
  emptyTitle,
  emptyDescription,
  className,
  listClassName,
  contentClassName,
}: LanguageTabsProps) {
  const { t } = useI18n();
  const resolvedListLabel = listLabel ?? t("language.tabs.label");
  const resolvedEmptyTitle = emptyTitle ?? t("language.emptyTitle");
  const resolvedEmptyDescription =
    emptyDescription ?? t("language.emptyDescription");

  if (items.length === 0) {
    return (
      <EmptyState
        description={resolvedEmptyDescription}
        title={resolvedEmptyTitle}
      />
    );
  }

  const resolvedValue =
    items.find((item) => item.code === value)?.code ?? items[0].code;

  return (
    <Tabs
      className={cn("gap-4", className)}
      value={resolvedValue}
      onValueChange={onValueChange}
    >
      <div className="overflow-x-auto pb-2">
        <TabsList
          aria-label={resolvedListLabel}
          className={cn(
            "h-auto w-max min-w-full items-stretch justify-start gap-2 rounded-2xl bg-muted/30 p-2",
            listClassName,
          )}
          variant="line"
        >
          {items.map((item) => (
            <TabsTrigger
              key={item.code}
              className="min-h-[5.5rem] min-w-40 flex-none items-start justify-start whitespace-normal rounded-xl border border-transparent px-3 py-2 text-left after:hidden data-active:border-border/70 data-active:bg-background"
              disabled={item.disabled}
              value={item.code}
            >
              <div className="flex min-h-full w-full flex-col items-start gap-2">
                <LanguageBadge
                  code={item.code}
                  label={item.label}
                  meta={item.meta}
                  tone={item.tone}
                />
                {item.description ? (
                  <p className="line-clamp-2 max-w-full text-xs leading-5 text-muted-foreground">
                    {item.description}
                  </p>
                ) : null}
              </div>
            </TabsTrigger>
          ))}
        </TabsList>
      </div>

      {renderContent
        ? items.map((item) => (
            <TabsContent
              key={item.code}
              className={cn("mt-0", contentClassName)}
              value={item.code}
            >
              {renderContent(item)}
            </TabsContent>
          ))
        : null}
    </Tabs>
  );
}
