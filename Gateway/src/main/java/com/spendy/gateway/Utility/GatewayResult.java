package com.spendy.gateway.Utility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gateway token operation outcome")
public class GatewayResult {

    @Schema(description = "Gateway operation status", example = "TOKEN_GENERATION_SUCCESS")
    private StatusGateway statusGateway;

    @Schema(description = "Token or username payload depending on operation", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    public GatewayResult(StatusGateway statusGateway, String token) {
        this.statusGateway = statusGateway;
        this.token = token;
    }

    public StatusGateway getStatusGateway() {
        return statusGateway;
    }

    public void setStatusGateway(StatusGateway statusGateway) {
        this.statusGateway = statusGateway;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
