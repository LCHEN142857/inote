package com.inote.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ReplayProtectionService {

    private static final Duration REPLAY_WINDOW = Duration.ofMillis(500);
    private static final Duration ENTRY_TTL = Duration.ofSeconds(5);

    private final Map<String, Instant> requestFingerprints = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public boolean isReplay(String rawFingerprint) {
        cleanupIfNeeded();
        String fingerprint = sha256(rawFingerprint);
        Instant now = Instant.now();
        AtomicReference<Boolean> replay = new AtomicReference<>(false);

        requestFingerprints.compute(fingerprint, (key, previous) -> {
            if (previous != null && Duration.between(previous, now).compareTo(REPLAY_WINDOW) < 0) {
                replay.set(true);
            }
            return now;
        });

        return replay.get();
    }

    private void cleanupIfNeeded() {
        if (cleanupCounter.incrementAndGet() % 256 != 0) {
            return;
        }

        Instant threshold = Instant.now().minus(ENTRY_TTL);
        requestFingerprints.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
