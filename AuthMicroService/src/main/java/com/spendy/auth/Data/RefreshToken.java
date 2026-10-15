package com.spendy.auth.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_token", columnList = "token"),
    @Index(name = "idx_refresh_tokens_username", columnList = "username")
})
@Schema(description = "Refresh token entity for managing user session rotation")
public class RefreshToken {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    @Schema(description = "Token database identifier (UUID)", accessMode = Schema.AccessMode.READ_ONLY, example = "60c72b2f-9b1d-8b2b-ad8e-4b8112345678")
    private String id;

    @Column(name = "token", length = 255, nullable = false, unique = true)
    @Schema(description = "Opaque refresh token string", example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...")
    private String token;

    @Column(name = "username", length = 50, nullable = false)
    @Schema(description = "Username associated with this refresh token", example = "mario_rossi")
    private String username;

    @Column(name = "expiry_date", nullable = false)
    @Schema(description = "Expiration timestamp of the refresh token", example = "2026-10-22T20:30:00")
    private LocalDateTime expiryDate;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public RefreshToken() {}

    public RefreshToken(String token, String username, LocalDateTime expiryDate) {
        this.token = token;
        this.username = username;
        this.expiryDate = expiryDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
