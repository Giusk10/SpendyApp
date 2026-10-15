package com.spendy.gateway.Client;

import com.spendy.gateway.DTO.TokenGenerationRequest;
import com.spendy.gateway.DTO.TokenResponse;
import com.spendy.gateway.DTO.TokenVerificationRequest;
import com.spendy.gateway.DTO.UsernameResponse;
import com.spendy.gateway.Service.GatwayClientService;
import com.spendy.gateway.Utility.GatewayResult;
import com.spendy.gateway.Utility.StatusGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Gateway Token Service", description = "Endpoints for generating and verifying JWT access tokens via the API Gateway")
@RestController
@RequestMapping("/gateway")
public class GatewayClientController {

    @Autowired
    private GatwayClientService gatwayClientService;

    @Operation(
        summary = "Generate JWT token",
        description = "Generates a signed JWT access token for the given username."
    )
    @RequestBody(
        description = "Username payload",
        required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenGenerationRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token generated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Token generation failed",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "Token generation failed"))
        )
    })
    @PostMapping(value = "/generate-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateToken(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        System.out.println(body);

        GatewayResult result = gatwayClientService.generateToken(body.get("username"));

        if (result.getStatusGateway() == StatusGateway.TOKEN_GENERATION_SUCCESS) {
            return ResponseEntity.ok(Map.of("token", result.getToken()));
        } else {
            return ResponseEntity.status(500).body("Token generation failed");
        }
    }

    @Operation(
        summary = "Verify JWT token",
        description = "Validates the signature and expiration of a JWT token, returning the subject username upon success."
    )
    @RequestBody(
        description = "Token verification payload",
        required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenVerificationRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token is valid; returns the username",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsernameResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token verification failed (invalid or expired)",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "Token verification failed"))
        )
    })
    @PostMapping(value = "/verify-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyToken(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {

        GatewayResult result = gatwayClientService.verifyToken(body.get("token"));

        if (result.getStatusGateway() == StatusGateway.TOKEN_VERIFICATION_SUCCESS) {
            return ResponseEntity.ok(Map.of("username", result.getToken()));
        } else {
            return ResponseEntity.status(401).body("Token verification failed");
        }
    }
}