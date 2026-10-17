package com.spendyteam.expense.Exception;

import com.spendyteam.expense.DTO.ExpenseErrorResponse;
import com.spendyteam.expense.Security.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/rest/expense/test");
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-id-1234");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with HTTP 400 Bad Request")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument value");
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid argument value", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("/rest/expense/test", response.getBody().getPath());
        assertEquals("test-corr-id-1234", response.getBody().getCorrelationId());
    }

    @Test
    @DisplayName("Should handle DateTimeParseException with HTTP 400 Bad Request")
    void testHandleFormatExceptions() {
        DateTimeParseException ex = new DateTimeParseException("Text could not be parsed", "bad-date", 0);
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleFormatExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Invalid format"));
    }

    @Test
    @DisplayName("Should handle MaxUploadSizeExceededException with HTTP 400 Bad Request")
    void testHandleMaxUploadSize() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10485760);
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleMaxSizeException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("exceeds maximum permitted upload limit"));
    }

    @Test
    @DisplayName("Should handle AuthenticationException with HTTP 401 Unauthorized")
    void testHandleAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Bad token");
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with HTTP 403 Forbidden")
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should handle NoSuchElementException with HTTP 404 Not Found")
    void testHandleNotFound() {
        NoSuchElementException ex = new NoSuchElementException("Expense not found");
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Expense not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle unexpected Exception with HTTP 500 without leaking stack traces")
    void testHandleGenericException() {
        NullPointerException ex = new NullPointerException("Secret internal DB crash detail");
        ResponseEntity<ExpenseErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        // Crucial: verify internal exception detail is sanitized and NOT leaked
        assertFalse(response.getBody().getMessage().contains("Secret internal DB crash detail"));
        assertTrue(response.getBody().getMessage().contains("internal error occurred"));
        assertEquals("test-corr-id-1234", response.getBody().getCorrelationId());
    }
}
