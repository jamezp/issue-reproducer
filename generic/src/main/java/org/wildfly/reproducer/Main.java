/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Main {

    public static void main(final String[] args) throws Throwable {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        try (Client client = ClientBuilder.newClient()) {
            final Response response = client.target("https://localhost/web-app/rest/Customers?customer=1")
                    .request()
                    .get();
            System.out.println(response.getStatusInfo());
            System.out.println(response.readEntity(CustomerView.class));
        }
    }
}
