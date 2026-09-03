package com.tellpal.v2.content.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tellpal.v2.content.domain.ContributorRole;

/** Read models for the database-backed contributor registry. */
public final class ContributorRegistryReadResults {

    private ContributorRegistryReadResults() { }

    public record Page(List<Item> items, int page, int size, long totalItems) {
        public Page {
            items = items == null ? List.of() : List.copyOf(items);
        }

        public long totalPages() {
            return size == 0 ? 0 : (totalItems + size - 1) / size;
        }
    }

    public record Item(
            Long contributorId,
            String displayName,
            Set<ContributorRole> roles,
            long totalUsageCount,
            Map<ContributorRole, Long> usageByRole,
            Instant updatedAt) {
        public Item {
            roles = Set.copyOf(roles);
            usageByRole = Map.copyOf(usageByRole);
        }
    }
}
