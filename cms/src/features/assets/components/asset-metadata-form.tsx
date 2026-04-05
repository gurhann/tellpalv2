import { useEffect } from "react";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { useUpdateAssetMetadata } from "@/features/assets/mutations/use-update-asset-metadata";
import {
  assetMetadataFormSchema,
  mapAssetToMetadataFormValues,
  type AssetMetadataFormValues,
} from "@/features/assets/schema/asset-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type AssetMetadataFormProps = {
  asset: AssetViewModel;
  onSuccess?: (asset: AssetViewModel) => void;
};

function isProblemMappedToField(problem: ApiProblemDetail) {
  return Object.keys(getProblemFieldErrors(problem)).length > 0;
}

export function AssetMetadataForm({
  asset,
  onSuccess,
}: AssetMetadataFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          loading: "Asset metadata kaydediliyor...",
          success: "Asset metadata kaydedildi.",
          genericError: "Asset metadata kaydedilemedi. Tekrar deneyin.",
          mimeType: "MIME türü",
          mimePlaceholder: "image/jpeg",
          mimeHelp:
            "MIME tespiti üst katmanda yapılıyorsa ve yönetici üzerine yazmamalıysa boş bırakın.",
          byteSize: "Bayt boyutu",
          optional: "İsteğe bağlı",
          byteHelp:
            "Storage metadata içindeki ham bayt sayısını kullanın. Form sıfır veya daha büyük değer kabul eder.",
          checksum: "SHA-256 checksum",
          checksumPlaceholder: "İsteğe bağlı checksum",
          checksumHelp:
            "Checksum zorunlu değildir ama alt akışlardaki bütünlük kontrollerine yardımcı olur.",
          save: "Metadata kaydet",
          saving: "Metadata kaydediliyor...",
        }
      : {
          loading: "Saving asset metadata...",
          success: "Asset metadata saved.",
          genericError: "Asset metadata could not be saved. Try again.",
          mimeType: "MIME type",
          mimePlaceholder: "image/jpeg",
          mimeHelp:
            "Leave blank if MIME detection is handled upstream and admin should not override it.",
          byteSize: "Byte size",
          optional: "Optional",
          byteHelp:
            "Use the raw byte count from storage metadata. The form accepts zero or greater.",
          checksum: "SHA-256 checksum",
          checksumPlaceholder: "Optional checksum",
          checksumHelp:
            "Checksums are optional but help downstream integrity checks and future asset refresh operations.",
          save: "Save metadata",
          saving: "Saving metadata...",
        };
  const form = useZodForm<AssetMetadataFormValues>({
    schema: assetMetadataFormSchema,
    defaultValues: mapAssetToMetadataFormValues(asset),
  });
  const updateMutation = useUpdateAssetMetadata(asset.id);
  const saveProblem =
    updateMutation.error instanceof ApiClientError
      ? updateMutation.error.problem
      : null;
  const alertProblem =
    saveProblem && !isProblemMappedToField(saveProblem) ? saveProblem : null;

  useEffect(() => {
    form.reset(mapAssetToMetadataFormValues(asset));
  }, [asset, form]);

  async function handleSubmit(values: AssetMetadataFormValues) {
    form.clearErrors();
    updateMutation.reset();

    try {
      const savedAsset = await toastMutation(
        updateMutation.mutateAsync(values),
        {
          loading: copy.loading,
          success: copy.success,
        },
      );

      form.reset(mapAssetToMetadataFormValues(savedAsset));
      onSuccess?.(savedAsset);
    } catch (error) {
      if (error instanceof ApiClientError) {
        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: copy.genericError,
      });
    }
  }

  return (
    <form
      className="grid gap-5"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      {alertProblem ? <ProblemAlert problem={alertProblem} /> : null}
      <FieldError error={form.formState.errors.root?.serverError} />

      <div className="grid gap-5">
        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="mimeType"
          >
            {copy.mimeType}
          </label>
          <Input
            id="mimeType"
            placeholder={copy.mimePlaceholder}
            {...form.register("mimeType")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">{copy.mimeHelp}</p>
          <FieldError error={form.formState.errors.mimeType} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="byteSize"
          >
            {copy.byteSize}
          </label>
          <Input
            id="byteSize"
            inputMode="numeric"
            min={0}
            placeholder={copy.optional}
            type="number"
            {...form.register("byteSize")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">{copy.byteHelp}</p>
          <FieldError error={form.formState.errors.byteSize} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="checksumSha256"
          >
            {copy.checksum}
          </label>
          <Input
            id="checksumSha256"
            placeholder={copy.checksumPlaceholder}
            {...form.register("checksumSha256")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">{copy.checksumHelp}</p>
          <FieldError error={form.formState.errors.checksumSha256} />
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
        <Button type="button" variant="outline" disabled>
          Refresh cached URL
        </Button>
        <SubmitButton
          isPending={updateMutation.isPending}
          pendingLabel={copy.saving}
        >
          {copy.save}
        </SubmitButton>
      </div>
    </form>
  );
}
