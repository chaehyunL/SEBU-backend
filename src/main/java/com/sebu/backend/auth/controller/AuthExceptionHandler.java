package com.sebu.backend.auth.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.auth.exception.InvalidLoginRequestException;
import com.sebu.backend.auth.exception.RefreshTokenInvalidException;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthController.class, MeController.class})
public class AuthExceptionHandler {
    @ExceptionHandler({InvalidLoginRequestException.class, MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidLoginRequest(Exception exception) {
        return failure(
            HttpStatus.BAD_REQUEST,
            "INVALID_LOGIN_REQUEST",
            "학번과 비밀번호를 모두 입력해주세요."
        );
    }

    @ExceptionHandler(SejongAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSejongAuthentication(SejongAuthenticationException exception) {
        if (exception.getReason() == SejongAuthenticationException.Reason.AUTHENTICATION_FAILED) {
            return failure(
                HttpStatus.UNAUTHORIZED,
                "SEJONG_AUTH_FAILED",
                "학번 또는 비밀번호를 확인해주세요."
            );
        }
        return failure(
            HttpStatus.BAD_GATEWAY,
            "SEJONG_SYSTEM_UNAVAILABLE",
            "세종대학교 시스템에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."
        );
    }

    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(RefreshTokenInvalidException exception) {
        return failure(
            HttpStatus.UNAUTHORIZED,
            "REFRESH_TOKEN_INVALID",
            "로그인이 만료되었습니다. 다시 로그인해주세요."
        );
    }

    @ExceptionHandler(AccessTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAccessToken(AccessTokenInvalidException exception) {
        return failure(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_INVALID",
            "유효하지 않은 인증 토큰입니다."
        );
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message));
    }
}
