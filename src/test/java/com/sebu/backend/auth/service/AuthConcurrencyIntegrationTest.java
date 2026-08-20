package com.sebu.backend.auth.service;

import com.sebu.backend.auth.exception.RefreshTokenInvalidException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import com.sebu.backend.auth.repository.RefreshTokenRepository;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthConcurrencyIntegrationTest {
    @Autowired
    AuthService authService;

    @Autowired
    AuthSessionService authSessionService;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    SejongAuthenticator sejongAuthenticator;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        cleanDatabase();
    }

    @Test
    void concurrentFirstLoginsReuseTheUserAfterUniqueConflict() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        when(sejongAuthenticator.authenticate(anyString(), anyString())).thenAnswer(invocation -> {
            barrier.await(5, TimeUnit.SECONDS);
            return new SejongIdentity("concurrent-user", "RUNNING", "LOGIN", "STUDENT");
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AuthSessionService.LoginSession> first = executor.submit(
                () -> authService.loginWithSejong("requested-a", "password")
            );
            Future<AuthSessionService.LoginSession> second = executor.submit(
                () -> authService.loginWithSejong("requested-b", "password")
            );

            List<AuthSessionService.LoginSession> sessions = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
            );

            assertThat(appUserRepository.count()).isOne();
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
            assertThat(sessions).extracting(AuthSessionService.LoginSession::userId)
                .containsOnly(sessions.getFirst().userId());
            assertThat(sessions).extracting(AuthSessionService.LoginSession::isNewUser)
                .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void onlyOneConcurrentRefreshCanRotateTheSameToken() throws Exception {
        AuthSessionService.LoginSession login = authSessionService.start("concurrent-refresh-user");
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> refreshAfterBarrier(barrier, login.refreshToken()));
            Future<Object> second = executor.submit(() -> refreshAfterBarrier(barrier, login.refreshToken()));
            List<Object> results = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
            );

            assertThat(results).filteredOn(AuthSessionService.RefreshSession.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(RefreshTokenInvalidException.class::isInstance).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Object refreshAfterBarrier(CyclicBarrier barrier, String refreshToken) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try {
            return authSessionService.refresh(refreshToken);
        } catch (RefreshTokenInvalidException exception) {
            return exception;
        }
    }
}
