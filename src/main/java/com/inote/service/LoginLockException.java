package com.inote.service;

// 表示当前账号在指定客户端维度上被登录失败保护机制锁定。
public class LoginLockException extends RuntimeException {

    // 记录剩余锁定秒数，便于前端提示用户何时重试。
    private final long lockSeconds;
    // 记录锁定结束时间戳，便于返回给调用方做倒计时。
    private final long lockedUntilEpochMillis;

    /**
     * 创建登录锁定异常。
     * @param message 锁定提示消息。
     * @param lockSeconds 剩余锁定秒数。
     * @param lockedUntilEpochMillis 锁定结束时间戳。
     */
    public LoginLockException(String message, long lockSeconds, long lockedUntilEpochMillis) {
        super(message);
        this.lockSeconds = lockSeconds;
        this.lockedUntilEpochMillis = lockedUntilEpochMillis;
    }

    /**
     * 获取剩余锁定秒数。
     * @return 剩余锁定秒数。
     */
    public long getLockSeconds() {
        return lockSeconds;
    }

    /**
     * 获取锁定结束时间戳。
     * @return 锁定结束的 epoch 毫秒值。
     */
    public long getLockedUntilEpochMillis() {
        return lockedUntilEpochMillis;
    }
}
