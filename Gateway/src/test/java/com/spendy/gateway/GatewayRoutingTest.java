package com.spendy.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.allow-ephemeral-keys=true"
})
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("Root welcome endpoint returns 200 OK")
    void testRootEndpoint() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Gateway public Actuator health returns 200 OK")
    void testActuatorHealthPermitAll() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Gateway administrative routes actuator is protected and returns 401 Unauthorized")
    void testActuatorGatewayRoutesProtected_Returns401() {
        webTestClient.get().uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Downstream actuator health paths through Gateway require authentication (401)")
    void testDownstreamActuatorHealthProtected_Returns401() {
        webTestClient.get().uri("/Auth/actuator/health")
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get().uri("/Expense/actuator/health")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("AuthMicroService route matches /Auth/** and rewrites path to /rest/auth/health")
    void testAuthRouteMatchingAndRewrite() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes, "Routes list should not be null");
        assertTrue(routes.size() >= 2, "Expected at least 2 routes loaded in Gateway");

        Route authRoute = routes.stream()
                .filter(r -> "AuthMicroService".equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("AuthMicroService route not found in RouteLocator"));

        MockServerHttpRequest request = MockServerHttpRequest.get("/Auth/rest/auth/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean predicateMatches = Boolean.TRUE.equals(Mono.from(authRoute.getPredicate().apply(exchange)).block());
        assertTrue(predicateMatches, "Predicate Path=/Auth/** must match /Auth/rest/auth/health");

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, authRoute.getUri());

        final String[] actualRewritten = new String[1];
        final URI[] actualTargetUri = new URI[1];

        GatewayFilterChain chain = new GatewayFilterChain() {
            private int index = 0;
            @Override
            public Mono<Void> filter(ServerWebExchange ex) {
                if (index < authRoute.getFilters().size()) {
                    var filter = authRoute.getFilters().get(index++);
                    return filter.filter(ex, this);
                } else {
                    actualRewritten[0] = ex.getRequest().getURI().getRawPath();
                    actualTargetUri[0] = ex.getAttribute(GATEWAY_REQUEST_URL_ATTR);
                    return Mono.empty();
                }
            }
        };
        chain.filter(exchange).block();

        assertEquals("/rest/auth/health", actualRewritten[0], "Path should be stripped of /Auth prefix");
        assertNotNull(actualTargetUri[0]);
        assertEquals("/rest/auth/health", actualTargetUri[0].getRawPath());
    }

    @Test
    @DisplayName("AuthMicroService route correctly rewrites OpenAPI documentation path /Auth/v3/api-docs")
    void testAuthOpenApiRouteRewrite() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);

        Route authRoute = routes.stream()
                .filter(r -> "AuthMicroService".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        MockServerHttpRequest request = MockServerHttpRequest.get("/Auth/v3/api-docs").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean predicateMatches = Boolean.TRUE.equals(Mono.from(authRoute.getPredicate().apply(exchange)).block());
        assertTrue(predicateMatches, "Predicate Path=/Auth/** must match /Auth/v3/api-docs");

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, authRoute.getUri());

        final String[] actualRewritten = new String[1];
        GatewayFilterChain chain = new GatewayFilterChain() {
            private int index = 0;
            @Override
            public Mono<Void> filter(ServerWebExchange ex) {
                if (index < authRoute.getFilters().size()) {
                    var filter = authRoute.getFilters().get(index++);
                    return filter.filter(ex, this);
                } else {
                    actualRewritten[0] = ex.getRequest().getURI().getRawPath();
                    return Mono.empty();
                }
            }
        };
        chain.filter(exchange).block();

        assertEquals("/v3/api-docs", actualRewritten[0], "Path should be rewritten to /v3/api-docs");
    }

    @Test
    @DisplayName("ExpenseMicroService route matches /Expense/** and rewrites path to /rest/expense/getExpenses")
    void testExpenseRouteMatchingAndRewrite() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);

        Route expenseRoute = routes.stream()
                .filter(r -> "ExpenseMicroService".equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ExpenseMicroService route not found in RouteLocator"));

        MockServerHttpRequest request = MockServerHttpRequest.get("/Expense/rest/expense/getExpenses").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean predicateMatches = Boolean.TRUE.equals(Mono.from(expenseRoute.getPredicate().apply(exchange)).block());
        assertTrue(predicateMatches, "Predicate Path=/Expense/** must match /Expense/rest/expense/getExpenses");

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, expenseRoute.getUri());

        final String[] actualExpenseRewritten = new String[1];
        GatewayFilterChain chain = new GatewayFilterChain() {
            private int index = 0;
            @Override
            public Mono<Void> filter(ServerWebExchange ex) {
                if (index < expenseRoute.getFilters().size()) {
                    var filter = expenseRoute.getFilters().get(index++);
                    return filter.filter(ex, this);
                } else {
                    actualExpenseRewritten[0] = ex.getRequest().getURI().getRawPath();
                    return Mono.empty();
                }
            }
        };
        chain.filter(exchange).block();

        assertEquals("/rest/expense/getExpenses", actualExpenseRewritten[0], "Path should be stripped of /Expense prefix");
    }

    @Test
    @DisplayName("AuthMicroService route matches /Auth/rest/auth/jwks.json and rewrites to /rest/auth/jwks.json")
    void testAuthJwksRouteMatchingAndRewrite() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);

        Route authRoute = routes.stream()
                .filter(r -> "AuthMicroService".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        MockServerHttpRequest request = MockServerHttpRequest.get("/Auth/rest/auth/jwks.json").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean predicateMatches = Boolean.TRUE.equals(Mono.from(authRoute.getPredicate().apply(exchange)).block());
        assertTrue(predicateMatches, "Predicate Path=/Auth/** must match /Auth/rest/auth/jwks.json");

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, authRoute.getUri());

        final String[] actualRewritten = new String[1];
        GatewayFilterChain chain = new GatewayFilterChain() {
            private int index = 0;
            @Override
            public Mono<Void> filter(ServerWebExchange ex) {
                if (index < authRoute.getFilters().size()) {
                    var filter = authRoute.getFilters().get(index++);
                    return filter.filter(ex, this);
                } else {
                    actualRewritten[0] = ex.getRequest().getURI().getRawPath();
                    return Mono.empty();
                }
            }
        };
        chain.filter(exchange).block();

        assertEquals("/rest/auth/jwks.json", actualRewritten[0], "Path should be rewritten to /rest/auth/jwks.json");
    }

    @Test
    @DisplayName("AuthMicroService route matches /Auth/rest/auth/login and rewrites to /rest/auth/login")
    void testAuthLoginRouteMatchingAndRewrite() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);

        Route authRoute = routes.stream()
                .filter(r -> "AuthMicroService".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        MockServerHttpRequest request = MockServerHttpRequest.post("/Auth/rest/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean predicateMatches = Boolean.TRUE.equals(Mono.from(authRoute.getPredicate().apply(exchange)).block());
        assertTrue(predicateMatches, "Predicate Path=/Auth/** must match /Auth/rest/auth/login");

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, authRoute.getUri());

        final String[] actualRewritten = new String[1];
        GatewayFilterChain chain = new GatewayFilterChain() {
            private int index = 0;
            @Override
            public Mono<Void> filter(ServerWebExchange ex) {
                if (index < authRoute.getFilters().size()) {
                    var filter = authRoute.getFilters().get(index++);
                    return filter.filter(ex, this);
                } else {
                    actualRewritten[0] = ex.getRequest().getURI().getRawPath();
                    return Mono.empty();
                }
            }
        };
        chain.filter(exchange).block();

        assertEquals("/rest/auth/login", actualRewritten[0], "Path should be rewritten to /rest/auth/login");
    }

    @Test
    @DisplayName("Protected Expense endpoints return 401 Unauthorized without bearer token")
    void testExpenseEndpointRequiresAuthentication_Returns401() {
        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Protected Auth endpoints return 401 Unauthorized without bearer token")
    void testAuthProfileRequiresAuthentication_Returns401() {
        webTestClient.get().uri("/Auth/rest/auth/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Unmapped non-public endpoints return 401 Unauthorized due to anyExchange().authenticated()")
    void testUnmappedEndpoint_Returns401() {
        webTestClient.get().uri("/admin/secret-dashboard")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
