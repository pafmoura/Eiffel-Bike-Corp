package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnNoteResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Path("/bikes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@CrossOrigin(origins = "http://localhost:4200")
@SecurityRequirement(name = "BearerAuth")
@Tag(
        name = "Bikes",
        description = "Bike catalog and rental offers"
)
@SecurityRequirement(name = "bearerAuth")
public class BikeCatalogController {

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
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response offerBikeForRent(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Bike creation payload",
                    content = @Content(schema = @Schema(implementation = BikeCreateRequest.class))
            ) BikeCreateRequest request) {
        BikeResponse created = bikeCatalogService.offerBikeForRent(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{bikeId}")
    @Operation(
            summary = "Update a bike",
            description = "Updates a bike description/status/etc depending on the request model."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bike updated",
                    content = @Content(schema = @Schema(implementation = BikeResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Bike not found")
    })
    public Response updateBike(
            @Parameter(description = "Bike id", required = true, example = "10")
            @PathParam("bikeId") Long bikeId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Bike update payload",
                    content = @Content(schema = @Schema(implementation = BikeUpdateRequest.class))
            )
            BikeUpdateRequest request
    ) {
        BikeResponse updated = bikeCatalogService.updateBike(bikeId, request);
        return Response.ok(updated).build();
    }

    /**
     * Customer searches bikes to rent.
     * <p>
     * Examples:
     * - /api/bikes?status=AVAILABLE
     * - /api/bikes?q=mountain
     * - /api/bikes?status=AVAILABLE&q=trek&offeredById=12
     */
    @GET
    @Operation(
            summary = "Search bikes available to rent",
            description = """
                    Customer searches bikes to rent (US_04).
                    
                    Examples:
                    - /api/bikes?status=AVAILABLE
                    - /api/bikes?q=mountain
                    - /api/bikes?status=AVAILABLE&q=trek&offeredById=<uuid>
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of bikes matching filters",
                    content = @Content(schema = @Schema(implementation = BikeResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response searchBikesToRent(
            @Parameter(description = "Bike status filter", example = "AVAILABLE")
            @QueryParam("status") String status,
            @Parameter(description = "Search by text in description", example = "trek")
            @QueryParam("q") String q,
            @Parameter(description = "Filter by provider (UUID)", example = "11d91bd6-7fed-4aa2-9cf3-6da80c79e41c")
            @QueryParam("offeredById") UUID offeredById
    ) {
        List<BikeResponse> results = bikeCatalogService.searchBikesToRent(status, q, offeredById);
        return Response.ok(results).build();
    }

    @GET
    @Path("/all")
    @Operation(
            summary = "List all bikes",
            description = "Returns all bikes (only for test purposes)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of all bikes",
                    content = @Content(schema = @Schema(implementation = BikeResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response findAllBikes() {
        List<BikeResponse> results = bikeCatalogService.findAll();
        return Response.ok(results).build();
    }


    @GET
    @Path("/{bikeId}/return-notes")
    public Response getBikeReturnHistory(@PathParam("bikeId") Long bikeId) {
        List<ReturnNoteResponse> history = bikeCatalogService.getReturnNotesForBike(bikeId);
        return Response.ok(history).build();
    }
}
