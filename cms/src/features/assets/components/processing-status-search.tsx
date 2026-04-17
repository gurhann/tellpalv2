import { Search } from "lucide-react";
import type { FormEvent } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useI18n } from "@/i18n/locale-provider";
import { getSupportedCmsLanguageOptions } from "@/lib/languages";

type ProcessingStatusSearchProps = {
  contentId: string;
  languageCode: string;
  isPending?: boolean;
  onContentIdChange: (value: string) => void;
  onLanguageCodeChange: (value: string) => void;
  onSubmit: () => void;
};

export function ProcessingStatusSearch({
  contentId,
  languageCode,
  isPending = false,
  onContentIdChange,
  onLanguageCodeChange,
  onSubmit,
}: ProcessingStatusSearchProps) {
  const { locale } = useI18n();
  const languageOptions = getSupportedCmsLanguageOptions(locale);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form className="grid gap-3" noValidate onSubmit={handleSubmit}>
      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor="processing-status-content-id"
        >
          {locale === "tr" ? "Icerik id" : "Content id"}
        </label>
        <Input
          id="processing-status-content-id"
          inputMode="numeric"
          placeholder={locale === "tr" ? "Ornek: 42" : "Example: 42"}
          value={contentId}
          onChange={(event) => onContentIdChange(event.target.value)}
        />
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-foreground">
          {locale === "tr" ? "Dil" : "Language"}
        </label>
        <Select value={languageCode} onValueChange={onLanguageCodeChange}>
          <SelectTrigger
            aria-label={locale === "tr" ? "Dil sec" : "Select language"}
            className="w-full"
          >
            <SelectValue
              placeholder={locale === "tr" ? "Dil sec" : "Select language"}
            />
          </SelectTrigger>
          <SelectContent>
            {languageOptions.map((option) => (
              <SelectItem key={option.code} value={option.code}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button className="w-full" type="submit" disabled={isPending}>
        <Search className="size-4" />
        {locale === "tr" ? "Durumu sorgula" : "Lookup status"}
      </Button>
    </form>
  );
}
