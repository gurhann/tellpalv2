import type {
  AdminCategoryResponse,
  CategoryType,
} from "@/features/categories/api/category-admin";
import { categoryTypeSchema } from "@/features/categories/api/category-admin";
import type { CategorySummaryViewModel } from "@/features/categories/model/category-view-model";
import { z } from "zod";

export const categoryTypeOptions: Array<{
  value: CategoryType;
  label: string;
}> = [
  { value: "STORY", label: "Story" },
  { value: "AUDIO_STORY", label: "Audio Story" },
  { value: "MEDITATION", label: "Meditation" },
  { value: "LULLABY", label: "Lullaby" },
];

export const categoryFormSchema = z.object({
  type: categoryTypeSchema,
  slug: z.string().trim().min(1, "Slug is required."),
  premium: z.boolean(),
  active: z.boolean(),
});

export type CategoryFormValues = z.infer<typeof categoryFormSchema>;

export function getCreateCategoryFormDefaults(): CategoryFormValues {
  return {
    type: "STORY",
    slug: "",
    premium: false,
    active: true,
  };
}

export function mapCategoryReadToFormValues(
  category: CategorySummaryViewModel,
): CategoryFormValues {
  return {
    type: category.type,
    slug: category.slug,
    premium: category.premium,
    active: category.active,
  };
}

export function mapCategoryResponseToFormValues(
  category: AdminCategoryResponse,
): CategoryFormValues {
  return {
    type: category.type,
    slug: category.slug,
    premium: category.premium,
    active: category.active,
  };
}
