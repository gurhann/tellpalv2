import {
  ArrowDown,
  ArrowUp,
  CirclePlus,
  ExternalLink,
  Pencil,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { AssignContributorDialog } from "@/features/contributors/components/assign-contributor-dialog";
import { EditContentContributorDialog } from "@/features/contributors/components/edit-content-contributor-dialog";
import { UnassignContributorButton } from "@/features/contributors/components/unassign-contributor-button";
import type { ContributorRole } from "@/features/contributors/api/contributor-admin";
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { useContentContributorAssignments } from "@/features/contributors/queries/use-content-contributor-assignments";
import { ApiClientError } from "@/lib/http/client";
import { getProblemMessage } from "@/lib/http/problem-details";
import { resolveLanguageLabel } from "@/lib/languages";
import { useI18n } from "@/i18n/locale-provider";

const ROLES: ContributorRole[] = [
  "AUTHOR",
  "ILLUSTRATOR",
  "NARRATOR",
  "MUSICIAN",
];
type Props = { content: ContentReadViewModel; activeLanguageCode?: string };

export function ContentContributorPanel({
  content,
  activeLanguageCode,
}: Props) {
  const { locale, t } = useI18n();
  const query = useContentContributorAssignments(content.summary.id);
  const actions = useContributorActions();
  const [assignRole, setAssignRole] = useState<ContributorRole | null>(null);
  const [editingAssignment, setEditingAssignment] =
    useState<ContentContributorViewModel | null>(null);
  const [optimisticAssignments, setOptimisticAssignments] = useState<
    ContentContributorViewModel[] | null
  >(null);
  const [reorderError, setReorderError] = useState<string | null>(null);
  const visible = optimisticAssignments ?? query.assignments;
  const grouped = useMemo(() => {
    const result = new Map<
      ContributorRole,
      Map<string, ContentContributorViewModel[]>
    >();
    ROLES.forEach((role) => result.set(role, new Map()));
    visible.forEach((item) => {
      const scopes = result.get(item.role)!;
      const key = item.languageCode ?? "global";
      scopes.set(key, [...(scopes.get(key) ?? []), item]);
    });
    return result;
  }, [visible]);
  async function move(
    item: ContentContributorViewModel,
    delta: -1 | 1,
    group: ContentContributorViewModel[],
  ) {
    const index = group.indexOf(item);
    const target = index + delta;
    if (index < 0 || target < 0 || target >= group.length) return;
    const nextGroup = [...group];
    [nextGroup[index], nextGroup[target]] = [
      nextGroup[target],
      nextGroup[index],
    ];
    const previous = visible;
    setReorderError(null);
    setOptimisticAssignments(
      visible.map((entry) => {
        const i = group.indexOf(entry);
        return i >= 0 ? nextGroup[i] : entry;
      }),
    );
    try {
      await actions.reorderContributors.mutateAsync({
        contentId: content.summary.id,
        role: item.role,
        languageCode: item.languageCode,
        assignmentIds: nextGroup.map((entry) => entry.assignmentId),
      });
      setOptimisticAssignments(null);
    } catch (error) {
      setOptimisticAssignments(previous);
      setReorderError(
        error instanceof ApiClientError
          ? getProblemMessage(error.problem)
          : t("contributors.panel.reorderError"),
      );
    }
  }
  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border/70 bg-muted/15 px-4 py-3">
        <div>
          <p className="text-sm font-medium">{t("contributors.panel.title")}</p>
          <p className="text-sm text-muted-foreground">
            {visible.length === 1
              ? t("contributors.panel.summaryOne")
              : t("contributors.panel.summary", { count: visible.length })}
          </p>
        </div>
        <Button asChild type="button" variant="outline">
          <Link to="/contributors">
            <ExternalLink className="size-4" />
            {t("contributors.panel.openRegistry")}
          </Link>
        </Button>
      </div>
      {query.problem ? <ProblemAlert problem={query.problem} /> : null}
      {reorderError ? (
        <p
          role="alert"
          className="rounded-lg border border-destructive/40 px-3 py-2 text-sm text-destructive"
        >
          {reorderError}
        </p>
      ) : null}
      {query.isLoading ? (
        <div className="rounded-2xl border px-4 py-8 text-sm text-muted-foreground">
          {t("contributors.panel.loading")}
        </div>
      ) : query.problem ? null : (
        <div className="grid gap-4">
          {ROLES.map((role) => {
            const scopes = grouped.get(role)!;
            return (
              <section
                key={role}
                aria-labelledby={`contributors-role-${role}`}
                className="rounded-2xl border border-border/70 bg-background p-4"
              >
                <div className="mb-3 flex items-center justify-between">
                  <h3
                    id={`contributors-role-${role}`}
                    className="font-semibold"
                  >
                    {t(`contributors.role.${role.toLowerCase()}` as never)}
                  </h3>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setAssignRole(role)}
                  >
                    <CirclePlus className="size-4" />
                    {t("contributors.panel.addSuffix")}
                  </Button>
                </div>
                {scopes.size === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t("contributors.panel.noAssignments")}
                  </p>
                ) : (
                  [...scopes.values()].map((entries) => (
                    <div
                      key={entries[0]?.languageCode ?? "global"}
                      className="mb-3 last:mb-0"
                    >
                      <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        {entries[0]?.languageCode
                          ? resolveLanguageLabel(
                              entries[0].languageCode,
                              locale,
                            )
                          : t("contributors.picker.allLanguages")}
                      </p>
                      <div className="grid gap-2">
                        {entries.map((assignment, index) => (
                          <div
                            key={assignment.assignmentId}
                            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border/60 px-3 py-3"
                          >
                            <div>
                              <p className="font-medium">
                                {assignment.effectiveCreditName}
                              </p>
                              <p className="text-sm text-muted-foreground">
                                {assignment.displayName}
                              </p>
                            </div>
                            <div className="flex items-center gap-1">
                              <Button
                                type="button"
                                size="icon"
                                variant="ghost"
                                aria-label={t("contributors.panel.edit")}
                                onClick={() => setEditingAssignment(assignment)}
                              >
                                <Pencil className="size-4" />
                              </Button>
                              <Button
                                type="button"
                                size="icon"
                                variant="ghost"
                                aria-label={t("contributors.panel.moveUp")}
                                disabled={
                                  actions.reorderContributors.isPending ||
                                  index === 0
                                }
                                onClick={() =>
                                  void move(assignment, -1, entries)
                                }
                              >
                                <ArrowUp className="size-4" />
                              </Button>
                              <Button
                                type="button"
                                size="icon"
                                variant="ghost"
                                aria-label={t("contributors.panel.moveDown")}
                                disabled={
                                  actions.reorderContributors.isPending ||
                                  index === entries.length - 1
                                }
                                onClick={() =>
                                  void move(assignment, 1, entries)
                                }
                              >
                                <ArrowDown className="size-4" />
                              </Button>
                              <UnassignContributorButton
                                assignment={assignment}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </section>
            );
          })}
        </div>
      )}
      {assignRole ? (
        <AssignContributorDialog
          content={content}
          existingAssignments={visible}
          role={assignRole}
          initialLanguageCode={activeLanguageCode}
          open
          onOpenChange={(open) => {
            if (!open) setAssignRole(null);
          }}
        />
      ) : null}
      {editingAssignment ? (
        <EditContentContributorDialog
          key={editingAssignment.assignmentId}
          assignment={editingAssignment}
          content={content}
          open
          onOpenChange={(open) => {
            if (!open) setEditingAssignment(null);
          }}
        />
      ) : null}
    </div>
  );
}
