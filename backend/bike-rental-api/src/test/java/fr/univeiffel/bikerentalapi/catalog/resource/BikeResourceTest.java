package fr.univeiffel.bikerentalapi.catalog.resource;

import fr.univeiffel.bikerentalapi.config.AppConfig;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class BikeResourceTest extends JerseyTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("uge_bike")
            .withUsername("uge")
            .withPassword("uge");

    @Override
    protected Application configure() {
        System.setProperty("DB_URL", mysql.getJdbcUrl());
        System.setProperty("DB_USER", mysql.getUsername());
        System.setProperty("DB_PASSWORD", mysql.getPassword());
        return new AppConfig();
    }

    @Test
    void create_and_get() {
        Response post = target("/bikes")
                .request()
                .post(Entity.entity("{\"description\":\"Teste\"}", MediaType.APPLICATION_JSON));
        assertEquals(201, post.getStatus());
        String body = post.readEntity(String.class);
        assertTrue(body.contains("\"description\":\"Teste\""));
        Response list = target("/bikes").request().get();
        assertEquals(200, list.getStatus());
        assertTrue(list.readEntity(String.class).contains("Teste"));
    }
}
