package com.sebu.backend.laboratory;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LaboratoryRatingControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CurrentUserProvider currentUserProvider;

    @Test
    void returnsReviewCountSortedLaboratoriesWithPagination() throws Exception {
        when(currentUserProvider.currentUserId())
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/laboratories")
                                .param("sort", "REVIEW_COUNT_DESC")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.laboratories").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.hasNext").isBoolean())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void keepsExistingLaboratoryListApiWorkingWithoutSort() throws Exception {
        when(currentUserProvider.currentUserId())
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/laboratories")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.laboratories").isArray())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
