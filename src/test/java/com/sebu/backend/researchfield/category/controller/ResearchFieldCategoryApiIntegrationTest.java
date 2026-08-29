package com.sebu.backend.researchfield.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResearchFieldCategoryApiIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void anonymousUserCanReadAllCategoriesInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/v1/research-field-categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.data.categories.length()").value(18))
            .andExpect(jsonPath("$.data.categories[0].code").value("AI_ML"))
            .andExpect(jsonPath("$.data.categories[0].name")
                .value("인공지능·기계학습"))
            .andExpect(jsonPath("$.data.categories[0].displayOrder").value(1))
            .andExpect(jsonPath("$.data.categories[0].description").isNotEmpty())
            .andExpect(jsonPath("$.data.categories[17].code")
                .value("POLICY_MANAGEMENT"))
            .andExpect(jsonPath("$.data.categories[17].displayOrder").value(18));
    }
}
