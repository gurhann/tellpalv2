import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { FormProvider, useFormContext } from "react-hook-form";
import { vi } from "vitest";
import { z } from "zod";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { FormSection } from "@/components/forms/form-section";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { SubmitButton } from "@/components/forms/submit-button";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

const toastMocks = vi.hoisted(() => ({
  loading: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: toastMocks,
}));

function ValidationHarness({
  onSubmit,
}: {
  onSubmit: (values: { title: string }) => void | Promise<void>;
}) {
  const form = useZodForm({
    schema: z.object({
      title: z.string().trim().min(1, "Title is required"),
    }),
    defaultValues: {
      title: "",
    },
  });

  const error = form.formState.errors.title;

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <label htmlFor="title-input">Title</label>
      <input id="title-input" {...form.register("title")} />
      <FieldError error={error} />
      <SubmitButton>Save</SubmitButton>
    </form>
  );
}

function RootErrorHarness() {
  const form = useZodForm({
    schema: z.object({
      title: z.string().trim().min(1),
    }),
    defaultValues: {
      title: "",
    },
  });

  return (
    <FormProvider {...form}>
      <form>
        <RootErrorReader />
        <button
          type="button"
          onClick={() =>
            applyProblemDetailToForm(form.setError, {
              type: "about:blank",
              title: "Save failed",
              status: 409,
              detail: "The current record is stale.",
            })
          }
        >
          Apply server error
        </button>
      </form>
    </FormProvider>
  );
}

function RootErrorReader() {
  const form = useFormContext<{ title: string }>();

  return <FieldError error={form.formState.errors.root?.serverError} />;
}

describe("form primitives", () => {
  it("uses the shared zod form helper for client validation", async () => {
    const onSubmit = vi.fn();

    render(<ValidationHarness onSubmit={onSubmit} />);

    fireEvent.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText("Title is required")).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("maps backend field errors into RHF setError calls", () => {
    const setError = vi.fn();

    const result = applyProblemDetailToForm(setError, {
      type: "about:blank",
      title: "Validation failed",
      status: 400,
      detail: "Request validation failed",
      fieldErrors: {
        title: "title is required",
      },
    });

    expect(result).toBe(true);
    expect(setError).toHaveBeenCalledWith("title", {
      type: "server",
      message: "title is required",
    });
  });

  it("maps backend root errors when there are no field errors", async () => {
    render(<RootErrorHarness />);

    fireEvent.click(
      screen.getByRole("button", { name: /apply server error/i }),
    );

    expect(
      await screen.findByText("The current record is stale."),
    ).toBeInTheDocument();
  });

  it("standardizes submit button loading and disabled states", () => {
    render(
      <SubmitButton isPending pendingLabel="Saving changes">
        Save
      </SubmitButton>,
    );

    const button = screen.getByRole("button", { name: /saving changes/i });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
  });

  it("renders the shared section, problem, and empty-state feedback components", () => {
    const problem: ApiProblemDetail = {
      type: "about:blank",
      title: "Validation failed",
      status: 400,
      detail: "Request validation failed",
      requestId: "req-42",
      fieldErrors: {
        title: "title is required",
      },
    };

    render(
      <div>
        <FormSection
          title="Metadata"
          description="Shared editor section layout."
          footer={<span>Footer actions</span>}
        >
          <ProblemAlert problem={problem} />
        </FormSection>

        <EmptyState
          title="No records yet"
          description="Create your first item from the toolbar."
        />
      </div>,
    );

    expect(
      screen.getByRole("heading", { name: /metadata/i }),
    ).toBeInTheDocument();
    expect(screen.getByText("Footer actions")).toBeInTheDocument();
    expect(screen.getByText("Request ID req-42")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /no records yet/i }),
    ).toBeInTheDocument();
  });

  it("wraps mutation promises with a consistent toast lifecycle", async () => {
    toastMocks.loading.mockClear();
    toastMocks.success.mockClear();
    toastMocks.error.mockClear();

    await expect(
      toastMutation(async () => "created", {
        loading: "Saving content",
        success: (value) => `Saved ${value}`,
      }),
    ).resolves.toBe("created");

    expect(toastMocks.loading).toHaveBeenCalledWith(
      "Saving content",
      expect.objectContaining({
        id: expect.stringContaining("mutation-"),
      }),
    );
    expect(toastMocks.success).toHaveBeenCalledWith(
      "Saved created",
      expect.objectContaining({
        id: expect.stringContaining("mutation-"),
      }),
    );
  });

  it("surfaces API failures through the shared toast error message", async () => {
    toastMocks.loading.mockClear();
    toastMocks.success.mockClear();
    toastMocks.error.mockClear();

    const response = new Response(
      JSON.stringify({
        type: "about:blank",
        title: "Conflict",
        status: 409,
        detail: "The item already exists.",
      }),
      {
        status: 409,
        statusText: "Conflict",
        headers: {
          "Content-Type": "application/json",
        },
      },
    );

    Object.defineProperty(response, "url", {
      configurable: true,
      value: "http://api.test/items",
    });

    await expect(
      toastMutation(
        async () => {
          throw new ApiClientError(
            {
              type: "about:blank",
              title: "Conflict",
              status: 409,
              detail: "The item already exists.",
            },
            response,
          );
        },
        {
          loading: "Saving content",
          success: "Saved",
        },
      ),
    ).rejects.toBeInstanceOf(ApiClientError);

    await waitFor(() => {
      expect(toastMocks.error).toHaveBeenCalledWith(
        "The item already exists.",
        expect.objectContaining({
          id: expect.stringContaining("mutation-"),
        }),
      );
    });
  });
});
