package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/rental-offers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200")
@SecurityRequirement(name = "BearerAuth")
@Tag(
        name = "Bikes",
        description = "Bike rental offers"
)
@SecurityRequirement(name = "bearerAuth")
public class RentalOfferController extends BaseController {

    private final BikeCatalogService bikeCatalogService;

    @POST
    @Operation(
            summary = "Offer a bike for rent",
            description = "Creates a bike available for rent. Used by Student/Employee/Corp providers."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Bike created",
                    content = @Content(schema = @Schema(implementation = BikeResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "EIFFEL_BIKE_CORP"})
    public Response offerBikeForRent(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Bike creation payload",
                    content = @Content(schema = @Schema(implementation = BikeCreateRequest.class))
            ) BikeCreateRequest request) {
        UUID offeredBy = userID();
        BikeResponse created = bikeCatalogService.offerBikeForRent(request, offeredBy);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
