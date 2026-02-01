package com.spendy.auth.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe User
 * Verifica la corretta gestione dei dati utente
 */
@DisplayName("Test per User")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "Test", "hashedpass", "User", "test@example.com");
    }

    /**
     * Verifica la creazione di un utente con costruttore completo
     */
    @Test
    @DisplayName("Creazione utente con costruttore completo")
    void testFullConstructor() {
        User newUser = new User("username", "Name", "password", "Surname", "email@test.com");

        assertEquals("username", newUser.getUsername());
        assertEquals("Name", newUser.getName());
        assertEquals("password", newUser.getPassword());
        assertEquals("Surname", newUser.getSurname());
        assertEquals("email@test.com", newUser.getEmail());
        assertNull(newUser.getProfileImage());
        assertNull(newUser.getHouseUser());
    }

    /**
     * Verifica la creazione di un utente con costruttore semplice
     */
    @Test
    @DisplayName("Creazione utente con costruttore semplice")
    void testSimpleConstructor() {
        User newUser = new User("username", "password");

        assertEquals("username", newUser.getUsername());
        assertEquals("password", newUser.getPassword());
        assertNull(newUser.getName());
        assertNull(newUser.getSurname());
        assertNull(newUser.getEmail());
    }

    /**
     * Verifica la creazione di un utente con costruttore vuoto
     */
    @Test
    @DisplayName("Creazione utente con costruttore vuoto")
    void testEmptyConstructor() {
        User newUser = new User();

        assertNull(newUser.getUsername());
        assertNull(newUser.getName());
        assertNull(newUser.getPassword());
        assertNull(newUser.getSurname());
        assertNull(newUser.getEmail());
    }

    /**
     * Verifica getter e setter per username
     */
    @Test
    @DisplayName("Verifica getter e setter per username")
    void testUsernameGetterSetter() {
        assertEquals("testuser", user.getUsername());

        user.setUsername("newusername");
        assertEquals("newusername", user.getUsername());
    }

    /**
     * Verifica getter e setter per name
     */
    @Test
    @DisplayName("Verifica getter e setter per name")
    void testNameGetterSetter() {
        assertEquals("Test", user.getName());

        user.setName("NewName");
        assertEquals("NewName", user.getName());
    }

    /**
     * Verifica getter e setter per password
     */
    @Test
    @DisplayName("Verifica getter e setter per password")
    void testPasswordGetterSetter() {
        assertEquals("hashedpass", user.getPassword());

        user.setPassword("newhashedpass");
        assertEquals("newhashedpass", user.getPassword());
    }

    /**
     * Verifica getter e setter per surname
     */
    @Test
    @DisplayName("Verifica getter e setter per surname")
    void testSurnameGetterSetter() {
        assertEquals("User", user.getSurname());

        user.setSurname("NewSurname");
        assertEquals("NewSurname", user.getSurname());
    }

    /**
     * Verifica getter e setter per email
     */
    @Test
    @DisplayName("Verifica getter e setter per email")
    void testEmailGetterSetter() {
        assertEquals("test@example.com", user.getEmail());

        user.setEmail("newemail@example.com");
        assertEquals("newemail@example.com", user.getEmail());
    }

    /**
     * Verifica getter e setter per id_user
     */
    @Test
    @DisplayName("Verifica getter e setter per id_user")
    void testIdUserGetterSetter() {
        assertNull(user.getId_user());

        user.setId_user("123456");
        assertEquals("123456", user.getId_user());
    }

    /**
     * Verifica getter e setter per language
     */
    @Test
    @DisplayName("Verifica getter e setter per language")
    void testLanguageGetterSetter() {
        assertNull(user.getLanguage());

        user.setLanguage("it");
        assertEquals("it", user.getLanguage());
    }

    /**
     * Verifica getter e setter per profileImage
     */
    @Test
    @DisplayName("Verifica getter e setter per profileImage")
    void testProfileImageGetterSetter() {
        assertNull(user.getProfileImage());

        byte[] image = new byte[]{1, 2, 3, 4, 5};
        user.setProfileImage(image);
        assertArrayEquals(image, user.getProfileImage());
    }

    /**
     * Verifica getter e setter per houseUser
     */
    @Test
    @DisplayName("Verifica getter e setter per houseUser")
    void testHouseUserGetterSetter() {
        assertNull(user.getHouseUser());

        user.setHouseUser("HOUSE123");
        assertEquals("HOUSE123", user.getHouseUser());
    }

    /**
     * Verifica la gestione di valori null
     */
    @Test
    @DisplayName("Gestione valori null")
    void testNullValues() {
        User nullUser = new User();

        nullUser.setUsername(null);
        nullUser.setName(null);
        nullUser.setPassword(null);
        nullUser.setSurname(null);
        nullUser.setEmail(null);
        nullUser.setLanguage(null);
        nullUser.setProfileImage(null);
        nullUser.setHouseUser(null);

        assertNull(nullUser.getUsername());
        assertNull(nullUser.getName());
        assertNull(nullUser.getPassword());
        assertNull(nullUser.getSurname());
        assertNull(nullUser.getEmail());
        assertNull(nullUser.getLanguage());
        assertNull(nullUser.getProfileImage());
        assertNull(nullUser.getHouseUser());
    }

    /**
     * Verifica la modifica di tutti i campi di un utente
     */
    @Test
    @DisplayName("Modifica completa di tutti i campi")
    void testCompleteModification() {
        User modUser = new User();

        modUser.setId_user("USER123");
        modUser.setUsername("moduser");
        modUser.setName("Modified");
        modUser.setPassword("modpass");
        modUser.setSurname("User");
        modUser.setEmail("mod@test.com");
        modUser.setLanguage("en");
        modUser.setProfileImage(new byte[]{10, 20, 30});
        modUser.setHouseUser("HOUSE456");

        assertEquals("USER123", modUser.getId_user());
        assertEquals("moduser", modUser.getUsername());
        assertEquals("Modified", modUser.getName());
        assertEquals("modpass", modUser.getPassword());
        assertEquals("User", modUser.getSurname());
        assertEquals("mod@test.com", modUser.getEmail());
        assertEquals("en", modUser.getLanguage());
        assertArrayEquals(new byte[]{10, 20, 30}, modUser.getProfileImage());
        assertEquals("HOUSE456", modUser.getHouseUser());
    }

    /**
     * Verifica che i costruttori inizializzino correttamente i campi opzionali
     */
    @Test
    @DisplayName("Verifica inizializzazione campi opzionali nei costruttori")
    void testOptionalFieldsInitialization() {
        // Costruttore completo
        User user1 = new User("user1", "Name1", "pass1", "Surname1", "email1@test.com");
        assertNull(user1.getProfileImage(), "profileImage dovrebbe essere null");
        assertNull(user1.getHouseUser(), "houseUser dovrebbe essere null");

        // Costruttore semplice
        User user2 = new User("user2", "pass2");
        assertNull(user2.getProfileImage(), "profileImage dovrebbe essere null");
        assertNull(user2.getHouseUser(), "houseUser dovrebbe essere null");
    }

    /**
     * Verifica la gestione di stringhe vuote
     */
    @Test
    @DisplayName("Gestione stringhe vuote")
    void testEmptyStrings() {
        User emptyUser = new User();

        emptyUser.setUsername("");
        emptyUser.setName("");
        emptyUser.setPassword("");
        emptyUser.setSurname("");
        emptyUser.setEmail("");
        emptyUser.setLanguage("");
        emptyUser.setHouseUser("");

        assertEquals("", emptyUser.getUsername());
        assertEquals("", emptyUser.getName());
        assertEquals("", emptyUser.getPassword());
        assertEquals("", emptyUser.getSurname());
        assertEquals("", emptyUser.getEmail());
        assertEquals("", emptyUser.getLanguage());
        assertEquals("", emptyUser.getHouseUser());
    }

    /**
     * Verifica scenario di aggiornamento profilo utente
     */
    @Test
    @DisplayName("Scenario aggiornamento profilo utente")
    void testUserProfileUpdateScenario() {
        // Utente iniziale
        User existingUser = new User("olduser", "OldName", "oldpass", "OldSurname", "old@test.com");
        existingUser.setId_user("USER001");
        existingUser.setHouseUser("HOUSE001");

        // Aggiornamento profilo (name e surname)
        existingUser.setName("UpdatedName");
        existingUser.setSurname("UpdatedSurname");

        // Verifica che i campi modificabili siano aggiornati
        assertEquals("UpdatedName", existingUser.getName());
        assertEquals("UpdatedSurname", existingUser.getSurname());

        // Verifica che i campi non modificabili siano rimasti invariati
        assertEquals("olduser", existingUser.getUsername());
        assertEquals("old@test.com", existingUser.getEmail());
        assertEquals("USER001", existingUser.getId_user());
        assertEquals("HOUSE001", existingUser.getHouseUser());
    }

    /**
     * Verifica gestione immagine profilo di grandi dimensioni
     */
    @Test
    @DisplayName("Gestione immagine profilo di grandi dimensioni")
    void testLargeProfileImage() {
        byte[] largeImage = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeImage.length; i++) {
            largeImage[i] = (byte) (i % 256);
        }

        user.setProfileImage(largeImage);

        assertNotNull(user.getProfileImage());
        assertEquals(1024 * 1024, user.getProfileImage().length);
        assertArrayEquals(largeImage, user.getProfileImage());
    }
}

