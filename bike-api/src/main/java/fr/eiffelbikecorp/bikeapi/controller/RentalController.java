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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Path("/rentals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200")
@SecurityRequirement(name = "BearerAuth")
public class RentalController {

    private final RentalService rentalService;
    @Context
    ContainerRequestContext requestContext;

    @POST
    public Response rentBikeOrJoinWaitingList(@Valid RentBikeRequest request) {
        RentBikeResultResponse result = rentalService.rentBikeOrJoinWaitingList(request);
        if (result.result() == RentResult.RENTED) {
            return Response.status(Response.Status.CREATED).entity(result).build();
        }
        return Response.status(Response.Status.ACCEPTED).entity(result).build();
    }

    @POST
    @Path("/{rentalId}/return")
    public Response returnBike(
            @PathParam("rentalId") Long rentalId,
            @Valid ReturnBikeRequest request
    ) {
        ReturnBikeResponse response = rentalService.returnBike(rentalId, request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/active")
    public Response getActiveRentals() {
        UUID customerId = customerId();
        List<RentBikeResultResponse> activeRentals = rentalService.findActiveRentalsByCustomer(customerId);
        return Response.ok(activeRentals).build();
    }

    @GET
    @Path("/active/bikes")
    public Response getMyActiveBikeIds() {
        UUID customerId = customerId();
        return Response.ok(rentalService.findMyActiveBikeIds(customerId)).build();
    }

    @GET
    @Path("/waitlist")
    public Response getCustomerWaitlist() {
        UUID customerId = customerId();
        List<NotificationResponse> waitlist = rentalService.findWaitlistByCustomer(customerId);
        return Response.ok(waitlist).build();
    }

    @GET
    @Path("/notifications")
    public Response listNotifications() {
        UUID customerId = customerId();
        List<NotificationResponse> notifications = rentalService.listMyNotifications(customerId);
        return Response.ok(notifications).build();
    }

    @GET
    public Response listMyRentals() {
        UUID customerId = customerId();
        List<RentalResponse> rentals = rentalService.listMyRentals(customerId);
        return Response.ok(rentals).build();
    }

    private UUID customerId() {
        Object v = requestContext.getProperty("userId");
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
