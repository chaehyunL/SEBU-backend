package com.sebu.backend.auth.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.auth.config.SejongClientProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class SejongSsoClient implements SejongAuthenticator {
    private static final String PORTAL_SESSION_COOKIE = "SSOTOKEN";
    private static final String SJPT_SESSION_COOKIE = "JSESSIONID";

    private final SejongHttpClientFactory httpClientFactory;
    private final SejongClientProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public SejongIdentity authenticate(String studentId, String password) {
        SejongHttpClientFactory.Session session = httpClientFactory.create();

        HttpResponse<String> portalResponse = send(session, portalLoginRequest(studentId, password));
        if (portalResponse.statusCode() == 401 || portalResponse.statusCode() == 403) {
            throw SejongAuthenticationException.authenticationFailed();
        }
        requireSuccessful(portalResponse);
        if (!hasCookie(session, PORTAL_SESSION_COOKIE)) {
            throw SejongAuthenticationException.systemUnavailable();
        }

        HttpResponse<String> ssoResponse = send(session, ssoLoginRequest());
        requireSuccessful(ssoResponse);
        if (!hasCookie(session, SJPT_SESSION_COOKIE)) {
            throw SejongAuthenticationException.systemUnavailable();
        }

        HttpResponse<String> userInfoResponse = send(session, userInfoRequest());
        requireSuccessful(userInfoResponse);
        return parseIdentity(userInfoResponse.body());
    }

    private HttpRequest portalLoginRequest(String studentId, String password) {
        String form = "mainLogin=Y&id=" + formEncode(studentId) + "&password=" + formEncode(password);
        return HttpRequest.newBuilder(properties.portalLoginUrl())
            .timeout(properties.requestTimeout())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    }

    private HttpRequest ssoLoginRequest() {
        return HttpRequest.newBuilder(properties.ssoLoginUrl())
            .timeout(properties.requestTimeout())
            .header(HttpHeaders.REFERER, origin(properties.portalLoginUrl()) + "/")
            .GET()
            .build();
    }

    private HttpRequest userInfoRequest() {
        URI requestUri = URI.create(properties.userInfoUrl() + "?addParam=" + emptyRunContext());
        String sjptOrigin = origin(properties.ssoLoginUrl());
        return HttpRequest.newBuilder(requestUri)
            .timeout(properties.requestTimeout())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ORIGIN, sjptOrigin)
            .header(HttpHeaders.REFERER, sjptOrigin + "/")
            .header("submissionid", "mf___subMainUserInfoInit")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();
    }

    private HttpResponse<String> send(SejongHttpClientFactory.Session session, HttpRequest request) {
        try {
            return session.httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw SejongAuthenticationException.systemUnavailable(exception);
        } catch (IOException | RuntimeException exception) {
            throw SejongAuthenticationException.systemUnavailable(exception);
        }
    }

    private void requireSuccessful(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw SejongAuthenticationException.systemUnavailable();
        }
    }

    private boolean hasCookie(SejongHttpClientFactory.Session session, String expectedName) {
        return session.cookieManager().getCookieStore().getCookies().stream()
            .map(HttpCookie::getName)
            .anyMatch(expectedName::equalsIgnoreCase);
    }

    private SejongIdentity parseIdentity(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String providerUserId = requiredText(root, "dm_UserInfo", "INTG_USR_NO");
            String runningSejong = requiredText(root, "dm_UserInfo", "RUNNING_SEJONG");
            String loginDateTime = requiredText(root, "dm_UserInfoGam", "LOGIN_DT");
            String organizationCode = requiredText(root, "dm_UserInfoSch", "ORGN_CLSF_CD");
            if (providerUserId.isBlank()) {
                throw SejongAuthenticationException.systemUnavailable();
            }
            return new SejongIdentity(
                providerUserId.trim(),
                runningSejong,
                loginDateTime,
                organizationCode
            );
        } catch (SejongAuthenticationException exception) {
            throw exception;
        } catch (JsonProcessingException | RuntimeException exception) {
            throw SejongAuthenticationException.systemUnavailable(exception);
        }
    }

    private String requiredText(JsonNode root, String objectName, String fieldName) {
        JsonNode object = root.get(objectName);
        if (object == null || !object.isObject()) {
            throw new IllegalStateException("SEJONG_RESPONSE_STRUCTURE_INVALID");
        }
        JsonNode value = object.get(fieldName);
        if (value == null || !value.isValueNode() || value.isNull()) {
            throw new IllegalStateException("SEJONG_RESPONSE_STRUCTURE_INVALID");
        }
        return value.asText();
    }

    private String emptyRunContext() {
        String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";
        String urlEncoded = formEncode(json);
        return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
    }

    private String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String origin(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
