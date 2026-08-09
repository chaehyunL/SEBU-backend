package com.sebu.backend.api;

import com.sebu.backend.auth.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmptyLaboratoryApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUserProvider;

    @Test
    void returnsOkWithEmptyArray() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/laboratories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.laboratories").isEmpty())
            .andExpect(jsonPath("$.error").doesNotExist());
    }
}
