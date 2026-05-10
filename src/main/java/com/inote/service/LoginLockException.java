package com.inote.service;

public class LoginLockException extends RuntimeException {

    private final long lockSeconds;
    private final long lockedUntilEpochMillis;

    public LoginLockException(String message, long lockSeconds, long lockedUntilEpochMillis) {
        super(message);
        this.lockSeconds = lockSeconds;
        this.lockedUntilEpochMillis = lockedUntilEpochMillis;
    }

    public long getLockSeconds() {
        return lockSeconds;
    }

    public long getLockedUntilEpochMillis() {
        return lockedUntilEpochMillis;
    }
}
