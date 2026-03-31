import { BookOpenText } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";

type StoryPageEntryLinkProps = {
  contentId: number;
  canOpen: boolean;
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
  label = "Open story pages",
  unavailableLabel = "Story pages unavailable",
  variant = "outline",
}: StoryPageEntryLinkProps) {
  if (canOpen) {
    return (
      <Button asChild variant={variant}>
        <Link to={`/contents/${contentId}/story-pages`}>
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
