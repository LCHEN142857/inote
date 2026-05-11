package com.inote.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 记录登录失败次数，并在短时间多次失败后锁定账号和客户端组合。
@Service
public class LoginLockService {

    // 连续失败达到该次数后触发锁定。
    private static final int MAX_FAILURES = 5;
    // 触发锁定后的等待时长。
    private static final Duration LOCK_DURATION = Duration.ofMinutes(1);

    // 按用户名和客户端 IP 记录失败状态。
    private final Map<String, FailureState> failures = new ConcurrentHashMap<>();

    /**
     * 校验当前用户和客户端是否处于登录锁定期。
     * @param username 登录用户名。
     * @param clientIp 客户端 IP。
     * @throws LoginLockException 仍处于锁定期时抛出。
     */
    public void assertNotLocked(String username, String clientIp) {
        // 使用账号和 IP 共同限定锁定范围。
        String key = key(username, clientIp);
        // 没有失败记录时直接允许登录尝试。
        FailureState state = failures.get(key);
        if (state == null) {
            return;
        }

        // 当前时间用于判断锁定是否过期。
        Instant now = Instant.now();
        // 锁定未过期时阻止继续尝试。
        if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) {
            throw new LoginLockException(
                    "登录失败次数过多，请在" + state.remainingLockSeconds(now) + "秒后重试。",
                    state.remainingLockSeconds(now),
                    state.lockedUntil.toEpochMilli());
        }

        // 锁定期已过时清理状态，允许重新计数。
        if (state.lockedUntil != null) {
            failures.remove(key);
        }
    }

    /**
     * 记录一次登录失败。
     * @param username 登录用户名。
     * @param clientIp 客户端 IP。
     */
    public void recordFailure(String username, String clientIp) {
        // 使用同一组合键累积失败次数。
        String key = key(username, clientIp);
        // 原子更新失败状态，避免并发登录尝试覆盖计数。
        failures.compute(key, (ignored, state) -> {
            FailureState next = state == null ? new FailureState() : state;
            Instant now = Instant.now();
            next.failureCount++;
            if (next.failureCount >= MAX_FAILURES) {
                // 达到阈值后设置锁定截止时间并重置计数。
                next.lockedUntil = now.plus(LOCK_DURATION);
                next.failureCount = 0;
            }
            next.lastUpdated = now;
            return next;
        });
    }

    /**
     * 登录成功后清理失败状态。
     * @param username 登录用户名。
     * @param clientIp 客户端 IP。
     */
    public void clear(String username, String clientIp) {
        failures.remove(key(username, clientIp));
    }

    /**
     * 构造登录锁定维度键。
     * @param username 登录用户名。
     * @param clientIp 客户端 IP。
     * @return 标准化后的组合键。
     */
    private String key(String username, String clientIp) {
        return normalize(username) + "|" + normalize(clientIp);
    }

    /**
     * 标准化锁定维度值。
     * @param value 原始用户名或 IP。
     * @return 小写且去除首尾空白后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    // 保存单个账号和客户端组合的失败次数及锁定状态。
    @Getter
    private static class FailureState {
        // 当前连续失败次数。
        private int failureCount;
        // 锁定截止时间，未锁定时为空。
        private Instant lockedUntil;
        // 最近一次更新状态的时间。
        private Instant lastUpdated;

        /**
         * 计算锁定剩余秒数。
         * @param now 当前时间。
         * @return 至少为 1 的剩余秒数，未锁定时返回 0。
         */
        private long remainingLockSeconds(Instant now) {
            if (lockedUntil == null || now.isAfter(lockedUntil)) {
                return 0;
            }
            // 向上取整，避免前端显示 0 秒但仍被锁定。
            long remainingMillis = Duration.between(now, lockedUntil).toMillis();
            return Math.max(1, (remainingMillis + 999) / 1000);
        }
    }
}
