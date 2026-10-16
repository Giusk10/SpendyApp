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
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter providing rate limiting on sensitive authentication endpoints (login, register, refresh)
 * to mitigate brute-force and denial-of-service abuse.
 *
 * Implements:
 * - Strict verification of X-Forwarded-For only from configured/trusted proxies
 * - Accurate dynamic Retry-After header calculation
 * - In-memory bounded cache with automatic eviction of expired windows
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${rate.limiting.enabled:true}")
    private boolean rateLimitingEnabled = true;

    @Value("${rate.limiting.trusted-proxies:127.0.0.1,::1,localhost,0:0:0:0:0:0:0:1}")
    private String trustedProxiesConfig = "127.0.0.1,::1,localhost,0:0:0:0:0:0:0:1";

    // Window duration: 1 minute (60,000 ms)
    public static final long WINDOW_MS = 60_000L;
    public static final int MAX_LOGIN_PER_WINDOW = 10;
    public static final int MAX_REGISTER_PER_WINDOW = 5;
    public static final int MAX_REFRESH_PER_WINDOW = 30;
    private static final int MAX_TRACKED_ENTRIES = 5000;

    public static class RequestCounter {
        public final long windowStart;
        public final AtomicInteger count;

        public RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final Set<String> trustedProxies = Collections.synchronizedSet(new HashSet<>());

    public RateLimitingFilter() {
        initTrustedProxies();
    }

    private void initTrustedProxies() {
        trustedProxies.clear();
        if (trustedProxiesConfig != null) {
            for (String p : trustedProxiesConfig.split(",")) {
                String trimmed = p.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    trustedProxies.add(trimmed);
                }
            }
        }
    }

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
            String clientIp = resolveClientIp(request);
            String key = clientIp + ":" + path;
            long now = System.currentTimeMillis();

            // Evict expired entries if table exceeds threshold to prevent memory growth
            evictExpiredEntriesIfOverCapacity(now);

            RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RequestCounter(now);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > maxAllowed) {
                long elapsed = now - counter.windowStart;
                long remainingSeconds = Math.max(1, (WINDOW_MS - elapsed + 999) / 1000);

                log.warn("Rate limit exceeded for IP {} on endpoint {}. Count: {}, max: {}. Retry-After: {}s",
                        clientIp, path, counter.count.get(), maxAllowed, remainingSeconds);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(remainingSeconds));
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

    /**
     * Resolves the client IP address. Accepts X-Forwarded-For ONLY if the direct
     * caller is a configured trusted proxy or internal private/loopback network.
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) {
                String clientIp = xf.split(",")[0].trim();
                if (isValidIp(clientIp)) {
                    return clientIp;
                }
            }
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        String clean = remoteAddr.trim().toLowerCase();
        if (trustedProxies.contains(clean) || clean.equals("127.0.0.1") || clean.equals("::1") || clean.equals("localhost")) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(clean);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        // Basic check for valid IPv4 / IPv6 structure
        return ip.matches("^[0-9a-fA-F:.]+$");
    }

    private void evictExpiredEntriesIfOverCapacity(long now) {
        if (requestCounts.size() >= MAX_TRACKED_ENTRIES) {
            requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
        }
    }

    public void pruneExpiredEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
    }

    public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
        this.rateLimitingEnabled = rateLimitingEnabled;
    }

    public boolean isRateLimitingEnabled() {
        return rateLimitingEnabled;
    }

    public void addTrustedProxy(String proxyIp) {
        if (proxyIp != null) {
            trustedProxies.add(proxyIp.trim().toLowerCase());
        }
    }

    public void clearRates() {
        requestCounts.clear();
    }

    public int getTrackedEntriesCount() {
        return requestCounts.size();
    }
}
