package com.sebu.backend.auth.service;

import com.sebu.backend.auth.exception.InvalidLoginRequestException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SejongAuthenticator sejongAuthenticator;
    private final AuthSessionService authSessionService;

    public AuthSessionService.LoginSession loginWithSejong(String studentId, String password) {
        if (studentId == null || studentId.isBlank() || password == null || password.isBlank()) {
            throw new InvalidLoginRequestException();
        }
        SejongIdentity identity = sejongAuthenticator.authenticate(studentId, password);
        try {
            return authSessionService.start(identity.providerUserId());
        } catch (DataIntegrityViolationException exception) {
            return authSessionService.startExisting(identity.providerUserId())
                .orElseThrow(() -> exception);
        }
    }
}
