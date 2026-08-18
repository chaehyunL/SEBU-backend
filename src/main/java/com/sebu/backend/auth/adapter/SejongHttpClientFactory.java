package com.sebu.backend.auth.adapter;

import com.sebu.backend.auth.config.SejongClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

@Component
@RequiredArgsConstructor
public class SejongHttpClientFactory {
    private final SejongClientProperties properties;

    public Session create() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(properties.connectTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        return new Session(httpClient, cookieManager);
    }

    public record Session(HttpClient httpClient, CookieManager cookieManager) {
    }
}
