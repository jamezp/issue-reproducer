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

package org.wildfly.reproducer;

import java.util.function.Consumer;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StdoutClient implements AutoCloseable, Consumer<String> {
    private final Authenticator authenticator;
    private final Client client;

    public StdoutClient(final String user, final char[] password) {
        authenticator = new Authenticator(user, new String(password));
        client = ClientBuilder.newClient();
    }

    @Override
    public void accept(final String url) {
        printResponse(url);
    }

    @Override
    public void close() {
        client.close();
    }

    private void printResponse(final String url) {
        try {
            final Response response = client.target(url)
                    .register(authenticator)
                    .request()
                    .get(Response.class);

            System.out.printf("***** %s *****%n", url);
            System.out.printf("Status: %s - %s%n", response.getStatus(), response.getStatusInfo());
            System.out.printf("Metadata: %s%n", response.getMetadata());
            System.out.printf("Headers: %s%n", response.getHeaders());
            System.out.printf("Cookies: %s%n", response.getCookies());
            System.out.println(response.readEntity(String.class));
            System.out.println();
        } catch (WebApplicationException e) {
            System.out.println("Caught exception: " + e);
        }
    }
}
