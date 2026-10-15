package com.spendy.auth.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile update payload")
public class UpdateProfileRequest {

    @Schema(description = "Updated user first name", example = "Mario")
    private String name;

    @Schema(description = "Updated user surname", example = "Rossi")
    private String surname;

    @Schema(description = "Updated email address", example = "mario.rossi@example.com")
    private String email;

    @Schema(description = "Updated preferred language (e.g. it, en)", example = "it")
    private String language;

    @Schema(description = "Associated household identifier", example = "house_123")
    private String houseUser;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String name, String surname, String email, String language, String houseUser) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.language = language;
        this.houseUser = houseUser;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getHouseUser() { return houseUser; }
    public void setHouseUser(String houseUser) { this.houseUser = houseUser; }
}
