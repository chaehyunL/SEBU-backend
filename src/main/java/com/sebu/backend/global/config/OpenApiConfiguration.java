package com.sebu.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private static final String ERROR_SCHEMA_NAME = "ErrorApiResponse";
    private static final String BAD_REQUEST_RESPONSE = "BadRequest";
    private static final String UNAUTHORIZED_RESPONSE = "Unauthorized";
    private static final String FORBIDDEN_RESPONSE = "Forbidden";
    private static final String NOT_FOUND_RESPONSE = "NotFound";
    private static final String CONFLICT_RESPONSE = "Conflict";

    @Bean
    OpenAPI sebuOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SEBU API")
                        .description("학부 연구실 검색 서비스 API 명세")
                        .version("v1"))
                .components(openApiComponents());
    }

    @Bean
    OperationCustomizer commonErrorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            MethodParameter[] parameters = handlerMethod.getMethodParameters();
            boolean hasPathVariable = hasParameterAnnotation(
                    parameters,
                    PathVariable.class
            );
            if (hasParameterAnnotation(parameters, RequestBody.class)
                    || hasParameterAnnotation(parameters, RequestParam.class)
                    || hasPathVariable) {
                addResponseIfAbsent(responses, "400", BAD_REQUEST_RESPONSE);
            }
            if (requiresBearerAuth(handlerMethod)) {
                addResponseIfAbsent(responses, "401", UNAUTHORIZED_RESPONSE);
            }
            if (hasPathVariable) {
                addResponseIfAbsent(responses, "404", NOT_FOUND_RESPONSE);
            }

            return operation;
        };
    }

    private Components openApiComponents() {
        return new Components()
                .addSchemas(ERROR_SCHEMA_NAME, errorResponseSchema())
                .addResponses(
                        BAD_REQUEST_RESPONSE,
                        errorResponse("요청 형식 또는 값이 올바르지 않습니다.")
                )
                .addResponses(
                        UNAUTHORIZED_RESPONSE,
                        errorResponse("인증 정보가 없거나 유효하지 않습니다.")
                )
                .addResponses(
                        FORBIDDEN_RESPONSE,
                        errorResponse("요청한 작업을 수행할 권한이 없습니다.")
                )
                .addResponses(
                        NOT_FOUND_RESPONSE,
                        errorResponse("요청한 리소스를 찾을 수 없습니다.")
                )
                .addResponses(
                        CONFLICT_RESPONSE,
                        errorResponse("현재 리소스 상태와 요청이 충돌합니다.")
                )
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("로그인 응답으로 발급받은 Access Token을 입력합니다.")
                        );
    }

    private Schema<?> errorResponseSchema() {
        Schema<?> fieldErrorSchema = new ObjectSchema()
                .addProperty("field", new StringSchema().example("title"))
                .addProperty("reason", new StringSchema().example("NotBlank"))
                .addProperty("message", new StringSchema().example("제목은 필수입니다."));

        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("code", new StringSchema().example("VALIDATION_ERROR"))
                .addProperty("message", new StringSchema().example("요청 값을 확인해주세요."))
                .addProperty("fieldErrors", new ArraySchema().items(fieldErrorSchema))
                .addProperty("traceId", new StringSchema().nullable(true).example("trace-id"));

        return new ObjectSchema()
                .description("SEBU API 공통 오류 응답")
                .addProperty("success", new BooleanSchema().example(false))
                .addProperty("data", new ObjectSchema().nullable(true))
                .addProperty("error", errorSchema);
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(
                                new Schema<>().$ref(
                                        "#/components/schemas/" + ERROR_SCHEMA_NAME
                                )
                        )
                ));
    }

    private boolean requiresBearerAuth(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getMethod(),
                SecurityRequirement.class
        ) || AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getBeanType(),
                SecurityRequirement.class
        );
    }

    private boolean hasParameterAnnotation(
            MethodParameter[] parameters,
            Class<? extends java.lang.annotation.Annotation> annotationType
    ) {
        return Arrays.stream(parameters)
                .anyMatch(parameter ->
                        parameter.hasParameterAnnotation(annotationType)
                );
    }

    private void addResponseIfAbsent(
            ApiResponses responses,
            String responseCode,
            String componentName
    ) {
        responses.putIfAbsent(
                responseCode,
                new ApiResponse().$ref(
                        "#/components/responses/" + componentName
                )
        );
    }
}
