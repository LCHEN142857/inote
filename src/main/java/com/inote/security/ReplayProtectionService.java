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

// 通过短时间内请求指纹去重，降低重复提交风险。
@Service
public class ReplayProtectionService {

    // 指纹重放窗口，窗口内重复视为重放。
    private static final Duration REPLAY_WINDOW = Duration.ofMillis(500);
    // 指纹记录的生存时间，防止内存无限增长。
    private static final Duration ENTRY_TTL = Duration.ofSeconds(5);

    // 记录最近出现过的请求指纹及其时间戳。
    private final Map<String, Instant> requestFingerprints = new ConcurrentHashMap<>();
    // 用于降低每次请求都触发清理的成本。
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    /**
     * 判断请求指纹是否在短时间内重复出现。
     * @param rawFingerprint 原始请求指纹。
     * @return true 表示判定为重复提交，false 表示可继续处理。
     */
    public boolean isReplay(String rawFingerprint) {
        // 控制清理频率，避免每次都遍历缓存。
        cleanupIfNeeded();
        // 对指纹做哈希，降低直接暴露原文的风险。
        String fingerprint = sha256(rawFingerprint);
        // 记录当前时间用于窗口比较。
        Instant now = Instant.now();
        // 使用可变引用记录 compute 过程中的判断结果。
        AtomicReference<Boolean> replay = new AtomicReference<>(false);

        // 原子更新最近请求时间，并判断是否落在重放窗口内。
        requestFingerprints.compute(fingerprint, (key, previous) -> {
            if (previous != null && Duration.between(previous, now).compareTo(REPLAY_WINDOW) < 0) {
                replay.set(true);
            }
            return now;
        });

        // 返回本次是否命中重放保护。
        return replay.get();
    }

    /**
     * 按固定频率清理过期指纹。
     */
    private void cleanupIfNeeded() {
        // 每 256 次调用触发一次过期清理。
        if (cleanupCounter.incrementAndGet() % 256 != 0) {
            return;
        }

        // 移除超出存活期的历史指纹。
        Instant threshold = Instant.now().minus(ENTRY_TTL);
        requestFingerprints.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }

    /**
     * 计算请求指纹的 SHA-256 摘要。
     * @param value 原始指纹字符串。
     * @return 十六进制摘要。
     * @throws IllegalStateException SHA-256 不可用时抛出。
     */
    private String sha256(String value) {
        try {
            // 使用标准消息摘要生成固定长度指纹。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 统一使用 UTF-8 编码保证跨节点一致。
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
