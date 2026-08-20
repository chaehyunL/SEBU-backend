package com.sebu.backend.mypage.controller;

import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MyPageController {

    private final MyPageService myPageService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(){
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(()->new IllegalArgumentException("AUTHENTICATION_REQUIRED"));

        MyPageResponse response = myPageService.getMyPage(userId);

        return ResponseEntity.ok()
                .header("Cache-Control","private, no-store")
                .body(ApiResponse.success(response));
    }
}
