package com.spendyteam.expense.Security;

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

@DisplayName("Unit Tests for CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate new correlation ID if not provided in request header")
    void testGeneratesCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String correlationHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(correlationHeader);
        assertFalse(correlationHeader.isBlank());
        // Verify MDC is cleared after request processing
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Should propagate incoming correlation ID in response header")
    void testPropagatesExistingCorrelationId() throws ServletException, IOException {
        String existingId = "client-trace-id-xyz-987";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            // Inside the filter chain, MDC must contain the incoming ID
            assertEquals(existingId, MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, filterChain);

        assertEquals(existingId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
