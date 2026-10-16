package com.tellpal.v2.asset.domain.reading;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Collection;
import java.util.TreeMap;

/** P1 ASCII canonical JSON and domain-separated SHA-256 framing; no host-default serialization. */
public final class ReadingIdentity {
    private ReadingIdentity() { }
    public static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String hash(String domain, Object value) {
        return sha((domain + "\n" + canonical(value)).getBytes(StandardCharsets.UTF_8));
    }
    public static String canonical(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return string(s);
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) return value.toString();
        if (value instanceof Enum<?> e) return string(e.name());
        if (value instanceof Map<?, ?> m) {
            var sorted = new TreeMap<String, Object>();
            m.forEach((k,v) -> sorted.put((String) k,v));
            return "{" + sorted.entrySet().stream().map(e -> string(e.getKey()) + ":" + canonical(e.getValue())).collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (value instanceof Collection<?> c) return "[" + c.stream().map(ReadingIdentity::canonical).collect(java.util.stream.Collectors.joining(",")) + "]";
        if (value.getClass().isRecord()) {
            var fields = new TreeMap<String, Object>();
            for (var field : value.getClass().getRecordComponents()) {
                try { fields.put(field.getName(), field.getAccessor().invoke(value)); }
                catch (ReflectiveOperationException e) { throw new IllegalArgumentException(e); }
            }
            return canonical(fields);
        }
        throw new IllegalArgumentException("Unsupported canonical value: " + value.getClass());
    }
    private static String string(String value) {
        var result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c > 127) throw new IllegalArgumentException("Non-ASCII identity value");
            switch (c) {
                case '"' -> result.append("\\\""); case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b"); case '\f' -> result.append("\\f"); case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r"); case '\t' -> result.append("\\t");
                default -> { if (c < 32 || c == 127) result.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c)); else result.append(c); }
            }
        }
        return result.append('"').toString();
    }
}
