package com.tellpal.v2.category.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by category management application services.
 */
public final class CategoryManagementCommands {

    private CategoryManagementCommands() {
    }

    /**
     * Command for creating a category aggregate.
     */
    public record CreateCategoryCommand(String slug, CategoryType type, boolean premium, boolean active) {

        public CreateCategoryCommand {
            slug = requireText(slug, "Category slug must not be blank");
            type = requireCategoryType(type);
        }
    }

    /**
     * Command for updating category identity and flags.
     */
    public record UpdateCategoryCommand(Long categoryId, String slug, CategoryType type, boolean premium, boolean active) {

        public UpdateCategoryCommand {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
            slug = requireText(slug, "Category slug must not be blank");
            type = requireCategoryType(type);
        }
    }

    /**
     * Command for deactivating one category aggregate.
     */
    public record DeleteCategoryCommand(Long categoryId) {

        public DeleteCategoryCommand {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
        }
    }

    /**
     * Command for creating a category localization.
     */
    public record CreateCategoryLocalizationCommand(
            Long categoryId,
            LanguageCode languageCode,
            String name,
            String description,
            Long imageMediaId,
            LocalizationStatus status,
            Instant publishedAt) {

        public CreateCategoryLocalizationCommand {
            validateLocalizationCommand(categoryId, languageCode, name, imageMediaId, status, publishedAt);
        }
    }

    /**
     * Command for updating a category localization.
     */
    public record UpdateCategoryLocalizationCommand(
            Long categoryId,
            LanguageCode languageCode,
            String name,
            String description,
            Long imageMediaId,
            LocalizationStatus status,
            Instant publishedAt) {

        public UpdateCategoryLocalizationCommand {
            validateLocalizationCommand(categoryId, languageCode, name, imageMediaId, status, publishedAt);
        }
    }

    /**
     * Command for adding curated content to a category.
     */
    public record AddCategoryContentCommand(Long categoryId, LanguageCode languageCode, Long contentId, int displayOrder) {

        public AddCategoryContentCommand {
            validateCategoryContentCommand(categoryId, languageCode, contentId, displayOrder);
        }
    }

    /**
     * Command for changing the order of curated category content.
     */
    public record UpdateCategoryContentOrderCommand(
            Long categoryId,
            LanguageCode languageCode,
            Long contentId,
            int displayOrder) {

        public UpdateCategoryContentOrderCommand {
            validateCategoryContentCommand(categoryId, languageCode, contentId, displayOrder);
        }
    }

    /**
     * Command for replacing the order of all curated content in one language lane.
     */
    public record ReorderCategoryContentsCommand(
            Long categoryId,
            LanguageCode languageCode,
            List<CategoryContentOrderAssignment> assignments) {

        public ReorderCategoryContentsCommand {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            if (assignments == null || assignments.isEmpty()) {
                throw new IllegalArgumentException("Curated content reorder assignments must not be empty");
            }
            Set<Long> contentIds = assignments.stream()
                    .map(CategoryContentOrderAssignment::contentId)
                    .collect(Collectors.toSet());
            if (contentIds.size() != assignments.size()) {
                throw new IllegalArgumentException("Curated content reorder must not contain duplicate content ids");
            }
            Set<Integer> displayOrders = assignments.stream()
                    .map(CategoryContentOrderAssignment::displayOrder)
                    .collect(Collectors.toSet());
            if (displayOrders.size() != assignments.size()) {
                throw new IllegalArgumentException("Curated content reorder must not contain duplicate display orders");
            }
            assignments = List.copyOf(assignments);
        }
    }

    /**
     * Assignment item used by curated content reorder commands.
     */
    public record CategoryContentOrderAssignment(Long contentId, int displayOrder) {

        public CategoryContentOrderAssignment {
            requirePositiveId(contentId, "Content ID must be positive");
            if (displayOrder < 0) {
                throw new IllegalArgumentException("Category display order must not be negative");
            }
        }
    }

    /**
     * Command for removing curated content from a category.
     */
    public record RemoveCategoryContentCommand(Long categoryId, LanguageCode languageCode, Long contentId) {

        public RemoveCategoryContentCommand {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
        }
    }

    private static void validateLocalizationCommand(
            Long categoryId,
            LanguageCode languageCode,
            String name,
            Long imageMediaId,
            LocalizationStatus status,
            Instant publishedAt) {
        requirePositiveId(categoryId, "Category ID must be positive");
        requireLanguageCode(languageCode);
        requireText(name, "Category localization name must not be blank");
        normalizePositiveId(imageMediaId, "Category image media ID must be positive");
        requireLocalizationStatus(status, publishedAt);
    }

    private static void validateCategoryContentCommand(
            Long categoryId,
            LanguageCode languageCode,
            Long contentId,
            int displayOrder) {
        requirePositiveId(categoryId, "Category ID must be positive");
        requireLanguageCode(languageCode);
        requirePositiveId(contentId, "Content ID must be positive");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Category display order must not be negative");
        }
    }

    private static CategoryType requireCategoryType(CategoryType type) {
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        return type;
    }

    private static void requireLocalizationStatus(LocalizationStatus status, Instant publishedAt) {
        if (status == null) {
            throw new IllegalArgumentException("Category localization status must not be null");
        }
        if (status == LocalizationStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published category localization must include a publish timestamp");
        }
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
