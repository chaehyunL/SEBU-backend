package com.sebu.backend.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {
    @Test
    void rejectsRequestsOverTheLimitAndResetsAfterTheWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-09T00:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties(2, Duration.ofMinutes(1)), clock);

        assertThat(limiter.tryAcquire("IP:127.0.0.1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("IP:127.0.0.1").allowed()).isTrue();
        RateLimitDecision rejected = limiter.tryAcquire("IP:127.0.0.1");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(60);

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("IP:127.0.0.1").allowed()).isTrue();
    }

    @Test
    void maintainsIndependentLimitsForDifferentKeys() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-09T00:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties(1, Duration.ofMinutes(1)), clock);

        assertThat(limiter.tryAcquire("USER:1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("USER:1").allowed()).isFalse();
        assertThat(limiter.tryAcquire("USER:2").allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
