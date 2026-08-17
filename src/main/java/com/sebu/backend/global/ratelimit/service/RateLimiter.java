package com.sebu.backend.global.ratelimit.service;

import com.sebu.backend.global.ratelimit.dto.RateLimitDecision;

public interface RateLimiter {
    RateLimitDecision tryAcquire(String key);
}
