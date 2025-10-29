package com.spendy.gateway.Client;

import com.spendy.gateway.Service.GatwayClientService;
import com.spendy.gateway.Utility.GatewayResult;
import com.spendy.gateway.Utility.StatusGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayClientController {

    @Autowired
    private GatwayClientService gatwayClientService;

    @PostMapping(value = "/generate-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateToken(@RequestBody Map<String, String> body) {
        System.out.println(body);

        GatewayResult result = gatwayClientService.generateToken(body.get("username"));

        if (result.getStatusGateway() == StatusGateway.TOKEN_GENERATION_SUCCESS) {
            return ResponseEntity.ok(Map.of("token", result.getToken()));
        } else {
            return ResponseEntity.status(500).body("Token generation failed");
        }
    }

    @PostMapping(value = "/verify-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> body) {

        GatewayResult result = gatwayClientService.verifyToken(body.get("token"));

        if (result.getStatusGateway() == StatusGateway.TOKEN_VERIFICATION_SUCCESS) {
            return ResponseEntity.ok(Map.of("username", result.getToken()));
        } else {
            return ResponseEntity.status(401).body("Token verification failed");
        }
    }
}