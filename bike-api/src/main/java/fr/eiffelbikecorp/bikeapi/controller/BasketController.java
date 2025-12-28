package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.AddToBasketRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BasketService;
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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Path("/basket")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Secured
@Tag(
        name = "Basket",
        description = "Basket management for purchases (US_16 add items, US_17 remove items)"
)
@SecurityRequirement(name = "bearerAuth")
public class BasketController extends BaseController {

    private final BasketService basketService;

    @Context
    private ContainerRequestContext requestContext;

    @GET
    @Operation(
            summary = "Get my open basket",
            description = "Returns the authenticated customer's open basket. If none exists, creates one."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Open basket",
                    content = @Content(schema = @Schema(implementation = BasketResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response getOpenBasket() {
        UUID customerId = customerId();
        BasketResponse basket = basketService.getOrCreateOpenBasket(customerId);
        return Response.ok(basket).build();
    }

    @POST
    @Path("/items")
    @Operation(
            summary = "Add a sale offer to my basket",
            description = "Adds a bike (sale offer) to the authenticated customer's basket."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated basket",
                    content = @Content(schema = @Schema(implementation = BasketResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Sale offer not found"),
            @ApiResponse(responseCode = "409", description = "Sale offer not available / already in basket / invalid basket state")
    })
    public Response addToBasket(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Add-to-basket payload",
                    content = @Content(schema = @Schema(implementation = AddToBasketRequest.class))
            )
            AddToBasketRequest request
    ) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.addItem(customerId, request);
        return Response.ok(basket).build();
    }

    @DELETE
    @Path("/items/{saleOfferId}")
    @Operation(
            summary = "Remove a sale offer from my basket",
            description = "Removes a bike (sale offer) from the authenticated customer's basket."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated basket",
                    content = @Content(schema = @Schema(implementation = BasketResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Basket item not found")
    })
    public Response removeFromBasket(
            @Parameter(description = "Sale offer id", required = true, example = "10")
            @PathParam("saleOfferId") Long saleOfferId
    ) {
        UUID customerId = customerId();
        BasketResponse basket = basketService.removeItem(customerId, saleOfferId);
        return Response.ok(basket).build();
    }

    @DELETE
    @Operation(
            summary = "Clear my basket",
            description = "Removes all items from the authenticated customer's open basket."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated (cleared) basket",
                    content = @Content(schema = @Schema(implementation = BasketResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response clearBasket() {
        UUID customerId = customerId();
        BasketResponse basket = basketService.clear(customerId);
        return Response.ok(basket).build();
    }

}
