package com.sebu.backend.global.ratelimit.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sebu.backend.global.ratelimit.dto.RateLimitDecision;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class InMemoryRateLimiter implements RateLimiter {
    private static final long MAXIMUM_KEYS = 100_000;

    private final Clock clock;
    private final Cache<String, TokenBucket> buckets;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
        this.buckets = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_KEYS)
            .expireAfterAccess(Duration.ofHours(1))
            .build();
    }

    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        String bucketKey = policy.name() + ":" + key;
        TokenBucket bucket = buckets.get(bucketKey, ignored -> new TokenBucket(policy.capacity(), clock.instant()));
        return bucket.tryAcquire(clock.instant(), policy);
    }

    private static final class TokenBucket {
        private double tokens;
        private Instant lastRefillAt;

        private TokenBucket(int capacity, Instant createdAt) {
            tokens = capacity;
            lastRefillAt = createdAt;
        }

        private synchronized RateLimitDecision tryAcquire(Instant now, RateLimitPolicy policy) {
            double elapsedNanos = Duration.between(lastRefillAt, now).toNanos();
            double refillPerNano = (double) policy.capacity() / policy.refillPeriod().toNanos();
            tokens = Math.min(policy.capacity(), tokens + elapsedNanos * refillPerNano);
            lastRefillAt = now;

            if (tokens >= 1) {
                tokens -= 1;
                return RateLimitDecision.permit();
            }

            long retryAfterNanos = (long) Math.ceil((1 - tokens) / refillPerNano);
            long retryAfterSeconds = (retryAfterNanos + 999_999_999L) / 1_000_000_000L;
            return RateLimitDecision.reject(retryAfterSeconds);
        }
    }
}
