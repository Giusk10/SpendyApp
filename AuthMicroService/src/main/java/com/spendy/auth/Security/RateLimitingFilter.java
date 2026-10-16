package com.spendy.auth.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter providing rate limiting on sensitive authentication endpoints (login, register, refresh)
 * to mitigate brute-force and denial-of-service abuse.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${rate.limiting.enabled:true}")
    private boolean rateLimitingEnabled = true;

    // Window duration: 1 minute (60,000 ms)
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_LOGIN_PER_WINDOW = 10;
    private static final int MAX_REGISTER_PER_WINDOW = 5;
    private static final int MAX_REFRESH_PER_WINDOW = 30;

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        int maxAllowed = getMaxAllowedForPath(path);

        if (maxAllowed > 0 && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = getClientIp(request);
            String key = clientIp + ":" + path;
            long now = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RequestCounter(now);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > maxAllowed) {
                log.warn("Rate limit exceeded for IP {} on endpoint {}. Count: {}", clientIp, path, counter.count.get());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private int getMaxAllowedForPath(String path) {
        if (path == null) return -1;
        if (path.endsWith("/login")) return MAX_LOGIN_PER_WINDOW;
        if (path.endsWith("/register")) return MAX_REGISTER_PER_WINDOW;
        if (path.endsWith("/refresh")) return MAX_REFRESH_PER_WINDOW;
        return -1;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
        this.rateLimitingEnabled = rateLimitingEnabled;
    }

    public void clearRates() {
        requestCounts.clear();
    }
}
