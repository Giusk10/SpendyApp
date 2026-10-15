package com.spendy.gateway.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to generate a JWT token for a given user")
public class TokenGenerationRequest {

    @Schema(description = "Username for whom the token will be generated", requiredMode = Schema.RequiredMode.REQUIRED, example = "mario_rossi")
    private String username;

    public TokenGenerationRequest() {}

    public TokenGenerationRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
