package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.response.PurchaseResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.PurchaseService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/purchases")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Secured
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Context
    private ContainerRequestContext requestContext;

    // US_17: checkout basket => create purchase
    @POST
    @Path("/checkout")
    public Response checkout() {
        UUID customerId = customerId();
        PurchaseResponse created = purchaseService.checkout(customerId);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // helpful: list purchase history
    @GET
    public Response listPurchases() {
        UUID customerId = customerId();
        List<PurchaseResponse> purchases = purchaseService.listPurchases(customerId);
        return Response.ok(purchases).build();
    }

    // view one purchase (customer-owned)
    @GET
    @Path("/{purchaseId}")
    public Response getPurchase(@PathParam("purchaseId") Long purchaseId) {
        UUID customerId = customerId();
        return Response.ok(purchaseService.getPurchase(customerId, purchaseId)).build();
    }

    private UUID customerId() {
        Object v = requestContext.getProperty("userId");
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
