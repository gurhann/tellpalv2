import { Languages } from "lucide-react";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useI18n, type AppLocale } from "@/i18n/locale-provider";

type LocaleSwitcherProps = {
  compact?: boolean;
  className?: string;
};

const availableLocales: AppLocale[] = ["en", "tr"];

export function LocaleSwitcher({
  compact = false,
  className,
}: LocaleSwitcherProps) {
  const { locale, setLocale, t } = useI18n();

  return (
    <div className={className}>
      {!compact ? (
        <label
          className="mb-2 inline-flex items-center gap-2 text-sm font-medium text-foreground"
          htmlFor="cms-locale-switcher"
        >
          <Languages className="size-4 text-muted-foreground" />
          {t("app.language")}
        </label>
      ) : null}
      <Select
        value={locale}
        onValueChange={(value) => setLocale(value as AppLocale)}
      >
        <SelectTrigger
          id="cms-locale-switcher"
          aria-label={t("app.language")}
          className={compact ? "w-[8.75rem]" : "w-full"}
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {availableLocales.map((item) => (
            <SelectItem key={item} value={item}>
              {t(`app.locale.${item}`)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
