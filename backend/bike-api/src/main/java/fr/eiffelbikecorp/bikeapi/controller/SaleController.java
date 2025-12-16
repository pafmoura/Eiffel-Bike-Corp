package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.service.SaleService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Path("/sales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SaleController {

    private final SaleService saleService;

    /**
     * US_04:
     * EiffelBikeCorp lists a bike for sale.
     *
     * POST /api/sales/offers
     * Body: { "bikeId": 1, "sellerCorpId": 100, "askingPriceEur": 250.00 }
     */
    @POST
    @Path("/offers")
    public Response createSaleOffer(@Valid CreateSaleOfferRequest request) {
        SaleOfferResponse created = saleService.createSaleOffer(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * US_04:
     * EiffelBikeCorp adds a note to a listed sale offer.
     *
     * POST /api/sales/notes
     * Body: { "saleOfferId": 10, "title": "...", "content": "...", "createdBy": "Alice" }
     */
    @POST
    @Path("/notes")
    public Response addSaleNote(@Valid CreateSaleNoteRequest request) {
        SaleNoteResponse created = saleService.addSaleNote(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * US_11:
     * Customer searches bikes to buy (LISTED sale offers).
     *
     * GET /api/sales/offers?q=mountain
     */
    @GET
    @Path("/offers")
    public Response searchSaleOffers(@QueryParam("q") String q) {
        List<SaleOfferResponse> results = saleService.searchSaleOffers(q);
        return Response.ok(results).build();
    }

    /**
     * US_12:
     * Customer views the sale offer and its notes for a given bike.
     *
     * GET /api/sales/bikes/{bikeId}
     */
    @GET
    @Path("/bikes/{bikeId}")
    public Response getSaleOfferDetailsByBike(@PathParam("bikeId") Long bikeId) {
        SaleOfferDetailsResponse details = saleService.getSaleOfferDetailsByBike(bikeId);
        return Response.ok(details).build();
    }

    /**
     * Alternative: details by offer id
     *
     * GET /api/sales/offers/{saleOfferId}
     */
    @GET
    @Path("/offers/{saleOfferId}")
    public Response getSaleOfferDetails(@PathParam("saleOfferId") Long saleOfferId) {
        SaleOfferDetailsResponse details = saleService.getSaleOfferDetails(saleOfferId);
        return Response.ok(details).build();
    }
}
