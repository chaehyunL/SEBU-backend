package com.sebu.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class InMemoryRateLimiter implements RateLimiter {
    private static final long MAXIMUM_KEYS = 100_000;

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Cache<String, FixedWindow> windows;

    @Autowired
    public InMemoryRateLimiter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InMemoryRateLimiter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.windows = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_KEYS)
            .expireAfterAccess(properties.window().multipliedBy(2))
            .build();
    }

    @Override
    public RateLimitDecision tryAcquire(String key) {
        FixedWindow window = windows.get(key, ignored -> new FixedWindow(clock.instant(), properties.window()));
        return window.tryAcquire(clock.instant(), properties);
    }

    private static final class FixedWindow {
        private Instant resetAt;
        private int requestCount;

        private FixedWindow(Instant startedAt, Duration duration) {
            resetAt = startedAt.plus(duration);
        }

        private synchronized RateLimitDecision tryAcquire(Instant now, RateLimitProperties properties) {
            if (!now.isBefore(resetAt)) {
                resetAt = now.plus(properties.window());
                requestCount = 0;
            }

            if (requestCount < properties.maxRequests()) {
                requestCount++;
                return RateLimitDecision.permit();
            }

            long remainingMillis = Duration.between(now, resetAt).toMillis();
            long retryAfterSeconds = (remainingMillis + 999) / 1_000;
            return RateLimitDecision.reject(retryAfterSeconds);
        }
    }
}
