package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.AddToBasketRequest;
import fr.eiffelbikecorp.bikeapi.dto.BasketResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BasketService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/basket")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Secured
public class BasketController {

    private final BasketService basketService;

    @Context
    private ContainerRequestContext requestContext;

    // US_15: view basket
    @GET
    public Response getOpenBasket() {
        UUID customerId = customerId();
        BasketResponse basket = basketService.getOrCreateOpenBasket(customerId);
        return Response.ok(basket).build();
    }

    // US_15: add to basket
    @POST
    @Path("/items")
    public Response addToBasket(@Valid AddToBasketRequest request) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.addItem(customerId, request);
        return Response.ok(basket).build();
    }

    // US_16: remove from basket
    @DELETE
    @Path("/items/{saleOfferId}")
    public Response removeFromBasket(@PathParam("saleOfferId") Long saleOfferId) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.removeItem(customerId, saleOfferId);
        return Response.ok(basket).build();
    }

    // optional helper: clear basket
    @DELETE
    public Response clearBasket() {
        UUID customerId = customerId();
        BasketResponse basket = basketService.clear(customerId);
        return Response.ok(basket).build();
    }

    private UUID customerId() {
        Object v = requestContext.getProperty("userId");
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
