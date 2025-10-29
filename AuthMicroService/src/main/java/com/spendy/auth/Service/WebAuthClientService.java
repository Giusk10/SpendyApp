package com.spendy.auth.Service;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service("WebAuthClientService")
public class WebAuthClientService {

    private final IUserRepository userRepository;
    private final WebClient webClient;

    public WebAuthClientService(IUserRepository userRepository, WebClient webClient) {
        this.userRepository = userRepository;
        this.webClient = webClient;
    }

    public AuthResult linkHouseToUser(String token, String houseCode) {
        if (token == null || houseCode == null) {
            return new AuthResult(StatusAuth.INVALID_CREDENTIALS, "Invalid input");
        }

        String username = getUsernameFromTokenViaRest(token);
        if (username == null) {
            return new AuthResult(StatusAuth.USER_NOT_FOUND, "User not found or invalid token");
        }

        User user = getUserInfo(username);
        if (user == null) {
            return new AuthResult(StatusAuth.USER_NOT_FOUND, "User not found");
        }

        if (user.getHouseUser() != null) {
            if (BCrypt.checkpw(houseCode, user.getHouseUser())) {
                return new AuthResult(StatusAuth.SUCCESS, "User linked to his house");
            }
            return new AuthResult(StatusAuth.USER_ALREADY_LINKED, "User already linked to a house");
        }

        try {
            String hashedHouseCode = BCrypt.hashpw(houseCode, BCrypt.gensalt());
            userRepository.setHouseUser(user.getUsername(), hashedHouseCode);
            return new AuthResult(StatusAuth.SUCCESS, "House linked successfully");
        } catch (Exception e) {
            return new AuthResult(StatusAuth.LINKED_ERROR, "Failed to link house");
        }
    }

    private String getUsernameFromTokenViaRest(String token) {
        try {
            Map<String, String> response = webClient.post()
                    .uri("http://localhost:8080/gateway/verify-token")
                    .bodyValue(Map.of("token", token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // La risposta JSON dovrebbe essere tipo {"username": "theUser"}
            return response != null ? response.get("username") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private User getUserInfo(String username) {
        return userRepository.findByUsername(username);
    }
}
