package com.spendy.auth.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information")
public class UserProfileResponse {

    @Schema(description = "Username", example = "mario_rossi")
    private String username;

    @Schema(description = "User first name", example = "Mario")
    private String name;

    @Schema(description = "User surname", example = "Rossi")
    private String surname;

    @Schema(description = "User registered email", example = "mario.rossi@example.com")
    private String email;

    public UserProfileResponse() {}

    public UserProfileResponse(String username, String name, String surname, String email) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
