package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Component
@RequiredArgsConstructor
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200") // Add this line
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * US_07:
     * Pay a rental in any currency, stored with conversion to EUR.
     * <p>
     * POST /api/payments
     * Body: { "rentalId": 123, "amount": 50.00, "currency": "USD" }
     */
    @POST
    public Response payRental(@Valid PayRentalRequest request) {
        RentalPaymentResponse created = paymentService.payRental(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Useful for audit/UI:
     * List all payments for a rental.
     * <p>
     * GET /api/rentals/{rentalId}/payments
     */
    @GET
    @Path("/rentals/{rentalId}")
    public Response listPayments(@PathParam("rentalId") Long rentalId) {
        List<RentalPaymentResponse> payments = paymentService.listPayments(rentalId);
        return Response.ok(payments).build();
    }
}
