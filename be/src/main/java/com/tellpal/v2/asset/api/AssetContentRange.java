package com.tellpal.v2.asset.api;

/**
 * Parsed single HTTP byte range requested for backend media streaming.
 */
public record AssetContentRange(
        Long startInclusive,
        Long endInclusive,
        Long suffixLength) {

    public AssetContentRange {
        if (startInclusive == null && suffixLength == null) {
            throw new IllegalArgumentException("Asset content range must include a start or suffix length");
        }
        if (startInclusive != null && startInclusive < 0) {
            throw new IllegalArgumentException("Asset content range start must not be negative");
        }
        if (endInclusive != null && endInclusive < 0) {
            throw new IllegalArgumentException("Asset content range end must not be negative");
        }
        if (startInclusive != null && endInclusive != null && endInclusive < startInclusive) {
            throw new IllegalArgumentException("Asset content range end must not be before start");
        }
        if (suffixLength != null && suffixLength <= 0) {
            throw new IllegalArgumentException("Asset content suffix length must be positive");
        }
        if (suffixLength != null && (startInclusive != null || endInclusive != null)) {
            throw new IllegalArgumentException("Asset content range cannot mix suffix and explicit positions");
        }
    }

    public static AssetContentRange fromStart(long startInclusive, Long endInclusive) {
        return new AssetContentRange(startInclusive, endInclusive, null);
    }

    public static AssetContentRange suffix(long suffixLength) {
        return new AssetContentRange(null, null, suffixLength);
    }
}
