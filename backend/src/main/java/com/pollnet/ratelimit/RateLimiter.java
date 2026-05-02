package com.pollnet.ratelimit;

import com.pollnet.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window counter in Redis: key = rl:{name}:{id}:{windowIndex}.
 * INCR is atomic; EXPIRE is set on first hit. When the window rolls over,
 * a new key starts at 1 and the old one expires on its own.
 *
 * Trade-off vs. a sliding window: cheaper and simpler, but allows up to
 * 2x bursts at the boundary. Acceptable for MVP.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redis;

    public void check(String name, String identifier, int max, Duration window) {
        long windowSeconds = Math.max(window.getSeconds(), 1);
        long windowIndex = Instant.now().getEpochSecond() / windowSeconds;
        String key = "rl:" + name + ":" + identifier + ":" + windowIndex;

        Long current = redis.opsForValue().increment(key);
        if (current == null) return; // Redis unavailable — fail open rather than locking users out

        if (current == 1L) {
            redis.expire(key, window);
        }
        if (current > max) {
            throw ApiException.tooManyRequests(
                    "RATE_LIMITED",
                    "Too many requests for " + name + " (limit " + max + " per " + windowSeconds + "s)"
            );
        }
    }

    /** Best-effort client IP. Trusts X-Forwarded-For if a proxy set it. */
    public static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }
}
