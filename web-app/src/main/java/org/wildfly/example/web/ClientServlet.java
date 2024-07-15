/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.web;

import java.io.IOException;

import jakarta.json.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/client")
public class ClientServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try (Client client = ClientBuilder.newClient()) {
            final String url = "https://localhost:8443/api/echo";
            final var entity = Json.createObjectBuilder().add("test", "value").build();
            try (
                    var response = client.target(url)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(entity))
            ) {
                resp.setStatus(response.getStatus());
                resp.getOutputStream().println(response.readEntity(String.class));
            }
        }
    }
}
