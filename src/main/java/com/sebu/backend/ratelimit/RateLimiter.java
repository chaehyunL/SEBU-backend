package com.sebu.backend.ratelimit;

public interface RateLimiter {
    RateLimitDecision tryAcquire(String key);
}
