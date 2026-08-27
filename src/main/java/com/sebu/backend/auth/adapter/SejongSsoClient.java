package com.sebu.backend.auth.adapter;

import com.sebu.backend.auth.config.SejongClientProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongUserProfile;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class SejongSsoClient implements SejongAuthenticator {
    private static final String PORTAL_SESSION_COOKIE = "SSOTOKEN";
    private static final String SJPT_SESSION_COOKIE = "JSESSIONID";
    private static final int MAX_USER_INFO_RESPONSE_BYTES = 64 * 1024;
    private static final MediaType JSON = MediaType.get("application/json; charset=UTF-8");

    private final SejongHttpClientFactory httpClientFactory;
    private final SejongClientProperties properties;
    private final SejongUserInfoParser userInfoParser;

    @Override
    public SejongUserProfile authenticate(String studentId, String password) {
        SejongHttpClientFactory.Session session = httpClientFactory.create();
        try {
            int portalStatus = sendPortalForStatus(session, portalLoginRequest(studentId, password));
            requirePortalAuthentication(portalStatus, session);

            int ssoStatus = sendForStatus(session, ssoLoginRequest());
            requireSuccessful(ssoStatus);
            if (!hasCookie(session, SJPT_SESSION_COOKIE)) {
                throw SejongAuthenticationException.systemUnavailable();
            }

            ClientResponse userInfoResponse = sendUserInfo(session, userInfoRequest());
            requireSuccessful(userInfoResponse.statusCode());
            return userInfoParser.parse(userInfoResponse.body());
        } finally {
            session.cookieManager().getCookieStore().removeAll();
        }
    }

    private Request portalLoginRequest(String studentId, String password) {
        FormBody form = new FormBody.Builder()
            .add("mainLogin", "Y")
            .add("id", studentId)
            .add("password", password)
            .build();
        return new Request.Builder()
            .url(properties.portalLoginUrl().toString())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Referer", origin(properties.portalLoginUrl()) + "/jsp/login/loginSSL.jsp")
            .post(form)
            .build();
    }

    private Request ssoLoginRequest() {
        return new Request.Builder()
            .url(properties.ssoLoginUrl().toString())
            .header("Referer", origin(properties.portalLoginUrl()) + "/")
            .get()
            .build();
    }

    private Request userInfoRequest() {
        URI requestUri = URI.create(properties.userInfoUrl() + "?addParam=" + encodedEmptyExecutionContext());
        String sjptOrigin = origin(properties.ssoLoginUrl());
        return new Request.Builder()
            .url(requestUri.toString())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Origin", sjptOrigin)
            .header("Referer", sjptOrigin + "/")
            .header("submissionid", "mf___subMainUserInfoInit")
            .post(RequestBody.create("", JSON))
            .build();
    }

    private int sendForStatus(SejongHttpClientFactory.Session session, Request request) {
        return sendForStatus(session.httpClient(), request);
    }

    private int sendPortalForStatus(SejongHttpClientFactory.Session session, Request request) {
        OkHttpClient noRedirectClient = session.httpClient().newBuilder()
            .followRedirects(false)
            .build();
        return sendForStatus(noRedirectClient, request);
    }

    private int sendForStatus(OkHttpClient httpClient, Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            return response.code();
        } catch (IOException | RuntimeException exception) {
            throw SejongAuthenticationException.systemUnavailable(exception);
        }
    }

    private ClientResponse sendUserInfo(SejongHttpClientFactory.Session session, Request request) {
        try (Response response = session.httpClient().newCall(request).execute()) {
            if (!isSuccessful(response.code()) || response.body() == null) {
                return new ClientResponse(response.code(), "");
            }
            if (response.body().contentLength() > MAX_USER_INFO_RESPONSE_BYTES) {
                throw SejongAuthenticationException.responseInvalid();
            }
            byte[] body = response.body().byteStream().readNBytes(MAX_USER_INFO_RESPONSE_BYTES + 1);
            if (body.length > MAX_USER_INFO_RESPONSE_BYTES) {
                throw SejongAuthenticationException.responseInvalid();
            }
            return new ClientResponse(response.code(), new String(body, StandardCharsets.UTF_8));
        } catch (SejongAuthenticationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw SejongAuthenticationException.systemUnavailable(exception);
        }
    }

    private void requireSuccessful(int statusCode) {
        if (!isSuccessful(statusCode)) {
            throw SejongAuthenticationException.systemUnavailable();
        }
    }

    private void requirePortalAuthentication(int statusCode, SejongHttpClientFactory.Session session) {
        if (statusCode == 401 || statusCode == 403) {
            throw SejongAuthenticationException.authenticationFailed();
        }
        if (!isSuccessful(statusCode) && !isRedirect(statusCode)) {
            throw SejongAuthenticationException.systemUnavailable();
        }
        if (!hasCookie(session, PORTAL_SESSION_COOKIE)) {
            throw SejongAuthenticationException.authenticationFailed();
        }
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
            || statusCode == 302
            || statusCode == 303
            || statusCode == 307
            || statusCode == 308;
    }

    private boolean hasCookie(SejongHttpClientFactory.Session session, String expectedName) {
        return session.cookieManager().getCookieStore().getCookies().stream()
            .anyMatch(cookie -> expectedName.equalsIgnoreCase(cookie.getName())
                && cookie.getValue() != null
                && !cookie.getValue().isBlank());
    }

    private String encodedEmptyExecutionContext() {
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

    private record ClientResponse(int statusCode, String body) {
    }
}
