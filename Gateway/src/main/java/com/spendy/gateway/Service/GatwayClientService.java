package com.spendy.gateway.Service;

import com.spendy.gateway.JWT.TokenManager;
import com.spendy.gateway.Utility.GatewayResult;
import com.spendy.gateway.Utility.StatusGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GatwayClientService {

    @Autowired
    private TokenManager tokenManager;

    public GatewayResult generateToken(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String token = tokenManager.generateToken(username);

        if (token != null) {
            return new GatewayResult(StatusGateway.TOKEN_GENERATION_SUCCESS, token);
        } else {
            return new GatewayResult(StatusGateway.TOKEN_GENERATION_FAILED, null);
        }

    }

    public GatewayResult verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String username = tokenManager.verifyToken(token);

        if (username != null) {
            return new GatewayResult(StatusGateway.TOKEN_VERIFICATION_SUCCESS, username);
        } else {
            return new GatewayResult(StatusGateway.TOKEN_VERIFICATION_FAILED, null);
        }
    }

}