import { useMutation } from "@tanstack/react-query";
import { ArrowRight, KeyRound, ShieldCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  applyProblemDetailToForm,
  useZodForm,
} from "@/components/forms/form-utils";
import { adminAuthApi } from "@/features/auth/api/admin-auth";
import {
  loginSchema,
  type LoginSchemaValues,
} from "@/features/auth/schema/login-schema";
import { useAuth } from "@/features/auth/providers/use-auth";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type LoginFormProps = {
  targetPath: string;
};

function mapLoginProblem(
  problem: ApiProblemDetail,
  t: ReturnType<typeof useI18n>["t"],
) {
  if (problem.status === 401) {
    return {
      ...problem,
      title: t("auth.error.incorrectCredentials"),
      detail: t("auth.error.incorrectCredentialsDetail"),
    };
  }

  if (problem.status === 403) {
    return {
      ...problem,
      title: t("auth.error.accountDisabled"),
      detail: t("auth.error.accountDisabledDetail"),
    };
  }

  return problem;
}

export function LoginForm({ targetPath }: LoginFormProps) {
  const navigate = useNavigate();
  const auth = useAuth();
  const { t } = useI18n();
  const form = useZodForm<LoginSchemaValues>({
    schema: loginSchema,
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const loginMutation = useMutation({
    mutationFn: adminAuthApi.login,
  });

  const formProblem =
    loginMutation.error instanceof ApiClientError
      ? mapLoginProblem(loginMutation.error.problem, t)
      : null;

  async function handleSubmit(values: LoginSchemaValues) {
    form.clearErrors("root.serverError");
    loginMutation.reset();

    try {
      const session = await loginMutation.mutateAsync(values);
      auth.setSession(session);
      navigate(targetPath, { replace: true });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (Object.keys(getProblemFieldErrors(error.problem)).length > 0) {
          applyProblemDetailToForm(form.setError, error.problem);
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: t("auth.error.signInFailed"),
      });
    }
  }

  return (
    <Card className="border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
      <CardHeader className="gap-4">
        <div className="inline-flex size-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
          <KeyRound className="size-5" />
        </div>
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
            {t("auth.login.adminAccess")}
          </p>
          <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            {t("auth.login.formTitle")}
          </h1>
          <CardDescription className="text-sm leading-6 sm:text-base">
            {t("auth.login.formDescription")}
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        {formProblem ? <ProblemAlert problem={formProblem} /> : null}
        <FieldError error={form.formState.errors.root?.serverError} />

        <form
          className="space-y-5"
          noValidate
          onSubmit={form.handleSubmit(handleSubmit)}
        >
          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="username"
            >
              {t("auth.login.username")}
            </label>
            <Input
              autoComplete="username"
              id="username"
              placeholder="bootstrap-admin"
              {...form.register("username")}
            />
            <FieldError error={form.formState.errors.username} />
          </div>

          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="password"
            >
              {t("auth.login.password")}
            </label>
            <Input
              autoComplete="current-password"
              id="password"
              placeholder={t("auth.login.passwordPlaceholder")}
              type="password"
              {...form.register("password")}
            />
            <FieldError error={form.formState.errors.password} />
          </div>

          <div className="rounded-2xl border border-border/70 bg-muted/40 p-4 text-sm text-muted-foreground">
            <p className="font-medium text-foreground">
              {t("auth.login.sessionBehavior")}
            </p>
            <ul className="mt-3 space-y-2">
              <li className="flex items-start gap-2">
                <ShieldCheck className="mt-0.5 size-4 shrink-0 text-primary" />
                {t("auth.login.sessionBehavior.storage")}
              </li>
              <li className="flex items-start gap-2">
                <ArrowRight className="mt-0.5 size-4 shrink-0 text-primary" />
                {t("auth.login.sessionBehavior.target", {
                  targetPath,
                })}
              </li>
            </ul>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <SubmitButton
              className="w-full sm:w-auto"
              isPending={loginMutation.isPending}
              pendingLabel={t("auth.login.pending")}
            >
              {t("auth.login.submit")}
            </SubmitButton>

            <Button
              asChild
              className="w-full sm:w-auto"
              size="lg"
              variant="ghost"
            >
              <a
                href="http://localhost:8080/swagger-ui/index.html"
                rel="noreferrer"
                target="_blank"
              >
                {t("auth.login.reviewAdminApi")}
              </a>
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
