import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { RegistryToolbar, RegistryToolbarGroup } from "@/components/data/registry-toolbar";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentListTable } from "@/features/contents/components/content-list-table";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentRegistry } from "@/features/contents/queries/use-content-registry";
import type { ContentRegistryReadiness, ContentType } from "@/features/contents/api/content-admin";
import { useI18n } from "@/i18n/locale-provider";
import { getCreateContentFormDefaults } from "@/features/contents/schema/content-schema";

const types: Array<ContentType | "ALL"> = ["ALL", "STORY", "AUDIO_STORY", "MEDITATION", "LULLABY"];
const readinesses: Array<ContentRegistryReadiness | "ALL"> = ["ALL", "ACTION_REQUIRED", "READY_TO_PUBLISH", "PUBLISHED"];

export function ContentsIndexRoute() {
  const { locale } = useI18n();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const language = searchParams.get("language") ?? "tr";
  const type = (searchParams.get("type") ?? "ALL") as ContentType | "ALL";
  const readiness = (searchParams.get("readiness") ?? "ALL") as ContentRegistryReadiness | "ALL";
  const [search, setSearch] = useState(searchParams.get("q") ?? "");
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const deferredSearch = useDeferredValue(search);
  const page = Math.max(0, Number(searchParams.get("page") ?? "0") || 0);
  const registry = useContentRegistry({ language, type: type === "ALL" ? undefined : type, readiness: readiness === "ALL" ? undefined : readiness, q: deferredSearch, page, size: 25 });
  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key));
    if (!("page" in changes)) next.delete("page");
    setSearchParams(next);
  };
  const stateLabel = (state: ContentRegistryReadiness | "ALL") => state === "ALL" ? (locale === "tr" ? "Tüm durumlar" : "All statuses") : state === "ACTION_REQUIRED" ? (locale === "tr" ? "Aksiyon gerekli" : "Action required") : state === "READY_TO_PUBLISH" ? (locale === "tr" ? "Yayına hazır" : "Ready to publish") : (locale === "tr" ? "Yayında" : "Published");
  return <><ContentPageShell eyebrow={locale === "tr" ? "Editoryal çekirdek" : "Editorial core"} title={locale === "tr" ? "İçerikler" : "Contents"} description={locale === "tr" ? "Seçili dildeki yayın engellerini bulun ve ilgili editöre geçin." : "Find selected-locale blockers and open the relevant editor."} actions={<><Button variant="outline" type="button" onClick={() => void registry.refetch()}><RefreshCw className={registry.isFetching ? "size-4 animate-spin" : "size-4"} />{locale === "tr" ? "Yenile" : "Refresh"}</Button><Button type="button" onClick={() => setIsCreateDialogOpen(true)}><CirclePlus className="size-4" />{locale === "tr" ? "İçerik oluştur" : "Create content"}</Button></>} toolbar={<RegistryToolbar ariaLabel={locale === "tr" ? "İçerik filtreleri" : "Content filters"} search={<RegistryToolbarGroup className="w-full" label={locale === "tr" ? "Arama" : "Search"}><div className="relative min-w-[16rem] flex-1"><Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" /><Input className="pl-8" value={search} placeholder={locale === "tr" ? "Başlık, anahtar veya ID" : "Title, key, or ID"} onChange={(event) => { setSearch(event.target.value); update({ q: event.target.value.trim() || null }); }} /></div></RegistryToolbarGroup>} filters={<><RegistryToolbarGroup label={locale === "tr" ? "Dil" : "Language"}><div className="flex flex-wrap gap-2">{["tr", "en", "de", "es", "pt"].map((value) => <Button key={value} type="button" size="sm" variant={language === value ? "secondary" : "outline"} aria-pressed={language === value} onClick={() => update({ language: value })}>{value.toUpperCase()}</Button>)}</div></RegistryToolbarGroup><RegistryToolbarGroup label={locale === "tr" ? "Tür" : "Type"}><div className="flex flex-wrap gap-2">{types.map((value) => <Button key={value} type="button" size="sm" variant={type === value ? "secondary" : "outline"} aria-pressed={type === value} onClick={() => update({ type: value === "ALL" ? null : value })}>{value === "ALL" ? (locale === "tr" ? "Tümü" : "All") : value}</Button>)}</div></RegistryToolbarGroup><RegistryToolbarGroup label={locale === "tr" ? "Durum" : "Readiness"}><div className="flex flex-wrap gap-2">{readinesses.map((value) => <Button key={value} type="button" size="sm" variant={readiness === value ? "secondary" : "outline"} aria-pressed={readiness === value} onClick={() => update({ readiness: value === "ALL" ? null : value })}>{stateLabel(value)}</Button>)}</div></RegistryToolbarGroup></>} summaryTitle={locale === "tr" ? `${registry.registry?.totalItems ?? 0} sonuç · ${language.toUpperCase()}` : `${registry.registry?.totalItems ?? 0} results · ${language.toUpperCase()}`} summaryDescription={locale === "tr" ? "Son güncellenen içerikler önce gelir." : "Most recently edited content appears first."} /> }><ContentListTable items={registry.registry?.items ?? []} isLoading={registry.isLoading} problem={registry.problem} onRetry={() => void registry.refetch()} onContentSelect={(item) => navigate(`/contents/${item.contentId}?language=${language}`)} /></ContentPageShell><Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}><DialogContent className="sm:max-w-2xl"><DialogHeader><DialogTitle>{locale === "tr" ? "İçerik oluştur" : "Create content"}</DialogTitle><DialogDescription>{locale === "tr" ? "Yeni editoryal kaydı oluşturun." : "Create a new editorial record."}</DialogDescription></DialogHeader><DialogBody><ContentForm initialValues={getCreateContentFormDefaults()} mode="create" onCancel={() => setIsCreateDialogOpen(false)} onSuccess={(content) => { setIsCreateDialogOpen(false); navigate(`/contents/${content.contentId}?language=${language}`); }} /></DialogBody></DialogContent></Dialog></>;
}
