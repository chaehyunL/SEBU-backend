package com.sebu.backend.auth.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.auth.config.SejongClientProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongIdentity;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SejongSsoClientTest {
    private HttpServer server;

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

        SejongIdentity identity = client.authenticate("21012345", "portal-password");

        assertThat(identity.providerUserId()).isEqualTo("21012345");
        assertThat(identity.runningSejong()).isEqualTo("RUNNING");
        assertThat(identity.loginDateTime()).isEqualTo("20260818120000");
        assertThat(identity.organizationClassificationCode()).isEqualTo("STUDENT");
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
    }

    @Test
    void treatsMissingPortalSessionAsSystemUnavailableWithoutLeakingCredentials() throws Exception {
        startServer(Scenario.NO_PORTAL_SESSION);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "sensitive-password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception -> {
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.SYSTEM_UNAVAILABLE);
                assertThat(exception.getMessage()).doesNotContain("21012345", "sensitive-password");
            });
    }

    @Test
    void treatsMissingIntegratedUserNumberAsSystemUnavailable() throws Exception {
        startServer(Scenario.MISSING_USER_ID);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.SYSTEM_UNAVAILABLE));
    }

    @Test
    void treatsMalformedUserInfoAsSystemUnavailable() throws Exception {
        startServer(Scenario.MALFORMED_USER_INFO);
        SejongSsoClient client = client(Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.authenticate("21012345", "password"))
            .isInstanceOfSatisfying(SejongAuthenticationException.class, exception ->
                assertThat(exception.getReason())
                    .isEqualTo(SejongAuthenticationException.Reason.SYSTEM_UNAVAILABLE));
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
            new ObjectMapper()
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
        server.createContext("/sso", this::handleSso);
        server.createContext("/user-info", exchange -> handleUserInfo(exchange, scenario));
        server.start();
    }

    private void handlePortal(HttpExchange exchange, Scenario scenario) throws IOException {
        exchange.getRequestBody().readAllBytes();
        if (scenario == Scenario.SLOW_PORTAL) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        if (scenario != Scenario.NO_PORTAL_SESSION) {
            exchange.getResponseHeaders().add("Set-Cookie", "SSOTOKEN=portal-session; Path=/; HttpOnly");
        }
        respond(exchange, 200, "portal");
    }

    private void handleSso(HttpExchange exchange) throws IOException {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null || !cookies.contains("SSOTOKEN=portal-session")) {
            respond(exchange, 401, "missing portal session");
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
        String userIdField = scenario == Scenario.MISSING_USER_ID ? "" : "\"INTG_USR_NO\":\"21012345\",";
        respond(exchange, 200, """
            {
              "dm_UserInfo": {%s "RUNNING_SEJONG":"RUNNING"},
              "dm_UserInfoGam": {"LOGIN_DT":"20260818120000"},
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
        NO_PORTAL_SESSION,
        MISSING_USER_ID,
        MALFORMED_USER_INFO,
        SLOW_PORTAL
    }
}
