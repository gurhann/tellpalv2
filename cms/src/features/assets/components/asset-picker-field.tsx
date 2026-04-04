import { Search, Trash2 } from "lucide-react";
import { useState } from "react";
import type { FieldError as ReactHookFormFieldError } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { AssetMediaType } from "@/features/assets/api/asset-admin";
import { AssetPickerDialog } from "@/features/assets/components/asset-picker-dialog";

type AssetPickerFieldProps = {
  id: string;
  label: string;
  value: number | null;
  onChange: (value: number | null) => void;
  mediaType: AssetMediaType;
  error?: ReactHookFormFieldError;
  description?: string;
  disabled?: boolean;
  placeholder?: string;
  pickerTitle: string;
  pickerDescription: string;
};

export function AssetPickerField({
  id,
  label,
  value,
  onChange,
  mediaType,
  error,
  description,
  disabled = false,
  placeholder = "Optional",
  pickerTitle,
  pickerDescription,
}: AssetPickerFieldProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-foreground" htmlFor={id}>
        {label}
      </label>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          id={id}
          inputMode="numeric"
          placeholder={placeholder}
          type="number"
          value={value ?? ""}
          onChange={(event) => {
            const nextValue = event.target.value;
            onChange(nextValue.trim().length === 0 ? null : Number(nextValue));
          }}
          disabled={disabled}
        />

        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => setIsDialogOpen(true)}
            disabled={disabled}
          >
            <Search className="size-4" />
            Browse
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => onChange(null)}
            disabled={disabled || value === null}
          >
            <Trash2 className="size-4" />
            Clear
          </Button>
        </div>
      </div>

      {description ? (
        <p className="text-sm text-muted-foreground">{description}</p>
      ) : null}
      {value !== null ? (
        <p className="text-xs text-muted-foreground">
          Selected asset #{value}. Picker results stay filtered to {mediaType}{" "}
          assets.
        </p>
      ) : null}
      <FieldError error={error} />

      {isDialogOpen ? (
        <AssetPickerDialog
          description={pickerDescription}
          mediaType={mediaType}
          open={isDialogOpen}
          selectedAssetId={value}
          title={pickerTitle}
          onOpenChange={setIsDialogOpen}
          onSelectAsset={(asset) => onChange(asset.id)}
        />
      ) : null}
    </div>
  );
}
