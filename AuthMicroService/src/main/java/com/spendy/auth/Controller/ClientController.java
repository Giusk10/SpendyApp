package com.spendy.auth.Controller;

import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Path("/client")
public class ClientController
{
    @Autowired private AuthService authService;

    @GET
    @Path("/house")
    @Produces(MediaType.TEXT_HTML)
    public Response houseRegistrationPage() {
        return Response.ok("{\"path\": \"http://localhost:8080/HousePage.html\"}", MediaType.APPLICATION_JSON).build();
    }
    @GET
    @Path("/retrieveCoinquy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveCoinquy(@QueryParam("houseId") String houseId)
    {
        if (houseId == null)
        {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Missing houseId parameter\"}")
                    .build();
        }

        List<User> coinquilini = authService.getCoinquilinibyHouseId(houseId);
        return Response.status(Response.Status.OK).entity(coinquilini).build();
    }
}