package com.spendy.auth.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "users")
@Schema(description = "User account entity representing a registered user in MongoDB")
public class User
{
    @Id
    @Field("_id")
    @Schema(description = "Unique user identifier (MongoDB ObjectId)", accessMode = Schema.AccessMode.READ_ONLY, example = "60c72b2f9b1d8b2bad8e4b80")
    private String id_user;

    @Field("username")
    @Schema(description = "Unique account username", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario_rossi")
    private String username;

    @Field("name")
    @Schema(description = "User first name", example = "Mario")
    private String name;

    @Field("password")
    @Schema(description = "User password (hashed in storage; write-only)", accessMode = Schema.AccessMode.WRITE_ONLY, example = "Password123!")
    private String password;

    @Field("surname")
    @Schema(description = "User last name", example = "Rossi")
    private String surname;

    @Field("email")
    @Schema(description = "User email address", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario.rossi@example.com")
    private String email;

    @Field("language")
    @Schema(description = "User preferred UI language", example = "it")
    private String language;

    @Field("profile_image")
    @Schema(description = "User profile picture bytes", accessMode = Schema.AccessMode.WRITE_ONLY)
    private byte[] profileImage;

    @Field("house_user")
    @Schema(description = "Household identifier associated with this user", example = "house_123")
    private String houseUser;

    public User() { }

    public User(String username, String name, String password, String surname, String email)
    {
        this.username = username;
        this.name = name;
        this.password = password;
        this.surname = surname;
        this.email = email;
        this.profileImage = null;
        this.houseUser=null;
    }

    public User(String username, String pass)
    {
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





