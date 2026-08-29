package com.sebu.backend.mypage.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.mypage.dto.ProfileResponse;
import com.sebu.backend.mypage.dto.ProfileUpdateRequest;
import com.sebu.backend.mypage.service.MyPageService;
import com.sebu.backend.mypage.service.ProfileService;
import com.sebu.backend.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MyPageController {

    private final MyPageService myPageService;
    private final CurrentUserProvider currentUserProvider;
    private final ProfileService profileService;
    private final AccountService accountService;


    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(){
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        MyPageResponse response = myPageService.getMyPage(userId);

        return ResponseEntity.ok()
                .header("Cache-Control","private, no-store")
                .body(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        ProfileResponse response =
                profileService.updateProfile(userId, request);

        return ResponseEntity.ok()
                .header("Cache-Control", "private, no-store")
                .body(ApiResponse.success(response));
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw() {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        accountService.withdraw(userId);

        return ResponseEntity.noContent().build();
    }
}
