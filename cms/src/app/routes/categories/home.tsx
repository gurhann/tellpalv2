import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { CategoryHomeEditor } from "@/features/categories/components/category-home-editor";
import {
  categoryHomeAdminApi,
  type HomeRow,
  type HomeSnapshot,
} from "@/features/categories/api/category-home-admin";
import { ApiClientError } from "@/lib/http/client";
import { useI18n } from "@/i18n/locale-provider";

export function CategoryHomeRoute() {
  const { locale } = useI18n();
  const tr = locale === "tr";
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [params, setParams] = useSearchParams();
  const language = ["tr", "en", "pt"].includes(params.get("lang") ?? "")
    ? params.get("lang")!
    : "tr";
  return (
    <ContentPageShell
      title={tr ? "Kitaplık sırası" : "Library order"}
      eyebrow={tr ? "Kategoriler" : "Categories"}
      description={
        tr
          ? "Her dil için bağımsız kategori satırları. Kategori içindeki hikâye sırası korunur."
          : "Independent category rows for each language. Within-category story order is preserved."
      }
      actions={
        <Button variant="outline" onClick={() => navigate("/categories")}>
          {tr ? "Kategorilere dön" : "Back to categories"}
        </Button>
      }
      toolbar={
        <label className="flex items-center gap-3 text-sm font-medium">
          {tr ? "Sunum dili" : "Presentation language"}
          <select
            aria-label={tr ? "Sunum dili" : "Presentation language"}
            className="rounded-lg border border-input bg-background px-3 py-2"
            value={language}
            disabled={busy}
            onChange={(e) => {
              setParams({ lang: e.target.value });
            }}
          >
            <option value="tr">Türkçe</option>
            <option value="en">English</option>
            <option value="pt">Português</option>
          </select>
        </label>
      }
    >
      <HomeConfiguration key={language} language={language} onBusy={setBusy} />
    </ContentPageShell>
  );
}

function HomeConfiguration({
  language,
  onBusy,
}: {
  language: string;
  onBusy: (busy: boolean) => void;
}) {
  const { locale } = useI18n();
  const tr = locale === "tr";
  const client = useQueryClient();
  const [snapshot, setSnapshot] = useState<HomeSnapshot | null>(null);
  const [generation, setGeneration] = useState(0);
  const [reloading, setReloading] = useState(false);
  const [reloadFailed, setReloadFailed] = useState(false);
  const key = ["categories", "library-home", language];
  const query = useQuery({
    queryKey: key,
    queryFn: () => categoryHomeAdminApi.read(language),
  });
  // The first successful read owns this editing session until an explicit replacement.
  useEffect(() => {
    if (!snapshot && query.data) setSnapshot(query.data);
  }, [snapshot, query.data]);
  const replace = (data: HomeSnapshot) => {
    setSnapshot(data);
    setGeneration((value) => value + 1);
    setReloadFailed(false);
  };
  const save = useMutation({
    mutationFn: ({ revision, rows }: { revision: string; rows: HomeRow[] }) =>
      categoryHomeAdminApi.save(
        language,
        revision,
        rows.map(({ categoryId, displayOrder, audience, contentLanguage }) => ({
          categoryId,
          displayOrder,
          audience,
          contentLanguage,
        })),
      ),
    onSuccess: (data) => {
      client.setQueryData(key, data);
      replace(data);
    },
  });
  useEffect(() => {
    onBusy(save.isPending || reloading);
    return () => onBusy(false);
  }, [onBusy, save.isPending, reloading]);
  const reload = async () => {
    save.reset();
    setReloading(true);
    const result = await query.refetch();
    if (result.isSuccess && result.data) replace(result.data);
    else setReloadFailed(true);
    setReloading(false);
  };
  const problem =
    save.error instanceof ApiClientError
      ? save.error.problem
      : save.error
        ? {
            title: tr ? "Kaydedilemedi" : "Save failed",
            detail: tr
              ? "Tekrar deneyin veya güncel sırayı yükleyin."
              : "Retry or reload the current order.",
            status: 500,
            type: "about:blank",
          }
        : null;
  if (!snapshot)
    return query.error ? (
      <ProblemAlert
        title={tr ? "Sıra yüklenemedi" : "Order unavailable"}
        actions={
          <Button onClick={() => void query.refetch()}>
            {tr ? "Tekrar dene" : "Retry"}
          </Button>
        }
      />
    ) : (
      <p role="status">
        {tr ? "Kitaplık sırası yükleniyor…" : "Loading Library order…"}
      </p>
    );
  const changed = query.data && query.data.revision !== snapshot.revision;
  return (
    <>
      {reloadFailed || query.error ? (
        <ProblemAlert title={tr ? "Sıra yüklenemedi" : "Order unavailable"} />
      ) : changed ? (
        <ProblemAlert
          title={tr ? "Kitaplık sırası değişti" : "Library order changed"}
        />
      ) : null}
      {save.isSuccess ? (
        <p role="status">{tr ? "Sıra kaydedildi." : "Order saved."}</p>
      ) : null}
      <CategoryHomeEditor
        key={`${language}:${generation}`}
        snapshot={snapshot}
        saving={save.isPending || reloading}
        problem={problem}
        onSave={(rows) => save.mutate({ revision: snapshot.revision, rows })}
        onReload={() => void reload()}
      />
    </>
  );
}
