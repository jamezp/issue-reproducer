/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.reproducer.web.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/user")
@PermitAll
public class UserResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrent(@Context HttpServletRequest request) {
        return Response.ok(Json.createObjectBuilder()
                .add("request", createRequestJsonBuilder(request))
                .build())
                .build();
    }

    @GET
    @Path("/error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response throwError(@Context HttpServletRequest request) {
        throw new WebApplicationException("Failed on purpose", Response.serverError()
                .entity(createResponseJsonBuilder(getCurrent(request)).build())
                .build());
    }

    @GET
    @Path("/client/error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clientError(@Context HttpServletRequest request) {
        return doClientRequest(request, request.getHeader("Authorization"), "error");
    }

    @GET
    @Path("/client")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Admin")
    public Response getUsersWithClient(@Context HttpServletRequest request) {
        return doClientRequest(request, request.getHeader("Authorization"));
    }

    @GET
    @Path("/client/auth/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("User")
    public Response getUsersWithClientAuth(@Context HttpServletRequest request, @PathParam("user") String user) {
        final String token = String.format("%1$s:%1$s.12345", user);
        return doClientRequest(request, "Basic " +
                Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
    }

    private JsonObject createJson(final HttpServletRequest request, final Response response) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("request", createRequestJsonBuilder(request));
        builder.add("response", createResponseJsonBuilder(response));
        return builder.build();
    }

    private JsonObjectBuilder createRequestJsonBuilder(final HttpServletRequest request) {
        return Json.createObjectBuilder()
                .add("user", request.getUserPrincipal().getName())
                .add("remoteUser", request.getRemoteUser())
                .add("authType", request.getAuthType())
                .add("sessionId", request.getSession().getId());
    }

    private JsonObjectBuilder createResponseJsonBuilder(final Response response) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        // Add all headers
        final JsonObjectBuilder headerBuilder = Json.createObjectBuilder();
        response.getStringHeaders().forEach((name, value) -> headerBuilder.add(name, Json.createArrayBuilder(value)));
        builder.add("headers", headerBuilder);

        final JsonObjectBuilder cookieBuilder = Json.createObjectBuilder();
        response.getCookies().forEach((name, cookie) -> cookieBuilder.add(name, cookie.toString()));
        builder.add("cookies", cookieBuilder);

        if (response.getMediaType() != null && response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
            builder.add("entity", response.readEntity(JsonObject.class));
        } else {
            if (response.hasEntity()) {
                final Object entity;
                if (response.bufferEntity()) {
                    if (response.getMediaType() == MediaType.APPLICATION_JSON_TYPE) {
                        entity = response.readEntity(JsonObject.class);
                    } else {
                        entity = response.readEntity(String.class);
                    }
                } else {
                    entity = response.getEntity();
                }
                if (entity instanceof JsonObject) {
                    builder.add("entity", (JsonObject) entity);
                } else {
                    builder.add("entity", String.valueOf(entity));
                }
            }
        }
        return builder;
    }

    private Response doClientRequest(final HttpServletRequest request, final String auth, final String... paths) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            final Response response = client.target(createUri(request, paths))
                    .request()
                    .header("Authorization", auth)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                    .get();
            return Response.status(response.getStatus())
                    .entity(createJson(request, response))
                    .build();
        } finally {
            if (client != null) client.close();
        }
    }

    private String createUri(final ServletRequest request, final String... paths) {
        final StringBuilder result = new StringBuilder();
        result.append(request.getScheme())
                .append("://")
                .append(request.getServerName())
                .append(':')
                .append(request.getServerPort())
                .append(request.getServletContext().getContextPath())
                .append("/rest/user");
        for (String path : paths) {
            result.append('/').append(path);
        }
        return result.toString();
    }
}
