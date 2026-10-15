package com.spendy.gateway.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing the generated JWT token")
public class TokenResponse {

    @Schema(description = "Generated JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    public TokenResponse() {}

    public TokenResponse(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
