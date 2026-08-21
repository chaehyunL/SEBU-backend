package com.sebu.backend.global.exception;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.mypage.moderation.IntroductionModerationException;
import com.sebu.backend.mypage.moderation.IntroductionModerationUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(IntroductionModerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntroductionModeration(
            IntroductionModerationException exception
    ) {
        return ResponseEntity
                .unprocessableEntity()
                .body(ApiResponse.failure(
                        "CONTENT_POLICY_VIOLATION",
                        "입력 내용을 확인해 주세요.",
                        List.of(
                                new ApiResponse.FieldError(
                                        "introduction",
                                        "INAPPROPRIATE_CONTENT",
                                        "자기소개에 사용할 수 없는 표현이 포함되어 있습니다. 욕설이나 선정적인 표현을 수정해 주세요."
                                )
                        ),
                        null
                ));
    }
    @ExceptionHandler(IntroductionModerationUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntroductionModerationUnavailable(
            IntroductionModerationUnavailableException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(
                        "CONTENT_MODERATION_UNAVAILABLE",
                        "자기소개를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                        List.of(),
                        null
                ));
    }

    @ExceptionHandler(AccessTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessTokenInvalid(
            AccessTokenInvalidException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(
                        "ACCESS_TOKEN_INVALID",
                        "유효하지 않은 Access Token입니다."
                ));
    }
}
