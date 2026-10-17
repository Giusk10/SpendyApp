package com.spendy.gateway;

import com.spendy.gateway.Filter.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for Gateway CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate correlation ID in Gateway when missing and attach to downstream request and response")
    void testFilterGeneratesCorrelationId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/Expense/rest/expense/getExpenses").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String reqCid = mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertNotNull(reqCid);
            assertFalse(reqCid.isBlank());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String resCid = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(resCid);
        assertFalse(resCid.isBlank());
    }

    @Test
    @DisplayName("Should preserve and propagate existing correlation ID")
    void testFilterPropagatesExistingCorrelationId() {
        String existingId = "existing-frontend-trace-456";
        MockServerHttpRequest request = MockServerHttpRequest.get("/Expense/rest/expense/getExpenses")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String reqCid = mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertEquals(existingId, reqCid);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String resCid = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertEquals(existingId, resCid);
    }
}
