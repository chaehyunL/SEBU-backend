package com.sebu.backend.auth.controller;

import com.sebu.backend.auth.dto.LoginResponse;
import com.sebu.backend.auth.dto.LogoutResponse;
import com.sebu.backend.auth.dto.RefreshResponse;
import com.sebu.backend.auth.dto.SejongLoginRequest;
import com.sebu.backend.auth.service.AuthService;
import com.sebu.backend.auth.service.AuthSessionService;
import com.sebu.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증", description = "로그인, 토큰 재발급 및 로그아웃 API")
public class AuthController {
    private final AuthService authService;
    private final AuthSessionService authSessionService;
    private final RefreshTokenCookieFactory cookieFactory;

    @Operation(summary = "세종대학교 로그인", description = "세종대학교 포털 계정으로 로그인하고 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            ref = "#/components/responses/Unauthorized"
    )
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

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰 쿠키를 사용해 액세스 토큰과 리프레시 토큰을 재발급합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            ref = "#/components/responses/Unauthorized"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken
    ) {
        AuthSessionService.RefreshSession session = authSessionService.refresh(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.create(session.refreshToken()).toString())
            .body(ApiResponse.success(RefreshResponse.from(session)));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰 세션을 종료하고 리프레시 토큰 쿠키를 삭제합니다.")
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
