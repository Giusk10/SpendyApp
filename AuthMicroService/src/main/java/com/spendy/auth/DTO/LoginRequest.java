package com.spendy.auth.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User login request payload. Provide either username or email along with password.")
public class LoginRequest {

    @Schema(description = "Account username (required if email is not provided)", example = "mario_rossi")
    private String username;

    @Schema(description = "Account email address (required if username is not provided)", example = "mario.rossi@example.com")
    private String email;

    @Schema(description = "User account password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Password123!")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
