package com.spendy.gateway.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing the verified username")
public class UsernameResponse {

    @Schema(description = "Username extracted from valid JWT token claims", example = "mario_rossi")
    private String username;

    public UsernameResponse() {}

    public UsernameResponse(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
