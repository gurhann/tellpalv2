import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import {
  useForm,
  type FieldPath,
  type FieldValues,
  type Resolver,
  type UseFormProps,
  type UseFormReturn,
  type UseFormSetError,
} from "react-hook-form";
import type { ZodTypeAny } from "zod";

import { ApiClientError } from "@/lib/http/client";
import {
  getProblemFieldErrors,
  getProblemMessage,
} from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type UseZodFormOptions<TValues extends FieldValues> = Omit<
  UseFormProps<TValues>,
  "resolver"
> & {
  schema: ZodTypeAny;
};

type MutationToastMessages<TData> = {
  loading: string;
  success: string | ((data: TData) => string);
  error?: string | ((error: unknown) => string);
};

export function useZodForm<TValues extends FieldValues>({
  schema,
  ...options
}: UseZodFormOptions<TValues>): UseFormReturn<TValues> {
  return useForm<TValues>({
    ...options,
    resolver: zodResolver(schema as never) as Resolver<TValues>,
  });
}

export function getSubmissionErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    return getProblemMessage(error.problem);
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return "Something went wrong while saving your changes.";
}

export function applyProblemDetailToForm<TValues extends FieldValues>(
  setError: UseFormSetError<TValues>,
  problem: ApiProblemDetail,
) {
  const fieldErrors = getProblemFieldErrors(problem);
  const entries = Object.entries(fieldErrors);

  if (entries.length === 0) {
    setError("root.serverError", {
      type: "server",
      message: getProblemMessage(problem),
    });
    return false;
  }

  for (const [field, message] of entries) {
    setError(field as FieldPath<TValues>, {
      type: "server",
      message,
    });
  }

  return true;
}

export function applyApiClientErrorToForm<TValues extends FieldValues>(
  setError: UseFormSetError<TValues>,
  error: unknown,
) {
  if (!(error instanceof ApiClientError)) {
    return null;
  }

  applyProblemDetailToForm(setError, error.problem);
  return error.problem;
}

export async function toastMutation<TData>(
  action: Promise<TData> | (() => Promise<TData>),
  messages: MutationToastMessages<TData>,
) {
  const toastId = `mutation-${Date.now()}-${Math.round(Math.random() * 1_000)}`;
  toast.loading(messages.loading, { id: toastId });

  try {
    const result = await (typeof action === "function" ? action() : action);
    const successMessage =
      typeof messages.success === "function"
        ? messages.success(result)
        : messages.success;

    toast.success(successMessage, { id: toastId });
    return result;
  } catch (error) {
    const errorMessage =
      typeof messages.error === "function"
        ? messages.error(error)
        : (messages.error ?? getSubmissionErrorMessage(error));

    toast.error(errorMessage, { id: toastId });
    throw error;
  }
}
