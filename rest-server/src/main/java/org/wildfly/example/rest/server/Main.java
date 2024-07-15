/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.rest.server;

import jakarta.ws.rs.SeBootstrap;

import org.wildfly.example.shared.ssl.SslUtil;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Main {
    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    public static void main(final String[] args) throws Exception {
        SslUtil.clean();
        final var configuration = SeBootstrap.Configuration.builder()
                .sslContext(SslUtil.createServerSslContext())
                .protocol("https")
                .port(8443)
                .build();
        final var instance = SeBootstrap.start(RestActivator.class, configuration)
                .toCompletableFuture().get();
        // Wait
        Thread.currentThread().join();
    }
}
