/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.rest.server;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationPath("/api")
public class RestActivator extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(EchoResource.class);
    }
}
