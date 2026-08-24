package com.sebu.backend.auth.adapter;

import com.sebu.backend.auth.config.SejongClientProperties;
import org.conscrypt.Conscrypt;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@Component
public class SejongHttpClientFactory {
    static final String SEJONG_TLS_PROTOCOL = "TLSv1.2";
    static final String SEJONG_TLS_CIPHER_SUITE = "TLS_RSA_WITH_AES_256_CBC_SHA";

    private final SejongClientProperties properties;
    private final SSLContext sslContext;

    public SejongHttpClientFactory(SejongClientProperties properties) {
        this.properties = properties;
        this.sslContext = createSejongSslContext();
    }

    public Session create() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[]{SEJONG_TLS_PROTOCOL});
        sslParameters.setCipherSuites(new String[]{SEJONG_TLS_CIPHER_SUITE});
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

        HttpClient legacyHttpClient = baseClient(cookieManager)
            .version(HttpClient.Version.HTTP_1_1)
            .sslContext(sslContext)
            .sslParameters(sslParameters)
            .build();
        HttpClient standardHttpClient = baseClient(cookieManager).build();
        return new Session(legacyHttpClient, standardHttpClient, cookieManager);
    }

    private HttpClient.Builder baseClient(CookieManager cookieManager) {
        return HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(properties.connectTimeout())
            .followRedirects(HttpClient.Redirect.NEVER);
    }

    private SSLContext createSejongSslContext() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init((KeyStore) null);

            SSLContext context = SSLContext.getInstance("TLS", Conscrypt.newProvider());
            context.init(null, trustManagerFactory.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to initialize the Sejong TLS client", exception);
        }
    }

    public record Session(
        HttpClient legacyHttpClient,
        HttpClient standardHttpClient,
        CookieManager cookieManager
    ) {
        public <T> HttpResponse<T> send(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) throws IOException, InterruptedException {
            try {
                return legacyHttpClient.send(request, responseBodyHandler);
            } catch (SSLHandshakeException exception) {
                return standardHttpClient.send(request, responseBodyHandler);
            }
        }
    }
}
