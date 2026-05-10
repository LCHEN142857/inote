package com.inote.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestRateLimitService {

    private static final int DEFAULT_CAPACITY = 120;
    private static final Duration DEFAULT_REFILL_PERIOD = Duration.ofMinutes(1);

    private static final int AUTH_CAPACITY = 30;
    private static final Duration AUTH_REFILL_PERIOD = Duration.ofMinutes(1);

    private static final int CAPTCHA_CAPACITY = 20;
    private static final Duration CAPTCHA_REFILL_PERIOD = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String path, String clientIp) {
        String bucketKey = bucketKey(path, clientIp);
        BucketPolicy policy = policy(path);
        return buckets.compute(bucketKey, (key, current) -> {
            Bucket bucket = current == null ? new Bucket(policy.capacity, policy.refillPeriod) : current;
            if (bucket.allow()) {
                return bucket;
            }
            return bucket;
        }).lastDecisionAllowed;
    }

    private String bucketKey(String path, String clientIp) {
        return normalize(path) + "|" + normalize(clientIp);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private BucketPolicy policy(String path) {
        if (path != null && path.startsWith("/api/v1/auth/captcha")) {
            return new BucketPolicy(CAPTCHA_CAPACITY, CAPTCHA_REFILL_PERIOD);
        }
        if (path != null && path.startsWith("/api/v1/auth/login")) {
            return new BucketPolicy(AUTH_CAPACITY, AUTH_REFILL_PERIOD);
        }
        return new BucketPolicy(DEFAULT_CAPACITY, DEFAULT_REFILL_PERIOD);
    }

    private record BucketPolicy(int capacity, Duration refillPeriod) {
    }

    private static final class Bucket {
        private final int capacity;
        private final double refillTokensPerSecond;
        private double tokens;
        private Instant lastRefill;
        private boolean lastDecisionAllowed = true;

        private Bucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillTokensPerSecond = (double) capacity / Math.max(1, refillPeriod.toMillis()) * 1000.0;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
        }

        private boolean allow() {
            refill();
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                lastDecisionAllowed = true;
            } else {
                lastDecisionAllowed = false;
            }
            return lastDecisionAllowed;
        }

        private void refill() {
            Instant now = Instant.now();
            long elapsedMillis = Duration.between(lastRefill, now).toMillis();
            if (elapsedMillis <= 0) {
                return;
            }
            tokens = Math.min(capacity, tokens + (elapsedMillis * refillTokensPerSecond / 1000.0d));
            lastRefill = now;
        }
    }
}
