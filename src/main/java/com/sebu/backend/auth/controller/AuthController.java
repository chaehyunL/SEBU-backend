package com.sebu.backend.auth.controller;

import com.sebu.backend.auth.dto.LoginResponse;
import com.sebu.backend.auth.dto.LogoutResponse;
import com.sebu.backend.auth.dto.RefreshResponse;
import com.sebu.backend.auth.dto.SejongLoginRequest;
import com.sebu.backend.auth.service.AuthService;
import com.sebu.backend.auth.service.AuthSessionService;
import com.sebu.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthSessionService authSessionService;
    private final RefreshTokenCookieFactory cookieFactory;

    @PostMapping("/sejong/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody SejongLoginRequest request) {
        AuthSessionService.LoginSession session = authService.loginWithSejong(
            request.studentId(),
            request.password()
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.create(session.refreshToken()).toString())
            .body(ApiResponse.success(LoginResponse.from(session)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken
    ) {
        AuthSessionService.RefreshSession session = authSessionService.refresh(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.create(session.refreshToken()).toString())
            .body(ApiResponse.success(RefreshResponse.from(session)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
        @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken
    ) {
        authSessionService.logout(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.delete().toString())
            .body(ApiResponse.success(new LogoutResponse("로그아웃되었습니다.")));
    }
}
