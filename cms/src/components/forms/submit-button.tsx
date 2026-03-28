import { LoaderCircle } from "lucide-react";
import type { ComponentProps } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type SubmitButtonProps = ComponentProps<typeof Button> & {
  isPending?: boolean;
  pendingLabel?: string;
};

export function SubmitButton({
  children,
  disabled,
  isPending = false,
  pendingLabel = "Saving...",
  className,
  type = "submit",
  ...props
}: SubmitButtonProps) {
  return (
    <Button
      aria-busy={isPending}
      className={cn("min-w-32", className)}
      disabled={disabled || isPending}
      type={type}
      {...props}
    >
      {isPending ? <LoaderCircle className="size-4 animate-spin" /> : null}
      <span>{isPending ? pendingLabel : children}</span>
    </Button>
  );
}
