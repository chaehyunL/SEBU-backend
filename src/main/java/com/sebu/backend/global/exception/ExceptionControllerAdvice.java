package com.sebu.backend.global.exception;

import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.mypage.moderation.IntroductionModerationException;
import com.sebu.backend.mypage.moderation.IntroductionModerationUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(IntroductionModerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntroductionModeration(
            IntroductionModerationException exception
    ) {
        return ResponseEntity
                .unprocessableEntity()
                .body(ApiResponse.failure(
                        "INTRODUCTION_POLICY_VIOLATION",
                        "자기소개가 콘텐츠 정책을 위반했습니다."
                ));
    }

    @ExceptionHandler(IntroductionModerationUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntroductionModerationUnavailable(
            IntroductionModerationUnavailableException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(
                        "INTRODUCTION_MODERATION_UNAVAILABLE",
                        "자기소개 검사 시스템을 사용할 수 없습니다."
                ));
    }
}
