package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SalePaymentResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200")
@Tag(
        name = "Payments",
        description = "Payments through gateway + currency conversion (US_08 rental payment, US_19 purchase payment)"
)
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentService salePaymentService;

    @Context
    private ContainerRequestContext requestContext;

    @POST
    @Path("/rentals")
    @Operation(
            summary = "Pay a rental",
            description = """
                    Pays a rental fee through the payment gateway (e.g., Stripe) (US_08).
                    Supports paying in any currency; the backend converts and stores the amount in EUR for consistency.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Rental payment created and paid",
                    content = @Content(schema = @Schema(implementation = RentalPaymentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error (amount/currency/paymentMethodId)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "409", description = "Rental already paid / invalid state")
    })
    public Response payRental(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Pay rental payload",
                    content = @Content(schema = @Schema(implementation = PayRentalRequest.class))
            )
            PayRentalRequest request
    ) {
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

    @POST
    @Path("/purchases")
    @Operation(
            summary = "Pay a purchase",
            description = """
                    Pays a purchase through the payment gateway so the system can verify funds and complete payment (US_19).
                    The backend records the payment and updates the purchase status accordingly.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Purchase payment created and paid",
                    content = @Content(schema = @Schema(implementation = SalePaymentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error (amount/currency/paymentMethodId)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Purchase not found"),
            @ApiResponse(responseCode = "409", description = "Purchase already paid / invalid state")
    })
    public Response payPurchase(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Pay purchase payload",
                    content = @Content(schema = @Schema(implementation = PayPurchaseRequest.class))
            )
            PayPurchaseRequest request
    ) {
        UUID customerId = customerId();
        SalePaymentResponse paid = salePaymentService.payPurchase(customerId, request);
        return Response.status(Response.Status.CREATED).entity(paid).build();
    }

    private UUID customerId() {
        Object v = requestContext.getProperty("userId");
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
