package com.sebu.backend.global.ratelimit.web;

import com.sebu.backend.global.auth.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitKeyResolver {
    private final CurrentUserProvider currentUserProvider;

    public String resolve(HttpServletRequest request) {
        return currentUserProvider.currentUserId()
            .map(userId -> "USER:" + userId)
            .orElseGet(() -> "IP:" + request.getRemoteAddr());
    }
}
