package com.spendy.auth.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Test di Sicurezza RateLimitingFilter (Anti-Abuso e Proxy Fidati)")
class RateLimitingFilterSecurityTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filter.clearRates();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Login limitato a 10 tentativi per finestra di 1 minuto, poi HTTP 429 con Retry-After")
    void testLoginRateLimitExceeded() throws ServletException, IOException {
        String clientIp = "192.168.1.100";

        for (int i = 1; i <= RateLimitingFilter.MAX_LOGIN_PER_WINDOW; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Auth/rest/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertEquals(HttpStatus.OK.value(), response.getStatus());
            verify(filterChain, times(i)).doFilter(any(), any());
        }

        // 11th attempt must be rejected with 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/Auth/rest/auth/login");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilterInternal(blockedRequest, blockedResponse, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blockedResponse.getStatus());
        assertNotNull(blockedResponse.getHeader("Retry-After"));
        int retryAfter = Integer.parseInt(blockedResponse.getHeader("Retry-After"));
        assertTrue(retryAfter > 0 && retryAfter <= 60);
        assertTrue(blockedResponse.getContentAsString().contains("Too many requests"));
    }

    @Test
    @DisplayName("Registrazione limitata a 5 richieste per finestra")
    void testRegisterRateLimitExceeded() throws ServletException, IOException {
        String clientIp = "192.168.1.101";

        for (int i = 1; i <= RateLimitingFilter.MAX_REGISTER_PER_WINDOW; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Auth/rest/auth/register");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);
            assertEquals(HttpStatus.OK.value(), response.getStatus());
        }

        // 6th attempt must be rejected
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/Auth/rest/auth/register");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilterInternal(blockedRequest, blockedResponse, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blockedResponse.getStatus());
    }

    @Test
    @DisplayName("X-Forwarded-For accettato solo se il chiamante diretto è un proxy fidato")
    void testTrustedProxyXForwardedForAccepted() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Auth/rest/auth/login");
        // Localhost/loopback is trusted
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        String resolved = filter.resolveClientIp(request);
        assertEquals("203.0.113.195", resolved, "Deve estrarre il primo IP client dell'header inoltrato da un proxy fidato");
    }

    @Test
    @DisplayName("X-Forwarded-For ignorato (anti-spoofing) se il chiamante diretto non è un proxy fidato")
    void testUntrustedDirectCallerXForwardedForIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Auth/rest/auth/login");
        // Public untrusted IP directly hitting the service
        request.setRemoteAddr("198.51.100.50");
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        String resolved = filter.resolveClientIp(request);
        assertEquals("198.51.100.50", resolved, "Non deve fidarsi dell'header X-Forwarded-For proveniente da un host non fidato");
    }

    @Test
    @DisplayName("Pulizia della memoria: prune delle voci scadute")
    void testPruneExpiredEntries() {
        filter.clearRates();
        assertEquals(0, filter.getTrackedEntriesCount());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Auth/rest/auth/login");
        request.setRemoteAddr("192.168.1.102");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));
        assertEquals(1, filter.getTrackedEntriesCount());

        filter.pruneExpiredEntries();
        // Since window is still fresh, entry remains
        assertEquals(1, filter.getTrackedEntriesCount());
    }
}
