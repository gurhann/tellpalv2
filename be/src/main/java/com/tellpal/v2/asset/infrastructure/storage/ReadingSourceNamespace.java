package com.tellpal.v2.asset.infrastructure.storage;

/** Backend-owned admission paths accept conditional creation only, never signed or legacy writes. */
public final class ReadingSourceNamespace {
    private ReadingSourceNamespace() {
    }

    public static void requireLegacyWrite(String path) {
        if (path != null && ("/" + path.replace('\\', '/') + "/").contains("/reading/admitted/")) throw new IllegalArgumentException("RESERVED_READING_SOURCE_NAMESPACE");
    }
}
