import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Controller } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import { useZodForm } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import {
  contentAdminApi,
} from "@/features/contents/api/content-admin";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { queryKeys } from "@/lib/query-keys";
import { useI18n } from "@/i18n/locale-provider";
import { z } from "zod";

type Props = {
  content: ContentReadViewModel;
  languageCode?: string;
};

const playbackSchema = z.object({
  audioMediaId: z.number().int().positive("Audio asset is required."),
  durationMinutes: z.number().int().nonnegative("Duration must not be negative."),
});

function problemMessage(error: unknown, fallback: string) {
  return error instanceof Error
    ? error.message
    : fallback;
}

/** Content-scoped editor: playback and instruments are never copied into a locale. */
export function LullabyPlaybackEditor({ content, languageCode }: Props) {
  const { locale } = useI18n();
  const copy = locale === "tr"
    ? {
        playback: "Ortak playback",
        playbackDescription: "Bu ses ve süre tüm ninni dilleriyle paylaşılır.",
        noPlayback: "Henüz ortak playback tanımlanmadı; ses ve süreyi girerek oluşturun.",
        audioLabel: "Ortak playback ses asset'i",
        audioDescription: "Tek ortak ninni ses asset'ini seçin veya yükleyin.",
        duration: "Ortak playback süresi (dakika)",
        savePlayback: "Ortak playback'i kaydet",
        savingPlayback: "Ortak playback kaydediliyor...",
        processing: "İşleme",
        notStarted: "Başlamadı",
        playbackError: "Ortak ninni playback'i güncellenemedi. Tekrar deneyin.",
        instruments: "Katalog enstrümanları",
        instrumentsDescription: "Etiketler seçili dile göre gösterilir; kayıt stable katalog kodlarıyla yapılır.",
        refresh: "Yenile",
        minInstrument: "En az bir katalog enstrümanı seçin.",
        selectLocale: "Yerelleştirme seçerek yerelleştirilmiş enstrüman etiketlerini yükleyin.",
        loading: "Enstrümanlar yükleniyor…",
        catalogError: "Enstrüman kataloğu yüklenemedi. Tekrar deneyin; mevcut seçimler korunur.",
        noSelection: "Henüz enstrüman seçilmedi.",
        saveInstruments: "Enstrüman sırasını kaydet",
        savingInstruments: "Enstrümanlar kaydediliyor...",
        instrumentsError: "Enstrüman seçimi güncellenemedi. Tekrar deneyin.",
        available: "Kullanılabilir katalog enstrümanları",
        selected: "Seçili enstrümanlar",
        advancedAudioOptions: "Gelişmiş ninni ses seçenekleri",
        moveUp: (name: string) => `${name} yukarı taşı`,
        moveDown: (name: string) => `${name} aşağı taşı`,
      }
    : {
        playback: "Shared playback",
        playbackDescription: "This audio and duration are shared by every lullaby locale.",
        noPlayback: "No shared playback exists yet; enter an audio asset and duration to create it.",
        audioLabel: "Shared playback audio",
        audioDescription: "Choose or upload the single shared lullaby audio asset.",
        duration: "Shared playback duration (minutes)",
        savePlayback: "Save shared playback",
        savingPlayback: "Saving shared playback...",
        processing: "Processing",
        notStarted: "Not started",
        playbackError: "The shared lullaby playback could not be updated. Try again.",
        instruments: "Catalog instruments",
        instrumentsDescription: "Labels follow the selected locale; saved selections use stable catalog codes.",
        refresh: "Refresh",
        minInstrument: "Select at least one catalog instrument.",
        selectLocale: "Select a localization to load localized instrument labels.",
        loading: "Loading instruments…",
        catalogError: "The instrument catalog could not be loaded. Refresh to try again; existing selections remain unchanged.",
        noSelection: "No instruments selected yet.",
        saveInstruments: "Save instrument order",
        savingInstruments: "Saving instruments...",
        instrumentsError: "The instrument selection could not be updated. Try again.",
        available: "Available catalog instruments",
        selected: "Selected instruments",
        advancedAudioOptions: "Advanced lullaby audio options",
        moveUp: (name: string) => `Move ${name} up`,
        moveDown: (name: string) => `Move ${name} down`,
      };
  const queryClient = useQueryClient();
  const [instrumentSelectionDirty, setInstrumentSelectionDirty] =
    useState(false);
  const [selectedCodes, setSelectedCodes] = useState<string[]>(
    () => content.playback?.instruments.map((instrument) => instrument.code) ?? [],
  );
  const playback = content.playback;
  const form = useZodForm<z.infer<typeof playbackSchema>>({
    schema: playbackSchema,
    defaultValues: {
      audioMediaId: playback?.audioAssetId ?? 0,
      durationMinutes: playback?.durationMinutes ?? 0,
    },
  });
  useEffect(() => {
    form.reset({
      audioMediaId: playback?.audioAssetId ?? 0,
      durationMinutes: playback?.durationMinutes ?? 0,
    });
  }, [form, playback?.audioAssetId, playback?.durationMinutes]);
  const instrumentsQuery = useQuery({
    queryKey: queryKeys.contents.instruments(content.summary.id, languageCode),
    enabled: Boolean(languageCode),
    queryFn: () => contentAdminApi.listLullabyInstruments(content.summary.id, languageCode),
  });
  const catalogQuery = useQuery({
    queryKey: ["instrument-catalog", languageCode ?? null],
    enabled: Boolean(languageCode),
    queryFn: () => contentAdminApi.listInstrumentCatalog(languageCode as string),
  });

  useEffect(() => {
    if (instrumentsQuery.data && !instrumentSelectionDirty) {
      setSelectedCodes(instrumentsQuery.data.map((instrument) => instrument.code));
    }
  }, [instrumentSelectionDirty, instrumentsQuery.data]);

  const instrumentsByCode = useMemo(
    () => new Map((catalogQuery.data ?? []).map((instrument) => [instrument.code, instrument])),
    [catalogQuery.data],
  );
  const selectedInstruments = selectedCodes.map((code, index) => {
    const catalog = instrumentsByCode.get(code);
    const persisted = instrumentsQuery.data?.find((instrument) => instrument.code === code);
    return {
      code,
      displayName: catalog?.displayName ?? persisted?.displayName ?? code,
      displayOrder: index,
    };
  });
  const playbackMutation = useMutation({
    mutationFn: (values: z.infer<typeof playbackSchema>) =>
      contentAdminApi.updateLullabyPlayback(content.summary.id, values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.contents.detail(content.summary.id),
      });
    },
  });
  const instrumentsMutation = useMutation({
    mutationFn: (instrumentCodes: string[]) =>
      contentAdminApi.replaceLullabyInstruments(
        content.summary.id,
        instrumentCodes,
        languageCode,
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.contents.instruments(content.summary.id, languageCode),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.contents.detail(content.summary.id),
        }),
      ]);
      setInstrumentSelectionDirty(false);
    },
  });

  function toggleInstrument(code: string) {
    setSelectedCodes((current) =>
      current.includes(code)
        ? current.filter((value) => value !== code)
        : [...current, code],
    );
    setInstrumentSelectionDirty(true);
  }

  function moveInstrument(index: number, delta: -1 | 1) {
    const nextIndex = index + delta;
    if (nextIndex < 0 || nextIndex >= selectedCodes.length) return;
    setSelectedCodes((current) => {
      const next = [...current];
      [next[index], next[nextIndex]] = [next[nextIndex]!, next[index]!];
      return next;
    });
    setInstrumentSelectionDirty(true);
  }

  return (
    <div className="grid gap-5" data-testid="lullaby-playback-editor">
      <div className="rounded-2xl border border-border/70 bg-muted/10 p-4">
        <p className="text-sm font-medium text-foreground">{copy.playback}</p>
        <p className="mt-1 text-sm text-muted-foreground">
          {copy.playbackDescription}
        </p>
        {!playback ? (
          <p className="mt-3 text-sm text-muted-foreground" role="status">
            {copy.noPlayback}
          </p>
        ) : null}
        <form
          className="mt-4 grid gap-4 md:grid-cols-2"
          noValidate
          onSubmit={form.handleSubmit((values) => playbackMutation.mutate(values))}
        >
          <Controller
            control={form.control}
            name="audioMediaId"
            render={({ field, fieldState }) => (
              <AssetPickerField
                advancedLabel={copy.advancedAudioOptions}
                description={copy.audioDescription}
                disabled={playbackMutation.isPending}
                error={fieldState.error}
                id="lullabyPlaybackAudioMediaId"
                label={copy.audioLabel}
                manualInputLabel={`${copy.audioLabel} id`}
                mediaType="AUDIO"
                pickerDescription={copy.audioDescription}
                pickerTitle={copy.audioLabel}
                testId="lullaby-playback-audio"
                value={field.value || null}
                variant="editor"
                onChange={field.onChange}
              />
            )}
          />
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground" htmlFor="lullabyPlaybackDurationMinutes">
              {copy.duration}
            </label>
            <Input
              id="lullabyPlaybackDurationMinutes"
              inputMode="numeric"
              min={0}
              type="number"
              {...form.register("durationMinutes", { setValueAs: (value) => Number(value) })}
              disabled={playbackMutation.isPending}
            />
            <FieldError error={form.formState.errors.durationMinutes} />
          </div>
          {playback ? (
            <p className="text-sm text-muted-foreground md:col-span-2" role="status">
              {copy.processing}: {playback.processingStatus ?? copy.notStarted}
              {playback.processingError ? ` — ${playback.processingError}` : ""}
            </p>
          ) : null}
          {playbackMutation.error ? (
            <p className="text-sm text-destructive md:col-span-2" role="alert">
              {problemMessage(playbackMutation.error, copy.playbackError)}
            </p>
          ) : null}
          <div className="md:col-span-2 flex justify-end">
            <SubmitButton isPending={playbackMutation.isPending} pendingLabel={copy.savingPlayback}>
              {copy.savePlayback}
            </SubmitButton>
          </div>
        </form>
      </div>

      <div className="rounded-2xl border border-border/70 bg-muted/10 p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-foreground">{copy.instruments}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {copy.instrumentsDescription}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => void Promise.all([instrumentsQuery.refetch(), catalogQuery.refetch()])}
            disabled={!languageCode || instrumentsQuery.isFetching || catalogQuery.isFetching}
          >
            <RefreshCw className="size-4" /> {copy.refresh}
          </Button>
        </div>
        {!languageCode ? (
          <p className="mt-4 text-sm text-muted-foreground" role="status">
            {copy.selectLocale}
          </p>
        ) : instrumentsQuery.isLoading || catalogQuery.isLoading ? (
          <p className="mt-4 text-sm text-muted-foreground" role="status">{copy.loading}</p>
        ) : instrumentsQuery.error || catalogQuery.error ? (
          <p className="mt-4 text-sm text-destructive" role="alert">
            {copy.catalogError}
          </p>
        ) : (
          <>
            <div className="mt-4 flex flex-wrap gap-2" aria-label={copy.available}>
              {(catalogQuery.data ?? []).map((instrument) => (
                <Button
                  key={instrument.code}
                  aria-pressed={selectedCodes.includes(instrument.code)}
                  type="button"
                  variant={selectedCodes.includes(instrument.code) ? "default" : "outline"}
                  onClick={() => toggleInstrument(instrument.code)}
                >
                  {instrument.displayName}
                </Button>
              ))}
            </div>
            {selectedInstruments.length === 0 ? (
              <p className="mt-4 text-sm text-muted-foreground" role="status">{copy.noSelection}</p>
            ) : (
              <ol className="mt-4 grid gap-2" aria-label={copy.selected}>
                {selectedInstruments.map((instrument, index) => (
                  <li key={instrument.code} className="flex items-center justify-between gap-3 rounded-xl border border-border/60 px-3 py-2">
                    <span className="min-w-0 text-sm font-medium">{index + 1}. {instrument.displayName}</span>
                    <span className="flex shrink-0 gap-1">
                      <Button aria-label={copy.moveUp(instrument.displayName)} disabled={index === 0 || instrumentsMutation.isPending} size="icon" type="button" variant="ghost" onClick={() => moveInstrument(index, -1)}><ArrowUp className="size-4" /></Button>
                      <Button aria-label={copy.moveDown(instrument.displayName)} disabled={index === selectedInstruments.length - 1 || instrumentsMutation.isPending} size="icon" type="button" variant="ghost" onClick={() => moveInstrument(index, 1)}><ArrowDown className="size-4" /></Button>
                    </span>
                  </li>
                ))}
              </ol>
            )}
            {selectedCodes.length === 0 ? (
              <p className="mt-3 text-sm text-destructive" role="alert">{copy.minInstrument}</p>
            ) : null}
            {instrumentsMutation.error ? <p className="mt-3 text-sm text-destructive" role="alert">{problemMessage(instrumentsMutation.error, copy.instrumentsError)}</p> : null}
            <div className="mt-4 flex justify-end">
                <SubmitButton isPending={instrumentsMutation.isPending} disabled={selectedCodes.length === 0} pendingLabel={copy.savingInstruments} onClick={() => instrumentsMutation.mutate(selectedCodes)} type="button">
                {copy.saveInstruments}
              </SubmitButton>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
