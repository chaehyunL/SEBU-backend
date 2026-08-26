package com.sebu.backend.auth.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.auth.config.SejongClientProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class SejongSsoClient implements SejongAuthenticator {
    private static final String PORTAL_SESSION_COOKIE = "SSOTOKEN";
    private static final String SJPT_SESSION_COOKIE = "JSESSIONID";
    private static final int MAX_REDIRECTS = 5;
    private static final Set<Integer> REDIRECT_STATUS_CODES = Set.of(301, 302, 303, 307, 308);
    private static final Pattern PORTAL_LOGIN_RESULT_PATTERN = Pattern.compile(
        "\\bvar\\s+result\\s*=\\s*['\"]([^'\"]+)['\"]",
        Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> PORTAL_REJECTION_RESULTS = Set.of(
        "erridpwd",
        "Error",
        "pwdNeedChg",
        "invalidDt",
        "invalid"
    );
    private static final String BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private static final String HTML_ACCEPT =
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "ko-KR,ko;q=0.9,en;q=0.8";

    private final SejongHttpClientFactory httpClientFactory;
    private final SejongClientProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public SejongIdentity authenticate(String studentId, String password) {
        SejongHttpClientFactory.Session session = httpClientFactory.create();

        HttpResponse<String> portalPageResponse = sendFollowingAllowedGetRedirects(
            session,
            portalLoginPageRequest(),
            "portal-login-page"
        );
        requireSuccessful("portal-login-page", portalPageResponse);

        HttpResponse<String> portalResponse = send(
            session,
            portalLoginRequest(studentId, password),
            "portal-login"
        );
        String portalLoginResult = portalLoginResult(portalResponse);
        if (isPortalAuthenticationFailure(portalResponse, portalLoginResult)) {
            log.warn(
                "Sejong authentication rejected: stage=portal-login status={} finalPath={}",
                portalResponse.statusCode(),
                portalResponse.uri().getPath()
            );
            throw SejongAuthenticationException.authenticationFailed();
        }
        if (portalLoginResult != null && !"OK".equalsIgnoreCase(portalLoginResult)) {
            log.warn(
                "Sejong authentication upstream failure: stage=portal-login-result status={}",
                portalResponse.statusCode()
            );
            throw SejongAuthenticationException.systemUnavailable();
        }
        requireSuccessful("portal-login", portalResponse);

        HttpResponse<String> portalSsoResponse = sendFollowingAllowedGetRedirects(
            session,
            portalSsoLoginRequest(),
            "portal-sso-login"
        );
        requireSuccessful("portal-sso-login", portalSsoResponse);
        if (!hasCookie(session, PORTAL_SESSION_COOKIE)) {
            log.warn(
                "Sejong authentication upstream failure: stage=portal-session-cookie status={}",
                portalSsoResponse.statusCode()
            );
            throw SejongAuthenticationException.systemUnavailable();
        }

        HttpResponse<String> ssoResponse = sendFollowingAllowedGetRedirects(
            session,
            ssoLoginRequest(),
            "sso-login"
        );
        requireSuccessful("sso-login", ssoResponse);
        if (!hasCookie(session, SJPT_SESSION_COOKIE)) {
            log.warn(
                "Sejong authentication upstream failure: stage=sso-session-cookie status={}",
                ssoResponse.statusCode()
            );
            throw SejongAuthenticationException.systemUnavailable();
        }

        HttpResponse<String> userInfoResponse = send(session, userInfoRequest(), "user-info");
        requireSuccessful("user-info", userInfoResponse);
        return parseIdentity(userInfoResponse.body());
    }

    private HttpRequest portalLoginRequest(String studentId, String password) {
        String form = "mainLogin=Y&rtUrl=" + formEncode(properties.portalReturnUrl())
            + "&id=" + formEncode(studentId.trim())
            + "&password=" + formEncode(password);
        return browserRequestBuilder(properties.portalLoginUrl())
            .header(HttpHeaders.ACCEPT, HTML_ACCEPT)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .header(HttpHeaders.ORIGIN, origin(properties.portalLoginUrl()))
            .header(HttpHeaders.REFERER, properties.portalLoginPageUrl().toString())
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    }

    private HttpRequest portalLoginPageRequest() {
        return browserRequestBuilder(properties.portalLoginPageUrl())
            .header(HttpHeaders.ACCEPT, HTML_ACCEPT)
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .GET()
            .build();
    }

    private HttpRequest portalSsoLoginRequest() {
        return browserRequestBuilder(properties.portalSsoLoginUrl())
            .header(HttpHeaders.ACCEPT, HTML_ACCEPT)
            .header(HttpHeaders.REFERER, properties.portalLoginUrl().toString())
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .GET()
            .build();
    }

    private HttpRequest ssoLoginRequest() {
        return browserRequestBuilder(properties.ssoLoginUrl())
            .header(HttpHeaders.ACCEPT, HTML_ACCEPT)
            .header(HttpHeaders.REFERER, origin(properties.portalLoginUrl()) + "/")
            .GET()
            .build();
    }

    private HttpRequest userInfoRequest() {
        URI requestUri = URI.create(properties.userInfoUrl() + "?addParam=" + emptyRunContext());
        String sjptOrigin = origin(properties.ssoLoginUrl());
        return browserRequestBuilder(requestUri)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ORIGIN, sjptOrigin)
            .header(HttpHeaders.REFERER, sjptOrigin + "/")
            .header("submissionid", "mf___subMainUserInfoInit")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();
    }

    private HttpResponse<String> send(
        SejongHttpClientFactory.Session session,
        HttpRequest request,
        String stage
    ) {
        try {
            return session.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn(
                "Sejong authentication upstream failure: stage={} causeType={}",
                stage,
                exception.getClass().getSimpleName()
            );
            throw SejongAuthenticationException.systemUnavailable(exception);
        } catch (IOException | RuntimeException exception) {
            log.warn(
                "Sejong authentication upstream failure: stage={} causeType={}",
                stage,
                exception.getClass().getSimpleName()
            );
            throw SejongAuthenticationException.systemUnavailable(exception);
        }
    }

    private HttpResponse<String> sendFollowingAllowedGetRedirects(
        SejongHttpClientFactory.Session session,
        HttpRequest initialRequest,
        String stage
    ) {
        HttpRequest request = initialRequest;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpResponse<String> response = send(session, request, stage);
            if (!REDIRECT_STATUS_CODES.contains(response.statusCode())) {
                return response;
            }
            if (redirectCount == MAX_REDIRECTS) {
                rejectRedirect(stage, response.statusCode(), "limit-exceeded");
            }

            String location = response.headers().firstValue(HttpHeaders.LOCATION).orElse(null);
            URI redirectUri = resolveRedirectUri(request.uri(), location);
            if (!isAllowedRedirect(request.uri(), redirectUri)) {
                rejectRedirect(stage, response.statusCode(), "destination-not-allowed");
            }
            request = redirectedGetRequest(request, redirectUri);
        }
        throw SejongAuthenticationException.systemUnavailable();
    }

    private URI resolveRedirectUri(URI requestUri, String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            URI resolved = requestUri.resolve(URI.create(location)).normalize();
            if (resolved.getFragment() == null) {
                return resolved;
            }
            return new URI(
                resolved.getScheme(),
                resolved.getUserInfo(),
                resolved.getHost(),
                resolved.getPort(),
                resolved.getPath(),
                resolved.getQuery(),
                null
            );
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return null;
        }
    }

    private boolean isAllowedRedirect(URI requestUri, URI redirectUri) {
        if (redirectUri == null || redirectUri.getUserInfo() != null) {
            return false;
        }
        if ("https".equalsIgnoreCase(requestUri.getScheme())
            && !"https".equalsIgnoreCase(redirectUri.getScheme())) {
            return false;
        }
        String redirectOrigin = normalizedOrigin(redirectUri);
        return redirectOrigin != null && allowedRedirectOrigins().contains(redirectOrigin);
    }

    private Set<String> allowedRedirectOrigins() {
        Set<String> origins = new HashSet<>();
        addOrigin(origins, properties.portalLoginUrl());
        addOrigin(origins, properties.portalLoginPageUrl());
        addOrigin(origins, properties.portalSsoLoginUrl());
        addOrigin(origins, properties.ssoLoginUrl());
        addOrigin(origins, properties.userInfoUrl());
        return origins;
    }

    private void addOrigin(Set<String> origins, URI uri) {
        String origin = normalizedOrigin(uri);
        if (origin != null) {
            origins.add(origin);
        }
    }

    private String normalizedOrigin(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            return null;
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (port == -1) {
            port = "https".equals(normalizedScheme) ? 443 : "http".equals(normalizedScheme) ? 80 : -1;
        }
        return normalizedScheme + "://" + host.toLowerCase(Locale.ROOT) + ":" + port;
    }

    private HttpRequest redirectedGetRequest(HttpRequest previousRequest, URI redirectUri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(redirectUri).GET();
        previousRequest.timeout().ifPresent(builder::timeout);
        previousRequest.headers().map().forEach((name, values) ->
            values.forEach(value -> builder.header(name, value))
        );
        return builder.build();
    }

    private void rejectRedirect(String stage, int status, String reason) {
        log.warn(
            "Sejong authentication upstream failure: stage={} status={} redirect={}",
            stage,
            status,
            reason
        );
        throw SejongAuthenticationException.systemUnavailable();
    }

    private boolean isPortalAuthenticationFailure(
        HttpResponse<String> response,
        String portalLoginResult
    ) {
        if (response.statusCode() == 401) {
            return true;
        }
        boolean resultCanDescribeAuthentication = response.statusCode() == 403
            || response.statusCode() >= 200 && response.statusCode() < 300;
        return resultCanDescribeAuthentication
            && portalLoginResult != null
            && PORTAL_REJECTION_RESULTS.contains(portalLoginResult);
    }

    private String portalLoginResult(HttpResponse<String> response) {
        if (response.body() == null) {
            return null;
        }
        Matcher resultMatcher = PORTAL_LOGIN_RESULT_PATTERN.matcher(response.body());
        return resultMatcher.find() ? resultMatcher.group(1) : null;
    }

    private void requireSuccessful(String stage, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn(
                "Sejong authentication upstream failure: stage={} status={}",
                stage,
                response.statusCode()
            );
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
            log.warn(
                "Sejong authentication upstream failure: stage=user-info-parse causeType={}",
                exception.getClass().getSimpleName()
            );
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

    private HttpRequest.Builder browserRequestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
            .timeout(properties.requestTimeout())
            .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
            .header(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
    }

    private String origin(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
