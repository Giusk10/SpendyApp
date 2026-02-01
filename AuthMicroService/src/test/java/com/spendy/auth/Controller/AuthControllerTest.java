package com.spendy.auth.Controller;

import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Abilita l'uso di Mockito nei test JUnit
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    // Mock del servizio di autenticazione
    @Mock
    private AuthService authService;

    // Inietta i mock nella classe AuthController
    @InjectMocks
    private AuthController authController;

    /**
     * Verifica login con username valido: deve restituire 200 OK e il token
     */
    @Test
    void testLoginSuccessWithUsername() {
        // Crea un utente di test con username e password
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, "token123");
        // Simula il comportamento del servizio di autenticazione
        when(authService.login("testuser", "password")).thenReturn(result);

        // Esegue il login tramite il controller
        Response response = authController.login(user);

        // Verifica che la risposta sia 200 OK e contenga il token
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("token123"));
        verify(authService, times(1)).login("testuser", "password");
    }

    /**
     * Verifica login con email valida (username nullo): deve restituire 200 OK e il token
     */
    @Test
    void testLoginSuccessWithEmail() {
        // Crea un utente di test con email e password
        User user = new User();
        user.setUsername(null);
        user.setEmail("test@example.com");
        user.setPassword("password");
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, "token456");
        // Simula il comportamento del servizio di autenticazione
        when(authService.login("test@example.com", "password")).thenReturn(result);

        // Esegue il login tramite il controller
        Response response = authController.login(user);

        // Verifica che la risposta sia 200 OK e contenga il token
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("token456"));
    }

    /**
     * Verifica login con utente non trovato: deve restituire 404 NOT FOUND e messaggio di errore
     */
    @Test
    void testLoginUserNotFound() {
        // Crea un utente di test con username non esistente
        User user = new User();
        user.setUsername("notfound");
        user.setPassword("password");
        AuthResult result = new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        // Simula il comportamento del servizio di autenticazione
        when(authService.login("notfound", "password")).thenReturn(result);

        // Esegue il login tramite il controller
        Response response = authController.login(user);

        // Verifica che la risposta sia 404 NOT FOUND e il messaggio corretto
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("Utente non trovato", response.getEntity());
    }

    /**
     * Verifica login con credenziali errate: deve restituire 401 UNAUTHORIZED e messaggio di errore
     */
    @Test
    void testLoginInvalidCredentials() {
        // Crea un utente di test con password errata
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("wrongpassword");
        AuthResult result = new AuthResult(StatusAuth.INVALID_CREDENTIALS, null);
        // Simula il comportamento del servizio di autenticazione
        when(authService.login("testuser", "wrongpassword")).thenReturn(result);

        // Esegue il login tramite il controller
        Response response = authController.login(user);

        // Verifica che la risposta sia 401 UNAUTHORIZED e il messaggio corretto
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Credenziali errate o non valide", response.getEntity());
    }

    /**
     * Verifica login con errore generico (status nullo): deve restituire 500 INTERNAL SERVER ERROR
     */
    @Test
    void testLoginServerError() {
        // Crea un utente di test
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        AuthResult result = new AuthResult(null, null);
        // Simula il comportamento del servizio di autenticazione
        when(authService.login("testuser", "password")).thenReturn(result);

        // Esegue il login tramite il controller
        Response response = authController.login(user);

        // Verifica che la risposta sia 500 INTERNAL SERVER ERROR e il messaggio corretto
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Server error", response.getEntity());
    }

    /**
     * Verifica registrazione con dati validi: deve restituire 200 OK e messaggio di successo
     */
    @Test
    void testRegisterSuccess() {
        // Crea un utente di test con tutti i dati validi
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setName("Test");
        user.setSurname("User");
        user.setEmail("test@example.com");
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, null);
        // Simula il comportamento del servizio di registrazione
        when(authService.register(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(result);

        // Esegue la registrazione tramite il controller
        Response response = authController.register(user);

        // Verifica che la risposta sia 200 OK e il messaggio corretto
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Registration successful", response.getEntity());
    }

    /**
     * Verifica registrazione con email non valida: deve restituire 401 UNAUTHORIZED e messaggio di errore
     */
    @Test
    void testRegisterInvalidEmail() {
        // Crea un utente di test con email non valida
        User user = new User();
        user.setEmail("invalidemail");
        // Esegue la registrazione tramite il controller
        Response response = authController.register(user);

        // Verifica che la risposta sia 401 UNAUTHORIZED e il messaggio corretto
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Email invalido", response.getEntity());
    }

    /**
     * Verifica registrazione con utente già esistente: deve restituire 409 CONFLICT e messaggio di errore
     */
    @Test
    void testRegisterUserAlreadyExists() {
        // Crea un utente di test già esistente
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setName("Test");
        user.setSurname("User");
        user.setEmail("test@example.com");
        AuthResult result = new AuthResult(StatusAuth.USER_ALREADY_EXISTS, null);
        // Simula il comportamento del servizio di registrazione
        when(authService.register(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(result);

        // Esegue la registrazione tramite il controller
        Response response = authController.register(user);

        // Verifica che la risposta sia 409 CONFLICT e il messaggio corretto
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals("User already exists", response.getEntity());
    }

    /**
     * Verifica registrazione con errore generico: deve restituire 500 INTERNAL SERVER ERROR
     */
    @Test
    void testRegisterServerError() {
        // Crea un utente di test
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setName("Test");
        user.setSurname("User");
        user.setEmail("test@example.com");
        AuthResult result = new AuthResult(null, null);
        // Simula il comportamento del servizio di registrazione
        when(authService.register(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(result);

        // Esegue la registrazione tramite il controller
        Response response = authController.register(user);

        // Verifica che la risposta sia 500 INTERNAL SERVER ERROR e il messaggio corretto
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("An error occurred", response.getEntity());
    }

    /**
     * Verifica refresh token con token valido: deve restituire 200 OK con nuovi token
     */
    @Test
    void testRefreshTokenSuccess() {
        // Crea una richiesta con refresh token
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("refreshToken", "validRefreshToken123");

        // Simula risposta del servizio con nuovi token
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, "newAccessToken", "newRefreshToken");
        when(authService.refreshAccessToken("validRefreshToken123")).thenReturn(result);

        // Esegue il refresh tramite il controller
        Response response = authController.refreshToken(requestBody);

        // Verifica che la risposta sia 200 OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Verifica che la risposta contenga i nuovi token
        assertTrue(response.getEntity().toString().contains("newAccessToken"));
        assertTrue(response.getEntity().toString().contains("newRefreshToken"));
        verify(authService, times(1)).refreshAccessToken("validRefreshToken123");
    }

    /**
     * Verifica refresh token con token mancante: deve restituire 400 BAD REQUEST
     */
    @Test
    void testRefreshTokenMissing() {
        // Crea una richiesta senza refresh token
        Map<String, String> requestBody = new HashMap<>();

        // Esegue il refresh tramite il controller
        Response response = authController.refreshToken(requestBody);

        // Verifica che la risposta sia 400 BAD REQUEST
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Refresh Token missing", response.getEntity());
        verify(authService, never()).refreshAccessToken(anyString());
    }

    /**
     * Verifica refresh token con token vuoto: deve restituire 400 BAD REQUEST
     */
    @Test
    void testRefreshTokenEmpty() {
        // Crea una richiesta con refresh token vuoto
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("refreshToken", "");

        // Esegue il refresh tramite il controller
        Response response = authController.refreshToken(requestBody);

        // Verifica che la risposta sia 400 BAD REQUEST
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Refresh Token missing", response.getEntity());
        verify(authService, never()).refreshAccessToken(anyString());
    }

    /**
     * Verifica refresh token con token invalido/scaduto: deve restituire 401 UNAUTHORIZED
     */
    @Test
    void testRefreshTokenInvalidOrExpired() {
        // Crea una richiesta con refresh token invalido
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("refreshToken", "invalidToken");

        // Simula risposta del servizio con errore
        AuthResult result = new AuthResult(StatusAuth.TOKEN_INVALID, null);
        when(authService.refreshAccessToken("invalidToken")).thenReturn(result);

        // Esegue il refresh tramite il controller
        Response response = authController.refreshToken(requestBody);

        // Verifica che la risposta sia 401 UNAUTHORIZED
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Refresh Token expired or invalid", response.getEntity());
    }

    /**
     * Verifica getProfile con token valido: deve restituire 200 OK con dati del profilo
     */
    @Test
    void testGetProfileSuccess() {
        // Crea header di autorizzazione con token valido
        String authHeader = "Bearer validAccessToken";

        // Crea utente di test
        User user = new User();
        user.setUsername("testuser");
        user.setName("Test");
        user.setSurname("User");
        user.setEmail("test@example.com");

        // Simula risposta del servizio
        when(authService.getUserFromToken("validAccessToken")).thenReturn(user);

        // Esegue la richiesta del profilo
        Response response = authController.getProfile(authHeader);

        // Verifica che la risposta sia 200 OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Verifica che la risposta contenga i dati corretti
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("testuser"));
        assertTrue(entity.contains("Test"));
        assertTrue(entity.contains("User"));
        assertTrue(entity.contains("test@example.com"));
    }

    /**
     * Verifica getProfile senza header Authorization: deve restituire 401 UNAUTHORIZED
     */
    @Test
    void testGetProfileMissingAuthHeader() {
        // Esegue la richiesta senza header
        Response response = authController.getProfile(null);

        // Verifica che la risposta sia 401 UNAUTHORIZED
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Missing or invalid Authorization header", response.getEntity());
        verify(authService, never()).getUserFromToken(anyString());
    }

    /**
     * Verifica getProfile con header Authorization invalido: deve restituire 401 UNAUTHORIZED
     */
    @Test
    void testGetProfileInvalidAuthHeader() {
        // Header senza "Bearer " prefix
        String authHeader = "InvalidToken";

        // Esegue la richiesta con header invalido
        Response response = authController.getProfile(authHeader);

        // Verifica che la risposta sia 401 UNAUTHORIZED
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Missing or invalid Authorization header", response.getEntity());
        verify(authService, never()).getUserFromToken(anyString());
    }

    /**
     * Verifica getProfile con token scaduto/invalido: deve restituire 401 UNAUTHORIZED
     */
    @Test
    void testGetProfileInvalidToken() {
        String authHeader = "Bearer invalidToken";

        // Simula risposta del servizio con utente null
        when(authService.getUserFromToken("invalidToken")).thenReturn(null);

        // Esegue la richiesta del profilo
        Response response = authController.getProfile(authHeader);

        // Verifica che la risposta sia 401 UNAUTHORIZED
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Invalid access token", response.getEntity());
    }

    /**
     * Verifica updateProfile con token valido: deve restituire 200 OK
     */
    @Test
    void testUpdateProfileSuccess() {
        String authHeader = "Bearer validAccessToken";

        // Crea dati aggiornati
        User updatedUser = new User();
        updatedUser.setName("NewName");
        updatedUser.setSurname("NewSurname");

        // Simula risposta del servizio con successo
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, null);
        when(authService.updateUserProfile("validAccessToken", updatedUser)).thenReturn(result);

        // Esegue l'aggiornamento del profilo
        Response response = authController.updateProfile(authHeader, updatedUser);

        // Verifica che la risposta sia 200 OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Profile updated successfully", response.getEntity());
        verify(authService, times(1)).updateUserProfile("validAccessToken", updatedUser);
    }

    /**
     * Verifica updateProfile senza header Authorization: deve restituire 401 UNAUTHORIZED
     */
    @Test
    void testUpdateProfileMissingAuthHeader() {
        User updatedUser = new User();
        updatedUser.setName("NewName");

        // Esegue l'aggiornamento senza header
        Response response = authController.updateProfile(null, updatedUser);

        // Verifica che la risposta sia 401 UNAUTHORIZED
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Missing or invalid Authorization header", response.getEntity());
        verify(authService, never()).updateUserProfile(anyString(), any(User.class));
    }

    /**
     * Verifica updateProfile con utente non trovato: deve restituire 404 NOT FOUND
     */
    @Test
    void testUpdateProfileUserNotFound() {
        String authHeader = "Bearer validAccessToken";

        User updatedUser = new User();
        updatedUser.setName("NewName");

        // Simula risposta del servizio con utente non trovato
        AuthResult result = new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        when(authService.updateUserProfile("validAccessToken", updatedUser)).thenReturn(result);

        // Esegue l'aggiornamento del profilo
        Response response = authController.updateProfile(authHeader, updatedUser);

        // Verifica che la risposta sia 404 NOT FOUND
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("User not found", response.getEntity());
    }

    /**
     * Verifica updateProfile con errore generico: deve restituire 500 INTERNAL SERVER ERROR
     */
    @Test
    void testUpdateProfileServerError() {
        String authHeader = "Bearer validAccessToken";

        User updatedUser = new User();
        updatedUser.setName("NewName");

        // Simula risposta del servizio con errore generico
        AuthResult result = new AuthResult(StatusAuth.TOKEN_INVALID, null);
        when(authService.updateUserProfile("validAccessToken", updatedUser)).thenReturn(result);

        // Esegue l'aggiornamento del profilo
        Response response = authController.updateProfile(authHeader, updatedUser);

        // Verifica che la risposta sia 500 INTERNAL SERVER ERROR
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("An error occurred", response.getEntity());
    }

    /**
     * Verifica che l'endpoint healthCheck restituisca 200 OK
     */
    @Test
    void testHealthCheck() {
        // Esegue il health check
        Response response = authController.healthCheck();

        // Verifica che la risposta sia 200 OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("AuthMicroService is running", response.getEntity());
    }

    /**
     * Verifica login con successo che restituisca anche il refresh token
     */
    @Test
    void testLoginSuccessWithRefreshToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");

        // Simula risposta con access e refresh token
        AuthResult result = new AuthResult(StatusAuth.SUCCESS, "accessToken123", "refreshToken456");
        when(authService.login("testuser", "password")).thenReturn(result);

        Response response = authController.login(user);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("accessToken123"));
        assertTrue(entity.contains("refreshToken456"));
    }
}