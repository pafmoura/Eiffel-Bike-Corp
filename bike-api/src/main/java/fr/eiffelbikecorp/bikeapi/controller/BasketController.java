package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.AddToBasketRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BasketService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "BearerAuth")
public class BasketController {

    private final BasketService basketService;

    @Context
    private ContainerRequestContext requestContext;

    @GET
    public Response getOpenBasket() {
        UUID customerId = customerId();
        BasketResponse basket = basketService.getOrCreateOpenBasket(customerId);
        return Response.ok(basket).build();
    }

    @POST
    @Path("/items")
    public Response addToBasket(@Valid AddToBasketRequest request) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.addItem(customerId, request);
        return Response.ok(basket).build();
    }

    @DELETE
    @Path("/items/{saleOfferId}")
    public Response removeFromBasket(@PathParam("saleOfferId") Long saleOfferId) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.removeItem(customerId, saleOfferId);
        return Response.ok(basket).build();
    }

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
