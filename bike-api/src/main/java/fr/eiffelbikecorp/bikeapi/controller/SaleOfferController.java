package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleNoteRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleNoteResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferDetailsResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferResponse;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.SaleOfferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@CrossOrigin(origins = "http://localhost:4200")
@SecurityRequirement(name = "BearerAuth")
public class SaleOfferController {

    private final SaleOfferService saleOfferService;

    @POST
    @Path("/offers" )
    @Secured
    public Response createSaleOffer(@Valid CreateSaleOfferRequest request) {
        SaleOfferResponse created = saleOfferService.createSaleOffer(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/offers/notes" )
    @Secured
    public Response addSaleNote(@Valid CreateSaleNoteRequest request) {
        SaleNoteResponse created = saleOfferService.addSaleNote(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/offers" )
    public Response searchSaleOffers(@QueryParam("q" ) String q) {
        List<SaleOfferResponse> results = saleOfferService.searchSaleOffers(q);
        return Response.ok(results).build();
    }

    @GET
    @Path("/offers/{saleOfferId}" )
    public Response getSaleOfferDetails(@PathParam("saleOfferId" ) Long saleOfferId) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetails(saleOfferId);
        return Response.ok(details).build();
    }

    @GET
    @Path("/offers/by-bike/{bikeId}" )
    public Response getSaleOfferDetailsByBike(@PathParam("bikeId" ) Long bikeId) {
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetailsByBike(bikeId);
        return Response.ok(details).build();
    }
}
