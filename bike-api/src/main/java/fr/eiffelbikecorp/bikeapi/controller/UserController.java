package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.dto.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserResponse;
import fr.eiffelbikecorp.bikeapi.service.UserService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    private final UserService userService;

    @POST
    @Path("/register")
    public Response register(@Valid UserRegisterRequest request) {
        UserResponse created = userService.register(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid UserLoginRequest request) {
        UserLoginResponse login = userService.login(request);
        return Response.ok(login).build();
    }
}
