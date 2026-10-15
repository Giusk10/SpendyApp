package com.spendy.auth.Repository;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JPA Persistence Integration Tests for AuthMicroService")
public class AuthJpaPersistenceIntegrationTest {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist user and auto-generate UUID identifier")
    void testPersistUserAndGenerateId() {
        User user = new User("giuseppe_dev", "Giuseppe", "securePass123", "Verdi", "giuseppe@example.com");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId_user());
        assertFalse(saved.getId_user().isBlank());
        assertEquals("giuseppe_dev", saved.getUsername());
        assertEquals("giuseppe@example.com", saved.getEmail());
    }

    @Test
    @DisplayName("Should find user by username and by email")
    void testFindByUsernameAndEmail() {
        User user = new User("mario_rossi", "Mario", "pass456", "Rossi", "mario.rossi@example.com");
        userRepository.save(user);

        User byUsername = userRepository.findByUsername("mario_rossi");
        assertNotNull(byUsername);
        assertEquals("Mario", byUsername.getName());

        User byEmail = userRepository.findByEmail("mario.rossi@example.com");
        assertNotNull(byEmail);
        assertEquals("mario_rossi", byEmail.getUsername());
    }

    @Test
    @DisplayName("Should find user by id_user via query method")
    void testFindByIdUser() {
        User user = new User("luigi_bianchi", "Luigi", "pass789", "Bianchi", "luigi@example.com");
        user.setId_user("custom-uuid-1234");
        userRepository.save(user);

        User found = userRepository.findByIdUser("custom-uuid-1234");
        assertNotNull(found);
        assertEquals("luigi_bianchi", found.getUsername());
    }

    @Test
    @DisplayName("Should update houseUser and find by houseUser")
    void testSetHouseUserAndFindByHouse() {
        User user = new User("household_user", "House", "pass", "Hold", "house@example.com");
        userRepository.save(user);

        userRepository.setHouseUser("household_user", "HOUSE_99");

        User updated = userRepository.findByUsername("household_user");
        assertEquals("HOUSE_99", updated.getHouseUser());

        User byHouse = userRepository.findByHouseUser("HOUSE_99");
        assertNotNull(byHouse);
        assertEquals("household_user", byHouse.getUsername());

        List<User> list = userRepository.findByHouseId("HOUSE_99");
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Should persist and manage RefreshToken lifecycle")
    void testRefreshTokenPersistence() {
        User user = new User("mario_rossi", "Mario", "pass", "Rossi", "mario@example.com");
        userRepository.save(user);

        RefreshToken token = new RefreshToken("token-abc-123", "mario_rossi", LocalDateTime.now().plusDays(30));
        RefreshToken saved = refreshTokenRepository.save(token);

        assertNotNull(saved.getId());
        assertEquals("token-abc-123", saved.getToken());

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-abc-123");
        assertTrue(found.isPresent());
        assertEquals("mario_rossi", found.get().getUsername());

        refreshTokenRepository.deleteByUsername("mario_rossi");
        assertTrue(refreshTokenRepository.findByToken("token-abc-123").isEmpty());
    }
}
