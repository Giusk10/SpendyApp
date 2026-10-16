package com.spendy.auth.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_token", columnList = "token"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_tokens_family_id", columnList = "family_id"),
    @Index(name = "idx_refresh_tokens_username", columnList = "username")
})
@Schema(description = "Refresh token entity for managing secure user session rotation and reuse detection")
public class RefreshToken {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    @Schema(description = "Token database identifier (UUID)", accessMode = Schema.AccessMode.READ_ONLY, example = "60c72b2f-9b1d-8b2b-ad8e-4b8112345678")
    private String id;

    @Column(name = "token", length = 255, nullable = true)
    @Schema(description = "Legacy refresh token string (null for strictly hashed tokens)", example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...")
    private String token;

    @Column(name = "token_hash", length = 64, nullable = true)
    @Schema(description = "SHA-256 hash of the refresh token", example = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e")
    private String tokenHash;

    @Column(name = "family_id", length = 64, nullable = true)
    @Schema(description = "Identifier linking rotated refresh tokens belonging to the same session family", example = "fam-9b1d-8b2b")
    private String familyId;

    @Column(name = "username", length = 50, nullable = false)
    @Schema(description = "Username associated with this refresh token", example = "mario_rossi")
    private String username;

    @Column(name = "expiry_date", nullable = false)
    @Schema(description = "Expiration timestamp of the refresh token", example = "2026-10-22T20:30:00")
    private LocalDateTime expiryDate;

    @Column(name = "revoked", nullable = false)
    @Schema(description = "Indicates whether this refresh token has been revoked or already rotated")
    private boolean revoked = false;

    @Column(name = "replaced_by", length = 64, nullable = true)
    @Schema(description = "Hash of the new refresh token that replaced this one during rotation")
    private String replacedBy;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.familyId == null || this.familyId.isBlank()) {
            this.familyId = UUID.randomUUID().toString();
        }
    }

    public RefreshToken() {}

    public RefreshToken(String token, String username, LocalDateTime expiryDate) {
        this.token = token;
        this.username = username;
        this.expiryDate = expiryDate;
        this.revoked = false;
    }

    public RefreshToken(String token, String tokenHash, String username, String familyId, LocalDateTime expiryDate) {
        this.token = token;
        this.tokenHash = tokenHash;
        this.username = username;
        this.familyId = familyId;
        this.expiryDate = expiryDate;
        this.revoked = false;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
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

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }
}
