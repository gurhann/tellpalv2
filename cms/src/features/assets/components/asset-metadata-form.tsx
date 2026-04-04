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
          loading: "Saving asset metadata...",
          success: "Asset metadata saved.",
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
        message: "Asset metadata could not be saved. Try again.",
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
            MIME type
          </label>
          <Input
            id="mimeType"
            placeholder="image/jpeg"
            {...form.register("mimeType")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">
            Leave blank if MIME detection is handled upstream and admin should
            not override it.
          </p>
          <FieldError error={form.formState.errors.mimeType} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="byteSize"
          >
            Byte size
          </label>
          <Input
            id="byteSize"
            inputMode="numeric"
            min={0}
            placeholder="Optional"
            type="number"
            {...form.register("byteSize")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">
            Use the raw byte count from storage metadata. The form accepts zero
            or greater.
          </p>
          <FieldError error={form.formState.errors.byteSize} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="checksumSha256"
          >
            SHA-256 checksum
          </label>
          <Input
            id="checksumSha256"
            placeholder="Optional checksum"
            {...form.register("checksumSha256")}
            disabled={updateMutation.isPending}
          />
          <p className="text-sm text-muted-foreground">
            Checksums are optional but help downstream integrity checks and
            future asset refresh operations.
          </p>
          <FieldError error={form.formState.errors.checksumSha256} />
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
        <Button type="button" variant="outline" disabled>
          Refresh cached URL
        </Button>
        <SubmitButton
          isPending={updateMutation.isPending}
          pendingLabel="Saving metadata..."
        >
          Save metadata
        </SubmitButton>
      </div>
    </form>
  );
}
