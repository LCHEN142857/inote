package com.inote.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// 缓存请求体，保证过滤器读取 body 后下游仍能再次读取。
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    // 保存原始请求体字节，用于重放输入流。
    private final byte[] cachedBody;

    /**
     * 读取并缓存请求体。
     * @param request 原始 HTTP 请求。
     * @throws IOException 请求体读取失败时抛出。
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * 返回缓存请求体的防御性副本。
     * @return 请求体字节副本。
     */
    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    /**
     * 重新构造可读取缓存内容的 Servlet 输入流。
     * @return 基于缓存请求体的新输入流。
     */
    @Override
    public ServletInputStream getInputStream() {
        // 每次调用都创建新的字节流，避免重复读取时游标互相影响。
        ByteArrayInputStream input = new ByteArrayInputStream(cachedBody);
        // 适配 ServletInputStream 接口以供后续过滤器和控制器读取。
        return new ServletInputStream() {
            /**
             * 判断缓存请求体是否已经读完。
             * @return 读完返回 true，否则返回 false。
             */
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            /**
             * 表示当前缓存输入流可立即读取。
             * @return 始终返回 true。
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 拒绝异步读取监听，因为缓存包装仅支持同步读取。
             * @param readListener Servlet 异步读取监听器。
             * @throws UnsupportedOperationException 始终抛出，表示不支持异步 body 读取。
             */
            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Async request body reads are not supported.");
            }

            /**
             * 从缓存请求体中读取下一个字节。
             * @return 下一个字节，读完时返回 -1。
             */
            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 基于请求字符集创建文本读取器。
     * @return 可重复读取缓存请求体的字符流。
     */
    @Override
    public BufferedReader getReader() {
        // 请求未声明编码时使用 UTF-8 兼容 JSON 和表单默认处理。
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(getCharacterEncoding());
        // 使用缓存输入流构造 reader，避免消耗原始请求体。
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
