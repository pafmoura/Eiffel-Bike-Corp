package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.security.Secured;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
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
@CrossOrigin(origins = "http://localhost:4200") // Add this line
public class BikeCatalogController {

    private final BikeCatalogService bikeCatalogService;

    /**
     * US_01/US_02/US_03:
     * Student/Employee/EiffelBikeCorp offers a bike to rent.
     */
    @POST
    public Response offerBikeForRent(@Valid BikeCreateRequest request) {
        BikeResponse created = bikeCatalogService.offerBikeForRent(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Admin/provider maintenance endpoint (useful to change status, description, price).
     */
    @PUT
    @Path("/{bikeId}")
    public Response updateBike(@PathParam("bikeId") Long bikeId, @Valid BikeUpdateRequest request) {
        BikeResponse updated = bikeCatalogService.updateBike(bikeId, request);
        return Response.ok(updated).build();
    }

    /**
     * US_05:
     * Customer searches bikes to rent.
     * <p>
     * Examples:
     * - /api/bikes?status=AVAILABLE
     * - /api/bikes?q=mountain
     * - /api/bikes?status=AVAILABLE&q=trek&offeredById=12
     */
    @GET
    public Response searchBikesToRent(
            @QueryParam("status") String status,
            @QueryParam("q") String q,
            @QueryParam("offeredById") UUID offeredById
    ) {
        List<BikeResponse> results = bikeCatalogService.searchBikesToRent(status, q, offeredById);
        return Response.ok(results).build();
    }

    //find all bikes: Test purpose
    @GET
    @Path("/all")
    public Response findAllBikes() {
        List<BikeResponse> results = bikeCatalogService.findAll();
        return Response.ok(results).build();
    }
}
