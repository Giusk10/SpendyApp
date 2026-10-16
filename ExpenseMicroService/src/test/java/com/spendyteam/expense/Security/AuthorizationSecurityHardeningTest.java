package com.spendyteam.expense.Security;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Service.*;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 10 - Test di Autorizzazione e Prevenzione IDOR (ExpenseMicroService)")
class AuthorizationSecurityHardeningTest {

    @Mock
    private IExpenseRepository expenseRepository;

    @Mock
    private SmartCategorizerService smartCategorizerService;

    @Mock
    private ExpenseCsvParser expenseCsvParser;

    @Mock
    private ExpenseBatchWriter expenseBatchWriter;

    private UserAuthService userAuthService;
    private ExpenseService expenseService;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    private static final String ISSUER = "spendy-auth";
    private static final String AUDIENCE = "spendy-app";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        userAuthService = new UserAuthService(publicKey, ISSUER, AUDIENCE);
        expenseService = new ExpenseService(
                expenseRepository,
                smartCategorizerService,
                userAuthService,
                new ExpenseDataParser(),
                expenseCsvParser,
                expenseBatchWriter
        );
    }

    private String createValidToken(String username) throws Exception {
        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject(username)
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .jwtID(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_USER"))
                .build();

        com.nimbusds.jwt.SignedJWT signedJWT = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256)
                        .keyID("spendy-auth-key-1")
                        .build(),
                claims
        );
        signedJWT.sign(new com.nimbusds.jose.crypto.RSASSASigner(privateKey));
        return signedJWT.serialize();
    }

    // 1. Utente non autenticato bloccato sugli endpoint protetti
    @Test
    @DisplayName("1. Utente non autenticato o con token assente/invalido bloccato con 401 UNAUTHORIZED")
    void testUnauthenticatedUserBlocked() {
        Response responseNoToken = expenseService.getExpenses(null);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), responseNoToken.getStatus());

        Response responseInvalidToken = expenseService.getExpenses("invalid.bearer.token");
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), responseInvalidToken.getStatus());
    }

    // 2. Utente autenticato autorizzato ad accedere alle proprie risorse
    @Test
    @DisplayName("2. Utente autenticato autorizzato ad accedere alle proprie spese")
    void testAuthenticatedUserAuthorizedForOwnResources() throws Exception {
        String tokenUserA = createValidToken("userA");

        Expense expA = new Expense();
        expA.setId("exp-1");
        expA.setUsername("userA");
        expA.setAmount(new BigDecimal("50.00"));

        when(expenseRepository.findByUsername(eq("userA"), any(Sort.class))).thenReturn(List.of(expA));

        Response response = expenseService.getExpenses(tokenUserA);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    // 3. Utente A impossibilitato ad accedere alle spese dell'utente B (Prevenzione IDOR su cancellazione e modifica)
    @Test
    @DisplayName("3. IDOR Prevention: Utente A non può cancellare o modificare una spesa dell'Utente B (403 FORBIDDEN)")
    void testUserCannotModifyOrDeleteOtherUserExpense() throws Exception {
        String tokenUserA = createValidToken("userA");

        Expense expenseOfUserB = new Expense();
        expenseOfUserB.setId("exp-user-b-999");
        expenseOfUserB.setUsername("userB");
        expenseOfUserB.setAmount(new BigDecimal("100.00"));

        when(expenseRepository.findById("exp-user-b-999")).thenReturn(Optional.of(expenseOfUserB));

        // Tentativo di cancellazione della spesa dell'Utente B da parte di Utente A
        Response deleteResponse = expenseService.deleteExpense("exp-user-b-999", tokenUserA);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), deleteResponse.getStatus(),
                "La cancellazione di una risorsa appartenente a un altro utente deve restituire 403 FORBIDDEN");
        verify(expenseRepository, never()).deleteById("exp-user-b-999");

        // Tentativo di modifica della spesa dell'Utente B da parte di Utente A
        Map<String, String> updateBody = new HashMap<>();
        updateBody.put("id", "exp-user-b-999");
        updateBody.put("amount", "200.00");

        Response updateResponse = expenseService.updateExpense(updateBody, tokenUserA);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateResponse.getStatus(),
                "La modifica di una risorsa di un altro utente deve restituire 403 FORBIDDEN");
    }

    // 4. Modifica di userId o identificativi arbitrari nel body rifiutata (identità tratta rigorosamente dal token)
    @Test
    @DisplayName("4. L'identità è derivata esclusivamente dal token verificato: parametri spoofed nel body vengono ignorati")
    void testIdentityDerivedStrictlyFromVerifiedToken() throws Exception {
        String tokenUserA = createValidToken("userA");

        Map<String, String> newExpenseBody = new HashMap<>();
        newExpenseBody.put("description", "Lunch");
        newExpenseBody.put("amount", "15.00");
        // Tentativo dell'utente A di creare una spesa attribuendola all'utente B tramite parametro nel body
        newExpenseBody.put("username", "userB");
        newExpenseBody.put("userId", "userB");

        Response response = expenseService.addExpense(newExpenseBody, tokenUserA);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository, times(1)).save(captor.capture());

        assertEquals("userA", captor.getValue().getUsername(),
                "La spesa deve essere attribuita unicamente all'utente autenticato dal token (userA) e non al parametro spoofed (userB)");
    }

    // 5. Direct access to microservice verifies token autonomously (Defense-in-depth)
    @Test
    @DisplayName("5. Defense-in-depth: Il microservizio convalida autonomamente il token crittografico anche in accesso diretto")
    void testDirectAccessEnforcesAutonomousValidation() {
        // Tentativo di inviare una richiesta con token fittizio o privo di firma crittografica valida
        String fakeToken = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalidsig";

        Response response = expenseService.getExpenses(fakeToken);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(),
                "L'accesso diretto con token non firmato o alterato deve essere bloccato autonomamente dal microservizio");
    }
}
