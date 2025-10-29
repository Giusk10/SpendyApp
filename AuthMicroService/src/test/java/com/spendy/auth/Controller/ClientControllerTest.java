package com.spendy.auth.Controller;

import com.spendy.auth.Data.User;
import com.spendy.auth.Service.AuthService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Abilita l'uso di Mockito nei test JUnit
@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    // Mock del servizio di autenticazione
    @Mock
    private AuthService authService;

    // Inietta i mock nel controller da testare
    @InjectMocks
    private ClientController clientController;

    /**
     * Verifica che, dato un houseId valido,
     * il controller restituisca 200 OK e la lista degli utenti coinquilini
     */
    @Test
    void testRetrieveCoinquySuccess() {
        // Dato un houseId valido e una lista di utenti
        String houseId = "house123";
        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");
        List<User> coinquilini = Arrays.asList(user1, user2);

        // Quando il servizio restituisce la lista degli utenti
        when(authService.getCoinquilinibyHouseId(houseId)).thenReturn(coinquilini);

        // Quando si chiama il controller
        Response response = clientController.retrieveCoinquy(houseId);

        // Allora la risposta deve essere OK e contenere la lista degli utenti
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(coinquilini, response.getEntity());
    }

    /**
     * Verifica che, se il parametro houseId è mancante (null),
     * il controller restituisca 400 BAD REQUEST e un messaggio di errore
     */
    @Test
    void testRetrieveCoinquyMissingHouseId() {
        // Quando si chiama il controller con houseId nullo
        Response response = clientController.retrieveCoinquy(null);

        // Allora la risposta deve essere BAD_REQUEST e contenere il messaggio di errore
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Missing houseId parameter"));
    }
}