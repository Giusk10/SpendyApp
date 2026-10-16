package com.spendy.auth.Service;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Security.JwtTokenService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service("AuthService")
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthService(IUserRepository userRepository,
                       IRefreshTokenRepository refreshTokenRepository,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder(12);
    }

    /**
     * Backward-compatible constructor for legacy tests that pass WebClient.
     */
    public AuthService(IUserRepository userRepository, WebClient webClient, IRefreshTokenRepository refreshTokenRepository) {
        this(userRepository, refreshTokenRepository, null, new BCryptPasswordEncoder(12));
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
            String familyId = UUID.randomUUID().toString();
            String refreshToken = createAndSaveRefreshToken(user.getUsername(), familyId);
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
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        String tokenHash = hashToken(requestRefreshToken);
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (rtOpt.isEmpty()) {
            rtOpt = refreshTokenRepository.findByToken(requestRefreshToken);
        }

        if (rtOpt.isEmpty()) {
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        RefreshToken currentToken = rtOpt.get();

        // 1. REUSE DETECTION: If token was already revoked, someone is reusing a rotated token!
        if (currentToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user '{}'. Revoking session family '{}'.",
                    currentToken.getUsername(), currentToken.getFamilyId());
            if (currentToken.getFamilyId() != null) {
                refreshTokenRepository.revokeFamily(currentToken.getFamilyId());
            }
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        // 2. EXPIRY CHECK
        if (currentToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            currentToken.setRevoked(true);
            refreshTokenRepository.save(currentToken);
            return new AuthResult(StatusAuth.TOKEN_EXPIRED, null);
        }

        // 3. ROTATION: Invalidate current token and generate replacement in same family
        currentToken.setRevoked(true);
        String newRawToken = generateSecureRefreshToken();
        String newTokenHash = hashToken(newRawToken);
        currentToken.setReplacedBy(newTokenHash);
        refreshTokenRepository.save(currentToken);

        String familyId = currentToken.getFamilyId() != null ? currentToken.getFamilyId() : UUID.randomUUID().toString();
        RefreshToken newRefreshToken = new RefreshToken(newRawToken, newTokenHash, currentToken.getUsername(), familyId, LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = generateToken(currentToken.getUsername());
        return new AuthResult(StatusAuth.SUCCESS, newAccessToken, newRawToken);
    }

    @Transactional
    public boolean logout(String requestRefreshToken) {
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return false;
        }
        String tokenHash = hashToken(requestRefreshToken);
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (rtOpt.isEmpty()) {
            rtOpt = refreshTokenRepository.findByToken(requestRefreshToken);
        }
        if (rtOpt.isPresent()) {
            RefreshToken token = rtOpt.get();
            if (token.getFamilyId() != null) {
                refreshTokenRepository.revokeFamily(token.getFamilyId());
            } else {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
            return true;
        }
        return false;
    }

    @Transactional
    public void revokeAllUserTokens(String username) {
        if (username != null && !username.isBlank()) {
            refreshTokenRepository.deleteByUsername(username);
        }
    }

    @Transactional
    public String createAndSaveRefreshToken(String username) {
        return createAndSaveRefreshToken(username, UUID.randomUUID().toString());
    }

    @Transactional
    public String createAndSaveRefreshToken(String username, String familyId) {
        String rawToken = generateSecureRefreshToken();
        String tokenHash = hashToken(rawToken);
        RefreshToken rt = new RefreshToken(rawToken, tokenHash, username, familyId, LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(rt);
        return rawToken;
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

    public String generateSecureRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
