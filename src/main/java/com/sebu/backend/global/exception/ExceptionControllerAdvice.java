package com.sebu.backend.global.exception;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.bookmark.exception.InvalidCursorException;
import com.sebu.backend.bookmark.exception.InvalidSizeException;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.laboratory.exception.LaboratoryNotFoundException;
import com.sebu.backend.mypage.moderation.IntroductionModerationException;
import com.sebu.backend.mypage.moderation.IntroductionModerationUnavailableException;
import com.sebu.backend.user.exception.InvalidNicknameException;
import com.sebu.backend.user.exception.NicknameAlreadyExistsException;
import com.sebu.backend.user.exception.ProfileUpdateConflictException;
import com.sebu.backend.user.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(NicknameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleNicknameAlreadyExists(
            NicknameAlreadyExistsException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        "NICKNAME_ALREADY_EXISTS",
                        "이미 사용 중인 닉네임입니다.",
                        List.of(new ApiResponse.FieldError(
                                "nickname",
                                "DUPLICATE",
                                "다른 닉네임을 입력해 주세요."
                        )),
                        null
                ));
    }

    @ExceptionHandler(InvalidNicknameException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidNickname(
            InvalidNicknameException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        "VALIDATION_ERROR",
                        "입력값을 확인해 주세요.",
                        List.of(new ApiResponse.FieldError(
                                "nickname",
                                exception.reason(),
                                exception.userMessage()
                        )),
                        null
                ));
    }

    @ExceptionHandler(ProfileUpdateConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleProfileUpdateConflict(
            ProfileUpdateConflictException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        "PROFILE_UPDATE_CONFLICT",
                        "프로필이 변경되었습니다. 최신 정보를 확인한 후 다시 시도해 주세요."
                ));
    }

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

    @ExceptionHandler(LaboratoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLaboratoryNotFound(
            LaboratoryNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(
                        "LABORATORY_NOT_FOUND",
                        "연구실을 찾을 수 없습니다."
                ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UserNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCursor(
            InvalidCursorException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        "INVALID_CURSOR",
                        "유효하지 않은 커서입니다."
                ));
    }

    @ExceptionHandler(InvalidSizeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSize(
            InvalidSizeException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        "INVALID_SIZE",
                        "조회 개수가 올바르지 않습니다."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<ApiResponse.FieldError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(fieldError ->
                                new ApiResponse.FieldError(
                                        fieldError.getField(),
                                        "INVALID_VALUE",
                                        fieldError.getDefaultMessage()
                                )
                        )
                        .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        "VALIDATION_ERROR",
                        "입력값을 확인해 주세요.",
                        fieldErrors,
                        null
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        "VALIDATION_ERROR",
                        "입력값을 확인해 주세요."
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        "INVALID_QUERY_PARAMETER",
                        "조회 조건을 확인해 주세요."
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedRuntimeException(
            RuntimeException exception
    ) {
        log.error("Unexpected API failure", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했습니다."
                ));
    }
}
