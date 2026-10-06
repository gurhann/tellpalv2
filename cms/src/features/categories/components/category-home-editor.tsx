import { useState } from "react";
import { ArrowDown, ArrowUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormSection } from "@/components/forms/form-section";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { useI18n } from "@/i18n/locale-provider";
import type {
  HomeRow,
  HomeSnapshot,
} from "@/features/categories/api/category-home-admin";
import type { ApiProblemDetail } from "@/types/api";

type Props = {
  snapshot: HomeSnapshot;
  saving: boolean;
  problem: ApiProblemDetail | null;
  onSave: (rows: HomeRow[]) => void;
  onReload: () => void;
};
export function CategoryHomeEditor({
  snapshot,
  saving,
  problem,
  onSave,
  onReload,
}: Props) {
  const { locale } = useI18n();
  const tr = locale === "tr";
  const [rows, setRows] = useState(snapshot.rows);
  const patch = (id: number, values: Partial<HomeRow>) =>
    setRows((current) =>
      current.map((r) => (r.categoryId === id ? { ...r, ...values } : r)),
    );
  const configured = rows
    .filter((r) => r.displayOrder !== null)
    .sort((a, b) => a.displayOrder! - b.displayOrder!);
  const move = (id: number, direction: number) => {
    const index = configured.findIndex((r) => r.categoryId === id);
    const other = configured[index + direction];
    const current = configured[index];
    if (!other || !current) return;
    setRows((list) =>
      list.map((r) =>
        r.categoryId === id
          ? { ...r, displayOrder: other.displayOrder }
          : r.categoryId === other.categoryId
            ? { ...r, displayOrder: current.displayOrder }
            : r,
      ),
    );
  };
  const toggle = (id: number, included: boolean) => {
    if (!included) {
      patch(id, { displayOrder: null });
      return;
    }
    const maximum = Math.max(-1, ...configured.map((r) => r.displayOrder!));
    if (maximum < 2_147_483_647) {
      patch(id, { displayOrder: maximum + 1 });
      return;
    }
    // Preserve editorial relative order while creating a valid backend position.
    const positions = new Map(
      configured.map((row, index) => [row.categoryId, index]),
    );
    setRows((current) =>
      current.map((row) => ({
        ...row,
        displayOrder:
          row.categoryId === id
            ? configured.length
            : (positions.get(row.categoryId) ?? null),
      })),
    );
  };
  const sorted = [...rows].sort(
    (a, b) =>
      (a.displayOrder ?? Number.MAX_SAFE_INTEGER) -
        (b.displayOrder ?? Number.MAX_SAFE_INTEGER) ||
      a.categoryId - b.categoryId,
  );
  return (
    <FormSection
      title={tr ? "Kitaplık satırları" : "Library rows"}
      description={
        tr
          ? "Seçili dilin satır sırasını kaydedin. Farklı içerik dili seçilen satırın adı bu dili açıkça belirtmelidir."
          : "Save the selected language's row order. A row's name must clearly identify a deliberately different content language."
      }
      actions={
        <div className="flex gap-2">
          <Button variant="outline" disabled={saving} onClick={onReload}>
            {tr ? "Yeniden yükle" : "Reload"}
          </Button>
          <Button disabled={saving} onClick={() => onSave(rows)}>
            {saving
              ? tr
                ? "Kaydediliyor…"
                : "Saving…"
              : tr
                ? "Sırayı kaydet"
                : "Save order"}
          </Button>
        </div>
      }
    >
      <ProblemAlert problem={problem} />
      <ol
        aria-label={tr ? "Kitaplık satır sırası" : "Library row order"}
        className="space-y-3"
      >
        {sorted.map((row) => {
          const index = configured.findIndex(
            (r) => r.categoryId === row.categoryId,
          );
          return (
            <li
              key={row.categoryId}
              className="rounded-xl border border-border p-4"
            >
              <div className="flex flex-wrap items-center gap-4">
                <label className="flex min-w-56 flex-1 items-center gap-3">
                  <input
                    type="checkbox"
                    checked={row.displayOrder !== null}
                    disabled={saving}
                    onChange={(e) => toggle(row.categoryId, e.target.checked)}
                    aria-label={`${row.name} ${tr ? "Kitaplıkta göster" : "Show in Library"}`}
                  />
                  <span>
                    <span className="block font-medium">{row.name}</span>
                    <span className="text-xs text-muted-foreground">
                      #{row.categoryId} · {row.slug} · {row.status}
                      {!row.active ? (tr ? " · Pasif" : " · Inactive") : ""}
                    </span>
                  </span>
                </label>
                <label className="grid gap-1 text-sm">
                  {tr ? "Hedef kitle" : "Audience"}
                  <select
                    aria-label={`${row.name} ${tr ? "hedef kitle" : "audience"}`}
                    className="rounded-lg border border-input bg-background px-3 py-2"
                    value={row.audience}
                    disabled={saving}
                    onChange={(e) =>
                      patch(row.categoryId, {
                        audience: e.target.value as HomeRow["audience"],
                      })
                    }
                  >
                    <option value="ALL">{tr ? "Herkes" : "Everyone"}</option>
                    <option value="NON_SUBSCRIBER">
                      {tr ? "Abone olmayanlar" : "Non-subscribers"}
                    </option>
                  </select>
                </label>
                <label className="grid gap-1 text-sm">
                  {tr ? "İçerik dili" : "Content language"}
                  <select
                    aria-label={`${row.name} ${tr ? "içerik dili" : "content language"}`}
                    className="rounded-lg border border-input bg-background px-3 py-2"
                    value={row.contentLanguage ?? ""}
                    disabled={saving}
                    onChange={(e) =>
                      patch(row.categoryId, {
                        contentLanguage: e.target.value || null,
                      })
                    }
                  >
                    <option value="">
                      {tr ? "Seçili dil" : "Selected language"}
                    </option>
                    <option value="tr">Türkçe</option>
                    <option value="en">English</option>
                    <option value="pt">Português</option>
                  </select>
                </label>
                <div className="flex gap-1">
                  <Button
                    variant="outline"
                    size="icon"
                    disabled={saving || index <= 0}
                    aria-label={`${tr ? "Yukarı taşı" : "Move up"}: ${row.name}`}
                    onClick={() => move(row.categoryId, -1)}
                  >
                    <ArrowUp className="size-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="icon"
                    disabled={
                      saving || index < 0 || index === configured.length - 1
                    }
                    aria-label={`${tr ? "Aşağı taşı" : "Move down"}: ${row.name}`}
                    onClick={() => move(row.categoryId, 1)}
                  >
                    <ArrowDown className="size-4" />
                  </Button>
                </div>
              </div>
            </li>
          );
        })}
      </ol>
      {rows.length === 0 ? (
        <p>
          {tr
            ? "Bu dilde STORY kategorisi yok."
            : "No STORY categories in this language."}
        </p>
      ) : null}
    </FormSection>
  );
}
