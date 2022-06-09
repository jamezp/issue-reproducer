/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/info")
@RequestScoped
public class InfoResource {

    @Inject
    private UriInfo uriInfo;

    @Inject
    private BeanManager beanManager;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response uriInfo() {
        if (uriInfo == null) {
            return Response.serverError().entity("UriInfo is null").build();
        }
        return Response.ok(uriInfo.getBaseUri()).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/bm")
    public Response beanManager() {
        if (beanManager == null) {
            return Response.serverError().entity("BeanManager is null").build();
        }
        return Response.ok(beanManager.toString()).build();
    }

}
