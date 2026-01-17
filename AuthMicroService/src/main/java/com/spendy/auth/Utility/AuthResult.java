package com.spendy.auth.Utility;

public class AuthResult {
    private StatusAuth statusAuth;
    private String accessToken;
    private String refreshToken; // Nuovo campo

    // Costruttore per successo (2 token)
    public AuthResult(StatusAuth statusAuth, String accessToken, String refreshToken) {
        this.statusAuth = statusAuth;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // Costruttore per errore (0 token) o compatibilità
    public AuthResult(StatusAuth statusAuth, String accessToken) {
        this.statusAuth = statusAuth;
        this.accessToken = accessToken;
        this.refreshToken = null;
    }

    public StatusAuth getStatusAuth() { return statusAuth; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}
