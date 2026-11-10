package com.syncflow.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private static final long WINDOW_MILLIS = 60_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger operations = new AtomicInteger();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        int limit = path.endsWith("/refresh") ? 20 : 10;
        String key = request.getRemoteAddr() + ":" + path;
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) ->
            current == null || now - current.startedAt >= WINDOW_MILLIS ? new Window(now) : current);

        synchronized (window) {
            if (window.count >= limit) {
                long retryAfter = Math.max(1, (window.startedAt + WINDOW_MILLIS - now + 999) / 1000);
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setHeader("Retry-After", Long.toString(retryAfter));
                response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Too many authentication attempts. Try again later.\"}");
                return false;
            }
            window.count++;
        }

        if (operations.incrementAndGet() % 256 == 0) {
            long cutoff = now - WINDOW_MILLIS;
            windows.entrySet().removeIf(entry -> entry.getValue().startedAt < cutoff);
        }
        return true;
    }

    private static final class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
