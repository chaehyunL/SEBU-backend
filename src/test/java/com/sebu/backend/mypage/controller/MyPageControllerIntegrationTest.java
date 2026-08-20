package com.sebu.backend.mypage.controller;

import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MyPageControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Test
    void 로그인한_사용자는_마이페이지를_조회할_수_있다() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("student@example.com")
        );

        mockMvc.perform(
                        get("/api/v1/users/me/mypage")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject(user.getId().toString())
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        "private, no-store"
                ))
                .andExpect(jsonPath("$.data.profile").exists())
                .andExpect(jsonPath("$.data.summary.bookmarkedLaboratoryCount")
                        .value(0))
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.items")
                        .isArray())
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.items")
                        .isEmpty())
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.hasNext")
                        .value(false));
    }
}
