package com.spendy.auth.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "refresh_tokens")
@Schema(description = "Refresh token entity for managing user session rotation")
public class RefreshToken {
    @Id
    @Schema(description = "Token database identifier", accessMode = Schema.AccessMode.READ_ONLY, example = "60c72b2f9b1d8b2bad8e4b81")
    private String id;

    @Field("token")
    @Schema(description = "Opaque refresh token string", example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...")
    private String token;

    @Field("username")
    @Schema(description = "Username associated with this refresh token", example = "mario_rossi")
    private String username;

    @Field("expiryDate")
    @Schema(description = "Expiration timestamp of the refresh token", example = "2026-10-22T20:30:00")
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
