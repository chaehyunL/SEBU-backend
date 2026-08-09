package com.sebu.backend.ratelimit;

import com.sebu.backend.auth.CurrentUserProvider;
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
