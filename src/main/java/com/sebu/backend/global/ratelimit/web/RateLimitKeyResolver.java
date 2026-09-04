package com.sebu.backend.global.ratelimit.web;

import com.sebu.backend.global.auth.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
            .orElseGet(() -> anonymousKeys(request));
    }

    private ResolvedKeys anonymousKeys(HttpServletRequest request) {
        String ipKey = "IP:" + request.getRemoteAddr();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new ResolvedKeys(List.of(ipKey), false);
        }
        return new ResolvedKeys(List.of(ipKey, "SESSION:" + session.getId()), false);
    }

    public record ResolvedKeys(List<String> values, boolean authenticated) {
    }
}
