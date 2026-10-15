package com.spendy.auth.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh token request payload")
public class RefreshTokenRequest {

    @Schema(description = "Refresh token string received from a previous authentication", requiredMode = Schema.RequiredMode.REQUIRED, example = "60c72b2f-9b1d-8b2b-ad8e-4b8012345678")
    private String refreshToken;

    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
