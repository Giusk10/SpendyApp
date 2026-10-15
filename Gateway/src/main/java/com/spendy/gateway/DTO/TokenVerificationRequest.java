package com.spendy.gateway.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to verify a JWT token and resolve username")
public class TokenVerificationRequest {

    @Schema(description = "JWT Token string to verify", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    public TokenVerificationRequest() {}

    public TokenVerificationRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
