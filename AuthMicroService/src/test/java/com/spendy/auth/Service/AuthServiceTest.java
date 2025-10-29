package com.spendy.auth.Service;

import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import com.spendy.auth.Utility.UserResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Estensione di Mockito per i test
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Mock del repository utente
    @Mock
    private IUserRepository userRepository;

    // Mock del WebClient per chiamate HTTP
    @Mock(answer = RETURNS_DEEP_STUBS)
    private WebClient webClient;

    // Inietta i mock nel service da testare
    @InjectMocks
    private AuthService authService;

    /**
     * Verifica che la registrazione fallisca se l'utente esiste già (restituisce USER_ALREADY_EXISTS)
     */
    @Test
    void register_UserAlreadyExists_ReturnsUserAlreadyExists() {
        // Simula utente già esistente
        when(userRepository.findByUsername("user")).thenReturn(new User());

        // Esegue la registrazione
        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        // Verifica che lo stato sia USER_ALREADY_EXISTS
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
    }

    /**
     * Verifica che la registrazione di un nuovo utente abbia successo, venga salvato e restituisca il token
     */
    @Test
    void register_NewUser_ReturnsSuccess() {
        // Simula utente non esistente
        when(userRepository.findByUsername("user")).thenReturn(null);
        // Simula risposta del WebClient con token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "testtoken"));

        // Esegue la registrazione
        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        // Verifica che lo stato sia SUCCESS e il token sia corretto
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("testtoken", result.getToken());
        // Verifica che il salvataggio sia stato chiamato una volta
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Verifica che il login fallisca se l'utente non viene trovato (restituisce USER_NOT_FOUND)
     */
    @Test
    void login_UserNotFound_ReturnsUserNotFound() {
        // Simula utente non trovato per username ed email
        when(userRepository.findByUsername("user")).thenReturn(null);
        when(userRepository.findByEmail("user")).thenReturn(null);

        // Esegue il login
        AuthResult result = authService.login("user", "pass");

        // Verifica che lo stato sia USER_NOT_FOUND
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
    }

    /**
     * Verifica che il login fallisca se la password è errata (restituisce INVALID_CREDENTIALS)
     */
    @Test
    void login_InvalidPassword_ReturnsInvalidCredentials() {
        // Crea utente con password hashata
        User user = new User("user", "nome", BCrypt.hashpw("rightpass", BCrypt.gensalt()), "cognome", "email@test.com");
        // Simula utente trovato
        when(userRepository.findByUsername("user")).thenReturn(user);

        // Esegue il login con password errata
        AuthResult result = authService.login("user", "wrongpass");

        // Verifica che lo stato sia INVALID_CREDENTIALS
        assertEquals(StatusAuth.INVALID_CREDENTIALS, result.getStatusAuth());
    }

    /**
     * Verifica che il login abbia successo con credenziali valide e restituisca il token
     */
    @Test
    void login_ValidCredentials_ReturnsSuccess() {
        // Password corretta e hashata
        String password = "rightpass";
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User("user", "nome", hashed, "cognome", "email@test.com");

        // Simula utente trovato e risposta del WebClient
        when(userRepository.findByUsername("user")).thenReturn(user);
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "testtoken"));

        // Esegue il login
        AuthResult result = authService.login("user", password);

        // Verifica che lo stato sia SUCCESS e il token sia corretto
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("testtoken", result.getToken());
    }

    /**
     * Verifica che la ricerca utenti per houseId trovi utenti e restituisca USERS_FOUNDED
     */
    @Test
    void getUserByHouseId_UsersFound_ReturnsUsersFounded() {
        // HouseId e hash corrispondente
        String houseId = "house";
        String hashedHouseId = BCrypt.hashpw(houseId, BCrypt.gensalt());
        User user = mock(User.class);

        // Simula valore di houseUser e lista utenti
        when(user.getHouseUser()).thenReturn(hashedHouseId);
        when(userRepository.findAll()).thenReturn(List.of(user));

        // Mock statico di BCrypt.checkpw per forzare true
        try (var mocked = Mockito.mockStatic(BCrypt.class)) {
            mocked.when(() -> BCrypt.checkpw(eq(houseId), anyString())).thenReturn(true);

            // Esegue la ricerca
            UserResult result = authService.getUserByHouseId(houseId);

            // Verifica che lo stato sia USERS_FOUNDED e la lista non sia nulla
            assertEquals(StatusAuth.USERS_FOUNDED, result.getStatusAuth());
            assertNotNull(result.getUsers());
        }
    }

    /**
     * Verifica che la ricerca coinquilini per houseId restituisca la lista corretta di utenti
     */
    @Test
    void getCoinquilinibyHouseId_UsersFound_ReturnsList() {
        // HouseId e hash corrispondente
        String houseId = "house";
        String hashedHouseId = BCrypt.hashpw(houseId, BCrypt.gensalt());
        User user = mock(User.class);

        // Simula valore di houseUser e lista utenti
        when(user.getHouseUser()).thenReturn(hashedHouseId);
        when(userRepository.findAll()).thenReturn(List.of(user));

        // Mock statico di BCrypt.checkpw per forzare true
        try (var mocked = Mockito.mockStatic(BCrypt.class)) {
            mocked.when(() -> BCrypt.checkpw(eq(houseId), anyString())).thenReturn(true);

            // Esegue la ricerca dei coinquilini
            List<User> result = authService.getCoinquilinibyHouseId(houseId);

            // Verifica che la lista sia corretta
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(user, result.get(0));
        }
    }

}
