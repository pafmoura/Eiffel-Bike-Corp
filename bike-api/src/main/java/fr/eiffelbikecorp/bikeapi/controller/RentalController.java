package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.NotificationResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/rentals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200")
@Tag(
        name = "Rentals",
        description = "Renting flow: rent/waitlist/notifications/return/history"
)
@SecurityRequirement(name = "bearerAuth")
public class RentalController extends BaseController {

    private final RentalService rentalService;

    @POST
    @Operation(
            summary = "Rent a bike (or join waiting list)",
            description = """
                    Customer rents a bike (US_05). If the bike is unavailable, the customer is added to the waiting list (US_06).
                    Response is:
                    - 201 when result = RENTED
                    - 202 when result = WAITLISTED
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Bike rented successfully (result=RENTED)",
                    content = @Content(schema = @Schema(implementation = RentBikeResultResponse.class))
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Customer joined waiting list (result=WAITLISTED)",
                    content = @Content(schema = @Schema(implementation = RentBikeResultResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response rentBikeOrJoinWaitingList(@Valid
                                              @RequestBody(
                                                      required = true,
                                                      description = "Rent request payload",
                                                      content = @Content(schema = @Schema(implementation = RentBikeRequest.class))
                                              ) RentBikeRequest request) {
        RentBikeResultResponse result = rentalService.rentBikeOrJoinWaitingList(request);
        if (result.result() == RentResult.RENTED) {
            return Response.status(Response.Status.CREATED).entity(result).build();
        }
        return Response.status(Response.Status.ACCEPTED).entity(result).build();
    }

    @POST
    @Path("/{rentalId}/return")
    @Operation(
            summary = "Return a rented bike",
            description = "Customer returns a bike and may attach return notes/condition information (US_09)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bike returned",
                    content = @Content(schema = @Schema(implementation = ReturnBikeResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response returnBike(
            @Parameter(description = "Rental id", required = true, example = "100")
            @PathParam("rentalId") Long rentalId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Return request payload (may contain notes/condition)",
                    content = @Content(schema = @Schema(implementation = ReturnBikeRequest.class))
            )
            ReturnBikeRequest request
    ) {
        ReturnBikeResponse response = rentalService.returnBike(rentalId, request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/active")
    @Operation(
            summary = "Get active rentals for a customer",
            description = "Returns current active rentals for the given customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of active rentals",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RentBikeResultResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response getActiveRentals(
    ) {
        UUID customerId = customerId();
        List<RentBikeResultResponse> activeRentals = rentalService.findActiveRentalsByCustomer(customerId);
        return Response.ok(activeRentals).build();
    }

    @GET
    @Path("/active/bikes")
    @Operation(
            summary = "Get active bikes for a customer",
            description = "Returns current active bikes for the given customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of active rentals",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RentBikeResultResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response getMyActiveBikeIds() {
        UUID customerId = customerId();
        return Response.ok(rentalService.findMyActiveBikeIds(customerId)).build();
    }

    @GET
    @Path("/waitlist")
    @Operation(
            summary = "Get waiting list for a customer",
            description = "Returns the waiting list entries for the given customer (US_06)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Waiting list entries",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response getCustomerWaitlist(
    ) {
        UUID customerId = customerId();
        List<NotificationResponse> waitlist = rentalService.findWaitlistByCustomer(customerId);
        return Response.ok(waitlist).build();
    }

    @GET
    @Path("/notifications")
    @Operation(
            summary = "List notifications for a customer",
            description = "Returns notifications when a bike becomes available (US_07)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response listNotifications(
    ) {
        UUID customerId = customerId();
        List<NotificationResponse> notifications = rentalService.listMyNotifications(customerId);
        return Response.ok(notifications).build();
    }

    @GET
    @Operation(
            summary = "Get rent history for the authenticated customer",
            description = "Returns the rental history (what was rented and when) for the authenticated customer (US_21)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental history list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RentalResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY"})
    public Response listMyRentals() {
        UUID customerId = customerId();
        List<RentalResponse> rentals = rentalService.listMyRentals(customerId);
        return Response.ok(rentals).build();
    }
}
