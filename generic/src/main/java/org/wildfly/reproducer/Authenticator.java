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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Authenticator implements ClientRequestFilter {

    private final String user;
    private final String password;

    Authenticator(final String user, final String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) {
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        final String token = Base64.getEncoder().encodeToString((this.user + ":" + this.password).getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + token);
    }
}
