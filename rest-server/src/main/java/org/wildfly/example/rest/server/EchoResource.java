/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.rest.server;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("echo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class EchoResource {

    @Inject
    private HttpHeaders headers;

    @POST
    public JsonObject echo(final JsonObject payload) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonObjectBuilder headerBuilder = Json.createObjectBuilder();
        // Add the headers
        headers.getRequestHeaders().forEach((name, value) -> {
            headerBuilder.add(name, Json.createArrayBuilder(value));
        });
        builder.add("headers", headerBuilder);

        if (payload == null) {
            builder.addNull("payload").build();
        }
        return builder.add("payload", payload).build();
    }
}
