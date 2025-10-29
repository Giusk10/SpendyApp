package com.spendy.gateway.Utility;

public class GatewayResult {

    private StatusGateway statusGateway;
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
