package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.NotificationResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.RentalService;
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
@CrossOrigin(origins = "http://localhost:4200") // Add this line
public class RentalController {

    private final RentalService rentalService;

    /**
     * US_06 + US_09:
     * Rent a bike if available, otherwise join the waiting list (FIFO).
     *
     * POST /api/rentals
     * Body: { "bikeId": 1, "customerId": 10, "days": 3 }
     *
     * Response indicates whether it was RENTED or WAITLISTED.
     */
    @POST
    public Response rentBikeOrJoinWaitingList(@Valid RentBikeRequest request) {
        RentBikeResultResponse result = rentalService.rentBikeOrJoinWaitingList(request);

        // 201 when rental created; 202 when put in waiting list (accepted)
        if (result.result() == RentResult.RENTED) {
            return Response.status(Response.Status.CREATED).entity(result).build();
        }
        return Response.status(Response.Status.ACCEPTED).entity(result).build();
    }

    /**
     * US_08 + (domain rule) close rental + add return note
     * plus US_10: notify next waiting customer and auto-create next rental (FIFO).
     *
     * POST /api/rentals/{rentalId}/return
     * Body: { "authorCustomerId": 10, "comment": "...", "condition": "Good" }
     */
    @POST
    @Path("/{rentalId}/return")
    public Response returnBike(
            @PathParam("rentalId") Long rentalId,
            @Valid ReturnBikeRequest request
    ) {
        ReturnBikeResponse response = rentalService.returnBike(rentalId, request);
        return Response.ok(response).build();
    }

    /**
     * US_10:
     * Customer checks notifications about bike availability.
     *
     * GET /api/notifications?customerId=10
     */
    @GET
    @Path("/notifications")
    public Response listNotifications(@QueryParam("customerId") UUID customerId) {
        if (customerId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Query param 'customerId' is required.")
                    .build();
        }

        List<NotificationResponse> notifications = rentalService.listMyNotifications(customerId);
        return Response.ok(notifications).build();
    }
}
