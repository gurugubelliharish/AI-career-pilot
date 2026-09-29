package com.aicareer.jobradar.module.discovery.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Persists Workable pagination cursor and rate-limit backoff state in Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkableCursorStore {

    private static final String CURSOR_KEY  = "workable:cursor:next_page_token";
    private static final String BACKOFF_KEY = "workable:rate_limited";

    private final StringRedisTemplate stringRedisTemplate;

    public String getCursor() {
        return stringRedisTemplate.opsForValue().get(CURSOR_KEY);
    }

    public void saveCursor(String token) {
        stringRedisTemplate.opsForValue().set(CURSOR_KEY, token);
    }

    public void resetCursor() {
        stringRedisTemplate.delete(CURSOR_KEY);
        log.info("Workable cursor reset — next scan restarts from page 1");
    }

    /** Mark Workable as rate-limited for the given duration so scrapers skip it. */
    public void markRateLimited(Duration backoff) {
        stringRedisTemplate.opsForValue().set(BACKOFF_KEY, "1", backoff);
        log.warn("Workable marked as rate-limited for {}m — skipping until backoff clears", backoff.toMinutes());
    }

    public boolean isRateLimited() {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BACKOFF_KEY));
    }
}
