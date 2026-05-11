package com.inote.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 基于令牌桶为不同接口类型提供内存级请求限流。
@Service
public class RequestRateLimitService {

    // 普通 API 默认每分钟允许的请求数。
    private static final int DEFAULT_CAPACITY = 120;
    // 普通 API 的令牌恢复窗口。
    private static final Duration DEFAULT_REFILL_PERIOD = Duration.ofMinutes(1);

    // 登录接口更严格，降低撞库和暴力尝试频率。
    private static final int AUTH_CAPACITY = 30;
    // 登录限流的恢复窗口。
    private static final Duration AUTH_REFILL_PERIOD = Duration.ofMinutes(1);

    // 验证码接口限制更低，避免图形验证码被高频刷新。
    private static final int CAPTCHA_CAPACITY = 20;
    // 验证码限流的恢复窗口。
    private static final Duration CAPTCHA_REFILL_PERIOD = Duration.ofMinutes(1);

    // 按路径和客户端 IP 保存令牌桶状态。
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 判断当前请求是否仍有可用令牌。
     * @param path 请求路径。
     * @param clientIp 客户端 IP。
     * @return true 表示允许通过，false 表示触发限流。
     */
    public boolean allow(String path, String clientIp) {
        // 将接口路径和客户端 IP 合并成独立限流维度。
        String bucketKey = bucketKey(path, clientIp);
        // 根据接口类型选择令牌桶容量和补充速度。
        BucketPolicy policy = policy(path);
        // 原子更新令牌桶，避免并发请求绕过限流判断。
        return buckets.compute(bucketKey, (key, current) -> {
            // 新访问维度直接创建满令牌桶。
            Bucket bucket = current == null ? new Bucket(policy.capacity, policy.refillPeriod) : current;
            // 记录本次限流决策供 compute 后读取。
            if (bucket.allow()) {
                return bucket;
            }
            return bucket;
        }).lastDecisionAllowed;
    }

    /**
     * 构造限流桶键。
     * @param path 请求路径。
     * @param clientIp 客户端 IP。
     * @return 标准化后的限流桶键。
     */
    private String bucketKey(String path, String clientIp) {
        return normalize(path) + "|" + normalize(clientIp);
    }

    /**
     * 标准化限流维度值。
     * @param value 原始路径或 IP。
     * @return 小写且去除首尾空白后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 按接口类型选择限流策略。
     * @param path 请求路径。
     * @return 对应路径的令牌桶策略。
     */
    private BucketPolicy policy(String path) {
        // 验证码接口优先使用更严格的刷新限流。
        if (path != null && path.startsWith("/api/v1/auth/captcha")) {
            return new BucketPolicy(CAPTCHA_CAPACITY, CAPTCHA_REFILL_PERIOD);
        }
        // 登录接口使用认证限流策略。
        if (path != null && path.startsWith("/api/v1/auth/login")) {
            return new BucketPolicy(AUTH_CAPACITY, AUTH_REFILL_PERIOD);
        }
        // 其他 API 使用默认限流策略。
        return new BucketPolicy(DEFAULT_CAPACITY, DEFAULT_REFILL_PERIOD);
    }

    // 描述令牌桶容量和补充周期。
    private record BucketPolicy(int capacity, Duration refillPeriod) {
    }

    // 保存单个路径和客户端组合的令牌桶运行状态。
    private static final class Bucket {
        // 当前桶允许持有的最大令牌数。
        private final int capacity;
        // 每秒恢复的令牌数。
        private final double refillTokensPerSecond;
        // 当前剩余令牌数。
        private double tokens;
        // 上一次补充令牌的时间。
        private Instant lastRefill;
        // 最近一次访问是否被允许。
        private boolean lastDecisionAllowed = true;

        /**
         * 创建令牌桶。
         * @param capacity 桶容量。
         * @param refillPeriod 从空桶恢复到满桶所需时间。
         */
        private Bucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillTokensPerSecond = (double) capacity / Math.max(1, refillPeriod.toMillis()) * 1000.0;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
        }

        /**
         * 消耗一次请求令牌。
         * @return true 表示消耗成功，false 表示令牌不足。
         */
        private boolean allow() {
            // 判断前先按时间恢复令牌。
            refill();
            // 可用令牌足够时扣减一个请求额度。
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                lastDecisionAllowed = true;
            } else {
                // 令牌不足时记录拒绝结果。
                lastDecisionAllowed = false;
            }
            return lastDecisionAllowed;
        }

        /**
         * 根据经过时间补充令牌。
         */
        private void refill() {
            // 使用当前时间计算距离上次补充的时间差。
            Instant now = Instant.now();
            long elapsedMillis = Duration.between(lastRefill, now).toMillis();
            if (elapsedMillis <= 0) {
                return;
            }
            // 补充后的令牌数不能超过桶容量。
            tokens = Math.min(capacity, tokens + (elapsedMillis * refillTokensPerSecond / 1000.0d));
            // 更新补充时间，作为下一次计算基准。
            lastRefill = now;
        }
    }
}
