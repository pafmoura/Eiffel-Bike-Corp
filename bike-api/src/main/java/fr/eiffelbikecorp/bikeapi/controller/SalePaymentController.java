package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.SalePaymentResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.PaymentService;
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
@Path("/api/sales/payments" )
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Secured
public class SalePaymentController {

    private final PaymentService salePaymentService;

    @Context
    private ContainerRequestContext requestContext;

    // US_18: pay purchase (authorize + capture)
    @POST
    public Response pay(@Valid PayPurchaseRequest request) {
        UUID customerId = customerId();
        SalePaymentResponse paid = salePaymentService.payPurchase(customerId, request);
        return Response.status(Response.Status.CREATED).entity(paid).build();
    }

    private UUID customerId() {
        Object v = requestContext.getProperty("customerId" );
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
