package com.tellpal.v2.content.web.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tellpal.v2.content.application.ContributorRegistryReadResults;
import com.tellpal.v2.content.domain.ContributorRole;

/** HTTP response models for the paged contributor registry. */
public final class AdminContributorRegistryResponse {

    private AdminContributorRegistryResponse() { }

    public record Page(List<Item> items, int page, int size, long totalItems, long totalPages) {
        static Page from(ContributorRegistryReadResults.Page page) {
            return new Page(page.items().stream().map(Item::from).toList(), page.page(), page.size(),
                    page.totalItems(), page.totalPages());
        }
    }

    public record Item(Long contributorId, String displayName, Set<ContributorRole> roles,
            long totalUsageCount, Map<ContributorRole, Long> usageByRole, Instant updatedAt) {
        static Item from(ContributorRegistryReadResults.Item item) {
            return new Item(item.contributorId(), item.displayName(), item.roles(), item.totalUsageCount(),
                    item.usageByRole(), item.updatedAt());
        }
    }
}
