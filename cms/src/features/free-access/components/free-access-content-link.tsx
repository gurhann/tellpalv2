import { Link } from "react-router-dom";

import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";

type FreeAccessContentLinkProps = {
  contentId: number;
  content?: ContentReadViewModel | null;
};

function getContentLabel(content?: ContentReadViewModel | null) {
  if (!content) {
    return null;
  }

  return content.primaryLocalization?.title ?? content.summary.externalKey;
}

export function FreeAccessContentLink({
  contentId,
  content = null,
}: FreeAccessContentLinkProps) {
  const contentLabel = getContentLabel(content);

  if (!contentLabel) {
    return (
      <div className="space-y-1">
        <p className="font-medium text-foreground">#{contentId}</p>
        <p className="text-xs text-muted-foreground">
          Content details unavailable
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-1">
      <Link
        className="font-medium text-foreground underline underline-offset-4"
        to={`/contents/${contentId}`}
      >
        {contentLabel}
      </Link>
      <p className="text-xs text-muted-foreground">
        {content?.summary.externalKey}
      </p>
    </div>
  );
}
