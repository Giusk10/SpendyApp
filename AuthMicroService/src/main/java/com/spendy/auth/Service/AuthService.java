package com.spendy.auth.Service;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Security.JwtTokenService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service("AuthService")
public class AuthService {
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(IUserRepository userRepository,
                       IRefreshTokenRepository refreshTokenRepository,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    /**
     * Backward-compatible constructor for legacy tests that pass WebClient.
     */
    public AuthService(IUserRepository userRepository, WebClient webClient, IRefreshTokenRepository refreshTokenRepository) {
        this(userRepository, refreshTokenRepository, null, new BCryptPasswordEncoder());
    }

    @Transactional
    public AuthResult register(String username, String name, String password, String surname, String email) {
        if (userRepository.findByUsername(username) != null || userRepository.findByEmail(email) != null) {
            return new AuthResult(StatusAuth.USER_ALREADY_EXISTS, null);
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, name, hashedPassword, surname, email);
        userRepository.save(user);

        String token = generateToken(username);
        return new AuthResult(StatusAuth.SUCCESS, token);
    }

    @Transactional
    public AuthResult login(String identifier, String password) {
        User user = userRepository.findByUsername(identifier);
        if (user == null) {
            user = userRepository.findByEmail(identifier);
        }
        if (user == null) {
            return new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        }
        if (passwordEncoder.matches(password, user.getPassword())) {
            String accessToken = generateToken(user.getUsername());
            String refreshToken = createAndSaveRefreshToken(user.getUsername());
            return new AuthResult(StatusAuth.SUCCESS, accessToken, refreshToken);
        }
        return new AuthResult(StatusAuth.INVALID_CREDENTIALS, null);
    }

    private String generateToken(String username) {
        if (jwtTokenService != null) {
            return jwtTokenService.generateToken(username);
        }
        // Fallback for isolated legacy tests without full context
        return UUID.randomUUID().toString();
    }

    @Transactional
    public AuthResult refreshAccessToken(String requestRefreshToken) {
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByToken(requestRefreshToken);

        if (rtOpt.isEmpty()) {
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        RefreshToken currentToken = rtOpt.get();

        if (currentToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(currentToken);
            return new AuthResult(StatusAuth.TOKEN_EXPIRED, null);
        }

        // *** ROTATION ***
        refreshTokenRepository.delete(currentToken);

        String newAccessToken = generateToken(currentToken.getUsername());
        String newRefreshToken = createAndSaveRefreshToken(currentToken.getUsername());

        return new AuthResult(StatusAuth.SUCCESS, newAccessToken, newRefreshToken);
    }

    @Transactional
    public String createAndSaveRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        // Scadenza 30 giorni
        RefreshToken rt = new RefreshToken(token, username, LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(rt);
        return token;
    }

    @Transactional(readOnly = true)
    public User getUserFromToken(String accessToken) {
        String username = verifyToken(accessToken);
        if (username != null) {
            return userRepository.findByUsername(username);
        }
        return null;
    }

    @Transactional
    public AuthResult updateUserProfile(String accessToken, User updatedUser) {
        String username = verifyToken(accessToken);
        if (username == null) {
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            return new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        }

        existingUser.setName(updatedUser.getName());
        existingUser.setSurname(updatedUser.getSurname());

        userRepository.save(existingUser);
        return new AuthResult(StatusAuth.SUCCESS, null);
    }

    private String verifyToken(String token) {
        if (jwtTokenService != null) {
            return jwtTokenService.verifyTokenAndGetSubject(token);
        }
        return null;
    }
}
