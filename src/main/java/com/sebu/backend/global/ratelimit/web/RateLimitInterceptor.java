package com.sebu.backend.global.ratelimit.web;

import com.sebu.backend.global.ratelimit.dto.RateLimitDecision;
import com.sebu.backend.global.ratelimit.service.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";
    static final String ERROR_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";

    private final RateLimiter rateLimiter;
    private final RateLimitKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }

        RateLimitDecision decision = rateLimiter.tryAcquire(keyResolver.resolve(request));
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ERROR_CODE, ERROR_MESSAGE));
        return false;
    }
}
