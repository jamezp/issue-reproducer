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

import java.util.Collections;
import javax.annotation.security.RolesAllowed;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RolesAllowed("User")
@Path("test")
public class TestResource {

    @Path("client/response")
    @GET
    public Response clientResponse(@Context HttpServletRequest request) {
        return doClientRequest(Response.class, request, "error");
    }

    @Path("error")
    @GET
    public Response error() {
        throw new WebApplicationException(
                "Leaking confidential information",
                Response.status(500)
                        .header("confidential", "nuke-codes")
                        .cookie(NewCookie.valueOf("confidential=more-nuke-codes"))
                        .entity("even-more-nuke-codes")
                        .build());
    }

    @Path("client/string")
    @GET
    public String clientString(@Context HttpServletRequest request) {
        return doClientRequest(String.class, request, "error");
    }

    @Path("client/cleaned")
    @GET
    public Response cleaned(@Context HttpServletRequest request) {
        final Response clientResponse = doClientRequest(Response.class, request, "error");
        return Response.status(clientResponse.getStatus())
                .allow(clientResponse.getAllowedMethods())
                .entity(clientResponse.getEntity())
                .language(clientResponse.getLanguage())
                .tag(clientResponse.getEntityTag())
                .type(clientResponse.getMediaType())
                .build();
    }

    @Path("headers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestHeaders(@Context HttpServletRequest request) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (String name : Collections.list(request.getHeaderNames())) {
            builder.add(name, request.getHeader(name));
        }
        return Response.ok(builder.build())
                .header("confidential", "nuke-codes")
                .cookie(NewCookie.valueOf("confidential=more-nuke-codes"))
                .build();
    }

    @Path("client/headers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestHeadersClient(@Context HttpServletRequest request) {
        return doClientRequest(Response.class, request, "headers");
    }

    private <T> T doClientRequest(final Class<T> type, final HttpServletRequest request, final String... paths) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            return client.target(createUri(request, paths))
                    .request()
                    .header("Authorization", request.getHeader("Authorization"))
                    .get(type);
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
                .append("/rest/test");
        for (String path : paths) {
            result.append('/').append(path);
        }
        return result.toString();
    }
}
