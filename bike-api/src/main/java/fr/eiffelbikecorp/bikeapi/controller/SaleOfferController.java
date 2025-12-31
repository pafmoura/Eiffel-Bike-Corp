package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleNoteRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleNoteResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferDetailsResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.SaleOfferService;
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

@Component
@RequiredArgsConstructor
@Path("/sale-offers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@CrossOrigin(origins = "http://localhost:4200")
@SecurityRequirement(name = "BearerAuth")
@Tag(
        name = "Sales",
        description = "Sale offers + notes + search + details"
)
public class SaleOfferController {

    private final SaleOfferService saleOfferService;

    @POST
    @Secured
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create a sale offer",
            description = """
                    Creates a sale offer for a bike.
                    Business rule: EiffelBikeCorp can list for sale ONLY corporate bikes that have been rented at least once.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Sale offer created",
                    content = @Content(schema = @Schema(implementation = SaleOfferResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Bike not found"),
            @ApiResponse(responseCode = "409", description = "Rule violated")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "EIFFEL_BIKE_CORP"})
    public Response createSaleOffer(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Create sale offer payload",
                    content = @Content(schema = @Schema(implementation = CreateSaleOfferRequest.class))
            )
            CreateSaleOfferRequest request
    ) {
        SaleOfferResponse created = saleOfferService.createSaleOffer(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/notes")
    @Secured
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add a note to a sale offer",
            description = "Adds a detailed note about a bike on sale so buyers can assess its condition."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Sale note created",
                    content = @Content(schema = @Schema(implementation = SaleNoteResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Sale offer not found"),
            @ApiResponse(responseCode = "409", description = "Offer not in a state that accepts notes")
    })
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "EIFFEL_BIKE_CORP"})
    public Response addSaleNote(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Create sale note payload",
                    content = @Content(schema = @Schema(implementation = CreateSaleNoteRequest.class))
            )
            CreateSaleNoteRequest request
    ) {
        SaleNoteResponse created = saleOfferService.addSaleNote(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Operation(
            summary = "Search bikes available to buy",
            description = """
                    Searches sale offers available to buy.
                    Use 'q' to search by bike description / keywords.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of matching sale offers",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SaleOfferResponse.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Response searchSaleOffers(
            @Parameter(
                    description = "Search by keyword",
                    example = "city",
                    required = false
            )
            @QueryParam("q") String q
    ) {
        List<SaleOfferResponse> results = saleOfferService.searchSaleOffers(q);
        return Response.ok(results).build();
    }

    @GET
    @Path("/{saleOfferId}")
    @Operation(
            summary = "Get sale offer details",
            description = """
                    Returns full details of a sale offer:
                    - price
                    - notes
                    - current status/availability
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sale offer details",
                    content = @Content(schema = @Schema(implementation = SaleOfferDetailsResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Sale offer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Response getSaleOfferDetails(
            @Parameter(description = "Sale offer id", required = true, example = "10")
            @PathParam("saleOfferId") Long saleOfferId
    ) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetails(saleOfferId);
        return Response.ok(details).build();
    }

    @GET
    @Path("/by-bike/{bikeId}")
    @Operation(
            summary = "Get sale offer details by bike id",
            description = "Convenience endpoint to find the sale offer for a bike and return its details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sale offer details",
                    content = @Content(schema = @Schema(implementation = SaleOfferDetailsResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Sale offer not found for this bike"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Secured
    @RolesAllowed(value = {"STUDENT", "EMPLOYEE", "ORDINARY", "EIFFEL_BIKE_CORP"})
    public Response getSaleOfferDetailsByBike(
            @Parameter(description = "Bike id", required = true, example = "100")
            @PathParam("bikeId") Long bikeId
    ) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetailsByBike(bikeId);
        return Response.ok(details).build();
    }
}