package com.sebu.backend.auth.port;

public interface SejongAuthenticator {
    SejongIdentity authenticate(String studentId, String password);
}
