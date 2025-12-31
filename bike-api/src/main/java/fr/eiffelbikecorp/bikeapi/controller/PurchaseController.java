package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.response.PurchaseResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
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
@Tag(
        name = "Purchases",
        description = "Checkout and purchase history"
)
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController extends BaseController {

    private final PurchaseService purchaseService;

    @POST
    @Path("/checkout")
    @Operation(
            summary = "Checkout my basket",
            description = """
                    Converts the authenticated customer's open basket into a purchase.
                    Typically clears the basket after checkout.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Purchase created",
                    content = @Content(schema = @Schema(implementation = PurchaseResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Basket is empty / invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Basket not in OPEN state / some offer is no longer available")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response checkout() {
        UUID customerId = userID();
        PurchaseResponse created = purchaseService.checkout(customerId);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Operation(
            summary = "Get my purchase history",
            description = "Returns the authenticated customer's purchase history."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of purchases",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PurchaseResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response listMyPurchases() {
        UUID customerId = userID();
        List<PurchaseResponse> purchases = purchaseService.listPurchases(customerId);
        return Response.ok(purchases).build();
    }

    @GET
    @Path("/{purchaseId}")
    @Operation(
            summary = "Get purchase details",
            description = "Returns details of a single purchase belonging to the authenticated customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase details",
                    content = @Content(schema = @Schema(implementation = PurchaseResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Purchase not found")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response getPurchase(
            @Parameter(description = "Purchase id", required = true, example = "10")
            @PathParam("purchaseId") Long purchaseId
    ) {
        UUID customerId = userID();
        PurchaseResponse p = purchaseService.getPurchase(customerId, purchaseId);
        return Response.ok(p).build();
    }
}
