package com.spendy.auth.Service;

import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import com.spendy.auth.Utility.UserResult;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service("AuthService")
public class AuthService {
    private final IUserRepository userRepository;
    private final WebClient webClient;

    public AuthService(IUserRepository userRepository, WebClient webClient) {
        this.userRepository = userRepository;
        this.webClient = webClient;
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
            String token = generateTokenViaRest(user.getUsername());
            return new AuthResult(StatusAuth.SUCCESS, token);
        }
        return new AuthResult(StatusAuth.INVALID_CREDENTIALS, null);
    }

    private String generateTokenViaRest(String username) {
        Map responseMap = webClient.post()
                .uri("http://localhost:8080/gateway/generate-token")
                .bodyValue(Map.of("username", username))
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // blocca fino a ricevere risposta

        return (String) responseMap.get("token");
    }

    public UserResult getUserByHouseId(String houseId) {

        List<User> users = userRepository.findAll()
                .stream()
                .filter(u -> BCrypt.checkpw(houseId, u.getHouseUser()))
                .toList();

        if (users != null) {
            return new UserResult(StatusAuth.USERS_FOUNDED, users);
        }
        return new UserResult(StatusAuth.USERS_NOT_FOUND, null);
    }

    public List<User> getCoinquilinibyHouseId(String houseId)
    {
        List<User> list = userRepository.findAll().stream()
                .filter(u -> BCrypt.checkpw(houseId, u.getHouseUser()))
                .toList();
        list.forEach(System.out::println);
        return list;
    }
}
