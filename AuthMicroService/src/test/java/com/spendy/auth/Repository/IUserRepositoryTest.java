package com.spendy.auth.Repository;

import com.spendy.auth.Data.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Abilita l'uso di Mockito nei test JUnit
@ExtendWith(MockitoExtension.class)
@DisplayName("Test per IUserRepository")
class IUserRepositoryTest {

    // Mock della repository utente
    @Mock
    private IUserRepository userRepository;

    // Utenti di test
    private User testUser;
    private User testUser2;

    // Metodo eseguito prima di ogni test per inizializzare i dati
    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "Test", "password", "User", "test@example.com");
        testUser.setId_user("123456");
        testUser.setHouseUser("HOUSE123");

        testUser2 = new User("testuser2", "Test2", "password2", "User2", "test2@example.com");
        testUser2.setId_user("789012");
        testUser2.setHouseUser("HOUSE123");
    }

    // Verifica che venga trovato un utente dato l'email
    @Test
    @DisplayName("Trova user da email")
    void testFindUserByEmail() {
        // Dato un indirizzo email
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        // Quando si cerca l'utente per email
        User foundUser = userRepository.findByEmail(email);

        // Allora l'utente trovato non deve essere null e i dati devono corrispondere
        assertNotNull(foundUser);
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
    }

    // Verifica che venga restituito null quando l'utente non viene trovato per email
    @Test
    @DisplayName("Restituisce null quando l'utente non viene trovato per email")
    void testReturnNullWhenUserNotFoundByEmail() {
        // Dato un'email inesistente
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(null);

        // Quando si cerca l'utente per email
        User foundUser = userRepository.findByEmail(nonExistentEmail);

        // Allora il risultato deve essere null
        assertNull(foundUser);
        verify(userRepository, times(1)).findByEmail(nonExistentEmail);
    }

    // Verifica che venga trovato un utente dato lo username
    @Test
    @DisplayName("Trova user da username")
    void shouldFindUserByUsername() {
        // Dato uno username
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(testUser);

        // Quando si cerca l'utente per username
        User foundUser = userRepository.findByUsername(username);

        // Allora l'utente trovato non deve essere null e i dati devono corrispondere
        assertNotNull(foundUser);
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        verify(userRepository, times(1)).findByUsername(username);
    }

    // Verifica che venga restituito null quando l'utente non viene trovato per username
    @Test
    @DisplayName("Restituisce null quando l'utente non viene trovato per username")
    void testReturnNullWhenUserNotFoundByUsername() {
        // Dato uno username inesistente
        String nonExistentUsername = "nonexistentuser";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(null);

        // Quando si cerca l'utente per username
        User foundUser = userRepository.findByUsername(nonExistentUsername);

        // Allora il risultato deve essere null
        assertNull(foundUser);
        verify(userRepository, times(1)).findByUsername(nonExistentUsername);
    }

    // Verifica che venga trovato un utente dato l'id_user
    @Test
    @DisplayName("Trova user da id_user")
    void testFindUserByIdUser() {
        // Dato un id_user
        String idUser = "123456";
        when(userRepository.findByIdUser(idUser)).thenReturn(testUser);

        // Quando si cerca l'utente per id_user
        User foundUser = userRepository.findByIdUser(idUser);

        // Allora l'utente trovato non deve essere null e l'id deve corrispondere
        assertNotNull(foundUser);
        assertEquals(testUser.getId_user(), foundUser.getId_user());
        verify(userRepository, times(1)).findByIdUser(idUser);
    }

    // Verifica che venga trovato un utente dato il campo houseUser
    @Test
    @DisplayName("Trova user da HouseUser")
    void testFindUserByHouseUser() {
        // Dato un houseUser
        String houseUser = "HOUSE123";
        when(userRepository.findByHouseUser(houseUser)).thenReturn(testUser);

        // Quando si cerca l'utente per houseUser
        User foundUser = userRepository.findByHouseUser(houseUser);

        // Allora l'utente trovato non deve essere null e il campo houseUser deve corrispondere
        assertNotNull(foundUser);
        assertEquals(testUser.getHouseUser(), foundUser.getHouseUser());
        verify(userRepository, times(1)).findByHouseUser(houseUser);
    }

    // Verifica che venga chiamato il metodo per settare il campo houseUser di uno user
    @Test
    @DisplayName("Setta HouseUser per uno username dato")
    void testSetHouseUserForUsername() {
        // Dato uno username e un nuovo houseCode
        String username = "testuser";
        String houseCode = "NEWHOUSE456";

        // Quando si setta il campo houseUser
        userRepository.setHouseUser(username, houseCode);

        // Allora il metodo deve essere chiamato una volta
        verify(userRepository, times(1)).setHouseUser(username, houseCode);
    }

    // Verifica che vengano trovati tutti gli utenti associati a uno specifico houseId
    @Test
    @DisplayName("Trova utenti per houseId")
    void testFindUsersByHouseId() {
        // Dato un houseId
        String houseId = "HOUSE123";
        List<User> expectedUsers = Arrays.asList(testUser, testUser2);
        when(userRepository.findByHouseId(houseId)).thenReturn(expectedUsers);

        // Quando si cercano gli utenti per houseId
        List<User> foundUsers = userRepository.findByHouseId(houseId);

        // Allora la lista non deve essere null, deve contenere 2 utenti e i dati devono corrispondere
        assertNotNull(foundUsers);
        assertEquals(2, foundUsers.size());
        assertTrue(foundUsers.contains(testUser));
        assertTrue(foundUsers.contains(testUser2));
        verify(userRepository, times(1)).findByHouseId(houseId);
    }

}