package com.sebu.backend.auth.service;

import com.sebu.backend.auth.exception.InvalidLoginRequestException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    SejongAuthenticator sejongAuthenticator;

    @Mock
    AuthSessionService authSessionService;

    @InjectMocks
    AuthService authService;

    @Test
    void provisionsUserWithAuthenticatedIdentityInsteadOfRequestedStudentId() {
        when(sejongAuthenticator.authenticate("requested-id", "password"))
            .thenReturn(new SejongIdentity("verified-id", "running", "login", "student"));
        when(authSessionService.start("verified-id"))
            .thenReturn(mock(AuthSessionService.LoginSession.class));

        authService.loginWithSejong("requested-id", "password");

        verify(authSessionService).start("verified-id");
    }

    @Test
    void rejectsBlankCredentialsBeforeCallingExternalSystem() {
        assertThatThrownBy(() -> authService.loginWithSejong(" ", "password"))
            .isInstanceOf(InvalidLoginRequestException.class)
            .hasMessage("INVALID_LOGIN_REQUEST");
        verifyNoInteractions(sejongAuthenticator, authSessionService);
    }
}
