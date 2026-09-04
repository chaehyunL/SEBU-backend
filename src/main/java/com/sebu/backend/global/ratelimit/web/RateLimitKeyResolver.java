package com.sebu.backend.global.ratelimit.web;

import com.sebu.backend.global.auth.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitKeyResolver {
    private final CurrentUserProvider currentUserProvider;

    public ResolvedKeys resolve(HttpServletRequest request) {
        return currentUserProvider.currentUserId()
            .map(userId -> new ResolvedKeys(List.of("USER:" + userId), true))
            .orElseGet(() -> new ResolvedKeys(
                List.of("SESSION:" + request.getSession(true).getId(), "IP:" + request.getRemoteAddr()),
                false
            ));
    }

    public record ResolvedKeys(List<String> values, boolean authenticated) {
    }
}
