package com.pollnet.common.pagination;

import com.pollnet.common.error.ApiException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque cursor encoding (createdAt ISO-8601 + UUID), base64url'd. The frontend treats
 * it as a black-box string. Decode failures map to a 400.
 */
public record Cursor(OffsetDateTime time, UUID id) {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public static Cursor first() {
        // "Oldest possible past" sentinel for descending feeds — newest-first means
        // we want everything *before* "now-ish"; pass max so first page covers all rows.
        return new Cursor(OffsetDateTime.now().plusYears(100), new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    public String encode() {
        String raw = time.toString() + "|" + id.toString();
        return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return first();
        try {
            String raw = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            if (sep < 0) throw new IllegalArgumentException("missing separator");
            return new Cursor(OffsetDateTime.parse(raw.substring(0, sep)),
                              UUID.fromString(raw.substring(sep + 1)));
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("INVALID_CURSOR", "Cursor is malformed");
        }
    }
}
