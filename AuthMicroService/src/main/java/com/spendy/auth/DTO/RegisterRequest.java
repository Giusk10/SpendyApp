package com.spendy.auth.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration payload")
public class RegisterRequest {

    @Schema(description = "Unique username for the account", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario_rossi")
    private String username;

    @Schema(description = "User first name", example = "Mario")
    private String name;

    @Schema(description = "User last name", example = "Rossi")
    private String surname;

    @Schema(description = "User password", requiredMode = Schema.RequiredMode.REQUIRED, example = "SecurePassword123!")
    private String password;

    @Schema(description = "Valid email address (must match standard email format)", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario.rossi@example.com")
    private String email;

    public RegisterRequest() {}

    public RegisterRequest(String username, String name, String surname, String password, String email) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.password = password;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
