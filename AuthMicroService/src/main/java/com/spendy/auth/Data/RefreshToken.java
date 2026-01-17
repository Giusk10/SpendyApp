package com.spendy.auth.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;

    @Field("token")
    private String token;

    @Field("username")
    private String username;

    @Field("expiryDate")
    private LocalDateTime expiryDate;

    public RefreshToken() {}

    public RefreshToken(String token, String username, LocalDateTime expiryDate) {
        this.token = token;
        this.username = username;
        this.expiryDate = expiryDate;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
}
