package com.sebu.backend.auth.port;

public record SejongIdentity(
    String providerUserId,
    String runningSejong,
    String loginDateTime,
    String organizationClassificationCode
) {
}
