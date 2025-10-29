package com.spendy.auth.Controller;

import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import com.spendy.auth.Utility.UserResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


@Path("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @POST
    @Path("/login")
    @Consumes("application/json")
    @Produces("application/json")
    public Response login(User user) {
        AuthResult result;
        String password = user.getPassword();
        String username = user.getUsername();
        if (username == null) {
            String email = user.getEmail();
            result = authService.login(email, password);
        } else {
            result = authService.login(username, password);
        }

        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            //System.out.println("Token: " + result.getToken());
            // Generate a token and return it in the response
            return Response.ok("{\"token\":\"" + result.getToken() + "\"}").type("application/json").build();
        } else if (result.getStatusAuth() == StatusAuth.USER_NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND).entity("Utente non trovato").build();
        } else if (result.getStatusAuth() == StatusAuth.INVALID_CREDENTIALS) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Credenziali errate o non valide").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Server error").build();
    }

    @POST
    @Path("/register")
    public Response register(User user) {

        String username = user.getUsername();
        String password = user.getPassword();
        String name = user.getName();
        String surname = user.getSurname();
        String email = user.getEmail();

        if (!validateEmail(email)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Email invalido").build();
        }

        AuthResult result = authService.register(username, name, password, surname, email);
        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            return Response.ok("Registration successful").build();
        } else if (result.getStatusAuth() == StatusAuth.USER_ALREADY_EXISTS) {
            return Response.status(Response.Status.CONFLICT).entity("User already exists").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
    }

    private static boolean validateEmail(String email) {
        if (email == null) return false;
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    @POST
    @Path("/getUserByHouseId")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getUserByHouseId(Map<String, String> requestBody) {
        String houseId = requestBody.get("houseId");
        UserResult result = authService.getUserByHouseId(houseId);
        if (result.getStatusAuth() == StatusAuth.USERS_FOUNDED) {
            return Response.ok(result.getUsers()).build();
        } else if (result.getStatusAuth() == StatusAuth.USERS_NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND).entity("No users found for the given house ID").build();
        }
        return null;
    }

    @GET
    @Path("/health")
    public Response healthCheck() {
        return Response.ok("AuthMicroService is running").build();
    }
}