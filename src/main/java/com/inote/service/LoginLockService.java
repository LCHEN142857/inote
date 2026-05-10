package com.inote.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginLockService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(1);

    private final Map<String, FailureState> failures = new ConcurrentHashMap<>();

    public void assertNotLocked(String username, String clientIp) {
        String key = key(username, clientIp);
        FailureState state = failures.get(key);
        if (state == null) {
            return;
        }

        Instant now = Instant.now();
        if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) {
            throw new LoginLockException(
                    "登录失败次数过多，请在" + state.remainingLockSeconds(now) + "秒后重试。",
                    state.remainingLockSeconds(now),
                    state.lockedUntil.toEpochMilli());
        }

        if (state.lockedUntil != null) {
            failures.remove(key);
        }
    }

    public void recordFailure(String username, String clientIp) {
        String key = key(username, clientIp);
        failures.compute(key, (ignored, state) -> {
            FailureState next = state == null ? new FailureState() : state;
            Instant now = Instant.now();
            next.failureCount++;
            if (next.failureCount >= MAX_FAILURES) {
                next.lockedUntil = now.plus(LOCK_DURATION);
                next.failureCount = 0;
            }
            next.lastUpdated = now;
            return next;
        });
    }

    public void clear(String username, String clientIp) {
        failures.remove(key(username, clientIp));
    }

    private String key(String username, String clientIp) {
        return normalize(username) + "|" + normalize(clientIp);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    private static class FailureState {
        private int failureCount;
        private Instant lockedUntil;
        private Instant lastUpdated;

        private long remainingLockSeconds(Instant now) {
            if (lockedUntil == null || now.isAfter(lockedUntil)) {
                return 0;
            }
            long remainingMillis = Duration.between(now, lockedUntil).toMillis();
            return Math.max(1, (remainingMillis + 999) / 1000);
        }
    }
}
