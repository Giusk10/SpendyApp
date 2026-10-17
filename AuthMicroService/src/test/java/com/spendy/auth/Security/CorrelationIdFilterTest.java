package com.spendy.auth.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for Auth CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate correlation ID if missing in AuthMicroService")
    void testGeneratesCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String header = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(header);
        assertFalse(header.isBlank());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Should propagate existing correlation ID in AuthMicroService")
    void testPropagatesCorrelationId() throws ServletException, IOException {
        String existingId = "auth-trace-id-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertEquals(existingId, MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, filterChain);

        assertEquals(existingId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
