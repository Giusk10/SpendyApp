package com.spendy.auth.Controller;

import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import com.spendy.auth.Utility.UserResult;
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
     * Verifica ricerca utenti per houseId con utenti trovati: deve restituire 200 OK e una lista di utenti
     */
    @Test
    void testGetUserByHouseIdUsersFound() {
        // Crea una richiesta con houseId valido
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "HOUSE123");
        UserResult result = mock(UserResult.class);
        // Simula il comportamento del servizio di ricerca utenti
        when(result.getStatusAuth()).thenReturn(StatusAuth.USERS_FOUNDED);
        when(result.getUsers()).thenReturn(Arrays.asList(new User(), new User()));
        when(authService.getUserByHouseId("HOUSE123")).thenReturn(result);

        // Esegue la ricerca tramite il controller
        Response response = authController.getUserByHouseId(requestBody);

        // Verifica che la risposta sia 200 OK e contenga una lista di utenti
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertInstanceOf(List.class, response.getEntity());
    }

    /**
     * Verifica ricerca utenti per houseId senza utenti trovati: deve restituire 404 NOT FOUND e messaggio di errore
     */
    @Test
    void testGetUserByHouseIdUsersNotFound() {
        // Crea una richiesta con houseId valido ma senza utenti associati
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "HOUSE123");
        UserResult result = mock(UserResult.class);
        // Simula il comportamento del servizio di ricerca utenti
        when(result.getStatusAuth()).thenReturn(StatusAuth.USERS_NOT_FOUND);
        when(authService.getUserByHouseId("HOUSE123")).thenReturn(result);

        // Esegue la ricerca tramite il controller
        Response response = authController.getUserByHouseId(requestBody);

        // Verifica che la risposta sia 404 NOT FOUND e il messaggio corretto
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("No users found for the given house ID", response.getEntity());
    }
}