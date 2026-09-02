package com.tellpal.v2.content.api;

import java.util.List;

/**
 * One page of CMS registry rows.
 */
public record AdminContentRegistryPage(List<AdminContentRegistryItem> items, int page, int size, long totalItems) {
    public AdminContentRegistryPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
