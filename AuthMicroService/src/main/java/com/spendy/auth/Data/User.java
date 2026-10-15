package com.spendy.auth.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_house_user", columnList = "house_user")
})
@Schema(description = "User account entity representing a registered user in SpendyApp")
public class User {

    @Id
    @Column(name = "id_user", length = 64, nullable = false, updatable = false)
    @Schema(description = "Unique user identifier (UUID)", accessMode = Schema.AccessMode.READ_ONLY, example = "60c72b2f-9b1d-8b2b-ad8e-4b8012345678")
    private String id_user;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    @Schema(description = "Unique account username", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario_rossi")
    private String username;

    @Column(name = "name", length = 100)
    @Schema(description = "User first name", example = "Mario")
    private String name;

    @Column(name = "password", length = 255, nullable = false)
    @Schema(description = "User password (hashed in storage; write-only)", accessMode = Schema.AccessMode.WRITE_ONLY, example = "Password123!")
    private String password;

    @Column(name = "surname", length = 100)
    @Schema(description = "User last name", example = "Rossi")
    private String surname;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    @Schema(description = "User email address", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario.rossi@example.com")
    private String email;

    @Column(name = "language", length = 10)
    @Schema(description = "User preferred UI language", example = "it")
    private String language;

    @Lob
    @Column(name = "profile_image", columnDefinition = "LONGBLOB")
    @Schema(description = "User profile picture bytes", accessMode = Schema.AccessMode.WRITE_ONLY)
    private byte[] profileImage;

    @Column(name = "house_user", length = 50)
    @Schema(description = "Household identifier associated with this user", example = "house_123")
    private String houseUser;

    @PrePersist
    public void prePersist() {
        if (this.id_user == null || this.id_user.isBlank()) {
            this.id_user = UUID.randomUUID().toString();
        }
    }

    public User() { }

    public User(String username, String name, String password, String surname, String email) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.surname = surname;
        this.email = email;
        this.profileImage = null;
        this.houseUser = null;
    }

    public User(String username, String pass) {
        this.username = username;
        this.password = pass;
    }

    public String getId_user() {
        return id_user;
    }

    public void setId_user(String id_user) {
        this.id_user = id_user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    public String getHouseUser() {
        return houseUser;
    }

    public void setHouseUser(String houseUser) {
        this.houseUser = houseUser;
    }
}
