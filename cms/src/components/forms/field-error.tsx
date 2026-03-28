import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

type FieldErrorLike = {
  message?: string;
} | null;

type FieldErrorProps = {
  error?: FieldErrorLike | string;
  id?: string;
  className?: string;
  prefix?: ReactNode;
};

function getFieldErrorMessage(error?: FieldErrorLike | string) {
  if (typeof error === "string") {
    return error.trim().length > 0 ? error : null;
  }

  const message = error?.message?.trim();
  return message && message.length > 0 ? message : null;
}

export function FieldError({ error, id, className, prefix }: FieldErrorProps) {
  const message = getFieldErrorMessage(error);

  if (!message) {
    return null;
  }

  return (
    <p
      className={cn("text-sm font-medium text-destructive", className)}
      id={id}
      role="alert"
    >
      {prefix ? <span className="mr-1">{prefix}</span> : null}
      {message}
    </p>
  );
}
