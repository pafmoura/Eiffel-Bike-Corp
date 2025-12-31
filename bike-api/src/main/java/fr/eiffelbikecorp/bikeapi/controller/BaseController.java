package fr.eiffelbikecorp.bikeapi.controller;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

public abstract class BaseController {
    @Context
    ContainerRequestContext requestContext;

    protected UUID userID() {
        Object v = requestContext.getProperty("userId");
        if (v instanceof UUID id) return id;
        throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
    }
}
