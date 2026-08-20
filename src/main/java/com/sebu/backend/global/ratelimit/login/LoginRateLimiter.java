package com.sebu.backend.global.ratelimit.login;

import com.sebu.backend.global.ratelimit.config.RateLimitProperties;
import com.sebu.backend.global.ratelimit.dto.RateLimitDecision;
import com.sebu.backend.global.ratelimit.service.InMemoryRateLimiter;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    private final InMemoryRateLimiter delegate;

    public LoginRateLimiter(LoginRateLimitProperties properties) {
        this.delegate = new InMemoryRateLimiter(
            new RateLimitProperties(properties.maxRequests(), properties.window())
        );
    }

    public RateLimitDecision tryAcquire(String clientIp) {
        return delegate.tryAcquire("LOGIN_IP:" + clientIp);
    }
}
