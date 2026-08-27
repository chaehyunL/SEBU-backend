package com.sebu.backend.auth.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.auth.config.SejongClientProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongUserProfile;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Protocol;
import okhttp3.TlsVersion;
import org.conscrypt.Conscrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SejongSsoClientTest {
    private HttpServer server;
    private final AtomicInteger portalRedirectTargetRequests = new AtomicInteger();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void usesOneCookieStoreThroughoutAFlowAndParsesIdentity() throws Exception {
        startServer(Scenario.SUCCESS);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        SejongUserProfile profile = client.authenticate("21012345", "portal-password");

        assertThat(profile.studentId()).isEqualTo("21012345");
        assertThat(profile.name()).isEqualTo("홍길동");
        assertThat(profile.departmentName()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void createsAnIndependentCookieStoreForEveryLoginRequest() throws Exception {
        startServer(Scenario.SUCCESS);
        SejongClientProperties properties = properties(Duration.ofSeconds(1));
        SejongHttpClientFactory factory = new SejongHttpClientFactory(properties);

        SejongHttpClientFactory.Session first = factory.create();
        SejongHttpClientFactory.Session second = factory.create();

        assertThat(first.httpClient()).isNotSameAs(second.httpClient());
        assertThat(first.cookieManager()).isNotSameAs(second.cookieManager());
        assertThat(first.cookieManager().getCookieStore()).isNotSameAs(second.cookieManager().getCookieStore());
        assertThat(first.httpClient().connectionPool()).isSameAs(second.httpClient().connectionPool());
        assertThat(first.httpClient().dispatcher()).isSameAs(second.httpClient().dispatcher());
        assertThat(first.httpClient().protocols()).containsExactly(Protocol.HTTP_1_1);
        assertThat(first.httpClient().connectionSpecs().getFirst().tlsVersions())
            .containsExactly(TlsVersion.TLS_1_2);
        assertThat(Conscrypt.isConscrypt(first.httpClient().sslSocketFactory())).isTrue();
    }

    @Test
    void clearsSchoolCookiesWhenAuthenticationRequestEnds() throws Exception {
        startServer(Scenario.SUCCESS);
        SejongClientProperties properties = properties(Duration.ofSeconds(1));
        SejongHttpClientFactory.Session session = new SejongHttpClientFactory(properties).create();
        SejongHttpClientFactory factory = mock(SejongHttpClientFactory.class);
        when(factory.create()).thenReturn(session);
        SejongSsoClient client = new SejongSsoClient(
            factory,
            properties,
            new SejongUserInfoParser(new ObjectMapper())
        );

        client.authenticate("21012345", "known-fake-password");

        assertThat(session.cookieManager().getCookieStore().getCookies()).isEmpty();
    }

    @Test
    void treatsMissingPortalSessionAsBadCredentialsWithoutLeakingCredentials() throws Exception {
        startServer(Scenario.NO_PORTAL_SESSION);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "sensitive-password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception -> {
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.AUTHENTICATION_FAILED);
                assertThat(exception.getMessage()).doesNotContain("21012345", "sensitive-password");
            });
    }

    @Test
    void treatsMissingIntegratedUserNumberAsInvalidResponse() throws Exception {
        startServer(Scenario.MISSING_USER_ID);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.RESPONSE_INVALID));
    }

    @Test
    void treatsMalformedUserInfoAsInvalidResponse() throws Exception {
        startServer(Scenario.MALFORMED_USER_INFO);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.RESPONSE_INVALID));
    }

    @Test
    void acceptsPortalRedirectWhenItIssuedTheAuthenticatedSessionCookie() throws Exception {
        startServer(Scenario.PORTAL_REDIRECT_WITH_SESSION);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        SejongUserProfile profile = client.authenticate("21012345", "portal-password");

        assertThat(profile.studentId()).isEqualTo("21012345");
        assertThat(portalRedirectTargetRequests).hasValue(0);
    }

    @Test
    void neverRepostsCredentialsFor307PortalRedirect() throws Exception {
        startServer(Scenario.PORTAL_307_REDIRECT_WITH_SESSION);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        SejongUserProfile profile = client.authenticate("21012345", "sensitive-password");

        assertThat(profile.studentId()).isEqualTo("21012345");
        assertThat(portalRedirectTargetRequests).hasValue(0);
    }

    @Test
    void rejectsEmptyPortalSessionCookie() throws Exception {
        startServer(Scenario.EMPTY_PORTAL_SESSION);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "sensitive-password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.AUTHENTICATION_FAILED));
    }

    @Test
    void refusesSsoRedirectsOutsideConfiguredSchoolOrigins() throws Exception {
        startServer(Scenario.CROSS_ORIGIN_SSO_REDIRECT);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "sensitive-password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception -> {
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.SYSTEM_UNAVAILABLE);
                assertThat(exception.getMessage()).doesNotContain("sensitive-password");
            });
    }

    @Test
    void rejectsUserInfoResponseLargerThan64KiB() throws Exception {
        startServer(Scenario.OVERSIZED_USER_INFO);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.RESPONSE_INVALID));
    }

    @Test
    void convertsTimeoutToSystemUnavailable() throws Exception {
        startServer(Scenario.SLOW_PORTAL);
        SejongSsoClient client = client(Duration.ofMillis(50));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.SYSTEM_UNAVAILABLE));
    }

    private SejongSsoClient client(Duration requestTimeout) {
        SejongClientProperties properties = properties(requestTimeout);
        return new SejongSsoClient(
            new SejongHttpClientFactory(properties),
            properties,
            new SejongUserInfoParser(new ObjectMapper())
        );
    }

    private SejongClientProperties properties(Duration requestTimeout) {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new SejongClientProperties(
            URI.create(baseUrl + "/portal"),
            URI.create(baseUrl + "/sso"),
            URI.create(baseUrl + "/user-info"),
            Duration.ofSeconds(1),
            requestTimeout
        );
    }

    private void startServer(Scenario scenario) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/portal", exchange -> handlePortal(exchange, scenario));
        server.createContext("/sso", exchange -> handleSso(exchange, scenario));
        server.createContext("/user-info", exchange -> handleUserInfo(exchange, scenario));
        server.createContext("/portal-redirect-target", exchange -> {
            portalRedirectTargetRequests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 500, "portal credentials must not be reposted");
        });
        server.start();
    }

    private void handlePortal(HttpExchange exchange, Scenario scenario) throws IOException {
        exchange.getRequestBody().readAllBytes();
        if (scenario == Scenario.PORTAL_REDIRECT_WITH_SESSION
            || scenario == Scenario.PORTAL_307_REDIRECT_WITH_SESSION) {
            exchange.getResponseHeaders().add("Set-Cookie", "SSOTOKEN=portal-session; Path=/; HttpOnly");
            exchange.getResponseHeaders().add(
                "Location",
                "http://localhost:" + server.getAddress().getPort() + "/portal-redirect-target"
            );
            int status = scenario == Scenario.PORTAL_REDIRECT_WITH_SESSION ? 302 : 307;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        if (scenario == Scenario.SLOW_PORTAL) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        if (scenario != Scenario.NO_PORTAL_SESSION) {
            String value = scenario == Scenario.EMPTY_PORTAL_SESSION ? "" : "portal-session";
            exchange.getResponseHeaders().add("Set-Cookie", "SSOTOKEN=" + value + "; Path=/; HttpOnly");
        }
        respond(exchange, 200, "portal");
    }

    private void handleSso(HttpExchange exchange, Scenario scenario) throws IOException {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null || !cookies.contains("SSOTOKEN=portal-session")) {
            respond(exchange, 401, "missing portal session");
            return;
        }
        if (scenario == Scenario.CROSS_ORIGIN_SSO_REDIRECT) {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:1/session-sink");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=sjpt-session; Path=/; HttpOnly");
        respond(exchange, 200, "sso");
    }

    private void handleUserInfo(HttpExchange exchange, Scenario scenario) throws IOException {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null || !cookies.contains("SSOTOKEN=portal-session") || !cookies.contains("JSESSIONID=sjpt-session")) {
            respond(exchange, 401, "missing session");
            return;
        }
        if (scenario == Scenario.MALFORMED_USER_INFO) {
            respond(exchange, 200, "not-json");
            return;
        }
        if (scenario == Scenario.OVERSIZED_USER_INFO) {
            respond(exchange, 200, "x".repeat(64 * 1024 + 1));
            return;
        }
        String userIdField = scenario == Scenario.MISSING_USER_ID ? "" : "\"INTG_USR_NO\":\"21012345\",";
        respond(exchange, 200, """
            {
              "dm_UserInfo": {%s "INTG_KOR_NM":"홍길동", "RUNNING_SEJONG":"RUNNING"},
              "dm_UserInfoGam": {"LOGIN_DT":"20260818120000", "DEPT_NM":"컴퓨터공학과", "DEPT_NO":"3513"},
              "dm_UserInfoSch": {"ORGN_CLSF_CD":"STUDENT"}
            }
            """.formatted(userIdField));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private enum Scenario {
        SUCCESS,
        PORTAL_REDIRECT_WITH_SESSION,
        PORTAL_307_REDIRECT_WITH_SESSION,
        EMPTY_PORTAL_SESSION,
        NO_PORTAL_SESSION,
        CROSS_ORIGIN_SSO_REDIRECT,
        MISSING_USER_ID,
        MALFORMED_USER_INFO,
        OVERSIZED_USER_INFO,
        SLOW_PORTAL
    }
}
