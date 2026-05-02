package com.pollnet.common.pagination;

import java.util.List;

/**
 * Cursor-paginated response envelope. Frontend pages by passing back {@code nextCursor}
 * (null when no more pages).
 */
public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
        return new CursorPage<>(items, nextCursor, nextCursor != null);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null, false);
    }
}
