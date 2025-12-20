package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.SaleOfferService;
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
@Path("/sales" )
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@CrossOrigin(origins = "http://localhost:4200") // Add this line
public class SaleOfferController {

    private final SaleOfferService saleOfferService;

    /**
     * US_04 + US_19:
     * EiffelBikeCorp lists a bike for sale (only corp bikes rented at least once).
     * <p>
     * POST /api/sales/offers
     * Body: { "bikeId": 1, "sellerCorpId": 100, "askingPriceEur": 250.00 }
     */
    @POST
    @Path("/offers" )
    @Secured
    public Response createSaleOffer(@Valid CreateSaleOfferRequest request) {
        SaleOfferResponse created = saleOfferService.createSaleOffer(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * US_04:
     * EiffelBikeCorp adds a note to a listed sale offer.
     * <p>
     * POST /api/sales/offers/notes
     * Body: { "saleOfferId": 10, "title": "...", "content": "...", "createdBy": "Alice" }
     */
    @POST
    @Path("/offers/notes" )
    @Secured
    public Response addSaleNote(@Valid CreateSaleNoteRequest request) {
        SaleNoteResponse created = saleOfferService.addSaleNote(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * US_11 + US_13 + US_14:
     * Customer searches/list sale offers (LISTED).
     * <p>
     * GET /api/sales/offers?q=mountain
     */
    @GET
    @Path("/offers" )
    public Response searchSaleOffers(@QueryParam("q" ) String q) {
        List<SaleOfferResponse> results = saleOfferService.searchSaleOffers(q);
        return Response.ok(results).build();
    }

    /**
     * US_12:
     * Customer views offer details + notes by offer id.
     * <p>
     * GET /api/sales/offers/{saleOfferId}
     */
    @GET
    @Path("/offers/{saleOfferId}" )
    public Response getSaleOfferDetails(@PathParam("saleOfferId" ) Long saleOfferId) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetails(saleOfferId);
        return Response.ok(details).build();
    }

    /**
     * US_12:
     * Customer views offer details + notes by bike id.
     * <p>
     * GET /api/sales/offers/by-bike/{bikeId}
     */
    @GET
    @Path("/offers/by-bike/{bikeId}" )
    public Response getSaleOfferDetailsByBike(@PathParam("bikeId" ) Long bikeId) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetailsByBike(bikeId);
        return Response.ok(details).build();
    }
}
