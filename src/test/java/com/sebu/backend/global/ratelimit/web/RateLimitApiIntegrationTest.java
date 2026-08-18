package com.sebu.backend.global.ratelimit.web;

import com.sebu.backend.global.auth.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.rate-limit.max-requests=2",
    "app.rate-limit.window=1m"
})
@AutoConfigureMockMvc
class RateLimitApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUserProvider;

    @Test
    void returns429WithRetryAfterWhenAnonymousIpExceedsLimit() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(requestFrom("192.0.2.10")).andExpect(status().isOk());
        mockMvc.perform(requestFrom("192.0.2.10")).andExpect(status().isOk());
        mockMvc.perform(requestFrom("192.0.2.10"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "60"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.error.message").value("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."));

        mockMvc.perform(requestFrom("192.0.2.11")).andExpect(status().isOk());
    }

    @Test
    void authenticatedUsersHaveIndependentLimitsRegardlessOfIp() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(100L));
        mockMvc.perform(requestFrom("192.0.2.20")).andExpect(status().isOk());
        mockMvc.perform(requestFrom("192.0.2.20")).andExpect(status().isOk());
        mockMvc.perform(requestFrom("192.0.2.20")).andExpect(status().isTooManyRequests());

        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(200L));
        mockMvc.perform(requestFrom("192.0.2.20")).andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestFrom(String ip) {
        return get("/api/v1/laboratories").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
    }
}
