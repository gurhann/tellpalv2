import { BookOpenText } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";

type StoryPageEntryLinkProps = {
  contentId: number;
  canOpen: boolean;
  preferredLanguageCode?: string | null;
  label?: string;
  unavailableLabel?: string;
  variant?:
    | "default"
    | "outline"
    | "secondary"
    | "ghost"
    | "destructive"
    | "link";
};

export function StoryPageEntryLink({
  contentId,
  canOpen,
  preferredLanguageCode = null,
  label = "Open story pages",
  unavailableLabel = "Story pages unavailable",
  variant = "outline",
}: StoryPageEntryLinkProps) {
  if (canOpen) {
    const storyPagesHref = preferredLanguageCode
      ? `/contents/${contentId}/story-pages?language=${preferredLanguageCode}`
      : `/contents/${contentId}/story-pages`;

    return (
      <Button asChild variant={variant}>
        <Link to={storyPagesHref}>
          <BookOpenText className="size-4" />
          {label}
        </Link>
      </Button>
    );
  }

  return (
    <Button disabled type="button" variant={variant}>
      <BookOpenText className="size-4" />
      {unavailableLabel}
    </Button>
  );
}
