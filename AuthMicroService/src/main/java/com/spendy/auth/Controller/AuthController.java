package com.spendy.auth.Controller;

import com.spendy.auth.DTO.*;
import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, token refresh, and profile management")
@RestController
@RequestMapping("/rest/auth")
@Path("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private com.spendy.auth.Security.JwtTokenService jwtTokenService;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    @Operation(
        summary = "User login",
        description = "Authenticates a user using either username or email along with password. Returns JWT access token and refresh token upon successful authentication."
    )
    @RequestBody(
        description = "User credentials payload",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful; returns access and refresh tokens",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing or invalid request payload",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Payload di login obbligatorio"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials provided",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Credenziali errate o non valide"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User account not found",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Utente non trovato"))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Server error"))
        )
    })
    @POST
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    @Path("/login")
    @Consumes("application/json")
    @Produces("application/json")
    public Response login(@org.springframework.web.bind.annotation.RequestBody(required = false) User user) {
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Payload di login obbligatorio").build();
        }

        String password = user.getPassword();
        String username = user.getUsername();
        String email = user.getEmail();

        if (password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Password obbligatoria").build();
        }
        if (password.length() > 128) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Password troppo lunga").build();
        }
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username o email obbligatori").build();
        }
        if (username != null && username.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username non valido").build();
        }
        if (email != null && email.length() > 150) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email non valida").build();
        }

        AuthResult result;
        if (username == null || username.isBlank()) {
            result = authService.login(email.trim(), password);
        } else {
            result = authService.login(username.trim(), password);
        }

        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("accessToken", result.getAccessToken());
            responseMap.put("refreshToken", result.getRefreshToken());
            return Response.ok(responseMap).build();
        } else if (result.getStatusAuth() == StatusAuth.USER_NOT_FOUND || result.getStatusAuth() == StatusAuth.INVALID_CREDENTIALS) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Credenziali errate o non valide").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Server error").build();
    }

    @Operation(
        summary = "User registration",
        description = "Registers a new user account with unique username, email, full name, and password. Performs email format validation before creation."
    )
    @RequestBody(
        description = "Registration details for the new user",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User successfully registered",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Registration successful"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid registration data or password length",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Password deve contenere tra 8 e 128 caratteri"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid email format supplied",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Email invalido"))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username or email already in use",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "User already exists"))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during registration",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "An error occurred"))
        )
    })
    @POST
    @PostMapping(value = "/register", consumes = "application/json")
    @Path("/register")
    public Response register(@org.springframework.web.bind.annotation.RequestBody(required = false) User user) {
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Payload di registrazione obbligatorio").build();
        }

        String username = user.getUsername();
        String password = user.getPassword();
        String name = user.getName();
        String surname = user.getSurname();
        String email = user.getEmail();

        if (!validateEmail(email) || (email != null && email.length() > 150)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Email invalido").build();
        }
        if (username == null || username.isBlank() || username.length() < 3 || username.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username deve contenere tra 3 e 50 caratteri").build();
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username contiene caratteri non consentiti").build();
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Password deve contenere tra 8 e 128 caratteri").build();
        }
        if (name != null && name.length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Nome troppo lungo").build();
        }
        if (surname != null && surname.length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Cognome troppo lungo").build();
        }

        AuthResult result = authService.register(username.trim(), name, password, surname, email.trim());
        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            return Response.ok("Registration successful").build();
        } else if (result.getStatusAuth() == StatusAuth.USER_ALREADY_EXISTS) {
            return Response.status(Response.Status.CONFLICT).entity("User already exists").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
    }

    @Operation(
        summary = "Refresh access token",
        description = "Generates a new access token and performs refresh token rotation given a valid refresh token."
    )
    @RequestBody(
        description = "Payload containing current refresh token",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshTokenRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tokens rotated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Refresh token is missing or blank",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Refresh Token missing"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token is invalid or expired",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Refresh Token expired or invalid"))
        )
    })
    @POST
    @PostMapping(value = "/refresh", consumes = "application/json", produces = "application/json")
    @Path("/refresh")
    @Consumes("application/json")
    @Produces("application/json")
    public Response refreshToken(@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Refresh Token missing").build();
        }
        String requestRefreshToken = body.get("refreshToken");

        if (requestRefreshToken == null || requestRefreshToken.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Refresh Token missing").build();
        }
        if (requestRefreshToken.length() > 255) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Refresh Token missing").build();
        }

        AuthResult result = authService.refreshAccessToken(requestRefreshToken);

        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("accessToken", result.getAccessToken());
            responseMap.put("refreshToken", result.getRefreshToken());
            return Response.ok(responseMap).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Refresh Token expired or invalid").build();
        }
    }

    private static boolean validateEmail(String email) {
        if (email == null) return false;
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    @Operation(
        summary = "Get user profile",
        description = "Fetches authenticated user profile details from the JWT Bearer token claims.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile successfully retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired authorization header/token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Missing or invalid Authorization header"))
        )
    })
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    @Operation(
        summary = "Get user profile",
        description = "Fetches authenticated user profile details from the JWT Bearer token claims.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile successfully retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired authorization header/token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Missing or invalid Authorization header"))
        )
    })
    @GET
    @GetMapping(value = "/profile", produces = "application/json")
    @Path("/profile")
    @Produces("application/json")
    public Response getProfile(
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false)
        @HeaderParam("Authorization") String authHeader
    ) {
        String accessToken = extractBearerToken(authHeader);
        if (accessToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Missing or invalid Authorization header").build();
        }
        User user = authService.getUserFromToken(accessToken);
        if (user != null) {
            Map<String, String> profileMap = new HashMap<>();
            profileMap.put("username", user.getUsername());
            profileMap.put("name", user.getName());
            profileMap.put("surname", user.getSurname());
            profileMap.put("email", user.getEmail());
            return Response.ok(profileMap).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid access token").build();
        }
    }

    @Operation(
        summary = "Update user profile",
        description = "Updates profile attributes for the authenticated user identified by the Bearer token.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Updated user profile details",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateProfileRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile successfully updated",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Profile updated successfully"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Authorization header",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Missing or invalid Authorization header"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "User not found"))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during profile update",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "An error occurred"))
        )
    })
    @PUT
    @PutMapping(value = "/updateProfile", consumes = "application/json", produces = "application/json")
    @Path("/updateProfile")
    @Consumes("application/json")
    @Produces("application/json")
    public Response updateProfile(
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false)
        @HeaderParam("Authorization") String authHeader,
        @org.springframework.web.bind.annotation.RequestBody(required = false) User updatedUser
    ) {
        String accessToken = extractBearerToken(authHeader);
        if (accessToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Missing or invalid Authorization header").build();
        }
        if (updatedUser == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User payload required").build();
        }
        if (updatedUser.getName() != null && updatedUser.getName().length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Nome troppo lungo").build();
        }
        if (updatedUser.getSurname() != null && updatedUser.getSurname().length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Cognome troppo lungo").build();
        }
        AuthResult result = authService.updateUserProfile(accessToken, updatedUser);

        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            return Response.ok("Profile updated successfully").build();
        }
        else if (result.getStatusAuth() == StatusAuth.USER_NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }
        else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
        }
    }

    @Operation(
        summary = "JWKS Key Set",
        description = "Provides JSON Web Key Set (JWKS) containing the RSA public key used to verify JWT tokens."
    )
    @GET
    @GetMapping(value = "/jwks.json", produces = "application/json")
    @Path("/jwks.json")
    @Produces("application/json")
    public Response getJwkSet() {
        if (jwtTokenService != null) {
            return Response.ok(jwtTokenService.getJwkSetAsMap()).build();
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("JWKS service unavailable").build();
    }

    @Operation(
        summary = "User logout",
        description = "Revokes the specified refresh token and invalidates its session family."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully logged out",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"message\":\"Logged out successfully\"}"))
        )
    })
    @POST
    @PostMapping(value = "/logout", consumes = "application/json", produces = "application/json")
    @Path("/logout")
    @Consumes("application/json")
    @Produces("application/json")
    public Response logout(@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
        String refreshToken = body != null ? body.get("refreshToken") : null;
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return Response.ok(Map.of("message", "Logged out successfully")).build();
    }

    @Operation(
        summary = "Authentication service health check",
        description = "Lightweight health check endpoint to verify that the AuthMicroService is online and responsive."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and responding",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "AuthMicroService is running"))
        )
    })
    @GET
    @GetMapping(value = "/health", produces = "text/plain")
    @Path("/health")
    public Response healthCheck() {
        return Response.ok("AuthMicroService is running").build();
    }
}