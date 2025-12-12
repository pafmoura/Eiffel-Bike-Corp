package fr.univeiffel.bikerentalapi.catalog.resource;

import fr.univeiffel.bikerentalapi.catalog.dto.BikeRequest;
import fr.univeiffel.bikerentalapi.catalog.dto.BikeResponse;
import fr.univeiffel.bikerentalapi.catalog.model.Bike;
import fr.univeiffel.bikerentalapi.catalog.repository.BikeRepository;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/bikes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BikeResource {

    private final BikeRepository repo = new BikeRepository();

    @GET
    public List<BikeResponse> list() {
        return repo.findAll().stream()
                .map(b -> new BikeResponse(b.getId(), b.getDescription()))
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        return repo.findById(id)
                .map(b -> Response.ok(new BikeResponse(b.getId(), b.getDescription())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response create(BikeRequest req) {
        if (req == null || req.description() == null || req.description().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("description is required")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        Bike bike = repo.create(req.description().trim());
        return Response.created(URI.create("/api/bikes/" + bike.getId()))
                .entity(new BikeResponse(bike.getId(), bike.getDescription()))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, BikeRequest req) {
        if (req == null || req.description() == null || req.description().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("description is required")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        return repo.update(id, req.description().trim())
                .map(b -> Response.ok(new BikeResponse(b.getId(), b.getDescription())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        return repo.delete(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }
}
