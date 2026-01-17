package com.spendy.auth.Service;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service("AuthService")
public class AuthService {
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final WebClient webClient;

    public AuthService(IUserRepository userRepository, WebClient webClient, IRefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.webClient = webClient;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthResult register(String username, String name, String password, String surname, String email) {
        if (userRepository.findByUsername(username) != null || userRepository.findByEmail(email) != null) {
            return new AuthResult(StatusAuth.USER_ALREADY_EXISTS, null);
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, name, hashedPassword, surname, email);
        userRepository.save(user);

        String token = generateTokenViaRest(username);
        return new AuthResult(StatusAuth.SUCCESS, token);
    }

    public AuthResult login(String identifier, String password) {
        User user = userRepository.findByUsername(identifier);
        if (user == null) {
            user = userRepository.findByEmail(identifier);
        }
        if (user == null) {
            return new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        }
        if (BCrypt.checkpw(password, user.getPassword())) {
            String accessToken = generateTokenViaRest(user.getUsername());
            String refreshToken = createAndSaveRefreshToken(user.getUsername());
            return new AuthResult(StatusAuth.SUCCESS, accessToken, refreshToken);
        }
        return new AuthResult(StatusAuth.INVALID_CREDENTIALS, null);
    }

    private String generateTokenViaRest(String username) {
        Map responseMap = webClient.post()
                .uri("http://localhost:7860/gateway/generate-token")
                .bodyValue(Map.of("username", username))
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // blocca fino a ricevere risposta

        return (String) responseMap.get("token");
    }

    public AuthResult refreshAccessToken(String requestRefreshToken) {
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByToken(requestRefreshToken);

        if (rtOpt.isEmpty()) {
            // Token non trovato: possibile attacco o token già ruotato/scaduto.
            return new AuthResult(StatusAuth.TOKEN_INVALID, null);
        }

        RefreshToken currentToken = rtOpt.get();

        // Verifica scadenza temporale
        if (currentToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(currentToken);
            return new AuthResult(StatusAuth.TOKEN_EXPIRED, null);
        }

        // *** ROTATION ***
        // Eliminiamo il token usato (così non può essere riusato da un hacker)
        refreshTokenRepository.delete(currentToken);

        // Generiamo una nuova coppia pulita
        String newAccessToken = generateTokenViaRest(currentToken.getUsername());
        String newRefreshToken = createAndSaveRefreshToken(currentToken.getUsername());

        return new AuthResult(StatusAuth.SUCCESS, newAccessToken, newRefreshToken);
    }

    // --- HELPER PRIVATO ---
    private String createAndSaveRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        // Scadenza 30 giorni
        RefreshToken rt = new RefreshToken(token, username, LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(rt);
        return token;
    }
}
